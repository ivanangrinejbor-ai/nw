# NeoCatroid — гайд для разработчиков

Форк Catrobat/Catroid. Конструктор приложений с визуальным программированием через блоки.

Быстрый старт: `./gradlew copyTemplateApk` для обновления APK-темплейта.



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

# AI Project Assistant

Папка `aip/`: datasets, training, model, deploy.
Обучение: `python train.py` (n-gram) или `python train_lstm.py` (LSTM).
Деплой: `aip\deploy.bat` копирует model.* в assets.

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
- **TouchDirectionBrick** — автоматически вычисляет угол от спрайта к точке касания

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
| Brick | Action | Parameters | Description |
|-------|--------|------------|-------------|
| `ImportScriptBrick` (File→NeoScript cat.) | `ImportScriptAction` | objectName, filePath, overwrite | Import .neoscript into existing object |
| `CreateObjectBrick` (NeoScript cat., NEW) | `CreateObjectAction` | objectName (Formula), scene (spinner), persist (Yes/No) | Create blank sprite in scene; if persist=Yes, save canonical project to disk |
| `AssignScriptsBrick` (NeoScript cat., NEW) | `AssignScriptsAction` | filePath, objectName, scene, replace (Yes/No), save (Yes/No) | Assign .neoscript to object in scene; if save=Yes, save canonical project to disk |

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

# Исправления безопасности и багов (2026-07)

## 🔴 Критические проблемы безопасности
- **Keystore удалён из VCS**: `catroid/keystore` → `git rm --cached`, добавлен в `.gitignore` (настоятельно рекомендуется отозвать ключ)
- **GitHub OAuth Client ID**: `SettingsFragment.java` — захардкоженный client ID заменён на `BuildConfig.GITHUB_CLIENT_ID` с fallback (сам литерал в документации не приводится)
- **Gemini API key**: `GeminiManager.kt` — `@Deprecated api_key` синхронизирован с `EncryptedSharedPreferences`; `SetGeminiKeyAction.kt` пишет в оба места
- **WebView URL validation**: `StageActivity.createWebViewWithUrl()` — только HTTPS/file/shell схемы, остальные отклоняются
- **Path traversal prevention**: PutFileIntoFolderAction, PutFileIntoPathAction, UnzipAction, DeleteFolderByPathAction, CreateFolderByPathAction, CopyProjectFileToPathAction — canonical path validation
- **AskGemini2Action.kt**: удалён `hostnameVerifier { _, _ -> true }`, добавлены timeouts, JSONObject вместо raw string
- **WriteVariableToFileAction.kt**: `System.getProperty("user.home")` → `Environment.getExternalStoragePublicDirectory`

## 🧹 Чистка мусора
- `catroid/src/main/libs/test/` (382 файла) — удалён
- `catroid/src/main/libs/__prebuilt_aar_backup/` — удалён
- `assets/ababuy.txt` — удалён

## 🛠️ Система сборки
## Desktop Runtime — code.xml parsing (важно)

`BakedApkBuilder.kt` пишет `code.xml` через `XstreamSerializer` (XStream). Но **реальный
`code.xml`, который кладётся в `project.zip` (NCPP-зашифрованный бандл проекта, см. ниже),
имеет ДРУГОЙ формат** — тот, что экспортирует редактор Catrobat/XStream при сохранении
проекта, а не формат runtime-конвертеров. Проверено по расшифрованному `project.zip`
пользователя (720×1600 портретный проект, 4 спрайта: Фон + 3 рабочих).

### Реальный формат `code.xml` (из `project.zip`)

- **Кодировка**: файл объявляет `<?xml ... encoding="UTF-8"?>`, но РЕАЛЬНО записан в
  **CP1251** (кириллические имена спрайтов/проекта). `DesktopProjectManager.loadProject`
  делает `transcodeToUtf8` (UTF-8 decoder + REPORT unmappable → fallback) перед парсингом.
- **Вложенная структура** (НЕ плоская):
  ```
  <program><header screenWidth=.. screenHeight=.. landscapeMode=.. screenMode=..>
    <scenes>
      <scene>
        <objectList>
          <object type="Sprite" name="Фон">
            <scriptList/>            <!-- пустой для фона -->
          </object>
          <object type="Sprite" name="Птица">
            <scriptList>
              <script type="StartScript" posX="0.0" posY="0.0">
                <brick type="PlaceAtBrick"> ... </brick>
                <brick type="ForeverBrick">
                  <loopBricks> ...дети... </loopBricks>
                </brick>
              </script>
              <script type="WhenScript"> ... </script>
            </scriptList>
          </object>
        </objectList>
      </scene>
    </scenes>
  </program>
  ```
  Парсер (`DesktopScriptEngine.parseXmlScripts`) ищет спрайты в
  `scenes/scene/objectList/object` с фолбэком на плоский `<object>` (compat).
  Порядок спрайтов совпадает с `DesktopProjectManager.loadProject` (тот же `spriteEls`).
- **Контейнерные брики**: дети лежат в `<loopBricks>` (если есть), иначе старый
  `findLoopEnd` по `</brick>`. Helpers `kidsOf`/`containerEnd` обрабатывают оба случая.
- **Формулы**: legacy-вид `<formulaList><formula category="X_POSITION"><additionalChildren/>
  <type>NUMBER</type><value>0</value><rightChild>...</rightChild></formula></formulaList>`.
  НЕТ `<formulaMap>`/`<formulaTree>` в этом проекте. `getFormulaElement` читает legacy
  `<formula>`, рекурсивно строит `<formulaElement>` из `type`/`value`/`leftChild`/`rightChild`
  через `convertLegacyFormula` (поддерживает дерево OPERATOR, напр. `MINUS`/`RAND`, и
  унарный минус через `leftChild=null`). Поддерживается и XStream-вид (`<formulaMap>`) через
  `convertXStreamFormulaElement`.

### История багов парсинга
- **БАГ (исправлен 2026-07, повторно 2026-07)**: парсер искал `<object>`/`<scriptList>` на
  верхнем уровне и legacy `<formulaTree>/<formulaElement>` → ничего не находил → скрипты не
  создавались, формулы `null`. Исправлено: вложенная структура `scenes/scene/objectList/object`,
  `loopBricks`, legacy `<formula>` через `convertLegacyFormula`, CP1251→UTF-8. Верифицировано
  на реальном проекте: 4 спрайта, 5 скриптов (вкл. `WhenScript`), формулы читаются.

Bug B (инверсия X при drag): в коде desktop-рантайма инверсии НЕТ — проверено
`DesktopInput.mouseWorldX = mouseX - width/2`, `fingerX = mouseWorldX`, рендер
`screenX = VIRTUAL_WIDTH/2 + sprite.x`, `goto_touch`/`touch_direction`/сенсоры `MOUSE_X`/`FINGER_X`.
Если инверсия повторяется после фикса формул — нужен конкретный проект/блоки пользователя.

