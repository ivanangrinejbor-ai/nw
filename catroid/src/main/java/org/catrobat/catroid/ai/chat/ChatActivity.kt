package org.catrobat.catroid.ai.chat

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var thinkingContainer: FrameLayout
    private lateinit var thinkingText: TextView
    private lateinit var adapter: ChatAdapter
    private var thinkingAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_chat)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.ai_agent_chat_title)

        recyclerView = findViewById(R.id.chat_recycler)
        inputField = findViewById(R.id.chat_input)
        sendButton = findViewById(R.id.btn_send)
        thinkingContainer = findViewById(R.id.chat_thinking_container)
        thinkingText = findViewById(R.id.chat_thinking_text)

        val agent = AiAgentManager.instance

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
                if (messages.isNotEmpty() && recyclerView.isAttachedToWindow) {
                    recyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        lifecycleScope.launch {
            agent.state.collectLatest { state ->
                val isThinking = state != AiAgentState.IDLE && state != AiAgentState.ERROR
                thinkingContainer.visibility = if (isThinking) View.VISIBLE else View.GONE
                sendButton.isEnabled = !isThinking
                if (isThinking) startThinkingAnimation() else stopThinkingAnimation()
            }
        }
    }

    private fun startThinkingAnimation() {
        if (thinkingAnimator?.isRunning == true) return
        val blue = ContextCompat.getColor(this, R.color.progress_blue)
        val white = Color.WHITE
        thinkingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val alpha = (127 + (128 * Math.sin(fraction * 2 * Math.PI)).toInt()).coerceIn(0, 255)
                val r = (Color.red(blue) * (1 - fraction) + Color.red(white) * fraction).toInt()
                val g = (Color.green(blue) * (1 - fraction) + Color.green(white) * fraction).toInt()
                val b = (Color.blue(blue) * (1 - fraction) + Color.blue(white) * fraction).toInt()
                thinkingText.setTextColor(Color.argb(alpha, r, g, b))
            }
            start()
        }
    }

    private fun stopThinkingAnimation() {
        thinkingAnimator?.cancel()
        thinkingAnimator = null
        thinkingText.setTextColor(ContextCompat.getColor(this, R.color.progress_blue))
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
                    Toast.makeText(this@ChatActivity,
                        getString(R.string.ai_agent_model_selected, selected),
                        Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onDestroy() {
        stopThinkingAnimation()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            ModelManager.unloadModel()
        }
    }
}
