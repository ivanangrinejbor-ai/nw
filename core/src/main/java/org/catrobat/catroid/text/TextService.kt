package org.catrobat.catroid.text

/**
 * Platform-independent text rasterization used by ShowTextActor.
 *
 * Turns a string + style into raw RGBA8888 pixels so the stage actor can build
 * a libGDX [com.badlogic.gdx.graphics.Texture] without touching any platform
 * font API. `typefaceName` is a portable font identifier (e.g. a file path on
 * Android); the active implementation resolves it to a platform font.
 */
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
