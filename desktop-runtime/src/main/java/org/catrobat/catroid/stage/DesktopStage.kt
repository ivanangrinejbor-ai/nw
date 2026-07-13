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

/**
 * Desktop (Windows) entry point for the NeoCatroid player.
 *
 * Регистрирует десктопные реализации сервисов (аудио, MIDI, текст, уведомления,
 * рантайм-сервисы), загружает проект (из аргумента командной строки, встроенного
 * payload или текущей директории) и запускает полноценный libGDX цикл с
 * [DesktopStageListener] в качестве ApplicationListener — так же, как это делает
 * [StageActivity] на Android, но без Android-зависимостей.
 */
object DesktopStage {
    private const val EMBEDDED_PAYLOAD_FLAG = "--embedded-payload"
    private const val PAYLOAD_MAGIC = "NEOCAT01"

    // Project encryption — same scheme as the Android Baked APK (org.catrobat.catroid.io.ProjectCrypto).
    // Format: [magic "NCPP":4][salt:32][iv:12][ciphertext]. Password is the shared payload password.
    private const val CRYPTO_MAGIC = "NCPP"
    private const val PAYLOAD_PASSWORD = "NeoCatroid:BakedProject:Payload:v1"

    @JvmStatic
    fun main(args: Array<String>) {
        // 1. Регистрируем все десктопные сервисы
        RuntimeServicesHolder.services = DesktopRuntimeServices()
        AudioServiceHolder.audioService = DesktopAudioService()
        MidiServiceHolder.midiService = DesktopMidiService()
        TextServiceHolder.textService = DesktopTextService()
        NotificationServiceHolder.service = DesktopNotificationService()
        NetworkServiceHolder.service = DesktopNetworkService()

        // 2. Определяем источник проекта.
        // Приоритет: явно переданный путь > встроенный payload (NEOCAT01, если exe/jar
        // собран с проектом) > пустая сцена. Флаг --embedded-payload больше не нужен.
        val projectInput = args.firstOrNull { !it.startsWith("--") && it != EMBEDDED_PAYLOAD_FLAG }
        val loadedProject: File? = when {
            projectInput != null -> resolveProjectInput(File(projectInput))
            else -> loadEmbeddedPayload()?.let { extractPayload(it) }
        }

        if (loadedProject != null) {
            // Загружаем проект в десктопный менеджер (code.xml + images/)
            DesktopProjectManager.getInstance().loadProject(loadedProject)
        }

        // 3. Настраиваем LWJGL3 окно
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeoCatroid Desktop Player")
            setWindowedMode(1280, 720)
            useVsync(true)
            setForegroundFPS(60)
            setBackBufferConfig(8, 8, 8, 8, 16, 0, 4)
        }

        // 4. Запускаем полноценный DesktopStageListener как ApplicationListener
        Lwjgl3Application(DesktopStageListener(), config)
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
            // Рекурсивно удалить всё при выходе (walk + deleteOnExit на каждом файле)
            tempDir.walkTopDown().forEach { it.deleteOnExit() }
            val zis = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(data))
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(tempDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
            tempDir
        } catch (_: Exception) {
            null
        }
    }

    /**
     * If [payload] is an NCPP-encrypted project (same scheme as the Android
     * [org.catrobat.catroid.io.ProjectCrypto] used by the Baked APK), decrypt it
     * with the shared payload password. Otherwise return it unchanged so plain
     * (legacy) projects still load.
     *
     * Format: [magic "NCPP":4][salt:32][iv:12][ciphertext]
     */
    private fun maybeDecrypt(payload: ByteArray): ByteArray {
        val magic = CRYPTO_MAGIC.toByteArray(StandardCharsets.US_ASCII)
        if (payload.size >= magic.size && payload.copyOfRange(0, magic.size).contentEquals(magic)) {
            return decryptPayload(payload)
        }
        return payload
    }

    private fun decryptPayload(data: ByteArray): ByteArray {
        val input = java.io.ByteArrayInputStream(data)
        val header = ByteArray(4)
        if (input.read(header) < 4 ||
            !header.contentEquals(CRYPTO_MAGIC.toByteArray(StandardCharsets.US_ASCII))
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
