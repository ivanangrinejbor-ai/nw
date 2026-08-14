package org.catrobat.catroid.test.robolectric.crash

import android.content.Context
import android.preference.PreferenceManager
import org.catrobat.catroid.ui.CrashReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CrashReporterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun testBuildReportContainsHeaderAndStackTrace() {
        val throwable = IllegalStateException("boom")
        val logcat = "E/AndroidRuntime: FATAL EXCEPTION\nE/MyApp: some error"
        val report = CrashReporter.buildReport(Thread.currentThread(), throwable, logcat)

        assertTrue(report.startsWith("TIME: "))
        assertTrue(report.contains("APP_VERSION: "))
        assertTrue(report.contains("ANDROID_VERSION: "))
        assertTrue(report.contains("DEVICE_MODEL: "))
        assertTrue(report.contains("THREAD: " + Thread.currentThread().name))
        assertTrue(report.contains("EXCEPTION: java.lang.IllegalStateException: boom"))
        assertTrue(report.contains("===== STACK TRACE ====="))
        assertTrue(report.contains("===== LOGCAT (E/F) ====="))
        assertTrue(report.contains(logcat))
    }

    @Test
    fun testBuildReportTruncatesOversizedLogcat() {
        val hugeLogcat = (0..3000).joinToString("\n") { "E/Line$it" }
        val report = CrashReporter.buildReport(Thread.currentThread(), RuntimeException(), hugeLogcat)
        assertTrue(report.length <= 200_000)
    }

    @Test
    fun testSaveReportCreatesFileInCrashReportsDir() {
        val report = CrashReporter.buildReport(Thread.currentThread(), RuntimeException("x"), "logcat")
        val file = CrashReporter.saveReport(context, report)

        assertNotNull(file)
        file?.let {
            assertTrue(it.exists())
            assertTrue(it.absolutePath.contains("crashReports"))
            assertTrue(it.name.startsWith("crash_"))
            assertTrue(it.name.endsWith(".txt"))
            assertEquals(report, it.readText())
        }
    }

    @Test
    fun testCrashReportingEnabledByDefault() {
        assertTrue(CrashReporter.isCrashReportingEnabled(context))
    }

    @Test
    fun testCrashReportingDisabledRespected() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean("setting_enable_crash_reports", false)
            .apply()
        assertFalse(CrashReporter.isCrashReportingEnabled(context))
    }

    @Test
    fun testSendToFirestoreDoesNotDeleteFileWhenDisabled() {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean("setting_enable_crash_reports", false)
            .apply()
        val file = CrashReporter.saveReport(context, "report")
        assertNotNull(file)
        CrashReporter.sendToFirestore(context, file!!)
        assertTrue(file.exists())
    }

    @Test
    fun testSendPendingReportsDoesNotCrash() {
        val reportDir = File(context.cacheDir, "crashReports")
        reportDir.mkdirs()
        File(reportDir, "crash_pending.txt").writeText("pending report")
        CrashReporter.sendPendingReports(context)
        assertTrue(File(reportDir, "crash_pending.txt").exists())
    }
}