# NeoCatroid — гайд для разработчиков

Быстрый старт: `./gradlew copyTemplateApk` для обновления APK-темплейта (только для регенерации и пабликации в репозиторий Neocatroid-Template; при сборке APK-игр игры V3 шаблон скачивается с GitHub и кэшируется).



# Структура проекта

`res/values` — глобальные значения: цвета, строки.

`res/values/strings.xml` — английский (обязательно обновлять). `values-ru/` — русский.

`res/layout` — все layout'ы (xml блоков, меню, диалогов).

`assets` — ассеты (зелёное = тестовое, не включается в релиз).

`kotlin+java/org.catrobat.catroid/` — основные .java/.kt файлы.

`content/` — контент: блоки, действия, контроллеры (Gemini, Firebase, микрофон).
`content/actions/` — код действий блоков.
`content/bricks/` — классы блоков (соединяют action + layout).
`content/ActionFactory.java` — фабрика: создаёт Action с параметрами.
`content/GlobalManager.kt` — глобальные флаги (stopSounds, saveScenes).

`raptor/` — 3D (ThreeDManager, SceneManager, компоненты).
`fast2d/` — 2D рендер (ECS-based).
`editor/` — 3D редактор.
`utils/lunoscript/` — LunoScript (Interpreter, Parser, Lexer).
`stage/` — StageActivity, рендер-луп, события.
`formulaeditor/` — FormulaElement, Functions, парсер формул.
`ui/` — Activity, Fragment'ы, адаптеры, диалоги.

---

# Гайд: добавление блока

### 1. Action (Kotlin)

```kotlin
class MyAction : TemporalAction() {
    var scope: Scope? = null
    var myParam: Formula? = null

    override fun update(percent: Float) {
        val valStr = myParam?.interpretString(scope) ?: ""
        // логика блока (выполняется 1 раз)
    }
}
```

### 2. Переводы (values/strings.xml + values-ru/strings.xml)

```xml
<string formatted="false" name="my_block_label">Do something</string>
```

Текст в блоке — максимально короткий.

### 3. Layout (brick_my_block.xml)

```xml
<LinearLayout ...>
    <CheckBox android:id="@+id/brick_checkbox" android:visibility="gone" />
    <BrickLayout style="@style/BrickContainer.Look.Small|Medium|Big">
        <include layout="@layout/icon_brick_category_..." />
        <TextView style="@style/BrickText.SingleLine" android:text="@string/my_block_label" />
        <TextView android:id="@+id/brick_my_edit" style="@style/BrickEditText" />
    </BrickLayout>
</LinearLayout>
```

Размер: 1 параметр = Small, 2-3 = Medium, 4+ = Big. Каждый параметр на новой строке.

### 4. ActionFactory (Java)

```java
public Action createMyAction(Sprite sprite, SequenceAction seq, Formula param) {
    MyAction action = action(MyAction.class);
    Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, seq);
    action.setScope(scope);
    action.setMyParam(param);
    return action;
}
```

### 5. Brick (Java)

```java
public class MyBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public MyBrick() {
        addAllowedBrickField(BrickField.TEXT, R.id.brick_my_edit);
    }
    public MyBrick(String value) { this(new Formula(value)); }
    public MyBrick(Formula f) { this(); setFormulaWithBrickField(BrickField.TEXT, f); }

    @Override public int getViewResource() { return R.layout.brick_my_block; }
    @Override public void addActionToSequence(Sprite s, ScriptSequenceAction seq) {
        seq.addAction(s.getActionFactory().createMyAction(s, seq,
            getFormulaWithBrickField(BrickField.TEXT)));
    }
}
```

Обязательно: конструктор из простых значений (String/double), не только из Formula.

---

# Гайд: добавление формулы

1. Переводы: `formula_my_func` + `formula_my_func_param`
2. `Functions.java` — добавить `MY_FUNC` в enum + в `TEXT` сет
3. `InternFormulaAdapter` — case в switch
4. `InternToExternGenerator` — запись в `INTERN_EXTERN_LANGUAGE_CONVERTER_MAP`
5. `CategoryListFragment` — в соответствующий список FUNCTIONS/PARAMS
6. `FormulaElement.java` — основной case в switch

---


---

# Добавленные блоки (2026-07)

## 1. File category — блоки для папок
- **CreateFolderBrick** — создать папку (уже существовал, зарегистрирован в File)
- **DeleteFolderBrick** — удалить папку (уже существовал, зарегистрирован)
- **CreateFolderByPathBrick** — создать по пути (уже существовал, зарегистрирован)
- **DeleteFolderByPathBrick** — удалить по пути (уже существовал, зарегистрирован)
- **CopyProjectFileToFolderBrick** — копировать в папку (уже существовал, зарегистрирован)
- **CopyProjectFileToPathBrick** — копировать по пути (уже существовал, зарегистрирован)
- **PutFileIntoFolderBrick** — положить файл в папку (NEW)
- **PutFileIntoPathBrick** — положить файл по пути (NEW)

## 2. Device category — уведомления
- **SendNotificationBrick** — отправляет уведомление по ID (использует NOTIFICATION_ID)
- **ShowScheduledNotificationBrick** — отправляет отложенное уведомление с заголовком, текстом и временем
- **NotificationActionBrick** — действие при нажатии на уведомление
- **PrepareNotificationBrick** — подготовка уведомления с заголовком, текстом, важностью и pin

## 3. Motion category — направление на касание
- **TouchDirectionBrick** — автоматически вычисляет угол от спрайта к точке касанию

## 3a. Physics category — регдолл
- **SetRagdollBrick** — включает/выключает режим регдолла (1 = вкл, 0 = выкл, **2 = регдолл со следованием**)
- **Формула `Sprite_ragdolled`** — возвращает 1 если спрайт в регдолле (любой режим), иначе 0

### Рантайм-эффект регдолла (Android)
- **Sprite.ragdollMode** (transient int) — состояние: 0 = выкл, 1 = регдолл, 2 = регдолл-следование
- **SetRagdollAction** — ставит режим из формулы (>= 2 → 2; != 0 → 1; иначе 0)
- **PhysicsLook** — при `isRagdolled()` (mode > 0):
  - `setX/setY/setPosition/setXInUserInterfaceDimensionUnit` — **НЕ пишут** в physicsObject (движение игнорируется)
  - `setRotation` — **НЕ меняет** направление physicsObject
  - `setScale` — **НЕ перестраивает** физическую форму
  - `getX/getY/getRotation` — читают с physicsObject (спрайт визуально следует за телом)
- **Режим 2 (регдолл-следование / «кукла на верёвке»)**:
  - Сеттеры (см. выше) при mode == 2 запоминают **цель** (центр спрайта в мировых координатах) вместо игнорирования
  - `PhysicsLook.draw()` вызывает `updateRagdollFollow()`: P-контроллер
    `v += (dx*stiffness - v) * blend` (stiffness=6, blend=0.2, в px/с) — тело плавно
    догоняет цель с инерцией: свисает, качается, сталкивается, но едет за скриптом
    (goto touch, glide и т.д.)
  - Затем draw вызывает getX/getY/getRotation — актор рисуется на позиции тела
  - Если цель не задана скриптом — берётся текущая позиция тела (followTargetSet)
- **Что продолжает работать** в регдолле:
  - Гравитация и коллизии (тело DYNAMIC)
  - `SetVelocityAction`, `ApplyForceAction` и другие physics-actions (идут напрямую в physicsObject/mинуя PhysicsLook)
- **Формула `SPRITE_RAGDOLLED`** — читает `s.ragdollMode > 0` из FormulaElement

### Файлы
```
content/actions/SetRagdollAction.kt     — ставит sprite.ragdollMode (0/1/2)
content/bricks/SetRagdollBrick.java     — brick (FormulaBrick, PHYSICS_TOGGLE field)
content/Sprite.java                     — поле ragdollMode (int)
physics/PhysicsLook.java                — isRagdolled()/isRagdollFollow() guard в сеттерах + updateRagdollFollow()
formulaeditor/Functions.java            — SPRITE_RAGDOLLED enum
formulaeditor/FormulaElement.java       — case SPRITE_RAGDOLLED
test/.../SetRagdollBrickTest.java       — 13 тестов (brick wiring, action 0/1/2, clamp, formula)
```

## 4. Control category — клоны по номеру
- **DeleteCloneByNumberBrick** — удаляет клон по номеру (cloneIndex)
- **ExecuteForCloneNumberBrick** — контейнерный блок (CompositeBrick + EndBrick), выполняет внутренние блоки только если cloneIndex совпадает

## 5. Look category — привязка к камере со смещением
- **AttachToCameraWithOffsetBrick** — привязывает 3D объект к камере с X/Y/Z смещением

## 6. Sprite.java
- Добавлено поле `cloneIndex` (int, transient) — 0 для оригинала, 1+ для клонов

## 7. StageListener.java
- Поле `cloneCounter` — счётчик номеров клонов
- Метод `removeCloneByIndex(int)` — удаление клона по номеру
- В `cloneSpriteAndAddToStage()` — `clone.cloneIndex = cloneCounter++`

## 8. SceneManager.java
- Метод `attachObjectToCamera(String objectId, float offsetX, float offsetY, float offsetZ)` — новая перегрузка с 4 параметрами

