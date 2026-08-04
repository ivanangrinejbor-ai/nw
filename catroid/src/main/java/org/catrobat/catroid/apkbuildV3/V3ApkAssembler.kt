package org.catrobat.catroid.apkbuildV3

import android.content.Context
import android.util.Log
import com.reandroid.apk.ApkModule
import com.reandroid.archive.FileInputSource
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.util.Date
import com.android.apksig.ApkSigner
import com.android.apksig.ApkSigner.SignerConfig

object V3ApkAssembler {
    private const val TAG = "V3ApkAssembler"

    private const val ATTR_NAME = 0x01010003
    private const val ATTR_LABEL = 0x01010001
    private const val ATTR_DEBUGGABLE = 0x0101000f
    private const val ATTR_TEST_ONLY = 0x01010272
    private const val ATTR_EXPORTED = 0x01010010
    private const val ATTR_AUTHORITIES = 0x01010018

    private const val ACTION_MAIN = "android.intent.action.MAIN"
    private const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"

    private const val RUNTIME_LOADER = "org.catrobat.catroid.apkbuildV3.runtime.RuntimeLoaderActivityV3"
    private const val ASSET_PAYLOAD = "project.ncv3"
    private const val FULL_MARKER = "template_v3_full.marker"

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun assemble(
        context: Context,
        config: ApkBuilderV3Config,
        encryptedPayload: File,
        keyFileName: String,
        keyContent: String,
        workDir: File,
        firebaseConfig: FirebaseConfig? = null,
        onProgress: ((Float, String) -> Unit)? = null
    ): File {
        onProgress?.invoke(0f, "")
        workDir.mkdirs()

        val baseApk = locateBaseApk(context, workDir)
        onProgress?.invoke(0.15f, baseApk.name)

        val injectedApk = File(workDir, "v3_injected.apk")
        injectAssets(baseApk, injectedApk, encryptedPayload, keyFileName, keyContent, config.templateType)
        onProgress?.invoke(0.4f, injectedApk.name)

        val patchedApk = File(workDir, "v3_patched.apk")
        patchManifest(injectedApk, patchedApk, config)
        onProgress?.invoke(0.55f, patchedApk.name)

        val firebaseApk = if (firebaseConfig != null) {
            val fApk = File(workDir, "v3_firebase.apk")
            injectFirebaseConfig(patchedApk, fApk, firebaseConfig, config.packageName)
            fApk
        } else {
            patchedApk
        }
        onProgress?.invoke(0.7f, firebaseApk.name)

        val signedApk = File(workDir, "v3_signed.apk")
        val keystore = File(workDir, "v3_keystore.jks")
        signApk(firebaseApk, signedApk, keystore, "neocatroidv3", "keystore")
        onProgress?.invoke(1f, signedApk.name)

        Log.i(TAG, "APK собран: ${signedApk.absolutePath} (${signedApk.length() / (1024 * 1024)} MB)")
        return signedApk
    }

    private fun locateBaseApk(context: Context, workDir: File): File {
        return TemplateManagerV3.prepareBaseApk(context, workDir)
    }

    private fun forceArscUncompressed(module: ApkModule) {
        try {
            module.zipEntryMap.getInputSource("resources.arsc")?.isUncompressed = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not force resources.arsc uncompressed", e)
        }
    }

    private fun injectAssets(
        baseApk: File,
        outApk: File,
        payload: File,
        keyFileName: String,
        keyContent: String,
        templateType: TemplateType
    ) {
        baseApk.copyTo(outApk, overwrite = true)

        ApkModule.loadApkFile(outApk).use { module ->
            module.zipEntryMap.remove("assets/project")
            module.zipEntryMap.remove("assets/project.zip")
            module.zipEntryMap.remove("assets/$ASSET_PAYLOAD")

            module.add(FileInputSource(payload, "assets/$ASSET_PAYLOAD"))

            val keyFile = File.createTempFile("v3_key_", ".tmp")
            keyFile.writeText(keyContent)
            module.add(FileInputSource(keyFile, "assets/$keyFileName"))

            if (templateType == TemplateType.FULL) {
                val marker = File.createTempFile("v3_marker_", ".tmp")
                marker.writeText("FULL")
                module.add(FileInputSource(marker, "assets/$FULL_MARKER"))
            }

            forceArscUncompressed(module)
            module.writeApk(outApk)
        }
    }

