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

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import org.catrobat.catroid.R
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reportPath = intent?.getStringExtra(EXTRA_REPORT_PATH)
        val reportFile = reportPath?.let { File(it) }
        showCrashDialog(reportFile)
        sendReportOnce(reportFile)
    }

    private fun showCrashDialog(reportFile: File?) {
        AlertDialog.Builder(this)
            .setTitle(R.string.crash_dialog_title)
            .setMessage(R.string.crash_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.crash_dialog_ok) { _, _ -> openMainMenu() }
            .show()
    }

    private fun sendReportOnce(reportFile: File?) {
        if (reportFile == null || !reportFile.exists()) {
            return
        }
        if (!sendAttempted.compareAndSet(false, true)) {
            return
        }
        Thread {
            CrashReporter.sendToFirestore(applicationContext, reportFile)
        }.start()
    }

    private fun openMainMenu() {
        val intent = Intent(this, MainMenuActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_REPORT_PATH = "crash_report_path"
        private val sendAttempted = AtomicBoolean(false)
    }
}