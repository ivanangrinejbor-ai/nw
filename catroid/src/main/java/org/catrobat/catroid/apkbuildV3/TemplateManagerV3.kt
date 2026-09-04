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
        "https://raw.githubusercontent.com/Ivproduction-dev/Neocatroid-Template/main/template_runtime.apk"
    private const val TEMPLATE_RUNTIME_MEDIA_URL =
        "https://media.githubusercontent.com/media/Ivproduction-dev/Neocatroid-Template/main/template_runtime.apk"
    private const val LFS_POINTER_PREFIX = "version https://git-lfs.github.com/spec/v1"
    private const val CACHE_DIR_NAME = "v3_template"
    private const val CACHE_FILE_NAME = "template_runtime_v3.apk"
    private const val ETAG_FILE_NAME = "template_runtime_v3.apk.etag"
    private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
    private const val FALLBACK_TEMPLATE_SIZE = 200L * 1024 * 1024

    data class TemplateCacheStatus(val cached: Boolean, val sizeBytes: Long)

    enum class TemplateFailure { NO_SPACE, NETWORK, BAD_FILE }

    sealed interface TemplateOutcome {
        data class Ready(val file: File, val updated: Boolean) : TemplateOutcome
        data class Failed(val failure: TemplateFailure, val detail: String, val cachedFile: File?) : TemplateOutcome
    }

    private data class FetchResult(val success: Boolean, val etag: String, val detail: String)

    fun getCacheStatus(context: Context): TemplateCacheStatus {
        val cached = getCachedTemplate(context)
        return if (cached != null && isZip(cached)) {
            TemplateCacheStatus(true, cached.length())
        } else {
            TemplateCacheStatus(false, 0L)
        }
    }

    fun ensureCachedTemplate(
        context: Context,
        force: Boolean = false,
        onProgress: ((Float, String) -> Unit)? = null
    ): TemplateOutcome {
        val cached = getCachedTemplate(context)
        val validCache = if (cached != null && isZip(cached)) cached else null
        if (!force && validCache != null) return TemplateOutcome.Ready(validCache, false)
        return downloadTemplate(context, validCache, onProgress)
    }

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
            if (copyValidTemplate(cached, target)) {
                Log.d(TAG, "Использую кэшированный шаблон (${cached.length() / (1024 * 1024)} MB)")
                return target
            }
            reasons += "кэшированный шаблон не является корректным APK"
        }

        val outcome = downloadTemplate(context, null) { p, msg ->
            onProgress?.invoke(p, msg)
        }
        val downloaded = (outcome as? TemplateOutcome.Ready)?.file
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
            val failure = outcome as TemplateOutcome.Failed
            reasons += "не удалось скачать шаблон с GitHub (${failure.failure}: ${failure.detail})"
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

    private fun downloadTemplate(
        context: Context,
        previousCache: File?,
        onProgress: ((Float, String) -> Unit)?
    ): TemplateOutcome {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        cacheDir.mkdirs()
        val tmp = File(cacheDir, "$CACHE_FILE_NAME.download")
        val finalFile = File(cacheDir, CACHE_FILE_NAME)
        val etagFile = File(cacheDir, ETAG_FILE_NAME)

        if (!hasEnoughSpace(cacheDir, FALLBACK_TEMPLATE_SIZE + 64L * 1024 * 1024)) {
            Log.e(TAG, "Недостаточно места для скачивания шаблона в ${cacheDir.absolutePath}")
            return TemplateOutcome.Failed(TemplateFailure.NO_SPACE, "", previousCache)
        }

        return try {
            var fetch = fetchToFile(client(15, 300), TEMPLATE_RUNTIME_URL, tmp, onProgress)
            if (!fetch.success) {
                tmp.delete()
                return TemplateOutcome.Failed(TemplateFailure.NETWORK, fetch.detail, previousCache)
            }
            if (isLfsPointer(tmp)) {
                Log.d(TAG, "raw URL вернул LFS-указатель, скачиваю с media.githubusercontent.com")
                tmp.delete()
                fetch = fetchToFile(client(15, 300), TEMPLATE_RUNTIME_MEDIA_URL, tmp, onProgress)
                if (!fetch.success) {
                    tmp.delete()
                    return TemplateOutcome.Failed(TemplateFailure.NETWORK, fetch.detail, previousCache)
                }
            }
            if (!isZip(tmp)) {
                Log.e(TAG, "Скачанный файл не является ZIP/APK")
                tmp.delete()
                return TemplateOutcome.Failed(TemplateFailure.BAD_FILE, "", previousCache)
            }
            val storedEtag = runCatching { etagFile.readText().trim() }.getOrNull().orEmpty()
            if (previousCache != null && previousCache.exists() &&
                fetch.etag.isNotEmpty() && fetch.etag == storedEtag &&
                tmp.length() == previousCache.length()
            ) {
                tmp.delete()
                return TemplateOutcome.Ready(previousCache, false)
            }
            if (finalFile.exists()) finalFile.delete()
            if (!tmp.renameTo(finalFile)) {
                tmp.copyTo(finalFile, overwrite = true)
                tmp.delete()
            }
            runCatching { etagFile.writeText(fetch.etag) }
            TemplateOutcome.Ready(finalFile, true)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка скачивания шаблона", e)
            tmp.delete()
            TemplateOutcome.Failed(TemplateFailure.NETWORK, e.message.orEmpty(), previousCache)
        }
    }

    private fun fetchToFile(
        client: OkHttpClient,
        url: String,
        target: File,
        onProgress: ((Float, String) -> Unit)?
    ): FetchResult {
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "Скачивание $url: HTTP ${resp.code}")
                    return FetchResult(false, "", "HTTP ${resp.code}")
                }
                val body = resp.body ?: return FetchResult(false, "", "HTTP ${resp.code}")
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
                FetchResult(true, resp.header("ETag").orEmpty(), "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка скачивания $url", e)
            FetchResult(false, "", e.message.orEmpty())
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
