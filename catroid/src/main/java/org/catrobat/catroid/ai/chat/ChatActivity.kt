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
import org.catrobat.catroid.ai.model.ModelManager
import org.catrobat.catroid.ai.settings.AiPreferences

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var modelButton: TextView
    private lateinit var adapter: ChatAdapter

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
        val providers = org.catrobat.catroid.ai.model.AiProvider.values()
        val providerNames = providers.map { it.displayName }.toTypedArray()
        val currentProvider = CloudModelRuntime.getActiveProvider()
        val checkedIndex = providers.indexOf(currentProvider).let { if (it >= 0) it else 0 }

        AlertDialog.Builder(this)
            .setTitle("Выберите Провайдера ИИ")
            .setSingleChoiceItems(providerNames, checkedIndex) { dialog, which ->
                val selectedProvider = providers[which]
                AiPreferences.setProvider(selectedProvider.id)
                dialog.dismiss()
                promptApiKeyForProvider(selectedProvider)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun promptApiKeyForProvider(provider: org.catrobat.catroid.ai.model.AiProvider) {
        val currentKey = AiPreferences.getApiKeyForProvider(provider.id) ?: ""
        val input = EditText(this).apply {
            hint = "Введите API Key для ${provider.displayName}..."
            setText(currentKey)
        }
        AlertDialog.Builder(this)
            .setTitle("API Key для ${provider.displayName}")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    AiPreferences.setApiKeyForProvider(provider.id, key)
                    Toast.makeText(this, "API Key сохранен для ${provider.displayName}!", Toast.LENGTH_SHORT).show()
                    showModelSelectorForProvider(provider, key)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showModelSelector() {
        val provider = CloudModelRuntime.getActiveProvider()
        val apiKey = CloudModelRuntime.getApiKey()
        if (apiKey.isNullOrBlank()) {
            promptApiKeyForProvider(provider)
            return
        }
        showModelSelectorForProvider(provider, apiKey)
    }

    private fun showModelSelectorForProvider(provider: org.catrobat.catroid.ai.model.AiProvider, apiKey: String) {
        val loading = Toast.makeText(this, "Запрос списка моделей из API ключа ${provider.displayName}...", Toast.LENGTH_SHORT)
        loading.show()
        lifecycleScope.launch {
            val fetchedModels = org.catrobat.catroid.ai.model.CloudModelProvider.fetchModelsForProvider(provider, apiKey).toMutableList()
            loading.cancel()
            if (isFinishing || isDestroyed) return@launch

            val optionCustom = "Ввести имя модели вручную..."
            val displayList = ArrayList(fetchedModels)
            displayList.add(0, optionCustom)

            val current = AiPreferences.getCloudModelId()
            val checked = displayList.indexOf(current).let { if (it >= 0) it else 1 }

            AlertDialog.Builder(this@ChatActivity)
                .setTitle("Модели из API-ключа (${provider.displayName})")
                .setSingleChoiceItems(displayList.toTypedArray(), checked) { dialog, which ->
                    val selected = displayList[which]
                    if (selected == optionCustom) {
                        dialog.dismiss()
                        promptCustomModel(provider)
                    } else {
                        AiPreferences.setProvider(provider.id)
                        AiPreferences.setCloudModelId(selected)
                        AiPreferences.setBackend(AiPreferences.BACKEND_CLOUD)
                        ModelManager.unloadModel()
                        updateModelButtonLabel()
                        Toast.makeText(this@ChatActivity, "Выбрана модель: $selected", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
                .setNeutralButton("Сменить Провайдер") { _, _ ->
                    showApiKeyDialog()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun promptCustomModel(provider: org.catrobat.catroid.ai.model.AiProvider) {
        val input = EditText(this).apply {
            hint = "Имя модели (например, gpt-4o, deepseek-chat, claude-3-5-sonnet)..."
            setText(AiPreferences.getCloudModelId())
        }
        AlertDialog.Builder(this)
            .setTitle("Ручной ввод модели (${provider.displayName})")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val modelName = input.text.toString().trim()
                if (modelName.isNotEmpty()) {
                    AiPreferences.setProvider(provider.id)
                    AiPreferences.setCloudModelId(modelName)
                    AiPreferences.setBackend(AiPreferences.BACKEND_CLOUD)
                    ModelManager.unloadModel()
                    updateModelButtonLabel()
                    Toast.makeText(this, "Установлена кастомная модель: $modelName", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateModelButtonLabel() {
        val provider = CloudModelRuntime.getActiveProvider()
        val model = AiPreferences.getCloudModelId().substringAfterLast('/')
        modelButton.text = "[${provider.displayName}] $model"
    }

    override fun onResume() {
        super.onResume()
        updateModelButtonLabel()
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            ModelManager.unloadModel()
        }
    }

    companion object {
        const val EXTRA_SCOPE_PROJECT = "extra_scope_project"
    }
}
