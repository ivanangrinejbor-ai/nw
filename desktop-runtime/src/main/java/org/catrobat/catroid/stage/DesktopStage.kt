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

    @JvmStatic
    fun main(args: Array<String>) {
        // 1. Регистрируем все десктопные сервисы
        RuntimeServicesHolder.services = DesktopRuntimeServices()
        AudioServiceHolder.audioService = DesktopAudioService()
        MidiServiceHolder.midiService = DesktopMidiService()
        TextServiceHolder.textService = DesktopTextService()
        NotificationServiceHolder.service = DesktopNotificationService()
        NetworkServiceHolder.service = DesktopNetworkService()

        // 2. Определяем источник проекта
        val embeddedPayloadRequested = args.any { it == EMBEDDED_PAYLOAD_FLAG }
        val projectInput = args.firstOrNull { !it.startsWith("--") && it != EMBEDDED_PAYLOAD_FLAG }
        val loadedProject: File? = when {
            embeddedPayloadRequested -> loadEmbeddedPayload()?.let { extractPayload(it) }
            projectInput != null -> resolveProjectInput(File(projectInput))
            else -> null
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
        return try {
            val tempDir = Files.createTempDirectory("neocatroid-player").toFile()
            // Рекурсивно удалить всё при выходе (walk + deleteOnExit на каждом файле)
            tempDir.walkTopDown().forEach { it.deleteOnExit() }
            val zis = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(payload))
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
}
