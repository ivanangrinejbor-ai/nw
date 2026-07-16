package org.catrobat.catroid.utils.lunoscript.baker

import android.content.Context
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.*
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.FormulaBrick
import org.catrobat.catroid.content.bricks.ScriptBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.userbrick.UserDefinedBrickInput
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.Writer
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.IdentityHashMap

class ProjectBaker(private val context: Context) {

    private val imports = mutableSetOf<String>()

    private val bakedObjects = IdentityHashMap<Any, String>()
    private var varCounter = 0

    private fun addDefaultImports() {
        addImport("org.catrobat.catroid.content.Project")
        addImport("org.catrobat.catroid.content.Scene")
        addImport("org.catrobat.catroid.content.Sprite")
        addImport("org.catrobat.catroid.content.Script")
        addImport("org.catrobat.catroid.content.Look")
        addImport("org.catrobat.catroid.common.LookData")
        addImport("org.catrobat.catroid.common.SoundInfo")
        addImport("org.catrobat.catroid.formulaeditor.UserVariable")
        addImport("org.catrobat.catroid.formulaeditor.UserList")
        addImport("org.catrobat.catroid.formulaeditor.Formula")
        addImport("org.catrobat.catroid.formulaeditor.FormulaElement")
        addImport("org.catrobat.catroid.content.bricks.Brick")
        addImport("java.io.File")
        addImport("java.util.ArrayList")
        addImport("java.util.UUID")
        addImport("org.catrobat.catroid.userbrick.UserDefinedBrickLabel")
        addImport("org.catrobat.catroid.userbrick.UserDefinedBrickInput")
        addImport("org.catrobat.catroid.content.bricks.UserDefinedBrick")
        addImport("org.catrobat.catroid.content.bricks.ScriptBrick")
    }

    fun bake(project: Project, writer: Writer): List<String> {
        bakedObjects.clear()
        varCounter = 0
        imports.clear()

        addDefaultImports()

        val pVar = "proj"
        bakedObjects[project] = pVar
        writer.append("var $pVar = CreateEmptyProjectContext();\n")

        fillFields(writer, pVar, project)

        writer.append("\nSetGlobalProject($pVar);\n")

        return imports.sorted()
    }

    /**
     * Bakes [project] into a minified LunoScript file at [initFile].
     *
     * The (potentially huge) script is streamed straight to disk, so it is never held
     * as a single String in the heap -- this is what lets very large projects be baked
     * without an OutOfMemoryError. Imports are collected during traversal and written at
     * the top of the file (as required by LunoScript).
     */
    fun bakeToFile(project: Project, initFile: File) {
        val bodyFile = File(initFile.parent ?: ".", "${initFile.nameWithoutExtension}.body.tmp")
        val imports = try {
            FileOutputStream(bodyFile).bufferedWriter(Charsets.UTF_8).use { bw ->
                MinifyingWriter(bw).use { mw -> bake(project, mw) }
            }
        } catch (e: Throwable) {
            bodyFile.delete()
            throw e
        }
        try {
            val header = buildString { imports.forEach { append("import $it;\n") } }
            FileOutputStream(initFile).use { fos ->
                fos.write(header.toByteArray(Charsets.UTF_8))
                FileInputStream(bodyFile).use { bis -> bis.copyTo(fos, 64 * 1024) }
            }
        } finally {
            bodyFile.delete()
        }
    }

    private fun getValueCode(body: Appendable, value: Any?): String {
        if (value == null) return "null"

        return when (value) {
            is String -> {
                if (value.contains("#ifdef GL_ES") || value.contains("void main")) {
                    val cleanShader = value.lines()
                        .map { line ->
                            val idx = line.indexOf("//")
                            if (idx != -1) line.substring(0, idx) else line
                        }
                        .joinToString("\n")
                    escape(cleanShader)
                } else {
                    escape(value)
                }
            }
            is Number, is Boolean -> value.toString()
            is Enum<*> -> {
                val clazz = value.javaClass
                addImport(clazz.name)

                if (clazz.isMemberClass && clazz.declaringClass != null) {
                    val parentName = clazz.declaringClass!!.simpleName
                    val simpleName = clazz.simpleName
                    "$parentName.$simpleName.${value.name}"
                } else {
                    "${clazz.simpleName}.${value.name}"
                }
            }
            is File -> "File(${escape(value.path)})"
            is Formula -> bakeFormula(body, value)
            is List<*> -> bakeList(body, value)
            is java.util.UUID -> {
                addImport("java.util.UUID")
                "UUID.fromString(\"${value.toString()}\")"
            }
            else -> bakeObject(body, value)
        }
    }

