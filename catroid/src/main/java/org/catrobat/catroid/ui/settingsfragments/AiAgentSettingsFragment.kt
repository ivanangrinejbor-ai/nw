package org.catrobat.catroid.ui.settingsfragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.R
import org.catrobat.catroid.ai.AiAgentManager
import org.catrobat.catroid.ai.chat.ChatActivity
import org.catrobat.catroid.ai.settings.AiPreferences

class AiAgentSettingsFragment : PreferenceFragment() {

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = preferenceScreen.title
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        SettingsFragment.setToChosenLanguage(activity)
        addPreferencesFromResource(R.xml.ai_agent_preferences)

        setupChatPreference()
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

    companion object {
        const val TAG = "AiAgentSettingsFragment"
    }
}
