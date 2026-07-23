package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import dalvik.system.DexClassLoader
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File

class LoadAndRunDexAction : TemporalAction() {
    var scope: Scope? = null
    var dexPath: Formula? = null

    override fun update(percent: Float) {
        if (scope == null) return
        val path = dexPath?.interpretString(scope) ?: return
        if (path.isEmpty()) return

        try {
            val dexFile = File(path)
            if (!dexFile.exists()) {
                Log.e("LoadAndRunDex", "DEX file not found: $path")
                return
            }

            val parentDir = dexFile.parent ?: "/data/data/tmp"
            val optimizedDir = File(parentDir, "optimized")
            optimizedDir.mkdirs()

            val loader = DexClassLoader(
                dexFile.absolutePath,
                optimizedDir.absolutePath,
                null,
                this::class.java.classLoader
            )

            val mainClass = loader.loadClass("Main")
            val mainMethod = mainClass.getMethod("main", Array<String>::class.java)
            mainMethod.invoke(null, emptyArray<String>())

            Log.d("LoadAndRunDex", "Executed Main class from $path")
        } catch (e: ClassNotFoundException) {
            Log.e("LoadAndRunDex", "Main class not found in $path", e)
        } catch (e: Exception) {
            Log.e("LoadAndRunDex", "Failed to load/run DEX: ${e.message}", e)
        }
    }
}