    private fun patchManifest(baseApk: File, outApk: File, config: ApkBuilderV3Config) {
        baseApk.copyTo(outApk, overwrite = true)

        ApkModule.loadApkFile(outApk).use { module ->
            val manifest = module.androidManifest

            if (config.packageName.isNotBlank()) {
                applyPackageRename(manifest, config.packageName)
            }

            manifest.versionCode = config.versionCode
            manifest.versionName = config.versionName

            manifest.minSdkVersion = config.minSdk
            manifest.targetSdkVersion = config.targetSdk

            val appElem = manifest.applicationElement
            appElem.getOrCreateAndroidAttribute("label", ATTR_LABEL)
                .setValueAsString(config.appName)

            appElem.getOrCreateAndroidAttribute("debuggable", ATTR_DEBUGGABLE)
                .setValueAsBoolean(config.debuggable)
            appElem.getOrCreateAndroidAttribute("testOnly", ATTR_TEST_ONLY)
                .setValueAsBoolean(false)

            config.permissions.forEach { perm ->
                manifest.addUsesPermission(perm)
            }

            makeRuntimeLoaderLauncher(manifest)

            forceArscUncompressed(module)
            module.writeApk(outApk)
        }
    }

    internal fun makeRuntimeLoaderLauncher(manifest: com.reandroid.arsc.chunk.xml.AndroidManifestBlock) {
        val appElem = manifest.applicationElement

        val activities = appElem.listElements("activity")
        for (act in activities) {
            val filters = act.listElements("intent-filter")
            for (filter in filters) {
                val categories = filter.listElements("category")
                for (cat in categories) {
                    val nameAttr = cat.searchAttributeByResourceId(ATTR_NAME)
                    if (nameAttr?.valueAsString == CATEGORY_LAUNCHER) {
                        filter.removeSelf()
                        break
                    }
                }
            }
        }

        val loaderElem = appElem.createChildElement("activity")
        loaderElem.getOrCreateAndroidAttribute("name", ATTR_NAME)
            .setValueAsString(RUNTIME_LOADER)
        loaderElem.getOrCreateAndroidAttribute("exported", ATTR_EXPORTED)
            .setValueAsBoolean(true)

        val filter = loaderElem.createChildElement("intent-filter")
        val action = filter.createChildElement("action")
        action.getOrCreateAndroidAttribute("name", ATTR_NAME)
            .setValueAsString(ACTION_MAIN)
        val category = filter.createChildElement("category")
        category.getOrCreateAndroidAttribute("name", ATTR_NAME)
            .setValueAsString(CATEGORY_LAUNCHER)
    }

    internal fun applyPackageRename(manifest: AndroidManifestBlock, newPackage: String) {
        val oldPackage = manifest.packageName ?: return
        if (newPackage == oldPackage) return

        manifest.ensureFullClassNames()

        val appClassName = manifest.applicationClassName
        if (appClassName != null && appClassName.startsWith(".")) {
            manifest.applicationClassName = manifest.fullClassName(appClassName)
        }

        for (provider in manifest.listApplicationElementsByTag("provider")) {
            val attr = provider.searchAttributeByResourceId(ATTR_AUTHORITIES)
            val oldAuth = attr?.getValueString()
            if (oldAuth != null) {
                attr.setValueAsString(replacePackageInAuthority(oldAuth, oldPackage, newPackage))
            }
        }

        manifest.packageName = newPackage
    }

