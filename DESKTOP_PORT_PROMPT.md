# ПРОМПТ: Завершить Windows Desktop Player для NeoCatroid (форк Catroid)

> Этот промпт — самодостаточная инструкция для ИИ-агента, который продолжит порт
> Catroid под Windows desktop. Всё ниже ПРОВЕРЕНО по реальному коду репозитория
> `C:\Users\ivanp\NewCatroid` (состояние на момент передачи). Не выдумывай API —
> исследуй код перед правкой, как описано в разделе «Стиль работы».

---

## 0. TL;DR — что вообще делаем

Сделать **полный Catroid-порт под Windows desktop**: отдельное JVM-приложение-плеер,
которое исполняет проекты Catroid (все 30 категорий блоков) на десктопе БЕЗ Android.
Итоговый артефакт — `.zip` с пребилднутым `.exe`-рантаймом + зашифрованным проектом,
собираемый через `build_exe.bat` (launch4j + иконка `icon/icon.png`).

Уже сделано (см. раздел 2): аудит+фикс 30 категорий, фундамент `RuntimeServices`,
модуль `:core`, **Audio seam (полностью)**, анализ графики (portable libGDX),
**Text seam (полностью)**. Дальше по порядку user: **Файлы → Уведомления → (миграция
логики в :core) → :desktop-runtime → Desktop-реализации seam → Stage/рендер →
LunoScript desktop → Экспорт/шифрование → Упаковка**.

---

## 1. Факты о репозитории (проверено)

- Корень: `C:\Users\ivanp\NewCatroid`. Форк Catrobat/Catroid (Kotlin/Java, Android, libGDX).
- Gradle-модули (`settings.gradle`, строки 24-28, ПРОВЕРЕНО):
  - `include ':catroid'` — основной Android-library модуль (вся логика сейчас тут).
  - `include ':core'` — **чистый JVM-модуль** (создан нами), дом для платформонезависимых seam-интерфейсов и (в будущем) портативной игровой логики.
  - `include ':lunoscript-processor'`, `include ':lunoscript-annotations'` — аннотации/процессор для LunoScript.
  - `include ':vncclient'` — VNC-клиент (не в приоритете).
- `core/build.gradle` (ПРОВЕРЕНО): `plugins { id 'org.jetbrains.kotlin.jvm' }`,
  зависимость `implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"`.
  НЕТ android-плагина. Значит `:core` компилируется на обычной JVM.
- `catroid/build.gradle` — Android library (`com.android.library`), тянет
  `implementation project(':core')`.
- Сборка Android (проверка каждого среза):  
  `cd C:\Users\ivanp\NewCatroid; ./gradlew :catroid:compileCatroidDebugSources --offline -q`
  - Флаг `--offline` ОБЯЗАТЕЛЕН (сеть может быть недоступна; при ошибках сети пробуй
    без `--offline`, но это риск). `-q` подавляет шум.
  - Критерий готовности среза: **BUILD SUCCESSFUL** (warnings deprecation/unchecked — OK).
- Сборка `:core` (когда туда перенесёшь логику): `./gradlew :core:compileKotlin --offline -q`.

---

## 2. Что УЖЕ сделано (с доказательствами — файлы существуют и компилируются)

### 2.1 Аудит + фикс 30 категорий блоков — ЗАВЕРШЕНО, BUILD SUCCESSFUL.
### 2.2 Аудит + фикс подсистемы формул — ЗАВЕРШЕНО, BUILD SUCCESSFUL (вкл. фикс `FormulaEditorFragment.java:1046` missing return).
### 2.3 Фундамент `RuntimeServices` — ГОТОВ:
- `core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt` (интерфейс, ПРОВЕРЕНО):
  ```kotlin
  interface RuntimeServices {
      fun getExternalStorageDir(): String
      fun getDownloadsDir(): String
      fun postToMainThread(runnable: Runnable)
      fun postDelayed(runnable: Runnable, delayMs: Long)
      fun isGpsAvailable(): Boolean
      fun hasVibrator(): Boolean
      fun vibrate(durationMs: Long)
  }
  ```
- `core/src/main/java/org/catrobat/catroid/runtime/RuntimeServicesHolder.kt`:
  `object RuntimeServicesHolder { lateinit var services: RuntimeServices }`
- `catroid/.../runtime/AndroidRuntimeServices.kt` (ПРОВЕРЕНО): делегирует
  `Environment.getExternalStorageDirectory()`, `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)`,
  `Handler(Looper.getMainLooper())`, `VibrationManager`. Помечен «pure additive».
- Зафиксированные вызовы: `VibrateAction`, `RunOnUiThreadAction`, `ScheduleAction`, путь Downloads в `FormulaElement`.
- Init в `StageActivity.onCreate` (см. раздел 3.4).

