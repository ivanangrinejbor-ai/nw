# NeoCatroid — гайд для разработчиков

Форк Catrobat/Catroid. Конструктор приложений с визуальным программированием через блоки.

Быстрый старт: `./gradlew copyTemplateApk` для обновления APK-темплейта.

---

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

---

# Исправления безопасности и багов (2026-07)

## 🔴 Критические проблемы безопасности
- **Keystore удалён из VCS**: `catroid/keystore` → `git rm --cached`, добавлен в `.gitignore` (настоятельно рекомендуется отозвать ключ)
- **GitHub OAuth Client ID**: `SettingsFragment.java` — hardcoded `"Ov23liKoq3h0cTgAbVYA"` заменён на `BuildConfig.GITHUB_CLIENT_ID` с fallback
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
- XStream 1.4.11.1 → 1.4.20
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
