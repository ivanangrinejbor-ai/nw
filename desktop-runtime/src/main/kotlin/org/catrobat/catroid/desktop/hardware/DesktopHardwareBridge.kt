package org.catrobat.catroid.desktop.hardware

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI

object DesktopHardwareBridge {

    fun setFlashlight(enabled: Boolean) {}

    fun vibrate(milliseconds: Long) {}

    fun vibratePattern(pattern: LongArray) {}

    fun copyToClipboard(text: String) {
        try {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        } catch (e: Exception) {
            System.err.println("[DesktopHardware] Failed to copy to clipboard: ${e.message}")
        }
    }

    fun shareText(text: String) {
        copyToClipboard(text)
    }

    fun openApp(packageNameOrCommand: String) {
        try {
            if (packageNameOrCommand.startsWith("http://", ignoreCase = true) ||
                packageNameOrCommand.startsWith("https://", ignoreCase = true)
            ) {
                openUrl(packageNameOrCommand)
            } else {
                val file = File(packageNameOrCommand)
                if (file.exists() && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file)
                } else {
                    Runtime.getRuntime().exec(packageNameOrCommand)
                }
            }
        } catch (e: Exception) {
            System.err.println("[DesktopHardware] Failed to open app/command: $packageNameOrCommand (${e.message})")
        }
    }

    fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            System.err.println("[DesktopHardware] Failed to open URL: $url (${e.message})")
        }
    }

    var isShaking: Boolean = false
        private set

    fun triggerShake() {
        isShaking = true
    }

    fun resetShake() {
        isShaking = false
    }

    fun onNfcScanned(tagId: String) {}
    fun sendArduinoData(pin: Int, value: Int) {}
    fun sendRaspberryPiData(pin: Int, value: Int) {}
    fun sendLegoCommand(command: String) {}
    fun sendDroneCommand(command: String) {}
}
