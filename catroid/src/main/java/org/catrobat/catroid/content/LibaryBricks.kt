import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.bricks.FormulaBrick
import java.io.File

class LibaryBricks {
    companion object {
        private val TAG = "LibaryBricks"
        private val actions: MutableList<TemporalAction> = mutableListOf()
        private val bricks: MutableList<FormulaBrick> = mutableListOf()

        fun addAction(action: TemporalAction) {
            actions.add(action)
        }

        fun addBrick(brick: FormulaBrick) {
            bricks.add(brick)
        }

        fun getActions(): List<TemporalAction> {
            return actions
        }

        fun getBricks(): List<FormulaBrick> {
            return bricks
        }

        fun loadActionsFromFile(filePath: String) {
            val file = File(filePath)
            if (!file.exists()) {
                Log.w(TAG, "Actions file not found: $filePath")
                return
            }
            Log.i(TAG, "Loading actions from $filePath (deserialization not implemented)")
        }

        fun loadBricksFromFile(filePath: String) {
            val file = File(filePath)
            if (!file.exists()) {
                Log.w(TAG, "Bricks file not found: $filePath")
                return
            }
            Log.i(TAG, "Loading bricks from $filePath (deserialization not implemented)")
        }
    }
}
