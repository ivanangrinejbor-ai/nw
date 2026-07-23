package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class CompileJavaToDexAction : TemporalAction() {
    var scope: Scope? = null
    var sourcePath: Formula? = null
    var outputPath: Formula? = null

    override fun update(percent: Float) {
        if (scope == null) return
        val src = sourcePath?.interpretString(scope) ?: return
        val out = outputPath?.interpretString(scope) ?: return

        try {
            Class.forName("com.android.dx.command.dexer.Main")
            Log.w("CompileJavaToDex", "dx dexer found, but compilation not implemented; would compile $src -> $out")
        } catch (e: ClassNotFoundException) {
            Log.e("CompileJavaToDex", "com.android.dx.command.dexer.Main not available on this platform")
        }
    }
}
