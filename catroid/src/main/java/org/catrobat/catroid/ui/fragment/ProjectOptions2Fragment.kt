package org.catrobat.catroid.ui.fragment

import android.app.AlertDialog
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
import org.catrobat.catroid.content.Project
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
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = "Опции проекта 2.0"
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
        val title1 = createCardTitle("Имя и Описание проекта")
        cardName.addView(title1)

        val nameLabel = TextView(requireContext()).apply {
            text = "Название проекта:"
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
            text = "Описание проекта:"
            setTextColor(0xFF94A3B8.toInt())
            textSize = 12f
            setPadding(0, dp10, 0, 0)
        }
        cardName.addView(descLabel)

        val descInput = EditText(requireContext()).apply {
            setText(proj.description ?: "")
            setHint("Добавьте описание Вашей игры/приложения...")
            setHintTextColor(0xFF64748B.toInt())
            setTextColor(Color.WHITE)
            textSize = 13f
            minLines = 3
            setPadding(dp10, dp10, dp10, dp10)
            setBackgroundResource(R.drawable.bg_object_thumb_cube)
        }
        cardName.addView(descInput)

        val saveInfoBtn = createActionButton("Сохранить имя и описание") {
            val newName = nameInput.text.toString().trim()
            if (newName.isNotEmpty()) {
                proj.name = newName
            }
            proj.description = descInput.text.toString().trim()
            saveProjectAsync()
            Toast.makeText(requireContext(), "Информация сохранена!", Toast.LENGTH_SHORT).show()
        }
        cardName.addView(saveInfoBtn)

        col1.addView(cardName)

        val cardDisplay = createCardLayout()
        val title2 = createCardTitle("Экран и Разрешение")
        cardDisplay.addView(title2)

        val header = proj.xmlHeader
        val curW = header?.virtualScreenWidth ?: 480
        val curH = header?.virtualScreenHeight ?: 800

        val resLabel = TextView(requireContext()).apply {
            text = "Виртуальное разрешение сцены (Ш x В):"
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

        val saveResBtn = createActionButton("Применить разрешение") {
            try {
                val w = inputW.text.toString().toInt()
                val h = inputH.text.toString().toInt()
                if (w in 64..8192 && h in 64..8192 && header != null) {
                    header.virtualScreenWidth = w
                    header.virtualScreenHeight = h
                    saveProjectAsync()
                    Toast.makeText(requireContext(), "Разрешение $w x $h установлено!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Разрешение должно быть от 64 до 8192", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Разрешение должно быть от 64 до 8192", Toast.LENGTH_SHORT).show()
            }
        }
        cardDisplay.addView(saveResBtn)

        col1.addView(cardDisplay)

        val cardExport = createCardLayout()
        val title3 = createCardTitle("Экспорт и Резервные копии")
        cardExport.addView(title3)

        val exportZipBtn = createActionButton("Экспортировать проект (.catrobat / .zip)") {
            exportProject()
        }
        cardExport.addView(exportZipBtn)

        val apkBuildBtn = createActionButton("Собрать в APK (APK Builder V3)") {
            showApkBuilderDialog()
        }
        cardExport.addView(apkBuildBtn)

        col2.addView(cardExport)

        val cardNav = createCardLayout()
        val title4 = createCardTitle("Файлы проекта 2.0")
        cardNav.addView(title4)

        val infoFiles = TextView(requireContext()).apply {
            text = "Просмотр, редактирование и управление скриптовыми и исходными файлами проекта в интерфейсе 2.0."
            setTextColor(0xFF94A3B8.toInt())
            textSize = 12f
            setPadding(0, 0, 0, dp10)
        }
        cardNav.addView(infoFiles)

        val openFilesBtn = createActionButton("Открыть Файлы проекта 2.0") {
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
            setTextColor(0xFF38BDF8.toInt())
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
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "Проект экспортирован в Download/NeoCatroidExports!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                mediaStoreUri?.let { appContext.contentResolver.delete(it, null, null) }
                activity?.runOnUiThread {
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
        return

        AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Сборщик APK V3")
            .setMessage("Функция сборки готового APK приложения для размещения или тестирования на устройстве.")
            .setPositiveButton("Собрать APK") { _, _ ->
                Toast.makeText(requireContext(), "Запуск сборки APK...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
