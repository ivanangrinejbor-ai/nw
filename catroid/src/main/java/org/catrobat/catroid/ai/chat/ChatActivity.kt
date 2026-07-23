package org.catrobat.catroid.ai.chat

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.catrobat.catroid.R
import org.catrobat.catroid.ai.AiAgentManager
import org.catrobat.catroid.ai.AiAgentState
import org.catrobat.catroid.ai.model.CloudModelProvider
import org.catrobat.catroid.ai.model.CloudModelRuntime
import org.catrobat.catroid.ai.model.ModelDownloadService
import org.catrobat.catroid.ai.model.ModelInfo
import org.catrobat.catroid.ai.model.ModelManager
import org.catrobat.catroid.ai.settings.AiPreferences

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var modelButton: TextView
    private lateinit var adapter: ChatAdapter

    private var downloadDialog: AlertDialog? = null
    private var downloadObserverJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_chat)

        val agent = AiAgentManager.instance
        val scopeProject = intent.getStringExtra(EXTRA_SCOPE_PROJECT)
        agent.setScope(scopeProject)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.ai_agent_chat_title)
        supportActionBar?.subtitle = scopeProject ?: getString(R.string.ai_agent_scope_all)

        recyclerView = findViewById(R.id.chat_recycler)
        inputField = findViewById(R.id.chat_input)
        sendButton = findViewById(R.id.btn_send)
        modelButton = findViewById(R.id.btn_model)

        modelButton.setOnClickListener { showModelSelector() }
        updateModelButtonLabel()

        adapter = ChatAdapter(emptyList()) { position ->
            agent.messages.value.getOrNull(position)?.let { msg ->
                agent.sendMessage(msg.content)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        sendButton.setOnClickListener {
            val text = inputField.text.toString().trim()
            if (text.isNotEmpty()) {
                agent.sendMessage(text)
                inputField.setText("")
                inputField.clearFocus()
            }
        }

        lifecycleScope.launch {
            agent.messages.collectLatest { messages ->
                adapter.updateMessages(messages)
                scrollToBottom()
            }
        }

        lifecycleScope.launch {
            agent.state.collectLatest { state ->
                val isThinking = state != AiAgentState.IDLE && state != AiAgentState.ERROR
                sendButton.isEnabled = !isThinking
                adapter.setThinking(isThinking, agent.activity.value)
                if (isThinking) scrollToBottom()
            }
        }

        lifecycleScope.launch {
            agent.activity.collectLatest { detail ->
                val state = agent.state.value
                val isThinking = state != AiAgentState.IDLE && state != AiAgentState.ERROR
                adapter.setThinking(isThinking, detail)
                if (isThinking) scrollToBottom()
            }
        }
    }

    private fun scrollToBottom() {
        val count = adapter.itemCount
        if (count > 0 && recyclerView.isAttachedToWindow) {
            recyclerView.smoothScrollToPosition(count - 1)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
            R.id.action_set_api_key -> {
                showApiKeyDialog()
                return true
            }
            R.id.action_select_model -> {
                showModelSelector()
                return true
            }
            R.id.action_clear_chat -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.ai_agent_clear_history_title)
                    .setMessage(R.string.ai_agent_clear_history_message)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        AiAgentManager.instance.clearHistory()
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showApiKeyDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.ai_agent_set_api_key_hint)
            setText(CloudModelRuntime.getApiKey() ?: "")
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.ai_agent_set_api_key_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    CloudModelRuntime.setApiKey(key)
                    Toast.makeText(this, R.string.ai_agent_api_key_saved, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showModelSelector() {
        val apiKey = CloudModelRuntime.getApiKey()
        if (apiKey.isNullOrBlank()) {
            Toast.makeText(this, R.string.ai_agent_no_api_key, Toast.LENGTH_LONG).show()
            return
        }
        val loading = Toast.makeText(this, R.string.ai_agent_loading_models, Toast.LENGTH_SHORT)
        loading.show()
        lifecycleScope.launch {
            val models = CloudModelProvider.fetchModels(apiKey)
            loading.cancel()
            if (isFinishing || isDestroyed) return@launch
            val current = AiPreferences.getCloudModelId()
            val checked = models.indexOf(current).let { if (it >= 0) it else 0 }
            AlertDialog.Builder(this@ChatActivity)
                .setTitle(R.string.ai_agent_select_model_title)
                .setSingleChoiceItems(models.toTypedArray(), checked) { dialog, which ->
                    val selected = models[which]
                    AiPreferences.setCloudModelId(selected)
                    AiPreferences.setBackend(AiPreferences.BACKEND_CLOUD)
                    ModelManager.unloadModel()
                    updateModelButtonLabel()
                    Toast.makeText(this@ChatActivity,
                        getString(R.string.ai_agent_model_selected, selected),
                        Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.ai_agent_more_models) { _, _ -> showLocalModels() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun showLocalModels() {
        val models = ModelManager.availableModels.value
        val currentLocalId = AiPreferences.getSelectedModelId()
        val isLocal = AiPreferences.isLocalBackend()
        val labels = models.map { model ->
            val marker = when {
                isLocal && model.id == currentLocalId -> " ✓"
                model.isDownloaded -> " ⬇"
                else -> ""
            }
            model.name + marker
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.ai_agent_local_models_title)
            .setItems(labels) { _, which ->
                val model = models.getOrNull(which) ?: return@setItems
                if (model.isDownloaded) {
                    showDownloadedModelActions(model)
                } else {
                    confirmDownload(model)
                }
            }
            .setNegativeButton(R.string.ai_agent_back, null)
            .show()
    }

    private fun confirmDownload(model: ModelInfo) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.ai_agent_download_confirm, model.name))
            .setPositiveButton(R.string.ai_agent_download) { _, _ -> startDownload(model) }
            .setNegativeButton(R.string.ai_agent_back, null)
            .show()
    }

    private fun showDownloadedModelActions(model: ModelInfo) {
        AlertDialog.Builder(this)
            .setTitle(model.name)
            .setPositiveButton(R.string.ai_agent_use_model) { _, _ -> useLocalModel(model) }
            .setNeutralButton(R.string.ai_agent_delete_model) { _, _ ->
                lifecycleScope.launch {
                    ModelManager.deleteModel(model.id)
                    if (AiPreferences.getSelectedModelId() == null) {
                        AiPreferences.setBackend(AiPreferences.BACKEND_CLOUD)
                    }
                    updateModelButtonLabel()
                }
            }
            .setNegativeButton(R.string.ai_agent_back, null)
            .show()
    }

    private fun useLocalModel(model: ModelInfo) {
        lifecycleScope.launch {
            val loaded = ModelManager.loadModel(model.id)
            if (loaded) {
                AiPreferences.setBackend(AiPreferences.BACKEND_LOCAL)
                updateModelButtonLabel()
                Toast.makeText(this@ChatActivity,
                    getString(R.string.ai_agent_local_ready, model.name),
                    Toast.LENGTH_SHORT).show()
            } else {
                val reason = ModelManager.lastLoadError
                    ?: getString(R.string.ai_agent_download_failed, model.name)
                AlertDialog.Builder(this@ChatActivity)
                    .setTitle(model.name)
                    .setMessage(reason)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    private fun startDownload(model: ModelInfo) {
        maybeRequestNotificationPermission()
        val intent = Intent(this, ModelDownloadService::class.java).apply {
            putExtra(ModelDownloadService.EXTRA_MODEL_ID, model.id)
            putExtra(ModelDownloadService.EXTRA_URL, model.uri)
            putExtra(ModelDownloadService.EXTRA_FILENAME, model.filename)
            putExtra(ModelDownloadService.EXTRA_NAME, model.name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        showDownloadDialog(model)
    }

    private fun showDownloadDialog(model: ModelInfo) {
        if (downloadDialog?.isShowing == true) return
        val view = layoutInflater.inflate(R.layout.ai_dialog_download_progress, null)
        val nameView = view.findViewById<TextView>(R.id.download_model_name)
        val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.download_progress_bar)
        val percentView = view.findViewById<TextView>(R.id.download_percent)
        val detailView = view.findViewById<TextView>(R.id.download_detail)
        nameView.text = getString(R.string.ai_agent_downloading, model.name)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        downloadDialog = dialog

        downloadObserverJob?.cancel()
        downloadObserverJob = lifecycleScope.launch {
            ModelManager.detailedProgress.collectLatest { state ->
                if (state != null) {
                    val pct = if (state.progress >= 0) state.progress else 0
                    progressBar.isIndeterminate = state.progress < 0
                    progressBar.progress = pct
                    percentView.text = "$pct%"
                    detailView.text = buildString {
                        append(formatBytes(state.downloadedBytes))
                        if (state.totalBytes > 0) {
                            append(" / ")
                            append(formatBytes(state.totalBytes))
                        }
                        append("  ·  ")
                        append(ModelManager.downloadSpeed.value ?: "")
                    }
                } else if (!ModelManager.isDownloadRunning()) {
                    // Download finished.
                    dialog.dismiss()
                    downloadDialog = null
                    val refreshed = ModelManager.getModelById(model.id)
                    if (refreshed?.isDownloaded == true) {
                        showDownloadedModelActions(refreshed)
                    } else {
                        Toast.makeText(this@ChatActivity,
                            getString(R.string.ai_agent_download_failed, model.name),
                            Toast.LENGTH_LONG).show()
                    }
                    return@collectLatest
                }
            }
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
        else -> "$bytes B"
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }
    }

    private fun updateModelButtonLabel() {
        modelButton.text = if (AiPreferences.isLocalBackend()) {
            ModelManager.getCurrentModel()?.name
                ?: AiPreferences.getCloudModelId().substringAfterLast('/')
        } else {
            AiPreferences.getCloudModelId().substringAfterLast('/')
        }
    }

    override fun onResume() {
        super.onResume()
        updateModelButtonLabel()
        if (ModelManager.isDownloadRunning()) {
            val activeId = ModelManager.detailedProgress.value?.modelId
            val model = activeId?.let { ModelManager.getModelById(it) }
                ?: ModelManager.availableModels.value.firstOrNull { !it.isDownloaded }
            if (model != null) {
                showDownloadDialog(model)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            ModelManager.unloadModel()
        }
    }

    companion object {
        /** Optional String extra: when set, the agent is limited to this single project. */
        const val EXTRA_SCOPE_PROJECT = "extra_scope_project"
    }
}
