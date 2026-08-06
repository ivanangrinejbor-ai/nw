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

                onProgress("Protecting project payload...")
                val encodedProject = File(tempDir, ProtectedProjectPayload.ENCRYPTED_ASSET_NAME)
                val payloadPassword = config.payloadPassword ?: ProjectCrypto.generateRandomPassword()
                writeEncryptedPayload(context, projectDir, encodedProject, payloadPassword, currentProject, tempDir)

                val keyFile = File(tempDir, ProtectedProjectPayload.KEY_ASSET_NAME)
                keyFile.writeText(payloadPassword)

                val keystoreFile = config.customKeystore ?: getOrCreateDebugKeystore(context, tempDir)
                val sigFile: File? = try {
                    val certHash = PayloadIntegrity.certHashFromKeystore(
                        keystoreFile, config.keyPass, config.keyAlias
                    )
                    if (certHash != null) {
                        val f = File(tempDir, ProtectedProjectPayload.SIG_ASSET_NAME)
                        f.writeText(PayloadIntegrity.buildSigContent(encodedProject.readBytes(), certHash))
                        f
                    } else null
                } catch (e: Exception) {
                    null
                }

                onProgress("Configuring manifest...")
                val manifestBytes = generatePatchedManifest(templateFile, config)

                onProgress("Building APK...")
                val unsignedApk = File(tempDir, "unsigned.apk")
                buildAlignedApk(
                    templateFile = templateFile,
                    outputFile = unsignedApk,
                    manifestBytes = manifestBytes,
                    iconFile = config.iconFile,
                    filesToAdd = buildList {
                        add(encodedProject to "assets/${ProtectedProjectPayload.ENCRYPTED_ASSET_NAME}")
                        add(keyFile to "assets/${ProtectedProjectPayload.KEY_ASSET_NAME}")
                        if (sigFile != null) {
                            add(sigFile to "assets/${ProtectedProjectPayload.SIG_ASSET_NAME}")
                        }
                    },
                    workDir = tempDir
                )

                onProgress("Signing APK...")
                val safeName = config.appName.replace(" ", "_")
                    .replace(Regex("""[\\/:*?"<>|]"""), "_").trim('_', '.')
                    .ifBlank { "app" }
                val signedApk = File(tempDir, "$safeName.apk")

                if (!ApkToolboxManager.signApk(
                        context,
                        unsignedApk.absolutePath, signedApk.absolutePath,
                        keystoreFile.absolutePath, config.keyAlias, config.keyPass
                    )) {
                    tempDir?.deleteRecursively()
                    return@withContext BuildResult.Error("APK signing failed")
                }

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


    private fun writeLocalEntry(
        w: LittleEndianWriter,
        name: String,
        payload: ByteArray,
        uncompressedSize: Int,
        method: Int,
        crc: Long
    ) {
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        val crc32 = crc.toInt()

        val headerBase = 30 + nameBytes.size
        val misalign = (w.position + headerBase) % 4
        val padding = if (method == ZipEntry.STORED && misalign != 0L) (4 - misalign).toInt() else 0

        w.writeInt(0x04034b50)
        w.writeShort(20)
        w.writeShort(0)
        w.writeShort(method)
        w.writeShort(0)
        w.writeShort(0)
        w.writeInt(crc32)
        w.writeInt(payload.size)
        w.writeInt(uncompressedSize)
        w.writeShort(nameBytes.size)
        w.writeShort(padding)
        w.writeBytes(nameBytes)
        if (padding > 0) w.writeBytes(ByteArray(padding))

        w.writeBytes(payload)
    }

    private fun buildAlignedApk(
        templateFile: File,
        outputFile: File,
        manifestBytes: ByteArray,
        iconFile: File?,
        filesToAdd: List<Pair<File, String>>,
        workDir: File?
    ) {

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


        val tempFile = File(workDir ?: outputFile.parentFile, "apk_build_${System.currentTimeMillis()}.apk")

        data class CdEntry(
            val name: String, val method: Int, val crc: Int,
            val compressedSize: Int, val uncompressedSize: Int, val localHeaderOffset: Int
        )
        val cdEntries = mutableListOf<CdEntry>()

        BufferedOutputStream(FileOutputStream(tempFile)).use { rawOut ->
            LittleEndianWriter(rawOut).use { w ->

                fun writeStored(name: String, data: ByteArray, crc: Long): CdEntry {
                    val offset = w.position.toInt()
                    writeLocalEntry(w, name, data, data.size, ZipEntry.STORED, crc)
                    return CdEntry(name, ZipEntry.STORED, crc.toInt(), data.size, data.size, offset)
                }

                fun writeEntry(name: String, uncompressed: ByteArray, method: Int, crc: Long): CdEntry {
                    if (method != ZipEntry.DEFLATED) {
                        return writeStored(name, uncompressed, crc)
                    }
                    val deflater = java.util.zip.Deflater(java.util.zip.Deflater.DEFAULT_COMPRESSION, true)
                    deflater.setInput(uncompressed)
                    deflater.finish()
                    val buf = ByteArrayOutputStream(maxOf(64, uncompressed.size / 2))
                    val tmp = ByteArray(8192)
                    while (!deflater.finished()) {
                        val n = deflater.deflate(tmp)
                        if (n > 0) buf.write(tmp, 0, n)
                    }
                    deflater.end()
                    val comp = buf.toByteArray()
                    val offset = w.position.toInt()
                    writeLocalEntry(w, name, comp, uncompressed.size, ZipEntry.DEFLATED, crc)
                    return CdEntry(name, ZipEntry.DEFLATED, crc.toInt(), comp.size, uncompressed.size, offset)
                }

                val crc32m = java.util.zip.CRC32().apply { update(manifestBytes) }
                cdEntries.add(writeStored("AndroidManifest.xml", manifestBytes, crc32m.value))
                Log.d(TAG, "Written manifest @ pos=${w.position}")

                if (arscData != null) {
                    cdEntries.add(writeStored("resources.arsc", arscData, arscCrc))
                    Log.d(TAG, "Written resources.arsc @ pos=${w.position}")
                }

                for (e in templateEntries) {
                    cdEntries.add(writeEntry(e.name, e.data, e.method, e.crc))
                }

                if (iconFile != null && iconFile.exists()) {
                    val iconBytes = iconFile.readBytes()
                    val crc32i = java.util.zip.CRC32().apply { update(iconBytes) }
                    for (iconPath in iconTargets) {
                        cdEntries.add(writeStored(iconPath, iconBytes, crc32i.value))
                    }
                }

                for ((src, dst) in filesToAdd) {
                    if (src.exists()) {
                        val data = src.readBytes()
                        val crc32p = java.util.zip.CRC32().apply { update(data) }
                        cdEntries.add(writeStored(dst, data, crc32p.value))
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
        ProjectCrypto.encrypt(payloadZip, encryptedFile, password, locked = true)
        payloadZip.delete()
        stagingDir.deleteRecursively()
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
