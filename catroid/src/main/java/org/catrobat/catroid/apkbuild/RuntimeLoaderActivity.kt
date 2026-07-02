package org.catrobat.catroid.apkbuild

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import org.catrobat.catroid.R
import org.catrobat.catroid.stage.StageActivity

class RuntimeLoaderActivity : Activity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var progress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runtime_loader)

        progressBar = findViewById(R.id.progress_bar)
        statusText = findViewById(R.id.status_text)

        simulateLoading()
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

    private fun startProject() {
        val intent = Intent(this, StageActivity::class.java)
        intent.putExtra("EXTRA_PROJECT_PATH", intent.getStringExtra("EXTRA_PROJECT_PATH"))
        intent.putExtra("EXTRA_PROJECT_NAME", intent.getStringExtra("EXTRA_PROJECT_NAME"))
        startActivity(intent)
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