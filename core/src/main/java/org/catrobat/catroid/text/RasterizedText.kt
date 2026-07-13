package org.catrobat.catroid.text

/** Portable rasterized text result: raw RGBA8888 pixels. */
data class RasterizedText(val width: Int, val height: Int, val rgba: ByteArray)
