package org.catrobat.catroid.content.actions

import android.content.Intent
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class LaunchProjectAction : TemporalAction() {
    var scope: Scope? = null
    var projectNameFormula: Formula? = null

    override fun update(percent: Float) {
        val stage = StageActivity.activeStageActivity?.get()
        val currentProject = ProjectManager.getInstance().currentProject
        if (stage == null || currentProject == null) return

        val projectFileName = projectNameFormula?.interpretString(scope)
        if (projectFileName.isNullOrEmpty()) {
            Log.e("LaunchProjectAction", "Project file name is empty.")
            return
        }

        val isZipFile = projectFileName.endsWith(".zip", true) || projectFileName.endsWith(".neotrobat", true)
                || projectFileName.endsWith(".newtrobat", true) || projectFileName.endsWith(".catrobat", true)

        val projectToLaunchDir: File?

        if (isZipFile) {
            val sourceZipFile = currentProject.getFile(projectFileName)
            if (sourceZipFile == null || !sourceZipFile.exists()) {
                Log.e("LaunchProjectAction", "Project archive not found in project files: $projectFileName")
                return
            }

            val cacheDir = File(stage.cacheDir, "sub_projects")
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                Log.e("LaunchProjectAction", "Failed to create cache directory: ${cacheDir.absolutePath}")
                return
            }
            val projectName = projectFileName.substringBeforeLast('.')
            val unpackedProjectDir = File(cacheDir, projectName)

            if (!unpackedProjectDir.exists()) {
                Log.i("LaunchProjectAction", "Unpacking '$projectFileName' to cache...")
                try {
                    unzip(sourceZipFile, unpackedProjectDir)
                } catch (e: IOException) {
                    Log.e("LaunchProjectAction", "Failed to unzip project.", e)
                    return
                }
            } else {
                Log.i("LaunchProjectAction", "Project '$projectName' already in cache. Skipping unpack.")
            }
            projectToLaunchDir = unpackedProjectDir

        } else {
            val dir = currentProject.getFile(projectFileName)
            if (dir == null || !dir.exists() || !dir.isDirectory) {
                Log.e("LaunchProjectAction", "Project directory not found: $projectFileName")
                return
            }
            projectToLaunchDir = dir
        }

        if (!projectToLaunchDir.isDirectory) {
            Log.e("LaunchProjectAction", "Project path is not a directory: ${projectToLaunchDir.absolutePath}")
            return
        }

        ProjectManager.pushProjectHistory(currentProject.directory.absolutePath)

        val intent = Intent(stage, StageActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(StageActivity.EXTRA_PROJECT_PATH, projectToLaunchDir.absolutePath)
        }

        stage.startActivity(intent)
        stage.finish()
    }

    @Throws(IOException::class)
    private fun unzip(zipFile: File, targetDirectory: File) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val canonicalTarget = targetDirectory.canonicalPath
                val canonicalDest = File(canonicalTarget, entry.name).canonicalPath
                if (!canonicalDest.startsWith(canonicalTarget + File.separator)) {
                    throw IOException("Zip slip attack detected: ${entry.name}")
                }
                val newFile = File(targetDirectory, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    File(newFile.parent).mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}