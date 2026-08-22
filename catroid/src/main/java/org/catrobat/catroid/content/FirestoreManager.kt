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

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import org.catrobat.catroid.CatroidApplication
import org.json.JSONArray
import org.json.JSONObject

object FirestoreManager {
    private const val TAG = "FirestoreManager"

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

    private val db: FirebaseFirestore? by lazy {
        if (!isInitialized) return@lazy null
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Firestore init failed", e)
            null
        }
    }

    private fun hasInternet(): Boolean {
        return try {
            val cm = CatroidApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resolves the Firestore instance for the given base id.
     * Empty/blank base id -> default FirebaseApp (google-services.json).
     * Non-empty base id -> existing FirebaseApp with that name, or a new
     * FirebaseApp initialized with the default options under that name.
     */
    private fun getFirestore(baseId: String): FirebaseFirestore? {
        if (!isInitialized) return null
        if (baseId.isBlank()) return db
        return try {
            val apps = FirebaseApp.getApps(CatroidApplication.getAppContext())
            val existing = apps.firstOrNull { it.name == baseId }
            if (existing != null) {
                FirebaseFirestore.getInstance(existing)
            } else {
                val options = FirebaseApp.getInstance().options
                val newApp = FirebaseApp.initializeApp(CatroidApplication.getAppContext(), options, baseId)
                FirebaseFirestore.getInstance(newApp)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve Firestore for base '$baseId'", e)
            null
        }
    }

    /**
     * Path format: "collection/docId" or "collection/docId/subcollection/subDocId" ...
     * Returns the document reference for the given path, or null if invalid/offline.
     */
    private fun getDocumentRef(baseId: String, path: String): DocumentReference? {
        if (!isInitialized || !hasInternet() || path.isBlank()) return null
        val segments = path.split("/").filter { it.isNotBlank() }
        if (segments.size % 2 != 0) {
            Log.e(TAG, "Path '$path' does not end with a document id")
            return null
        }
        return try {
            val firestore = getFirestore(baseId) ?: return null
            var ref: DocumentReference? = null
            var i = 0
            while (i < segments.size) {
                val collectionName = segments[i]
                val docId = segments[i + 1]
                ref = if (ref == null) firestore.collection(collectionName).document(docId)
                else ref.collection(collectionName).document(docId)
                i += 2
            }
            ref
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build document reference for '$path'", e)
            null
        }
    }

    /**
     * Converts a raw string value into a Firestore-compatible value:
     * JSON object/array -> Map/List, numeric -> Double, otherwise String.
     */
    private fun toFirestoreValue(value: String): Any {
        val trimmed = value.trim()
        if (trimmed.startsWith("{")) {
            return try {
                jsonObjectToMap(JSONObject(trimmed))
            } catch (e: Exception) {
                value
            }
        }
        if (trimmed.startsWith("[")) {
            return try {
                jsonArrayToList(JSONArray(trimmed))
            } catch (e: Exception) {
                value
            }
        }
        trimmed.toDoubleOrNull()?.let { return it }
        return value
    }

    private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
        val map = HashMap<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val raw = obj.opt(key)
            map[key] = when {
                raw == JSONObject.NULL -> null
                raw is JSONObject -> jsonObjectToMap(raw)
                raw is JSONArray -> jsonArrayToList(raw)
                else -> raw
            }
        }
        return map
    }

    private fun jsonArrayToList(arr: JSONArray): List<Any?> {
        val list = ArrayList<Any?>()
        for (i in 0 until arr.length()) {
            val raw = arr.opt(i)
            list.add(when {
                raw == JSONObject.NULL -> null
                raw is JSONObject -> jsonObjectToMap(raw)
                raw is JSONArray -> jsonArrayToList(raw)
                else -> raw
            })
        }
        return list
    }

    /** Serializes a Firestore document into a JSON string. */
    private fun documentToJson(doc: com.google.firebase.firestore.DocumentSnapshot?): String {
        if (doc == null || !doc.exists()) return "No data"
        return try {
            JSONObject(doc.data ?: mapOf<String, Any?>()).toString()
        } catch (e: Exception) {
            doc.data?.toString() ?: "No data"
        }
    }

    fun writeDocument(baseId: String, path: String, value: String, callback: (Boolean) -> Unit = {}) {
        val ref = getDocumentRef(baseId, path)
        if (ref == null) {
            callback(false)
            return
        }
        ref.set(toFirestoreValue(value))
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error writing document: ${error.message}")
                callback(false)
            }
    }

    fun updateDocument(baseId: String, path: String, value: String, callback: (Boolean) -> Unit = {}) {
        val ref = getDocumentRef(baseId, path)
        if (ref == null) {
            callback(false)
            return
        }
        ref.set(toFirestoreValue(value), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error updating document: ${error.message}")
                callback(false)
            }
    }

    fun addDocument(baseId: String, collection: String, value: String, callback: (String?) -> Unit) {
        if (!isInitialized || !hasInternet() || collection.isBlank()) {
            callback(null)
            return
        }
        val firestore = getFirestore(baseId)
        if (firestore == null) {
            callback(null)
            return
        }
        val ref = firestore.collection(collection)
        ref.add(toFirestoreValue(value))
            .addOnSuccessListener { documentReference ->
                callback(documentReference.id)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error adding document: ${error.message}")
                callback(null)
            }
    }

    fun readDocument(baseId: String, path: String, callback: (String?) -> Unit) {
        val ref = getDocumentRef(baseId, path)
        if (ref == null) {
            callback(null)
            return
        }
        ref.get()
            .addOnSuccessListener { documentSnapshot ->
                callback(documentToJson(documentSnapshot))
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error reading document: ${error.message}")
                callback(null)
            }
    }

    fun deleteDocument(baseId: String, path: String, callback: (Boolean) -> Unit = {}) {
        val ref = getDocumentRef(baseId, path)
        if (ref == null) {
            callback(false)
            return
        }
        ref.delete()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error deleting document: ${error.message}")
                callback(false)
            }
    }

    fun queryDocuments(baseId: String, collection: String, field: String, operator: String, value: String, limit: Int,
                       callback: (String?) -> Unit) {
        if (!isInitialized || !hasInternet() || collection.isBlank()) {
            callback(null)
            return
        }
        val firestore = getFirestore(baseId)
        if (firestore == null) {
            callback(null)
            return
        }
        val baseQuery = firestore.collection(collection)
        val query: Query = if (field.isNotBlank()) {
            val filterValue = toFirestoreValue(value)
            when (operator) {
                ">", "greater than" -> baseQuery.whereGreaterThan(field, filterValue)
                ">=", "greater or equal" -> baseQuery.whereGreaterThanOrEqualTo(field, filterValue)
                "<", "less than" -> baseQuery.whereLessThan(field, filterValue)
                "<=", "less or equal" -> baseQuery.whereLessThanOrEqualTo(field, filterValue)
                "!=", "not equal" -> baseQuery.whereNotEqualTo(field, filterValue)
                "contains", "array contains" -> baseQuery.whereArrayContains(field, filterValue)
                else -> baseQuery.whereEqualTo(field, filterValue)
            }
        } else {
            baseQuery
        }
        val limited = if (limit > 0) query.limit(limit.toLong()) else query
        limited.get()
            .addOnSuccessListener { snapshot: QuerySnapshot ->
                val array = JSONArray()
                for (doc in snapshot.documents) {
                    val obj = JSONObject()
                    obj.put("id", doc.id)
                    obj.put("data", JSONObject(doc.data ?: mapOf<String, Any?>()))
                    array.put(obj)
                }
                callback(array.toString())
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error querying documents: ${error.message}")
                callback(null)
            }
    }

    fun observeDocument(baseId: String, path: String, listener: com.google.firebase.firestore.EventListener<com.google.firebase.firestore.DocumentSnapshot>): ListenerRegistration? {
        val ref = getDocumentRef(baseId, path) ?: return null
        return try {
            ref.addSnapshotListener(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe document '$path'", e)
            null
        }
    }

    fun observeCollection(baseId: String, collection: String, listener: com.google.firebase.firestore.EventListener<QuerySnapshot>): ListenerRegistration? {
        if (!isInitialized || !hasInternet() || collection.isBlank()) return null
        val firestore = getFirestore(baseId) ?: return null
        return try {
            firestore.collection(collection).addSnapshotListener(listener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to observe collection '$collection'", e)
            null
        }
    }
}