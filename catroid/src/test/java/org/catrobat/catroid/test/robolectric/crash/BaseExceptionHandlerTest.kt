package org.catrobat.catroid.test.robolectric.crash

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import org.catrobat.catroid.ui.BaseExceptionHandler
import org.catrobat.catroid.ui.CrashActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BaseExceptionHandlerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun testUncaughtExceptionSavesReportAndLaunchesCrashActivity() {
        val handler = BaseExceptionHandler(context)
        handler.uncaughtException(Thread.currentThread(), RuntimeException("test crash"))

        val crashDir = File(context.cacheDir, "crashReports")
        val files = crashDir.listFiles { f -> f.name.startsWith("crash_") && f.name.endsWith(".txt") }
        assertNotNull(files)
        assertTrue(files!!.isNotEmpty())
        assertTrue(files[0].readText().contains("test crash"))

        val startedIntent = Shadows.shadowOf(context as Application).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(CrashActivity::class.java.name, startedIntent!!.component?.className)
        assertEquals(files[0].absolutePath, startedIntent.getStringExtra(CrashActivity.EXTRA_REPORT_PATH))
    }

    @Test
    fun testUncaughtExceptionSetsRecoveredFromCrashFlag() {
        val handler = BaseExceptionHandler(context)
        handler.uncaughtException(Thread.currentThread(), RuntimeException("test crash"))
        assertTrue(
            PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("RECOVERED_FROM_CRASH", false)
        )
    }
}