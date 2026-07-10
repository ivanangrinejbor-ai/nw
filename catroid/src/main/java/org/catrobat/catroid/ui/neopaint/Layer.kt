package org.catrobat.catroid.ui.neopaint

import android.graphics.Bitmap

data class PaintLayer(
    var bitmap: Bitmap,
    var name: String,
    var visible: Boolean = true,
    var opacity: Float = 1f
)
