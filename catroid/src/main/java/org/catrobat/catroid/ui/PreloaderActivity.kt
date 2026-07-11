package org.catrobat.catroid.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.widget.ProgressBar
import android.widget.TextView
import org.catrobat.catroid.R
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageWorkspaceActivity
import kotlin.random.Random

class PreloaderActivity : Activity() {

    private var progressBar: ProgressBar? = null
    private var percentText: TextView? = null
    private var factText: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentProgress = 0
    private var factIndex = 0
    private var isFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If recreated after process death, just finish — stage already running
        if (savedInstanceState != null) {
            finish()
            return
        }

        setContentView(R.layout.activity_preloader)

        progressBar = findViewById(R.id.preloader_progress)
        percentText = findViewById(R.id.preloader_percent)
        factText = findViewById(R.id.preloader_fact)

        // Load facts from resources
        val facts = resources.getStringArray(R.array.loading_facts)
        if (facts.isEmpty()) {
            factText?.text = getString(R.string.preloader_loading)
        } else {
            factIndex = Random.nextInt(facts.size)
            scheduleFact(facts)
        }

        // Start progress simulation
        handler.post(progressRunnable)
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (isFinished || isFinishing || isDestroyed) return
            currentProgress += 1
            if (currentProgress > 100) {
                currentProgress = 100
                finishLoading()
                return
            }
            progressBar?.progress = currentProgress
            percentText?.text = "${currentProgress}%"
            handler.postDelayed(this, 30L) // ~3 seconds total
        }
    }

    private fun scheduleFact(facts: Array<String>) {
        if (isFinished || isFinishing || isDestroyed) return
        factText?.text = facts[factIndex % facts.size]
        factIndex++
        handler.postDelayed({ scheduleFact(facts) }, 7000L)
    }

    private fun finishLoading() {
        if (isFinished || isFinishing || isDestroyed) return
        isFinished = true
        handler.removeCallbacksAndMessages(null)

        // Check free stage preference (same logic as StageActivity.startStageActivity)
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isFreeStageEnabled = prefs.getBoolean("pref_workspace_stage", false)

        val targetClass = if (isFreeStageEnabled) {
            StageWorkspaceActivity::class.java
        } else {
            StageActivity::class.java
        }
        val intent = Intent(this, targetClass)
        startActivityForResult(intent, REQUEST_START_STAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_START_STAGE) {
            setResult(resultCode, data)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val REQUEST_START_STAGE = 1001
    }
}
