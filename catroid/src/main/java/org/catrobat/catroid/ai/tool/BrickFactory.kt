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

/**
 * Reflection-based factory that turns the plain-text brick/script descriptions the
 * cloud model produces (via the `buildScript` tool) into real [Script] / [Brick]
 * instances.
 *
 * The brick-spec language supports:
 *  - Simple bricks: `SetXBrick(100)`
 *  - Container bricks with nested children: `ForeverBrick { MoveNStepsBrick(10) TurnRightBrick(15) }`
 *  - If/else containers: `IfLogicBeginBrick(x > 5) { SetYBrick(10) } else { SetYBrick(-10) }`
 *  - Nested containers may be arbitrarily deep.
 *  - Formula args: any string argument is auto-wrapped into a `Formula` when the
 *    constructor expects one, so expressions like `x + 1`, `random(1, 6)`, or `length of "abc"`
 *    work out of the box.
 */
object BrickFactory {

    private const val TAG = "BrickFactory"

    private val BRICK_PACKAGES = listOf(
        "org.catrobat.catroid.content.bricks.",
        "org.catrobat.catroid.physics.content.bricks."
    )

    /**
     * Short-name aliases for bricks that live in the physics package but are commonly
     * needed by the agent. The key is the alias the model uses; the value is the
     * fully-qualified class name. This avoids the "physics version shadowed by
     * content.bricks" problem (see resolveBrickClass).
     */
    private val PHYSICS_ALIASES = mapOf(
        "PhysicsSetXBrick" to "org.catrobat.catroid.physics.content.bricks.SetXBrick",
        "PhysicsSetYBrick" to "org.catrobat.catroid.physics.content.bricks.SetYBrick",
        "PhysicsChangeXByBrick" to "org.catrobat.catroid.physics.content.bricks.ChangeXByBrick",
        "PhysicsChangeYByBrick" to "org.catrobat.catroid.physics.content.bricks.ChangeYByBrick",
        "PhysicsPlaceAtBrick" to "org.catrobat.catroid.physics.content.bricks.PlaceAtBrick",
        "PhysicsGlideToBrick" to "org.catrobat.catroid.physics.content.bricks.GlideToBrick",
        "PhysicsSetRotationBrick" to "org.catrobat.catroid.physics.content.bricks.SetRotationBrick"
    )

    /**
     * Cache of "is this brick class a container?" — true if the class exposes any of
     * `addBrick`, `addBrickToIfBranch`, `addBrickToElseBranch`. Computed lazily and
     * reused by both [buildBrick] and [BrickInfo.getFullCatalog] (via [isContainerBrick]).
     */
    private val containerCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /** True if [clazz] can hold child bricks (loop/if/else container). */
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

    /** True if the brick supports an else-branch (i.e. has `addBrickToElseBranch`). */
    fun supportsElse(className: String): Boolean {
        val clazz = resolveBrickClass(className) ?: return false
        return findMethod(clazz, "addBrickToElseBranch", Brick::class.java) != null
    }

    // ---------------------------------------------------------------- specs

    sealed class BrickSpec {
        /** `BrickName(arg1, arg2, ...)` with no children. */
        data class Simple(val className: String, val args: List<String>) : BrickSpec()
        /** `BrickName(args) { children } [else { elseChildren }]`. */
        data class Container(
            val className: String,
            val args: List<String>,
            val children: List<BrickSpec>,
            val elseChildren: List<BrickSpec>
        ) : BrickSpec()
    }

    // ----------------------------------------------------------- scripts

    /**
     * Create a [Script] from a scriptType token. Supported tokens (case-insensitive):
     *  - `StartScript` (default)
     *  - `WhenScript` / `WhenTapped`
     *  - `WhenClonedScript` / `WhenCloned`
     *  - `WhenConditionScript:<formula>`
     *  - `BroadcastScript:<message>` / `WhenBroadcast:<message>`
     */
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

    // ------------------------------------------------- legacy single-brick

    /**
     * Create a single [Brick] from a class name and a flat list of string arguments.
     * Used by callers that don't need nested children (kept for backward compatibility).
     * For nested bricks use [parseBrickSpecs] + [buildBrick].
     */
    fun createBrick(className: String, args: List<String>): Brick? {
        val spec = BrickSpec.Simple(className, args)
        return buildBrick(spec)
    }

    // --------------------------------------------------------- spec parser

    /**
     * Parse a multi-line brick specification text into a list of [BrickSpec] trees.
     * Supports nested `{ }` for container bricks and `else { }` for if/else branches.
     * Returns null on syntax errors (unmatched braces, etc.).
     */
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

    /** First [BrickSpec] from [parseBrickSpecs], or null. */
    fun parseSingleBrickSpec(text: String): BrickSpec? = parseBrickSpecs(text)?.firstOrNull()

    // ---- tokenizer