## 9. XStream
- `XStreamBrickConverter` автоматически обнаруживает все Brick-классы в пакете `org.catrobat.catroid.content.bricks` по имени класса, поэтому явная регистрация не требуется.

## 10. Formula fixes
- FILE_PROJECT_SIZE, FILE_SIZE_IN_DIR, FILE_SIZE_AT_PATH добавлены в TEXT EnumSet в Functions.java
- Добавлены в DEVICE_FUNCTIONS/DEVICE_PARAMS в CategoryListFragment.java
- Добавлена строка `formula_file_project_size_param`

## 11. NeoScript — reusable script modules (.neoscript)

### Summary
Система экспорта/импорта переиспользуемых модулей скриптов в формате `.neoscript`. Позволяет сохранить выделенные скрипты в файл и импортировать их в любой объект того же или другого проекта.

### Files
```
neoscript/
  NeoScriptFile.java        — корневая модель (список Script + UserVariable + UserList)
  NeoScriptSerializer.java  — XStream-сериализация с валидацией версии
  NeoScriptExporter.java    — сборка NeoScriptFile из выбранных скриптов + референсов
  NeoScriptImporter.java    — вливание скриптов в target Sprite с dedup
  NeoScriptUserData.java    — сбор/перелинковка UserVariable/UserList (reflection)
  NeoScriptException.java   — кастомное исключение

content/actions/
  ImportScriptAction.kt     — TemporalAction: runtime-импорт .neoscript в объект

content/bricks/
  ImportScriptBrick.java    — Brick (File category): objectName + filePath + overwrite Spinner

res/layout/
  brick_import_script.xml   — BrickLayout с двумя FormulaEditText + Spinner

ui/recyclerview/fragment/
  ScriptFragment.java       — SAVE_AS_SCRIPT action mode + exportScripts() + launchNeoScriptFilePicker()

ui/
  SpriteActivity.java       — importNeoScriptModule() + REQUEST_NEO_SCRIPT_FILE/IMPORT handlers

test/neoscript/
  NeoScriptModuleTest.java  — 13 тестов: round-trip, import, dedup, overwrite, version validation, large load, undo model
```

### Design decisions
- **Container root**: `NeoScriptFile` (не Project) — содержит только выбранные скрипты + необходимые переменные/списки. Без сцен, ассетов, настроек.
- **Serialization**: переиспользует XStream-конфигурацию проекта (`XstreamSerializer.getInstance().getXstream()`), поэтому все Brick/Formula-конвертеры работают автоматически. Добавлен алиас `<neoscript>` для корня.
- **Versioning**: `formatVersion` (int), MIN=1, MAX=1. Старые/будущие версии отклоняются с понятным сообщением.
- **Unknown blocks**: `XStreamBrickConverter` автоматически создаёт `UnknownBrick` для неизвестных типов блоков — совместимость с будущими версиями.
- **ID remapping**: при импорте скрипты клонируются через `Script.clone()`, который генерирует свежие scriptId и brickId (через XStream ID-генератор).
- **Variable relinking**: `NeoScriptUserData` через reflection обходит все `UserVariable`/`UserList` поля в бриках, находит или создаёт переменные с тем же именем в целевом проекте/спрайте.
- **Duplicate detection**: стабильная сигнатура = `ClassName(simple)#trigger(TEXT)` (для BroadcastScript — broadcastMessage). Overwrite = replace, иначе skip.
- **Undo/redo**: редакторский импорт вызывает `copyProjectForUndoOption()` перед изменениями.
- **Security**: XStream security deny-by-wildcard для системных пакетов. File path validation через кастомные Formula (не raw strings).
- **Runtime brick** (`ImportScriptBrick`): Formula-поля для objectName и filePath, Spinner для overwrite. Файл открывается через `ACTION_OPEN_DOCUMENT` с `REQUEST_NEO_SCRIPT_FILE`.
- **Save path**: `Download/NeocatroidScript/{name}.neoscript` через `Constants.DOWNLOAD_DIRECTORY`.
- **Format**: XML с `<neoscript>` корнем, без сжатия/архивации — plain text для ручного редактирования.

### Adding a new .neoscript brick type
Любой новый Brick в пакете `org.catrobat.catroid.content.bricks` обнаруживается автоматически — не требуется регистрация в XStream. Для корректной сериализации достаточно конструктора без параметров и соответствия имени класса.

### NeoScript brick reference (current)


 `ImportScriptBrick` (File→NeoScript cat.) `ImportScriptAction` | objectName, filePath, overwrite | Import .neoscript into existing object |
 `CreateObjectBrick` (NeoScript cat., NEW)  `CreateObjectAction` | objectName (Formula), scene (spinner), persist (Yes/No) | Create blank sprite in scene; if persist=Yes, save canonical project to disk
 `AssignScriptsBrick` (NeoScript cat., NEW)  `AssignScriptsAction` | filePath, objectName, scene, replace (Yes/No), save (Yes/No) | Assign .neoscript to object in scene; if save=Yes, save canonical project to disk 

### Scene-aware bricks design
- Scene stored as `String` (name): `null`/empty = Current scene, otherwise `project.getSceneByName(name)`.
- Spinner: StringOption("Current scene") + Scene items.
- Backward compat: missing/empty scene field → Current scene.
- Object lookup scoped to the resolved scene (not global).
- Inactive scene: scripts added to model only (no runtime registration).
- Active scene: `executeConsoleScript()` starts added scripts.
- UnknownBrick detection: `AssignScriptsAction` checks for `UnknownBrick` instances pre-import, replaces with `NoteBrick`.
- `AssignScriptsBrick` "Replace existing scripts?" spinner [0/1]: 0 = keep existing + add imported (`ImportStrategy.APPEND_ALL`), 1 = remove ALL existing + add imported (`ImportStrategy.REPLACE_ALL`). This is SEPARATE from the `ImportScriptBrick` duplicate-overwrite (boolean → `SKIP_DUPLICATES`/`REPLACE_DUPLICATES`). Do not conflate the two.
- `NeoScriptImporter.ImportStrategy` enum: `SKIP_DUPLICATES`, `REPLACE_DUPLICATES`, `APPEND_ALL`, `REPLACE_ALL`. `REPLACE_ALL` is atomic — all scripts are cloned+relinked first; only on full success is the target sprite's script list cleared and the new scripts added. Default serialized value MUST be 0 (least destructive).

### NeoScript persistence (2026-07)

`CreateObjectBrick` and `AssignScriptsBrick` have an OPTIONAL persistence flag so a runtime change can also be written to the canonical project on disk.

- **Flags**: `CreateObjectBrick.persistentSelection` (0 = runtime only, 1 = persist) and `AssignScriptsBrick.savePersistentSelection` (same). Both plain `int`, serialized by XStream. Missing field on load → `0` (runtime only, least destructive). Getters: `isPersistent()` / `isSavePersistent()`.
- **Default**: No (runtime only). Old serialized bricks without the field keep working.
- **Mechanism**: the action mutates the live canonical `Project` (which `scope.project` already is — no clone), THEN calls `ProjectSaver(project, CatroidApplication.getAppContext()).saveProjectAsync {}`. On device this serialises the project (XstreamSerializer atomic temp+rename). The save is best-effort / fire-and-forget and is wrapped in try/catch: if the app context is unavailable the save is skipped (the in-memory model is still mutated).
- **Scene isolation**: the resolved scene (see above) is authoritative; object lookup is scoped to it. A persisted object/script lands in that scene's model.
- **Behaviour**: `persist`/`save` do NOT change runtime behaviour — the object/script is added to the canonical project model regardless; the flag only decides whether the canonical project is also written to disk.
- **Tests**: `NeoScriptPersistenceTest` (catroid/src/test/.../neoscript) covers in-memory canonical mutation, scene isolation, replace semantics, unknown→Note, large import, and XStream round-trip of the flag (forward + backward-compat). Full project save/load is environment-gated (device / Robolectric) and reuses Catroid's standard `ProjectSaver`.

### XStream
- `XStreamBrickConverter` автоматически обнаруживает все Brick-классы по имени класса.
- Пакеты поиска: `org.catrobat.catroid.content.bricks`, `org.catrobat.catroid.physics.content.bricks`.
- Неизвестные типы → `UnknownBrick` (не ломает загрузку).
- Явная регистрация не требуется, но для обратной совместимости в `XstreamSerializer.java` есть `xstream.alias("brick", ConcreteBrick.class)`.

---

# AI category — единый блок «Спросить ИИ» + событие ответа (2026-08)

Замена старых блоков AskGPT/AskGemini/AskGemini2/SetGeminiKey на один универсальный
блок с выбором провайдера (OpenAI, Gemini, DeepSeek, OpenRouter, Anthropic, OpenCode):

- **AskAIBrick** («Спросить ИИ», Neural → LLM) — поля: TEXT (промпт), BODY (системный
  промпт), MODEL (модель, пусто = дефолт провайдера), спиннер провайдера, спиннер
  переменной для ответа. Пишет ответ в UserVariable и fire `AiResponseEventId`.
- **WhenAIResponseBrick** («Когда ИИ ответил», Events) — ScriptBrick, спиннер провайдера
  («Любой провайдер» = пустая строка). Триггерится после завершения AskAIBrick.
