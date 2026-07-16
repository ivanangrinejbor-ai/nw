package org.catrobat.catroid.ui.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import org.catrobat.catroid.R
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.apkbuildV3.ApkBuilderV3Config
import org.catrobat.catroid.apkbuildV3.ApkBuilderV3Engine
import org.catrobat.catroid.apkbuildV3.AssemblyResult
import org.catrobat.catroid.apkbuildV3.BuildProgressListener
import org.catrobat.catroid.apkbuildV3.TemplateType
import org.catrobat.catroid.io.asynctask.saveProjectSerial
import org.catrobat.catroid.utils.ToastUtil
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Export settings dialog for APK Builder V3.
 *
 * Provides a full-featured UI for configuring the APK build:
 * - App name, package name, version, version code
 * - Min/Target SDK
 * - Template type (Light / Full)
 * - Permissions selection
 * - Icon selection
 *
 * Opens as a dialog fragment integrated into the ProjectOptionsFragment flow.
 */
class ApkBuilderV3ExportDialog {
    private val tag = "ApkBuilderV3ExportDialog"
    private var config: ApkBuilderV3Config? = null
    private var projectDir: File? = null

    fun show(activity: Fragment, projectDir: File) {
        this.projectDir = projectDir
        val dialog = buildDialog(activity)
        dialog.show()
    }

    private fun buildDialog(host: Fragment): AlertDialog {
        val ctx = host.requireContext()
        val project = ProjectManager.getInstance().currentProject ?: return AlertDialog.Builder(ctx).create()

        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_apk_builder_v3_export, null)

        // Pre-fill fields
        view.findViewById<TextInputEditText>(R.id.v3_app_name_input)?.setText(project.name)
        view.findViewById<TextInputEditText>(R.id.v3_package_input)?.setText("org.neocatroid.runtime.v3")
        view.findViewById<TextInputEditText>(R.id.v3_version_input)?.setText("1.0")
        view.findViewById<TextInputEditText>(R.id.v3_version_code_input)?.setText("1")
        view.findViewById<TextInputEditText>(R.id.v3_min_sdk_input)?.setText("21")
        view.findViewById<TextInputEditText>(R.id.v3_target_sdk_input)?.setText("35")

        // Populate permissions
        val permContainer = view.findViewById<LinearLayout>(R.id.v3_permissions_container)
        val permChecks = ApkBuilderV3Config.ALL_PERMISSIONS.map { perm ->
            val cb = CheckBox(ctx).apply {
                text = perm.substringAfterLast('.')
                isChecked = perm in ApkBuilderV3Config.DEFAULT_PERMISSIONS
            }
            permContainer?.addView(cb)
            perm to cb
        }

        // Icon state
        val hasProjectIcon = File(project.directory, "manual_screenshot.png").exists() ||
                File(project.directory, "automatic_screenshot.png").exists()
        view.findViewById<RadioButton>(R.id.v3_icon_project)?.isChecked = hasProjectIcon
        view.findViewById<RadioButton>(R.id.v3_icon_default)?.isChecked = !hasProjectIcon

        // Template type — Full is the default (complete, proven path; Light is experimental)
        view.findViewById<RadioButton>(R.id.v3_template_full)?.isChecked = true

        val builder = AlertDialog.Builder(ctx)
            .setTitle("APK Builder V3")
            .setView(view)
            .setPositiveButton("Build") { _, _ ->
                val appName = view.findViewById<TextInputEditText>(R.id.v3_app_name_input)?.text.toString()
                    .ifBlank { project.name }
                val pkgRaw = view.findViewById<TextInputEditText>(R.id.v3_package_input)?.text.toString()
                    .ifBlank { "org.neocatroid.runtime.v3" }
                val pkg = pkgRaw.lowercase()

                // Validate package name
                if (!pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+$"))) {
                    ToastUtil.showError(ctx, "Invalid package name format")
                    return@setPositiveButton
                }

                val versionName = view.findViewById<TextInputEditText>(R.id.v3_version_input)?.text.toString()
                    .ifBlank { "1.0" }
                val versionCode = view.findViewById<TextInputEditText>(R.id.v3_version_code_input)?.text.toString()
                    .toIntOrNull() ?: 1
                val minSdk = view.findViewById<TextInputEditText>(R.id.v3_min_sdk_input)?.text.toString()
                    .toIntOrNull() ?: 21
                val targetSdk = view.findViewById<TextInputEditText>(R.id.v3_target_sdk_input)?.text.toString()
                    .toIntOrNull() ?: 35

                if (minSdk > targetSdk) {
                    ToastUtil.showError(ctx, "Min SDK cannot be greater than Target SDK")
                    return@setPositiveButton
                }

                val templateType = if (view.findViewById<RadioButton>(R.id.v3_template_full)?.isChecked == true)
                    TemplateType.FULL else TemplateType.LIGHT

                val permissions = permChecks.filter { it.second.isChecked }.map { it.first }

                val iconFile: File? = if (view.findViewById<RadioButton>(R.id.v3_icon_project)?.isChecked == true) {
                    val manual = File(project.directory, "manual_screenshot.png")
                    val auto = File(project.directory, "automatic_screenshot.png")
                    when {
                        manual.exists() -> manual
                        auto.exists() -> auto
                        else -> null
                    }
                } else null

                config = ApkBuilderV3Config(
                    appName = appName,
                    packageName = pkg,
                    versionName = versionName,
                    versionCode = versionCode,
                    minSdk = minSdk,
                    targetSdk = targetSdk,
                    iconFile = iconFile,
                    permissions = permissions,
                    templateType = templateType
                )

                // Start build
                startBuild(host)
            }
            .setNegativeButton("Cancel", null)