### 2.4 Модуль `:core` СОЗДАН (ПРОВЕРЕНО: `settings.gradle` + `core/build.gradle`).
Текущее содержимое `:core` (ПРОВЕРЕНО через `Get-ChildItem`):
```
org/catrobat/catroid/audio/AudioService.kt
org/catrobat/catroid/audio/AudioServiceHolder.kt
org/catrobat/catroid/audio/MidiService.kt
org/catrobat/catroid/audio/MidiServiceHolder.kt
org/catrobat/catroid/pocketmusic/note/Drum.java          (ПЕРЕНЕСЁН из :catroid)
org/catrobat/catroid/pocketmusic/note/MusicalInstrument.java (ПЕРЕНЕСЁН из :catroid)
org/catrobat/catroid/runtime/RuntimeServices.kt
org/catrobat/catroid/runtime/RuntimeServicesHolder.kt
org/catrobat/catroid/text/RasterizedText.kt
org/catrobat/catroid/text/TextService.kt
org/catrobat/catroid/text/TextServiceHolder.kt
```

### 2.5 AUDIO SEAM — ПОЛНОСТЬЮ ГОТОВ (срезы 1–4), BUILD SUCCESSFUL.
- `core/.../audio/AudioService.kt` (ПРОВЕРЕНО, 31 строка) — интерфейс:
  `setVolume/getVolume/setPan/getPan/setPitch/getPitch/stopAllSounds/clear/pause/resume`
  + Sprite-методы `playSoundFile(filePath,spriteName)`,
  `playSoundFileWithStartTime(filePath,spriteName,startTime:Int)`,
  `stopSoundInSprite(filePath,spriteName)`, `setVolumeForSound(filePath,spriteName,volume:Float)`
  + `playTone(samples:ShortArray,sampleRate:Int)`, `stopTone()`,
  `setEqualizerBand(band:Int,gain:Short)`, `isSoundPlaying(soundFilePath,spriteName):Boolean`.
- `core/.../audio/MidiService.kt` (ПРОВЕРЕНО) — интерфейс:
  `playSoundFile/playSoundFileWithStartTime/playDrumForBeats(drum,beats,spriteName)/
  playNoteForBeats(midiValue,beats)/stopSoundInSprite/setInstrument/getInstrument/
  setTempo/getTempo/setVolume/getVolume/stopAllSounds/pause/resume/getDurationForBeats/reset`.
  Импортирует `org.catrobat.catroid.pocketmusic.note.Drum` и `.MusicalInstrument` (теперь в :core).
- `core/.../audio/AudioServiceHolder.kt`, `MidiServiceHolder.kt` — `object` с `lateinit`.
- `catroid/.../audio/AndroidAudioService.kt` (ПРОВЕРЕНО, 118 строк) — делегирует
  `SoundManager.getInstance()` + `MidiSoundManager.getInstance()`; tone/equalizer через
  `android.media.AudioTrack`/`android.media.audiofx.Equalizer`; Sprite резолвится по имени
  через `ProjectManager.getInstance().getCurrentlyPlayingScene().spriteList.firstOrNull { it.name == name }`.
- `catroid/.../audio/AndroidMidiService.kt` (ПРОВЕРЕНО) — делегирует `MidiSoundManager`.
- `Drum.java`/`MusicalInstrument.java` ПЕРЕНЕСЕНЫ в `:core` (git mv, FQN не изменился:
  `org.catrobat.catroid.pocketmusic.note.*`).
- 13 экшенов зашиты на holder-ы (volume/pan/pitch/fade/PlaySound/PlaySoundAt/StopSound/
  SetSoundVolume/PlayNote/PlayDrum/SetInstrument/SetTempo/ChangeTempo/StopAllSounds +
  PlayTone/Equalizer/WaitForSound). Экшены остались в `:catroid` (Kotlin/Java) и зовут
  `AudioServiceHolder.audioService.*` / `MidiServiceHolder.midiService.*`.

### 2.6 ГРАФИКА — ПРОАНАЛИЗИРОВАНА (важное уточнение):
- Render-ядро (`StageListener`, `TextActor`, `PenActor`, `PlotActor`, `ShowBubbleActor`,
  `Passepartout`) использует libGDX (`Texture`/`Pixmap`/`BitmapFont`/`FrameBuffer`) — portable.
- **НО** `StageListener.java` (ПРОВЕРЕНО, импорты в начале файла) всё же тянет
  `android.content.Context`, `android.content.res.Resources`, `android.os.SystemClock`,
  `android.provider.Settings`, `android.util.DisplayMetrics`, `android.util.Log`,
  `android.view.WindowManager`. То есть «графика portable» требует правки `StageListener`
  (заменить android-вызовы на libGDX-эквиваленты: `Gdx.graphics`, `TimeUtils`,
  `java.util.logging` вместо `Log`). См. раздел 6.4.
- `ShowTextActor.java` уже переписан (см. 2.7) — он больше НЕ android.graphics.
- `DrawTextAction.kt` (см. раздел 8, appendix) ВСЁ ЕЩЁ использует `android.graphics.*`
  (Bitmap/Canvas/Paint/GLES20/GLUtils) + `StageActivity.getActiveStageListener()`. Его
  тоже надо зашить через `TextServiceHolder` (как `ShowTextActor`). Это «хвост» текст-шага.

### 2.7 TEXT SEAM — ГОТОВ (срез A+B), BUILD SUCCESSFUL:
- `core/.../text/TextService.kt` (ПРОВЕРЕНО):
  ```kotlin
  interface TextService {
      fun rasterizeText(
          text: String, textSizePx: Float, color: String?, typefaceName: String?,
          isWrapped: Boolean, alignment: Int
      ): RasterizedText
  }
  ```
