package org.catrobat.catroid.ide

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.catrobat.catroid.R
import org.catrobat.catroid.utils.git.GitController
import org.catrobat.catroid.utils.git.GitHubActionsApi
import org.catrobat.catroid.utils.git.TokenManager
import org.json.JSONObject
import retrofit2.Retrofit
import java.io.File

class IdeActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var editor: CodeEditor
    private lateinit var bottomPanel: LinearLayout
    private lateinit var aiInputField: EditText
    private lateinit var fileTreeRecycler: RecyclerView
    private lateinit var currentFileTab: TextView

    private lateinit var projectDir: File
    private lateinit var currentDir: File
    private var currentOpenedFile: File? = null

    private val modifiedFiles = HashSet<File>()


    private val filesList = mutableListOf<File>()
    private lateinit var fileAdapter: FileBrowserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ide)

        drawerLayout = findViewById(R.id.drawer_layout)
        editor = findViewById(R.id.editor)
        bottomPanel = findViewById(R.id.bottom_panel)
        aiInputField = findViewById(R.id.ai_input_field)
        fileTreeRecycler = findViewById(R.id.file_tree_recycler)
        currentFileTab = findViewById(R.id.current_file_tab)

        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "UnknownProject"
        projectDir = File(filesDir, "IdeProjects/$projectName")
        currentDir = projectDir

        setupEditor()
        setupButtons()
        setupFileBrowser()

        loadDirectory(currentDir)

        checkNotificationPermission()
    }

    private fun setupEditor() {
        editor.colorScheme = SchemeDarcula()
        editor.setEditorLanguage(JavaLanguage() as Language)
        editor.setText("// Выберите файл слева в меню для редактирования")
    }



    private fun setupFileBrowser() {
        fileAdapter = FileBrowserAdapter(filesList) { file ->
            if (file.name == "..") {
                loadDirectory(currentDir.parentFile!!)
            } else if (file.isDirectory) {
                loadDirectory(file)
            } else {
                openFileInEditor(file)
            }
        }
        fileTreeRecycler.layoutManager = LinearLayoutManager(this)
        fileTreeRecycler.adapter = fileAdapter
    }

    private fun loadDirectory(dir: File) {
        currentDir = dir
        filesList.clear()


        if (dir.absolutePath != projectDir.absolutePath) {
            filesList.add(File(".."))
        }

        val items = dir.listFiles()?.toList() ?: emptyList()

        val sortedItems = items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        filesList.addAll(sortedItems)

        fileAdapter.notifyDataSetChanged()
    }

    private fun openFileInEditor(file: File) {

        saveCurrentFile()

        currentOpenedFile = file
        currentFileTab.text = file.name


        val text = file.readText()
        editor.setText(text)


        val lang: Language = when (file.extension.lowercase()) {
            "java" -> JavaLanguage()
            else -> JavaLanguage()
        }
        editor.setEditorLanguage(lang)


        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun saveCurrentFile() {
        currentOpenedFile?.let { file ->
            val newContent = editor.text.toString()

            if (file.exists() && file.readText() != newContent) {
                file.writeText(newContent)
                modifiedFiles.add(file)
                android.util.Log.d("IDE_DEBUG", "Файл помечен как измененный: ${file.name}")
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun startCloudBuild() {
        val token = TokenManager.getToken(this) ?: return
        if (currentOpenedFile != null) saveCurrentFile()

        val featureId = projectDir.name.substringAfter("_")
        val branchName = "feature-$featureId"

        lifecycleScope.launch(Dispatchers.IO) {
            try {

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.github.com/")
                    .build()
                val api = retrofit.create(GitHubActionsApi::class.java)
                val authHeader = "Bearer $token"
                val jsonType = "application/json; charset=utf-8".toMediaType()


                val userRes = api.getCurrentUser(authHeader)
                val login = JSONObject(userRes.body()?.string() ?: "").getString("login")

                ensureBranchExists(api, authHeader, login, branchName, jsonType)


                if (modifiedFiles.isNotEmpty()) {
                    for (file in modifiedFiles) {
                        val relativePath = file.absolutePath.removePrefix(projectDir.absolutePath).removePrefix("/")

                        var fileSha: String? = null
                        val fileRes = api.getFile(authHeader, login, "NewCatroid", relativePath, branchName)
                        if (fileRes.isSuccessful) {
                            fileSha = JSONObject(fileRes.body()?.string() ?: "").getString("sha")
                        }

                        val contentBase64 = android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)


                        val updateJson = JSONObject().apply {
                            put("message", "Update $relativePath via IDE")
                            put("content", contentBase64)
                            put("branch", branchName)
                            if (fileSha != null) put("sha", fileSha)
                        }

                        api.updateFile(authHeader, login, "NewCatroid", relativePath,
                            updateJson.toString().toRequestBody(jsonType))
                    }
                    modifiedFiles.clear()
                }


                val workflowJson = JSONObject().apply {
                    put("ref", branchName)
                    put("inputs", JSONObject().apply {
                        put("feature_name", projectDir.name)
                    })
                }

                android.util.Log.d("IDE_DEBUG", "Запуск Action с телом: ${workflowJson.toString()}")

                val actionRes = api.triggerWorkflow(authHeader, login, "NewCatroid",
                    workflowJson.toString().toRequestBody(jsonType))

                if (actionRes.isSuccessful || actionRes.code() == 204) {
                    withContext(Dispatchers.Main) {
                        startBuildService(token, login, branchName)
                        Toast.makeText(this@IdeActivity, "Сборка запущена! 🚀", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMsg = actionRes.errorBody()?.string() ?: "Код ${actionRes.code()}"
                    throw Exception("GitHub API Error: $errorMsg")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@IdeActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun ensureBranchExists(
        api: GitHubActionsApi,
        auth: String,
        login: String,
        branch: String,
        jsonType: okhttp3.MediaType
    ) {
        android.util.Log.d("IDE_DEBUG", "Проверка ветки $branch на сервере...")

        val check = api.getRef(auth, login, "NewCatroid", branch)

        if (check.isSuccessful) {
            android.util.Log.d("IDE_DEBUG", "Ветка уже существует.")
            return
        }

        android.util.Log.d("IDE_DEBUG", "Ветки нет. Создаем от main...")


        var baseSha: String? = null
        val mainRef = api.getRef(auth, login, "NewCatroid", "main")

        if (mainRef.isSuccessful) {
            baseSha = JSONObject(mainRef.body()?.string() ?: "").getJSONObject("object").getString("sha")
        } else {

            val masterRef = api.getRef(auth, login, "NewCatroid", "master")
            if (masterRef.isSuccessful) {
                baseSha = JSONObject(masterRef.body()?.string() ?: "").getJSONObject("object").getString("sha")
            }
        }

        if (baseSha == null) throw Exception("Не удалось найти базовую ветку (main/master) для создания форка.")


        val createJson = JSONObject().apply {
            put("ref", "refs/heads/$branch")
            put("sha", baseSha)
        }

        val createRes = api.createRef(auth, login, "NewCatroid", createJson.toString().toRequestBody(jsonType))
        if (!createRes.isSuccessful) {
            throw Exception("Не удалось создать ветку на GitHub: ${createRes.code()}")
        }

        android.util.Log.d("IDE_DEBUG", "Ветка $branch успешно создана на GitHub!")
    }

    private fun startBuildService(token: String, login: String, branch: String) {
        val intent = Intent(this, BuildMonitorService::class.java).apply {
            putExtra("TOKEN", token)
            putExtra("LOGIN", login)
            putExtra("BRANCH", branch)
            putExtra("FEATURE_NAME", projectDir.name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        saveCurrentFile()
    }



    private fun setupButtons() {
        findViewById<ImageButton>(R.id.btn_open_files).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<Button>(R.id.btn_ai_assistant).setOnClickListener {
            bottomPanel.visibility = if (bottomPanel.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        findViewById<ImageButton>(R.id.btn_close_bottom_panel).setOnClickListener {
            bottomPanel.visibility = View.GONE
        }

        findViewById<Button>(R.id.btn_check_syntax).setOnClickListener {
            saveCurrentFile()
            Toast.makeText(this, "Код сохранен. Вызов ECJ...", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_build_cloud).setOnClickListener {
            saveCurrentFile()
            startCloudBuild()
        }
    }



    inner class FileBrowserAdapter(
        private val items: List<File>,
        private val onClick: (File) -> Unit
    ) : RecyclerView.Adapter<FileBrowserAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.file_icon)
            val name: TextView = view.findViewById(R.id.file_name)

            init {
                view.setOnClickListener { onClick(items[adapterPosition]) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file_browser, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = items[position]
            holder.name.text = file.name

            if (file.name == "..") {
                holder.icon.setImageResource(android.R.drawable.ic_menu_revert)
            } else if (file.isDirectory) {
                holder.icon.setImageResource(android.R.drawable.ic_menu_sort_by_size)
            } else {

                if (file.extension == "java") {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_edit)
                } else if (file.extension == "xml") {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_view)
                } else {
                    holder.icon.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
