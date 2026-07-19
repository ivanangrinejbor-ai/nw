package org.catrobat.catroid.ai.model

data class ModelInfo(
    val id: String,
    val name: String,
    val provider: String,
    val size: ModelSize,
    val uri: String,
    val filename: String,
    val description: String,
    val isDownloaded: Boolean = false,
    val isLoaded: Boolean = false,
    val fileSizeBytes: Long = 0
)

enum class ModelSize(val label: String, val paramCount: String) {
    SIZE_0_5B("0.5B parameters", "0.5B"),
    SIZE_1B("1B parameters", "1B"),
    SIZE_2B("2B parameters", "2B"),
    SIZE_3B("3B parameters", "3B"),
    SIZE_4B("4B parameters", "4B"),
    SIZE_7B("7B parameters", "7B"),
    SIZE_8B("8B parameters", "8B"),
    SIZE_CUSTOM("Custom", "CUSTOM");

    companion object {
        fun fromString(s: String): ModelSize = entries.find { it.paramCount == s } ?: SIZE_1B
    }
}
