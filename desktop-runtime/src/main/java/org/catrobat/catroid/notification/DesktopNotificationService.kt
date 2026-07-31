package org.catrobat.catroid.notification

import org.catrobat.catroid.content.notification.NotificationData
import org.catrobat.catroid.content.notification.NotificationStorage
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.swing.JOptionPane

class DesktopNotificationService : NotificationService {
    private val isHeadless = GraphicsEnvironment.isHeadless()

    override fun show(id: Int) {
        val data = NotificationStorage.get(id) ?: return
        showTray(data)
    }

    override fun showScheduled(id: Int, delayMs: Long) {
        if (delayMs <= 0) {
            show(id)
            NotificationStorage.removeNotification(id)
            return
        }
        Thread {
            try {
                Thread.sleep(delayMs)
            } catch (_: InterruptedException) {
            }
            show(id)
            NotificationStorage.removeNotification(id)
        }.start()
    }

    override fun remove(id: Int) {
        NotificationStorage.removeNotification(id)
    }

    override fun ensureChannel(name: String, importance: Int) {
    }

    private fun showTray(data: NotificationData) {
        if (isHeadless) return
        try {
            if (!SystemTray.isSupported()) {
                JOptionPane.showMessageDialog(null, data.text, data.title, JOptionPane.INFORMATION_MESSAGE)
                return
            }
            val tray = SystemTray.getSystemTray()
            val icon: Image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
            val trayIcon = TrayIcon(icon, data.title)
            trayIcon.toolTip = data.title
            tray.add(trayIcon)
            trayIcon.displayMessage(data.title, data.text, TrayIcon.MessageType.INFO)
            Thread {
                try {
                    Thread.sleep(5000)
                } catch (_: InterruptedException) {
                }
                try {
                    tray.remove(trayIcon)
                } catch (_: Exception) {
                }
            }.start()
        } catch (_: Exception) {
        }
    }
}