    internal fun replacePackageInAuthority(authority: String, oldPackage: String, newPackage: String): String {
        return authority.split(';').joinToString(";") { segment ->
            when {
                segment == oldPackage -> newPackage
                segment.startsWith("$oldPackage.") -> newPackage + segment.substring(oldPackage.length)
                else -> segment
            }
        }
    }

    private fun injectFirebaseConfig(
        inputApk: File,
        outputApk: File,
        firebaseConfig: FirebaseConfig,
        targetPackage: String
    ) {
        inputApk.copyTo(outputApk, overwrite = true)

        ApkModule.loadApkFile(outputApk).use { module ->
            val table = module.getTableBlock(false) ?: run {
                Log.w(TAG, "Cannot inject Firebase config: no TableBlock found")
                return
            }

            val replacements = mapOf(
                "google_app_id" to firebaseConfig.mobileSdkAppId,
                "gcm_defaultSenderId" to firebaseConfig.projectNumber,
                "google_api_key" to firebaseConfig.apiKey,
                "google_crash_reporting_api_key" to firebaseConfig.apiKey,
                "project_id" to firebaseConfig.projectId,
                "google_storage_bucket" to firebaseConfig.storageBucket,
                "firebase_database_url" to firebaseConfig.databaseUrl,
                "default_web_client_id" to firebaseConfig.defaultWebClientId
            )

            var updatedCount = 0
            for ((resName, resValue) in replacements) {
                if (resValue.isBlank()) {
                    Log.d(TAG, "Firebase resource '$resName' has empty value, skipping")
                    continue
                }
                try {
                    val entry = table.getEntry("string", resName, "")
                    if (entry != null) {
                        entry.resValue?.setValueAsString(resValue)
                        updatedCount++
                        Log.d(TAG, "Updated Firebase resource: $resName = $resValue")
                    } else {
                        Log.w(TAG, "Firebase resource '$resName' not found in resources.arsc")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update Firebase resource '$resName'", e)
                }
            }

            Log.i(TAG, "Firebase config injected: $updatedCount resources updated")
            forceArscUncompressed(module)
            module.writeApk(outputApk)
        }
    }

    private fun signApk(inputApk: File, outputApk: File, keystoreFile: File, alias: String, password: String) {
        doSign(inputApk, outputApk, keystoreFile, alias, password)
        Log.i(TAG, "APK подписан: ${outputApk.absolutePath}")
    }

    internal fun doSign(inputApk: File, outputApk: File, keystoreFile: File, alias: String, password: String) {
        val bc = BouncyCastleProvider()

        val kpGen = KeyPairGenerator.getInstance("RSA", bc)
        kpGen.initialize(2048)
        val kp = kpGen.generateKeyPair()

        val now = Date()
        val validityDays = 3650L * 24 * 60 * 60 * 1000
        val certBuilder = JcaX509v3CertificateBuilder(
            X500Name("CN=NeoCatroid V3, O=NeoCatroid, C=US"),
            BigInteger.valueOf(System.currentTimeMillis()),
            Date(now.time - 1000),
            Date(now.time + validityDays),
            X500Name("CN=NeoCatroid V3, O=NeoCatroid, C=US"),
            kp.public
        )
        val signer = JcaContentSignerBuilder("SHA256WithRSA")
            .setProvider(bc)
            .build(kp.private)
        val certificate = JcaX509CertificateConverter()
            .setProvider(bc)
            .getCertificate(certBuilder.build(signer))

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, null)
        keyStore.setKeyEntry(alias, kp.private, password.toCharArray(), arrayOf(certificate))
        keystoreFile.outputStream().use { keyStore.store(it, password.toCharArray()) }

        val signerConfig = SignerConfig.Builder(alias, kp.private as PrivateKey, listOf(certificate)).build()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(inputApk)
            .setOutputApk(outputApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }
}
