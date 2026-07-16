package org.catrobat.catroid.apkbuildV3

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Локатор базового шаблона APK для сборщика V3.
 *
 * Не использует существующий модуль apkbuild. Просто находит подходящий
 * базовый APK (готовый runtime-шаблон из assets либо собственный установленный APK)
 * и копирует его в рабочую директорию. Вся дальнейшая сборка выполняется
 * классом [V3ApkAssembler].
 */
object TemplateManagerV3 {
    private const val TAG = "TemplateManagerV3"
    private const val TEMPLATE_RUNTIME_ASSET = "template_runtime.apk"

    /**
     * Возвращает копию базового APK в рабочей директории.
     * Приоритет: template_runtime.apk из assets → собственный APK.
     *
     * Бросает [IllegalStateException] с описанием обеих причин отказа, чтобы
     * пользователь видел реальную проблему (нет файла в assets, нет места на
     * диске, недоступен собственный APK), а не обобщённое сообщение.
     */
    fun prepareBaseApk(context: Context, workDir: File): File {
        workDir.mkdirs()
        val target = File(workDir, "v3_base.apk")
        val reasons = mutableListOf<String>()

        // 0) Сначала проверяем свободное место в рабочей директории.
        val templateSize = runCatching {
            context.assets.open(TEMPLATE_RUNTIME_ASSET).use { it.available().toLong() }
        }.getOrElse { 0L }
        val needed = (templateSize.takeIf { it > 0 } ?: 200L * 1024 * 1024) + 64L * 1024 * 1024
        if (!hasEnoughSpace(workDir, needed)) {
            reasons += "недостаточно места в ${workDir.absolutePath} (нужно ~${needed / (1024 * 1024)} МБ)"
        }

        // 1) Готовый runtime-шаблон
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

        // 2) Собственный установленный APK
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

    /**
     * Проверяет, что в [dir] доступно не менее [neededBytes] свободного места.
     */
    private fun hasEnoughSpace(dir: File, neededBytes: Long): Boolean {
        return runCatching {
            val stat = android.os.StatFs(dir.absolutePath)
            val usable = stat.blockSizeLong * stat.availableBlocksLong
            usable >= neededBytes
        }.getOrDefault(true)
    }

    /**
     * Минимальная проверка, что файл — это ZIP/APK (магия "PK").
     */
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
