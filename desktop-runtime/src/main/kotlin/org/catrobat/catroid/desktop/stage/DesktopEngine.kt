package org.catrobat.catroid.desktop.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import org.catrobat.catroid.desktop.admob.DesktopAdMobManager
import org.catrobat.catroid.desktop.firebase.DesktopFirebaseManager
import org.catrobat.catroid.desktop.hardware.DesktopHardwareBridge
import org.catrobat.catroid.desktop.project.DesktopBrick
import org.catrobat.catroid.desktop.project.DesktopFormula
import org.catrobat.catroid.desktop.project.DesktopFormulaNode
import org.catrobat.catroid.desktop.project.DesktopLook
import org.catrobat.catroid.desktop.project.DesktopProject
import org.catrobat.catroid.desktop.project.DesktopScene
import org.catrobat.catroid.desktop.project.DesktopScript
import org.catrobat.catroid.desktop.project.DesktopSound
import org.catrobat.catroid.desktop.project.DesktopSprite
import org.catrobat.catroid.desktop.project.parseNeoScripts
import org.catrobat.catroid.desktop.speech.DesktopSpeechSynthesizer
import org.catrobat.catroid.runtime.RuntimeServicesHolder
import java.io.File
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class DesktopSpriteRuntime(var model: DesktopSprite) {
    var x = 0f
    var y = 0f
    var rotation = 0f
    var size = 100f
    var visible = true
    var transparency = 0f
    var brightness = 0f
    var color = 0f
    var lookIndex = 0
    var xScale = 1f
    var yScale = 1f
    var velocityX = 0f
    var velocityY = 0f
    var rotationStyle = 0
    var physicsEnabled = false
    var lastBounceSprite: String? = null

    var widthPx: Float = 80f
    var heightPx: Float = 80f

    val name: String get() = model.name
    val isBackground: Boolean get() = model.isBackground

    fun currentLook(): DesktopLook? = model.looks.getOrNull(lookIndex)
    fun hasLook(name: String): Boolean = model.looks.any { it.name == name || it.fileName == name }
    fun findLook(name: String): DesktopLook? = model.looks.firstOrNull { it.name == name || it.fileName == name }
    fun setLook(name: String) {
        val idx = model.looks.indexOfFirst { it.name == name || it.fileName == name }
        if (idx >= 0) lookIndex = idx
    }
    fun nextLook() { if (model.looks.isNotEmpty()) lookIndex = (lookIndex + 1) % model.looks.size }
    fun previousLook() { if (model.looks.isNotEmpty()) lookIndex = (lookIndex - 1 + model.looks.size) % model.looks.size }

    fun sound(name: String): DesktopSound? =
        model.sounds.firstOrNull { it.name == name || it.fileName == name }
}

class DesktopTextBubble(
    val spriteName: String,
    var text: String,
    var x: Float,
    var y: Float,
    var remaining: Float
)

class DesktopTextWidget(
    val variableName: String,
    val spriteName: String,
    var x: Float,
    var y: Float,
    var size: Float,
    var colorHex: String,
    var alignment: Int
)

enum class BrickFlow { ADVANCE, STOP }

