package org.catrobat.catroid.ui.settingsfragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.R

class TelemetrySettingsFragment : PreferenceFragment() {

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = preferenceScreen.title
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        SettingsFragment.setToChosenLanguage(activity)
        addPreferencesFromResource(R.xml.telemetry_preferences)
        showInstallUuid()
        setupPrivacyPolicyButton()
    }

    private fun showInstallUuid() {
        val prefs = activity?.getSharedPreferences("telemetry_prefs", Context.MODE_PRIVATE)
        val uuid = prefs?.getString("telemetry_id", null)
        if (!uuid.isNullOrEmpty()) {
            findPreference("telemetry_uuid")?.summary = uuid
        }
    }

    private fun setupPrivacyPolicyButton() {
        findPreference("telemetry_privacy_policy")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                } catch (e: Exception) {
                    Log.w(TAG, "open privacy policy failed", e)
                }
                true
            }
    }

    companion object {
        const val TAG = "TelemetrySettingsFragment"
        private const val PRIVACY_POLICY_URL = "https://neocatroid.oikkpip.workers.dev/#privacy"
    }
}