- `core/.../text/RasterizedText.kt` (ПРОВЕРЕНО): `data class RasterizedText(val width: Int, val height: Int, val rgba: ByteArray)` (RGBA8888).
- `core/.../text/TextServiceHolder.kt`: `object TextServiceHolder { lateinit var textService: TextService }`.
- `catroid/.../text/AndroidTextService.kt` (ПРОВЕРЕНО, 87 строк): `Paint`+`Canvas`+`Bitmap`
  → массив RGBA-байт; `typefaceName` трактует как путь к файлу шрифта
  (`Typeface.createFromFile`); цвет через `ShowTextUtils.calculateColorRGBs`.
- `catroid/.../stage/ShowTextActor.java` ПЕРЕПИСАН (ПРОВЕРЕНО, 255 строк): убраны
  `android.graphics.*`; рендер через `TextServiceHolder.textService.rasterizeText(...)`
  + `buildTexture()` собирает libGDX `Texture` из `Pixmap` (RGBA8888) из `rt.getRgba()`.
  `setFont(android.graphics.Typeface)` → `setFont(String typefaceName)`.
- 3 вызывающих (`ShowTextFontAction`, `ShowVarFontAction`, `ShowTextRotationAction`):
  `setFont(font)` → `setFont(file.getAbsolutePath())`; мёртвые `Typeface font = ...` удалены.

---

## 3. УСТАНОВЛЕННЫЙ ПАТТЕРН (ШАБЛОН SEAM) — ОБЯЗАТЕЛЬНО ПРИДЕРЖИВАЙСЯ

Каждый «шов» между портативной логикой и платформой делается ТОЧНО так:

1. **Интерфейс** в `:core` (пакет `org.catrobat.catroid.<area>`), без `android.*`/`java.awt.*`.
   Для свойств — явные `getX()/setX()` (Kotlin-интерфейс НЕ синтезирует property из них).
2. **Holder** в `:core`: `object XxxServiceHolder { lateinit var xxx: XxxService }`.
3. **Android-реализация** в `:catroid` (тот же пакет), делегирует реальному Android-API.
4. **Wire calls**: в портативных экшенах/классах вызовы идут через `XxxServiceHolder.xxx.method(...)`.
5. **Init** в `StageActivity.onCreate`: `XxxServiceHolder.xxx = AndroidXxx()`.

Пример реального init (ПРОВЕРЕНО, `StageActivity.java` строки 272-282):
```java
org.catrobat.catroid.runtime.RuntimeServicesHolder.services =
        new org.catrobat.catroid.runtime.AndroidRuntimeServices(this);
org.catrobat.catroid.audio.AudioServiceHolder.audioService =
        new org.catrobat.catroid.audio.AndroidAudioService();
org.catrobat.catroid.audio.MidiServiceHolder.midiService =
        new org.catrobat.catroid.audio.AndroidMidiService();
org.catrobat.catroid.text.TextServiceHolder.textService =
        new org.catrobat.catroid.text.AndroidTextService();
```

Позже Desktop-реализация кладётся в `:desktop-runtime` (тот же пакет), и `DesktopStage`
делает `XxxServiceHolder.xxx = DesktopXxx()`. Holder в `:core` — поэтому ОБЕ платформы
могут его установить. Это ключевая причина, почему интерфейсы и holder-ы живут в `:core`.

**Логирование:** в `:core` НЕЛЬЗЯ использовать `android.util.Log` (оно сломает desktop-сборку).
Создай в `:core` портативный логгер, напр. `org.catrobat.catroid.util.Logger` (обёртка над
`java.util.logging.Logger` или `println` с тегом), и используй его вместо `Log` во всех
файлах, переносимых в `:core`. В `:catroid` `android.util.Log` пока допустим (это Android).

---

## 4. ⚠️ КРИТИЧЕСКАЯ АРХИТЕКТУРНАЯ РАЗВИЛКА (прочитай ДО кодинга)

Сейчас `:catroid` — Android library. Его классы (в т.ч. `StageListener`, экшены, брики,
формулы) ссылаются на `android.*` напрямую во многих местах, не только в seam-имплементациях.
**Нельзя просто запустить `:catroid` на desktop-JVM** — он не соберётся без `android.jar`.

Поэтому рекомендуемый путь (выбери его, но ПЕРЕД большим переносом подтверди у пользователя):

**A) Постепенно переноси портативную игровую логику в `:core`.**
   - `:core` уже JVM-модуль. Туда перемещаются: интерфейсы seam (уже там), `StageListener`
     (после чистки android-вызовов), экшены/брики/формулы/контент, `LunoScript`, утилиты.
   - `android.*` внутри переносимого кода заменяется на: seam-holder вызовы,
     libGDX-API (`Gdx.*`), чистый JDK (`java.io`, `java.util`, `java.util.logging`),
     либо новые интерфейсы в `:core`.
   - `:catroid` становится «тонкой Android-оболочкой»: `StageActivity`, UI, Android-реализации
     seam, интенты, пермишены. Он зависит от `:core`.
   - Тогда desktop-плеер = `:core` + `:desktop-runtime` (+ libGDX LWJGL backend для рендера).

**B) Создать модуль `:desktop-runtime`** (`org.jetbrains.kotlin.jvm`, зависит от `:core`):
   он предоставляет Desktop-реализации всех seam. Подробно — раздел 6.6.

