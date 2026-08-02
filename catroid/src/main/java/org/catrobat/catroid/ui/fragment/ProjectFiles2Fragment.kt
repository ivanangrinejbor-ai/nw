package org.catrobat.catroid.ui.fragment

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.utils.SimpleTextEditorActivity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectFiles2Fragment : Fragment() {

    private var project: Project? = null
    private lateinit var containerLayout: LinearLayout
    private val ADD_FILE_REQUEST = 2026

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
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = "Файлы проекта 2.0"
        project = ProjectManager.getInstance().currentProject
        refreshFiles()
    }

    fun refreshFiles() {
        containerLayout.removeAllViews()
        val density = resources.displayMetrics.density
        val dp8 = (8 * density).toInt()
        val dp10 = (10 * density).toInt()
        val dp12 = (12 * density).toInt()

        val actionsRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp10 }
        }

        val createBtn = TextView(requireContext()).apply {
            text = "Создать"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp10, dp10, dp10, dp10)
            setBackgroundResource(R.drawable.bg_object_card_cube)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp8
            }
            setOnClickListener { showCreateFileDialog() }
        }
        actionsRow.addView(createBtn)

        val importBtn = TextView(requireContext()).apply {
            text = "Импорт"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp10, dp10, dp10, dp10)
            setBackgroundResource(R.drawable.bg_object_card_cube)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp8
            }
            setOnClickListener { handleImportFile() }
        }
        actionsRow.addView(importBtn)

        val cmdBtn = TextView(requireContext()).apply {
            text = "Терминал"
            setTextColor(0xFF38BDF8.toInt())
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp10, dp10, dp10, dp10)
            setBackgroundResource(R.drawable.bg_object_card_cube)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { openTerminal() }
        }
        actionsRow.addView(cmdBtn)

        containerLayout.addView(actionsRow)

        val proj = project ?: return
        val filesDir = File(proj.directory, "files")
        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }

        val files = filesDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (files.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "В папке проекта пока нет файлов"
                setTextColor(0xFF94A3B8.toInt())
                textSize = 14f
                setPadding(dp12, dp12, dp12, dp12)
            }
            containerLayout.addView(emptyTv)
            return
        }

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        for (file in files) {
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_object_card_cube)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp10, dp10, dp10, dp10)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp10 }
            }

            val ext = file.extension.uppercase(Locale.getDefault())
            val badge = TextView(requireContext()).apply {
                text = if (ext.isNotEmpty()) (if (ext.length <= 4) ext else ext.substring(0, 4)) else "FILE"
                setTextColor(Color.WHITE)
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_object_thumb_cube)
                val badgeSize = (44 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(badgeSize, badgeSize).apply {
                    rightMargin = dp10
                }
            }
            card.addView(badge)

            val textLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameTv = TextView(requireContext()).apply {
                text = file.name
                setTextColor(0xFFF8FAFC.toInt())
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
            }
            textLayout.addView(nameTv)

            val infoTv = TextView(requireContext()).apply {
                text = "Изменено: ${sdf.format(Date(file.lastModified()))} • ${formatBytes(file.length())}"
                setTextColor(0xFF94A3B8.toInt())
                textSize = 11f
            }
            textLayout.addView(infoTv)

            card.addView(textLayout)

            card.setOnClickListener { openFile(file) }
            card.setOnLongClickListener {
                showFileActions(file)
                true
            }

            containerLayout.addView(card)
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0f)
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0f * 1024.0f))
    }

    private fun showCreateFileDialog() {
        val input = EditText(requireContext()).apply {
            hint = "script.py / data.json / note.txt"
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Новый файл 2.0")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val proj = project ?: return@setPositiveButton
                    val target = safeProjectFile(name)
                    if (target == null) {
                        Toast.makeText(requireContext(), "Недопустимое имя файла", Toast.LENGTH_SHORT).show()
                    } else if (!target.exists()) {
                        target.createNewFile()
                        refreshFiles()
                        openFile(target)
                    } else {
                        Toast.makeText(requireContext(), "Файл уже существует", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun handleImportFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        val chooser = Intent.createChooser(intent, "Выберите файл для импорта")
        startActivityForResult(chooser, ADD_FILE_REQUEST)
    }

    private fun openTerminal() {
        val proj = project ?: return
        val filesDir = File(proj.directory, "files")
        if (!filesDir.exists()) filesDir.mkdirs()

        val dialog = CommandPromptDialogFragment.newInstance(filesDir.absolutePath)
        dialog.show(parentFragmentManager, CommandPromptDialogFragment.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                saveUriToProjectFiles(uri)
            }
        }
    }

    private fun saveUriToProjectFiles(uri: Uri) {
        val proj = project ?: return
        val fileName = getFileName(uri)
        val filesDir = File(proj.directory, "files")
        if (!filesDir.exists()) filesDir.mkdirs()

        val destinationFile = safeProjectFile(fileName)
        if (destinationFile == null) {
            Toast.makeText(requireContext(), "Недопустимое имя импортируемого файла", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return
            FileOutputStream(destinationFile).use { outputStream ->
                inputStream.use { input -> input.copyTo(outputStream) }
            }
            if (isAdded && context != null) {
                refreshFiles()
                Toast.makeText(requireContext(), "Файл импортирован!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            if (isAdded && context != null) {
                Toast.makeText(requireContext(), "Ошибка импорта: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = ""
        if (uri.scheme == "content") {
            val cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        } else if (uri.scheme == "file") {
            fileName = File(uri.path ?: "").name
        }
        return fileName.ifEmpty { "imported_file_${System.currentTimeMillis()}" }
    }

    private fun safeProjectFile(rawName: String): File? {
        val name = rawName.trim()
        if (name.isEmpty() || name == "." || name == ".." || name.contains('/') || name.contains('\\')) {
            return null
        }
        val proj = project ?: return null
        val root = File(proj.directory, "files")
        if (!root.exists()) root.mkdirs()
        return try {
            val rootCanonical = root.canonicalFile
            val candidate = File(rootCanonical, name).canonicalFile
            if (candidate.parentFile == rootCanonical) candidate else null
        } catch (_: Exception) {
            null
        }
    }

    private fun openFile(file: File) {
        val ext = file.extension.lowercase(Locale.getDefault())
        val editable = listOf("txt", "py", "json", "xml", "lua", "md", "csv", "log", "rscene", "js", "html")

        if (ext in editable) {
            val intent = Intent(requireContext(), SimpleTextEditorActivity::class.java)
            intent.putExtra("FILE_PATH", file.absolutePath)
            startActivity(intent)
        } else {
            try {
                val authority = "${requireContext().packageName}.fileProvider"
                val uri = FileProvider.getUriForFile(requireContext(), authority, file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, context?.contentResolver?.getType(uri) ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Нечем открыть данный файл", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFileActions(file: File) {
        val items = arrayOf("Открыть / Редактировать", "Скопировать путь", "Удалить")
        AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(file.name)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openFile(file)
                    1 -> {
                        val cb = CatroidApplication.getAppContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("path", file.name))
                        Toast.makeText(requireContext(), "Имя файла скопировано", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        file.delete()
                        refreshFiles()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
