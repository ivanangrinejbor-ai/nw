package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import kotlinx.coroutines.*
import org.catrobat.catroid.apkbuild.ApkToolboxManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import android.util.Log

class SignApkAction : Action() {
    var scope: Scope? = null

    var inputApkFormula: Formula? = null
    var outputApkFormula: Formula? = null
    var keystoreFormula: Formula? = null
    var passwordFormula: Formula? = null
    var aliasFormula: Formula? = null

    private var executed = false

    override fun act(delta: Float): Boolean {
        if (!executed) {
            executed = true
            GlobalScope.launch(Dispatchers.IO) {
                val inputFile = inputApkFormula?.interpretString(scope) ?: "game-unsigned.apk"
                val outputFile = outputApkFormula?.interpretString(scope) ?: "game-signed.apk"

                var keystoreFile = keystoreFormula?.interpretString(scope) ?: ""
                var pass = passwordFormula?.interpretString(scope) ?: ""
                var alias = aliasFormula?.interpretString(scope) ?: ""

                val context = org.catrobat.catroid.CatroidApplication.getAppContext()

                val inputF = scope?.project?.getFile(inputFile)
                val outputF = scope?.project?.getFile(outputFile)

                val keyF = if (keystoreFile.isNotEmpty()) scope?.project?.getFile(keystoreFile) else null

                if (inputF != null && outputF != null) {
                    ApkToolboxManager.signApk(
                        context = context,
                        inputApkPath = inputF.absolutePath,
                        outputApkPath = outputF.absolutePath,
                        keyStorePath = keyF?.absolutePath,
                        keyAlias = if (alias.isNotEmpty()) alias else null,
                        keyPass = if (pass.isNotEmpty()) pass else null
                    )
                } else {
                    Log.e("SignApkAction", "Input or Output file path is invalid")
                }
            }
        }
        return true
    }
}
