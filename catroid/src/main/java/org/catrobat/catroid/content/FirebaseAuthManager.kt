/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

package org.catrobat.catroid.content

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import org.catrobat.catroid.CatroidApplication

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"

    private val isInitialized by lazy {
        try {
            if (FirebaseApp.getApps(CatroidApplication.getAppContext()).isEmpty()) {
                FirebaseApp.initializeApp(CatroidApplication.getAppContext())
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Firebase init failed", e)
            false
        }
    }

    private val auth: FirebaseAuth? by lazy {
        if (!isInitialized) return@lazy null
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Auth init failed", e)
            null
        }
    }

    fun signInAnonymously(callback: (Boolean) -> Unit = {}) {
        val a = auth
        if (a == null) {
            callback(false)
            return
        }
        a.signInAnonymously()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error signing in anonymously: ${error.message}")
                callback(false)
            }
    }

    fun signInWithEmail(email: String, password: String, callback: (Boolean) -> Unit = {}) {
        val a = auth
        if (a == null || email.isBlank() || password.isBlank()) {
            callback(false)
            return
        }
        a.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error signing in with email: ${error.message}")
                callback(false)
            }
    }

    fun signOut() {
        auth?.signOut()
    }

    fun getUserId(): String {
        return auth?.currentUser?.uid ?: ""
    }

    fun isSignedIn(): Boolean {
        return auth?.currentUser != null
    }
}