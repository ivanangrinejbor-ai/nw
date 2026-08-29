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

package org.catrobat.catroid.content.actions

import android.content.Intent
import android.net.Uri
import android.util.Log
import org.catrobat.catroid.stage.StageActivity

private val ALLOWED_SCHEMES = setOf("http", "https", "tg", "intent", "market", "mailto", "tel", "geo")

class OpenUrlAction : WebAction() {
    var response: String? = null

    private fun openUrl() {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase()
        if (scheme == null || scheme !in ALLOWED_SCHEMES) {
            Log.w(javaClass.simpleName, "Blocked URL with disallowed scheme: $scheme")
            return
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            StageActivity.activeStageActivity.get()?.startActivity(browserIntent)
        } catch (e: Exception) {
            Log.w(javaClass.simpleName, "Failed to open URL: $url", e)
            try {
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                StageActivity.activeStageActivity.get()?.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }

    override fun act(delta: Float): Boolean {
        if (url == null) {
            try {
                val raw = formula?.interpretString(scope) ?: return true
                url = if (raw.contains("://")) raw else "https://$raw"
                val nl = url!!.indexOf("\n")
                if (nl != -1) url = url!!.substring(0, nl)
            } catch (e: Exception) {
                Log.w(javaClass.simpleName, "Failed to interpret url", e)
                return true
            }
        }
        openUrl()
        return true
    }

    override fun handleResponse() {
        openUrl()
    }

    override fun handleError(error: String) {
    }
}
