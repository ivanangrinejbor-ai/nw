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

    private var started = false

    override fun update(percent: Float) {
        if (started) return
        val stage = StageActivity.activeStageActivity?.get()
        val currentProject = ProjectManager.getInstance().currentProject
        if (stage == null || currentProject == null) return

        val projectFileName = projectNameFormula?.interpretString(scope)
        if (projectFileName.isNullOrEmpty()) {
            Log.e("LaunchProjectAction", "Project file name is empty.")
            return
        }
        started = true

        val isZipFile = projectFileName.endsWith(".zip", true) || projectFileName.endsWith(".neotrobat", true)
                || projectFileName.endsWith(".newtrobat", true) || projectFileName.endsWith(".catrobat", true)
                || projectFileName.endsWith(".npc", true) || projectFileName.endsWith(".ncp", true)

        val projectToLaunchDir: File?

        if (isZipFile) {
            val sourceZipFile = currentProject.getFile(projectFileName)
            if (sourceZipFile == null || !sourceZipFile.exists()) {
                Log.e("LaunchProjectAction", "Project archive not found in project files: $projectFileName")
                started = false
                return
            }

            val cacheDir = File(stage.cacheDir, "sub_projects")
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                Log.e("LaunchProjectAction", "Failed to create cache directory: ${cacheDir.absolutePath}")
                started = false
                return
            }
            val projectName = projectFileName.substringBeforeLast('.')
            val unpackedProjectDir = File(cacheDir, projectName)
            val tmpProjectDir = File(cacheDir, "${projectName}_tmp")

            if (!unpackedProjectDir.exists()) {
                Log.i("LaunchProjectAction", "Unpacking '$projectFileName' to cache...")
                // ProgressDialog and Thread must be started from the UI thread
                stage.runOnUiThread {
                    val progressDialog = android.app.ProgressDialog(stage).apply {
                        setMessage("Unpacking sub-project...")
                        setCancelable(false)
                        show()
                    }
                    Thread {
                        try {
                            if (tmpProjectDir.exists()) {
                                tmpProjectDir.deleteRecursively()
                            }
                            unzip(sourceZipFile, tmpProjectDir)
                            if (unpackedProjectDir.exists()) {
                                unpackedProjectDir.deleteRecursively()
                            }
                            if (tmpProjectDir.renameTo(unpackedProjectDir)) {
                                stage.runOnUiThread {
                                    progressDialog.dismiss()
                                    launchProjectIntent(stage, currentProject.directory.absolutePath, unpackedProjectDir)
                                }
                            } else {
                                throw IOException("Failed to rename temporary directory to destination")
                            }
                        } catch (e: Exception) {
                            Log.e("LaunchProjectAction", "Failed to unzip project.", e)
                            tmpProjectDir.deleteRecursively()
                            stage.runOnUiThread {
                                progressDialog.dismiss()
                                android.widget.Toast.makeText(stage, "Failed to load sub-project: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                started = false
                            }
                        }
                    }.start()
                }
            } else {
                launchProjectIntent(stage, currentProject.directory.absolutePath, unpackedProjectDir)
            }

        } else {
            val dir = currentProject.getFile(projectFileName)
            if (dir == null || !dir.exists() || !dir.isDirectory) {
                Log.e("LaunchProjectAction", "Project directory not found: $projectFileName")
                started = false
                return
            }
            launchProjectIntent(stage, currentProject.directory.absolutePath, dir)
        }
    }

    private fun launchProjectIntent(activity: StageActivity, currentProjectPath: String, projectToLaunchDir: File) {
        ProjectManager.pushProjectHistory(currentProjectPath)

        val intent = Intent(activity, StageActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(StageActivity.EXTRA_PROJECT_PATH, projectToLaunchDir.absolutePath)
        }

        activity.startActivity(intent)
        activity.finish()
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