- Старые классы (`AskGPTBrick`, `AskGeminiBrick`, `AskGemini2Brick`, `SetGeminiKeyBrick`)
  **оставлены** для десериализации старых проектов (XStream по имени класса), убраны
  только из палитры `setupNeuralCategoryList`.

## Файлы

```
content/
  WhenAIResponseScript.java           — Script (provider: String, "" = любой)
  eventids/AiResponseEventId.java     — equals: sprite + provider; пустой provider = wildcard
content/actions/
  AskAIAction.kt                      — TemporalAction: Thread + runBlocking CloudModelRuntime,
                                        пишет ответ в переменную, fire AiResponseEventId(sprite, providerId)
content/bricks/
  AskAIBrick.kt                       — UserVariableBrickWithFormula + BrickSpinner<StringOption>
  WhenAIResponseBrick.java            — ScriptBrickBaseType + BrickSpinner<StringOption>
res/layout/
  brick_ask_ai.xml                    — Neural.Big: prompt/system/model edit + 2 спиннера
  brick_when_ai_response.xml          — Motion.MediumWhen: label + spinner
ai/model/CloudModelRuntime.kt         — добавлен `generateForProvider(provider, model, system, user)`
                                        (ключ через AiPreferences.getApiKeyForProvider(provider.id))
```

## Регистрация

- `XstreamSerializer`: алиасы `WhenAIResponseScript`/`WhenAIResponseBrick`/`AskAIBrick`.
- `CategoryBricksFactory.kt`: `AskAIBrick("Hello!")` в Neural (обе ветки grouped/ungrouped);
  `WhenAIResponseBrick()` в Events (в обеих ветках, доступен и фону).
- `BrickInfo.java`: справка ru/en. `RecentBrickListManager`: WhenAIResponseBrick в
  nonBackgroundSpriteClasses.
- `strings.xml`/`values-ru`: `brick_ask_ai`, `brick_ask_ai_system`, `brick_ask_ai_model`,
  `brick_ask_ai_provider`, `brick_when_ai_response`, `ai_provider_any`.
- `AiProvider` (ai/model/AiProvider.kt) — единственный источник списка провайдеров
  (id, displayName, baseUrl, defaultModels); спиннеры строятся из `AiProvider.values()`.

## Тесты

- `AskAIBrickTest.java` (4): wiring через ActionFactory.createAskAIAction, дефолтный
  провайдер, конструкторы, clone.
- `WhenAIResponseBrickTest.java` (6): script↔brick, конструкторы, clone, createEventId,
  wildcard-equals (пустой провайдер = любой) + hashCode-консистентность.
- Проверка: `./gradlew :catroid:testCatroidDebugUnitTest --tests "*AskAIBrickTest*" --tests "*WhenAIResponseBrickTest*"`.

---

# Event category — касание спрайтов (WhenTouchingSprite, 2026-08)

Два блока событий, срабатывающих при перекрытии хитбоксов (AABB) БЕЗ физики:

- **WhenTouchingSpriteBrick** («Когда касается другого актёра») — триггерится на касание ЛЮБОГО спрайта
- **WhenTouchingSpriteByNameBrick** («Когда касается …») — spinner с выбором конкретного спрайта (или «любого актёра»)
- У обоих есть CheckBox «реагировать на фон» (`reactToBackground`) — по умолчанию фон игнорируется

## Файлы

```
content/
  WhenTouchingSpriteScript.java           — Script (reactToBackground; eventId = TouchingSpriteEventId(sprite, ""))
  WhenTouchingSpriteByNameScript.java     — Script (spriteToTouchName + reactToBackground)
  TouchingSpriteTrigger.java              — edge-trigger: TRIGGER_NOW → fire → ALREADY_TRIGGERED → reset при расхождении
  eventids/TouchingSpriteEventId.java     — equality: sprite + touchedSpriteName (String, "" = любой)
content/bricks/
  WhenTouchingSpriteBrick.java            — checkbox background (R.id.brick_when_touching_sprite_background_checkbox)
  WhenTouchingSpriteByNameBrick.java      — BrickSpinner<Sprite> + checkbox
res/layout/
  brick_when_touching_sprite.xml
  brick_when_touching_sprite_by_name.xml
```

## Рантайм (Android)

- `Sprite.touchingSpriteTriggers` (transient Set<TouchingSpriteTrigger>), инициализация в `initTouchingSpriteTriggers()` (вызывается из `StageListener` при старте сцены и при клонировании, по аналогии с condition/firebase-триггерами).
- Проверка каждый кадр: `Look.update()` → `sprite.evaluateTouchingSpriteTriggers()`.
- `TouchingSpriteTrigger.isTouching()`: оба спрайта видны, не фон (если не reactToBackground), имя совпадает (или любой), AABB-перекрытие через `Look.getX/Y/Width/HeightInUserInterfaceDimensionUnit()`.
- Fire: `sprite.look.fire(new EventWrapper(new TouchingSpriteEventId(sprite, name), false))` — edge-triggered: событие шлётся один раз при ВХОДЕ в касание, статус сбрасывается когда касание пропало.
- Клоны: `matchesTargetName` матчит и по имени оригинала (`other.myOriginal`).

## Desktop (DesktopScriptEngine.kt)

- `mapScriptTypeToEvent`: `WhenTouchingSpriteScript`/`WhenTouchingSpriteByNameScript` → `"touching_sprite"`.
- Парсинг: `eventParam2 = <spriteToTouchName>` (для универсального скрипта элемента нет → пустая строка = любой).
- `checkEvents`: `checkSpriteCollision(sprite, eventParam?.takeIf { isNotEmpty })` — пустое имя → любой спрайт.

## Регистрация

- `XstreamSerializer`: алиасы script/brick для обоих типов.
- `CategoryBricksFactory.kt`: оба блока в Events (только для не-фоновых спрайтов, обе ветки grouped/ungrouped).
- `BrickInfo.java`: справка ru/en.
- `RecentBrickListManager`: оба в nonBackgroundSpriteClasses.
- `strings.xml`/`values-ru`: `brick_when_touching_sprite`, `brick_when_touching_sprite_by_name`, `touching_sprite_anything`, `brick_when_touching_sprite_background`.

## Тесты

- `WhenTouchingSpriteBrickTest.java` (4) + `WhenTouchingSpriteByNameBrickTest.java` (3) — brick↔script linkage, clone, конструкторы.
- Проверка: `./gradlew :catroid:testCatroidDebugUnitTest --tests "*WhenTouchingSprite*"`.

---

# Backpack — портфель скриптов/объектов

## Обзор

Backpack («портфель»/«рюкзак») — система копирования скриптов, объектов, сцен, звуков и образов между проектами без экспорта в файл. Данные хранятся в директории приложения как JSON + файлы.

## Файлы

```
common/Backpack.java                                  — модель данных (списки для каждого типа)
io/BackpackSerializer.java                            — JSON-сериализация/десериализация
io/BackpackScriptSerializerAndDeserializer.java       — Gson-адаптер для Script
io/BackpackFormulaFieldSerializerAndDeserializer.java — Gson-адаптер для формул
io/BackpackInterfaceSerializerAndDeserializer.java    — базовый адаптер
ui/controller/BackpackListManager.java                — singleton, доступ к рюкзау + сохранение/загрузка
ui/recyclerview/backpack/
  BackpackActivity.java                               — Activity с ViewPager (вкладки Scripts/Sounds/Looks/Sprites/Scenes)
  BackpackScriptFragment.java                         — список скрипт-групп, unpack/delete
  BackpackSoundFragment.java                          — список звуков в рюкзаке
  BackpackLookFragment.java                           — список образов
  BackpackSpriteFragment.java                         — список спрайтов
  BackpackSceneFragment.java                          — список сцен
ui/recyclerview/fragment/
  ScriptFragment.java                                 — action mode "Pack", диалоги упаковки/распаковки
  SoundFragment.java                                  — action mode "Pack" для звуков
ui/recyclerview/controller/
  ScriptController.java                               — pack()/unpack() — упаковка/распаковка скриптов
  SoundController.java                                — pack()/unpack()/copy() — упаковка/распаковка звуков
  LookController.java                                 — pack()/unpack()/copy() — упаковка/распаковка образов
layout/
  dialog_pack_options.xml                             — диалог с чекбоксами (звуки + значения)
```

## Модель данных (Backpack.java)


 `backpackedScripts` | `HashMap<String, List<Script>>` | Скрипт-группы по имени |
 `backpackedUserDefinedBricks` | `HashMap<String, List<UserDefinedBrick>>` | UserDefined брики по группе |
 `backpackedUserVariables` | `HashMap<String, HashMap<String, Int>>` | Имена переменных + тип (GLOBAL/LOCAL/MULTIPLAYER) по группе |
 `backpackedUserLists` | `HashMap<String, HashMap<String, Int>>` | Имена списков + тип по группе |
`backpackedSounds` | `List<SoundInfo>` | Звуки в рюкзаке (отдельно от скриптов) |
 `backpackedLooks` | `List<LookData>` | Образы в рюкзаке |
`backpackedScriptSounds` | `HashMap<String, List<SoundInfo>>` | Звуки ВНУТРИ скрипт-групп (NEW) |
 `backpackedVariableValues` | `HashMap<String, HashMap<String, String>>` | Значения переменных по группе (NEW) |
 `backpackedListValues` | `HashMap<String, HashMap<String, String>>` | Значения списков (CSV) по группе (NEW) |