    private fun bakeObject(body: Appendable, obj: Any): String {
        if (bakedObjects.containsKey(obj)) {
            return bakedObjects[obj]!!
        }

        val clazz = obj.javaClass
        if (clazz.isAnonymousClass || clazz.isSynthetic) return "null"

        addImport(clazz.name)
        val varName = nextVar(clazz.simpleName.toLowerCase())
        bakedObjects[obj] = varName

        val constructorCode = when {
            clazz.simpleName == "UserDefinedBrickLabel" -> {
                val nameField = try { clazz.getDeclaredField("label") } catch (e: Exception) { null }
                nameField?.isAccessible = true
                val labelVal = nameField?.get(obj) as? String ?: ""
                "UserDefinedBrickLabel(${escape(labelVal)})"
            }
            clazz.simpleName == "UserDefinedBrickInput" -> {
                val nameField = try { clazz.getDeclaredField("name") } catch (e: Exception) { null }
                nameField?.isAccessible = true
                val inputField = nameField?.get(obj)
                val inputStr = inputField?.toString() ?: ""
                "UserDefinedBrickInput(${escape(inputStr)})"
            }
            else -> "${clazz.simpleName}()"
        }

        body.append("var $varName = $constructorCode;\n")
        fillFields(body, varName, obj)

        return varName
    }

    private fun bakeList(body: Appendable, list: List<*>): String {
        val listVar = nextVar("list")
        body.append("var $listVar = ArrayList();\n")

        for (item in list) {
            if (item != null) {
                // Игнорируем закомментированные элементы
                if (item is Brick && item.isCommentedOut) continue
                if (item is Script && item.isCommentedOut) continue

                val itemCode = getValueCode(body, item)
                body.append("$listVar.add($itemCode);\n")
            }
        }
        return listVar
    }

    private fun fillFields(body: Appendable, varName: String, obj: Any) {

        if (obj is Scene) {
            val proj = obj.project
            if (proj != null) {
                val projVar = bakedObjects[proj]
                if (projVar != null) {
                    body.append("$varName.setProject($projVar);\n")
                }
            }
        }

        val fields = getAllFields(obj.javaClass)

        for (field in fields) {
            if (Modifier.isStatic(field.modifiers) || Modifier.isTransient(field.modifiers)) continue

            if (field.name == "view" || field.name == "checkbox" || field.name == "stage" || field.name == "batch") continue
            if (field.type.name.startsWith("android.view")) continue

            if (obj is Script && field.name == "formulaMap") continue

            field.isAccessible = true
            val value = try { field.get(obj) } catch (e: Exception) { null }

            if (obj is ScriptBrick && field.name == "script") {
                if (value != null) {
                    val scriptCode = getValueCode(body, value)

                    body.append("$varName.script = $scriptCode;\n")

                    if (value is WhenConditionScript) {
                        body.append("$varName.formulaMap = $scriptCode.getFormulaMap();\n")
                    }

                    body.append("$scriptCode.setScriptBrick($varName);\n")
                }
                continue
            }

            if (value != null) {
                if (obj is FormulaBrick && field.name == "formulaMap") {
                    val formulas = obj.allFormulasMap

                    for ((k, v) in formulas) {
                        var keyExpression = "null"

                        if (k is Enum<*>) {
                            val enumClass = k.javaClass.declaringClass?.simpleName ?: "Brick"
                            keyExpression = "$enumClass.BrickField.${k.name}"
                        } else {
                            for ((bakedObj, vName) in bakedObjects) {
                                if (bakedObj is UserDefinedBrickInput) {
                                    if (bakedObj.inputFormulaField === k || bakedObj.inputFormulaField == k) {
                                        keyExpression = "$vName.getInputFormulaField()"
                                        break
                                    }
                                }
                            }
                        }

                        if (keyExpression != "null") {
                            val formulaCode = getValueCode(body, v)
                            body.append("$varName.formulaMap.put($keyExpression, $formulaCode);\n")
                        }
                    }
                    continue
                }

                val valueCode = getValueCode(body, value)
                body.append("$varName.${field.name} = $valueCode;\n")
            }
        }

        if (obj is LookData) {
            val fileName = obj.fileName ?: obj.name
            if (fileName != null) {
                body.append("$varName.file = File(ROOT_PATH, \"images/$fileName\");\n")
            }
        }

        if (obj is SoundInfo) {
            val fileName = obj.fileName ?: obj.name
            if (fileName != null) {
                body.append("$varName.file = File(ROOT_PATH, \"sounds/$fileName\");\n")
            }
        }
    }

    private fun getAllFields(type: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        var clazz: Class<*>? = type
        while (clazz != null && clazz != Any::class.java) {
            fields.addAll(clazz.declaredFields)
            clazz = clazz.superclass
        }
        return fields
    }