Альтернатива (НЕ рекомендуется, сложнее): пытаться рантить `:catroid` целиком через
android-stubs — нерабочая для реального рантайма.

**Рендер-stage на desktop:** libGDX уже зависимость (Android backend). Добавить
`com.badlogic.gdx:gdx-backend-lwjgl3` для desktop. ⚠️ РИСК: офлайн-Gradle не сможет
скачать LWJGL-артефакты, если их нет в `.gradle/caches`. **СНАЧАЛА проверь наличие**
`gdx-backend-lwjgl3` в кэше Gradle (`~/.gradle/caches/` или `C:\Users\ivanp\.gradle\caches`).
Если нет сети — это блокер для ВИЗУАЛИЗАЦИИ; тогда либо подними локальное зеркало/кэш,
либо делай сначала headless-исполнение логики без отрисовки, либо собери LWJGL отдельно и
положи jar-ы в `libs/`. Не зацикливайся — логика важнее пикселей на первом этапе.

**Text/Audio/Midi/Files/Notifications на desktop можно сделать на ЧИСТОМ JDK** (без LWJGL),
чтобы не зависеть от сети:
   - `DesktopTextService`: `java.awt.Font` + `java.awt.Graphics2D` + `java.awt.image.BufferedImage`
     → RGBA-байты (точное зеркало `AndroidTextService`, тот же контракт `rasterizeText`).
   - `DesktopAudioService`: `javax.sound.sampled` (`Clip`/`SourceDataLine`/`AudioFormat`).
     `playTone(samples,sampleRate)` — `SourceDataLine` с PCM-16; `setEqualizerBand` —
     `FloatControl`/`BooleanControl` (или no-op, если не поддерживается); `isSoundPlaying` —
     отслеживать активные клипы в Map.
   - `DesktopMidiService`: `javax.sound.midi` (синтез нот/барабанов через `Synthesizer`/`MidiChannel`)
     либо делегировать в `DesktopAudioService.playTone` для простых тонов.
   - `DesktopFileService` (или просто `RuntimeServices`): `java.io.File` (уже portable).
     Storage-root → `user.home` / рабочая папка плеера.
   - `DesktopNotificationService`: `java.awt.SystemTray` + `TrayIcon`, либо простое немодальное окно.

---

## 5. Что ЕЩЁ предстоит (по приоритету, выбранному пользователем)

Порядок user: **аудио → графика → текст → файлы → уведомления**.
Аудио/графика(анализ)/текст ЗАКРЫТЫ. Следующие срезы:

### 5.1 FILES seam (СЛЕДУЮЩИЙ — делай первым)
- Найти ВСЕ File-экшены в `catroid/src/main/java/org/catrobat/catroid/content/actions/`:
  `CreateFolderAction.kt`, `DeleteFolderAction.kt`, `CreateFolderByPathAction.kt`,
  `DeleteFolderByPathAction.kt`, `PutFileIntoFolderAction.kt`, `PutFileIntoPathAction.kt`,
  `CopyProjectFileAction.kt`, `CopyProjectFileToFolderAction.kt`, `CopyProjectFileToPathAction.kt`,
  `WriteVariableToFileAction.kt` (и `UnzipAction`, если есть).
  Брики (в `content/bricks/`): `CreateFolderBrick`, `DeleteFolderBrick`, `CreateFolderByPathBrick`,
  `DeleteFolderByPathBrick`, `PutFileIntoFolderBrick`, `PutFileIntoPathBrick`,
  `CopyProjectFileBrick`, `CopyProjectFileToFolderBrick`, `CopyProjectFileToPathBrick`.
  (ПРОВЕРЕНО: они зарегистрированы в `ActionFactory.java` строки ~4831-4901 и в
  `XstreamSerializer.java` строки ~386,675,828-844.)
- Реальная Android-зависимость в этих экшенах (ПРОВЕРЕНО на примере `PutFileIntoFolderAction.kt`):
  только `android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)`
  + `android.util.Log`. Большая часть — `java.io.File` + `project.getFile(name)` + canonical-path
  валидация (path-traversal защита УЖЕ на месте — НЕ ломай её).
- **Срез (узкий, механический):**
  1. Создай `FileService` (интерфейс в `:core`, `FileServiceHolder` в `:core`) ИЛИ просто
     расширь `RuntimeServices` методом `getDownloadsDir()` (он УЖЕ есть в `RuntimeServices`!).
     → значит для downloads-рута НЕ нужен новый seam: используй
     `RuntimeServicesHolder.services.getDownloadsDir()`.
  2. В каждом File-экшене замени `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)`
     на `RuntimeServicesHolder.services.getDownloadsDir()` (или параметр, переданный из брика/экшена).
  3. Замени `import android.util.Log` + `Log.e/Log.d` на портативный `Logger` (см. раздел 3).
  4. Остальное (`java.io.File`, canonical-path проверки, `project.getFile`) — оставь как есть (portable).
  5. Компилируй `:catroid`.
- НЕ трогай существующую path-traversal безопасность (canonical path checks) — она критична.
- `WriteVariableToFileAction.kt` (по AGENTS.md) использовал `Environment.getExternalStoragePublicDirectory`
  — проверь grep-ом и зашей так же.

