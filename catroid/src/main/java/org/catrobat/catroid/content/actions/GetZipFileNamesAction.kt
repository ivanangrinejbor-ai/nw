package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InterpretationException
import org.catrobat.catroid.formulaeditor.UserVariable
import java.util.zip.ZipFile
import java.io.IOException

class GetZipFileNamesAction : TemporalAction() {
    var scope: Scope? = null
    var zipFileName: Formula? = null
    var userVariable: UserVariable? = null

    override fun update(percent: Float) {
        val project = scope?.project
        val variable = userVariable
        if (project == null || variable == null) return

        val fileName: String
        try {
            fileName = zipFileName?.interpretString(scope) ?: ""
        } catch (e: InterpretationException) {
            variable.value = "Error: Invalid formula"
            return
        }

        if (fileName.isEmpty()) {
            variable.value = "Error: ZIP file name is empty"
            return
        }

        try {
            val zipFile = project.getFile(fileName)
            if (!zipFile.exists()) {
                variable.value = "Error: File not found"
                return
            }

            val zf = ZipFile(zipFile)
            try {
                val fileNames = zf.entries().asSequence()
                    .map { it.name }
                    .joinToString(",")
                variable.value = fileNames
            } finally {
                zf.close()
            }
        } catch (e: IOException) {
            variable.value = "Error: Failed to read ZIP file"
        } catch (e: SecurityException) {
            variable.value = "Error: Permission denied"
        }
    }
}