package org.catrobat.catroid.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import org.catrobat.catroid.runtime.RuntimeServicesHolder
import java.io.File

object DesktopLauncher {

    @JvmStatic
    fun main(args: Array<String>) {
        val targetPath = if (args.isNotEmpty()) args[0] else "projects/default"
        val projectDir = File(targetPath)

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
}
