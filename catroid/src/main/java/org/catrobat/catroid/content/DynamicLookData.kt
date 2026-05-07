package org.catrobat.catroid.content

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Polygon
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.sensing.CollisionInformation

class DynamicLookData(
    val bufferName: String,
    val region: TextureRegion,
    val bufferWidth: Int,
    val bufferHeight: Int
) : LookData() {

    private var dummyPixmap: Pixmap? = null
    private var customCollision: CollisionInformation? = null

    init {
        val safeW = if (bufferWidth <= 0) 1 else bufferWidth
        val safeH = if (bufferHeight <= 0) 1 else bufferHeight

        this.name = "Buffer_$bufferName"
        this.lookId = "buffer_${bufferName}_${System.currentTimeMillis()}"

        dummyPixmap = Pixmap(safeW, safeH, Pixmap.Format.RGBA8888)

        customCollision = CollisionInformation(this)
        val vertices = floatArrayOf(
            0f, 0f,
            safeW.toFloat(), 0f,
            safeW.toFloat(), safeH.toFloat(),
            0f, safeH.toFloat()
        )
        customCollision!!.collisionPolygons = arrayOf(Polygon(vertices))
    }

    override fun getPixmap(): Pixmap = dummyPixmap!!
    override fun getTextureRegion(): TextureRegion = region
    override fun getCollisionInformation(): CollisionInformation = customCollision!!

    override fun dispose() {
        super.dispose()
        dummyPixmap?.dispose()
    }
}