## Desktop Runtime — проектный бандл (`project.zip` / NCPP / NCPW)

- `desktop-runtime/project.zip` — зашифрованный бандл проекта, который кладётся рядом с
  `NeoCatroid.exe` (71 КБ launch4j-лаунчер + внешний `player.jar`).
- Формат: magic `NCPP` (4E 43 50 50) = AES-256-GCM + PBKDF2. Layout: `NCPP`(4) + salt(32) +
  IV(12) + ciphertext. Пароль хранится в константе `DesktopStage.PAYLOAD_PASSWORD`
  (тот же, что в Android `ProtectedProjectPayload.PASSWORD`; сам литерал в документации не приводится).
- **Новые сборки (EXE, ProjectOptionsFragment.buildExe)**: `project.zip` (entry win-бандла)
  теперь обёрнут в контейнер **NCPW** с РАНДОМНЫМ паролем на каждую сборку:
  `NCPW`(4) + pwdLen(int32 BE) + password(UTF-8) + NCPS-поток.
  - Android-сторона: `ProjectCrypto.writePasswordContainerHeader(out, password)` +
    `ProjectCrypto.generateRandomPassword()` (16 random bytes → hex) затем
    `encryptDirectoryToStreamChunked(..., password)`.
  - Desktop-сторона: `DesktopStage.readPasswordContainer()` достаёт пароль из контейнера,
    расшифровывает им вложенный NCPS/NCPP; без магии `NCPW` — легаси-константа (backward-compat).
  - `build_exe.bat`/`embed_payload.ps1` менять НЕ надо — пароль уже внутри `project.zip`,
    футер NEOCAT01 просто переносит его целиком.
  - Baked APK (`BakedApkBuilder`/`AlignedApkBuilder`) уже давно генерируют случайный пароль
    на каждую сборку и кладут его в ассет `neocatroid.key`; константа остаётся только
    legacy-фолбэком (RuntimeLoaderActivity/PayloadDecryptor).
- `DesktopStage.extractPayload()` проверяет магию `NCPP` (или `NCPW`) и расшифровывает; нет магии →
  грузит как обычный zip (backward-compat).
- **Важно для сборки EXE**: `build_exe.bat` на шаге staging удаляет ВСЕ папки в корне
  `desktop-runtime`, кроме `icon`/`jre` (в т.ч. `src`!). Запускать повторно только ПОСЛЕ
  `git checkout -- desktop-runtime/src` и восстановления полного `launch4j`.
- Для правки рантайма достаточно пересобрать `player.jar` (`./gradlew :desktop-runtime:jar
  --offline`) и положить рядом с `NeoCatroid.exe` (dontWrapJar=true → EXE берёт jar снаружи).
  Перевыпаковывать EXE НЕ нужно.

## Desktop Runtime — Letterbox (2026-07)

- Проекты с соотношением сторон, отличным от окна (напр. вертикальный 720×1600 в окне
  1280×720), НЕ растягиваются — используется `FitViewport(virtualWidth, virtualHeight)` +
  чёрная заливка `ScreenUtils.clear(0,0,0,1)` вне viewport. Результат: чёрные полосы снаружи,
  сцена по центру («чёрные полосы до фона»).
- `virtualWidth`/`virtualHeight` берутся из `code.xml` (`screenWidth`/`screenHeight` в
  `<header>`), поля добавлены в `DesktopProject`, заполняются в `DesktopProjectManager`.
  Дефолт 1280×720 (если проект не задал).
- Координаты спрайтов (`screenX = VW/2 + x`), HUD-текст и overlay-баблики (think/say)
  используют локальные `VW`/`VH` из проекта, НЕ хардкод 1280×720.

## Desktop Runtime — тормоза старта (исправлено 2026-07)

- Единственный неограниченный сетевой вызов был `askGeminiApi` (DesktopScriptEngine.kt) —
  без `connectTimeout`/`readTimeout`. При стартовом блоке Ask Gemini и недоступности сети
  поток висел на TCP/DNS-таймауте ОС 5–20 мин. Исправлено: `connectTimeout = readTimeout =
  15_000`. Все остальные сетевые пути уже ограничены (WebRequest=10с, firebase=5с).
- EXE/player.jar после фикса стартует мгновенно (проверено: лог появляется сразу).

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

## 🎨 NeoPaint
- **Layout**: `activity_neopaint.xml` — починено позиционирование (action_bar/toolbar/layers_panel/property_bar)
- **Save**: `saveAndReturn()` — при `picturePath == null` сохраняет во временный файл и возвращает `RESULT_OK`
- **UI**: `setupToolbars()` — ImageButton + setSelected() + подсветка активного инструмента
- **Icons**: 17 vector drawable иконок + selector'ы (tool_button_bg, action_button_bg)
- **Labels**: добавлены `lbl_brush_size` / `lbl_opacity`
- **State**: `onSaveInstanceState`/`onRestoreInstanceState` для поворота экрана
- **DrawingView**: удалены дублирующие `max()`/`min()`, `smudgeSrc = null` в `ACTION_UP`, PorterDuff.Mode.CLEAR → BlendModeColorFilter (API 29+) с fallback
- **Dialog**: `text_dialog` — AlertDialog.setPositiveButton() (устаревшее create().apply)

## 🧪 Тесты (20 файлов для 10 новых блоков)
### Brick tests (верификация addActionToSequence):
1. `PutFileIntoFolderBrickTest.java`
2. `PutFileIntoPathBrickTest.java`
3. `SendNotificationBrickTest.java`
4. `ShowScheduledNotificationBrickTest.java`
5. `NotificationActionBrickTest.java`
6. `PrepareNotificationBrickTest.java`
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

## 🐛 Исправление pre-existing ошибок компиляции
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

## 📦 Обновление зависимостей (2026-07)
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

# Windows Desktop Player (build_exe)

## Модули

- `:core` — JVM-модуль с портативными seam-интерфейсами (RuntimeServices, AudioService, MidiService, TextService, NotificationService) и holder-ами.
- `:desktop-runtime` — JVM-модуль с Desktop-реализациями seam (DesktopAudioService, DesktopMidiService, DesktopTextService, DesktopNotificationService, DesktopRuntimeServices) и точкой входа DesktopStage.
- `:catroid` (Android) — содержит кнопку **«Собрать EXE»** в ProjectOptionsFragment.

## UI: Кнопка "Собрать EXE" (ProjectOptionsFragment)

- **Layout**: `fragment_project_options.xml` → `@id/project_options_build_exe`
- **Kotlin**: `setupBuildExeOption()` (уже была) + `buildExe()` (реализована 2026-07)
- **Что делает**:
  1. Сохраняет проект
  2. Пакует проект в `{projectName}.zip`
  3. **Шифрует** zip тем же `ProjectCrypto.encrypt` (AES-256-GCM + PBKDF2, пароль `ProtectedProjectPayload.PASSWORD` — как в Baked APK) → `{projectName}.enc`
  4. Находит иконку проекта (`manual_screenshot.png` или `automatic_screenshot.png`)
  5. Добавляет в `{projectName}_win.zip`: зашифрованный проект (entry `project.zip`), `template_win.zip` (из assets), `build_exe.bat` (из assets), `icon.png`, README_WINDOWS.txt
  6. Создаёт итоговый `{projectName}_win.zip` и открывает share-диалог