class DesktopEngine(
    val project: DesktopProject,
    private val projectDir: File
) {
    val sprites = mutableListOf<DesktopSpriteRuntime>()
    val variables = mutableMapOf<String, Any?>()
    val spriteVariables = mutableMapOf<String, MutableMap<String, Any?>>()
    val lists = mutableMapOf<String, MutableList<Any?>>()
    val preloadedScenes = mutableSetOf<String>()
    var preloadEnabled = false
    val texts = mutableListOf<DesktopTextBubble>()
    val textWidgets = mutableListOf<DesktopTextWidget>()
    val textures = mutableMapOf<String, Texture>()

    private val speech = DesktopSpeechSynthesizer()
    private val scriptStates = mutableListOf<ScriptState>()
    private var timerSeconds = 0f
    var mouseX = 0f
    var mouseY = 0f
    var isTouched = false
    var touchDownSprite: DesktopSpriteRuntime? = null
    private var touchJustPressed = false
    private var random = java.util.Random()

    private var firebasePollTimer = 0f
    private val firebasePrevValues = mutableMapOf<String, String>()
    private var admobUnitIds = mutableMapOf<String, String>()

    private var soundManager: DesktopSoundManagerBridge? = null

    val physics = DesktopPhysicsWorld { a, b -> onPhysicsCollision(a, b) }
    val fast2d = DesktopFast2D()
    private var activeSceneIndex = 0
    private val sceneBackStack = ArrayDeque<Int>()
    var windowWidth = 0
    var windowHeight = 0
    var appMinimized = false

    private val touchingTriggers = mutableMapOf<String, Boolean>()
    private val bounceTriggers = mutableMapOf<String, Boolean>()
    private val conditionTriggers = mutableMapOf<String, Boolean>()
    private val variableTriggers = mutableMapOf<String, Any?>()
    private val backgroundTriggerValue = mutableMapOf<String, String>()
    private val timeTriggerFired = mutableMapOf<String, Boolean>()
    private val clonedNames = mutableSetOf<String>()
    private var mouseButtonPending: Int? = null
    private var scrollPending: Int = 0
    private var swipePending: String? = null
    private var spriteReleasePending: DesktopSpriteRuntime? = null
    private var fingerMovedPending = false
    private var fingerOverPending = false
    private var shakeWasActive = false
    private var focusChangedPending: Boolean? = null

    val sceneName: String get() = project.scenes.getOrNull(activeSceneIndex)?.name ?: ""

    var cameraFocusSprite: DesktopSpriteRuntime? = null
    var cameraHorizFlex = 0f
    var cameraVertFlex = 0f
    var cameraOffsetX = 0f
    var cameraOffsetY = 0f

    private fun updateCamera() {
        val focus = cameraFocusSprite ?: return
        val limitX = (project.screenWidth / 2f) * (cameraHorizFlex / 100f)
        val limitY = (project.screenHeight / 2f) * (cameraVertFlex / 100f)
        val dx = focus.x
        val dy = focus.y
        if (abs(dx) > limitX) cameraOffsetX = dx - (if (dx < 0) -limitX else limitX)
        if (abs(dy) > limitY) cameraOffsetY = dy - (if (dy < 0) -limitY else limitY)
    }

    fun setSoundBridge(bridge: DesktopSoundManagerBridge?) {
        soundManager = bridge
    }

    init {
        fast2d.touchProvider = { if (isTouched) mouseX to mouseY else null }
        fast2d.textureResolver = { name -> textures.entries.firstOrNull { it.key.endsWith(name) || name.endsWith(it.key) }?.value }
    }

    fun start() {
        sprites.clear()
        lists.clear()
        activeSceneIndex = 0
        sceneBackStack.clear()
        loadSceneSprites(activeSceneIndex)
        startSceneScripts()
    }

    private fun loadSceneSprites(index: Int) {
        sprites.clear()
        spriteVariables.clear()
        lists.clear()
        fast2d.clear()
        val scene = project.scenes.getOrNull(index) ?: return
        for (s in scene.sprites) {
            sprites += DesktopSpriteRuntime(s)
            spriteVariables[s.name] = mutableMapOf()
            if (s.looks.isNotEmpty()) {
                loadTexture(s.looks[0])
            }
        }
        for (ref in project.globalLists) {
            lists.getOrPut(ref.name) { mutableListOf() }
        }
        for (s in sprites) {
            for (ref in s.model.userLists) {
                lists.getOrPut(ref.name) { mutableListOf() }
            }
        }
        loadAllLooks()
    }

    private fun startSceneScripts() {
        for (s in sprites) {
            for (script in s.model.scripts) {
                when (script.type) {
                    "StartScript", "SceneStartScript", "WhenSceneLaunchedScript" -> {
                        if (script.type != "WhenSceneLaunchedScript" || script.sceneName.isNullOrEmpty() || script.sceneName == sceneName) {
                            startScript(s, script)
                        }
                    }
                }
            }
        }
        fireSceneLaunchedTriggers()
    }

    private fun fireSceneLaunchedTriggers() {
        for (s in sprites) {
            for (script in s.model.scripts) {
                if (script.type == "WhenSceneLaunchedScript" &&
                    (script.sceneName.isNullOrEmpty() || script.sceneName == sceneName)
                ) {
                    startScript(s, script)
                }
            }
        }
    }

    fun switchScene(name: String, additive: Boolean = false) {
        val index = project.scenes.indexOfFirst { it.name == name }
        if (index < 0) return
        if (index == activeSceneIndex) return

        // exit-скрипты текущей сцены
        for (s in sprites) {
            for (script in s.model.scripts) {
                if (script.type == "WhenSceneExitedScript" &&
                    (script.sceneName.isNullOrEmpty() || script.sceneName == sceneName)
                ) {
                    startScript(s, script)
                }
            }
        }

        sceneBackStack.addLast(activeSceneIndex)
        activeSceneIndex = index

        if (!additive) {
            killAllScripts()
            physics.clearAll()
            touchingTriggers.clear()
            bounceTriggers.clear()
            conditionTriggers.clear()
            timeTriggerFired.clear()
            texts.clear()
            textWidgets.clear()
            loadSceneSprites(index)
        } else {
            val scene = project.scenes[index]
            for (s in scene.sprites) {
                val rt = DesktopSpriteRuntime(s)
                sprites += rt
                spriteVariables[rt.name] = mutableMapOf()
                if (s.looks.isNotEmpty()) loadTexture(s.looks[0])
            }
            loadAllLooks()
        }
        startSceneScripts()
    }

    fun sceneBack() {
        val prev = sceneBackStack.removeLastOrNull() ?: return
        val name = project.scenes.getOrNull(prev)?.name ?: return
        switchSceneToIndex(prev)
    }

    private fun switchSceneToIndex(index: Int) {
        if (index == activeSceneIndex) return
        activeSceneIndex = index
        killAllScripts()
        physics.clearAll()
        touchingTriggers.clear()
        bounceTriggers.clear()
        conditionTriggers.clear()
        timeTriggerFired.clear()
        texts.clear()
        textWidgets.clear()
        loadSceneSprites(index)
        startSceneScripts()
    }

    private fun killAllScripts() {
        scriptStates.forEach { it.dead = true }
        scriptStates.clear()
    }

    private fun loadAllLooks() {
        for (s in sprites) {
            for (look in s.model.looks) loadTexture(look)
        }
    }

    private fun loadTexture(look: DesktopLook) {
        if (textures.containsKey(look.fileName)) return
        val f = File(File(projectDir, "images"), look.fileName)
        if (f.exists()) {
            try {
                textures[look.fileName] = Texture(Gdx.files.absolute(f.absolutePath))
            } catch (e: Exception) {
                GdxLog("Не удалось загрузить текстуру ${look.fileName}: $e")
            }
        }
    }

    fun dispose() {
        physics.dispose()
        for (t in textures.values) t.dispose()
        textures.clear()
    }

    // ---------- скрипты ----------

    private class Frame(
        val blocks: List<DesktopBrick>,
        var ip: Int = 0,
        var repeatRemaining: Int = 0,
        var waitTimer: Float = 0f,
        var glideX: Float = 0f,
        var glideY: Float = 0f,
        var glideStartX: Float = 0f,
        var glideStartY: Float = 0f,
        var glideDuration: Float = 0f,
        var glideElapsed: Float = 0f,
        var glideActive: Boolean = false,
        var broadcastWait: String? = null,
        var forVariable: String? = null,
        var forEnd: Float = 0f,
        var forIncrement: Float = 0f,
        var conditionBlocks: List<DesktopBrick> = emptyList(),
        var elseBlocks: List<DesktopBrick> = emptyList(),
        var asyncWait: Boolean = false,
        var asyncDone: Boolean = false,
        var asyncCallback: (() -> Unit)? = null,
        var waitUntil: DesktopFormula? = null,
        var repeatCondition: DesktopFormula? = null,
        var repeatInterval: Float = 0f,
        var asyncRepeat: AsyncRepeatInfo? = null,
        var forList: String? = null,
        var forListIndex: Int = 0
    )

    private class AsyncRepeatInfo(
        var remaining: Int,
        val loopDelay: Boolean,
        val body: List<DesktopBrick>,
        var running: Int = 0
    )

    private class ScriptState(
        val script: DesktopScript,
        val sprite: DesktopSpriteRuntime,
        val frames: ArrayDeque<Frame>
    ) {
        var dead = false
        var broadcastSource: String? = null
        var onDeath: (() -> Unit)? = null
    }

    fun tick(dt: Float) {
        timerSeconds += dt
        updateCamera()
        fast2d.step(dt)
        for (t in texts.toList()) {
            t.remaining -= dt
            if (t.remaining <= 0) texts.remove(t)
        }

        // актуальные размеры спрайтов (для физики)
        for (s in sprites) {
            s.widthPx = widthOf(s)
            s.heightPx = heightOf(s)
        }

        firebasePollTimer -= dt
        if (firebasePollTimer <= 0f) {
            firebasePollTimer = 2f
            pollFirebaseTriggers()
        }

        processPendingEvents()

        var i = 0
        while (i < scriptStates.size) {
            val state = scriptStates[i]
            if (state.dead) {
                scriptStates.removeAt(i)
                state.onDeath?.invoke()
                continue
            }
            stepState(state, dt)
            if (state.dead) {
                scriptStates.removeAt(i)
                state.onDeath?.invoke()
                continue
            }
            i++
        }
        touchJustPressed = false

        physics.step(dt)
        pollWhenScripts()
    }

    private fun stepState(state: ScriptState, dt: Float) {
        val frame = state.frames.lastOrNull() ?: run { state.dead = true; return }
        if (frame.asyncWait) {
            if (!frame.asyncDone) return
            frame.asyncWait = false
            frame.asyncDone = false
            frame.asyncCallback?.invoke()
            frame.asyncCallback = null
            frame.ip++
            return
        }
        if (frame.waitTimer > 0f) {
            frame.waitTimer -= dt
            if (frame.waitTimer > 0f) return
        }
        if (frame.asyncRepeat != null) {
            val ar = frame.asyncRepeat!!
            if (ar.remaining > 0 && (!ar.loopDelay || ar.running == 0)) {
                ar.remaining--
                ar.running++
                spawnParallelBody(state, ar.body) { ar.running-- }
            }
            if (ar.remaining <= 0 && ar.running == 0) {
                frame.asyncRepeat = null
                frame.ip++
            }
            return
        }
        if (frame.waitUntil != null) {
            val f = frame.waitUntil!!
            if (evaluateNumber(f, state.sprite) != 0f) return
            frame.waitUntil = null
        }
        if (frame.broadcastWait != null) {
            val msg = frame.broadcastWait
            val pending = scriptStates.any { it != state && !it.dead && it.broadcastSource == msg }
            if (pending) return
            frame.broadcastWait = null
        }
        if (frame.glideActive) {
            frame.glideElapsed += dt
            val t = min(1f, frame.glideElapsed / max(0.001f, frame.glideDuration))
            val eased = 1f - (1f - t) * (1f - t)
            state.sprite.x = frame.glideStartX + (frame.glideX - frame.glideStartX) * eased
            state.sprite.y = frame.glideStartY + (frame.glideY - frame.glideStartY) * eased
            if (t >= 1f) frame.glideActive = false
            return
        }

        // если верхний фрейм — контейнер и он исчерпан
        if (frame.ip >= frame.blocks.size) {
            finishFrame(state)
            return
        }

        val brick = frame.blocks[frame.ip]
        if (brick.commentedOut) {
            frame.ip++
            return
        }

        val flow = executeBrick(brick, state)
        if (flow == BrickFlow.STOP) {
            state.dead = true
            return
        }
        if (frame.asyncWait) return
        if (frame.broadcastWait != null) return
        if (frame.waitTimer > 0f) return
        if (frame.asyncRepeat != null) return
        if (frame.waitUntil != null) return
        if (state.frames.isEmpty() || frame !== state.frames.last()) return
        frame.ip++
    }

    private fun finishFrame(state: ScriptState) {
        val frame = state.frames.last()
        if (frame.repeatRemaining > 0) {
            frame.repeatRemaining--
            if (frame.repeatRemaining > 0) {
                frame.ip = 0
                if (frame.repeatInterval > 0f) frame.waitTimer = frame.repeatInterval
                return
            }
        } else if (frame.repeatRemaining == -1) {
            // forever
            frame.ip = 0
            return
        } else if (frame.repeatRemaining == -2 || frame.repeatRemaining == -3) {
            // -2 repeat-until (пока условие ложно), -3 repeat-while (пока истинно)
            val cond = frame.repeatCondition?.let { evaluateNumber(it, state.sprite) } ?: 0f
            val again = if (frame.repeatRemaining == -2) cond == 0f else cond != 0f
            if (again) {
                frame.ip = 0
                return
            }
        } else if (frame.repeatRemaining == -4) {
            // for-item-in-list: следующий элемент
            val list = frame.forList?.let { lists[it] } ?: emptyList<Any?>()
            frame.forListIndex++
            if (frame.forListIndex < list.size) {
                setVariable(state.sprite, frame.forVariable ?: "", list[frame.forListIndex])
                frame.ip = 0
                return
            }
        }
        state.frames.removeLast()
        if (state.frames.isEmpty()) {
            state.dead = true
        } else {
            // после завершения контейнера — шагнуть в родителе
            val parent = state.frames.last()
            parent.ip++
            if (parent.ip >= parent.blocks.size) finishFrame(state)
        }
    }

    private fun startScript(sprite: DesktopSpriteRuntime, script: DesktopScript): ScriptState {
        val state = ScriptState(script, sprite, ArrayDeque())
        state.frames.add(Frame(script.brickList))
        scriptStates.add(state)
        return state
    }

    fun broadcast(message: String) {
        for (s in sprites) {
            for (script in s.model.scripts) {
                if (script.type == "WhenScript" && script.broadcastMessage == message) {
                    startScript(s, script).broadcastSource = message
                }
            }
        }
    }

    fun onTouchDown(worldX: Float, worldY: Float) {
        touchJustPressed = true
        for (s in sprites) {
            for (script in s.model.scripts) {
                if (script.type == "WhenTouchDownScript") {
                    startScript(s, script)
                }
            }
        }
    }

    fun spriteAt(wx: Float, wy: Float): DesktopSpriteRuntime? {
        for (s in sprites.asReversed()) {
            if (s.isBackground || !s.visible) continue
            if (isPointOnSprite(wx, wy, s)) return s
        }
        return null
    }

    // ---------- переменные ----------

    private fun getVariable(sprite: DesktopSpriteRuntime?, name: String): Any? {
        if (sprite != null) {
            spriteVariables[sprite.name]?.get(name)?.let { return it }
        }
        return variables[name]
    }

    private fun setVariable(sprite: DesktopSpriteRuntime?, name: String, value: Any?) {
        val knownSpriteVar = sprite != null && spriteVariables[sprite.name]?.containsKey(name) == true
        if (knownSpriteVar) {
            spriteVariables[sprite.name]!![name] = value
        } else {
            variables[name] = value
        }
    }

    // ---------- формулы ----------

    fun evaluateFormula(
        formula: DesktopFormula?,
        sprite: DesktopSpriteRuntime?
    ): Any? {
        if (formula == null) return 0.0
        return evalNode(formula.toNode(), sprite)
    }

    fun evaluateString(formula: DesktopFormula?, sprite: DesktopSpriteRuntime?): String {
        val v = evaluateFormula(formula, sprite)
        return when (v) {
            null -> ""
            is Double -> if (v == floor(v) && !v.isInfinite()) v.toLong().toString() else v.toString()
            else -> v.toString()
        }
    }

    fun evaluateNumber(formula: DesktopFormula?, sprite: DesktopSpriteRuntime?): Float {
        val v = evaluateFormula(formula, sprite)
        return when (v) {
            is Double -> v.toFloat()
            is Float -> v
            is Int -> v.toFloat()
            is Long -> v.toFloat()
            is Boolean -> if (v) 1f else 0f
            is String -> v.toDoubleOrNull()?.toFloat() ?: 0f
            else -> 0f
        }
    }

    private fun evalNode(node: DesktopFormulaNode, sprite: DesktopSpriteRuntime?): Any? {
        return when (node) {
            is DesktopFormulaNode.Num -> node.v
            is DesktopFormulaNode.Str -> node.s
            is DesktopFormulaNode.Var -> getVariable(sprite, node.name) ?: 0.0
            is DesktopFormulaNode.ListRef -> 0.0
            is DesktopFormulaNode.Sensor -> evalSensor(node.sensor, sprite)
            is DesktopFormulaNode.Func -> evalFunction(node.func, node.args, sprite)
            is DesktopFormulaNode.Op -> evalOperator(node.op, node.l, node.r, sprite)
            is DesktopFormulaNode.Null -> 0.0
        }
    }

    private fun evalOperator(op: String, l: DesktopFormulaNode?, r: DesktopFormulaNode?, sprite: DesktopSpriteRuntime?): Any? {
        val lv = if (l != null) evalNode(l, sprite) else null
        val rv = if (r != null) evalNode(r, sprite) else null
        val a = num(lv)
        val b = num(rv)
        return when (op) {
            "PLUS" -> a + b
            "MINUS" -> a - b
            "MULT" -> a * b
            "DIVIDE" -> if (b == 0.0) 0.0 else a / b
            "MOD" -> if (b == 0.0) 0.0 else a % b
            "POW" -> a.pow(b)
            "EQUAL" -> bool(a == b)
            "NOT_EQUAL" -> bool(a != b)
            "SMALLER_THAN" -> bool(a < b)
            "GREATER_THAN" -> bool(a > b)
            "SMALLER_OR_EQUAL" -> bool(a <= b)
            "GREATER_OR_EQUAL" -> bool(a >= b)
            "LOGICAL_AND" -> bool((a != 0.0) && (b != 0.0))
            "LOGICAL_OR" -> bool((a != 0.0) || (b != 0.0))
            "LOGICAL_NOT" -> bool(a == 0.0)
            "JOIN" -> str(lv) + str(rv)
            else -> 0.0
        }
    }

    private fun evalFunction(func: String, args: List<DesktopFormulaNode>, sprite: DesktopSpriteRuntime?): Any? {
        val n = args.size
        val a0 = if (n > 0) num(evalNode(args[0], sprite)) else 0.0
        val a1 = if (n > 1) num(evalNode(args[1], sprite)) else 0.0
        val a2 = if (n > 2) num(evalNode(args[2], sprite)) else 0.0
        val s0 = if (n > 0) str(evalNode(args[0], sprite)) else ""

        return when (func) {
            "SIN" -> sin(a0 * PI / 180.0)
            "COS" -> cos(a0 * PI / 180.0)
            "TAN" -> tan(a0 * PI / 180.0)
            "ARCSIN" -> asin(a0) * 180.0 / PI
            "ARCCOS" -> acos(a0) * 180.0 / PI
            "ARCTAN" -> atan(a0) * 180.0 / PI
            "ARCTAN2" -> atan2(a0, a1) * 180.0 / PI
            "LN" -> ln(a0)
            "LOG" -> log10(a0)
            "SQRT" -> sqrt(a0)
            "ABS" -> abs(a0)
            "ROUND" -> round(a0).toDouble()
            "FLOOR" -> floor(a0)
            "CEIL" -> ceil(a0)
            "EXP" -> exp(a0)
            "PI" -> PI
            "TRUE" -> 1.0
            "FALSE" -> 0.0
            "RAND" -> if (n >= 2) randomBetween(min(a0, a1), max(a0, a1)) else randomBetween(0.0, 1.0)
            "MAX" -> max(a0, a1)
            "MIN" -> min(a0, a1)
            "POWER" -> a0.pow(a1)
            "MOD" -> if (a1 == 0.0) 0.0 else a0 % a1
            "ROUNDTO" -> {
                val factor = 10.0.pow(a1.toInt())
                round(a0 * factor) / factor
            }
            "CLAMP" -> min(max(a0, a1), a2)
            "LENGTH" -> s0.length.toDouble()
            "LETTER" -> if (s0.isNotEmpty()) s0.getOrElse(a1.toInt() - 1) { ' ' }.toString() else ""
            "SUBTEXT" -> {
                val start = a1.toInt().coerceAtLeast(1)
                val len = a2.toInt().coerceAtLeast(0)
                if (s0.isEmpty() || start > s0.length) "" else s0.substring(start - 1, min(s0.length, start - 1 + len))
            }
            "UPPER" -> s0.uppercase()
            "LOWER" -> s0.lowercase()
            "JOIN" -> args.joinToString("") { str(evalNode(it, sprite)) }
            "JOIN3" -> args.joinToString("") { str(evalNode(it, sprite)) }
            "REVERSE" -> s0.reversed()
            "SCREEN_WIDTH" -> project.screenWidth.toDouble()
            "SCREEN_HEIGHT" -> project.screenHeight.toDouble()
            "DEVICE_NAME" -> "Desktop"
            "USER_LANGUAGE" -> "ru"
            "SYSTEM_LANGUAGE" -> "ru"
            "TIMER" -> timerSeconds.toDouble()
            "CURRENT_SCENE_NAME" -> sceneName
            "SCENE_TIME" -> timerSeconds.toDouble()
            "RAY_DID_HIT2" -> {
                val r = physics.rayResult(s0)
                if (r != null && r.hasHit) 1.0 else 0.0
            }
            "RAY_HIT_SPRITE_NAME" -> physics.rayResult(s0)?.sprite?.name ?: ""
            "RAY_HIT_X" -> {
                val r = physics.rayResult(s0)
                if (r != null && r.hasHit) r.x.toDouble() else 0.0
            }
            "RAY_HIT_Y" -> {
                val r = physics.rayResult(s0)
                if (r != null && r.hasHit) r.y.toDouble() else 0.0
            }
            "RAY_HIT_DISTANCE" -> {
                val r = physics.rayResult(s0)
                if (r != null && r.hasHit && sprite != null) {
                    hypot((r.x - sprite.x).toDouble(), (r.y - sprite.y).toDouble())
                } else 0.0
            }
            "F2D_X" -> fast2d.getX(s0).toDouble()
            "F2D_Y" -> fast2d.getY(s0).toDouble()
            "F2D_ROTATION" -> fast2d.getRotation(s0).toDouble()
            "F2D_SCALE_X" -> fast2d.getScaleX(s0).toDouble()
            "F2D_SCALE_Y" -> fast2d.getScaleY(s0).toDouble()
            "F2D_COLOR_R" -> fast2d.getColorR(s0).toDouble()
            "F2D_COLOR_G" -> fast2d.getColorG(s0).toDouble()
            "F2D_COLOR_B" -> fast2d.getColorB(s0).toDouble()
            "F2D_ALPHA" -> fast2d.getAlpha(s0).toDouble()
            "F2D_TEXTURE" -> fast2d.getTextureName(s0)
            "F2D_CAM_X" -> fast2d.getCamX().toDouble()
            "F2D_CAM_Y" -> fast2d.getCamY().toDouble()
            "F2D_CAM_ZOOM" -> fast2d.getCamZoom().toDouble()
            "F2D_IS_TOUCHED" -> if (fast2d.isTouched(s0)) 1.0 else 0.0
            "F2D_IS_TOUCHED_INDEX" -> {
                if (n > 1 && a1 != 0.0) 0.0 else if (fast2d.isTouched(s0)) 1.0 else 0.0
            }
            else -> 0.0
        }
    }

    private fun evalSensor(sensor: String, sprite: DesktopSpriteRuntime?): Any? {
        return when (sensor) {
            "OBJECT_X" -> sprite?.x?.toDouble() ?: 0.0
            "OBJECT_Y" -> sprite?.y?.toDouble() ?: 0.0
            "OBJECT_DIRECTION" -> sprite?.rotation?.toDouble() ?: 0.0
            "MOTION_DIRECTION" -> {
                val s = sprite ?: return 0.0
                if (s.velocityX == 0f && s.velocityY == 0f) 0.0
                else Math.toDegrees(atan2(s.velocityY.toDouble(), s.velocityX.toDouble()))
            }
            "OBJECT_SIZE" -> sprite?.size?.toDouble() ?: 0.0
            "OBJECT_WIDTH" -> sprite?.let { widthOf(it).toDouble() } ?: 0.0
            "OBJECT_HEIGHT" -> sprite?.let { heightOf(it).toDouble() } ?: 0.0
            "OBJECT_TRANSPARENCY" -> sprite?.transparency?.toDouble() ?: 0.0
            "OBJECT_BRIGHTNESS" -> sprite?.brightness?.toDouble() ?: 0.0
            "OBJECT_COLOR" -> sprite?.color?.toDouble() ?: 0.0
            "OBJECT_LOOK_NUMBER" -> (sprite?.lookIndex?.plus(1))?.toDouble() ?: 0.0
            "OBJECT_NUMBER_OF_LOOKS" -> sprite?.model?.looks?.size?.toDouble() ?: 0.0
            "OBJECT_X_VELOCITY" -> sprite?.velocityX?.toDouble() ?: 0.0
            "OBJECT_Y_VELOCITY" -> sprite?.velocityY?.toDouble() ?: 0.0
            "STAGE_WIDTH" -> project.screenWidth.toDouble()
            "STAGE_HEIGHT" -> project.screenHeight.toDouble()
            "MOUSE_X" -> mouseX.toDouble()
            "MOUSE_Y" -> mouseY.toDouble()
            "MOUSE_DELTA_X" -> 0.0
            "MOUSE_DELTA_Y" -> 0.0
            "FINGER_X" -> mouseX.toDouble()
            "FINGER_Y" -> mouseY.toDouble()
            "FINGER_TOUCHED" -> if (isTouched) 1.0 else 0.0
            "NUMBER_CURRENT_TOUCHES" -> if (isTouched) 1.0 else 0.0
            "LAST_FINGER_INDEX" -> if (isTouched) 0.0 else -1.0
            "DATE_YEAR" -> Calendar.getInstance().get(Calendar.YEAR).toDouble()
            "DATE_MONTH" -> (Calendar.getInstance().get(Calendar.MONTH) + 1).toDouble()
            "DATE_DAY" -> Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toDouble()
            "DATE_WEEKDAY" -> (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1).toDouble()
            "TIME_HOUR" -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toDouble()
            "TIME_MINUTE" -> Calendar.getInstance().get(Calendar.MINUTE).toDouble()
            "TIME_SECOND" -> Calendar.getInstance().get(Calendar.SECOND).toDouble()
            "X_ACCELERATION", "Y_ACCELERATION", "Z_ACCELERATION", "COMPASS_DIRECTION",
            "LATITUDE", "LONGITUDE" -> 0.0
            else -> 0.0
        }
    }

    fun widthOf(sprite: DesktopSpriteRuntime): Float {
        val look = sprite.currentLook() ?: return 0f
        val texture = textures[look.fileName] ?: return 0f
        return sprite.size / 100f * texture.width * abs(sprite.xScale)
    }

    fun heightOf(sprite: DesktopSpriteRuntime): Float {
        val look = sprite.currentLook() ?: return 0f
        val texture = textures[look.fileName] ?: return 0f
        return sprite.size / 100f * texture.height * abs(sprite.yScale)
    }

    private fun randomBetween(a: Double, b: Double): Double =
        if (a > b) randomBetween(b, a) else a + (b - a) * random.nextDouble()

    private fun num(v: Any?): Double = when (v) {
        is Double -> v
        is Float -> v.toDouble()
        is Int -> v.toDouble()
        is Long -> v.toDouble()
        is Boolean -> if (v) 1.0 else 0.0
        is String -> v.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    private fun str(v: Any?): String = when (v) {
        null -> ""
        is Double -> if (v == floor(v) && !v.isInfinite()) v.toLong().toString() else v.toString()
        else -> v.toString()
    }

    private fun bool(b: Boolean): Double = if (b) 1.0 else 0.0

    // ---------- блоки ----------

    private fun executeBrick(brick: DesktopBrick, state: ScriptState): BrickFlow {
        val sprite = state.sprite
        return when (brick.type) {
            // ---------- Motion ----------
            "PlaceAtBrick" -> {
                sprite.x = evaluateNumber(brick.field("X_POSITION"), sprite)
                sprite.y = evaluateNumber(brick.field("Y_POSITION"), sprite)
                if (sprite.physicsEnabled) physics.syncSpriteToBody(sprite)
                BrickFlow.ADVANCE
            }
            "SetXBrick", "SetXPositionBrick" -> {
                sprite.x = evaluateNumber(brick.field("X_POSITION"), sprite)
                if (sprite.physicsEnabled) physics.syncSpriteToBody(sprite)
                BrickFlow.ADVANCE
            }
            "SetYBrick", "SetYPositionBrick" -> {
                sprite.y = evaluateNumber(brick.field("Y_POSITION"), sprite)
                if (sprite.physicsEnabled) physics.syncSpriteToBody(sprite)
                BrickFlow.ADVANCE
            }
            "ChangeXByNBrick" -> {
                sprite.x += evaluateNumber(brick.field("X_CHANGE"), sprite)
                if (sprite.physicsEnabled) physics.syncSpriteToBody(sprite)
                BrickFlow.ADVANCE
            }
            "ChangeYByNBrick" -> {
                sprite.y += evaluateNumber(brick.field("Y_CHANGE"), sprite)
                if (sprite.physicsEnabled) physics.syncSpriteToBody(sprite)
                BrickFlow.ADVANCE
            }
            "MoveNStepsBrick" -> {
                val steps = evaluateNumber(brick.field("STEPS"), sprite)
                val rad = Math.toRadians(sprite.rotation.toDouble())
                sprite.x += (steps * cos(rad)).toFloat()
                sprite.y += (steps * sin(rad)).toFloat()
                BrickFlow.ADVANCE
            }
            "TurnLeftBrick" -> {
                sprite.rotation += evaluateNumber(brick.field("TURN_LEFT_DEGREES"), sprite)
                BrickFlow.ADVANCE
            }
            "TurnRightBrick" -> {
                sprite.rotation += evaluateNumber(brick.field("TURN_RIGHT_DEGREES"), sprite)
                BrickFlow.ADVANCE
            }
            "PointInDirectionBrick" -> {
                sprite.rotation = evaluateNumber(brick.field("DEGREES"), sprite)
                BrickFlow.ADVANCE
            }
            "PointToBrick" -> {
                val targetName = brick.value("pointedObject")
                val target = resolveTargetName(targetName, sprite)
                if (target != null) {
                    sprite.rotation = Math.toDegrees(
                        atan2((target.y - sprite.y).toDouble(), (target.x - sprite.x).toDouble())
                    ).toFloat()
                } else {
                    sprite.rotation = random.nextFloat() * 360f
                }
                BrickFlow.ADVANCE
            }
            "GoToBrick" -> {
                val dest = brick.value("spinnerSelection") ?: ""
                when (dest) {
                    "80" -> { // touch
                        sprite.x = mouseX
                        sprite.y = mouseY
                    }
                    "81" -> { // random
                        sprite.x = randomBetween(-project.screenWidth / 2.0, project.screenWidth / 2.0).toFloat()
                        sprite.y = randomBetween(-project.screenHeight / 2.0, project.screenHeight / 2.0).toFloat()
                    }
                    "82" -> { // other sprite
                        val target = resolveTargetName(brick.value("destinationSprite"), sprite)
                        if (target != null) {
                            sprite.x = target.x
                            sprite.y = target.y
                        }
                    }
                }
                BrickFlow.ADVANCE
            }
            "GlideToBrick" -> {
                val frame = state.frames.last()
                frame.glideStartX = sprite.x
                frame.glideStartY = sprite.y
                frame.glideX = evaluateNumber(brick.field("X_POSITION"), sprite)
                frame.glideY = evaluateNumber(brick.field("Y_POSITION"), sprite)
                frame.glideDuration = evaluateNumber(brick.field("DURATION_IN_SECONDS"), sprite)
                frame.glideElapsed = 0f
                frame.glideActive = true
                BrickFlow.ADVANCE
            }
            "IfOnEdgeBounceBrick" -> {
                val halfW = widthOf(sprite) / 2f
                val halfH = heightOf(sprite) / 2f
                val maxX = project.screenWidth / 2f
                val maxY = project.screenHeight / 2f
                if (sprite.x - halfW < -maxX) { sprite.x = -maxX + halfW; bounceX(sprite) }
                if (sprite.x + halfW > maxX) { sprite.x = maxX - halfW; bounceX(sprite) }
                if (sprite.y - halfH < -maxY) { sprite.y = -maxY + halfH; bounceY(sprite) }
                if (sprite.y + halfH > maxY) { sprite.y = maxY - halfH; bounceY(sprite) }
                BrickFlow.ADVANCE
            }
            "ComeToFrontBrick" -> {
                val idx = sprites.indexOf(sprite)
                if (idx >= 0 && idx != sprites.lastIndex) {
                    sprites.removeAt(idx)
                    sprites.add(sprite)
                }
                BrickFlow.ADVANCE
            }
            "GoNStepsBackBrick" -> {
                val steps = evaluateNumber(brick.field("STEPS"), sprite).toInt().coerceAtLeast(1)
                val idx = sprites.indexOf(sprite)
                if (idx >= 0) {
                    val newIdx = (idx - steps).coerceAtLeast(0)
                    if (newIdx != idx) {
                        sprites.removeAt(idx)
                        sprites.add(newIdx, sprite)
                    }
                }
                BrickFlow.ADVANCE
            }
            "TurnLeftSpeedBrick" -> {
                physics.setAngularVelocity(
                    sprite,
                    -evaluateNumber(brick.field("PHYSICS_TURN_LEFT_SPEED"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "TurnRightSpeedBrick" -> {
                physics.setAngularVelocity(
                    sprite,
                    evaluateNumber(brick.field("PHYSICS_TURN_RIGHT_SPEED"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "SetRotationStyleBrick" -> {
                sprite.rotationStyle = brick.value("selection")?.toIntOrNull() ?: 0
                BrickFlow.ADVANCE
            }
            "SetCameraFocusPointBrick" -> {
                cameraFocusSprite = sprite
                cameraHorizFlex = evaluateNumber(brick.field("HORIZONTAL_FLEXIBILITY"), sprite)
                cameraVertFlex = evaluateNumber(brick.field("VERTICAL_FLEXIBILITY"), sprite)
                BrickFlow.ADVANCE
            }
            "TouchDirectionBrick" -> {
                val dx = mouseX - sprite.x
                val dy = mouseY - sprite.y
                sprite.rotation = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                BrickFlow.ADVANCE
            }

            // ---------- Looks ----------
            "ShowBrick" -> { sprite.visible = true; BrickFlow.ADVANCE }
            "HideBrick" -> { sprite.visible = false; BrickFlow.ADVANCE }
            "SetLookBrick" -> {
                brick.lookRefs.values.firstOrNull()?.let { sprite.setLook(it) }
                val idx = brick.value("spinnerSelection")?.toIntOrNull()
                if (idx != null && sprite.model.looks.isNotEmpty()) {
                    sprite.lookIndex = (idx - 1).coerceIn(0, sprite.model.looks.size - 1)
                }
                BrickFlow.ADVANCE
            }
            "NextLookBrick" -> { sprite.nextLook(); BrickFlow.ADVANCE }
            "PreviousLookBrick" -> { sprite.previousLook(); BrickFlow.ADVANCE }
            "SetSizeToBrick" -> {
                sprite.size = evaluateNumber(brick.field("SIZE"), sprite)
                BrickFlow.ADVANCE
            }
            "ChangeSizeByNBrick" -> {
                sprite.size += evaluateNumber(brick.field("CHANGE_SIZE"), sprite)
                BrickFlow.ADVANCE
            }
            "SetTransparencyBrick" -> {
                sprite.transparency = evaluateNumber(brick.field("TRANSPARENCY"), sprite)
                BrickFlow.ADVANCE
            }
            "ChangeTransparencyByNBrick" -> {
                sprite.transparency += evaluateNumber(brick.field("TRANSPARENCY_CHANGE"), sprite)
                BrickFlow.ADVANCE
            }
            "SetBrightnessBrick" -> {
                sprite.brightness = evaluateNumber(brick.field("BRIGHTNESS"), sprite)
                BrickFlow.ADVANCE
            }
            "ChangeBrightnessByNBrick" -> {
                sprite.brightness += evaluateNumber(brick.field("BRIGHTNESS_CHANGE"), sprite)
                BrickFlow.ADVANCE
            }
            "SetColorBrick" -> {
                sprite.color = evaluateNumber(brick.field("COLOR"), sprite)
                BrickFlow.ADVANCE
            }
            "ChangeColorByNBrick" -> {
                sprite.color += evaluateNumber(brick.field("COLOR_CHANGE"), sprite)
                BrickFlow.ADVANCE
            }
            "ClearGraphicEffectBrick" -> {
                sprite.transparency = 0f
                sprite.brightness = 0f
                sprite.color = 0f
                BrickFlow.ADVANCE
            }
            "SetScaleBrick" -> {
                val scale = evaluateNumber(brick.field("SCALE"), sprite) / 100f
                sprite.xScale = scale
                sprite.yScale = scale
                BrickFlow.ADVANCE
            }
            "SetWidthBrick", "SetHeightBrick" -> BrickFlow.ADVANCE
            "SetBackgroundBrick" -> {
                val idx = brick.value("spinnerSelection")?.toIntOrNull()
                val bg = sprites.firstOrNull { it.isBackground } ?: return BrickFlow.ADVANCE
                if (idx != null && bg.model.looks.isNotEmpty()) {
                    bg.lookIndex = (idx - 1).coerceIn(0, bg.model.looks.size - 1)
                }
                BrickFlow.ADVANCE
            }

            // ---------- Sound ----------
            "PlaySoundBrick" -> {
                val soundName = brick.soundRefs.values.firstOrNull()
                if (soundName != null) soundManager?.playSound(sprite.sound(soundName))
                BrickFlow.ADVANCE
            }
            "PlaySoundAndWaitBrick" -> {
                val soundName = brick.soundRefs.values.firstOrNull()
                if (soundName != null) {
                    val s = sprite.sound(soundName)
                    if (s != null) {
                        soundManager?.playSound(s)
                        state.frames.last().waitTimer = soundManager?.durationOf(s) ?: 0.5f
                    }
                }
                BrickFlow.ADVANCE
            }
            "StopAllSoundsBrick" -> {
                soundManager?.stopAll()
                BrickFlow.ADVANCE
            }
            "SetVolumeToBrick" -> {
                soundManager?.setVolume(evaluateNumber(brick.field("VOLUME"), sprite))
                BrickFlow.ADVANCE
            }
            "ChangeVolumeByNBrick" -> {
                soundManager?.changeVolume(evaluateNumber(brick.field("VOLUME_CHANGE"), sprite))
                BrickFlow.ADVANCE
            }
            "SetSoundVolumeBrick", "SetGlobalSoundVolumeBrick", "SetGameVolumeBrick" -> BrickFlow.ADVANCE

            // ---------- Speech ----------
            "SpeakBrick" -> {
                val text = evaluateString(brick.field("SPEECH_TEXT"), sprite)
                if (text.isNotEmpty()) speak(text)
                BrickFlow.ADVANCE
            }
            "SpeakAndWaitBrick" -> {
                val text = evaluateString(brick.field("SPEECH_TEXT"), sprite)
                if (text.isNotEmpty()) speak(text)
                BrickFlow.ADVANCE
            }

            // ---------- Control ----------
            "WaitBrick" -> {
                state.frames.last().waitTimer = evaluateNumber(brick.field("TIME_TO_WAIT_IN_SECONDS"), sprite)
                BrickFlow.ADVANCE
            }
            "NoteBrick" -> BrickFlow.ADVANCE
            "FinishStageBrick" -> {
                println("FinishStage: ${project.name}")
                GdxExit()
                BrickFlow.STOP
            }
            "StopScriptBrick" -> {
                val option = brick.value("spinnerSelection") ?: "0"
                if (option == "1") {
                    // stop all scripts
                    scriptStates.forEach { it.dead = true }
                    BrickFlow.STOP
                } else if (option == "0") {
                    BrickFlow.STOP
                } else {
                    BrickFlow.ADVANCE
                }
            }
            "StopOtherScriptsBrick" -> {
                val self = state
                scriptStates.forEach { if (it !== self) it.dead = true }
                BrickFlow.ADVANCE
            }
            "BroadcastBrick" -> {
                val msg = evaluateString(brick.field("BROADCAST_MESSAGE"), sprite)
                if (msg.isNotEmpty()) broadcast(msg)
                BrickFlow.ADVANCE
            }
            "BroadcastWaitBrick" -> {
                val msg = evaluateString(brick.field("BROADCAST_MESSAGE"), sprite)
                if (msg.isNotEmpty()) {
                    broadcast(msg)
                    state.frames.last().broadcastWait = msg
                }
                BrickFlow.ADVANCE
            }
            "ForeverBrick" -> {
                state.frames.last().repeatRemaining = -1
                pushChildFrame(state, brick)
                BrickFlow.ADVANCE
            }
            "RepeatBrick" -> {
                val times = evaluateNumber(brick.field("TIMES_TO_REPEAT"), sprite).toInt()
                val frame = state.frames.last()
                frame.repeatRemaining = max(0, times)
                pushChildFrame(state, brick)
                BrickFlow.ADVANCE
            }
            "RepeatUntilBrick" -> {
                val frame = state.frames.last()
                frame.repeatRemaining = -2
                frame.repeatCondition = brick.field("REPEAT_UNTIL_CONDITION")
                pushChildFrame(state, brick)
                BrickFlow.ADVANCE
            }
            "RepeatWhileBrick" -> {
                val cond = evaluateNumber(brick.field("REPEAT_UNTIL_CONDITION"), sprite) != 0f
                val frame = state.frames.last()
                if (cond) {
                    frame.repeatRemaining = -3
                    frame.repeatCondition = brick.field("REPEAT_UNTIL_CONDITION")
                    pushChildFrame(state, brick)
                }
                BrickFlow.ADVANCE
            }
            "AsyncRepeatBrick" -> {
                val times = evaluateNumber(brick.field("TIMES_TO_REPEAT"), sprite).toInt()
                val loopDelay = brick.value("isLoopDelay") == "true" || brick.value("isLoopDelaySelection") == "1"
                val frame = state.frames.last()
                frame.asyncRepeat = AsyncRepeatInfo(max(0, times), loopDelay, brick.children["loopBricks"] ?: emptyList())
                BrickFlow.ADVANCE
            }
            "IntervalRepeatBrick" -> {
                val times = evaluateNumber(brick.field("TIMES_TO_REPEAT"), sprite).toInt()
                val interval = max(0f, evaluateNumber(brick.field("INTERVAL"), sprite))
                val frame = state.frames.last()
                if (times > 0) {
                    frame.repeatRemaining = times
                    frame.repeatInterval = interval
                    frame.waitTimer = interval
                    pushChildFrame(state, brick)
                }
                BrickFlow.ADVANCE
            }
            "ForItemInUserListBrick" -> {
                val listName = brick.variableRefs["userList"] ?: ""
                val varName = brick.variableRefs["userVariable"] ?: ""
                val list = lists[listName] ?: emptyList<Any?>()
                val frame = state.frames.last()
                if (list.isNotEmpty()) {
                    frame.repeatRemaining = -4
                    frame.forList = listName
                    frame.forVariable = varName
                    frame.forListIndex = 0
                    setVariable(sprite, varName, list[0])
                    pushChildFrame(state, brick)
                }
                BrickFlow.ADVANCE
            }
            "WaitWhileBrick" -> {
                if (evaluateNumber(brick.field("IF_CONDITION"), sprite) != 0f) {
                    state.frames.last().waitUntil = brick.field("IF_CONDITION")
                }
                BrickFlow.ADVANCE
            }
            "IfLogicBeginBrick" -> {
                val condition = evaluateNumber(brick.field("IF_CONDITION"), sprite) != 0f
                val frame = state.frames.last()
                frame.conditionBlocks = brick.children["ifBricks"] ?: emptyList()
                frame.elseBlocks = brick.children["elseBricks"] ?: emptyList()
                val child = Frame(if (condition) frame.conditionBlocks else frame.elseBlocks)
                state.frames.add(child)
                BrickFlow.ADVANCE
            }
            "IfThenLogicBeginBrick" -> {
                val condition = evaluateNumber(brick.field("IF_CONDITION"), sprite) != 0f
                val frame = state.frames.last()
                frame.conditionBlocks = brick.children["ifBricks"] ?: emptyList()
                val child = Frame(if (condition) frame.conditionBlocks else emptyList())
                state.frames.add(child)
                BrickFlow.ADVANCE
            }
            "ForVariableFromToBrick" -> {
                val varName = brick.variableRefs["userVariable"] ?: ""
                val frame = state.frames.last()
                frame.forVariable = varName
                frame.forEnd = evaluateNumber(brick.field("TO"), sprite)
                frame.forIncrement = 1f
                setVariable(sprite, varName, evaluateNumber(brick.field("FROM"), sprite))
                pushChildFrame(state, brick)
                BrickFlow.ADVANCE
            }
            "CloneObjectBrick" -> {
                cloneSprite(sprite)
                BrickFlow.ADVANCE
            }
            "DeleteThisCloneBrick" -> {
                sprite.visible = false
                BrickFlow.STOP
            }
            "WaitTillIdleBrick", "WaitUntilBrick", "ExecuteForCloneNumberBrick",
            "RunAsSpriteBrick", "RunOnUiThreadBrick", "TryCatchFinallyBrick" -> BrickFlow.ADVANCE

            // ---------- Variables ----------
            "SetVariableBrick" -> {
                val name = brick.variableRefs["userVariable"]
                if (name != null) {
                    setVariable(sprite, name, evaluateFormula(brick.field("VARIABLE"), sprite))
                }
                BrickFlow.ADVANCE
            }
            "ChangeVariableByNBrick" -> {
                val name = brick.variableRefs["userVariable"]
                if (name != null) {
                    val current = num(getVariable(sprite, name))
                    setVariable(sprite, name, current + num(evaluateFormula(brick.field("VARIABLE_CHANGE"), sprite)))
                }
                BrickFlow.ADVANCE
            }

            // ---------- Look text ----------
            "SayBrick" -> {
                val text = evaluateString(brick.field("STRING"), sprite)
                texts += DesktopTextBubble(sprite.name, text, sprite.x, sprite.y, 2f)
                BrickFlow.ADVANCE
            }
            "SayForBubbleBrick" -> {
                val text = evaluateString(brick.field("STRING"), sprite)
                val duration = evaluateNumber(brick.field("DURATION_IN_SECONDS"), sprite)
                texts += DesktopTextBubble(sprite.name, text, sprite.x, sprite.y, duration)
                BrickFlow.ADVANCE
            }
            "ThinkBrick" -> {
                val text = evaluateString(brick.field("STRING"), sprite)
                texts += DesktopTextBubble(sprite.name, "… $text", sprite.x, sprite.y, 2f)
                BrickFlow.ADVANCE
            }
            "ThinkForBubbleBrick" -> {
                val text = evaluateString(brick.field("STRING"), sprite)
                val duration = evaluateNumber(brick.field("DURATION_IN_SECONDS"), sprite)
                texts += DesktopTextBubble(sprite.name, "… $text", sprite.x, sprite.y, duration)
                BrickFlow.ADVANCE
            }

            // ---------- Firebase Realtime Database ----------
            "WriteBaseBrick" -> {
                val url = evaluateString(brick.field("FIREBASE_ID"), sprite)
                val key = evaluateString(brick.field("FIREBASE_KEY"), sprite)
                val value = evaluateString(brick.field("FIREBASE_VALUE"), sprite)
                if (url.isNotEmpty() && key.isNotEmpty()) {
                    val wait = brick.value("waitForResponseSelection") != "0"
                    if (wait) {
                        startAsyncWait(state) { done ->
                            DesktopFirebaseManager.writeToDatabase(url, key, value) {
                                post { done() }
                            }
                        }
                    } else {
                        DesktopFirebaseManager.writeToDatabase(url, key, value)
                    }
                }
                BrickFlow.ADVANCE
            }
            "ReadBaseBrick" -> {
                val url = evaluateString(brick.field("FIREBASE_ID"), sprite)
                val key = evaluateString(brick.field("FIREBASE_KEY"), sprite)
                val varName = brick.variableRefs["userVariable"]
                if (url.isNotEmpty() && key.isNotEmpty() && varName != null) {
                    val wait = brick.value("waitForResponseSelection") != "0"
                    if (wait) {
                        startAsyncWait(state) { done ->
                            DesktopFirebaseManager.readFromDatabase(url, key) { value ->
                                post { setVariable(sprite, varName, value ?: "No data"); done() }
                            }
                        }
                    } else {
                        DesktopFirebaseManager.readFromDatabase(url, key) { value ->
                            post { setVariable(sprite, varName, value ?: "No data") }
                        }
                    }
                }
                BrickFlow.ADVANCE
            }
            "DeleteBaseBrick" -> {
                val url = evaluateString(brick.field("FIREBASE_ID"), sprite)
                val key = evaluateString(brick.field("FIREBASE_KEY"), sprite)
                if (url.isNotEmpty() && key.isNotEmpty()) {
                    val wait = brick.value("waitForResponseSelection") != "0"
                    if (wait) {
                        startAsyncWait(state) { done ->
                            DesktopFirebaseManager.deleteFromDatabase(url, key) { post { done() } }
                        }
                    } else {
                        DesktopFirebaseManager.deleteFromDatabase(url, key)
                    }
                }
                BrickFlow.ADVANCE
            }

            // ---------- Firebase Storage ----------
            "UploadFileToFirebaseBrick" -> {
                val bucket = evaluateString(brick.field("FIREBASE_BUCKET"), sprite)
                val path = evaluateString(brick.field("FIREBASE_STORAGE_PATH"), sprite)
                val file = resolveProjectFile(evaluateString(brick.field("FILE"), sprite))
                if (bucket.isNotEmpty() && path.isNotEmpty() && file != null && file.exists()) {
                    startAsyncWait(state) { done ->
                        DesktopFirebaseManager.uploadFile(bucket, path, file) { post { done() } }
                    }
                }
                BrickFlow.ADVANCE
            }
            "DownloadFileFromFirebaseBrick" -> {
                val bucket = evaluateString(brick.field("FIREBASE_BUCKET"), sprite)
                val path = evaluateString(brick.field("FIREBASE_STORAGE_PATH"), sprite)
                val dest = evaluateString(brick.field("DOWNLOAD_PATH"), sprite)
                val varName = brick.variableRefs["userVariable"]
                if (bucket.isNotEmpty() && path.isNotEmpty()) {
                    val destFile = resolveProjectFile(dest) ?: File(externalDir(), dest.ifEmpty { "downloaded" })
                    startAsyncWait(state) { done ->
                        DesktopFirebaseManager.downloadFile(bucket, path, destFile) { ok ->
                            post {
                                if (varName != null) setVariable(sprite, varName, if (ok) destFile.absolutePath else "ERROR")
                                done()
                            }
                        }
                    }
                }
                BrickFlow.ADVANCE
            }
            "ListFirebaseFilesBrick" -> {
                val bucket = evaluateString(brick.field("FIREBASE_BUCKET"), sprite)
                val prefix = evaluateString(brick.field("FIREBASE_STORAGE_PATH"), sprite)
                val varName = brick.variableRefs["userVariable"]
                if (bucket.isNotEmpty() && varName != null) {
                    val names = listFirebaseFiles(bucket, prefix)
                    setVariable(sprite, varName, names.joinToString(", "))
                }
                BrickFlow.ADVANCE
            }
            "DeleteFirebaseFileBrick" -> {
                val bucket = evaluateString(brick.field("FIREBASE_BUCKET"), sprite)
                val path = evaluateString(brick.field("FIREBASE_STORAGE_PATH"), sprite)
                if (bucket.isNotEmpty() && path.isNotEmpty()) {
                    startAsyncWait(state) { done ->
                        DesktopFirebaseManager.deleteFile(bucket, path) { post { done() } }
                    }
                }
                BrickFlow.ADVANCE
            }

            // ---------- Hardware / Device ----------
            "CopyTextBrick" -> {
                val text = evaluateString(brick.field("TEXT"), sprite)
                DesktopHardwareBridge.copyToClipboard(text)
                BrickFlow.ADVANCE
            }
            "ShareBrick" -> {
                val text = evaluateString(brick.field("VALUE_1"), sprite)
                DesktopHardwareBridge.shareText(text)
                BrickFlow.ADVANCE
            }
            "VibrationBrick" -> {
                val seconds = evaluateNumber(brick.field("VIBRATE_DURATION_IN_SECONDS"), sprite)
                DesktopHardwareBridge.vibrate((seconds * 1000).toLong())
                BrickFlow.ADVANCE
            }
            "VibratePatternBrick" -> BrickFlow.ADVANCE
            "KeepScreenOnBrick", "KeepScreenOffBrick" -> BrickFlow.ADVANCE
            "SetFlashlightBrick", "FlashBrick" -> {
                DesktopHardwareBridge.setFlashlight(true)
                BrickFlow.ADVANCE
            }

            // ---------- Data on device ----------
            "WriteVariableOnDeviceBrick" -> {
                val name = brick.variableRefs["userVariable"]
                if (name != null) {
                    val value = getVariable(sprite, name)
                    writeDeviceData(name, str(value))
                }
                BrickFlow.ADVANCE
            }
            "ReadVariableFromDeviceBrick" -> {
                val name = brick.variableRefs["userVariable"]
                if (name != null) {
                    val value = readDeviceData(name)
                    setVariable(sprite, name, value ?: "")
                }
                BrickFlow.ADVANCE
            }
            "WriteListOnDeviceBrick", "ReadListFromDeviceBrick" -> BrickFlow.ADVANCE

            // ---------- ShowText / HideText ----------
            "ShowTextColorSizeAlignmentBrick" -> {
                val name = brick.variableRefs["userVariable"]
                if (name != null) {
                    textWidgets.removeAll { it.variableName == name && it.spriteName == sprite.name }
                    textWidgets += DesktopTextWidget(
                        variableName = name,
                        spriteName = sprite.name,
                        x = evaluateNumber(brick.field("X_POSITION"), sprite),
                        y = evaluateNumber(brick.field("Y_POSITION"), sprite),
                        size = evaluateNumber(brick.field("SIZE"), sprite),
                        colorHex = evaluateString(brick.field("COLOR"), sprite),
                        alignment = brick.value("alignmentSelection")?.toIntOrNull() ?: 1
                    )
                }
                BrickFlow.ADVANCE
            }
            "HideTextBrick" -> {
                val name = brick.variableRefs["userVariable"]
                if (name != null) {
                    textWidgets.removeAll { it.variableName == name && it.spriteName == sprite.name }
                }
                BrickFlow.ADVANCE
            }

            // ---------- AdMob ----------
            "AdmobInitializeBrick" -> BrickFlow.ADVANCE
            "AdmobSetAppIdBrick" -> BrickFlow.ADVANCE
            "AdmobEnableTestModeBrick" -> BrickFlow.ADVANCE
            "AdmobSetBannerUnitIdBrick" -> { admobUnitIds["banner"] = evaluateString(brick.field("AD_UNIT_ID"), sprite); BrickFlow.ADVANCE }
            "AdmobSetInterstitialUnitIdBrick" -> { admobUnitIds["interstitial"] = evaluateString(brick.field("AD_UNIT_ID"), sprite); BrickFlow.ADVANCE }
            "AdmobSetRewardedUnitIdBrick" -> { admobUnitIds["rewarded"] = evaluateString(brick.field("AD_UNIT_ID"), sprite); BrickFlow.ADVANCE }
            "AdmobSetAppOpenUnitIdBrick" -> { admobUnitIds["app_open"] = evaluateString(brick.field("AD_UNIT_ID"), sprite); BrickFlow.ADVANCE }
            "AdmobLoadBannerBrick" -> { DesktopAdMobManager.loadBanner(admobUnitIds["banner"] ?: ""); BrickFlow.ADVANCE }
            "AdmobShowBannerBrick" -> { DesktopAdMobManager.showBanner(); BrickFlow.ADVANCE }
            "AdmobHideBannerBrick" -> { DesktopAdMobManager.hideBanner(); BrickFlow.ADVANCE }
            "AdmobDestroyBannerBrick" -> { DesktopAdMobManager.destroyBanner(); BrickFlow.ADVANCE }
            "AdmobLoadInterstitialBrick" -> { DesktopAdMobManager.loadInterstitial(admobUnitIds["interstitial"] ?: ""); BrickFlow.ADVANCE }
            "AdmobShowInterstitialBrick" -> { DesktopAdMobManager.showInterstitial(); BrickFlow.ADVANCE }
            "AdmobLoadRewardedBrick" -> { DesktopAdMobManager.loadRewarded(admobUnitIds["rewarded"] ?: ""); BrickFlow.ADVANCE }
            "AdmobShowRewardedBrick" -> {
                DesktopAdMobManager.showRewarded { GdxLog("AdMob: награда выдана (test mode)") }
                BrickFlow.ADVANCE
            }
            "AdmobLoadAppOpenBrick" -> BrickFlow.ADVANCE
            "AdmobShowAppOpenBrick" -> BrickFlow.ADVANCE

            // ---------- Physics ----------
            "SetPhysicsObjectTypeBrick" -> {
                val typeName = brick.value("type") ?: "NONE"
                val type = when (typeName.uppercase()) {
                    "DYNAMIC" -> DesktopPhysicsWorld.BodyType.DYNAMIC
                    "FIXED" -> DesktopPhysicsWorld.BodyType.FIXED
                    else -> DesktopPhysicsWorld.BodyType.NONE
                }
                sprite.physicsEnabled = type != DesktopPhysicsWorld.BodyType.NONE
                physics.setType(sprite, type, widthOf(sprite), heightOf(sprite))
                BrickFlow.ADVANCE
            }
            "SetGravityBrick" -> {
                physics.setGravity(
                    evaluateNumber(brick.field("GRAVITY_X"), sprite),
                    evaluateNumber(brick.field("GRAVITY_Y"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "SetVelocityBrick" -> {
                val vx = evaluateNumber(brick.field("VELOCITY_X"), sprite)
                val vy = evaluateNumber(brick.field("VELOCITY_Y"), sprite)
                sprite.velocityX = vx
                sprite.velocityY = vy
                physics.setVelocity(sprite, vx, vy)
                BrickFlow.ADVANCE
            }
            "ApplyForceBrick" -> {
                physics.applyForce(
                    sprite,
                    evaluateNumber(brick.field("X_FORCE"), sprite),
                    evaluateNumber(brick.field("Y_FORCE"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "ApplyImpulseBrick" -> {
                physics.applyImpulse(
                    sprite,
                    evaluateNumber(brick.field("X_IMPULSE"), sprite),
                    evaluateNumber(brick.field("Y_IMPULSE"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "ApplyTorqueBrick" -> {
                physics.applyTorque(sprite, evaluateNumber(brick.field("TORQUE"), sprite))
                BrickFlow.ADVANCE
            }
            "ApplyAngularImpulseBrick" -> {
                physics.applyAngularImpulse(sprite, evaluateNumber(brick.field("ANGULAR_IMPULSE"), sprite))
                BrickFlow.ADVANCE
            }
            "SetAngularVelocityBrick" -> {
                physics.setAngularVelocity(sprite, evaluateNumber(brick.field("ANGULAR_VELOCITY"), sprite))
                BrickFlow.ADVANCE
            }
            "SetMassBrick" -> {
                physics.setMass(sprite, evaluateNumber(brick.field("MASS"), sprite))
                BrickFlow.ADVANCE
            }
            "SetDampingBrick", "SetLinearDampingBrick" -> {
                physics.setDamping(sprite, evaluateNumber(brick.field("DAMPING"), sprite))
                BrickFlow.ADVANCE
            }
            "SetAngularDampingBrick" -> {
                physics.setAngularDamping(sprite, evaluateNumber(brick.field("ANGULAR_DAMPING"), sprite))
                BrickFlow.ADVANCE
            }
            "SetBounceBrick" -> {
                val bounce = evaluateNumber(brick.field("BOUNCE"), sprite)
                physics.setBounce(sprite, (bounce / 100f).coerceIn(0f, 1f))
                BrickFlow.ADVANCE
            }
            "SetFrictionBrick" -> {
                physics.setFriction(sprite, evaluateNumber(brick.field("FRICTION"), sprite))
                BrickFlow.ADVANCE
            }
            "SetRestitutionBrick" -> {
                physics.setBounce(sprite, evaluateNumber(brick.field("RESTITUTION"), sprite))
                BrickFlow.ADVANCE
            }
            "SetHitboxBrick" -> {
                val w = evaluateNumber(brick.field("WIDTH"), sprite)
                val h = evaluateNumber(brick.field("HEIGHT"), sprite)
                physics.setHitbox(sprite, w, h)
                BrickFlow.ADVANCE
            }
            "SetPhysicsBulletBrick" -> {
                physics.setBullet(sprite, brick.value("isBullet") == "true")
                BrickFlow.ADVANCE
            }
            "SetPhysicsFixedRotationBrick" -> {
                physics.setFixedRotation(sprite, brick.value("isFixed") == "true")
                BrickFlow.ADVANCE
            }
            "SetPhysicsSensorBrick" -> {
                physics.setSensor(sprite, brick.value("isSensor") == "true")
                BrickFlow.ADVANCE
            }
            "SetGravityScaleBrick" -> {
                physics.setGravityScale(sprite, evaluateNumber(brick.field("GRAVITY_SCALE"), sprite))
                BrickFlow.ADVANCE
            }
            "SetRagdollBrick" -> {
                val value = evaluateNumber(brick.field("PHYSICS_TOGGLE"), sprite)
                physics.setRagdoll(sprite, value != 0f)
                BrickFlow.ADVANCE
            }
            "SetPhysicsStateBrick" -> BrickFlow.ADVANCE
            "ApplyForceAtPointBrick" -> {
                physics.applyForce(
                    sprite,
                    evaluateNumber(brick.field("X_FORCE"), sprite),
                    evaluateNumber(brick.field("Y_FORCE"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "CreateRevoluteJointBrick" -> {
                val id = evaluateString(brick.field("JOINT_ID"), sprite)
                val other = findSprite(evaluateString(brick.field("SPRITE"), sprite))
                if (id.isNotEmpty() && other != null) {
                    physics.createRevoluteJoint(
                        id, sprite, other,
                        evaluateNumber(brick.field("X_POSITION"), sprite),
                        evaluateNumber(brick.field("Y_POSITION"), sprite)
                    )
                }
                BrickFlow.ADVANCE
            }
            "CreateDistanceJointBrick" -> {
                val id = evaluateString(brick.field("JOINT_ID"), sprite)
                val other = findSprite(evaluateString(brick.field("SPRITE"), sprite))
                if (id.isNotEmpty() && other != null) {
                    physics.createDistanceJoint(
                        id, sprite, other,
                        evaluateNumber(brick.field("JOINT_LENGTH"), sprite),
                        evaluateNumber(brick.field("JOINT_FREQUENCY"), sprite),
                        evaluateNumber(brick.field("JOINT_DAMPING"), sprite)
                    )
                }
                BrickFlow.ADVANCE
            }
            "CreateWeldJointBrick" -> {
                val id = evaluateString(brick.field("JOINT_ID"), sprite)
                val other = findSprite(evaluateString(brick.field("SPRITE"), sprite))
                if (id.isNotEmpty() && other != null) {
                    physics.createWeldJoint(
                        id, sprite, other,
                        evaluateNumber(brick.field("X_POSITION"), sprite),
                        evaluateNumber(brick.field("Y_POSITION"), sprite)
                    )
                }
                BrickFlow.ADVANCE
            }
            "CreatePrismaticJointBrick" -> {
                val id = evaluateString(brick.field("JOINT_ID"), sprite)
                val other = findSprite(evaluateString(brick.field("SPRITE"), sprite))
                if (id.isNotEmpty() && other != null) {
                    physics.createPrismaticJoint(
                        id, sprite, other,
                        evaluateNumber(brick.field("X_POSITION"), sprite),
                        evaluateNumber(brick.field("Y_POSITION"), sprite),
                        evaluateNumber(brick.field("AXIS_X"), sprite),
                        evaluateNumber(brick.field("AXIS_Y"), sprite)
                    )
                }
                BrickFlow.ADVANCE
            }
            "CreatePulleyJointBrick" -> {
                val id = evaluateString(brick.field("JOINT_ID"), sprite)
                val a = findSprite(evaluateString(brick.field("SPRITE_A"), sprite))
                val b = findSprite(evaluateString(brick.field("SPRITE_B"), sprite))
                if (id.isNotEmpty() && a != null && b != null) {
                    physics.createPulleyJoint(
                        id, a, b,
                        evaluateNumber(brick.field("GROUND_ANCHOR_A_X"), sprite),
                        evaluateNumber(brick.field("GROUND_ANCHOR_A_Y"), sprite),
                        evaluateNumber(brick.field("GROUND_ANCHOR_B_X"), sprite),
                        evaluateNumber(brick.field("GROUND_ANCHOR_B_Y"), sprite),
                        evaluateNumber(brick.field("RATIO"), sprite)
                    )
                }
                BrickFlow.ADVANCE
            }
            "CreateGearJointBrick" -> {
                val id = evaluateString(brick.field("JOINT_ID"), sprite)
                physics.createGearJoint(
                    id,
                    evaluateString(brick.field("JOINT_A_ID"), sprite),
                    evaluateString(brick.field("JOINT_B_ID"), sprite),
                    evaluateNumber(brick.field("RATIO"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "DestroyJointBrick" -> {
                physics.destroyJoint(evaluateString(brick.field("JOINT_ID"), sprite))
                BrickFlow.ADVANCE
            }
            "PerformRayCastBrick" -> {
                physics.performRayCast(
                    evaluateString(brick.field("RAY_ID"), sprite),
                    evaluateNumber(brick.field("X_START"), sprite),
                    evaluateNumber(brick.field("Y_START"), sprite),
                    evaluateNumber(brick.field("X_END"), sprite),
                    evaluateNumber(brick.field("Y_END"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "CreatePointJointBrick" -> BrickFlow.ADVANCE

            // ---------- NeoScript (.neoscript модули) ----------
            "CreateObjectBrick" -> {
                val objName = evaluateString(brick.field("CREATE_OBJECT_NAME"), sprite)
                if (objName.isNotEmpty() && sprites.none { it.name == objName }) {
                    val rt = DesktopSpriteRuntime(
                        DesktopSprite(objName, false, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
                    )
                    sprites.add(rt)
                    spriteVariables[objName] = mutableMapOf()
                    startInitialScripts(rt)
                }
                BrickFlow.ADVANCE
            }
            "AssignScriptsBrick" -> {
                val path = evaluateString(brick.field("ASSIGN_SCRIPTS_FILE"), sprite)
                val objName = evaluateString(brick.field("ASSIGN_SCRIPTS_OBJECT"), sprite)
                val replace = brick.value("replaceExistingSelection") == "1"
                val target = findSprite(objName)
                if (target != null && path.isNotEmpty()) {
                    val loaded = loadNeoScripts(path)
                    if (loaded != null) {
                        val toStart = if (replace) {
                            target.model = target.model.copy(scripts = loaded)
                            loaded
                        } else {
                            applyNeoScripts(target, loaded, overwrite = false, dedup = false)
                        }
                        for (ns in toStart) startInitialScript(target, ns)
                    }
                }
                BrickFlow.ADVANCE
            }
            "ImportScriptBrick" -> {
                val path = evaluateString(brick.field("IMPORT_SCRIPT_FILE"), sprite)
                val objName = evaluateString(brick.field("IMPORT_SCRIPT_OBJECT"), sprite)
                val overwrite = brick.value("overwriteSelection") == "1"
                val target = findSprite(objName)
                if (target != null && path.isNotEmpty()) {
                    val loaded = loadNeoScripts(path)
                    if (loaded != null) {
                        val toStart = applyNeoScripts(target, loaded, overwrite = overwrite, dedup = true)
                        for (ns in toStart) startInitialScript(target, ns)
                    }
                }
                BrickFlow.ADVANCE
            }

            // ---------- Fast2D (сущности по ID, зеркало FastTwoDManager) ----------
            "Fast2DCreateBrick" -> {
                fast2d.createEntity(evaluateString(brick.field("NAME"), sprite))
                BrickFlow.ADVANCE
            }
            "Fast2DDeleteBrick" -> {
                fast2d.destroyEntity(evaluateString(brick.field("NAME"), sprite))
                BrickFlow.ADVANCE
            }
            "Fast2DMakePhysicsBrick" -> {
                val id = evaluateString(brick.field("NAME"), sprite)
                fast2d.makePhysicsBody(
                    id,
                    evaluateNumber(brick.field("X_POSITION"), sprite) >= 0.5f,
                    evaluateString(brick.field("STRING"), sprite).ifEmpty { "BOX" },
                    evaluateNumber(brick.field("Y_POSITION"), sprite),
                    evaluateNumber(brick.field("ROTATION"), sprite),
                    evaluateNumber(brick.field("SIZE"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DApplyForceBrick" -> {
                fast2d.applyForce(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("X_POSITION"), sprite),
                    evaluateNumber(brick.field("Y_POSITION"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DApplyImpulseBrick" -> {
                fast2d.applyImpulse(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("X_POSITION"), sprite),
                    evaluateNumber(brick.field("Y_POSITION"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetVelocityBrick" -> {
                fast2d.setVelocity(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("X_POSITION"), sprite),
                    evaluateNumber(brick.field("Y_POSITION"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetPhysicsVelocityBrick" -> {
                fast2d.setPhysicsVelocity(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("X_POSITION"), sprite),
                    evaluateNumber(brick.field("Y_POSITION"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetAngularVelocityBrick" -> {
                fast2d.setAngularVelocity(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("ROTATION"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetGravityBrick" -> {
                fast2d.setGravity(
                    evaluateNumber(brick.field("VALUE_1"), sprite),
                    evaluateNumber(brick.field("VALUE_2"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetCameraBrick" -> {
                fast2d.setCamera(
                    evaluateNumber(brick.field("X_POSITION"), sprite),
                    evaluateNumber(brick.field("Y_POSITION"), sprite),
                    evaluateNumber(brick.field("SIZE"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetCollisionFilterBrick" -> {
                fast2d.setCollisionFilter(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("X_POSITION"), sprite) >= 0.5f,
                    evaluateNumber(brick.field("Y_POSITION"), sprite).toInt()
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetColorBrick" -> {
                fast2d.setColor(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("X_POSITION"), sprite),
                    evaluateNumber(brick.field("Y_POSITION"), sprite),
                    evaluateNumber(brick.field("ROTATION"), sprite),
                    evaluateNumber(brick.field("SIZE"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetPositionBrick" -> {
                fast2d.setPosition(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("X_POSITION"), sprite),
                    evaluateNumber(brick.field("Y_POSITION"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetRotationBrick" -> {
                fast2d.setRotation(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("ROTATION"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetScaleBrick" -> {
                fast2d.setScale(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("X_SCALE"), sprite),
                    evaluateNumber(brick.field("Y_SCALE"), sprite)
                )
                BrickFlow.ADVANCE
            }
            "Fast2DSetTextureBrick" -> {
                val name = evaluateString(brick.field("STRING"), sprite)
                if (name.isNotEmpty()) fast2d.setTexture(evaluateString(brick.field("NAME"), sprite), name)
                BrickFlow.ADVANCE
            }
            "Fast2DSetZIndexBrick" -> {
                fast2d.setZIndex(
                    evaluateString(brick.field("NAME"), sprite),
                    evaluateNumber(brick.field("VIBRATE_DURATION"), sprite)
                )
                BrickFlow.ADVANCE
            }

            // ---------- Scenes ----------
            "SceneTransitionBrick" -> {
                val sceneToStart = brick.value("sceneForTransition") ?: ""
                if (sceneToStart.isNotEmpty()) switchScene(sceneToStart)
                BrickFlow.STOP
            }
            "LoadSceneBrick" -> {
                val sceneToLoad = evaluateString(brick.field("VALUE"), sprite)
                if (sceneToLoad.isNotEmpty()) switchScene(sceneToLoad)
                BrickFlow.STOP
            }
            "LoadSceneAdditiveBrick" -> {
                val sceneToLoad = evaluateString(brick.field("VALUE"), sprite)
                if (sceneToLoad.isNotEmpty()) switchScene(sceneToLoad, additive = true)
                BrickFlow.STOP
            }
            "SceneBackBrick" -> {
                sceneBack()
                BrickFlow.STOP
            }
            "SceneIdBrick" -> {
                val varName = brick.variableRefs["userVariable"]
                if (varName != null) {
                    setVariable(sprite, varName, activeSceneIndex.toDouble())
                }
                BrickFlow.ADVANCE
            }
            "ClearSceneBrick" -> {
                for (s in sprites.toList()) {
                    if (s.isBackground) continue
                    s.visible = false
                    physics.removeBody(s)
                }
                BrickFlow.ADVANCE
            }
            "SceneStartBrick",
            "SetSaveScenesBrick", "SetStopSoundsBrick" -> BrickFlow.ADVANCE

            // ---------- Предзагрузка сцен (Preload category) ----------
            "PreloadSceneBrick" -> {
                brick.value("sceneToPreload")?.takeIf { it.isNotEmpty() }?.let { preloadedScenes.add(it) }
                BrickFlow.ADVANCE
            }
            "ScenePreloadedBrick" -> {
                val sceneName = brick.value("sceneToCheck")
                val varName = brick.variableRefs["userVariable"]
                if (sceneName != null && varName != null) {
                    setVariable(sprite, varName, if (preloadedScenes.contains(sceneName)) 1.0 else 0.0)
                }
                BrickFlow.ADVANCE
            }
            "SetPreloadingBrick" -> {
                preloadEnabled = brick.value("preloadEnabled") == "1"
                BrickFlow.ADVANCE
            }

            // ---------- Event ----------
            "WhenConditionBrick", "WhenBounceOffBrick", "WhenGamepadButtonBrick",
            "WhenNfcBrick", "WhenTouchDownBrick", "WhenTappedBrick", "WhenBackgroundChangesBrick" ->
                BrickFlow.ADVANCE

            else -> {
                GdxLog("Блок не реализован: ${brick.type}")
                BrickFlow.ADVANCE
            }
        }
    }

    private fun pushChildFrame(state: ScriptState, container: DesktopBrick) {
        val blocks = container.children["loopBricks"] ?: emptyList()
        state.frames.add(Frame(blocks))
    }

    private fun spawnParallelBody(parent: ScriptState, blocks: List<DesktopBrick>, onDeath: () -> Unit) {
        val st = ScriptState(DesktopScript("__async", blocks), parent.sprite, ArrayDeque())
        st.frames.add(Frame(blocks))
        st.onDeath = onDeath
        scriptStates.add(st)
    }

    // ---------- события от listener ----------

    fun onMouseButton(button: Int) {
        mouseButtonPending = button
    }

    fun onScrolled(amountY: Float) {
        scrollPending = if (amountY > 0f) 1 else -1
    }

    fun onSwipe(deltaX: Float, deltaY: Float) {
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            swipePending = if (deltaX > 0f) "RIGHT" else "LEFT"
        } else {
            swipePending = if (deltaY > 0f) "UP" else "DOWN"
        }
    }

    fun onSpriteReleased(sprite: DesktopSpriteRuntime?) {
        spriteReleasePending = sprite
    }

    fun onWindowResized(width: Int, height: Int) {
        windowWidth = width
        windowHeight = height
        triggerByType("WhenWindowResizedScript", null)
    }

    fun onAppMinimized() {
        if (!appMinimized) {
            appMinimized = true
            triggerByType("WhenAppMinimizedScript", null)
        }
    }

    fun onAppRestored() {
        if (appMinimized) {
            appMinimized = false
            triggerByType("WhenAppRestoredScript", null)
        }
    }

    fun onFingerMoved() {
        fingerMovedPending = true
    }

    private fun triggerByType(type: String, sprite: DesktopSpriteRuntime?) {
        val targets = if (sprite != null) listOf(sprite) else sprites
        for (s in targets) {
            for (script in s.model.scripts) {
                if (script.type == type) startScript(s, script)
            }
        }
    }

    private fun processPendingEvents() {
        mouseButtonPending?.let { button ->
            mouseButtonPending = null
            for (s in sprites) {
                for (script in s.model.scripts) {
                    if (script.type == "WhenMouseButtonClickedScript") {
                        val code = script.values["buttonCode"]?.toIntOrNull() ?: 0
                        if (code == button || (button == 0 && code == 0)) startScript(s, script)
                    }
                }
            }
        }
        if (scrollPending != 0) {
            val sc = scrollPending
            scrollPending = 0
            for (s in sprites) {
                for (script in s.model.scripts) {
                    if (script.type == "WhenMouseWheelScrolledScript") startScript(s, script)
                }
            }
        }
        swipePending?.let { dir ->
            swipePending = null
            for (s in sprites) {
                for (script in s.model.scripts) {
                    if (script.type == "WhenSwipedScript") {
                        val direction = script.values["direction"]?.toIntOrNull() ?: 0
                        val dirName = when (direction) {
                            0 -> "UP"; 1 -> "RIGHT"; 2 -> "DOWN"; else -> "LEFT"
                        }
                        if (dirName == dir) startScript(s, script)
                    }
                }
            }
        }
        spriteReleasePending?.let { sprite ->
            spriteReleasePending = null
            if (sprite != null) {
                triggerByType("WhenSpriteReleasedScript", sprite)
            }
        }
        if (fingerMovedPending) {
            fingerMovedPending = false
            for (s in sprites) {
                for (script in s.model.scripts) {
                    if (script.type == "WhenFingerMovedOnScreenScript") startScript(s, script)
                }
            }
            checkFingerOverSprites()
        }
        // shake
        val shaking = DesktopHardwareBridge.isShaking
        if (shaking && !shakeWasActive) {
            for (s in sprites) {
                for (script in s.model.scripts) {
                    if (script.type == "WhenShakeScript") startScript(s, script)
                }
            }
        }
        shakeWasActive = shaking
    }

    private fun checkFingerOverSprites() {
        for (s in sprites) {
            if (s.isBackground) continue
            for (script in s.model.scripts) {
                if (script.type == "WhenFingerMovedOverSpriteScript") {
                    val over = isPointOnSprite(mouseX, mouseY, s)
                    val key = "fingerOver|${s.name}"
                    val wasOver = fingerOverStates[key] == true
                    if (over && !wasOver) {
                        startScript(s, script)
                    }
                    fingerOverStates[key] = over
                }
            }
        }
    }

    private val fingerOverStates = mutableMapOf<String, Boolean>()

    private fun isPointOnSprite(px: Float, py: Float, sprite: DesktopSpriteRuntime): Boolean {
        val halfW = widthOf(sprite) / 2f
        val halfH = heightOf(sprite) / 2f
        return px >= sprite.x - halfW && px <= sprite.x + halfW &&
            py >= sprite.y - halfH && py <= sprite.y + halfH
    }

    private fun onPhysicsCollision(a: DesktopSpriteRuntime, b: DesktopSpriteRuntime) {
        val key = if (a.name < b.name) "${a.name}|${b.name}" else "${b.name}|${a.name}"
        val prev = bounceTriggers[key] == true
        if (!prev) {
            fireBounce(a, b)
        }
        bounceTriggers[key] = true
    }

    private fun fireBounce(a: DesktopSpriteRuntime, b: DesktopSpriteRuntime) {
        for (s in listOf(a, b)) {
            for (script in s.model.scripts) {
                if (script.type != "WhenBounceOffScript") continue
                val target = script.values["spriteToBounceOffName"] ?: ""
                val other = if (s === a) b else a
                if (target.isEmpty() || target == other.name) {
                    s.lastBounceSprite = other.name
                    startScript(s, script)
                }
            }
        }
    }

    private fun pollWhenScripts() {
        // touching sprite (AABB, без физики)
        for (s in sprites) {
            if (s.isBackground) continue
            for (script in s.model.scripts) {
                if (script.type != "WhenTouchingSpriteScript" && script.type != "WhenTouchingSpriteByNameScript") continue
                val targetName = script.values["spriteToTouchName"] ?: ""
                val key = "touch|${s.name}|${script.type}|$targetName"
                val touching = sprites.any { other ->
                    other !== s && !other.isBackground && other.visible &&
                        (targetName.isEmpty() || targetName == other.name) &&
                        aabbOverlap(s, other)
                }
                val prev = touchingTriggers[key] == true
                if (touching && !prev) startScript(s, script)
                touchingTriggers[key] = touching
            }
        }
        // bounce off: расхождение сбрасывает
        val iter = bounceTriggers.entries.iterator()
        while (iter.hasNext()) {
            val (key, value) = iter.next()
            if (value) {
                val names = key.split("|")
                val a = sprites.firstOrNull { it.name == names[0] }
                val b = sprites.firstOrNull { it.name == names[1] }
                if (a == null || b == null || !aabbOverlap(a, b)) iter.remove()
            }
        }
        // condition
        for (s in sprites) {
            for (script in s.model.scripts) {
                if (script.type != "WhenConditionScript") continue
                val condition = script.triggerFormulas["IF_CONDITION"]
                val value = evaluateNumber(condition, s) != 0f
                val key = "cond|${s.name}|${script.hashCode()}"
                val prev = conditionTriggers[key] == true
                if (value && !prev) startScript(s, script)
                conditionTriggers[key] = value
            }
        }
        // variable changed
        for (s in sprites) {
            for (script in s.model.scripts) {
                if (script.type != "WhenVariableChangedScript") continue
                val varName = script.variableName ?: continue
                val value = getVariable(s, varName)
                val key = "var|${s.name}|$varName"
                val prev = variableTriggers[key]
                if (prev != null && prev != value) startScript(s, script)
                variableTriggers[key] = value
            }
        }
        // background changed
        for (s in sprites) {
            if (!s.isBackground) continue
            for (script in s.model.scripts) {
                if (script.type != "WhenBackgroundChangesScript") continue
                val key = "bg|${s.name}"
                val current = s.currentLook()?.fileName ?: ""
                val prev = backgroundTriggerValue[key]
                if (prev != null && prev != current) startScript(s, script)
                backgroundTriggerValue[key] = current
            }
        }
        // time reached (HH:MM -> минуты от полуночи)
        val nowMinutes = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 + Calendar.getInstance().get(Calendar.MINUTE)
        for (s in sprites) {
            for (script in s.model.scripts) {
                if (script.type != "WhenTimeReachedScript") continue
                val target = evaluateNumber(script.triggerFormulas["TIME"], s).toInt()
                val key = "time|${s.name}"
                if (nowMinutes == target && timeTriggerFired[key] != true) {
                    timeTriggerFired[key] = true
                    startScript(s, script)
                } else if (nowMinutes != target) {
                    timeTriggerFired[key] = false
                }
            }
        }
    }

    private fun aabbOverlap(a: DesktopSpriteRuntime, b: DesktopSpriteRuntime): Boolean {
        val ahw = widthOf(a) / 2f
        val ahh = heightOf(a) / 2f
        val bhw = widthOf(b) / 2f
        val bhh = heightOf(b) / 2f
        return a.x - ahw < b.x + bhw && a.x + ahw > b.x - bhw &&
            a.y - ahh < b.y + bhh && a.y + ahh > b.y - bhh
    }

    private fun startAsyncWait(state: ScriptState, job: (done: () -> Unit) -> Unit) {
        val frame = state.frames.last()
        frame.asyncWait = true
        frame.asyncDone = false
        frame.asyncCallback = null
        try {
            job {
                val f = state.frames.lastOrNull()
                if (f != null) {
                    f.asyncDone = true
                } else {
                    state.dead = true
                }
            }
        } catch (e: Exception) {
            GdxLog("Async job failed: $e")
            frame.asyncDone = true
        }
    }

    private fun post(runnable: () -> Unit) {
        try {
            Gdx.app.postRunnable(runnable)
        } catch (e: Exception) {
            runnable()
        }
    }

    private fun resolveProjectFile(name: String): File? {
        if (name.isEmpty()) return null
        val direct = File(name)
        if (direct.isAbsolute && direct.exists()) return direct
        val inProject = File(projectDir, name)
        if (inProject.exists()) return inProject
        val inImages = File(File(projectDir, "images"), name)
        if (inImages.exists()) return inImages
        val inSounds = File(File(projectDir, "sounds"), name)
        if (inSounds.exists()) return inSounds
        return direct
    }

    private fun externalDir(): File {
        val base = RuntimeServicesHolder.services?.getExternalStorageDir() ?: System.getProperty("user.dir")
        return File(base)
    }

    private fun deviceDataDir(): File = File(externalDir(), "device_vars").apply { mkdirs() }

    private fun writeDeviceData(name: String, value: String) {
        try {
            val sanitized = name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            File(deviceDataDir(), "$sanitized.txt").writeText(value)
        } catch (e: Exception) {
            GdxLog("WriteVariableOnDevice failed: $e")
        }
    }

    private fun readDeviceData(name: String): String? {
        return try {
            val sanitized = name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val f = File(deviceDataDir(), "$sanitized.txt")
            if (f.exists()) f.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun listFirebaseFiles(bucket: String, prefix: String): List<String> {
        val result = mutableListOf<String>()
        DesktopFirebaseManager.listFiles(bucket, prefix) { names -> result += names }
        return result
    }

    private fun pollFirebaseTriggers() {
        for (s in sprites) {
            for (script in s.model.scripts) {
                if (script.type != "WhenFirebaseChangedScript") continue
                val url = evaluateString(script.triggerFormulas["FIREBASE_TRIGGER_BUCKET"], s)
                val path = evaluateString(script.triggerFormulas["FIREBASE_TRIGGER_PATH"], s)
                if (url.isEmpty() || path.isEmpty()) continue
                val key = "firebase|$url|$path"
                DesktopFirebaseManager.readFromDatabase(url, path) { value ->
                    val v = value ?: ""
                    post {
                        val prev = firebasePrevValues[key]
                        if (prev != null && prev != v) {
                            startScript(s, script)
                        }
                        firebasePrevValues[key] = v
                    }
                }
            }
        }
    }

    private fun resolveTargetName(raw: String?, self: DesktopSpriteRuntime): DesktopSpriteRuntime? {
        if (raw.isNullOrEmpty()) return null
        val name = raw.substringAfterLast("object[").substringBefore("]").let { idx ->
            if (idx.toIntOrNull() != null) sprites.getOrNull(idx.toInt())
            else sprites.firstOrNull { it.name == raw }
        }
        return name?.takeIf { it !== self }
    }

    private fun findSprite(name: String): DesktopSpriteRuntime? =
        if (name.isEmpty()) null else sprites.firstOrNull { it.name == name }

    // ---------- NeoScript helpers ----------

    private fun startInitialScripts(s: DesktopSpriteRuntime) {
        for (script in s.model.scripts) startInitialScript(s, script)
    }

    private fun startInitialScript(s: DesktopSpriteRuntime, script: DesktopScript) {
        if (script.type !in setOf("StartScript", "SceneStartScript", "WhenSceneLaunchedScript")) return
        if (script.type == "WhenSceneLaunchedScript" && !script.sceneName.isNullOrEmpty() && script.sceneName != sceneName) return
        startScript(s, script)
    }

    private fun loadNeoScripts(path: String): List<DesktopScript>? {
        var file = File(path)
        if (!file.exists()) {
            val name = path.substringAfterLast('/').substringAfterLast('\\')
            file = projectDir.walkTopDown().firstOrNull { it.isFile && (it.name == name || it.name.equals(path, true)) }
                ?: return null
        }
        if (!file.exists() || !file.isFile) return null
        return try {
            parseNeoScripts(file)
        } catch (e: Exception) {
            null
        }
    }

    private fun neoSig(script: DesktopScript): String {
        val trigger = script.broadcastMessage ?: script.touchedSpriteName ?: script.variableName ?: ""
        return "${script.type}#$trigger"
    }

    private fun applyNeoScripts(
        target: DesktopSpriteRuntime,
        loaded: List<DesktopScript>,
        overwrite: Boolean,
        dedup: Boolean
    ): List<DesktopScript> {
        val existing = target.model.scripts.toMutableList()
        val existingKeys = existing.map { neoSig(it) }.toMutableSet()
        val toStart = mutableListOf<DesktopScript>()
        for (ns in loaded) {
            val key = neoSig(ns)
            if (dedup && key in existingKeys) {
                if (overwrite) {
                    existing.removeAll { neoSig(it) == key }
                    existing.add(ns)
                    toStart += ns
                }
                continue
            }
            existing.add(ns)
            existingKeys.add(key)
            toStart += ns
        }
        target.model = target.model.copy(scripts = existing)
        return toStart
    }

    private fun bounceX(sprite: DesktopSpriteRuntime) {
        sprite.rotation = 180f - sprite.rotation
        sprite.velocityX = -sprite.velocityX
    }

    private fun bounceY(sprite: DesktopSpriteRuntime) {
        sprite.rotation = -sprite.rotation
        sprite.velocityY = -sprite.velocityY
    }

    private fun cloneSprite(source: DesktopSpriteRuntime) {
        val clone = DesktopSpriteRuntime(source.model)
        clone.x = source.x
        clone.y = source.y
        clone.rotation = source.rotation
        clone.size = source.size
        clone.visible = source.visible
        clone.lookIndex = source.lookIndex
        sprites.add(clone)
        spriteVariables[clone.name] = mutableMapOf()
        for (script in clone.model.scripts) {
            when (script.type) {
                "WhenClonedScript" -> startScript(clone, script)
                "WhenClonedWithNameScript" -> {
                    val name = evaluateString(script.triggerFormulas["VALUE_1"], clone)
                    if (name.isEmpty() || name == "clone_name") startScript(clone, script)
                }
            }
        }
    }

    private fun speak(text: String) {
        speech.speak(text)
    }

    private fun GdxExit() {
        com.badlogic.gdx.Gdx.app.exit()
    }

    private fun GdxLog(msg: String) {
        com.badlogic.gdx.Gdx.app.log("DesktopEngine", msg)
    }

    interface DesktopSoundManagerBridge {
        fun playSound(sound: DesktopSound?)
        fun stopAll()
        fun setVolume(v: Float)
        fun changeVolume(delta: Float)
        fun durationOf(sound: DesktopSound): Float
    }

    fun DesktopFormula.toNode(): DesktopFormulaNode {
        return when (type) {
            "NUMBER" -> DesktopFormulaNode.Num(value.toDoubleOrNull() ?: 0.0)
            "STRING" -> DesktopFormulaNode.Str(value)
            "OPERATOR" -> DesktopFormulaNode.Op(value, leftChild, rightChild)
            "FUNCTION" -> DesktopFormulaNode.Func(
                value,
                listOfNotNull(leftChild, rightChild) + additionalChildren
            )
            "SENSOR" -> DesktopFormulaNode.Sensor(value)
            "USER_VARIABLE" -> DesktopFormulaNode.Var(value)
            "USER_LIST" -> DesktopFormulaNode.ListRef(value)
            "BRACKET" -> rightChild ?: leftChild ?: DesktopFormulaNode.Null
            "COLLISION_FORMULA" -> DesktopFormulaNode.Num(value.toDoubleOrNull() ?: 0.0)
            "USER_DEFINED_BRICK_INPUT", "USER_DEFINED_BRICK_ARGUMENT" -> DesktopFormulaNode.Var(value)
            "STRING_NUMBER_CONVERSION" -> leftChild ?: DesktopFormulaNode.Null
            else -> DesktopFormulaNode.Null
        }
    }
}