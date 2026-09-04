package org.catrobat.catroid.collab

import android.content.Context
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.FirebaseAuthManager

object CollabAuth {
    private const val PREFS = "collab_prefs"
    private const val KEY_NAME = "display_name"

    fun ensureSignedIn(callback: (String?) -> Unit) {
        try {
            if (FirebaseAuthManager.isSignedIn()) {
                val uid = FirebaseAuthManager.getUserId()
                if (uid.isNotEmpty()) {
                    callback(uid)
                    return
                }
            }
            FirebaseAuthManager.signInAnonymously {
                if (it) {
                    val uid = FirebaseAuthManager.getUserId()
                    callback(uid.ifEmpty { null })
                } else {
                    callback(null)
                }
            }
        } catch (e: Exception) {
            callback(null)
        }
    }

    fun savedDisplayName(): String {
        return try {
            CatroidApplication.getAppContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_NAME, "").orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    fun saveDisplayName(name: String) {
        try {
            CatroidApplication.getAppContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_NAME, name).apply()
        } catch (e: Exception) {
        }
    }
}
