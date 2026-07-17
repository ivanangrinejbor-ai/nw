package org.catrobat.catroid.apkbuildV3

import android.content.Context
import android.util.Log
import com.reandroid.apk.ApkModule
import com.reandroid.archive.FileInputSource
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlElement
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
import java.security.cert.X509Certificate
import java.util.Date
import com.android.apksig.ApkSigner
import com.android.apksig.ApkSigner.SignerConfig

/**
 * Собственный сборщик APK для APK Builder V3 (написан с нуля).
 *
 * Не использует существующий модуль apkbuild. Работает напрямую с библиотеками:
 *  - reandroid (через APKEditor) — для чтения/записи AndroidManifest и добавления файлов в APK;
 *  - apksig — для подписи APK (v1+v2+v3);
 *  - bouncycastle — для генерации ключевой пары и самоподписанного сертификата.
 *
 * Этапы сборки:
 *  1) Берём базовый APK (собственное приложение NeoCatroid или template_runtime.apk из assets).
 *  2) Вливаем зашифрованный проект (assets/project.ncv3) и ключ (assets/<random>.k3y).
 *  3) При необходимости кладём маркер шаблона (FULL / LIGHT).
 *  4) Патчим AndroidManifest: package, appName, version, minSdk/targetSdk, permissions,
 *     делаем RuntimeLoaderActivityV3 главным launcher-ом.
 *  5) Подписываем APK своим сгенерированным ключом.
 */
object V3ApkAssembler {
    private const val TAG = "V3ApkAssembler"

    // Android resource id для атрибутов манифеста
    private const val ATTR_NAME = 0x01010003
    private const val ATTR_LABEL = 0x01010001
    private const val ATTR_DEBUGGABLE = 0x0101000f
    private const val ATTR_TEST_ONLY = 0x01010272
    private const val ATTR_EXPORTED = 0x01010010
    private const val ATTR_AUTHORITIES = 0x01010018

    private const val ACTION_MAIN = "android.intent.action.MAIN"
    private const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"

    // Full class name — the launcher activity is always referenced by its real,
    // fully-qualified name so it keeps resolving after the manifest package is renamed.
    private const val RUNTIME_LOADER = "org.catrobat.catroid.apkbuildV3.runtime.RuntimeLoaderActivityV3"
    private const val ASSET_PAYLOAD = "project.ncv3"
    private const val FULL_MARKER = "template_v3_full.marker"

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * Собирает итоговый APK.
     *
     * @param context          контекст
     * @param config           конфиг сборки
     * @param encryptedPayload зашифрованный проект (project.ncv3)
     * @param keyFileName      имя файла ключа в assets
     * @param keyContent       содержимое файла ключа (обфусцированная строка)
     * @param workDir          рабочая директория
     * @param onProgress       обратный вызов прогресса (0.0..1.0)
     * @return собранный (подписанный) APK или null при ошибке
     */
    fun assemble(
        context: Context,
        config: ApkBuilderV3Config,
        encryptedPayload: File,
        keyFileName: String,
        keyContent: String,
        workDir: File,
        firebaseConfig: FirebaseConfig? = null,
        onProgress: ((Float) -> Unit)? = null
    ): File {
        onProgress?.invoke(0f)
        workDir.mkdirs()

        // ── 1. Базовый APK ──
        // locateBaseApk бросает IllegalStateException с описанием причины, если
        // ни template_runtime.apk из assets, ни собственный APK недоступны.
        val baseApk = locateBaseApk(context, workDir)
        onProgress?.invoke(0.15f)

        // ── 2. Вливаем payload + key + marker ──
        val injectedApk = File(workDir, "v3_injected.apk")
        injectAssets(baseApk, injectedApk, encryptedPayload, keyFileName, keyContent, config.templateType)
        onProgress?.invoke(0.4f)

        // ── 3. Патчим манифест ──
        val patchedApk = File(workDir, "v3_patched.apk")
        patchManifest(injectedApk, patchedApk, config)
        onProgress?.invoke(0.55f)

        // ── 4. Внедряем Firebase-конфиг (если выбран) ──
        val firebaseApk = if (firebaseConfig != null) {
            val fApk = File(workDir, "v3_firebase.apk")
            injectFirebaseConfig(patchedApk, fApk, firebaseConfig, config.packageName)
            fApk
        } else {
            patchedApk
        }
        onProgress?.invoke(0.7f)

        // ── 5. Подписываем ──
        val signedApk = File(workDir, "v3_signed.apk")
        val keystore = File(workDir, "v3_keystore.jks")
        signApk(firebaseApk, signedApk, keystore, "neocatroidv3", "keystore")
        onProgress?.invoke(1f)

        Log.i(TAG, "APK собран: ${signedApk.absolutePath} (${signedApk.length() / (1024 * 1024)} MB)")
        return signedApk
    }

