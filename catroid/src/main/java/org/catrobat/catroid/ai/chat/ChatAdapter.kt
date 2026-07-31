package org.catrobat.catroid.ai.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import org.catrobat.catroid.R

class ChatAdapter(
    private var messages: List<ChatMessage>,
    private val onRegenerate: ((Int) -> Unit)?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var markwon: Markwon? = null

    private var thinkingActive = false
    private var thinkingDetail = ""
    private var thinkingExpanded = false

    fun updateMessages(newMessages: List<ChatMessage>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = messages.size
            override fun getNewListSize(): Int = newMessages.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return messages[oldPos].timestamp == newMessages[newPos].timestamp
            }
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return messages[oldPos] == newMessages[newPos]
            }
        })
        messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }

    fun setThinking(active: Boolean, detail: String) {
        val wasActive = thinkingActive
        thinkingDetail = detail
        when {
            active && !wasActive -> {
                thinkingActive = true
                notifyItemInserted(messages.size)
            }
            !active && wasActive -> {
                thinkingActive = false
                thinkingExpanded = false
                notifyItemRemoved(messages.size)
            }
            active && wasActive -> {
                notifyItemChanged(messages.size)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (thinkingActive && position == messages.size) return TYPE_THINKING
        return when (messages[position].role) {
            ChatMessage.Role.USER -> TYPE_USER
            ChatMessage.Role.ASSISTANT -> TYPE_ASSISTANT
            ChatMessage.Role.CHANGE -> TYPE_CHANGE
            ChatMessage.Role.SYSTEM, ChatMessage.Role.TOOL -> TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (markwon == null) {
            markwon = Markwon.create(parent.context)
        }
        return when (viewType) {
            TYPE_THINKING ->
                ThinkingViewHolder(inflater.inflate(R.layout.ai_item_chat_message_thinking, parent, false))
            TYPE_CHANGE ->
                ChangeViewHolder(inflater.inflate(R.layout.ai_item_change_card, parent, false))
            TYPE_USER ->
                MessageViewHolder(inflater.inflate(R.layout.ai_item_chat_message_user, parent, false))
            TYPE_SYSTEM ->
                MessageViewHolder(inflater.inflate(R.layout.ai_item_chat_message_system, parent, false))
            else ->
                MessageViewHolder(inflater.inflate(R.layout.ai_item_chat_message_assistant, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ThinkingViewHolder -> holder.bind(thinkingDetail, thinkingExpanded)
            is ChangeViewHolder -> holder.bind(messages[position])
            is MessageViewHolder -> holder.bind(messages[position], markwon)
        }
    }

    override fun getItemCount(): Int = messages.size + if (thinkingActive) 1 else 0

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentText: TextView = itemView.findViewById(R.id.message_content)
        private val copyButton: View? = itemView.findViewById(R.id.btn_copy)
        private val regenerateButton: View? = itemView.findViewById(R.id.btn_regenerate)

        fun bind(msg: ChatMessage, markwon: Markwon?) {
            if (markwon != null) {
                markwon.setMarkdown(contentText, msg.content)
            } else {
                contentText.text = msg.content
            }

            copyButton?.setOnClickListener {
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("AI Message", msg.content))
                Toast.makeText(itemView.context, R.string.ai_agent_copied, Toast.LENGTH_SHORT).show()
            }

            regenerateButton?.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onRegenerate?.invoke(pos)
                }
            }

            copyButton?.visibility = if (msg.role == ChatMessage.Role.ASSISTANT) View.VISIBLE else View.GONE
            regenerateButton?.visibility = if (msg.role == ChatMessage.Role.ASSISTANT) View.VISIBLE else View.GONE
        }
    }

    inner class ChangeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label: TextView = itemView.findViewById(R.id.change_label)
        private val location: TextView = itemView.findViewById(R.id.change_location)
        private val added: TextView = itemView.findViewById(R.id.change_added)
        private val removed: TextView = itemView.findViewById(R.id.change_removed)

        fun bind(msg: ChatMessage) {
            val card = msg.changeCard
            label.text = card?.label ?: msg.content
            val loc = when {
                card?.objectName != null && card.sceneName != null -> "${card.objectName} · ${card.sceneName}"
                card?.objectName != null -> card.objectName
                card?.sceneName != null -> card.sceneName
                else -> null
            }
            if (loc != null) {
                location.visibility = View.VISIBLE
                location.text = loc
            } else {
                location.visibility = View.GONE
            }
            val addedCount = card?.added ?: 0
            val removedCount = card?.removed ?: 0
            if (addedCount > 0) {
                added.visibility = View.VISIBLE
                added.text = "+$addedCount"
            } else {
                added.visibility = View.GONE
            }
            if (removedCount > 0) {
                removed.visibility = View.VISIBLE
                removed.text = "-$removedCount"
            } else {
                removed.visibility = View.GONE
            }
        }
    }

    inner class ThinkingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val root: View = itemView.findViewById(R.id.thinking_root)
        private val label: ShimmerTextView = itemView.findViewById(R.id.thinking_label)
        private val detail: TextView = itemView.findViewById(R.id.thinking_detail)

        fun bind(detailText: String, expanded: Boolean) {
            label.startShimmer()
            detail.text = detailText
            detail.visibility = if (expanded && detailText.isNotBlank()) View.VISIBLE else View.GONE
            root.setOnClickListener {
                thinkingExpanded = !thinkingExpanded
                notifyItemChanged(messages.size)
            }
        }
    }

    companion object {
        const val TYPE_USER = 0
        const val TYPE_ASSISTANT = 1
        const val TYPE_SYSTEM = 2
        const val TYPE_THINKING = 3
        const val TYPE_CHANGE = 4
    }
}
