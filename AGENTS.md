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
