package org.catrobat.catroid.apkbuild

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import org.catrobat.catroid.R
import org.catrobat.catroid.io.ProjectCrypto
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object PayloadDecryptor {
    fun decrypt(context: Context, encryptedFile: File, decryptedZip: File, password: String): Boolean {
        if (ProjectCrypto.decrypt(encryptedFile, decryptedZip, password)) {
            return true
        }

        val result = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        var passphrase: String? = null

        Handler(Looper.getMainLooper()).post {
            val input = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                hint = context.getString(R.string.payload_password_hint)
                importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            }
            AlertDialog.Builder(context)
                .setTitle(R.string.payload_password_title)
                .setMessage(R.string.payload_password_message)
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    passphrase = input.text.toString()
                    latch.countDown()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> latch.countDown() }
                .setCancelable(false)
                .show()
        }

        latch.await(60, TimeUnit.SECONDS)
        if (!passphrase.isNullOrEmpty()) {
            result.set(ProjectCrypto.decrypt(encryptedFile, decryptedZip, passphrase!!))
        }
        return result.get()
    }
}
