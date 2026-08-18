package org.catrobat.catroid.desktop.firebase

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DesktopFirebaseManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val activeStreams = ConcurrentHashMap<String, okhttp3.Call>()

    private fun normalizeDbUrl(url: String, key: String): String {
        val cleanUrl = url.trim().removeSuffix("/")
        val cleanKey = key.trim().removePrefix("/").removeSuffix("/")
        return if (cleanKey.isEmpty()) "$cleanUrl.json" else "$cleanUrl/$cleanKey.json"
    }

    fun readFromDatabase(databaseUrl: String, key: String, callback: (String?) -> Unit) {
        val endpoint = normalizeDbUrl(databaseUrl, key)
        val request = Request.Builder().url(endpoint).get().build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                System.err.println("[DesktopFirebase] Read failed for $key: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        val value = body?.trim()?.removePrefix("\"")?.removeSuffix("\"") ?: ""
                        callback(value)
                    } else {
                        callback(null)
                    }
                }
            }
        })
    }

    fun writeToDatabase(databaseUrl: String, key: String, value: String, callback: (Boolean) -> Unit = {}) {
        val endpoint = normalizeDbUrl(databaseUrl, key)
        val jsonPayload = if (value.startsWith("{") || value.startsWith("[") || value.toDoubleOrNull() != null || value == "true" || value == "false") {
            value
        } else {
            "\"${value.replace("\"", "\\\"")}\""
        }

        val request = Request.Builder()
            .url(endpoint)
            .put(jsonPayload.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                System.err.println("[DesktopFirebase] Write failed: ${e.message}")
                callback(false)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { callback(response.isSuccessful) }
            }
        })
    }

    fun deleteFromDatabase(databaseUrl: String, key: String, callback: (Boolean) -> Unit = {}) {
        val endpoint = normalizeDbUrl(databaseUrl, key)
        val request = Request.Builder().url(endpoint).delete().build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { callback(response.isSuccessful) }
            }
        })
    }

    private const val STORAGE_API = "https://firebasestorage.googleapis.com/v0/b"

    fun uploadFile(bucket: String, path: String, localFile: File, callback: (Boolean) -> Unit = {}) {
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        val endpoint = "$STORAGE_API/$bucket/o?name=$encodedPath"
        val requestBody = localFile.readBytes().toRequestBody("application/octet-stream".toMediaType())

        val request = Request.Builder().url(endpoint).post(requestBody).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { callback(response.isSuccessful) }
            }
        })
    }

    fun downloadFile(bucket: String, path: String, destFile: File, callback: (Boolean) -> Unit = {}) {
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        val endpoint = "$STORAGE_API/$bucket/o/$encodedPath?alt=media"

        val request = Request.Builder().url(endpoint).get().build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (response.isSuccessful) {
                        destFile.parentFile?.mkdirs()
                        response.body?.byteStream()?.use { input ->
                            destFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        callback(true)
                    } else {
                        callback(false)
                    }
                }
            }
        })
    }

    fun listFiles(bucket: String, prefix: String, callback: (List<String>) -> Unit) {
        val encodedPrefix = URLEncoder.encode(prefix, "UTF-8")
        val endpoint = "$STORAGE_API/$bucket/o?maxResults=1000&prefix=$encodedPrefix"

        val request = Request.Builder().url(endpoint).get().build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(emptyList())
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!response.isSuccessful) {
                        callback(emptyList())
                        return
                    }
                    val body = response.body?.string() ?: ""
                    val names = mutableListOf<String>()
                    val pattern = Regex("\"name\":\"(.*?)\"")
                    for (m in pattern.findAll(body)) {
                        names += m.groupValues[1]
                    }
                    callback(names)
                }
            }
        })
    }

    fun deleteFile(bucket: String, path: String, callback: (Boolean) -> Unit = {}) {
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        val endpoint = "$STORAGE_API/$bucket/o/$encodedPath"

        val request = Request.Builder().url(endpoint).delete().build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(false)
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { callback(response.isSuccessful) }
            }
        })
    }
}