    private sealed class Tok {
        data class Name(val value: String) : Tok()
        data class Arg(val value: String) : Tok()
        object Open : Tok()      // {
        object Close : Tok()     // }
        object Else : Tok()      // else
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
                    // find matching ')' respecting nested parens
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
                c == ',' -> i++ // skip commas between args (we already captured the full arg block)
                else -> {
                    // read a name token (brick class or 'else')
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

    // ---- recursive-descent parser

    private fun parseOneSpec(tokens: List<Tok>, cur: IntArray, stopAtElse: Boolean): BrickSpec? {
        if (cur[0] >= tokens.size) return null
        val nameTok = tokens[cur[0]] as? Tok.Name ?: return null
        cur[0]++

        // optional (args)
        var args: List<String> = emptyList()
        if (cur[0] < tokens.size && tokens[cur[0]] is Tok.Arg) {
            args = splitTopLevelCommas((tokens[cur[0]] as Tok.Arg).value)
            cur[0]++
        }

        // optional { children } [else { elseChildren }]
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
            cur[0]++ // skip '}'
            children = acc

            // optional else { ... }
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

    /** Split a string by commas that are NOT inside nested parentheses. */
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

    // ---------------------------------------------------- brick construction

    /**
     * Validate a [BrickSpec] before building it. Returns null if the spec is valid,
     * or a human-readable error message if it should be rejected. Used by tools to
     * give the model specific, actionable error messages (e.g. "ForeverBrick is a
     * container but was used without `{ }`").
     *
     * Validation is recursive: children and else-children are also validated, so a
     * deeply nested invalid brick is caught before construction. Error messages for
     * nested failures are prefixed with the ancestor chain for context.
     */
    fun validateBrickSpec(spec: BrickSpec): String? = validateBrickSpecInternal(spec, isRoot = true)

    /**
     * Internal validator. When [isRoot] is true, prefixes the path for nested errors.
     * The recursion itself walks children unconditionally.
     */
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
        // Recurse into children so deeply nested invalid bricks are caught here.
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

    /**
     * Build a real [Brick] from a [BrickSpec]. For container specs, children are
     * recursively built and attached via the container's `addBrick` /
     * `addBrickToIfBranch` / `addBrickToElseBranch` methods (detected via reflection).
     *
     * Returns null on validation failures. Callers SHOULD call [validateBrickSpec]
     * first to get a detailed error message; this method only re-checks the top-level
     * spec (children are assumed to be already validated by the recursive call in
     * [validateBrickSpec]).
     */
    fun buildBrick(spec: BrickSpec): Brick? {
        // Top-level check only (children already validated by the recursive validateBrickSpec).
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

        // ---- attach children ----
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

    /** Build a list of bricks from a list of specs, skipping any that fail to resolve. */
    /**
     * Build a list of bricks from specs. Returns a [BuildResult] containing the
     * successfully built bricks and any validation errors (with the spec that failed).
     * Callers can decide whether to proceed with the partial list or abort.
     */
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

    /**
     * Instantiate a brick by class name + flat string args. Tries constructors in
     * order of decreasing parameter count, coercing each arg to the parameter type.
     * Falls back to the no-arg constructor if no match is found.
     */
    private fun instantiateBrick(className: String, args: List<String>): Brick? {
        val clazz = resolveBrickClass(className) ?: run {
            Log.w(TAG, "Unknown brick class: $className")
            return null
        }
        val constructors = clazz.declaredConstructors.sortedByDescending { it.parameterTypes.size }
        // 1) exact arg-count match with successful coercion
        for (ctor in constructors) {
            if (ctor.parameterTypes.size != args.size) continue
            val instance = tryConstruct(ctor, args)
            if (instance != null) return instance as Brick
        }
        // 2) fall back to no-arg constructor (ignore args) so the brick still appears
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
        // 1) physics alias (e.g. PhysicsSetXBrick -> physics package)
        PHYSICS_ALIASES[trimmed]?.let { fqn ->
            return try {
                val clazz = Class.forName(fqn)
                if (Brick::class.java.isAssignableFrom(clazz)) clazz else null
            } catch (_: Exception) { null }
        }
        val candidates = if (trimmed.contains('.')) {
            listOf(trimmed)
        } else {
            BRICK_PACKAGES.map { it + trimmed }
        }
        for (candidate in candidates) {
            try {
                val clazz = Class.forName(candidate)
                if (Brick::class.java.isAssignableFrom(clazz)) return clazz
            } catch (_: ClassNotFoundException) {
                // try next package
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
                    // Parse the expression into a proper FormulaElement tree so that
                    // things like `x + 10`, `random(1, 6)`, or `(x > 5) and (y < 10)`
                    // actually evaluate at runtime instead of being treated as literal strings.
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

    // ------------------------------------------------ reflection helpers

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

    /**
     * If [brick] is a UserVariableBrick with a null userVariable, try to resolve it
     * from the variable name stored in the brick's Formula (first argument).
     * This allows the AI to write `SetVariableBrick("score", 1)` and have the
     * variable "score" automatically linked.
     */
    private fun resolveUserVariable(brick: Brick) {
        if (brick !is org.catrobat.catroid.content.bricks.UserVariableBrick) return
        if (brick.userVariable != null) return
        try {
            // Try to get the variable name from the brick's formula field
            val formula = if (brick is org.catrobat.catroid.content.bricks.FormulaBrick) {
                // Get the first formula field (usually the variable value or name)
                brick.allFormulaFieldsWithFormulas.values.firstOrNull()
            } else null
            formula ?: return
            val root = formula.root ?: return
            
            // For SetVariableBrick/ChangeVariableBrick, the formula contains the VALUE, not the variable name.
            // We need to check if the formula is a simple STRING (variable name) or USER_VARIABLE reference.
            val varName = when (root.elementType) {
                org.catrobat.catroid.formulaeditor.FormulaElement.ElementType.USER_VARIABLE -> root.value
                org.catrobat.catroid.formulaeditor.FormulaElement.ElementType.STRING -> {
                    // If it's a string, check if it looks like a variable name (no spaces, no special chars)
                    val v = root.value?.trim() ?: return
                    if (v.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) v else null
                }
                else -> null
            } ?: return
            
            // Resolve the variable in the current project
            val project = ProjectManager.getInstance().currentProject ?: return
            val sprite = ProjectManager.getInstance().currentSprite ?: return
            
            // Try to find the variable in sprite's local variables first, then global
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
