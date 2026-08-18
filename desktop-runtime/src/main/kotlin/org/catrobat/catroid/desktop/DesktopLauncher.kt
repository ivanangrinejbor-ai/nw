package org.catrobat.catroid.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import org.catrobat.catroid.desktop.project.DesktopProjectLoader
import org.catrobat.catroid.runtime.RuntimeServicesHolder
import java.io.File

object DesktopLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        val target = if (args.isNotEmpty()) args[0] else "projects/default"
        val targetFile = File(target)
        val workDir = File("neocatroid_work").absoluteFile
        val projectDir = resolveProjectDir(targetFile, workDir)

        val runtimeServices = DesktopRuntimeServices(projectDir)
        RuntimeServicesHolder.services = runtimeServices

        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("NeoCatroid Player - ${projectDir.name}")
            setWindowedMode(1280, 720)
            useVsync(true)
            setForegroundFPS(60)
            setBackBufferConfig(8, 8, 8, 8, 16, 0, 4)
        }

        println("Starting NeoCatroid Desktop Player (LWJGL 3)...")
        Lwjgl3Application(DesktopGameListener(projectDir), config)
    }

    private fun resolveProjectDir(targetFile: File, workDir: File): File {
        val loader = DesktopProjectLoader(targetFile)

        // 1. EXE/файл с NEOCAT01-футером → извлечь payload
        if (loader.hasEmbeddedPayload(targetFile)) {
            val out = File(workDir, "embedded")
            if (loader.extractEmbeddedPayload(targetFile, out)) {
                println("Embedded payload extracted to $out")
                return out
            }
        }

        // 2. .catrobat бандл → распаковать
        if (targetFile.name.endsWith(".catrobat", ignoreCase = true)) {
            val out = File(workDir, targetFile.nameWithoutExtension)
            val extracted = loader.extractIfCatrobatBundle(targetFile, out)
            println("Catrobat bundle extracted to $extracted")
            return extracted
        }

        // 3. папка с проектом
        return targetFile
    }
}