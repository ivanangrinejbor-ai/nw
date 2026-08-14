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
package org.catrobat.catroid.ui

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import org.catrobat.catroid.BuildConfig
import org.catrobat.catroid.telemetry.TelemetryManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val SETTINGS_CRASH_REPORTS = "setting_enable_crash_reports"
    private const val REPORT_DIR_NAME = "crashReports"
    private const val MAX_LOGCAT_LINES = 1000
    private const val MAX_REPORT_CHARS = 200_000
    private const val COLLECTION_NAME = "crashes"
    private const val REPORT_FILE_PREFIX = "crash_"
    private const val REPORT_FILE_SUFFIX = ".txt"

    fun isCrashReportingEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(SETTINGS_CRASH_REPORTS, true)
    }

    fun collectLogcat(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "threadtime", "*:E"))
            val text = process.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            process.waitFor()
            val lines = text.lines()
            if (lines.size > MAX_LOGCAT_LINES) {
                lines.takeLast(MAX_LOGCAT_LINES).joinToString("\n")
            } else {
                text
            }
        } catch (e: Exception) {
            Log.w(TAG, "logcat collection failed", e)
            "logcat unavailable: ${e.message}"
        }
    }

    fun buildReport(thread: Thread, throwable: Throwable, logcat: String): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        pw.println("TIME: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        pw.println("APP_VERSION: " + BuildConfig.VERSION_NAME)
        pw.println("ANDROID_VERSION: " + Build.VERSION.RELEASE)
        pw.println("DEVICE_MODEL: " + Build.MODEL)
        pw.println("THREAD: " + thread.name)
        pw.println("EXCEPTION: " + throwable.javaClass.name + ": " + throwable.message)
        pw.println("===== STACK TRACE =====")
        throwable.printStackTrace(pw)
        pw.println("===== LOGCAT (E/F) =====")
        pw.println(logcat)
        pw.flush()
        return sw.toString().take(MAX_REPORT_CHARS)
    }

    fun saveReport(context: Context, report: String): File? {
        return try {
            val dir = File(context.cacheDir, REPORT_DIR_NAME)
            if (!dir.exists() && !dir.mkdirs()) {
                return null
            }
            val file = File(dir, REPORT_FILE_PREFIX + UUID.randomUUID() + REPORT_FILE_SUFFIX)
            file.writeText(report)
            file
        } catch (e: Exception) {
            Log.w(TAG, "failed to save crash report", e)
            null
        }
    }

    fun sendToFirestore(context: Context, file: File) {
        try {
            if (!isCrashReportingEnabled(context)) {
                return
            }
            val firestore = TelemetryManager.getTelemetryFirestore(context) ?: return
            val report = file.readText()
            val data = linkedMapOf<String, Any>(
                "crash" to report,
                "timestamp" to System.currentTimeMillis(),
                "app_version" to BuildConfig.VERSION_NAME,
                "android_version" to Build.VERSION.RELEASE,
                "device_model" to Build.MODEL,
                "thread" to extractThreadName(report)
            )
            firestore.collection(COLLECTION_NAME)
                .document(UUID.randomUUID().toString())
                .set(data)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        file.delete()
                    } else {
                        Log.w(TAG, "crash report send failed", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "crash report send failed", e)
        }
    }

    fun sendPendingReports(context: Context) {
        try {
            val dir = File(context.cacheDir, REPORT_DIR_NAME)
            if (!dir.exists()) {
                return
            }
            dir.listFiles()
                ?.filter { it.isFile && it.name.startsWith(REPORT_FILE_PREFIX) && it.name.endsWith(REPORT_FILE_SUFFIX) }
                ?.forEach { sendToFirestore(context, it) }
        } catch (e: Exception) {
            Log.w(TAG, "pending crash reports check failed", e)
        }
    }

    private fun extractThreadName(report: String): String {
        return report.lineSequence()
            .firstOrNull { it.startsWith("THREAD: ") }
            ?.removePrefix("THREAD: ")
            ?.trim()
            .orEmpty()
    }
}