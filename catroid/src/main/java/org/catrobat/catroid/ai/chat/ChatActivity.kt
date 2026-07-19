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
import org.catrobat.catroid.ai.model.ModelManager

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