## Упаковка скриптов (2026-08)

### Диалог выбора

При упаковке скрипта через `ScriptFragment.showNewScriptGroupAlert()`:
1. Пользователь вводит имя группы
2. Если скрипт содержит звуки или переменные — показывается `dialog_pack_options.xml`:
   - ☑ «Скрипт содержит звуки. Сохранить их вместе со скриптом?»
   - ☑ «Сохранить текущие значения переменных и списков?»
3. Чекбоксы скрываются если соответствующих данных нет в скрипте

### ScriptController.pack()

```
pack(groupName, bricksToPack, includeSounds, includeValues)
```

- Клонирует скрипты и UserDefined брики
- `includeSounds=true` → собирает `SoundInfo` из `PlaySoundBrick`/`PlaySoundAndWaitBrick` (dedup по имя)
- `includeValues=true` → собирает значения `UserVariable`/`UserList` из бриков и формул (рекурсивный обход `FormulaElement`)
- Сохраняет всё в `BackpackListManager`

### Хранение звуков в скрипт-группе

Звуки хранятся отдельно от глобального списка `backpackedSounds`:
- `backpackedScriptSounds[groupName]` → список `SoundInfo` (файлы в `backpackSoundDirectory`)
- При удалении группы звуки тоже удаляются (`removeItemFromScriptBackPack`)

## Распаковка скриптов (2026-08)

### ScriptController.unpack()

```
unpack(scriptName, scriptToUnpack, destinationSprite)
```

- Клонирует скрипт
- **Звуки:** для каждого `PlaySoundBrick`:
  - Если звук с таким именем уже есть в спрайте → использовать существующий
  - Если есть в рюкзаке (`backpackedScriptSounds[scriptName]`) → копировать через `SoundController.copy()`
  - Иначе оставить как есть (звук не привязан)
- **Значения:** для `UserVariableBrickInterface`/`UserListBrick`:
  - Если есть сохранённое значение в `backpackedVariableValues`/`backpackedListValues` → восстановить
  - Числа парсятся как `Double`, остальное как `String`
  - Списки хранятся как CSV (comma-separated)

### Dedup по имя

При распаковке звуков поиск сначала по имя файла (не по UUID), что позволяет корректно резолвить звуки после упаковки.

---

##  Чистка мусора
- `catroid/src/main/libs/test/` (382 файла) — удалён
- `catroid/src/main/libs/__prebuilt_aar_backup/` — удалён
- `assets/ababuy.txt` — удалён

\
## Сборка/зависимости (обновлено 2026-07)
- Coroutines unified to 1.7.3 → 1.9.0
- material:1.2.1 → 1.13.0 → 1.14.0, removed resolutionStrategy force
- Gradle: `-Xmx6g` → `-Xmx4g`
- Удалён дублирующийся `apksig:7.0.0`
- Дубликаты `configurations { pluginLibs }` → объединены
- Дубликаты `packagingOptions` → объединены (pickFirsts, excludes)
- `ext.useAndroidLocales` → исправлен синтаксис (closure вместо сломанного)
- `testCoverageEnabled` → `enableUnitTestCoverage` (deprecation)
- Дублирующийся `kotlin-stdlib` и мёртвый код удалены


##  Тесты (21 файл для 11 новых блоков)
### Brick tests (верификация addActionToSequence):
1. `PutFileIntoFolderBrickTest.java`
2. `PutFileIntoPathBrickTest.java`
3. `SendNotificationBrickTest.java`
4. `ShowScheduledNotificationBrickTest.java`
5. `NotificationActionBrickTest.java`
6. `PrepareNotificationBrickTest.java`
6a. `SetRagdollBrickTest.java` — 12 тестов (brick wiring, action, formula)
7. `TouchDirectionBrickTest.java`
8. `DeleteCloneByNumberBrickTest.java`
9. `ExecuteForCloneNumberBrickTest.java` (+ composite brick тесты)
10. `AttachToCameraWithOffsetBrickTest.java`

### Action tests (unit + PowerMock):
1. `PrepareNotificationActionTest.kt` — проверка NotificationStorage
2. `NotificationActionActionTest.kt` — проверка addAction/execution guard
3. `ExecuteForCloneNumberActionTest.java` — cloneIndex matching/restart
4. `TouchDirectionActionTest.java` — PowerMock(TouchUtil), 8 тестов
5. `DeleteCloneByNumberActionTest.kt` — PowerMock(StageActivity)
6. `AttachToCameraWithOffsetActionTest.java` — PowerMock(StageActivity, SceneManager)
7. `PutFileIntoFolderActionTest.java` — PowerMock(Environment, TemporaryFolder)
8. `PutFileIntoPathActionTest.java` — PowerMock(Environment, TemporaryFolder + path traversal)
9. `SendNotificationActionTest.kt` — PowerMock(StageActivity, NotificationStorage)
10. `ShowScheduledNotificationActionTest.kt` — PowerMock(StageActivity, NotificationStorage)

##  Исправление pre-existing ошибок компиляции
### Java (main):
- `ShowColorPickerFormulaEditorStrategy.java` — добавлен импорт `FragmentManager`, `value -> { ... return null; }` для Kotlin `Unit`
- `FormulaEditorFragment.java` — то же исправление lambda return
- `UiUtils.java` — добавлен `R.string.menu_rate_us` (отсутствовал)
### Kotlin (tests):
- `ObjectDetectorOnSuccessListener` — создан недостающий класс в `camera/mlkitdetectors/`
- `DetectedObject` stub — добавлены конструкторы `(Rect, Int, List<Label>)` + `Label(String, Float, Int)`
- Мои 20 тестов — исправлены: `ScriptSequenceAction(null)` → mock, missing imports, `SequenceAction` → `ScriptSequenceAction`

## Stage/Actors
- **StageActivity.onDestroy()**: `super.onDestroy()` moved to end, `messageHandler` nulled
- **StageActivity.setupAskHandler()**: changed to `WeakReference<StageActivity>`
- **ShowTextActor.drawText()**: added texture caching (skip per-frame Bitmap/Texture allocation when text unchanged)
- **StageListener**: `cloneCounter` changed to `AtomicInteger`, removed unused `accumulator`/`TIME_STEP`

## Null safety
- Added `if (scope == null) return;` to 19 Java action files (TouchDirectionAction, etc.)
- LookPostRequestAction/LookRequestAction: replaced `!!` with local `val ec = errorCode`
- PrepareNotificationBrick: removed `transient` from `importanceLevel` and `isPinned`

## XStream serialization
- Added 5 brick aliases to XstreamSerializer.java (PutFileIntoFolder, PutFileIntoPath, ExecuteForCloneNumber, DeleteCloneByNumber, TouchDirection)
- XStreamBrickConverter: fixed `result = new UnknownBrick(type)` (was creating unused local)
- XStreamFormulaElementConverter: fixed SECOND_FACE_Y_POSITION → FACE_Y sensor mapping

## Прочее
- ActionFactory: `RunShellAction()` → `runShellAction()` (Java naming convention)
- PanoramicConverter: uncommented `fbo.dispose()`
- ErrorInterceptor.kt: `response.body?.toString()` → `response.body?.string()`
- Removed duplicate commented `package` lines from 14 action files
- Removed duplicate `createDeleteCloneByNumberAction` from ActionFactory
- Добавлены missing resources: `cancel_button_text`, `import_step_prepare`, `menu_rate_us`, `ic_pocketpaint_tool_resize_adjust`

##  Обновление зависимостей (2026-07)
| Зависимость | Было | Стало |
|---|---|---|
| AGP | 8.3.0 | 8.7.3 |
| Kotlin | 1.9.22 | 2.0.21 |
| KSP | 1.9.22-1.0.16 | 2.0.21-1.0.28 |
| compileSdk / targetSdk | 34 | 35 |
| Lifecycle | 2.2.0 | 2.8.7 |
| Room | 2.3.0 | 2.6.1 |
| Core KTX | 1.3.2 | 1.15.0 |
| Coroutines | 1.7.3 | 1.9.0 |
| WorkManager | 2.7.1 | 2.10.0 |
| Robolectric | 4.7.3 | 4.14.1 |
| Espresso | 3.1.0 | 3.6.1 |
| AndroidX Test JUnit | 1.1.5 | 1.2.1 |
| Material | 1.13.0 | 1.14.0 |
| Glide | 4.11.0 | 4.16.0 |
| Gson | 2.8.7 | 2.11.0 |
| OkHttp | 4.9.3 | 4.12.0 |
| Guava | 28.2-android | 33.4.0-android |
| Browser | 1.2.0 | 1.8.0 |
| **Не обновлено** (high risk): Koin 2.1.6, CameraX 1.0.0-beta07, Mockito 3.12.4 (заблокирован PowerMock)

---




## APK Builder V3 — полная замена имени пакета (2026-07)
V3 собирает автономный APK из 	emplate_runtime.apk с переименованием пакета на выбранный пользователем.

- **Реализация**: catroid/.../apkbuildV3/V3ApkAssembler.kt
  - pplyPackageRename(manifest, newPackage): manifest.packageName = newPackage + manifest.ensureFullClassNames() (квалифицирует относительные имена компонентов против СТАРОГО пакета ДО смены) + 
