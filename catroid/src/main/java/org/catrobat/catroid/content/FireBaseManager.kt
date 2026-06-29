package org.catrobat.catroid.content

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import org.catrobat.catroid.CatroidApplication

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

    fun writeToDatabase(databaseUrl: String, key: String, value: String) {
        val ref = getDbRef(databaseUrl, key) ?: return
        ref.setValue(value).addOnFailureListener { error ->
            Log.e("FireBaseManager", "Error writing data: ${error.message}")
        }
    }

    fun deleteFromDatabase(databaseUrl: String, key: String) {
        val ref = getDbRef(databaseUrl, key) ?: return
        ref.removeValue().addOnFailureListener { error ->
            Log.e("FireBaseManager", "Error deleting data: ${error.message}")
        }
    }
}