        return builder.create()
    }

    private fun startBuild(host: Fragment) {
        val ctx = host.requireContext()
        val cfg = config ?: return
        val projDir = projectDir ?: return

        // Show progress dialog
        val progressView = LayoutInflater.from(ctx).inflate(R.layout.activity_apk_builder_v3_progress, null)
        val progressBar = progressView.findViewById<ProgressBar>(R.id.v3_build_progress_bar)
        val percentText = progressView.findViewById<TextView>(R.id.v3_progress_percent)
        val stageText = progressView.findViewById<TextView>(R.id.v3_progress_stage)
        val factText = progressView.findViewById<TextView>(R.id.v3_build_fact)

        val buildDialog = AlertDialog.Builder(ctx)
            .setView(progressView)
            .setCancelable(false)
            .create()
        buildDialog.show()

        // Show initial random fact
        showV3Fact(factText)

        val mainHandler = Handler(Looper.getMainLooper())
        host.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Save project first
                ProjectManager.getInstance().currentProject?.let {
                    saveProjectSerial(it, ctx)
                }

                val result = ApkBuilderV3Engine.build(
                    context = ctx,
                    projectDir = projDir,
                    config = cfg,
                    listener = BuildProgressListener { progress, stage ->
                        mainHandler.post {
                            progressBar.progress = progress.toInt()
                            percentText.text = "${progress.toInt()}%"
                            stageText.text = stage
                            if (Math.random() < 0.3) {
                                showV3Fact(factText)
                            }
                        }
                    }
                )

                withContext(Dispatchers.Main) {
                    buildDialog.dismiss()
                    when (result) {
                        is AssemblyResult.Success -> {
                            saveApkToDownloads(ctx, result.apkFile)
                            Log.i(tag, "Build successful: ${result.apkFile.absolutePath} " +
                                    "(${result.totalSizeBytes / (1024 * 1024)} MB, ${result.templateType})")
                        }
                        is AssemblyResult.Failure -> {
                            ToastUtil.showError(ctx, "Build failed: ${result.message}")
                            Log.e(tag, "Build failed: ${result.message}", result.cause)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    buildDialog.dismiss()
                    ToastUtil.showError(ctx, "Build error: ${e.message}")
                    Log.e(tag, "Build error", e)
                }
            }
        }
    }

    private fun showV3Fact(textView: TextView) {
        val facts = listOf(
            "The first Android phone was released in 2008.",
            "Catrobat was inspired by Scratch from MIT.",
            "NeoCatroid supports over 390 different brick types.",
            "APK Builder V3 uses dynamic AES-256-GCM encryption.",
            "Over 2.5 billion Android devices are active worldwide.",
            "Visual programming helps children learn logic and creativity.",
            "FULL template preloads all scenes for instant transitions.",
            "LIGHT template loads only ~30% on startup, rest on demand.",
            "Each build generates a unique encryption key.",
            "The key is stored in assets with a random filename.",
            "Android's first version had no copy-paste support.",
            "The Android robot logo was created by Irina Blok."
        )
        val fact = facts[Math.abs(kotlin.random.Random.nextInt()) % facts.size]
        textView.text = "Fun fact: $fact"
    }

    private fun saveApkToDownloads(context: android.content.Context, apkFile: File) {
        val apkDir = "NeoCatroidV3"
        val fileName = apkFile.name
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$apkDir")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(apkFile).use { it.copyTo(out) }
                    }
                    ToastUtil.showSuccess(context, "APK saved to Downloads/$apkDir/")
                } else {
                    ToastUtil.showError(context, "Failed to save APK")
                }
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    apkDir
                )
                dir.mkdirs()
                val dest = File(dir, fileName)
                apkFile.copyTo(dest, true)
                ToastUtil.showSuccess(context, "APK saved: ${dest.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to save APK", e)
            ToastUtil.showError(context, "Error saving APK: ${e.message}")
        }
    }
}
