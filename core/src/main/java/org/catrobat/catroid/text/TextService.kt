package org.catrobat.catroid.text

interface TextService {
    fun rasterizeText(
        text: String,
        textSizePx: Float,
        color: String?,
        typefaceName: String?,
        isWrapped: Boolean,
        alignment: Int
    ): RasterizedText
}
