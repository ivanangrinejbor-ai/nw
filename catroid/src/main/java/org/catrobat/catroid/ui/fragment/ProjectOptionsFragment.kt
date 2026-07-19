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
package org.catrobat.catroid.ui.fragment

import android.Manifest.permission
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.text.Editable
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.danvexteam.lunoscript_annotations.LunoClass
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.RadioGroup
import android.widget.RadioButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.apkbuild.ApkToolboxManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.Nameable
import org.catrobat.catroid.common.ProjectData
import org.catrobat.catroid.common.ScreenModes
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.XmlHeader
import org.catrobat.catroid.databinding.FragmentProjectOptionsBinding
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.io.asynctask.ProjectExportTask
import org.catrobat.catroid.io.asynctask.ProjectSaver
import org.catrobat.catroid.io.asynctask.loadProject
import org.catrobat.catroid.io.asynctask.renameProject
import org.catrobat.catroid.io.asynctask.saveProjectSerial
import org.catrobat.catroid.merge.NewProjectNameTextWatcher
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.BottomBar.hideBottomBar
import org.catrobat.catroid.ui.PROJECT_DIR
import org.catrobat.catroid.ui.ProjectUploadActivity
import org.catrobat.catroid.ui.SettingsActivity
import org.catrobat.catroid.ui.runtimepermissions.RequiresPermissionTask
import org.catrobat.catroid.utils.ToastUtil
import org.catrobat.catroid.apkbuild.ApkBuildService
import org.catrobat.catroid.apkbuild.BakedApkBuilder
import org.catrobat.catroid.apkbuild.AlignedApkBuilder
import org.catrobat.catroid.ui.fragment.ApkBuilderV3ExportDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.catrobat.catroid.utils.Utils
import org.catrobat.catroid.utils.git.GitController
import org.catrobat.catroid.utils.git.GitResult
import org.catrobat.catroid.utils.git.TokenManager
import org.catrobat.catroid.utils.lunoscript.baker.ProjectBaker
import org.catrobat.catroid.utils.lunoscript.security.LunoSecurity
import org.catrobat.catroid.utils.notifications.StatusBarNotificationManager
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import com.android.apksig.ApkSigner
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import kotlin.random.Random

@LunoClass
class ProjectOptionsFragment : Fragment() {

    private val projectManager: ProjectManager by inject()
    private var _binding: FragmentProjectOptionsBinding? = null
    private val binding get() = _binding!!
    private var project: Project? = null
    private var sceneName: String? = null
    private var projectInZip: File? = null
    private var buildFilename: String? = null
    private var zipTempDir: File? = null
    private var encFileToSave: File? = null
    private lateinit var gitController: GitController
    private var progressDialog: AlertDialog? = null

    private lateinit var firebaseLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            ApkBuilderV3ExportDialog.onFirebaseUriResult(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        encFileToSave?.let { outState.putString(KEY_ENC_FILE, it.absolutePath) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.getString(KEY_ENC_FILE)?.let { encFileToSave = File(it) }
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.project_options)

        project = projectManager.currentProject
        sceneName = projectManager.currentlyEditedScene.name

        gitController = GitController(project!!.directory)

        setupNameInputLayout()
        setupPhysicsInputLayout()
        setupDescriptionInputLayout()
        setupNotesAndCreditsInputLayout()
        addTags()
        setupProjectAspectRatio()
        setupCustomResolution()
        setupPreloader()
        setupProjectUpload()
        setupExportMenu()
        setupClearVars()
        setupChangeIcon()
        setupChangeOrientation()
        setupProjectMoreDetails()
        setupProjectOptionDelete()
        setupMishkFrede()

        setupRebuildCache()

        setupGitButtons()


        hideBottomBar(requireActivity())
    }

