package org.catrobat.catroid.ai.localization

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.*
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.io.StorageOperations
import java.io.File
import java.io.FileOutputStream

class SpriteLocalizer(
    private val context: Context,
    private val targetLanguage: String,
    private val sourceLanguage: String = "auto",
    private val quality: QualityMode = QualityMode.STANDARD,
    private val isPixelArt: Boolean = false
) {
    var onProgress: ((spriteIndex: Int, totalSprites: Int, spriteName: String, status: String) -> Unit)? = null
    var onComplete: ((LocalizationReport) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var geminiCalls = 0
    private val ocrConfidences = mutableListOf<Float>()
    private val textExpansions = mutableListOf<Float>()

    fun localizeProject() {
        scope.launch {
            val startTime = System.currentTimeMillis()
            val project = ProjectManager.getInstance().currentProject ?: run {
                val report = LocalizationReport(targetLanguage, 0, 0, 0, emptyList(), startTime, System.currentTimeMillis())
                withContext(Dispatchers.Main) { onComplete?.invoke(report) }
                return@launch
            }

            val spritesToProcess = collectSpritesWithText(project)
            if (spritesToProcess.isEmpty()) {
                val report = LocalizationReport(targetLanguage, 0, 0, 0, emptyList(), startTime, System.currentTimeMillis())
                withContext(Dispatchers.Main) { onComplete?.invoke(report) }
                return@launch
            }

            val total = spritesToProcess.size
            val results = mutableListOf<SpriteLocalizationResult>()
            var processed = 0
            var failed = 0

            for ((sprite, lookFile) in spritesToProcess) {
                val status = "extracting text from ${sprite.name}"
                withContext(Dispatchers.Main) {
                    onProgress?.invoke(processed + 1, total, sprite.name, status)
                }

                val result = processSprite(sprite, lookFile, total)
                results.add(result)
                if (result.success) processed++ else failed++

                withContext(Dispatchers.Main) {
                    onProgress?.invoke(processed + failed, total, sprite.name,
                        if (result.success) "done" else "failed: ${result.errorMessage}")
                }
            }

            val report = LocalizationReport(
                targetLanguage, total, processed, failed, results,
                startTime, System.currentTimeMillis(),
                avgOcrConfidence = if (ocrConfidences.isNotEmpty()) ocrConfidences.average().toFloat() else 0f,
                avgTextExpansion = if (textExpansions.isNotEmpty()) textExpansions.average().toFloat() else 0f,
                geminiRequestCount = geminiCalls,
                spritesWithText = total
            )
            withContext(Dispatchers.Main) { onComplete?.invoke(report) }
        }
    }

    private suspend fun processSprite(
        sprite: Sprite,
        lookFile: File,
        totalSprites: Int
    ): SpriteLocalizationResult = withContext(Dispatchers.IO) {
        try {
            val regions = SpriteTextExtractor.extractTextRegions(context, lookFile)
            for (r in regions) ocrConfidences.add(r.confidence)
            if (regions.isEmpty()) {
                return@withContext SpriteLocalizationResult(
                    sprite.name, lookFile.name, emptyList(),
                    success = true, outputPath = null
                )
            }

            val originalTexts = regions.map { it.originalText }
            val translationResult = GeminiTranslator.translateBatch(
                context, originalTexts, targetLanguage, sourceLanguage
            )
            geminiCalls++

            if (!translationResult.success) {
                return@withContext SpriteLocalizationResult(
                    sprite.name, lookFile.name, regions,
                    success = false, errorMessage = translationResult.errorMessage
                )
            }

            val translatedRegions = regions.mapIndexed { i, region ->
                val translated = translationResult.translatedTexts.getOrElse(i) { region.originalText }
                val expansion = (translated.length.toFloat() / maxOf(1, region.originalText.length)) - 1f
                textExpansions.add(expansion)
                region.copy(translatedText = translated)
            }

            val bitmap = BitmapFactory.decodeFile(lookFile.absolutePath)
                ?: return@withContext SpriteLocalizationResult(
                    sprite.name, lookFile.name, regions,
                    success = false, errorMessage = "Failed to decode bitmap"
                )

            var resultBitmap = bitmap
            for (region in translatedRegions) {
                resultBitmap = TextRenderer.replaceText(resultBitmap, region,
                    quality = quality, isPixelArt = isPixelArt)
            }

            val langCode = targetLanguage.take(2).lowercase()
            // Пишем локализованную картинку прямо в images-каталог сцены (не в скрытую
            // подпапку): XStream резолвит файлы образов как new File(imageDir, fileName)
            // только из корня images.
            val imagesDir = lookFile.parentFile
            val outputName = lookFile.nameWithoutExtension + "_" + langCode + "_" +
                java.util.UUID.randomUUID().toString().substring(0, 8) + "." + lookFile.extension
            val outputFile = File(imagesDir, outputName)

            FileOutputStream(outputFile).use { out ->
                resultBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }

            // Регистрируем локализованную картинку как НОВЫЙ образ спрайта, чтобы результат
            // реально появился в проекте (раньше он писался в скрытую папку и терялся).
            val originalName = sprite.lookList.firstOrNull { it.file == lookFile }?.name
                ?: lookFile.nameWithoutExtension
            withContext(Dispatchers.Main) {
                try {
                    val localizedLook = LookData("$originalName ($langCode)", outputFile)
                    sprite.lookList.add(localizedLook)
                    try { localizedLook.collisionInformation.calculate() } catch (_: Exception) {}
                } catch (e: Exception) {
                    android.util.Log.e("SpriteLocalizer", "Failed to register localized look", e)
                }
            }

            val outputDir = File(lookFile.parent, "localized")
            outputDir.mkdirs()

            val metaFile = File(outputDir,
                lookFile.nameWithoutExtension + "_$langCode.meta.json")
            val meta = org.json.JSONObject().apply {
                put("language", targetLanguage)
                put("source", lookFile.name)
                put("output", outputFile.name)
                put("generatedAt", System.currentTimeMillis())
                val textsArr = org.json.JSONArray()
                for (r in translatedRegions) {
                    textsArr.put(org.json.JSONObject().apply {
                        put("original", r.originalText)
                        put("translated", r.translatedText)
                        put("x", r.boundingBox.left)
                        put("y", r.boundingBox.top)
                        put("w", r.boundingBox.width())
                        put("h", r.boundingBox.height())
                        put("fontSize", r.estimatedFontSize.toDouble())
                        put("rotation", r.rotationAngle.toDouble())
                        put("hasOutline", r.outlineColor != 0)
                    })
                }
                put("translatedTexts", textsArr)
            }
            metaFile.writeText(meta.toString(2))

            if (resultBitmap !== bitmap) resultBitmap.recycle()
            bitmap.recycle()

            SpriteLocalizationResult(
                sprite.name, lookFile.name, translatedRegions,
                success = true, outputPath = outputFile.absolutePath
            )
        } catch (e: Exception) {
            SpriteLocalizationResult(
                sprite.name, lookFile.name, emptyList(),
                success = false, errorMessage = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    private fun collectSpritesWithText(project: Project): List<Pair<Sprite, File>> {
        val result = mutableListOf<Pair<Sprite, File>>()
        val sceneCount = project.sceneList.size
        for (si in 0 until sceneCount) {
            val scene = project.sceneList[si]
            val spriteList = scene.getSpriteList()
            for (sprite in spriteList) {
                val looks = sprite.getLookList()
                for (lookData in looks) {
                    val lookFile = lookData.getFile()
                    if (lookFile.exists() && lookFile.isFile && isImageFile(lookFile)) {
                        result.add(Pair(sprite, lookFile))
                        break
                    }
                }
            }
        }
        return result
    }

    private fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in setOf("png", "jpg", "jpeg", "webp", "bmp")
    }

    fun cancel() {
        scope.cancel()
    }

    companion object {
        fun regenerateFromMeta(
            context: Context,
            projectDir: File,
            quality: QualityMode = QualityMode.STANDARD,
            isPixelArt: Boolean = false
        ): Int {
            var regenerated = 0
            val localizedDir = File(projectDir, "localized")
            if (!localizedDir.exists()) return 0

            val metaFiles = localizedDir.listFiles { f ->
                f.name.endsWith(".meta.json")
            } ?: return 0

            for (metaFile in metaFiles) {
                try {
                    val meta = org.json.JSONObject(metaFile.readText())
                    val sourceName = meta.optString("source", "")
                    val outputName = meta.optString("output", "")
                    val sourceFile = File(projectDir, sourceName)
                    val outputFile = File(localizedDir, outputName)

                    if (!sourceFile.exists() || outputName.isEmpty()) continue

                    val textsArr = meta.optJSONArray("translatedTexts") ?: continue
                    val bitmap = android.graphics.BitmapFactory.decodeFile(sourceFile.absolutePath)
                        ?: continue

                    var resultBitmap = bitmap
                    for (i in 0 until textsArr.length()) {
                        val t = textsArr.getJSONObject(i)
                        val box = android.graphics.Rect(
                            t.optInt("x"), t.optInt("y"),
                            t.optInt("x") + t.optInt("w"),
                            t.optInt("y") + t.optInt("h")
                        )
                        val region = TextRegion(
                            originalText = t.optString("original"),
                            translatedText = t.optString("translated"),
                            boundingBox = box,
                            textColor = android.graphics.Color.BLACK,
                            backgroundColor = android.graphics.Color.WHITE,
                            estimatedFontSize = t.optDouble("fontSize", 24.0).toFloat(),
                            rotationAngle = t.optDouble("rotation", 0.0).toFloat(),
                            outlineColor = if (t.optBoolean("hasOutline", false))
                                android.graphics.Color.BLACK else 0,
                            outlineWidth = if (t.optBoolean("hasOutline", false)) 2f else 0f
                        )
                        resultBitmap = TextRenderer.replaceText(
                            resultBitmap, region,
                            quality = quality, isPixelArt = isPixelArt
                        )
                    }

                    java.io.FileOutputStream(outputFile).use { out ->
                        resultBitmap.compress(
                            android.graphics.Bitmap.CompressFormat.PNG, 100, out
                        )
                    }

                    if (resultBitmap !== bitmap) resultBitmap.recycle()
                    bitmap.recycle()
                    regenerated++
                } catch (_: Exception) { }
            }
            return regenerated
        }
    }
}
