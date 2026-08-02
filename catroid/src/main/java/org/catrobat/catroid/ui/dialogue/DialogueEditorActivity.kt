package org.catrobat.catroid.ui.dialogue

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.R
import org.catrobat.catroid.dialogue.*
import java.io.File
import java.util.*

class DialogueEditorActivity : AppCompatActivity() {

    private lateinit var canvas: DialogueEditorCanvas
    private var dialogueTree: DialogueTree = DialogueTree(name = "Untitled")
    private var currentFilePath: String? = null
    private val undoStack = LinkedList<DialogueTree>()
    private val redoStack = LinkedList<DialogueTree>()
    private val maxUndo = 50
    private var autoSaveFile: File? = null
    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private var dirtyFlag = false

    private val autoSaveRunnable = object : Runnable {
        override fun run() {
            if (dirtyFlag) saveAutosave()
            autoSaveHandler.postDelayed(this, 10_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialogue_editor)

        canvas = findViewById(R.id.dialogue_editor_canvas)
        canvas.editorActivity = this

        val filePath = intent.getStringExtra("file_path")
        if (filePath != null) {
            currentFilePath = filePath
            autoSaveFile = File(cacheDir, "dialogue_autosave_${File(filePath).nameWithoutExtension}.json")
            try {
                dialogueTree = DialogueTree.fromJson(File(filePath).readText())
            } catch (_: Exception) {
                dialogueTree = DialogueTree(name = File(filePath).nameWithoutExtension)
            }
        } else {
            autoSaveFile = File(cacheDir, "dialogue_autosave.json")
        }

        if (autoSaveFile!!.exists()) {
            AlertDialog.Builder(this)
                .setTitle("Autosave found")
                .setMessage("Restore autosaved dialogue?")
                .setPositiveButton("Restore") { _, _ ->
                    try {
                        dialogueTree = DialogueTree.fromJson(autoSaveFile!!.readText())
                    } catch (_: Exception) {}
                    canvas.setDialogueTree(dialogueTree)
                    pushUndo()
                }
                .setNegativeButton("Discard") { _, _ ->
                    canvas.setDialogueTree(dialogueTree)
                    pushUndo()
                }
                .setOnCancelListener { canvas.setDialogueTree(dialogueTree); pushUndo() }
                .show()
        } else {
            canvas.setDialogueTree(dialogueTree)
            pushUndo()
        }

        setupToolbar()
        autoSaveHandler.postDelayed(autoSaveRunnable, 10_000)
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.dialogue_editor_back).setOnClickListener { showSaveDialog() }
        findViewById<ImageButton>(R.id.dialogue_editor_undo).setOnClickListener { undo() }
        findViewById<ImageButton>(R.id.dialogue_editor_redo).setOnClickListener { redo() }
        findViewById<ImageButton>(R.id.dialogue_editor_add_node).setOnClickListener { showAddNodeMenu() }
        findViewById<ImageButton>(R.id.dialogue_editor_save).setOnClickListener { saveDialogue() }

        val searchBtn = findViewById<ImageButton>(R.id.dialogue_editor_search)
        searchBtn.setOnClickListener { showSearchDialog() }
    }

    private fun showSearchDialog() {
        val input = EditText(this).apply {
            hint = "Search nodes..."
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.GRAY)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Search")
            .setView(input)
            .setPositiveButton("Find") { _, _ ->
                canvas.searchTerm = input.text.toString()
            }
            .setNegativeButton("Clear") { _, _ ->
                canvas.searchTerm = ""
            }
            .create()
        dialog.show()
        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { canvas.searchTerm = s?.toString() ?: "" }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    fun pushUndo() {
        dirtyFlag = true
        val snapshot = DialogueTree.fromJson(dialogueTree.toJson())
        undoStack.push(snapshot)
        if (undoStack.size > maxUndo) undoStack.removeLast()
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.size <= 1) return
        redoStack.push(DialogueTree.fromJson(dialogueTree.toJson()))
        if (redoStack.size > maxUndo) redoStack.removeLast()
        undoStack.pop()
        dialogueTree = DialogueTree.fromJson(undoStack.peek().toJson())
        canvas.setDialogueTree(dialogueTree)
        dirtyFlag = true
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.push(DialogueTree.fromJson(dialogueTree.toJson()))
        if (undoStack.size > maxUndo) undoStack.removeLast()
        dialogueTree = DialogueTree.fromJson(redoStack.pop().toJson())
        canvas.setDialogueTree(dialogueTree)
        dirtyFlag = true
    }