eplacePackageInAuthority (authority provider через searchAttributeByResourceId(0x01010018)).
  - makeRuntimeLoaderLauncher(manifest) (internal) — делает RuntimeLoaderActivityV3 единственным launcher.
  - doSign(input, output, keystore, alias, password) (internal) — подпись apksig v1+v2+v3.
- **Runtime пакет-независим**: FileProvider authority, content URI, PendingIntent, getPackageInfo/getPackageName, reflection (BRICKS_PACKAGE_NAMES — FQN) — всё строится динамически из getPackageName(); хардкод-строк org.catrobat.catroid в манифест-зависимом коде НЕТ. ProjectFilesFragment/ProjectLibsFragment: BuildConfig.APPLICATION_ID → 
equireContext().packageName.
- **Верификация**: catroid/src/test/java/org/catrobat/catroid/apkbuildV3/V3PackageRenameTest.kt (5 тестов, все зелёные), в т.ч. exportTwoGames_coexistAndVerify — реальный репак 	emplate_runtime.apk (188 МБ) ×2 → org.test.game1/org.test.game2, reandroid-репарс + apksig verify (package, <pkg>.fileProvider authority, RuntimeLoaderActivityV3 launcher, payload project.ncv3, отсутствие ${...} плейсхолдеров и старого пакета вне 
ame). test heap -Xmx4g в catroid/build.gradle.
- **Ограничение среды**: нет устройства/SDK ⇒ реальный db install не проверялся; сосуществование доказано логически (разные applicationId + authorities) и тестом.
- **Локатор шаблона**: catroid/.../apkbuildV3/TemplateManagerV3.kt — prepareBaseApk теперь: 1) кэш `filesDir/v3_template/template_runtime_v3.apk` используется как есть (без HEAD-проверки — свежесть отвечает визард, сборка кэш не перекачивает); 2) скачивание с GitHub при пустом кэше (`raw.githubusercontent.com/Ivproduction-dev/Neocatroid-Template` → LFS-детект по префиксу `version https://git-lfs.github.com/spec/v1` → media.githubusercontent.com), OkHttp (connect 15s, read 300s, buffer 64КБ, текущий прогресс в onProgress); 3) fallback assets (legacy); 4) fallback на собственный APK (applicationInfo.sourceDir); бросает IllegalStateException с причинами отказа вместо null. Прогресс скачивания маппится в 0..0.15f окна assemble (locateBaseApk передаёт p*0.15f). `ensureCachedTemplate()` возвращает `TemplateOutcome.Ready(file, updated)` / `Failed(failure, detail, cachedFile)` (failure = NO_SPACE/NETWORK/BAD_FILE); отсутствие ETag у ответа больше не считается ошибкой (сравнение по ETag+размеру только для детекта «уже актуально», `.etag` рядом с кэшем); ошибка Refresh при живом кэше не сносит ready-статус. Диалог показывает причину (строки `v3_template_error_*`, `v3_template_uptodate`, en+ru). V3ApkAssembler.assemble пробрасывает исключение, поэтому ApkBuilderV3Engine показывает реальную причину, а не обобщённое «проверьте template_runtime.apk». Пайплайн inject→patch→sign проверен headless на обеих базах (runtime-шаблон 188 МБ и self-APK 624 МБ) — работает; значит сбой на устройстве = prepareBaseApk вернул null (нет файла в установленном APK, не хватает места в cacheDir либо офлайн без кэша). `catroid/src/main/assets/template_runtime.apk` больше не лежит в assets игры V3 (гитигнорирован, гит не трекит) — при тесте V3PackageRenameTest.exportTwoGames_coexistAndVerify репак берётся из src/main/assets/template_runtime.apk с assumeTrue и равно скипается.
- **Подпись (исправлено 2026-07)**: `V3ApkAssembler.doSign` НЕ должен ссылаться на провайдер по имени `BouncyCastleProvider.PROVIDER_NAME` (= "BC") — на Android под именем "BC" уже зарегистрирован урезанный платформенный провайдер (Conscrypt), который не реализует BC content-signer, отсюда `NoSuchAlgorithmException: SHA256WithRSA for provider BC`. Используется ЭКЗЕМПЛЯР `BouncyCastleProvider()` (`.setProvider(bc)`) и генерация ключа `KeyPairGenerator.getInstance("RSA", bc)`. На JVM-тесте "BC" — полный BC, поэтому тест проходил, а устройство падало.
- **СТАЛЫЙ template_runtime.apk (исправлено 2026-07)**: закоммиченный `catroid/src/main/assets/template_runtime.apk` был СТАРЫМ (собран до появления V3-runtime) и НЕ содержал классов `RuntimeLoaderActivityV3`/`ProjectLoaderV3`. Игра собиралась и ставилась, но падала сразу при запуске (ClassNotFoundException на launcher). Перегенерирован через `./gradlew copyTemplateApk` (собирает `assembleRuntimeTemplate` = flavor `runtime` + buildType `template`, minify с `proguard-runtime.pro`, который держит `org.catrobat.catroid.apkbuildV3.**` и `apkbuildV3.runtime.**`). Результат 171 МБ и содержит V3-runtime (проверено dex-сканом).   `copyTemplateApk` падает на задаче `uploadCrashlyticsMappingFileRuntimeTemplate` (нет Firebase appId для runtime-флейвора) — обход: `./gradlew copyTemplateApk -x uploadCrashlyticsMappingFileRuntimeTemplate`. Рекомендация: перегенерировать template при любом изменении V3-runtime; желательно зашить `copyTemplateApk` в mergeAssets редактора, чтобы ассет не протухал.
- **Дедупликация пейлоада (2026-09)**: `stageProjectPayload` пакует стейджинг через `ZipArchiver.zipDedup` (MD5-дедуп одинаковых байтов + `dedup_manifest.json` в корне zip, медиа — STORED, остальное — DEFLATED level 9). `ProjectLoaderV3` (`loadFull`/`loadLight`) после распаковки применяет манифест через `DedupManifestApplier` (восстанавливает дубли копированием, манифест удаляет; без манифеста — no-op). Без дедупа пейлоад ≈ размеру папки проекта с дублями (1 ГБ → ~700 МБ в APK), с дедупом ≈ размеру `.catrobat`. ВАЖНО: `ProjectLoaderV3` — код темплейта, после правок лоадера **обязательно** перегенерировать `template_runtime.apk` (и обновить шаблон на GitHub), иначе старый лоадер не восстановит дубли из нового пейлоада и игра потеряет файлы.
- **V3 build settings fixes (2026-09)**:
  - Permissions: `patchManifest` → `syncPermissions()` — сначала удаляет ВСЕ `uses-permission` темплейта, затем добавляет только выбранные (`distinct()`). Раньше выбор только добавлял поверх ~20 разрешений редактора.
  - Firebase: чистая `FirebaseConfigManager.matchClient()` (тесты); mismatch-ошибка показывает найденные в json пакеты (`%1$s`/`%2$s`, en+ru); диалог перепроверяет json с ФИНАЛЬНЫМ package в момент Build.
  - Icon: стейдж `injectAppIcon()` (firebase→icon→sign; фейл = template-иконка, сборка живёт). Механика как `ApkToolboxManager.replaceIconInApk`, но только `ic_launcher*.png` — скомпилированные `mipmap-anydpi-v26/*.xml` не трогаются (PNG в .xml = битая adaptive-иконка на API 26+). Следствие: на API 26+ лаунчер пока показывает adaptive-иконку темплейта — нужна генерация adaptive-иконки (отдельная задача).
  - Тесты: `V3PermissionsTest` (3, без ассета), `FirebaseClientMatchTest` (5), `V3IconInjectionTest` (2, позитивный через `assumeTrue(template_runtime.apk)`).
- **V3 wizard (2026-09)**: диалог сборки переделан в 4-шаговый визард как в оригинале (`ApkBuilderV3ExportDialog` + `dialog_apk_builder_v3_export.xml`): сверху подзаголовок `v3_step_format` + анимированный ProgressBar (`ObjectAnimator`, 25/50/75/100), внизу Cancel/Back/Next (на 4-м шаге Next=Build), переходы через `AutoTransition`. Шаг 1 Template: статус кэша (`TemplateManagerV3.getCacheStatus()`), кнопка Download/Refresh → `ensureCachedTemplate()` с прогрессом; дальше без готового шаблона не пускает. Шаг 2 App (+иконка), шаг 3 Permissions, шаг 4 Firebase. Валидация package переехала на переход 2→3 (в Build оставлена как страховка). Факты в прогрессе сборки: берутся из `R.array.loading_facts` (локализованы), ротация по таймеру 10с через `Handler.postDelayed` независимо от колбэков прогресса (раньше менялись на каждый файл = мелькание).

---

#

Аудит `PathfindingManager.kt` + `MoveToObjectAction`/`HasPathAction` после правок динамических препятствий и стратегий пути. Исправлено:

