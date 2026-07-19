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
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private var markwon: Markwon? = null

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

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].role) {
            ChatMessage.Role.USER -> TYPE_USER
            ChatMessage.Role.ASSISTANT -> TYPE_ASSISTANT
            ChatMessage.Role.SYSTEM, ChatMessage.Role.TOOL -> TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = when (viewType) {
            TYPE_USER -> R.layout.ai_item_chat_message_user
            TYPE_SYSTEM -> R.layout.ai_item_chat_message_system
            else -> R.layout.ai_item_chat_message_assistant
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        if (markwon == null) {
            markwon = Markwon.create(parent.context)
        }
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position], markwon)
    }

    override fun getItemCount(): Int = messages.size

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

    companion object {
        const val TYPE_USER = 0
        const val TYPE_ASSISTANT = 1
        const val TYPE_SYSTEM = 2
    }
}
