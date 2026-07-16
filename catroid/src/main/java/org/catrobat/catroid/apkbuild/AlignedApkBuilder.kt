package org.catrobat.catroid.apkbuild

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.os.StatFs
import android.util.Log
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.io.ProjectCrypto
import org.catrobat.catroid.io.XstreamSerializer
import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.xml.ResXmlElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Second-generation APK builder with guaranteed 4-byte alignment.
 *
 * Key improvement over BakedApkBuilder:
 * - Writes AndroidManifest.xml then resources.arsc as the FIRST two entries
 *   (both STORED), so alignment is trivial — no DEFLATED entries between them.
 * - Uses manual ZIP writing (raw local file headers) instead of ZipOutputStream,
 *   to have 100% control over extra-field padding.
 * - BakedApkBuilder is kept untouched for backward compatibility.
 */
object AlignedApkBuilder {
    private const val TAG = "AlignedApkBuilder"
    private const val TEMPLATE_RUNTIME_APK = "template_runtime.apk"

    data class ApkConfig(
        val appName: String,
        val packageName: String = "org.danvexteam.catroidruntime",
        val versionName: String = "1.0",
        val versionCode: Int = 1,
        val permissions: List<String> = emptyList(),
        val minSdk: Int = 21,
        val targetSdk: Int = 35,
        val iconFile: File? = null,
        val payloadPassword: String? = null,
        val customKeystore: File? = null,
        val keystorePass: String = "keystore",
        val keyAlias: String = "newcatroid",
        val keyPass: String = "keystore"
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            appName = parcel.readString() ?: "",
            packageName = parcel.readString() ?: "org.danvexteam.catroidruntime",
            versionName = parcel.readString() ?: "1.0",
            versionCode = parcel.readInt(),
            permissions = parcel.createStringArrayList() ?: emptyList(),
            minSdk = parcel.readInt(),
            targetSdk = parcel.readInt(),
            iconFile = parcel.readString()?.let { File(it) },
            payloadPassword = parcel.readString(),
            customKeystore = parcel.readString()?.let { File(it) },
            keystorePass = parcel.readString() ?: "keystore",
            keyAlias = parcel.readString() ?: "newcatroid",
            keyPass = parcel.readString() ?: "keystore"
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(appName)
            parcel.writeString(packageName)
            parcel.writeString(versionName)
            parcel.writeInt(versionCode)
            parcel.writeStringList(permissions)
            parcel.writeInt(minSdk)
            parcel.writeInt(targetSdk)
            parcel.writeString(iconFile?.absolutePath)
            parcel.writeString(payloadPassword)
            parcel.writeString(customKeystore?.absolutePath)
            parcel.writeString(keystorePass)
            parcel.writeString(keyAlias)
            parcel.writeString(keyPass)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<ApkConfig> {
            override fun createFromParcel(parcel: Parcel): ApkConfig = ApkConfig(parcel)
            override fun newArray(size: Int): Array<ApkConfig?> = arrayOfNulls(size)
        }
    }

    sealed class BuildResult {
        data class Success(val apkFile: File) : BuildResult()
        data class Error(val message: String) : BuildResult()
    }