- **Data race (CRITICAL)**: A* (`findPath`/`smoothPath`/`hasLineOfSight`) читал `navGrid.walkable` из `pathExecutor`-потока, пока render-поток мутировал его в `updateObstaclesDynamic` → «рваные» пути. Фикс: `snapshotGrid()` копирует `walkable` в начале `findPath`/`smoothPath`, все проверки внутри работают со снапшотом; `hasLineOfSight(from,to,grid,scm,sw,sh)` теперь принимает grid параметром (вызовы из render-контекста передают свежий снапшот).
- **addObstacle без сетки (HIGH)**: `step = navGrid?.cellSize ?: 1f` → до 100 000 точек на препятствие (вызывалось до проверки `navGrid == null` в `MoveToObjectAction`). Фикс: ранний `return` при `navGrid == null` + очередь `pendingObstacleNames`; `createGrid()` после построения пересканирует отложенные имена.
- **HasPathAction (HIGH)**: синхронный A* (до 50 000 итераций) на render-потоке в каждом кадре. Фикс: `findPathToObjectAsync` + колбэк; блок завершается сразу, результат пишется в переменную по приходу (на кадр позже).
- **update() (MEDIUM)**: линейный поиск спрайта по имени на каждый фолловер каждый кадр → `HashMap<name, Sprite>` строится один раз за кадр.
- **Вечный replan (MEDIUM)**: при недостижимой цели и `enableDynamicReplanning` фолловер перепланировал путь до конца времен — цикл не сходился. Фикс: stalePath-проверка во всех трёх replan-колбэках — если последняя точка нового пути совпадает со старой (< 1f), путь не обновляется; фолловер завершается (REACHED для end-replan, onPathBlocked+IDLE для waypoint-replan).
- **MAX_ITERATIONS**: 50000 → 200000 (пути на больших сетках не находились; A* работает на executor-потоке).
- **Мёртвый код удалён**: `updateObstacles()`, `createObstaclesFromBackground()`, `rebuildGrid()`, `findPathToObject()` (sync), `findPathWithSmoothing()`, `findPathToObjectWithSmoothing()`, `setPathForFollowerWithSmoothing()`, `getGridInfo()`, `getObstacleCount()`, `getFollowerCount()`, `getFollowerInfo()`, `isPathWalkable()`, `getNearestWalkablePoint()`, `debugPrintGrid()`, `getWalkableAreaPercentage()` — нигде не вызывались (проверено grep по всему репо).

## Редакторы: инвентаризация + подключение ParticleEditor (2026-08)

В проекте 9 редакторов. 4 были «спящими» (в манифесте, но без точек входа): TilemapEditor, DialogueEditor, ParticleEditor, NeoPaint. Подключён **ParticleEditor** (полноэкранный редактор 3D-частиц, Unity-стиль, `editor/ParticleEditorActivity.kt`):

- **Точка входа**: кнопка `btn_quick_particles` в quick-actions панели 3D-редактора (`editor_activity.xml`, иконка `drawable/ic_particles.xml`, между инспектором и разделителем).
- `EditorActivity.setupUI()`: если у выбранного объекта есть `ParticleSystem3DComponent` → `ParticleEditorActivity.Companion.launch(this, id, false)`; иначе AlertDialog «Добавить Particle System 3D?» → `go.addComponent(ps3d)` + `sceneManager.engine.createParticleProxy(id)` + `updateParticleEffect3D(...)` (тот же код, что case 8 в `InspectorManager.showAddComponentDialog`) → launch.
- Из Java companion-методы Kotlin вызываются как `Companion.launch(context, id, false)` (3 аргумента, есть дефолтный `useUi2`).
- Строки: `editor_3d_particles`, `editor_3d_particles_add_prompt` (en + ru).

Не подключены (осталось на потом): TilemapEditor (рантайм есть: `SetTilemapSolidBrick`, формулы `tilemap_width/height`; строки `look_new_tilemap`/`look_edit_tilemap` уже есть), DialogueEditor (рантайм есть: `DialogueRunner`, `StartDialogueBrick`), NeoPaint (основной флоу идёт в PocketPaint).

Идеи новых редакторов (компоненты для переиспользования): Level Designer (`TilemapEditorView`+`SceneEditorView`+`writePosition`), Animation/Keyframe (`CurveEditorView`), Physics Shape Editor (расширение `HitboxEditorView` до полигонов/окружностей), UI/HUD Editor (ShowText + WhenTouchDown с якорями), Path Editor (GlideTo waypoints), Atlas Cutter (нарезка спрайт-листов), Skeleton/Bone Editor (фундамент — ragdoll-режим 2 `PhysicsLook.updateRagdollFollow`), Game State Editor (UserVariable/UserList), AI/Behavior FSM Editor (как DialogueEditor, но для ИИ: состояния+переходы → WhenCondition/Switch), Input Mapper (геймпад/клавиши → WhenGamepadButton/KeyEvent), Variable Watch (отладка значений переменных в рантайме).

## Physics collision fix — Desktop + Android (2026-07-19)


### Android (PhysicsObject.java)
- **Баг**: `setType(Type.FIXED)` и `setType(Type.NONE)` использовали `BodyType.KinematicBody`. FIXED-объекты (стены, пол) должны быть `StaticBody` — truly immovable. KinematicBody может проталкивать динамические тела иначе, чем StaticBody, что приводит к некорректной коллизии при наклонных ударах.
- **Фикс**:
  - `FIXED` → `BodyType.StaticBody` + `gravityScale(0.0f)`
  - `NONE` → `BodyType.StaticBody` + `gravityScale(0.0f)`
  - `body.setBullet(false)` при выходе из DYNAMIC (CCD не нужен на статике)
- **PhysicsBoundaryBox**: уже `StaticBody` + `PolygonShape` — трогать не пришлось.
- **Тесты**: `PhysicsObjectTest` — обновлён `testSetType()` (KinematicBody→StaticBody), добавлен `testSetTypeBulletTransitions()` (7 переходов bullet=true/false).

---

# 3D-модели: устойчивость загрузки GLB/GLTF (2026-08)

Проблема: некоторые GLB-модели (напр. Sketchfab) давали «синий экран» в 3D-редакторе и ломали 3D-игру.

## Анализ проблемной модели (vanessa_-_fnaf_security_breach.glb, Sketchfab)

- 9.5 МБ, glTF 2.0, БЕЗ extensionsUsed/Required (нет Draco) — структурно валидна для gdx-gltf.
- 12 узлов, 10 мешей, 10 материалов, 19 текстур (JPEG+PNG), 0 анимаций/скинов.
- Индексы UNSIGNED_INT (componentType 5125), до 57К индексов на меш; TEXCOORD_0..4 (gdx-gltf читает только texcoord0, лишние игнорирует).
- Материалы: alphaMode=BLEND (ресницы/волосы/стекло), normal/emissive текстуры, doubleSided.
- Недочёт модели: `emissiveFactor: [1,0,1,0,1,0]` (6 значений вместо 3) у `MI_GregFlashlight_00` — gdx-gltf читает первые 3, не падает.
- Реальные причины «синего экрана» могут быть: OOM/GL-OOM при декодировании 19 текстур (Error, не Exception — раньше НЕ ловился), нативный GL-краш при upload (текстуры > maxTextureSize), либо исключение рендера сцены (realisticMode → gdx-gltf SceneManager).

## Что исправлено

- **`ThreeDManager.createObject()`** и **`replaceModel()`**: `catch (Exception)` → `catch (Throwable)` (OOM/Error больше не убивают приложение), в лог пишется полный стектрейс с путём модели.
- **Preflight-проверка GLB**: `hasUnsupportedGltfExtensions(FileHandle)` — читает JSON-chunk бинарного GLB (`readGltfJsonChunk`, magic glTF + chunk0) и отклоняет модели с `KHR_draco_mesh_compression` / `EXT_meshopt_compression` / `KHR_texture_basisu` (gdx-gltf 2.2.1 их не умеет) с понятным сообщением в логе.
- **Защита рендер-цикла** `renderColorsOnly()`:
  - non-realistic: `modelBatch.render()` каждого объекта обёрнут в try/catch(Throwable); упавший инстанс удаляется из `sceneObjects`/`gltfObjectIds`/`animationControllers` после `modelBatch.end()` (без ConcurrentModificationException) — игра продолжает работать.
  - realistic: `sceneManager.renderMirror()/renderTransmission()/renderColors()` каждый в своём try/catch(Throwable) с логом.
- **`SceneManager.rebuildGameObject_internal()`**: если `engine.createObject()` вернул false — вместо «пустого» объекта ставится куб-примитив (`engine.createCube`) + лог с причиной; редактор больше не показывает пустоту («синий экран»). Существующий объект при этом не трогается (guard `containsKey` в createCube).

## Проверка

`./gradlew :catroid:compileCatroidDebugJavaWithJavac --offline -q` — BUILD SUCCESSFUL (только стандартные Note).
Диагностика причины у пользователя: logcat-теги `3DManager_PBR` / `3DManager` / `SceneManager` — там теперь стектрейс причины отказа.
---

# Перенос функций и регистрации из оригинала (2026-08)

## Новые формулы (перенесены из Danveyd/NewCatroid)

- **FILE_TO_BASE64** («файл_в_base64») — файл из проекта -> Base64 (NO_WRAP).
- **MD5** («md5») — MD5-хеш строки (HashUtils.hashString).
- **NOTIFICATION_REPLY** («ввод_из_уведомления») — ответ, сохранённый в уведомлении
  (NewCatroidNotificationManager.getSavedReplies по cleanStringId).
