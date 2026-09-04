package org.catrobat.catroid.apkbuildV3

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Serializable

data class FirebaseConfig(
    val projectNumber: String,
    val projectId: String,
    val mobileSdkAppId: String,
    val apiKey: String,
    val storageBucket: String,
    val databaseUrl: String,
    val defaultWebClientId: String,
    val matchedPackageName: String,
    val sourceFileName: String,
    val originalJson: String
) : Serializable

object FirebaseConfigManager {
    private const val TAG = "FirebaseConfigManager"

    data class FirebaseError(val message: String)
    data class FirebaseResult(val config: FirebaseConfig?, val error: FirebaseError?)
    data class ClientMatch(val client: JSONObject?, val foundPackages: List<String>)

    fun matchClient(clients: JSONArray, targetPackageName: String): ClientMatch {
        var matchedClient: JSONObject? = null
        val foundPackages = mutableListOf<String>()
        for (i in 0 until clients.length()) {
            val client = clients.optJSONObject(i) ?: continue
            val clientInfo = client.optJSONObject("client_info") ?: continue
            val androidInfo = clientInfo.optJSONObject("android_client_info") ?: continue
            val pkg = androidInfo.optString("package_name", "")
            if (pkg.isNotBlank() && pkg !in foundPackages) {
                foundPackages.add(pkg)
            }
            if (matchedClient == null && pkg == targetPackageName) {
                matchedClient = client
            }
        }
        return ClientMatch(matchedClient, foundPackages)
    }

    fun processGoogleServicesJson(
        context: Context,
        uri: Uri,
        targetPackageName: String
    ): FirebaseResult {
        try {
            val jsonContent = readUriContent(context, uri) ?: return FirebaseResult(
                null, FirebaseError(getErrorMessage(context, "v3_firebase_error_read_failed"))
            )
            if (jsonContent.isBlank()) {
                return FirebaseResult(null, FirebaseError(getErrorMessage(context, "v3_firebase_error_read_failed")))
            }

            val json = try {
                JSONObject(jsonContent)
            } catch (e: Exception) {
                Log.e(TAG, "Invalid JSON", e)
                return FirebaseResult(null, FirebaseError(getErrorMessage(context, "v3_firebase_error_invalid_json")))
            }

            if (!json.has("project_info")) {
                return FirebaseResult(null, FirebaseError(getErrorMessage(context, "v3_firebase_error_no_project_info")))
            }
            val projectInfo = json.getJSONObject("project_info")
            val projectNumber = projectInfo.optString("project_number", "")
            val projectId = projectInfo.optString("project_id", "")
            val storageBucket = projectInfo.optString("storage_bucket", "")
            val databaseUrl = projectInfo.optString("firebase_url", "")

            if (!json.has("client")) {
                return FirebaseResult(null, FirebaseError(getErrorMessage(context, "v3_firebase_error_no_client")))
            }
            val clients = json.getJSONArray("client")
            if (clients.length() == 0) {
                return FirebaseResult(null, FirebaseError(getErrorMessage(context, "v3_firebase_error_no_client")))
            }

            val match = matchClient(clients, targetPackageName)
            val matchedClient = match.client

            if (matchedClient == null) {
                val msg = if (match.foundPackages.isNotEmpty()) {
                    val foundText = match.foundPackages.joinToString(", ")
                    val resId = context.resources.getIdentifier(
                        "v3_firebase_error_package_mismatch", "string", context.packageName)
                    if (resId != 0) {
                        context.getString(resId, targetPackageName, foundText)
                    } else {
                        "v3_firebase_error_package_mismatch"
                    }
                } else {
                    getErrorMessage(context, "v3_firebase_error_no_package_name")
                }
                return FirebaseResult(null, FirebaseError(msg))
            }

            val clientInfo = matchedClient.getJSONObject("client_info")
            val androidInfo = clientInfo.getJSONObject("android_client_info")
            val matchedPackage = androidInfo.getString("package_name")
            val mobileSdkAppId = clientInfo.optString("mobilesdk_app_id", "")

            val apiKey = extractApiKey(matchedClient)
            val defaultWebClientId = extractDefaultWebClientId(matchedClient)

            val fileName = uri.lastPathSegment ?: "google-services.json"

            val config = FirebaseConfig(
                projectNumber = projectNumber,
                projectId = projectId,
                mobileSdkAppId = mobileSdkAppId,
                apiKey = apiKey,
                storageBucket = storageBucket,
                databaseUrl = databaseUrl,
                defaultWebClientId = defaultWebClientId,
                matchedPackageName = matchedPackage,
                sourceFileName = fileName,
                originalJson = jsonContent
            )
            return FirebaseResult(config, null)

        } catch (e: Exception) {
            Log.e(TAG, "Firebase processing error", e)
            return FirebaseResult(null, FirebaseError(
                getErrorMessage(context, "v3_firebase_error_processing")
            ))
        }
    }

    private fun extractApiKey(client: JSONObject): String {
        val apiKeyArr = client.optJSONArray("api_key")
        if (apiKeyArr != null && apiKeyArr.length() > 0) {
            return apiKeyArr.getJSONObject(0).optString("current_key", "")
        }
        return ""
    }

    private fun extractDefaultWebClientId(client: JSONObject): String {
        val oauthArr = client.optJSONArray("oauth_client")
        if (oauthArr != null) {
            for (i in 0 until oauthArr.length()) {
                val oauth = oauthArr.getJSONObject(i)
                if (oauth.optInt("client_type", 0) == 3) {
                    return oauth.optString("client_id", "")
                }
            }
        }
        return ""
    }

    private fun readUriContent(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read URI: $uri", e)
            null
        }
    }

    private fun getErrorMessage(context: Context, resName: String): String {
        val resId = context.resources.getIdentifier(resName, "string", context.packageName)
        return if (resId != 0) context.getString(resId) else resName
    }
}
