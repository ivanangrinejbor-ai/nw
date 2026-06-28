package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Action
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity

class PhotoAction() : Action() {
    var scope: Scope? = null
    var toast: Formula? = null

    private var hasStarted = false
    private var finished = false

    override fun act(delta: Float): Boolean {
        if (finished) return true
        if (hasStarted) return false
        hasStarted = true

        StageActivity.getActiveCameraManager().takePicture2 { success, file ->
            if (success && file != null) {
                Gdx.app.postRunnable {
                    Log.d("PhotoAction", "Running on libGDX thread...")
                    val look = LookData(file.name, file)
                    val loadedPixmap = look.pixmap
                    if (loadedPixmap != null && loadedPixmap.width > 1) {
                        setLook(look)
                        Log.d("PhotoAction", "Look successfully prepared and set!")
                    } else {
                        Log.e("PhotoAction", "Failed to load Pixmap from file.")
                    }
                    finished = true
                }
            } else {
                finished = true
            }
        }

        return false
    }

    override fun restart() {
        hasStarted = false
        finished = false
        super.restart()
    }

    private fun setLook(lookData: LookData) {
        val sprite = scope?.sprite ?: return
        val look = sprite.look ?: return
        look.lookData = lookData
        sprite.lookList.add(lookData)
    }
}