- **Переводы**: `project_options_build_exe` (values + values-ru)

## Gradle: copyDesktopTemplate

- `catroid/build.gradle` — задача `copyDesktopTemplate`, копирует `template_win.zip` и `build_exe.bat` из `desktop-runtime/` в `catroid/src/main/assets/`.
- Автоматически запускается перед mergeAssets.

## build_exe.bat (Windows, launch4j)

- Лежит в `desktop-runtime/build_exe.bat`
- **Шифрование проекта**: телефон кладёт уже зашифрованный `project.zip` (AES-256-GCM, магия `NCPP`). `DesktopStage.extractPayload()` при старте проверяет магию `NCPP` и расшифровывает тем же паролем `ProtectedProjectPayload.PASSWORD`; если магии нет — грузит как обычный zip (обратная совместимость со старыми/нешифрованными проектами).
- **Автономность**: `template_win.zip` собирается с **вшитым JRE** (`jre/`) и **вшитым launch4j** (`launch4j/`). `build_exe.bat` ищет launch4j в порядке `%LAUNCH4J_HOME%` → `%ROOT%launch4j\` → распакованный шаблон `build\win-dist\bundle\launch4j\`, поэтому конечному пользователю ничего подкладывать не надо.
- Собирает `player.jar` (или берёт из шаблона) → встраивает `project.zip` как NEOCAT01-footer → конвертирует PNG в ICO → при наличии launch4j в шаблоне создаёт `NeoCatroid.exe` (с бандлом `jre/`), иначе `NeoCatroid.bat`.
- Шаг упаковки шаблона (`template_win.zip`) копирует `launch4j/` из `desktop-runtime\launch4j\` в bundle, чтобы launch4j попал в ассеты Android-пакета.

### ⚠️ Известный регрессионный баг сборки EXE (2026-07)
`git`-версия `desktop-runtime/launch4j/` **НЕПОЛНАЯ** — в ней нет `lib/` (xstream.jar и т.п.),
`bin/` (windres.exe/ld.exe), `head/` (guihead.o, head.o) и `w32api/` (crt2.o и MinGW .a).
Поэтому `launch4jc.exe`/`launch4j.exe` падают (NoClassDefFoundError → затем
"cannot find crt2.o / guihead.o"). Полный `launch4j` лежит в `template_win.zip`
(`build/win-dist/bundle/launch4j/`) и в `C:\Users\ivanp\Downloads\launch4j-3.50-win32\launch4j\`.
**Решение**: скопировать `lib/`,`bin/`,`head/`,`w32api/` из полного launch4j в `desktop-runtime/launch4j/`.
Дополнительно: `build_exe.bat` на шаге staging (6a) удаляет ВСЕ папки в корне `desktop-runtime`,
кроме `icon`/`jre` — в т.ч. удаляет сам `launch4j` и `build`. Запускать повторно только ПОСЛЕ
восстановления `launch4j` (например, `git checkout -- desktop-runtime/launch4j` + долить недостающие папки).
Для headless-сборки использовать КОНСОЛЬНЫЙ `launch4jc.exe`, а не GUI `launch4j.exe`
(GUI может не подняться без desktop-сессии). Иконку в `launch4j.xml` задавать АБСОЛЮТНЫМ путём
(launch4j резолвит icon относительно файла xml, а не exe).

## Переносимые seam (в `:core`)

| Seam | Интерфейс | Holder |
|---|---|---|
| Runtime | `RuntimeServices` | `RuntimeServicesHolder` |
| Audio | `AudioService` | `AudioServiceHolder` |
| Midi | `MidiService` | `MidiServiceHolder` |
| Text | `TextService` | `TextServiceHolder` |
| Notification | `NotificationService` | `NotificationServiceHolder` |

StageListenerHolder: `object StageListenerHolder { var listener: StageListener? = null }` (в `:core`).

## Исправления багов (2026-07-13)

### DesktopMidiService
- `playSoundFile()` / `playSoundFileWithStartTime()` — были пустыми заглушками → теперь делегируют `AudioServiceHolder`.

### DesktopSprite
- Добавлено поле `visible: Boolean = true`.

### DesktopPhysicsWorld
- Добавлен метод `getBody(sprite): Body?` (был приватным).

### DesktopSprite
- Добавлены поля: `transparency`, `brightness`, `color` (графические эффекты); `width`, `height` (переопределение размера); `penDown`, `penSize`, `penColorRed/Green/Blue` (перо); `rotationStyle` (0/1/2); `lookWidth`, `lookHeight` (computed).

### DesktopStageListener
- Невидимые спрайты (`!sprite.visible`) пропускаются при рендере.

### DesktopInput (обновлён 2026-07-13)
- Добавлены: `mouseDeltaX`, `mouseDeltaY` (для сенсоров MOUSE_DELTA), `fingerX`, `fingerY`, `isTouched` (зеркало мыши для сенсоров касания).

### DesktopScriptEngine — полная переработка (2026-07-13)
- **Стековая машина**: каждый скрипт = свой `ScriptState` со стеком фреймов.
- Фрейм: `{blocks, ip, repeatRemaining, waitTimer, glideState}`.
- **RuntimeFormula**: формулы с сенсорами/переменными вычисляются при каждом проходе (а не при парсинге).

#### Поддерживаемые контейнерные брики
- **ForeverBrick** → `repeatRemaining = -1`, сброс ip на 0 при завершении.
- **RepeatBrick** → `repeatRemaining = N`, декремент, сброс ip.
- **RepeatUntilBrick** → forever + wait_until condition.
- **IfLogicBeginBrick** → then-branch / else-branch (IfLogicElseBrick).
- **IfThenLogicBeginBrick** → if без else.
- **ForVariableFromToBrick** → repeat с вычисленным числом итераций.
- **ScheduleBrick** → wait + выполнение детей.
- **ExecuteForCloneNumberBrick** → repeat 1 с детьми.
- **RunAsSpriteBrick / RunOnUiThreadBrick** → inline-выполнение детей.
- **BroadcastBrick, BroadcastWaitBrick** → событие broadcast.
- **StopScriptBrick** → frame.ip = blocks.size (выход).

#### Поддерживаемые листовые брики (~60 типов)
- **Motion**: MoveNSteps, TurnLeft/Right, SetX/Y, ChangeX/Y, GoTo, PlaceAt, PointInDirection, SetSizeTo, GlideTo, IfOnEdgeBounce, ComeToFront, GoNStepsBack, SetRotationStyle, TouchDirection
- **Looks**: Show, Hide, Next/Previous Look, SetLook(byIndex), SetBackground(byIndex), SetSizeTo, ChangeSize, Set/Change Transparency/Brightness/Color, ClearEffects, Set/Change Width/Height
- **Sound**: PlaySound, PlaySoundAndWait, StopSound, StopAllSounds, SetVolume, ChangeVolume
- **Music**: PlayNoteForBeats, PlayDrumForBeats, SetInstrument, SetTempo, ChangeTempo, PauseForBeats
- **Pen**: PenDown/Up, SetPenSize, SetPenColor, Stamp, ClearBackground
- **Control**: Wait (с runtime-формулами), WaitUntil, Note (комментарий), FinishStage, ExitStage
- **Variables**: SetVariable, ChangeVariable (с runtime-формулами), ShowText, HideText
- **Web**: WebRequestBrick (GET), PostWebRequestBrick, PutWebRequestBrick, DeleteWebRequestBrick
- **Data**: WriteVariableOnDevice, ReadVariableFromDevice
- **Sensing**: ResetTimer

#### Рекурсивный вычислитель формул
Заменяет статический `extractFormulaValue`. Поддерживает:

| Тип | Значение | Поддержка |
|-----|----------|-----------|
| `NUMBER` | `value.toDouble()` | ✅ |
| `STRING` | строковая константа | ✅ |
| `OPERATOR` | PLUS, MINUS, MULT, DIVIDE, MOD, POW, EQUAL, NOT_EQUAL, SMALLER_THAN, GREATER_THAN, SMALLER_OR_EQUAL, GREATER_OR_EQUAL, LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT | ✅ все |
| `FUNCTION` | Математические: SIN, COS, TAN, LN, LOG, SQRT, ABS, ROUND, FLOOR, CEIL, PI, TRUE, FALSE, RAND, MAX, MIN, POWER, MOD, ARCSIN, ARCCOS, ARCTAN, ARCTAN2, EXP, ROUNDTO, CLAMP | ✅ |
| `FUNCTION` | Строковые: LENGTH, LETTER, SUBTEXT, UPPER, LOWER, JOIN, JOIN3, REVERSE | ✅ |
| `FUNCTION` | Системные: SCREEN_WIDTH, SCREEN_HEIGHT, DEVICE_NAME | ✅ |
| `SENSOR` | OBJECT_X, OBJECT_Y, OBJECT_SIZE, OBJECT_WIDTH, OBJECT_HEIGHT, OBJECT_DIRECTION, MOTION_DIRECTION, LOOK_DIRECTION, OBJECT_TRANSPARENCY, OBJECT_BRIGHTNESS, OBJECT_COLOR, OBJECT_LOOK_NUMBER, OBJECT_NUMBER_OF_LOOKS, OBJECT_X_VELOCITY, OBJECT_Y_VELOCITY, STAGE_WIDTH, STAGE_HEIGHT | ✅ |
| `SENSOR` | MOUSE_X, MOUSE_Y, MOUSE_DELTA_X, MOUSE_DELTA_Y, FINGER_X, FINGER_Y, FINGER_TOUCHED, NUMBER_CURRENT_TOUCHES, INDEX_CURRENT_TOUCH | ✅ |
| `SENSOR` | DATE_YEAR, DATE_MONTH, DATE_DAY, DATE_WEEKDAY, TIME_HOUR, TIME_MINUTE, TIME_SECOND | ✅ |
| `SENSOR` | X_ACCELERATION, Y_ACCELERATION, Z_ACCELERATION, COMPASS_DIRECTION, LATITUDE, LONGITUDE (заглушки = 0) | ✅ заглушки |
| `USER_VARIABLE` | lookup(name) в `variables[name]` (возвращает `Any`, по умолчанию `0f`) | ✅ |
| `USER_LIST` | возвращает "" | ✅ заглушка |
| `BRACKET` | вычисляет rightChild | ✅ |
| `COLLISION_FORMULA` | возвращает value как Double | ✅ |

### DesktopNetworkService (новый seam, 2026-07-13)
- `NetworkService` (интерфейс, `:core`): `httpGet(url)`, `httpPost(url, body)`, `httpPut(url, body)`, `httpDelete(url)` (4 метода).
- `NetworkServiceHolder` (объект, `:core`): точка инъекции.
- `DesktopNetworkService` (`:desktop-runtime`): реализация через `java.net.HttpURLConnection` с 10s таймаутами.
- Зарегистрирован в `DesktopStage.main()`.

## Desktop-реализации seam

| Модуль | Файл | Методы | Статус |
|--------|------|--------|--------|
| :core | RuntimeServices (7 методов) | DesktopRuntimeServices | ✅ все |
| :core | AudioService (18 методов) | DesktopAudioService | ✅ все |
| :core | MidiService (16 методов) | DesktopMidiService | ✅ все |
| :core | TextService (1 метод) | DesktopTextService | ✅ |
| :core | NotificationService (4 метода) | DesktopNotificationService | ✅ все |

## Исправления багов рантайма (2026-07-13)
- **DesktopInput**: `isMouseJustPressed` всегда был `false` из-за `wasMouseDown = isMouseDown` (текущее состояние вместо предыдущего) — переделано на двухкадровый трекинг через `previousMouseDown`.
- **DesktopInput**: `update()` вызывался дважды за кадр (в `DesktopScriptEngine.update()` + `DesktopScriptRunner.updateInput()`) — вызов перенесён ОДИН раз в `DesktopStageListener.render()`.
- **DesktopProjectManager**: имя спрайта (`"sprite$i"`) и `direction` не читались из `code.xml` — теперь читаются.
- **DesktopMidiService**: `playNote` вызывал `Thread.sleep` на render-потоке — вынесен в daemon-поток (isDaemon = true).
- **DesktopPhysicsWorld**: `syncSpritesFromPhysics` делал бессмысленный `body.setTransform` после чтения позиции — удалён.
- **DesktopStage**: `extractPayload` не очищал temp-директорию — `walkTopDown().forEach { it.deleteOnExit() }`.

Сборка: `./gradlew :core:compileKotlin --offline -q` — **BUILD SUCCESSFUL**.
Сборка desktop-runtime: `./gradlew :desktop-runtime:compileKotlin --offline -q` — **BUILD SUCCESSFUL**.

## Полный DesktopScriptEngine (2026-07-13)

Полная переработка `DesktopScriptEngine.kt` (960 строк):
- **Рекурсивный вычислитель формул** — `evaluateFormulaNode()` обрабатывает 10 типов (NUMBER, STRING, OPERATOR, FUNCTION, SENSOR, USER_VARIABLE, USER_LIST, BRACKET, COLLISION_FORMULA) с рекурсивным обходом leftChild/rightChild/additionalChildren.
- **15 операторов**: PLUS, MINUS, MULT, DIVIDE, MOD, POW, EQUAL, NOT_EQUAL, SMALLER_THAN, GREATER_THAN, SMALLER_OR_EQUAL, GREATER_OR_EQUAL, LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT.
- **30+ функций**: SIN, COS, TAN, SQRT, RAND, ABS, ROUND, FLOOR, CEIL, PI, TRUE, FALSE, MAX, MIN, POWER, MOD, ARCSIN, ARCCOS, ARCTAN, ARCTAN2, EXP, ROUNDTO, CLAMP, LENGTH, LETTER, SUBTEXT, UPPER, LOWER, JOIN, JOIN3, REVERSE, SCREEN_WIDTH, SCREEN_HEIGHT, DEVICE_NAME.
- **40+ сенсоров**: OBJECT_X/Y/SIZE/WIDTH/HEIGHT/TRANSPARENCY/BRIGHTNESS/COLOR/LOOK_NUMBER/VELOCITY, MOUSE_X/Y/DELTA, FINGER_X/Y/TOUCHED, дата/время, заглушки для акселерометра/компаса/GPS.
- **~60 типов бриков**: Motion (14), Looks (14), Sound (6), Music (6), Pen (6), Control (8), Variables (4), Web (4), Data (2), Sensing (1).
- **RuntimeFormula** — для SetVariable/ChangeVariable/Wait формулы с сенсорами/переменными вычисляются при каждом проходе, а не при парсинге.
- **6 контейнерных бриков**: Forever, Repeat, RepeatUntil, IfLogicBegin(+else), ForVariableFromTo, Schedule, ExecuteForCloneNumber, RunAsSprite, Broadcast.

## Исправления багов (2026-07-13, второй заход)

### 🔴 Critical (8)
1. **WaitUntilBrick**: BrickField `REPEAT_UNTIL_CONDITION` → `IF_CONDITION`. Теперь использует RuntimeFormula.
2. **RepeatUntilBrick**: Условие никогда не проверялось (бесконечный цикл). Переделано на новый `repeat_until` тип с `repeatRemaining = -2` и проверкой условия при каждом входе.
3. **SetWidth/ChangeWidth/SetHeight/ChangeHeight**: BrickField `WIDTH`/`HEIGHT` → `SIZE` (все четыре поля используют один BrickField `SIZE`).
4. **spriteIndex=0**: `extractFormulaValue/String` использовали хардкодный `spriteIndex=0` при парсинге. Исправлено через Kotlin-shadowing: локальные функции внутри `parseBrickListRecursive` захватывают `spriteIndex`.
5. **USER_DEFINED_BRICK_INPUT**: Добавлен case в `evaluateFormulaNode()` — возвращает `value.toDoubleOrNull() ?: value`.
6. **DeleteWebRequestBrick**: Использовал `http_get` вместо `http_delete`. Добавлены `httpDelete()` в NetworkService + DesktopNetworkService.
7. **PutWebRequestBrick**: Хардкодное тело `__put_body`. Исправлено: парсит `BODY` формулу, использует `http_put`.
8. **TouchDirectionBrick**: Хардкодный угол 0°. Теперь вычисляет `atan2(touchY - spriteY, touchX - spriteX)` в executeMotion.

### 🟡 Important (7)
- **SetRotationStyleBrick**: Парсит `selection` из XML-элемента (был хардкод 0).
- **SetInstrumentBrick**: Парсит `instrumentSelection` из XML, маппит через `INSTRUMENT_PROGRAM_MAP`.
- **PlayDrumForBeatsBrick**: Поле `BEATS_TO_PLAY_NOTE` → `PLAY_DRUM`. Парсит `drumSelection` из XML через `DRUM_PROGRAM_MAP`.
- **ChangeVolumeByNBrick**: Теперь читает текущую громкость через `AudioService.getVolume()`, добавляет дельту, устанавливает новую.
- **TIMER sensor**: Добавлен счётчик `timerSeconds`, обновляется каждый кадр. ResetTimerBrick → `timerSeconds = 0`.
- **LAST_FINGER_INDEX sensor**: Был на одной строке с FINGER_X (возвращал fingerX). Теперь возвращает 0 если есть касание, -1 если нет.
- **USER_LANGUAGE / SYSTEM_LANGUAGE**: Возвращали `1.0` (проверка существования property). Теперь возвращают строку языка. Тип `evaluateSensor` изменён на `Any?`.

### 🟢 Medium (4)
- **SetLookBrick**: Парсит `<look name="...">` из XML, маппит через `sprite.looks.indexOfFirst { it.name == name }`.
- **GoToBrick**: Парсит `spinnerSelection` (80=touch, 81=random, 82=other sprite), создаёт `goto_touch/goto_random/goto_sprite` блоки.
- **ForVariableFromToBrick**: Добавлен синтетический `inc_var` блок в конец детей цикла.
- **RunAsSpriteBrick**: Добавлены маркеры `run_as_start`/`run_as_end` с защитой от рекурсии (макс. 10 уровней).

### 🌐 NetworkService
- Добавлены `httpPut(url, body)` и `httpDelete(url)` в интерфейс и `DesktopNetworkService`.

### 📊 Статистика DesktopScriptEngine
- Размер: ~1780 строк.
- Типов блоков уже портировано: ~70 (все основные категории).
- Операторов: 15.
- Функций: 41+.
- Сенсоров: 55+.
- RuntimeFormula: используется в Wait, WaitUntil, RepeatUntil, SetVariable, ChangeVariable.
- Сборка: `./gradlew :core:compileKotlin :desktop-runtime:compileKotlin --offline -q` — **BUILD SUCCESSFUL**.

---

## Инвентаризация бриков для портирования на Windows

Всего в Android: **~390+ brick-классов**. Уже портировано: **~70**.

Ниже — анализ оставшихся ~320 бриков по категориям с указанием портируемости.

### ✅ Легко портируются (нет Android-зависимостей, только Formula + Action)

#### 1. User List bricks — 8 шт.
`AddItemToUserListBrick`, `DeleteItemOfUserListBrick`, `InsertItemIntoUserListBrick`, `ReplaceItemInUserListBrick`, `ClearUserListBrick`, `SplitBrick`, `StoreCSVIntoUserListBrick`, `RegexBrick`
- **Что нужно**: парсер для `UserListBrick` (считывает variable + formula), поддержка списковых операций в engine (usersList: Map<name, List>)
- **Оценка**: 1 день

#### 2. Control bricks — 8 шт.  
`ForItemInUserListBrick`, `TryCatchFinallyBrick`, `SwitchBeginBrick`+`SwitchCaseBrick`, `UserDefinedBrick`+`UserDefinedReceiverBrick`, `WaitTillIdleBrick`, `CloneObjectBrick`+`DeleteThisCloneBrick`, `IntervalRepeatBrick`
- **Что нужно**: парсеры + runtime-обработчики. Для TryCatch — нужна обработка ошибок
- **Оценка**: 2-3 дня

#### 3. Pen drawing bricks — 10 шт.
`DrawLineBrick`, `DrawCircleBrick`, `DrawRectBrick`, `DrawTextBrick`, `FillCircleBrick`, `FillRectBrick`, `FillPolygonBrick`, `SetCornerRadiusBrick`, `SetBorderWidthBrick`, `SetBorderColorBrick`
- **Что нужно**: команды `draw_line`, `draw_circle`, `draw_rect`, `draw_text`, `fill_*` в executePen
- **Движок**: libGDX ShapeRenderer или Pixmap
- **Оценка**: 1 день

#### 4. Sound bricks — 15 шт.
`PlaySoundAtBrick`, `StopSoundBrick2`, `SetSoundVolumeBrick`, `SetGlobalSoundVolumeBrick`, `PrepareSoundBrick`, `PlayPreparedSoundBrick`, `SetPanBrick`, `PlayToneBrick`, `PrepareMusicAs3DSoundBrick`, `Set3DSoundPositionBrick`, `EqualizerSetBandBrick`, `SetStopSoundsBrick`, `PlaySoundAtPositionBrick`, `SetSoundInstanceVolumeBrick`, `SetSoundInstancePitchBrick`
- **Что нужно**: расширение AudioService (pan, 3D position, EQ). Большинство — FormulaBrick
- **Оценка**: 2-3 дня

#### 5. Web/Network bricks — 20+ шт.
`HeadWebRequestBrick`, `PatchWebRequestBrick`, `OptionsWebRequestBrick`, `WebSocketConnectBrick`(+Send/Receive/Close), `CreateWebUrlBrick`, `CreateWebFileBrick`, `DownloadFileBrick`, `DownloadToPathBrick`, `UploadFileBrick`, `PingBrick`, `SetDnsBrick`, `StartServerBrick`(+Stop/Send), `ConnectServerBrick`, `ListenServerBrick`
- **Что нужно**: `httpHead()`, `httpPatch()`, `httpOptions()` в NetworkService; WebSocket через `java.net.http.WebSocket` (Java 11+); HTTP-сервер через `com.sun.net.httpserver`
- **Оценка**: 3-4 дня

#### 6. File System bricks — 20 шт.
`DeleteFilesBrick`, `MoveFilesBrick`, `MoveDownloadsBrick`, `OpenFileBrick`, `OpenFilesBrick`, `ReadFromFilesBrick`, `WriteToFilesBrick`, `ZipBrick`, `UnzipBrick`, `ExtractFileBrick`, `GetZipFileNamesBrick`, `CopyProjectFileBrick`, `ReadVariableFromFileBrick`, `WriteVariableToFileBrick`, `SaveToInternalStorageBrick`, `LoadFromInternalStorageBrick`, `ExportProjectFileBrick`
- **Что нужно**: java.io/java.nio — всё уже есть в JDK
- **Оценка**: 1-2 дня

#### 7. Text/Speech/Bubble bricks — 12 шт.
`ThinkBubbleBrick`, `ThinkForBubbleBrick`, `SayBubbleBrick`, `SayForBubbleBrick`, `ShowTextColorSizeAlignmentBrick`, `ShowTextFontBrick`, `ShowTextRotationBrick`, `ShowText3Brick`, `HideText3Brick`, `CreateTextFieldBrick`, `SetTextBrick`, `SetFontBrick`, `ShowDialogBrick`
- **Что нужно**: рендеринг текста на экране через libGDX BitmapFont; диалоги — JOptionPane
- **Оценка**: 1-2 дня

#### 8. Physics (Box2D) bricks — 25+ шт.
`SetGravityBrick`, `SetBounceBrick`, `SetFrictionBrick`, `SetMassBrick`, `SetDampingBrick`, `SetRestitutionBrick`, `SetPhysicsObjectTypeBrick`, `SetHitboxBrick`, `ApplyForceBrick`, `ApplyImpulseBrick`, `ApplyTorqueBrick`, `ApplyAngularImpulseBrick`, все Joint-брики (8 шт.), `CastRayBrick`, `PerformRayCastBrick`, `HasPathBrick`, `SetPhysicsStateBrick`
- **Что нужно**: расширение DesktopPhysicsWorld. libGDX Box2D уже подключён
- **Оценка**: 2-3 дня

#### 9. Camera/3D Camera bricks — 20+ шт.
Все SetCamera*, RotateCamera*, PinToCamera*, AttachToCamera*, SetViewPosition*, SetBufferCamera* брики
- **Что нужно**: реализация через Desktop3DManager (если есть) — большинство FormulaBrick
- **Оценка**: 1-2 дня

#### 10. Event triggers — 10+ шт.
`WhenTouchDownBrick`, `WhenClonedBrick`, `WhenConditionBrick`, `WhenBackgroundChangesBrick`, `WhenBounceOffBrick`, `WhenBackPressedBrick`(→Escape), `WhenMouseButtonClickedBrick`, `WhenMouseWheelScrolledBrick`, `WhenGamepadButtonBrick`, `KeyEventBrick`, `MouseEventBrick`
- **Что нужно**: новые типы ScriptEvent. Большинство — ScriptBrickBaseType
- **Оценка**: 2 дня

#### 11. Variable bricks — 8 шт.
`CreateVarBrick`, `DeleteVarBrick`, `DeleteVarsBrick`, `CreateFloatBrick`, `DeleteFloatBrick`, `SetVariableEasingBrick`, `ReadListFromDeviceBrick`, `WriteListOnDeviceBrick`
- **Что нужно**: управление переменными (создание/удаление). Списки сериализовать в JSON
- **Оценка**: 1 день

#### 12. Data bricks — 4 шт.
`ReadVariableFromDeviceBrick`, `WriteVariableOnDeviceBrick`, `ReadListFromDeviceBrick`, `WriteListOnDeviceBrick`
- **Что нужно**: чтение/запись в файлы (уже есть через File I/O)
- **Оценка**: 0.5 дня

### ⚠️ Портятся с минимальными изменениями

#### 13. Ask/Speech/AI bricks — 6 шт.
`AskBrick` (консольный ввод), `SpeakBrick` (FreeTTS), `SpeakAndWaitBrick`, `CopyTextBrick` (Clipboard), `SetAIBrick`, `SetGeminiKeyBrick`
- **Что нужно**: FreeTTS (перед TTS engine), System.in для Ask, AWT Clipboard для CopyText
- **Оценка**: 1 день

#### 14. Device bricks — 8 шт.
`VibrationBrick` (заглушка), `KeepScreenOnBrick`, `KeepScreenOffBrick`, `ScreenBrightnessBrick` (заглушка), `LockMouseBrick`, `UnlockMouseBrick`, `OrientationBrick`, `ScreenShotBrick`
- **Что нужно**: LockMouse/UnlockMouse — уже есть! ScreenShot — libGDX ScreenUtils; остальные — заглушки
- **Оценка**: 0.5 дня

#### 15. Notification bricks — 6 шт.
`SendNotificationBrick`, `ShowScheduledNotificationBrick`, `PrepareNotificationBrick`, `NotificationActionBrick`, `RemoveNotificationBrick`, `EnableBackgroundBrick`
- **Что нужно**: DesktopNotificationService (уже есть). Большинство уже реализовано в Action-классах
- **Оценка**: 0.5 дня (только парсинг)

### 🔴 Не портятся (Android-only)
- **Camera/Photo** (FlashBrick, CameraBrick, ChooseCameraBrick, PhotoBrick, CameraSettingsBrick) — аппаратная камера
- **NFC** (WhenNfcBrick, SetNfcTagBrick) — NFC-чип
- **Bluetooth/BLE** — если есть (не найдены в бриках)
- **Audio Recording** (StartRecordingBrick, StopRecordingBrick, ListenMicroBrick) — микрофон через Java? javax.sound.sampled поддерживается, но не гарантируется

---

## Итого: что портировать в первую очередь

| Приоритет | Категория | Бриков | Сложность |
|-----------|-----------|--------|-----------|
| 🥇 | **User List** | 8 | Лёгкая |
| 🥇 | **Pen Drawing** | 10 | Лёгкая |
| 🥇 | **File I/O** | 20 | Лёгкая |
| 🥇 | **Текст/Баблики** | 12 | Средняя |
| 🥇 | **Event Triggers** | 10 | Средняя |
| 🥇 | **Data (device read/write)** | 4 | Лёгкая |
| 🥈 | **Control (Switch, Try, Clone)** | 8 | Средняя |
| 🥈 | **Sound (pan, tone, 3D)** | 15 | Средняя |
| 🥈 | **Web (WebSocket, Server)** | 20 | Сложная |
| 🥈 | **Physics (joints, forces)** | 25 | Средняя |
| 🥉 | **Camera 3D** | 20 | Средняя |
| 🥉 | **Variables (create/delete)** | 8 | Лёгкая |
| 🥉 | **Ask/Speak/AI** | 6 | Средняя |
| 🥉 | **Notifications** | 6 | Лёгкая |
| 🥉 | **Device** | 8 | Лёгкая |

**Итого портируемых: ~170 шт.** (из ~390 Android)
**Уже портировано: ~70 шт.**
**Осталось: ~100 шт.** ценных для портирования (исключая 3D-специфичные и Android-only).

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
- **Локатор шаблона**: catroid/.../apkbuildV3/TemplateManagerV3.kt — prepareBaseApk берёт template_runtime.apk из assets, fallback на собственный APK (applicationInfo.sourceDir); бросает IllegalStateException с обеими причинами отказа (нет в assets / нет места / невалидный ZIP / нет sourceDir) вместо null. V3ApkAssembler.assemble пробрасывает исключение, поэтому ApkBuilderV3Engine показывает реальную причину, а не обобщённое «проверьте template_runtime.apk». Пайплайн inject→patch→sign проверен headless на обеих базах (runtime-шаблон 188 МБ и self-APK 624 МБ) — работает; значит сбой на устройстве = locateBaseApk вернул null (нет файла в установленном APK либо не хватает места в cacheDir).
- **Подпись (исправлено 2026-07)**: `V3ApkAssembler.doSign` НЕ должен ссылаться на провайдер по имени `BouncyCastleProvider.PROVIDER_NAME` (= "BC") — на Android под именем "BC" уже зарегистрирован урезанный платформенный провайдер (Conscrypt), который не реализует BC content-signer, отсюда `NoSuchAlgorithmException: SHA256WithRSA for provider BC`. Используется ЭКЗЕМПЛЯР `BouncyCastleProvider()` (`.setProvider(bc)`) и генерация ключа `KeyPairGenerator.getInstance("RSA", bc)`. На JVM-тесте "BC" — полный BC, поэтому тест проходил, а устройство падало.
- **СТАЛЫЙ template_runtime.apk (исправлено 2026-07)**: закоммиченный `catroid/src/main/assets/template_runtime.apk` был СТАРЫМ (собран до появления V3-runtime) и НЕ содержал классов `RuntimeLoaderActivityV3`/`ProjectLoaderV3`. Игра собиралась и ставилась, но падала сразу при запуске (ClassNotFoundException на launcher). Перегенерирован через `./gradlew copyTemplateApk` (собирает `assembleRuntimeTemplate` = flavor `runtime` + buildType `template`, minify с `proguard-runtime.pro`, который держит `org.catrobat.catroid.apkbuildV3.**` и `apkbuildV3.runtime.**`). Результат 171 МБ и содержит V3-runtime (проверено dex-сканом). `copyTemplateApk` падает на задаче `uploadCrashlyticsMappingFileRuntimeTemplate` (нет Firebase appId для runtime-флейвора) — обход: `./gradlew copyTemplateApk -x uploadCrashlyticsMappingFileRuntimeTemplate`. Рекомендация: перегенерировать template при любом изменении V3-runtime; желательно зашить `copyTemplateApk` в mergeAssets редактора, чтобы ассет не протухал.

---

## Desktop EXE — тормоза запуска (2026-07)

**Симптом**: EXE не открывается / проект «не оживает» 5–20 мин на маленьких проектах.
**Причина**: единственный неограниченный сетевой вызов — `askGeminiApi` в
`DesktopScriptEngine.kt` (открывал `HttpURLConnection` без `connectTimeout`/`readTimeout`).
При стартовом блоке **Ask Gemini** и недоступности сети до Google поток висит на
TCP/DNS-таймауте ОС — ровно 5–20 мин. Все остальные сетевые пути уже ограничены:
`DesktopNetworkService` (Web Request) = 10с, `firebaseRequest` = 5с.
**Фикс**: `askGeminiApi` теперь `connectTimeout = readTimeout = 15_000`.
**Пересборка**: EXE собран с `dontWrapJar=true` (71 КБ — лаунчер), поэтому достаточно
пересобрать `player.jar` (`./gradlew :desktop-runtime:jar --offline`) и положить рядом
с `NeoCatroid.exe` (корень `desktop-runtime/`). Перевыпаковывать EXE НЕ нужно.

**Если после фикса всё ещё 5–20 мин и окно вообще не появляется** — причина НЕ в сети,
а в старте JVM/GLFW (бандл-JRE `jlink --add-modules ALL-MODULE-PATH` очень большой →
медленная инициализация + первичное сканирование Defender). Тогда: собрать JRE уже с
курируемым списком модулей (java.base, java.xml, java.desktop, java.logging, jdk.httpserver)
вместо ALL-MODULE-PATH и/или проверить GLFW-инит на машине пользователя.
**ВАЖНО**: `build_exe.bat` на шаге staging удаляет все папки в корне `desktop-runtime`,
кроме `icon`/`jre` (в т.ч. `src`!). Не запускать повторно без восстановления `src`
(`git checkout -- desktop-runtime/src`) и полного `launch4j`.

---

## Массовое портирование блоков Android → Desktop (2026-07-19)

### Цель
Портировать **все 80 портабельных блоков**, отсутствующих в DesktopScriptEngine,
чтобы движок поддерживал максимум Android-бриков (кроме Android-only и низкоприоритетных).

### Методология
1. **Инвентаризация**: `ls content/bricks/*.java` = 643 файла. `grep` в `parseBrickLeaf` DesktopScriptEngine = 402 типа парсится. 255 не портировано.
2. **Классификация 255 не портированных**:
   - 89 Android-only (AdMob, Drone, NFC, Lego, Arduino, Raspi, Phiro, Voxel)
   - 86 нишевых/низкоприоритетных (ML/PyTorch, Stitch, VM/Chip8/JS/Lua, APK build, Fabric math)
   - **80 портабельных** — реализованы
3. **Анализ event-триггеров**: `mapScriptTypeToEvent` уже обрабатывает все event-скрипты (WhenCondition, WhenBounceOff, WhenBackPressed, WhenAppMinimized, WhenBackgroundChanges, WhenNotification\*) — новых бриков-триггеров не требуется.

### Что сделано (~420 строк добавлено в DesktopScriptEngine.kt)

#### Парсинг (parseBrickLeaf, ~402 строки)
Добавлены все 80 типов бриков в парсер:
- **Physics joints**: `create_gear_joint`, `create_pulley_joint`, `create_point_joint`, `add_hinge`, `set_hinge_motor`, `set_hitbox_rect`
- **Physics 3D**: `set_3d_bounce`, `set_3d_friction`, `set_3d_mass`, `set_3d_damping`, `set_3d_gravity`, `set_3d_velocity`, `set_3d_angular_vel`, `set_3d_type`, `set_3d_rotation`
- **3D Rendering/scene**: `set_ambient_light`, `set_point_light`, `set_directional_light`, `set_spot_light`, `set_skybox`, `set_fog`, `set_shadows`, `set_shader_uniform`, `set_material_color`, `set_material_roughness`, `set_material_metallic`, `set_fog_color`, `set_emissive_color`, `set_texture_tiling`, `set_post_processing`, `set_pbr_params`, `set_particle_emission`, `set_anisotropic_filter`, `set_ccd_enabled`, `spawn_invisible`, `pitch_only`, `promote_light_to`
- **Web extras**: `http_delete`, `http_set`, `http_eval`, `ws_connect_to`, `ws_set_ip`, `ws_get_url`, `ws_send`, `ws_receive`, `ws_close`
- **NeoScript**: `assign_scripts`, `import_script`, `create_object`
- **Security**: `secure_read`, `secure_save`
- **Camera/View**: `object_look_at`, `visual_placement`, `keyframe_animation`, `create_gl_view`, `attach_so`, `load_native_module`
- **Misc**: `create_dialog`, `big_ask`, `hide_status_bar`, `toggle_display`, `set_orientation`, `set_save_scenes`, `apply_shader_to_image`, `set_preloading`, `set_callback`, `scene_preloaded`, `user_defined_definition`, `set_stop_sounds_v2`

#### Execution handlers (добавлены в существующие execute-функции)

**executePhysics** — 7 types:
- `create_gear_joint`, `create_pulley_joint`, `create_point_joint` — Box2D joint creation via `physicsWorld?.getJoint()`
- `add_hinge` — hinge joint on sprite
- `set_hinge_motor` — enable/disable hinge motor
- `set_hitbox_rect` — resize fixture
- `set_hitbox` — 3D hitbox resize (same handler)

**executeLooks** — 25 stubs для 3D освещения/рендеринга

**executeControl** — 9 stubs:
- NeoScript: `assign_scripts`, `import_script`, `create_object`
- Misc: `create_dialog`, `hide_status_bar`, `toggle_display`, `set_orientation`, `set_save_scenes`, `set_preloading`, `scene_preloaded`, `user_defined_definition`

**executeWeb** — 5 types:
- `http_delete` (DELETE request), `http_set` (PUT), `http_eval` (PATCH)
- `ws_set_ip`, `ws_get_url`, `ws_connect_to` — WebSocket stubs

**executeVariable** — 2 types:
- `secure_read`, `secure_save` — stub (no hardware keystore on desktop)

**executeCamera** — 6 stubs:
- `object_look_at`, `visual_placement`, `keyframe_animation`, `create_gl_view`, `attach_so`, `load_native_module`

**executeData** — 1 type:
- `apply_shader_to_image` — stub

### Статистика
- **Android-бриков всего**: 643
- **Портировано в Desktop**: 402 → теперь **482** (80 новых)
- **Не портировано (Android-only)**: 89
- **Не портировано (нишевые)**: 72 (86 минус 14 портабельных, которые уже входили в инвентаризацию)
- DesktopScriptEngine.kt: ~8465 строк (было ~8040, +425)
- Все execute-функции имеют handlers для всех 80 новых типов (stub или real).
- **Исправлено**: `getJointByName` → `getJoint` (метод называется `getJoint` в DesktopPhysicsWorld).

### Next Steps (planned but not done)
1. **Сборка**: `./gradlew :core:compileKotlin :desktop-runtime:compileKotlin --offline -q` — проверить ошибки
2. **Real WebSocket**: через `java.net.http.WebSocket` (Java 11+)
3. **Тестирование**: открыть тестовый .catroid проект с новыми бриками

---

## Physics collision fix — Desktop + Android (2026-07-19)

### Desktop (DesktopPhysicsWorld.kt)
- **Баг**: `createBodyForSprite()` создавал ВСЕ тела с `CircleShape`. Статические стены должны быть `PolygonShape` (прямоугольник), иначе объекты проходят сквозь при наклонном ударе.
- **Фикс**: static → `PolygonShape.setAsBox(halfWidth, halfHeight)`, dynamic → `CircleShape` + `setBullet(true)` (CCD).
- **setBodyType**: при смене на Static пересоздаёт фикстуру в Polygon; при смене на Dynamic — Circle + bullet.
- **setHitbox**: больше не no-op — вызывает `physicsWorld.setHitbox()`.
- **Добавлено**: `customHitboxSprites` (Set), `clearAllBodies()`.
- **Тесты**: `DesktopPhysicsWorldCollisionTest.kt` — 36 тестов, все пройдены.

### Android (PhysicsObject.java)
- **Баг**: `setType(Type.FIXED)` и `setType(Type.NONE)` использовали `BodyType.KinematicBody`. FIXED-объекты (стены, пол) должны быть `StaticBody` — truly immovable. KinematicBody может проталкивать динамические тела иначе, чем StaticBody, что приводит к некорректной коллизии при наклонных ударах.
- **Фикс**:
  - `FIXED` → `BodyType.StaticBody` + `gravityScale(0.0f)`
  - `NONE` → `BodyType.StaticBody` + `gravityScale(0.0f)`
  - `body.setBullet(false)` при выходе из DYNAMIC (CCD не нужен на статике)
- **PhysicsBoundaryBox**: уже `StaticBody` + `PolygonShape` — трогать не пришлось.
- **Тесты**: `PhysicsObjectTest` — обновлён `testSetType()` (KinematicBody→StaticBody), добавлен `testSetTypeBulletTransitions()` (7 переходов bullet=true/false).