### 5.2 NOTIFICATIONS seam (после файлов)
- Экшены (ПРОВЕРЕНО по `ActionFactory`, строки 4901-5509, и grep android-импортов):
  - `SendNotificationAction.kt` (ПРОВЕРЕНО: импорты `NotificationChannel`, `NotificationManager`,
    `Context`, `Build`, `NotificationCompat`, `NotificationManagerCompat`, `ContextCompat`,
    + `StageActivity.activeStageActivity` — см. ниже).
  - `ShowScheduledNotificationAction.kt` (ПРОВЕРЕНО: импорты `AlarmManager`, `NotificationChannel`,
    `NotificationManager`, `PendingIntent`, `Context`, `Intent`, `Build`, `NotificationCompat`,
    `NotificationManagerCompat`, `RemoteInput`, `ContextCompat`). Использует
    `NotificationEventReceiver` (BroadcastReceiver) для отложенного показа.
  - `PrepareNotificationAction.kt`, `NotificationActionAction.kt` (создание/действие кнопки уведомления).
  - `RemoveNotificationAction.kt` (NotificationManagerCompat).
- `NotificationStorage` (в `content/notification/`): ПРОВЕРЕНО — в пакете `content/notification`
  ТОЛЬКО `NotificationEventReceiver.java` тянет `android.*` (BroadcastReceiver/Context/Intent/RemoteInput).
  → `NotificationStorage` (хранилище данных уведомления) ПОРТАТИВЕН, НЕ трогай его логику,
  просто убедись, что он компилируется в `:core` (никаких android-импортов).
- **Срез (NotificationService seam):**
  1. `core/.../notification/NotificationService.kt` (интерфейс):
     ```kotlin
     interface NotificationService {
         fun prepare(id: Int, channelName: String, title: String, text: String,
                     importance: Int, pinned: Boolean)
         fun show(id: Int)
         fun showScheduled(id: Int, delayMs: Long)
         fun remove(id: Int)
         fun addAction(id: Int, actionId: Int, text: String, iconPath: String?, hint: String?, behavior: Int, hasInput: Boolean)
     }
     ```
     (сигнатуры УТОЧНИ по фактическим полям `NotificationStorage` — прочитай `NotificationStorage`
     перед определением интерфейса).
  2. `core/.../notification/NotificationServiceHolder.kt`: `object NotificationServiceHolder { lateinit var service: NotificationService }`.
  3. `catroid/.../notification/AndroidNotificationService.kt`: делегирует
     `NotificationManager`/`NotificationCompat`/`AlarmManager`/`PendingIntent`/`NotificationEventReceiver`.
     Вместо `StageActivity.activeStageActivity.get()` (Android Activity) — получай `Context` через
     `RuntimeServicesHolder.services` (добавь в `RuntimeServices` метод `getAppContext(): Any?` или
     храни `Context` в `AndroidRuntimeServices`; для desktop — верни null/desktop-контекст).
  4. Перепиши 5 экшенов: вместо прямых `NotificationManager`/Intent/PendingIntent — звонки через
     `NotificationServiceHolder.service.*`. Экшены остаются в `:catroid` и компилируются под Android.
  5. Для desktop (`NotificationServiceHolder.service = DesktopNotificationService()`) — реализация
     через `java.awt.SystemTray`/окно + `java.util.Timer` для `showScheduled`.
  6. Компилируй `:catroid`.

### 5.3 «Хвост» текст-графики: `DrawTextAction.kt`
- ПРОВЕРЕНО: `DrawTextAction.kt` (104 строки) импортирует `android.graphics.Bitmap/Canvas/Color/Paint`
  и `android.opengl.GLES20/GLUtils`, а также `StageActivity.getActiveStageListener()`.
- Это блок «нарисовать текст» (рисует текст на pen/stage-слой). Зашивай аналогично `ShowTextActor`:
  получи RGBA через `TextServiceHolder.textService.rasterizeText(...)`, собери libGDX `Texture`/`Pixmap`,
  рисуй через `SpriteBatch` (уже импортирован). Убери `GLES20`/`GLUtils`/`android.graphics`.
  `StageActivity.getActiveStageListener()` → замени на получение `StageListener` через holder
  (см. раздел 6.4 про доступ к StageListener без Android Activity).

### 5.4 `StageListener.java` android-чистка (часть «графика»)
- ПРОВЕРЕНО импорты в начале `StageListener.java`: `android.content.Context`,
  `android.content.res.Resources`, `android.os.SystemClock`, `android.provider.Settings`,
  `android.util.DisplayMetrics`, `android.util.Log`, `android.view.WindowManager`.
- Замени на libGDX/чистый JDK:
  - `SystemClock.uptimeMillis()` → `com.badlogic.gdx.utils.TimeUtils.millis()`.
  - `DisplayMetrics`/`WindowManager` (размеры экрана) → `Gdx.graphics.getWidth()/getHeight()`.
  - `Context`/`Resources` (строковые ресурсы) → либо убрать, либо передавать строки явно;
    если нужны строки — вынеси в портативный `StringProvider` (интерфейс в `:core`).
  - `Settings` (device id и т.п.) → убрать или через `RuntimeServices`.
  - `Log` → портативный `Logger` (раздел 3).
