package org.catrobat.catroid.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import org.catrobat.catroid.audio.AudioServiceHolder
import org.catrobat.catroid.audio.MidiServiceHolder
import org.catrobat.catroid.network.NetworkServiceHolder
import org.catrobat.catroid.pocketmusic.note.Drum
import org.catrobat.catroid.pocketmusic.note.MusicalInstrument
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.*

/**
 * Полноценный движок скриптов для десктопного раннера NeoCatroid.
 *
 * Исполняет Catrobat-скрипты с поддержкой:
 *  - **Control**: Forever, Repeat N, RepeatUntil, If/Then/Else, ForVariableFromTo,
 *    ForItemInUserList, Wait, WaitUntil, Schedule, Broadcast, Clone, StopScript
 *  - **Motion**: move N steps, turn, goto, glide, set direction, set x/y, change x/y,
 *    go N steps back, come to front, point to, set rotation style, bounce
 *  - **Looks**: show, hide, switch look, set size, change size, set transparency,
 *    change transparency, set brightness, change brightness, set color, change color,
 *    clear graphic effects, set width/height, change width/height, ask, speak
 *  - **Sound**: play sound, play note, set volume, change volume, stop all sounds
 *  - **Music**: set instrument, set tempo, change tempo, pause for beats
 *  - **Variables**: set variable, change variable, show/hide text
 *  - **Pen**: pen down, pen up, set pen size, set pen color, stamp, clear
 *  - **Web**: HTTP GET/POST (через NetworkService)
 *  - **Events**: green flag, when clicked, broadcast, clone
 *  - **Sensing**: mouse position, touch, object position/size/direction,
 *    stage dimensions, timer, keyboard
 *  - **Data**: write/read variable to/from device
 *
 * Движок — стековая машина: каждый скрипт имеет стек фреймов.
 * Фрейм = {блоки, instruction pointer, счётчик повторений, таймер ожидания}.
 * Формулы вычисляются рекурсивно с поддержкой операторов, функций и сенсоров.
 */
class DesktopScriptEngine(
    private val project: DesktopProject,
    private val physicsWorld: DesktopPhysicsWorld?,
    private val input: DesktopInput,
    val cameraState: DesktopCameraState = DesktopCameraState()
) {
    // ──────────────────────────── константы ────────────────────────────

    companion object {
        private val DRUM_PROGRAM_MAP = mapOf(
            "SNARE_DRUM" to 38, "BASS_DRUM" to 35, "SIDE_STICK" to 37,
            "CRASH_CYMBAL" to 49, "OPEN_HI_HAT" to 46, "CLOSED_HI_HAT" to 42,
            "TAMBOURINE" to 54, "HAND_CLAP" to 39, "CLAVES" to 75,
            "WOOD_BLOCK" to 76, "COWBELL" to 56, "TRIANGLE" to 81,
            "BONGO" to 60, "CONGA" to 63, "CABASA" to 69, "GUIRO" to 73,
            "VIBRASLAP" to 58, "OPEN_CUICA" to 79
        )

        private val INSTRUMENT_PROGRAM_MAP = mapOf(
            "PIANO" to 1, "ELECTRONIC_PIANO" to 5, "ORGAN" to 17,
            "GUITAR" to 26, "ELECTRIC_GUITAR" to 28, "BASS" to 34,
            "PIZZICATO" to 46, "CELLO" to 43, "TROMBONE" to 58,
            "FLUTE" to 74, "SAXOPHONE" to 66, "OBOE" to 69,
            "TRUMPET" to 57, "HARP" to 47, "XYLOPHONE" to 14,
            "SYNTH_LEAD" to 81, "SYNTH_PAD" to 89
        )

        private val CONTAINER_BRICK_TYPES = setOf(
            "ForeverBrick",
            "RepeatBrick",
            "CountLoopBrick",
            "RepeatUntilBrick",
            "ForVariableFromToBrick",
            "ScheduleBrick",
            "ExecuteForCloneNumberBrick",
            "RunAsSpriteBrick",
            "RunOnUiThreadBrick",
            "ForItemInUserListBrick",
            "IntervalRepeatBrick",
            "TryCatchFinallyBrick",
            "SwitchBeginBrick",
            "IfLogicBeginBrick",
            "IfThenLogicBeginBrick"
        )

        private val CONTAINER_BOUNDARY_TYPES = setOf(
            "LoopEndBrick",
            "IfLogicElseBrick",
            "IfLogicEndBrick",
            "IfThenLogicEndBrick",
            "EndBrick",
            "SwitchEndBrick",
            "CatchBrick",
            "FinallyBrick"
        )
    }

    // ──────────────────────────── data classes ────────────────────────────

    data class Block(
        val type: Type,
        val args: List<Any> = emptyList(),
        val children: List<Block> = emptyList()
    ) {
        enum class Type { MOTION, LOOKS, SOUND, MUSIC, PEN, CONTROL, EVENT, SENSING, VARIABLE, WEB, DATA, FILE, PHYSICS, CAMERA, VIDEO }
    }

    /** Невычисленная формула — вычисляется в рантайме. */
    private data class RuntimeFormula(
        val brickFieldName: String,
        val formulaElement: Element
    )

    /** Состояние анимации GlideTo. */
    private data class GlideState(
        var startX: Float,
        var startY: Float,
        var targetX: Float,
        var targetY: Float,
        var duration: Float,
        var elapsed: Float = 0f,
        /** Позиция спрайта на предыдущем кадре — для детекции внешнего перемещения. */
        var previousX: Float = startX,
        var previousY: Float = startY
    )

    /** Один фрейм исполнения — стек таких фреймов на скрипт. */
    private data class Frame(
        val blocks: List<Block>,
        var ip: Int = 0,
        /** -1 = forever, 0 = run once (normal/if), >0 = repeat remaining */
        var repeatRemaining: Int = 0
    ) {
        /** Таймер ожидания wait-блока. Пока > 0 — фрейм заблокирован. */
        var waitTimer: Float = 0f
        /** Активная анимация GlideTo. Пока не null — фрейм занят анимацией. */
        var glideState: GlideState? = null
        /** Локальные параметры процедуры (user-defined brick). Доступны только в этом фрейме. */
        var procVars: MutableMap<String, Any>? = null
        /** for_item_list: имя переменной и списка для привязки текущего элемента. */
        var loopVarName: String? = null
        var loopListName: String? = null
        /** for_item_list: номер текущей итерации (0-based) для привязки элемента. */
        var loopCounter: Int = 0
        /** try_catch: помечает фрейм как try-контекст и хранит ветки catch/finally. */
        var isTryFrame: Boolean = false
        var catchVar: String? = null
        var catchBlocks: List<Block>? = null
        var finallyBlocks: List<Block>? = null
    }

    /** Определение пользовательской процедуры (UserDefinedBrick). */
    private data class ProcedureDef(val paramNames: List<String>, val body: List<Block>)

    /** Одна ветка switch/case: значение для сравнения + тело блоков. */
    private data class SwitchCase(val value: String, val body: List<Block>)

    /** Состояние видеопроигрывателя (видео-блоки). Воспроизведение best-effort. */
    private data class VideoState(
        var fileName: String = "",
        var x: Float = 0f, var y: Float = 0f,
        var width: Float = 0f, var height: Float = 0f,
        var looped: Boolean = false,
        var playing: Boolean = false,
        var position: Float = 0f   // текущая позиция (сек)
    )

    /** Best-effort 2D buffer state. Desktop does not render off-screen buffers yet, but we keep them alive. */
    private data class BufferState(
        var width: Int = 0,
        var height: Int = 0,
        var autoUpdate: Boolean = true,
        var bufferOnly: Boolean = false,
        var mode2d: Boolean = true,
        val entries: MutableList<String> = mutableListOf()
    )

    /** Состояние исполнения одного скрипта. */
    private inner class ScriptState(
        var spriteIndex: Int,
        /** Сохраняем оригинальные блоки для reset(). */
        private val originalBlocks: List<Block>,
        /** Runtime-формулы, вычисляемые при каждом проходе блока. */
        val runtimeFormulas: List<RuntimeFormula> = emptyList(),
        /** Тип события, запускающего этот скрипт (null = обычный скрипт). */
        var eventType: String? = null,
        /** Имя спрайта, по которому фильтруется событие. */
        var spriteName: String? = null,
        /** Дополнительный параметр события (имя спрайта для BounceOff, сообщение для BroadcastReceiver). */
        var eventParam: String? = null,
        /** Формула условия для WhenConditionBrick. */
        var conditionFormula: Element? = null
    ) {
        /** Стек фреймов. Всегда как минимум один (корневой). */
        val frames = mutableListOf(Frame(originalBlocks, repeatRemaining = 0))

        val isDone: Boolean get() = frames.isEmpty()

        /** Флаг — событие произошло, скрипт готов к выполнению. */
        var eventFired: Boolean = false

        /** Фрейм, в котором сейчас выполняется код (верхушка стека). */
        val currentFrame: Frame? get() = frames.lastOrNull()

        /** Пока фрейм ждёт (waitTimer) — не запускать executeState. */
        val isWaiting: Boolean get() = frames.any { it.waitTimer > 0f }

        /** Только glide-анимация активна (без waitTimer). */
        val hasGlide: Boolean get() = frames.any { it.glideState != null && it.waitTimer <= 0f }

        /** Обновить таймеры ожидания. */
        fun tick(delta: Float) {
            for (f in frames) {
                if (f.waitTimer > 0f) f.waitTimer -= delta
            }
        }

        /** Удалить фреймы, в которых ip вышел за границы и не стоит на loop/repeat. */
        fun cleanFinishedFrames(): Boolean {
            while (frames.size > 1) {
                val top = frames.last()
                if (top.ip < top.blocks.size) return true // ещё есть работа
                // Фрейм завершён — обработать loop/repeat или вытолкнуть
                if (top.repeatRemaining == -1) {
                    // forever: сброс IP на начало
                    top.ip = 0
                    return true
                }
                if (top.repeatRemaining > 1) {
                    // repeat: декремент, сброс IP
                    top.repeatRemaining--
                    top.ip = 0
                    // for_item_list: привязать переменную к следующему элементу списка
                    top.loopCounter++
                    bindLoopVar(top)
                    return true
                }
                if (top.repeatRemaining == -2) {
                    // repeat_until: вытолкнуть без продвижения IP родителя
                    // Родитель перезапустит repeat_until блок заново
                    frames.removeAt(frames.lastIndex)
                    return true
                }
                // Вытолкнуть фрейм:
                // repeatRemaining == 1 → декремент до 0 и вытолкнуть
                // repeatRemaining == 0 (обычный / if-then) → вытолкнуть
                val removed = frames.removeAt(frames.lastIndex)
                if (removed.isTryFrame) {
                    // try/catch occupies ONE slot in its parent: the parent must
                    // advance exactly once when the whole construct is done. If a
                    // finally exists, queue it now — its own completion advances the
                    // parent. If there is no finally, advance the parent immediately.
                    // (Do NOT advance the freshly pushed finally frame, or its first
                    // block would be skipped and the parent never advanced.)
                    if (!removed.finallyBlocks.isNullOrEmpty()) {
                        frames.add(Frame(removed.finallyBlocks!!, repeatRemaining = 0))
                    } else {
                        frames.lastOrNull()?.let { it.ip++ }
                    }
                } else {
                    // Обычный фрейм: продвинуть IP родителя на один блок
                    // (минуя блок-родитель), чтобы исполнение продолжилось дальше.
                    frames.lastOrNull()?.let { it.ip++ }
                }
            }
            // Root frame: если blocks закончились — скрипт завершён
            val root = frames.firstOrNull() ?: return false
            if (root.ip >= root.blocks.size) {
                frames.clear()
                return false
            }
            return true
        }

        fun reset() {
            frames.clear()
            frames.add(Frame(originalBlocks, repeatRemaining = 0))
            eventFired = false
        }
    }

    // ──────────────────────────── поля ────────────────────────────

    private val scriptStates = mutableListOf<ScriptState>()
    private val variables = mutableMapOf<String, Any>()   // глобальные переменные (Float или String)
    private val userLists = mutableMapOf<String, MutableList<Any>>() // пользовательские списки
    /** Определения пользовательских процедур, ключ — userDefinedBrickID (строка). */
    private val procedures = mutableMapOf<String, ProcedureDef>()
    /** Текущий исполняемый скрипт (для доступа к procVars из вычислителя формул). */
    private var activeState: ScriptState? = null
    /** Text overlays rendered each frame (think/say bubbles, show text, etc.) */
    val textOverlays = mutableMapOf<String, TextOverlay>()
    private var running = true
    private var timerSeconds = 0f                       // таймер сенсора TIMER
    private var timerRunning = true                     // TimerStart/TimerStop управляют инкрементом
    private var cloneCounter = java.util.concurrent.atomic.AtomicInteger(0) // счётчик номеров клонов (cloneIndex)
    /** Локальная «база данных» таблиц (2D), ключ — имя таблицы. */
    private val localDb = mutableMapOf<String, MutableList<MutableList<Double>>>()
    /** Локальное облачное хранилище (Firebase-like), ключ — "id:key". */
    private val baseStore = mutableMapOf<String, String>()
    /** 2D buffers by name. */
    private val buffers = mutableMapOf<String, BufferState>()
    /** Проигрыватели видео (имя → состояние). Воспроизведение best-effort. */
    private val videos = mutableMapOf<String, VideoState>()
    private var runAsSpriteDepth = 0                    // глубина RunAsSpriteBrick (защита от рекурсии)
    private var scriptIndexForSprite: (Int) -> Int = { 0 }
    /** Флаг: был ли вызван ExitStage/FinishStage. */
    private var hasProjectExited = false
    /** Локальный HTTP-сервер (для Server-блоков). */
    private var localHttpServer: com.sun.net.httpserver.HttpServer? = null
    /** Broadcast-wait: сообщения, ожидающие завершения получателей.
     *  Ключ = сообщение, значение = список индексов sender-скриптов (ScriptState). */
    private val pendingBroadcastWaits = mutableMapOf<String, MutableList<ScriptState>>()
    /** Screen shader placeholder (2D best-effort). */
    private var screenShaderVertexCode: String = ""
    private var screenShaderFragmentCode: String = ""
    /** Stack for ReturnToPreviousProjectBrick / LaunchProjectBrick. */
    private val projectHistory = java.util.ArrayDeque<java.io.File>()
    /** Best-effort FPS cap requested by SetFpsBrick. */
    private var targetFps = 0

    init {
        parseProject()
    }

    fun getTargetFps(): Int = targetFps

    fun start() { running = true }
    fun stop() { running = false }
    fun isRunning(): Boolean = running

    @Suppress("UNCHECKED_CAST")
    fun setVariable(name: String, value: Any) { variables[name] = value }
    fun getVariable(name: String): Any = variables[name] ?: 0f
    fun getVariableFloat(name: String): Float {
        val v = variables[name]
        return when (v) {
            is Number -> v.toFloat()
            is String -> v.toFloatOrNull() ?: 0f
            else -> 0f
        }
    }

    fun resetTimer() { timerSeconds = 0f }

    // ──────────────────────────── update loop ────────────────────────────

    fun update(deltaSeconds: Float) {
        if (!running) return

        if (timerRunning) timerSeconds += deltaSeconds

        // update text overlays (bubble timers)
        updateTextOverlays(deltaSeconds)

        // update variable display overlays with latest values
        updateVariableOverlays()

        // Audio fade processing
        processAudioFades(deltaSeconds)

        // ── Фаза 1: проверка событий для event-скриптов ──
        checkEvents()

        // ── Проверка broadcast_wait: если все получатели завершились, разблокировать sender'ов ──
        checkBroadcastWaits()

        // ── Фаза 2: выполнение ──
        for (state in scriptStates) {
            if (state.isDone) {
                // Event-скрипты: если завершились, сбрасываем в ожидание следующего события
                if (state.eventType != null) {
                    state.reset()
                }
                continue
            }
            state.tick(deltaSeconds)

            // Glide-анимацию обрабатываем даже если waitTimer активен
            if (state.hasGlide) {
                processGlideForState(state, deltaSeconds)
                continue
            }

            if (state.isWaiting) continue

            // Event-скрипты не выполняются, пока событие не произойдёт
            if (state.eventType != null && !state.eventFired) continue

            executeState(state, deltaSeconds)
        }
    }

    /** Проверить условия для event-скриптов и установить eventFired. */
    private fun checkEvents() {
        // Собираем, какие broadcst сообщения были отправлены в этом кадре
        val activeBroadcasts = mutableSetOf<String>()
        val keyJustPressed = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)
        val mouseJustClicked = Gdx.input.justTouched()
        val scrollAmount = input.mouseScroll

        for (state in scriptStates) {
            if (state.eventType == null) continue
            if (state.eventFired) continue

            val sprite = project.sprites.getOrNull(state.spriteIndex)
            if (sprite == null) { state.eventFired = true; continue }

            when (state.eventType) {
                "touch_down" -> {
                    // Касание/клик на сцене
                    if (input.isTouched && input.fingerY != 0f) {
                        state.eventFired = true
                    }
                }
                "cloned" -> {
                    // WhenClonedScript: всегда запускается один раз при старте
                    // Чтобы не запускать бесконечно, разрешаем только если скрипт не запускался
                    state.eventFired = (state.currentFrame?.ip ?: 0) == 0
                }
                "condition" -> {
                    // WhenConditionScript: проверяем формулу условия IF_CONDITION
                    val condEl = state.conditionFormula
                    if (condEl != null) {
                        val result = evaluateFormulaNode(condEl, state.spriteIndex)
                        val cond = when (result) {
                            is Number -> result.toDouble() != 0.0
                            is Boolean -> result
                            else -> false
                        }
                        state.eventFired = cond
                    }
                }
                "back_pressed" -> {
                    if (keyJustPressed) {
                        state.eventFired = true
                    }
                }
                "mouse_clicked" -> {
                    if (mouseJustClicked) {
                        // Если есть eventParam (buttonCode), проверяем совпадение
                        val btnCode = state.eventParam?.toIntOrNull()
                        if (btnCode == null || btnCode == 0) {
                            state.eventFired = true
                        }
                    }
                }
                "mouse_wheel" -> {
                    if (scrollAmount != 0f) {
                        state.eventFired = true
                    }
                }
                "gamepad_button" -> {
                    val action = state.eventParam ?: ""
                    when (action) {
                        "A" -> if (Gdx.input.isKeyJustPressed(Input.Keys.A)) state.eventFired = true
                        "B" -> if (Gdx.input.isKeyJustPressed(Input.Keys.B)) state.eventFired = true
                        "X" -> if (Gdx.input.isKeyJustPressed(Input.Keys.X)) state.eventFired = true
                        "Y" -> if (Gdx.input.isKeyJustPressed(Input.Keys.Y)) state.eventFired = true
                        "up" -> if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) state.eventFired = true
                        "down" -> if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) state.eventFired = true
                        "left" -> if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) state.eventFired = true
                        "right" -> if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) state.eventFired = true
                        "start" -> if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) state.eventFired = true
                        "select" -> if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)) state.eventFired = true
                        "R1" -> if (Gdx.input.isKeyJustPressed(Input.Keys.E)) state.eventFired = true
                        "L1" -> if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) state.eventFired = true
                    }
                }
                "bounce_off" -> {
                    // Проверяем коллизию с другим спрайтом
                    if (physicsWorld != null && physicsWorld.hasBody(sprite)) {
                        val targetName = state.eventParam
                        val touching = checkSpriteCollision(sprite, targetName)
                        if (touching) {
                            state.eventFired = true
                        }
                    }
                }
                "background_changes" -> {
                    // Запускается при смене фона — по умолчанию один раз
                    state.eventFired = (state.currentFrame?.ip ?: 0) == 0
                }
                "broadcast_receiver" -> {
                    // Доставка broadcast теперь выполняется напрямую в executeControl
                    // (см. deliverBroadcast), поэтому здесь ничего проверять не нужно.
                }
                "scene_start", "scene_preloaded" -> {
                    // Запускается при старте сцены
                    state.eventFired = (state.currentFrame?.ip ?: 0) == 0
                }
                "project_exits" -> {
                    // При завершении — триггерим все
                    state.eventFired = !hasProjectExited
                }
                "app_minimized", "app_restored" -> {
                    // На десктопе не поддерживается — пропускаем
                }
            }
        }
    }

    /** Проверить коллизию спрайта с другим спрайтом (bounding box). */
    private fun checkSpriteCollision(sprite: DesktopSprite, targetName: String?): Boolean {
        for (other in project.sprites) {
            if (other === sprite) continue
            if (targetName != null && targetName.isNotEmpty() && other.name != targetName) continue
            val dx = abs(sprite.x - other.x)
            val dy = abs(sprite.y - other.y)
            val halfW = (sprite.lookWidth + other.lookWidth) / 2f
            val halfH = (sprite.lookHeight + other.lookHeight) / 2f
            if (dx < halfW && dy < halfH) return true
        }
        return false
    }

    /**
     * Запустить WhenCloned скрипты для нового клона.
     * @param cloneIdx индекс клона в project.sprites
     * @param srcIdx индекс исходного спрайта
     */
    private fun triggerWhenClonedForClone(cloneIdx: Int, srcIdx: Int) {
        val clone = project.sprites.getOrNull(cloneIdx) ?: return
        for (origState in scriptStates) {
            if (origState.eventType == "cloned" && origState.spriteIndex == srcIdx) {
                // Копируем блоки из оригинального WhenCloned скрипта
                val blocks = origState.frames.firstOrNull()?.blocks ?: continue
                val cloneState = ScriptState(
                    spriteIndex = cloneIdx,
                    originalBlocks = blocks,
                    eventType = "cloned",
                    spriteName = clone.name
                )
                cloneState.eventFired = true
                scriptStates.add(cloneState)
            }
        }
    }

    /**
     * Доставить broadcast-сообщение всем BroadcastScript-получателям.
     * Активирует (eventFired = true) каждый скрипт-получатель с совпадающим
     * сообщением, независимо от их количества (все получатели запускаются).
     */
    private fun deliverBroadcast(msg: String) {
        if (msg.isEmpty()) return
        for (state in scriptStates) {
            if (state.eventType == "broadcast_receiver" && state.eventParam == msg) {
                state.eventFired = true
            }
        }
    }

    /**
     * Проверить, завершились ли все получатели для каждого broadcast_wait.
     * Если да — разблокировать sender'ов (продвинуть их IP).
     */
    private fun checkBroadcastWaits() {
        val completed = mutableListOf<String>()
        for ((msg, senders) in pendingBroadcastWaits) {
            // Проверяем, есть ли ещё активные receiver-скрипты для этого сообщения
            val anyReceiverStillRunning = scriptStates.any { state ->
                state.eventType == "broadcast_receiver" &&
                state.eventParam == msg &&
                !state.isDone
            }
            if (!anyReceiverStillRunning) {
                // Все получатели завершились — разблокируем sender'ов
                for (sender in senders) {
                    val rootFrame = sender.frames.firstOrNull()
                    if (rootFrame != null) {
                        // Продвигаем IP sender'а за broadcast_wait блок
                        rootFrame.ip++
                    }
                }
                completed.add(msg)
            }
        }
        completed.forEach { pendingBroadcastWaits.remove(it) }
    }

    /**
     * Привязать переменную цикла for_item_list к текущему элементу списка.
     * Вызывается перед первой итерацией и в начале каждой следующей итерации.
     */
    private fun bindLoopVar(frame: Frame) {
        val varName = frame.loopVarName ?: return
        val listName = frame.loopListName ?: return
        val list = userLists[listName] ?: return
        val idx = frame.loopCounter
        if (idx in list.indices) variables[varName] = list[idx]
    }

    /** Обработать glide-анимацию на любом фрейме в стеке. */
    private fun processGlideForState(state: ScriptState, delta: Float) {
        for (frame in state.frames) {
            val g = frame.glideState ?: continue
            val sprite = project.sprites.getOrNull(state.spriteIndex) ?: return

            // Детекция внешнего перемещения: если спрайт сдвинулся с предыдущей
            // позиции (другим блоком, физикой или drag-ом), перезапускаем glide
            // из новой позиции с оставшимся временем.
            val movedExternally = abs(sprite.x - g.previousX) > 0.001f || abs(sprite.y - g.previousY) > 0.001f
            if (movedExternally && g.elapsed > 0f) {
                val remaining = maxOf(g.duration - g.elapsed, 0.01f)
                g.startX = sprite.x
                g.startY = sprite.y
                g.duration = remaining
                g.elapsed = 0f
                g.previousX = sprite.x
                g.previousY = sprite.y
                break
            }

            g.elapsed += delta
            val dur = if (g.duration > 0f) g.duration else 1f
            val t = (g.elapsed / dur).coerceIn(0f, 1f)
            sprite.x = g.startX + (g.targetX - g.startX) * t
            sprite.y = g.startY + (g.targetY - g.startY) * t
            g.previousX = sprite.x
            g.previousY = sprite.y
            val body = physicsWorld?.getBody(sprite)
            body?.setTransform(sprite.x, sprite.y, body.angle)
            if (t >= 1f) {
                frame.glideState = null
                frame.ip++
            }
            break // только один glide за раз
        }
    }

    /** Process audio fade in/out over multiple frames. */
    private fun processAudioFades(delta: Float) {
        // Fade in
        val fadeInDur = variables["__fade_in_dur"] as? Float ?: return
        val el = (variables["__fade_in_elapsed"] as? Float ?: 0f) + delta
        variables["__fade_in_elapsed"] = el
        if (fadeInDur > 0f) {
            val vol = (el / fadeInDur).coerceAtMost(1f)
            AudioServiceHolder.audioService?.setVolume(vol)
            MidiServiceHolder.midiService?.setVolume(vol)
        }
        if (el >= fadeInDur) {
            variables.remove("__fade_in_dur")
            variables.remove("__fade_in_elapsed")
            variables.remove("__fade_in_vol")
        }
        // Fade out
        val fadeOutDur = variables["__fade_out_dur"] as? Float ?: return
        val elOut = (variables["__fade_out_elapsed"] as? Float ?: 0f) + delta
        variables["__fade_out_elapsed"] = elOut
        if (fadeOutDur > 0f) {
            val vol = (1f - elOut / fadeOutDur).coerceIn(0f, 1f)
            AudioServiceHolder.audioService?.setVolume(vol)
            MidiServiceHolder.midiService?.setVolume(vol)
        }
        if (elOut >= fadeOutDur) {
            variables.remove("__fade_out_dur")
            variables.remove("__fade_out_elapsed")
            variables.remove("__fade_out_vol")
        }
    }

    /** Исполнить один шаг state-машины (один блок или управление фреймом). */
    private fun executeState(state: ScriptState, delta: Float) {
        activeState = state
        if (!state.cleanFinishedFrames()) return
        val frame = state.currentFrame ?: return
        val sprite = project.sprites.getOrNull(state.spriteIndex) ?: return

        if (frame.ip >= frame.blocks.size) return

        val block = frame.blocks[frame.ip]

        // A single block executes per frame. If it throws, route the error to the
        // nearest enclosing try-frame (catch + finally) implemented by TryCatchFinallyBrick.
        // Without a handler the script is stopped to avoid an infinite error loop.
        try {
            when (block.type) {
                Block.Type.CONTROL -> executeControl(block, sprite, frame, state)
                Block.Type.EVENT -> executeEvent(block, frame)
                Block.Type.LOOKS -> executeLooks(block, sprite, frame)
                Block.Type.MOTION -> executeMotion(block, sprite, frame)
                Block.Type.SOUND -> executeSound(block, sprite, frame)
                Block.Type.MUSIC -> executeMusic(block, frame)
                Block.Type.PEN -> executePen(block, sprite, frame)
                Block.Type.VARIABLE -> executeVariable(block, sprite, frame, state)
                Block.Type.WEB -> executeWeb(block, frame)
                Block.Type.SENSING -> executeSensing(block, frame)
                Block.Type.DATA -> executeData(block, frame)
                Block.Type.FILE -> executeFile(block, frame)
                Block.Type.PHYSICS -> executePhysics(block, sprite, frame)
                Block.Type.CAMERA -> executeCamera(block, sprite, frame)
                Block.Type.VIDEO -> executeVideo(block, frame)
            }
        } catch (e: Exception) {
            handleExecutionException(state, e)
        }
    }

    /**
     * Routes an exception thrown while executing a block to the innermost
     * enclosing try-frame (set up by [executeControl] "try_catch"). Binds the
     * optional catch variable to the error message, then queues the catch body
     * (runs first) followed by the finally body (always runs). If no try-frame
     * is active, the script is stopped (its frames cleared) so a broken block
     * does not crash or stall the whole engine.
     */
    private fun handleExecutionException(state: ScriptState, e: Exception) {
        val tryIdx = state.frames.indexOfLast { it.isTryFrame }
        if (tryIdx < 0) {
            Gdx.app.error("ScriptEngine", "Unhandled script error", e)
            state.frames.clear()
            return
        }
        val tryFrame = state.frames[tryIdx]
        val catchVar = tryFrame.catchVar
        if (!catchVar.isNullOrEmpty()) {
            variables[catchVar] = e.message ?: "error"
        }
        // Drop the try-frame and anything pushed on top of it (abort the body).
        while (state.frames.size > tryIdx) state.frames.removeAt(state.frames.lastIndex)
        // Run the catch body (if any) followed by the finally body (if any) as a
        // single frame, so the parent advances exactly once when the whole
        // try/catch construct completes.
        val onError = mutableListOf<Block>()
        if (!tryFrame.catchBlocks.isNullOrEmpty()) onError.addAll(tryFrame.catchBlocks!!)
        if (!tryFrame.finallyBlocks.isNullOrEmpty()) onError.addAll(tryFrame.finallyBlocks!!)
        if (onError.isNotEmpty()) {
            state.frames.add(Frame(onError, repeatRemaining = 0))
        }
    }

    /**
     * After a sprite at [removedIndex] is removed from [DesktopProject.sprites],
     * decrement the stored [ScriptState.spriteIndex] of every script that pointed
     * at a sprite after it, so scripts keep acting on the correct sprite instead
     * of a stale (shifted) index.
     */
    private fun remapSpriteIndicesAfterRemoval(removedIndex: Int) {
        for (s in scriptStates) {
            if (s.spriteIndex > removedIndex) s.spriteIndex--
        }
    }

    // ──────────────────────────── CONTROL ────────────────────────────

    private fun executeControl(block: Block, sprite: DesktopSprite, frame: Frame, state: ScriptState) {
        when (block.args.getOrNull(0) as? String) {
            "wait" -> {
                val secs = evalBlockArgFloat(block, 1, sprite, state) ?: 1f
                frame.waitTimer = maxOf(secs, 0.01f)
                frame.ip++
            }
            "wait_until" -> {
                val cond = evalBlockArgFloat(block, 1, sprite, state) ?: 0f
                if (cond == 0f) {
                    // условие не выполнено — ждём следующий кадр
                } else {
                    frame.ip++
                }
            }
            "repeat_until" -> {
                val condRf = block.args.getOrNull(1) as? RuntimeFormula
                if (condRf != null) {
                    val cond = evaluateBrickFieldFormula(sprite, state, condRf) ?: 0f
                    if (cond != 0f) {
                        // condition met — advance to next block
                        frame.ip++
                    } else {
                        // condition not met — push children, re-evaluate on next cycle
                        state.frames.add(Frame(block.children, repeatRemaining = -2))
                    }
                } else {
                    // fallback: static condition value
                    val cond = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                    if (cond != 0f) {
                        frame.ip++
                    } else {
                        state.frames.add(Frame(block.children, repeatRemaining = -2))
                    }
                }
            }
            "forever" -> {
                state.frames.add(Frame(block.children, repeatRemaining = -1))
            }
            "repeat" -> {
                val times = evalBlockArgFloat(block, 1, sprite, state)?.toInt() ?: 1
                if (times > 0) {
                    state.frames.add(Frame(block.children, repeatRemaining = times))
                } else {
                    frame.ip++
                }
            }
            "if" -> {
                val conditionValue = evalBlockArgFloat(block, 1, sprite, state) ?: 0f
                if (conditionValue != 0f) {
                    state.frames.add(Frame(block.children, repeatRemaining = 0))
                } else {
                    val elseChildren = block.args.getOrNull(3) as? List<*> ?: emptyList<Block>()
                    @Suppress("UNCHECKED_CAST")
                    val elseBlocks = elseChildren as? List<Block>
                    if (elseBlocks != null && elseBlocks.isNotEmpty()) {
                        state.frames.add(Frame(elseBlocks, repeatRemaining = 0))
                    } else {
                        frame.ip++
                    }
                }
            }
            "broadcast" -> {
                // broadcast — доставляем сообщение всем получателям (BroadcastScript)
                val msg = block.args.getOrNull(1) as? String ?: ""
                deliverBroadcast(msg)
                frame.ip++
            }
            "broadcast_wait" -> {
                // broadcast and wait — доставляем сообщение и ждём завершения
                // всех скриптов-получателей.
                val msg = block.args.getOrNull(1) as? String ?: ""
                deliverBroadcast(msg)
                // Регистрируем sender в pendingBroadcastWaits — executeState
                // не будет продвигать IP, пока все получатели не завершатся.
                val list = pendingBroadcastWaits.getOrPut(msg) { mutableListOf() }
                if (!list.contains(state)) list.add(state)
                // frame.ip НЕ продвигаем — wait снимается после завершения получателей
            }
            "clone" -> {
                // клонировать спрайт и запустить WhenCloned скрипты
                val srcSprite = project.sprites.getOrNull(state.spriteIndex)
                if (srcSprite != null) {
                    val clone = srcSprite.copy()
                    clone.cloneIndex = cloneCounter.incrementAndGet()
                    project.sprites.add(clone)
                    val cloneIdx = project.sprites.lastIndex
                    triggerWhenClonedForClone(cloneIdx, state.spriteIndex)
                }
                frame.ip++
            }
            "delete_this_clone" -> {
                val spriteIdx = state.spriteIndex
                if (spriteIdx > 0 && spriteIdx < project.sprites.size) {
                    val removedSprite = project.sprites[spriteIdx]
                    project.sprites.removeAt(spriteIdx)
                    physicsWorld?.removeBody(removedSprite)
                    for (s in scriptStates) {
                        if (s !== state && s.spriteIndex > spriteIdx) s.spriteIndex--
                    }
                    state.frames.clear()
                }
                frame.ip = frame.blocks.size
            }
            "stop_script" -> {
                // остановить текущий скрипт
                frame.ip = frame.blocks.size // выход
            }
            "run_as_start" -> {
                runAsSpriteDepth++
                if (runAsSpriteDepth > 10) {
                    // Превышена глубина рекурсии — пропускаем детей
                    frame.ip = frame.blocks.size
                } else {
                    frame.ip++
                }
            }
            "run_as_end" -> {
                runAsSpriteDepth--
                frame.ip++
            }
            // ── New Control bricks ──
            "set_fps" -> {
                targetFps = (block.args.getOrNull(1) as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
                frame.ip++
            }
            "set_render_resolution" -> {
                variables["__render_scale"] = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
                variables["__render_aspect_mode"] = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                frame.ip++
            }
            "launch_project" -> {
                val projectName = block.args.getOrNull(1) as? String ?: ""
                if (projectName.isNotEmpty()) {
                    val currentDir = project.projectDir
                    val candidate = when {
                        currentDir == null -> java.io.File(projectName)
                        else -> currentDir.resolve(projectName)
                    }
                    if (candidate.exists()) {
                        if (currentDir != null) projectHistory.addLast(currentDir)
                        project.projectDir = candidate
                        parseProject()
                    }
                }
                frame.ip++
            }
            "return_previous_project" -> {
                val previous = if (projectHistory.isNotEmpty()) projectHistory.removeLast() else null
                if (previous != null && previous.exists()) {
                    project.projectDir = previous
                    parseProject()
                }
                frame.ip++
            }
            "async_repeat" -> {
                val times = evalBlockArgFloat(block, 1, sprite, state)?.toInt() ?: 1
                if (times > 0) {
                    state.frames.add(Frame(block.children, repeatRemaining = times))
                } else {
                    frame.ip++
                }
            }
            "for_item_list" -> {
                val listName = block.args.getOrNull(1) as? String ?: ""
                val varName = block.args.getOrNull(2) as? String ?: ""
                val list = userLists[listName]
                val items = (list?.size ?: 0)
                // Iterate: for each item, execute children. Loop variable is bound to
                // the current item at the start of every iteration (see bindLoopVar).
                if (items > 0) {
                    val loopFrame = Frame(block.children, repeatRemaining = items)
                    loopFrame.loopVarName = varName
                    loopFrame.loopListName = listName
                    bindLoopVar(loopFrame)
                    state.frames.add(loopFrame)
                } else {
                    frame.ip++
                }
            }
            "interval_repeat" -> {
                val count = (block.args.getOrNull(1) as? Number)?.toInt() ?: 1
                val interval = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                if (count > 0) {
                    state.frames.add(Frame(block.children, repeatRemaining = count))
                    frame.waitTimer = interval
                } else {
                    frame.ip++
                }
            }
            "try_catch" -> {
                val catchVar = block.args.getOrNull(1) as? String ?: ""
                @Suppress("UNCHECKED_CAST")
                val tryBlocks = (block.args.getOrNull(2) as? List<*>)?.filterIsInstance<Block>() ?: block.children
                @Suppress("UNCHECKED_CAST")
                val catchBlocks = (block.args.getOrNull(3) as? List<*>)?.filterIsInstance<Block>() ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val finallyBlocks = (block.args.getOrNull(4) as? List<*>)?.filterIsInstance<Block>() ?: emptyList()
                // Execute try blocks inside a try-frame that carries catch/finally
                // context so errors can be routed and finally always runs.
                val tryFrame = Frame(tryBlocks, repeatRemaining = 0)
                tryFrame.isTryFrame = true
                tryFrame.catchVar = catchVar
                tryFrame.catchBlocks = catchBlocks
                tryFrame.finallyBlocks = finallyBlocks
                state.frames.add(tryFrame)
                    // Do NOT frame.ip++ here: when the try-frame completes,
                    // cleanFinishedFrames advances the parent frame past this block.
                    // Advancing here too would skip the block after the try/catch.
                }
                "switch_begin" -> {
                    val switchVal = block.args.getOrNull(1) as? String ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val cases = (block.args.getOrNull(2) as? List<*>)?.filterIsInstance<SwitchCase>() ?: emptyList()
                    // Execute only the first matching case body. Advancing past the
                    // switch block is handled when this nested frame completes
                    // (cleanFinishedFrames advances the parent frame), so do NOT ip++
                    // here when a body was pushed — otherwise the block right after
                    // the switch would be skipped (double advance).
                    val matched = cases.firstOrNull { it.value == switchVal }
                    if (matched != null) {
                        state.frames.add(Frame(matched.body, repeatRemaining = 0))
                    } else {
                        frame.ip++
                    }
                }
            "user_call" -> {
                val procId = block.args.getOrNull(1) as? String ?: ""
                val argFormulas = (block.args.getOrNull(2) as? List<*>) ?: emptyList<Any>()
                val proc = procedures[procId]
                if (proc == null) {
                    frame.ip++
                    return
                }
                // Вычисляем аргументы и связываем с именами параметров
                val procVars = mutableMapOf<String, Any>()
                for (i in proc.paramNames.indices) {
                    val rf = argFormulas.getOrNull(i) as? RuntimeFormula
                    val v = if (rf != null) {
                        evaluateFormulaNode(rf.formulaElement, state.spriteIndex)
                    } else null
                    procVars[proc.paramNames[i]] = v ?: 0f
                }
                // Запускаем тело процедуры в новом фрейме с локальной областью параметров
                val procFrame = Frame(proc.body, repeatRemaining = 0)
                procFrame.procVars = procVars
                state.frames.add(procFrame)
                frame.ip++
            }
            "load_scene" -> {
                val sceneName = block.args.getOrNull(1) as? String ?: ""
                if (sceneName.isNotEmpty()) {
                    // Reload project from scene file
                    val projectDir = project.projectDir?.resolve(sceneName) ?: java.io.File(sceneName)
                    if (projectDir.isDirectory || projectDir.exists()) {
                        project.projectDir = projectDir
                        parseProject()
                    }
                }
            }
            "clear_scene" -> {
                val sceneName = block.args.getOrNull(1) as? String ?: ""
                if (sceneName.isNotEmpty()) {
                    val targetDir = project.projectDir?.resolve(sceneName)
                    if (targetDir?.exists() == true) {
                        project.projectDir = targetDir
                        parseProject()
                    }
                }
            }
            "scene_transition" -> {
                val sceneName = block.args.getOrNull(1) as? String ?: ""
                if (sceneName.isNotEmpty()) {
                    val targetDir = project.projectDir?.resolve(sceneName)
                    if (targetDir?.exists() == true) {
                        project.projectDir = targetDir
                        parseProject()
                    }
                }
            }
            "slide_scene" -> {
                val direction = block.args.getOrNull(1) as? String ?: ""
                val sceneName = block.args.getOrNull(2) as? String ?: ""
                if (sceneName.isNotEmpty()) {
                    val targetDir = project.projectDir?.resolve(sceneName)
                    if (targetDir?.exists() == true) {
                        project.projectDir = targetDir
                        parseProject()
                    }
                }
            }
            "fade_scene" -> {
                val mode = block.args.getOrNull(1) as? String ?: ""
                val sceneName = block.args.getOrNull(2) as? String ?: ""
                if (sceneName.isNotEmpty()) {
                    val targetDir = project.projectDir?.resolve(sceneName)
                    if (targetDir?.exists() == true) {
                        project.projectDir = targetDir
                        parseProject()
                    }
                }
            }
            "crossfade_scene" -> {
                val sceneName = block.args.getOrNull(1) as? String ?: ""
                if (sceneName.isNotEmpty()) {
                    val targetDir = project.projectDir?.resolve(sceneName)
                    if (targetDir?.exists() == true) {
                        project.projectDir = targetDir
                        parseProject()
                    }
                }
            }
            "run_shell" -> {
                val code = block.args.getOrNull(1) as? String ?: ""
                val varName = block.args.getOrNull(2) as? String ?: ""
                if (code.isNotEmpty()) {
                    try {
                        val proc = Runtime.getRuntime().exec(code)
                        val output = proc.inputStream.bufferedReader().readText()
                        if (varName.isNotEmpty()) variables[varName] = output
                    } catch (e: Exception) {
                        if (varName.isNotEmpty()) variables[varName] = "Error: ${e.message}"
                    }
                }
            }
            "wait_till_idle" -> {
                // Wait until all frames in the state are completed
                if (!state.isDone) {
                    // Stay on this block; will re-evaluate next frame
                } else {
                    frame.ip++
                }
            }
            // ── Key/Mouse event bricks ──
            "key_event" -> {
                val keyChar = block.args.getOrNull(1) as? String ?: ""
                val keyDown = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                if (keyChar.isNotEmpty()) {
                    val isPressed = com.badlogic.gdx.Gdx.input.isKeyPressed(
                        com.badlogic.gdx.Input.Keys.valueOf(keyChar.uppercase())
                    )
                    if ((keyDown != 0f && isPressed) || (keyDown == 0f && !isPressed)) {
                        frame.ip++
                    }
                } else {
                    frame.ip++
                }
            }
            "mouse_event" -> {
                val mx = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val my = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val state = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                frame.ip++
            }
            // ── Notification bricks (delegated to NotificationService) ──
            "send_notification" -> {
                val notifId = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                try {
                    org.catrobat.catroid.notification.NotificationServiceHolder.service?.show(notifId)
                } catch (_: Exception) { }
                frame.ip++
            }
            "show_scheduled_notification" -> {
                val notifId = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val delaySec = (block.args.getOrNull(2) as? Number)?.toDouble() ?: 0.0
                try {
                    org.catrobat.catroid.notification.NotificationServiceHolder.service
                        ?.showScheduled(notifId, (delaySec * 1000).toLong())
                } catch (_: Exception) { }
                frame.ip++
            }
            "prepare_notification" -> {
                val id = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val channel = block.args.getOrNull(2) as? String ?: "default"
                val title = block.args.getOrNull(3) as? String ?: ""
                val text = block.args.getOrNull(4) as? String ?: ""
                val icon = block.args.getOrNull(5) as? String ?: ""
                try {
                    org.catrobat.catroid.content.notification.NotificationStorage.save(
                        id,
                        org.catrobat.catroid.content.notification.NotificationData(
                            id = id,
                            channelName = channel,
                            title = title,
                            text = text,
                            iconPath = icon,
                            importanceLevel = org.catrobat.catroid.notification.NotificationService.IMPORTANCE_DEFAULT,
                            isPinned = false
                        )
                    )
                } catch (_: Exception) { }
                frame.ip++
            }
            "notification_action" -> {
                val id = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val actionId = block.args.getOrNull(2) as? String ?: ""
                val text = block.args.getOrNull(3) as? String ?: ""
                val icon = block.args.getOrNull(4) as? String ?: ""
                val hint = block.args.getOrNull(5) as? String ?: ""
                try {
                    org.catrobat.catroid.content.notification.NotificationStorage.addAction(
                        id,
                        org.catrobat.catroid.content.notification.NotificationActionData(
                            actionId = actionId,
                            text = text,
                            iconPath = icon,
                            behavior = org.catrobat.catroid.content.notification.ActionBehavior.RUN_IN_BACKGROUND,
                            hasInput = false,
                            inputHint = hint,
                            autoCancel = true
                        )
                    )
                } catch (_: Exception) { }
                frame.ip++
            }
            "remove_notification" -> {
                val id = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                try {
                    org.catrobat.catroid.notification.NotificationServiceHolder.service?.remove(id)
                    org.catrobat.catroid.content.notification.NotificationStorage.removeNotification(id)
                } catch (_: Exception) { }
                frame.ip++
            }
            "enable_background" -> {
                // На Desktop фоновое выполнение не применимо — no-op
                frame.ip++
            }
            // ── Clone bricks ──
            "clone_object" -> {
                val srcName = block.args.getOrNull(1) as? String ?: ""
                val newName = block.args.getOrNull(2) as? String ?: ""
                val src = project.sprites.find { it.name == srcName }
                if (src != null) {
                    val clone = src.copy()
                    clone.name = if (newName.isNotEmpty()) newName else "${srcName}_clone"
                    clone.cloneIndex = cloneCounter.incrementAndGet()
                    project.sprites.add(clone)
                    val cloneIdx = project.sprites.lastIndex
                    val srcIdx = project.sprites.indexOfFirst { it.name == srcName }
                    if (srcIdx >= 0) triggerWhenClonedForClone(cloneIdx, srcIdx)
                }
                frame.ip++
            }
            // ── Clone-by-number ──
            "delete_clone_by_number" -> {
                val n = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                if (n > 0) {
                    for (state in scriptStates) {
                        val sp = project.sprites.getOrNull(state.spriteIndex)
                        if (sp != null && sp.cloneIndex == n) {
                            project.sprites.removeAt(state.spriteIndex)
                            physicsWorld?.removeBody(sp)
                            state.frames.clear()
                            break
                        }
                    }
                }
                frame.ip++
            }
            "clone_and_name" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val srcSprite = project.sprites.getOrNull(state.spriteIndex)
                if (srcSprite != null && name.isNotEmpty()) {
                    val clone = srcSprite.copy()
                    clone.name = name
                    clone.cloneIndex = cloneCounter.incrementAndGet()
                    project.sprites.add(clone)
                    val cloneIdx = project.sprites.lastIndex
                    triggerWhenClonedForClone(cloneIdx, state.spriteIndex)
                }
                frame.ip++
            }
            "timer_start" -> {
                variables["__timer_running"] = 1f
                variables["__timer_start"] = System.nanoTime().toDouble()
                frame.ip++
            }
            "timer_stop" -> {
                variables["__timer_running"] = 0f
                frame.ip++
            }
            "stop_background" -> {
                // Desktop: no background processing — no-op
                frame.ip++
            }
            "load_scene_additive" -> {
                val scene = block.args.getOrNull(1) as? String ?: ""
                if (scene.isNotEmpty()) {
                    val targetDir = project.projectDir?.resolve(scene)
                    if (targetDir?.exists() == true) {
                        project.projectDir = targetDir
                        parseProject()
                    }
                }
                frame.ip++
            }
            "preload_scene" -> {
                // Desktop: scenes preloaded on demand — no-op
                frame.ip++
            }
            "cast_ray" -> {
                val x1 = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val y1 = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val x2 = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val y2 = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val dx = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val dy = (block.args.getOrNull(6) as? Number)?.toFloat() ?: 0f
                val maxd = (block.args.getOrNull(7) as? Number)?.toFloat() ?: 1000f
                val results = physicsWorld?.rayCast(x1, y1, x2, y2) ?: emptyList()
                val asVar = "raycast_result"
                variables[asVar] = results.size.toFloat()
                results.forEachIndexed { idx, r ->
                    variables["${asVar}_${idx}_x"] = r.pointX
                    variables["${asVar}_${idx}_y"] = r.pointY
                }
                frame.ip++
            }
            "set_parent" -> {
                val child = block.args.getOrNull(1) as? String ?: ""
                val parent = block.args.getOrNull(2) as? String ?: ""
                if (child.isNotEmpty() && parent.isNotEmpty()) {
                    variables["__parent_$child"] = parent
                }
                frame.ip++
            }
            "remove_parent" -> {
                val child = block.args.getOrNull(1) as? String ?: ""
                if (child.isNotEmpty()) variables.remove("__parent_$child")
                frame.ip++
            }
            "delay" -> {
                val t = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                frame.waitTimer = maxOf(frame.waitTimer, t)
                frame.ip++
            }
            // ── NeoScript runtime stubs ──
            "assign_scripts" -> {
                val filePath = block.args.getOrNull(1) as? String ?: ""
                val objName = block.args.getOrNull(2) as? String ?: ""
                val sceneName = block.args.getOrNull(3) as? String ?: ""
                val replaceSel = block.args.getOrNull(4) as? String ?: "0"
                val saveSel = block.args.getOrNull(5) as? String ?: "0"
                // На Desktop .neoscript импорт — заглушка (загрузка file-path не поддерживается в runtime)
                if (filePath.isNotEmpty()) variables["__assign_script_$objName"] = filePath
                frame.ip++
            }
            "import_script" -> {
                val objName = block.args.getOrNull(1) as? String ?: ""
                val filePath = block.args.getOrNull(2) as? String ?: ""
                val overwriteSel = block.args.getOrNull(3) as? String ?: "0"
                if (filePath.isNotEmpty()) variables["__import_script_$objName"] = filePath
                frame.ip++
            }
            "create_object" -> {
                val objName = block.args.getOrNull(1) as? String ?: ""
                val sceneSel = block.args.getOrNull(2) as? String ?: "0"
                val persistSel = block.args.getOrNull(3) as? String ?: "0"
                if (objName.isNotEmpty()) {
                    val newSprite = DesktopSprite(name = objName)
                    newSprite.cloneIndex = cloneCounter.incrementAndGet()
                    project.sprites.add(newSprite)
                }
                frame.ip++
            }
            // ── Dialog/misc stubs ──
            "create_dialog" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                val varName = block.args.getOrNull(3) as? String ?: ""
                if (text.isNotEmpty() && varName.isNotEmpty()) {
                    val answer = javax.swing.JOptionPane.showInputDialog(null, text, name.ifEmpty { "Dialog" }, javax.swing.JOptionPane.QUESTION_MESSAGE)
                    variables[varName] = answer ?: ""
                }
                frame.ip++
            }
            "hide_status_bar" -> {
                // Desktop: нет status bar — no-op
                frame.ip++
            }
            "toggle_display" -> {
                val stateVal = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__display_on"] = stateVal
                frame.ip++
            }
            "set_orientation" -> {
                val orientation = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__orientation"] = orientation
                frame.ip++
            }
            "set_save_scenes" -> {
                val save = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__save_scenes"] = save
                frame.ip++
            }
            "add_edit" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                // Add/edit UI element — no-op on desktop
                frame.ip++
            }
            "add_radio" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                frame.ip++
            }
            "create_buffer" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val w = (block.args.getOrNull(2) as? Number)?.toInt() ?: 0
                val h = (block.args.getOrNull(3) as? Number)?.toInt() ?: 0
                if (name.isNotEmpty()) variables["__buffer_$name"] = "$w,$h"
                frame.ip++
            }
            "add_to_buffer" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                frame.ip++
            }
            "remove_from_buffer" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                frame.ip++
            }
            "save_buffer" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val file = block.args.getOrNull(2) as? String ?: ""
                frame.ip++
            }
            "apply_buffer_look" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                frame.ip++
            }
            "set_buffer_auto_update" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val stateVal = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                frame.ip++
            }
            "set_buffer_mode" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val r2d = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val r3d = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                frame.ip++
            }
            "set_buffer_only" -> {
                val stateVal = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                frame.ip++
            }
            "grid" -> {
                val x = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val w = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val h = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                variables["__grid"] = "$x,$y,$w,$h"
                frame.ip++
            }
            "set_ai" -> {
                val objId = block.args.getOrNull(1) as? String ?: ""
                val mode = (block.args.getOrNull(2) as? Number)?.toInt() ?: 0
                val speed = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val dist = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val range = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val avoid = (block.args.getOrNull(6) as? Number)?.toFloat() ?: 0f
                val step = (block.args.getOrNull(7) as? Number)?.toFloat() ?: 0f
                val target = block.args.getOrNull(8) as? String ?: ""
                if (objId.isNotEmpty()) variables["__ai_$objId"] = "$mode,$speed,$dist,$range,$avoid,$step,$target"
                frame.ip++
            }
        }
    }

    // ──────────────────────────── EVENT ────────────────────────────

    private fun executeEvent(block: Block, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "broadcast_msg" -> {
                // broadcast_msg внутри скрипта
                frame.ip++
            }
            "green_flag" -> {
                // Event-маркер — просто продвигаем IP
                frame.ip++
            }
            else -> {
                frame.ip++
            }
        }
    }

    // ──────────────────────────── LOOKS ────────────────────────────

    private fun executeLooks(block: Block, sprite: DesktopSprite, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "show" -> sprite.visible = true
            "hide" -> sprite.visible = false
            "switch_look" -> {
                val idx = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                if (sprite.looks.isNotEmpty()) {
                    sprite.currentLookIndex = idx.coerceIn(0, sprite.looks.lastIndex)
                    sprite.resetSprite()
                }
            }
            "switch_look_by_name" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    val idx = sprite.looks.indexOfFirst { it.name == name }
                    if (idx >= 0) {
                        sprite.currentLookIndex = idx
                        sprite.resetSprite()
                    }
                }
            }
            "next_look" -> {
                if (sprite.looks.isNotEmpty()) {
                    sprite.currentLookIndex = (sprite.currentLookIndex + 1) % sprite.looks.size
                    sprite.resetSprite()
                }
            }
            "previous_look" -> {
                if (sprite.looks.isNotEmpty()) {
                    sprite.currentLookIndex = (sprite.currentLookIndex - 1 + sprite.looks.size) % sprite.looks.size
                    sprite.resetSprite()
                }
            }
            "set_size" -> sprite.size = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 100f
            "change_size" -> sprite.size += (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "set_transparency" -> sprite.transparency = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "change_transparency" -> sprite.transparency += (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "set_brightness" -> sprite.brightness = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 100f
            "change_brightness" -> sprite.brightness += (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "set_color" -> sprite.color = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "change_color" -> sprite.color += (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "clear_effects" -> {
                sprite.transparency = 0f
                sprite.brightness = 100f
                sprite.color = 0f
            }
            "set_filter_blur" -> sprite.filterBlur = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "set_filter_pixelate" -> sprite.filterPixelate = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "set_filter_sepia" -> sprite.filterSepia = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "set_font" -> {
                sprite.fontName = block.args.getOrNull(1) as? String ?: ""
                sprite.fontSize = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 14f
            }
            "set_object_color" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val r = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val g = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                val b = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 1f
                val target = project.sprites.find { it.name == name } ?: sprite
                target.objectColorRed = if (r > 1f) (r / 255f) else r
                target.objectColorGreen = if (g > 1f) (g / 255f) else g
                target.objectColorBlue = if (b > 1f) (b / 255f) else b
            }
            "set_object_texture" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val textureName = block.args.getOrNull(2) as? String ?: ""
                val target = project.sprites.find { it.name == name } ?: sprite
                target.objectTexturePath = textureName
                if (textureName.isNotEmpty()) {
                    val imagesDir = project.imagesDir
                    val file = when {
                        java.io.File(textureName).isFile -> java.io.File(textureName)
                        imagesDir != null && java.io.File(imagesDir, textureName).isFile -> java.io.File(imagesDir, textureName)
                        imagesDir != null && java.io.File(imagesDir, "$textureName.png").isFile -> java.io.File(imagesDir, "$textureName.png")
                        else -> null
                    }
                    if (file != null) {
                        try {
                            val tex = com.badlogic.gdx.graphics.Texture(com.badlogic.gdx.Gdx.files.absolute(file.absolutePath))
                            val lookName = file.nameWithoutExtension.ifEmpty { textureName }
                            target.looks.clear()
                            target.looks.add(DesktopLook(lookName, file.name, tex))
                            target.currentLookIndex = 0
                            target.resetSprite()
                        } catch (_: Exception) { /* ignore */ }
                    }
                }
            }
            "set_object_shader" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val vertex = block.args.getOrNull(2) as? String ?: ""
                val fragment = block.args.getOrNull(3) as? String ?: ""
                val target = project.sprites.find { it.name == name } ?: sprite
                target.objectShaderVertex = vertex
                target.objectShaderFragment = fragment
            }
            "set_object_shader_uniform" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val uniformName = block.args.getOrNull(2) as? String ?: ""
                val v1 = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val v2 = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val v3 = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val target = project.sprites.find { it.name == name } ?: sprite
                if (uniformName.isNotEmpty()) {
                    target.objectShaderUniforms[uniformName] = Triple(v1, v2, v3)
                }
            }
            "set_screen_shader" -> {
                screenShaderVertexCode = block.args.getOrNull(1) as? String ?: ""
                screenShaderFragmentCode = block.args.getOrNull(2) as? String ?: ""
            }
            "set_width" -> sprite.width = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 100f
            "change_width" -> sprite.width += (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "set_height" -> sprite.height = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 100f
            "change_height" -> sprite.height += (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            "set_volume" -> {
                val vol = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
                AudioServiceHolder.audioService?.setVolume(vol)
                MidiServiceHolder.midiService?.setVolume(vol)
            }
            // ── Text/bubble overlays ──
            "think_bubble" -> {
                val text = block.args.getOrNull(1) as? String ?: ""
                val name = "think_${sprite.name}"
                textOverlays[name] = TextOverlay(
                    name = name, text = text,
                    x = sprite.x, y = sprite.y + 60f,
                    remainingSeconds = -1f, isThink = true
                )
            }
            "say_bubble" -> {
                val text = block.args.getOrNull(1) as? String ?: ""
                val name = "say_${sprite.name}"
                textOverlays[name] = TextOverlay(
                    name = name, text = text,
                    x = sprite.x, y = sprite.y + 60f,
                    remainingSeconds = -1f, isThink = false
                )
            }
            "think_for_bubble" -> {
                val text = block.args.getOrNull(1) as? String ?: ""
                val duration = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 2f
                val name = "think_${sprite.name}"
                textOverlays[name] = TextOverlay(
                    name = name, text = text,
                    x = sprite.x, y = sprite.y + 60f,
                    remainingSeconds = duration, isThink = true
                )
                frame.waitTimer = maxOf(frame.waitTimer, duration)
            }
            "say_for_bubble" -> {
                val text = block.args.getOrNull(1) as? String ?: ""
                val duration = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 2f
                val name = "say_${sprite.name}"
                textOverlays[name] = TextOverlay(
                    name = name, text = text,
                    x = sprite.x, y = sprite.y + 60f,
                    remainingSeconds = duration, isThink = false
                )
                frame.waitTimer = maxOf(frame.waitTimer, duration)
            }
            "show_dialog" -> {
                val msg = block.args.getOrNull(1) as? String ?: ""
                if (msg.isNotEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(null, msg)
                }
            }
            "show_text_overlay" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                val x = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val size = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 14f
                val color = (block.args.getOrNull(6) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) {
                    textOverlays[name] = TextOverlay(
                        name = name, text = text,
                        x = x, y = y, size = size,
                        colorRed = color, colorGreen = color, colorBlue = color,
                        remainingSeconds = -1f
                    )
                }
            }
            "hide_text_overlay" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                textOverlays.remove(name)
            }
            "set_text_overlay" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                textOverlays[name]?.text = text
            }

            // ── New Looks bricks ──
            "set_background" -> {
                val lookName = block.args.getOrNull(1) as? String ?: ""
                val waitFlag = block.args.getOrNull(2) as? String ?: "0"
                // Find background look in the project's first sprite (background)
                val bgSprite = project.sprites.firstOrNull()
                if (bgSprite != null && lookName.isNotEmpty()) {
                    val idx = bgSprite.looks.indexOfFirst { it.name == lookName }
                    if (idx >= 0) {
                        bgSprite.currentLookIndex = idx
                        bgSprite.resetSprite()
                        if (waitFlag == "1") frame.waitTimer = 0.5f
                    }
                }
            }
            "set_look_files" -> {
                val file = block.args.getOrNull(1) as? String ?: ""
                if (file.isNotEmpty() && sprite.looks.isNotEmpty()) {
                    val idx = sprite.looks.indexOfFirst { it.name.contains(file, ignoreCase = true) }
                    if (idx >= 0) {
                        sprite.currentLookIndex = idx
                        sprite.resetSprite()
                    }
                }
            }
            "save_look" -> {
                val fileName = block.args.getOrNull(1) as? String ?: ""
                if (fileName.isNotEmpty()) {
                    val tex = sprite.currentLook()?.texture
                    if (tex != null) {
                        val pixmap = com.badlogic.gdx.graphics.Pixmap.createFromFrameBuffer(0, 0, tex.width, tex.height)
                        com.badlogic.gdx.graphics.PixmapIO.writePNG(
                            com.badlogic.gdx.Gdx.files.absolute(fileName), pixmap)
                        pixmap.dispose()
                    }
                }
            }
            "cut_look" -> {
                // Cut/transform look is not easily doable on desktop without OpenGL framebuffer manipulation
                // For now, log a message
            }
            "resize_img" -> {
                val file = block.args.getOrNull(1) as? String ?: ""
                val w = (block.args.getOrNull(2) as? Number)?.toInt() ?: 100
                val h = (block.args.getOrNull(3) as? Number)?.toInt() ?: 100
                if (file.isNotEmpty()) {
                    try {
                        val img = javax.imageio.ImageIO.read(java.io.File(file))
                        val resized = java.awt.image.BufferedImage(w, h, img.type)
                        val g2d = resized.createGraphics()
                        g2d.drawImage(img, 0, 0, w, h, null)
                        g2d.dispose()
                        javax.imageio.ImageIO.write(resized, "png", java.io.File(file))
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "grayscale_img" -> {
                val file = block.args.getOrNull(1) as? String ?: ""
                if (file.isNotEmpty()) {
                    try {
                        val img = javax.imageio.ImageIO.read(java.io.File(file))
                        val gray = java.awt.image.BufferedImage(img.width, img.height, java.awt.image.BufferedImage.TYPE_BYTE_GRAY)
                        val g2d = gray.createGraphics()
                        g2d.drawImage(img, 0, 0, null)
                        g2d.dispose()
                        javax.imageio.ImageIO.write(gray, "png", java.io.File(file))
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "normalize_img" -> {
                val file = block.args.getOrNull(1) as? String ?: ""
                if (file.isNotEmpty()) {
                    try {
                        val img = javax.imageio.ImageIO.read(java.io.File(file))
                        javax.imageio.ImageIO.write(img, "png", java.io.File(file))
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "set_anim_speed" -> {
                // Placeholder — animation system not implemented on desktop
            }
            "play_anim" -> {
                // Placeholder — animation system not implemented on desktop
            }
            "stop_anim" -> {
                // Placeholder
            }
            "set_active" -> {
                val objName = block.args.getOrNull(1) as? String ?: ""
                val active = block.args.getOrNull(2) as? String ?: "1"
                val target = project.sprites.find { it.name == objName }
                target?.visible = active == "1"
            }
            "show_text_font" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                val font = block.args.getOrNull(3) as? String ?: ""
                val x = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val size = (block.args.getOrNull(6) as? Number)?.toFloat() ?: 14f
                val color = block.args.getOrNull(7) as? String ?: "0"
                if (name.isNotEmpty()) {
                    textOverlays[name] = TextOverlay(name = name, text = text, x = x, y = y, size = size,
                        remainingSeconds = -1f)
                }
            }
            "set_canvas" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    sprite.canvasName = name
                }
            }

            // ── Text/Bubble: Ask, Speak, Clipboard, Toast, AI ──
            "ask" -> {
                val question = block.args.getOrNull(1) as? String ?: ""
                val varName = block.args.getOrNull(2) as? String ?: ""
                if (question.isNotEmpty() && varName.isNotEmpty()) {
                    val answer = javax.swing.JOptionPane.showInputDialog(null, question, "Catroid Ask", javax.swing.JOptionPane.QUESTION_MESSAGE)
                    variables[varName] = answer ?: ""
                }
            }
            "copy_text" -> {
                val text = block.args.getOrNull(1) as? String ?: ""
                try {
                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                    val selection = java.awt.datatransfer.StringSelection(text)
                    clipboard.setContents(selection, null)
                } catch (_: Exception) { /* ignore */ }
            }
            "show_toast" -> {
                val msg = block.args.getOrNull(1) as? String ?: ""
                if (msg.isNotEmpty()) {
                    javax.swing.JOptionPane.showMessageDialog(null, msg, "Toast", javax.swing.JOptionPane.INFORMATION_MESSAGE)
                }
            }
            "speak" -> {
                val text = block.args.getOrNull(1) as? String ?: ""
                if (text.isNotEmpty()) {
                    Thread {
                        try {
                            val proc = Runtime.getRuntime().exec(arrayOf("cmd", "/c", "echo", text))
                            proc.waitFor()
                        } catch (_: Exception) { /* ignore */ }
                    }.start()
                }
            }
            "speak_wait" -> {
                val text = block.args.getOrNull(1) as? String ?: ""
                if (text.isNotEmpty()) {
                    frame.waitTimer = 1f // approximate wait
                }
            }
            "set_gemini_key" -> {
                val key = block.args.getOrNull(1) as? String ?: ""
                if (key.isNotEmpty()) variables["__gemini_key"] = key
            }
            "ask_gemini" -> {
                val question = block.args.getOrNull(1) as? String ?: ""
                val varName = block.args.getOrNull(2) as? String ?: ""
                if (question.isNotEmpty() && varName.isNotEmpty()) {
                    val apiKey = variables["__gemini_key"] as? String ?: ""
                    variables[varName] = askGeminiApi(question, apiKey, "gemini-pro")
                }
            }
            "ask_gemini2" -> {
                val question = block.args.getOrNull(1) as? String ?: ""
                val model = block.args.getOrNull(2) as? String ?: "gemini-pro"
                val varName = block.args.getOrNull(3) as? String ?: ""
                if (question.isNotEmpty() && varName.isNotEmpty()) {
                    val apiKey = variables["__gemini_key"] as? String ?: ""
                    variables[varName] = askGeminiApi(question, apiKey, model)
                }
            }
            "ask_speech" -> {
                val prompt = block.args.getOrNull(1) as? String ?: ""
                val varName = block.args.getOrNull(2) as? String ?: ""
                if (prompt.isNotEmpty() && varName.isNotEmpty()) {
                    val answer = javax.swing.JOptionPane.showInputDialog(null, prompt, "Speech Input", javax.swing.JOptionPane.QUESTION_MESSAGE)
                    variables[varName] = answer ?: ""
                }
            }

            // ═══════ 3D lighting/rendering stubs ═══════
            "set_ambient_light" -> {
                val r = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val g = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val b = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val intensity = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 1f
                variables["__ambient_light"] = "$r,$g,$b,$intensity"
            }
            "set_point_light" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val r = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val g = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                val b = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 1f
                val intensity = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 1f
                val range = (block.args.getOrNull(6) as? Number)?.toFloat() ?: 10f
                if (name.isNotEmpty()) variables["__point_light_$name"] = "$r,$g,$b,$intensity,$range"
            }
            "set_spot_light" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) variables["__spot_light_$name"] = "stored"
            }
            "set_directional_light" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val r = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val g = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                val b = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 1f
                val intensity = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 1f
                if (name.isNotEmpty()) variables["__dir_light_$name"] = "$r,$g,$b,$intensity"
            }
            "set_directional_light2" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val r = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val g = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                val b = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 1f
                val intensity = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 1f
                if (name.isNotEmpty()) variables["__dir_light2_$name"] = "$r,$g,$b,$intensity"
            }
            "set_skybox" -> {
                val texture = block.args.getOrNull(1) as? String ?: ""
                if (texture.isNotEmpty()) variables["__skybox"] = texture
            }
            "set_sky_color" -> {
                val r = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val g = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val b = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                variables["__sky_color"] = "$r,$g,$b"
            }
            "set_fog" -> {
                val r = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val g = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val b = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val density = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                variables["__fog"] = "$r,$g,$b,$density"
            }
            "set_shadows" -> {
                val enabled = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__shadows"] = enabled
            }
            "set_shadow_quality" -> {
                val quality = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__shadow_quality"] = quality
            }
            "set_material" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val metallic = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val roughness = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                if (objectId.isNotEmpty()) variables["__material_$objectId"] = "$metallic,$roughness"
            }
            "set_emissive" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val r = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val g = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val b = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val intensity = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 1f
                if (objectId.isNotEmpty()) variables["__emissive_$objectId"] = "$r,$g,$b,$intensity"
            }
            "set_texture_tiling" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val tx = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val ty = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                if (objectId.isNotEmpty()) variables["__tiling_$objectId"] = "$tx,$ty"
            }
            "set_post_processing" -> {
                val effect = block.args.getOrNull(1) as? String ?: ""
                val intensity = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                if (effect.isNotEmpty()) variables["__post_effect"] = "$effect,$intensity"
            }
            "set_post_processing_new" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val intensity = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                if (name.isNotEmpty()) variables["__post_effect_$name"] = intensity
            }
            "enable_pbr" -> {
                val enabled = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__pbr_enabled"] = enabled
            }
            "set_anisotropic" -> {
                val level = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__anisotropic"] = level
            }
            "set_ccd" -> {
                val enabled = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__ccd"] = enabled
            }
            "set_particle_emission" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val rate = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                if (objectId.isNotEmpty()) variables["__particle_$objectId"] = rate
            }
            "set_spawn_invisible" -> {
                val invisible = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__spawn_invisible"] = invisible
            }
            "set_pitch_only" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val pitchOnly = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                if (objectId.isNotEmpty()) variables["__pitch_only_$objectId"] = pitchOnly
            }
            "promote_light" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) variables["__promote_light_$name"] = 1f
            }
            "set_shader_code" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val vertex = block.args.getOrNull(2) as? String ?: ""
                val fragment = block.args.getOrNull(3) as? String ?: ""
                if (objectId.isNotEmpty()) {
                    variables["__shader_v_$objectId"] = vertex
                    variables["__shader_f_$objectId"] = fragment
                }
            }
            "set_shader_uniform_float" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val uniformName = block.args.getOrNull(2) as? String ?: ""
                val value = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                if (objectId.isNotEmpty() && uniformName.isNotEmpty()) {
                    variables["__uniform_${objectId}_$uniformName"] = value
                }
            }
            "set_shader_uniform_vec3" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val uniformName = block.args.getOrNull(2) as? String ?: ""
                val v1 = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val v2 = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val v3 = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                if (objectId.isNotEmpty() && uniformName.isNotEmpty()) {
                    variables["__uniform3_${objectId}_$uniformName"] = "$v1,$v2,$v3"
                }
            }
            "set_max_point_lights" -> {
                val count = (block.args.getOrNull(1) as? Number)?.toInt() ?: 4
                variables["__max_point_lights"] = count.toFloat()
            }
            "remove_pbr_light" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    variables.remove("__point_light_$name")
                    variables.remove("__spot_light_$name")
                    variables.remove("__dir_light_$name")
                    variables.remove("__dir_light2_$name")
                }
            }
            "set_background_light" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val r = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val g = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val b = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) variables["__bg_light_$name"] = "$r,$g,$b"
            }
            "apply_shader_to_image" -> {
                val fileName = block.args.getOrNull(1) as? String ?: ""
                val shaderName = block.args.getOrNull(2) as? String ?: ""
                if (fileName.isNotEmpty() && shaderName.isNotEmpty()) {
                    variables["__shader_img_$fileName"] = shaderName
                }
            }
            "big_ask" -> {
                val question = block.args.getOrNull(1) as? String ?: ""
                val varName = block.args.getOrNull(2) as? String ?: ""
                if (question.isNotEmpty() && varName.isNotEmpty()) {
                    val answer = javax.swing.JOptionPane.showInputDialog(null, question, "Input", javax.swing.JOptionPane.QUESTION_MESSAGE)
                    variables[varName] = answer ?: ""
                }
            }
        }
        frame.ip++
    }

    // ── Gemini API call helper ──
    private fun askGeminiApi(question: String, apiKey: String, model: String): String {
        if (apiKey.isEmpty()) return "Error: No API key set"
        return try {
            val url = java.net.URI("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey").toURL()
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            val escaped = question.replace("\\", "\\\\").replace("\"", "\\\"")
            val jsonBody = """{"contents":[{"parts":[{"text":"$escaped"}]}]}"""
            conn.outputStream.write(jsonBody.toByteArray())
            val response = conn.inputStream.bufferedReader().readText()
            // Simple JSON parse: extract "text" field after "parts"
            val textPattern = """"text"\s*:\s*"(?:[^"\\]|\\.)*"""".toRegex()
            val textMatch = textPattern.find(response)
            if (textMatch != null) {
                val raw = textMatch.value
                val start = raw.indexOf('"', raw.indexOf(':') + 1) + 1
                val end = raw.lastIndexOf('"')
                if (start < end) raw.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"")
                else "No response"
            } else "No response"
        } catch (e: Exception) { "Error: ${e.message}" }
    }

    // ──────────────────────────── update text overlays ────────────────────────────

    /** Decrement bubble timers and remove expired overlays. */
    private fun updateTextOverlays(deltaSeconds: Float) {
        val expired = mutableListOf<String>()
        for ((key, overlay) in textOverlays) {
            if (overlay.remainingSeconds > 0f) {
                overlay.remainingSeconds -= deltaSeconds
                if (overlay.remainingSeconds <= 0f) {
                    expired.add(key)
                }
            }
        }
        expired.forEach { textOverlays.remove(it) }
    }

    /** Обновить текст переменных в overlay каждый кадр (для ShowTextBrick). */
    private fun updateVariableOverlays() {
        val toRemove = mutableListOf<String>()
        for ((key, overlay) in textOverlays) {
            if (!overlay.isVariable) continue
            // key format: "var_${spriteName}_$varName"
            val parts = key.split("_", limit = 3)
            if (parts.size < 3) continue
            val varName = parts[2]
            val varValue = variables[varName]
            val text = when (varValue) {
                is Double -> if (varValue == varValue.toLong().toDouble()) varValue.toLong().toString() else varValue.toString()
                is Float -> if (varValue == varValue.toLong().toFloat()) varValue.toLong().toString() else varValue.toString()
                is Number -> varValue.toString()
                else -> varValue?.toString() ?: ""
            }
            overlay.text = "$varName: $text"
        }
        toRemove.forEach { textOverlays.remove(it) }
    }

    private fun executeMotion(block: Block, sprite: DesktopSprite, frame: Frame) {
        val body = physicsWorld?.getBody(sprite)
        when (block.args.getOrNull(0) as? String) {
            "move_steps" -> {
                val steps = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                // Catrobat uses Scratch-style angles: 0 = up, 90 = right.
                // Convert to the stage basis where +Y is up and +X is right.
                val angleRad = Math.toRadians((90f - sprite.direction).toDouble()).toFloat()
                sprite.x += (cos(angleRad.toDouble())).toFloat() * steps
                sprite.y += (sin(angleRad.toDouble())).toFloat() * steps
                body?.setTransform(sprite.x, sprite.y, body.angle)
            }
            "turn_right" -> {
                val deg = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                sprite.direction = (sprite.direction + deg) % 360f
            }
            "turn_left" -> {
                val deg = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                sprite.direction = (sprite.direction - deg + 360f) % 360f
            }
            "goto_xy" -> {
                sprite.x = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                sprite.y = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                body?.setTransform(sprite.x, sprite.y, body.angle)
            }
            "goto_touch" -> {
                sprite.x = input.fingerX
                sprite.y = input.fingerY
                body?.setTransform(sprite.x, sprite.y, body.angle)
            }
            "goto_random" -> {
                val w = (project.stageWidth ?: 480).toFloat()
                val h = (project.stageHeight ?: 720).toFloat()
                sprite.x = (Math.random() * (w + 1f)).toFloat() - w / 2f
                sprite.y = (Math.random() * (h + 1f)).toFloat() - h / 2f
                body?.setTransform(sprite.x, sprite.y, body.angle)
            }
            "goto_sprite" -> {
                val destName = block.args.getOrNull(1) as? String ?: ""
                val dest = project.sprites.find { it.name == destName }
                if (dest != null) {
                    sprite.x = dest.x
                    sprite.y = dest.y
                    body?.setTransform(sprite.x, sprite.y, body.angle)
                }
            }
            "set_x" -> {
                sprite.x = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                body?.setTransform(sprite.x, sprite.y, body.angle)
            }
            "set_y" -> {
                sprite.y = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                body?.setTransform(sprite.x, sprite.y, body.angle)
            }
            "change_x" -> {
                sprite.x += (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                body?.setTransform(sprite.x, sprite.y, body.angle)
            }
            "change_y" -> {
                sprite.y += (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                body?.setTransform(sprite.x, sprite.y, body.angle)
            }
            "set_direction" -> sprite.direction = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 90f
            "glide" -> {
                val targetX = (block.args.getOrNull(1) as? Number)?.toFloat() ?: sprite.x
                val targetY = (block.args.getOrNull(2) as? Number)?.toFloat() ?: sprite.y
                val duration = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                frame.glideState = GlideState(
                    startX = sprite.x,
                    startY = sprite.y,
                    targetX = targetX,
                    targetY = targetY,
                    duration = duration
                )
            }
            "bounce" -> {
                // Edge bounce with direction mirroring
                val halfW = sprite.lookWidth / 2f * sprite.size / 100f
                val halfH = sprite.lookHeight / 2f * sprite.size / 100f
                val sw = (project.stageWidth ?: 480) / 2f
                val sh = (project.stageHeight ?: 720) / 2f
                val hitLeft = sprite.x - halfW <= -sw
                val hitRight = sprite.x + halfW >= sw
                val hitBottom = sprite.y - halfH <= -sh
                val hitTop = sprite.y + halfH >= sh
                if (hitLeft || hitRight) {
                    sprite.direction = (180f - sprite.direction + 360f) % 360f
                }
                if (hitTop || hitBottom) {
                    sprite.direction = (360f - sprite.direction) % 360f
                }
            }
            "come_to_front" -> {
                // переместить на передний план
            }
            "go_back_layers" -> {
                val n = (block.args.getOrNull(1) as? Number)?.toInt() ?: 1
                // переместить назад на n слоёв
            }
            "set_rotation_style" -> {
                // 0 = свободно, 1 = зеркально, 2 = не поворачивать
                sprite.rotationStyle = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
            }
            "touch_direction" -> {
                // Вычислить угол от спрайта к точке касания
                val touchX = input.fingerX
                val touchY = input.fingerY
                val dx = touchX - sprite.x
                val dy = touchY - sprite.y
                sprite.direction = (90f - Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
            }
            // ── New Motion bricks ──
            "turn_left_speed" -> {
                val speed = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                sprite.direction = (sprite.direction - speed * 0.02f + 360f) % 360f
            }
            "turn_right_speed" -> {
                val speed = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                sprite.direction = (sprite.direction + speed * 0.02f) % 360f
            }
            "point_to" -> {
                val destName = block.args.getOrNull(1) as? String ?: ""
                val dest = project.sprites.find { it.name == destName }
                if (dest != null) {
                    val dx = dest.x - sprite.x
                    val dy = dest.y - sprite.y
                    sprite.direction = (90f - Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                }
            }
            "set_velocity" -> {
                val vx = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val vy = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val b = physicsWorld?.getBody(sprite)
                if (b != null) {
                    b.linearVelocity = com.badlogic.gdx.math.Vector2(vx, vy)
                } else {
                    sprite.x += vx * 0.02f
                    sprite.y += vy * 0.02f
                }
            }
            "move_to_object" -> {
                val dist = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 10f
                val destName = block.args.getOrNull(2) as? String ?: ""
                val dest = project.sprites.find { it.name == destName }
                if (dest != null) {
                    val dx = dest.x - sprite.x
                    val dy = dest.y - sprite.y
                    val len = sqrt(dx * dx + dy * dy)
                    if (len > 0.01f) {
                        sprite.x += dx / len * dist
                        sprite.y += dy / len * dist
                        body?.setTransform(sprite.x, sprite.y, body.angle)
                    }
                }
            }
            "set_bounce" -> {
                val bounce = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0.5f
                val b = physicsWorld?.getBody(sprite)
                if (b != null && b.fixtureList.size > 0) {
                    b.fixtureList.first().restitution = bounce
                }
            }
            "set_restitution" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val restitution = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0.5f
                val target = project.sprites.find { it.name == objectId } ?: sprite
                physicsWorld?.setBounce(target, restitution)
            }
            "set_rotation_lock" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val lockX = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val lockY = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val lockZ = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val target = project.sprites.find { it.name == objectId } ?: sprite
                target.rotationLockX = lockX != 0f
                target.rotationLockY = lockY != 0f
                target.rotationLockZ = lockZ != 0f
                physicsWorld?.getBody(target)?.isFixedRotation = target.rotationLockX || target.rotationLockY || target.rotationLockZ
            }

            // ── Core game logic (remaining portable) ──
            "clone_and_name" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    val clone = sprite.copy()
                    clone.name = name
                    clone.cloneIndex = cloneCounter.incrementAndGet()
                    project.sprites.add(clone)
                    val cloneIdx = project.sprites.lastIndex
                    triggerWhenClonedForClone(cloneIdx, project.sprites.indexOfFirst { it.name == sprite.name })
                }
            }
                "delete_clone_by_number" -> {
                    val n = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                    val idx = project.sprites.indexOfFirst { it.cloneIndex == n && n > 0 }
                    if (idx >= 0) {
                        val removed = project.sprites[idx]
                        project.sprites.removeAt(idx)
                        // Item 7: drop the physics body so it is not leaked in the world.
                        physicsWorld?.removeBody(removed)
                        // Item 2: indices of later sprites shift down — keep scripts aligned.
                        remapSpriteIndicesAfterRemoval(idx)
                    }
                }
            "timer_start" -> timerRunning = true
            "timer_stop" -> timerRunning = false
            "set_parent" -> {
                val childName = block.args.getOrNull(1) as? String ?: ""
                val parentName = block.args.getOrNull(2) as? String ?: ""
                project.sprites.find { it.name == childName }?.parentName = parentName
            }
            "remove_parent" -> {
                val childName = block.args.getOrNull(1) as? String ?: ""
                project.sprites.find { it.name == childName }?.parentName = null
            }
            "stop_background" -> {
                AudioServiceHolder.audioService?.stopAllSounds()
                MidiServiceHolder.midiService?.stopAllSounds()
            }
            "load_scene_additive" -> { /* Desktop: единая сцена — no-op */ }
            "preload_scene" -> { /* Desktop: no-op */ }
            "cast_ray" -> {
                val x1 = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val y1 = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val dx = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val dy = (block.args.getOrNull(6) as? Number)?.toFloat() ?: 0f
                val maxd = (block.args.getOrNull(7) as? Number)?.toFloat() ?: 1000f
                val results = physicsWorld?.rayCast(x1, y1, x1 + dx * maxd, y1 + dy * maxd) ?: emptyList()
                val hit = results.firstOrNull()?.let { r ->
                    project.sprites.firstOrNull { sp ->
                        val b = physicsWorld?.getBody(sp)
                        b != null && b.fixtureList.any { it === r.fixture }
                    }?.name ?: ""
                } ?: ""
                variables["__cast_ray_hit"] = hit
            }
            "add_edit" -> { /* редактор-блок: no-op в рантайме */ }
            "add_radio" -> { /* редактор-блок: no-op в рантайме */ }
            "set_ai" -> { /* AI-блок: no-op в рантайме (best-effort) */ }
            "create_buffer" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val width = (block.args.getOrNull(2) as? Number)?.toInt() ?: 0
                val height = (block.args.getOrNull(3) as? Number)?.toInt() ?: 0
                if (name.isNotEmpty()) {
                    buffers[name] = BufferState(width = width, height = height)
                }
            }
            "add_to_buffer" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    val state = buffers.getOrPut(name) { BufferState() }
                    state.entries.add(sprite.name)
                }
            }
            "remove_from_buffer" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    buffers[name]?.entries?.removeLastOrNull()
                }
            }
            "save_buffer" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val fileName = block.args.getOrNull(2) as? String ?: ""
                val state = buffers[name]
                if (state != null && fileName.isNotEmpty()) {
                    try {
                        val out = java.io.File(fileName)
                        out.parentFile?.mkdirs()
                        out.writeText(state.entries.joinToString("\n"))
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "apply_buffer_look" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    buffers.getOrPut(name) { BufferState() }
                }
            }
            "set_buffer_auto_update" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val state = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) {
                    buffers.getOrPut(name) { BufferState() }.autoUpdate = state != 0f
                }
            }
            "set_buffer_mode" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val mode2d = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val mode3d = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) {
                    val state = buffers.getOrPut(name) { BufferState() }
                    state.mode2d = mode2d != 0f
                    state.bufferOnly = mode3d != 0f && !state.mode2d
                }
            }
            "set_buffer_only" -> {
                val state = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                buffers.getOrPut(sprite.name) { BufferState() }.bufferOnly = state != 0f
            }
            "grid" -> {
                variables["__grid_x"] = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                variables["__grid_y"] = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                variables["__grid_w"] = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                variables["__grid_h"] = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
            }
            "delay" -> {
                val t = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                if (t > 0f) frame.waitTimer = maxOf(frame.waitTimer, t)
            }
        }
        frame.ip++
    }

    // ──────────────────────────── PHYSICS ────────────────────────────

    /** Выполнить физический блок (Box2D). */
    private fun executePhysics(block: Block, sprite: DesktopSprite, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "set_gravity" -> {
                val gx = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val gy = (block.args.getOrNull(2) as? Number)?.toFloat() ?: -9.8f
                physicsWorld?.setGravity(gx, gy)
            }
            "set_friction" -> {
                val friction = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0.5f
                physicsWorld?.setFriction(sprite, friction)
            }
            "set_mass" -> {
                val mass = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
                physicsWorld?.setMass(sprite, mass)
            }
            "set_restitution" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val restitution = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0.5f
                val target = project.sprites.find { it.name == objectId } ?: sprite
                physicsWorld?.setBounce(target, restitution)
            }
            "set_rotation_lock" -> {
                val objectId = block.args.getOrNull(1) as? String ?: ""
                val lockX = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val lockY = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val lockZ = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val target = project.sprites.find { it.name == objectId } ?: sprite
                target.rotationLockX = lockX != 0f
                target.rotationLockY = lockY != 0f
                target.rotationLockZ = lockZ != 0f
                physicsWorld?.getBody(target)?.isFixedRotation = target.rotationLockX || target.rotationLockY || target.rotationLockZ
            }
            "set_damping" -> {
                val linear = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val angular = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                physicsWorld?.setDamping(sprite, linear, angular)
            }
            "set_physics_type" -> {
                // 0 = Static, 1 = Dynamic, 2 = Kinematic
                val type = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val bodyType = when (type) {
                    1 -> BodyDef.BodyType.DynamicBody
                    2 -> BodyDef.BodyType.KinematicBody
                    else -> BodyDef.BodyType.StaticBody
                }
                physicsWorld?.ensureBody(sprite, type == 0)
                physicsWorld?.setBodyType(sprite, bodyType)
            }
            "set_physics_state" -> {
                val objectId = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val mass = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val stateSelection = (block.args.getOrNull(3) as? Number)?.toInt() ?: 0
                val shapeSelection = (block.args.getOrNull(4) as? Number)?.toInt() ?: 0
                // stateSelection: 1=static, 2=dynamic
                val isStatic = stateSelection != 2
                physicsWorld?.ensureBody(sprite, isStatic)
                physicsWorld?.setMass(sprite, mass)
            }
            "apply_force" -> {
                val fx = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val fy = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                physicsWorld?.applyForce(sprite, fx, fy)
            }
            "apply_impulse" -> {
                val ix = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val iy = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                physicsWorld?.applyImpulse(sprite, ix, iy)
            }
            "apply_torque" -> {
                val torque = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                physicsWorld?.applyTorque(sprite, torque)
            }
            "apply_angular_impulse" -> {
                val impulse = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                physicsWorld?.applyAngularImpulse(sprite, impulse)
            }
            "ray_cast" -> {
                val rayId = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val sx = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val sy = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val ex = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val ey = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val results = physicsWorld?.rayCast(sx, sy, ex, ey) ?: emptyList()
                // Сохраняем результат луча в переменную raycast_result_X
                variables["raycast_count_${rayId.toInt()}"] = results.size.toFloat()
                results.forEachIndexed { idx, r ->
                    val prefix = "raycast_${rayId.toInt()}_$idx"
                    variables["${prefix}_fixture"] = r.fixture.toString()
                    variables["${prefix}_x"] = r.pointX
                    variables["${prefix}_y"] = r.pointY
                    variables["${prefix}_nx"] = r.normalX
                    variables["${prefix}_ny"] = r.normalY
                    variables["${prefix}_frac"] = r.fraction
                }
            }
            "create_joint_distance" -> {
                val name = (block.args.getOrNull(1) as? String) ?: ""
                val sprite2Name = (block.args.getOrNull(2) as? String) ?: ""
                val length = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                val freq = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val damping = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val other = project.sprites.find { it.name == sprite2Name }
                val bA = physicsWorld?.getBody(sprite)
                val bB = if (other != null) physicsWorld?.ensureBody(other) else null
                if (bA != null && bB != null) {
                    val jd = com.badlogic.gdx.physics.box2d.joints.DistanceJointDef()
                    jd.bodyA = bA
                    jd.bodyB = bB
                    jd.collideConnected = true
                    jd.length = length
                    if (freq > 0f) jd.frequencyHz = freq
                    if (damping > 0f) jd.dampingRatio = damping
                    val joint = physicsWorld?.world?.createJoint(jd)
                    if (joint != null && name.isNotEmpty()) physicsWorld?.addJoint(name, joint)
                }
            }
            "create_joint_revolute" -> {
                val name = (block.args.getOrNull(1) as? String) ?: ""
                val sprite2Name = (block.args.getOrNull(2) as? String) ?: ""
                val x = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val other = project.sprites.find { it.name == sprite2Name }
                val bA = physicsWorld?.getBody(sprite)
                val bB = if (other != null) physicsWorld?.ensureBody(other) else null
                if (bA != null && bB != null) {
                    val jd = com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef()
                    jd.bodyA = bA
                    jd.bodyB = bB
                    jd.collideConnected = true
                    jd.localAnchorA.set(x, y)
                    val joint = physicsWorld?.world?.createJoint(jd)
                    if (joint != null && name.isNotEmpty()) physicsWorld?.addJoint(name, joint)
                }
            }
            "create_joint_prismatic" -> {
                val name = (block.args.getOrNull(1) as? String) ?: ""
                val sprite2Name = (block.args.getOrNull(2) as? String) ?: ""
                val x = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val ax = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 1f
                val ay = (block.args.getOrNull(6) as? Number)?.toFloat() ?: 0f
                val other = project.sprites.find { it.name == sprite2Name }
                val bA = physicsWorld?.getBody(sprite)
                val bB = if (other != null) physicsWorld?.ensureBody(other) else null
                if (bA != null && bB != null) {
                    val jd = com.badlogic.gdx.physics.box2d.joints.PrismaticJointDef()
                    jd.bodyA = bA
                    jd.bodyB = bB
                    jd.collideConnected = true
                    jd.localAnchorA.set(x, y)
                    jd.localAxisA.set(ax, ay)
                    val joint = physicsWorld?.world?.createJoint(jd)
                    if (joint != null && name.isNotEmpty()) physicsWorld?.addJoint(name, joint)
                }
            }
            "create_joint_weld" -> {
                val name = (block.args.getOrNull(1) as? String) ?: ""
                val sprite2Name = (block.args.getOrNull(2) as? String) ?: ""
                val x = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val other = project.sprites.find { it.name == sprite2Name }
                val bA = physicsWorld?.getBody(sprite)
                val bB = if (other != null) physicsWorld?.ensureBody(other) else null
                if (bA != null && bB != null) {
                    val jd = com.badlogic.gdx.physics.box2d.joints.WeldJointDef()
                    jd.bodyA = bA
                    jd.bodyB = bB
                    jd.collideConnected = true
                    jd.localAnchorA.set(x, y)
                    val joint = physicsWorld?.world?.createJoint(jd)
                    if (joint != null && name.isNotEmpty()) physicsWorld?.addJoint(name, joint)
                }
            }
            "destroy_joint" -> {
                val name = (block.args.getOrNull(1) as? String) ?: ""
                if (name.isNotEmpty()) physicsWorld?.destroyJoint(name)
            }

            // ── Gear joint (Box2D) ──
            "create_joint_gear" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val jointAName = block.args.getOrNull(2) as? String ?: ""
                val jointBName = block.args.getOrNull(3) as? String ?: ""
                val ratio = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 1f
                if (name.isNotEmpty() && jointAName.isNotEmpty() && jointBName.isNotEmpty()) {
                    val jointA = physicsWorld?.getJoint(jointAName)
                    val jointB = physicsWorld?.getJoint(jointBName)
                    if (jointA != null && jointB != null) {
                        val jd = com.badlogic.gdx.physics.box2d.joints.GearJointDef()
                        jd.joint1 = jointA
                        jd.joint2 = jointB
                        jd.ratio = ratio
                        val joint = physicsWorld?.world?.createJoint(jd)
                        if (joint != null) physicsWorld?.addJoint(name, joint)
                    }
                }
            }
            // ── Pulley joint (Box2D) ──
            "create_joint_pulley" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val spriteAName = block.args.getOrNull(2) as? String ?: ""
                val spriteBName = block.args.getOrNull(3) as? String ?: ""
                val gaAX = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val gaAY = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val gaBX = (block.args.getOrNull(6) as? Number)?.toFloat() ?: 0f
                val gaBY = (block.args.getOrNull(7) as? Number)?.toFloat() ?: 0f
                val ratio = (block.args.getOrNull(8) as? Number)?.toFloat() ?: 1f
                val spriteA = project.sprites.find { it.name == spriteAName }
                val spriteB = project.sprites.find { it.name == spriteBName }
                val bA = if (spriteA != null) physicsWorld?.ensureBody(spriteA) else physicsWorld?.getBody(sprite)
                val bB = if (spriteB != null) physicsWorld?.ensureBody(spriteB) else bA
                if (bA != null && bB != null && bA != bB) {
                    val jd = com.badlogic.gdx.physics.box2d.joints.PulleyJointDef()
                    jd.bodyA = bA
                    jd.bodyB = bB
                    jd.collideConnected = true
                    jd.groundAnchorA.set(gaAX, gaAY)
                    jd.groundAnchorB.set(gaBX, gaBY)
                    jd.lengthA = spriteA?.let { kotlin.math.sqrt((it.x - gaAX)*(it.x - gaAX) + (it.y - gaAY)*(it.y - gaAY)) } ?: 1f
                    jd.lengthB = spriteB?.let { kotlin.math.sqrt((it.x - gaBX)*(it.x - gaBX) + (it.y - gaBY)*(it.y - gaBY)) } ?: 1f
                    jd.ratio = ratio
                    val joint = physicsWorld?.world?.createJoint(jd)
                    if (joint != null && name.isNotEmpty()) physicsWorld?.addJoint(name, joint)
                }
            }
            // ── Point joint (Box2D: approximated as WeldJoint at anchor) ──
            "create_joint_point" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val objAName = block.args.getOrNull(2) as? String ?: ""
                val objBName = block.args.getOrNull(3) as? String ?: ""
                val paX = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val paY = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val spriteA = if (objAName.isNotEmpty()) project.sprites.find { it.name == objAName } else null
                val spriteB = if (objBName.isNotEmpty()) project.sprites.find { it.name == objBName } else null
                val bA = if (spriteA != null) physicsWorld?.ensureBody(spriteA) else physicsWorld?.getBody(sprite)
                val bB = if (spriteB != null) physicsWorld?.ensureBody(spriteB) else null
                if (bA != null && bB != null) {
                    val jd = com.badlogic.gdx.physics.box2d.joints.WeldJointDef()
                    jd.bodyA = bA
                    jd.bodyB = bB
                    jd.collideConnected = true
                    jd.localAnchorA.set(paX, paY)
                    val joint = physicsWorld?.world?.createJoint(jd)
                    if (joint != null && name.isNotEmpty()) physicsWorld?.addJoint(name, joint)
                }
            }
            // ── Hinge and motor (3D stubs) ──
            "add_hinge" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val objA = block.args.getOrNull(2) as? String ?: ""
                val objB = block.args.getOrNull(3) as? String ?: ""
                if (name.isNotEmpty()) variables["__hinge_$name"] = "$objA|$objB"
            }
            "set_hinge_motor" -> {
                val id = block.args.getOrNull(1) as? String ?: ""
                val target = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val maxForce = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                if (id.isNotEmpty()) variables["__hinge_motor_$id"] = "$target|$maxForce"
            }
            // ── Hitbox: set polygon fixture matching look/sprite dimensions ──
            "set_hitbox" -> {
                val lookName = block.args.getOrNull(1) as? String ?: ""
                if (lookName.isNotEmpty()) {
                    // Try to find a look by name and use its texture dimensions
                    val look = sprite.looks.find { it.name == lookName }
                    val tex = look?.texture
                    val w = if (tex != null) tex.width.toFloat()
                        else sprite.width.takeIf { it > 0f } ?: sprite.size
                    val h = if (tex != null) tex.height.toFloat()
                        else sprite.height.takeIf { it > 0f } ?: sprite.size
                    physicsWorld?.ensureBody(sprite)
                    physicsWorld?.setHitbox(sprite, w, h)
                }
            }
            // ── 3D object stubs (no 3D renderer on desktop) ──
            "create_3d_object" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val file = block.args.getOrNull(2) as? String ?: ""
                if (name.isNotEmpty()) variables["__3dobj_$name"] = file
            }
            "remove_3d_object" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                variables.remove("__3dobj_$name")
            }
            "set_3d_position" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val z = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) variables["__3dpos_$name"] = "$x,$y,$z"
            }
            "set_3d_rotation" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val rx = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val ry = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val rz = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) variables["__3drot_$name"] = "$rx,$ry,$rz"
            }
            "set_3d_scale" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val sx = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val sy = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                val sz = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 1f
                if (name.isNotEmpty()) variables["__3dscale_$name"] = "$sx,$sy,$sz"
            }
            "set_3d_velocity" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val vx = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val vy = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val vz = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) variables["__3dvel_$name"] = "$vx,$vy,$vz"
            }
            "set_3d_friction" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val friction = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) variables["__3dfric_$name"] = friction
            }
            "set_3d_gravity" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val gx = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val gy = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val gz = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) variables["__3dgrav_$name"] = "$gx,$gy,$gz"
            }
            "apply_3d_force" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val fx = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val fy = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val fz = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) variables["__3dforce_$name"] = "$fx,$fy,$fz"
            }

            // ── Fast2D bricks (best-effort на Desktop: 2D-спрайты + Box2D) ──
            "fast2d_create" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty() && project.sprites.none { it.name == name }) {
                    val ns = DesktopSprite(name = name)
                    ns.cloneIndex = cloneCounter.incrementAndGet()
                    project.sprites.add(ns)
                }
            }
                "fast2d_delete" -> {
                    val name = block.args.getOrNull(1) as? String ?: ""
                    val idx = project.sprites.indexOfFirst { it.name == name }
                    if (idx >= 0) {
                        val removed = project.sprites[idx]
                        project.sprites.removeAt(idx)
                        // Item 7: drop the physics body so it is not leaked in the world.
                        physicsWorld?.removeBody(removed)
                        // Item 2: indices of later sprites shift down — keep scripts aligned.
                        remapSpriteIndicesAfterRemoval(idx)
                    }
                }
            "fast2d_make_physics" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val rot = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val size = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 100f
                val tex = block.args.getOrNull(6) as? String ?: ""
                val sp = project.sprites.find { it.name == name }
                if (sp != null) {
                    sp.x = x; sp.y = y; sp.direction = rot; sp.size = size
                    physicsWorld?.ensureBody(sp)
                }
            }
            "fast2d_set_position" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                project.sprites.find { it.name == name }?.let { it.x = x; it.y = y }
            }
            "fast2d_set_rotation" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val rot = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                project.sprites.find { it.name == name }?.direction = rot
            }
            "fast2d_set_scale" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val sx = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val sy = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                project.sprites.find { it.name == name }?.let { it.scaleX = sx; it.scaleY = sy }
            }
            "fast2d_set_color" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val rot = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val size = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 100f
                project.sprites.find { it.name == name }?.let { it.x = x; it.y = y; it.direction = rot; it.size = size }
            }
            "fast2d_set_texture" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val tex = block.args.getOrNull(2) as? String ?: ""
                // Меняем look по имени файла (best-effort)
                project.sprites.find { it.name == name }?.let { sp ->
                    if (tex.isNotEmpty()) {
                        val idx = sp.looks.indexOfFirst { it.name == tex }
                        if (idx >= 0) { sp.currentLookIndex = idx; sp.resetSprite() }
                    }
                }
            }
            "fast2d_set_velocity" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val sp = project.sprites.find { it.name == name }
                val b = if (sp != null) physicsWorld?.getBody(sp) else null
                if (b != null) b.linearVelocity = com.badlogic.gdx.math.Vector2(x, y)
            }
            "fast2d_phys_vel" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val sp = project.sprites.find { it.name == name }
                val b = if (sp != null) physicsWorld?.getBody(sp) else null
                if (b != null) b.linearVelocity = com.badlogic.gdx.math.Vector2(x, y)
            }
            "fast2d_angular_vel" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val rot = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val sp = project.sprites.find { it.name == name }
                val b = if (sp != null) physicsWorld?.getBody(sp) else null
                if (b != null) b.angularVelocity = rot
            }
            "fast2d_apply_force" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val fx = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val fy = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                project.sprites.find { it.name == name }?.let { physicsWorld?.applyForce(it, fx, fy) }
            }
            "fast2d_apply_impulse" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val ix = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val iy = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                project.sprites.find { it.name == name }?.let { physicsWorld?.applyImpulse(it, ix, iy) }
            }
            "fast2d_collision_filter" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val cat = (block.args.getOrNull(2) as? Number)?.toInt() ?: 1
                val mask = (block.args.getOrNull(3) as? Number)?.toInt() ?: -1
                // Категория/маска коллизий: best-effort (храним в переменной)
                if (name.isNotEmpty()) variables["__collision_${name}"] = "$cat:$mask"
            }
            "fast2d_set_zindex" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val z = (block.args.getOrNull(2) as? Number)?.toInt() ?: 0
                project.sprites.find { it.name == name }?.zIndex = z
            }
        }
        frame.ip++
    }

    // ──────────────────────────── CAMERA (3D → 2D emulation) ────────────────────────────

    /**
     * Camera 3D bricks mapped onto the orthographic view (pan / zoom / rotate).
     * Z / pitch / roll are accepted but have no visual effect in the 2D renderer.
     */
    private fun executeCamera(block: Block, sprite: DesktopSprite, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "set_camera_position" -> {
                val x = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                cameraState.x = x
                cameraState.y = y
            }
            "set_camera_position2" -> {
                val x = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                cameraState.x = x
                cameraState.y = y
            }
            "fast2d_set_camera" -> {
                val x = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val zoom = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                cameraState.x = x
                cameraState.y = y
                cameraState.zoom = zoom
            }
            "set_camera_rotation" -> {
                val yaw = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                cameraState.rotation = yaw
            }
            "set_camera_rotation2" -> {
                val deg = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                cameraState.rotation = deg
            }
            "set_camera_zoom" -> {
                val zoom = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
                cameraState.zoom = zoom
            }
            "rotate_camera_by" -> {
                val yaw = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                cameraState.rotation += yaw
            }
            "pin_to_camera" -> {
                // Keep sprite fixed on screen: store its screen offset relative to the camera
                // (so pan + zoom are compensated each frame in the renderer).
                val sx = (sprite.x - cameraState.x) * cameraState.zoom
                val sy = (sprite.y + cameraState.y) * cameraState.zoom
                cameraState.cameraPinned[sprite.name] = sx to sy
            }
            "unpin_from_camera" -> {
                cameraState.cameraPinned.remove(sprite.name)
            }
            "attach_to_camera" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val target = project.sprites.find { it.name == name } ?: sprite
                val sx = (target.x - cameraState.x) * cameraState.zoom
                val sy = (target.y + cameraState.y) * cameraState.zoom
                cameraState.cameraPinned[target.name] = sx to sy
            }
            "attach_to_camera_with_offset" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val offX = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val offY = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val target = project.sprites.find { it.name == name } ?: sprite
                val sx = ((target.x + offX) - cameraState.x) * cameraState.zoom
                val sy = ((target.y + offY) + cameraState.y) * cameraState.zoom
                cameraState.cameraPinned[target.name] = sx to sy
            }
            "detach_from_camera" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) cameraState.cameraPinned.remove(name)
                else cameraState.cameraPinned.remove(sprite.name)
            }
            "set_view_position" -> {
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                cameraState.x = x
                cameraState.y = y
            }
            "set_free_camera" -> {
                cameraState.followTargetName = null
                cameraState.cameraPinned.clear()
            }
            "set_third_person_camera" -> {
                val targetName = block.args.getOrNull(1) as? String ?: ""
                val distance = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val height = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val pitch = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                cameraState.followTargetName = targetName.ifEmpty { sprite.name }
                cameraState.followDistance = distance
                cameraState.followHeight = height
                cameraState.followPitch = pitch
                cameraState.followOffsetX = distance
                cameraState.followOffsetY = height
            }
            "camera_look_at" -> {
                val x = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                cameraState.followTargetName = null
                cameraState.x = x
                cameraState.y = y
                cameraState.rotation = 0f
            }
            "camera_settings" -> {
                cameraState.fieldOfView = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                cameraState.shakeIntensity = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                cameraState.shakeDuration = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
            }
            "camera_touch_control" -> {
                cameraState.touchControlEnabled = ((block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f) != 0f
            }
            "set_camera_range" -> {
                cameraState.rangeNear = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                cameraState.rangeFar = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
            }
            // SetBufferCamera / SetBufferCamera3D: off-screen buffers have no 2D equivalent.
            "set_buffer_camera", "set_buffer_camera_3d" -> {
                // No-op: buffer cameras are not rendered in the desktop player.
            }

            // ── Camera HW stubs (no-op on desktop — no camera hardware) ──
            "camera_preview" -> {
                // CameraBrick: enable/disable camera preview — no-op on desktop
            }
            "camera_choose" -> {
                // ChooseCameraBrick: front/back camera — no-op on desktop
            }
            "camera_flash" -> {
                // FlashBrick: flash on/off — no-op on desktop
            }
            "camera_photo" -> {
                // PhotoBrick: take picture — no-op on desktop
            }
            "camera_tracking" -> {
                // CameraTrackingBrick: 3D tracking — no-op on desktop
            }
            "camera_focus" -> {
                // SetCameraFocusPointBrick: 3D focus point — no-op on desktop
            }

            // ── New Camera stubs (no 3D scene on desktop) ──
            "object_look_at" -> {
                // ObjectLookAtBrick: makes 3D object look at a point — no-op
            }
            "visual_placement" -> {
                // VisualPlacementBrick: placement mode — no-op
            }
            "keyframe_animation" -> {
                // KeyframeAnimationBrick: animate 3D object along keyframes — no-op
            }
            "create_gl_view" -> {
                // CreateGLViewBrick: separate GLSurfaceView — no-op
            }
            "attach_so" -> {
                // AttachSOB: attach sub-object — no-op
            }
            "load_native_module" -> {
                // LoadNativeModuleBrick: load native library — no-op (security)
            }
        }
        frame.ip++
    }

    // ──────────────────────────── SOUND ────────────────────────────

    private fun resolveSoundPath(name: String): String {
        if (name.isEmpty()) return name
        val base = DesktopProjectManager.getInstance().getCurrentProject()?.soundsDir
        return if (base != null) File(base, name).absolutePath else name
    }

    private fun executeSound(block: Block, sprite: DesktopSprite, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "play_sound" -> {
                val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                if (path.isNotEmpty()) {
                    AudioServiceHolder.audioService?.playSoundFile(path, sprite.name)
                }
            }
            "play_sound_wait" -> {
                val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                if (path.isNotEmpty()) {
                    AudioServiceHolder.audioService?.playSoundFile(path, sprite.name)
                    // простейшая реализация: ждём 1 секунду
                    frame.waitTimer = 1f
                }
            }
            "stop_sound" -> {
                val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                AudioServiceHolder.audioService?.stopSoundInSprite(path, sprite.name)
            }
            "stop_all_sounds" -> {
                AudioServiceHolder.audioService?.stopAllSounds()
            }
            "sound_file" -> { /* выбор звукового файла: no-op в рантайме */ }
            "sound_files" -> { /* выбор списка звуков: no-op в рантайме */ }
            "set_volume" -> {
                val vol = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
                AudioServiceHolder.audioService?.setVolume(vol)
                MidiServiceHolder.midiService?.setVolume(vol)
            }
            "change_volume" -> {
                val delta = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val current = AudioServiceHolder.audioService?.getVolume() ?: 1f
                val newVol = (current + delta).coerceIn(0f, 1f)
                AudioServiceHolder.audioService?.setVolume(newVol)
                MidiServiceHolder.midiService?.setVolume(newVol)
            }
            "play_sound_3d" -> {
                val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                val vol = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val pitch = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 1f
                if (path.isNotEmpty()) {
                    AudioServiceHolder.audioService?.setVolume(vol)
                    AudioServiceHolder.audioService?.setPitch(pitch)
                    AudioServiceHolder.audioService?.playSoundFile(path, sprite.name)
                }
            }
            "set_sound_inst_vol" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val vol = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                if (name.isNotEmpty()) {
                    AudioServiceHolder.audioService?.setVolumeForSound(name, sprite.name, vol)
                }
            }
            "set_sound_inst_pitch" -> {
                val pitch = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                AudioServiceHolder.audioService?.setPitch(pitch)
            }
            "prepare_3d_sound" -> {
                // 3D-подготовка звука не поддерживается на Desktop — no-op
            }
            "set_3d_pos" -> {
                // 3D-позиция звука не поддерживается на Desktop — no-op
            }

            // ── New Sound bricks ──
            "stop_sound_v2" -> {
                val instName = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                if (instName.isNotEmpty()) {
                    AudioServiceHolder.audioService?.stopSoundInSprite(instName, sprite.name)
                }
            }
            "play_sound_at" -> {
                val sound = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                val param = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                if (sound.isNotEmpty()) {
                    AudioServiceHolder.audioService?.playSoundFile(sound, sprite.name)
                }
            }
            "set_global_volume" -> {
                val vol = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
                AudioServiceHolder.audioService?.setVolume(vol.coerceIn(0f, 1f))
                MidiServiceHolder.midiService?.setVolume(vol.coerceIn(0f, 1f))
            }
            "set_pan" -> {
                val pan = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                // Pan not directly supported by basic audio service, store as variable
                variables["__audio_pan"] = pan.coerceIn(-1f, 1f)
            }
            "play_tone" -> {
                val freq = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 440f
                val dur = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                // Play tone via Midi service or fallback
                val midiNote = (69 + 12 * (Math.log((freq / 440.0).toDouble()) / Math.log(2.0))).toInt()
                MidiServiceHolder.midiService?.playNoteForBeats(midiNote.coerceIn(0, 127), dur)
            }
            "prepare_sound" -> {
                val file = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                val cache = block.args.getOrNull(2) as? String ?: ""
                if (file.isNotEmpty() && cache.isNotEmpty()) {
                    variables["__sound_cache_$cache"] = file
                }
            }
            "play_prepared" -> {
                val cache = block.args.getOrNull(1) as? String ?: ""
                if (cache.isNotEmpty()) {
                    val file = variables["__sound_cache_$cache"] as? String ?: ""
                    if (file.isNotEmpty()) {
                        AudioServiceHolder.audioService?.playSoundFile(file, sprite.name)
                    }
                }
            }
            "eq_set_band" -> {
                val band = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val gain = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                // Equalizer not supported on desktop — store as placeholder
                variables["__eq_band_$band"] = gain
            }
            "set_stop_sounds" -> {
                val value = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                if (value != 0f) {
                    AudioServiceHolder.audioService?.stopAllSounds()
                }
            }
            "audio_fade_in" -> {
                val dur = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 2f
                val currentVol = AudioServiceHolder.audioService?.getVolume() ?: 0f
                frame.waitTimer = maxOf(frame.waitTimer, dur)
                // Simple fade: set volume on each frame via update loop
                variables["__fade_in_dur"] = dur
                variables["__fade_in_elapsed"] = 0f
                variables["__fade_in_vol"] = currentVol
            }
            "audio_fade_out" -> {
                val dur = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 2f
                val currentVol = AudioServiceHolder.audioService?.getVolume() ?: 1f
                frame.waitTimer = maxOf(frame.waitTimer, dur)
                variables["__fade_out_dur"] = dur
                variables["__fade_out_elapsed"] = 0f
                variables["__fade_out_vol"] = currentVol
            }
        }
        frame.ip++
    }

    // ──────────────────────────── MUSIC ────────────────────────────

    private fun executeMusic(block: Block, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "play_note" -> {
                val midi = (block.args.getOrNull(1) as? Number)?.toInt() ?: 60
                val beats = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                MidiServiceHolder.midiService?.playNoteForBeats(midi, beats)
            }
            "play_drum" -> {
                val drumProg = (block.args.getOrNull(1) as? Number)?.toInt() ?: 36
                val beats = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 1f
                val drum = Drum.getDrumFromProgram(drumProg)
                MidiServiceHolder.midiService?.playDrumForBeats(drum, beats, "player")
            }
            "set_instrument" -> {
                val instProg = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val inst = MusicalInstrument.getInstrumentFromProgram(instProg)
                MidiServiceHolder.midiService?.setInstrument(inst)
            }
            "set_tempo" -> {
                val tempo = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 60f
                // сохраняем темп
                variables["__tempo"] = tempo
            }
            "change_tempo" -> {
                val delta = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val current = (variables["__tempo"] as? Number)?.toFloat() ?: 60f
                variables["__tempo"] = current + delta
            }
            "pause_beats" -> {
                val beats = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
                val tempo = (variables["__tempo"] as? Number)?.toFloat() ?: 60f
                val secs = beats * 60f / maxOf(tempo, 1f)
                frame.waitTimer = secs
            }
        }
        frame.ip++
    }

    // ──────────────────────────── PEN ────────────────────────────

    private fun executePen(block: Block, sprite: DesktopSprite, frame: Frame) {
        val VIRTUAL_WIDTH = (project.stageWidth ?: 480).toFloat()
        val VIRTUAL_HEIGHT = (project.stageHeight ?: 720).toFloat()
        fun toScreenX(x: Float) = VIRTUAL_WIDTH / 2f + x
        fun toScreenY(y: Float) = VIRTUAL_HEIGHT / 2f - y

        when (block.args.getOrNull(0) as? String) {
            "pen_down" -> sprite.penDown = true
            "pen_up" -> sprite.penDown = false
            "clear_canvas" -> {
                project.sprites.forEach { it.penDrawCommands.clear() }
            }
            "set_pen_size" -> sprite.penSize = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
            "set_pen_color" -> {
                sprite.penColorRed = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                sprite.penColorGreen = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                sprite.penColorBlue = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
            }
            "stamp" -> {
                // stamp the current look at current position
                sprite.penDrawCommands.add(PenDrawCommand.StampSprite(
                    sprite.currentLook()?.texture,
                    toScreenX(sprite.x), toScreenY(sprite.y),
                    sprite.width.takeIf { it > 0f } ?: sprite.lookWidth,
                    sprite.height.takeIf { it > 0f } ?: sprite.lookHeight
                ))
            }
            "clear_background" -> {
                sprite.penDrawCommands.clear()
            }
            // ── Shape drawing bricks ──
            "draw_line" -> {
                val x1 = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val y1 = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val x2 = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val y2 = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                sprite.penDrawCommands.add(PenDrawCommand.DrawLine(
                    toScreenX(x1), toScreenY(y1),
                    toScreenX(x2), toScreenY(y2),
                    sprite.penColorRed, sprite.penColorGreen, sprite.penColorBlue,
                    sprite.penSize
                ))
            }
            "draw_circle" -> {
                val cx = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val cy = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val r = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 10f
                sprite.penDrawCommands.add(PenDrawCommand.DrawCircle(
                    toScreenX(cx), toScreenY(cy), r,
                    sprite.penColorRed, sprite.penColorGreen, sprite.penColorBlue,
                    sprite.penSize, fill = false
                ))
            }
            "draw_rect" -> {
                val rx = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val ry = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val rw = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 50f
                val rh = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 50f
                sprite.penDrawCommands.add(PenDrawCommand.DrawRect(
                    toScreenX(rx), toScreenY(ry), rw, rh,
                    sprite.penColorRed, sprite.penColorGreen, sprite.penColorBlue,
                    sprite.penSize, fill = false
                ))
            }
            "draw_text" -> {
                val tx = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val ty = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val txt = block.args.getOrNull(3) as? String ?: ""
                sprite.penDrawCommands.add(PenDrawCommand.DrawText(
                    toScreenX(tx), toScreenY(ty), txt,
                    sprite.penColorRed, sprite.penColorGreen, sprite.penColorBlue,
                    sprite.penSize
                ))
            }
            "fill_circle" -> {
                val cx = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val cy = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val r = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 10f
                sprite.penDrawCommands.add(PenDrawCommand.DrawCircle(
                    toScreenX(cx), toScreenY(cy), r,
                    sprite.penColorRed, sprite.penColorGreen, sprite.penColorBlue,
                    sprite.penSize, fill = true
                ))
            }
            "fill_rect" -> {
                val rx = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val ry = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val rw = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 50f
                val rh = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 50f
                sprite.penDrawCommands.add(PenDrawCommand.DrawRect(
                    toScreenX(rx), toScreenY(ry), rw, rh,
                    sprite.penColorRed, sprite.penColorGreen, sprite.penColorBlue,
                    sprite.penSize, fill = true
                ))
            }
            "fill_polygon" -> {
                val pointsStr = block.args.getOrNull(1) as? String ?: ""
                val pairs = pointsStr.split(",").mapNotNull { token ->
                    val trimmed = token.trim()
                    if (trimmed.isEmpty()) return@mapNotNull null
                    // format: "(x,y)" or "x,y"
                    val coords = trimmed.replace("(", "").replace(")", "").split(";")
                    if (coords.size >= 2) {
                        val px = coords[0].trim().toFloatOrNull() ?: return@mapNotNull null
                        val py = coords[1].trim().toFloatOrNull() ?: return@mapNotNull null
                        Pair(toScreenX(px), toScreenY(py))
                    } else null
                }
                if (pairs.isNotEmpty()) {
                    sprite.penDrawCommands.add(PenDrawCommand.FillPolygon(
                        pairs,
                        sprite.penColorRed, sprite.penColorGreen, sprite.penColorBlue
                    ))
                }
            }
            "set_corner_radius" -> {
                sprite.penCornerRadius = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
            }
            "set_border_width" -> {
                sprite.penBorderWidth = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
            }
            "set_border_color" -> {
                sprite.penBorderColorRed = sprite.penColorRed
                sprite.penBorderColorGreen = sprite.penColorGreen
                sprite.penBorderColorBlue = sprite.penColorBlue
            }
        }
        frame.ip++
    }

    // ──────────────────────────── VARIABLE ────────────────────────────

    private fun executeVariable(block: Block, sprite: DesktopSprite, frame: Frame, state: ScriptState) {
        when (block.args.getOrNull(0) as? String) {
            "set" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val arg = block.args.getOrNull(2)
                if (name.isNotEmpty()) {
                    val value = when (arg) {
                        is RuntimeFormula -> evaluateBrickFieldFormulaAsObject(sprite, state, arg)
                        is Number -> arg.toDouble()
                        else -> 0.0
                    }
                    variables[name] = value
                }
            }
            "change" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val arg = block.args.getOrNull(2)
                if (name.isNotEmpty()) {
                    val delta = when (arg) {
                        is RuntimeFormula -> (evaluateBrickFieldFormula(sprite, state, arg) ?: 0f).toDouble()
                        is Number -> arg.toDouble()
                        else -> 0.0
                    }
                    val old = getVariableDouble(name)
                    variables[name] = old + delta
                }
            }
            "inc_var" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    val current = getVariableFloat(name)
                    variables[name] = current + 1f
                }
            }
            "show_variable" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                val x = evalBlockArgFloat(block, 2, sprite, state) ?: 0f
                val y = evalBlockArgFloat(block, 3, sprite, state) ?: 0f
                if (varName.isNotEmpty()) {
                    val varValue = variables[varName]
                    val text = when (varValue) {
                        is Double -> if (varValue == varValue.toLong().toDouble()) varValue.toLong().toString() else varValue.toString()
                        is Float -> if (varValue == varValue.toLong().toFloat()) varValue.toLong().toString() else varValue.toString()
                        is Number -> varValue.toString()
                        else -> varValue?.toString() ?: ""
                    }
                    val overlayName = "var_${sprite.name}_$varName"
                    textOverlays[overlayName] = TextOverlay(
                        name = overlayName,
                        text = "$varName: $text",
                        x = x, y = y,
                        remainingSeconds = -1f,
                        isVariable = true
                    )
                }
            }
            "hide_variable" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    val overlayName = "var_${sprite.name}_$name"
                    textOverlays.remove(overlayName)
                }
            }
            "create_float" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty() && !variables.containsKey(name)) variables[name] = 0f
            }
            "delete_float" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                variables.remove(name)
            }
            "set_easing" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val end = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) variables[name] = end
            }

            // ── User List operations ──
            "list_add" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val value = block.args.getOrNull(2) as? String ?: ""
                if (name.isNotEmpty()) {
                    userLists.getOrPut(name) { mutableListOf() }.add(value)
                }
            }
            "list_delete" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val index = (block.args.getOrNull(2) as? Number)?.toInt() ?: 1
                if (name.isNotEmpty()) {
                    val list = userLists[name]
                    if (list != null && index > 0 && index <= list.size) {
                        list.removeAt(index - 1)
                    }
                }
            }
            "list_insert" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val index = (block.args.getOrNull(2) as? Number)?.toInt() ?: 1
                val value = block.args.getOrNull(3) as? String ?: ""
                if (name.isNotEmpty()) {
                    val list = userLists.getOrPut(name) { mutableListOf() }
                    val idx = (index - 1).coerceIn(0, list.size)
                    list.add(idx, value)
                }
            }
            "list_replace" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val index = (block.args.getOrNull(2) as? Number)?.toInt() ?: 1
                val value = block.args.getOrNull(3) as? String ?: ""
                if (name.isNotEmpty()) {
                    val list = userLists[name]
                    if (list != null && index > 0 && index <= list.size) {
                        list[index - 1] = value
                    }
                }
            }
            "list_clear" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    userLists[name]?.clear()
                }
            }
            "list_split" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                val separator = block.args.getOrNull(3) as? String ?: ""
                if (name.isNotEmpty()) {
                    val items: MutableList<Any> = if (separator.isNotEmpty()) text.split(separator).toMutableList() else text.chunked(1).toMutableList()
                    userLists[name] = items
                }
            }
            "list_csv" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val csv = block.args.getOrNull(2) as? String ?: ""
                val column = (block.args.getOrNull(3) as? Number)?.toInt() ?: 1
                if (name.isNotEmpty() && csv.isNotEmpty()) {
                    val lines = csv.split("\n")
                    val result = mutableListOf<Any>()
                    for (line in lines) {
                        val parts = line.split(",")
                        val idx = (column - 1).coerceIn(0, parts.lastIndex)
                        result.add(parts[idx].trim())
                    }
                    userLists[name] = result
                }
            }
            "list_regex" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                val regex = block.args.getOrNull(3) as? String ?: ""
                if (name.isNotEmpty() && regex.isNotEmpty()) {
                    try {
                        val pattern = Regex(regex)
                        val matches = pattern.findAll(text).map { it.value as Any }.toMutableList()
                        userLists[name] = matches
                    } catch (_: Exception) {
                        userLists[name] = mutableListOf<Any>()
                    }
                }
            }

            // ── Data Structures (Map, Queue, Stack) ──
            "map_create" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                if (varName.isNotEmpty()) {
                    variables[varName] = mutableMapOf<String, Any>()
                }
            }
            "map_set" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                val key = block.args.getOrNull(2) as? String ?: ""
                val value = block.args.getOrNull(3) as? String ?: ""
                if (varName.isNotEmpty() && key.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    val map = variables[varName] as? MutableMap<String, Any>
                    map?.put(key, value)
                }
            }
            "map_get" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                val key = block.args.getOrNull(2) as? String ?: ""
                if (varName.isNotEmpty() && key.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    val map = variables[varName] as? Map<String, Any>
                    if (map != null) {
                        // Store result back as a special var
                        variables["__map_get_result"] = map[key] ?: ""
                    }
                }
            }
            "map_delete" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                val key = block.args.getOrNull(2) as? String ?: ""
                if (varName.isNotEmpty() && key.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    val map = variables[varName] as? MutableMap<String, Any>
                    map?.remove(key)
                }
            }
            "queue_enqueue" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                val value = block.args.getOrNull(2) as? String ?: ""
                if (varName.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    var queue = variables[varName] as? MutableList<Any>
                    if (queue == null) {
                        queue = mutableListOf()
                        variables[varName] = queue
                    }
                    queue.add(value)
                }
            }
            "queue_dequeue" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                if (varName.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    val queue = variables[varName] as? MutableList<Any>
                    if (queue != null && queue.isNotEmpty()) {
                        val item = queue.removeAt(0)
                        variables["__queue_dequeue_result"] = item
                    }
                }
            }
            "stack_push" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                val value = block.args.getOrNull(2) as? String ?: ""
                if (varName.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    var stack = variables[varName] as? MutableList<Any>
                    if (stack == null) {
                        stack = mutableListOf()
                        variables[varName] = stack
                    }
                    stack.add(value)
                }
            }
            "stack_pop" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                if (varName.isNotEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    val stack = variables[varName] as? MutableList<Any>
                    if (stack != null && stack.isNotEmpty()) {
                        val item = stack.removeAt(stack.lastIndex)
                        variables["__stack_pop_result"] = item
                    }
                }
            }
            // ── Variable management ──
            "create_var" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                val value = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                if (varName.isNotEmpty() && varName !in variables) {
                    variables[varName] = value
                }
            }
            "delete_var" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                if (varName.isNotEmpty()) {
                    variables.remove(varName)
                }
            }
            "delete_all_vars" -> {
                variables.clear()
            }
            // ── Text field bricks ──
            "set_text" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                if (name.isNotEmpty()) {
                    val overlay = textOverlays[name]
                    if (overlay != null) {
                        textOverlays[name] = TextOverlay(name = name, text = text,
                            x = overlay.x, y = overlay.y, size = overlay.size,
                            colorRed = overlay.colorRed, colorGreen = overlay.colorGreen,
                            colorBlue = overlay.colorBlue)
                    }
                }
            }
            "create_text_field" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val text = block.args.getOrNull(2) as? String ?: ""
                val x = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) {
                    textOverlays[name] = TextOverlay(name = name, text = text, x = x, y = y)
                }
            }

            // ── Show text with rotation / font (UserVariableBrickWithVisualPlacement) ──
            "show_text_rotation" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val rot = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                if (name.isNotEmpty()) {
                    val value = getVariable(name).toString()
                    textOverlays[name] = TextOverlay(name = name, text = value, x = x, y = y, rotation = rot)
                }
            }
            "show_var_font" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val size = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 14f
                if (name.isNotEmpty()) {
                    val value = getVariable(name).toString()
                    textOverlays[name] = TextOverlay(name = name, text = value, x = x, y = y, size = size)
                }
            }

            // ── Database tables (local, in-memory) ──
            "db_create" -> {
                val tname = block.args.getOrNull(1) as? String ?: ""
                val sx = (block.args.getOrNull(2) as? Number)?.toInt() ?: 1
                val sy = (block.args.getOrNull(3) as? Number)?.toInt() ?: 1
                if (tname.isNotEmpty()) {
                    val table = MutableList(sy) { MutableList(sx) { 0.0 } }
                    localDb[tname] = table
                }
            }
            "db_delete_all" -> localDb.clear()
            "db_delete_table" -> {
                val tname = block.args.getOrNull(1) as? String ?: ""
                localDb.remove(tname)
            }
            "db_delete_base" -> {
                val id = block.args.getOrNull(1) as? String ?: ""
                val key = block.args.getOrNull(2) as? String ?: ""
                val resp = firebaseRequest("DELETE", firebaseUrl(id, key))
                if (resp == null) baseStore.remove("$id:$key")   // фолбэк при ошибке сети
            }
            "db_insert" -> {
                val tname = block.args.getOrNull(1) as? String ?: ""
                val col = (block.args.getOrNull(2) as? Number)?.toInt() ?: 0
                val row = (block.args.getOrNull(3) as? Number)?.toInt() ?: 0
                val value = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                localDb[tname]?.let { table ->
                    if (row in table.indices && col in table[row].indices) table[row][col] = value.toDouble()
                }
            }
            "db_look_from" -> {
                // Читает значение из первой таблицы в цветовые переменные (best-effort)
                val red = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val green = (block.args.getOrNull(2) as? Number)?.toInt() ?: 0
                val blue = (block.args.getOrNull(3) as? Number)?.toInt() ?: 0
                val alpha = (block.args.getOrNull(4) as? Number)?.toInt() ?: 0
                variables["__table_red"] = red.toDouble()
                variables["__table_green"] = green.toDouble()
                variables["__table_blue"] = blue.toDouble()
                variables["__table_alpha"] = alpha.toDouble()
            }
            "db_look_to" -> {
                val red = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val green = (block.args.getOrNull(2) as? Number)?.toInt() ?: 0
                val blue = (block.args.getOrNull(3) as? Number)?.toInt() ?: 0
                val alpha = (block.args.getOrNull(4) as? Number)?.toInt() ?: 0
                variables["__table_red"] = red.toDouble()
                variables["__table_green"] = green.toDouble()
                variables["__table_blue"] = blue.toDouble()
                variables["__table_alpha"] = alpha.toDouble()
            }
            "db_read_base" -> {
                val id = block.args.getOrNull(1) as? String ?: ""
                val key = block.args.getOrNull(2) as? String ?: ""
                val name = block.args.getOrNull(3) as? String ?: ""
                val resp = firebaseRequest("GET", firebaseUrl(id, key))
                if (name.isNotEmpty()) {
                    variables[name] = if (resp != null) stripJsonString(resp) else (baseStore["$id:$key"] ?: "")
                }
            }
            "db_string_to" -> {
                val tname = block.args.getOrNull(1) as? String ?: ""
                val str = block.args.getOrNull(2) as? String ?: ""
                val col = (block.args.getOrNull(3) as? Number)?.toInt() ?: 0
                val row = (block.args.getOrNull(4) as? Number)?.toInt() ?: 0
                if (tname.isNotEmpty()) {
                    val table = localDb.getOrPut(tname) { mutableListOf(mutableListOf(0.0)) }
                    while (table.size <= row) table.add(mutableListOf(0.0))
                    while (table[row].size <= col) table[row].add(0.0)
                    table[row][col] = str.length.toDouble()
                }
            }
            "db_table_to_float" -> {
                val tname = block.args.getOrNull(1) as? String ?: ""
                val name = block.args.getOrNull(2) as? String ?: ""
                val v = localDb[tname]?.firstOrNull()?.firstOrNull() ?: 0.0
                if (name.isNotEmpty()) variables[name] = v
            }
            "db_write_base" -> {
                val id = block.args.getOrNull(1) as? String ?: ""
                val key = block.args.getOrNull(2) as? String ?: ""
                val value = block.args.getOrNull(3) as? String ?: ""
                val resp = firebaseRequest("PUT", firebaseUrl(id, key), jsonString(value))
                if (resp == null) baseStore["$id:$key"] = value   // фолбэк при ошибке сети
            }

            // ── Firebase Storage (REST API — firebasestorage.googleapis.com) ──
            "firebase_upload" -> {
                val bucket = block.args.getOrNull(1) as? String ?: ""
                val path = block.args.getOrNull(2) as? String ?: ""
                val file = block.args.getOrNull(3) as? String ?: ""
                if (bucket.isNotEmpty() && path.isNotEmpty() && file.isNotEmpty()) {
                    try {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        val localFile = java.io.File(projectDir ?: java.io.File("."), file)
                        if (localFile.exists()) {
                            val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                            val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o?name=$encodedPath"
                            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = "POST"
                            conn.doOutput = true
                            conn.setRequestProperty("Content-Type", "application/octet-stream")
                            conn.connectTimeout = 15000
                            conn.readTimeout = 15000
                            localFile.inputStream().use { input ->
                                conn.outputStream.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            val code = conn.responseCode
                            if (code in 200..299) {
                                // success
                            }
                            conn.disconnect()
                        }
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "firebase_download" -> {
                val bucket = block.args.getOrNull(1) as? String ?: ""
                val path = block.args.getOrNull(2) as? String ?: ""
                val dest = block.args.getOrNull(3) as? String ?: ""
                val varName = block.args.getOrNull(4) as? String ?: ""
                if (bucket.isNotEmpty() && path.isNotEmpty()) {
                    try {
                        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                        val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedPath?alt=media"
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.connectTimeout = 15000
                        conn.readTimeout = 15000
                        if (conn.responseCode in 200..299) {
                            val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                            val destFile = if (dest.isNotEmpty()) java.io.File(dest) else
                                java.io.File(projectDir ?: java.io.File("."), java.io.File(path).name)
                            destFile.parentFile?.mkdirs()
                            destFile.outputStream().use { output ->
                                conn.inputStream.use { input ->
                                    input.copyTo(output)
                                }
                            }
                            if (varName.isNotEmpty()) {
                                variables[varName] = destFile.absolutePath
                            }
                        } else {
                            if (varName.isNotEmpty()) variables[varName] = "ERROR"
                        }
                        conn.disconnect()
                    } catch (_: Exception) {
                        if (varName.isNotEmpty()) variables[varName] = "ERROR"
                    }
                }
            }
            "firebase_delete" -> {
                val bucket = block.args.getOrNull(1) as? String ?: ""
                val path = block.args.getOrNull(2) as? String ?: ""
                if (bucket.isNotEmpty() && path.isNotEmpty()) {
                    try {
                        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                        val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedPath"
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "DELETE"
                        conn.connectTimeout = 15000
                        conn.readTimeout = 15000
                        conn.disconnect()
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "firebase_list" -> {
                val bucket = block.args.getOrNull(1) as? String ?: ""
                val prefix = block.args.getOrNull(2) as? String ?: ""
                val varName = block.args.getOrNull(3) as? String ?: ""
                if (bucket.isNotEmpty() && varName.isNotEmpty()) {
                    try {
                        val encodedPrefix = java.net.URLEncoder.encode(prefix, "UTF-8")
                        val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o?prefix=$encodedPrefix"
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.connectTimeout = 15000
                        conn.readTimeout = 15000
                        if (conn.responseCode in 200..299) {
                            val body = conn.inputStream.bufferedReader().readText()
                            // Manual parse: walk "name":" occurrences
                            val names = mutableListOf<String>()
                            var searchFrom = 0
                            while (true) {
                                val keyIdx = body.indexOf("\"name\":\"", searchFrom)
                                if (keyIdx < 0) break
                                val start = keyIdx + "\"name\":\"".length
                                val end = body.indexOf("\"", start)
                                if (end < 0) break
                                names.add(body.substring(start, end))
                                searchFrom = end + 1
                            }
                            variables[varName] = if (names.isEmpty()) "" else names.joinToString(", ")
                        } else {
                            variables[varName] = "ERROR"
                        }
                        conn.disconnect()
                    } catch (_: Exception) {
                        variables[varName] = "ERROR"
                    }
                }
            }

            // ── Secure read / secure save (stubs — no hardware-backed keystore on desktop) ──
            "secure_read" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                // key is arg[2], unused — desktop stub returns 0
                if (varName.isNotEmpty()) {
                    // No encrypted storage on desktop; return current value or 0
                    variables[varName] = getVariableDouble(varName)
                }
            }
            "secure_save" -> {
                val varName = block.args.getOrNull(1) as? String ?: ""
                val value = block.args.getOrNull(2)
                // key is arg[3], unused
                if (varName.isNotEmpty()) {
                    val v = when (value) {
                        is Number -> value.toDouble()
                        is String -> value.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    variables[varName] = v
                }
            }
        }
        frame.ip++
    }

    // ──────────────────────────── WEB ────────────────────────────

    /** Запускает локальный HTTP-сервер. storeVar (опционально) получает тело запроса. */
    private fun startLocalServer(port: Int, storeVar: String?) {
        try {
            localHttpServer?.stop(0)
            val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress(port), 0)
            server.createContext("/") { exchange ->
                val body = try { exchange.requestBody.bufferedReader().readText() } catch (_: Exception) { "" }
                if (storeVar != null && body.isNotEmpty()) variables[storeVar] = body
                val response = "Catroid Desktop Server"
                exchange.sendResponseHeaders(200, response.length.toLong())
                exchange.responseBody.write(response.toByteArray())
                exchange.responseBody.close()
            }
            server.executor = null
            server.start()
            localHttpServer = server
            variables["__server_port"] = port.toDouble()
        } catch (_: Exception) { /* ignore */ }
    }

    private fun executeWeb(block: Block, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "http_get" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val resultVar = block.args.getOrNull(2) as? String ?: ""
                if (url.isNotEmpty() && resultVar.isNotEmpty()) {
                    NetworkServiceHolder.service?.let { svc ->
                        try {
                            val result = svc.httpGet(url)
                            variables[resultVar] = result
                        } catch (e: Exception) {
                            variables[resultVar] = "Error: ${e.message}"
                        }
                    }
                }
            }
            "http_post" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val body = block.args.getOrNull(2) as? String ?: ""
                val resultVar = block.args.getOrNull(3) as? String ?: ""
                if (url.isNotEmpty() && resultVar.isNotEmpty()) {
                    NetworkServiceHolder.service?.let { svc ->
                        try {
                            val result = svc.httpPost(url, body)
                            variables[resultVar] = result
                        } catch (e: Exception) {
                            variables[resultVar] = "Error: ${e.message}"
                        }
                    }
                }
            }
            "http_put" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val body = block.args.getOrNull(2) as? String ?: ""
                val resultVar = block.args.getOrNull(3) as? String ?: ""
                if (url.isNotEmpty() && resultVar.isNotEmpty()) {
                    NetworkServiceHolder.service?.let { svc ->
                        try {
                            val result = svc.httpPut(url, body)
                            variables[resultVar] = result
                        } catch (e: Exception) {
                            variables[resultVar] = "Error: ${e.message}"
                        }
                    }
                }
            }
            "http_delete" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val resultVar = block.args.getOrNull(2) as? String ?: ""
                if (url.isNotEmpty() && resultVar.isNotEmpty()) {
                    NetworkServiceHolder.service?.let { svc ->
                        try {
                            val result = svc.httpDelete(url)
                            variables[resultVar] = result
                        } catch (e: Exception) {
                            variables[resultVar] = "Error: ${e.message}"
                        }
                    }
                }
            }
            // ── Additional Web bricks ──
            "http_head" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val header = block.args.getOrNull(2) as? String ?: ""
                val resultVar = block.args.getOrNull(3) as? String ?: ""
                if (url.isNotEmpty() && resultVar.isNotEmpty()) {
                    try {
                        val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "HEAD"
                        if (header.isNotEmpty()) conn.setRequestProperty("Custom-Header", header)
                        conn.connect()
                        variables[resultVar] = conn.responseCode.toString()
                        conn.disconnect()
                    } catch (e: Exception) { variables[resultVar] = "Error: ${e.message}" }
                }
            }
            "http_options" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val header = block.args.getOrNull(2) as? String ?: ""
                val resultVar = block.args.getOrNull(3) as? String ?: ""
                if (url.isNotEmpty() && resultVar.isNotEmpty()) {
                    try {
                        val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "OPTIONS"
                        if (header.isNotEmpty()) conn.setRequestProperty("Custom-Header", header)
                        conn.connect()
                        val allow = conn.getHeaderField("Allow") ?: ""
                        variables[resultVar] = "$allow (${conn.responseCode})"
                        conn.disconnect()
                    } catch (e: Exception) { variables[resultVar] = "Error: ${e.message}" }
                }
            }
            "http_patch" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val header = block.args.getOrNull(2) as? String ?: ""
                val body = block.args.getOrNull(3) as? String ?: ""
                val resultVar = block.args.getOrNull(4) as? String ?: ""
                if (url.isNotEmpty() && resultVar.isNotEmpty()) {
                    try {
                        val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "PATCH"
                        conn.setRequestProperty("Content-Type", "application/json")
                        if (header.isNotEmpty()) conn.setRequestProperty("Custom-Header", header)
                        conn.doOutput = true
                        if (body.isNotEmpty()) conn.outputStream.write(body.toByteArray())
                        val response = conn.inputStream.bufferedReader().readText()
                        variables[resultVar] = response
                        conn.disconnect()
                    } catch (e: Exception) { variables[resultVar] = "Error: ${e.message}" }
                }
            }
            "ws_connect" -> {
                val wsUrl = block.args.getOrNull(1) as? String ?: ""
                if (wsUrl.isNotEmpty()) {
                    try {
                        val httpUrl = wsUrl.replace("ws://", "http://").replace("wss://", "https://")
                        variables["__ws_connected"] = "1"
                        variables["__ws_url"] = wsUrl
                    } catch (_: Exception) { variables["__ws_connected"] = "0" }
                }
            }
            "ws_send" -> {
                val msg = block.args.getOrNull(1) as? String ?: ""
                if (msg.isNotEmpty() && variables["__ws_connected"] == "1") {
                    variables["__ws_last_sent"] = msg
                }
            }
            "ws_close" -> {
                variables["__ws_connected"] = "0"
            }
            "create_web_url" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val url = block.args.getOrNull(2) as? String ?: ""
                if (name.isNotEmpty()) variables[name] = url
            }
            "create_web_file" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val file = block.args.getOrNull(2) as? String ?: ""
                if (name.isNotEmpty()) variables[name] = file
            }
            "download_to_path" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val path = block.args.getOrNull(2) as? String ?: ""
                if (url.isNotEmpty() && path.isNotEmpty()) {
                    try {
                        val dest = java.io.File(path)
                        dest.parentFile?.mkdirs()
                        val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 15_000
                        conn.readTimeout = 15_000
                        conn.connect()
                        dest.outputStream().use { out -> conn.inputStream.copyTo(out) }
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "set_dns" -> {
                // DNS-сервер не настраивается на Desktop — no-op
            }
            "stop_server" -> {
                try { localHttpServer?.stop(0) } catch (_: Exception) { /* ignore */ }
                localHttpServer = null
            }
            "connect_server" -> {
                val port = (block.args.getOrNull(2) as? Number)?.toInt() ?: 8080
                startLocalServer(port, null)
            }
            "listen_server" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val port = (variables["__server_port"] as? Number)?.toInt() ?: 8080
                startLocalServer(port, if (name.isNotEmpty()) name else null)
            }
            "ws_receive" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) variables[name] = variables["__ws_last_received"] ?: ""
            }
            "download_file" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val fileName = block.args.getOrNull(2) as? String ?: ""
                if (url.isNotEmpty() && fileName.isNotEmpty()) {
                    try {
                        val dest = java.io.File(fileName)
                        dest.parentFile?.mkdirs()
                        val conn = java.net.URI(url).toURL().openConnection()
                        conn.connect()
                        dest.outputStream().use { out -> conn.inputStream.copyTo(out) }
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "upload_file" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val file = block.args.getOrNull(2) as? String ?: ""
                val mime = block.args.getOrNull(3) as? String ?: "application/octet-stream"
                if (url.isNotEmpty() && file.isNotEmpty()) {
                    try {
                        val f = java.io.File(file)
                        val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", mime)
                        conn.doOutput = true
                        f.inputStream().use { fis -> conn.outputStream.use { it.write(fis.readBytes()) } }
                        conn.responseCode // trigger send
                        conn.disconnect()
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "ping" -> {
                val host = block.args.getOrNull(1) as? String ?: ""
                val varName = block.args.getOrNull(2) as? String ?: ""
                if (host.isNotEmpty() && varName.isNotEmpty()) {
                    try {
                        val start = System.nanoTime()
                        val addr = java.net.InetAddress.getByName(host)
                        val reachable = addr.isReachable(3000)
                        val rtt = (System.nanoTime() - start) / 1_000_000.0
                        variables[varName] = if (reachable) "OK (${rtt.toInt()}ms)" else "TIMEOUT"
                    } catch (e: Exception) { variables[varName] = "Error: ${e.message}" }
                }
            }
            // ── Web extras ──
            "http_set" -> {
                val url = block.args.getOrNull(1) as? String ?: ""
                val body = block.args.getOrNull(2) as? String ?: ""
                val resultVar = block.args.getOrNull(3) as? String ?: ""
                if (url.isNotEmpty() && resultVar.isNotEmpty()) {
                    try {
                        val conn = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        conn.doOutput = true
                        if (body.isNotEmpty()) conn.outputStream.write(body.toByteArray())
                        val response = conn.inputStream.bufferedReader().readText()
                        variables[resultVar] = response
                        conn.disconnect()
                    } catch (e: Exception) { variables[resultVar] = "Error: ${e.message}" }
                }
            }
            "http_eval" -> {
                val script = block.args.getOrNull(1) as? String ?: ""
                val resultVar = block.args.getOrNull(2) as? String ?: ""
                if (script.isNotEmpty() && resultVar.isNotEmpty()) {
                    try {
                        // Eval-style: POST script as text/plain to a server endpoint
                        val conn = java.net.URI("http://localhost:8080/eval").toURL().openConnection() as java.net.HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.setRequestProperty("Content-Type", "text/plain")
                        conn.doOutput = true
                        conn.outputStream.write(script.toByteArray())
                        val response = conn.inputStream.bufferedReader().readText()
                        variables[resultVar] = response
                        conn.disconnect()
                    } catch (e: Exception) { variables[resultVar] = "Error: ${e.message}" }
                }
            }
            "start_server" -> {
                val port = (block.args.getOrNull(1) as? Number)?.toInt() ?: 8080
                startLocalServer(port, null)
            }
            "cancel_download" -> { /* активные загрузки не трекаются на Desktop — no-op */ }
            "send_server" -> {
                val value = block.args.getOrNull(1) as? String ?: ""
                val url = (variables["__server_url"] as? String)
                    ?: (variables["__ws_url"] as? String) ?: ""
                if (url.isNotEmpty()) {
                    NetworkServiceHolder.service?.let { svc ->
                        try { svc.httpPost(url, value) } catch (_: Exception) { /* ignore */ }
                    }
                }
            }
            "file_url" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val url = block.args.getOrNull(2) as? String ?: ""
                if (name.isNotEmpty()) variables[name] = url
            }
            "files_url" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val url = block.args.getOrNull(2) as? String ?: ""
                if (name.isNotEmpty()) variables[name] = url
            }
        }
        frame.ip++
    }

    // ──────────────────────────── SENSING ────────────────────────────

    private fun executeSensing(block: Block, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "lock_mouse" -> {
                try { com.badlogic.gdx.Gdx.input.setCursorCatched(true) } catch (_: Exception) { /* ignore */ }
            }
            "unlock_mouse" -> {
                try { com.badlogic.gdx.Gdx.input.setCursorCatched(false) } catch (_: Exception) { /* ignore */ }
            }
            "screenshot" -> {
                try {
                    val pixmap = com.badlogic.gdx.utils.ScreenUtils.getFrameBufferPixmap(0, 0,
                        com.badlogic.gdx.Gdx.graphics.width, com.badlogic.gdx.Gdx.graphics.height)
                    val fileName = "screenshot_${System.currentTimeMillis()}.png"
                    com.badlogic.gdx.graphics.PixmapIO.writePNG(com.badlogic.gdx.Gdx.files.absolute(fileName), pixmap)
                    pixmap.dispose()
                } catch (_: Exception) { /* ignore */ }
            }
            "tap_at" -> {
                val x = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val y = (block.args.getOrNull(2) as? Number)?.toInt() ?: 0
                input.simulateTap(x.toFloat(), y.toFloat())
            }
            "reset_timer" -> timerSeconds = 0f

            // Device bricks (ported with stubs where needed)
            "vibrate" -> {
                val ms = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0.5f
                // Desktop vibration not supported — stub
            }
            "keep_screen_on" -> {
                // Desktop — no-op (screen always on)
            }
            "keep_screen_off" -> {
                // Desktop — no-op
            }
            "screen_brightness" -> {
                val brightness = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 1f
                // Desktop brightness not adjustable — stub
            }
        }
        frame.ip++
    }

    // ──────────────────────────── DATA (file I/O) ────────────────────────────

    private fun executeData(block: Block, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "write_variable" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    try {
                        val value = getVariable(name)?.toString() ?: ""
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        val file = java.io.File(projectDir ?: java.io.File("."), "${name}.txt")
                        file.parentFile?.mkdirs()
                        file.writeText(value)
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "read_variable" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    try {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        val file = java.io.File(projectDir ?: java.io.File("."), "${name}.txt")
                        if (file.exists()) {
                            val content = file.readText().trim()
                            variables[name] = content.toDoubleOrNull() ?: content
                        }
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "read_list_device" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    try {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        val file = java.io.File(projectDir ?: java.io.File("."), "${name}.list.txt")
                        if (file.exists()) {
                            val lines = file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                            userLists[name] = lines.map { it.toDoubleOrNull() ?: it as Any }.toMutableList()
                        }
                    } catch (_: Exception) { /* ignore */ }
                }
            }
            "write_list_device" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    try {
                        val list = userLists[name] ?: mutableListOf()
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        val file = java.io.File(projectDir ?: java.io.File("."), "${name}.list.txt")
                        file.parentFile?.mkdirs()
                        file.writeText(list.joinToString("\n") { it.toString() })
                    } catch (_: Exception) { /* ignore */ }
                }
            }
        }
        frame.ip++
    }

    // ──────────────────────────── FILE ────────────────────────────

    private fun executeFile(block: Block, frame: Frame) {
        val cmd = block.args.getOrNull(0) as? String ?: ""
        try {
            when (cmd) {
                "move" -> {
                    val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                    val src = java.io.File(path)
                    val destDir = java.io.File(path).parentFile ?: java.io.File(".")
                    if (src.exists()) {
                        val dest = java.io.File(destDir, src.name)
                        java.nio.file.Files.move(src.toPath(), dest.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                    }
                }
                "zip" -> {
                    val zipName = block.args.getOrNull(1) as? String ?: ""
                    val filesStr = block.args.getOrNull(2) as? String ?: ""
                    if (zipName.isNotEmpty()) {
                        val files = filesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipName)).use { zos ->
                            for (filePath in files) {
                                val f = java.io.File(filePath)
                                if (f.exists()) {
                                    val entryName = f.name
                                    zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                                    f.inputStream().use { it.copyTo(zos) }
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                }
                "unzip" -> {
                    val zipPath = block.args.getOrNull(1) as? String ?: ""
                    val zipFile = java.io.File(zipPath)
                    if (zipFile.exists()) {
                        val destDir = zipFile.parentFile ?: java.io.File(".")
                        java.util.zip.ZipInputStream(java.io.FileInputStream(zipFile)).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val outFile = java.io.File(destDir, entry.name).canonicalFile
                                val destCanonical = destDir.canonicalPath
                                if (!outFile.path.startsWith(destCanonical)) {
                                    entry = zis.nextEntry
                                    continue
                                }
                                if (!entry.isDirectory) {
                                    outFile.parentFile?.mkdirs()
                                    java.io.FileOutputStream(outFile).use { zis.copyTo(it) }
                                }
                                entry = zis.nextEntry
                            }
                        }
                    }
                }
                "copy_project_file" -> {
                    val srcName = block.args.getOrNull(1) as? String ?: ""
                    val destName = block.args.getOrNull(2) as? String ?: ""
                    val project = DesktopProjectManager.getInstance().getCurrentProject()
                    val projectDir = project?.projectDir
                    if (projectDir != null) {
                        val src = java.io.File(projectDir, srcName)
                        val dest = java.io.File(projectDir, destName)
                        if (src.exists()) {
                            src.copyTo(dest, overwrite = true)
                        }
                    }
                }
                "export_file" -> {
                    val fileName = block.args.getOrNull(1) as? String ?: ""
                    val project = DesktopProjectManager.getInstance().getCurrentProject()
                    val projectDir = project?.projectDir
                    if (projectDir != null) {
                        val src = java.io.File(projectDir, fileName)
                        if (src.exists()) {
                            // copy to desktop downloads dir
                            val dest = java.io.File(System.getProperty("user.home") ?: ".", "Downloads")
                                .resolve(fileName)
                            dest.parentFile?.mkdirs()
                            src.copyTo(dest, overwrite = true)
                        }
                    }
                }
                "delete_file" -> {
                    val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                    val f = java.io.File(path)
                    if (f.exists()) f.delete()
                }
                "open_file" -> {
                    val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                    val f = java.io.File(path)
                    if (f.exists()) {
                        try {
                            val rt = Runtime.getRuntime()
                            if (System.getProperty("os.name").lowercase().contains("win")) {
                                rt.exec(arrayOf("cmd", "/c", "start", "", f.absolutePath))
                            } else {
                                rt.exec(arrayOf("xdg-open", f.absolutePath))
                            }
                        } catch (_: Exception) { /* ignore */ }
                    }
                }
                "read_from_files" -> {
                    val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                    val varName = block.args.getOrNull(2) as? String ?: ""
                    val f = java.io.File(path)
                    if (f.exists() && varName.isNotEmpty()) {
                        val content = f.readText()
                        variables[varName] = content
                    }
                }
                "write_to_files" -> {
                    val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                    val varName = block.args.getOrNull(2) as? String ?: ""
                    val f = java.io.File(path)
                    if (varName.isNotEmpty()) {
                        val value = getVariable(varName)?.toString() ?: ""
                        f.parentFile?.mkdirs()
                        f.writeText(value)
                    }
                }
                "read_variable_from_file" -> {
                    val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                    val varName = block.args.getOrNull(2) as? String ?: ""
                    val deleteAfter = block.args.getOrNull(3) as? Boolean ?: false
                    val f = java.io.File(path)
                    if (f.exists() && varName.isNotEmpty()) {
                        try {
                            val content = f.readText().trim()
                            val num = content.toDoubleOrNull()
                            variables[varName] = num ?: content
                        } catch (_: Exception) { /* ignore */ }
                        if (deleteAfter) f.delete()
                    }
                }
                "write_variable_to_file" -> {
                    val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                    val varName = block.args.getOrNull(2) as? String ?: ""
                    val f = java.io.File(path)
                    if (varName.isNotEmpty()) {
                        val value = getVariable(varName)?.toString() ?: ""
                        f.parentFile?.mkdirs()
                        f.writeText(value)
                    }
                }
                "save_internal" -> {
                    val projectFileName = block.args.getOrNull(1) as? String ?: ""
                    val fileName = block.args.getOrNull(2) as? String ?: ""
                    val project = DesktopProjectManager.getInstance().getCurrentProject()
                    val dataDir = project?.projectDir?.resolve("internal_data") ?: java.io.File("internal_data")
                    dataDir.mkdirs()
                    val src = project?.projectDir?.resolve(projectFileName)
                    if (src?.exists() == true) {
                        val dest = java.io.File(dataDir, fileName)
                        src.copyTo(dest, overwrite = true)
                    }
                }
                "load_internal" -> {
                    val fileName = block.args.getOrNull(1) as? String ?: ""
                    val project = DesktopProjectManager.getInstance().getCurrentProject()
                    val dataDir = project?.projectDir?.resolve("internal_data") ?: java.io.File("internal_data")
                    val src = java.io.File(dataDir, fileName)
                    if (src.exists()) {
                        val dest = project?.projectDir?.resolve(fileName) ?: java.io.File(fileName)
                        src.copyTo(dest, overwrite = true)
                    }
                }
                "extract_file" -> {
                    val apkPath = block.args.getOrNull(1) as? String ?: ""
                    val innerPath = block.args.getOrNull(2) as? String ?: ""
                    val destPath = block.args.getOrNull(3) as? String ?: ""
                    // extract innerPath from apk (zip) to destPath
                    if (apkPath.isNotEmpty() && innerPath.isNotEmpty() && destPath.isNotEmpty()) {
                        java.util.zip.ZipInputStream(java.io.FileInputStream(apkPath)).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                if (entry.name == innerPath && !entry.isDirectory) {
                                    java.io.File(destPath).parentFile?.mkdirs()
                                    java.io.FileOutputStream(destPath).use { zis.copyTo(it) }
                                    break
                                }
                                entry = zis.nextEntry
                            }
                        }
                    }
                }
                "get_zip_names" -> {
                    val zipPath = block.args.getOrNull(1) as? String ?: ""
                    val varName = block.args.getOrNull(2) as? String ?: ""
                    if (zipPath.isNotEmpty() && varName.isNotEmpty()) {
                        val names = mutableListOf<String>()
                        java.util.zip.ZipInputStream(java.io.FileInputStream(zipPath)).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory) names.add(entry.name)
                                entry = zis.nextEntry
                            }
                        }
                        variables[varName] = names.joinToString(",")
                    }
                }

                // ── Remaining File operations ──
                "create_folder" -> {
                    val folder = block.args.getOrNull(1) as? String ?: ""
                    if (folder.isNotEmpty()) {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        val dir = if (projectDir != null) java.io.File(projectDir, folder) else java.io.File(folder)
                        dir.mkdirs()
                    }
                }
                "delete_folder" -> {
                    val folder = block.args.getOrNull(1) as? String ?: ""
                    if (folder.isNotEmpty()) {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        val dir = if (projectDir != null) java.io.File(projectDir, folder) else java.io.File(folder)
                        if (dir.isDirectory) dir.deleteRecursively()
                    }
                }
                "create_folder_path" -> {
                    val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                    val folder = block.args.getOrNull(2) as? String ?: ""
                    if (path.isNotEmpty() && folder.isNotEmpty()) {
                        java.io.File(path, folder).mkdirs()
                    }
                }
                "delete_folder_path" -> {
                    val path = resolveSoundPath(block.args.getOrNull(1) as? String ?: "")
                    val folder = block.args.getOrNull(2) as? String ?: ""
                    if (path.isNotEmpty() && folder.isNotEmpty()) {
                        val dir = java.io.File(path, folder)
                        if (dir.isDirectory) dir.deleteRecursively()
                    }
                }
                "put_file_into_folder" -> {
                    val name = block.args.getOrNull(1) as? String ?: ""
                    val folder = block.args.getOrNull(2) as? String ?: ""
                    if (name.isNotEmpty() && folder.isNotEmpty()) {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        if (projectDir != null) {
                            val src = java.io.File(projectDir, name)
                            val dest = java.io.File(java.io.File(projectDir, folder), name)
                            if (src.exists()) {
                                dest.parentFile?.mkdirs()
                                src.copyTo(dest, overwrite = true)
                                src.delete()
                            }
                        }
                    }
                }
                "put_file_into_path" -> {
                    val name = block.args.getOrNull(1) as? String ?: ""
                    val path = block.args.getOrNull(2) as? String ?: ""
                    if (name.isNotEmpty() && path.isNotEmpty()) {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        if (projectDir != null) {
                            val src = java.io.File(projectDir, name)
                            val dest = java.io.File(path)
                            if (src.exists()) {
                                dest.parentFile?.mkdirs()
                                src.copyTo(dest, overwrite = true)
                                src.delete()
                            }
                        }
                    }
                }
                "copy_to_folder" -> {
                    val name = block.args.getOrNull(1) as? String ?: ""
                    val folder = block.args.getOrNull(2) as? String ?: ""
                    if (name.isNotEmpty() && folder.isNotEmpty()) {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        if (projectDir != null) {
                            val src = java.io.File(projectDir, name)
                            val dest = java.io.File(java.io.File(projectDir, folder), name)
                            if (src.exists()) {
                                dest.parentFile?.mkdirs()
                                src.copyTo(dest, overwrite = true)
                            }
                        }
                    }
                }
                "copy_to_path" -> {
                    val name = block.args.getOrNull(1) as? String ?: ""
                    val path = block.args.getOrNull(2) as? String ?: ""
                    if (name.isNotEmpty() && path.isNotEmpty()) {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        if (projectDir != null) {
                            val src = java.io.File(projectDir, name)
                            val dest = java.io.File(path)
                            if (src.exists()) {
                                dest.parentFile?.mkdirs()
                                src.copyTo(dest, overwrite = true)
                            }
                        }
                    }
                }
                "move_downloads" -> {
                    val destPath = block.args.getOrNull(1) as? String ?: ""
                    if (destPath.isNotEmpty()) {
                        val dir = java.io.File(destPath)
                        dir.mkdirs()
                    }
                }
                "open_files" -> {
                    val pattern = block.args.getOrNull(1) as? String ?: ""
                    if (pattern.isNotEmpty()) {
                        try {
                            val dir = java.io.File(pattern).parentFile ?: java.io.File(".")
                            if (dir.isDirectory) {
                                java.awt.Desktop.getDesktop().open(dir)
                            }
                        } catch (_: Exception) { /* ignore */ }
                    }
                }
                "has_path" -> {
                    val varName = block.args.getOrNull(1) as? String ?: ""
                    if (varName.isNotEmpty()) {
                        val path = variables[varName]?.toString() ?: ""
                        val exists = java.io.File(path).exists()
                        variables[varName] = if (exists) 1.0 else 0.0
                    }
                }
                "put_float" -> {
                    val name = block.args.getOrNull(1) as? String ?: ""
                    val value = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                    if (name.isNotEmpty()) {
                        val projectDir = DesktopProjectManager.getInstance().getCurrentProject()?.projectDir
                        val f = if (projectDir != null) java.io.File(projectDir, "$name.float.txt") else java.io.File("$name.float.txt")
                        f.parentFile?.mkdirs()
                        f.appendText("${value}\n")
                    }
                }
            }
        } catch (_: Exception) {
            // SILENT FAIL — same as Android: file ops don't crash the script
        }
        frame.ip++
    }

    // ──────────────────────────── VIDEO (best-effort) ────────────────────────────

    /**
     * Видео-блоки. На Desktop нет аппаратного воспроизведения видео в рантайме —
     * состояние проигрывателя хранится, но кадры не отрисовываются (best-effort).
     */
    private fun executeVideo(block: Block, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "create_video" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val file = block.args.getOrNull(2) as? String ?: ""
                val x = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(4) as? Number)?.toFloat() ?: 0f
                val w = (block.args.getOrNull(5) as? Number)?.toFloat() ?: 0f
                val h = (block.args.getOrNull(6) as? Number)?.toFloat() ?: 0f
                val looped = (block.args.getOrNull(7) as? Number)?.toInt() ?: 0
                if (name.isNotEmpty()) {
                    videos[name] = VideoState(fileName = file, x = x, y = y, width = w, height = h, looped = looped == 1)
                }
            }
            "play_video" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                videos[name]?.playing = true
            }
            "pause_video" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                videos[name]?.playing = false
            }
            "seek_video" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val t = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                videos[name]?.position = t
            }
        }
        frame.ip++
    }

    // ──────────────────── Runtime formula evaluation ────────────────────

    /** Вычислить аргумент блока как Float с поддержкой runtime-формул. */
    private fun evalBlockArgFloat(block: Block, argIndex: Int, sprite: DesktopSprite, state: ScriptState): Float? {
        // Аргумент может быть простым числом (Number) или RuntimeFormula
        val arg = block.args.getOrNull(argIndex)
        return when (arg) {
            is Number -> arg.toFloat()
            is RuntimeFormula -> evaluateBrickFieldFormula(sprite, state, arg)
            else -> null
        }
    }

    /** Вычислить runtime-формулу для указанного BrickField. */
    private fun evaluateBrickFieldFormula(sprite: DesktopSprite, state: ScriptState, rf: RuntimeFormula): Float? {
        val spriteIndex = state.spriteIndex
        return evaluateFormulaNode(rf.formulaElement, spriteIndex)?.let { v ->
            when (v) {
                is Double -> v.toFloat()
                is Float -> v
                is String -> v.toFloatOrNull()
                else -> null
            }
        }
    }

    /** Вычислить runtime-формулу как строку. */
    private fun evaluateBrickFieldFormulaString(sprite: DesktopSprite, state: ScriptState, rf: RuntimeFormula): String? {
        val spriteIndex = state.spriteIndex
        return evaluateFormulaNode(rf.formulaElement, spriteIndex)?.toString()
    }

    /** Вычислить runtime-формулу с сохранением оригинального типа (Double или String). */
    private fun evaluateBrickFieldFormulaAsObject(sprite: DesktopSprite, state: ScriptState, rf: RuntimeFormula): Any {
        val spriteIndex = state.spriteIndex
        val result = evaluateFormulaNode(rf.formulaElement, spriteIndex)
        return when (result) {
            is Double -> result
            is String -> result
            is Number -> result.toDouble()
            else -> result ?: 0.0
        }
    }

    /** Получить числовое значение переменной как Double. */
    private fun getVariableDouble(name: String): Double {
        val v = variables[name]
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    // ════════════════════════ ПАРСИНГ code.xml ════════════════════════

    private fun parseProject() {
        val dir = project.projectDir
        if (dir != null && parseXmlScripts(dir)) return
        // Fallback: демо-скрипт с forever
        for (i in project.sprites.indices) {
            scriptStates.add(ScriptState(i, listOf(
                Block(Block.Type.EVENT, listOf("green_flag")),
                Block(Block.Type.LOOKS, listOf("show")),
                Block(Block.Type.CONTROL, listOf("forever"), listOf(
                    Block(Block.Type.MOTION, listOf("move_steps", 1f)),
                    Block(Block.Type.CONTROL, listOf("wait", 0.05f))
                ))
            )))
        }
    }

    /** Отобразить тип скрипта на eventType для движка. */
    private fun mapScriptTypeToEvent(scriptType: String): String? {
        return when (scriptType) {
            "StartScript", "StartedScript" -> null // green flag = обычный скрипт
            "WhenTouchDownScript" -> "touch_down"
            "WhenClonedScript" -> "cloned"
            "WhenConditionScript" -> "condition"
            "WhenFirebaseChangedScript" -> "firebase_changed"
            "WhenBackgroundChangesScript" -> "background_changes"
            "WhenBounceOffScript" -> "bounce_off"
            "BackPressedScript" -> "back_pressed"
            "WhenMouseButtonClickedScript" -> "mouse_clicked"
            "WhenMouseWheelScrolledScript" -> "mouse_wheel"
            "WhenGamepadButtonScript" -> "gamepad_button"
            "BroadcastScript" -> "broadcast_receiver"
            "SceneStartScript" -> "scene_start"
            "ScenePreloadedScript" -> "scene_preloaded"
            "WhenProjectExitsScript" -> "project_exits"
            "WhenAppMinimizedScript" -> "app_minimized"
            "WhenAppRestoredScript" -> "app_restored"
            "UserDefinedScript" -> "user_defined"
            else -> null
        }
    }

    /**
     * Регистрирует определение пользовательской процедуры (UserDefinedScript).
     * Первый брик в brickList — UserDefinedBrick (имя процедуры + параметры),
     * остальные брики — тело процедуры.
     */
    private fun parseUserDefinedScriptDefinition(scriptEl: Element, spriteIndex: Int) {
        val brickListEl = scriptEl.getElementsByTagName("brickList")?.item(0) as? Element ?: return
        val nodes = brickListEl.childNodes
        if (nodes.length == 0) return

        // Первый брик — определение UserDefinedBrick
        var firstDef: Element? = null
        for (i in 0 until nodes.length) {
            val n = nodes.item(i)
            if (n.nodeType == Node.ELEMENT_NODE) { firstDef = n as Element; break }
        }
        firstDef ?: return

        val procId = getTagText(firstDef, "userDefinedBrickID") ?: return
        val paramNames = extractUserDefinedInputNames(firstDef)

        // Тело: все брики после определения
        val (body, _) = parseBrickListRecursive(nodes, 1, spriteIndex)
        procedures[procId] = ProcedureDef(paramNames, body)
    }

    /** Извлекает имена параметров из <userDefinedBrickDataList>/<userDefinedBrickInput>/<input>. */
    private fun extractUserDefinedInputNames(el: Element): List<String> {
        val dataList = el.getElementsByTagName("userDefinedBrickDataList")?.item(0) as? Element ?: return emptyList()
        val inputs = dataList.getElementsByTagName("userDefinedBrickInput")
        val names = mutableListOf<String>()
        for (i in 0 until inputs.length) {
            val input = inputs.item(i) as? Element ?: continue
            val name = getTagText(input, "input")?.takeIf { it.isNotBlank() } ?: continue
            names.add(name)
        }
        return names
    }

    /** Парсит code.xml: каждый <script> создаёт свой ScriptState (не склеиваются!). */
    private fun parseXmlScripts(projectDir: File): Boolean {
        val codeXml = File(projectDir, "code.xml")
        if (!codeXml.exists()) return false
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(codeXml)
            doc.documentElement.normalize()

            val objects = doc.getElementsByTagName("object")
            for (i in 0 until objects.length) {
                val objNode = objects.item(i)
                if (objNode.nodeType != Node.ELEMENT_NODE) continue
                val objEl = objNode as Element

                val scriptsNode = objEl.getElementsByTagName("scriptList")?.item(0) as? Element
                if (scriptsNode == null) continue

                val scriptNodes = scriptsNode.childNodes
                for (s in 0 until scriptNodes.length) {
                    val scriptNode = scriptNodes.item(s)
                    if (scriptNode.nodeType != Node.ELEMENT_NODE) continue
                    val scriptEl = scriptNode as Element

                    // Определяем тип события из типа скрипта
                    val scriptType = scriptEl.getAttribute("type")
                    val eventType = mapScriptTypeToEvent(scriptType)

                    // UserDefinedScript — это определение процедуры, а не исполняемый скрипт.
                    // Регистрируем процедуру и пропускаем создание ScriptState.
                    if (eventType == "user_defined") {
                        parseUserDefinedScriptDefinition(scriptEl, i)
                        continue
                    }

                    val brickListEl = scriptEl
                        .getElementsByTagName("brickList")?.item(0) as? Element
                    if (brickListEl == null) continue

                    // Проверяем, есть ли firstBrick с Внешней ссылкой на формулу
                    // (некоторые события хранят условие/параметр в скрипте, а не в блоках)
                    var conditionElement: Element? = null
                    var eventParam2: String? = null

                    if (eventType == "condition") {
                        // WhenConditionBrick – достаём IF_CONDITION из первого брика в списке
                        val firstBrick = brickListEl.firstChild
                        if (firstBrick != null && firstBrick.nodeType == Node.ELEMENT_NODE) {
                            val fb = firstBrick as Element
                            if (fb.getAttribute("type") == "WhenConditionBrick") {
                                conditionElement = getFormulaElement(fb, "IF_CONDITION")
                            }
                        }
                    } else if (eventType == "firebase_changed") {
                        // WhenFirebaseChangedBrick — buckets + path как для WhenCondition
                        val firstBrick = brickListEl.firstChild
                        if (firstBrick != null && firstBrick.nodeType == Node.ELEMENT_NODE) {
                            val fb = firstBrick as Element
                            if (fb.getAttribute("type") == "WhenFirebaseChangedBrick") {
                                eventParam2 = extractFormulaString(fb, "FIREBASE_TRIGGER_BUCKET") ?: ""
                            }
                        }
                    } else if (eventType == "bounce_off") {
                        // WhenBounceOffScript – имя спрайта в <spriteToBounceOffName>
                        eventParam2 = extractTextContent(scriptEl, "spriteToBounceOffName")
                    } else if (eventType == "gamepad_button") {
                        eventParam2 = extractTextContent(scriptEl, "action")
                    } else if (eventType == "mouse_clicked") {
                        eventParam2 = extractTextContent(scriptEl, "buttonCode")
                    } else if (eventType == "broadcast_receiver") {
                        eventParam2 = extractMessageText(scriptEl, "receivedMessage")
                    } else if (eventType == "scene_start") {
                        eventParam2 = extractTextContent(scriptEl, "sceneToStart")
                    }

                    val (bricks, runtimeFormulas) = try {
                        parseBrickListRecursive(brickListEl.childNodes, 0, i)
                    } catch (e: Exception) {
                        Gdx.app.error("ScriptEngine", "Skipping broken script type=$scriptType spriteIndex=$i", e)
                        continue
                    }

                    // Для обычных скриптов (green flag) не добавляем show — они уже есть
                    val sprite = project.sprites.getOrNull(i)
                    val allBricks = if (sprite?.looks?.isNotEmpty() == true && eventType == null) {
                        mutableListOf(Block(Block.Type.LOOKS, listOf("show"))).apply { addAll(bricks) }
                    } else bricks

                    scriptStates.add(ScriptState(
                        spriteIndex = i,
                        originalBlocks = allBricks,
                        runtimeFormulas = runtimeFormulas,
                        eventType = eventType,
                        eventParam = eventParam2,
                        conditionFormula = conditionElement
                    ))
                }
            }
            objects.length > 0
        } catch (e: Exception) {
            Gdx.app.error("ScriptEngine", "XML parse failed", e)
            false
        }
    }

    /**
     * Рекурсивный парсинг нод бриков.
     * Возвращает пару (список блоков, список runtime-формул).
     *
     * Поддерживает:
     *  ForeverBrick … LoopEndBrick
     *  RepeatBrick … LoopEndBrick
     *  RepeatUntilBrick … LoopEndBrick
     *  IfLogicBeginBrick … [IfLogicElseBrick …] IfLogicEndBrick
     *  ForVariableFromToBrick … LoopEndBrick
     *  ForItemInUserListBrick … LoopEndBrick
     *  ScheduleBrick … LoopEndBrick
     *  ExecuteForCloneNumberBrick … LoopEndBrick
     *  RunAsSpriteBrick … LoopEndBrick
     *  TryCatchFinallyBrick … (catch/finally) … EndBrick
     *
     * LoopEndBrick / IfLogicElseBrick / IfLogicEndBrick — возврат к родителю.
     */
    private fun parseBrickListRecursive(nodes: NodeList, startIdx: Int, spriteIndex: Int = 0, endIndex: Int = nodes.length): Pair<List<Block>, List<RuntimeFormula>> {
        // Локальные обёртки, чтобы передавать spriteIndex во все вызовы extractFormulaValue/String
        fun extractFormulaValue(el: Element, field: String) =
            this@DesktopScriptEngine.extractFormulaValue(el, field, spriteIndex)
        fun extractFormulaString(el: Element, field: String) =
            this@DesktopScriptEngine.extractFormulaString(el, field, spriteIndex)

        val result = mutableListOf<Block>()
        val allRuntimeFormulas = mutableListOf<RuntimeFormula>()
        var idx = startIdx
        while (idx < nodes.length) {
            // Ограничение сверху: тело должно заканчиваться ровно на endIndex
            // (используется для case/switch и try/catch/finally, где граница — не LoopEndBrick).
            if (idx >= endIndex) return Pair(result, allRuntimeFormulas)
            val node = nodes.item(idx)
            if (node.nodeType != Node.ELEMENT_NODE) { idx++; continue }
            val el = node as Element
            val brickType = el.getAttribute("type")
            if (brickType.isBlank()) { idx++; continue }

            try {
                when (brickType) {
                // ── Loop containers ──
                "LoopEndlessBrick" -> {
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("forever"), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "AsyncRepeatBrick" -> {
                    val times = extractFormulaValue(el, "TIMES_TO_REPEAT")
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("async_repeat", (times ?: 1f).toInt()), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "ForeverBrick" -> {
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("forever"), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "RepeatBrick" -> {
                    val timesRf = getRuntimeFormula(el, "TIMES_TO_REPEAT")
                    val times = extractFormulaValue(el, "TIMES_TO_REPEAT") ?: 1f
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    val arg: Any = timesRf ?: times
                    result.add(Block(Block.Type.CONTROL, listOf("repeat", arg), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "CountLoopBrick" -> {
                    val times = extractFormulaValue(el, "TIMES_TO_REPEAT") ?: 1f
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("repeat", times.toInt().coerceAtLeast(1)), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "RepeatUntilBrick" -> {
                    val condFe = getFormulaElement(el, "REPEAT_UNTIL_CONDITION")
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    val condRf = if (condFe != null) RuntimeFormula("REPEAT_UNTIL_CONDITION", condFe) else null
                    result.add(Block(Block.Type.CONTROL, listOf("repeat_until", condRf ?: 0f), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "ForVariableFromToBrick" -> {
                    val varName = extractVariableName(el)
                    val fromRf = getRuntimeFormula(el, "FOR_LOOP_FROM")
                    val toRf = getRuntimeFormula(el, "FOR_LOOP_TO")
                    val fromVal = extractFormulaValue(el, "FOR_LOOP_FROM") ?: 0f
                    val toVal = extractFormulaValue(el, "FOR_LOOP_TO") ?: 0f
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    val fromArg: Any = fromRf ?: fromVal
                    val toArg: Any = toRf ?: toVal
                    if (varName != null && fromRf == null) variables[varName] = fromVal
                    // repeat count: use static values only if both are non-RuntimeFormula
                    val count = if (fromRf == null && toRf == null) {
                        maxOf(1, (toVal - fromVal + 1f).toInt())
                    } else 1 // dynamic: assume at least 1, runtime re-evaluates via inc_var
                    // Добавляем синтетический блок для инкремента переменной в конце каждой итерации
                    val loopChildren = if (varName != null) {
                        children + Block(Block.Type.VARIABLE, listOf("inc_var", varName))
                    } else children
                    result.add(Block(Block.Type.CONTROL, listOf("repeat", count), loopChildren))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "ScheduleBrick" -> {
                    val delayRf = getRuntimeFormula(el, "TIME_TO_WAIT_IN_SECONDS")
                    val delay = extractFormulaValue(el, "TIME_TO_WAIT_IN_SECONDS") ?: 1f
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    val arg: Any = delayRf ?: delay
                    result.add(Block(Block.Type.CONTROL, listOf("wait", arg)))
                    result.addAll(children)
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "ExecuteForCloneNumberBrick" -> {
                    val cloneNum = extractFormulaValue(el, "NUMBER") ?: 0f
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("repeat", 1), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "RunAsSpriteBrick" -> {
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    // Оборачиваем детей в маркеры для защиты от рекурсии
                    result.add(Block(Block.Type.CONTROL, listOf("run_as_start")))
                    result.addAll(children)
                    result.add(Block(Block.Type.CONTROL, listOf("run_as_end")))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "RunOnUiThreadBrick" -> {
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.addAll(children)
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "UserDefinedReceiverBrick" -> {
                    // Вызов пользовательской процедуры.
                    val procId = getTagText(el, "userDefinedBrickID") ?: ""
                    val argFormulas = extractUserDefinedArgFormulas(el)
                    argFormulas.filterNotNull().forEach { allRuntimeFormulas.add(it) }
                    result.add(Block(Block.Type.CONTROL, listOf("user_call", procId, argFormulas)))
                    idx++
                }
                "ForItemInUserListBrick" -> {
                    val listName = extractUserListName(el) ?: ""
                    val varName = extractVariableName(el) ?: ""
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("for_item_list", listName, varName), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "IntervalRepeatBrick" -> {
                    val countRf = getRuntimeFormula(el, "TIMES_TO_REPEAT")
                    val intervalRf = getRuntimeFormula(el, "INTERVAL")
                    val count = extractFormulaValue(el, "TIMES_TO_REPEAT") ?: 1f
                    val interval = extractFormulaValue(el, "INTERVAL") ?: 0f
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    val countArg: Any = countRf ?: count.toInt()
                    val intervalArg: Any = intervalRf ?: interval
                    result.add(Block(Block.Type.CONTROL, listOf("interval_repeat", countArg, intervalArg), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "TryCatchFinallyBrick" -> {
                    // try section — ограничиваем тело первым Catch/Finally/End на глубине 0,
                    // иначе parseBrickListRecursive захватит и тела catch/finally (они не LoopEndBrick).
                    val trySectionEnd = findTrySectionEnd(nodes, idx + 1)
                    val (tryChildren, rf1) = parseBrickListRecursive(nodes, idx + 1, spriteIndex, endIndex = trySectionEnd)
                    allRuntimeFormulas.addAll(rf1)
                    var nextIdx = trySectionEnd
                    // catch section (after CatchBrick marker)
                    var catchBricks = emptyList<Block>()
                    var catchVar = ""
                    val catchNode = if (nextIdx < nodes.length) nodes.item(nextIdx) else null
                    if (catchNode != null && catchNode.nodeType == Node.ELEMENT_NODE) {
                        val catchEl = catchNode as Element
                        if (catchEl.getAttribute("type") == "CatchBrick") {
                            catchVar = extractVariableName(catchEl) ?: ""
                            val catchSectionEnd = findTrySectionEnd(nodes, nextIdx + 1)
                            val (cb, rf2) = parseBrickListRecursive(nodes, nextIdx + 1, spriteIndex, endIndex = catchSectionEnd)
                            allRuntimeFormulas.addAll(rf2)
                            catchBricks = cb
                            nextIdx = catchSectionEnd
                        }
                    }
                    // finally section (after FinallyBrick marker)
                    var finallyBricks = emptyList<Block>()
                    val finallyNode = if (nextIdx < nodes.length) nodes.item(nextIdx) else null
                    if (finallyNode != null && finallyNode.nodeType == Node.ELEMENT_NODE) {
                        val finallyEl = finallyNode as Element
                        if (finallyEl.getAttribute("type") == "FinallyBrick") {
                            val finallySectionEnd = findTrySectionEnd(nodes, nextIdx + 1)
                            val (fb, rf3) = parseBrickListRecursive(nodes, nextIdx + 1, spriteIndex, endIndex = finallySectionEnd)
                            allRuntimeFormulas.addAll(rf3)
                            finallyBricks = fb
                            nextIdx = finallySectionEnd
                        }
                    }
                    result.add(Block(Block.Type.CONTROL, listOf("try_catch", catchVar, tryChildren, catchBricks, finallyBricks), tryChildren))
                    idx = nextIdx  // EndBrick
                }
                "SwitchBeginBrick" -> {
                    val switchVal = extractFormulaString(el, "TEXT") ?: ""
                    // Parse cases until SwitchEndBrick or LoopEndBrick
                    val caseBlocks = mutableListOf<Pair<String, List<Block>>>()
                    var si = idx + 1
                    while (si < nodes.length) {
                        val sn = nodes.item(si)
                        if (sn.nodeType != Node.ELEMENT_NODE) { si++; continue }
                        val se = sn as Element
                        val stype = se.getAttribute("type")
                        if (stype == "LoopEndBrick" || stype == "SwitchEndBrick") break
                        if (stype == "SwitchCaseBrick") {
                            val caseVal = extractFormulaString(se, "TEXT") ?: ""
                            // Тело case ограничиваем следующим SwitchCaseBrick/SwitchEndBrick
                            // (с учётом вложенных контейнеров), иначе parseBrickListRecursive
                            // «сливает» тела всех case в одно.
                            val caseEnd = findSwitchCaseEnd(nodes, si + 1)
                            val (caseChildren, rf) = parseBrickListRecursive(nodes, si + 1, spriteIndex, endIndex = caseEnd)
                            allRuntimeFormulas.addAll(rf)
                            caseBlocks.add(caseVal to caseChildren)
                            si = caseEnd
                            continue
                        }
                        si++
                    }
                    // Keep the case bodies: each SwitchCase carries its comparison
                    // value and the parsed block list so the matching branch can run.
                    val cases = caseBlocks.map { SwitchCase(it.first, it.second) }
                    result.add(Block(Block.Type.CONTROL, listOf("switch_begin", switchVal, cases)))
                    idx = findLoopEnd(nodes, si)
                }
                "LoopEndBrick" -> return Pair(result, allRuntimeFormulas)

                // ── If container ──
                "IfLogicBeginBrick" -> {
                    val condRf = getRuntimeFormula(el, "IF_CONDITION")
                    val condVal = extractFormulaValue(el, "IF_CONDITION") ?: 0f
                    val (thenChildren, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)

                    var elseChildren = emptyList<Block>()
                    var afterIfIdx = findIfEnd(nodes, idx + 1)

                    // Проверяем, остановилась ли рекурсия на IfLogicElseBrick
                    val stopNode = if (afterIfIdx < nodes.length) nodes.item(afterIfIdx) else null
                    if (stopNode != null && stopNode.nodeType == Node.ELEMENT_NODE &&
                        (stopNode as Element).getAttribute("type") == "IfLogicElseBrick") {
                        // Откатываемся и парсим else
                        val (elseB, rf2) = parseBrickListRecursive(nodes, afterIfIdx + 1)
                        allRuntimeFormulas.addAll(rf2)
                        elseChildren = elseB
                        afterIfIdx = findIfEnd(nodes, afterIfIdx + 1)
                    }

                    val condArg: Any = condRf ?: condVal
                    result.add(Block(Block.Type.CONTROL, listOf("if", condArg, thenChildren, elseChildren), thenChildren))
                    idx = afterIfIdx
                }
                "IfLogicElseBrick" -> {
                    return Pair(result, allRuntimeFormulas)
                }
                "IfLogicEndBrick" -> {
                    return Pair(result, allRuntimeFormulas)
                }
                "IfThenLogicBeginBrick" -> {
                    val condRf = getRuntimeFormula(el, "IF_CONDITION")
                    val condVal = extractFormulaValue(el, "IF_CONDITION") ?: 0f
                    val (thenChildren, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    val condArg: Any = condRf ?: condVal
                    result.add(Block(Block.Type.CONTROL, listOf("if", condArg, thenChildren, emptyList<Block>()), thenChildren))
                    idx = findIfEnd(nodes, idx + 1)
                }
                "IfThenLogicEndBrick" -> {
                    return Pair(result, allRuntimeFormulas)
                }

                // ── Variables ──
                "SetVariableBrick" -> {
                    val varName = extractVariableName(el)
                    val value = extractFormulaValue(el, "VARIABLE")
                    val rf = getRuntimeFormula(el, "VARIABLE")
                    val arg: Any = rf ?: (value ?: 0f)
                    result.add(Block(Block.Type.VARIABLE, listOf("set", varName ?: "", arg)))
                    if (rf != null) allRuntimeFormulas.add(rf)
                    idx++
                }
                "ChangeVariableBrick" -> {
                    val varName = extractVariableName(el)
                    val delta = extractFormulaValue(el, "VARIABLE_CHANGE")
                    val rf = getRuntimeFormula(el, "VARIABLE_CHANGE")
                    val arg: Any = rf ?: (delta ?: 0f)
                    result.add(Block(Block.Type.VARIABLE, listOf("change", varName ?: "", arg)))
                    if (rf != null) allRuntimeFormulas.add(rf)
                    idx++
                }
                "ShowTextBrick" -> {
                    val varName = extractVariableName(el) ?: ""
                    val xRf = getRuntimeFormula(el, "X_POSITION")
                    val yRf = getRuntimeFormula(el, "Y_POSITION")
                    val x = xRf ?: (extractFormulaValue(el, "X_POSITION") ?: 0f)
                    val y = yRf ?: (extractFormulaValue(el, "Y_POSITION") ?: 0f)
                    if (xRf != null) allRuntimeFormulas.add(xRf)
                    if (yRf != null) allRuntimeFormulas.add(yRf)
                    result.add(Block(Block.Type.VARIABLE, listOf("show_variable", varName, x, y)))
                    idx++
                }
                "HideTextBrick" -> {
                    val varName = extractVariableName(el) ?: ""
                    result.add(Block(Block.Type.VARIABLE, listOf("hide_variable", varName)))
                    idx++
                }

                // ── Variable extras (float / easing / device list) ──
                "CreateFloatBrick" -> {
                    val name = extractFormulaString(el, "FLOAT_ARRAY") ?: ""
                    result.add(Block(Block.Type.VARIABLE, listOf("create_float", name)))
                    idx++
                }
                "DeleteFloatBrick" -> {
                    val name = extractFormulaString(el, "FLOAT_ARRAY") ?: ""
                    result.add(Block(Block.Type.VARIABLE, listOf("delete_float", name)))
                    idx++
                }
                "SetVariableEasingBrick" -> {
                    val name = extractVariableName(el) ?: ""
                    val end = extractFormulaValue(el, "INSERT_ITEM_INTO_USERLIST_INDEX") ?: 0f
                    val duration = extractFormulaValue(el, "INSERT_ITEM_INTO_USERLIST_VALUE") ?: 0f
                    result.add(Block(Block.Type.VARIABLE, listOf("set_easing", name, end, duration)))
                    idx++
                }
                "ReadListFromDeviceBrick" -> {
                    val name = extractUserListName(el) ?: ""
                    result.add(Block(Block.Type.DATA, listOf("read_list_device", name)))
                    idx++
                }
                "WriteListOnDeviceBrick" -> {
                    val name = extractUserListName(el) ?: ""
                    result.add(Block(Block.Type.DATA, listOf("write_list_device", name)))
                    idx++
                }

                // ── Web ──
                "WebRequestBrick" -> {
                    val url = extractFormulaString(el, "WEB_REQUEST") ?: ""
                    val varName = extractVariableName(el) ?: ""
                    result.add(Block(Block.Type.WEB, listOf("http_get", url, varName)))
                    idx++
                }
                "PostWebRequestBrick" -> {
                    val url = extractFormulaString(el, "URL") ?: ""
                    val body = extractFormulaString(el, "BODY") ?: ""
                    val varName = extractVariableName(el) ?: ""
                    result.add(Block(Block.Type.WEB, listOf("http_post", url, body, varName)))
                    idx++
                }
                "PutWebRequestBrick" -> {
                    val url = extractFormulaString(el, "URL") ?: ""
                    val body = extractFormulaString(el, "BODY") ?: ""
                    val varName = extractVariableName(el) ?: ""
                    result.add(Block(Block.Type.WEB, listOf("http_put", url, body, varName)))
                    idx++
                }
                "DeleteWebRequestBrick" -> {
                    val url = extractFormulaString(el, "URL") ?: ""
                    val varName = extractVariableName(el) ?: ""
                    result.add(Block(Block.Type.WEB, listOf("http_delete", url, varName)))
                    idx++
                }

                // ── Control (non-container) ──
                "BroadcastBrick" -> {
                    val msg = extractMessageText(el, "broadcastMessage")
                    result.add(Block(Block.Type.CONTROL, listOf("broadcast", msg)))
                    idx++
                }
                "BroadcastWaitBrick" -> {
                    val msg = extractMessageText(el, "broadcastMessage")
                    result.add(Block(Block.Type.CONTROL, listOf("broadcast_wait", msg)))
                    idx++
                }
                "CloneBrick" -> {
                    result.add(Block(Block.Type.CONTROL, listOf("clone")))
                    idx++
                }
                "DeleteThisCloneBrick" -> {
                    result.add(Block(Block.Type.CONTROL, listOf("delete_clone")))
                    idx++
                }
                "StopScriptBrick" -> {
                    result.add(Block(Block.Type.CONTROL, listOf("stop_script")))
                    idx++
                }

                // ── Data (file) ──
                "WriteVariableOnDeviceBrick" -> {
                    val varName = extractVariableName(el)
                    result.add(Block(Block.Type.DATA, listOf("write_variable", varName ?: "")))
                    idx++
                }
                "ReadVariableFromDeviceBrick" -> {
                    val varName = extractVariableName(el)
                    result.add(Block(Block.Type.DATA, listOf("read_variable", varName ?: "")))
                    idx++
                }

                // ── User List bricks ──
                "AddItemToUserListBrick" -> {
                    val listName = extractUserListName(el) ?: ""
                    val value = extractFormulaString(el, "LIST_ADD_ITEM") ?: ""
                    result.add(Block(Block.Type.VARIABLE, listOf("list_add", listName, value)))
                    idx++
                }
                "DeleteItemOfUserListBrick" -> {
                    val listName = extractUserListName(el) ?: ""
                    val index = extractFormulaValue(el, "LIST_DELETE_ITEM") ?: 1f
                    result.add(Block(Block.Type.VARIABLE, listOf("list_delete", listName, index.toInt())))
                    idx++
                }
                "InsertItemIntoUserListBrick" -> {
                    val listName = extractUserListName(el) ?: ""
                    val value = extractFormulaString(el, "INSERT_ITEM_INTO_USERLIST_VALUE") ?: ""
                    val index = extractFormulaValue(el, "INSERT_ITEM_INTO_USERLIST_INDEX") ?: 1f
                    result.add(Block(Block.Type.VARIABLE, listOf("list_insert", listName, index.toInt(), value)))
                    idx++
                }
                "ReplaceItemInUserListBrick" -> {
                    val listName = extractUserListName(el) ?: ""
                    val value = extractFormulaString(el, "REPLACE_ITEM_IN_USERLIST_VALUE") ?: ""
                    val index = extractFormulaValue(el, "REPLACE_ITEM_IN_USERLIST_INDEX") ?: 1f
                    result.add(Block(Block.Type.VARIABLE, listOf("list_replace", listName, index.toInt(), value)))
                    idx++
                }
                "ClearUserListBrick" -> {
                    val listName = extractUserListName(el) ?: ""
                    result.add(Block(Block.Type.VARIABLE, listOf("list_clear", listName)))
                    idx++
                }
                "SplitBrick" -> {
                    val listName = extractUserListName(el) ?: ""
                    val text = extractFormulaString(el, "TEXT") ?: ""
                    val separator = extractFormulaString(el, "VALUE") ?: ""
                    result.add(Block(Block.Type.VARIABLE, listOf("list_split", listName, text, separator)))
                    idx++
                }
                "StoreCSVIntoUserListBrick" -> {
                    val listName = extractUserListName(el) ?: ""
                    val csv = extractFormulaString(el, "STORE_CSV_INTO_USERLIST_CSV") ?: ""
                    val column = extractFormulaValue(el, "STORE_CSV_INTO_USERLIST_COLUMN") ?: 1f
                    result.add(Block(Block.Type.VARIABLE, listOf("list_csv", listName, csv, column.toInt())))
                    idx++
                }
                "RegexBrick" -> {
                    val listName = extractUserListName(el) ?: ""
                    val text = extractFormulaString(el, "TEXT") ?: ""
                    val regex = extractFormulaString(el, "REGEX") ?: ""
                    result.add(Block(Block.Type.VARIABLE, listOf("list_regex", listName, text, regex)))
                    idx++
                }

                // ── Camera 3D (emulated on the orthographic 2D view) ──
                "SetCameraPositionBrick" -> {
                    val x = extractFormulaValue(el, "VALUE") ?: 0f
                    val y = extractFormulaValue(el, "VALUE_2") ?: 0f
                    result.add(Block(Block.Type.CAMERA, listOf("set_camera_position", x, y)))
                    idx++
                }
                "SetCameraPosition2Brick" -> {
                    val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                    val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                    result.add(Block(Block.Type.CAMERA, listOf("set_camera_position2", x, y)))
                    idx++
                }
                "Fast2DSetCameraBrick" -> {
                    val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                    val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                    val zoom = extractFormulaValue(el, "SIZE") ?: 1f
                    result.add(Block(Block.Type.CAMERA, listOf("fast2d_set_camera", x, y, zoom)))
                    idx++
                }
                "SetCameraRotationBrick" -> {
                    val yaw = extractFormulaValue(el, "VALUE_1") ?: 0f
                    result.add(Block(Block.Type.CAMERA, listOf("set_camera_rotation", yaw)))
                    idx++
                }
                "SetCameraRotation2Brick" -> {
                    val deg = extractFormulaValue(el, "DEGREES") ?: 0f
                    result.add(Block(Block.Type.CAMERA, listOf("set_camera_rotation2", deg)))
                    idx++
                }
                "SetCameraZoomBrick" -> {
                    val zoom = extractFormulaValue(el, "VALUE") ?: 1f
                    result.add(Block(Block.Type.CAMERA, listOf("set_camera_zoom", zoom)))
                    idx++
                }
                "RotateCameraByBrick" -> {
                    val yaw = extractFormulaValue(el, "YAW") ?: 0f
                    result.add(Block(Block.Type.CAMERA, listOf("rotate_camera_by", 0f, yaw, 0f)))
                    idx++
                }
                "PinToCameraBrick" -> {
                    result.add(Block(Block.Type.CAMERA, listOf("pin_to_camera")))
                    idx++
                }
                "UnpinFromCameraBrick" -> {
                    result.add(Block(Block.Type.CAMERA, listOf("unpin_from_camera")))
                    idx++
                }
                "AttachToCameraBrick" -> {
                    val name = extractFormulaString(el, "NAME") ?: ""
                    result.add(Block(Block.Type.CAMERA, listOf("attach_to_camera", name)))
                    idx++
                }
                "AttachToCameraWithOffsetBrick" -> {
                    val name = extractFormulaString(el, "NAME") ?: ""
                    val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                    val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                    result.add(Block(Block.Type.CAMERA, listOf("attach_to_camera_with_offset", name, x, y)))
                    idx++
                }
                "DetachFromCameraBrick" -> {
                    val name = extractFormulaString(el, "NAME") ?: ""
                    result.add(Block(Block.Type.CAMERA, listOf("detach_from_camera", name)))
                    idx++
                }
                "SetBufferCameraBrick" -> {
                    val name = extractFormulaString(el, "NAME") ?: ""
                    val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                    val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                    val zoom = extractFormulaValue(el, "ZOOM") ?: 1f
                    val deg = extractFormulaValue(el, "DEGREES") ?: 0f
                    result.add(Block(Block.Type.CAMERA, listOf("set_buffer_camera", name, x, y, zoom, deg)))
                    idx++
                }
                "SetBufferCamera3DBrick" -> {
                    val name = extractFormulaString(el, "NAME") ?: ""
                    result.add(Block(Block.Type.CAMERA, listOf("set_buffer_camera_3d", name)))
                    idx++
                }

                // ── Individual bricks (leaf nodes) ──
                else -> {
                    if (brickType in CONTAINER_BRICK_TYPES || hasNestedBrickList(el)) {
                        idx = skipUnsupportedContainer(nodes, idx)
                        continue
                    }
                    val parsed = runCatching { parseBrickLeaf(el, brickType) }
                        .onFailure {
                            Gdx.app.error("ScriptEngine", "Skipping unsupported brick type=$brickType", it)
                        }
                        .getOrNull()
                    if (parsed != null) result.add(parsed)
                    idx++
                }
                }
            } catch (e: Exception) {
                Gdx.app.error("ScriptEngine", "Skipping broken brick type=$brickType", e)
                idx = if (brickType in CONTAINER_BRICK_TYPES || hasNestedBrickList(el)) {
                    skipUnsupportedContainer(nodes, idx)
                } else {
                    idx + 1
                }
            }
        }
        return Pair(result, allRuntimeFormulas)
    }

    /** Найти позицию LoopEndBrick, начиная со startIdx. */
    private fun findLoopEnd(nodes: NodeList, startIdx: Int): Int {
        var depth = 0
        for (i in startIdx until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val el = node as Element
            val type = el.getAttribute("type")
            when (type) {
                "ForeverBrick", "RepeatBrick", "RepeatUntilBrick",
                "ForVariableFromToBrick", "ForItemInUserListBrick",
                "ScheduleBrick", "ExecuteForCloneNumberBrick",
                "IntervalRepeatBrick", "TryCatchFinallyBrick", "SwitchBeginBrick",
                "IfLogicBeginBrick", "IfThenLogicBeginBrick" -> depth++
                "LoopEndBrick" -> if (depth == 0) return i + 1 else depth--
                "IfLogicEndBrick", "IfThenLogicEndBrick" -> if (depth == 0) return i + 1
            }
        }
        return nodes.length
    }

    /** Найти позицию EndBrick для TryCatchFinally (игнорируя вложенные контейнеры). */
    /**
     * Найти позицию следующего SwitchCaseBrick / SwitchEndBrick / LoopEndBrick на
     * глубине 0 — это конец тела текущего case внутри switch. Вложенные контейнеры
     * (loop/if/try/switch) учитываются по depth, чтобы их собственные границы не
     * обрывали тело case преждевременно.
     */
    private fun findSwitchCaseEnd(nodes: NodeList, startIdx: Int): Int {
        var depth = 0
        for (i in startIdx until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val el = node as Element
            val type = el.getAttribute("type")
            when {
                type in CONTAINER_BRICK_TYPES -> depth++
                type in CONTAINER_BOUNDARY_TYPES -> if (depth == 0) return i else depth--
                type == "SwitchCaseBrick" -> if (depth == 0) return i
            }
        }
        return nodes.length
    }

    /**
     * Найти позицию первого CatchBrick / FinallyBrick / EndBrick на глубине 0 —
     * это граница очередной секции try/catch/finally. Вложенные контейнеры
     * учитываются по depth.
     */
    private fun findTrySectionEnd(nodes: NodeList, startIdx: Int): Int {
        var depth = 0
        for (i in startIdx until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val el = node as Element
            val type = el.getAttribute("type")
            when {
                type in CONTAINER_BRICK_TYPES -> depth++
                type in CONTAINER_BOUNDARY_TYPES -> if (depth == 0) return i else depth--
            }
        }
        return nodes.length
    }

    /** Найти позицию IfLogicEndBrick / IfThenLogicEndBrick. */
    private fun findIfEnd(nodes: NodeList, startIdx: Int): Int {
        var depth = 0
        for (i in startIdx until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val el = node as Element
            val type = el.getAttribute("type")
            when (type) {
                "IfLogicBeginBrick", "IfThenLogicBeginBrick",
                "ForeverBrick", "RepeatBrick" -> depth++
                "IfLogicEndBrick", "IfThenLogicEndBrick" -> if (depth == 0) return i + 1 else depth--
                "LoopEndBrick" -> if (depth == 0) return i + 1 else depth--
            }
        }
        return nodes.length
    }

    /** Парсит отдельный (не-контейнерный) брик, используя атрибут type. */
    /** Пропустить неподдерживаемый контейнерный брик вместе с его телом. */
    private fun skipUnsupportedContainer(nodes: NodeList, startIdx: Int): Int {
        var depth = 0
        for (i in startIdx + 1 until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val el = node as Element
            val type = el.getAttribute("type")
            when {
                type in CONTAINER_BRICK_TYPES -> depth++
                type in CONTAINER_BOUNDARY_TYPES -> {
                    if (depth == 0) return i + 1
                    depth--
                }
            }
        }
        return nodes.length
    }

    private fun hasNestedBrickList(el: Element): Boolean {
        return (el.getElementsByTagName("brickList")?.length ?: 0) > 0
    }

    private fun parseBrickLeaf(el: Element, typeName: String): Block? {
        return when (typeName) {
            // ═══════ MOTION ═══════
            "MoveNStepsBrick" -> {
                val steps = extractFormulaValue(el, "STEPS") ?: 10f
                Block(Block.Type.MOTION, listOf("move_steps", steps))
            }
            "TurnLeftBrick" -> {
                val deg = extractFormulaValue(el, "TURN_LEFT_DEGREES") ?: 15f
                Block(Block.Type.MOTION, listOf("turn_left", deg))
            }
            "TurnRightBrick" -> {
                val deg = extractFormulaValue(el, "TURN_RIGHT_DEGREES") ?: 15f
                Block(Block.Type.MOTION, listOf("turn_right", deg))
            }
            "SetXBrick" -> {
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                Block(Block.Type.MOTION, listOf("set_x", x))
            }
            "SetYBrick" -> {
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.MOTION, listOf("set_y", y))
            }
            "ChangeXByNBrick" -> {
                val dx = extractFormulaValue(el, "X_POSITION_CHANGE") ?: 0f
                Block(Block.Type.MOTION, listOf("change_x", dx))
            }
            "ChangeYByNBrick" -> {
                val dy = extractFormulaValue(el, "Y_POSITION_CHANGE") ?: 0f
                Block(Block.Type.MOTION, listOf("change_y", dy))
            }
            "GoToBrick" -> {
                val selText = extractTextContent(el, "spinnerSelection") ?: ""
                val sel = selText.toIntOrNull() ?: 80
                when (sel) {
                    80 -> Block(Block.Type.MOTION, listOf("goto_touch"))
                    81 -> Block(Block.Type.MOTION, listOf("goto_random"))
                    else -> {
                        val destEl = getChildElement(el, "destinationSprite")
                        val destName = destEl?.getAttribute("name") ?: ""
                        Block(Block.Type.MOTION, listOf("goto_sprite", destName))
                    }
                }
            }
            "PlaceAtBrick" -> {
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.MOTION, listOf("goto_xy", x, y))
            }
            "SetSizeToBrick" -> Block(Block.Type.LOOKS, listOf("set_size", extractFormulaValue(el, "SIZE") ?: 100f))
            "PointInDirectionBrick" -> {
                val dir = extractFormulaValue(el, "DEGREES") ?: 90f
                Block(Block.Type.MOTION, listOf("set_direction", dir))
            }
            "GlideToBrick" -> {
                val x = extractFormulaValue(el, "X_DESTINATION") ?: 0f
                val y = extractFormulaValue(el, "Y_DESTINATION") ?: 0f
                val dur = extractFormulaValue(el, "DURATION_IN_SECONDS") ?: 1f
                Block(Block.Type.MOTION, listOf("glide", x, y, dur))
            }
            "IfOnEdgeBounceBrick" -> Block(Block.Type.MOTION, listOf("bounce"))
            "ComeToFrontBrick" -> Block(Block.Type.MOTION, listOf("come_to_front"))
            "GoNStepsBackBrick" -> {
                val n = extractFormulaValue(el, "STEPS") ?: 1f
                Block(Block.Type.MOTION, listOf("go_back_layers", n.toInt()))
            }
            "SetRotationStyleBrick" -> {
                val selText = extractTextContent(el, "selection") ?: "0"
                Block(Block.Type.MOTION, listOf("set_rotation_style", selText.toIntOrNull() ?: 0))
            }
            "TouchDirectionBrick" -> Block(Block.Type.MOTION, listOf("touch_direction"))

            // ═══════ LOOKS ═══════
            "ShowBrick" -> Block(Block.Type.LOOKS, listOf("show"))
            "HideBrick" -> Block(Block.Type.LOOKS, listOf("hide"))
            "NextLookBrick" -> Block(Block.Type.LOOKS, listOf("next_look"))
            "PreviousLookBrick" -> Block(Block.Type.LOOKS, listOf("previous_look"))
            "SetLookBrick" -> {
                val lookEl = getChildElement(el, "look")
                val lookName = lookEl?.getAttribute("name") ?: ""
                Block(Block.Type.LOOKS, listOf("switch_look_by_name", lookName))
            }
            "SetLookByIndexBrick" -> {
                val idx = extractFormulaValue(el, "LOOK_INDEX") ?: 0f
                Block(Block.Type.LOOKS, listOf("switch_look", idx.toInt()))
            }
            "SetBackgroundByIndexBrick" -> {
                val idx = extractFormulaValue(el, "BACKGROUND_INDEX") ?: 0f
                Block(Block.Type.LOOKS, listOf("switch_look", idx.toInt()))
            }
            "ChangeSizeByNBrick" -> {
                val delta = extractFormulaValue(el, "SIZE_CHANGE") ?: 0f
                Block(Block.Type.LOOKS, listOf("change_size", delta))
            }
            "SetTransparencyBrick" -> {
                val v = extractFormulaValue(el, "TRANSPARENCY") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_transparency", v))
            }
            "ChangeTransparencyByNBrick" -> {
                val v = extractFormulaValue(el, "TRANSPARENCY_CHANGE") ?: 0f
                Block(Block.Type.LOOKS, listOf("change_transparency", v))
            }
            "SetBrightnessBrick" -> {
                val v = extractFormulaValue(el, "BRIGHTNESS") ?: 100f
                Block(Block.Type.LOOKS, listOf("set_brightness", v))
            }
            "ChangeBrightnessByNBrick" -> {
                val v = extractFormulaValue(el, "BRIGHTNESS_CHANGE") ?: 0f
                Block(Block.Type.LOOKS, listOf("change_brightness", v))
            }
            "SetColorBrick" -> {
                val v = extractFormulaValue(el, "COLOR") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_color", v))
            }
            "ChangeColorByNBrick" -> {
                val v = extractFormulaValue(el, "COLOR_CHANGE") ?: 0f
                Block(Block.Type.LOOKS, listOf("change_color", v))
            }
            "ClearGraphicEffectBrick" -> Block(Block.Type.LOOKS, listOf("clear_effects"))
            "SetFilterBlurBrick" -> {
                val v = extractFormulaValue(el, "INTENSITY") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_filter_blur", v))
            }
            "SetFilterPixelateBrick" -> {
                val v = extractFormulaValue(el, "SIZE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_filter_pixelate", v))
            }
            "SetFilterSepiaBrick" -> {
                val v = extractFormulaValue(el, "VALUE_1") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_filter_sepia", v))
            }
            "SetWidthBrick" -> {
                val v = extractFormulaValue(el, "SIZE") ?: 100f
                Block(Block.Type.LOOKS, listOf("set_width", v))
            }
            "ChangeWidthBrick" -> {
                val v = extractFormulaValue(el, "SIZE") ?: 0f
                Block(Block.Type.LOOKS, listOf("change_width", v))
            }
            "SetHeightBrick" -> {
                val v = extractFormulaValue(el, "SIZE") ?: 100f
                Block(Block.Type.LOOKS, listOf("set_height", v))
            }
            "ChangeHeightBrick" -> {
                val v = extractFormulaValue(el, "SIZE") ?: 0f
                Block(Block.Type.LOOKS, listOf("change_height", v))
            }

            // ── Text/bubble ──
            "ThinkBubbleBrick" -> {
                val text = extractFormulaString(el, "STRING") ?: ""
                Block(Block.Type.LOOKS, listOf("think_bubble", text))
            }
            "SayBubbleBrick" -> {
                val text = extractFormulaString(el, "STRING") ?: ""
                Block(Block.Type.LOOKS, listOf("say_bubble", text))
            }
            "ThinkForBubbleBrick" -> {
                val text = extractFormulaString(el, "STRING") ?: ""
                val duration = extractFormulaValue(el, "DURATION_IN_SECONDS") ?: 2f
                Block(Block.Type.LOOKS, listOf("think_for_bubble", text, duration))
            }
            "SayForBubbleBrick" -> {
                val text = extractFormulaString(el, "STRING") ?: ""
                val duration = extractFormulaValue(el, "DURATION_IN_SECONDS") ?: 2f
                Block(Block.Type.LOOKS, listOf("say_for_bubble", text, duration))
            }
            "ShowDialogBrick" -> {
                val msg = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.LOOKS, listOf("show_dialog", msg))
            }
            "ShowText3Brick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val text = extractFormulaString(el, "TEXT") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                val size = extractFormulaValue(el, "SIZE") ?: 14f
                val color = extractFormulaValue(el, "COLOR") ?: 0f
                Block(Block.Type.LOOKS, listOf("show_text_overlay", name, text, x, y, size, color))
            }
            "HideText3Brick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.LOOKS, listOf("hide_text_overlay", name))
            }
            "SetTextBrick" -> {
                val name = extractFormulaString(el, "X_DESTINATION") ?: ""
                val text = extractFormulaString(el, "STRING") ?: ""
                Block(Block.Type.LOOKS, listOf("set_text_overlay", name, text))
            }
            "SetFontBrick" -> {
                val fontName = extractFormulaString(el, "NAME") ?: ""
                val fontSize = extractFormulaValue(el, "SIZE") ?: 14f
                Block(Block.Type.LOOKS, listOf("set_font", fontName, fontSize))
            }
            "SetCanvasBrick" -> {
                val canvasName = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.LOOKS, listOf("set_canvas", canvasName))
            }
            "SetObjectColorBrick" -> {
                val objectId = extractFormulaString(el, "VALUE_1") ?: ""
                val r = extractFormulaValue(el, "VALUE_2") ?: 1f
                val g = extractFormulaValue(el, "VALUE_3") ?: 1f
                val b = extractFormulaValue(el, "VALUE_4") ?: 1f
                Block(Block.Type.LOOKS, listOf("set_object_color", objectId, r, g, b))
            }
            "SetObjectTextureBrick" -> {
                val objectId = extractFormulaString(el, "VALUE") ?: ""
                val textureName = extractFormulaString(el, "VALUE_2") ?: ""
                Block(Block.Type.LOOKS, listOf("set_object_texture", objectId, textureName))
            }
            "SetObjectShaderBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val vertex = extractFormulaString(el, "TEXT") ?: ""
                val fragment = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.LOOKS, listOf("set_object_shader", objectId, vertex, fragment))
            }
            "SetObjectShaderUniformBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val uniform = extractFormulaString(el, "TEXT") ?: ""
                val v1 = extractFormulaValue(el, "X") ?: 0f
                val v2 = extractFormulaValue(el, "Y") ?: 0f
                val v3 = extractFormulaValue(el, "Z") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_object_shader_uniform", objectId, uniform, v1, v2, v3))
            }
            "SetScreenShaderBrick" -> {
                val vertex = extractFormulaString(el, "VERTEX") ?: ""
                val fragment = extractFormulaString(el, "FRAGMENT") ?: ""
                Block(Block.Type.LOOKS, listOf("set_screen_shader", vertex, fragment))
            }

            // ═══════ SOUND ═══════
            "PlaySoundBrick" -> {
                val sound = extractSoundName(el)
                Block(Block.Type.SOUND, listOf("play_sound", sound ?: ""))
            }
            "PlaySoundAndWaitBrick" -> {
                val sound = extractSoundName(el)
                Block(Block.Type.SOUND, listOf("play_sound_wait", sound ?: ""))
            }
            "StopSoundBrick" -> {
                val sound = extractSoundName(el)
                Block(Block.Type.SOUND, listOf("stop_sound", sound ?: ""))
            }
            "StopAllSoundsBrick" -> Block(Block.Type.SOUND, listOf("stop_all_sounds"))
            "SetVolumeToBrick" -> {
                val vol = extractFormulaValue(el, "VOLUME") ?: 100f
                Block(Block.Type.SOUND, listOf("set_volume", vol / 100f))
            }
            "ChangeVolumeByNBrick" -> {
                val delta = extractFormulaValue(el, "VOLUME_CHANGE") ?: 0f
                Block(Block.Type.SOUND, listOf("change_volume", delta))
            }
            "SetSoundVolumeBrick" -> {
                val vol = extractFormulaValue(el, "VOLUME") ?: 100f
                Block(Block.Type.SOUND, listOf("set_volume", vol / 100f))
            }
            // ── Sound 3D / instance (best-effort на Desktop) ──
            "PlaySoundAtPositionBrick" -> {
                val sound = extractFormulaString(el, "SOUND_NAME") ?: ""
                val vol = extractFormulaValue(el, "VOLUME") ?: 100f
                val pitch = extractFormulaValue(el, "PITCH") ?: 100f
                Block(Block.Type.SOUND, listOf("play_sound_3d", sound, vol / 100f, pitch / 100f))
            }
            "SetSoundInstanceVolumeBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val vol = extractFormulaValue(el, "VOLUME") ?: 100f
                Block(Block.Type.SOUND, listOf("set_sound_inst_vol", name, vol / 100f))
            }
            "SetSoundInstancePitchBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val pitch = extractFormulaValue(el, "PITCH") ?: 100f
                Block(Block.Type.SOUND, listOf("set_sound_inst_pitch", name, pitch / 100f))
            }
            "PrepareMusicAs3DSoundBrick" -> {
                val file = extractFormulaString(el, "FILE_NAME") ?: ""
                val sound = extractFormulaString(el, "SOUND_NAME") ?: ""
                Block(Block.Type.SOUND, listOf("prepare_3d_sound", file, sound))
            }
            "Set3DSoundPositionBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val x = extractFormulaValue(el, "VALUE_X") ?: 0f
                val y = extractFormulaValue(el, "VALUE_Y") ?: 0f
                val z = extractFormulaValue(el, "VALUE_Z") ?: 0f
                Block(Block.Type.SOUND, listOf("set_3d_pos", name, x, y, z))
            }

            // ═══════ MUSIC ═══════
            "PlayNoteForBeatsBrick" -> {
                val note = extractFormulaValue(el, "NOTE_TO_PLAY") ?: 60f
                val beats = extractFormulaValue(el, "BEATS_TO_PLAY_NOTE") ?: 1f
                Block(Block.Type.MUSIC, listOf("play_note", note.toInt(), beats))
            }
            "PlayDrumForBeatsBrick" -> {
                val beats = extractFormulaValue(el, "PLAY_DRUM") ?: 1f
                val drumText = extractTextContent(el, "drumSelection") ?: ""
                val drumProgram = DRUM_PROGRAM_MAP[drumText] ?: 36
                Block(Block.Type.MUSIC, listOf("play_drum", drumProgram, beats))
            }
            "SetInstrumentBrick" -> {
                val instText = extractTextContent(el, "instrumentSelection") ?: ""
                val instProg = INSTRUMENT_PROGRAM_MAP[instText] ?: 0
                Block(Block.Type.MUSIC, listOf("set_instrument", instProg))
            }
            "SetTempoBrick" -> {
                val tempo = extractFormulaValue(el, "TEMPO") ?: 60f
                Block(Block.Type.MUSIC, listOf("set_tempo", tempo))
            }
            "ChangeTempoByNBrick" -> {
                val delta = extractFormulaValue(el, "TEMPO_CHANGE") ?: 0f
                Block(Block.Type.MUSIC, listOf("change_tempo", delta))
            }
            "PauseForBeatsBrick" -> {
                val beats = extractFormulaValue(el, "BEATS_TO_PAUSE") ?: 1f
                Block(Block.Type.MUSIC, listOf("pause_beats", beats))
            }

            // ═══════ PEN ═══════
            "PenDownBrick" -> Block(Block.Type.PEN, listOf("pen_down"))
            "PenUpBrick" -> Block(Block.Type.PEN, listOf("pen_up"))
            "SetPenSizeBrick" -> {
                val sz = extractFormulaValue(el, "PEN_SIZE") ?: 1f
                Block(Block.Type.PEN, listOf("set_pen_size", sz))
            }
            "SetPenColorBrick" -> {
                val r = extractFormulaValue(el, "PEN_COLOR_RED") ?: 0f
                val g = extractFormulaValue(el, "PEN_COLOR_GREEN") ?: 0f
                val b = extractFormulaValue(el, "PEN_COLOR_BLUE") ?: 0f
                Block(Block.Type.PEN, listOf("set_pen_color", r, g, b))
            }
            "StampBrick" -> Block(Block.Type.PEN, listOf("stamp"))
            "ClearBackgroundBrick" -> Block(Block.Type.PEN, listOf("clear_background"))

            // ── Pen drawing (shapes) ──
            "DrawLineBrick" -> {
                val x1 = extractFormulaValue(el, "X1") ?: 0f
                val y1 = extractFormulaValue(el, "Y1") ?: 0f
                val x2 = extractFormulaValue(el, "X2") ?: 0f
                val y2 = extractFormulaValue(el, "Y2") ?: 0f
                Block(Block.Type.PEN, listOf("draw_line", x1, y1, x2, y2))
            }
            "DrawCircleBrick" -> {
                val cx = extractFormulaValue(el, "X") ?: 0f
                val cy = extractFormulaValue(el, "Y") ?: 0f
                val r = extractFormulaValue(el, "SIZE") ?: 10f
                Block(Block.Type.PEN, listOf("draw_circle", cx, cy, r))
            }
            "DrawRectBrick" -> {
                val rx = extractFormulaValue(el, "X") ?: 0f
                val ry = extractFormulaValue(el, "Y") ?: 0f
                val rw = extractFormulaValue(el, "WIDTH") ?: 50f
                val rh = extractFormulaValue(el, "HEIGHT") ?: 50f
                Block(Block.Type.PEN, listOf("draw_rect", rx, ry, rw, rh))
            }
            "DrawTextBrick" -> {
                val tx = extractFormulaValue(el, "X") ?: 0f
                val ty = extractFormulaValue(el, "Y") ?: 0f
                val txt = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.PEN, listOf("draw_text", tx, ty, txt))
            }
            "FillCircleBrick" -> {
                val cx = extractFormulaValue(el, "X") ?: 0f
                val cy = extractFormulaValue(el, "Y") ?: 0f
                val r = extractFormulaValue(el, "SIZE") ?: 10f
                Block(Block.Type.PEN, listOf("fill_circle", cx, cy, r))
            }
            "FillRectBrick" -> {
                val rx = extractFormulaValue(el, "X") ?: 0f
                val ry = extractFormulaValue(el, "Y") ?: 0f
                val rw = extractFormulaValue(el, "WIDTH") ?: 50f
                val rh = extractFormulaValue(el, "HEIGHT") ?: 50f
                Block(Block.Type.PEN, listOf("fill_rect", rx, ry, rw, rh))
            }
            "FillPolygonBrick" -> {
                val pointsStr = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.PEN, listOf("fill_polygon", pointsStr))
            }
            "SetCornerRadiusBrick" -> {
                val cr = extractFormulaValue(el, "CORNER") ?: 0f
                Block(Block.Type.PEN, listOf("set_corner_radius", cr))
            }
            "SetBorderWidthBrick" -> {
                val bw = extractFormulaValue(el, "WIDTH") ?: 1f
                Block(Block.Type.PEN, listOf("set_border_width", bw))
            }
            "SetBorderColorBrick" -> {
                val bc = extractFormulaValue(el, "COLOR") ?: 0f
                Block(Block.Type.PEN, listOf("set_border_color", bc))
            }

            // ═══════ CONTROL (leaf) ═══════
            "WaitBrick" -> {
                val secs = extractFormulaValue(el, "TIME_TO_WAIT_IN_SECONDS")
                val rf = getRuntimeFormula(el, "TIME_TO_WAIT_IN_SECONDS")
                val arg: Any = rf ?: (secs ?: 1f)
                Block(Block.Type.CONTROL, listOf("wait", arg))
            }
            "WaitUntilBrick" -> {
                val condFe = getFormulaElement(el, "IF_CONDITION")
                val condRf = if (condFe != null) RuntimeFormula("IF_CONDITION", condFe) else null
                Block(Block.Type.CONTROL, listOf("wait_until", condRf ?: 0f))
            }
            "NoteBrick" -> null // комментарий

            // ═══════ SENSING ═══════
            "ResetTimerBrick" -> Block(Block.Type.SENSING, listOf("reset_timer"))

            // ═══════ FILE ═══════
            "MoveFilesBrick" -> {
                val path = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.FILE, listOf("move", path))
            }
            "ZipBrick" -> {
                val zipName = extractFormulaString(el, "FILE") ?: ""
                val files = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.FILE, listOf("zip", zipName, files))
            }
            "UnzipBrick" -> {
                val zipPath = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.FILE, listOf("unzip", zipPath))
            }
            "CopyProjectFileBrick" -> {
                val srcName = extractFormulaString(el, "NAME") ?: ""
                val destName = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.FILE, listOf("copy_project_file", srcName, destName))
            }
            "ExportProjectFileBrick" -> {
                val fileName = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.FILE, listOf("export_file", fileName))
            }
            "DeleteFilesBrick" -> {
                val path = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.FILE, listOf("delete_file", path))
            }
            "OpenFileBrick" -> {
                val path = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.FILE, listOf("open_file", path))
            }
            "ReadFromFilesBrick" -> {
                val path = extractFormulaString(el, "VALUE") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.FILE, listOf("read_from_files", path, varName))
            }
            "WriteToFilesBrick" -> {
                val path = extractFormulaString(el, "VALUE") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.FILE, listOf("write_to_files", path, varName))
            }
            "ReadVariableFromFileBrick" -> {
                val path = extractFormulaString(el, "READ_FILENAME") ?: ""
                val varName = extractVariableName(el) ?: ""
                val deleteStr = el.getAttribute("spinnerSelectionID") ?: ""
                val deleteAfter = deleteStr == "1"
                Block(Block.Type.FILE, listOf("read_variable_from_file", path, varName, deleteAfter))
            }
            "WriteVariableToFileBrick" -> {
                val path = extractFormulaString(el, "WRITE_FILENAME") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.FILE, listOf("write_variable_to_file", path, varName))
            }
            "SaveToInternalStorageBrick" -> {
                val projectFileName = extractFormulaString(el, "PROJECT_FILE_NAME") ?: ""
                val fileName = extractFormulaString(el, "FILE_NAME") ?: ""
                Block(Block.Type.FILE, listOf("save_internal", projectFileName, fileName))
            }
            "LoadFromInternalStorageBrick" -> {
                val fileName = extractFormulaString(el, "FILE_NAME") ?: ""
                Block(Block.Type.FILE, listOf("load_internal", fileName))
            }
            "ExtractFileBrick" -> {
                val apkPath = extractFormulaString(el, "TEXT_1") ?: ""
                val innerPath = extractFormulaString(el, "TEXT_2") ?: ""
                val destPath = extractFormulaString(el, "TEXT_3") ?: ""
                Block(Block.Type.FILE, listOf("extract_file", apkPath, innerPath, destPath))
            }
            "GetZipFileNamesBrick" -> {
                val zipPath = extractFormulaString(el, "NAME") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.FILE, listOf("get_zip_names", zipPath, varName))
            }

            // ═══════ OTHER ═══════
            "FinishStageBrick" -> {
                stop()
                null
            }
            "ExitStageBrick" -> {
                stop()
                null
            }

            // ═══════ MOTION (remaining portable) ═══════
            "TurnLeftSpeedBrick" -> {
                val speed = extractFormulaValue(el, "PHYSICS_TURN_LEFT_SPEED") ?: 0f
                Block(Block.Type.MOTION, listOf("turn_left_speed", speed))
            }
            "TurnRightSpeedBrick" -> {
                val speed = extractFormulaValue(el, "PHYSICS_TURN_RIGHT_SPEED") ?: 0f
                Block(Block.Type.MOTION, listOf("turn_right_speed", speed))
            }
            "PointToBrick" -> {
                val destEl = getChildElement(el, "pointedObject")
                val destName = destEl?.getAttribute("name") ?: ""
                Block(Block.Type.MOTION, listOf("point_to", destName))
            }
            "SetVelocityBrick" -> {
                val vx = extractFormulaValue(el, "PHYSICS_VELOCITY_X") ?: 0f
                val vy = extractFormulaValue(el, "PHYSICS_VELOCITY_Y") ?: 0f
                Block(Block.Type.MOTION, listOf("set_velocity", vx, vy))
            }
            "MoveToObjectBrick" -> {
                val dist = extractFormulaValue(el, "SPEED") ?: 10f
                val target = extractFormulaString(el, "SPRITE") ?: ""
                Block(Block.Type.MOTION, listOf("move_to_object", dist, target))
            }
            "SetBounceBrick" -> {
                val bounce = extractFormulaValue(el, "PHYSICS_BOUNCE_FACTOR") ?: 0.5f
                Block(Block.Type.MOTION, listOf("set_bounce", bounce))
            }

            // ═══════ LOOKS (remaining portable) ═══════
            "SetRestitutionBrick" -> {
                val objectId = extractFormulaString(el, "VALUE_1") ?: ""
                val restitution = extractFormulaValue(el, "VALUE_2") ?: 0.5f
                Block(Block.Type.PHYSICS, listOf("set_restitution", objectId, restitution))
            }
            "SetRotationLockBrick" -> {
                val objectId = extractFormulaString(el, "STRING") ?: ""
                val lockX = if ((el.getElementsByTagName("brick_rotation_lock_x")?.length ?: 0) > 0) 1f else 0f
                val lockY = if ((el.getElementsByTagName("brick_rotation_lock_y")?.length ?: 0) > 0) 1f else 0f
                val lockZ = if ((el.getElementsByTagName("brick_rotation_lock_z")?.length ?: 0) > 0) 1f else 0f
                Block(Block.Type.PHYSICS, listOf("set_rotation_lock", objectId, lockX, lockY, lockZ))
            }
            "SetBackgroundBrick" -> {
                val lookEl = getChildElement(el, "look")
                val lookName = lookEl?.getAttribute("name") ?: ""
                Block(Block.Type.LOOKS, listOf("set_background", lookName, "0"))
            }
            "SetBackgroundAndWaitBrick" -> {
                val lookEl = getChildElement(el, "look")
                val lookName = lookEl?.getAttribute("name") ?: ""
                Block(Block.Type.LOOKS, listOf("set_background", lookName, "1"))
            }
            "SetLookFilesBrick" -> {
                val file = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.LOOKS, listOf("set_look_files", file))
            }
            "SaveLookBrick" -> {
                val fileName = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.LOOKS, listOf("save_look", fileName))
            }
            "CutLookBrick" -> {
                val x1 = extractFormulaValue(el, "X1") ?: 0f
                val y1 = extractFormulaValue(el, "Y1") ?: 0f
                val x2 = extractFormulaValue(el, "X2") ?: 0f
                val y2 = extractFormulaValue(el, "Y2") ?: 0f
                Block(Block.Type.LOOKS, listOf("cut_look", x1, y1, x2, y2))
            }
            "ResizeImgBrick" -> {
                val w = extractFormulaValue(el, "WIDTH") ?: 100f
                val h = extractFormulaValue(el, "HEIGHT") ?: 100f
                val file = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.LOOKS, listOf("resize_img", file, w, h))
            }
            "GrayscaleImgBrick" -> {
                val file = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.LOOKS, listOf("grayscale_img", file))
            }
            "NormalizeImgBrick" -> {
                val file = extractFormulaString(el, "FILE") ?: ""
                val r = extractFormulaValue(el, "RED") ?: 255f
                val g = extractFormulaValue(el, "GREEN") ?: 255f
                val b = extractFormulaValue(el, "BLUE") ?: 255f
                Block(Block.Type.LOOKS, listOf("normalize_img", file, r, g, b))
            }
            "SetAnimationSpeedBrick" -> {
                val speed = extractFormulaValue(el, "SPEED") ?: 1f
                val objId = extractFormulaString(el, "OBJECT_ID") ?: ""
                Block(Block.Type.LOOKS, listOf("set_anim_speed", objId, speed))
            }
            "PlayAnimationBrick" -> {
                val objId = extractFormulaString(el, "VALUE_1") ?: ""
                val animName = extractFormulaString(el, "VALUE_2") ?: ""
                val loops = extractFormulaValue(el, "VALUE_3") ?: 1f
                val speed = extractFormulaValue(el, "VALUE_4") ?: 1f
                val transition = extractFormulaValue(el, "VALUE_5") ?: 0f
                Block(Block.Type.LOOKS, listOf("play_anim", objId, animName, loops.toInt(), speed, transition))
            }
            "StopAnimationBrick" -> {
                val objId = extractFormulaString(el, "VALUE_1") ?: ""
                Block(Block.Type.LOOKS, listOf("stop_anim", objId))
            }
            "SetActiveBrick" -> {
                val objName = extractFormulaString(el, "OBJECT_NAME") ?: ""
                val activeStr = extractTextContent(el, "activeStateSelection") ?: "1"
                Block(Block.Type.LOOKS, listOf("set_active", objName, activeStr))
            }
            // ── Slide/Fade transitions ──
            "SlideDownBrick" -> {
                val sceneName = extractFormulaString(el, "TEXT_1") ?: ""
                Block(Block.Type.CONTROL, listOf("slide_scene", "down", sceneName))
            }
            "SlideUpBrick" -> {
                val sceneName = extractFormulaString(el, "TEXT_1") ?: ""
                Block(Block.Type.CONTROL, listOf("slide_scene", "up", sceneName))
            }
            "SlideLeftBrick" -> {
                val sceneName = extractFormulaString(el, "TEXT_1") ?: ""
                Block(Block.Type.CONTROL, listOf("slide_scene", "left", sceneName))
            }
            "SlideRightBrick" -> {
                val sceneName = extractFormulaString(el, "TEXT_1") ?: ""
                Block(Block.Type.CONTROL, listOf("slide_scene", "right", sceneName))
            }
            "FadeFromBlackBrick" -> {
                val sceneName = extractFormulaString(el, "TEXT_1") ?: ""
                Block(Block.Type.CONTROL, listOf("fade_scene", "from_black", sceneName))
            }
            "FadeToBlackBrick" -> {
                val sceneName = extractFormulaString(el, "TEXT_1") ?: ""
                Block(Block.Type.CONTROL, listOf("fade_scene", "to_black", sceneName))
            }
            "CrossfadeBrick" -> {
                val sceneName = extractFormulaString(el, "TEXT_1") ?: ""
                Block(Block.Type.CONTROL, listOf("crossfade_scene", sceneName))
            }

            // ═══════ SOUND (remaining portable) ═══════
            "StopSoundBrick2" -> {
                val instName = extractFormulaString(el, "INSTANCE_NAME") ?: ""
                Block(Block.Type.SOUND, listOf("stop_sound_v2", instName))
            }
            "PlaySoundAtBrick" -> {
                val sound = extractSoundName(el) ?: ""
                val param = extractFormulaValue(el, "PLAY_SOUND_AT") ?: 1f
                Block(Block.Type.SOUND, listOf("play_sound_at", sound, param))
            }
            "SetGlobalSoundVolumeBrick" -> {
                val vol = extractFormulaValue(el, "VOLUME") ?: 100f
                Block(Block.Type.SOUND, listOf("set_global_volume", vol / 100f))
            }
            "SetPanBrick" -> {
                val pan = extractFormulaValue(el, "CUSTOM_PARAM_1") ?: 0f
                Block(Block.Type.SOUND, listOf("set_pan", pan))
            }
            "PlayToneBrick" -> {
                val freq = extractFormulaValue(el, "CUSTOM_PARAM_1") ?: 440f
                val dur = extractFormulaValue(el, "DURATION") ?: 1f
                Block(Block.Type.SOUND, listOf("play_tone", freq, dur))
            }
            "PrepareSoundBrick" -> {
                val file = extractFormulaString(el, "FILE_NAME") ?: ""
                val cache = extractFormulaString(el, "CACHE_NAME") ?: ""
                Block(Block.Type.SOUND, listOf("prepare_sound", file, cache))
            }
            "PlayPreparedSoundBrick" -> {
                val cache = extractFormulaString(el, "CACHE_NAME") ?: ""
                Block(Block.Type.SOUND, listOf("play_prepared", cache))
            }
            "EqualizerSetBandBrick" -> {
                val band = extractFormulaValue(el, "CUSTOM_PARAM_1") ?: 0f
                val gain = extractFormulaValue(el, "CUSTOM_PARAM_2") ?: 0f
                Block(Block.Type.SOUND, listOf("eq_set_band", band.toInt(), gain))
            }
            "SetStopSoundsBrick" -> {
                val value = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.SOUND, listOf("set_stop_sounds", value))
            }
            "AudioFadeInBrick" -> {
                val dur = extractFormulaValue(el, "DURATION") ?: 2f
                Block(Block.Type.SOUND, listOf("audio_fade_in", dur))
            }
            "AudioFadeOutBrick" -> {
                val dur = extractFormulaValue(el, "DURATION") ?: 2f
                Block(Block.Type.SOUND, listOf("audio_fade_out", dur))
            }

            // ═══════ TEXT/BUBBLE (remaining portable) ═══════
            "ShowTextColorSizeAlignmentBrick" -> {
                val name = extractVariableName(el) ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                val size = extractFormulaValue(el, "SIZE") ?: 14f
                val color = extractFormulaString(el, "COLOR") ?: "0"
                val alignStr = extractTextContent(el, "alignmentSelection") ?: "0"
                Block(Block.Type.LOOKS, listOf("show_text_overlay", name, name, x, y, size, alignStr, color))
            }
            "ShowTextFontBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val text = extractFormulaString(el, "TEXT") ?: ""
                val font = extractFormulaString(el, "FILE") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                val size = extractFormulaValue(el, "SIZE") ?: 14f
                val color = extractFormulaString(el, "COLOR") ?: "0"
                Block(Block.Type.LOOKS, listOf("show_text_font", name, text, font, x, y, size, color))
            }
            "AskBrick" -> {
                val question = extractFormulaString(el, "ASK_QUESTION") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.LOOKS, listOf("ask", question, varName))
            }
            "CopyTextBrick" -> {
                val text = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.LOOKS, listOf("copy_text", text))
            }
            "ShowToastBrick" -> {
                val msg = extractFormulaString(el, "TOAST") ?: ""
                Block(Block.Type.LOOKS, listOf("show_toast", msg))
            }
            "SpeakBrick" -> {
                val text = extractFormulaString(el, "SPEAK") ?: ""
                Block(Block.Type.LOOKS, listOf("speak", text))
            }
            "SpeakAndWaitBrick" -> {
                val text = extractFormulaString(el, "SPEAK") ?: ""
                Block(Block.Type.LOOKS, listOf("speak_wait", text))
            }
            "SetGeminiKeyBrick" -> {
                val key = extractFormulaString(el, "KEY") ?: ""
                Block(Block.Type.LOOKS, listOf("set_gemini_key", key))
            }
            "AskGeminiBrick" -> {
                val question = extractFormulaString(el, "QUESTION") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.LOOKS, listOf("ask_gemini", question, varName))
            }
            "AskGemini2Brick" -> {
                val question = extractFormulaString(el, "QUESTION") ?: ""
                val model = extractFormulaString(el, "MODEL") ?: "gemini-pro"
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.LOOKS, listOf("ask_gemini2", question, model, varName))
            }
            "AskSpeechBrick" -> {
                val prompt = extractFormulaString(el, "ASK_SPEECH_QUESTION") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.LOOKS, listOf("ask_speech", prompt, varName))
            }

            // ═══════ WEB (remaining portable) ═══════
            "HeadWebRequestBrick" -> {
                val url = extractFormulaString(el, "URL") ?: ""
                val header = extractFormulaString(el, "HEADER") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.WEB, listOf("http_head", url, header, varName))
            }
            "OptionsWebRequestBrick" -> {
                val url = extractFormulaString(el, "URL") ?: ""
                val header = extractFormulaString(el, "HEADER") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.WEB, listOf("http_options", url, header, varName))
            }
            "PatchWebRequestBrick" -> {
                val url = extractFormulaString(el, "URL") ?: ""
                val header = extractFormulaString(el, "HEADER") ?: ""
                val body = extractFormulaString(el, "BODY") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.WEB, listOf("http_patch", url, header, body, varName))
            }
            "WebSocketConnectBrick" -> {
                val wsUrl = extractFormulaString(el, "WS_URL") ?: ""
                Block(Block.Type.WEB, listOf("ws_connect", wsUrl))
            }
            "WebSocketSendBrick" -> {
                val msg = extractFormulaString(el, "WS_MESSAGE") ?: ""
                Block(Block.Type.WEB, listOf("ws_send", msg))
            }
            "WebSocketCloseBrick" -> Block(Block.Type.WEB, listOf("ws_close"))
            "DownloadFileBrick" -> {
                val url = extractFormulaString(el, "URL") ?: ""
                val fileName = extractFormulaString(el, "DOWNLOAD_FILENAME") ?: ""
                Block(Block.Type.WEB, listOf("download_file", url, fileName))
            }
            "UploadFileBrick" -> {
                val url = extractFormulaString(el, "URL") ?: ""
                val file = extractFormulaString(el, "FILE") ?: ""
                val mime = extractFormulaString(el, "TEXT") ?: "application/octet-stream"
                val fileTypeStr = extractTextContent(el, "fileTypeSelection") ?: "0"
                val storageTypeStr = extractTextContent(el, "storageTypeSelection") ?: "0"
                Block(Block.Type.WEB, listOf("upload_file", url, file, mime, fileTypeStr, storageTypeStr))
            }
            "PingBrick" -> {
                val host = extractFormulaString(el, "PING_HOST") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.WEB, listOf("ping", host, varName))
            }
            "StartServerBrick" -> {
                val port = extractFormulaValue(el, "PORT") ?: 8080f
                Block(Block.Type.WEB, listOf("start_server", port.toInt()))
            }
            // ── Web extras (server / websocket / create / download-to-path) ──
            "CreateWebUrlBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val url = extractFormulaString(el, "URL") ?: ""
                Block(Block.Type.WEB, listOf("create_web_url", name, url))
            }
            "CreateWebFileBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val file = extractFormulaString(el, "HTML") ?: ""
                Block(Block.Type.WEB, listOf("create_web_file", name, file))
            }
            "DownloadToPathBrick" -> {
                val url = extractFormulaString(el, "URL") ?: ""
                val path = extractFormulaString(el, "DOWNLOAD_PATH") ?: ""
                Block(Block.Type.WEB, listOf("download_to_path", url, path))
            }
            "SetDnsBrick" -> {
                val value = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.WEB, listOf("set_dns", value))
            }
            "StopServerBrick" -> Block(Block.Type.WEB, listOf("stop_server"))
            "ConnectServerBrick" -> {
                val ip = extractFormulaString(el, "IP") ?: ""
                val port = extractFormulaValue(el, "PORT") ?: 8080f
                Block(Block.Type.WEB, listOf("connect_server", ip, port.toInt()))
            }
            "ListenServerBrick" -> {
                val name = extractVariableName(el) ?: ""
                Block(Block.Type.WEB, listOf("listen_server", name))
            }
            "WebSocketReceiveBrick" -> {
                val name = extractVariableName(el) ?: ""
                Block(Block.Type.WEB, listOf("ws_receive", name))
            }

            // ═══════ SENSING (remaining portable) ═══════
            "LockMouseBrick" -> Block(Block.Type.SENSING, listOf("lock_mouse"))
            "UnlockMouseBrick" -> Block(Block.Type.SENSING, listOf("unlock_mouse"))
            "ScreenShotBrick" -> Block(Block.Type.SENSING, listOf("screenshot"))
            "TapAtBrick" -> {
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.SENSING, listOf("tap_at", x, y))
            }

            // ═══════ CONTROL leaf (remaining portable) ═══════
            "WaitTillIdleBrick" -> Block(Block.Type.CONTROL, listOf("wait_till_idle"))
            "CloneObjectBrick" -> {
                val src = extractFormulaString(el, "VALUE_1") ?: ""
                val name = extractFormulaString(el, "VALUE_2") ?: ""
                Block(Block.Type.CONTROL, listOf("clone_object", src, name))
            }

            // ═══════ FILE (remaining portable) ═══════
            "CreateFolderBrick" -> {
                val folder = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.FILE, listOf("create_folder", folder))
            }
            "DeleteFolderBrick" -> {
                val folder = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.FILE, listOf("delete_folder", folder))
            }
            "CreateFolderByPathBrick" -> {
                val path = extractFormulaString(el, "PATH") ?: ""
                val folder = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.FILE, listOf("create_folder_path", path, folder))
            }
            "DeleteFolderByPathBrick" -> {
                val path = extractFormulaString(el, "PATH") ?: ""
                val folder = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.FILE, listOf("delete_folder_path", path, folder))
            }
            "PutFileIntoFolderBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val folder = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.FILE, listOf("put_file_into_folder", name, folder))
            }
            "PutFileIntoPathBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val path = extractFormulaString(el, "FILE_URL") ?: ""
                Block(Block.Type.FILE, listOf("put_file_into_path", name, path))
            }
            "CopyProjectFileToFolderBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val folder = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.FILE, listOf("copy_to_folder", name, folder))
            }
            "CopyProjectFileToPathBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val path = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.FILE, listOf("copy_to_path", name, path))
            }
            "MoveDownloadsBrick" -> {
                val file = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.FILE, listOf("move_downloads", file))
            }
            "OpenFilesBrick" -> {
                val file = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.FILE, listOf("open_files", file))
            }

            // ═══════ DATA STRUCTURES ═══════
            "MapCreateBrick" -> {
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.VARIABLE, listOf("map_create", varName))
            }
            "MapSetBrick" -> {
                val varName = extractVariableName(el) ?: ""
                val key = extractFormulaString(el, "KEY") ?: ""
                val value = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.VARIABLE, listOf("map_set", varName, key, value))
            }
            "MapGetBrick" -> {
                val varName = extractVariableName(el) ?: ""
                val key = extractFormulaString(el, "KEY") ?: ""
                Block(Block.Type.VARIABLE, listOf("map_get", varName, key))
            }
            "MapDeleteBrick" -> {
                val varName = extractVariableName(el) ?: ""
                val key = extractFormulaString(el, "KEY") ?: ""
                Block(Block.Type.VARIABLE, listOf("map_delete", varName, key))
            }
            "QueueEnqueueBrick" -> {
                val varName = extractVariableName(el) ?: ""
                val value = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.VARIABLE, listOf("queue_enqueue", varName, value))
            }
            "QueueDequeueBrick" -> {
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.VARIABLE, listOf("queue_dequeue", varName))
            }
            "StackPushBrick" -> {
                val varName = extractVariableName(el) ?: ""
                val value = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.VARIABLE, listOf("stack_push", varName, value))
            }
            "StackPopBrick" -> {
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.VARIABLE, listOf("stack_pop", varName))
            }

            // ═══════ SCENE ═══════
            "LoadSceneBrick" -> {
                val sceneName = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.CONTROL, listOf("load_scene", sceneName))
            }
            "ClearSceneBrick" -> {
                val sceneName = extractTextContent(el, "sceneToStart") ?: ""
                Block(Block.Type.CONTROL, listOf("clear_scene", sceneName))
            }
            "SceneTransitionBrick" -> {
                val sceneName = extractTextContent(el, "sceneForTransition") ?: ""
                Block(Block.Type.CONTROL, listOf("scene_transition", sceneName))
            }

            // ═══════ AI/SCRIPT ═══════
            "RunShellBrick" -> {
                val code = extractFormulaString(el, "CODE") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.CONTROL, listOf("run_shell", code, varName))
            }

            // ═══════ EVENT trigger bricks (stored as scripts, only parsed as leaf for When* bricks used inline) ═══════
            "BroadcastMessageBrick" -> {
                val msg = extractTextContent(el, "broadcastMessage") ?: ""
                Block(Block.Type.EVENT, listOf("broadcast_msg", msg))
            }
            "SceneStartBrick" -> {
                val sceneName = extractTextContent(el, "sceneToStart") ?: ""
                Block(Block.Type.EVENT, listOf("scene_start", sceneName))
            }

            // ═══════ PHYSICS bricks (Box2D) ═══════
            "SetGravityBrick" -> {
                val gx = extractFormulaValue(el, "PHYSICS_GRAVITY_X") ?: 0f
                val gy = extractFormulaValue(el, "PHYSICS_GRAVITY_Y") ?: -9.8f
                Block(Block.Type.PHYSICS, listOf("set_gravity", gx, gy))
            }
            "SetFrictionBrick" -> {
                val friction = extractFormulaValue(el, "PHYSICS_FRICTION") ?: 0.5f
                Block(Block.Type.PHYSICS, listOf("set_friction", friction))
            }
            "SetMassBrick" -> {
                val mass = extractFormulaValue(el, "PHYSICS_MASS") ?: 1f
                Block(Block.Type.PHYSICS, listOf("set_mass", mass))
            }
            "SetDampingBrick" -> {
                val linear = extractFormulaValue(el, "LINEAR_DAMPING") ?: 0f
                val angular = extractFormulaValue(el, "ANGULAR_DAMPING") ?: 0f
                Block(Block.Type.PHYSICS, listOf("set_damping", linear, angular))
            }
            "SetPhysicsObjectTypeBrick" -> {
                val typeStr = extractTextContent(el, "type") ?: "0"
                val typeVal = typeStr.toIntOrNull() ?: 0
                Block(Block.Type.PHYSICS, listOf("set_physics_type", typeVal))
            }
            "SetPhysicsStateBrick" -> {
                val objectId = extractFormulaValue(el, "VALUE_1") ?: 0f
                val mass = extractFormulaValue(el, "VALUE_2") ?: 1f
                val stateSel = extractTextContent(el, "stateSelection") ?: "0"
                val shapeSel = extractTextContent(el, "shapeSelection") ?: "0"
                Block(Block.Type.PHYSICS, listOf("set_physics_state", objectId, mass,
                    stateSel.toIntOrNull() ?: 0, shapeSel.toIntOrNull() ?: 0))
            }
            "ApplyForceBrick" -> {
                val fx = extractFormulaValue(el, "FORCE_X") ?: 0f
                val fy = extractFormulaValue(el, "FORCE_Y") ?: 0f
                Block(Block.Type.PHYSICS, listOf("apply_force", fx, fy))
            }
            "ApplyImpulseBrick" -> {
                val ix = extractFormulaValue(el, "IMPULSE_X") ?: 0f
                val iy = extractFormulaValue(el, "IMPULSE_Y") ?: 0f
                Block(Block.Type.PHYSICS, listOf("apply_impulse", ix, iy))
            }
            "ApplyTorqueBrick" -> {
                val torque = extractFormulaValue(el, "TORQUE") ?: 0f
                Block(Block.Type.PHYSICS, listOf("apply_torque", torque))
            }
            "ApplyAngularImpulseBrick" -> {
                val impulse = extractFormulaValue(el, "ANGULAR_IMPULSE") ?: 0f
                Block(Block.Type.PHYSICS, listOf("apply_angular_impulse", impulse))
            }
            "PerformRayCastBrick" -> {
                val rayId = extractFormulaValue(el, "RAY_ID") ?: 0f
                val sx = extractFormulaValue(el, "X_START") ?: 0f
                val sy = extractFormulaValue(el, "Y_START") ?: 0f
                val ex = extractFormulaValue(el, "X_END") ?: 0f
                val ey = extractFormulaValue(el, "Y_END") ?: 0f
                Block(Block.Type.PHYSICS, listOf("ray_cast", rayId, sx, sy, ex, ey))
            }
            "CreateRevoluteJointBrick" -> {
                val name = extractFormulaString(el, "JOINT_ID") ?: ""
                val sprite2 = extractFormulaString(el, "SPRITE") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("create_joint_revolute", name, sprite2, x, y))
            }
            "CreatePrismaticJointBrick" -> {
                val name = extractFormulaString(el, "JOINT_ID") ?: ""
                val sprite2 = extractFormulaString(el, "SPRITE") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                val ax = extractFormulaValue(el, "AXIS_X") ?: 1f
                val ay = extractFormulaValue(el, "AXIS_Y") ?: 0f
                Block(Block.Type.PHYSICS, listOf("create_joint_prismatic", name, sprite2, x, y, ax, ay))
            }
            "CreateWeldJointBrick" -> {
                val name = extractFormulaString(el, "JOINT_ID") ?: ""
                val sprite2 = extractFormulaString(el, "SPRITE") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("create_joint_weld", name, sprite2, x, y))
            }
            "CreateDistanceJointBrick" -> {
                val name = extractFormulaString(el, "JOINT_ID") ?: ""
                val sprite2 = extractFormulaString(el, "SPRITE") ?: ""
                val len = extractFormulaValue(el, "JOINT_LENGTH") ?: 1f
                val freq = extractFormulaValue(el, "JOINT_FREQUENCY") ?: 0f
                val damp = extractFormulaValue(el, "JOINT_DAMPING") ?: 0f
                Block(Block.Type.PHYSICS, listOf("create_joint_distance", name, sprite2, len, freq, damp))
            }
            "DestroyJointBrick" -> {
                val name = extractFormulaString(el, "JOINT_ID") ?: ""
                Block(Block.Type.PHYSICS, listOf("destroy_joint", name))
            }
            "RemoveJointBrick" -> {
                val name = extractFormulaString(el, "JOINT_NAME") ?: ""
                Block(Block.Type.PHYSICS, listOf("destroy_joint", name))
            }

            // ═══════ KEY / MOUSE event bricks ═══════
            "KeyEventBrick" -> {
                val keyChar = extractFormulaString(el, "VALUE") ?: ""
                val keyDown = extractFormulaValue(el, "VM_KEY_DOWN") ?: 0f
                Block(Block.Type.CONTROL, listOf("key_event", keyChar, keyDown))
            }
            "MouseEventBrick" -> {
                val mx = extractFormulaValue(el, "X_POSITION") ?: 0f
                val my = extractFormulaValue(el, "Y_POSITION") ?: 0f
                val state = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.CONTROL, listOf("mouse_event", mx, my, state))
            }

            // ═══════ VARIABLE management bricks ═══════
            "CreateVarBrick" -> {
                val varName = extractVariableName(el) ?: ""
                val value = extractFormulaValue(el, "VARIABLE") ?: 0f
                Block(Block.Type.VARIABLE, listOf("create_var", varName, value))
            }
            "DeleteVarBrick" -> {
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.VARIABLE, listOf("delete_var", varName))
            }
            "DeleteVarsBrick" -> Block(Block.Type.VARIABLE, listOf("delete_all_vars"))

            // ═══════ TEXT bricks ═══════
            "SetTextBrick" -> {
                val overlayName = extractFormulaString(el, "NAME") ?: ""
                val text = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.LOOKS, listOf("set_text", overlayName, text))
            }
            "CreateTextFieldBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val text = extractFormulaString(el, "TEXT") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.LOOKS, listOf("create_text_field", name, text, x, y))
            }

            // ═══════ DEVICE bricks (stubs or portable) ═══════
            "VibrationBrick" -> {
                val ms = extractFormulaValue(el, "VIBRATE_DURATION_IN_SECONDS") ?: 0.5f
                Block(Block.Type.SENSING, listOf("vibrate", ms))
            }
            "KeepScreenOnBrick" -> Block(Block.Type.SENSING, listOf("keep_screen_on"))
            "KeepScreenOffBrick" -> Block(Block.Type.SENSING, listOf("keep_screen_off"))
            "ScreenBrightnessBrick" -> {
                val brightness = extractFormulaValue(el, "VALUE") ?: 1f
                Block(Block.Type.SENSING, listOf("screen_brightness", brightness))
            }
            // ═══════ NOTIFICATION bricks ═══════
            "SendNotificationBrick" -> {
                val notifId = extractFormulaValue(el, "NOTIFICATION_ID") ?: 1f
                Block(Block.Type.CONTROL, listOf("send_notification", notifId.toInt()))
            }
            "ShowScheduledNotificationBrick" -> {
                val id = extractFormulaValue(el, "NOTIFICATION_ID") ?: 0f
                val delay = extractFormulaValue(el, "DURATION_IN_SECONDS") ?: 0f
                Block(Block.Type.CONTROL, listOf("show_scheduled_notification", id.toInt(), delay))
            }
            "PrepareNotificationBrick" -> {
                val id = extractFormulaValue(el, "NOTIFICATION_ID") ?: 0f
                val channel = extractFormulaString(el, "NOTIFICATION_CHANNEL") ?: "default"
                val title = extractFormulaString(el, "NOTIFICATION_TITLE") ?: ""
                val text = extractFormulaString(el, "NOTIFICATION_TEXT") ?: ""
                val icon = extractFormulaString(el, "NOTIFICATION_ICON") ?: ""
                Block(Block.Type.CONTROL, listOf("prepare_notification", id.toInt(), channel, title, text, icon))
            }
            "NotificationActionBrick" -> {
                val id = (extractFormulaValue(el, "NOTIFICATION_ID") ?: 0f).toInt()
                val actionId = extractFormulaString(el, "NOTIFICATION_ACTION_ID") ?: ""
                val text = extractFormulaString(el, "TEXT") ?: ""
                val icon = extractFormulaString(el, "NOTIFICATION_ACTION_ICON") ?: ""
                val hint = extractFormulaString(el, "NOTIFICATION_ACTION_HINT") ?: ""
                Block(Block.Type.CONTROL, listOf("notification_action", id, actionId, text, icon, hint))
            }
            "RemoveNotificationBrick" -> {
                val id = (extractFormulaValue(el, "NOTIFICATION_ID") ?: 0f).toInt()
                Block(Block.Type.CONTROL, listOf("remove_notification", id))
            }
            "EnableBackgroundBrick" -> {
                val id = (extractFormulaValue(el, "NOTIFICATION_ID") ?: 0f).toInt()
                val channel = extractFormulaString(el, "NOTIFICATION_CHANNEL") ?: "default"
                val title = extractFormulaString(el, "NOTIFICATION_TITLE") ?: ""
                val text = extractFormulaString(el, "NOTIFICATION_TEXT") ?: ""
                val icon = extractFormulaString(el, "NOTIFICATION_ICON") ?: ""
                Block(Block.Type.CONTROL, listOf("enable_background", id, channel, title, text, icon))
            }

            // ═══════ Core game logic (remaining portable) ═══════
            "CloneAndNameBrick" -> {
                val name = extractFormulaString(el, "CLONE_NAME") ?: ""
                Block(Block.Type.CONTROL, listOf("clone_and_name", name))
            }
            "DeleteCloneByNumberBrick" -> {
                val n = extractFormulaValue(el, "NUMBER") ?: 0f
                Block(Block.Type.CONTROL, listOf("delete_clone_by_number", n.toInt()))
            }
            "TimerResetBrick" -> Block(Block.Type.SENSING, listOf("reset_timer"))
            "TimerStartBrick" -> Block(Block.Type.CONTROL, listOf("timer_start"))
            "TimerStopBrick" -> Block(Block.Type.CONTROL, listOf("timer_stop"))
            "SetParentBrick" -> {
                val child = extractFormulaString(el, "CHILD_OBJECT") ?: ""
                val parent = extractFormulaString(el, "PARENT_OBJECT") ?: ""
                Block(Block.Type.CONTROL, listOf("set_parent", child, parent))
            }
            "RemoveParentBrick" -> {
                val child = extractFormulaString(el, "CHILD_OBJECT") ?: ""
                Block(Block.Type.CONTROL, listOf("remove_parent", child))
            }
            "StopBackgroundBrick" -> Block(Block.Type.CONTROL, listOf("stop_background"))
            "LoadSceneAdditiveBrick" -> {
                val scene = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.CONTROL, listOf("load_scene_additive", scene))
            }
            "PreloadSceneBrick" -> Block(Block.Type.CONTROL, listOf("preload_scene"))
            "CastRayBrick" -> {
                val x1 = extractFormulaValue(el, "VALUE_1") ?: 0f
                val y1 = extractFormulaValue(el, "VALUE_2") ?: 0f
                val x2 = extractFormulaValue(el, "VALUE_3") ?: 0f
                val y2 = extractFormulaValue(el, "VALUE_4") ?: 0f
                val dx = extractFormulaValue(el, "VALUE_5") ?: 0f
                val dy = extractFormulaValue(el, "VALUE_6") ?: 0f
                val maxd = extractFormulaValue(el, "VALUE_7") ?: 1000f
                Block(Block.Type.CONTROL, listOf("cast_ray", x1, y1, x2, y2, dx, dy, maxd))
            }
            "ShowTextRotationBrick" -> {
                val name = extractVariableName(el) ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                val rot = extractFormulaValue(el, "COLOR") ?: 0f
                Block(Block.Type.VARIABLE, listOf("show_text_rotation", name, x, y, rot))
            }
            "ShowVarFontBrick" -> {
                val name = extractVariableName(el) ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                val size = extractFormulaValue(el, "SIZE") ?: 14f
                Block(Block.Type.VARIABLE, listOf("show_var_font", name, x, y, size))
            }

            // ═══════ Network / File (remaining portable) ═══════
            "CancelDownloadBrick" -> Block(Block.Type.WEB, listOf("cancel_download"))
            "SendServerBrick" -> {
                val value = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.WEB, listOf("send_server", value))
            }
            "FileUrlBrick" -> {
                val file = extractFormulaString(el, "FILE") ?: ""
                val url = extractFormulaString(el, "FILE_URL") ?: ""
                Block(Block.Type.WEB, listOf("file_url", file, url))
            }
            "FilesUrlBrick" -> {
                val file = extractFormulaString(el, "FILE") ?: ""
                val url = extractFormulaString(el, "FILE_URL") ?: ""
                Block(Block.Type.WEB, listOf("files_url", file, url))
            }
            "HasPathBrick" -> {
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.FILE, listOf("has_path", varName))
            }
            "PutFloatBrick" -> {
                val name = extractFormulaString(el, "FLOAT_ARRAY") ?: ""
                val idx = extractFormulaValue(el, "LOOK_INDEX") ?: 0f
                val value = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.FILE, listOf("put_float", name, idx.toInt(), value))
            }

            // ═══════ Database tables (remaining portable) ═══════
            "CreateTableBrick" -> {
                val sx = extractFormulaValue(el, "SIZE_X") ?: 1f
                val sy = extractFormulaValue(el, "SIZE_Y") ?: 1f
                val tname = extractFormulaString(el, "TABLE_NAME") ?: ""
                Block(Block.Type.VARIABLE, listOf("db_create", tname, sx.toInt(), sy.toInt()))
            }
            "DeleteAllTablesBrick" -> Block(Block.Type.VARIABLE, listOf("db_delete_all"))
            "DeleteBaseBrick" -> {
                val id = extractFormulaString(el, "FIREBASE_ID") ?: ""
                val key = extractFormulaString(el, "FIREBASE_KEY") ?: ""
                Block(Block.Type.VARIABLE, listOf("db_delete_base", id, key))
            }
            "DeleteTableBrick" -> {
                val tname = extractFormulaString(el, "TABLE_NAME") ?: ""
                Block(Block.Type.VARIABLE, listOf("db_delete_table", tname))
            }
            "InsertTableBrick" -> {
                val sx = extractFormulaValue(el, "SIZE_X") ?: 0f
                val sy = extractFormulaValue(el, "SIZE_Y") ?: 0f
                val tname = extractFormulaString(el, "TABLE_NAME") ?: ""
                val value = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.VARIABLE, listOf("db_insert", tname, sx.toInt(), sy.toInt(), value))
            }
            "LookFromTableBrick" -> {
                val alpha = extractFormulaValue(el, "ALPHA") ?: 0f
                val blue = extractFormulaValue(el, "BLUE") ?: 0f
                val green = extractFormulaValue(el, "GREEN") ?: 0f
                val red = extractFormulaValue(el, "RED") ?: 0f
                Block(Block.Type.VARIABLE, listOf("db_look_from", red.toInt(), green.toInt(), blue.toInt(), alpha.toInt()))
            }
            "LookToTableBrick" -> {
                val alpha = extractFormulaValue(el, "ALPHA") ?: 0f
                val blue = extractFormulaValue(el, "BLUE") ?: 0f
                val green = extractFormulaValue(el, "GREEN") ?: 0f
                val red = extractFormulaValue(el, "RED") ?: 0f
                Block(Block.Type.VARIABLE, listOf("db_look_to", red.toInt(), green.toInt(), blue.toInt(), alpha.toInt()))
            }
            "ReadBaseBrick" -> {
                val id = extractFormulaString(el, "FIREBASE_ID") ?: ""
                val key = extractFormulaString(el, "FIREBASE_KEY") ?: ""
                val name = extractVariableName(el) ?: ""
                Block(Block.Type.VARIABLE, listOf("db_read_base", id, key, name))
            }
            "StringToTableBrick" -> {
                val str = extractFormulaString(el, "STRING") ?: ""
                val tname = extractFormulaString(el, "TABLE_NAME") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.VARIABLE, listOf("db_string_to", tname, str, x.toInt(), y.toInt()))
            }
            "TableToFloatBrick" -> {
                val name = extractFormulaString(el, "FLOAT_ARRAY") ?: ""
                val tname = extractFormulaString(el, "TABLE_NAME") ?: ""
                Block(Block.Type.VARIABLE, listOf("db_table_to_float", tname, name))
            }
            "WriteBaseBrick" -> {
                val id = extractFormulaString(el, "FIREBASE_ID") ?: ""
                val key = extractFormulaString(el, "FIREBASE_KEY") ?: ""
                val value = extractFormulaString(el, "FIREBASE_VALUE") ?: ""
                Block(Block.Type.VARIABLE, listOf("db_write_base", id, key, value))
            }

            // ═══════ Fast2D (remaining portable) ═══════
            "Fast2DCreateBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.PHYSICS, listOf("fast2d_create", name))
            }
            "Fast2DDeleteBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.PHYSICS, listOf("fast2d_delete", name))
            }
            "Fast2DMakePhysicsBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val rot = extractFormulaValue(el, "ROTATION") ?: 0f
                val size = extractFormulaValue(el, "SIZE") ?: 100f
                val tex = extractFormulaString(el, "STRING") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_make_physics", name, x, y, rot, size, tex))
            }
            "Fast2DSetAngularVelocityBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val rot = extractFormulaValue(el, "ROTATION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_angular_vel", name, rot))
            }
            "Fast2DSetCollisionFilterBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val cat = extractFormulaValue(el, "X_POSITION") ?: 0f
                val mask = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_collision_filter", name, cat.toInt(), mask.toInt()))
            }
            "Fast2DSetColorBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val rot = extractFormulaValue(el, "ROTATION") ?: 0f
                val size = extractFormulaValue(el, "SIZE") ?: 100f
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_set_color", name, x, y, rot, size))
            }
            "Fast2DSetPhysicsVelocityBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_phys_vel", name, x, y))
            }
            "Fast2DSetPositionBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_set_position", name, x, y))
            }
            "Fast2DSetRotationBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val rot = extractFormulaValue(el, "ROTATION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_set_rotation", name, rot))
            }
            "Fast2DSetScaleBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val sx = extractFormulaValue(el, "X_SCALE") ?: 1f
                val sy = extractFormulaValue(el, "Y_SCALE") ?: 1f
                Block(Block.Type.PHYSICS, listOf("fast2d_set_scale", name, sx, sy))
            }
            "Fast2DSetTextureBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val tex = extractFormulaString(el, "STRING") ?: ""
                Block(Block.Type.PHYSICS, listOf("fast2d_set_texture", name, tex))
            }
            "Fast2DSetVelocityBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_set_velocity", name, x, y))
            }
            "Fast2DApplyForceBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_apply_force", name, x, y))
            }
            "Fast2DApplyImpulseBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_apply_impulse", name, x, y))
            }
            "Fast2DSetZIndexBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val z = extractFormulaValue(el, "VIBRATE_DURATION") ?: 0f
                Block(Block.Type.PHYSICS, listOf("fast2d_set_zindex", name, z.toInt()))
            }

            // ═══════ Video (remaining portable) ═══════
            "CreateVideoBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val file = extractFormulaString(el, "FILE") ?: ""
                val x = extractFormulaValue(el, "POSX") ?: 0f
                val y = extractFormulaValue(el, "POSY") ?: 0f
                val w = extractFormulaValue(el, "WIDTH") ?: 0f
                val h = extractFormulaValue(el, "HEIGHT") ?: 0f
                val looped = (extractFormulaValue(el, "LOOPED") ?: 0f) != 0f
                Block(Block.Type.VIDEO, listOf("create_video", name, file, x, y, w, h, if (looped) 1 else 0))
            }
            "PlayVideoBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.VIDEO, listOf("play_video", name))
            }
            "PauseVideoBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.VIDEO, listOf("pause_video", name))
            }
            "SeekVideoBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val t = extractFormulaValue(el, "TIME") ?: 0f
                Block(Block.Type.VIDEO, listOf("seek_video", name, t))
            }

            // ═══════ Editor / AI / UI (remaining portable) ═══════
            // ── Portable camera / project / utility bricks ──
            "SetViewPositionBrick" -> {
                val viewId = extractFormulaString(el, "VIEW_ID") ?: ""
                val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.CAMERA, listOf("set_view_position", viewId, x, y))
            }
            "SetFreeCameraBrick" -> Block(Block.Type.CAMERA, listOf("set_free_camera"))
            "SetThirdPersonCameraBrick" -> {
                val objectId = extractFormulaString(el, "OBJECT_ID") ?: ""
                val distance = extractFormulaValue(el, "DISTANCE") ?: 0f
                val height = extractFormulaValue(el, "HEIGHT") ?: 0f
                val pitch = extractFormulaValue(el, "PITCH") ?: 0f
                Block(Block.Type.CAMERA, listOf("set_third_person_camera", objectId, distance, height, pitch))
            }
            "CameraLookAtBrick" -> {
                val x = extractFormulaValue(el, "VALUE") ?: 0f
                val y = extractFormulaValue(el, "VALUE_2") ?: 0f
                val z = extractFormulaValue(el, "VALUE_3") ?: 0f
                Block(Block.Type.CAMERA, listOf("camera_look_at", x, y, z))
            }
            "CameraSettingsBrick" -> {
                val fov = extractFormulaValue(el, "FOV") ?: 0f
                val shakeInt = extractFormulaValue(el, "INTENSITY") ?: 0f
                val shakeDur = extractFormulaValue(el, "DURATION") ?: 0f
                Block(Block.Type.CAMERA, listOf("camera_settings", fov, shakeInt, shakeDur))
            }
            "CameraTouchControlBrick" -> {
                val enabled = extractFormulaValue(el, "ENABLED") ?: 0f
                val sensitivity = extractFormulaValue(el, "SENSITIVITY") ?: 0f
                val x = extractFormulaValue(el, "X") ?: 0f
                val y = extractFormulaValue(el, "Y") ?: 0f
                val w = extractFormulaValue(el, "WIDTH") ?: 0f
                val h = extractFormulaValue(el, "HEIGHT") ?: 0f
                Block(Block.Type.CAMERA, listOf("camera_touch_control", enabled, sensitivity, x, y, w, h))
            }
            "SetCameraRangeBrick" -> {
                val near = extractFormulaValue(el, "NEAR") ?: 0f
                val far = extractFormulaValue(el, "FAR") ?: 0f
                Block(Block.Type.CAMERA, listOf("set_camera_range", near, far))
            }
            "CameraTrackingBrick" -> Block(Block.Type.CAMERA, listOf("camera_tracking"))
            "SetCameraFocusPointBrick" -> Block(Block.Type.CAMERA, listOf("camera_focus"))
            "ClearCanvasBrick" -> Block(Block.Type.PEN, listOf("clear_canvas"))
            "SetFpsBrick" -> {
                val fps = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.CONTROL, listOf("set_fps", fps))
            }
            "SetRenderResolutionBrick" -> {
                val scale = extractFormulaValue(el, "NAME") ?: 1f
                val mode = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.CONTROL, listOf("set_render_resolution", scale, mode))
            }
            "LaunchProjectBrick" -> {
                val name = extractFormulaString(el, "PROJECT_NAME") ?: ""
                Block(Block.Type.CONTROL, listOf("launch_project", name))
            }
            "ReturnToPreviousProjectBrick" -> Block(Block.Type.CONTROL, listOf("return_previous_project"))
            "AddEditBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val text = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.CONTROL, listOf("add_edit", name, text))
            }
            "AddRadioBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val text = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.CONTROL, listOf("add_radio", name, text))
            }
            "SetAIBrick" -> {
                val avoid = extractFormulaValue(el, "AVOID_OBSTACLES") ?: 0f
                val dist = extractFormulaValue(el, "DISTANCE") ?: 0f
                val mode = extractFormulaValue(el, "MODE") ?: 0f
                val objId = extractFormulaString(el, "OBJECT_ID") ?: ""
                val range = extractFormulaValue(el, "RANGE") ?: 0f
                val speed = extractFormulaValue(el, "SPEED") ?: 0f
                val step = extractFormulaValue(el, "STEP_HEIGHT") ?: 0f
                val target = extractFormulaString(el, "TARGET") ?: ""
                Block(Block.Type.CONTROL, listOf("set_ai", objId, mode.toInt(), speed, dist, range, avoid, step, target))
            }
            "CreateBufferBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val width = extractFormulaValue(el, "WIDTH") ?: 0f
                val height = extractFormulaValue(el, "HEIGHT") ?: 0f
                Block(Block.Type.CONTROL, listOf("create_buffer", name, width.toInt(), height.toInt()))
            }
            "AddToBufferBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.CONTROL, listOf("add_to_buffer", name))
            }
            "RemoveFromBufferBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.CONTROL, listOf("remove_from_buffer", name))
            }
            "SaveBufferBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val file = extractFormulaString(el, "STRING_VALUE") ?: ""
                Block(Block.Type.CONTROL, listOf("save_buffer", name, file))
            }
            "ApplyBufferLookBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.CONTROL, listOf("apply_buffer_look", name))
            }
            "SetBufferAutoUpdateBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val state = extractFormulaValue(el, "TIME") ?: 0f
                Block(Block.Type.CONTROL, listOf("set_buffer_auto_update", name, state))
            }
            "SetBufferModeBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val r2d = extractFormulaValue(el, "X_POSITION") ?: 0f
                val r3d = extractFormulaValue(el, "Y_POSITION") ?: 0f
                Block(Block.Type.CONTROL, listOf("set_buffer_mode", name, r2d, r3d))
            }
            "SetBufferOnlyBrick" -> {
                val state = extractFormulaValue(el, "TIME") ?: 0f
                Block(Block.Type.CONTROL, listOf("set_buffer_only", state))
            }
            "GridBrick" -> {
                val x = extractFormulaValue(el, "POSX") ?: 0f
                val y = extractFormulaValue(el, "POSY") ?: 0f
                val w = extractFormulaValue(el, "SIZE_X") ?: 0f
                val h = extractFormulaValue(el, "SIZE_Y") ?: 0f
                Block(Block.Type.CONTROL, listOf("grid", x, y, w, h))
            }
            "SoundFileBrick" -> {
                val file = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.SOUND, listOf("sound_file", file))
            }
            "SoundFilesBrick" -> {
                val file = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.SOUND, listOf("sound_files", file))
            }
            "Sound_StopAllBrick" -> Block(Block.Type.SOUND, listOf("stop_all_sounds"))
            "DelayMicrosecondsBrick" -> {
                val t = extractFormulaValue(el, "TIME_TO_WAIT_IN_SECONDS") ?: 0f
                Block(Block.Type.CONTROL, listOf("delay", t))
            }

            // ═══════ CLONE bricks ═══════
            "WhenStartedBrick" -> Block(Block.Type.EVENT, listOf("green_flag"))
            "WhenTouchDownBrick" -> Block(Block.Type.EVENT, listOf("touch_down"))
            "WhenMouseButtonClickedBrick" -> Block(Block.Type.EVENT, listOf("mouse_clicked"))
            "WhenMouseWheelScrolledBrick" -> Block(Block.Type.EVENT, listOf("mouse_wheel"))
            "WhenGamepadButtonBrick" -> Block(Block.Type.EVENT, listOf("gamepad_button"))
            "BroadcastReceiverBrick" -> {
                val msg = extractMessageText(el, "broadcastMessage")
                Block(Block.Type.EVENT, listOf("broadcast_receiver", msg))
            }
            "WhenClonedBrick" -> Block(Block.Type.EVENT, listOf("cloned"))

            // ═══════ Firebase event brick ═══════
            "WhenFirebaseChangedBrick" -> Block(Block.Type.EVENT, listOf("firebase_changed"))

            // ═══════ Camera HW bricks (no-op on desktop) ═══════
            "CameraBrick" -> {
                val onText = getTagText(el, "spinnerSelectionON")
                val on = onText != null && (onText.equals("true", ignoreCase = true) || onText == "1")
                Block(Block.Type.CAMERA, listOf("camera_preview", if (on) 1 else 0))
            }
            "ChooseCameraBrick" -> {
                val frontText = getTagText(el, "spinnerSelectionFRONT")
                val front = frontText != null && (frontText.equals("true", ignoreCase = true) || frontText == "1")
                Block(Block.Type.CAMERA, listOf("camera_choose", if (front) 1 else 0))
            }
            "FlashBrick" -> {
                val id = getTagText(el, "spinnerSelectionID")?.toIntOrNull() ?: 0
                Block(Block.Type.CAMERA, listOf("camera_flash", id))
            }
            "PhotoBrick" -> Block(Block.Type.CAMERA, listOf("camera_photo"))

            // ═══════ Firebase Storage bricks ═══════
            "UploadFileToFirebaseBrick" -> {
                val bucket = extractFormulaString(el, "FIREBASE_BUCKET") ?: ""
                val path = extractFormulaString(el, "FIREBASE_STORAGE_PATH") ?: ""
                val file = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.DATA, listOf("firebase_upload", bucket, path, file))
            }
            "DownloadFileFromFirebaseBrick" -> {
                val bucket = extractFormulaString(el, "FIREBASE_BUCKET") ?: ""
                val path = extractFormulaString(el, "FIREBASE_STORAGE_PATH") ?: ""
                val dest = extractFormulaString(el, "DOWNLOAD_PATH") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.DATA, listOf("firebase_download", bucket, path, dest, varName))
            }
            "DeleteFirebaseFileBrick" -> {
                val bucket = extractFormulaString(el, "FIREBASE_BUCKET") ?: ""
                val path = extractFormulaString(el, "FIREBASE_STORAGE_PATH") ?: ""
                Block(Block.Type.DATA, listOf("firebase_delete", bucket, path))
            }
            "ListFirebaseFilesBrick" -> {
                val bucket = extractFormulaString(el, "FIREBASE_BUCKET") ?: ""
                val prefix = extractFormulaString(el, "FIREBASE_STORAGE_PATH") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.DATA, listOf("firebase_list", bucket, prefix, varName))
            }

            // ═══════ Physics (new joints) ═══════
            "CreateGearJointBrick" -> {
                val name = extractFormulaString(el, "JOINT_ID") ?: ""
                val jointA = extractFormulaString(el, "JOINT_A_ID") ?: ""
                val jointB = extractFormulaString(el, "JOINT_B_ID") ?: ""
                val ratio = extractFormulaValue(el, "RATIO") ?: 1f
                Block(Block.Type.PHYSICS, listOf("create_joint_gear", name, jointA, jointB, ratio))
            }
            "CreatePulleyJointBrick" -> {
                val name = extractFormulaString(el, "JOINT_ID") ?: ""
                val spriteA = extractFormulaString(el, "SPRITE_A") ?: ""
                val spriteB = extractFormulaString(el, "SPRITE_B") ?: ""
                val gaAX = extractFormulaValue(el, "GROUND_ANCHOR_A_X") ?: 0f
                val gaAY = extractFormulaValue(el, "GROUND_ANCHOR_A_Y") ?: 0f
                val gaBX = extractFormulaValue(el, "GROUND_ANCHOR_B_X") ?: 0f
                val gaBY = extractFormulaValue(el, "GROUND_ANCHOR_B_Y") ?: 0f
                val ratio = extractFormulaValue(el, "RATIO") ?: 1f
                Block(Block.Type.PHYSICS, listOf("create_joint_pulley", name, spriteA, spriteB, gaAX, gaAY, gaBX, gaBY, ratio))
            }
            "CreatePointJointBrick" -> {
                val name = extractFormulaString(el, "JOINT_NAME") ?: ""
                val objA = extractFormulaString(el, "OBJECT_A") ?: ""
                val objB = extractFormulaString(el, "OBJECT_B") ?: ""
                val paX = extractFormulaValue(el, "PIVOT_A_X") ?: 0f
                val paY = extractFormulaValue(el, "PIVOT_A_Y") ?: 0f
                val paZ = extractFormulaValue(el, "PIVOT_A_Z") ?: 0f
                val pbX = extractFormulaValue(el, "PIVOT_B_X") ?: 0f
                val pbY = extractFormulaValue(el, "PIVOT_B_Y") ?: 0f
                val pbZ = extractFormulaValue(el, "PIVOT_B_Z") ?: 0f
                Block(Block.Type.PHYSICS, listOf("create_joint_point", name, objA, objB, paX, paY, paZ, pbX, pbY, pbZ))
            }
            "AddHingeBrick" -> {
                val name = extractFormulaString(el, "CONSTRAINT_ID") ?: ""
                val objA = extractFormulaString(el, "OBJECT_A") ?: ""
                val objB = extractFormulaString(el, "OBJECT_B") ?: ""
                Block(Block.Type.PHYSICS, listOf("add_hinge", name, objA, objB))
            }
            "SetHingeMotorBrick" -> {
                val id = extractFormulaString(el, "CONSTRAINT_ID") ?: ""
                val target = extractFormulaValue(el, "MOTOR_TARGET") ?: 0f
                val maxForce = extractFormulaValue(el, "MOTOR_MAX_FORCE") ?: 0f
                Block(Block.Type.PHYSICS, listOf("set_hinge_motor", id, target, maxForce))
            }
            "SetHitboxBrick" -> {
                val lookEl = getChildElement(el, "look")
                val lookName = lookEl?.getAttribute("lookName") ?: ""
                Block(Block.Type.PHYSICS, listOf("set_hitbox", lookName))
            }

            // ═══════ Web extra ═══════
            "DeleteWebBrick" -> {
                val url = extractFormulaString(el, "URL") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.WEB, listOf("http_delete", url, varName))
            }
            "SetWebBrick" -> {
                val url = extractFormulaString(el, "URL") ?: ""
                val varName = extractVariableName(el) ?: ""
                val body = extractFormulaString(el, "BODY") ?: ""
                Block(Block.Type.WEB, listOf("http_set", url, body, varName))
            }
            "EvalWebBrick" -> {
                val script = extractFormulaString(el, "TEXT") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.WEB, listOf("http_eval", script, varName))
            }

            // ═══════ NeoScript bricks (runtime stubs) ═══════
            "AssignScriptsBrick" -> {
                val filePath = extractFormulaString(el, "TEXT") ?: ""
                val objName = extractFormulaString(el, "NAME") ?: ""
                val sceneName = extractFormulaString(el, "TEXT_2") ?: ""
                val replaceSel = extractTextContent(el, "spinnerSelection") ?: "0"
                val saveSel = extractTextContent(el, "spinnerSelection_2") ?: "0"
                Block(Block.Type.CONTROL, listOf("assign_scripts", filePath, objName, sceneName, replaceSel, saveSel))
            }
            "ImportScriptBrick" -> {
                val objName = extractFormulaString(el, "NAME") ?: ""
                val filePath = extractFormulaString(el, "TEXT") ?: ""
                val overwriteSel = extractTextContent(el, "spinnerSelection") ?: "0"
                Block(Block.Type.CONTROL, listOf("import_script", objName, filePath, overwriteSel))
            }
            "CreateObjectBrick" -> {
                val objName = extractFormulaString(el, "NAME") ?: ""
                val sceneSel = extractTextContent(el, "spinnerSelection") ?: "0"
                val persistSel = extractTextContent(el, "spinnerSelection_2") ?: "0"
                Block(Block.Type.CONTROL, listOf("create_object", objName, sceneSel, persistSel))
            }

            // ═══════ Security bricks ═══════
            "SecureReadVariableBrick" -> {
                val varName = extractVariableName(el) ?: ""
                val key = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.VARIABLE, listOf("secure_read", varName, key))
            }
            "SecureSaveVariableBrick" -> {
                val varName = extractVariableName(el) ?: ""
                val value = extractFormulaValue(el, "VARIABLE") ?: 0f
                val key = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.VARIABLE, listOf("secure_save", varName, value, key))
            }

            // ═══════ 3D lighting/rendering stubs ═══════
            "SetAmbientLightBrick" -> {
                val r = extractFormulaValue(el, "RED") ?: 0f
                val g = extractFormulaValue(el, "GREEN") ?: 0f
                val b = extractFormulaValue(el, "BLUE") ?: 0f
                val intensity = extractFormulaValue(el, "INTENSITY") ?: 1f
                Block(Block.Type.LOOKS, listOf("set_ambient_light", r, g, b, intensity))
            }
            "SetPointLightBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val r = extractFormulaValue(el, "RED") ?: 1f
                val g = extractFormulaValue(el, "GREEN") ?: 1f
                val b = extractFormulaValue(el, "BLUE") ?: 1f
                val intensity = extractFormulaValue(el, "INTENSITY") ?: 1f
                val range = extractFormulaValue(el, "RANGE") ?: 10f
                val x = extractFormulaValue(el, "X") ?: 0f
                val y = extractFormulaValue(el, "Y") ?: 0f
                val z = extractFormulaValue(el, "Z") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_point_light", name, r, g, b, intensity, range, x, y, z))
            }
            "SetSpotLightBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val r = extractFormulaValue(el, "RED") ?: 1f
                val g = extractFormulaValue(el, "GREEN") ?: 1f
                val b = extractFormulaValue(el, "BLUE") ?: 1f
                val intensity = extractFormulaValue(el, "INTENSITY") ?: 1f
                val range = extractFormulaValue(el, "RANGE") ?: 10f
                val x = extractFormulaValue(el, "X") ?: 0f
                val y = extractFormulaValue(el, "Y") ?: 0f
                val z = extractFormulaValue(el, "Z") ?: 0f
                val dx = extractFormulaValue(el, "DIRECTION_X") ?: 0f
                val dy = extractFormulaValue(el, "DIRECTION_Y") ?: 0f
                val dz = extractFormulaValue(el, "DIRECTION_Z") ?: -1f
                val cutoff = extractFormulaValue(el, "CUTOFF") ?: 45f
                Block(Block.Type.LOOKS, listOf("set_spot_light", name, r, g, b, intensity, range, x, y, z, dx, dy, dz, cutoff))
            }
            "SetDirectionalLightBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val r = extractFormulaValue(el, "RED") ?: 1f
                val g = extractFormulaValue(el, "GREEN") ?: 1f
                val b = extractFormulaValue(el, "BLUE") ?: 1f
                val intensity = extractFormulaValue(el, "INTENSITY") ?: 1f
                val dx = extractFormulaValue(el, "DIRECTION_X") ?: 0f
                val dy = extractFormulaValue(el, "DIRECTION_Y") ?: -1f
                val dz = extractFormulaValue(el, "DIRECTION_Z") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_directional_light", name, r, g, b, intensity, dx, dy, dz))
            }
            "SetDirectionalLight2Brick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val r = extractFormulaValue(el, "RED") ?: 1f
                val g = extractFormulaValue(el, "GREEN") ?: 1f
                val b = extractFormulaValue(el, "BLUE") ?: 1f
                val intensity = extractFormulaValue(el, "INTENSITY") ?: 1f
                val dx = extractFormulaValue(el, "VALUE_1") ?: 0f
                val dy = extractFormulaValue(el, "VALUE_2") ?: -1f
                val dz = extractFormulaValue(el, "VALUE_3") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_directional_light2", name, r, g, b, intensity, dx, dy, dz))
            }
            "SetSkyboxBrick" -> {
                val texture = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.LOOKS, listOf("set_skybox", texture))
            }
            "SetSkyColorBrick" -> {
                val r = extractFormulaValue(el, "RED") ?: 0f
                val g = extractFormulaValue(el, "GREEN") ?: 0f
                val b = extractFormulaValue(el, "BLUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_sky_color", r, g, b))
            }
            "SetFogBrick" -> {
                val r = extractFormulaValue(el, "RED") ?: 0f
                val g = extractFormulaValue(el, "GREEN") ?: 0f
                val b = extractFormulaValue(el, "BLUE") ?: 0f
                val density = extractFormulaValue(el, "DENSITY") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_fog", r, g, b, density))
            }
            "SetShadowsBrick" -> {
                val enabled = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_shadows", enabled))
            }
            "SetShadowQualityBrick" -> {
                val quality = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_shadow_quality", quality))
            }
            "SetMaterialBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val metallic = extractFormulaValue(el, "METALLIC") ?: 0f
                val roughness = extractFormulaValue(el, "ROUGHNESS") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_material", objectId, metallic, roughness))
            }
            "SetEmissiveBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val r = extractFormulaValue(el, "VALUE") ?: 0f
                val g = extractFormulaValue(el, "VALUE_2") ?: 0f
                val b = extractFormulaValue(el, "VALUE_3") ?: 0f
                val intensity = extractFormulaValue(el, "VALUE_4") ?: 1f
                Block(Block.Type.LOOKS, listOf("set_emissive", objectId, r, g, b, intensity))
            }
            "SetTextureTilingBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val tx = extractFormulaValue(el, "X") ?: 1f
                val ty = extractFormulaValue(el, "Y") ?: 1f
                Block(Block.Type.LOOKS, listOf("set_texture_tiling", objectId, tx, ty))
            }
            "SetPostProcessingBrick" -> {
                val effect = extractFormulaString(el, "EFFECT") ?: ""
                val intensity = extractFormulaValue(el, "INTENSITY") ?: 1f
                Block(Block.Type.LOOKS, listOf("set_post_processing", effect, intensity))
            }
            "SetPostProcessingNewBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val intensity = extractFormulaValue(el, "VALUE") ?: 1f
                Block(Block.Type.LOOKS, listOf("set_post_processing_new", name, intensity))
            }
            "EnablePbrRenderBrick" -> {
                val enabled = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("enable_pbr", enabled))
            }
            "SetAnisotropicFilterBrick" -> {
                val level = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_anisotropic", level))
            }
            "SetCCDBrick" -> {
                val enabled = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_ccd", enabled))
            }
            "Apply3dForceBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val fx = extractFormulaValue(el, "FORCE_X") ?: 0f
                val fy = extractFormulaValue(el, "FORCE_Y") ?: 0f
                val fz = extractFormulaValue(el, "FORCE_Z") ?: 0f
                Block(Block.Type.PHYSICS, listOf("apply_3d_force", objectId, fx, fy, fz))
            }
            "SetParticleEmissionBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val rate = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_particle_emission", objectId, rate))
            }
            "SetSpawnInvisibleBrick" -> {
                val invisible = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_spawn_invisible", invisible))
            }
            "SetPitchOnlyBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val pitchOnly = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_pitch_only", objectId, pitchOnly))
            }
            "PromoteLightBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.LOOKS, listOf("promote_light", name))
            }
            "SetShaderCodeBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val vertex = extractFormulaString(el, "VERTEX_SHADER") ?: ""
                val fragment = extractFormulaString(el, "FRAGMENT_SHADER") ?: ""
                Block(Block.Type.LOOKS, listOf("set_shader_code", objectId, vertex, fragment))
            }
            "SetShaderUniformFloatBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val uniformName = extractFormulaString(el, "UNIFORM_NAME") ?: ""
                val value = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_shader_uniform_float", objectId, uniformName, value))
            }
            "SetShaderUniformVec3Brick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val uniformName = extractFormulaString(el, "UNIFORM_NAME") ?: ""
                val v1 = extractFormulaValue(el, "VALUE") ?: 0f
                val v2 = extractFormulaValue(el, "VALUE_2") ?: 0f
                val v3 = extractFormulaValue(el, "VALUE_3") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_shader_uniform_vec3", objectId, uniformName, v1, v2, v3))
            }
            "SetMaxPointLightsBrick" -> {
                val count = extractFormulaValue(el, "VALUE") ?: 4f
                Block(Block.Type.LOOKS, listOf("set_max_point_lights", count.toInt()))
            }
            "RemovePbrLightBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.LOOKS, listOf("remove_pbr_light", name))
            }
            "SetBackgroundLightBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val r = extractFormulaValue(el, "VALUE") ?: 0f
                val g = extractFormulaValue(el, "VALUE_2") ?: 0f
                val b = extractFormulaValue(el, "VALUE_3") ?: 0f
                Block(Block.Type.LOOKS, listOf("set_background_light", name, r, g, b))
            }
            "Create3dObjectBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val file = extractFormulaString(el, "FILE") ?: ""
                Block(Block.Type.PHYSICS, listOf("create_3d_object", name, file))
            }
            "Remove3dObjectBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.PHYSICS, listOf("remove_3d_object", name))
            }
            "Set3dPositionBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val x = extractFormulaValue(el, "X") ?: 0f
                val y = extractFormulaValue(el, "Y") ?: 0f
                val z = extractFormulaValue(el, "Z") ?: 0f
                Block(Block.Type.PHYSICS, listOf("set_3d_position", name, x, y, z))
            }
            "Set3dRotationBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val rx = extractFormulaValue(el, "VALUE") ?: 0f
                val ry = extractFormulaValue(el, "VALUE_2") ?: 0f
                val rz = extractFormulaValue(el, "VALUE_3") ?: 0f
                Block(Block.Type.PHYSICS, listOf("set_3d_rotation", name, rx, ry, rz))
            }
            "Set3dScaleBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val sx = extractFormulaValue(el, "VALUE") ?: 1f
                val sy = extractFormulaValue(el, "VALUE_2") ?: 1f
                val sz = extractFormulaValue(el, "VALUE_3") ?: 1f
                Block(Block.Type.PHYSICS, listOf("set_3d_scale", name, sx, sy, sz))
            }
            "Set3dVelocityBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val vx = extractFormulaValue(el, "VALUE") ?: 0f
                val vy = extractFormulaValue(el, "VALUE_2") ?: 0f
                val vz = extractFormulaValue(el, "VALUE_3") ?: 0f
                Block(Block.Type.PHYSICS, listOf("set_3d_velocity", name, vx, vy, vz))
            }
            "Set3dFrictionBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val friction = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.PHYSICS, listOf("set_3d_friction", name, friction))
            }
            "Set3dGravityBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val gx = extractFormulaValue(el, "VALUE") ?: 0f
                val gy = extractFormulaValue(el, "VALUE_2") ?: 0f
                val gz = extractFormulaValue(el, "VALUE_3") ?: 0f
                Block(Block.Type.PHYSICS, listOf("set_3d_gravity", name, gx, gy, gz))
            }

            // ═══════ 3D Camera/View stubs ═══════
            "ObjectLookAtBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val x = extractFormulaValue(el, "X") ?: 0f
                val y = extractFormulaValue(el, "Y") ?: 0f
                val z = extractFormulaValue(el, "Z") ?: 0f
                Block(Block.Type.CAMERA, listOf("object_look_at", objectId, x, y, z))
            }
            "VisualPlacementBrick" -> {
                val enabled = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.CAMERA, listOf("visual_placement", enabled))
            }
            "KeyframeAnimationBrick" -> {
                val objectId = extractFormulaString(el, "NAME") ?: ""
                val animName = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.CAMERA, listOf("keyframe_anim", objectId, animName))
            }
            "CreateGLViewBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                Block(Block.Type.CAMERA, listOf("create_gl_view", name))
            }
            "AttachSOBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val target = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.CAMERA, listOf("attach_so", name, target))
            }
            "LoadNativeModuleBrick" -> {
                val path = extractFormulaString(el, "VALUE") ?: ""
                Block(Block.Type.CAMERA, listOf("load_native_module", path))
            }

            // ═══════ Misc stubs ═══════
            "CreateDialogBrick" -> {
                val name = extractFormulaString(el, "NAME") ?: ""
                val text = extractFormulaString(el, "TEXT") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.CONTROL, listOf("create_dialog", name, text, varName))
            }
            "BigAskBrick" -> {
                val question = extractFormulaString(el, "ASK_QUESTION") ?: ""
                val varName = extractVariableName(el) ?: ""
                Block(Block.Type.LOOKS, listOf("big_ask", question, varName))
            }
            "HideStatusBarBrick" -> {
                val hidden = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.CONTROL, listOf("hide_status_bar", hidden))
            }
            "ToggleDisplayBrick" -> {
                val state = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.CONTROL, listOf("toggle_display", state))
            }
            "OrientationBrick" -> {
                val orientation = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.CONTROL, listOf("set_orientation", orientation))
            }
            "SetSaveScenesBrick" -> {
                val save = extractFormulaValue(el, "VALUE") ?: 0f
                Block(Block.Type.CONTROL, listOf("set_save_scenes", save))
            }
            "ApplyShaderToImageBrick" -> {
                val fileName = extractFormulaString(el, "FILE") ?: ""
                val shaderName = extractFormulaString(el, "TEXT") ?: ""
                Block(Block.Type.LOOKS, listOf("apply_shader_to_image", fileName, shaderName))
            }

            // Неизвестный тип — вернём null, будет пропущен
            else -> null
        }
    }

    // ════════════════════════ XML helpers ════════════════════════

    /** Найти формулу по BrickField и получить element formulaElement для вычисления. */
    private fun getFormulaElement(el: Element, brickFieldEnumName: String): Element? {
        val formulaList = el.getElementsByTagName("formulaList")?.item(0) as? Element ?: return null
        val formulas = formulaList.getElementsByTagName("formula")
        for (i in 0 until formulas.length) {
            val formula = formulas.item(i) as? Element ?: continue
            val category = formula.getAttribute("category")
            if (category == brickFieldEnumName) {
                // Ищем formulaElement (может быть внутри formulaTree)
                val ft = formula.getElementsByTagName("formulaTree")?.item(0) as? Element
                val fe = ft?.getElementsByTagName("formulaElement")?.item(0) as? Element
                if (fe != null) return fe
                // Старый формат без formulaElement: type/value прямо в formulaTree
                return ft
            }
        }
        return null
    }

    /** Создать RuntimeFormula для отложенного вычисления. */
    private fun getRuntimeFormula(el: Element, brickFieldEnumName: String): RuntimeFormula? {
        val formulaElement = getFormulaElement(el, brickFieldEnumName) ?: return null
        return RuntimeFormula(brickFieldEnumName, formulaElement)
    }

    /**
     * Извлекает формулы аргументов вызова процедуры (UserDefinedReceiverBrick).
     * Для каждого <userDefinedBrickInput> берёт <value> → первый <formulaElement>.
     * Возвращает список (по порядку входов); null — если аргумент не задан.
     */
    private fun extractUserDefinedArgFormulas(el: Element): List<RuntimeFormula?> {
        val dataList = el.getElementsByTagName("userDefinedBrickDataList")?.item(0) as? Element
            ?: return emptyList()
        val inputs = dataList.getElementsByTagName("userDefinedBrickInput")
        val result = mutableListOf<RuntimeFormula?>()
        for (i in 0 until inputs.length) {
            val input = inputs.item(i) as? Element ?: continue
            val valueEl = input.getElementsByTagName("value")?.item(0) as? Element
            val fe = valueEl?.let { findFirstFormulaElement(it) }
            result.add(if (fe != null) RuntimeFormula("USER_DEFINED_ARG", fe) else null)
        }
        return result
    }

    /** Рекурсивно ищет первый <formulaElement> внутри parent. */
    private fun findFirstFormulaElement(parent: Element): Element? {
        val nodes = parent.childNodes
        for (i in 0 until nodes.length) {
            val child = nodes.item(i)
            if (child.nodeType != Node.ELEMENT_NODE) continue
            if (child.nodeName == "formulaElement") return child as Element
            val deeper = findFirstFormulaElement(child as Element)
            if (deeper != null) return deeper
        }
        return null
    }

    /**
     * Извлекает числовое значение формулы из code.xml (парсинг).
     * Для простых константных формул — вычисляет сразу.
     * Для формул с сенсорами/переменными — вычисляет начальное значение.
     */
    private fun extractFormulaValue(el: Element, brickFieldEnumName: String, spriteIndex: Int = 0): Float? {
        val fe = getFormulaElement(el, brickFieldEnumName) ?: return null
        return evaluateFormulaNode(fe, spriteIndex)?.let { v ->
            when (v) {
                is Double -> v.toFloat()
                is Float -> v
                is Number -> v.toFloat()
                else -> null
            }
        }
    }

    /** Извлекает строковое значение формулы. */
    private fun extractFormulaString(el: Element, brickFieldEnumName: String, spriteIndex: Int = 0): String? {
        val fe = getFormulaElement(el, brickFieldEnumName) ?: return null
        return evaluateFormulaNode(fe, spriteIndex)?.toString()
    }

    // ════════════════════════ RECURSIVE FORMULA EVALUATOR ════════════════════════

    /**
     * Рекурсивно вычисляет формулу, представленную DOM-элементом.
     *
     * Поддерживаемые типы:
     *  - NUMBER: числовая константа
     *  - STRING: строковая константа
     *  - OPERATOR: бинарные/унарные операторы (PLUS, MINUS, MULT, DIVIDE, MOD, POW,
     *    EQUAL, NOT_EQUAL, SMALLER_THAN, GREATER_THAN, SMALLER_OR_EQUAL, GREATER_OR_EQUAL,
     *    LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT)
     *  - FUNCTION: математические/строковые функции (SIN, COS, SQRT, RAND, JOIN, ...)
     *  - SENSOR: сенсоры (OBJECT_X, MOUSE_X, STAGE_WIDTH, ...)
     *  - USER_VARIABLE: переменная пользователя
     *  - USER_LIST: список пользователя
     *  - BRACKET: скобки (вычисляют rightChild)
     *
     * @param node DOM-элемент formulaElement
     * @param spriteIndex индекс текущего спрайта
     * @return вычисленное значение (Double или String)
     */
    private fun evaluateFormulaNode(node: Element, spriteIndex: Int): Any? {
        val type = getTagText(node, "type") ?: return null
        val value = getTagText(node, "value") ?: ""

        return when (type) {
            "NUMBER" -> value.toDoubleOrNull()

            "STRING" -> value

            "OPERATOR" -> evaluateOperator(value, node, spriteIndex)

            "FUNCTION" -> evaluateFunction(value, node, spriteIndex)

            "SENSOR" -> evaluateSensor(value, spriteIndex)

            "USER_VARIABLE" -> {
                val v = variables[value]
                when (v) {
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
            }

            "USER_LIST" -> {
                // список — возвращаем первую строку
                ""
            }

            "BRACKET" -> {
                val rightChild = getChildElement(node, "rightChild")
                rightChild?.let { evaluateFormulaNode(it, spriteIndex) }
            }

            "COLLISION_FORMULA" -> value.toDoubleOrNull()

            "USER_DEFINED_BRICK_INPUT" -> {
                // Пользовательский ввод процедуры — ищем в локальной области параметров,
                // затем в глобальных переменных, иначе возвращаем имя как строку.
                val local = activeState?.currentFrame?.procVars?.get(value)
                if (local != null) return local
                val gv = variables[value]
                if (gv != null) return gv
                value.toDoubleOrNull() ?: value
            }

            else -> null
        }
    }

    /** Вычисление операторов. */
    private fun evaluateOperator(op: String, node: Element, spriteIndex: Int): Double? {
        val leftChild = getChildElement(node, "leftChild")
        val rightChild = getChildElement(node, "rightChild")

        val leftVal = leftChild?.let { evaluateFormulaNode(it, spriteIndex) }
        val rightVal = rightChild?.let { evaluateFormulaNode(it, spriteIndex) }

        val left = (leftVal as? Double) ?: 0.0
        val right = (rightVal as? Double) ?: 0.0

        return when (op) {
            "PLUS" -> left + right
            "MINUS" -> if (leftChild == null) -right else left - right
            "MULT" -> left * right
            "DIVIDE" -> if (right != 0.0) left / right else 0.0
            "MOD" -> left % right
            "POW" -> left.pow(right)
            "EQUAL" -> if (left == right) 1.0 else 0.0
            "NOT_EQUAL" -> if (left != right) 1.0 else 0.0
            "SMALLER_THAN" -> if (left < right) 1.0 else 0.0
            "GREATER_THAN" -> if (left > right) 1.0 else 0.0
            "SMALLER_OR_EQUAL" -> if (left <= right) 1.0 else 0.0
            "GREATER_OR_EQUAL" -> if (left >= right) 1.0 else 0.0
            "LOGICAL_AND" -> if (left != 0.0 && right != 0.0) 1.0 else 0.0
            "LOGICAL_OR" -> if (left != 0.0 || right != 0.0) 1.0 else 0.0
            "LOGICAL_NOT" -> if (right == 0.0) 1.0 else 0.0
            else -> null
        }
    }

    /** Вычисление функций. */
    private fun evaluateFunction(func: String, node: Element, spriteIndex: Int): Any? {
        val leftChild = getChildElement(node, "leftChild")
        val rightChild = getChildElement(node, "rightChild")
        val additional = getAdditionalChildren(node)

        val leftVal = leftChild?.let { evaluateFormulaNode(it, spriteIndex) }
        val rightVal = rightChild?.let { evaluateFormulaNode(it, spriteIndex) }

        val a = (leftVal as? Double) ?: 0.0
        val b = (rightVal as? Double) ?: 0.0
        val aStr = leftVal?.toString() ?: ""
        val bStr = rightVal?.toString() ?: ""

        // Helper: find sprite by name from leftChild
        fun findSprite(name: String) = project.sprites.find { it.name == name }

        return when (func) {
            // ═══════════════ Базовые математические (23) ═══════════════
            "SIN" -> sin(Math.toRadians(a))
            "COS" -> cos(Math.toRadians(a))
            "TAN" -> tan(Math.toRadians(a))
            "LN" -> ln(a)
            "LOG" -> log10(a)
            "SQRT" -> sqrt(a)
            "ABS" -> abs(a)
            "ROUND" -> round(a)
            "FLOOR" -> floor(a)
            "CEIL" -> ceil(a)
            "PI" -> Math.PI
            "TRUE" -> 1.0
            "FALSE" -> 0.0
            "RAND" -> if (leftChild != null && rightChild != null) a + Math.random() * (b - a) else Math.random()
            "MAX" -> maxOf(a, b)
            "MIN" -> minOf(a, b)
            "POWER" -> a.pow(b)
            "MOD" -> a % b
            "ARCSIN" -> Math.toDegrees(asin(a.coerceIn(-1.0, 1.0)))
            "ARCCOS" -> Math.toDegrees(acos(a.coerceIn(-1.0, 1.0)))
            "ARCTAN" -> Math.toDegrees(atan(a))
            "ARCTAN2" -> Math.toDegrees(atan2(a, b))
            "EXP" -> exp(a)
            "ROUNDTO" -> {
                val places = b.toInt()
                val factor = 10.0.pow(places)
                round(a * factor) / factor
            }
            "CLAMP" -> {
                val c = additional.getOrNull(0)?.let { evaluateFormulaNode(it, spriteIndex) as? Double } ?: 1.0
                a.coerceIn(b, c)
            }

            // ═══════════════ Строковые функции (18) ═══════════════
            "LENGTH" -> (leftVal?.toString()?.length ?: 0).toDouble()
            "LETTER" -> {
                val idx = b.toInt() - 1
                if (idx in aStr.indices) aStr[idx].toString() else ""
            }
            "SUBTEXT" -> {
                val start = b.toInt()
                val end = additional.getOrNull(0)?.let { (evaluateFormulaNode(it, spriteIndex) as? Double)?.toInt() } ?: aStr.length
                aStr.substring((start - 1).coerceAtLeast(0), end.coerceAtMost(aStr.length))
            }
            "UPPER" -> aStr.uppercase()
            "LOWER" -> aStr.lowercase()
            "JOIN" -> aStr + bStr
            "JOIN3" -> {
                val s3 = additional.getOrNull(0)?.let { evaluateFormulaNode(it, spriteIndex)?.toString() } ?: ""
                aStr + bStr + s3
            }
            "REVERSE" -> aStr.reversed()
            "CONTAINS" -> if (aStr.contains(bStr)) 1.0 else 0.0
            "REPLACE" -> {
                val replacement = additional.getOrNull(0)?.let { evaluateFormulaNode(it, spriteIndex)?.toString() } ?: ""
                aStr.replace(bStr, replacement)
            }
            "REGEX" -> {
                try {
                    if (Regex(bStr).containsMatchIn(aStr)) 1.0 else 0.0
                } catch (_: Exception) { 0.0 }
            }
            "TO_HEX" -> a.toLong().toString(16).uppercase()
            "TO_DEC" -> {
                try { aStr.toLong(16).toDouble() } catch (_: Exception) { 0.0 }
            }
            "RANDOM_STR" -> {
                val len = a.toInt().coerceAtLeast(1)
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                (1..len).map { chars[kotlin.random.Random.nextInt(chars.length)] }.joinToString("")
            }
            "REPEAT" -> aStr.repeat(b.toInt().coerceAtLeast(0))
            "VAR" -> variables[aStr]?.let {
                when (it) { is Number -> it.toDouble() else -> it.toString() }
            } ?: 0.0
            "VARNAME" -> {
                val idx = a.toInt()
                val keys = variables.keys.toList()
                keys.getOrElse(idx) { "" }
            }
            "VARVALUE" -> variables[aStr]?.let {
                when (it) { is Number -> it.toDouble() else -> it.toString() }
            } ?: 0.0
            "DISTAN" -> {
                // Levenshtein distance
                if (aStr.isEmpty()) bStr.length.toDouble()
                else if (bStr.isEmpty()) aStr.length.toDouble()
                else {
                    val dp = Array(aStr.length + 1) { IntArray(bStr.length + 1) }
                    for (i in 0..aStr.length) dp[i][0] = i
                    for (j in 0..bStr.length) dp[0][j] = j
                    for (i in 1..aStr.length) {
                        for (j in 1..bStr.length) {
                            val cost = if (aStr[i - 1] == bStr[j - 1]) 0 else 1
                            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                        }
                    }
                    dp[aStr.length][bStr.length].toDouble()
                }
            }
            "JOINNUMBER" -> {
                val s3 = additional.getOrNull(0)?.let { evaluateFormulaNode(it, spriteIndex)?.toString() } ?: ""
                aStr + bStr + s3
            }

            // ═══════════════ Списковые функции (6) ═══════════════
            "NUMBER_OF_ITEMS" -> {
                val list = userLists[aStr]
                (list?.size ?: 0).toDouble()
            }
            "LIST_ITEM" -> {
                val idx = b.toInt()
                userLists[aStr]?.getOrNull(idx)?.toString() ?: ""
            }
            "INDEX_OF_ITEM" -> {
                val idx = userLists[aStr]?.indexOfFirst { it.toString() == bStr }
                (idx ?: -1).toDouble()
            }
            "FLATTEN" -> {
                userLists.values.flatten().joinToString(", ")
            }
            "CONNECT" -> {
                userLists[aStr]?.joinToString(", ") ?: ""
            }
            "FIND" -> {
                userLists[aStr]?.filter { it.toString().contains(bStr, ignoreCase = true) }?.joinToString(", ") ?: ""
            }

            // ═══════════════ Файловые функции (13) ═══════════════
            "FILE_EXISTS" -> {
                val path = aStr
                if (path.isNotEmpty()) { if (java.io.File(path).exists()) 1.0 else 0.0 } else 0.0
            }
            "FILE_SIZE" -> {
                val path = aStr
                if (path.isNotEmpty()) java.io.File(path).length().toDouble() else 0.0
            }
            "FILE" -> project.projectDir?.absolutePath ?: ""
            "FILES_PATH" -> project.projectDir?.absolutePath ?: ""
            "ALL_FILES" -> {
                val dir = if (aStr.isNotEmpty()) java.io.File(aStr) else (project.projectDir ?: java.io.File("."))
                if (dir.isDirectory) dir.list()?.joinToString(", ") ?: "" else ""
            }
            "FILE_PROJECT_EXISTS" -> {
                val projectDir = project.projectDir
                if (projectDir != null && aStr.isNotEmpty()) {
                    if (java.io.File(projectDir, aStr).exists()) 1.0 else 0.0
                } else 0.0
            }
            "FILE_EXISTS_IN_DIR" -> {
                if (aStr.isNotEmpty() && bStr.isNotEmpty()) {
                    if (java.io.File(java.io.File(aStr), bStr).exists()) 1.0 else 0.0
                } else 0.0
            }
            "FILE_EXISTS_AT_PATH" -> {
                if (aStr.isNotEmpty()) { if (java.io.File(aStr).exists()) 1.0 else 0.0 } else 0.0
            }
            "FILE_PROJECT_SIZE" -> {
                val projectDir = project.projectDir
                if (projectDir != null && aStr.isNotEmpty()) {
                    java.io.File(projectDir, aStr).length().toDouble()
                } else 0.0
            }
            "FILE_SIZE_IN_DIR" -> {
                if (aStr.isNotEmpty() && bStr.isNotEmpty()) {
                    java.io.File(java.io.File(aStr), bStr).length().toDouble()
                } else 0.0
            }
            "FILE_SIZE_AT_PATH" -> {
                if (aStr.isNotEmpty()) java.io.File(aStr).length().toDouble() else 0.0
            }
            "FILE_READ_STRING" -> {
                try {
                    if (aStr.isNotEmpty()) java.io.File(aStr).readText() else ""
                } catch (_: Exception) { "" }
            }

            // ═══════════════ Системная информация (18+) ═══════════════
            "SCREEN_WIDTH" -> project.stageWidth?.toDouble() ?: 480.0
            "SCREEN_HEIGHT" -> project.stageHeight?.toDouble() ?: 720.0
            "DEVICE_NAME" -> "Desktop PC"
            "SYSTEM_LANGUAGE" -> System.getProperty("user.language") ?: "en"
            "CPU_NAME" -> System.getProperty("os.arch") ?: "unknown"
            "CPU_CORES" -> Runtime.getRuntime().availableProcessors().toDouble()
            "TOTAL_RAM" -> Runtime.getRuntime().maxMemory().toDouble()
            "FREE_RAM" -> Runtime.getRuntime().freeMemory().toDouble()
            "OS_NAME" -> System.getProperty("os.name") ?: "unknown"
            "OS_VERSION" -> System.getProperty("os.version") ?: "unknown"
            "FPS" -> com.badlogic.gdx.Gdx.graphics.framesPerSecond.toDouble()
            "LOCAL_IP" -> {
                try { java.net.InetAddress.getLocalHost().hostAddress ?: "0.0.0.0" }
                catch (_: Exception) { "0.0.0.0" }
            }
            "INTERNET_CONNECTED" -> {
                try {
                    val sock = java.net.Socket()
                    sock.connect(java.net.InetSocketAddress("8.8.8.8", 53), 500)
                    sock.close()
                    1.0
                } catch (_: Exception) { 0.0 }
            }
            "SCREEN_DPI" -> {
                try {
                    val tk = java.awt.Toolkit.getDefaultToolkit()
                    tk.screenResolution.toDouble()
                } catch (_: Exception) { 96.0 }
            }
            "SCREEN_REFRESH" -> {
                try {
                    val env = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    val device = env.defaultScreenDevice
                    device.displayMode.refreshRate.toDouble()
                } catch (_: Exception) { 60.0 }
            }
            "ANDROID_VERSION" -> 0.0 // Not applicable on desktop
            "API_LEVEL" -> 0.0
            "SCREEN_ORIENTATION" -> 0.0 // landscape=0 on desktop
            "IS_IN_FOREGROUND" -> 1.0 // always in foreground on desktop
            "OPENGL_VERSION" -> {
                try { com.badlogic.gdx.Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_VERSION) ?: "unknown" }
                catch (_: Exception) { "unknown" }
            }
            "CPU_FREQUENCY" -> 0.0
            "CPU_USAGE" -> 0.0
            "GPU_NAME" -> {
                try { com.badlogic.gdx.Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_RENDERER) ?: "unknown" }
                catch (_: Exception) { "unknown" }
            }
            "VULKAN_SUPPORTED" -> 0.0

            // ═══════════════ SPRITE_* функции (22) ═══════════════
            "SPRITE_EXISTS" -> {
                val sprite = findSprite(aStr)
                if (sprite != null) 1.0 else 0.0
            }
            "SPRITE_X" -> {
                findSprite(aStr)?.x?.toDouble() ?: 0.0
            }
            "SPRITE_Y" -> {
                findSprite(aStr)?.y?.toDouble() ?: 0.0
            }
            "SPRITE_SIZE" -> {
                findSprite(aStr)?.size?.toDouble() ?: 100.0
            }
            "SPRITE_WIDTH" -> {
                findSprite(aStr)?.lookWidth?.toDouble() ?: 0.0
            }
            "SPRITE_HEIGHT" -> {
                findSprite(aStr)?.lookHeight?.toDouble() ?: 0.0
            }
            "SPRITE_DIRECTION" -> {
                findSprite(aStr)?.direction?.toDouble() ?: 90.0
            }
            "SPRITE_VISIBLE" -> {
                if (findSprite(aStr)?.visible == true) 1.0 else 0.0
            }
            "SPRITE_TRANSPARENCY" -> {
                findSprite(aStr)?.transparency?.toDouble() ?: 0.0
            }
            "SPRITE_LAYER" -> {
                val idx = project.sprites.indexOfFirst { it.name == aStr }
                if (idx >= 0) idx.toDouble() else 0.0
            }
            "SPRITE_NAME_GET" -> {
                val idx = b.toInt()
                project.sprites.getOrNull(idx)?.name ?: ""
            }
            "SPRITE_INDEX_GET" -> {
                project.sprites.indexOfFirst { it.name == aStr }.toDouble()
            }
            "SPRITE_CLONE_COUNT" -> {
                project.sprites.count { it.name.startsWith(aStr) && it.name != aStr }.toDouble()
            }
            "SPRITE_LOOK_COUNT" -> {
                findSprite(aStr)?.looks?.size?.toDouble() ?: 0.0
            }
            "SPRITE_SOUND_COUNT" -> 0.0 // Not tracked on desktop
            "SPRITE_VARIABLE_COUNT" -> variables.size.toDouble()
            "SPRITE_LIST_COUNT" -> userLists.size.toDouble()
            "SPRITE_DISTANCE" -> {
                val name2 = bStr
                val s1 = findSprite(aStr)
                val s2 = findSprite(name2)
                if (s1 != null && s2 != null) {
                    val dx = s1.x - s2.x
                    val dy = s1.y - s2.y
                    sqrt((dx * dx + dy * dy).toDouble())
                } else 0.0
            }
            "SPRITE_TOUCHING" -> {
                val name2 = bStr
                val s1 = findSprite(aStr)
                val s2 = findSprite(name2)
                // Simple bounding-box collision check
                if (s1 != null && s2 != null) {
                    val dx = abs(s1.x - s2.x)
                    val dy = abs(s1.y - s2.y)
                    val halfW = (s1.lookWidth + s2.lookWidth) / 2f
                    val halfH = (s1.lookHeight + s2.lookHeight) / 2f
                    if (dx < halfW && dy < halfH) 1.0 else 0.0
                } else 0.0
            }
            "SPRITE_ANGLE_TO" -> {
                val name2 = bStr
                val s1 = findSprite(aStr)
                val s2 = findSprite(name2)
                if (s1 != null && s2 != null) {
                    Math.toDegrees(atan2((s2.y - s1.y).toDouble(), (s2.x - s1.x).toDouble()))
                } else 0.0
            }
            "SPRITE_UUID" -> aStr

            // ═══════════════ Цветовые функции ═══════════════
            "IF_THEN_ELSE" -> {
                val c = additional.getOrNull(0)?.let { evaluateFormulaNode(it, spriteIndex) }
                if (a != 0.0) bStr else c?.toString() ?: ""
            }

            // ═══════════════ Функции-заглушки (Android-only / 3D) ═══════════════
            "COLLIDES_WITH_COLOR", "COLOR_TOUCHES_COLOR", "COLOR_AT_XY",
            "COLOR_EQUALS_COLOR", "TOUCHES_OBJECT_BY_NAME" -> 0.0

            "MULTI_FINGER_X", "MULTI_FINGER_Y", "MULTI_FINGER_TOUCHED" -> 0.0

            "TEXT_BLOCK_X", "TEXT_BLOCK_SIZE" -> 0.0
            "TEXT_BLOCK_Y" -> 0.0
            "TEXT_BLOCK_FROM_CAMERA", "TEXT_BLOCK_LANGUAGE_FROM_CAMERA" -> ""

            "VIDEO_PLAYING", "VIDEO_TIME" -> 0.0

            "JSON_GET", "JSON_SET", "JSON_IS_VALID" -> 0.0

            "ARDUINOANALOG", "ARDUINODIGITAL", "RASPIDIGITAL" -> 0.0

            "LUA" -> 0.0

            "DELTA" -> com.badlogic.gdx.Gdx.graphics.deltaTime.toDouble()

            // 3D заглушки (35)
            "GET_3D_POSITION_X", "GET_3D_POSITION_Y", "GET_3D_POSITION_Z",
            "GET_3D_ROTATION_YAW", "GET_3D_ROTATION_PITCH", "GET_3D_ROTATION_ROLL",
            "GET_3D_SCALE_X", "GET_3D_SCALE_Y", "GET_3D_SCALE_Z",
            "GET_3D_DISTANCE", "GET_DIRECTION_X", "GET_DIRECTION_Y",
            "GET_ANGLE", "OBJECT_TOUCHES_OBJECT", "OBJECT_INTERSECTS_OBJECT",
            "ID_OF_DETECTED_OBJECT", "OBJECT_WITH_ID_VISIBLE",
            "IS_MOUSE_BUTTON_DOWN" -> 0.0

            // 3D Ray заглушки (12)
            "GET_RAY_DISTANCE", "GET_RAY_HIT_OBJECT",
            "GET_RAY_HIT_X", "GET_RAY_HIT_Y", "GET_RAY_HIT_Z",
            "GET_RAY_HIT_NORMAL_X", "GET_RAY_HIT_NORMAL_Y", "GET_RAY_HIT_NORMAL_Z",
            "RAY_DID_HIT", "RAY_DID_HIT2", "RAY_HIT_SPRITE_NAME",
            "RAY_HIT_X", "RAY_HIT_Y", "RAY_HIT_DISTANCE" -> 0.0

            // Камера 3D (9)
            "GET_CAMERA_POS_X", "GET_CAMERA_POS_Y", "GET_CAMERA_POS_Z",
            "GET_CAMERA_DIR_X", "GET_CAMERA_DIR_Y", "GET_CAMERA_DIR_Z",
            "GET_CAMERA_ROTATION_YAW", "GET_CAMERA_ROTATION_PITCH",
            "GET_CAMERA_ROTATION_ROLL" -> 0.0

            // 2D заглушки (14)
            "F2D_X", "F2D_Y", "F2D_ROTATION",
            "F2D_SCALE_X", "F2D_SCALE_Y",
            "F2D_COLOR_R", "F2D_COLOR_G", "F2D_COLOR_B", "F2D_COLOR_ALPHA",
            "F2D_TEXTURE",
            "F2D_CAM_X", "F2D_CAM_Y", "F2D_CAM_ZOOM",
            "F2D_IS_TOUCHED", "F2D_IS_TOUCHED_INDEX" -> 0.0

            // Voxel (2)
            "VOXEL_GET_ID", "VOXEL_GET_DATA" -> 0.0

            // AdMob (9)
            "ADMOB_IS_INITIALIZED", "ADMOB_IS_TEST_MODE", "ADMOB_IS_BANNER_LOADED",
            "ADMOB_IS_INTERSTITIAL_LOADED", "ADMOB_IS_REWARDED_LOADED",
            "ADMOB_IS_APP_OPEN_LOADED", "ADMOB_LAST_ERROR_CODE",
            "ADMOB_LAST_ERROR_MESSAGE", "ADMOB_IS_GOOGLE_PLAY_SERVICES_AVAILABLE" -> 0.0

            // Crypto (27) — заглушки, реализация позже
            "SHA_224", "SHA_256", "SHA_384", "SHA_512",
            "HASH_BYTES", "HASH_FILE",
            "AES_ENCRYPT", "AES_DECRYPT",
            "CHACHA20_ENCRYPT", "CHACHA20_DECRYPT",
            "PBKDF2", "GENERATE_SALT", "DERIVE_KEY",
            "GENERATE_AES_KEY", "GENERATE_RANDOM_BYTES",
            "GENERATE_PASSWORD", "GENERATE_UUID",
            "RANDOM_HEX", "RANDOM_BASE64",
            "RANDOM_INT_SECURE", "RANDOM_STRING_SECURE",
            "BASE64_ENCODE", "BASE64_DECODE",
            "HEX_ENCODE", "HEX_DECODE",
            "COMPARE_HASH", "IS_BASE64", "IS_HEX",
            "HMAC_SHA_256", "HMAC_SHA_512",
            "RSA_GENERATE_KEY_PAIR", "RSA_ENCRYPT", "RSA_DECRYPT",
            "RSA_SIGN", "RSA_VERIFY" -> 0.0

            // Physical simulation (заглушки)
            "FLOATARRAY", "VIEW_X", "VIEW_Y", "VIEW_WIDTH", "VIEW_HEIGHT" -> 0.0
            "COLLISION_LIST", "INTERSECT_LIST" -> ""
            "PT_ARGMAX", "PT_VALUE", "PT_VALUEND",
            "PT_SHAPE", "PT_DUMP", "PT_TOTALSIZE" -> 0.0

            // Voxel/Vision
            "GET_3D_VELOCITY_X", "GET_3D_VELOCITY_Y", "GET_3D_VELOCITY_Z" -> 0.0

            // Velocity
            "USED_RAM", "USED_STORAGE", "VOLUME_LEVEL", "SCREEN_BRIGHTNESS" -> 0.0
            "BATTERY_PERCENT", "BATTERY_CHARGING", "BATTERY_TEMP",
            "BATTERY_VOLTAGE", "BATTERY_STATE" -> 0.0
            "GET_STATUS_CODE", "DOWNLOAD_PROGRESS" -> 0.0
            "INTERNET_TYPE", "INTERNET_SPEED" -> 0.0
            "CPU_ARCHITECTURE", "CPU_FREQUENCY_MIN" -> 0.0
            "TOTAL_STORAGE", "FREE_STORAGE" -> 0.0
            "DEVICE_MANUFACTURER" -> "Desktop"
            "THEME" -> "light"
            "IS_PC" -> 1.0
            "IS_MOBILE" -> 0.0

            else -> null
        }
    }

    /** Вычисление сенсоров. Возвращает Any? — Double для чисел, String для строк. */
    private fun evaluateSensor(sensor: String, spriteIndex: Int): Any? {
        val sprite = project.sprites.getOrNull(spriteIndex)
        return when (sensor) {
            // ═══════════════ Объектные сенсоры ═══════════════
            "OBJECT_X" -> sprite?.x?.toDouble() ?: 0.0
            "OBJECT_Y" -> sprite?.y?.toDouble() ?: 0.0
            "OBJECT_SIZE" -> sprite?.size?.toDouble() ?: 100.0
            "OBJECT_WIDTH" -> sprite?.lookWidth?.toDouble() ?: 0.0
            "OBJECT_HEIGHT" -> sprite?.lookHeight?.toDouble() ?: 0.0
            "WIDTH" -> sprite?.lookWidth?.toDouble() ?: 0.0
            "HEIGHT" -> sprite?.lookHeight?.toDouble() ?: 0.0
            "OBJECT_DIRECTION", "MOTION_DIRECTION", "LOOK_DIRECTION" -> sprite?.direction?.toDouble() ?: 90.0
            "OBJECT_TRANSPARENCY" -> sprite?.transparency?.toDouble() ?: 0.0
            "OBJECT_BRIGHTNESS" -> sprite?.brightness?.toDouble() ?: 100.0
            "OBJECT_COLOR" -> sprite?.color?.toDouble() ?: 0.0
            "OBJECT_LAYER" -> spriteIndex.toDouble()
            "OBJECT_LOOK_NUMBER" -> (sprite?.currentLookIndex?.plus(1))?.toDouble() ?: 1.0
            "OBJECT_LOOK_NAME" -> sprite?.currentLook()?.name ?: ""
            "OBJECT_LOOK_WIDTH" -> sprite?.lookWidth?.toDouble() ?: 0.0
            "OBJECT_LOOK_HEIGHT" -> sprite?.lookHeight?.toDouble() ?: 0.0
            "OBJECT_BACKGROUND_NUMBER" -> sprite?.currentLookIndex?.plus(1)?.toDouble() ?: 1.0
            "OBJECT_BACKGROUND_NAME" -> sprite?.currentLook()?.name ?: ""
            "OBJECT_NUMBER_OF_LOOKS" -> (sprite?.looks?.size)?.toDouble() ?: 1.0

            // Physics velocity (настоящие, из Box2D)
            "OBJECT_X_VELOCITY" -> {
                val body = if (sprite != null) physicsWorld?.getBody(sprite) else null
                body?.linearVelocity?.x?.toDouble() ?: 0.0
            }
            "OBJECT_Y_VELOCITY" -> {
                val body = if (sprite != null) physicsWorld?.getBody(sprite) else null
                body?.linearVelocity?.y?.toDouble() ?: 0.0
            }
            "OBJECT_ANGULAR_VELOCITY" -> {
                val body = if (sprite != null) physicsWorld?.getBody(sprite) else null
                body?.angularVelocity?.toDouble() ?: 0.0
            }

            // Collision detection
            "COLLIDES_WITH_EDGE" -> {
                if (sprite == null) 0.0 else {
                    val halfW = sprite.lookWidth / 2f * sprite.size / 100f
                    val halfH = sprite.lookHeight / 2f * sprite.size / 100f
                    val sw = (project.stageWidth ?: 480) / 2f
                    val sh = (project.stageHeight ?: 720) / 2f
                    if (sprite.x + halfW >= sw || sprite.x - halfW <= -sw ||
                        sprite.y + halfH >= sh || sprite.y - halfH <= -sh) 1.0 else 0.0
                }
            }
            "COLLIDES_WITH_FINGER" -> {
                if (sprite == null || !input.isTouched) 0.0 else {
                    val fx = input.fingerX
                    val fy = input.fingerY
                    val halfW = sprite.lookWidth / 2f * sprite.size / 100f
                    val halfH = sprite.lookHeight / 2f * sprite.size / 100f
                    if (fx >= sprite.x - halfW && fx <= sprite.x + halfW &&
                        fy >= sprite.y - halfH && fy <= sprite.y + halfH) 1.0 else 0.0
                }
            }

            // ═══════════════ Сцена ═══════════════
            "STAGE_WIDTH" -> project.stageWidth?.toDouble() ?: 480.0
            "STAGE_HEIGHT" -> project.stageHeight?.toDouble() ?: 720.0

            // ═══════════════ Мышь ═══════════════
            "MOUSE_X" -> input.mouseWorldX.toDouble()
            "MOUSE_Y" -> input.mouseWorldY.toDouble()
            "MOUSE_DELTA_X" -> input.mouseDeltaX.toDouble()
            "MOUSE_DELTA_Y" -> input.mouseDeltaY.toDouble()
            "MOUSE_SCROLL" -> input.mouseScroll.toDouble()

            // ═══════════════ Касание ═══════════════
            "FINGER_X" -> input.fingerX.toDouble()
            "FINGER_Y" -> input.fingerY.toDouble()
            "FINGER_TOUCHED" -> if (input.isTouched) 1.0 else 0.0
            "LAST_FINGER_INDEX" -> if (input.isTouched) 0.0 else -1.0
            "NUMBER_CURRENT_TOUCHES" -> if (input.isTouched) 1.0 else 0.0
            "INDEX_CURRENT_TOUCH" -> if (input.isTouched) 0.0 else -1.0

            // ═══════════════ Таймер ═══════════════
            "TIMER" -> timerSeconds.toDouble()

            // ═══════════════ Акселерометр / GPS (заглушки) ═══════════════
            "X_ACCELERATION" -> 0.0
            "Y_ACCELERATION" -> 0.0
            "Z_ACCELERATION" -> 0.0
            "X_INCLINATION" -> 0.0
            "Y_INCLINATION" -> 0.0
            "COMPASS_DIRECTION" -> 0.0
            "LATITUDE" -> 0.0
            "LONGITUDE" -> 0.0
            "ALTITUDE" -> 0.0
            "LOCATION_ACCURACY" -> 0.0

            // ═══════════════ Дата / Время ═══════════════
            "DATE_YEAR" -> java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toDouble()
            "DATE_MONTH" -> (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1).toDouble()
            "DATE_DAY" -> java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH).toDouble()
            "DATE_WEEKDAY" -> (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)).toDouble()
            "TIME_HOUR" -> java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY).toDouble()
            "TIME_MINUTE" -> java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE).toDouble()
            "TIME_SECOND" -> java.util.Calendar.getInstance().get(java.util.Calendar.SECOND).toDouble()

            // ═══════════════ Язык ═══════════════
            "USER_LANGUAGE" -> (System.getProperty("user.language") ?: "en") as Any?
            "SYSTEM_LANGUAGE" -> (System.getProperty("user.language") ?: "en") as Any?

            // ═══════════════ Дополнительные сенсоры (работающие на ПК) ═══════════════
            "FPS" -> com.badlogic.gdx.Gdx.graphics.framesPerSecond.toDouble()
            "IP" -> {
                try { java.net.InetAddress.getLocalHost().hostAddress ?: "0.0.0.0" }
                catch (_: Exception) { "0.0.0.0" }
            }
            "PORT" -> 0.0
            "INTERNET" -> {
                try {
                    val sock = java.net.Socket()
                    sock.connect(java.net.InetSocketAddress("8.8.8.8", 53), 500)
                    sock.close()
                    1.0
                } catch (_: Exception) { 0.0 }
            }
            "ARCH" -> System.getProperty("os.arch") ?: "unknown"
            "FREQ" -> 0.0
            "BATTARY" -> 100.0 // Desktop is always "fully charged"
            "LOUDNESS" -> 0.0

            // ═══════════════ ML Kit стubs (face / text) ═══════════════
            "FACE_DETECTED", "FACE_SIZE", "FACE_X", "FACE_Y" -> 0.0
            "SECOND_FACE_DETECTED", "SECOND_FACE_SIZE", "SECOND_FACE_X", "SECOND_FACE_Y" -> 0.0
            "TEXT_FROM_CAMERA" -> ""
            "TEXT_BLOCKS_NUMBER" -> 0.0

            // ═══════════════ Все остальное — 0.0 ═══════════════
            else -> 0.0
        }
    }

    /** Получить дочерний элемент по имени тега (первое вхождение). */
    private fun getChildElement(parent: Element, tagName: String): Element? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == tagName) {
                // Внутри leftChild/rightChild может быть formulaElement
                val subChildren = child.childNodes
                for (j in 0 until subChildren.length) {
                    val sub = subChildren.item(j)
                    if (sub.nodeType == Node.ELEMENT_NODE && sub.nodeName == "formulaElement") {
                        return sub as Element
                    }
                }
                return child as? Element
            }
        }
        return null
    }

    /** Получить список additionalChildren (элементов formulaElement). */
    private fun getAdditionalChildren(parent: Element): List<Element> {
        val result = mutableListOf<Element>()
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == "additionalChildren") {
                val addChildren = child.childNodes
                for (j in 0 until addChildren.length) {
                    val add = addChildren.item(j)
                    if (add.nodeType == Node.ELEMENT_NODE && add.nodeName == "formulaElement") {
                        result.add(add as Element)
                    }
                }
            }
        }
        return result
    }

    /** Достаёт текст из первого вхождения тега внутри parent (рекурсивно). */
    private fun getTagText(parent: Element, tag: String): String? {
        val nodes = parent.getElementsByTagName(tag)
        return if (nodes.length > 0) nodes.item(0).textContent?.trim() else null
    }

    private fun extractSoundName(el: Element): String? {
        val sound = el.getElementsByTagName("sound")?.item(0) as? Element
        return sound?.let { getTagText(it, "fileName") }
    }

    private fun extractVariableName(el: Element): String? {
        val userVar = el.getElementsByTagName("userVariable")?.item(0) as? Element
        return userVar?.let { getTagText(it, "name") }
    }

    private fun extractUserListName(el: Element): String? {
        val userList = el.getElementsByTagName("userList")?.item(0) as? Element
        return userList?.let { getTagText(it, "name") }
    }

    private fun extractTextContent(el: Element, tagName: String): String? {
        val field = el.getElementsByTagName(tagName)?.item(0) as? Element
        return field?.textContent?.trim()
    }

    /**
     * Извлечь строковое сообщение бродкаста (broadcastMessage / receivedMessage).
     * Поддерживает оба формата сериализации Catrobat:
     *  - plain text: <broadcastMessage>msg</broadcastMessage>
     *  - formula:     <broadcastMessage><formulaTree><type>STRING</type><value>msg</value>...
     * Используется и на стороне отправителя, и на стороне получателя, чтобы они
     * всегда совпадали.
     */
    private fun extractMessageText(parent: Element, tag: String): String {
        val field = parent.getElementsByTagName(tag)?.item(0) as? Element ?: return ""
        val ft = field.getElementsByTagName("formulaTree")?.item(0) as? Element
        if (ft != null) {
            val v = runCatching { evaluateFormulaNode(ft, 0) }.getOrNull()
            if (v != null) return v.toString()
        }
        return field.textContent?.trim() ?: ""
    }

    // ════════════════════════ FIREBASE REST (Realtime Database) ════════════════════════

    /** Построить REST-URL Firebase Realtime Database: <base>/<key>.json */
    private fun firebaseUrl(base: String, key: String): String {
        var url = base.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        url = url.trimEnd('/')
        val k = key.trim().trimStart('/').trimEnd('/')
        return "$url/$k.json"
    }

    /** Экранировать строку как JSON-строку (тело PUT). */
    private fun jsonString(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    /** Убрать обёртку JSON-строки из ответа Firebase. */
    private fun stripJsonString(s: String?): String {
        if (s == null) return ""
        val t = s.trim()
        if (t == "null" || t.isEmpty()) return ""
        if (t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length - 1)
                .replace("\\\\", "\\").replace("\\\"", "\"")
                .replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t")
        }
        return t
    }

    /** Синхронный REST-запрос к Firebase. Возвращает тело ответа или null при ошибке. */
    private fun firebaseRequest(method: String, url: String, body: String? = null): String? {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = method
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val resp = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            conn.disconnect()
            resp
        } catch (_: Exception) { null }
    }
}