    fun getDialogueTree(): DialogueTree = dialogueTree

    fun updateTree(tree: DialogueTree) {
        dialogueTree = tree
    }

    private fun showAddNodeMenu() {
        val types = arrayOf("Start", "Dialogue", "Choice", "Condition", "Action", "End", "Comment")
        AlertDialog.Builder(this)
            .setTitle("Add Node")
            .setItems(types) { _, which ->
                canvas.addNodeAtCenter(types[which])
                pushUndo()
            }
            .show()
    }

    fun showEditNodeDialog(node: DialogueNode) {
        when (node) {
            is DialogueNode.DialogueLine -> showEditDialogueDialog(node)
            is DialogueNode.ChoiceNode -> showEditChoiceDialog(node)
            is DialogueNode.ConditionNode -> showEditConditionDialog(node)
            is DialogueNode.ActionNode -> showEditActionDialog(node)
            is DialogueNode.CommentNode -> showEditCommentDialog(node)
            else -> {}
        }
    }

    private fun showEditDialogueDialog(node: DialogueNode.DialogueLine) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_node, null)
        val textIdInput = view.findViewById<EditText>(R.id.et_node_textid)
        val speakerInput = view.findViewById<EditText>(R.id.et_node_speaker)
        val textInput = view.findViewById<EditText>(R.id.et_node_text)
        val portraitInput = view.findViewById<EditText>(R.id.et_node_portrait)
        val soundInput = view.findViewById<EditText>(R.id.et_node_sound)
        val speedInput = view.findViewById<EditText>(R.id.et_node_speed)
        val bgInput = view.findViewById<EditText>(R.id.et_node_background)

        textIdInput.visibility = View.VISIBLE
        textIdInput.setText(node.textId)
        textIdInput.hint = "text_id (for localization)"
        speakerInput.setText(node.speaker)
        textInput.setText(node.text)
        portraitInput.setText(node.portrait)
        soundInput.setText(node.voiceSound)
        speedInput.setText(node.typingSpeed.toString())
        bgInput.setText(node.backgroundImage)

        AlertDialog.Builder(this)
            .setTitle("Edit Dialogue Node")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                pushUndo()
                replaceNode(node.id, node.copy(
                    textId = textIdInput.text.toString(),
                    speaker = speakerInput.text.toString(),
                    text = textInput.text.toString(),
                    portrait = portraitInput.text.toString(),
                    voiceSound = soundInput.text.toString(),
                    typingSpeed = speedInput.text.toString().toFloatOrNull() ?: 0.05f,
                    backgroundImage = bgInput.text.toString()
                ))
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ -> deleteNode(node.id) }
            .show()
    }

    private fun showEditChoiceDialog(node: DialogueNode.ChoiceNode) {
        val choicesText = node.choices.joinToString("\n") { "${it.text} -> ${it.next ?: ""}" }
        val view = layoutInflater.inflate(R.layout.dialog_edit_node, null)
        val textInput = view.findViewById<EditText>(R.id.et_node_text)
        textInput.hint = "Choice1 -> nextId\nChoice2 -> nextId\n\nFirst line visible condition\nSecond line enable condition"
        textInput.setLines(8)
        textInput.setText(choicesText)

        view.findViewById<EditText>(R.id.et_node_textid).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_speaker).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_portrait).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_sound).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_speed).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_background).visibility = View.GONE

        AlertDialog.Builder(this)
            .setTitle("Edit Choices (text -> nextId)")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                pushUndo()
                val lines = textInput.text.toString().lines().filter { it.isNotBlank() }
                val choices = lines.map { line ->
                    val parts = line.split("->").map { it.trim() }
                    DialogueNode.Choice(text = parts.getOrElse(0) { "" }, next = parts.getOrElse(1) { null })
                }.toMutableList()
                replaceNode(node.id, node.copy(choices = choices))
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ -> deleteNode(node.id) }
            .show()
    }

    private fun showEditConditionDialog(node: DialogueNode.ConditionNode) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_node, null)
        val textInput = view.findViewById<EditText>(R.id.et_node_text)
        val trueInput = view.findViewById<EditText>(R.id.et_node_portrait)
        val falseInput = view.findViewById<EditText>(R.id.et_node_sound)

        textInput.hint = "Expression: coins >= 100  OR  hasKey == true"
        textInput.setText(node.expression)
        trueInput.hint = "True -> nodeId"
        trueInput.setText(node.trueNext ?: "")
        falseInput.hint = "False -> nodeId"
        falseInput.setText(node.falseNext ?: "")

        view.findViewById<EditText>(R.id.et_node_textid).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_speaker).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_speed).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_background).visibility = View.GONE

        AlertDialog.Builder(this)
            .setTitle("Edit Condition")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                pushUndo()
                replaceNode(node.id, node.copy(
                    expression = textInput.text.toString(),
                    trueNext = trueInput.text.toString().ifEmpty { null },
                    falseNext = falseInput.text.toString().ifEmpty { null }
                ))
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ -> deleteNode(node.id) }
            .show()
    }

    private fun showEditActionDialog(node: DialogueNode.ActionNode) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_node, null)
        val textInput = view.findViewById<EditText>(R.id.et_node_text)
        textInput.hint = "type:name:value  (one per line)\nsetVariable:coins:+100\ngiveItem:Sword"
        textInput.setLines(8)
        textInput.setText(node.actions.joinToString("\n") { "${it.type}:${it.name}:${it.value}" })

        view.findViewById<EditText>(R.id.et_node_textid).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_speaker).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_portrait).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_sound).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_speed).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_background).visibility = View.GONE

        AlertDialog.Builder(this)
            .setTitle("Edit Actions")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                pushUndo()
                val actions = textInput.text.toString().lines().filter { it.isNotBlank() }.map { line ->
                    val parts = line.split(":").map { it.trim() }
                    DialogueNode.ActionEntry(type = parts.getOrElse(0) { "setVariable" }, name = parts.getOrElse(1) { "" }, value = parts.getOrElse(2) { "" })
                }.toMutableList()
                replaceNode(node.id, node.copy(actions = actions))
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ -> deleteNode(node.id) }
            .show()
    }

    private fun showEditCommentDialog(node: DialogueNode.CommentNode) {
        val view = layoutInflater.inflate(R.layout.dialog_edit_node, null)
        val textInput = view.findViewById<EditText>(R.id.et_node_text)
        textInput.hint = "Comment text"
        textInput.setText(node.text)
        view.findViewById<EditText>(R.id.et_node_textid).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_speaker).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_portrait).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_sound).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_speed).visibility = View.GONE
        view.findViewById<EditText>(R.id.et_node_background).visibility = View.GONE

        AlertDialog.Builder(this)
            .setTitle("Edit Comment")
            .setView(view)
            .setPositiveButton("Save") { _, _ -> pushUndo(); replaceNode(node.id, node.copy(text = textInput.text.toString())) }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ -> deleteNode(node.id) }
            .show()
    }

    fun deleteNode(nodeId: String) {
        pushUndo()
        dialogueTree.nodes.removeAll { it.id == nodeId }
        fixReferences(nodeId)
        canvas.setDialogueTree(dialogueTree)
    }

    private fun fixReferences(removedId: String) {
        for (i in dialogueTree.nodes.indices) {
            dialogueTree.nodes[i] = when (val n = dialogueTree.nodes[i]) {
                is DialogueNode.StartNode -> if (n.next == removedId) n.copy(next = null) else n
                is DialogueNode.DialogueLine -> if (n.next == removedId) n.copy(next = null) else n
                is DialogueNode.ActionNode -> if (n.next == removedId) n.copy(next = null) else n
                is DialogueNode.ConditionNode -> {
                    var changed = n
                    if (n.trueNext == removedId) changed = changed.copy(trueNext = null)
                    if (n.falseNext == removedId) changed = changed.copy(falseNext = null)
                    changed
                }
                is DialogueNode.ChoiceNode -> {
                    n.copy(choices = n.choices.map { if (it.next == removedId) it.copy(next = null) else it }.toMutableList())
                }
                else -> n
            }
        }
    }

    fun replaceNode(nodeId: String, newNode: DialogueNode) {
        val idx = dialogueTree.nodes.indexOfFirst { it.id == nodeId }
        if (idx >= 0) dialogueTree.nodes[idx] = newNode
        canvas.setDialogueTree(dialogueTree)
    }

    fun connectNodes(fromId: String, toId: String, portIndex: Int) {
        pushUndo()
        val idx = dialogueTree.nodes.indexOfFirst { it.id == fromId }; if (idx < 0) return
        dialogueTree.nodes[idx] = when (val n = dialogueTree.nodes[idx]) {
            is DialogueNode.StartNode -> n.copy(next = toId)
            is DialogueNode.DialogueLine -> n.copy(next = toId)
            is DialogueNode.ActionNode -> n.copy(next = toId)
            is DialogueNode.ConditionNode -> if (portIndex == 0) n.copy(trueNext = toId) else n.copy(falseNext = toId)
            is DialogueNode.ChoiceNode -> {
                val updated = n.choices.toMutableList()
                if (portIndex < updated.size) updated[portIndex] = updated[portIndex].copy(next = toId)
                n.copy(choices = updated)
            }
            else -> n
        }
        canvas.setDialogueTree(dialogueTree)
    }

    private fun saveAutosave() {
        if (autoSaveFile != null) {
            try { autoSaveFile!!.writeText(dialogueTree.toJson()); dirtyFlag = false } catch (_: Exception) {}
        }
    }

    private fun showSaveDialog() {
        val errors = dialogueTree.validate()
        if (errors.isNotEmpty() && errors.any { it.startsWith("No Start") || it.contains("references missing") }) {
            AlertDialog.Builder(this)
                .setTitle("Validation issues")
                .setMessage(errors.joinToString("\n• ", "• "))
                .setPositiveButton("Save anyway") { _, _ -> saveDialogue(); finish() }
                .setNegativeButton("Continue editing", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setMessage("Save before exit?")
            .setPositiveButton("Save") { _, _ -> saveDialogue(); finish() }
            .setNegativeButton("Discard") { _, _ -> finish() }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun saveDialogue() {
        if (currentFilePath != null) {
            DialogueSerializer.saveToFile(dialogueTree, File(currentFilePath!!))
            autoSaveFile?.delete()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "${dialogueTree.name}.json")
            }
            startActivityForResult(intent, REQUEST_SAVE_FILE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SAVE_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { it.write(dialogueTree.toJson().toByteArray()) }
                autoSaveFile?.delete()
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() { showSaveDialog() }

    override fun onDestroy() {
        super.onDestroy()
        autoSaveHandler.removeCallbacks(autoSaveRunnable)
        saveAutosave()
    }

    companion object {
        private const val REQUEST_SAVE_FILE = 101
    }
}
