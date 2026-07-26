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

    private const val CRYPTO_MAGIC = "NCPP"
    // Запечённый (locked) пейлоад — тот же формат, другая магия (редактор такой не импортирует).
    private const val CRYPTO_MAGIC_LOCKED = "NCPX"
    private const val PAYLOAD_PASSWORD = "SA?D3Ft?ZZHufE9Ma#NA#A9HdQDAWbJ8WHfDPKfD4!G3ST+!=x;Z!wPD=7;B=9JTHRHsT@zZH@kFUu8tgQ8FLH%RPpZpLwJC2A*e"

    @JvmStatic
    fun main(args: Array<String>) {
        RuntimeServicesHolder.services = DesktopRuntimeServices()
        AudioServiceHolder.audioService = DesktopAudioService()
        MidiServiceHolder.midiService = DesktopMidiService()
        TextServiceHolder.textService = DesktopTextService()
        NotificationServiceHolder.service = DesktopNotificationService()
        NetworkServiceHolder.service = DesktopNetworkService()

        val projectInput = args.firstOrNull { !it.startsWith("--") && it != EMBEDDED_PAYLOAD_FLAG }
        val loadedProject: File? = when {
            projectInput != null -> resolveProjectInput(File(projectInput))
            else -> loadEmbeddedPayload()?.let { extractPayload(it) }
        }

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
            input.isFile -> extractPayload(input.readBytes())
            else -> null
        }
    }

    private fun loadEmbeddedPayload(): ByteArray? {
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

    private fun extractPayload(payload: ByteArray): File? {
        val data = maybeDecrypt(payload)
        return try {
            val tempDir = Files.createTempDirectory("neocatroid-player").toFile()
            Runtime.getRuntime().addShutdownHook(Thread {
                tempDir.deleteRecursively()
            })
            val destPath = tempDir.toPath().normalize()
            val zis = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(data))
            var entry = zis.nextEntry
            while (entry != null) {
                val resolved = destPath.resolve(entry.name).normalize()
                if (resolved != destPath && !resolved.startsWith(destPath)) {
                    zis.closeEntry()
                    throw SecurityException("Zip entry '${entry.name}' escapes the target directory")
                }
                val outFile = resolved.toFile()
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry() // Redundant for directories (nextEntry skips them), but harmless
                entry = zis.nextEntry
            }
            zis.close()
            tempDir
        } catch (_: Exception) {
            null
        }
    }

    private fun maybeDecrypt(payload: ByteArray): ByteArray {
        val magic = CRYPTO_MAGIC.toByteArray(StandardCharsets.US_ASCII)
        val lockedMagic = CRYPTO_MAGIC_LOCKED.toByteArray(StandardCharsets.US_ASCII)
        if (payload.size >= magic.size) {
            val head = payload.copyOfRange(0, magic.size)
            if (head.contentEquals(magic) || head.contentEquals(lockedMagic)) {
                return decryptPayload(payload)
            }
        }
        return payload
    }

    private fun decryptPayload(data: ByteArray): ByteArray {
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
        val key = deriveKey(PAYLOAD_PASSWORD, salt)
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
