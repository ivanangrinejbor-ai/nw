package org.catrobat.catroid.apkbuildV3

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class V3IconInjectionTest {

    @Test
    fun injectAppIcon_replacesLauncherPngsAndKeepsAdaptiveXml() {
        val base = File("src/main/assets/template_runtime.apk")
        assumeTrue("template_runtime.apk not found", base.exists())
        val fakeIcon = File("src/main/res/mipmap-hdpi/ic_launcher_round.png")
        assumeTrue("test icon png not found", fakeIcon.exists())

        val work = File("build/tmp/v3_icon")
        work.mkdirs()
        val input = File(work, "in.apk")
        base.copyTo(input, overwrite = true)
        val output = File(work, "out.apk")
        if (output.exists()) output.delete()
        val iconBytes = fakeIcon.readBytes()

        assertTrue(V3ApkAssembler.injectAppIcon(input, output, fakeIcon))
        assertTrue(output.exists())

        var pngChecked = 0
        var xmlChecked = 0
        ZipFile(output).use { zf ->
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val fileName = entry.name.substringAfterLast('/')
                if (!fileName.startsWith("ic_launcher")) continue
                val content = zf.getInputStream(entry).readBytes()
                if (fileName.endsWith(".png", ignoreCase = true)) {
                    assertTrue(
                        "launcher png must be replaced: ${entry.name}",
                        content.contentEquals(iconBytes)
                    )
                    pngChecked++
                } else if (fileName.endsWith(".xml", ignoreCase = true)) {
                    assertFalse(
                        "adaptive icon xml must not be overwritten with png bytes: ${entry.name}",
                        content.contentEquals(iconBytes)
                    )
                    xmlChecked++
                }
            }
        }
        assertTrue("expected launcher png targets, got $pngChecked", pngChecked > 0)
        println("icon targets replaced: png=$pngChecked, xml kept=$xmlChecked")
    }

    @Test
    fun injectAppIcon_missingIconFileReturnsFalse() {
        val work = File("build/tmp/v3_icon")
        work.mkdirs()
        val input = File(work, "dummy_in.apk")
        input.writeBytes(byteArrayOf(1, 2, 3))
        val output = File(work, "dummy_out.apk")
        if (output.exists()) output.delete()

        assertFalse(
            V3ApkAssembler.injectAppIcon(input, output, File(work, "no_such_icon.png"))
        )
        assertFalse(output.exists())
    }
}
