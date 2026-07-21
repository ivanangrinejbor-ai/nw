package org.catrobat.catroid.apkbuild

import android.content.Context
import android.util.Log
import com.android.apksig.ApkSigner
import com.reandroid.apk.ApkModule
import com.reandroid.archive.FileInputSource
import com.reandroid.arsc.chunk.xml.ResXmlElement
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

object ApkToolboxManager {

    private const val TAG = "ApkToolbox"

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }




    data class ManifestConfig(
        val appName: String? = null,
        val packageName: String? = null,
        val versionCode: Int? = null,
        val versionName: String? = null,
        val minSdkVersion: Int? = null,
        val targetSdkVersion: Int? = null,
        val permissionsToAdd: List<String>? = null,
        val permissionsToRemove: List<String>? = null,
        val debuggable: Boolean? = null
    )


    fun updateManifest(apkPath: String, config: ManifestConfig): Boolean {
        return modifyApk(apkPath) { module ->
            val manifest = module.androidManifestBlock
            val oldPackage = manifest.packageName
            val newPackage = config.packageName ?: oldPackage


            if (config.packageName != null) {
                manifest.packageName = config.packageName

                fixManifestRecursive(manifest.manifestElement, oldPackage, newPackage)

                fixClassNameReferences(manifest.manifestElement, oldPackage, newPackage)
            }
            if (config.versionCode != null) manifest.versionCode = config.versionCode
            if (config.versionName != null) manifest.versionName = config.versionName


            if (config.minSdkVersion != null) manifest.minSdkVersion = config.minSdkVersion
            if (config.targetSdkVersion != null) manifest.targetSdkVersion = config.targetSdkVersion


            if (config.appName != null) {
                val appElem = manifest.applicationElement

                val labelAttr = appElem.getOrCreateAndroidAttribute("label", 0x01010001)


                labelAttr.valueAsString = config.appName
            }


            if (config.debuggable != null) {
                val appElem = manifest.applicationElement

                val debugAttr = appElem.getOrCreateAndroidAttribute("debuggable", 0x0101000f)
                debugAttr.setValueAsBoolean(config.debuggable)
            }


            fun clearTestOnly(element: com.reandroid.arsc.chunk.xml.ResXmlElement?) {
                if (element == null) return
                try {
                    element
                        .getOrCreateAndroidAttribute("testOnly", 0x01010272)
                        .setValueAsBoolean(false)
                } catch (ignored: Exception) { }
            }
            clearTestOnly(manifest.manifestElement)
            clearTestOnly(manifest.applicationElement)


            config.permissionsToAdd?.forEach { perm ->
                manifest.addUsesPermission(perm)
            }


            config.permissionsToRemove?.forEach { permToRemove ->
                val root = manifest.manifestElement
                val permissions = root.listElements("uses-permission")
                val toDelete = mutableListOf<ResXmlElement>()

                for (permElem in permissions) {
                    val nameAttr = permElem.searchAttributeByResourceId(0x01010003)
                    if (nameAttr?.valueAsString == permToRemove) {
                        toDelete.add(permElem)
                    }
                }
                toDelete.forEach { root.remove(it) }
            }
        }
    }

    fun addFileToApk(apkPath: String, sourceFile: File, pathInsideApk: String): Boolean {
        if (!sourceFile.exists() || sourceFile.isDirectory) return false
        return modifyApk(apkPath) { module ->

            module.zipEntryMap.remove(pathInsideApk)

            module.add(FileInputSource(sourceFile, pathInsideApk))
        }
    }

    fun addFolderToApk(apkPath: String, sourceFolder: File, destPathInApk: String): Boolean {
        if (!sourceFolder.exists() || !sourceFolder.isDirectory) return false

        return modifyApk(apkPath) { module ->

            sourceFolder.walk().forEach { file ->
                if (file.isFile) {

                    val relativePath = file.toRelativeString(sourceFolder)


                    val finalPath = if (destPathInApk.isEmpty()) {
                        relativePath
                    } else {

                        val cleanDest = destPathInApk.trimEnd('/')
                        "$cleanDest/$relativePath"
                    }


                    module.zipEntryMap.remove(finalPath)

                    module.add(FileInputSource(file, finalPath))
                }
            }
        }
    }

    fun deleteFromApk(apkPath: String, pathPattern: String): Boolean {
        return modifyApk(apkPath) { module ->
            val cleanPattern = pathPattern.replace("\\", "/")


            val toRemove = ArrayList<String>()


            for (entry in module.zipEntryMap.listInputSources()) {
                val entryName = entry.name



                val isDirectoryMatch = entryName.startsWith("$cleanPattern/")
                val isFileMatch = entryName == cleanPattern

                if (isFileMatch || isDirectoryMatch) {
                    toRemove.add(entryName)
                }
            }


            for (name in toRemove) {
                module.zipEntryMap.remove(name)
            }
        }
    }
    fun extractFileFromApk(apkPath: String, pathInsideApk: String, outputLocalPath: String): Boolean {
        var module: ApkModule? = null
        try {
            module = ApkModule.loadApkFile(File(apkPath))
            val entry = module.zipEntryMap.getInputSource(pathInsideApk)

            return if (entry != null) {
                File(outputLocalPath).parentFile?.mkdirs()

                entry.openStream().use { input ->
                    FileOutputStream(outputLocalPath).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { module?.close() } catch (e: Exception) {}
        }
    }





    fun generateKeyStore(outputPath: String, alias: String, pass: String, commonName: String): Boolean {
        return try {

            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()

            val notBefore = Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24)
            val notAfter = Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 25)
            val serialNumber = BigInteger.valueOf(System.currentTimeMillis())
            val owner = X500Principal("CN=$commonName, OU=CatroidBuilder, O=NewCatroid, C=WW")


            val certBuilder = JcaX509v3CertificateBuilder(
                owner,
                serialNumber,
                notBefore,
                notAfter,
                owner,
                keyPair.public
            )



            val contentSigner = JcaContentSignerBuilder("SHA256WithRSA")
                .build(keyPair.private)


            val certHolder = certBuilder.build(contentSigner)




            val cert = JcaX509CertificateConverter()
                .getCertificate(certHolder)


            val ks = KeyStore.getInstance("PKCS12")
            ks.load(null, null)
            ks.setKeyEntry(alias, keyPair.private, pass.toCharArray(), arrayOf(cert))

            File(outputPath).parentFile?.mkdirs()
            FileOutputStream(outputPath).use { ks.store(it, pass.toCharArray()) }

            Log.d(TAG, "Successfully generated KeyStore: $outputPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Key generation failed", e)
            false
        }
    }

    fun signApk(
        context: Context,
        inputApkPath: String,
        outputApkPath: String,
        keyStorePath: String?,
        keyAlias: String?,
        keyPass: String?
    ): Boolean {
        return try {
            val input = File(inputApkPath)
            val output = File(outputApkPath)

            val signerBuilder: ApkSigner.Builder

            if (keyStorePath != null && File(keyStorePath).exists()) {
                val ks = KeyStore.getInstance("PKCS12")
                FileInputStream(keyStorePath).use { ks.load(it, keyPass?.toCharArray()) }

                val alias = keyAlias ?: ks.aliases().nextElement()
                val privateKey = ks.getKey(alias, keyPass?.toCharArray()) as PrivateKey
                val cert = ks.getCertificate(alias) as X509Certificate

                val config = com.android.apksig.ApkSigner.SignerConfig.Builder("CERT", privateKey, listOf(cert)).build()
                signerBuilder = ApkSigner.Builder(listOf(config))
            } else {
                val tempKey = File(context.cacheDir, "debug_auto.jks")
                if (!tempKey.exists()) {
                    generateKeyStore(tempKey.absolutePath, "debug", "android", "Debug User")
                }
                return signApk(context, inputApkPath, outputApkPath, tempKey.absolutePath, "debug", "android")
            }

            signerBuilder
                .setInputApk(input)
                .setOutputApk(output)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .build()
                .sign()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    fun replaceIconInApk(apkPath: String, iconFile: File): Boolean {
        if (!iconFile.exists()) return false
        return modifyApk(apkPath) { module ->
            val targets = mutableListOf<String>()
            for (entry in module.zipEntryMap.listInputSources()) {
                val name = entry.name
                val filename = name.substringAfterLast('/')
                if (filename.startsWith("ic_launcher")) {
                    targets.add(name)
                }
            }
            targets.forEach { pathInApk ->
                module.zipEntryMap.remove(pathInApk)
                module.add(FileInputSource(iconFile, pathInApk))
            }
        }
    }

    fun configureApk(
        apkPath: String,
        manifestConfig: ManifestConfig,
        iconFile: File? = null,
        pathsToDelete: List<String> = emptyList(),
        filesToAdd: List<Pair<File, String>> = emptyList(),
        workDir: File? = null
    ): Boolean {
        val originalFile = File(apkPath)
        if (!originalFile.exists()) return false


        val manifestBytes: ByteArray
        var reandroidModule: ApkModule? = null
        try {
            Log.d(TAG, "Phase A: loading APK into reandroid...")
            reandroidModule = ApkModule.loadApkFile(originalFile)
            reandroidModule.setLoadDefaultFramework(false)
            Log.d(TAG, "Phase A: APK loaded, modifying manifest...")

            val manifest = reandroidModule.androidManifestBlock
            val oldPackage = manifest.packageName
            val newPackage = manifestConfig.packageName ?: oldPackage

            if (manifestConfig.packageName != null) {
                manifest.packageName = manifestConfig.packageName
                fixManifestRecursive(manifest.manifestElement, oldPackage, newPackage)
                fixClassNameReferences(manifest.manifestElement, oldPackage, newPackage)
            }
            if (manifestConfig.versionCode != null) manifest.versionCode = manifestConfig.versionCode
            if (manifestConfig.versionName != null) manifest.versionName = manifestConfig.versionName
            if (manifestConfig.minSdkVersion != null) manifest.minSdkVersion = manifestConfig.minSdkVersion
            if (manifestConfig.targetSdkVersion != null) manifest.targetSdkVersion = manifestConfig.targetSdkVersion

            if (manifestConfig.appName != null) {
                val appElem = manifest.applicationElement
                val labelAttr = appElem.getOrCreateAndroidAttribute("label", 0x01010001)
                labelAttr.valueAsString = manifestConfig.appName
            }

            if (manifestConfig.debuggable != null) {
                val appElem = manifest.applicationElement
                val debugAttr = appElem.getOrCreateAndroidAttribute("debuggable", 0x0101000f)
                debugAttr.setValueAsBoolean(manifestConfig.debuggable)
            }

            fun clearTestOnly(element: com.reandroid.arsc.chunk.xml.ResXmlElement?) {
                if (element == null) return
                try {
                    element
                        .getOrCreateAndroidAttribute("testOnly", 0x01010272)
                        .setValueAsBoolean(false)
                } catch (ignored: Exception) { }
            }
            clearTestOnly(manifest.manifestElement)
            clearTestOnly(manifest.applicationElement)

            manifestConfig.permissionsToAdd?.forEach { perm ->
                manifest.addUsesPermission(perm)
            }

            manifestConfig.permissionsToRemove?.forEach { permToRemove ->
                val root = manifest.manifestElement
                val permissions = root.listElements("uses-permission")
                val toDelete = mutableListOf<ResXmlElement>()
                for (permElem in permissions) {
                    val nameAttr = permElem.searchAttributeByResourceId(0x01010003)
                    if (nameAttr?.valueAsString == permToRemove) {
                        toDelete.add(permElem)
                    }
                }
                toDelete.forEach { root.remove(it) }
            }

            manifest.refresh()
            Log.d(TAG, "Phase A: getting manifest bytes...")
            manifestBytes = manifest.getBytes()
            Log.d(TAG, "Phase A: manifest bytes = ${manifestBytes.size}")
            reandroidModule.close()
            reandroidModule = null
            Log.d(TAG, "Phase A: reandroid closed OK")
        } catch (e: Exception) {
            Log.e(TAG, "PHASE A FAILED: reandroid manifest step", e)
            try { reandroidModule?.close() } catch (ignored: Exception) {}
            return false
        }


        val tempFile = File(workDir ?: originalFile.parentFile, "apk_zip_build_${System.currentTimeMillis()}.apk")
        try {
            Log.d(TAG, "Phase B: tempFile = ${tempFile.absolutePath}")
            Log.d(TAG, "Phase B: originalFile = ${originalFile.absolutePath} (${originalFile.length() / (1024*1024)} MB)")
            val toDeleteNormalized = pathsToDelete.map { it.replace("\\", "/") }
            Log.d(TAG, "Phase B: pathsToDelete = $toDeleteNormalized")
            Log.d(TAG, "Phase B: iconFile = ${iconFile?.absolutePath} (exists=${iconFile?.exists()})")
            Log.d(TAG, "Phase B: filesToAdd = ${filesToAdd.map { it.second }}")


            val iconTargetNames = mutableSetOf<String>()
            if (iconFile != null && iconFile.exists()) {
                ZipInputStream(BufferedInputStream(FileInputStream(originalFile))).use { zis ->
                    var ze = zis.nextEntry
                    while (ze != null) {
                        val name = ze.name
                        if (name.substringAfterLast('/').startsWith("ic_launcher")) {
                            iconTargetNames.add(name)
                        }
                        ze = zis.nextEntry
                    }
                }
                Log.d(TAG, "Phase B: icon targets = $iconTargetNames")
            }


            class CountingOutputStream(delegate: java.io.OutputStream) : java.io.FilterOutputStream(delegate) {
                var count: Long = 0
                    private set
                override fun write(b: Int) { super.write(b); count++ }
                override fun write(b: ByteArray, off: Int, len: Int) { super.write(b, off, len); count += len }
            }

            ZipInputStream(BufferedInputStream(FileInputStream(originalFile))).use { zis ->
                val countingOut = CountingOutputStream(BufferedOutputStream(FileOutputStream(tempFile)))
                ZipOutputStream(countingOut).use { zos ->

                    var entryCount = 0

                    fun alignDataOffset(name: String): Int {
                        val rawOffset = countingOut.count + 30L + name.toByteArray().size
                        return ((4 - (rawOffset % 4)) % 4).toInt()
                    }


                    val writtenNames = mutableSetOf<String>()
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        entryCount++


                        if (!writtenNames.add(name)) {
                            Log.d(TAG, "  skip (duplicate): $name")
                            entry = zis.nextEntry
                            continue
                        }


                        val shouldDelete = toDeleteNormalized.any { pattern ->
                            name == pattern || name.startsWith("$pattern/")
                        }

                        if (shouldDelete) {
                            Log.d(TAG, "  delete: $name")
                            entry = zis.nextEntry
                            continue
                        }


                        if (iconTargetNames.contains(name)) {
                            Log.d(TAG, "  skip (icon replace): $name")
                            entry = zis.nextEntry
                            continue
                        }

                        if (name == "AndroidManifest.xml") {
                            Log.d(TAG, "  write manifest (${manifestBytes.size} bytes)")

                            val align = alignDataOffset("AndroidManifest.xml")
                            val extra = ByteArray(align)
                            val newEntry = ZipEntry("AndroidManifest.xml")
                            newEntry.method = ZipEntry.STORED
                            newEntry.size = manifestBytes.size.toLong()
                            newEntry.compressedSize = manifestBytes.size.toLong()
                            val crc32 = java.util.zip.CRC32()
                            crc32.update(manifestBytes)
                            newEntry.crc = crc32.value
                            if (align > 0) newEntry.extra = extra
                            zos.putNextEntry(newEntry)
                            zos.write(manifestBytes)
                            zos.closeEntry()
                        } else {

                            val align = alignDataOffset(name)
                            val extra = ByteArray(align)
                            val newEntry = ZipEntry(name)
                            newEntry.method = entry.method
                            newEntry.time = entry.time
                            if (entry.method == ZipEntry.STORED) {
                                newEntry.size = entry.size
                                newEntry.compressedSize = entry.compressedSize
                                newEntry.crc = entry.crc
                            }
                            if (align > 0) newEntry.extra = extra
                            zos.putNextEntry(newEntry)
                            zis.copyTo(zos)
                            zos.closeEntry()
                        }

                        entry = zis.nextEntry
                    }
                    Log.d(TAG, "Phase B: processed $entryCount entries from template, filePos=${countingOut.count / (1024*1024)} MB")


                    if (iconFile != null && iconFile.exists()) {
                        val iconBytes = iconFile.readBytes()
                        Log.d(TAG, "  adding icon: ${iconTargetNames.size} targets, ${iconBytes.size} bytes each")
                        for (iconPath in iconTargetNames) {
                            val align = alignDataOffset(iconPath)
                            val extra = ByteArray(align)
                            val iconEntry = ZipEntry(iconPath)
                            iconEntry.method = ZipEntry.STORED
                            iconEntry.size = iconBytes.size.toLong()
                            iconEntry.compressedSize = iconBytes.size.toLong()
                            val crc32 = java.util.zip.CRC32()
                            crc32.update(iconBytes)
                            iconEntry.crc = crc32.value
                            if (align > 0) iconEntry.extra = extra
                            zos.putNextEntry(iconEntry)
                            zos.write(iconBytes)
                            zos.closeEntry()
                        }
                    }


                    for ((sourceFile, pathInApk) in filesToAdd) {
                        if (sourceFile.exists() && !sourceFile.isDirectory) {
                            val apkPath = pathInApk.replace("\\", "/")
                            val entryBytes = sourceFile.readBytes()
                            Log.d(TAG, "  adding: $apkPath (${entryBytes.size / (1024*1024)} MB)")
                            val align = alignDataOffset(apkPath)
                            val extra = ByteArray(align)
                            val newEntry = ZipEntry(apkPath)
                            newEntry.method = ZipEntry.STORED
                            newEntry.size = entryBytes.size.toLong()
                            newEntry.compressedSize = entryBytes.size.toLong()
                            val crc32 = java.util.zip.CRC32()
                            crc32.update(entryBytes)
                            newEntry.crc = crc32.value
                            if (align > 0) newEntry.extra = extra
                            zos.putNextEntry(newEntry)
                            zos.write(entryBytes)
                            zos.closeEntry()
                        } else {
                            Log.w(TAG, "  SKIP (not found): ${sourceFile.absolutePath}")
                        }
                    }
                    Log.d(TAG, "Phase B: ZIP write complete, final filePos=${countingOut.count / (1024*1024)} MB")
                }
            }


            Log.d(TAG, "Phase B: swapping temp -> original...")
            val swapped = if (originalFile.delete()) {
                tempFile.renameTo(originalFile)
            } else {
                Log.w(TAG, "Phase B: delete failed, trying copy...")
                val copied = tempFile.copyTo(originalFile, overwrite = true)
                tempFile.delete()
                copied.exists()
            }
            Log.d(TAG, "Phase B: swap result = $swapped, output size = ${originalFile.length() / (1024*1024)} MB")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "PHASE B FAILED: ZIP streaming step", e)
            Log.e(TAG, "  cause: ${e.cause?.message ?: "none"}")
            e.stackTraceToString().let { Log.e(TAG, "  stack: $it") }
            try { tempFile.delete() } catch (ignored: Exception) {}
            return false
        }
    }

    private fun fixManifestRecursive(element: ResXmlElement, oldPkg: String, newPkg: String) {
        element.attributes.forEach { attr ->
            val value = attr.valueAsString
            if (value != null && value.contains(oldPkg)) {
                attr.valueAsString = value.replace(oldPkg, newPkg)
            }
        }
        element.listElements().forEach { child ->
            fixManifestRecursive(child, oldPkg, newPkg)
        }
    }


    private fun fixClassNameReferences(element: ResXmlElement, oldPkg: String, newPkg: String) {
        element.attributes.forEach { attr ->
            val value = attr.valueAsString

            if (value != null && value.startsWith(".")) {
                attr.valueAsString = "$oldPkg$value"
            }
        }
        element.listElements().forEach { child ->
            fixClassNameReferences(child, oldPkg, newPkg)
        }
    }



    private fun modifyApk(apkPath: String, action: (ApkModule) -> Unit): Boolean {
        var module: ApkModule? = null
        val tempFile = File("$apkPath.tmp")
        val originalFile = File(apkPath)

        return try {
            if (!originalFile.exists()) return false

            module = ApkModule.loadApkFile(originalFile)
            module.setLoadDefaultFramework(false)

            if (module.hasTableBlock()) {
                module.tableBlock.stringPool.setFlagSorted(false)
                module.tableBlock.stringPool.styleArray.clear()
            }


            action(module)

            if (module.hasTableBlock()) {
                module.tableBlock.refresh()
            }
            module.androidManifestBlock.refresh()

            module.writeApk(tempFile)
            module.close()

            if (originalFile.delete()) {
                tempFile.renameTo(originalFile)
            } else {
                tempFile.copyTo(originalFile, overwrite = true)
                tempFile.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try { module?.close() } catch (ignored: Exception) {}
            tempFile.delete()
            false
        }
    }
}