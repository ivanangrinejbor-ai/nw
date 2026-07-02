package org.catrobat.catroid.apkbuild

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.io.ProjectCrypto
import org.catrobat.catroid.io.ZipArchiver
import org.catrobat.catroid.stage.StageActivity
import java.io.File

/**
 * Загрузчик для собранных (baked) APK-проектов.
 * Показывает экран загрузки: расшифровка → распаковка → запуск StageActivity.
 */
class RuntimeLoaderActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val CACHE_DIR_NAME = "runtime_project"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runtime_loader)

        progressBar = findViewById(R.id.runtime_progress_bar)
        statusText = findViewById(R.id.runtime_status_text)

        setStatus("Загрузка...")
        progressBar.max = 100
        progressBar.progress = 0

        loadAndRunProject()
    }

    private fun setStatus(text: String) {
        handler.post { statusText.text = text }
    }

    private fun updateProgress(value: Int) {
        handler.post { progressBar.progress = value }
    }

    private fun loadAndRunProject() {
        Thread {
            try {
                // Шаг 1: Извлекаем шифрованный проект из assets
                setStatus("Расшифровка проекта...")
                updateProgress(10)

                val cacheDir = File(cacheDir, CACHE_DIR_NAME)
                if (cacheDir.exists()) cacheDir.deleteRecursively()
                cacheDir.mkdirs()

                val encryptedFile = File(cacheDir, "encrypted.dat")
                val decryptedZip = File(cacheDir, "project.zip")

                try {
                    assets.open(ProtectedProjectPayload.ENCRYPTED_ASSET_NAME).use { input ->
                        encryptedFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    // Возможно проект уже распакован
                    setStatus("Загрузка проекта...")
                    updateProgress(50)
                    startProject(cacheDir)
                    return@Thread
                }

                // Шаг 2: Расшифровываем
                setStatus("Расшифровка...")
                updateProgress(30)

                if (!ProjectCrypto.decrypt(encryptedFile, decryptedZip, ProtectedProjectPayload.PASSWORD)) {
                    setStatus("Ошибка: не удалось расшифровать проект")
                    return@Thread
                }
                encryptedFile.delete()

                // Шаг 3: Распаковываем
                setStatus("Распаковка проекта...")
                updateProgress(50)

                ZipArchiver().unzip(decryptedZip, cacheDir)
                decryptedZip.delete()

                // Шаг 4: Создаем проект из baked-данных
                setStatus("Инициализация проекта...")
                updateProgress(70)

                startProject(cacheDir)

            } catch (e: Exception) {
                setStatus("Ошибка: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    private fun startProject(projectDir: File) {
        setStatus("Запуск проекта...")
        updateProgress(90)

        // Проверяем, есть ли init.bin (baked LunoScript)
        val initFile = File(projectDir, "init.bin")
        val initLunoFile = File(projectDir, "init.luno.txt")

        if (initFile.exists() || initLunoFile.exists()) {
            // Запускаем StageActivity с baked-проектом
            val intent = Intent(this, StageActivity::class.java)
                .putExtra(StageActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
                .putExtra("IS_BAKED_LAUNCH", true)
            startActivityForResult(intent, StageActivity.REQUEST_START_STAGE)
        } else {
            // Пытаемся загрузить через ProjectManager
            try {
                val project = org.catrobat.catroid.io.asynctask.ProjectLoader(projectDir, null).execute().get()
                if (project != null) {
                    ProjectManager.getInstance().currentProject = project
                    StageActivity.handlePlayButton(ProjectManager.getInstance(), this)
                } else {
                    setStatus("Ошибка: не удалось загрузить проект")
                }
            } catch (e: Exception) {
                setStatus("Ошибка: ${e.message}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == StageActivity.REQUEST_START_STAGE) {
            finish()
        }
    }
}