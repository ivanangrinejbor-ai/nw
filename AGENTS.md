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

## Build & dependencies
- XStream 1.4.11.1 → 1.4.20 (`catroid/build.gradle`)
- Coroutines unified to 1.7.3
- material:1.2.1 → material:1.13.0, removed resolutionStrategy force
- Gradle: `-Xmx6g` → `-Xmx4g`
- Removed duplicate `apksig:7.0.0` dependency

## Безопасность
- **UnzipAction.kt**: canonical path check (zip-slip prevention)
- **PutFileIntoPathAction, DeleteFolderByPathAction, CreateFolderByPathAction, CopyProjectFileToPathAction**: canonical path validation
- **AskGemini2Action.kt**: removed `hostnameVerifier { _, _ -> true }`, added timeouts, replaced raw JSON string with `JSONObject`
- **WriteVariableToFileAction.kt**: replaced `System.getProperty("user.home")` with `Environment.getExternalStoragePublicDirectory`

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