    suspend fun build(context: Context, projectDir: File, config: ApkConfig, onProgress: (String) -> Unit): BuildResult =
        withContext(Dispatchers.IO) {
            var tempDir: File? = null
            try {
                // ── Phase 0: Pre-flight ────────────────────────────────────────
                onProgress("Preparing...")
                tempDir = File(context.cacheDir, "apk_v2_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                val needed = projectDir.sizeRecursively() * 3 + 200L * 1024 * 1024
                runCatching {
                    val stat = StatFs(tempDir.absolutePath)
                    val usable = stat.blockSizeLong * stat.availableBlocksLong
                    if (usable < needed) {
                        tempDir?.deleteRecursively()
                        return@withContext BuildResult.Error(
                            "Недостаточно места (~${needed / (1024 * 1024)} MB)"
                        )
                    }
                }

                // ── Phase 1: Load template ─────────────────────────────────────
                onProgress("Loading template APK...")
                val templateFile = File(tempDir, "template.apk")
                var templateLoaded = false
                try {
                    context.assets.open(TEMPLATE_RUNTIME_APK).use { input ->
                        FileOutputStream(templateFile).use { output -> input.copyTo(output) }
                    }
                    templateLoaded = true
                    Log.d(TAG, "Template: ${templateFile.length() / (1024 * 1024)} MB")
                } catch (_: Exception) {}

                if (!templateLoaded) {
                    val selfPath = context.applicationInfo.sourceDir
                    if (selfPath != null) File(selfPath).copyTo(templateFile, overwrite = true)
                    else {
                        tempDir?.deleteRecursively()
                        return@withContext BuildResult.Error("Template APK not found")
                    }
                }

                // ── Phase 2: Load project ──────────────────────────────────────
                onProgress("Loading project...")
                val currentProject = ProjectManager.getInstance()?.currentProject
                    ?: try {
                        XstreamSerializer.getInstance().loadProject(projectDir, context)
                    } catch (e: Exception) {
                        tempDir?.deleteRecursively()
                        return@withContext BuildResult.Error("Ошибка загрузки проекта: ${e.message}")
                    }
                if (currentProject == null || currentProject.sceneList.isEmpty()) {
                    tempDir?.deleteRecursively()
                    return@withContext BuildResult.Error("Проект пуст")
                }

                // ── Phase 3: Encrypt project payload ──────────────────────────
                onProgress("Protecting project payload...")
                val encodedProject = File(tempDir, ProtectedProjectPayload.ENCRYPTED_ASSET_NAME)
                val payloadPassword = config.payloadPassword ?: generateRandomPassword()
                writeEncryptedPayload(context, projectDir, encodedProject, payloadPassword, currentProject, tempDir)

                val keyFile = File(tempDir, ProtectedProjectPayload.KEY_ASSET_NAME)
                keyFile.writeText(payloadPassword)

                // ── Phase 4: Generate manifest via reandroid ────────────────────
                onProgress("Configuring manifest...")
                val manifestBytes = generatePatchedManifest(templateFile, config)

                // ── Phase 5: Build aligned APK ─────────────────────────────────
                onProgress("Building APK...")
                val unsignedApk = File(tempDir, "unsigned.apk")
                buildAlignedApk(
                    templateFile = templateFile,
                    outputFile = unsignedApk,
                    manifestBytes = manifestBytes,
                    iconFile = config.iconFile,
                    filesToAdd = listOf(
                        encodedProject to "assets/${ProtectedProjectPayload.ENCRYPTED_ASSET_NAME}",
                        keyFile to "assets/${ProtectedProjectPayload.KEY_ASSET_NAME}"
                    ),
                    workDir = tempDir
                )

                // ── Phase 6: Sign ──────────────────────────────────────────────
                onProgress("Signing APK...")
                val safeName = config.appName.replace(" ", "_")
                    .replace(Regex("""[\\/:*?"<>|]"""), "_").trim('_', '.')
                    .ifBlank { "app" }
                val signedApk = File(tempDir, "$safeName.apk")
                val keystoreFile = config.customKeystore ?: getOrCreateDebugKeystore(context, tempDir)

                if (!ApkToolboxManager.signApk(
                        context,
                        unsignedApk.absolutePath, signedApk.absolutePath,
                        keystoreFile.absolutePath, config.keyAlias, config.keyPass
                    )) {
                    tempDir?.deleteRecursively()
                    return@withContext BuildResult.Error("APK signing failed")
                }

                // ── Phase 7: Copy result ──────────────────────────────────────
                onProgress("Done")
                val resultFile = File(context.cacheDir, signedApk.name)
                resultFile.delete()
                signedApk.copyTo(resultFile, true)
                tempDir?.deleteRecursively()
                BuildResult.Success(resultFile)

            } catch (e: Throwable) {
                Log.e(TAG, "Build failed", e)
                tempDir?.deleteRecursively()
                BuildResult.Error(e.message ?: "Unknown error")
            }
        }

    // ══════════════════════════════════════════════════════════════════
    //  Manifest patching (reandroid)
    // ══════════════════════════════════════════════════════════════════

    private fun generatePatchedManifest(templateApk: File, config: ApkConfig): ByteArray {
        val module = ApkModule.loadApkFile(templateApk)
        try {
            module.setLoadDefaultFramework(false)
            val manifest = module.androidManifestBlock
            val oldPackage = manifest.packageName
            val newPackage = config.packageName

            if (config.packageName != null) {
                manifest.packageName = newPackage
                fixManifestAttributes(manifest.manifestElement, oldPackage, newPackage)
                fixClassNames(manifest.manifestElement, oldPackage)
            }
            if (config.versionCode != null) manifest.versionCode = config.versionCode
            if (config.versionName != null) manifest.versionName = config.versionName
            if (config.minSdk != null) manifest.minSdkVersion = config.minSdk
            if (config.targetSdk != null) manifest.targetSdkVersion = config.targetSdk

            if (config.appName.isNotEmpty()) {
                manifest.applicationElement
                    .getOrCreateAndroidAttribute("label", 0x01010001)
                    .valueAsString = config.appName
            }

            // debuggable=false, testOnly=false — critical for installability
            val appElem = manifest.applicationElement
            appElem.getOrCreateAndroidAttribute("debuggable", 0x0101000f).setValueAsBoolean(false)
            setTestOnlyFalse(manifest.manifestElement)
            setTestOnlyFalse(manifest.applicationElement)

            config.permissions.forEach { manifest.addUsesPermission(it) }

            manifest.refresh()
            return manifest.getBytes()
        } finally {
            module.close()
        }
    }

    private fun setTestOnlyFalse(element: ResXmlElement?) {
        if (element == null) return
        try {
            element.getOrCreateAndroidAttribute("testOnly", 0x01010272).setValueAsBoolean(false)
        } catch (_: Exception) {}
    }

    private fun fixManifestAttributes(element: ResXmlElement, oldPkg: String, newPkg: String) {
        element.attributes.forEach { attr ->
            val value = attr.valueAsString
            if (value != null && value.contains(oldPkg)) {
                attr.valueAsString = value.replace(oldPkg, newPkg)
            }
        }
        element.listElements().forEach { fixManifestAttributes(it, oldPkg, newPkg) }
    }

    private fun fixClassNames(element: ResXmlElement, oldPkg: String) {
        element.attributes.forEach { attr ->
            val value = attr.valueAsString
            if (value != null && value.startsWith(".")) {
                attr.valueAsString = "$oldPkg$value"
            }
        }
        element.listElements().forEach { fixClassNames(it, oldPkg) }
    }

    // ══════════════════════════════════════════════════════════════════
    //  APK builder — manual ZIP writing for guaranteed alignment
    // ══════════════════════════════════════════════════════════════════

    /**
     * Writes little-endian integers/bytes and tracks current file position.
     */
    private class LittleEndianWriter(private val out: OutputStream) : Closeable {
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
        override fun close() { out.close() }
        fun flush() { (out as? Flushable)?.flush() }
    }

    /**
     * Writes one ZIP local file header + entry data.
     * The local header extra field is padded so the data starts on a 4-byte boundary.
     */
    private fun writeLocalEntry(
        w: LittleEndianWriter,
        name: String,
        data: ByteArray,
        method: Int,
        crc: Long
    ) {
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        val size = data.size
        val crc32 = crc.toInt()

        // Alignment padding: data start = position + 30 + name.length + extra.length
        val headerBase = 30 + nameBytes.size
        val misalign = (w.position + headerBase) % 4
        val padding = if (misalign == 0L) 0 else (4 - misalign).toInt()

        // Local file header (30 bytes)
        w.writeInt(0x04034b50)                  // signature
        w.writeShort(20)                         // version needed (2.0)
        w.writeShort(0)                          // general purpose bit flag (0 = sizes in header)
        w.writeShort(method)                     // compression method
        w.writeShort(0)                          // last mod file time
        w.writeShort(0)                          // last mod file date
        w.writeInt(crc32)                        // crc-32
        w.writeInt(size)                         // compressed size (= size for STORED)
        w.writeInt(size)                         // uncompressed size
        w.writeShort(nameBytes.size)             // file name length
        w.writeShort(padding)                    // extra field length = alignment padding
        w.writeBytes(nameBytes)                  // file name
        if (padding > 0) w.writeBytes(ByteArray(padding)) // alignment padding

        // Entry data
        w.writeBytes(data)
    }

    private fun buildAlignedApk(
        templateFile: File,
        outputFile: File,
        manifestBytes: ByteArray,
        iconFile: File?,
        filesToAdd: List<Pair<File, String>>,
        workDir: File?
    ) {
        // Step 1: Read ALL template entry bytes into memory via ZipFile.
        // Memory peak: template APK ~180 MB; this adds ~180 MB temporarily.
        data class EntryData(val name: String, val method: Int, val data: ByteArray, val crc: Long)
        val templateEntries = mutableListOf<EntryData>()
        var arscData: ByteArray? = null
        var arscCrc = 0L
        val iconTargets = mutableSetOf<String>()

        ZipFile(templateFile).use { zf ->
            for (ze in zf.entries().asSequence()) {
                val name = ze.name
                when {
                    name.startsWith("META-INF/") || name == "META-INF" -> continue
                    name == "resources.arsc" -> {
                        val raw = zf.getInputStream(ze).readBytes()
                        val crc32 = java.util.zip.CRC32().apply { update(raw) }
                        arscData = raw
                        arscCrc = crc32.value
                        Log.d(TAG, "resources.arsc: ${raw.size / 1024} KB")
                        continue
                    }
                    name == "AndroidManifest.xml" -> continue
                    iconFile != null && name.substringAfterLast('/').startsWith("ic_launcher") -> {
                        iconTargets.add(name)
                        continue
                    }
                }
                val rawData = zf.getInputStream(ze).readBytes()
                val crc32 = java.util.zip.CRC32().apply { update(rawData) }
                templateEntries.add(EntryData(name, ze.method, rawData, crc32.value))
            }
            Log.d(TAG, "Template entries loaded: ${templateEntries.size}")
        }

        // Step 2: Write output APK with manual ZIP local headers + central directory
        val tempFile = File(workDir ?: outputFile.parentFile, "apk_build_${System.currentTimeMillis()}.apk")

        data class CdEntry(
            val name: String, val method: Int, val crc: Int,
            val size: Int, val localHeaderOffset: Int
        )
        val cdEntries = mutableListOf<CdEntry>()

        BufferedOutputStream(FileOutputStream(tempFile)).use { rawOut ->
            LittleEndianWriter(rawOut).use { w ->

                fun writeStored(name: String, data: ByteArray, crc: Long): CdEntry {
                    val offset = w.position.toInt()
                    writeLocalEntry(w, name, data, ZipEntry.STORED, crc)
                    return CdEntry(name, ZipEntry.STORED, crc.toInt(), data.size, offset)
                }

                // ── 1. AndroidManifest.xml (STORED, aligned) ────────────
                val crc32m = java.util.zip.CRC32().apply { update(manifestBytes) }
                cdEntries.add(writeStored("AndroidManifest.xml", manifestBytes, crc32m.value))
                Log.d(TAG, "Written manifest @ pos=${w.position}")

                // ── 2. resources.arsc (STORED, aligned — right after manifest) ──
                if (arscData != null) {
                    cdEntries.add(writeStored("resources.arsc", arscData, arscCrc))
                    Log.d(TAG, "Written resources.arsc @ pos=${w.position}")
                }

                // ── 3. Template entries ─────────────────────────────────
                for (e in templateEntries) {
                    val offset = w.position.toInt()
                    writeLocalEntry(w, e.name, e.data, e.method, e.crc)
                    cdEntries.add(CdEntry(e.name, e.method, e.crc.toInt(), e.data.size, offset))
                }

                // ── 4. Icon files ──────────────────────────────────────
                if (iconFile != null && iconFile.exists()) {
                    val iconBytes = iconFile.readBytes()
                    val crc32i = java.util.zip.CRC32().apply { update(iconBytes) }
                    for (iconPath in iconTargets) {
                        cdEntries.add(writeStored(iconPath, iconBytes, crc32i.value))
                    }
                }

                // ── 5. Payload files ───────────────────────────────────
                for ((src, dst) in filesToAdd) {
                    if (src.exists()) {
                        val data = src.readBytes()
                        val crc32p = java.util.zip.CRC32().apply { update(data) }
                        cdEntries.add(writeStored(dst, data, crc32p.value))
                    }
                }

                // ── 6. Central Directory ───────────────────────────────
                val cdOffset = w.position.toInt()
                for (ce in cdEntries) {
                    val nb = ce.name.toByteArray(Charsets.UTF_8)
                    w.writeInt(0x02014b50)         // central directory signature
                    w.writeShort(20)                // version made by
                    w.writeShort(20)                // version needed
                    w.writeShort(0)                 // general purpose bit flag
                    w.writeShort(ce.method)         // compression method
                    w.writeShort(0)                 // last mod file time
                    w.writeShort(0)                 // last mod file date
                    w.writeInt(ce.crc)              // crc-32
                    w.writeInt(ce.size)             // compressed size
                    w.writeInt(ce.size)             // uncompressed size
                    w.writeShort(nb.size)           // file name length
                    w.writeShort(0)                 // extra field length
                    w.writeShort(0)                 // file comment length
                    w.writeShort(0)                 // disk number start
                    w.writeShort(0)                 // internal file attributes
                    w.writeInt(0)                   // external file attributes
                    w.writeInt(ce.localHeaderOffset)// local header offset
                    w.writeBytes(nb)                // file name
                }
                val cdSize = w.position.toInt() - cdOffset

                // ── 7. End of Central Directory ────────────────────────
                w.writeInt(0x06054b50)             // EOCD signature
                w.writeShort(0)                    // disk number
                w.writeShort(0)                    // disk with central directory
                w.writeShort(cdEntries.size)       // entries on this disk
                w.writeShort(cdEntries.size)       // total entries
                w.writeInt(cdSize)                 // size of central directory
                w.writeInt(cdOffset)               // offset of central directory
                w.writeShort(0)                    // comment length
            }
        }

        // Verify resources.arsc — must be STORED
        try {
            ZipFile(tempFile).use { zf ->
                val ae = zf.getEntry("resources.arsc")
                if (ae != null) {
                    Log.d(TAG, "Verify: resources.arsc method=${ae.method} (STORED=0) size=${ae.size}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Verify failed", e)
        }

        outputFile.delete()
        tempFile.renameTo(outputFile)
        Log.d(TAG, "Aligned APK: ${outputFile.length() / (1024 * 1024)} MB")
    }

    // ══════════════════════════════════════════════════════════════════
    //  Encrypted payload builder
    // ══════════════════════════════════════════════════════════════════

    private fun writeEncryptedPayload(
        context: Context,
        projectDir: File,
        encryptedFile: File,
        password: String,
        project: Project,
        tempDir: File
    ) {
        val stagingDir = File(tempDir, "payload")
        val payloadZip = File(tempDir, "payload.zip")
        stagingDir.deleteRecursively()
        stagingDir.mkdirs()

        val xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n"
        val projectXml = xmlHeader + XstreamSerializer.getInstance().getXmlAsStringFromProject(project)
        File(stagingDir, Constants.CODE_XML_FILE_NAME).writeText(projectXml)

        val filesDir = File(projectDir, "files")
        if (filesDir.exists()) {
            filesDir.copyRecursively(File(stagingDir, "files"), overwrite = true)
        }

        val imagesDir = File(stagingDir, "images").apply { mkdirs() }
        val soundsDir = File(stagingDir, "sounds").apply { mkdirs() }
        project.sceneList.forEach { scene ->
            scene.spriteList.forEach { sprite ->
                sprite.lookList.forEach { look ->
                    look.file?.takeIf { it.exists() }?.let {
                        it.copyTo(File(imagesDir, it.name), overwrite = true)
                    }
                }
                sprite.soundList.forEach { sound ->
                    sound.file?.takeIf { it.exists() }?.let {
                        it.copyTo(File(soundsDir, it.name), overwrite = true)
                    }
                }
            }
        }

        zipDirectory(stagingDir, payloadZip)
        ProjectCrypto.encrypt(payloadZip, encryptedFile, password)
        payloadZip.delete()
        stagingDir.deleteRecursively()
    }

    // ══════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════

    private fun generateRandomPassword(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun zipDirectory(sourceDir: File, destFile: File) {
        ZipOutputStream(FileOutputStream(destFile)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file == sourceDir) return@forEach
                val path = file.relativeTo(sourceDir).path.replace('\\', '/')
                if (file.isDirectory) {
                    zos.putNextEntry(ZipEntry("$path/"))
                } else {
                    zos.putNextEntry(ZipEntry(path))
                    FileInputStream(file).use { it.copyTo(zos) }
                }
                zos.closeEntry()
            }
        }
    }

    private fun getOrCreateDebugKeystore(context: Context, tempDir: File): File {
        val debugDir = File(context.filesDir, "apk_signing")
        debugDir.mkdirs()
        val keystore = File(debugDir, "debug_fixed.jks")
        if (!keystore.exists()) {
            try {
                context.assets.open("debug_build.keystore").use { input ->
                    FileOutputStream(keystore).use { output -> input.copyTo(output) }
                }
            } catch (_: Exception) {
                ApkToolboxManager.generateKeyStore(
                    keystore.absolutePath, "newcatroid", "keystore", "CN=NewCatroid,O=NewCatroid,C=US"
                )
            }
        }
        return keystore
    }

    private fun File.sizeRecursively(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    }
}
