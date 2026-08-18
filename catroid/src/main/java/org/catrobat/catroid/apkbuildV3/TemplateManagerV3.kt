package org.catrobat.catroid.apkbuildV3

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object TemplateManagerV3 {
    private const val TAG = "TemplateManagerV3"
    private const val TEMPLATE_RUNTIME_ASSET = "template_runtime.apk"
    private const val TEMPLATE_RUNTIME_URL =
        "https://raw.githubusercontent.com/ivanangrinejbor-ai/Neocatroid-Template/main/template_runtime.apk"
    private const val TEMPLATE_RUNTIME_MEDIA_URL =
        "https://media.githubusercontent.com/media/ivanangrinejbor-ai/Neocatroid-Template/main/template_runtime.apk"
    private const val LFS_POINTER_PREFIX = "version https://git-lfs.github.com/spec/v1"
    private const val CACHE_DIR_NAME = "v3_template"
    private const val CACHE_FILE_NAME = "template_runtime_v3.apk"
    private const val ETAG_FILE_NAME = "template_runtime_v3.apk.etag"
    private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
    private const val FALLBACK_TEMPLATE_SIZE = 200L * 1024 * 1024

    fun prepareBaseApk(
        context: Context,
        workDir: File,
        onProgress: ((Float, String) -> Unit)? = null
    ): File {
        workDir.mkdirs()
        val target = File(workDir, "v3_base.apk")
        val reasons = mutableListOf<String>()

        val cached = getCachedTemplate(context)
        if (cached != null && isZip(cached)) {
            if (isTemplateFresh(cached)) {
                Log.d(TAG, "Использую кэшированный шаблон (${cached.length() / (1024 * 1024)} MB)")
                if (copyValidTemplate(cached, target)) {
                    return target
                }
                reasons += "кэшированный шаблон не является корректным APK"
            } else {
                Log.d(TAG, "Шаблон устарел, скачиваю новую версию")
            }
        }

        onProgress?.invoke(0f, "Скачивание runtime-шаблона...")
        val downloaded = downloadTemplate(context, onProgress)
        if (downloaded != null) {
            if (!hasEnoughSpace(workDir, downloaded.length() + 64L * 1024 * 1024)) {
                reasons += "недостаточно места в ${workDir.absolutePath} для копирования шаблона"
            } else if (isZip(downloaded) && copyValidTemplate(downloaded, target)) {
                Log.d(TAG, "Базовый шаблон: скачан с GitHub (${downloaded.length() / (1024 * 1024)} MB)")
                return target
            } else {
                reasons += "скачанный шаблон не является корректным APK"
            }
        } else {
            reasons += "не удалось скачать шаблон с GitHub"
            if (cached != null && isZip(cached) &&
                hasEnoughSpace(workDir, cached.length() + 64L * 1024 * 1024) &&
                copyValidTemplate(cached, target)
            ) {
                Log.w(TAG, "Офлайн: использую устаревший кэш шаблона")
                return target
            }
        }

        val templateSize = runCatching {
            context.assets.open(TEMPLATE_RUNTIME_ASSET).use { it.available().toLong() }
        }.getOrElse { 0L }
        val needed = (templateSize.takeIf { it > 0 } ?: FALLBACK_TEMPLATE_SIZE) + 64L * 1024 * 1024
        if (!hasEnoughSpace(workDir, needed)) {
            reasons += "недостаточно места в ${workDir.absolutePath} (нужно ~${needed / (1024 * 1024)} МБ)"
        }

        if (reasons.isEmpty()) {
            try {
                context.assets.open(TEMPLATE_RUNTIME_ASSET).use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
                if (target.exists() && target.length() > 0 && isZip(target)) {
                    Log.d(TAG, "Базовый шаблон: $TEMPLATE_RUNTIME_ASSET (${target.length() / (1024 * 1024)} MB)")
                    return target
                }
                reasons += "template_runtime.apk скопирован, но не является корректным APK " +
                        "(размер=${target.length()})"
            } catch (e: Exception) {
                reasons += "template_runtime.apk недоступен: ${e.message}"
                Log.d(TAG, "template_runtime.apk недоступен: ${e.message}", e)
            }
        }

        val selfPath = context.applicationInfo.sourceDir
        if (selfPath != null && File(selfPath).exists()) {
            try {
                File(selfPath).copyTo(target, overwrite = true)
                if (target.exists() && target.length() > 0 && isZip(target)) {
                    Log.d(TAG, "Базовый шаблон: собственный APK (${target.length() / (1024 * 1024)} MB)")
                    return target
                }
                reasons += "собственный APK скопирован, но не является корректным APK"
            } catch (e: Exception) {
                reasons += "не удалось скопировать собственный APK: ${e.message}"
                Log.e(TAG, "Не удалось скопировать собственный APK", e)
            }
        } else {
            reasons += "путь собственного APK (applicationInfo.sourceDir) отсутствует"
        }

        throw IllegalStateException(
            "Базовый шаблон не найден. Причины: " + reasons.joinToString("; ")
        )
    }

    private fun getCachedTemplate(context: Context): File? {
        val dir = File(context.filesDir, CACHE_DIR_NAME)
        val file = File(dir, CACHE_FILE_NAME)
        return if (file.exists() && file.length() > 0) file else null
    }

    private fun isTemplateFresh(cached: File): Boolean {
        val etagFile = File(cached.parentFile, ETAG_FILE_NAME)
        val stored = runCatching { etagFile.readText().trim() }.getOrNull()
        if (stored.isNullOrEmpty()) return false
        return try {
            val client = client(10, 10)
            val resp = client.newCall(Request.Builder().url(TEMPLATE_RUNTIME_MEDIA_URL).head().build()).execute()
            resp.use {
                it.isSuccessful && it.header("ETag") == stored
            }
        } catch (e: Exception) {
            Log.d(TAG, "HEAD-проверка шаблона недоступна, использую кэш: ${e.message}")
            true
        }
    }

    private fun downloadTemplate(context: Context, onProgress: ((Float, String) -> Unit)?): File? {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        cacheDir.mkdirs()
        val tmp = File(cacheDir, "$CACHE_FILE_NAME.download")
        val finalFile = File(cacheDir, CACHE_FILE_NAME)
        val etagFile = File(cacheDir, ETAG_FILE_NAME)

        if (!hasEnoughSpace(cacheDir, FALLBACK_TEMPLATE_SIZE + 64L * 1024 * 1024)) {
            Log.e(TAG, "Недостаточно места для скачивания шаблона в ${cacheDir.absolutePath}")
            return null
        }

        return try {
            var etag = downloadToFile(client(15, 300), TEMPLATE_RUNTIME_URL, tmp, onProgress)
            if (etag == null) {
                tmp.delete()
                return null
            }
            if (isLfsPointer(tmp)) {
                Log.d(TAG, "raw URL вернул LFS-указатель, скачиваю с media.githubusercontent.com")
                tmp.delete()
                etag = downloadToFile(client(15, 300), TEMPLATE_RUNTIME_MEDIA_URL, tmp, onProgress)
                if (etag == null) {
                    tmp.delete()
                    return null
                }
            }
            if (!isZip(tmp)) {
                Log.e(TAG, "Скачанный файл не является ZIP/APK")
                tmp.delete()
                return null
            }
            if (finalFile.exists()) finalFile.delete()
            tmp.renameTo(finalFile)
            runCatching { etagFile.writeText(etag.orEmpty()) }
            finalFile
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка скачивания шаблона", e)
            tmp.delete()
            null
        }
    }

    private fun downloadToFile(
        client: OkHttpClient,
        url: String,
        target: File,
        onProgress: ((Float, String) -> Unit)?
    ): String? {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "Скачивание $url: HTTP ${resp.code}")
                    return null
                }
                val body = resp.body ?: return null
                val total = body.contentLength()
                target.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress?.invoke(
                                    (downloaded.toFloat() / total).coerceIn(0f, 1f),
                                    "Скачивание шаблона (${downloaded / (1024 * 1024)}/${total / (1024 * 1024)} MB)"
                                )
                            }
                        }
                    }
                }
                resp.header("ETag")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка скачивания $url", e)
            null
        }
    }

    private fun isLfsPointer(file: File): Boolean {
        return runCatching {
            file.inputStream().use { input ->
                val buf = ByteArray(128)
                val read = input.read(buf)
                read > 0 && String(buf, 0, read).startsWith(LFS_POINTER_PREFIX)
            }
        }.getOrDefault(false)
    }

    private fun client(connectSeconds: Long, readSeconds: Long): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(connectSeconds, TimeUnit.SECONDS)
            .readTimeout(readSeconds, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private fun copyValidTemplate(source: File, target: File): Boolean {
        return runCatching {
            source.copyTo(target, overwrite = true)
            target.exists() && target.length() > 0 && isZip(target)
        }.getOrDefault(false)
    }

    private fun hasEnoughSpace(dir: File, neededBytes: Long): Boolean {
        return runCatching {
            val stat = android.os.StatFs(dir.absolutePath)
            val usable = stat.blockSizeLong * stat.availableBlocksLong
            usable >= neededBytes
        }.getOrDefault(true)
    }

    private fun isZip(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return runCatching {
            file.inputStream().use { is32 ->
                val head = ByteArray(4)
                val read = is32.read(head)
                read == 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
            }
        }.getOrDefault(false)
    }
}