- **READ_FILE** («прочитать_файл») — чтение файла проекта (лимит 2 МБ, UTF-8).
- Оператор **CONCAT** уже был у нас (не переименован в STRING_CONCAT ради совместимости проектов).

### Файлы правок
- `formulaeditor/Functions.java` — enum + TEXT-сет (после LIST_MAX).
- `formulaeditor/FormulaElement.java` — 4 case после case FILE; импорт NewCatroidNotificationManager.
- `formulaeditor/InternFormulaKeyboardAdapter.java` — case'ы formula_read_file/file_to_base64/md5/notification_reply.
- `formulaeditor/InternToExternGenerator.java` — маппинги в INTERN_EXTERN_LANGUAGE_CONVERTER_MAP.
- `ui/recyclerview/fragment/CategoryListFragment.java` — STRING_FUNCTIONS/STRING_PARAMS
  (после formula_file_read_string*).
- `strings.xml` + `values-ru` — formula_read_file(+_param), formula_file_to_base64(+_param),
  formula_md5(+_param), formula_notification_reply(+_param). ВАЖНО: апострофы в параметрах
  экранируются `\'\'` (aapt падает «Invalid unicode escape sequence» на голых кавычках).

## Регистрация Pen/Pt/ML бриков (2026-08)

- **CategoryBricksFactory.kt** (ui/fragment): penBrickList += SetPenPropertyBrick(0,"1"),
  PenDrawLineBrick, PenDrawTriangleBrick, PenDrawRectBrick, PenDrawCircleBrick, PenFlushBrick,
  PenClearColorBrick; setupPocketensorCategoryList += PtCreateNormalTensorBrick, MLStepAdamWBrick,
  PtSliceBrick, PtDropoutBrick, PtZeroGradBrick, PtClipGradBrick, PtLayerLinearBrick, PtConv2DBrick,
  PtMaxPool2DBrick, PtGruCellBrick, PtLstmCellBrick, PtEmbeddingBrick, PtAttentionBrick;
  dataBrickList += SetTextPropertyBrick("myText", 0, "100") (2 места).
- **XstreamSerializer.java** — 21 xstream.alias("brick", ...) после DeleteFirebaseFileBrick.class.
- **BrickInfo.java** — описания ru (add) + en (addEn) для 8 Pen/Text бриков (тексты из оригинала)
  и 13 Pt/ML (свои краткие); вставлены перед закрывающей "}" ru-секции (после addEn(VisualPlacementBrick)).
- **ActionFactory.java** — 21 create-метод (createPtLayerLinearAction ... createPenDrawCircleAction).
- **StageListener.java** — setActorZIndexSafely(actor, zIndex).

## ShowTextActor (float-позиции, 2026-08)

- xPosition/yPosition int -> float; сеттеры setPositionX/Y(float), setScaleX/Y, setAlphaValue,
  setRotationDegrees, setRelativeSize, setColorStr, setRawText, setAlignment, setFontFromFile.
- В drawText применены scaleX/scaleY/alpha/rotation (обе ветки: кэш и пересоздание).
- drawVariables использует rawText при isText. Kotlin-вызовы (ShowTextAction.kt) через .toFloat().

---

### Перенос формул из оригинала, часть 2 (2026-09)
Сверка с оригиналом показала 21 недостающую функцию + 5 сенсоров. Проверка на дубликаты: точные аналоги уже были только у `SHA256` (=`SHA_256`), `CLIPBOARD_TEXT` (=`CLIPBOARD_PASTE`), `ARCH` (=`CPU_ARCHITECTURE`), `BATTARY` (=`BATTERY_PERCENT`) — для совместимости старых проектов добавлены как отдельные константы с тем же поведением. Остальное портировано: `CAMERA_X/Y/ROTATION/ZOOM` (+4 геттера в `StageListener`), `CHAR_TO_UNICODE/UNICODE_TO_CHAR/UNESCAPE_UTF`, `FILE_LAST_MODIFIED/FILE_MD5/FILE_SHA256`, `HTTP_RESPONSE_TEXT/CODE/HEADER` (бэкенд `NewCatroidHttpManager` уже был), `MQTT_MESSAGE` (бэкенд уже был), `MEDIA_DURATION/IMAGE_WIDTH/IMAGE_HEIGHT`, `JOINNUMBER` (+`interpretFunctionJoinNumber`), `PT_SAMPLE` (бэкенд `MLBridge` уже был). Сенсоры: `ARCH/BATTARY/CLIPBOARD_TEXT` в `SensorHandler`, `KEYBOARD_HEIGHT` (+статики `isKeyboardVisible/realKeyboardHeight` и insets-listener в `StageActivity`), `OBJECT_NAME(true)` (→`sprite.name` в `FormulaElementOperations`). Палитра/адаптер/extern/строки en+ru заведены. Заодно: `NewCatroidBackgroundService` объявлен в манифесте (был только класс, фоновый тик не стартовал).

# Crash-обработка (2026-08)

- **BaseExceptionHandler.kt**: глобальный UncaughtExceptionHandler ставится в CatroidApplication.onCreate\n  (ранний перехват) И повторно в MainMenuActivity.onCreate (последняя установка — никем не перетирается,\n  как в оригинале; НЕ делегирует default-хендлеру, чтобы системный kill не помешал CrashActivity).
  Собирает отчёт (CrashReporter: logcat 1000 строк E/F + стектрейс, до 200КБ), сохраняет в
  `cacheDir/crashReports/crash_<uuid>.txt` **и** в `cacheDir/last_crash_log.txt`, ставит
  RECOVERED_FROM_CRASH=true, запускает CrashActivity (NEW_TASK|CLEAR_TASK).
- **CrashActivity**: диалог «Something went wrong» -> OK -> главное меню; в фоне отправка
  отчёта в Firestore (коллекция `crashes`, TelemetryManager.getTelemetryFirestore), после
  успеха файл удаляется; удаляет и last_crash_log.txt.
- **MainMenuActivity**: при старте читает/удаляет last_crash_log.txt -> если есть, показывает
  AlertDialog со стектрейсом (fallback на случай, когда CrashActivity не успела показаться —
  краши на GL-потоке/OOM) + sendPendingReports() донасылает неотправленные отчёты.
- **RECOVERED_FROM_CRASH**: BaseActivity закрывает все активити кроме MainMenuActivity.
- **EditorActivity**: свой handler (emergency-save сцены в AUTOSAVE_FILE_NAME JSON), затем
  делегирует дефолтному.
- Настройка: `setting_enable_crash_reports` (по умолчанию true).
- Отличие от оригинала (Danveyd/NewCatroid): у них нет отправки — только last_crash_log.txt +
  диалог в главном меню при следующем запуске (MainMenuActivity.onCreate); их глобальный
  BaseExceptionHandler в BaseActivity закомментирован.

# Scene-переменные (2026-08)

Scene-локальные переменные: видны только внутри своей сцены, сбрасываются при выходе из сцены. Сценных списков (UserList) НЕТ.

## Порядок поиска переменной (UserDataWrapper.getUserVariable)
sprite -> scene -> project -> multiplayer. Scene-переменная с тем же именем, что project-переменная, выигрывает (testSceneVariableHidesGlobalVariable).

## Модель и сброс
- Scene.java: поле @XStreamAlias("sceneVariables") (List<UserVariable>, transient getter, updateUserDataReferences), методы getSceneVariable(s)/addSceneVariable/removeSceneVariable/resetSceneVariables; sceneVariables добавлен в XStreamFieldKeyOrder.
- UserDataWrapper.getUserVariable — scene-lookup; UserDataWrapper.resetAllUserData — цикл по project.getSceneList() c scene.resetSceneVariables() (при старте проекта).
- StageListener.resetLeavingSceneVariables() вызывается до смены 	his.scene в 4 точках: doSceneSwitch, transitionToScene (2-arg), applySceneSwitch, applyStartScene.

## UI
- Спиннеры с scene-переменными (после sprite-vars, до project vars): UserVariableBrickWithFormula, UserVariableBrickWithVisualPlacement, UserDataBrick (только ветка variables), WhenVariableChangedBrick, UserVariableBrick.
- UserVariableBrickTextInputDialogBuilder — radio «Scene» показывается только если editedScene != null && !editedScene.isGlobalScene(); создание через editedScene.addSceneVariable().
- DataListAdapter VAR_SCENE=4, DataListFragment.kt (спиннеры/секции), ListSelectorFragment.kt:142.
- ShowTextActor.draw — отрисовка scene-переменных: Scene currentScene = ProjectManager.getInstance().getCurrentlyPlayingScene(); if (currentScene != null) drawVariables(currentScene.getSceneVariables(), batch);
- Строки: 7 новых en+ru.

## Тесты
- 	est/formulaeditor/SceneUserVariableTest.java — 9 тестов (add/get/remove/reset, lookup-порядок, изоляция сцен, сброс через resetAllUserData). Все зелёные.
- Факты: UserDataWrapper — final, приватный конструктор, статические методы getUserVariable(String, Scope)/getUserList(String, Scope)/resetAllUserData(Project); 
ew Scope(project, sprite, null) валиден; в plain-JUnit обязателен MockUtil.mockContextForProject() (Project.<init> зовёт context.getString — мок возвращает null для незаглушенных ключей).
- XStream: сериализация работает автоматически (алиас userVariable уже есть в XstreamSerializer:268).

