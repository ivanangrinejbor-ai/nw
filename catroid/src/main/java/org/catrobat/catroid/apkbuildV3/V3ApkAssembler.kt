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
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.Flushable
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.util.Date
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import com.android.apksig.ApkSigner
import com.android.apksig.ApkSigner.SignerConfig
import com.android.apksig.ApkVerifier

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
        keyFileNames: List<String>,
        keyFileContents: List<String>,
        workDir: File,
        firebaseConfig: FirebaseConfig? = null,
        onProgress: ((Float, String) -> Unit)? = null
    ): File {
        onProgress?.invoke(0f, "")
        workDir.mkdirs()

        val baseApk = locateBaseApk(context, workDir, onProgress)
        onProgress?.invoke(0.15f, baseApk.name)

        val injectedApk = File(workDir, "v3_injected.apk")
        injectAssets(baseApk, injectedApk, encryptedPayload, keyFileNames, keyFileContents, config.templateType, workDir)
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

        val iconFile = config.iconFile
        val iconApk = if (iconFile != null) {
            val iApk = File(workDir, "v3_icon.apk")
            if (injectAppIcon(firebaseApk, iApk, iconFile)) iApk else firebaseApk
        } else {
            firebaseApk
        }
        onProgress?.invoke(0.8f, iconApk.name)

        onProgress?.invoke(0.85f, iconApk.name)

        val alignedApk = File(workDir, "v3_aligned.apk")
        normalizeApkForInstall(iconApk, alignedApk, File(workDir, "v3_norm_tmp"))
        onProgress?.invoke(0.9f, alignedApk.name)

        val signedApk = File(workDir, "v3_signed.apk")
        val keystore = File(workDir, "v3_keystore.jks")
        signApk(alignedApk, signedApk, keystore, "neocatroidv3", "keystore")
        onProgress?.invoke(1f, signedApk.name)

        verifyInstallable(signedApk)

        Log.i(TAG, "APK собран: ${signedApk.absolutePath} (${signedApk.length() / (1024 * 1024)} MB)")
        return signedApk
    }

    private fun locateBaseApk(
        context: Context,
        workDir: File,
        onProgress: ((Float, String) -> Unit)? = null
    ): File {
        return TemplateManagerV3.prepareBaseApk(context, workDir) { p, msg ->
            onProgress?.invoke(p * 0.15f, msg)
        }
    }

    internal fun injectAssets(
        baseApk: File,
        outApk: File,
        payload: File,
        keyFileNames: List<String>,
        keyFileContents: List<String>,
        templateType: TemplateType,
        workDir: File
    ) {
        ApkModule.loadApkFile(baseApk).use { module ->
            module.setLoadDefaultFramework(false)

            for (old in listOf("assets/project", "assets/project.zip", "assets/$ASSET_PAYLOAD", "assets/$FULL_MARKER")) {
                runCatching { module.removeInputSource(old) }
                runCatching { module.removeDir(old) }
            }

            module.add(FileInputSource(payload, "assets/$ASSET_PAYLOAD"))

            for ((idx, fileName) in keyFileNames.withIndex()) {
                val content = keyFileContents.getOrElse(idx) { "" }.toByteArray()
                val keyFile = File(workDir, fileName)
                keyFile.writeBytes(content)
                module.add(FileInputSource(keyFile, "assets/$fileName"))
            }

            if (templateType == TemplateType.FULL) {
                val markerFile = File(workDir, FULL_MARKER)
                markerFile.writeText("FULL")
                module.add(FileInputSource(markerFile, "assets/$FULL_MARKER"))
            }

            module.writeApk(outApk)
        }
    }

    internal fun injectAppIcon(inputApk: File, outputApk: File, iconFile: File): Boolean {
        if (!iconFile.exists()) {
            Log.w(TAG, "App icon file not found, keeping template icon: ${iconFile.absolutePath}")
            return false
        }
        if (!inputApk.exists() || inputApk.length() < 4) {
            Log.w(TAG, "Input APK missing, keeping template icon")
            return false
        }
        val tmpFile = File(outputApk.absolutePath + ".tmp")
        return try {
            if (outputApk.exists()) outputApk.delete()
            val targets = mutableListOf<String>()
            ApkModule.loadApkFile(inputApk).use { module ->
                module.setLoadDefaultFramework(false)
                targets.addAll(module.zipEntryMap.listInputSources()
                    .map { it.name }
                    .filter { path ->
                        val fileName = path.substringAfterLast('/')
                        fileName.startsWith("ic_launcher") && fileName.endsWith(".png", ignoreCase = true)
                    })
                if (targets.isEmpty()) {
                    Log.w(TAG, "No launcher icon entries found, keeping template icon")
                    return false
                }
                for (pathInApk in targets) {
                    runCatching { module.removeInputSource(pathInApk) }
                    runCatching { module.zipEntryMap.remove(pathInApk) }
                    module.add(FileInputSource(iconFile, pathInApk))
                }
                module.writeApk(tmpFile)
            }
            if (!tmpFile.exists() || tmpFile.length() < 4) {
                Log.e(TAG, "Icon injection produced empty file")
                tmpFile.delete()
                return false
            }
            if (outputApk.exists()) outputApk.delete()
            if (!tmpFile.renameTo(outputApk)) {
                tmpFile.copyTo(outputApk, overwrite = true)
                tmpFile.delete()
            }
            Log.i(TAG, "App icon injected into ${targets.size} entries")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject app icon, keeping template icon", e)
            runCatching { tmpFile.delete() }
            runCatching { if (outputApk.exists() && outputApk.length() < 4) outputApk.delete() }
            false
        }
    }

    internal fun patchManifest(injectedApk: File, outApk: File, config: ApkBuilderV3Config) {
        try {
            ApkModule.loadApkFile(injectedApk).use { module ->
                module.setLoadDefaultFramework(false)
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

                syncPermissions(manifest, config.permissions)

                makeRuntimeLoaderLauncher(manifest)

                module.refreshManifest()
                module.writeApk(outApk)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to patch manifest", e)
            throw e
        }
    }

    internal fun syncPermissions(manifest: AndroidManifestBlock, permissions: List<String>) {
        val root = manifest.manifestElement
        if (root != null) {
            root.listElements("uses-permission").toList().forEach { root.remove(it) }
        }
        for (perm in permissions.distinct()) {
            manifest.addUsesPermission(perm)
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

    internal fun injectFirebaseConfig(
        inputApk: File,
        outputApk: File,
        firebaseConfig: FirebaseConfig,
        targetPackage: String
    ) {
        try {
            ApkModule.loadApkFile(inputApk).use { module ->
                module.setLoadDefaultFramework(false)
                val table = module.getTableBlock(false)

                if (table == null) {
                    Log.w(TAG, "Cannot inject Firebase config: no TableBlock found")
                    module.writeApk(outputApk)
                    return@use
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
                module.refreshTable()
                module.writeApk(outputApk)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to inject Firebase config", e)
            throw e
        }
    }

    internal fun normalizeApkForInstall(inputApk: File, outputApk: File, tmpDir: File) {
        require(inputApk.exists() && inputApk.length() > 0) {
            "normalizeApkForInstall: входной APK отсутствует: ${inputApk.absolutePath}"
        }
        tmpDir.mkdirs()
        if (outputApk.exists()) outputApk.delete()
        val tmpOut = File(outputApk.absolutePath + ".tmp")
        if (tmpOut.exists()) tmpOut.delete()

        data class CdEntry(
            val name: String, val method: Int, val crc: Int,
            val compressedSize: Int, val uncompressedSize: Int, val localHeaderOffset: Int
        )
        val cdEntries = mutableListOf<CdEntry>()
        val seenNames = mutableSetOf<String>()
        var entryCount = 0

        try {
            ZipFile(inputApk).use { zf ->
                BufferedOutputStream(FileOutputStream(tmpOut)).use { rawOut ->
                    LittleEndianWriter(rawOut).use { w ->

                        fun writeLocalHeader(
                            name: String, compSize: Int, uncompSize: Int,
                            method: Int, crc: Long, extraPad: Int
                        ) {
                            val nameBytes = name.toByteArray(Charsets.UTF_8)
                            w.writeInt(0x04034b50)
                            w.writeShort(20)
                            w.writeShort(0)
                            w.writeShort(method)
                            w.writeShort(0)
                            w.writeShort(0)
                            w.writeInt(crc.toInt())
                            w.writeInt(compSize)
                            w.writeInt(uncompSize)
                            w.writeShort(nameBytes.size)
                            w.writeShort(extraPad)
                            w.writeBytes(nameBytes)
                            if (extraPad > 0) w.writeBytes(ByteArray(extraPad))
                        }

                        fun alignPad(name: String): Int {
                            val rawOffset = w.position + 30L + name.toByteArray().size
                            return ((4 - (rawOffset % 4)) % 4).toInt()
                        }

                        val buf = ByteArray(64 * 1024)
                        val entries = zf.entries()
                        while (entries.hasMoreElements()) {
                            val ze = entries.nextElement()
                            val name = ze.name
                            if (ze.isDirectory) continue
                            if (name.startsWith("META-INF/") || name == "META-INF") continue
                            if (!seenNames.add(name)) {
                                Log.d(TAG, "normalize: skip duplicate: $name")
                                continue
                            }
                            if (name == "META-INF/MANIFEST.MF" || name.endsWith(".SF") ||
                                name.endsWith(".RSA") || name.endsWith(".EC")
                            ) continue

                            if (ze.method == ZipEntry.STORED) {
                                val size = ze.size
                                require(size >= 0 && size <= Int.MAX_VALUE) {
                                    "normalize: bad STORED size for $name: $size"
                                }
                                val crc = ze.crc
                                require(crc >= 0) { "normalize: bad CRC for $name" }
                                val pad = alignPad(name)
                                val offset = w.position.toInt()
                                writeLocalHeader(name, size.toInt(), size.toInt(), ZipEntry.STORED, crc, pad)
                                zf.getInputStream(ze).use { ins ->
                                    var len: Int
                                    while (ins.read(buf).also { len = it } != -1) {
                                        w.write(buf, 0, len)
                                    }
                                }
                                cdEntries.add(
                                    CdEntry(name, ZipEntry.STORED, crc.toInt(), size.toInt(), size.toInt(), offset)
                                )
                                entryCount++
                            } else {
                                val tmpEntry = File(tmpDir, "e_${entryCount}.bin")
                                val crc32 = CRC32()
                                var uncompSize = 0L
                                zf.getInputStream(ze).use { ins ->
                                    tmpEntry.outputStream().use { outs ->
                                        var len: Int
                                        while (ins.read(buf).also { len = it } != -1) {
                                            crc32.update(buf, 0, len)
                                            outs.write(buf, 0, len)
                                            uncompSize += len
                                        }
                                    }
                                }
                                require(uncompSize <= Int.MAX_VALUE) {
                                    "normalize: entry too large: $name ($uncompSize)"
                                }
                                val compOut = ByteArrayOutputStream(
                                    minOf(maxOf(64, uncompSize.toInt() / 2), 8 * 1024 * 1024)
                                )
                                val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
                                tmpEntry.inputStream().use { ins ->
                                    var len: Int
                                    while (ins.read(buf).also { len = it } != -1) {
                                        deflater.setInput(buf, 0, len)
                                        while (!deflater.needsInput()) {
                                            val n = deflater.deflate(buf)
                                            if (n > 0) compOut.write(buf, 0, n)
                                        }
                                    }
                                }
                                deflater.finish()
                                while (!deflater.finished()) {
                                    val n = deflater.deflate(buf)
                                    if (n > 0) compOut.write(buf, 0, n)
                                }
                                deflater.end()
                                tmpEntry.delete()
                                val comp = compOut.toByteArray()
                                val offset = w.position.toInt()
                                writeLocalHeader(
                                    name, comp.size, uncompSize.toInt(),
                                    ZipEntry.DEFLATED, crc32.value, 0
                                )
                                w.writeBytes(comp)
                                cdEntries.add(
                                    CdEntry(
                                        name, ZipEntry.DEFLATED, crc32.value.toInt(),
                                        comp.size, uncompSize.toInt(), offset
                                    )
                                )
                                entryCount++
                            }
                        }

                        val cdOffset = w.position.toInt()
                        for (ce in cdEntries) {
                            val nb = ce.name.toByteArray(Charsets.UTF_8)
                            w.writeInt(0x02014b50)
                            w.writeShort(20)
                            w.writeShort(20)
                            w.writeShort(0)
                            w.writeShort(ce.method)
                            w.writeShort(0)
                            w.writeShort(0)
                            w.writeInt(ce.crc)
                            w.writeInt(ce.compressedSize)
                            w.writeInt(ce.uncompressedSize)
                            w.writeShort(nb.size)
                            w.writeShort(0)
                            w.writeShort(0)
                            w.writeShort(0)
                            w.writeShort(0)
                            w.writeInt(0)
                            w.writeInt(ce.localHeaderOffset)
                            w.writeBytes(nb)
                        }
                        val cdSize = w.position.toInt() - cdOffset
                        w.writeInt(0x06054b50)
                        w.writeShort(0)
                        w.writeShort(0)
                        w.writeShort(cdEntries.size)
                        w.writeShort(cdEntries.size)
                        w.writeInt(cdSize)
                        w.writeInt(cdOffset)
                        w.writeShort(0)
                    }
                }
            }

            require(entryCount > 0) { "normalizeApkForInstall: нет записей в ${inputApk.name}" }
            ZipFile(tmpOut).use { zf ->
                requireNotNull(zf.getEntry("AndroidManifest.xml")) {
                    "normalizeApkForInstall: потерян AndroidManifest.xml"
                }
            }
            assertNoDataDescriptors(tmpOut)
            if (outputApk.exists()) outputApk.delete()
            if (!tmpOut.renameTo(outputApk)) {
                tmpOut.copyTo(outputApk, overwrite = true)
                tmpOut.delete()
            }
            Log.i(TAG, "normalize: $entryCount entries, ${outputApk.length() / (1024 * 1024)} MB, без дескрипторов")
        } finally {
            runCatching { tmpDir.deleteRecursively() }
            runCatching { if (tmpOut.exists() && !outputApk.exists()) tmpOut.delete() }
        }
    }

    private class LittleEndianWriter(private val out: OutputStream) : Closeable, Flushable {
        var position: Long = 0; private set
        fun writeShort(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
            position += 2
        }
        fun writeInt(v: Int) {
            out.write(v and 0xFF)
            out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF)
            out.write((v shr 24) and 0xFF)
            position += 4
        }
        fun writeBytes(data: ByteArray) {
            out.write(data)
            position += data.size
        }
        fun write(data: ByteArray, offset: Int, length: Int) {
            out.write(data, offset, length)
            position += length
        }
        override fun close() = out.close()
        override fun flush() = (out as? Flushable)?.flush() ?: out.flush()
    }

    internal fun assertNoDataDescriptors(apk: File) {
        apk.inputStream().use { ins ->
            val data = ins.readBytes()
            var pos = 0
            var checked = 0
            while (pos + 30 <= data.size) {
                if (data[pos] != 0x50.toByte() || data[pos + 1] != 0x4b.toByte() ||
                    data[pos + 2] != 0x03.toByte() || data[pos + 3] != 0x04.toByte()
                ) break
                val flags = (data[pos + 6].toInt() and 0xFF) or ((data[pos + 7].toInt() and 0xFF) shl 8)
                require((flags and 0x08) == 0) {
                    "normalizeApkForInstall: запись #$checked использует data descriptor (флаг 0x08) — " +
                        "такой APK не ставится (INSTALL_PARSE_FAILED_NOT_APK)"
                }
                val method = (data[pos + 8].toInt() and 0xFF) or ((data[pos + 9].toInt() and 0xFF) shl 8)
                val compSize = leInt(data, pos + 18)
                val fnLen = leShort(data, pos + 26)
                val exLen = leShort(data, pos + 28)
                require(compSize >= 0) { "normalizeApkForInstall: битый local header #$checked" }
                pos += 30 + fnLen + exLen + compSize
                checked++
                if (checked > 200_000) break
            }
            require(checked > 0) { "normalizeApkForInstall: не найдены local headers" }
            Log.d(TAG, "normalize: проверено local headers без дескрипторов: $checked")
        }
    }

    private fun leInt(data: ByteArray, pos: Int): Int =
        (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8) or
            ((data[pos + 2].toInt() and 0xFF) shl 16) or ((data[pos + 3].toInt() and 0xFF) shl 24)

    private fun leShort(data: ByteArray, pos: Int): Int =
        (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)

    internal fun verifyInstallable(signedApk: File) {
        require(signedApk.exists() && signedApk.length() > 0) {
            "Собранный APK пуст: ${signedApk.absolutePath}"
        }
        ZipFile(signedApk).use { zf ->
            requireNotNull(zf.getEntry("AndroidManifest.xml")) { "В APK нет AndroidManifest.xml" }
            requireNotNull(zf.getEntry("classes.dex")) { "В APK нет classes.dex — шаблон повреждён" }
        }
        val result = ApkVerifier.Builder(signedApk).build().verify()
        require(result.isVerified) {
            "Подпись APK не прошла проверку: ${result.errors}. " +
                "Файл ${signedApk.name} (${signedApk.length() / (1024 * 1024)} MB) ставить нельзя."
        }
        Log.i(TAG, "verify: подпись OK, warnings=${result.warnings}")
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
            BigInteger(64, java.security.SecureRandom()),
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

        try {
            val signerConfig = SignerConfig.Builder(alias, kp.private as PrivateKey, listOf(certificate)).build()
            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
                .sign()
        } finally {
            keystoreFile.delete()
        }
    }
}