- Цель: чтобы `StageListener` компилировался в `:core` (без android.*). Это большой, но
  механический рефакторинг — делай ПОСЛЕ 5.1–5.3 и ПЕРЕД миграцией в `:core`.

---

## 6. Дальнейшие крупные шаги (после seam-срезов)

### 6.1 Миграция портативной логики в `:core` (большой шаг — ПОДТВЕРДИ у user)
- Перенеси в `:core` (через `git mv`, см. раздел 7 про вложенные папки): `StageListener`,
  экшены/брики/формулы/контент, `LunoScript`, утилиты, НЕ содержащие android.* (после чистки).
- `:catroid` оставляет у себя: `StageActivity`, UI/fragments, Android-реализации seam, интенты.
- Каждый перенесённый файл: убедись, что нет `android.*` (кроме случаев, покрытых seam).
- Компилируй `:core` и `:catroid` после каждой пачки переноса.

### 6.2 Модуль `:desktop-runtime`
- `settings.gradle`: добавь `include ':desktop-runtime'`.
- `desktop-runtime/build.gradle`: `org.jetbrains.kotlin.jvm`,
  `implementation project(':core')`, плюс (опц.) `implementation "com.badlogic.gdx:gdx-backend-lwjgl3:..."`
  (только если есть в кэше/offline; иначе — без него, headless-режим).
- Внутри: `DesktopRuntimeServices`, `DesktopAudioService`, `DesktopMidiService`,
  `DesktopTextService`, `DesktopNotificationService`, `DesktopStage` (раздел 6.3/6.5).

### 6.3 `DesktopStage` (точка входа плеера)
- `fun main()` → создаёт `StageListener` (portable libGDX), устанавливает все holder-ы:
  `RuntimeServicesHolder.services = DesktopRuntimeServices()`,
  `AudioServiceHolder.audioService = DesktopAudioService()`,
  `MidiServiceHolder.midiService = DesktopMidiService()`,
  `TextServiceHolder.textService = DesktopTextService()`,
  `NotificationServiceHolder.service = DesktopNotificationService()`.
- Загружает/расшифровывает проект (раздел 6.8) и стартует libGDX LWJGL backend (или headless-луп).
- Обрабатывает инпут (касание/клавиатура) → `StageListener` уже умеет через libGDX `InputProcessor`.

### 6.4 Доступ к StageListener без Android Activity
- Сейчас `DrawTextAction`/`SendNotificationAction` лезут в `StageActivity.activeStageActivity`
  (`WeakReference<StageActivity>`, ПРОВЕРЕНО `StageActivity.java` строка 211).
- Для desktop нужен holder: `object StageListenerHolder { var listener: StageListener? = null }`
  (в `:core`), который `StageActivity`/DesktopStage заполняют при старте. Замени обращения
  `StageActivity.getActiveStageListener()` на `StageListenerHolder.listener`.

### 6.5 LunoScript desktop
- `utils/lunoscript/` (Lexer/Parser/Interpreter). Проверь на `android.*` (grep). Платформозависимые
  вызовы (файл/сеть/вибрация) — через те же seam-holder-ы. Если есть Android-`NativeProxy` —
  сделай `DesktopNativeProxy`, реализующий тот же интерфейс. Модули `:lunoscript-processor`/
  `:lunoscript-annotations` уже есть в `settings.gradle`.
- `StageActivity.onCreate` (ПРОВЕРЕНО строки 285-294) уже грузит `LunoScriptEngine` из
  `init.luno.txt`/`init.bin` — переиспользуй эту логику в `DesktopStage`.

### 6.6 Экспорт + шифрование проекта
- Переиспользуй `io/XstreamSerializer.java` (он уже сериализует проекты; ПРОВЕРЕНО —
  алиасы бриков в строках ~386,675,828-851). Убедись, что `XstreamSerializer` портативен
  (grep `android.*`; если есть — зашей).
- Зашифруй сериализованный проект (симметричное шифрование, ключ либо в рантайме, либо
  запрашивается при старте). Расшифровка в `DesktopStage` до загрузки `StageListener`.
- Сделай `copyTemplateWin.bat` (аналог `./gradlew copyTemplateApk` из AGENTS.md):
  собирает `player.jar` (`:core`+`:desktop-runtime`) + `jre/` + `build_exe.bat` + `icon/`
  → `template_win.zip`.

### 6.7 Упаковка (.exe)
- `build_exe.bat`: `magick icon/icon.png icon/icon.ico` (ImageMagick) → launch4j
  (`launch4j.xml` → `MyProject.exe`). Опционально `jar uf player.jar project.luno`.
- launch4j config: `mainClass` = `org.catrobat.catroid.stage.DesktopStage` (или `PlayerMain`),
  `bundledJre` → папка `jre/`.

---

## 7. ОГРАНИЧЕНИЯ и «подводные камни» (ПРОВЕРЕНО)

- **Инструменты манглят путь `ui`** (каталог `ui`). Для поиска:
  `Get-ChildItem -Recurse -Filter "StageActivity.java"` из `C:\Users\ivanp\NewCatroid`.
  Для правки файлов из `ui/` — копируй во временную папку (`$env:TEMP`) и правь там, либо
  используй прямые абсолютные пути в инструментах редактирования.
- **git mv не создаёт вложенные папки назначения** → перед `git mv` делай
  `New-Item -ItemType Directory -Force -Path <destDir>`.
