package org.catrobat.catroid.ai.tool

import android.util.Log
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.BroadcastScript
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.StartScript
import org.catrobat.catroid.content.WhenClonedScript
import org.catrobat.catroid.content.WhenConditionScript
import org.catrobat.catroid.content.WhenScript
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.formulaeditor.Formula
import java.lang.reflect.Constructor

object BrickFactory {

    private const val TAG = "BrickFactory"

    private val BRICK_PACKAGES = listOf(
        "org.catrobat.catroid.content.bricks.",
        "org.catrobat.catroid.physics.content.bricks."
    )

    private val ALLOWED_CLASS_PREFIXES = listOf(
        "org.catrobat.catroid.content.bricks.",
        "org.catrobat.catroid.physics.content.bricks."
    )

    private val VALID_CLASS_NAME_REGEX = Regex("^[A-Za-z0-9_]+$")

    private val PHYSICS_ALIASES = mapOf(
        "PhysicsSetXBrick" to "org.catrobat.catroid.physics.content.bricks.SetXBrick",
        "PhysicsSetYBrick" to "org.catrobat.catroid.physics.content.bricks.SetYBrick",
        "PhysicsChangeXByBrick" to "org.catrobat.catroid.physics.content.bricks.ChangeXByBrick",
        "PhysicsChangeYByBrick" to "org.catrobat.catroid.physics.content.bricks.ChangeYByBrick",
        "PhysicsPlaceAtBrick" to "org.catrobat.catroid.physics.content.bricks.PlaceAtBrick",
        "PhysicsGlideToBrick" to "org.catrobat.catroid.physics.content.bricks.GlideToBrick",
        "PhysicsSetRotationBrick" to "org.catrobat.catroid.physics.content.bricks.SetRotationBrick"
    )

    private val containerCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    fun isContainerBrick(clazz: Class<*>): Boolean {
        return containerCache.getOrPut(clazz.name) {
            findMethod(clazz, "addBrick", Brick::class.java) != null ||
                findMethod(clazz, "addBrickToIfBranch", Brick::class.java) != null ||
                findMethod(clazz, "addBrickToElseBranch", Brick::class.java) != null
        }
    }

    fun isContainerBrick(className: String): Boolean {
        val clazz = resolveBrickClass(className) ?: return false
        return isContainerBrick(clazz)
    }

    fun supportsElse(className: String): Boolean {
        val clazz = resolveBrickClass(className) ?: return false
        return findMethod(clazz, "addBrickToElseBranch", Brick::class.java) != null
    }

    sealed class BrickSpec {
        data class Simple(val className: String, val args: List<String>) : BrickSpec()
        data class Container(
            val className: String,
            val args: List<String>,
            val children: List<BrickSpec>,
            val elseChildren: List<BrickSpec>
        ) : BrickSpec()
    }

    fun createScript(scriptType: String?): Script {
        val raw = scriptType?.trim().orEmpty()
        val separatorIndex = raw.indexOf(':')
        val type = (if (separatorIndex >= 0) raw.substring(0, separatorIndex) else raw).trim()
        val param = if (separatorIndex >= 0) raw.substring(separatorIndex + 1).trim() else ""

        return when (type.lowercase()) {
            "", "startscript", "start" -> StartScript()
            "whenscript", "whentapped", "whentap", "tapped" -> WhenScript()
            "whenclonedscript", "whencloned", "cloned" -> WhenClonedScript()
            "whenconditionscript", "whencondition", "condition" ->
                if (param.isNotBlank()) WhenConditionScript(Formula(param)) else WhenConditionScript()
            "broadcastscript", "whenbroadcast", "broadcast" ->
                if (param.isNotBlank()) BroadcastScript(param) else BroadcastScript()
            else -> StartScript()
        }
    }

    fun createBrick(className: String, args: List<String>): Brick? {
        val spec = BrickSpec.Simple(className, args)
        return buildBrick(spec)
    }

    fun parseBrickSpecs(text: String): List<BrickSpec>? {
        val tokens = tokenize(text)
        val cursor = intArrayOf(0)
        val result = mutableListOf<BrickSpec>()
        while (cursor[0] < tokens.size) {
            val spec = parseOneSpec(tokens, cursor, stopAtElse = false) ?: return null
            result.add(spec)
        }
        return result
    }

    fun parseSingleBrickSpec(text: String): BrickSpec? = parseBrickSpecs(text)?.firstOrNull()