## Известная хрупкость (НЕ наша)
- SceneTransitionActionTest (2 теста) падает в пачке (GlobalSceneTest/NeoScriptSceneTest + др. в одном процессе): NPE «Scene.getName() is null» в SceneTransitionAction.update (~:45) — другой тест ставит currentlyPlayingScene = defaultScene с null-именем (мок getString(R.string.default_scene_name)). В изоляции тест проходит, на чистом HEAD в изоляции тоже проходит — pre-existing, не регрессия.
## Crash fix: NPE в hasUserDataChanged / hasSameValue (2026-08)

Причина: правка формулы из FormulaEditor (SpriteActivity.onBackPressed > ScriptFragment.checkVariables):
`UserVariable.hasSameValue` падал с NPE, т.к. `value`/`list` у UserVariable/UserList — **transient**
(не восстанавливаются при загрузке проекта = null). Любой вызов (List.size() on null) валил сравнение,
что блокировало выход и передавало null в `hasUserDataChanged`.

- `UserVariable.java`: `hasSameValue`/`equals`/`hashCode` — null-safe (value и name).
- `UserList.java`: `hasSameListSize`/`equals`/`hashCode` — null-safe (list и name).
- `Project.java` + `Sprite.java`: `hasUserDataChanged` — null-список = пустой список
  (size 0); `checkEquality`/`checkUserData` — guard на null oldUserData.
- Тест: `test/formulaeditor/UserVariableNullSafetyTest.java` (6 тестов).

# Fix: старые блоки и alias ShowToastBlock (2026-08)

Проблема: проекты, созданные до переименования `ShowToastBlock` > `ShowToastBrick` (2026-07-08),
падали при загрузке: `<brick type="ShowToastBlock">` не находил класс и заменялся на `UnknownBrick`
(данные блока терялись).

- `XStreamBrickConverter`: добавлена карта `LEGACY_BRICK_ALIASES`
  (`ShowToastBlock` > `ShowToastBrick`) + remap в `doUnmarshal`. Старые проекты загружаются корректно.
- `ShowToastAction.kt`: null-guard на `StageActivity.messageHandler` + fallback-тост через
  `CatroidApplication.getAppContext()` на main-looper (защита от NPE, если handler null),
  удалён неиспользуемый `contextt`.
- Тест: `test/formulaeditor/ShowToastLegacyAliasTest.java` (2 теста: legacy remap при
  десериализации + round-trip сохранения).

# Фиксы редактора и скриптов (2026-08)

## 3D-редактор (editor/)
- UndoManager: synchronized + исполнение команд на GL-потоке; clear() в onEngineReset.
- Commands: Delete/AddCommand сериализуют всё поддерево (undo/redo не теряет детей композитов).
- Crash-handler: identity-check перед restore в onDestroy; catch Throwable; атомарная tmp+rename запись.
- onCreate guard: savedInstanceState != null или нет проекта -> finish (process-death).
- Все мутации сцены из UI (delete/duplicate/add/focus/particles) обёрнуты в Gdx.app.postRunnable;
  то же для InspectorManager delete-object/prefab-remove и remove{Render,Physics,Light}Component,
  renameGameObject(engine), setObjectActive, setFriction/setRestitution.
- Autosave сцены на диск (tmp+rename) в onPause + дебаунс 3с от pushCommand; чистый выход/Clear Scene
  инвалидирует _recovery_autosave.rscene; EditorStateManager удалён (write-only кэш).
- Recovery: JSON-валидация до диалога, восстановление через resetEngine (GL-поток).
- Save scene: санитизация имени, сериализация GL + запись в фоне, compact-json.
- requireEngineReady() для save/load/clear/skybox; ACTION_CANCEL у камерных кнопок.
- bulk-duplicate клонирует источник каждый раз (был O(2^n)); TransformCommand создаётся после мутации.
- SpriteActivity.saveProject debounce 800мс; getCurrentSceneData deep-copy только при PrefabComponent.

## Вьюпорт
- Тап по хэндлу без движения вызывает gizmo.touchUp(); setCurrentTool завершает драг;
  мультитач-guard в touchDown; rotate детей конвертируется в пространство родителя.
- Камера: pitch-clamp ±89°, resetMotion() в onPause, pinch-dolly zoom, quick-focus через postRunnable.
- PS3D-объекты выделяемы тапом; прокси из worldTransform; dispose снимает InputProcessor.
- Кейфреймы: Play-guard пустого списка, scale clamp >=0.01, debugDrawer begin/end один на кадр,
  драг удалённого объекта прерывается. applyTransformToEngine: tmpQuaternion вместо new на объект.
- FALSE POSITIVE (не чинить): «двойной mul bbox гизмо» — calculateBoundingBox возвращает локальный bbox,
  один .mul корректен (проверено исходниками libGDX).

## Инспектор
- editor_3d_physics_states = 5 значений в порядке PhysicsState (en+ru) + clamp — был IOOBE и запись NONE.
- Новый editor_3d_easing_types (33 = enum EasingType) en+ru; brick_easing_types не тронут (для кирпичей).
- Preview анимации: previewingOwner + cancelStalePreview (поза возвращается правильному объекту).
- Rename guard selectedObject == go; DelayedTextWatcher = реальный debounce 300мс.
- Спиннеры physics/light/animation/fog/shape: post{} attach + clamp (нет фантомного первого fire).
- Collider/camera watcher'ы hasFocus-guard; EyeAdaptation updatePP; PS3D debounce проверяет живость GO.
- Удаление CameraComponent -> findAndSetMainCamera; setMaterialComponent null-guard; keyframes под
  synchronized(anim.keyframes); updateKeyframeAnimations итерирует снапшот; Play пустых keyframes -> тост.

## Скрипты (legacy)
- AI-таймер не дёргает updateItems во время drag/action mode; code analysis: снапшоты списков +
  toList() в CodeAnalyzer + try/catch + generation-counter (GlobalScope CME).
- Выделение: bounds-guard позиций (-1 свёрнутых детей) в setSelectionTo/selectedItems.
- copyProjectForUndoOption(2000ms): await снапшота ДО мутации (delete/cut/paste).
- handleContextualAction return после finish(); одиночный DELETE/COPY через полный путь guards.
- pasteBricksBelow: resolveBrickReferences перелинковывает Look/Sound/UserVariable/UserList на целевой
  спрайт (рекурсивно по композитам).
- exportScripts клонирует скрипты на main до Thread; backpack.json атомарно; unpack по flat-list;
  CSV-escape значений списков; повторный pack мержит звуки; пустой pack -> тост ошибки.
- ACTION_CANCEL в BrickListView = отмена переноса; onBackPressed проверяет isCurrentlyMoving до workspace.
- addItem -> Boolean (нет фантомного startMoving); showUndo(false) только в мутирующих ветках;
  Log.d удалены из getView; O(n^2) indexOf -> indices loop.

## Формульный редактор (+совместимость со старыми проектами)
- InternToExternGenerator: context==null -> fallback intern-имя (NPE в EventId.hashCode на stage-потоке);
  parseDouble в trim-пути -> try/catch.
- InternFormulaParser: парс строго на копии токенов; bracket-correction пишется обратно только при успехе.
- FormulaEditorFragment: null-guard полей в onCreateView (process-death); refreshFormulaPreviewString
  реинфлейтит brick-view только при смене кирпича/поля; hasFileChanged потоково сравнивает файлы;
  onActivityResult requestCode-guard; ACTION_CANCEL останавливает автоповтор Backspace.
- Formula.java: ensureInternFormula() во всех update*-методах (rename спрайта/переменной после загрузки
  старого проекта; миграции v<=0.993).
- WorkspaceLayout: окно формулы закрывается только при успешном сохранении формулы.
- ExternalIpFetcher: main-thread guard + негативный кэш 30с.
- Ui2: saveProjectToDisk в фоновом потоке.
- DEVICE_PARAMS генерируется размером DEVICE_FUNCTIONS (54<->54, хинты выровнены).
- Guard'ы: handleDeletion(RIGHT) null-check; DISTANCE null->0; Operators.getOperatorByValue null-check.

## Совместимость со старыми проектами
Форматы не менялись: XStream-поля, порядок Functions/Sensors/Operators, INTERN_EXTERN_MAP, .rscene,
backpack.json (CSV-escape обратно совместим). Автоскобки парсера попадают в кирпич при успешном OK.

## Отложено (требует рефакторинга)
RecyclerView-миграция BrickAdapter; объектное выделение кирпичей; command-дифы FormulaEditorHistory;
явный Cancel в формулах; утечки SensorHandler/FormulaEditorClipboard/IntroDialog; project-undo формул;
OBB/ray-triangle пикинг; dirty-flag трансформов; кэш bbox raycast; listFiles-кэш пикеров; Fog-ветка.

## Сборка
`./gradlew :catroid:compileCatroidDebugKotlin :catroid:compileCatroidDebugJavaWithJavac --offline` — OK.
Формула-тесты formulaeditor.*: 72 фейла pre-existing (stash-бисекция InternFormulaParser: те же на HEAD).