- **Kotlin-интерфейс НЕ синтезирует property** из `getX()/setX()` → в Kotlin-экшенах пиши
  явно `getX()/setX()` (а не `x`).
- **`ProjectManager`** в пакете `org.catrobat.catroid` (НЕ `content`); методы
  `getInstance()`, `getCurrentlyPlayingScene()`, `getCurrentProject()` (используются в
  `AndroidAudioService`/`AndroidMidiService`/`ShowTextActor` — ПРОВЕРЕНО).
- **Sprite-методы интерфейсов** несут `spriteName: String` (portable); резолв в Android-impl
  через `ProjectManager.getInstance().getCurrentlyPlayingScene().spriteList.firstOrNull { it.name == name }`.
- **Drum/MusicalInstrument УЖЕ в `:core`** (FQN не менялся) — не дублируй, не перемещай заново.
- **Не ломай существующую безопасность**: path-traversal canonical-path checks в File-экшенах,
  HTTPS/file/shell валидация WebView в `StageActivity.createWebViewWithUrl()`,
  encrypted Gemini key (`GeminiManager`/`SetGeminiKeyAction`).
- **Тесты**: `catroid/src/test/...` (Robolectric/PowerMock/Mockito). После правки экшенов при
  необходимости обновляй/добавляй тесты рядом с существующими (напр. `PutFileIntoFolderActionTest`).
- **BUILD SUCCESSFUL** — главный критерий готовности среза. Warnings (deprecation/unchecked) — OK.
- **Сеть**: Gradle может не иметь доступа. Используй `--offline`. Новые зависимости (LWJGL) могут
  не скачаться — см. раздел 4 про риск и headless-обход.

---

## 8. ПРИЛОЖЕНИЕ: полный список Android-связанных экшенов (объём «полного порта»)

Ниже — экшены с `android.*` импортами (ПРОВЕРЕНО grep `^import android` в `content/actions`).
Рядом с каждым — что с ним делать. **Следуй порядку user (файлы → уведомления)**, но знай объём.

**Уже зашиты (только `android.util.Log`, легко заменить при миграции в :core):**
`EqualizerSetBandAction`, `PlayToneAction`, `PlaySoundAtAction` (Log + `@VisibleForTesting`),
`ChangeTempoByAction`, `SetSoundVolumeAction`, `AudioFadeOutAction`, `AudioFadeInAction`,
`VibrateAction` (runtime зашит), `OptionsWebRequestAction`, `HeadWebRequestAction`,
`DeleteWebRequestAction`, `PatchWebRequestAction`, `PutWebRequestAction`, `AskGemini2Action`,
`ReadVariableFromDeviceAction`, `ReadListFromDeviceAction`, `StoreCSVIntoUserListAction`.

**ФАЙЛЫ (срез 5.1) — `android.os.Environment` + `Log`:**
`PutFileIntoFolderAction.kt`, `PutFileIntoPathAction.kt`, `WriteVariableToFileAction.kt`,
`DownloadToPathAction.kt`, `DownloadFileAction.kt` (проверь `CreateFolder*`/`DeleteFolder*`/
`CopyProjectFile*` — у них, вероятно, тот же паттерн `Environment` + canonical-path защита).
→ заменить `Environment.getExternalStoragePublicDirectory(...)` на
`RuntimeServicesHolder.services.getDownloadsDir()` (или аналог), `Log` → `Logger`.

**УВЕДОМЛЕНИЯ (срез 5.2) — тяжёлый Android:**
`SendNotificationAction.kt`, `ShowScheduledNotificationAction.kt`, `PrepareNotificationAction.kt`,
`NotificationActionAction.kt`, `RemoveNotificationAction.kt`, `EnableBackgroundAction.kt`.
→ `NotificationService` seam (раздел 5.2). `NotificationStorage` портативен.

**ТЕКСТ/ГРАФИКА (хвост 5.3):** `DrawTextAction.kt` (`android.graphics` + `GLES20` + `StageActivity`).

**WEB / DEVICE / AI / PAINT (вне текущего порядка, но нужны для «полного порта»):**
`FileUrlAction.kt` (Toast/Context/Activity/pm/Build/Environment/ActivityCompat/ContextCompat),
`PostWebRequestAction.kt` (Toast/Context/Activity), `AskGeminiAction.kt` (Toast/Context/Activity),
`AskGemini2Action.kt`, `SetGeminiKeyAction.kt` (Toast/Context/Activity),
`ReadVariableFromFileAction.kt` (Manifest/Activity/Intent/pm/Uri/Build/Environment/Settings/
DocumentsContract/DocumentFile — тяжёлый SAF), `PaintNewLookAction.kt` (Activity/Intent/Bitmap/Bundle).
→ Для этих нужны отдельные seam-интерфейсы (WebService, AiService, CameraService и т.д.) ИЛИ
  они остаются Android-only, а desktop показывает заглушку «не поддерживается». Решай по
  приоритету пользователя; НЕ блокируй ими файлы/уведомления.

