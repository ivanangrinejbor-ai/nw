package org.catrobat.catroid.apkbuild

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import org.catrobat.catroid.R
import org.catrobat.catroid.io.ProjectCrypto
import org.catrobat.catroid.io.ZipArchiver
import org.catrobat.catroid.stage.StageActivity
import java.io.File

class RuntimeLoaderActivity : Activity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var progress = 0
    private var bakedProjectDir: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runtime_loader)

        progressBar = findViewById(R.id.progress_bar)
        statusText = findViewById(R.id.status_text)
        statusText.text = getString(R.string.loading)

        Thread {
            val projectDir = prepareBakedProject()
            handler.post {
                if (projectDir == null) {
                    statusText.text = "Не удалось загрузить проект"
                    return@post
                }
                bakedProjectDir = projectDir
                simulateLoading()
            }
        }.start()
    }

    private fun simulateLoading() {
        val statuses = listOf(
            "Загрузка...",
            "Расшифровка проекта...",
            "Распаковка...",
            "Инициализация проекта...",
            "Запуск проекта..."
        )

        val delays = listOf(500L, 1000L, 1500L, 2000L, 2500L)

        for (i in statuses.indices) {
            handler.postDelayed({
                progress = ((i + 1) * 100 / statuses.size)
                progressBar.progress = progress
                statusText.text = statuses[i]

                if (i == statuses.size - 1) {
                    startProject()
                }
            }, delays[i])
        }
    }

    private fun prepareBakedProject(): File? {
        return try {
            val encryptedName = ProtectedProjectPayload.ENCRYPTED_ASSET_NAME
            val cacheBase = File(cacheDir, "baked_project")
            if (cacheBase.exists()) {
                cacheBase.deleteRecursively()
            }
            cacheBase.mkdirs()

            val encryptedFile = File(cacheBase, encryptedName)
            val decryptedZip = File(cacheBase, "project.zip")

            assets.open(encryptedName).use { input ->
                encryptedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!ProjectCrypto.decrypt(encryptedFile, decryptedZip, ProtectedProjectPayload.PASSWORD)) {
                Log.e("RuntimeLoader", "Failed to decrypt baked project")
                return null
            }

            ZipArchiver().unzip(decryptedZip, cacheBase)
            decryptedZip.delete()
            encryptedFile.delete()
            cacheBase
        } catch (e: Exception) {
            Log.e("RuntimeLoader", "Cannot prepare baked project", e)
            null
        }
    }

    private fun startProject() {
        val projectPath = bakedProjectDir?.absolutePath
            ?: intent.getStringExtra(StageActivity.EXTRA_PROJECT_PATH)

        if (projectPath.isNullOrEmpty()) {
            statusText.text = "Проект не найден"
            return
        }

        val stageIntent = Intent(this, StageActivity::class.java).apply {
            putExtra(StageActivity.EXTRA_PROJECT_PATH, projectPath)
            putExtra("IS_BAKED_LAUNCH", true)
        }
        startActivity(stageIntent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}