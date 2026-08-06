package org.catrobat.catroid.stage

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import org.catrobat.catroid.audio.AudioServiceHolder
import org.catrobat.catroid.audio.DesktopAudioService
import org.catrobat.catroid.audio.DesktopMidiService
import org.catrobat.catroid.audio.MidiServiceHolder
import org.catrobat.catroid.network.DesktopNetworkService
import org.catrobat.catroid.network.NetworkServiceHolder
import org.catrobat.catroid.notification.DesktopNotificationService
import org.catrobat.catroid.notification.NotificationServiceHolder
import org.catrobat.catroid.runtime.DesktopRuntimeServices
import org.catrobat.catroid.runtime.RuntimeServicesHolder
import org.catrobat.catroid.text.DesktopTextService
import org.catrobat.catroid.text.TextServiceHolder
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object DesktopStage {
    private const val EMBEDDED_PAYLOAD_FLAG = "--embedded-payload"
    private const val PAYLOAD_MAGIC = "NEOCAT01"
    private const val EMBEDDED_RESOURCE = "embedded_project.ncpp"

    private const val CRYPTO_MAGIC = "NCPP"
    private const val CRYPTO_MAGIC_LOCKED = "NCPX"
    private const val CRYPTO_MAGIC_STREAM = "NCPS"
    private const val CRYPTO_MAGIC_PASSWORD_CONTAINER = "NCPW"
    internal const val PAYLOAD_PASSWORD = "SA?D3Ft?ZZHufE9Ma#NA#A9HdQDAWbJ8WHfDPKfD4!G3ST+!=x;Z!wPD=7;B=9JTHRHsT@zZH@kFUu8tgQ8FLH%RPpZpLwJC2A*e"

    @JvmStatic
    fun main(args: Array<String>) {
        RuntimeServicesHolder.services = DesktopRuntimeServices()
        AudioServiceHolder.audioService = DesktopAudioService()
        MidiServiceHolder.midiService = DesktopMidiService()
        TextServiceHolder.textService = DesktopTextService()
        NotificationServiceHolder.service = DesktopNotificationService()
        NetworkServiceHolder.service = DesktopNetworkService()

        val projectInput = args.firstOrNull { !it.startsWith("--") && it != EMBEDDED_PAYLOAD_FLAG }
        val loadedProject: File? = (projectInput?.let { resolveProjectInput(File(it)) })
            ?: loadEmbeddedProject()

        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeoCatroid Desktop Player")
            setWindowedMode(1280, 720)
            useVsync(true)
            setForegroundFPS(60)
            setBackBufferConfig(8, 8, 8, 8, 16, 0, 4)
        }

        Lwjgl3Application(DesktopStageListener(loadedProject), config)
    }

    private fun resolveProjectInput(input: File): File? {
        return when {
            input.isDirectory -> input
            input.isFile -> java.io.FileInputStream(input).use { extractProjectFromStream(it) }
            else -> null
        }
    }

    private fun loadEmbeddedProject(): File? {
        try {
            DesktopStage::class.java.getResourceAsStream("/$EMBEDDED_RESOURCE")?.use { stream ->
                extractProjectFromStream(stream)?.let { return it }
            }
        } catch (_: Exception) {
        }
        val footer = loadFooterPayload() ?: return null
        return java.io.ByteArrayInputStream(footer).use { extractProjectFromStream(it) }
    }

    private fun loadFooterPayload(): ByteArray? {
        val classPath = System.getProperty("java.class.path") ?: return null
        val source = classPath
            .split(File.pathSeparatorChar)
            .map { File(it) }
            .firstOrNull { it.isFile && (it.name.endsWith(".jar") || it.name.endsWith(".exe")) }
            ?: return null
        if (!source.exists() || source.isDirectory) {
            return null
        }

        RandomAccessFile(source, "r").use { raf ->
            val length = raf.length()
            if (length < 16) {
                return null
            }

            raf.seek(length - 16)
            val sizeBytes = ByteArray(8)
            val magicBytes = ByteArray(8)
            raf.readFully(sizeBytes)
            raf.readFully(magicBytes)

            val magic = String(magicBytes, StandardCharsets.US_ASCII)
            if (magic != PAYLOAD_MAGIC) {
                return null
            }

            val size = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).long
            if (size <= 0 || size > Int.MAX_VALUE) {
                return null
            }

            val payloadStart = length - 16 - size
            if (payloadStart < 0) {
                return null
            }

            raf.seek(payloadStart)
            val payload = ByteArray(size.toInt())
            raf.readFully(payload)
            return payload
        }
    }

    internal fun extractProjectFromStream(rawInput: InputStream): File? {
        return try {
            val input = java.io.BufferedInputStream(rawInput, 1 shl 16)
            val magic = ByteArray(4)
            if (!readFully(input, magic)) return null
            val magicStr = String(magic, StandardCharsets.US_ASCII)
            System.out.println("[NeoCatroid] payload magic = '$magicStr'")
            val tempDir = Files.createTempDirectory("neocatroid-player").toFile()
            Runtime.getRuntime().addShutdownHook(Thread {
                try {
                    java.nio.file.Files.walk(tempDir.toPath())
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach { java.nio.file.Files.deleteIfExists(it) }
                } catch (_: Exception) { }
            })
            when (magicStr) {
                CRYPTO_MAGIC_PASSWORD_CONTAINER -> {
                    val password = readPasswordContainer(input)
                    val innerMagic = ByteArray(4)
                    if (!readFully(input, innerMagic)) throw java.io.IOException("NCPW: truncated inner payload magic")
                    when (String(innerMagic, StandardCharsets.US_ASCII)) {
                        CRYPTO_MAGIC_STREAM -> {
                            val tempZip = File(tempDir, "_payload.zip")
                            decryptNcpsStreamToFile(input, tempZip, password)
                            System.out.println("[NeoCatroid] NCPW/NCPS decrypted -> ${tempZip.length()} bytes; extracting zip...")
                            java.io.FileInputStream(tempZip).use { extractZipStream(it, tempDir) }
                            tempZip.delete()
                        }
                        CRYPTO_MAGIC, CRYPTO_MAGIC_LOCKED -> {
                            val full = innerMagic + input.readBytes()
                            extractZipStream(java.io.ByteArrayInputStream(decryptPayload(full, password)), tempDir)
                        }
                        else -> throw java.io.IOException("NCPW: unsupported inner payload magic")
                    }
                }
                CRYPTO_MAGIC_STREAM -> {
                    val tempZip = File(tempDir, "_payload.zip")
                    decryptNcpsStreamToFile(input, tempZip)
                    System.out.println("[NeoCatroid] NCPS decrypted -> ${tempZip.length()} bytes; extracting zip...")
                    java.io.FileInputStream(tempZip).use { extractZipStream(it, tempDir) }
                    tempZip.delete()
                }
                CRYPTO_MAGIC, CRYPTO_MAGIC_LOCKED -> {
                    val full = magic + input.readBytes()
                    extractZipStream(java.io.ByteArrayInputStream(decryptPayload(full)), tempDir)
                }
                else -> extractZipStream(
                    java.io.SequenceInputStream(java.io.ByteArrayInputStream(magic), input), tempDir)
            }
            System.out.println("[NeoCatroid] project extracted to ${tempDir.absolutePath}")
            tempDir
        } catch (e: Exception) {
            System.err.println("[NeoCatroid] project load FAILED: $e")
            e.printStackTrace()
            null
        }
    }

    private fun readPasswordContainer(input: InputStream): String {
        val lenBytes = ByteArray(4)
        if (!readFully(input, lenBytes)) throw java.io.IOException("NCPW: truncated password length")
        val len = ((lenBytes[0].toInt() and 0xFF) shl 24) or
            ((lenBytes[1].toInt() and 0xFF) shl 16) or
            ((lenBytes[2].toInt() and 0xFF) shl 8) or
            (lenBytes[3].toInt() and 0xFF)
        if (len <= 0 || len > 4096) throw java.io.IOException("NCPW: bad password length $len")
        val pwdBytes = ByteArray(len)
        if (!readFully(input, pwdBytes)) throw java.io.IOException("NCPW: truncated password")
        return String(pwdBytes, StandardCharsets.UTF_8)
    }

    private fun decryptNcpsStreamToFile(input: InputStream, dest: File, password: String = PAYLOAD_PASSWORD) {
        val salt = ByteArray(32)
        if (!readFully(input, salt)) throw java.io.IOException("NCPS: truncated salt")
        val ivPrefix = ByteArray(8)
        if (!readFully(input, ivPrefix)) throw java.io.IOException("NCPS: truncated iv prefix")
        val key = deriveKey(password, salt)
        val lenBytes = ByteArray(4)
        var segmentIndex = 0
        dest.outputStream().buffered().use { out ->
            while (readFully(input, lenBytes)) {
                val len = ((lenBytes[0].toInt() and 0xFF) shl 24) or
                    ((lenBytes[1].toInt() and 0xFF) shl 16) or
                    ((lenBytes[2].toInt() and 0xFF) shl 8) or
                    (lenBytes[3].toInt() and 0xFF)
                if (len <= 0 || len > 128 * 1024 * 1024) throw java.io.IOException("NCPS: bad segment length $len")
                val ct = ByteArray(len)
                if (!readFully(input, ct)) throw java.io.IOException("NCPS: truncated segment")
                val iv = ByteArray(12)
                System.arraycopy(ivPrefix, 0, iv, 0, 8)
                iv[8] = (segmentIndex ushr 24).toByte()
                iv[9] = (segmentIndex ushr 16).toByte()
                iv[10] = (segmentIndex ushr 8).toByte()
                iv[11] = segmentIndex.toByte()
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                out.write(cipher.doFinal(ct))
                segmentIndex++
            }
        }
    }

    private fun extractZipStream(input: InputStream, tempDir: File) {
        val destPath = tempDir.toPath().normalize()
        java.util.zip.ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val safeName = entry.name.split('/').joinToString("/") { seg ->
                    if (seg.isEmpty()) seg else {
                        val cleaned = buildString(seg.length) {
                            for (c in seg) append(
                                if (c.code < 0x20 || c == '<' || c == '>' || c == ':' ||
                                    c == '"' || c == '|' || c == '?' || c == '*' || c == '\\'
                                ) '_' else c
                            )
                        }.trimEnd(' ', '.')
                        if (cleaned.isEmpty()) "_" else cleaned
                    }
                }
                val resolved = destPath.resolve(safeName).normalize()
                if (resolved != destPath && !resolved.startsWith(destPath)) {
                    System.err.println("[NeoCatroid] skipping unsafe zip entry: ${entry.name}")
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }
                val outFile = resolved.toFile()
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun readFully(input: InputStream, buf: ByteArray): Boolean {
        var off = 0
        while (off < buf.size) {
            val n = input.read(buf, off, buf.size - off)
            if (n < 0) break
            off += n
        }
        return when (off) {
            buf.size -> true
            0 -> false
            else -> throw java.io.IOException("Truncated stream (expected ${buf.size}, got $off)")
        }
    }

    private fun decryptPayload(data: ByteArray, password: String = PAYLOAD_PASSWORD): ByteArray {
        val input = java.io.ByteArrayInputStream(data)
        val header = ByteArray(4)
        val magic = CRYPTO_MAGIC.toByteArray(StandardCharsets.US_ASCII)
        val lockedMagic = CRYPTO_MAGIC_LOCKED.toByteArray(StandardCharsets.US_ASCII)
        if (input.read(header) < 4 ||
            !(header.contentEquals(magic) || header.contentEquals(lockedMagic))
        ) {
            throw IllegalArgumentException("Encrypted payload has wrong magic")
        }
        val salt = ByteArray(32).also { input.read(it) }
        val iv = ByteArray(12).also { input.read(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var n: Int
        while (input.read(buffer).also { n = it } != -1) {
            if (n > 0) {
                val decoded = cipher.update(buffer, 0, n)
                if (decoded != null) out.write(decoded)
            }
        }
        val finalBlock = cipher.doFinal()
        if (finalBlock != null && finalBlock.isNotEmpty()) out.write(finalBlock)
        return out.toByteArray()
    }

    private fun deriveKey(password: String, salt: ByteArray): javax.crypto.SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 100_000, 256)
        val tmpKey = factory.generateSecret(spec)
        return SecretKeySpec(tmpKey.encoded, "AES")
    }
}