    private sealed class Tok {
        data class Name(val value: String) : Tok()
        data class Arg(val value: String) : Tok()
        object Open : Tok()
        object Close : Tok()
        object Else : Tok()
    }

    private fun tokenize(text: String): List<Tok> {
        val tokens = mutableListOf<Tok>()
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                c.isWhitespace() -> i++
                c == '{' -> { tokens.add(Tok.Open); i++ }
                c == '}' -> { tokens.add(Tok.Close); i++ }
                c == '(' -> {
                    var depth = 1
                    var j = i + 1
                    while (j < n && depth > 0) {
                        when (text[j]) {
                            '(' -> depth++
                            ')' -> depth--
                        }
                        if (depth > 0) j++
                    }
                    val inner = text.substring(i + 1, if (j < n) j else n)
                    tokens.add(Tok.Arg(inner))
                    i = j + 1
                }
                c == ',' -> i++
                else -> {
                    var j = i
                    while (j < n && !text[j].isWhitespace() && text[j] != '(' && text[j] != '{' && text[j] != '}' && text[j] != ',') {
                        j++
                    }
                    val word = text.substring(i, j)
                    if (word.equals("else", ignoreCase = true)) tokens.add(Tok.Else)
                    else tokens.add(Tok.Name(word))
                    i = j
                }
            }
        }
        return tokens
    }

    private fun parseOneSpec(tokens: List<Tok>, cur: IntArray, stopAtElse: Boolean): BrickSpec? {
        if (cur[0] >= tokens.size) return null
        val nameTok = tokens[cur[0]] as? Tok.Name ?: return null
        cur[0]++

        var args: List<String> = emptyList()
        if (cur[0] < tokens.size && tokens[cur[0]] is Tok.Arg) {
            args = splitTopLevelCommas((tokens[cur[0]] as Tok.Arg).value)
            cur[0]++
        }

        var children: List<BrickSpec> = emptyList()
        var elseChildren: List<BrickSpec> = emptyList()
        if (cur[0] < tokens.size && tokens[cur[0]] is Tok.Open) {
            cur[0]++
            val acc = mutableListOf<BrickSpec>()
            while (cur[0] < tokens.size && tokens[cur[0]] !is Tok.Close && tokens[cur[0]] !is Tok.Else) {
                val child = parseOneSpec(tokens, cur, stopAtElse = true) ?: return null
                acc.add(child)
            }
            if (cur[0] >= tokens.size || tokens[cur[0]] !is Tok.Close) return null
            cur[0]++
            children = acc

            if (cur[0] < tokens.size && tokens[cur[0]] is Tok.Else) {
                cur[0]++
                if (cur[0] >= tokens.size || tokens[cur[0]] !is Tok.Open) return null
                cur[0]++
                val elseAcc = mutableListOf<BrickSpec>()
                while (cur[0] < tokens.size && tokens[cur[0]] !is Tok.Close) {
                    val child = parseOneSpec(tokens, cur, stopAtElse = false) ?: return null
                    elseAcc.add(child)
                }
                if (cur[0] >= tokens.size || tokens[cur[0]] !is Tok.Close) return null
                cur[0]++
                elseChildren = elseAcc
            }
        }

        return if (children.isEmpty() && elseChildren.isEmpty()) {
            BrickSpec.Simple(nameTok.value, args)
        } else {
            BrickSpec.Container(nameTok.value, args, children, elseChildren)
        }
    }

    private fun splitTopLevelCommas(s: String): List<String> {
        if (s.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        for (c in s) {
            when {
                c == '(' -> { depth++; cur.append(c) }
                c == ')' -> { depth--; cur.append(c) }
                c == ',' && depth == 0 -> { out.add(cur.toString().trim()); cur.setLength(0) }
                else -> cur.append(c)
            }
        }
        if (cur.isNotBlank()) out.add(cur.toString().trim())
        return out
    }

    fun validateBrickSpec(spec: BrickSpec): String? = validateBrickSpecInternal(spec, isRoot = true)

    private fun validateBrickSpecInternal(spec: BrickSpec, isRoot: Boolean): String? {
        val className = when (spec) {
            is BrickSpec.Simple -> spec.className
            is BrickSpec.Container -> spec.className
        }
        val clazz = resolveBrickClass(className)
            ?: return "Unknown brick class '$className'"
        val isContainer = isContainerBrick(clazz)
        if (spec is BrickSpec.Container && spec.elseChildren.isNotEmpty() && !supportsElse(className)) {
            return "Brick '$className' does not support an else-branch. Use IfLogicBeginBrick instead of IfThenLogicBeginBrick."
        }
        if (spec is BrickSpec.Simple && spec.args.isNotEmpty()) {
            val hasMatchingCtor = clazz.declaredConstructors.any { it.parameterTypes.size == spec.args.size }
            if (!hasMatchingCtor) {
                val available = clazz.declaredConstructors.joinToString(", ") { "(${it.parameterTypes.size})" }
                return "Brick '$className' has no constructor for ${spec.args.size} arg(s). Available: $available."
            }
        }
        if (spec is BrickSpec.Simple && isContainer) {
            return "Brick '$className' is a container but was used without `{ }`. Wrap children: `${className} { ... }`."
        }
        if (spec is BrickSpec.Container) {
            for (child in spec.children) {
                validateBrickSpecInternal(child, isRoot = false)?.let {
                    return if (isRoot) "(inside $className) $it" else it
                }
            }
            for (child in spec.elseChildren) {
                validateBrickSpecInternal(child, isRoot = false)?.let {
                    return if (isRoot) "(inside $className else) $it" else it
                }
            }
        }
        return null
    }

    fun buildBrick(spec: BrickSpec): Brick? {
        val className = when (spec) {
            is BrickSpec.Simple -> spec.className
            is BrickSpec.Container -> spec.className
        }
        val clazz = resolveBrickClass(className) ?: return null
        val isContainer = isContainerBrick(clazz)
        if (spec is BrickSpec.Container && spec.elseChildren.isNotEmpty() && !supportsElse(className)) return null
        if (spec is BrickSpec.Simple && spec.args.isNotEmpty()) {
            val hasMatchingCtor = clazz.declaredConstructors.any { it.parameterTypes.size == spec.args.size }
            if (!hasMatchingCtor) return null
        }
        if (spec is BrickSpec.Simple && isContainer) return null

        val brick = when (spec) {
            is BrickSpec.Simple -> instantiateBrick(spec.className, spec.args)
            is BrickSpec.Container -> instantiateBrick(spec.className, spec.args)
        } ?: return null

        if (spec is BrickSpec.Container) {
            val hasElseMethod = hasMethod(brick.javaClass, "addBrickToElseBranch", Brick::class.java)
            val ifMethod = findMethod(brick.javaClass, "addBrickToIfBranch", Brick::class.java)
                ?: findMethod(brick.javaClass, "addBrick", Brick::class.java)
            for (child in spec.children) {
                val cb = buildBrick(child) ?: continue
                if (ifMethod != null) {
                    ifMethod.isAccessible = true
                    try { ifMethod.invoke(brick, cb) } catch (_: Exception) { }
                }
            }
            if (hasElseMethod) {
                val elseMethod = findMethod(brick.javaClass, "addBrickToElseBranch", Brick::class.java)!!
                for (child in spec.elseChildren) {
                    val cb = buildBrick(child) ?: continue
                    elseMethod.isAccessible = true
                    try { elseMethod.invoke(brick, cb) } catch (_: Exception) { }
                }
            }
        }
        return brick
    }

    data class BuildResult(val bricks: List<Brick>, val errors: List<String>)

    fun buildBricks(specs: List<BrickSpec>): BuildResult {
        val out = mutableListOf<Brick>()
        val errors = mutableListOf<String>()
        for (spec in specs) {
            val err = validateBrickSpec(spec)
            if (err != null) {
                errors.add("$spec — $err")
                continue
            }
            buildBrick(spec)?.let { brick ->
                resolveUserVariable(brick)
                out.add(brick)
            } ?: errors.add("$spec — construction failed")
        }
        return BuildResult(out, errors)
    }

    private fun instantiateBrick(className: String, args: List<String>): Brick? {
        val clazz = resolveBrickClass(className) ?: run {
            Log.w(TAG, "Unknown brick class: $className")
            return null
        }
        val constructors = clazz.declaredConstructors.sortedByDescending { it.parameterTypes.size }
        for (ctor in constructors) {
            if (ctor.parameterTypes.size != args.size) continue
            val instance = tryConstruct(ctor, args)
            if (instance != null) return instance as Brick
        }
        for (ctor in constructors) {
            if (ctor.parameterTypes.isEmpty()) {
                val instance = tryConstruct(ctor, emptyList())
                if (instance != null) {
                    if (args.isNotEmpty()) {
                        Log.w(TAG, "Used no-arg ctor for $className; ${args.size} arg(s) ignored")
                    }
                    return instance as Brick
                }
            }
        }
        Log.w(TAG, "No compatible constructor for $className with ${args.size} arg(s)")
        return null
    }

    private fun resolveBrickClass(name: String): Class<*>? {
        val trimmed = name.trim()
        PHYSICS_ALIASES[trimmed]?.let { fqn ->
            return try {
                val clazz = Class.forName(fqn)
                if (Brick::class.java.isAssignableFrom(clazz)) clazz else null
            } catch (_: Exception) { null }
        }
        if (!trimmed.matches(VALID_CLASS_NAME_REGEX)) {
            Log.w(TAG, "Rejected invalid class name '$trimmed' — only alphanumeric class names allowed")
            return null
        }
        for (pkg in BRICK_PACKAGES) {
            val candidate = pkg + trimmed
            try {
                val clazz = Class.forName(candidate)
                if (Brick::class.java.isAssignableFrom(clazz)) return clazz
            } catch (_: ClassNotFoundException) {
            } catch (e: Exception) {
                Log.w(TAG, "Error resolving $candidate", e)
            }
        }
        return null
    }

    private fun tryConstruct(ctor: Constructor<*>, args: List<String>): Any? {
        return try {
            ctor.isAccessible = true
            val params = ctor.parameterTypes
            val values = arrayOfNulls<Any?>(params.size)
            for (i in params.indices) {
                values[i] = coerce(params[i], args[i]) ?: return null
            }
            ctor.newInstance(*values)
        } catch (e: Exception) {
            Log.d(TAG, "Constructor ${ctor.declaringClass.simpleName} failed: ${e.message}")
            null
        }
    }

    private fun coerce(target: Class<*>, value: String): Any? {
        return try {
            when {
                target == String::class.java -> value
                target == Formula::class.java -> {
                    try {
                        FormulaParser.parse(value)
                    } catch (e: Exception) {
                        Log.w(TAG, "Formula parse failed for '$value', falling back to literal", e)
                        Formula(value)
                    }
                }
                target == Int::class.javaPrimitiveType || target == Integer::class.java ->
                    value.trim().toDouble().toInt()
                target == Double::class.javaPrimitiveType || target == java.lang.Double::class.java ->
                    value.trim().toDouble()
                target == Float::class.javaPrimitiveType || target == java.lang.Float::class.java ->
                    value.trim().toFloat()
                target == Long::class.javaPrimitiveType || target == java.lang.Long::class.java ->
                    value.trim().toDouble().toLong()
                target == Boolean::class.javaPrimitiveType || target == java.lang.Boolean::class.java ->
                    value.trim().equals("true", ignoreCase = true) || value.trim() == "1"
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun hasMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): Boolean {
        return findMethod(clazz, name, *paramTypes) != null
    }

    private fun findMethod(clazz: Class<*>, name: String, vararg paramTypes: Class<*>): java.lang.reflect.Method? {
        var c: Class<*>? = clazz
        while (c != null) {
            try {
                val m = c.getDeclaredMethod(name, *paramTypes)
                m.isAccessible = true
                return m
            } catch (_: NoSuchMethodException) {
                c = c.superclass
            }
        }
        return null
    }

    private fun resolveUserVariable(brick: Brick) {
        if (brick !is org.catrobat.catroid.content.bricks.UserVariableBrick) return
        if (brick.userVariable != null) return
        try {
            val formula = if (brick is org.catrobat.catroid.content.bricks.FormulaBrick) {
                brick.allFormulaFieldsWithFormulas.values.firstOrNull()
            } else null
            formula ?: return
            val root = formula.root ?: return
            
            val varName = when (root.elementType) {
                org.catrobat.catroid.formulaeditor.FormulaElement.ElementType.USER_VARIABLE -> root.value
                org.catrobat.catroid.formulaeditor.FormulaElement.ElementType.STRING -> {
                    val v = root.value?.trim() ?: return
                    if (v.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) v else null
                }
                else -> null
            } ?: return
            
            val project = ProjectManager.getInstance().currentProject ?: return
            val sprite = ProjectManager.getInstance().currentSprite ?: return
            
            val resolved = sprite.getUserVariable(varName) ?: project.getUserVariable(varName)
            if (resolved != null) {
                brick.userVariable = resolved
                Log.d(TAG, "Resolved userVariable '$varName' for ${brick::class.java.simpleName}")
            } else {
                Log.w(TAG, "Could not resolve userVariable '$varName' for ${brick::class.java.simpleName}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve userVariable for ${brick::class.java.simpleName}", e)
        }
    }
}
