package org.catrobat.catroid.exebuildv2

import android.content.Context
import android.content.res.AssetManager
import org.catrobat.catroid.io.ProjectCrypto
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * EXE Builder V2 — единый автономный NeoCatroid.exe (WebView2).
 *
 * Структура выходного файла:
 *   [NeoCatroid.exe (stub)] [web.zip] [size: Int64 LE] [NEOCAT01]
 *
 * web.zip содержит:
 *   app.html, player.js, title.txt, project.pak
 *
 * project.pak = NCPW-контейнер (NCPW + len BE + password + NCPP-поток),
 * где NCPP = AES-256-GCM + PBKDF2(100000) с СЛУЧАЙНЫМ паролем (как Baked APK).
 * Пароль внутри контейнера читает C#-stub (NeoCatroidStub.ReadFooterPayload) ->
 * WebView2 -> player.js (NCCrypto от NCPW/NCPP/NCPX/NCPS).
 */
object ExeBuilderV2 {
    private const val TAG = "ExeBuilderV2"
    private const val FOOTER_MAGIC = "NEOCAT01"
    private const val STREAM_BUFFER = 64 * 1024

    class BuilderException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun build(
        context: Context,
        projectDir: File,
        projectName: String,
        output: File
    ) {
        val assets: AssetManager = context.assets
        val tmpDir = File(context.cacheDir, "exe_v2_tmp").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            // 1. Экзешник-заглушка и веб-рантайм из ассетов
            val exeFile = File(tmpDir, "NeoCatroid.exe").also {
                copyAssetToFile(assets, "exe_v2/NeoCatroid.exe", it)
            }
            val playerJs = File(tmpDir, "player.js").also {
                copyAssetToFile(assets, "exe_v2/player.js", it)
            }
            val appHtml = File(tmpDir, "app.html").also {
                copyAssetToFile(assets, "exe_v2/app.html", it)
            }

            // 2. Случайный пароль на каждую сборку
            val password = ProjectCrypto.generateRandomPassword()

            // 3. Проект -> zip -> NCPP (AES-256-GCM + PBKDF2, магия NCPP)
            val projectZip = File(tmpDir, "project.zip")
            zipDirectory(projectDir, projectZip)
            val projectNcpp = File(tmpDir, "project.ncpp")
            ProjectCrypto.encrypt(projectZip, projectNcpp, password)

            // 4. web.zip: рантайм + project.pak = NCPW(password) + NCPP
            val webZip = File(tmpDir, "web.zip")
            FileOutputStream(webZip).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    zos.setLevel(6)
                    zos.putNextEntry(ZipEntry("app.html"))
                    copyStream(appHtml.inputStream(), zos)
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("player.js"))
                    copyStream(playerJs.inputStream(), zos)
                    zos.closeEntry()
                    val title = projectName
                        .replace('\n', ' ')
                        .replace('\r', ' ')
                        .take(200)
                    zos.putNextEntry(ZipEntry("title.txt"))
                    zos.write(title.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("project.pak"))
                    ProjectCrypto.writePasswordContainerHeader(zos, password)
                    copyStream(projectNcpp.inputStream(), zos)
                    zos.closeEntry()
                }
            }

            // 5. Итоговый exe = stub + web.zip + footer(size Int64 LE + NEOCAT01)
            FileOutputStream(output).use { out ->
                copyStream(exeFile.inputStream(), out)
                val webZipSize = webZip.length()
                copyStream(webZip.inputStream(), out)
                val sizeBytes = ByteArray(8)
                var v = webZipSize
                for (i in 0 until 8) {
                    sizeBytes[i] = (v and 0xFF).toByte()
                    v = v ushr 8
                }
                out.write(sizeBytes)
                out.write(FOOTER_MAGIC.toByteArray(Charsets.US_ASCII))
            }
        } catch (e: Throwable) {
            throw BuilderException("EXE v2 build failed: ${e.message}", e)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    /** Размер payload по footer (зеркало NeoCatroidStub.ReadFooterPayload). */
    fun readFooterPayloadSize(file: File): Long? {
        if (!file.exists() || file.length() < 16) return null
        file.inputStream().use { input ->
            input.skip(file.length() - 16)
            val tail = ByteArray(16)
            if (input.read(tail) != 16) return null
            val magic = String(tail, 8, 8, Charsets.US_ASCII)
            if (magic != FOOTER_MAGIC) return null
            var size = 0L
            for (i in 7 downTo 0) size = (size shl 8) or (tail[i].toLong() and 0xFF)
            if (size <= 0 || size > file.length() - 16) return null
            return size
        }
    }

    private fun zipDirectory(dir: File, outZip: File) {
        FileOutputStream(outZip).use { fos ->
            ZipOutputStream(fos).use { zos ->
                zos.setLevel(6)
                val buffer = ByteArray(STREAM_BUFFER)
                dir.walkTopDown().filter { it != dir }.forEach { file ->
                    val entryPath = file.relativeTo(dir).path.replace('\\', '/')
                    if (file.name == "undo_code.xml") return@forEach
                    if (file.isDirectory) {
                        zos.putNextEntry(ZipEntry("$entryPath/"))
                        zos.closeEntry()
                    } else {
                        zos.putNextEntry(ZipEntry(entryPath))
                        file.inputStream().use { input ->
                            var n: Int
                            while (input.read(buffer).also { n = it } != -1) {
                                if (n > 0) zos.write(buffer, 0, n)
                            }
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
    }

    private fun copyAssetToFile(assets: AssetManager, assetPath: String, dest: File) {
        try {
            assets.open(assetPath).use { input -> copyStream(input, dest) }
        } catch (e: Exception) {
            throw BuilderException("Asset missing: $assetPath", e)
        }
    }

    private fun copyStream(input: InputStream, dest: File) {
        FileOutputStream(dest).use { out -> copyStream(input, out) }
    }

    private fun copyStream(input: InputStream, out: java.io.OutputStream) {
        val buffer = ByteArray(STREAM_BUFFER)
        var n: Int
        while (input.read(buffer).also { n = it } != -1) {
            if (n > 0) out.write(buffer, 0, n)
        }
    }
}