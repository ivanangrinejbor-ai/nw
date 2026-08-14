package org.catrobat.catroid.ui.fragment

import android.content.Context
import android.content.ContentValues
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.io.AssetConverter
import org.catrobat.catroid.io.ProjectCrypto
import org.catrobat.catroid.apkbuild.ProtectedProjectPayload
import org.catrobat.catroid.utils.lunoscript.baker.ProjectBaker
import org.catrobat.catroid.io.asynctask.ProjectExportTask
import org.catrobat.catroid.ui.sceneeditor.Ui2PanelActivity
import org.catrobat.catroid.ui.sceneeditor.ProjectSaveCoordinator
import org.catrobat.catroid.ui.fragment.ApkBuilderV3ExportDialog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectOptions2Fragment : Fragment() {

    private var project: Project? = null
    private lateinit var containerLayout: LinearLayout
    private lateinit var firebaseLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            ApkBuilderV3ExportDialog.onFirebaseUriResult(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val scrollView = ScrollView(requireContext())
        containerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        scrollView.addView(containerLayout)
        return scrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.project_options_2_title)
        project = ProjectManager.getInstance().currentProject
        buildOptionsUi()
    }

    private fun buildOptionsUi() {
        containerLayout.removeAllViews()
        val proj = project ?: return
        val density = resources.displayMetrics.density
        val dp10 = (10 * density).toInt()
        val dp12 = (12 * density).toInt()
        val dp16 = (16 * density).toInt()

        val isTablet = resources.configuration.smallestScreenWidthDp >= 600

        val mainContent = if (isTablet) {
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                weightSum = 2f
            }
        } else {
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
            }
        }

        val col1 = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = if (isTablet) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp10
                }
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        }

        val col2 = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = if (isTablet) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp10
                }
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
        }

        val cardName = createCardLayout()
        val title1 = createCardTitle(getString(R.string.project_options_2_name_description))
        cardName.addView(title1)

        val nameLabel = TextView(requireContext()).apply {
            text = getString(R.string.project_options_2_project_name)
            setTextColor(0xFF94A3B8.toInt())
            textSize = 12f
        }
        cardName.addView(nameLabel)

        val nameInput = EditText(requireContext()).apply {
            setText(proj.name)
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp10, dp10, dp10, dp10)
            setBackgroundResource(R.drawable.bg_object_thumb_cube)
        }
        cardName.addView(nameInput)

        val descLabel = TextView(requireContext()).apply {
            text = getString(R.string.project_options_2_project_description)
            setTextColor(0xFF94A3B8.toInt())
            textSize = 12f
            setPadding(0, dp10, 0, 0)
        }
        cardName.addView(descLabel)

        val descInput = EditText(requireContext()).apply {
            setText(proj.description ?: "")
            setHint(R.string.project_options_2_description_hint)
            setHintTextColor(0xFF64748B.toInt())
            setTextColor(Color.WHITE)
            textSize = 13f
            minLines = 3
            setPadding(dp10, dp10, dp10, dp10)
            setBackgroundResource(R.drawable.bg_object_thumb_cube)
        }
        cardName.addView(descInput)

        val saveInfoBtn = createActionButton(getString(R.string.project_options_2_save_name)) {
            val newName = nameInput.text.toString().trim()
            if (newName.isNotEmpty()) {
                proj.name = newName
            }
            proj.description = descInput.text.toString().trim()
            saveProjectAsync()
            Toast.makeText(requireContext(), R.string.project_options_2_info_saved, Toast.LENGTH_SHORT).show()
        }
        cardName.addView(saveInfoBtn)

        col1.addView(cardName)

        val cardDisplay = createCardLayout()
        val title2 = createCardTitle(getString(R.string.project_options_2_screen_resolution))
        cardDisplay.addView(title2)

        val header = proj.xmlHeader
        val curW = header?.virtualScreenWidth ?: 480
        val curH = header?.virtualScreenHeight ?: 800

        val resLabel = TextView(requireContext()).apply {
            text = getString(R.string.project_options_2_virtual_resolution)
            setTextColor(0xFF94A3B8.toInt())
            textSize = 12f
        }
        cardDisplay.addView(resLabel)

        val resRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp10, 0, dp10)
        }

        val inputW = EditText(requireContext()).apply {
            setText(curW.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_object_thumb_cube)
            layoutParams = LinearLayout.LayoutParams(0, dp16 * 2, 1f).apply { rightMargin = dp10 }
        }
        resRow.addView(inputW)

        val xTv = TextView(requireContext()).apply {
            text = "×"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, 0, dp10, 0)
        }
        resRow.addView(xTv)

        val inputH = EditText(requireContext()).apply {
            setText(curH.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_object_thumb_cube)
            layoutParams = LinearLayout.LayoutParams(0, dp16 * 2, 1f)
        }
        resRow.addView(inputH)

        cardDisplay.addView(resRow)

        val saveResBtn = createActionButton(getString(R.string.project_options_2_apply_resolution)) {
            try {
                val w = inputW.text.toString().toInt()
                val h = inputH.text.toString().toInt()
                if (w in 64..8192 && h in 64..8192 && header != null) {
                    header.virtualScreenWidth = w
                    header.virtualScreenHeight = h
                    saveProjectAsync()
                    Toast.makeText(requireContext(), getString(R.string.project_options_2_resolution_set, w, h), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), R.string.project_options_2_resolution_invalid, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.project_options_2_resolution_invalid, Toast.LENGTH_SHORT).show()
            }
        }
        cardDisplay.addView(saveResBtn)

        col1.addView(cardDisplay)

        val cardExport = createCardLayout()
        val title3 = createCardTitle(getString(R.string.project_options_2_export_backup))
        cardExport.addView(title3)

        val exportZipBtn = createActionButton(getString(R.string.project_options_2_export_zip)) {
            exportProject()
        }
        cardExport.addView(exportZipBtn)

        val apkBuildBtn = createActionButton(getString(R.string.project_options_2_build_apk)) {
            showApkBuilderDialog()
        }
        cardExport.addView(apkBuildBtn)

        val bakedBtn = createActionButton(getString(R.string.project_options_2_export_baked)) {
            exportBakedProject()
        }
        cardExport.addView(bakedBtn)

        val pwdBtn = createActionButton(getString(R.string.project_options_2_export_password)) {
            exportWithPassword()
        }
        cardExport.addView(pwdBtn)

        val protectedBtn = createActionButton(getString(R.string.project_options_2_export_protected)) {
            exportProtectedProject()
        }
        cardExport.addView(protectedBtn)

        val fullExportBtn = createActionButton(getString(R.string.project_options_2_full_export)) {
            (activity as? Ui2PanelActivity)?.let {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProjectOptionsFragment(), "project_options_legacy")
                    .addToBackStack("project_options_legacy")
                    .commit()
            }
        }
        cardExport.addView(fullExportBtn)

        col2.addView(cardExport)

        val cardNav = createCardLayout()
        val title4 = createCardTitle(getString(R.string.project_options_2_files_title))
        cardNav.addView(title4)

        val infoFiles = TextView(requireContext()).apply {
            text = getString(R.string.project_options_2_files_description)
            setTextColor(0xFF94A3B8.toInt())
            textSize = 12f
            setPadding(0, 0, 0, dp10)
        }
        cardNav.addView(infoFiles)

        val openFilesBtn = createActionButton(getString(R.string.project_options_2_open_files)) {
            (activity as? Ui2PanelActivity)?.let {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProjectFiles2Fragment(), "ui2_panel_fragment")
                    .commit()
            }
        }
        cardNav.addView(openFilesBtn)

        col2.addView(cardNav)

        if (isTablet) {
            mainContent.addView(col1)
            mainContent.addView(col2)
            containerLayout.addView(mainContent)
        } else {
            containerLayout.addView(col1)
            containerLayout.addView(col2)
        }
    }

    private fun createCardLayout(): LinearLayout {
        val density = resources.displayMetrics.density
        val dp12 = (12 * density).toInt()
        val dp10 = (10 * density).toInt()
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_object_card_cube)
            setPadding(dp12, dp12, dp12, dp12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp10 }
        }
    }

    private fun createCardTitle(titleText: String): TextView {
        val density = resources.displayMetrics.density
        val dp10 = (10 * density).toInt()
        return TextView(requireContext()).apply {
            text = titleText
            setTextColor(0xFF94A3B8.toInt())
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp10)
        }
    }

    private fun createActionButton(textStr: String, onClick: () -> Unit): TextView {
        val density = resources.displayMetrics.density
        val dp10 = (10 * density).toInt()
        val dp8 = (8 * density).toInt()
        return TextView(requireContext()).apply {
            text = textStr
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp10, dp10, dp10, dp10)
            setBackgroundResource(R.drawable.bg_object_card_cube)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp8 }
            setOnClickListener { onClick() }
        }
    }

    private fun saveProjectAsync() {
        val proj = project ?: return
        ProjectSaveCoordinator.saveAsync(proj)
    }

    private fun exportProject() {
        val proj = project ?: return
        val appContext = requireContext().applicationContext
        val progress = FileProgressDialog("Экспорт проекта")
        activity?.runOnUiThread { progress.show() }
        Thread {
            var mediaStoreUri: android.net.Uri? = null
            try {
                if (!ProjectSaveCoordinator.saveBlocking(proj)) {
                    error("Не удалось сохранить проект перед экспортом")
                }
                val safeName = proj.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val fileName = "${safeName}_export.catrobat"
                val output: OutputStream
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/NeoCatroidExports")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    mediaStoreUri = appContext.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                    ) ?: error("Не удалось создать файл экспорта")
                    output = appContext.contentResolver.openOutputStream(mediaStoreUri!!)
                        ?: error("Не удалось открыть файл экспорта")
                } else {
                    val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NeoCatroidExports")
                    if (!exportDir.exists() && !exportDir.mkdirs()) error("Не удалось создать папку экспорта")
                    output = FileOutputStream(File(exportDir, fileName))
                }
                ZipOutputStream(output).use { zos ->
                    proj.directory.walk().filter { it != proj.directory }.forEach { file ->
                        val entryPath = file.relativeTo(proj.directory).path
                        progress.updateFile(entryPath)
                        val zipEntry = if (file.isDirectory) ZipEntry("$entryPath/") else ZipEntry(entryPath)
                        zos.putNextEntry(zipEntry)
                        if (file.isFile) {
                            FileInputStream(file).use { fis -> fis.copyTo(zos, 8192) }
                        }
                        zos.closeEntry()
                    }
                }
                if (mediaStoreUri != null) {
                    appContext.contentResolver.update(
                        mediaStoreUri!!,
                        ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                        null,
                        null
                    )
                }
                activity?.runOnUiThread {
                    progress.dismiss()
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Проект экспортирован в Download/NeoCatroidExports!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                mediaStoreUri?.let { appContext.contentResolver.delete(it, null, null) }
                activity?.runOnUiThread {
                    progress.dismiss()
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Ошибка экспорта: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun showApkBuilderDialog() {
        val proj = project ?: return
        saveProjectAsync()
        try {
            ApkBuilderV3ExportDialog().show(this, proj.directory, firebaseLauncher)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportBakedProject() {
        val proj = project ?: return
        val appContext = requireContext().applicationContext
        Toast.makeText(requireContext(), "Запекание проекта...", Toast.LENGTH_SHORT).show()
        val progress = FileProgressDialog("Запекание проекта")
        activity?.runOnUiThread { progress.show() }
        Thread {
            try {
                if (!ProjectSaveCoordinator.saveBlocking(proj)) {
                    error("Не удалось сохранить проект перед запеканием")
                }
                val tempDir = File(appContext.cacheDir, "bake_temp")
                tempDir.deleteRecursively()
                tempDir.mkdirs()

                val initFile = File(tempDir, "init.bin")
                ProjectBaker(appContext).bakeToFile(proj, initFile)

                val imagesDir = File(tempDir, "images")
                val soundsDir = File(tempDir, "sounds")
                imagesDir.mkdirs()
                soundsDir.mkdirs()
                val sourceDir = proj.directory
                val src = File(sourceDir, "files")
                val dest = File(tempDir, "files")
                if (src.exists()) {
                    src.copyRecursively(dest, overwrite = true)
                } else {
                    dest.mkdirs()
                }

                proj.sceneList.forEach { scene ->
                    scene.spriteList.forEach { sprite ->
                        sprite.lookList.forEach { look ->
                            val f = look.file
                            if (f != null && f.exists()) {
                                f.copyTo(File(imagesDir, f.name), overwrite = true)
                            }
                        }
                        sprite.soundList.forEach { sound ->
                            val f = sound.file
                            if (f != null && f.exists()) {
                                f.copyTo(File(soundsDir, f.name), overwrite = true)
                            }
                        }
                    }
                }

                val zipFile = File(appContext.cacheDir, "${proj.name}_baked.zip")
                zipDirectoryTo(tempDir, zipFile) { progress.updateFile(it) }
                val encFile = File(appContext.cacheDir, "${proj.name}_baked.enc")
                val bakedPassword = org.catrobat.catroid.io.ProjectCrypto.generateRandomPassword()
                ProjectCrypto.encrypt(zipFile, encFile, bakedPassword)
                zipFile.delete()
                tempDir.deleteRecursively()

                writeToDownloadsFile(encFile, "${proj.name}_baked.enc")
                encFile.delete()

                activity?.runOnUiThread {
                    progress.dismiss()
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Запеченный проект сохранен в Download/NeoCatroidExports!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progress.dismiss()
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Ошибка запекания: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun exportWithPassword() {
        val proj = project ?: return
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f).toInt(), dp(8f).toInt(), dp(24f).toInt(), 0)
        }
        val pwdEdit = EditText(requireContext()).apply {
            hint = "Введите пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirmEdit = EditText(requireContext()).apply {
            hint = "Повторите пароль"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(pwdEdit)
        layout.addView(confirmEdit)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Экспорт проекта с паролем")
            .setView(layout)
            .setPositiveButton("ОК") { _, _ ->
                val pwd = pwdEdit.text.toString()
                val confirm = confirmEdit.text.toString()
                if (pwd.isEmpty() || pwd != confirm) {
                    Toast.makeText(requireContext(), "Пароли не совпадают или пусты", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val appContext = requireContext().applicationContext
                val progress = FileProgressDialog("Экспорт с паролем")
                activity?.runOnUiThread { progress.show() }
                Thread {
                    try {
                        if (!ProjectSaveCoordinator.saveBlocking(proj)) {
                            error("Не удалось сохранить проект перед экспортом")
                        }
                        val zipFile = File(appContext.cacheDir, "${proj.name}_export.zip")
                        zipDirectoryTo(proj.directory, zipFile) { progress.updateFile(it) }
                        val encFile = File(appContext.cacheDir, "${proj.name}${Constants.NPC_EXTENSION}")
                        ProjectCrypto.encrypt(zipFile, encFile, pwd)
                        zipFile.delete()
                        writeToDownloadsFile(encFile, "${proj.name}${Constants.NPC_EXTENSION}")
                        encFile.delete()
                        activity?.runOnUiThread {
                            progress.dismiss()
                            if (isAdded && context != null) {
                                Toast.makeText(requireContext(), "Экспорт с паролем сохранён в Download/NeoCatroidExports!", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            progress.dismiss()
                            if (isAdded && context != null) {
                                Toast.makeText(requireContext(), "Ошибка экспорта: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }.start()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun exportProtectedProject() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.export_protected_project)
            .setMessage(R.string.export_protected_warning)
            .setPositiveButton(android.R.string.ok) { _: android.content.DialogInterface?, _: Int ->
                doProtectedExport()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun doProtectedExport() {
        val proj = project ?: return
        proj.xmlHeader?.setProtectedProject(true)
        saveProjectAsync()
        Toast.makeText(requireContext(), "Экспорт защищённого проекта...", Toast.LENGTH_SHORT).show()
        val appContext = requireContext().applicationContext
        val progress = FileProgressDialog("Экспорт защищённого проекта")
        activity?.runOnUiThread { progress.show() }
        Thread {
            try {
                if (!ProjectSaveCoordinator.saveBlocking(proj)) {
                    error("Не удалось сохранить проект перед экспортом")
                }
                val safeName = proj.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val fileName = "${safeName}_export.catrobat"
                val output: OutputStream
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/NeoCatroidExports")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = appContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: error("Не удалось создать файл экспорта")
                    try {
                        output = appContext.contentResolver.openOutputStream(uri)
                            ?: error("Не удалось открыть файл экспорта")
                        ZipOutputStream(output).use { zos ->
                            proj.directory.walk().filter { it != proj.directory }.forEach { file ->
                                val entryPath = file.relativeTo(proj.directory).path
                                progress.updateFile(entryPath)
                                val zipEntry = if (file.isDirectory) ZipEntry("$entryPath/") else ZipEntry(entryPath)
                                zos.putNextEntry(zipEntry)
                                if (file.isFile) {
                                    FileInputStream(file).use { fis -> fis.copyTo(zos, 8192) }
                                }
                                zos.closeEntry()
                            }
                        }
                        appContext.contentResolver.update(
                            uri,
                            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                            null,
                            null
                        )
                    } catch (e: Exception) {
                        appContext.contentResolver.delete(uri, null, null)
                        throw e
                    }
                } else {
                    val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NeoCatroidExports")
                    if (!exportDir.exists() && !exportDir.mkdirs()) error("Не удалось создать папку экспорта")
                    val outputFile = File(exportDir, fileName)
                    FileOutputStream(outputFile).use { out ->
                        ZipOutputStream(out).use { zos ->
                            proj.directory.walk().filter { it != proj.directory }.forEach { file ->
                                val entryPath = file.relativeTo(proj.directory).path
                                progress.updateFile(entryPath)
                                val zipEntry = if (file.isDirectory) ZipEntry("$entryPath/") else ZipEntry(entryPath)
                                zos.putNextEntry(zipEntry)
                                if (file.isFile) {
                                    FileInputStream(file).use { fis -> fis.copyTo(zos, 8192) }
                                }
                                zos.closeEntry()
                            }
                        }
                    }
                }
                activity?.runOnUiThread {
                    progress.dismiss()
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Защищённый проект экспортирован в Download/NeoCatroidExports!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    progress.dismiss()
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Ошибка экспорта: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private fun zipDirectoryTo(sourceDir: File, destZip: File, onFile: ((String) -> Unit)? = null) {
        ZipOutputStream(FileOutputStream(destZip)).use { zos ->
            zos.setLevel(1)
            sourceDir.walk().filter { it != sourceDir }.forEach { file ->
                if (file.name != "undo_code.xml") {
                    val entryPath = file.relativeTo(sourceDir).path
                    onFile?.invoke(entryPath)
                    val zipEntry = if (file.isDirectory) ZipEntry("$entryPath/") else ZipEntry(entryPath)
                    zos.putNextEntry(zipEntry)
                    if (file.isFile) {
                        FileInputStream(file).use { fis -> fis.copyTo(zos, 8192) }
                    }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun writeToDownloadsFile(sourceFile: File, fileName: String) {
        val appContext = requireContext().applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/NeoCatroidExports")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = appContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Не удалось создать файл экспорта")
            try {
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { fis -> fis.copyTo(out, 8192) }
                } ?: error("Не удалось открыть файл экспорта")
                appContext.contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                    null,
                    null
                )
            } catch (e: Exception) {
                appContext.contentResolver.delete(uri, null, null)
                throw e
            }
        } else {
            val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NeoCatroidExports")
            if (!exportDir.exists() && !exportDir.mkdirs()) error("Не удалось создать папку экспорта")
            sourceFile.copyTo(File(exportDir, fileName), overwrite = true)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private inner class FileProgressDialog(private val title: String) {
        private var dialog: androidx.appcompat.app.AlertDialog? = null
        private var progressBar: android.widget.ProgressBar? = null
        private var fileNameView: TextView? = null
        private val handler = android.os.Handler(android.os.Looper.getMainLooper())

        fun show() {
            val d = resources.displayMetrics.density
            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding((24 * d).toInt(), (16 * d).toInt(), (24 * d).toInt(), 0)
            }
            val nameLabel = TextView(requireContext()).apply {
                text = "Обрабатывается:"
                setTextColor(0xFF94A3B8.toInt())
                textSize = 13f
            }
            val nameView = TextView(requireContext()).apply {
                text = ""
                setTextColor(0xFF38BDF8.toInt())
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                setPadding(0, (6 * d).toInt(), 0, (8 * d).toInt())
            }
            val bar = android.widget.ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                isIndeterminate = true
            }
            fileNameView = nameView
            progressBar = bar
            layout.addView(nameLabel)
            layout.addView(nameView)
            layout.addView(bar)
            dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(layout)
                .setCancelable(false)
                .create()
            dialog?.show()
        }

        fun updateFile(name: String) {
            handler.post { fileNameView?.text = name }
        }

        fun dismiss() {
            handler.post {
                if (dialog?.isShowing == true) {
                    dialog?.dismiss()
                }
            }
        }
    }
}
