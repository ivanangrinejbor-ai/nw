package org.catrobat.catroid.content

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import org.catrobat.catroid.CatroidApplication
import org.json.JSONObject

object FireBaseManager {
    private val isInitialized by lazy {
        try {
            if (FirebaseApp.getApps(CatroidApplication.getAppContext()).isEmpty()) {
                FirebaseApp.initializeApp(CatroidApplication.getAppContext())
            }
            true
        } catch (e: Exception) {
            Log.e("FireBaseManager", "Firebase init failed", e)
            false
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

    private fun getDbRef(url: String, key: String): DatabaseReference? {
        if (!isInitialized || !hasInternet() || url.isBlank()) return null
        return try {
            FirebaseDatabase.getInstance(url).reference.child(key)
        } catch (e: Exception) {
            Log.e("FireBaseManager", "Failed to get DB ref", e)
            null
        }
    }

    fun readFromDatabase(databaseUrl: String, key: String, callback: (String?) -> Unit) {
        val ref = getDbRef(databaseUrl, key)
        if (ref == null) {
            callback(null)
            return
        }
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.value?.toString() ?: "No data")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FireBaseManager", "Error reading data: ${error.message}")
                callback(null)
            }
        })
    }

    fun writeToDatabase(databaseUrl: String, key: String, value: String, callback: (Boolean) -> Unit = {}) {
        val ref = getDbRef(databaseUrl, key)
        if (ref == null) {
            callback(false)
            return
        }
        ref.setValue(value)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { error ->
                Log.e("FireBaseManager", "Error writing data: ${error.message}")
                callback(false)
            }
    }

    fun deleteFromDatabase(databaseUrl: String, key: String, callback: (Boolean) -> Unit = {}) {
        val ref = getDbRef(databaseUrl, key)
        if (ref == null) {
            callback(false)
            return
        }
        ref.removeValue()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { error ->
                Log.e("FireBaseManager", "Error deleting data: ${error.message}")
                callback(false)
            }
    }

    fun observeValue(databaseUrl: String, key: String, listener: ValueEventListener): DatabaseReference? {
        val ref = getDbRef(databaseUrl, key) ?: return null
        ref.addValueEventListener(listener)
        return ref
    }

    fun pushToDatabase(databaseUrl: String, key: String, value: String, callback: (String?) -> Unit) {
        val ref = getDbRef(databaseUrl, key)
        if (ref == null) {
            callback(null)
            return
        }
        val pushRef = ref.push()
        pushRef.setValue(value)
            .addOnSuccessListener { callback(pushRef.key) }
            .addOnFailureListener { error ->
                Log.e("FireBaseManager", "Error pushing data: ${error.message}")
                callback(null)
            }
    }

    fun updateDatabase(databaseUrl: String, key: String, value: String, callback: (Boolean) -> Unit = {}) {
        val ref = getDbRef(databaseUrl, key)
        if (ref == null) {
            callback(false)
            return
        }
        val trimmed = value.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val updates = HashMap<String, Any?>()
            try {
                val obj = JSONObject(trimmed)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val childKey = keys.next()
                    updates[childKey] = obj.get(childKey)
                }
            } catch (e: Exception) {
                Log.e("FireBaseManager", "Invalid JSON for update: ${e.message}")
                callback(false)
                return
            }
            ref.updateChildren(updates)
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { error ->
                    Log.e("FireBaseManager", "Error updating data: ${error.message}")
                    callback(false)
                }
        } else {
            ref.setValue(value)
                .addOnSuccessListener { callback(true) }
                .addOnFailureListener { error ->
                    Log.e("FireBaseManager", "Error updating data: ${error.message}")
                    callback(false)
                }
        }
    }

    fun queryDatabase(databaseUrl: String, key: String, orderBy: String, limit: Long, equalTo: String,
                      callback: (String?) -> Unit) {
        val ref = getDbRef(databaseUrl, key)
        if (ref == null) {
            callback(null)
            return
        }
        var query: Query = if (orderBy.isBlank()) ref else ref.orderByChild(orderBy)
        if (limit > 0) {
            query = query.limitToFirst(limit.toInt())
        }
        if (equalTo.isNotBlank()) {
            query = query.equalTo(equalTo)
        }
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.value
                callback(if (value == null) "No data" else JSONObject(value as? Map<*, *> ?: mapOf("value" to value)).toString())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FireBaseManager", "Error querying data: ${error.message}")
                callback(null)
            }
        })
    }

    fun observeChild(databaseUrl: String, key: String, listener: ChildEventListener): DatabaseReference? {
        val ref = getDbRef(databaseUrl, key) ?: return null
        ref.addChildEventListener(listener)
        return ref
    }
}