    private fun addImport(className: String) {
        if (className.contains("$")) {
            imports.add(className.substringBefore("$"))
        } else {
            imports.add(className)
        }
    }

    private fun nextVar(prefix: String) = "${prefix}_${varCounter++}"

    private fun escape(str: String): String {
        val sb = StringBuilder("\"")
        for (char in str) {
            when (char) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (char.toInt() < 32 || char.toInt() > 126) {
                        sb.append(char)
                    } else {
                        sb.append(char)
                    }
                }
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private fun bakeFormula(body: Appendable, formula: Formula): String {
        addImport(org.catrobat.catroid.formulaeditor.Formula::class.java.name)
        val rootElement = formula.formulaTree
        val rootVar = bakeFormulaElement(body, rootElement)
        return "Formula($rootVar)"
    }

    private fun bakeFormulaElement(body: Appendable, element: org.catrobat.catroid.formulaeditor.FormulaElement?): String {
        if (element == null) return "null"

        val varName = nextVar("elem")
        addImport(org.catrobat.catroid.formulaeditor.FormulaElement::class.java.name)

        val type = element.type
        val value = element.value
        val left = bakeFormulaElement(body, element.leftChild)
        val right = bakeFormulaElement(body, element.rightChild)

        body.append("var $varName = FormulaElement(FormulaElement.ElementType.${type.name}, ${getValueCode(body, value)}, null, $left, $right);\n")

        if (element.additionalChildren != null && element.additionalChildren.isNotEmpty()) {
            for (child in element.additionalChildren) {
                val childVar = bakeFormulaElement(body, child)
                body.append("$varName.additionalChildren.add($childVar);\n")
                body.append("$childVar.parent = $varName;\n")
            }
        }

        return varName
    }

    companion object {
        // (Minification is now done on the fly by MinifyingWriter during streaming bake.)
    }
}

/**
 * A [Writer] that minifies LunoScript on the fly: it drops `//` and `/* */` comments and
 * collapses runs of whitespace to a single space (outside of string literals), while
 * preserving string contents verbatim. Because it writes straight to the wrapped writer,
 * the (potentially huge) script is never buffered as a whole in memory.
 */
private class MinifyingWriter(private val out: Writer) : Writer() {
    private var inString = false
    private var inLineComment = false
    private var inBlockComment = false
    private var prevSpace = false
    private var wroteAny = false
    private var pending = -1

    private fun emit(c: Char) {
        out.write(c.code)
        wroteAny = true
        prevSpace = false
    }

    override fun write(cbuf: CharArray?, off: Int, len: Int) {
        if (cbuf == null) return
        var i = off
        val end = off + len

        // Carry over a char buffered at the end of the previous write() because it needed
        // the following char to decide (a potential comment start, or an escape in a string).
        if (pending != -1) {
            val p = pending.toChar()
            pending = -1
            val nxt = if (i < end) cbuf[i] else '\u0000'
            when {
                p == '\\' && inString && nxt != '\u0000' -> { out.write(p.code); out.write(nxt.code); wroteAny = true; prevSpace = false; i++ }
                p == '/' && nxt == '/' -> { inLineComment = true; i++ }
                p == '/' && nxt == '*' -> { inBlockComment = true; i++ }
                else -> { emit(p); i++ }
            }
        }

        while (i < end) {
            val c = cbuf[i]
            val next = if (i + 1 < end) cbuf[i + 1] else '\u0000'
            when {
                inLineComment -> { if (c == '\n') inLineComment = false; i++ }
                inBlockComment -> { if (c == '*' && next == '/') { inBlockComment = false; i += 2 } else i++ }
                inString -> {
                    out.write(c.code); wroteAny = true; prevSpace = false
                    when {
                        c == '\\' && i + 1 < end -> { out.write(next.code); i += 2 }
                        c == '\\' -> { pending = c.code; i++ }   // escape; need next buffer
                        c == '"' -> { inString = false; i++ }
                        else -> i++
                    }
                }
                c == '"' -> { out.write(c.code); inString = true; wroteAny = true; prevSpace = false; i++ }
                c == '/' && next == '/' -> { inLineComment = true; i += 2 }
                c == '/' && next == '*' -> { inBlockComment = true; i += 2 }
                c == '/' && i + 1 >= end -> { pending = c.code; i++ }  // hold, decide next buffer
                c.isWhitespace() -> {
                    if (!prevSpace && wroteAny) { out.write(' '.code); prevSpace = true }
                    i++
                }
                else -> { out.write(c.code); wroteAny = true; prevSpace = false; i++ }
            }
        }
    }

    override fun flush() { out.flush() }
    override fun close() { out.close() }
}