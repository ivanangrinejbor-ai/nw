package org.catrobat.catroid.test.robolectric.exebuildv2

import android.content.Context
import org.catrobat.catroid.exebuildv2.ExeBuilderV2
import org.catrobat.catroid.io.ProjectCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
class ExeBuilderV2FooterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    private fun bytesContain(haystack: ByteArray, needle: ByteArray): Boolean {
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return true
        }
        return false
    }

    private fun makeProjectDir(root: File): File {
        val dir = File(root, "TestGame")
        dir.mkdirs()
        File(dir, "code.xml").writeText(
            "<?xml version=\"1.0\"?><program><header screenWidth=\"800\" screenHeight=\"600\"/>" +
                "<objectList><object type=\"Sprite\" name=\"Hero\"><scriptList/></object></objectList></program>"
        )
        File(dir, "images").mkdirs()
        File(dir, "images/ball.png").writeBytes(byteArrayOf(1, 2, 3))
        File(dir, "sounds").mkdirs()
        File(dir, "sounds/boop.wav").writeBytes(byteArrayOf(9, 9))
        File(dir, "undo_code.xml").writeText("should be excluded")
        return dir
    }

    private fun footerWebZip(output: File): ByteArray {
        val size = ExeBuilderV2.readFooterPayloadSize(output)!!
        val bytes = ByteArray(size.toInt())
        output.inputStream().use { input ->
            input.skip(output.length() - 16 - size)
            input.read(bytes)
        }
        return bytes
    }

    @Test
    fun footerLayout_isStubPlusWebZipPlusTag() {
        val projectDir = makeProjectDir(File(context.cacheDir, "exe_test_1"))
        val output = File(context.cacheDir, "out_1.exe")
        output.delete()

        ExeBuilderV2.build(context, projectDir, "TestGame", output)

        val size = ExeBuilderV2.readFooterPayloadSize(output)
        assertNotNull("footer magic must be present", size)
        // stub (exe asset) должен быть в начале файла без изменений
        val stubLen = context.assets.open("exe_v2/NeoCatroid.exe").use { it.available().toLong() }
        assertEquals("web.zip starts right after the stub", output.length() - 16 - size!!, stubLen)

        val names = mutableListOf<String>()
        ZipInputStream(footerWebZip(output).inputStream()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                names.add(e.name)
                e = zis.nextEntry
            }
        }
        assertEquals(listOf("app.html", "player.js", "title.txt", "project.pak"), names)
    }

    @Test
    fun webZipContainsPlayerFilesWithContent() {
        val projectDir = makeProjectDir(File(context.cacheDir, "exe_test_3"))
        val output = File(context.cacheDir, "out_3.exe")
        output.delete()

        ExeBuilderV2.build(context, projectDir, "TestGame", output)

        val contents = mutableMapOf<String, ByteArray>()
        ZipInputStream(footerWebZip(output).inputStream()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                contents[e.name] = zis.readBytes()
                e = zis.nextEntry
            }
        }
        assertTrue(bytesContain(contents.getValue("app.html"), "NCBoot".toByteArray()))
        assertTrue(bytesContain(contents.getValue("player.js"), "NCEngine".toByteArray()))
        assertEquals("TestGame", String(contents.getValue("title.txt"), Charsets.UTF_8))
    }

    @Test
    fun projectPak_isNcpwContainerWithNcppInside() {
        val projectDir = makeProjectDir(File(context.cacheDir, "exe_test_2"))
        val output = File(context.cacheDir, "out_2.exe")
        output.delete()

        ExeBuilderV2.build(context, projectDir, "TestGame", output)

        var pakData: ByteArray? = null
        ZipInputStream(footerWebZip(output).inputStream()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                if (e.name == "project.pak") pakData = zis.readBytes()
                e = zis.nextEntry
            }
        }
        assertNotNull("project.pak entry required", pakData)
        pakData = pakData!!

        // NCPW: magic + len(BE) + password(UTF-8) + NCPP-поток
        val magic = String(pakData, 0, 4, Charsets.US_ASCII)
        assertEquals("NCPW", magic)
        val pwdLen = ((pakData[4].toInt() and 0xFF) shl 24) or ((pakData[5].toInt() and 0xFF) shl 16) or
            ((pakData[6].toInt() and 0xFF) shl 8) or (pakData[7].toInt() and 0xFF)
        assertEquals(32, pwdLen) // 16 random bytes as hex
        val password = String(pakData, 8, pwdLen, Charsets.UTF_8)
        val inner = pakData.copyOfRange(8 + pwdLen, pakData.size)
        val innerMagic = String(inner, 0, 4, Charsets.US_ASCII)
        assertEquals("NCPP", innerMagic)

        // Полный round-trip: расшифровать NCPP и прочитать code.xml
        val ncppFile = File(context.cacheDir, "roundtrip.ncpp")
        ncppFile.writeBytes(inner)
        val decrypted = File(context.cacheDir, "roundtrip.zip")
        assertTrue(ProjectCrypto.decrypt(ncppFile, decrypted, password))

        val names = mutableListOf<String>()
        ZipInputStream(decrypted.inputStream()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                names.add(e.name)
                e = zis.nextEntry
            }
        }
        assertTrue("code.xml in payload", names.contains("code.xml"))
        assertTrue("images/ball.png in payload", names.contains("images/ball.png"))
        assertTrue("sounds/boop.wav in payload", names.contains("sounds/boop.wav"))
        assertTrue("undo_code.xml excluded", !names.contains("undo_code.xml"))
        assertTrue("junk not in payload", names.none { it.startsWith("exe_v2_tmp") })
    }
}