    /**
     * Находит базовый APK, делегируя локатору шаблона.
     * Пробрасывает исключение дальше, чтобы вызывающий код видел реальную причину.
     */
    private fun locateBaseApk(context: Context, workDir: File): File {
        return TemplateManagerV3.prepareBaseApk(context, workDir)
    }

    /**
     * Вливает assets (payload, key, marker) в копию APK.
     */
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
            // Удаляем старые project-ассеты (если есть)
            module.zipEntryMap.remove("assets/project")
            module.zipEntryMap.remove("assets/project.zip")
            module.zipEntryMap.remove("assets/$ASSET_PAYLOAD")

            // Вливаем зашифрованный проект
            module.add(FileInputSource(payload, "assets/$ASSET_PAYLOAD"))

            // Вливаем файл ключа
            val keyFile = File.createTempFile("v3_key_", ".tmp")
            keyFile.writeText(keyContent)
            module.add(FileInputSource(keyFile, "assets/$keyFileName"))

            // Маркер шаблона (FULL или ничего для LIGHT)
            if (templateType == TemplateType.FULL) {
                val marker = File.createTempFile("v3_marker_", ".tmp")
                marker.writeText("FULL")
                module.add(FileInputSource(marker, "assets/$FULL_MARKER"))
            }

            module.writeApk(outApk)
        }
    }

    /**
     * Патчит AndroidManifest: package, имя, версия, SDK, права, launcher-activity.
     */
    private fun patchManifest(baseApk: File, outApk: File, config: ApkBuilderV3Config) {
        baseApk.copyTo(outApk, overwrite = true)

        ApkModule.loadApkFile(outApk).use { module ->
            val manifest = module.androidManifest

            // Replace the application package name (fully qualifies every relative
            // component name and rewrites provider authorities so the APK keeps
            // installing and launching under the new identity).
            if (config.packageName.isNotBlank()) {
                applyPackageRename(manifest, config.packageName)
            }

            // Версия
            manifest.versionCode = config.versionCode
            manifest.versionName = config.versionName

            // SDK
            manifest.minSdkVersion = config.minSdk
            manifest.targetSdkVersion = config.targetSdk

            // Имя приложения
            val appElem = manifest.applicationElement
            appElem.getOrCreateAndroidAttribute("label", ATTR_LABEL)
                .setValueAsString(config.appName)

            // Снимаем debuggable и testOnly (чтобы APK ставился обычным установщиком)
            appElem.getOrCreateAndroidAttribute("debuggable", ATTR_DEBUGGABLE)
                .setValueAsBoolean(false)
            appElem.getOrCreateAndroidAttribute("testOnly", ATTR_TEST_ONLY)
                .setValueAsBoolean(false)

            // Права
            config.permissions.forEach { perm ->
                manifest.addUsesPermission(perm)
            }

            // Делаем RuntimeLoaderActivityV3 главным launcher-ом
            makeRuntimeLoaderLauncher(manifest)

            module.writeApk(outApk)
        }
    }

    /**
     * Делает RuntimeLoaderActivityV3 единственным LAUNCHER-ом:
     *  - убирает CATEGORY_LAUNCHER у всех существующих activity;
     *  - добавляет новый <activity> с LAUNCHER-фильтром.
     */
    internal fun makeRuntimeLoaderLauncher(manifest: com.reandroid.arsc.chunk.xml.AndroidManifestBlock) {
        val appElem = manifest.applicationElement

        // 1) Снимаем CATEGORY_LAUNCHER у всех существующих activity
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

        // 2) Добавляем новый <activity> с LAUNCHER-фильтром
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

    /**
     * Replaces the application package name throughout the manifest while keeping
     * the APK installable and runnable.
     *
     * Android resolves resource references by resource id (not by package name),
     * so renaming the manifest `package` does NOT break the `R.*` lookups baked
     * into the dex. What actually depends on the package name is:
     *  1. Component class names declared as relative (".Foo") — they are resolved
     *     against the manifest `package`, so they must be rewritten to fully
     *     qualified names (oldPackage.Foo) BEFORE the package is changed.
     *  2. Provider authorities that embed the package (e.g. "<pkg>.fileProvider") —
     *     they must be rewritten to the new package so the app can actually obtain
     *     a FileProvider uri at runtime, and so that two exported games with
     *     different packages do not declare a colliding authority.
     *
     * Order matters: qualify names and rewrite authorities while the OLD package is
     * still set, then change the package last.
     */
    internal fun applyPackageRename(manifest: AndroidManifestBlock, newPackage: String) {
        val oldPackage = manifest.packageName ?: return
        if (newPackage == oldPackage) return

        // 1) Qualify every relative class name (android:name) to a fully qualified
        //    name against the OLD package. Must run before the package changes.
        manifest.ensureFullClassNames()

        // Defensive: ensure the <application android:name> is also qualified.
        val appClassName = manifest.applicationClassName
        if (appClassName != null && appClassName.startsWith(".")) {
            manifest.applicationClassName = manifest.fullClassName(appClassName)
        }

        // 2) Rewrite provider authorities that embed the old package.
        for (provider in manifest.listApplicationElementsByTag("provider")) {
            val attr = provider.searchAttributeByResourceId(ATTR_AUTHORITIES)
            val oldAuth = attr?.getValueString()
            if (oldAuth != null) {
                attr.setValueAsString(replacePackageInAuthority(oldAuth, oldPackage, newPackage))
            }
        }

        // 3) Finally change the package name itself.
        manifest.packageName = newPackage
    }

    /**
     * Rewrites a provider authority string (possibly a ';'-separated list) so that
     * every segment equal to [oldPackage] or prefixed with "<oldPackage>." becomes
     * the corresponding new-package segment. Other segments are left untouched.
     */
    internal fun replacePackageInAuthority(authority: String, oldPackage: String, newPackage: String): String {
        return authority.split(';').joinToString(";") { segment ->
            when {
                segment == oldPackage -> newPackage
                segment.startsWith("$oldPackage.") -> newPackage + segment.substring(oldPackage.length)
                else -> segment
            }
        }
    }

    /**
     * Внедряет Firebase конфигурацию в ресурсы APK.
     * Обновляет существующие строковые ресурсы Firebase в resources.arsc
     * значениями из выбранного пользователем google-services.json.
     */
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
            module.writeApk(outputApk)
        }
    }

    /**
     * Подписывает APK своим сгенерированным ключом (apksig, v1+v2+v3).
     */
    private fun signApk(inputApk: File, outputApk: File, keystoreFile: File, alias: String, password: String) {
        doSign(inputApk, outputApk, keystoreFile, alias, password)
        Log.i(TAG, "APK подписан: ${outputApk.absolutePath}")
    }

    /**
     * Pure (Android-framework-free) signing routine: generates an RSA key pair +
     * self-signed certificate and signs the APK with apksig (v1+v2+v3).
     * Extracted from [signApk] so it can be unit-tested on the JVM.
     */
    internal fun doSign(inputApk: File, outputApk: File, keystoreFile: File, alias: String, password: String) {
        // Use the BouncyCastle provider INSTANCE (not the name "BC"): on Android a
        // provider already registered under "BC" is the stripped-down platform
        // provider, which does not implement BC's content-signer algorithm, so
        // referencing it by name yields "NoSuchAlgorithmException: SHA256WithRSA".
        val bc = BouncyCastleProvider()

        // Генерируем ключевую пару и сертификат
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

        // Сохраняем в keystore (PKCS12)
        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, null)
        keyStore.setKeyEntry(alias, kp.private, password.toCharArray(), arrayOf(certificate))
        keystoreFile.outputStream().use { keyStore.store(it, password.toCharArray()) }

        // Подписываем apksig
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
