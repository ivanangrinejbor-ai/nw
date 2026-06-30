/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui.settingsfragments

import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.BuildConfig
import org.catrobat.catroid.R
import org.catrobat.catroid.codeanalysis.AiConfig
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment.AI_SENSORS_SCREEN_KEY

class AISettingsFragment : PreferenceFragment() {
    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).supportActionBar?.title = preferenceScreen.title
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        SettingsFragment.setToChosenLanguage(activity)
        addPreferencesFromResource(R.xml.ai_preferences)

        if (!BuildConfig.FEATURE_AI_SENSORS_ENABLED) {
            val aiSensorsPreference =
                findPreference(AI_SENSORS_SCREEN_KEY) as PreferenceScreen
            aiSensorsPreference.isEnabled = false
            preferenceScreen.removePreference(aiSensorsPreference)
        }

        val tokenPref = findPreference("ai_max_tokens") as? EditTextPreference
        tokenPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val v = (newValue as? String)?.toIntOrNull() ?: return@OnPreferenceChangeListener true
            val clamped = v.coerceIn(500, AiConfig.maxLimit)
            AiConfig.setMaxTokens(clamped)
            if (v != clamped) {
                tokenPref?.text = "$clamped"
                Toast.makeText(activity, "Clamped to $clamped (valid range: 500..${AiConfig.maxLimit})", Toast.LENGTH_SHORT).show()
            }
            if (clamped > 25000) {
                Toast.makeText(activity, AiConfig.warningMessage(), Toast.LENGTH_LONG).show()
            }
            true
        }
    }

    companion object {
        val TAG = AISettingsFragment::class.java.simpleName
    }
}
