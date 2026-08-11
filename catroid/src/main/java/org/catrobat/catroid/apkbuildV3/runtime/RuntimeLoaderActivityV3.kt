package org.catrobat.catroid.apkbuildV3.runtime

import android.app.Activity
import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import org.catrobat.catroid.R
import org.catrobat.catroid.apkbuildV3.TemplateType
import org.catrobat.catroid.stage.StageActivity
import java.io.File

class RuntimeLoaderActivityV3 : Activity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var factText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var templateType: TemplateType = TemplateType.LIGHT
    private var projectPath: String? = null

    companion object {
        private const val TAG = "RuntimeLoaderV3"
        private const val EXTRA_TEMPLATE_TYPE = "template_type"

        private val FUN_FACTS = arrayOf(
            "The first Android phone was released in 2008.",
            "Catrobat was inspired by Scratch from MIT.",
            "NeoCatroid supports over 390 different brick types.",
            "The first version of Android had no copy-paste support.",
            "Android is based on the Linux kernel.",
            "The Android robot logo was created by Irina Blok.",
            "There are over 2.5 billion active Android devices worldwide.",
            "Visual programming helps children learn logic and creativity."
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runtime_loader_v3)

        progressBar = findViewById(R.id.v3_progress_bar)
        statusText = findViewById(R.id.v3_status_text)
        factText = findViewById(R.id.v3_fact_text)

        templateType = detectTemplateType(assets)
        statusText.text = getString(R.string.v3_loading)

        showRandomFact()

        Thread {
            try {
                val success = when (templateType) {
                    TemplateType.FULL -> loadFullTemplate()
                    TemplateType.LIGHT -> loadLightTemplate()
                }

                handler.post {
                    if (success) {
                        startProject()
                    } else {
                        statusText.text = getString(R.string.v3_load_failed)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Loading failed", e)
                handler.post {
                    statusText.text = "${getString(R.string.v3_load_failed)}: ${e.message}"
                }
            }
        }.start()
    }

    private fun detectTemplateType(assets: AssetManager): TemplateType {
        try {
            val files = assets.list("") ?: return TemplateType.LIGHT
            val hasFullMarker = files.any { it == "template_v3_full.marker" }
            return if (hasFullMarker) TemplateType.FULL else TemplateType.LIGHT
        } catch (e: Exception) {
            return TemplateType.LIGHT
        }
    }

    private fun loadFullTemplate(): Boolean {
        val cacheDir = File(cacheDir, "v3_project_full").apply {
            deleteRecursively()
            mkdirs()
        }

        val strategy = FullTemplateStrategy(this)
        val success = strategy.load(cacheDir) { progress ->
            updateProgress(progress, when {
                progress < 0.2f -> getString(R.string.v3_loading)
                progress < 0.4f -> getString(R.string.v3_decrypting)
                progress < 0.6f -> getString(R.string.v3_extracting)
                progress < 0.8f -> getString(R.string.v3_initializing)
                else -> getString(R.string.v3_starting)
            })
        }

        if (success) {
            projectPath = File(cacheDir, "project_extracted").absolutePath
        }
        return success
    }

    private fun loadLightTemplate(): Boolean {
        val cacheDir = File(cacheDir, "v3_project_light").apply {
            deleteRecursively()
            mkdirs()
        }

        val strategy = LightTemplateStrategy(this)
        val success = strategy.initialize(cacheDir) { progress ->
            updateProgress(progress, when {
                progress < 0.2f -> getString(R.string.v3_loading)
                progress < 0.4f -> getString(R.string.v3_decrypting_meta)
                progress < 0.6f -> getString(R.string.v3_preparing)
                progress < 0.8f -> getString(R.string.v3_initializing)
                else -> getString(R.string.v3_starting)
            })
        }

        if (success) {
            projectPath = File(cacheDir, "project_light").absolutePath
        }
        return success
    }

    private fun updateProgress(progress: Float, stage: String) {
        handler.post {
            progressBar.progress = (progress * 100).toInt()
            statusText.text = stage
                    if (Math.random() < 0.3) {
                showRandomFact()
            }
        }
    }

    private fun showRandomFact() {
        val fact = FUN_FACTS[java.util.concurrent.ThreadLocalRandom.current().nextInt(FUN_FACTS.size)]
        factText.text = getString(R.string.v3_fun_fact_prefix, fact)
    }

    private fun startProject() {
        val stageIntent = Intent(this, StageActivity::class.java).apply {
            putExtra(StageActivity.EXTRA_PROJECT_PATH, projectPath)
            putExtra("V3_TEMPLATE_TYPE", templateType.name)
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
        File(cacheDir, "v3_project_full").deleteRecursively()
        File(cacheDir, "v3_project_light").deleteRecursively()
    }
}
