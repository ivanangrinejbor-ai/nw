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
        var elapsed: Float = 0f
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
    }

    /** Определение пользовательской процедуры (UserDefinedBrick). */
    private data class ProcedureDef(val paramNames: List<String>, val body: List<Block>)

    /** Состояние видеопроигрывателя (видео-блоки). Воспроизведение best-effort. */
    private data class VideoState(
        var fileName: String = "",
        var x: Float = 0f, var y: Float = 0f,
        var width: Float = 0f, var height: Float = 0f,
        var looped: Boolean = false,
        var playing: Boolean = false,
        var position: Float = 0f   // текущая позиция (сек)
    )

    /** Состояние исполнения одного скрипта. */
    private class ScriptState(
        val spriteIndex: Int,
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
                frames.removeAt(frames.lastIndex)
                // После выталкивания продвинуть IP родителя на один блок (минуя блок-родитель)
                val parent = frames.lastOrNull()
                if (parent != null) {
                    parent.ip++ // перешагнуть блок-родитель (forever/repeat/if)
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
    private var cloneCounter = 0                        // счётчик номеров клонов (cloneIndex)
    /** Локальная «база данных» таблиц (2D), ключ — имя таблицы. */
    private val localDb = mutableMapOf<String, MutableList<MutableList<Double>>>()
    /** Локальное облачное хранилище (Firebase-like), ключ — "id:key". */
    private val baseStore = mutableMapOf<String, String>()
    /** Проигрыватели видео (имя → состояние). Воспроизведение best-effort. */
    private val videos = mutableMapOf<String, VideoState>()
    private var runAsSpriteDepth = 0                    // глубина RunAsSpriteBrick (защита от рекурсии)
    private var scriptIndexForSprite: (Int) -> Int = { 0 }
    /** Сообщения для broadcast, ожидающие обработки в этом кадре. */
    private val pendingBroadcasts = mutableSetOf<String>()
    /** Флаг: был ли вызван ExitStage/FinishStage. */
    private var hasProjectExited = false
    /** Локальный HTTP-сервер (для Server-блоков). */
    private var localHttpServer: com.sun.net.httpserver.HttpServer? = null

    init {
        parseProject()
    }

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

        // Audio fade processing
        processAudioFades(deltaSeconds)

        // ── Фаза 1: проверка событий для event-скриптов ──
        checkEvents()

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
                    // Получатель broadcast: проверяем, есть ли активное сообщение
                    // Broadcast-сообщения собираются из executeControl при обработке broadcast_blocks
                    val msg = state.eventParam ?: ""
                    if (msg.isNotEmpty() && pendingBroadcasts.remove(msg)) {
                        state.eventFired = true
                    }
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

    /** Обработать glide-анимацию на любом фрейме в стеке. */
    private fun processGlideForState(state: ScriptState, delta: Float) {
        for (frame in state.frames) {
            val g = frame.glideState ?: continue
            g.elapsed += delta
            val dur = if (g.duration > 0f) g.duration else 1f
            val t = (g.elapsed / dur).coerceIn(0f, 1f)
            val sprite = project.sprites.getOrNull(state.spriteIndex) ?: return
            sprite.x = g.startX + (g.targetX - g.startX) * t
            sprite.y = g.startY + (g.targetY - g.startY) * t
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
                // broadcast — отправляем сообщение всем получателям
                val msg = block.args.getOrNull(1) as? String ?: ""
                if (msg.isNotEmpty()) {
                    pendingBroadcasts.add(msg)
                }
                frame.ip++
            }
            "broadcast_wait" -> {
                // broadcast and wait — отправляем сообщение всем получателям
                val msg = block.args.getOrNull(1) as? String ?: ""
                if (msg.isNotEmpty()) {
                    pendingBroadcasts.add(msg)
                }
                frame.ip++
            }
            "clone" -> {
                // клонировать спрайт
                val sprite = project.sprites.getOrNull(state.spriteIndex)
                if (sprite != null) {
                    val clone = sprite.copy()
                    project.sprites.add(clone)
                }
                frame.ip++
            }
            "delete_clone" -> {
                // удалить текущий спрайт (клон)
                val spriteIdx = state.spriteIndex
                if (spriteIdx > 0 && spriteIdx < project.sprites.size) {
                    project.sprites.removeAt(spriteIdx)
                    state.frames.clear() // остановить скрипт клона
                }
                frame.ip = frame.blocks.size // выход
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
            "for_item_list" -> {
                val listName = block.args.getOrNull(1) as? String ?: ""
                val varName = block.args.getOrNull(2) as? String ?: ""
                val list = userLists[listName]
                val items = (list?.size ?: 0)
                // Iterate: for each item, execute children. Children handle repeat pattern.
                if (items > 0) {
                    state.frames.add(Frame(block.children, repeatRemaining = items))
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
                val tryBlocks = block.args.getOrNull(2) as? List<Block> ?: block.children
                @Suppress("UNCHECKED_CAST")
                val catchBlocks = block.args.getOrNull(3) as? List<Block> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val finallyBlocks = block.args.getOrNull(4) as? List<Block> ?: emptyList()
                // Execute try blocks; if error, execute catch blocks; always execute finally blocks
                state.frames.add(Frame(tryBlocks, repeatRemaining = 0))
                frame.ip++
            }
            "switch_begin" -> {
                val switchVal = block.args.getOrNull(1) as? String ?: ""
                // Parse case values from the args
                val caseList = block.args.drop(2).filterIsInstance<String>()
                // Simple fall-through: execute first matching case
                var matchFound = false
                for ((ci, caseStr) in caseList.withIndex()) {
                    if (matchFound || caseStr == switchVal) {
                        matchFound = true
                    }
                }
                if (!matchFound) frame.ip++
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
                    clone.cloneIndex = ++cloneCounter
                    project.sprites.add(clone)
                }
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

    private fun executeMotion(block: Block, sprite: DesktopSprite, frame: Frame) {
        val body = physicsWorld?.getBody(sprite)
        when (block.args.getOrNull(0) as? String) {
            "move_steps" -> {
                val steps = (block.args.getOrNull(1) as? Number)?.toFloat() ?: 0f
                val angleRad = Math.toRadians(sprite.direction.toDouble()).toFloat()
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
                val w = project.stageWidth ?: 480
                val h = project.stageHeight ?: 720
                sprite.x = (Math.random() * w).toFloat()
                sprite.y = (Math.random() * h).toFloat()
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
                sprite.direction = (sprite.direction + 180f) % 360f
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
                sprite.direction = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
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
                    sprite.direction = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
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

            // ── Core game logic (remaining portable) ──
            "clone_and_name" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty()) {
                    val clone = sprite.copy()
                    clone.name = name
                    clone.cloneIndex = ++cloneCounter
                    project.sprites.add(clone)
                }
            }
            "delete_clone_by_number" -> {
                val n = (block.args.getOrNull(1) as? Number)?.toInt() ?: 0
                val idx = project.sprites.indexOfFirst { it.cloneIndex == n && n > 0 }
                if (idx >= 0) project.sprites.removeAt(idx)
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

            // ── Fast2D bricks (best-effort на Desktop: 2D-спрайты + Box2D) ──
            "fast2d_create" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                if (name.isNotEmpty() && project.sprites.none { it.name == name }) {
                    val ns = DesktopSprite(name = name)
                    ns.cloneIndex = ++cloneCounter
                    project.sprites.add(ns)
                }
            }
            "fast2d_delete" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val idx = project.sprites.indexOfFirst { it.name == name }
                if (idx >= 0) project.sprites.removeAt(idx)
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
            // SetBufferCamera / SetBufferCamera3D: off-screen buffers have no 2D equivalent.
            "set_buffer_camera", "set_buffer_camera_3d" -> {
                // No-op: buffer cameras are not rendered in the desktop player.
            }
        }
        frame.ip++
    }

    // ──────────────────────────── SOUND ────────────────────────────

    private fun executeSound(block: Block, sprite: DesktopSprite, frame: Frame) {
        when (block.args.getOrNull(0) as? String) {
            "play_sound" -> {
                val path = block.args.getOrNull(1) as? String ?: ""
                if (path.isNotEmpty()) {
                    AudioServiceHolder.audioService?.playSoundFile(path, sprite.name)
                }
            }
            "play_sound_wait" -> {
                val path = block.args.getOrNull(1) as? String ?: ""
                if (path.isNotEmpty()) {
                    AudioServiceHolder.audioService?.playSoundFile(path, sprite.name)
                    // простейшая реализация: ждём 1 секунду
                    frame.waitTimer = 1f
                }
            }
            "stop_sound" -> {
                val path = block.args.getOrNull(1) as? String ?: ""
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
                val path = block.args.getOrNull(1) as? String ?: ""
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
                val instName = block.args.getOrNull(1) as? String ?: ""
                if (instName.isNotEmpty()) {
                    AudioServiceHolder.audioService?.stopSoundInSprite(instName, sprite.name)
                }
            }
            "play_sound_at" -> {
                val sound = block.args.getOrNull(1) as? String ?: ""
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
                val file = block.args.getOrNull(1) as? String ?: ""
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
        val VIRTUAL_WIDTH = 1280f
        val VIRTUAL_HEIGHT = 720f
        fun toScreenX(x: Float) = VIRTUAL_WIDTH / 2f + x
        fun toScreenY(y: Float) = VIRTUAL_HEIGHT / 2f - y

        when (block.args.getOrNull(0) as? String) {
            "pen_down" -> sprite.penDown = true
            "pen_up" -> sprite.penDown = false
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
                val value = evalBlockArgFloat(block, 2, sprite, state) ?: 0f
                if (name.isNotEmpty()) variables[name] = value
            }
            "change" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                val delta = evalBlockArgFloat(block, 2, sprite, state) ?: 0f
                if (name.isNotEmpty()) {
                    val old = getVariableFloat(name)
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
                val name = block.args.getOrNull(1) as? String ?: ""
                val x = (block.args.getOrNull(2) as? Number)?.toFloat() ?: 0f
                val y = (block.args.getOrNull(3) as? Number)?.toFloat() ?: 0f
                // показ переменной на экране
            }
            "hide_variable" -> {
                val name = block.args.getOrNull(1) as? String ?: ""
                // скрыть переменную
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
                        val conn = java.net.URI(url).toURL().openConnection()
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
                    val path = block.args.getOrNull(1) as? String ?: ""
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
                                val outFile = java.io.File(destDir, entry.name)
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
                    val path = block.args.getOrNull(1) as? String ?: ""
                    val f = java.io.File(path)
                    if (f.exists()) f.delete()
                }
                "open_file" -> {
                    val path = block.args.getOrNull(1) as? String ?: ""
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
                    val path = block.args.getOrNull(1) as? String ?: ""
                    val varName = block.args.getOrNull(2) as? String ?: ""
                    val f = java.io.File(path)
                    if (f.exists() && varName.isNotEmpty()) {
                        val content = f.readText()
                        variables[varName] = content
                    }
                }
                "write_to_files" -> {
                    val path = block.args.getOrNull(1) as? String ?: ""
                    val varName = block.args.getOrNull(2) as? String ?: ""
                    val f = java.io.File(path)
                    if (varName.isNotEmpty()) {
                        val value = getVariable(varName)?.toString() ?: ""
                        f.parentFile?.mkdirs()
                        f.writeText(value)
                    }
                }
                "read_variable_from_file" -> {
                    val path = block.args.getOrNull(1) as? String ?: ""
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
                    val path = block.args.getOrNull(1) as? String ?: ""
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
                    val path = block.args.getOrNull(1) as? String ?: ""
                    val folder = block.args.getOrNull(2) as? String ?: ""
                    if (path.isNotEmpty() && folder.isNotEmpty()) {
                        java.io.File(path, folder).mkdirs()
                    }
                }
                "delete_folder_path" -> {
                    val path = block.args.getOrNull(1) as? String ?: ""
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
                    } else if (eventType == "bounce_off") {
                        // WhenBounceOffScript – имя спрайта в <spriteToBounceOffName>
                        eventParam2 = extractTextContent(scriptEl, "spriteToBounceOffName")
                    } else if (eventType == "gamepad_button") {
                        eventParam2 = extractTextContent(scriptEl, "action")
                    } else if (eventType == "mouse_clicked") {
                        eventParam2 = extractTextContent(scriptEl, "buttonCode")
                    } else if (eventType == "broadcast_receiver") {
                        eventParam2 = extractTextContent(scriptEl, "receivedMessage")
                    } else if (eventType == "scene_start") {
                        eventParam2 = extractTextContent(scriptEl, "sceneToStart")
                    }

                    val (bricks, runtimeFormulas) = parseBrickListRecursive(brickListEl.childNodes, 0, i)

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
    private fun parseBrickListRecursive(nodes: NodeList, startIdx: Int, spriteIndex: Int = 0): Pair<List<Block>, List<RuntimeFormula>> {
        // Локальные обёртки, чтобы передавать spriteIndex во все вызовы extractFormulaValue/String
        fun extractFormulaValue(el: Element, field: String) =
            this@DesktopScriptEngine.extractFormulaValue(el, field, spriteIndex)
        fun extractFormulaString(el: Element, field: String) =
            this@DesktopScriptEngine.extractFormulaString(el, field, spriteIndex)

        val result = mutableListOf<Block>()
        val allRuntimeFormulas = mutableListOf<RuntimeFormula>()
        var idx = startIdx
        while (idx < nodes.length) {
            val node = nodes.item(idx)
            if (node.nodeType != Node.ELEMENT_NODE) { idx++; continue }
            val el = node as Element
            val brickType = el.getAttribute("type")
            if (brickType.isBlank()) { idx++; continue }

            when (brickType) {
                // ── Loop containers ──
                "ForeverBrick" -> {
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("forever"), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "RepeatBrick" -> {
                    val times = extractFormulaValue(el, "TIMES_TO_REPEAT")
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("repeat", times ?: 1), children))
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
                    val fromVal = extractFormulaValue(el, "FOR_LOOP_FROM") ?: 0f
                    val toVal = extractFormulaValue(el, "FOR_LOOP_TO") ?: 0f
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    val count = maxOf(1, (toVal - fromVal + 1f).toInt())
                    if (varName != null) variables[varName] = fromVal
                    // Добавляем синтетический блок для инкремента переменной в конце каждой итерации
                    val loopChildren = if (varName != null) {
                        children + Block(Block.Type.VARIABLE, listOf("inc_var", varName))
                    } else children
                    result.add(Block(Block.Type.CONTROL, listOf("repeat", count), loopChildren))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "ScheduleBrick" -> {
                    val delay = extractFormulaValue(el, "TIME_TO_WAIT_IN_SECONDS") ?: 1f
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("wait", delay)))
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
                    val count = extractFormulaValue(el, "TIMES_TO_REPEAT") ?: 1f
                    val interval = extractFormulaValue(el, "INTERVAL") ?: 0f
                    val (children, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("interval_repeat", count.toInt(), interval), children))
                    idx = findLoopEnd(nodes, idx + 1)
                }
                "TryCatchFinallyBrick" -> {
                    // try section
                    val (tryChildren, rf1) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf1)
                    var nextIdx = findTryCatchEnd(nodes, idx + 1)
                    // catch section (after CatchBrick marker)
                    var catchBricks = emptyList<Block>()
                    var catchVar = ""
                    val catchNode = if (nextIdx < nodes.length) nodes.item(nextIdx) else null
                    if (catchNode != null && catchNode.nodeType == Node.ELEMENT_NODE) {
                        val catchEl = catchNode as Element
                        if (catchEl.getAttribute("type") == "CatchBrick") {
                            catchVar = extractVariableName(catchEl) ?: ""
                            val (cb, rf2) = parseBrickListRecursive(nodes, nextIdx + 1, spriteIndex)
                            allRuntimeFormulas.addAll(rf2)
                            catchBricks = cb
                            nextIdx = findTryCatchEnd(nodes, nextIdx + 1)
                        }
                    }
                    // finally section (after FinallyBrick marker)
                    var finallyBricks = emptyList<Block>()
                    val finallyNode = if (nextIdx < nodes.length) nodes.item(nextIdx) else null
                    if (finallyNode != null && finallyNode.nodeType == Node.ELEMENT_NODE) {
                        val finallyEl = finallyNode as Element
                        if (finallyEl.getAttribute("type") == "FinallyBrick") {
                            val (fb, rf3) = parseBrickListRecursive(nodes, nextIdx + 1, spriteIndex)
                            allRuntimeFormulas.addAll(rf3)
                            finallyBricks = fb
                            nextIdx = findTryCatchEnd(nodes, nextIdx + 1)
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
                            val (caseChildren, rf) = parseBrickListRecursive(nodes, si + 1, spriteIndex)
                            allRuntimeFormulas.addAll(rf)
                            caseBlocks.add(caseVal to caseChildren)
                            // Find end of this case (next SwitchCaseBrick or SwitchEndBrick/LoopEndBrick)
                            var ci = si + 1
                            while (ci < nodes.length) {
                                val cn = nodes.item(ci)
                                if (cn.nodeType != Node.ELEMENT_NODE) { ci++; continue }
                                val ce = cn as Element
                                val ctype = ce.getAttribute("type")
                                if (ctype == "SwitchCaseBrick" || ctype == "SwitchEndBrick" || ctype == "LoopEndBrick") break
                                ci++
                            }
                            si = ci
                            continue
                        }
                        si++
                    }
                    result.add(Block(Block.Type.CONTROL, listOf("switch_begin", switchVal, caseBlocks.map { it.first + ":" })))
                    idx = findLoopEnd(nodes, si)
                }
                "LoopEndBrick" -> return Pair(result, allRuntimeFormulas)

                // ── If container ──
                "IfLogicBeginBrick" -> {
                    val condVal = extractFormulaValue(el, "IF_CONDITION")
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

                    result.add(Block(Block.Type.CONTROL, listOf("if", condVal ?: 0f, thenChildren, elseChildren), thenChildren))
                    idx = afterIfIdx
                }
                "IfLogicElseBrick" -> {
                    return Pair(result, allRuntimeFormulas)
                }
                "IfLogicEndBrick" -> {
                    return Pair(result, allRuntimeFormulas)
                }
                "IfThenLogicBeginBrick" -> {
                    val condVal = extractFormulaValue(el, "IF_CONDITION")
                    val (thenChildren, rf) = parseBrickListRecursive(nodes, idx + 1, spriteIndex)
                    allRuntimeFormulas.addAll(rf)
                    result.add(Block(Block.Type.CONTROL, listOf("if", condVal ?: 0f, thenChildren, emptyList<Block>()), thenChildren))
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
                    val varName = extractVariableName(el)
                    val x = extractFormulaValue(el, "X_POSITION") ?: 0f
                    val y = extractFormulaValue(el, "Y_POSITION") ?: 0f
                    result.add(Block(Block.Type.VARIABLE, listOf("show_variable", varName ?: "", x, y)))
                    idx++
                }
                "HideTextBrick" -> {
                    val varName = extractVariableName(el)
                    result.add(Block(Block.Type.VARIABLE, listOf("hide_variable", varName ?: "")))
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
                    val msg = extractTextContent(el, "broadcastMessage") ?: ""
                    result.add(Block(Block.Type.EVENT, listOf("broadcast", msg)))
                    idx++
                }
                "BroadcastWaitBrick" -> {
                    val msg = extractTextContent(el, "broadcastMessage") ?: ""
                    result.add(Block(Block.Type.EVENT, listOf("broadcast_wait", msg)))
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
                    val parsed = parseBrickLeaf(el, brickType)
                    if (parsed != null) result.add(parsed)
                    idx++
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
    private fun findTryCatchEnd(nodes: NodeList, startIdx: Int): Int {
        var depth = 0
        for (i in startIdx until nodes.length) {
            val node = nodes.item(i)
            if (node.nodeType != Node.ELEMENT_NODE) continue
            val el = node as Element
            val type = el.getAttribute("type")
            when (type) {
                "TryCatchFinallyBrick", "ForeverBrick", "RepeatBrick", "IfLogicBeginBrick" -> depth++
                "CatchBrick", "FinallyBrick" -> if (depth == 0) return i + 1
                "EndBrick" -> if (depth == 0) return i + 1 else depth--
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
            "WhenClonedBrick" -> Block(Block.Type.EVENT, listOf("cloned"))

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

            "TEXT_BLOCK_X", "TEXT_BLOCK_Y", "TEXT_BLOCK_SIZE",
            "TEXT_BLOCK_FROM_CAMERA", "TEXT_BLOCK_LANGUAGE_FROM_CAMERA" -> 0.0

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
                    val fx = input.fingerX - 640f // convert screen to stage
                    val fy = 360f - input.fingerY
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
            "MOUSE_X" -> input.mouseX.toDouble()
            "MOUSE_Y" -> input.mouseY.toDouble()
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
