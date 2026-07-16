package org.catrobat.catroid.content

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object FireBaseStorageManager {
    private const val TAG = "FireBaseStorageManager"
    private const val STORAGE_API = "https://firebasestorage.googleapis.com/v0/b"

    fun uploadFile(bucket: String, path: String, localFile: File): Boolean {
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        val url = URL("$STORAGE_API/$bucket/o?name=$encodedPath")
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            localFile.inputStream().use { input ->
                connection.outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            val code = connection.responseCode
            (code in 200..299).also { success ->
                if (!success) {
                    Log.e(TAG, "Upload failed: HTTP $code ${connection.responseMessage}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            false
        }
    }

    fun downloadFile(bucket: String, path: String, destFile: File): Boolean {
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        val url = URL("$STORAGE_API/$bucket/o/$encodedPath?alt=media")
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            val code = connection.responseCode
            if (code !in 200..299) {
                Log.e(TAG, "Download failed: HTTP $code ${connection.responseMessage}")
                return false
            }
            destFile.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            false
        }
    }

    fun listFiles(bucket: String, prefix: String): List<String> {
        val encodedPrefix = URLEncoder.encode(prefix, "UTF-8")
        val url = URL("$STORAGE_API/$bucket/o?prefix=$encodedPrefix")
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val code = connection.responseCode
            if (code !in 200..299) {
                Log.e(TAG, "List failed: HTTP $code ${connection.responseMessage}")
                return emptyList()
            }
            val json = connection.inputStream.bufferedReader().readText()
            parseListResponse(json)
        } catch (e: Exception) {
            Log.e(TAG, "List error", e)
            emptyList()
        }
    }

    fun deleteFile(bucket: String, path: String): Boolean {
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        val url = URL("$STORAGE_API/$bucket/o/$encodedPath")
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val code = connection.responseCode
            (code in 200..299).also { success ->
                if (!success) {
                    Log.e(TAG, "Delete failed: HTTP $code ${connection.responseMessage}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete error", e)
            false
        }
    }

    private fun parseListResponse(json: String): List<String> {
        val items = mutableListOf<String>()
        val searchKey = "\"name\":\""
        var start = json.indexOf(searchKey)
        while (start >= 0) {
            start += searchKey.length
            val end = json.indexOf("\"", start)
            if (end < 0) break
            items.add(json.substring(start, end))
            start = json.indexOf(searchKey, end)
        }
        return items
    }
}