    private fun setupExportMenu() {
        val exportBtn = view?.findViewById<android.widget.TextView>(R.id.project_options_export)
        exportBtn?.setOnClickListener {
            val items = arrayOf(
                getString(R.string.export_bake),
                getString(R.string.export_project),
                getString(R.string.export_with_password),
                getString(R.string.export_as_exe),
                getString(R.string.export_as_apk_v3)
            )
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.export_project)
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> runExportWalkthrough { exportBakedProject() }
                        1 -> runExportWalkthrough { exportProject() }
                        2 -> runExportWalkthrough { exportWithPassword() }
                        3 -> {
                            android.widget.Toast.makeText(
                                requireContext(),
                                getString(R.string.export_exe_2d_only_warning),
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            runExportWalkthrough { buildExe() }
                        }
                        4 -> buildApkV3()
                    }
                }
                .show()
        }
    }



    private fun exportBakedProject() {
        saveProject()
        project ?: return

        showProgressDialog("Запекание проекта...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tempDir = File(requireContext().cacheDir, "bake_temp")
                tempDir.deleteRecursively()
                tempDir.mkdirs()

                val initFile = File(tempDir, "init.bin")
                org.catrobat.catroid.utils.lunoscript.baker.ProjectBaker(requireContext())
                    .bakeToFile(project!!, initFile)



                val imagesDir = File(tempDir, "images")
                val soundsDir = File(tempDir, "sounds")
                imagesDir.mkdirs()
                soundsDir.mkdirs()
                val sourceDir = project!!.directory

                val foldersToCopy = listOf("files")

                for (folderName in foldersToCopy) {
                    val src = File(sourceDir, folderName)
                    val dest = File(tempDir, folderName)

                    if (src.exists()) {
                        src.copyRecursively(dest, overwrite = true)
                    } else {
                        dest.mkdirs()
                    }
                }

                project!!.sceneList.forEach { scene ->
                    scene.spriteList.forEach { sprite ->
                        sprite.lookList.forEach { look ->
                            val src = look.file
                            if (src != null && src.exists()) {
                                src.copyTo(File(imagesDir, src.name), overwrite = true)
                            }
                        }
                        sprite.soundList.forEach { sound ->
                            val src = sound.file
                            if (src != null && src.exists()) {
                                src.copyTo(File(soundsDir, src.name), overwrite = true)
                            }
                        }
                    }
                }

                val zipFile = File(requireContext().cacheDir, "${project!!.name}_baked.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    zipDirectory3(tempDir, zos)
                }

                // Зашифровать запечённый проект тем же статическим ключом, что и EXE
                // (AES-256-GCM, магия NCPP). Обычный unzip не даст содержимого;
                // импорт расшифрует по той же константе.
                val encFile = File(requireContext().cacheDir, "${project!!.name}_baked.enc")
                org.catrobat.catroid.io.ProjectCrypto.encrypt(
                    zipFile,
                    encFile,
                    org.catrobat.catroid.apkbuild.ProtectedProjectPayload.PASSWORD
                )
                zipFile.delete()
                tempDir.deleteRecursively()

                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    shareFile(encFile)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    ToastUtil.showError(requireContext(), "Ошибка: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun shareFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".fileProvider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = if (file.name.endsWith(".enc", true)) "application/octet-stream" else "application/zip"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Сохранить запеченный проект"))
    }

    private fun setupRebuildCache() {
        binding.projectOptionsRebuildCache.setOnClickListener {
            showRebuildConfirmationDialog()
        }
    }

    private fun showRebuildConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Обновить кэш физики?")
            .setMessage("Это может занять некоторое время, но исправит проблемы с хитбоксами в старых проектах. Продолжить?")
            .setPositiveButton("Да") { _, _ ->
                startCacheRebuilding()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun startCacheRebuilding() {
        project ?: return

        val allLooks = mutableListOf<LookData>()
        project!!.sceneList.forEach { scene ->
            scene.spriteList.forEach { sprite ->
                allLooks.addAll(sprite.lookList)
            }
        }

        if (allLooks.isEmpty()) {
            ToastUtil.showInfoLong(requireContext(), "В проекте нет образов для обработки.")
            return
        }

        val progressDialog = ProgressDialog(requireContext()).apply {
            setTitle("Обновление кэша")
            setMessage("Обработка образов...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = allLooks.size
            progress = 0
            setCancelable(false)
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            var successCount = 0
            allLooks.forEachIndexed { index, lookData ->
                withContext(Dispatchers.Main) {
                    progressDialog.setMessage("Обработка: ${lookData.name}")
                }

                val success = lookData.collisionInformation.forceRecalculateAndSave()
                if (success) {
                    successCount++
                }

                withContext(Dispatchers.Main) {
                    progressDialog.progress = index + 1
                }
            }

            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                ToastUtil.showSuccess(requireContext(), "Готово! Обработано $successCount из ${allLooks.size} образов.")
            }
        }
    }

    private fun setupGitButtons() {
        updateGitButtonsVisibility()

        binding.gitConnectButton.setOnClickListener { handleGitConnect() }
        binding.gitPublishButton.setOnClickListener { showCommitMessageDialog() }
    }

    private fun updateGitButtonsVisibility() {
        val remoteUrl = project?.xmlHeader?.gitRemoteUrl
        val isConnected = !remoteUrl.isNullOrEmpty()

        binding.gitConnectButton.visibility = if (isConnected) View.GONE else View.VISIBLE
        binding.gitPublishButton.visibility = if (isConnected) View.VISIBLE else View.GONE
    }

    private fun handleGitConnect() {
        if (TokenManager.getToken(requireContext()) == null) {
            showLoginRequiredDialog()
            return
        }
        showGitConnectDialog()
    }

    private fun showLoginRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Требуется вход")
            .setMessage("Для подключения проекта к Git необходимо войти в свой аккаунт GitHub в настройках приложения.")
            .setPositiveButton("В настройки") { _, _ ->
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showGitConnectDialog() {
        val options = arrayOf("Создать новый репозиторий", "Подключиться к существующему")
        AlertDialog.Builder(requireContext())
            .setTitle("Подключение к Git")
            .setItems(options) { _, which ->
                if (which == 0) {
                    showCreateRepoDialog()
                } else {
                    showCloneRepoDialog()
                }
            }
            .show()
    }

    private fun showCreateRepoDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_repo, null)
        val repoNameEditText = dialogView.findViewById<TextInputEditText>(R.id.repo_name_edit_text)
        val privateSwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.repo_private_switch)

        AlertDialog.Builder(requireContext())
            .setTitle("Создание нового репозитория")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                val repoName = repoNameEditText.text.toString().trim()
                if (repoName.isEmpty()) {
                    ToastUtil.showError(requireContext(), "Имя репозитория не может быть пустым")
                    return@setPositiveButton
                }
                val isPrivate = privateSwitch.isChecked
                val token = TokenManager.getToken(requireContext()) ?: return@setPositiveButton

                showProgressDialog("Создание репозитория...")
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = gitController.initializeAndPushNewRepository(token, repoName, isPrivate)
                    withContext(Dispatchers.Main) {
                        hideProgressDialog()
                        when (result) {
                            is GitResult.Success -> {
                                ToastUtil.showSuccess(requireContext(), "Проект успешно опубликован!")
                                project?.xmlHeader?.gitRemoteUrl = result.data
                                saveProject()
                                updateGitButtonsVisibility()
                            }
                            is GitResult.Error -> ToastUtil.showError(requireContext(), "Ошибка: ${result.message}")
                            else -> {}
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showCloneRepoDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_clone_repo, null)
        val repoUrlEditText = dialogView.findViewById<TextInputEditText>(R.id.repo_url_edit_text)

        AlertDialog.Builder(requireContext())
            .setTitle("Подключение к репозиторию")
            .setView(dialogView)
            .setMessage("Внимание! Текущие файлы проекта будут ЗАМЕНЕНЫ файлами из удаленного репозитория. Это действие необратимо.")
            .setPositiveButton("Подключить") { _, _ ->
                val repoUrl = repoUrlEditText.text.toString().trim()
                if (!repoUrl.startsWith("https://") || !repoUrl.endsWith(".git")) {
                    ToastUtil.showError(requireContext(), "Введите корректный HTTPS URL репозитория")
                    return@setPositiveButton
                }
                val token = TokenManager.getToken(requireContext()) ?: return@setPositiveButton
                val originalProjectDir = project?.directory ?: return@setPositiveButton

                val tempDir = File(requireContext().cacheDir, "git_clone_temp_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                showProgressDialog("Клонирование проекта...")
                lifecycleScope.launch(Dispatchers.IO) {

                    val result = gitController.cloneRepository(repoUrl, token, tempDir)

                    var finalResult: GitResult<Unit> = GitResult.Error("Operation failed before replacement.")
                    if (result is GitResult.Success) {
                        try {

                            originalProjectDir.deleteRecursively()
                            if (!tempDir.renameTo(originalProjectDir)) {
                                throw IOException("Failed to replace project directory.")
                            }



                            val clonedProject = XstreamSerializer.getInstance().loadProject(originalProjectDir, requireContext())
                                ?: throw IOException("Failed to load cloned project from disk.")


                            clonedProject.xmlHeader.gitRemoteUrl = repoUrl
                            XstreamSerializer.getInstance().saveProject(clonedProject)


                            projectManager.currentProject = clonedProject
                            project = clonedProject

                            clonedProject.xmlHeader.gitRemoteUrl = repoUrl
                            XstreamSerializer.getInstance().saveProject(clonedProject)

                            finalResult = GitResult.Success(Unit)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            tempDir.deleteRecursively()
                            finalResult = GitResult.Error("Failed to replace or load project: ${e.message}")
                        }
                    } else {
                        finalResult = result
                    }

                    withContext(Dispatchers.Main) {
                        hideProgressDialog()
                        when (finalResult) {
                            is GitResult.Success -> {
                                ToastUtil.showSuccess(requireContext(), "Проект успешно склонирован!")
                                projectManager.loadProject(originalProjectDir)


                                projectManager.currentProject.xmlHeader.gitRemoteUrl = repoUrl

                                project = projectManager.currentProject
                                shouldSaveOnPause = true
                                showProjectReloadDialog()
                            }
                            is GitResult.Error -> {
                                tempDir.deleteRecursively()
                                ToastUtil.showError(requireContext(), "Ошибка: ${finalResult.message}")
                            }
                            else -> {}
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showCommitMessageDialog() {
        val editText = TextInputEditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Синхронизировать")
            .setMessage("Введите краткое описание сделанных изменений (коммит):")
            .setView(editText)
            .setPositiveButton("Начать") { _, _ ->
                val commitMessage = editText.text.toString().ifEmpty { "Update project" }
                handleGitPublish(commitMessage)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private var shouldSaveOnPause = true

    private fun handleGitPublish(commitMessage: String) {
        val token = TokenManager.getToken(requireContext()) ?: return
        showProgressDialog("Синхронизация и публикация...")
        lifecycleScope.launch(Dispatchers.IO) {
            saveProject()
            val result = gitController.commitAndPush(commitMessage, "NeoCatroid_user", "nc_user@email.com", token)
            withContext(Dispatchers.Main) {
                hideProgressDialog()
                when (result) {
                    is GitResult.Success -> ToastUtil.showSuccess(requireContext(), "Синхронизация завершена!")
                    is GitResult.Error -> ToastUtil.showError(requireContext(), "Ошибка: ${result.message}")
                    else -> {}
                }
            }
        }
    }

    private fun handleGitUpdate() {
        val token = TokenManager.getToken(requireContext()) ?: return
        showProgressDialog("Обновление проекта...")
        lifecycleScope.launch(Dispatchers.IO) {
            val result = gitController.pullAndMerge(token)
            withContext(Dispatchers.Main) {
                hideProgressDialog()
                when (result) {
                    is GitResult.Success -> {
                        ToastUtil.showSuccess(requireContext(), "Проект обновлен!")
                        shouldSaveOnPause = false




                        val mergedProject = result.data.mergedProject
                        project = mergedProject
                        projectManager.currentProject = mergedProject


                        if (result.data.conflicts.isNotEmpty()) {
                            val conflictsString = result.data.conflicts.joinToString("\n") { "- ${it.path}" }
                            AlertDialog.Builder(requireContext())
                                .setTitle("Обнаружены конфликты")
                                .setMessage("Система обнаружила конфликты, думайте сами какой вариант оставить. Конфликты:\n$conflictsString")
                                .setPositiveButton("OK") { _, _ ->
                                    showProjectReloadDialog()
                                }
                                .show()
                        } else {
                            showProjectReloadDialog()
                        }
                    }
                    is GitResult.Error -> ToastUtil.showError(requireContext(), "Ошибка: ${result.message}")
                    is GitResult.MergeConflict -> {
                        val conflictsString = result.conflicts.joinToString("\n") { "- ${it.fieldName}" }
                        AlertDialog.Builder(requireContext())
                            .setTitle("Конфликты слияния!")
                            .setMessage("Не удалось автоматически обновить проект. Конфликты:\n$conflictsString")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }
    }

    private fun showProgressDialog(message: String) {
        progressDialog = AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setView(R.layout.dialog_progress)
            .setMessage(message)
            .show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
    }

    private fun showProjectReloadDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Требуется перезагрузка")
            .setMessage("Проект был изменен. Чтобы увидеть изменения, необходимо его перезапустить.")
            .setPositiveButton("Перезапустить") { _, _ ->

                requireActivity().finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupMishkFrede() {
    }

    private fun setupNameInputLayout() {
        binding.projectOptionsNameLayout.editText?.apply {
            setText(project?.name)
            addTextChangedListener(object : NewProjectNameTextWatcher<Nameable>() {
                override fun afterTextChanged(s: Editable?) {
                    val error = if (s.toString() != project!!.name) {
                        validateInput(s.toString(), getContext())
                    } else {
                        null
                    }
                    binding.projectOptionsNameLayout.error = error
                }
            })
        }
    }

    private fun setupPhysicsInputLayout() {
        val xml: XmlHeader? = project?.xmlHeader
        binding.projectOptionsPhysicsWidthLayout.editText?.apply {
            setText(xml?.getPhysicsWidthArea().toString())
            addTextChangedListener(object : NewProjectNameTextWatcher<Nameable>() {
                override fun afterTextChanged(s: Editable?) {
                    val error = if (s.toString() != project!!.name) {
                        validatePhysicsInput(s.toString(), getContext())
                    } else {
                        null
                    }
                    binding.projectOptionsPhysicsWidthLayout.error = error
                }
            })
        }
        binding.projectOptionsPhysicsHeightLayout.editText?.apply {
            setText(xml?.getPhysicsHeightArea().toString())
            addTextChangedListener(object : NewProjectNameTextWatcher<Nameable>() {
                override fun afterTextChanged(s: Editable?) {
                    val error = if (s.toString() != project!!.name) {
                        validatePhysicsInput(s.toString(), getContext())
                    } else {
                        null
                    }
                    binding.projectOptionsPhysicsHeightLayout.error = error
                }
            })
        }
    }

    private fun setupDescriptionInputLayout() {
        binding.projectOptionsDescriptionLayout.editText?.setText(project?.description)

        binding.projectOptionsDescriptionLayout.setEndIconOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val projectDir = project?.filesDir ?: return
        val imageFiles = projectDir.listFiles { file ->
            file.isFile && file.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp", "gif")
        }

        if (imageFiles.isNullOrEmpty()) {
            ToastUtil.showInfoLong(requireContext(), "В проекте нет изображений.")
            return
        }

        val imageFileNames = imageFiles.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Вставить изображение")
            .setItems(imageFileNames) { _, which ->
                val selectedFileName = imageFileNames[which]
                insertImageMarkdown(selectedFileName)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun insertImageMarkdown(fileName: String) {
        val editText = binding.projectOptionsDescriptionLayout.editText ?: return
        val markdownToInsert = "\n![${fileName}]($fileName)\n"

        val cursorPosition = editText.selectionStart
        editText.text?.insert(cursorPosition, markdownToInsert)
    }

    private fun setupNotesAndCreditsInputLayout() {
        binding.projectOptionsNotesAndCreditsLayout.editText?.setText(project?.notesAndCredits)
    }

    private fun addTags() {
        binding.chipGroupTags.removeAllViews()
        val tags = project!!.tags

        if (tags.size == 1 && tags[0].isEmpty()) {
            binding.tags.visibility = View.GONE
            return
        }
        binding.tags.visibility = View.VISIBLE
        for (tag in tags) {
            val chip = Chip(context)
            chip.text = tag
            chip.isClickable = false
            binding.chipGroupTags.addView(chip)
        }
    }

    private fun setupProjectAspectRatio() {
        binding.projectOptionsAspectRatio.apply {
            isChecked = project?.screenMode == ScreenModes.MAXIMIZE
            setOnCheckedChangeListener { _, isChecked ->
                handleAspectRatioChecked(isChecked)
            }
        }
    }

    private fun setupCustomResolution() {
        binding.projectOptionsCustomResolution.apply {
            isChecked = project?.xmlHeader?.customResolution == true
            setOnCheckedChangeListener { _, isChecked ->
                handleCustomResolutionChecked(isChecked)
            }
        }
    }

    private fun setupPreloader() {
        binding.projectOptionsPreloader.apply {
            isChecked = project?.xmlHeader?.isPreloaderEnabled == true
            setOnCheckedChangeListener { _, isChecked ->
                project?.xmlHeader?.setPreloaderEnabled(isChecked)
            }
        }
    }

    private fun setupProjectUpload() {
        binding.projectOptionsUpload.setOnClickListener {
            exportMatryoshkaForServer()
        }
    }

    private fun exportMatryoshkaForServer() {
        saveProject()
        val currentProject = project ?: return

        showProgressDialog("Сборка матрешки для сервера...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val readyToUploadZip = org.catrobat.catroid.utils.MatryoshkaManager.packForUpload(requireContext(), currentProject)

                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    ToastUtil.showSuccess(requireContext(), "Матрешка собрана!")
                    shareFile(readyToUploadZip)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    ToastUtil.showError(requireContext(), "Ошибка: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }



    /**
     * Mini progress menu shown while an export begins: walks the project's
     * scenes, their objects and sounds, showing the current item, then starts
     * the real export (save to device). No upfront "what will be exported" list.
     */
    private fun runExportWalkthrough(onExport: () -> Unit) {
        saveProject()
        val proj = project ?: return
        val context: Context = requireContext()
        val density = resources.displayMetrics.density
        val pad = (24 * density).toInt()

        val status = TextView(context).apply {
            textSize = 15f
            gravity = android.view.Gravity.CENTER
            setPadding(pad, pad, pad, pad)
            setText(R.string.export_exporting)
        }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.export_project)
            .setView(status)
            .setCancelable(false)
            .create()
        dialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            val scenes = proj.sceneList
            if (scenes.isEmpty()) {
                withContext(Dispatchers.Main) { status.setText(R.string.export_summary_empty) }
                delay(600)
            }
            for (scene in scenes) {
                withContext(Dispatchers.Main) {
                    status.text = "${getString(R.string.export_summary_scene)}: ${scene.name}"
                }
                delay(120)
                for (sprite in scene.spriteList) {
                    withContext(Dispatchers.Main) {
                        status.text = "${getString(R.string.export_summary_object)}: ${sprite.name}"
                    }
                    delay(60)
                    for (sound in sprite.soundList) {
                        withContext(Dispatchers.Main) {
                            status.text = "${getString(R.string.export_summary_sound)}: ${sound.name}"
                        }
                        delay(30)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                onExport()
            }
        }
    }

    private fun setupClearVars() {
        binding.projectOptionsClearVars.setOnClickListener {
            clearVars()
        }
    }

    private fun setupChangeIcon() {
        binding.projectOptionsChangeIcon.setOnClickListener {
            changeIcon()
        }
    }

    private fun setupChangeOrientation() {
        binding.projectOptionsChangeOrientation.setOnClickListener {
            changeOrientation()
        }
    }



    private fun setupProjectMoreDetails() {
        binding.projectOptionsMoreDetails.setOnClickListener {
            moreDetails()
        }
    }

    private fun setupProjectOptionDelete() {
        binding.projectOptionsDelete.setOnClickListener {
            handleDeleteButtonPressed()
        }
    }

    private fun handleAspectRatioChecked(checked: Boolean) {
        project?.screenMode = if (checked) {
            ScreenModes.MAXIMIZE
        } else {
            ScreenModes.STRETCH
        }
    }

    private fun handleCustomResolutionChecked(checked: Boolean) {
        project?.xmlHeader?.setCustomResolution(checked)
    }

    private fun handleDeleteButtonPressed() {
        val currentProject = project ?: return

        org.catrobat.catroid.utils.ProjectTrashManager.showDeleteProjectDialog(
            requireContext(),
            currentProject.directory
        ) {
            project = null
            projectManager.currentProject = null
            requireActivity().onBackPressed()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        for (index in 0 until menu.size()) {
            menu.getItem(index).isVisible = false
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onPause() {
        if (shouldSaveOnPause) {
            saveProject()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        progressDialog?.dismiss()
        progressDialog = null
        _binding = null
        super.onDestroyView()
    }

    private fun saveProject() {
        project ?: return
        setProjectName()
        setPhysicsArea()
        saveDescription()
        saveCreditsAndNotes()
        saveProjectSerial(project, requireContext())
    }

    override fun onResume() {
        super.onResume()

        projectManager.currentProject = project
        binding.projectOptionsNameLayout.editText?.setText(project?.name)
        setupDescriptionInputLayout()
        setupNotesAndCreditsInputLayout()

        // Sync switch states from project
        setupPreloader()

        addTags()
        hideBottomBar(requireActivity())
    }

    private fun setProjectName() {
        val name = binding.projectOptionsNameLayout.editText?.text.toString().trim()
        project ?: return

        if (project!!.name != name) {
            XstreamSerializer.getInstance().saveProject(project)
            val renamedDirectory = renameProject(project!!.directory, name)
            if (renamedDirectory == null) {
                Log.e(TAG, "Creating renamed directory failed!")
                return
            }
            loadProject(renamedDirectory, requireContext().applicationContext)
            project = projectManager.currentProject
            projectManager.currentlyEditedScene = project!!.getSceneByName(sceneName)
        }
    }

    private fun setPhysicsArea() {
        project ?: return
        val xml = project?.xmlHeader ?: return
        try {
            val width = binding.projectOptionsPhysicsWidthLayout.editText?.text.toString().toFloat()
            val height = binding.projectOptionsPhysicsHeightLayout.editText?.text.toString().toFloat()
            xml.setPhysicsWidthArea(width)
            xml.setPhysicsHeightArea(height)
        } catch (e: NumberFormatException) {
            // Empty or invalid physics fields — leave defaults
        }
    }

    fun saveDescription() {
        val description = binding.projectOptionsDescriptionLayout.editText?.text.toString().trim()
        if (project?.description == null || project?.description != description) {
            project?.description = description
            if (!XstreamSerializer.getInstance().saveProject(project)) {
                ToastUtil.showError(activity, R.string.error_set_description)
            }
        }
    }

    fun saveCreditsAndNotes() {
        val notesAndCredits = binding.projectOptionsNotesAndCreditsLayout.editText
            ?.text.toString().trim()
        if (project?.notesAndCredits == null || project?.notesAndCredits != notesAndCredits) {
            project?.notesAndCredits = notesAndCredits
            if (!XstreamSerializer.getInstance().saveProject(project)) {
                ToastUtil.showError(requireContext(), R.string.error_set_notes_and_credits)
            }
        }
    }

    fun projectUpload() {
        val currentProject = projectManager.currentProject
        ProjectSaver(currentProject, requireContext())
            .saveProjectAsync({ onSaveProjectComplete() })
        Utils.setLastUsedProjectName(requireContext(), currentProject.name)
    }

    private fun onSaveProjectComplete() {
        val currentProject = projectManager.currentProject

        if (Utils.isDefaultProject(currentProject, activity)) {
            binding.root.apply {
                Snackbar.make(binding.root, R.string.error_upload_default_project, Snackbar.LENGTH_LONG).show()
            }
            return
        }

        val intent = Intent(requireContext(), ProjectUploadActivity::class.java)
        intent.putExtra(PROJECT_DIR, currentProject.directory)

        startActivity(intent)
    }

    fun copyInputStreamToFile(context: Context, inputStream: InputStream, outputFile: File) {
        val outputStream = FileOutputStream(outputFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }

    fun createApkFromTemplate(context: Context, projectZipFile: File): File {
        val tempDir = File(context.cacheDir, "apk_temp")
        tempDir.mkdirs()

        val templateApk = File(tempDir, "template_runtime.apk")
        val templateCopied = try {
            context.assets.open("template_runtime.apk").use { input ->
                FileOutputStream(templateApk).use { output -> input.copyTo(output) }
            }
            true
        } catch (_: Exception) {
            false
        }
        if (!templateCopied) {
            tempDir.deleteRecursively()
            throw IllegalStateException("Template APK missing from assets")
        }

        val configured = ApkToolboxManager.configureApk(
            templateApk.absolutePath,
            ApkToolboxManager.ManifestConfig(debuggable = false),
            pathsToDelete = listOf("assets/project", "assets/project.zip", "META-INF/"),
            filesToAdd = listOf(projectZipFile to "assets/project.zip"),
            workDir = tempDir
        )
        if (!configured) {
            tempDir.deleteRecursively()
            throw IllegalStateException("Failed to configure APK template")
        }


        val signedApkFile = File(context.cacheDir, "signed_project_build.apk")
        val keystoreInputStream = CatroidApplication.getAppContext().assets.open("debug.jks")
        val outputFile = File(context.filesDir, "debug.jks")

        copyInputStreamToFile(CatroidApplication.getAppContext(), keystoreInputStream, outputFile)
        signApkWithApksig(context, templateApk, signedApkFile, "debug.p12", "keystore", "dbg", "keystore")
        templateApk.delete()
        tempDir.deleteRecursively()

        return signedApkFile
    }

    fun copyKeystoreFromAssets(context: Context, keystoreFileName: String, destFile: File) {
        context.assets.open(keystoreFileName).use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun signApkWithApksig(
        context: Context,
        unsignedApk: File,
        signedApk: File,
        keystoreAssetName: String,
        keystorePassword: String,
        keyAlias: String,
        keyPassword: String
    ) {
        try {

            val keystoreFile = File(context.filesDir, keystoreAssetName)
            if (!keystoreFile.exists()) {
                copyKeystoreFromAssets(context, keystoreAssetName, keystoreFile)
            }


            val keyStore = KeyStore.getInstance("PKCS12").apply {
                load(FileInputStream(keystoreFile), keystorePassword.toCharArray())
            }


            val privateKey = keyStore.getKey(keyAlias, keyPassword.toCharArray()) as PrivateKey


            val certificates = keyStore.getCertificateChain(keyAlias)
                .map { it as X509Certificate }
                .toList()


            val signerConfig = ApkSigner.SignerConfig.Builder(
                keyAlias,
                privateKey,
                certificates
            ).build()

            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(unsignedApk)
                .setOutputApk(signedApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .build()
                .sign()
        } catch (e: Exception) {
            //ErrorLog.log(e.message?: "**message not provided :(**")
            throw RuntimeException("Ошибка при подписании APK: ${e.message}", e)
        }
    }





    fun unzip(inputStream: InputStream, outDir: File) {
        val outCanonical = outDir.canonicalPath
        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val file = File(outDir, entry!!.name)
                if (!file.canonicalPath.startsWith(outCanonical + File.separator)) {
                    zis.closeEntry()
                    continue
                }
                if (entry!!.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
            }
        }
    }

    fun zipDirectory3(dir: File, zos: ZipOutputStream, basePath: String = "") {



        dir.listFiles()?.forEach { file ->
            val filePath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

            if (file.isDirectory) {


                val dirEntry = ZipEntry("$filePath/")
                zos.putNextEntry(dirEntry)
                zos.closeEntry()


                zipDirectory3(file, zos, filePath)
            } else {

                FileInputStream(file).use { fis ->
                    zos.putNextEntry(ZipEntry(filePath))
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    private fun exportProject() {
        saveProject()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportUsingSystemFilePicker()
        } else {
            exportToExternalMemory()
        }
    }

    fun showToast(toast: String) {
        if (StageActivity.messageHandler != null) {
            val params = ArrayList<Any>(listOf(toast))
            StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
        } else {

            Log.e("ShowToast", "messageHandler is null!")
        }
    }

    fun getRandomMessage(): String {
        val messages = listOf(
            "Готово!",
            "Сделано!",
            "Успех!",
            "Завершено!",
            "Готово к использованию!",
            "Задача выполнена!",
            "Отличная работа!",
            "Все готово!",
            "Яйцо или курица..?",
            "Готово! Проверяй!",
            "Поехали!",
            "Вроде сделано..",
            "Проверяй, начальник э!",
            "Готово. Удачи с проектом!",
            "Работа завершена, как кофе на утро!",
            "Готово! Как будто я маг, а не программист!",
            "Все сделано! Как раз вовремя перед обедом.",
            "Все завершено! Можно идти за пирожками!",
            "Задача выполнена! Теперь можно отдохнуть и посмотреть котиков.",
            "Готово! Даже не успел заметить, как это произошло.",
            "Сделано! Осталось только отпраздновать с танцами.",
            "Готово! Минутка успокоения перед новыми приключениями.",
            "Отличная работа! Ты как супергерой, только без плаща.",
            "Готово! Наконец-то смогу отвлечься на онлайн-шопинг.",
            "Как сказать: «Сделай это» и получить: «Сделано!»? Вот так!",
            "Все готово! Теперь можем заниматься более важными делами.",
            "Задача выполнена! Как хорошая книга – не отпускает до последней страницы.",
            "Готово! Можно отдыхать, как будто мы все это сделали за пятюню.",
            "Сделано! Готовы к новым подвигам?"
        )


        val randomIndex = Random.nextInt(messages.size)

        return messages[randomIndex]
    }

    fun getRandomError(): String {
        val errorMessages = listOf(
            "Произошла ошибка! Кажется, я не тот алгоритм заказывал.",
            "Упс! Что-то пошло не так. Как будто кошка пробежала по клавиатуре.",
            "Произошла ошибка! Может, система решила немного отдохнуть?",
            "Ой! Похоже, произошла ошибка. Возможно, это программистская шутка?",
            "Произошла ошибка! Да кто придумал обновлять программу перед дедлайном?",
            "Упс! Ошибка. Наверное, мой код тоже решил поспать.",
            "Произошла ошибка! Как бы я ни старался, выводы не совпали.",
            "Ой-ой! Ошибка! Это как раз то, что нам нужно было избежать.",
            "Произошла ошибка! По всей видимости, сервер тоже устал.",
            "Упс! Ошибка. Это как забыть о важной встрече.",
            "Произошла ошибка! Может, стоит заказывать пиццу вместо кода?",
            "Ой! Ошибка. Обычно говорят, что все дороги ведут к Риму, но не сегодня.",
            "Произошла ошибка! Это не то, что я хотел об этом напомнить.",
            "Упс! Ошибка! Возможно, машина решила, что у нее выходной.",
            "Произошла ошибка! Я попытался угостить код печеньками и вот что вышло!",
            "Ой-ой! Ошибка. Наверное, в коде слишком много любопытных переменных.",
            "Произошла ошибка! Извините, не я такой - жизнь такая!",
            "Упс! Произошла ошибка. Код сам по себе иногда делает капризы.",
            "Ой! Произошла ошибка! Как будто интернет пошел на пикник без меня.",
            "Произошла ошибка! И тут, конечно, глюк всегда оказывается виноват.",
            "Упс! Ошибка. Вы знаете, прощать - это тоже искусство."
        )

        val randomIndex = Random.nextInt(errorMessages.size)

        return errorMessages[randomIndex]
    }

    private fun changeOrientation() {
        saveProject()
        val width = project?.xmlHeader?.getVirtualScreenWidth() ?: 800
        val height = project?.xmlHeader?.getVirtualScreenHeight() ?: 1080
        project?.xmlHeader?.setVirtualScreenWidth(height)
        project?.xmlHeader?.setVirtualScreenHeight(width)
        showToast(getRandomMessage())
    }

    private fun changeIcon() {
        saveProject()
        project?.let { proj ->
            val directory: File = proj.directory


            val oldIconFile = File(directory, "automatic_screenshot.png")
            val newIconFile = File(directory, "manual_screenshot.png")


            if (oldIconFile.exists()) {
                oldIconFile.delete()
            }


            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_SELECT_IMAGE)
        } ?: run {
            showToast(getRandomError())
        }
    }


    private fun clearVars() {
        saveProject()
        project?.let {
            val directory: File = it.directory

            val deviceVariablesFile = File(directory, "DeviceVariables.json")
            val deviceListsFile = File(directory, "DeviceLists.json")


            val variablesDeleted = deviceVariablesFile.delete()
            val listsDeleted = deviceListsFile.delete()


            if (variablesDeleted || listsDeleted) {
                showToast(getRandomMessage())
            } else {
                showToast(getRandomMessage())
            }
        } ?: run {
            showToast(getRandomError())
        }
    }


    private fun buildExe() {
        saveProject()
        project ?: return

        // Force GC before starting heavy build to maximize available heap
        System.gc()

        showProgressDialog("Сборка Windows-пакета...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val proj = project!!
                val projectName = proj.name
                val tempDir = File(requireContext().cacheDir, "exe_build_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                // 1. Zip the project directory
                val projectZip = File(tempDir, "${projectName}.zip")
                zipDirectory(proj.directory, projectZip)
                System.gc()

                // 1b. Encrypt the project (AES-256-GCM + PBKDF2)
                val projectEnc = File(tempDir, "${projectName}.enc")
                org.catrobat.catroid.io.ProjectCrypto.encrypt(
                    projectZip,
                    projectEnc,
                    org.catrobat.catroid.apkbuild.ProtectedProjectPayload.PASSWORD
                )
                projectZip.delete()
                System.gc()

                // 2. Find project icon
                var iconFile: File? = null
                val manualIcon = File(proj.directory, "manual_screenshot.png")
                val autoIcon = File(proj.directory, "automatic_screenshot.png")
                if (manualIcon.exists()) iconFile = manualIcon
                else if (autoIcon.exists()) iconFile = autoIcon

                // 3. Build the final output package
                val outputZip = File(requireContext().cacheDir, "${projectName}_win.zip")
                ZipOutputStream(FileOutputStream(outputZip)).use { zos ->
                    zos.setLevel(1) // fastest compression, reduces CPU/memory
                    zos.putNextEntry(ZipEntry("project.zip"))
                    FileInputStream(projectEnc).use { input ->
                        input.copyTo(zos, 8192) // explicit small buffer to avoid OOM
                    }
                    zos.closeEntry()

                    // Add project icon as icon.png (for EXE conversion)
                    if (iconFile != null) {
                        zos.putNextEntry(ZipEntry("icon.png"))
                        FileInputStream(iconFile).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }

                    // Try to include template_win.zip from assets (player runtime)
                    try {
                        val templateAsset = "template_win.zip"
                        requireContext().assets.open(templateAsset).use { input ->
                            zos.putNextEntry(ZipEntry("template_win.zip"))
                            input.copyTo(zos)
                            zos.closeEntry()
                        }
                    } catch (_: Exception) {
                        // template_win.zip not in assets — skip
                    }

                    // Try to include build_exe.bat from assets
                    try {
                        val batAsset = "build_exe.bat"
                        requireContext().assets.open(batAsset).use { input ->
                            zos.putNextEntry(ZipEntry("build_exe.bat"))
                            input.copyTo(zos)
                            zos.closeEntry()
                        }
                    } catch (_: Exception) {
                        // Not found — skip
                    }

                    // Add instructions for Windows build
                    zos.putNextEntry(ZipEntry("README_WINDOWS.txt"))
                    val readme = buildString {
                        appendLine("NeoCatroid Windows Desktop Build")
                        appendLine("=================================")
                        appendLine()
                        appendLine("Проект: $projectName")
                        appendLine()
                        appendLine("Инструкция по сборке EXE на Windows:")
                        appendLine("1. Убедитесь, что у вас установлен Java 11+ (launch4j опционален)")
                        appendLine("2. Распакуйте этот архив в отдельную папку")
                        appendLine("3. Запустите build_exe.bat")
                        appendLine("4. Готовый NeoCatroid.exe появится в build/win-dist/")
                        appendLine()
                        appendLine("Проект (project.zip) копируется рядом с NeoCatroid.exe,")
                        appendLine("а build_exe.bat дополнительно встраивает его внутрь EXE")
                        appendLine("(footer NEOCAT01) — так что можно распространять один")
                        appendLine("только NeoCatroid.exe. Проект шифруется (AES-256-GCM)")
                        appendLine("перед упаковкой и расшифровывается при запуске.")
                        appendLine("Если launch4j не установлен, запустите: java -jar player_embedded.jar")
                        appendLine()
                        appendLine("Или соберите шаблон вручную:")
                        appendLine("  copyTemplateWin.bat")
                        appendLine("  build_exe.bat")
                    }
                    zos.write(readme.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                // Cleanup temp
                tempDir.deleteRecursively()

                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    shareFile(outputZip)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    ToastUtil.showError(requireContext(), "Ошибка: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun buildApk() {
        saveProject()
        showApkBuildDialog()
    }

    private fun buildApkV3() {
        val p = project ?: return
        saveProject()
        try {
            ApkBuilderV3ExportDialog().show(this, p.directory, firebaseLauncher)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open APK Builder V3 dialog", e)
            ToastUtil.showError(requireContext(), "Ошибка открытия сборщика APK: ${e.message}")
        }
    }

    private fun buildApkV2() {
        val p = project ?: return
        val ctx = context ?: return
        saveProject()

        // Simple progress dialog — v2 uses defaults (no tabs, just builds)
        val progressText = android.widget.TextView(ctx).apply {
            text = getString(R.string.build_apk_progress)
            setPadding(60, 30, 60, 10)
        }
        val buildDialog = AlertDialog.Builder(ctx)
            .setTitle("APK v2")
            .setView(android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                addView(progressText)
                addView(android.widget.ProgressBar(ctx).apply { isIndeterminate = true; setPadding(60, 0, 60, 20) })
            })
            .setCancelable(false)
            .create()
        buildDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val iconFile: File? = {
                    val manual = File(p.directory, "manual_screenshot.png")
                    val auto = File(p.directory, "automatic_screenshot.png")
                    when { manual.exists() -> manual; auto.exists() -> auto; else -> null }
                }()
                val config = AlignedApkBuilder.ApkConfig(
                    appName = p.name,
                    permissions = listOf("android.permission.INTERNET"),
                    iconFile = iconFile
                )
                val result = AlignedApkBuilder.build(ctx, p.directory, config) { progress ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post { progressText.text = progress }
                }
                withContext(Dispatchers.Main) {
                    buildDialog.dismiss()
                    when (result) {
                        is AlignedApkBuilder.BuildResult.Success -> {
                            saveApkToDownloads(ctx, result.apkFile)
                        }
                        is AlignedApkBuilder.BuildResult.Error -> {
                            ToastUtil.showError(ctx, result.message)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    buildDialog.dismiss()
                    ToastUtil.showError(ctx, e.message ?: "Build failed")
                }
            }
        }
    }

    private val BUILD_APK_PERMISSIONS = listOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.VIBRATE",
        "android.permission.WAKE_LOCK",
        "android.permission.POST_NOTIFICATIONS"
    )

    private fun showApkBuildDialog() {
        val project = project ?: return
        val ctx = requireContext()
        val builder = AlertDialog.Builder(ctx)
        builder.setTitle(R.string.build_apk_title)

        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 16, 24, 16) }

        val tabLayout = TabLayout(ctx)
        tabLayout.addTab(tabLayout.newTab().setText(R.string.build_apk_tab_basic))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.build_apk_tab_permissions))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.build_apk_tab_sdk))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.build_apk_tab_icon))
        root.addView(tabLayout)

        val container = FrameLayout(ctx).apply { setPadding(0, 12, 0, 0) }
        root.addView(container)

        // ---- Basic panel ----
        val basicPanel = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val nameInput = EditText(ctx).apply { setText(project.name); hint = getString(R.string.build_apk_name) }
        val pkgInput = EditText(ctx).apply { setText("org.danvexteam.newcatroidruntime"); hint = getString(R.string.build_apk_package) }
        val verInput = EditText(ctx).apply { setText("1.0"); hint = getString(R.string.build_apk_version_name) }
        val codeInput = EditText(ctx).apply {
            setText("1"); hint = getString(R.string.build_apk_version_code)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val passInput = EditText(ctx).apply {
            hint = getString(R.string.build_apk_password)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        basicPanel.addView(nameInput)
        basicPanel.addView(pkgInput)
        basicPanel.addView(verInput)
        basicPanel.addView(codeInput)
        basicPanel.addView(passInput)

        // ---- Permissions panel ----
        val permPanel = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        permPanel.addView(TextView(ctx).apply {
            text = getString(R.string.build_apk_permissions_hint)
            setPadding(0, 0, 0, 8)
        })
        val permChecks = BUILD_APK_PERMISSIONS.map { perm ->
            val cb = CheckBox(ctx).apply {
                text = perm.substringAfterLast('.')
                isChecked = perm == "android.permission.INTERNET"
            }
            permPanel.addView(cb)
            perm to cb
        }

        // ---- SDK panel ----
        val sdkPanel = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val minSdkInput = EditText(ctx).apply {
            setText("21"); hint = getString(R.string.build_apk_min_sdk)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val targetSdkInput = EditText(ctx).apply {
            setText("35"); hint = getString(R.string.build_apk_target_sdk)
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        sdkPanel.addView(minSdkInput)
        sdkPanel.addView(targetSdkInput)

        // ---- Icon panel ----
        val iconPanel = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val projectHasIcon = File(project.directory, "manual_screenshot.png").exists()
            || File(project.directory, "automatic_screenshot.png").exists()
        val iconRadio = RadioGroup(ctx)
        val rbProject = RadioButton(ctx).apply { text = getString(R.string.build_apk_icon_from_project); isChecked = projectHasIcon }
        val rbDefault = RadioButton(ctx).apply { text = getString(R.string.build_apk_icon_default); isChecked = !projectHasIcon }
        iconRadio.addView(rbProject)
        iconRadio.addView(rbDefault)
        iconPanel.addView(iconRadio)
        if (!projectHasIcon) {
            iconPanel.addView(TextView(ctx).apply {
                text = getString(R.string.build_apk_icon_none_hint)
                setPadding(0, 8, 0, 0)
            })
        }

        container.addView(basicPanel)
        container.addView(permPanel)
        container.addView(sdkPanel)
        container.addView(iconPanel)
        permPanel.visibility = View.GONE
        sdkPanel.visibility = View.GONE
        iconPanel.visibility = View.GONE

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                basicPanel.visibility = if (tab.position == 0) View.VISIBLE else View.GONE
                permPanel.visibility = if (tab.position == 1) View.VISIBLE else View.GONE
                sdkPanel.visibility = if (tab.position == 2) View.VISIBLE else View.GONE
                iconPanel.visibility = if (tab.position == 3) View.VISIBLE else View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        builder.setView(root)
        builder.setPositiveButton(R.string.build_apk_start) { _, _ ->
            val rawPkg = pkgInput.text.toString().ifEmpty { "org.danvexteam.newcatroidruntime" }
            val pkg = rawPkg.lowercase()
            if (!pkg.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+\$"))) {
                ToastUtil.showError(ctx, getString(R.string.build_apk_invalid_package))
                return@setPositiveButton
            }
            val minSdk = minSdkInput.text.toString().toIntOrNull() ?: 21
            val targetSdk = targetSdkInput.text.toString().toIntOrNull() ?: 35
            if (minSdk > targetSdk) {
                ToastUtil.showError(ctx, getString(R.string.build_apk_invalid_sdk))
                return@setPositiveButton
            }
            val code = codeInput.text.toString().toIntOrNull() ?: 1
            if (code <= 1) {
                ToastUtil.showInfoLong(ctx, getString(R.string.build_apk_versioncode_warning))
            }
            val permissions = permChecks.filter { it.second.isChecked }.map { it.first }
            val iconFile: File? = if (rbProject.isChecked) {
                val manual = File(project.directory, "manual_screenshot.png")
                val auto = File(project.directory, "automatic_screenshot.png")
                when {
                    manual.exists() -> manual
                    auto.exists() -> auto
                    else -> null
                }
            } else null
            val config = BakedApkBuilder.ApkConfig(
                appName = nameInput.text.toString().ifEmpty { project.name },
                packageName = pkg,
                versionName = verInput.text.toString().ifEmpty { "1.0" },
                versionCode = code,
                permissions = permissions,
                minSdk = minSdk,
                targetSdk = targetSdk,
                iconFile = iconFile,
                payloadPassword = passInput.text.toString().ifEmpty { null }
            )
            startApkBuild(config)
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun startApkBuild(config: BakedApkBuilder.ApkConfig) {
        val projectDir = project?.directory ?: return
        val ctx = context ?: return
        val progressText = android.widget.TextView(ctx).apply {
            text = getString(R.string.build_apk_progress)
            setPadding(60, 30, 60, 10)
        }
        val buildDialog = AlertDialog.Builder(ctx)
            .setTitle(R.string.build_apk_title)
            .setView(android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                addView(progressText)
                addView(android.widget.ProgressBar(ctx).apply { isIndeterminate = true; setPadding(60, 0, 60, 20) })
            })
            .setCancelable(false)
            .create()
        buildDialog.show()

        // Results arrive from the isolated :apkbuild process via this receiver (runs on main thread).
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                when (resultCode) {
                    ApkBuildService.WHAT_PROGRESS -> {
                        val p = resultData?.getString(ApkBuildService.KEY_PROGRESS)
                        if (!p.isNullOrEmpty()) progressText.text = p
                    }
                    ApkBuildService.WHAT_RESULT -> {
                        buildDialog.dismiss()
                        val c = context ?: return
                        val success = resultData?.getBoolean(ApkBuildService.KEY_SUCCESS) ?: false
                        if (success) {
                            val apkPath = resultData?.getString(ApkBuildService.KEY_APK_PATH)
                            if (!apkPath.isNullOrEmpty()) saveApkToDownloads(c, java.io.File(apkPath))
                        } else {
                            val err = resultData?.getString(ApkBuildService.KEY_ERROR) ?: getString(R.string.build_apk_error)
                            ToastUtil.showError(c, err)
                        }
                    }
                }
            }
        }

        // Persist the (possibly edited) project so the build process reloads the latest
        // version from disk, then launch the isolated build service.
        lifecycleScope.launch(Dispatchers.IO) {
            projectManager.currentProject?.let { saveProjectSerial(it, ctx) }
            withContext(Dispatchers.Main) {
                val intent = Intent(ctx, ApkBuildService::class.java).apply {
                    putExtra(ApkBuildService.EXTRA_PROJECT_DIR, projectDir.absolutePath)
                    putExtra(ApkBuildService.EXTRA_CONFIG, config)
                    putExtra(ApkBuildService.EXTRA_RECEIVER, receiver)
                }
                ContextCompat.startForegroundService(ctx, intent)
            }
        }
    }

    private fun saveApkToDownloads(context: Context, apkFile: File) {
        val apkDir = "NeoCatroidAPK"
        val fileName = apkFile.name
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$apkDir")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(apkFile).use { it.copyTo(out) }
                    }
                    ToastUtil.showSuccess(context, "APK сохранён в Загрузки/$apkDir/")
                } else {
                    ToastUtil.showError(context, "Не удалось сохранить APK")
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), apkDir)
                dir.mkdirs()
                val dest = File(dir, fileName)
                apkFile.copyTo(dest, true)
                ToastUtil.showSuccess(context, "APK сохранён: ${dest.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save APK to Downloads", e)
            ToastUtil.showError(context, "Ошибка сохранения APK: ${e.message}")
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun exportUsingSystemFilePicker() {
        val fileName = project?.name + Constants.CATROBAT_EXTENSION
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        intent.type = "*/*"
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
        val title = requireContext().getString(R.string.export_project)
        startActivityForResult(Intent.createChooser(intent, title), REQUEST_EXPORT_PROJECT)
    }

    private fun exportWithPassword() {
        saveProject()
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 20, 60, 0)
        }
        val pwdEdit = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val confirmEdit = android.widget.EditText(requireContext()).apply {
            hint = getString(R.string.confirm_password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(pwdEdit)
        layout.addView(confirmEdit)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.enter_export_password)
            .setView(layout)
            .setPositiveButton(R.string.ok) { _, _ ->
                val pwd = pwdEdit.text.toString()
                val confirm = confirmEdit.text.toString()
                if (pwd.isEmpty() || pwd != confirm) {
                    ToastUtil.showError(activity, R.string.passwords_dont_match)
                    return@setPositiveButton
                }
                startEncryptedExport(pwd)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startEncryptedExport(password: String) {
        val proj = project ?: return
        val progressView = android.widget.ProgressBar(requireContext()).apply {
            isIndeterminate = true
        }
        val progressLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(80, 40, 80, 0)
            addView(android.widget.TextView(requireContext()).apply { text = getString(R.string.encrypting_project) })
            addView(progressView)
        }
        val progressDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.exporting_project)
            .setView(progressLayout)
            .setCancelable(false)
            .create()
        progressDialog.show()

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val zipFile = java.io.File(requireContext().cacheDir, "${proj.name}_export.zip")
                zipDirectory(proj.directory, zipFile)
                val encFile = java.io.File(requireContext().cacheDir, "${proj.name}${Constants.NPC_EXTENSION}")
                org.catrobat.catroid.io.ProjectCrypto.encrypt(zipFile, encFile, password)
                if (zipFile.exists()) zipFile.delete()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    progressDialog.dismiss()
                    encFileToSave = encFile
                    startEncryptedFilePicker(encFile, proj.name)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Encrypted export failed", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    progressDialog.dismiss()
                    ToastUtil.showError(activity, "Export failed: ${e.message}")
                }
            }
        }
    }

    private fun startEncryptedFilePicker(encFile: java.io.File, projectName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, "$projectName${Constants.NPC_EXTENSION}")
        intent.type = "*/*"
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
        startActivityForResult(Intent.createChooser(intent, getString(R.string.export_with_password)), REQUEST_EXPORT_ENCRYPTED)
    }

    private fun saveEncryptedFileToUri(sourceFile: java.io.File, uri: Uri) {
        requireContext().contentResolver.openOutputStream(uri)?.use { out ->
            java.io.FileInputStream(sourceFile).use { it.copyTo(out) }
        }
        sourceFile.delete()
        showToast(getString(R.string.export_project) + " ✓")
    }

    fun zipDirectory(sourceDir: File, zipFile: File): File {
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            zipOut.setLevel(1) // fastest compression to reduce CPU/memory
            sourceDir.walk().filter { it != sourceDir }.forEach { file ->
                if(file.name != "undo_code.xml") {
                    val entryPath = file.relativeTo(sourceDir).path
                    val zipEntry = if (file.isDirectory) {
                        ZipEntry("$entryPath/")
                    } else {
                        ZipEntry(entryPath)
                    }
                    zipOut.putNextEntry(zipEntry)

                    if (file.isFile) {
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zipOut, 8192) // explicit small buffer
                        }
                    }

                    zipOut.closeEntry()
                }
            }
        }

        return zipFile
    }

    private fun exportToExternalMemory() {
        object : RequiresPermissionTask(
            PERMISSIONS_REQUEST_EXPORT_TO_EXTERNAL_STORAGE,
            listOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE),
            R.string.runtime_permission_general
        ) {
            override fun task() {
                val fileName = project?.name + Constants.CATROBAT_EXTENSION
                val projectZip = File(Constants.DOWNLOAD_DIRECTORY, fileName)
                Constants.DOWNLOAD_DIRECTORY.mkdirs()
                if (!Constants.DOWNLOAD_DIRECTORY.isDirectory) {
                    return
                }
                if (projectZip.exists()) {
                    projectZip.delete()
                }
                val projectDestination = Uri.fromFile(projectZip)
                startAsyncProjectExport(projectDestination)
            }
        }.execute(requireActivity())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return
        if (requestCode == REQUEST_EXPORT_PROJECT && resultCode == Activity.RESULT_OK) {
            val projectDestination = data.data ?: return
            startAsyncProjectExport(projectDestination)
        }
        if (requestCode == REQUEST_EXPORT_ENCRYPTED && resultCode == Activity.RESULT_OK) {
            val uri = data.data ?: return
            encFileToSave?.let { saveEncryptedFileToUri(it, uri) }
            encFileToSave = null
        }
        if (requestCode == REQUEST_BUILD_PROJECT && resultCode == Activity.RESULT_OK) {
            val projectDestination = data.data ?: return
            startAsyncProjectBuild(projectDestination)
        }
        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val contentResolver: ContentResolver = CatroidApplication.getAppContext().contentResolver

                    val inputStream = contentResolver.openInputStream(uri)
                    val outputStream = FileOutputStream(File(project?.directory, "manual_screenshot.png"))


                    inputStream?.copyTo(outputStream)

                    inputStream?.close()
                    outputStream.close()

                    showToast(getRandomMessage())
                } catch (e: Exception) {
                    showToast("Ошибка при сохранении изображения: ${e.message}")
                }
            } ?: showToast(getRandomError())
        }
    }

    fun copyFileToUri(context: Context, sourceFile: File, directoryUri: Uri, fileName: String) {

        val resolver: ContentResolver = context.contentResolver


        val fileUri = Uri.withAppendedPath(directoryUri, fileName)


        resolver.openOutputStream(fileUri)?.use { outputStream: OutputStream ->

            FileInputStream(sourceFile).use { inputStream ->

                inputStream.copyTo(outputStream)
            }
        } ?: run {

            println("Ошибка: Не удалось создать файл в указанной директории.")
        }
    }

    private fun copyFileToUri2(context: Context, sourceFile: File, destinationUri: Uri, fileName: String) {
        context.contentResolver.openOutputStream(destinationUri).use { outputStream ->
            FileInputStream(sourceFile).use { inputStream ->
                outputStream?.let {
                    inputStream.copyTo(it)
                } ?: Log.e("BUILD", "Ошибка открытия OutputStream для $destinationUri")
            }
        }
    }


    private fun startAsyncProjectExport(projectDestination: Uri) {
        project?.let {
            val notificationData = StatusBarNotificationManager(requireContext())
                .createSaveProjectToExternalMemoryNotification(
                    requireContext(),
                    projectDestination,
                    it.name
                )
            ProjectExportTask(it.directory, projectDestination, notificationData, requireContext())
                .execute()
        }
    }

    private fun startAsyncProjectBuild(projectDestination: Uri) {
        val ctx = context ?: return
        val pkg = requireActivity().packageName
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = buildFilename ?: return@launch
                val zip = projectInZip ?: return@launch
                val proj = project ?: return@launch
                if (!zip.exists()) {
                    Log.e("BUILD", "Файл project.zip не существует по состоянию на момент копирования!")
                    return@launch
                }
                val notificationData = StatusBarNotificationManager(ctx)
                    .createBuildProjectToExternalMemoryNotification(ctx, projectDestination, proj.name)
                Log.d("BUILD", "Project directory: ${zip.absolutePath}")
                val builded_apk = createApkFromTemplate(CatroidApplication.getAppContext(), zip)
                ctx.grantUriPermission(pkg, projectDestination, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                copyFileToUri2(CatroidApplication.getAppContext(), builded_apk, projectDestination, fileName)
                builded_apk.delete()
                zipTempDir?.deleteRecursively()
                StatusBarNotificationManager(ctx).showOrUpdateNotification(ctx, notificationData, 100, null)
            } catch (e: Exception) {
                Log.e("BUILD", "Build failed", e)
                withContext(Dispatchers.Main) {
                    ToastUtil.showError(ctx, "Ошибка сборки: ${e.message}")
                }
            }
        }
    }

    private fun moreDetails() {
        val fragment = ProjectDetailsFragment()
        val args = Bundle()
        project?.let {
            val projectData = ProjectData(
                it.name,
                it.directory,
                it.catrobatLanguageVersion,
                it.hasScene()
            )
            args.putSerializable(ProjectDetailsFragment.SELECTED_PROJECT_KEY, projectData)
        }
        fragment.arguments = args
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, ProjectDetailsFragment.TAG)
            .addToBackStack(ProjectDetailsFragment.TAG).commit()
    }

    private fun deleteProject(selectedProject: ProjectData) {
        try {
            StorageOperations.deleteDir(selectedProject.directory)
        } catch (exception: IOException) {
            Log.e(TAG, Log.getStackTraceString(exception))
        }
        ToastUtil.showSuccess(
            requireContext(),
            resources.getQuantityString(R.plurals.deleted_projects, 1, 1)
        )
        project = null
        projectManager.currentProject = project
        requireActivity().onBackPressed()
    }

    companion object {
        val TAG: String = ProjectOptionsFragment::class.java.simpleName
        private const val KEY_ENC_FILE = "encFileToSave"

        private const val PERMISSIONS_REQUEST_EXPORT_TO_EXTERNAL_STORAGE = 802
        private const val REQUEST_EXPORT_PROJECT = 10
        private const val REQUEST_BUILD_PROJECT = 11
        private const val REQUEST_SELECT_IMAGE = 12
        private const val REQUEST_EXPORT_ENCRYPTED = 13
    }
}