**Категории 30 блоков (из AGENTS.md, для контекста):** Motion, Looks, Sound (DONE),
Control, Event, Pen, Data(Variables/Lists), Device(sensors/camera/mic/vibration/notifications),
File (folders/copy/put — В РАБОТЕ), Web (requests), PocketCode/Multiplayer, Stage,
AR/Drone, Text/AI (Gemini), Paint (NeoPaint), Formula. Аудит показал, что ВСЕ 30 категорий
КОМПИЛИРУЮТСЯ; оставшаяся работа — убрать `android.*` из runtime-частей, чтобы собрать desktop-JVM.

---

## 9. Рекомендованная последовательность (пошагово)

1. **FILES seam (5.1)** — узкий механический срез. Компилируй `:catroid`.
2. **NOTIFICATIONS seam (5.2)** — `NotificationService` + 6 экшенов. Компилируй `:catroid`.
3. **DrawTextAction (5.3)** — зашить через `TextServiceHolder`. Компилируй `:catroid`.
4. **StageListener android-чистка (5.4)** — заменить android-вызовы на libGDX/JDK. Компилируй `:catroid`.
5. **ПОДТВЕРДИ у пользователя** архитектурный переход к миграции логики в `:core` (раздел 4/6.1).
6. **Миграция в `:core` (6.1)** — пачками, компилируя `:core` и `:catroid` после каждой.
7. **`:desktop-runtime` + Desktop-реализации seam (6.2/6.3)** — чистый JDK (без LWJGL для text/audio/midi/file/notification).
8. **`DesktopStage` + `StageListenerHolder` (6.3/6.4)** — точка входа, init holder-ов, загрузка проекта.
9. **LunoScript desktop (6.5)** — `DesktopNativeProxy` при необходимости.
10. **Экспорт + шифрование (6.6)** — `XstreamSerializer` + symmetric encrypt + `copyTemplateWin.bat`.
11. **Упаковка (6.7)** — `build_exe.bat` + launch4j + `icon/icon.png`.
12. **Сквозная проверка** — собрать `player.jar`, запустить на Windows, прогнать тестовый проект
    со всеми 30 категориями (или хотя бы звук/текст/файл/уведомления/рисование).

---

## 10. Стиль работы (обязательно)

- **Сначала исследуй** реальный код нужной области (grep/read), потом действуй. Не выдумывай API
  и сигнатуры — читай исходники (`ActionFactory`, `XstreamSerializer`, `NotificationStorage`,
  `StageListener`, реальные экшены) перед тем, как писать интерфейсы/вызовы.
- **Каждый срез маленький и атомарный**; компилируй после каждого (`BUILD SUCCESSFUL`).
- **Не расширяй scope** без запроса. Не делай destructive git-операции (force push, reset --hard,
  rebase -i, rm -rf). `git mv` — ок, с созданием папок назначения (раздел 7).
- **Перед большими архитектурными решениями** (миграция логики в `:core`, новые модули) —
  **спроси пользователя**, не действуй на свой страх.
- **Веди `AGENTS.md` и `SUMMARY.md`** в актуальном состоянии (добавляй новые seam по шаблону
  раздела 3, отмечай DONE/IN PROGRESS).
- **Не ломай безопасность** (path-traversal, WebView scheme validation, encrypted keys).
- **Не трогай чужой код без нужды** — минимальное изменение, решающее задачу.
- Общайся с пользователем на русском, кратко, по делу. После правок — резюмируй, что изменилось,
  где, и как проверено (команда сборки + результат).

---

## 11. Команды верификации (копипаста)

```powershell
cd C:\Users\ivanp\NewCatroid

# Проверка Android-сборки после правки экшенов/seam (офлайн):
./gradlew :catroid:compileCatroidDebugSources --offline -q

# Проверка :core после переноса логики:
./gradlew :core:compileKotlin --offline -q

# Проверка :desktop-runtime (после создания модуля):
./gradlew :desktop-runtime:compileKotlin --offline -q

# Поиск android-зависимостей в файле/папке:
Select-String -Path catroid/src/main/java/org/catrobat/catroid/content/actions/PutFileIntoFolderAction.kt -Pattern "import android"

# Поиск всех android-связанных экшенов:
Get-ChildItem -Recurse -Path catroid/src/main/java/org/catrobat/catroid/content/actions -Filter *.kt |
  ForEach-Object { if (Select-String -Path $_.FullName -Pattern "^import android" -Quiet) { $_.FullName } }
```

---

## 12. Финальный чек-лист готовности desktop-плеера

- [ ] Все 30 категорий исполняются на desktop (или имеют чётную заглушку «не поддерживается»).
- [ ] `:core` компилируется на чистой JVM (нет `android.*`).
- [ ] `:desktop-runtime` предоставляет Desktop-реализации ВСЕХ seam (Audio/Midi/Text/File/Notification/Runtime).
- [ ] `DesktopStage` грузит/расшифровывает проект и стартует рендер-луп (libGDX LWJGL или headless).
- [ ] `player.jar` + `jre/` + `build_exe.bat` + `icon/` собираются в `template_win.zip` через `copyTemplateWin.bat`.
- [ ] `.exe` запускается на Windows и проигрывает тестовый проект.
- [ ] Шифрование проекта работает (нельзя прочитать `.luno` вне рантайма).
- [ ] `AGENTS.md`/`SUMMARY.md` обновлены.

---

Конец промпта. Этот файл можно скормить другому ИИ-агенту целиком для продолжения работы.
