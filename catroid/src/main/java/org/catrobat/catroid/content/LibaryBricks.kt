import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.bricks.FormulaBrick
import org.catrobat.catroid.io.XstreamSerializer
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
            val xstream = XstreamSerializer.getInstance().getXstream()
            @Suppress("UNCHECKED_CAST")
            val loaded = xstream.fromXML(file) as List<TemporalAction>
            actions.clear()
            actions.addAll(loaded)
            Log.i(TAG, "Loaded ${loaded.size} actions from $filePath")
        }

        fun loadBricksFromFile(filePath: String) {
            val file = File(filePath)
            if (!file.exists()) {
                Log.w(TAG, "Bricks file not found: $filePath")
                return
            }
            val xstream = XstreamSerializer.getInstance().getXstream()
            @Suppress("UNCHECKED_CAST")
            val loaded = xstream.fromXML(file) as List<FormulaBrick>
            bricks.clear()
            bricks.addAll(loaded)
            Log.i(TAG, "Loaded ${loaded.size} bricks from $filePath")
        }
    }
}
