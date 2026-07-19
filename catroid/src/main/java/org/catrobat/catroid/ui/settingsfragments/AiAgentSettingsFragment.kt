package org.catrobat.catroid.ui.settingsfragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.provider.OpenableColumns
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.R
import org.catrobat.catroid.ai.AiAgentManager
import org.catrobat.catroid.ai.chat.ChatActivity
import org.catrobat.catroid.ai.model.ModelInfo
import org.catrobat.catroid.ai.model.ModelManager
import org.catrobat.catroid.ai.settings.AiPreferences
import java.io.File
import java.io.FileOutputStream

class AiAgentSettingsFragment : PreferenceFragment() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var downloadJob: Job? = null
    private var downloadProgressDialog: Dialog? = null

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = preferenceScreen.title
        refreshModelSummary()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        SettingsFragment.setToChosenLanguage(activity)
        addPreferencesFromResource(R.xml.ai_agent_preferences)

        setupChatPreference()
        setupModelList()
        setupDownloadPreference()
        setupDeletePreference()
        setupLoadCustomModel()
        setupClearHistory()
        setupClearMemory()
        setupToolHistory()
        setupTemperature()
        setupMaxContext()
        setupMaxToolCalls()
    }

    private fun setupChatPreference() {
        val chatPref = findPreference("ai_agent_chat")
        if (AiAgentManager.instance.isEnabled()) {
            chatPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                startActivity(Intent(activity, ChatActivity::class.java))
                true
            }
        } else {
            chatPref?.summary = getString(R.string.ai_agent_enable_first)
            chatPref?.isEnabled = false
        }

        val enablePref = findPreference("ai_agent_enabled")
        enablePref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            AiAgentManager.instance.setEnabled(enabled)
            chatPref?.isEnabled = enabled
            if (enabled) {
                chatPref?.summary = getString(R.string.ai_agent_chat_summary)
                chatPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    startActivity(Intent(activity, ChatActivity::class.java))
                    true
                }
            } else {
                chatPref?.summary = getString(R.string.ai_agent_enable_first)
                chatPref?.onPreferenceClickListener = null
            }
            true
        }
    }

    private fun setupModelList() {
        val modelPref = findPreference("ai_agent_model_id") as? ListPreference
        modelPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val modelId = newValue as? String ?: ""
            if (modelId.isEmpty()) {
                ModelManager.unloadModel()
                Toast.makeText(activity, R.string.ai_agent_model_unloaded, Toast.LENGTH_SHORT).show()
            } else {
                scope.launch {
                    Toast.makeText(activity, R.string.ai_agent_model_loading, Toast.LENGTH_SHORT).show()
                    val success = ModelManager.loadModel(modelId)
                    if (success) {
                        Toast.makeText(activity, getString(R.string.ai_agent_model_loaded, modelId), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(activity, getString(R.string.ai_agent_download_failed, modelId), Toast.LENGTH_LONG).show()
                    }
                }
            }
            true
        }
    }

    private fun setupDownloadPreference() {
        val downloadPref = findPreference("ai_agent_download_model")
        downloadPref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showDownloadDialog()
            true
        }
    }

    private fun setupDeletePreference() {
        val deletePref = findPreference("ai_agent_delete_model")
        deletePref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            showDeleteDialog()
            true
        }
    }

    private fun setupClearHistory() {
        findPreference("ai_agent_clear_history")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                AiAgentManager.instance.clearHistory()
                Toast.makeText(activity, R.string.ai_agent_history_cleared, Toast.LENGTH_SHORT).show()
                true
            }
    }

    private fun setupClearMemory() {
        findPreference("ai_agent_clear_memory")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                AlertDialog.Builder(activity)
                    .setTitle(R.string.ai_agent_clear_memory_title)
                    .setMessage(R.string.ai_agent_clear_memory_confirm)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        AiAgentManager.instance.clearMemory()
                        Toast.makeText(activity, R.string.ai_agent_memory_cleared, Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
                true
            }
    }

    private fun setupToolHistory() {
        findPreference("ai_agent_view_tool_history")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                val history = AiAgentManager.instance.toolEngine.toolHistory.value
                if (history.isEmpty()) {
                    Toast.makeText(activity, R.string.ai_agent_no_tool_history, Toast.LENGTH_SHORT).show()
                } else {
                    val content = history.takeLast(20).joinToString("\n\n") { entry ->
                        "Tool: ${entry.toolCall.name}\nArgs: ${entry.toolCall.args}\nResult: ${entry.result.data.take(200)}"
                    }
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.ai_agent_tool_history_title)
                        .setMessage(content)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                true
            }
    }

    private fun setupTemperature() {
        val pref = findPreference("ai_agent_temperature") as? EditTextPreference
        pref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val v = (newValue as? String)?.toFloatOrNull() ?: return@OnPreferenceChangeListener true
            val clamped = v.coerceIn(0.0f, 2.0f)
            AiPreferences.setTemperature(clamped)
            if (v != clamped) {
                pref.text = "%.1f".format(clamped)
                Toast.makeText(activity, "Temperature clamped to %.1f (0.0..2.0)".format(clamped), Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun setupMaxContext() {
        val pref = findPreference("ai_agent_max_context") as? EditTextPreference
        pref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val v = (newValue as? String)?.toIntOrNull() ?: return@OnPreferenceChangeListener true
            val clamped = v.coerceIn(512, 32000)
            AiPreferences.setMaxContext(clamped)
            if (v != clamped) {
                pref.text = "$clamped"
                Toast.makeText(activity, "Clamped to $clamped (512..32000)", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun setupMaxToolCalls() {
        val pref = findPreference("ai_agent_max_tool_calls") as? EditTextPreference
        pref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val v = (newValue as? String)?.toIntOrNull() ?: return@OnPreferenceChangeListener true
            val clamped = v.coerceIn(1, 50)
            AiPreferences.setMaxToolCalls(clamped)
            if (v != clamped) {
                pref.text = "$clamped"
                Toast.makeText(activity, "Clamped to $clamped (1..50)", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun refreshModelSummary() {
        val current = ModelManager.getCurrentModel()
        val summaryPref = findPreference("ai_agent_model_id")
        summaryPref?.summary = current?.let { "${it.name} (downloaded)" } ?: getString(R.string.ai_model_none)
    }

    private fun showDownloadDialog() {
        val models = ModelManager.availableModels.value.filter { !it.isDownloaded }
        if (models.isEmpty()) {
            Toast.makeText(activity, R.string.ai_agent_all_downloaded, Toast.LENGTH_SHORT).show()
            return
        }
        val names = models.map { "${it.name} - ${it.description}" }.toTypedArray()
        val modelIds = models.map { it.id }.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle(R.string.ai_agent_download_dialog_title)
            .setItems(names) { _, which ->
                val modelId = modelIds[which]
                showDownloadProgressDialog(modelId)
                scope.launch {
                    val success = ModelManager.downloadModel(modelId)
                    downloadProgressDialog?.dismiss()
                    downloadProgressDialog = null
                    if (success) {
                        Toast.makeText(activity, getString(R.string.ai_agent_download_complete, modelId), Toast.LENGTH_SHORT).show()
                        refreshModelSummary()
                    } else {
                        Toast.makeText(activity, getString(R.string.ai_agent_download_failed, modelId), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDownloadProgressDialog(modelId: String) {
        val builder = AlertDialog.Builder(activity)
        val view = layoutInflater.inflate(R.layout.dialog_download_progress, null)
        val progressBar = view.findViewById<ProgressBar>(R.id.download_progress_bar)
        val speedText = view.findViewById<TextView>(R.id.download_speed_text)
        val progressText = view.findViewById<TextView>(R.id.download_progress_text)
        builder.setTitle(getString(R.string.ai_agent_download_title))
        builder.setView(view)
        builder.setCancelable(true)
        downloadProgressDialog = builder.create()
        downloadProgressDialog?.show()

        scope.launch {
            var hasStarted = false
            ModelManager.detailedProgress.collect { state ->
                if (state != null && state.modelId == modelId) {
                    hasStarted = true
                    progressBar.progress = state.progress.coerceIn(0, 100)
                    val downloadedMb = state.downloadedBytes / 1_000_000.0
                    val totalMb = if (state.totalBytes > 0) state.totalBytes / 1_000_000.0 else 0.0
                    val pct = state.progress
                    progressText.text = if (totalMb > 0) {
                        "%.0f / %.0f MB (%d%%)".format(downloadedMb, totalMb, pct)
                    } else {
                        "%.0f MB".format(downloadedMb)
                    }
                    speedText.text = ModelManager.downloadSpeed.value ?: ""
                } else if (state == null && hasStarted) {
                    progressText.text = getString(R.string.ai_agent_download_complete_text)
                    speedText.text = ""
                }
            }
        }
    }

    private fun showDeleteDialog() {
        val models = ModelManager.availableModels.value.filter { it.isDownloaded }
        if (models.isEmpty()) {
            Toast.makeText(activity, R.string.ai_agent_no_models_downloaded, Toast.LENGTH_SHORT).show()
            return
        }
        val names = models.map { it.name }.toTypedArray()
        val modelIds = models.map { it.id }.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle(R.string.ai_agent_delete_title)
            .setItems(names) { _, which ->
                val modelId = modelIds[which]
                scope.launch {
                    ModelManager.deleteModel(modelId)
                    Toast.makeText(activity, getString(R.string.ai_agent_model_unloaded), Toast.LENGTH_SHORT).show()
                    refreshModelSummary()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupLoadCustomModel() {
        val pref = findPreference("ai_agent_load_custom_model")
        pref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, REQUEST_CUSTOM_GGUF)
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CUSTOM_GGUF && resultCode == AppCompatActivity.RESULT_OK) {
            val uri = data?.data ?: return
            scope.launch {
                try {
                    val tempFile = withContext(Dispatchers.IO) {
                        val resolver = activity?.contentResolver ?: return@withContext null
                        val inputStream = resolver.openInputStream(uri) ?: return@withContext null
                        val cursor = resolver.query(uri, null, null, null, null)
                        val displayName = cursor?.use {
                            if (it.moveToFirst()) it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME)) else "custom.gguf"
                        } ?: "custom.gguf"
                        val tempFile = File(activity?.cacheDir, displayName)
                        FileOutputStream(tempFile).use { output ->
                            inputStream.copyTo(output)
                        }
                        inputStream.close()
                        tempFile
                    } ?: return@launch
                    val success = ModelManager.loadCustomModelFile(tempFile)
                    if (success) {
                        Toast.makeText(activity, getString(R.string.ai_agent_model_loaded, tempFile.name), Toast.LENGTH_SHORT).show()
                        refreshModelSummary()
                    } else {
                        Toast.makeText(activity, R.string.ai_agent_load_custom_failed, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(activity, "Failed to load custom model: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val TAG = "AiAgentSettingsFragment"
        private const val REQUEST_CUSTOM_GGUF = 9001
    }
}
