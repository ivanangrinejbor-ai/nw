package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.Action
import kotlinx.coroutines.*
import org.catrobat.catroid.apkbuild.ApkToolboxManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula

class GenerateKeyAction : Action() {
    var scope: Scope? = null

    var filenameFormula: Formula? = null
    var passwordFormula: Formula? = null
    var aliasFormula: Formula? = null
    var commonNameFormula: Formula? = null

    private var executed = false

    override fun act(delta: Float): Boolean {
        if (!executed) {
            executed = true
            GlobalScope.launch(Dispatchers.IO) {
                val filename = filenameFormula?.interpretString(scope) ?: "release.jks"
                val password = passwordFormula?.interpretString(scope) ?: "123456"
                val alias = aliasFormula?.interpretString(scope) ?: "key0"
                val commonName = commonNameFormula?.interpretString(scope) ?: "User"

                val file = scope?.project?.getFile(filename)

                if (file != null) {
                    ApkToolboxManager.generateKeyStore(
                        outputPath = file.absolutePath,
                        alias = alias,
                        pass = password,
                        commonName = commonName
                    )
                }
            }
        }
        return true
    }
}
