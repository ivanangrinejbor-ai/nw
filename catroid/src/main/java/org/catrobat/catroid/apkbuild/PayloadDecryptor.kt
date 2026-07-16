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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Decrypts a baked-project payload (neocatroid.dat).
 *
 * First attempt uses the built-in static key (backward compatible with payloads baked
 * without a passphrase). If that fails, the user is prompted for the passphrase that was
 * set when the APK was built — this is what makes the payload actually confidential
 * (the static key alone gives zero secrecy since it ships in the open-source app).
 *
 * Must be called from a background thread: it blocks (via a latch) while an AlertDialog
 * is shown on the main thread.
 */
object PayloadDecryptor {
    /**
     * Decrypt with a known password (e.g. from neocatroid.key).
     * If it fails, falls back to the static [ProtectedProjectPayload.PASSWORD]
     * for backward compatibility, then prompts the user.
     */
    fun decrypt(context: Context, encryptedFile: File, decryptedZip: File, password: String = ProtectedProjectPayload.PASSWORD): Boolean {
        if (ProjectCrypto.decrypt(encryptedFile, decryptedZip, password)) {
            return true
        }
        if (password != ProtectedProjectPayload.PASSWORD &&
            ProjectCrypto.decrypt(encryptedFile, decryptedZip, ProtectedProjectPayload.PASSWORD)) {
            return true
        }

        val result = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        var passphrase: String? = null

        Handler(Looper.getMainLooper()).post {
            val input = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                hint = context.getString(R.string.payload_password_hint)
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

        latch.await()
        if (!passphrase.isNullOrEmpty()) {
            result.set(ProjectCrypto.decrypt(encryptedFile, decryptedZip, passphrase!!))
        }
        return result.get()
    }
}
