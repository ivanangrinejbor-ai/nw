package org.catrobat.catroid.test.robolectric.crash

import android.app.AlertDialog
import android.content.Intent
import android.os.Looper
import org.catrobat.catroid.ui.CrashActivity
import org.catrobat.catroid.ui.MainMenuActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAlertDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CrashActivityTest {

    @Test
    fun testCrashActivityShowsDialogAndNavigatesToMainMenu() {
        val controller = Robolectric.buildActivity(CrashActivity::class.java)
            .create()
            .start()
            .visible()
        val activity = controller.get()

        val shadowDialog = ShadowAlertDialog.getLatestAlertDialog()
        assertNotNull(shadowDialog)
        val dialog = shadowDialog as AlertDialog
        assertTrue(dialog.isShowing)

        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        assertNotNull(positiveButton)
        positiveButton.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val startedIntent = Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(MainMenuActivity::class.java.name, startedIntent!!.component?.className)
        assertTrue(startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(startedIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    @Test
    fun testCrashActivityWithReportPathDoesNotCrash() {
        val reportFile = File.createTempFile("crash_test", ".txt")
        reportFile.writeText("test crash report")
        val intent = Intent().putExtra(CrashActivity.EXTRA_REPORT_PATH, reportFile.absolutePath)

        val controller = Robolectric.buildActivity(CrashActivity::class.java, intent)
            .create()
            .start()
            .visible()
        assertNotNull(controller.get())
    }
}