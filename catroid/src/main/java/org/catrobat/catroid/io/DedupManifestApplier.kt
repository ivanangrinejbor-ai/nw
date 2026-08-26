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
package org.catrobat.catroid.io

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DedupManifestApplier {

    private const val TAG = "DedupManifest"
    const val MANIFEST_NAME = "dedup_manifest.json"

    fun apply(projectDir: File): Int {
        val manifestFile = File(projectDir, MANIFEST_NAME)
        if (!manifestFile.exists()) {
            return 0
        }
        return try {
            val root = JSONObject(manifestFile.readText())
            val pairs = root.optJSONArray("deduplicated") ?: JSONArray()
            val canonicalRoot = projectDir.canonicalPath
            var applied = 0
            for (i in 0 until pairs.length()) {
                val pair = pairs.optJSONArray(i) ?: continue
                val missingRelative = pair.optString(0)
                val existingRelative = pair.optString(1)
                if (missingRelative.isBlank() || existingRelative.isBlank()) {
                    continue
                }
                val target = File(projectDir, missingRelative)
                val source = File(projectDir, existingRelative)
                if (!target.canonicalPath.startsWith(canonicalRoot + File.separator) ||
                    !source.canonicalPath.startsWith(canonicalRoot + File.separator)) {
                    Log.w(TAG, "Skipping suspicious manifest pair: $missingRelative -> $existingRelative")
                    continue
                }
                if (target.exists() || !source.exists()) {
                    continue
                }
                target.parentFile?.mkdirs()
                source.copyTo(target, overwrite = false)
                applied++
            }
            manifestFile.delete()
            Log.i(TAG, "Restored $applied deduplicated file(s)")
            applied
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply dedup manifest", e)
            0
        }
    }
}
