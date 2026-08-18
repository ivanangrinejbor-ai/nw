# NeoCatroid вЂ” РіР°Р№Рґ РґР»СЏ СЂР°Р·СЂР°Р±РѕС‚С‡РёРєРѕРІ

Р‘С‹СЃС‚СЂС‹Р№ СЃС‚Р°СЂС‚: `./gradlew copyTemplateApk` РґР»СЏ РѕР±РЅРѕРІР»РµРЅРёСЏ APK-С‚РµРјРїР»РµР№С‚Р°.



# РЎС‚СЂСѓРєС‚СѓСЂР° РїСЂРѕРµРєС‚Р°

`res/values` вЂ” РіР»РѕР±Р°Р»СЊРЅС‹Рµ Р·РЅР°С‡РµРЅРёСЏ: С†РІРµС‚Р°, СЃС‚СЂРѕРєРё.

`res/values/strings.xml` вЂ” Р°РЅРіР»РёР№СЃРєРёР№ (РѕР±СЏР·Р°С‚РµР»СЊРЅРѕ РѕР±РЅРѕРІР»СЏС‚СЊ). `values-ru/` вЂ” СЂСѓСЃСЃРєРёР№.

`res/layout` вЂ” РІСЃРµ layout'С‹ (xml Р±Р»РѕРєРѕРІ, РјРµРЅСЋ, РґРёР°Р»РѕРіРѕРІ).

`assets` вЂ” Р°СЃСЃРµС‚С‹ (Р·РµР»С‘РЅРѕРµ = С‚РµСЃС‚РѕРІРѕРµ, РЅРµ РІРєР»СЋС‡Р°РµС‚СЃСЏ РІ СЂРµР»РёР·).

`kotlin+java/org.catrobat.catroid/` вЂ” РѕСЃРЅРѕРІРЅС‹Рµ .java/.kt С„Р°Р№Р»С‹.

`content/` вЂ” РєРѕРЅС‚РµРЅС‚: Р±Р»РѕРєРё, РґРµР№СЃС‚РІРёСЏ, РєРѕРЅС‚СЂРѕР»Р»РµСЂС‹ (Gemini, Firebase, РјРёРєСЂРѕС„РѕРЅ).
`content/actions/` вЂ” РєРѕРґ РґРµР№СЃС‚РІРёР№ Р±Р»РѕРєРѕРІ.
`content/bricks/` вЂ” РєР»Р°СЃСЃС‹ Р±Р»РѕРєРѕРІ (СЃРѕРµРґРёРЅСЏСЋС‚ action + layout).
`content/ActionFactory.java` вЂ” С„Р°Р±СЂРёРєР°: СЃРѕР·РґР°С‘С‚ Action СЃ РїР°СЂР°РјРµС‚СЂР°РјРё.
`content/GlobalManager.kt` вЂ” РіР»РѕР±Р°Р»СЊРЅС‹Рµ С„Р»Р°РіРё (stopSounds, saveScenes).

`raptor/` вЂ” 3D (ThreeDManager, SceneManager, РєРѕРјРїРѕРЅРµРЅС‚С‹).
`fast2d/` вЂ” 2D СЂРµРЅРґРµСЂ (ECS-based).
`editor/` вЂ” 3D СЂРµРґР°РєС‚РѕСЂ.
`utils/lunoscript/` вЂ” LunoScript (Interpreter, Parser, Lexer).
`stage/` вЂ” StageActivity, СЂРµРЅРґРµСЂ-Р»СѓРї, СЃРѕР±С‹С‚РёСЏ.
`formulaeditor/` вЂ” FormulaElement, Functions, РїР°СЂСЃРµСЂ С„РѕСЂРјСѓР».
`ui/` вЂ” Activity, Fragment'С‹, Р°РґР°РїС‚РµСЂС‹, РґРёР°Р»РѕРіРё.

---

# Р“Р°Р№Рґ: РґРѕР±Р°РІР»РµРЅРёРµ Р±Р»РѕРєР°

### 1. Action (Kotlin)

```kotlin
class MyAction : TemporalAction() {
    var scope: Scope? = null
    var myParam: Formula? = null

    override fun update(percent: Float) {
        val valStr = myParam?.interpretString(scope) ?: ""
        // Р»РѕРіРёРєР° Р±Р»РѕРєР° (РІС‹РїРѕР»РЅСЏРµС‚СЃСЏ 1 СЂР°Р·)
    }
}
```

### 2. РџРµСЂРµРІРѕРґС‹ (values/strings.xml + values-ru/strings.xml)

```xml
<string formatted="false" name="my_block_label">Do something</string>
```

РўРµРєСЃС‚ РІ Р±Р»РѕРєРµ вЂ” РјР°РєСЃРёРјР°Р»СЊРЅРѕ РєРѕСЂРѕС‚РєРёР№.

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

Р Р°Р·РјРµСЂ: 1 РїР°СЂР°РјРµС‚СЂ = Small, 2-3 = Medium, 4+ = Big. РљР°Р¶РґС‹Р№ РїР°СЂР°РјРµС‚СЂ РЅР° РЅРѕРІРѕР№ СЃС‚СЂРѕРєРµ.

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

РћР±СЏР·Р°С‚РµР»СЊРЅРѕ: РєРѕРЅСЃС‚СЂСѓРєС‚РѕСЂ РёР· РїСЂРѕСЃС‚С‹С… Р·РЅР°С‡РµРЅРёР№ (String/double), РЅРµ С‚РѕР»СЊРєРѕ РёР· Formula.

---

# Р“Р°Р№Рґ: РґРѕР±Р°РІР»РµРЅРёРµ С„РѕСЂРјСѓР»С‹

1. РџРµСЂРµРІРѕРґС‹: `formula_my_func` + `formula_my_func_param`
2. `Functions.java` вЂ” РґРѕР±Р°РІРёС‚СЊ `MY_FUNC` РІ enum + РІ `TEXT` СЃРµС‚
3. `InternFormulaAdapter` вЂ” case РІ switch
4. `InternToExternGenerator` вЂ” Р·Р°РїРёСЃСЊ РІ `INTERN_EXTERN_LANGUAGE_CONVERTER_MAP`
5. `CategoryListFragment` вЂ” РІ СЃРѕРѕС‚РІРµС‚СЃС‚РІСѓСЋС‰РёР№ СЃРїРёСЃРѕРє FUNCTIONS/PARAMS
6. `FormulaElement.java` вЂ” РѕСЃРЅРѕРІРЅРѕР№ case РІ switch

---


---

# Р”РѕР±Р°РІР»РµРЅРЅС‹Рµ Р±Р»РѕРєРё (2026-07)

## 1. File category вЂ” Р±Р»РѕРєРё РґР»СЏ РїР°РїРѕРє
- **CreateFolderBrick** вЂ” СЃРѕР·РґР°С‚СЊ РїР°РїРєСѓ (СѓР¶Рµ СЃСѓС‰РµСЃС‚РІРѕРІР°Р», Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅ РІ File)
- **DeleteFolderBrick** вЂ” СѓРґР°Р»РёС‚СЊ РїР°РїРєСѓ (СѓР¶Рµ СЃСѓС‰РµСЃС‚РІРѕРІР°Р», Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅ)
- **CreateFolderByPathBrick** вЂ” СЃРѕР·РґР°С‚СЊ РїРѕ РїСѓС‚Рё (СѓР¶Рµ СЃСѓС‰РµСЃС‚РІРѕРІР°Р», Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅ)
- **DeleteFolderByPathBrick** вЂ” СѓРґР°Р»РёС‚СЊ РїРѕ РїСѓС‚Рё (СѓР¶Рµ СЃСѓС‰РµСЃС‚РІРѕРІР°Р», Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅ)
- **CopyProjectFileToFolderBrick** вЂ” РєРѕРїРёСЂРѕРІР°С‚СЊ РІ РїР°РїРєСѓ (СѓР¶Рµ СЃСѓС‰РµСЃС‚РІРѕРІР°Р», Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅ)
- **CopyProjectFileToPathBrick** вЂ” РєРѕРїРёСЂРѕРІР°С‚СЊ РїРѕ РїСѓС‚Рё (СѓР¶Рµ СЃСѓС‰РµСЃС‚РІРѕРІР°Р», Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅ)
- **PutFileIntoFolderBrick** вЂ” РїРѕР»РѕР¶РёС‚СЊ С„Р°Р№Р» РІ РїР°РїРєСѓ (NEW)
- **PutFileIntoPathBrick** вЂ” РїРѕР»РѕР¶РёС‚СЊ С„Р°Р№Р» РїРѕ РїСѓС‚Рё (NEW)

## 2. Device category вЂ” СѓРІРµРґРѕРјР»РµРЅРёСЏ
- **SendNotificationBrick** вЂ” РѕС‚РїСЂР°РІР»СЏРµС‚ СѓРІРµРґРѕРјР»РµРЅРёРµ РїРѕ ID (РёСЃРїРѕР»СЊР·СѓРµС‚ NOTIFICATION_ID)
- **ShowScheduledNotificationBrick** вЂ” РѕС‚РїСЂР°РІР»СЏРµС‚ РѕС‚Р»РѕР¶РµРЅРЅРѕРµ СѓРІРµРґРѕРјР»РµРЅРёРµ СЃ Р·Р°РіРѕР»РѕРІРєРѕРј, С‚РµРєСЃС‚РѕРј Рё РІСЂРµРјРµРЅРµРј
- **NotificationActionBrick** вЂ” РґРµР№СЃС‚РІРёРµ РїСЂРё РЅР°Р¶Р°С‚РёРё РЅР° СѓРІРµРґРѕРјР»РµРЅРёРµ
- **PrepareNotificationBrick** вЂ” РїРѕРґРіРѕС‚РѕРІРєР° СѓРІРµРґРѕРјР»РµРЅРёСЏ СЃ Р·Р°РіРѕР»РѕРІРєРѕРј, С‚РµРєСЃС‚РѕРј, РІР°Р¶РЅРѕСЃС‚СЊСЋ Рё pin

## 3. Motion category вЂ” РЅР°РїСЂР°РІР»РµРЅРёРµ РЅР° РєР°СЃР°РЅРёРµ
- **TouchDirectionBrick** вЂ” Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё РІС‹С‡РёСЃР»СЏРµС‚ СѓРіРѕР» РѕС‚ СЃРїСЂР°Р№С‚Р° Рє С‚РѕС‡РєРµ РєР°СЃР°РЅРёСЋ

## 3a. Physics category вЂ” СЂРµРіРґРѕР»Р»
- **SetRagdollBrick** вЂ” РІРєР»СЋС‡Р°РµС‚/РІС‹РєР»СЋС‡Р°РµС‚ СЂРµР¶РёРј СЂРµРіРґРѕР»Р»Р° (1 = РІРєР», 0 = РІС‹РєР», **2 = СЂРµРіРґРѕР»Р» СЃРѕ СЃР»РµРґРѕРІР°РЅРёРµРј**)
- **Р¤РѕСЂРјСѓР»Р° `Sprite_ragdolled`** вЂ” РІРѕР·РІСЂР°С‰Р°РµС‚ 1 РµСЃР»Рё СЃРїСЂР°Р№С‚ РІ СЂРµРіРґРѕР»Р»Рµ (Р»СЋР±РѕР№ СЂРµР¶РёРј), РёРЅР°С‡Рµ 0

### Р Р°РЅС‚Р°Р№Рј-СЌС„С„РµРєС‚ СЂРµРіРґРѕР»Р»Р° (Android)
- **Sprite.ragdollMode** (transient int) вЂ” СЃРѕСЃС‚РѕСЏРЅРёРµ: 0 = РІС‹РєР», 1 = СЂРµРіРґРѕР»Р», 2 = СЂРµРіРґРѕР»Р»-СЃР»РµРґРѕРІР°РЅРёРµ
- **SetRagdollAction** вЂ” СЃС‚Р°РІРёС‚ СЂРµР¶РёРј РёР· С„РѕСЂРјСѓР»С‹ (>= 2 в†’ 2; != 0 в†’ 1; РёРЅР°С‡Рµ 0)
- **PhysicsLook** вЂ” РїСЂРё `isRagdolled()` (mode > 0):
  - `setX/setY/setPosition/setXInUserInterfaceDimensionUnit` вЂ” **РќР• РїРёС€СѓС‚** РІ physicsObject (РґРІРёР¶РµРЅРёРµ РёРіРЅРѕСЂРёСЂСѓРµС‚СЃСЏ)
  - `setRotation` вЂ” **РќР• РјРµРЅСЏРµС‚** РЅР°РїСЂР°РІР»РµРЅРёРµ physicsObject
  - `setScale` вЂ” **РќР• РїРµСЂРµСЃС‚СЂР°РёРІР°РµС‚** С„РёР·РёС‡РµСЃРєСѓСЋ С„РѕСЂРјСѓ
  - `getX/getY/getRotation` вЂ” С‡РёС‚Р°СЋС‚ СЃ physicsObject (СЃРїСЂР°Р№С‚ РІРёР·СѓР°Р»СЊРЅРѕ СЃР»РµРґСѓРµС‚ Р·Р° С‚РµР»РѕРј)
- **Р РµР¶РёРј 2 (СЂРµРіРґРѕР»Р»-СЃР»РµРґРѕРІР°РЅРёРµ / В«РєСѓРєР»Р° РЅР° РІРµСЂС‘РІРєРµВ»)**:
  - РЎРµС‚С‚РµСЂС‹ (СЃРј. РІС‹С€Рµ) РїСЂРё mode == 2 Р·Р°РїРѕРјРёРЅР°СЋС‚ **С†РµР»СЊ** (С†РµРЅС‚СЂ СЃРїСЂР°Р№С‚Р° РІ РјРёСЂРѕРІС‹С… РєРѕРѕСЂРґРёРЅР°С‚Р°С…) РІРјРµСЃС‚Рѕ РёРіРЅРѕСЂРёСЂРѕРІР°РЅРёСЏ
  - `PhysicsLook.draw()` РІС‹Р·С‹РІР°РµС‚ `updateRagdollFollow()`: P-РєРѕРЅС‚СЂРѕР»Р»РµСЂ
    `v += (dx*stiffness - v) * blend` (stiffness=6, blend=0.2, РІ px/СЃ) вЂ” С‚РµР»Рѕ РїР»Р°РІРЅРѕ
    РґРѕРіРѕРЅСЏРµС‚ С†РµР»СЊ СЃ РёРЅРµСЂС†РёРµР№: СЃРІРёСЃР°РµС‚, РєР°С‡Р°РµС‚СЃСЏ, СЃС‚Р°Р»РєРёРІР°РµС‚СЃСЏ, РЅРѕ РµРґРµС‚ Р·Р° СЃРєСЂРёРїС‚РѕРј
    (goto touch, glide Рё С‚.Рґ.)
  - Р—Р°С‚РµРј draw РІС‹Р·С‹РІР°РµС‚ getX/getY/getRotation вЂ” Р°РєС‚РѕСЂ СЂРёСЃСѓРµС‚СЃСЏ РЅР° РїРѕР·РёС†РёРё С‚РµР»Р°
  - Р•СЃР»Рё С†РµР»СЊ РЅРµ Р·Р°РґР°РЅР° СЃРєСЂРёРїС‚РѕРј вЂ” Р±РµСЂС‘С‚СЃСЏ С‚РµРєСѓС‰Р°СЏ РїРѕР·РёС†РёСЏ С‚РµР»Р° (followTargetSet)
- **Р§С‚Рѕ РїСЂРѕРґРѕР»Р¶Р°РµС‚ СЂР°Р±РѕС‚Р°С‚СЊ** РІ СЂРµРіРґРѕР»Р»Рµ:
  - Р“СЂР°РІРёС‚Р°С†РёСЏ Рё РєРѕР»Р»РёР·РёРё (С‚РµР»Рѕ DYNAMIC)
  - `SetVelocityAction`, `ApplyForceAction` Рё РґСЂСѓРіРёРµ physics-actions (РёРґСѓС‚ РЅР°РїСЂСЏРјСѓСЋ РІ physicsObject/mРёРЅСѓСЏ PhysicsLook)
- **Р¤РѕСЂРјСѓР»Р° `SPRITE_RAGDOLLED`** вЂ” С‡РёС‚Р°РµС‚ `s.ragdollMode > 0` РёР· FormulaElement

### Р¤Р°Р№Р»С‹
```
content/actions/SetRagdollAction.kt     вЂ” СЃС‚Р°РІРёС‚ sprite.ragdollMode (0/1/2)
content/bricks/SetRagdollBrick.java     вЂ” brick (FormulaBrick, PHYSICS_TOGGLE field)
content/Sprite.java                     вЂ” РїРѕР»Рµ ragdollMode (int)
physics/PhysicsLook.java                вЂ” isRagdolled()/isRagdollFollow() guard РІ СЃРµС‚С‚РµСЂР°С… + updateRagdollFollow()
formulaeditor/Functions.java            вЂ” SPRITE_RAGDOLLED enum
formulaeditor/FormulaElement.java       вЂ” case SPRITE_RAGDOLLED
test/.../SetRagdollBrickTest.java       вЂ” 13 С‚РµСЃС‚РѕРІ (brick wiring, action 0/1/2, clamp, formula)
```

## 4. Control category вЂ” РєР»РѕРЅС‹ РїРѕ РЅРѕРјРµСЂСѓ
- **DeleteCloneByNumberBrick** вЂ” СѓРґР°Р»СЏРµС‚ РєР»РѕРЅ РїРѕ РЅРѕРјРµСЂСѓ (cloneIndex)
- **ExecuteForCloneNumberBrick** вЂ” РєРѕРЅС‚РµР№РЅРµСЂРЅС‹Р№ Р±Р»РѕРє (CompositeBrick + EndBrick), РІС‹РїРѕР»РЅСЏРµС‚ РІРЅСѓС‚СЂРµРЅРЅРёРµ Р±Р»РѕРєРё С‚РѕР»СЊРєРѕ РµСЃР»Рё cloneIndex СЃРѕРІРїР°РґР°РµС‚

## 5. Look category вЂ” РїСЂРёРІСЏР·РєР° Рє РєР°РјРµСЂРµ СЃРѕ СЃРјРµС‰РµРЅРёРµРј
- **AttachToCameraWithOffsetBrick** вЂ” РїСЂРёРІСЏР·С‹РІР°РµС‚ 3D РѕР±СЉРµРєС‚ Рє РєР°РјРµСЂРµ СЃ X/Y/Z СЃРјРµС‰РµРЅРёРµРј

## 6. Sprite.java
- Р”РѕР±Р°РІР»РµРЅРѕ РїРѕР»Рµ `cloneIndex` (int, transient) вЂ” 0 РґР»СЏ РѕСЂРёРіРёРЅР°Р»Р°, 1+ РґР»СЏ РєР»РѕРЅРѕРІ

## 7. StageListener.java
- РџРѕР»Рµ `cloneCounter` вЂ” СЃС‡С‘С‚С‡РёРє РЅРѕРјРµСЂРѕРІ РєР»РѕРЅРѕРІ
- РњРµС‚РѕРґ `removeCloneByIndex(int)` вЂ” СѓРґР°Р»РµРЅРёРµ РєР»РѕРЅР° РїРѕ РЅРѕРјРµСЂСѓ
- Р’ `cloneSpriteAndAddToStage()` вЂ” `clone.cloneIndex = cloneCounter++`

## 8. SceneManager.java
- РњРµС‚РѕРґ `attachObjectToCamera(String objectId, float offsetX, float offsetY, float offsetZ)` вЂ” РЅРѕРІР°СЏ РїРµСЂРµРіСЂСѓР·РєР° СЃ 4 РїР°СЂР°РјРµС‚СЂР°РјРё

## 9. XStream
- `XStreamBrickConverter` Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё РѕР±РЅР°СЂСѓР¶РёРІР°РµС‚ РІСЃРµ Brick-РєР»Р°СЃСЃС‹ РІ РїР°РєРµС‚Рµ `org.catrobat.catroid.content.bricks` РїРѕ РёРјРµРЅРё РєР»Р°СЃСЃР°, РїРѕСЌС‚РѕРјСѓ СЏРІРЅР°СЏ СЂРµРіРёСЃС‚СЂР°С†РёСЏ РЅРµ С‚СЂРµР±СѓРµС‚СЃСЏ.

## 10. Formula fixes
- FILE_PROJECT_SIZE, FILE_SIZE_IN_DIR, FILE_SIZE_AT_PATH РґРѕР±Р°РІР»РµРЅС‹ РІ TEXT EnumSet РІ Functions.java
- Р”РѕР±Р°РІР»РµРЅС‹ РІ DEVICE_FUNCTIONS/DEVICE_PARAMS РІ CategoryListFragment.java
- Р”РѕР±Р°РІР»РµРЅР° СЃС‚СЂРѕРєР° `formula_file_project_size_param`

## 11. NeoScript вЂ” reusable script modules (.neoscript)

### Summary
РЎРёСЃС‚РµРјР° СЌРєСЃРїРѕСЂС‚Р°/РёРјРїРѕСЂС‚Р° РїРµСЂРµРёСЃРїРѕР»СЊР·СѓРµРјС‹С… РјРѕРґСѓР»РµР№ СЃРєСЂРёРїС‚РѕРІ РІ С„РѕСЂРјР°С‚Рµ `.neoscript`. РџРѕР·РІРѕР»СЏРµС‚ СЃРѕС…СЂР°РЅРёС‚СЊ РІС‹РґРµР»РµРЅРЅС‹Рµ СЃРєСЂРёРїС‚С‹ РІ С„Р°Р№Р» Рё РёРјРїРѕСЂС‚РёСЂРѕРІР°С‚СЊ РёС… РІ Р»СЋР±РѕР№ РѕР±СЉРµРєС‚ С‚РѕРіРѕ Р¶Рµ РёР»Рё РґСЂСѓРіРѕРіРѕ РїСЂРѕРµРєС‚Р°.

### Files
```
neoscript/
  NeoScriptFile.java        вЂ” РєРѕСЂРЅРµРІР°СЏ РјРѕРґРµР»СЊ (СЃРїРёСЃРѕРє Script + UserVariable + UserList)
  NeoScriptSerializer.java  вЂ” XStream-СЃРµСЂРёР°Р»РёР·Р°С†РёСЏ СЃ РІР°Р»РёРґР°С†РёРµР№ РІРµСЂСЃРёРё
  NeoScriptExporter.java    вЂ” СЃР±РѕСЂРєР° NeoScriptFile РёР· РІС‹Р±СЂР°РЅРЅС‹С… СЃРєСЂРёРїС‚РѕРІ + СЂРµС„РµСЂРµРЅСЃРѕРІ
  NeoScriptImporter.java    вЂ” РІР»РёРІР°РЅРёРµ СЃРєСЂРёРїС‚РѕРІ РІ target Sprite СЃ dedup
  NeoScriptUserData.java    вЂ” СЃР±РѕСЂ/РїРµСЂРµР»РёРЅРєРѕРІРєР° UserVariable/UserList (reflection)
  NeoScriptException.java   вЂ” РєР°СЃС‚РѕРјРЅРѕРµ РёСЃРєР»СЋС‡РµРЅРёРµ

content/actions/
  ImportScriptAction.kt     вЂ” TemporalAction: runtime-РёРјРїРѕСЂС‚ .neoscript РІ РѕР±СЉРµРєС‚

content/bricks/
  ImportScriptBrick.java    вЂ” Brick (File category): objectName + filePath + overwrite Spinner

res/layout/
  brick_import_script.xml   вЂ” BrickLayout СЃ РґРІСѓРјСЏ FormulaEditText + Spinner

ui/recyclerview/fragment/
  ScriptFragment.java       вЂ” SAVE_AS_SCRIPT action mode + exportScripts() + launchNeoScriptFilePicker()

ui/
  SpriteActivity.java       вЂ” importNeoScriptModule() + REQUEST_NEO_SCRIPT_FILE/IMPORT handlers

test/neoscript/
  NeoScriptModuleTest.java  вЂ” 13 С‚РµСЃС‚РѕРІ: round-trip, import, dedup, overwrite, version validation, large load, undo model
```

### Design decisions
- **Container root**: `NeoScriptFile` (РЅРµ Project) вЂ” СЃРѕРґРµСЂР¶РёС‚ С‚РѕР»СЊРєРѕ РІС‹Р±СЂР°РЅРЅС‹Рµ СЃРєСЂРёРїС‚С‹ + РЅРµРѕР±С…РѕРґРёРјС‹Рµ РїРµСЂРµРјРµРЅРЅС‹Рµ/СЃРїРёСЃРєРё. Р‘РµР· СЃС†РµРЅ, Р°СЃСЃРµС‚РѕРІ, РЅР°СЃС‚СЂРѕРµРє.
- **Serialization**: РїРµСЂРµРёСЃРїРѕР»СЊР·СѓРµС‚ XStream-РєРѕРЅС„РёРіСѓСЂР°С†РёСЋ РїСЂРѕРµРєС‚Р° (`XstreamSerializer.getInstance().getXstream()`), РїРѕСЌС‚РѕРјСѓ РІСЃРµ Brick/Formula-РєРѕРЅРІРµСЂС‚РµСЂС‹ СЂР°Р±РѕС‚Р°СЋС‚ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё. Р”РѕР±Р°РІР»РµРЅ Р°Р»РёР°СЃ `<neoscript>` РґР»СЏ РєРѕСЂРЅСЏ.
- **Versioning**: `formatVersion` (int), MIN=1, MAX=1. РЎС‚Р°СЂС‹Рµ/Р±СѓРґСѓС‰РёРµ РІРµСЂСЃРёРё РѕС‚РєР»РѕРЅСЏСЋС‚СЃСЏ СЃ РїРѕРЅСЏС‚РЅС‹Рј СЃРѕРѕР±С‰РµРЅРёРµРј.
- **Unknown blocks**: `XStreamBrickConverter` Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё СЃРѕР·РґР°С‘С‚ `UnknownBrick` РґР»СЏ РЅРµРёР·РІРµСЃС‚РЅС‹С… С‚РёРїРѕРІ Р±Р»РѕРєРѕРІ вЂ” СЃРѕРІРјРµСЃС‚РёРјРѕСЃС‚СЊ СЃ Р±СѓРґСѓС‰РёРјРё РІРµСЂСЃРёСЏРјРё.
- **ID remapping**: РїСЂРё РёРјРїРѕСЂС‚Рµ СЃРєСЂРёРїС‚С‹ РєР»РѕРЅРёСЂСѓСЋС‚СЃСЏ С‡РµСЂРµР· `Script.clone()`, РєРѕС‚РѕСЂС‹Р№ РіРµРЅРµСЂРёСЂСѓРµС‚ СЃРІРµР¶РёРµ scriptId Рё brickId (С‡РµСЂРµР· XStream ID-РіРµРЅРµСЂР°С‚РѕСЂ).
- **Variable relinking**: `NeoScriptUserData` С‡РµСЂРµР· reflection РѕР±С…РѕРґРёС‚ РІСЃРµ `UserVariable`/`UserList` РїРѕР»СЏ РІ Р±СЂРёРєР°С…, РЅР°С…РѕРґРёС‚ РёР»Рё СЃРѕР·РґР°С‘С‚ РїРµСЂРµРјРµРЅРЅС‹Рµ СЃ С‚РµРј Р¶Рµ РёРјРµРЅРµРј РІ С†РµР»РµРІРѕРј РїСЂРѕРµРєС‚Рµ/СЃРїСЂР°Р№С‚Рµ.
- **Duplicate detection**: СЃС‚Р°Р±РёР»СЊРЅР°СЏ СЃРёРіРЅР°С‚СѓСЂР° = `ClassName(simple)#trigger(TEXT)` (РґР»СЏ BroadcastScript вЂ” broadcastMessage). Overwrite = replace, РёРЅР°С‡Рµ skip.
- **Undo/redo**: СЂРµРґР°РєС‚РѕСЂСЃРєРёР№ РёРјРїРѕСЂС‚ РІС‹Р·С‹РІР°РµС‚ `copyProjectForUndoOption()` РїРµСЂРµРґ РёР·РјРµРЅРµРЅРёСЏРјРё.
- **Security**: XStream security deny-by-wildcard РґР»СЏ СЃРёСЃС‚РµРјРЅС‹С… РїР°РєРµС‚РѕРІ. File path validation С‡РµСЂРµР· РєР°СЃС‚РѕРјРЅС‹Рµ Formula (РЅРµ raw strings).
- **Runtime brick** (`ImportScriptBrick`): Formula-РїРѕР»СЏ РґР»СЏ objectName Рё filePath, Spinner РґР»СЏ overwrite. Р¤Р°Р№Р» РѕС‚РєСЂС‹РІР°РµС‚СЃСЏ С‡РµСЂРµР· `ACTION_OPEN_DOCUMENT` СЃ `REQUEST_NEO_SCRIPT_FILE`.
- **Save path**: `Download/NeocatroidScript/{name}.neoscript` С‡РµСЂРµР· `Constants.DOWNLOAD_DIRECTORY`.
- **Format**: XML СЃ `<neoscript>` РєРѕСЂРЅРµРј, Р±РµР· СЃР¶Р°С‚РёСЏ/Р°СЂС…РёРІР°С†РёРё вЂ” plain text РґР»СЏ СЂСѓС‡РЅРѕРіРѕ СЂРµРґР°РєС‚РёСЂРѕРІР°РЅРёСЏ.

### Adding a new .neoscript brick type
Р›СЋР±РѕР№ РЅРѕРІС‹Р№ Brick РІ РїР°РєРµС‚Рµ `org.catrobat.catroid.content.bricks` РѕР±РЅР°СЂСѓР¶РёРІР°РµС‚СЃСЏ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё вЂ” РЅРµ С‚СЂРµР±СѓРµС‚СЃСЏ СЂРµРіРёСЃС‚СЂР°С†РёСЏ РІ XStream. Р”Р»СЏ РєРѕСЂСЂРµРєС‚РЅРѕР№ СЃРµСЂРёР°Р»РёР·Р°С†РёРё РґРѕСЃС‚Р°С‚РѕС‡РЅРѕ РєРѕРЅСЃС‚СЂСѓРєС‚РѕСЂР° Р±РµР· РїР°СЂР°РјРµС‚СЂРѕРІ Рё СЃРѕРѕС‚РІРµС‚СЃС‚РІРёСЏ РёРјРµРЅРё РєР»Р°СЃСЃР°.

### NeoScript brick reference (current)


 `ImportScriptBrick` (Fileв†’NeoScript cat.) `ImportScriptAction` | objectName, filePath, overwrite | Import .neoscript into existing object |
 `CreateObjectBrick` (NeoScript cat., NEW)  `CreateObjectAction` | objectName (Formula), scene (spinner), persist (Yes/No) | Create blank sprite in scene; if persist=Yes, save canonical project to disk
 `AssignScriptsBrick` (NeoScript cat., NEW)  `AssignScriptsAction` | filePath, objectName, scene, replace (Yes/No), save (Yes/No) | Assign .neoscript to object in scene; if save=Yes, save canonical project to disk 

### Scene-aware bricks design
- Scene stored as `String` (name): `null`/empty = Current scene, otherwise `project.getSceneByName(name)`.
- Spinner: StringOption("Current scene") + Scene items.
- Backward compat: missing/empty scene field в†’ Current scene.
- Object lookup scoped to the resolved scene (not global).
- Inactive scene: scripts added to model only (no runtime registration).
- Active scene: `executeConsoleScript()` starts added scripts.
- UnknownBrick detection: `AssignScriptsAction` checks for `UnknownBrick` instances pre-import, replaces with `NoteBrick`.
- `AssignScriptsBrick` "Replace existing scripts?" spinner [0/1]: 0 = keep existing + add imported (`ImportStrategy.APPEND_ALL`), 1 = remove ALL existing + add imported (`ImportStrategy.REPLACE_ALL`). This is SEPARATE from the `ImportScriptBrick` duplicate-overwrite (boolean в†’ `SKIP_DUPLICATES`/`REPLACE_DUPLICATES`). Do not conflate the two.
- `NeoScriptImporter.ImportStrategy` enum: `SKIP_DUPLICATES`, `REPLACE_DUPLICATES`, `APPEND_ALL`, `REPLACE_ALL`. `REPLACE_ALL` is atomic вЂ” all scripts are cloned+relinked first; only on full success is the target sprite's script list cleared and the new scripts added. Default serialized value MUST be 0 (least destructive).

### NeoScript persistence (2026-07)

`CreateObjectBrick` and `AssignScriptsBrick` have an OPTIONAL persistence flag so a runtime change can also be written to the canonical project on disk.

- **Flags**: `CreateObjectBrick.persistentSelection` (0 = runtime only, 1 = persist) and `AssignScriptsBrick.savePersistentSelection` (same). Both plain `int`, serialized by XStream. Missing field on load в†’ `0` (runtime only, least destructive). Getters: `isPersistent()` / `isSavePersistent()`.
- **Default**: No (runtime only). Old serialized bricks without the field keep working.
- **Mechanism**: the action mutates the live canonical `Project` (which `scope.project` already is вЂ” no clone), THEN calls `ProjectSaver(project, CatroidApplication.getAppContext()).saveProjectAsync {}`. On device this serialises the project (XstreamSerializer atomic temp+rename). The save is best-effort / fire-and-forget and is wrapped in try/catch: if the app context is unavailable the save is skipped (the in-memory model is still mutated).
- **Scene isolation**: the resolved scene (see above) is authoritative; object lookup is scoped to it. A persisted object/script lands in that scene's model.
- **Behaviour**: `persist`/`save` do NOT change runtime behaviour вЂ” the object/script is added to the canonical project model regardless; the flag only decides whether the canonical project is also written to disk.
- **Tests**: `NeoScriptPersistenceTest` (catroid/src/test/.../neoscript) covers in-memory canonical mutation, scene isolation, replace semantics, unknownв†’Note, large import, and XStream round-trip of the flag (forward + backward-compat). Full project save/load is environment-gated (device / Robolectric) and reuses Catroid's standard `ProjectSaver`.

### XStream
- `XStreamBrickConverter` Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё РѕР±РЅР°СЂСѓР¶РёРІР°РµС‚ РІСЃРµ Brick-РєР»Р°СЃСЃС‹ РїРѕ РёРјРµРЅРё РєР»Р°СЃСЃР°.
- РџР°РєРµС‚С‹ РїРѕРёСЃРєР°: `org.catrobat.catroid.content.bricks`, `org.catrobat.catroid.physics.content.bricks`.
- РќРµРёР·РІРµСЃС‚РЅС‹Рµ С‚РёРїС‹ в†’ `UnknownBrick` (РЅРµ Р»РѕРјР°РµС‚ Р·Р°РіСЂСѓР·РєСѓ).
- РЇРІРЅР°СЏ СЂРµРіРёСЃС‚СЂР°С†РёСЏ РЅРµ С‚СЂРµР±СѓРµС‚СЃСЏ, РЅРѕ РґР»СЏ РѕР±СЂР°С‚РЅРѕР№ СЃРѕРІРјРµСЃС‚РёРјРѕСЃС‚Рё РІ `XstreamSerializer.java` РµСЃС‚СЊ `xstream.alias("brick", ConcreteBrick.class)`.

---

# AI category вЂ” РµРґРёРЅС‹Р№ Р±Р»РѕРє В«РЎРїСЂРѕСЃРёС‚СЊ РРВ» + СЃРѕР±С‹С‚РёРµ РѕС‚РІРµС‚Р° (2026-08)

Р—Р°РјРµРЅР° СЃС‚Р°СЂС‹С… Р±Р»РѕРєРѕРІ AskGPT/AskGemini/AskGemini2/SetGeminiKey РЅР° РѕРґРёРЅ СѓРЅРёРІРµСЂСЃР°Р»СЊРЅС‹Р№
Р±Р»РѕРє СЃ РІС‹Р±РѕСЂРѕРј РїСЂРѕРІР°Р№РґРµСЂР° (OpenAI, Gemini, DeepSeek, OpenRouter, Anthropic, OpenCode):

- **AskAIBrick** (В«РЎРїСЂРѕСЃРёС‚СЊ РРВ», Neural в†’ LLM) вЂ” РїРѕР»СЏ: TEXT (РїСЂРѕРјРїС‚), BODY (СЃРёСЃС‚РµРјРЅС‹Р№
  РїСЂРѕРјРїС‚), MODEL (РјРѕРґРµР»СЊ, РїСѓСЃС‚Рѕ = РґРµС„РѕР»С‚ РїСЂРѕРІР°Р№РґРµСЂР°), СЃРїРёРЅРЅРµСЂ РїСЂРѕРІР°Р№РґРµСЂР°, СЃРїРёРЅРЅРµСЂ
  РїРµСЂРµРјРµРЅРЅРѕР№ РґР»СЏ РѕС‚РІРµС‚Р°. РџРёС€РµС‚ РѕС‚РІРµС‚ РІ UserVariable Рё fire `AiResponseEventId`.
- **WhenAIResponseBrick** (В«РљРѕРіРґР° РР РѕС‚РІРµС‚РёР»В», Events) вЂ” ScriptBrick, СЃРїРёРЅРЅРµСЂ РїСЂРѕРІР°Р№РґРµСЂР°
  (В«Р›СЋР±РѕР№ РїСЂРѕРІР°Р№РґРµСЂВ» = РїСѓСЃС‚Р°СЏ СЃС‚СЂРѕРєР°). РўСЂРёРіРіРµСЂРёС‚СЃСЏ РїРѕСЃР»Рµ Р·Р°РІРµСЂС€РµРЅРёСЏ AskAIBrick.
- РЎС‚Р°СЂС‹Рµ РєР»Р°СЃСЃС‹ (`AskGPTBrick`, `AskGeminiBrick`, `AskGemini2Brick`, `SetGeminiKeyBrick`)
  **РѕСЃС‚Р°РІР»РµРЅС‹** РґР»СЏ РґРµСЃРµСЂРёР°Р»РёР·Р°С†РёРё СЃС‚Р°СЂС‹С… РїСЂРѕРµРєС‚РѕРІ (XStream РїРѕ РёРјРµРЅРё РєР»Р°СЃСЃР°), СѓР±СЂР°РЅС‹
  С‚РѕР»СЊРєРѕ РёР· РїР°Р»РёС‚СЂС‹ `setupNeuralCategoryList`.

## Р¤Р°Р№Р»С‹

```
content/
  WhenAIResponseScript.java           вЂ” Script (provider: String, "" = Р»СЋР±РѕР№)
  eventids/AiResponseEventId.java     вЂ” equals: sprite + provider; РїСѓСЃС‚РѕР№ provider = wildcard
content/actions/
  AskAIAction.kt                      вЂ” TemporalAction: Thread + runBlocking CloudModelRuntime,
                                        РїРёС€РµС‚ РѕС‚РІРµС‚ РІ РїРµСЂРµРјРµРЅРЅСѓСЋ, fire AiResponseEventId(sprite, providerId)
content/bricks/
  AskAIBrick.kt                       вЂ” UserVariableBrickWithFormula + BrickSpinner<StringOption>
  WhenAIResponseBrick.java            вЂ” ScriptBrickBaseType + BrickSpinner<StringOption>
res/layout/
  brick_ask_ai.xml                    вЂ” Neural.Big: prompt/system/model edit + 2 СЃРїРёРЅРЅРµСЂР°
  brick_when_ai_response.xml          вЂ” Motion.MediumWhen: label + spinner
ai/model/CloudModelRuntime.kt         вЂ” РґРѕР±Р°РІР»РµРЅ `generateForProvider(provider, model, system, user)`
                                        (РєР»СЋС‡ С‡РµСЂРµР· AiPreferences.getApiKeyForProvider(provider.id))
```

## Р РµРіРёСЃС‚СЂР°С†РёСЏ

- `XstreamSerializer`: Р°Р»РёР°СЃС‹ `WhenAIResponseScript`/`WhenAIResponseBrick`/`AskAIBrick`.
- `CategoryBricksFactory.kt`: `AskAIBrick("Hello!")` РІ Neural (РѕР±Рµ РІРµС‚РєРё grouped/ungrouped);
  `WhenAIResponseBrick()` РІ Events (РІ РѕР±РµРёС… РІРµС‚РєР°С…, РґРѕСЃС‚СѓРїРµРЅ Рё С„РѕРЅСѓ).
- `BrickInfo.java`: СЃРїСЂР°РІРєР° ru/en. `RecentBrickListManager`: WhenAIResponseBrick РІ
  nonBackgroundSpriteClasses.
- `strings.xml`/`values-ru`: `brick_ask_ai`, `brick_ask_ai_system`, `brick_ask_ai_model`,
  `brick_ask_ai_provider`, `brick_when_ai_response`, `ai_provider_any`.
- `AiProvider` (ai/model/AiProvider.kt) вЂ” РµРґРёРЅСЃС‚РІРµРЅРЅС‹Р№ РёСЃС‚РѕС‡РЅРёРє СЃРїРёСЃРєР° РїСЂРѕРІР°Р№РґРµСЂРѕРІ
  (id, displayName, baseUrl, defaultModels); СЃРїРёРЅРЅРµСЂС‹ СЃС‚СЂРѕСЏС‚СЃСЏ РёР· `AiProvider.values()`.

## РўРµСЃС‚С‹

- `AskAIBrickTest.java` (4): wiring С‡РµСЂРµР· ActionFactory.createAskAIAction, РґРµС„РѕР»С‚РЅС‹Р№
  РїСЂРѕРІР°Р№РґРµСЂ, РєРѕРЅСЃС‚СЂСѓРєС‚РѕСЂС‹, clone.
- `WhenAIResponseBrickTest.java` (6): scriptв†”brick, РєРѕРЅСЃС‚СЂСѓРєС‚РѕСЂС‹, clone, createEventId,
  wildcard-equals (РїСѓСЃС‚РѕР№ РїСЂРѕРІР°Р№РґРµСЂ = Р»СЋР±РѕР№) + hashCode-РєРѕРЅСЃРёСЃС‚РµРЅС‚РЅРѕСЃС‚СЊ.
- РџСЂРѕРІРµСЂРєР°: `./gradlew :catroid:testCatroidDebugUnitTest --tests "*AskAIBrickTest*" --tests "*WhenAIResponseBrickTest*"`.

---

# Event category вЂ” РєР°СЃР°РЅРёРµ СЃРїСЂР°Р№С‚РѕРІ (WhenTouchingSprite, 2026-08)

Р”РІР° Р±Р»РѕРєР° СЃРѕР±С‹С‚РёР№, СЃСЂР°Р±Р°С‚С‹РІР°СЋС‰РёС… РїСЂРё РїРµСЂРµРєСЂС‹С‚РёРё С…РёС‚Р±РѕРєСЃРѕРІ (AABB) Р‘Р•Р— С„РёР·РёРєРё:

- **WhenTouchingSpriteBrick** (В«РљРѕРіРґР° РєР°СЃР°РµС‚СЃСЏ РґСЂСѓРіРѕРіРѕ Р°РєС‚С‘СЂР°В») вЂ” С‚СЂРёРіРіРµСЂРёС‚СЃСЏ РЅР° РєР°СЃР°РЅРёРµ Р›Р®Р‘РћР“Рћ СЃРїСЂР°Р№С‚Р°
- **WhenTouchingSpriteByNameBrick** (В«РљРѕРіРґР° РєР°СЃР°РµС‚СЃСЏ вЂ¦В») вЂ” spinner СЃ РІС‹Р±РѕСЂРѕРј РєРѕРЅРєСЂРµС‚РЅРѕРіРѕ СЃРїСЂР°Р№С‚Р° (РёР»Рё В«Р»СЋР±РѕРіРѕ Р°РєС‚С‘СЂР°В»)
- РЈ РѕР±РѕРёС… РµСЃС‚СЊ CheckBox В«СЂРµР°РіРёСЂРѕРІР°С‚СЊ РЅР° С„РѕРЅВ» (`reactToBackground`) вЂ” РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ С„РѕРЅ РёРіРЅРѕСЂРёСЂСѓРµС‚СЃСЏ

## Р¤Р°Р№Р»С‹

```
content/
  WhenTouchingSpriteScript.java           вЂ” Script (reactToBackground; eventId = TouchingSpriteEventId(sprite, ""))
  WhenTouchingSpriteByNameScript.java     вЂ” Script (spriteToTouchName + reactToBackground)
  TouchingSpriteTrigger.java              вЂ” edge-trigger: TRIGGER_NOW в†’ fire в†’ ALREADY_TRIGGERED в†’ reset РїСЂРё СЂР°СЃС…РѕР¶РґРµРЅРёРё
  eventids/TouchingSpriteEventId.java     вЂ” equality: sprite + touchedSpriteName (String, "" = Р»СЋР±РѕР№)
content/bricks/
  WhenTouchingSpriteBrick.java            вЂ” checkbox background (R.id.brick_when_touching_sprite_background_checkbox)
  WhenTouchingSpriteByNameBrick.java      вЂ” BrickSpinner<Sprite> + checkbox
res/layout/
  brick_when_touching_sprite.xml
  brick_when_touching_sprite_by_name.xml
```

## Р Р°РЅС‚Р°Р№Рј (Android)

- `Sprite.touchingSpriteTriggers` (transient Set<TouchingSpriteTrigger>), РёРЅРёС†РёР°Р»РёР·Р°С†РёСЏ РІ `initTouchingSpriteTriggers()` (РІС‹Р·С‹РІР°РµС‚СЃСЏ РёР· `StageListener` РїСЂРё СЃС‚Р°СЂС‚Рµ СЃС†РµРЅС‹ Рё РїСЂРё РєР»РѕРЅРёСЂРѕРІР°РЅРёРё, РїРѕ Р°РЅР°Р»РѕРіРёРё СЃ condition/firebase-С‚СЂРёРіРіРµСЂР°РјРё).
- РџСЂРѕРІРµСЂРєР° РєР°Р¶РґС‹Р№ РєР°РґСЂ: `Look.update()` в†’ `sprite.evaluateTouchingSpriteTriggers()`.
- `TouchingSpriteTrigger.isTouching()`: РѕР±Р° СЃРїСЂР°Р№С‚Р° РІРёРґРЅС‹, РЅРµ С„РѕРЅ (РµСЃР»Рё РЅРµ reactToBackground), РёРјСЏ СЃРѕРІРїР°РґР°РµС‚ (РёР»Рё Р»СЋР±РѕР№), AABB-РїРµСЂРµРєСЂС‹С‚РёРµ С‡РµСЂРµР· `Look.getX/Y/Width/HeightInUserInterfaceDimensionUnit()`.
- Fire: `sprite.look.fire(new EventWrapper(new TouchingSpriteEventId(sprite, name), false))` вЂ” edge-triggered: СЃРѕР±С‹С‚РёРµ С€Р»С‘С‚СЃСЏ РѕРґРёРЅ СЂР°Р· РїСЂРё Р’РҐРћР”Р• РІ РєР°СЃР°РЅРёРµ, СЃС‚Р°С‚СѓСЃ СЃР±СЂР°СЃС‹РІР°РµС‚СЃСЏ РєРѕРіРґР° РєР°СЃР°РЅРёРµ РїСЂРѕРїР°Р»Рѕ.
- РљР»РѕРЅС‹: `matchesTargetName` РјР°С‚С‡РёС‚ Рё РїРѕ РёРјРµРЅРё РѕСЂРёРіРёРЅР°Р»Р° (`other.myOriginal`).

## Desktop (DesktopScriptEngine.kt)

- `mapScriptTypeToEvent`: `WhenTouchingSpriteScript`/`WhenTouchingSpriteByNameScript` в†’ `"touching_sprite"`.
- РџР°СЂСЃРёРЅРі: `eventParam2 = <spriteToTouchName>` (РґР»СЏ СѓРЅРёРІРµСЂСЃР°Р»СЊРЅРѕРіРѕ СЃРєСЂРёРїС‚Р° СЌР»РµРјРµРЅС‚Р° РЅРµС‚ в†’ РїСѓСЃС‚Р°СЏ СЃС‚СЂРѕРєР° = Р»СЋР±РѕР№).
- `checkEvents`: `checkSpriteCollision(sprite, eventParam?.takeIf { isNotEmpty })` вЂ” РїСѓСЃС‚РѕРµ РёРјСЏ в†’ Р»СЋР±РѕР№ СЃРїСЂР°Р№С‚.

## Р РµРіРёСЃС‚СЂР°С†РёСЏ

- `XstreamSerializer`: Р°Р»РёР°СЃС‹ script/brick РґР»СЏ РѕР±РѕРёС… С‚РёРїРѕРІ.
- `CategoryBricksFactory.kt`: РѕР±Р° Р±Р»РѕРєР° РІ Events (С‚РѕР»СЊРєРѕ РґР»СЏ РЅРµ-С„РѕРЅРѕРІС‹С… СЃРїСЂР°Р№С‚РѕРІ, РѕР±Рµ РІРµС‚РєРё grouped/ungrouped).
- `BrickInfo.java`: СЃРїСЂР°РІРєР° ru/en.
- `RecentBrickListManager`: РѕР±Р° РІ nonBackgroundSpriteClasses.
- `strings.xml`/`values-ru`: `brick_when_touching_sprite`, `brick_when_touching_sprite_by_name`, `touching_sprite_anything`, `brick_when_touching_sprite_background`.

## РўРµСЃС‚С‹

- `WhenTouchingSpriteBrickTest.java` (4) + `WhenTouchingSpriteByNameBrickTest.java` (3) вЂ” brickв†”script linkage, clone, РєРѕРЅСЃС‚СЂСѓРєС‚РѕСЂС‹.
- РџСЂРѕРІРµСЂРєР°: `./gradlew :catroid:testCatroidDebugUnitTest --tests "*WhenTouchingSprite*"`.

---

# Backpack вЂ” РїРѕСЂС‚С„РµР»СЊ СЃРєСЂРёРїС‚РѕРІ/РѕР±СЉРµРєС‚РѕРІ

## РћР±Р·РѕСЂ

Backpack (В«РїРѕСЂС‚С„РµР»СЊВ»/В«СЂСЋРєР·Р°РєВ») вЂ” СЃРёСЃС‚РµРјР° РєРѕРїРёСЂРѕРІР°РЅРёСЏ СЃРєСЂРёРїС‚РѕРІ, РѕР±СЉРµРєС‚РѕРІ, СЃС†РµРЅ, Р·РІСѓРєРѕРІ Рё РѕР±СЂР°Р·РѕРІ РјРµР¶РґСѓ РїСЂРѕРµРєС‚Р°РјРё Р±РµР· СЌРєСЃРїРѕСЂС‚Р° РІ С„Р°Р№Р». Р”Р°РЅРЅС‹Рµ С…СЂР°РЅСЏС‚СЃСЏ РІ РґРёСЂРµРєС‚РѕСЂРёРё РїСЂРёР»РѕР¶РµРЅРёСЏ РєР°Рє JSON + С„Р°Р№Р»С‹.

## Р¤Р°Р№Р»С‹

```
common/Backpack.java                                  вЂ” РјРѕРґРµР»СЊ РґР°РЅРЅС‹С… (СЃРїРёСЃРєРё РґР»СЏ РєР°Р¶РґРѕРіРѕ С‚РёРїР°)
io/BackpackSerializer.java                            вЂ” JSON-СЃРµСЂРёР°Р»РёР·Р°С†РёСЏ/РґРµСЃРµСЂРёР°Р»РёР·Р°С†РёСЏ
io/BackpackScriptSerializerAndDeserializer.java       вЂ” Gson-Р°РґР°РїС‚РµСЂ РґР»СЏ Script
io/BackpackFormulaFieldSerializerAndDeserializer.java вЂ” Gson-Р°РґР°РїС‚РµСЂ РґР»СЏ С„РѕСЂРјСѓР»
io/BackpackInterfaceSerializerAndDeserializer.java    вЂ” Р±Р°Р·РѕРІС‹Р№ Р°РґР°РїС‚РµСЂ
ui/controller/BackpackListManager.java                вЂ” singleton, РґРѕСЃС‚СѓРї Рє СЂСЋРєР·Р°Сѓ + СЃРѕС…СЂР°РЅРµРЅРёРµ/Р·Р°РіСЂСѓР·РєР°
ui/recyclerview/backpack/
  BackpackActivity.java                               вЂ” Activity СЃ ViewPager (РІРєР»Р°РґРєРё Scripts/Sounds/Looks/Sprites/Scenes)
  BackpackScriptFragment.java                         вЂ” СЃРїРёСЃРѕРє СЃРєСЂРёРїС‚-РіСЂСѓРїРї, unpack/delete
  BackpackSoundFragment.java                          вЂ” СЃРїРёСЃРѕРє Р·РІСѓРєРѕРІ РІ СЂСЋРєР·Р°РєРµ
  BackpackLookFragment.java                           вЂ” СЃРїРёСЃРѕРє РѕР±СЂР°Р·РѕРІ
  BackpackSpriteFragment.java                         вЂ” СЃРїРёСЃРѕРє СЃРїСЂР°Р№С‚РѕРІ
  BackpackSceneFragment.java                          вЂ” СЃРїРёСЃРѕРє СЃС†РµРЅ
ui/recyclerview/fragment/
  ScriptFragment.java                                 вЂ” action mode "Pack", РґРёР°Р»РѕРіРё СѓРїР°РєРѕРІРєРё/СЂР°СЃРїР°РєРѕРІРєРё
  SoundFragment.java                                  вЂ” action mode "Pack" РґР»СЏ Р·РІСѓРєРѕРІ
ui/recyclerview/controller/
  ScriptController.java                               вЂ” pack()/unpack() вЂ” СѓРїР°РєРѕРІРєР°/СЂР°СЃРїР°РєРѕРІРєР° СЃРєСЂРёРїС‚РѕРІ
  SoundController.java                                вЂ” pack()/unpack()/copy() вЂ” СѓРїР°РєРѕРІРєР°/СЂР°СЃРїР°РєРѕРІРєР° Р·РІСѓРєРѕРІ
  LookController.java                                 вЂ” pack()/unpack()/copy() вЂ” СѓРїР°РєРѕРІРєР°/СЂР°СЃРїР°РєРѕРІРєР° РѕР±СЂР°Р·РѕРІ
layout/
  dialog_pack_options.xml                             вЂ” РґРёР°Р»РѕРі СЃ С‡РµРєР±РѕРєСЃР°РјРё (Р·РІСѓРєРё + Р·РЅР°С‡РµРЅРёСЏ)
```

## РњРѕРґРµР»СЊ РґР°РЅРЅС‹С… (Backpack.java)


 `backpackedScripts` | `HashMap<String, List<Script>>` | РЎРєСЂРёРїС‚-РіСЂСѓРїРїС‹ РїРѕ РёРјРµРЅРё |
 `backpackedUserDefinedBricks` | `HashMap<String, List<UserDefinedBrick>>` | UserDefined Р±СЂРёРєРё РїРѕ РіСЂСѓРїРїРµ |
 `backpackedUserVariables` | `HashMap<String, HashMap<String, Int>>` | РРјРµРЅР° РїРµСЂРµРјРµРЅРЅС‹С… + С‚РёРї (GLOBAL/LOCAL/MULTIPLAYER) РїРѕ РіСЂСѓРїРїРµ |
 `backpackedUserLists` | `HashMap<String, HashMap<String, Int>>` | РРјРµРЅР° СЃРїРёСЃРєРѕРІ + С‚РёРї РїРѕ РіСЂСѓРїРїРµ |
`backpackedSounds` | `List<SoundInfo>` | Р—РІСѓРєРё РІ СЂСЋРєР·Р°РєРµ (РѕС‚РґРµР»СЊРЅРѕ РѕС‚ СЃРєСЂРёРїС‚РѕРІ) |
 `backpackedLooks` | `List<LookData>` | РћР±СЂР°Р·С‹ РІ СЂСЋРєР·Р°РєРµ |
`backpackedScriptSounds` | `HashMap<String, List<SoundInfo>>` | Р—РІСѓРєРё Р’РќРЈРўР Р СЃРєСЂРёРїС‚-РіСЂСѓРїРї (NEW) |
 `backpackedVariableValues` | `HashMap<String, HashMap<String, String>>` | Р—РЅР°С‡РµРЅРёСЏ РїРµСЂРµРјРµРЅРЅС‹С… РїРѕ РіСЂСѓРїРїРµ (NEW) |
 `backpackedListValues` | `HashMap<String, HashMap<String, String>>` | Р—РЅР°С‡РµРЅРёСЏ СЃРїРёСЃРєРѕРІ (CSV) РїРѕ РіСЂСѓРїРїРµ (NEW) |

## РЈРїР°РєРѕРІРєР° СЃРєСЂРёРїС‚РѕРІ (2026-08)

### Р”РёР°Р»РѕРі РІС‹Р±РѕСЂР°

РџСЂРё СѓРїР°РєРѕРІРєРµ СЃРєСЂРёРїС‚Р° С‡РµСЂРµР· `ScriptFragment.showNewScriptGroupAlert()`:
1. РџРѕР»СЊР·РѕРІР°С‚РµР»СЊ РІРІРѕРґРёС‚ РёРјСЏ РіСЂСѓРїРїС‹
2. Р•СЃР»Рё СЃРєСЂРёРїС‚ СЃРѕРґРµСЂР¶РёС‚ Р·РІСѓРєРё РёР»Рё РїРµСЂРµРјРµРЅРЅС‹Рµ вЂ” РїРѕРєР°Р·С‹РІР°РµС‚СЃСЏ `dialog_pack_options.xml`:
   - в‘ В«РЎРєСЂРёРїС‚ СЃРѕРґРµСЂР¶РёС‚ Р·РІСѓРєРё. РЎРѕС…СЂР°РЅРёС‚СЊ РёС… РІРјРµСЃС‚Рµ СЃРѕ СЃРєСЂРёРїС‚РѕРј?В»
   - в‘ В«РЎРѕС…СЂР°РЅРёС‚СЊ С‚РµРєСѓС‰РёРµ Р·РЅР°С‡РµРЅРёСЏ РїРµСЂРµРјРµРЅРЅС‹С… Рё СЃРїРёСЃРєРѕРІ?В»
3. Р§РµРєР±РѕРєСЃС‹ СЃРєСЂС‹РІР°СЋС‚СЃСЏ РµСЃР»Рё СЃРѕРѕС‚РІРµС‚СЃС‚РІСѓСЋС‰РёС… РґР°РЅРЅС‹С… РЅРµС‚ РІ СЃРєСЂРёРїС‚Рµ

### ScriptController.pack()

```
pack(groupName, bricksToPack, includeSounds, includeValues)
```

- РљР»РѕРЅРёСЂСѓРµС‚ СЃРєСЂРёРїС‚С‹ Рё UserDefined Р±СЂРёРєРё
- `includeSounds=true` в†’ СЃРѕР±РёСЂР°РµС‚ `SoundInfo` РёР· `PlaySoundBrick`/`PlaySoundAndWaitBrick` (dedup РїРѕ РёРјСЏ)
- `includeValues=true` в†’ СЃРѕР±РёСЂР°РµС‚ Р·РЅР°С‡РµРЅРёСЏ `UserVariable`/`UserList` РёР· Р±СЂРёРєРѕРІ Рё С„РѕСЂРјСѓР» (СЂРµРєСѓСЂСЃРёРІРЅС‹Р№ РѕР±С…РѕРґ `FormulaElement`)
- РЎРѕС…СЂР°РЅСЏРµС‚ РІСЃС‘ РІ `BackpackListManager`

### РҐСЂР°РЅРµРЅРёРµ Р·РІСѓРєРѕРІ РІ СЃРєСЂРёРїС‚-РіСЂСѓРїРїРµ

Р—РІСѓРєРё С…СЂР°РЅСЏС‚СЃСЏ РѕС‚РґРµР»СЊРЅРѕ РѕС‚ РіР»РѕР±Р°Р»СЊРЅРѕРіРѕ СЃРїРёСЃРєР° `backpackedSounds`:
- `backpackedScriptSounds[groupName]` в†’ СЃРїРёСЃРѕРє `SoundInfo` (С„Р°Р№Р»С‹ РІ `backpackSoundDirectory`)
- РџСЂРё СѓРґР°Р»РµРЅРёРё РіСЂСѓРїРїС‹ Р·РІСѓРєРё С‚РѕР¶Рµ СѓРґР°Р»СЏСЋС‚СЃСЏ (`removeItemFromScriptBackPack`)

## Р Р°СЃРїР°РєРѕРІРєР° СЃРєСЂРёРїС‚РѕРІ (2026-08)

### ScriptController.unpack()

```
unpack(scriptName, scriptToUnpack, destinationSprite)
```

- РљР»РѕРЅРёСЂСѓРµС‚ СЃРєСЂРёРїС‚
- **Р—РІСѓРєРё:** РґР»СЏ РєР°Р¶РґРѕРіРѕ `PlaySoundBrick`:
  - Р•СЃР»Рё Р·РІСѓРє СЃ С‚Р°РєРёРј РёРјРµРЅРµРј СѓР¶Рµ РµСЃС‚СЊ РІ СЃРїСЂР°Р№С‚Рµ в†’ РёСЃРїРѕР»СЊР·РѕРІР°С‚СЊ СЃСѓС‰РµСЃС‚РІСѓСЋС‰РёР№
  - Р•СЃР»Рё РµСЃС‚СЊ РІ СЂСЋРєР·Р°РєРµ (`backpackedScriptSounds[scriptName]`) в†’ РєРѕРїРёСЂРѕРІР°С‚СЊ С‡РµСЂРµР· `SoundController.copy()`
  - РРЅР°С‡Рµ РѕСЃС‚Р°РІРёС‚СЊ РєР°Рє РµСЃС‚СЊ (Р·РІСѓРє РЅРµ РїСЂРёРІСЏР·Р°РЅ)
- **Р—РЅР°С‡РµРЅРёСЏ:** РґР»СЏ `UserVariableBrickInterface`/`UserListBrick`:
  - Р•СЃР»Рё РµСЃС‚СЊ СЃРѕС…СЂР°РЅС‘РЅРЅРѕРµ Р·РЅР°С‡РµРЅРёРµ РІ `backpackedVariableValues`/`backpackedListValues` в†’ РІРѕСЃСЃС‚Р°РЅРѕРІРёС‚СЊ
  - Р§РёСЃР»Р° РїР°СЂСЃСЏС‚СЃСЏ РєР°Рє `Double`, РѕСЃС‚Р°Р»СЊРЅРѕРµ РєР°Рє `String`
  - РЎРїРёСЃРєРё С…СЂР°РЅСЏС‚СЃСЏ РєР°Рє CSV (comma-separated)

### Dedup РїРѕ РёРјСЏ

РџСЂРё СЂР°СЃРїР°РєРѕРІРєРµ Р·РІСѓРєРѕРІ РїРѕРёСЃРє СЃРЅР°С‡Р°Р»Р° РїРѕ РёРјСЏ С„Р°Р№Р»Р° (РЅРµ РїРѕ UUID), С‡С‚Рѕ РїРѕР·РІРѕР»СЏРµС‚ РєРѕСЂСЂРµРєС‚РЅРѕ СЂРµР·РѕР»РІРёС‚СЊ Р·РІСѓРєРё РїРѕСЃР»Рµ СѓРїР°РєРѕРІРєРё.

---

# РСЃРїСЂР°РІР»РµРЅРёСЏ Р±РµР·РѕРїР°СЃРЅРѕСЃС‚Рё Рё Р±Р°РіРѕРІ (2026-07)

##  РљСЂРёС‚РёС‡РµСЃРєРёРµ РїСЂРѕР±Р»РµРјС‹ Р±РµР·РѕРїР°СЃРЅРѕСЃС‚Рё
- **Keystore СѓРґР°Р»С‘РЅ РёР· VCS**: `catroid/keystore` в†’ `git rm --cached`, РґРѕР±Р°РІР»РµРЅ РІ `.gitignore` (РЅР°СЃС‚РѕСЏС‚РµР»СЊРЅРѕ СЂРµРєРѕРјРµРЅРґСѓРµС‚СЃСЏ РѕС‚РѕР·РІР°С‚СЊ РєР»СЋС‡)
- **GitHub OAuth Client ID**: `SettingsFragment.java` вЂ” Р·Р°С…Р°СЂРґРєРѕР¶РµРЅРЅС‹Р№ client ID Р·Р°РјРµРЅС‘РЅ РЅР° `BuildConfig.GITHUB_CLIENT_ID` СЃ fallback (СЃР°Рј Р»РёС‚РµСЂР°Р» РІ РґРѕРєСѓРјРµРЅС‚Р°С†РёРё РЅРµ РїСЂРёРІРѕРґРёС‚СЃСЏ)
- **Gemini API key**: `GeminiManager.kt` вЂ” `@Deprecated api_key` СЃРёРЅС…СЂРѕРЅРёР·РёСЂРѕРІР°РЅ СЃ `EncryptedSharedPreferences`; `SetGeminiKeyAction.kt` РїРёС€РµС‚ РІ РѕР±Р° РјРµСЃС‚Р°
- **WebView URL validation**: `StageActivity.createWebViewWithUrl()` вЂ” С‚РѕР»СЊРєРѕ HTTPS/file/shell СЃС…РµРјС‹, РѕСЃС‚Р°Р»СЊРЅС‹Рµ РѕС‚РєР»РѕРЅСЏСЋС‚СЃСЏ
- **Path traversal prevention**: PutFileIntoFolderAction, PutFileIntoPathAction, UnzipAction, DeleteFolderByPathAction, CreateFolderByPathAction, CopyProjectFileToPathAction вЂ” canonical path validation
- **AskGemini2Action.kt**: СѓРґР°Р»С‘РЅ `hostnameVerifier { _, _ -> true }`, РґРѕР±Р°РІР»РµРЅС‹ timeouts, JSONObject РІРјРµСЃС‚Рѕ raw string
- **WriteVariableToFileAction.kt**: `System.getProperty("user.home")` в†’ `Environment.getExternalStoragePublicDirectory`

##  Р§РёСЃС‚РєР° РјСѓСЃРѕСЂР°
- `catroid/src/main/libs/test/` (382 С„Р°Р№Р»Р°) вЂ” СѓРґР°Р»С‘РЅ
- `catroid/src/main/libs/__prebuilt_aar_backup/` вЂ” СѓРґР°Р»С‘РЅ
- `assets/ababuy.txt` вЂ” СѓРґР°Р»С‘РЅ

##  РЎРёСЃС‚РµРјР° СЃР±РѕСЂРєРё
## Desktop Runtime вЂ” code.xml parsing (РІР°Р¶РЅРѕ)

`BakedApkBuilder.kt` РїРёС€РµС‚ `code.xml` С‡РµСЂРµР· `XstreamSerializer` (XStream). РќРѕ **СЂРµР°Р»СЊРЅС‹Р№
`code.xml`, РєРѕС‚РѕСЂС‹Р№ РєР»Р°РґС‘С‚СЃСЏ РІ `project.zip` (NCPP-Р·Р°С€РёС„СЂРѕРІР°РЅРЅС‹Р№ Р±Р°РЅРґР» РїСЂРѕРµРєС‚Р°, СЃРј. РЅРёР¶Рµ),
РёРјРµРµС‚ Р”Р РЈР“РћР™ С„РѕСЂРјР°С‚** вЂ” С‚РѕС‚, С‡С‚Рѕ СЌРєСЃРїРѕСЂС‚РёСЂСѓРµС‚ СЂРµРґР°РєС‚РѕСЂ Catrobat/XStream РїСЂРё СЃРѕС…СЂР°РЅРµРЅРёРё
РїСЂРѕРµРєС‚Р°, Р° РЅРµ С„РѕСЂРјР°С‚ runtime-РєРѕРЅРІРµСЂС‚РµСЂРѕРІ. РџСЂРѕРІРµСЂРµРЅРѕ РїРѕ СЂР°СЃС€РёС„СЂРѕРІР°РЅРЅРѕРјСѓ `project.zip`
РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ (720Г—1600 РїРѕСЂС‚СЂРµС‚РЅС‹Р№ РїСЂРѕРµРєС‚, 4 СЃРїСЂР°Р№С‚Р°: Р¤РѕРЅ + 3 СЂР°Р±РѕС‡РёС…).

### Р РµР°Р»СЊРЅС‹Р№ С„РѕСЂРјР°С‚ `code.xml` (РёР· `project.zip`)

- **РљРѕРґРёСЂРѕРІРєР°**: С„Р°Р№Р» РѕР±СЉСЏРІР»СЏРµС‚ `<?xml ... encoding="UTF-8"?>`, РЅРѕ Р Р•РђР›Р¬РќРћ Р·Р°РїРёСЃР°РЅ РІ
  **CP1251** (РєРёСЂРёР»Р»РёС‡РµСЃРєРёРµ РёРјРµРЅР° СЃРїСЂР°Р№С‚РѕРІ/РїСЂРѕРµРєС‚Р°). `DesktopProjectManager.loadProject`
  РґРµР»Р°РµС‚ `transcodeToUtf8` (UTF-8 decoder + REPORT unmappable в†’ fallback) РїРµСЂРµРґ РїР°СЂСЃРёРЅРіРѕРј.
- **Р’Р»РѕР¶РµРЅРЅР°СЏ СЃС‚СЂСѓРєС‚СѓСЂР°** (РќР• РїР»РѕСЃРєР°СЏ):
  ```
  <program><header screenWidth=.. screenHeight=.. landscapeMode=.. screenMode=..>
    <scenes>
      <scene>
        <objectList>
          <object type="Sprite" name="Р¤РѕРЅ">
            <scriptList/>            <!-- РїСѓСЃС‚РѕР№ РґР»СЏ С„РѕРЅР° -->
          </object>
          <object type="Sprite" name="РџС‚РёС†Р°">
            <scriptList>
              <script type="StartScript" posX="0.0" posY="0.0">
                <brick type="PlaceAtBrick"> ... </brick>
                <brick type="ForeverBrick">
                  <loopBricks> ...РґРµС‚Рё... </loopBricks>
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
  РџР°СЂСЃРµСЂ (`DesktopScriptEngine.parseXmlScripts`) РёС‰РµС‚ СЃРїСЂР°Р№С‚С‹ РІ
  `scenes/scene/objectList/object` СЃ С„РѕР»Р±СЌРєРѕРј РЅР° РїР»РѕСЃРєРёР№ `<object>` (compat).
  РџРѕСЂСЏРґРѕРє СЃРїСЂР°Р№С‚РѕРІ СЃРѕРІРїР°РґР°РµС‚ СЃ `DesktopProjectManager.loadProject` (С‚РѕС‚ Р¶Рµ `spriteEls`).
- **РљРѕРЅС‚РµР№РЅРµСЂРЅС‹Рµ Р±СЂРёРєРё**: РґРµС‚Рё Р»РµР¶Р°С‚ РІ `<loopBricks>` (РµСЃР»Рё РµСЃС‚СЊ), РёРЅР°С‡Рµ СЃС‚Р°СЂС‹Р№
  `findLoopEnd` РїРѕ `</brick>`. Helpers `kidsOf`/`containerEnd` РѕР±СЂР°Р±Р°С‚С‹РІР°СЋС‚ РѕР±Р° СЃР»СѓС‡Р°СЏ.
- **Р¤РѕСЂРјСѓР»С‹**: legacy-РІРёРґ `<formulaList><formula category="X_POSITION"><additionalChildren/>
  <type>NUMBER</type><value>0</value><rightChild>...</rightChild></formula></formulaList>`.
  РќР•Рў `<formulaMap>`/`<formulaTree>` РІ СЌС‚РѕРј РїСЂРѕРµРєС‚Рµ. `getFormulaElement` С‡РёС‚Р°РµС‚ legacy
  `<formula>`, СЂРµРєСѓСЂСЃРёРІРЅРѕ СЃС‚СЂРѕРёС‚ `<formulaElement>` РёР· `type`/`value`/`leftChild`/`rightChild`
  С‡РµСЂРµР· `convertLegacyFormula` (РїРѕРґРґРµСЂР¶РёРІР°РµС‚ РґРµСЂРµРІРѕ OPERATOR, РЅР°РїСЂ. `MINUS`/`RAND`, Рё
  СѓРЅР°СЂРЅС‹Р№ РјРёРЅСѓСЃ С‡РµСЂРµР· `leftChild=null`). РџРѕРґРґРµСЂР¶РёРІР°РµС‚СЃСЏ Рё XStream-РІРёРґ (`<formulaMap>`) С‡РµСЂРµР·
  `convertXStreamFormulaElement`.

### РСЃС‚РѕСЂРёСЏ Р±Р°РіРѕРІ РїР°СЂСЃРёРЅРіР°
- **Р‘РђР“ (РёСЃРїСЂР°РІР»РµРЅ 2026-07, РїРѕРІС‚РѕСЂРЅРѕ 2026-07)**: РїР°СЂСЃРµСЂ РёСЃРєР°Р» `<object>`/`<scriptList>` РЅР°
  РІРµСЂС…РЅРµРј СѓСЂРѕРІРЅРµ Рё legacy `<formulaTree>/<formulaElement>` в†’ РЅРёС‡РµРіРѕ РЅРµ РЅР°С…РѕРґРёР» в†’ СЃРєСЂРёРїС‚С‹ РЅРµ
  СЃРѕР·РґР°РІР°Р»РёСЃСЊ, С„РѕСЂРјСѓР»С‹ `null`. РСЃРїСЂР°РІР»РµРЅРѕ: РІР»РѕР¶РµРЅРЅР°СЏ СЃС‚СЂСѓРєС‚СѓСЂР° `scenes/scene/objectList/object`,
  `loopBricks`, legacy `<formula>` С‡РµСЂРµР· `convertLegacyFormula`, CP1251в†’UTF-8. Р’РµСЂРёС„РёС†РёСЂРѕРІР°РЅРѕ
  РЅР° СЂРµР°Р»СЊРЅРѕРј РїСЂРѕРµРєС‚Рµ: 4 СЃРїСЂР°Р№С‚Р°, 5 СЃРєСЂРёРїС‚РѕРІ (РІРєР». `WhenScript`), С„РѕСЂРјСѓР»С‹ С‡РёС‚Р°СЋС‚СЃСЏ.

Bug B (РёРЅРІРµСЂСЃРёСЏ X РїСЂРё drag): РІ РєРѕРґРµ desktop-СЂР°РЅС‚Р°Р№РјР° РёРЅРІРµСЂСЃРёРё РќР•Рў вЂ” РїСЂРѕРІРµСЂРµРЅРѕ
`DesktopInput.mouseWorldX = mouseX - width/2`, `fingerX = mouseWorldX`, СЂРµРЅРґРµСЂ
`screenX = VIRTUAL_WIDTH/2 + sprite.x`, `goto_touch`/`touch_direction`/СЃРµРЅСЃРѕСЂС‹ `MOUSE_X`/`FINGER_X`.
Р•СЃР»Рё РёРЅРІРµСЂСЃРёСЏ РїРѕРІС‚РѕСЂСЏРµС‚СЃСЏ РїРѕСЃР»Рµ С„РёРєСЃР° С„РѕСЂРјСѓР» вЂ” РЅСѓР¶РµРЅ РєРѕРЅРєСЂРµС‚РЅС‹Р№ РїСЂРѕРµРєС‚/Р±Р»РѕРєРё РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ.

## Desktop Runtime вЂ” РїСЂРѕРµРєС‚РЅС‹Р№ Р±Р°РЅРґР» (`project.zip` / NCPP / NCPW)

- `desktop-runtime/project.zip` вЂ” Р·Р°С€РёС„СЂРѕРІР°РЅРЅС‹Р№ Р±Р°РЅРґР» РїСЂРѕРµРєС‚Р°, РєРѕС‚РѕСЂС‹Р№ РєР»Р°РґС‘С‚СЃСЏ СЂСЏРґРѕРј СЃ
  `NeoCatroid.exe` (71 РљР‘ launch4j-Р»Р°СѓРЅС‡РµСЂ + РІРЅРµС€РЅРёР№ `player.jar`).
- Р¤РѕСЂРјР°С‚: magic `NCPP` (4E 43 50 50) = AES-256-GCM + PBKDF2. Layout: `NCPP`(4) + salt(32) +
  IV(12) + ciphertext. РџР°СЂРѕР»СЊ С…СЂР°РЅРёС‚СЃСЏ РІ РєРѕРЅСЃС‚Р°РЅС‚Рµ `DesktopStage.PAYLOAD_PASSWORD`
  (С‚РѕС‚ Р¶Рµ, С‡С‚Рѕ РІ Android `ProtectedProjectPayload.PASSWORD`; СЃР°Рј Р»РёС‚РµСЂР°Р» РІ РґРѕРєСѓРјРµРЅС‚Р°С†РёРё РЅРµ РїСЂРёРІРѕРґРёС‚СЃСЏ).
- **РќРѕРІС‹Рµ СЃР±РѕСЂРєРё (EXE, ProjectOptionsFragment.buildExe)**: `project.zip` (entry win-Р±Р°РЅРґР»Р°)
  С‚РµРїРµСЂСЊ РѕР±С‘СЂРЅСѓС‚ РІ РєРѕРЅС‚РµР№РЅРµСЂ **NCPW** СЃ Р РђРќР”РћРњРќР«Рњ РїР°СЂРѕР»РµРј РЅР° РєР°Р¶РґСѓСЋ СЃР±РѕСЂРєСѓ:
  `NCPW`(4) + pwdLen(int32 BE) + password(UTF-8) + NCPS-РїРѕС‚РѕРє.
  - Android-СЃС‚РѕСЂРѕРЅР°: `ProjectCrypto.writePasswordContainerHeader(out, password)` +
    `ProjectCrypto.generateRandomPassword()` (16 random bytes в†’ hex) Р·Р°С‚РµРј
    `encryptDirectoryToStreamChunked(..., password)`.
  - Desktop-СЃС‚РѕСЂРѕРЅР°: `DesktopStage.readPasswordContainer()` РґРѕСЃС‚Р°С‘С‚ РїР°СЂРѕР»СЊ РёР· РєРѕРЅС‚РµР№РЅРµСЂР°,
    СЂР°СЃС€РёС„СЂРѕРІС‹РІР°РµС‚ РёРј РІР»РѕР¶РµРЅРЅС‹Р№ NCPS/NCPP; Р±РµР· РјР°РіРёРё `NCPW` вЂ” Р»РµРіР°СЃРё-РєРѕРЅСЃС‚Р°РЅС‚Р° (backward-compat).
  - `build_exe.bat`/`embed_payload.ps1` РјРµРЅСЏС‚СЊ РќР• РЅР°РґРѕ вЂ” РїР°СЂРѕР»СЊ СѓР¶Рµ РІРЅСѓС‚СЂРё `project.zip`,
    С„СѓС‚РµСЂ NEOCAT01 РїСЂРѕСЃС‚Рѕ РїРµСЂРµРЅРѕСЃРёС‚ РµРіРѕ С†РµР»РёРєРѕРј.
  - Baked APK (`BakedApkBuilder`/`AlignedApkBuilder`) СѓР¶Рµ РґР°РІРЅРѕ РіРµРЅРµСЂРёСЂСѓСЋС‚ СЃР»СѓС‡Р°Р№РЅС‹Р№ РїР°СЂРѕР»СЊ
    РЅР° РєР°Р¶РґСѓСЋ СЃР±РѕСЂРєСѓ Рё РєР»Р°РґСѓС‚ РµРіРѕ РІ Р°СЃСЃРµС‚ `neocatroid.key`; РєРѕРЅСЃС‚Р°РЅС‚Р° РѕСЃС‚Р°С‘С‚СЃСЏ С‚РѕР»СЊРєРѕ
    legacy-С„РѕР»Р±СЌРєРѕРј (RuntimeLoaderActivity/PayloadDecryptor).
- `DesktopStage.extractPayload()` РїСЂРѕРІРµСЂСЏРµС‚ РјР°РіРёСЋ `NCPP` (РёР»Рё `NCPW`) Рё СЂР°СЃС€РёС„СЂРѕРІС‹РІР°РµС‚; РЅРµС‚ РјР°РіРёРё в†’
  РіСЂСѓР·РёС‚ РєР°Рє РѕР±С‹С‡РЅС‹Р№ zip (backward-compat).
- **Р’Р°Р¶РЅРѕ РґР»СЏ СЃР±РѕСЂРєРё EXE**: `build_exe.bat` РЅР° С€Р°РіРµ staging СѓРґР°Р»СЏРµС‚ Р’РЎР• РїР°РїРєРё РІ РєРѕСЂРЅРµ
  `desktop-runtime`, РєСЂРѕРјРµ `icon`/`jre` (РІ С‚.С‡. `src`!). Р—Р°РїСѓСЃРєР°С‚СЊ РїРѕРІС‚РѕСЂРЅРѕ С‚РѕР»СЊРєРѕ РџРћРЎР›Р•
  `git checkout -- desktop-runtime/src` Рё РІРѕСЃСЃС‚Р°РЅРѕРІР»РµРЅРёСЏ РїРѕР»РЅРѕРіРѕ `launch4j`.
- Р”Р»СЏ РїСЂР°РІРєРё СЂР°РЅС‚Р°Р№РјР° РґРѕСЃС‚Р°С‚РѕС‡РЅРѕ РїРµСЂРµСЃРѕР±СЂР°С‚СЊ `player.jar` (`./gradlew :desktop-runtime:jar
  --offline`) Рё РїРѕР»РѕР¶РёС‚СЊ СЂСЏРґРѕРј СЃ `NeoCatroid.exe` (dontWrapJar=true в†’ EXE Р±РµСЂС‘С‚ jar СЃРЅР°СЂСѓР¶Рё).
  РџРµСЂРµРІС‹РїР°РєРѕРІС‹РІР°С‚СЊ EXE РќР• РЅСѓР¶РЅРѕ.

## Desktop Runtime вЂ” Letterbox (2026-07)

- РџСЂРѕРµРєС‚С‹ СЃ СЃРѕРѕС‚РЅРѕС€РµРЅРёРµРј СЃС‚РѕСЂРѕРЅ, РѕС‚Р»РёС‡РЅС‹Рј РѕС‚ РѕРєРЅР° (РЅР°РїСЂ. РІРµСЂС‚РёРєР°Р»СЊРЅС‹Р№ 720Г—1600 РІ РѕРєРЅРµ
  1280Г—720), РќР• СЂР°СЃС‚СЏРіРёРІР°СЋС‚СЃСЏ вЂ” РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ `FitViewport(virtualWidth, virtualHeight)` +
  С‡С‘СЂРЅР°СЏ Р·Р°Р»РёРІРєР° `ScreenUtils.clear(0,0,0,1)` РІРЅРµ viewport. Р РµР·СѓР»СЊС‚Р°С‚: С‡С‘СЂРЅС‹Рµ РїРѕР»РѕСЃС‹ СЃРЅР°СЂСѓР¶Рё,
  СЃС†РµРЅР° РїРѕ С†РµРЅС‚СЂСѓ (В«С‡С‘СЂРЅС‹Рµ РїРѕР»РѕСЃС‹ РґРѕ С„РѕРЅР°В»).
- `virtualWidth`/`virtualHeight` Р±РµСЂСѓС‚СЃСЏ РёР· `code.xml` (`screenWidth`/`screenHeight` РІ
  `<header>`), РїРѕР»СЏ РґРѕР±Р°РІР»РµРЅС‹ РІ `DesktopProject`, Р·Р°РїРѕР»РЅСЏСЋС‚СЃСЏ РІ `DesktopProjectManager`.
  Р”РµС„РѕР»С‚ 1280Г—720 (РµСЃР»Рё РїСЂРѕРµРєС‚ РЅРµ Р·Р°РґР°Р»).
- РљРѕРѕСЂРґРёРЅР°С‚С‹ СЃРїСЂР°Р№С‚РѕРІ (`screenX = VW/2 + x`), HUD-С‚РµРєСЃС‚ Рё overlay-Р±Р°Р±Р»РёРєРё (think/say)
  РёСЃРїРѕР»СЊР·СѓСЋС‚ Р»РѕРєР°Р»СЊРЅС‹Рµ `VW`/`VH` РёР· РїСЂРѕРµРєС‚Р°, РќР• С…Р°СЂРґРєРѕРґ 1280Г—720.

## Desktop Runtime вЂ” С‚РѕСЂРјРѕР·Р° СЃС‚Р°СЂС‚Р° (РёСЃРїСЂР°РІР»РµРЅРѕ 2026-07)

- Р•РґРёРЅСЃС‚РІРµРЅРЅС‹Р№ РЅРµРѕРіСЂР°РЅРёС‡РµРЅРЅС‹Р№ СЃРµС‚РµРІРѕР№ РІС‹Р·РѕРІ Р±С‹Р» `askGeminiApi` (DesktopScriptEngine.kt) вЂ”
  Р±РµР· `connectTimeout`/`readTimeout`. РџСЂРё СЃС‚Р°СЂС‚РѕРІРѕРј Р±Р»РѕРєРµ Ask Gemini Рё РЅРµРґРѕСЃС‚СѓРїРЅРѕСЃС‚Рё СЃРµС‚Рё
  РїРѕС‚РѕРє РІРёСЃРµР» РЅР° TCP/DNS-С‚Р°Р№РјР°СѓС‚Рµ РћРЎ 5вЂ“20 РјРёРЅ. РСЃРїСЂР°РІР»РµРЅРѕ: `connectTimeout = readTimeout =
  15_000`. Р’СЃРµ РѕСЃС‚Р°Р»СЊРЅС‹Рµ СЃРµС‚РµРІС‹Рµ РїСѓС‚Рё СѓР¶Рµ РѕРіСЂР°РЅРёС‡РµРЅС‹ (WebRequest=10СЃ, firebase=5СЃ).
- EXE/player.jar РїРѕСЃР»Рµ С„РёРєСЃР° СЃС‚Р°СЂС‚СѓРµС‚ РјРіРЅРѕРІРµРЅРЅРѕ (РїСЂРѕРІРµСЂРµРЅРѕ: Р»РѕРі РїРѕСЏРІР»СЏРµС‚СЃСЏ СЃСЂР°Р·Сѓ).

## РЎР±РѕСЂРєР°/Р·Р°РІРёСЃРёРјРѕСЃС‚Рё (РѕР±РЅРѕРІР»РµРЅРѕ 2026-07)
- Coroutines unified to 1.7.3 в†’ 1.9.0
- material:1.2.1 в†’ 1.13.0 в†’ 1.14.0, removed resolutionStrategy force
- Gradle: `-Xmx6g` в†’ `-Xmx4g`
- РЈРґР°Р»С‘РЅ РґСѓР±Р»РёСЂСѓСЋС‰РёР№СЃСЏ `apksig:7.0.0`
- Р”СѓР±Р»РёРєР°С‚С‹ `configurations { pluginLibs }` в†’ РѕР±СЉРµРґРёРЅРµРЅС‹
- Р”СѓР±Р»РёРєР°С‚С‹ `packagingOptions` в†’ РѕР±СЉРµРґРёРЅРµРЅС‹ (pickFirsts, excludes)
- `ext.useAndroidLocales` в†’ РёСЃРїСЂР°РІР»РµРЅ СЃРёРЅС‚Р°РєСЃРёСЃ (closure РІРјРµСЃС‚Рѕ СЃР»РѕРјР°РЅРЅРѕРіРѕ)
- `testCoverageEnabled` в†’ `enableUnitTestCoverage` (deprecation)
- Р”СѓР±Р»РёСЂСѓСЋС‰РёР№СЃСЏ `kotlin-stdlib` Рё РјС‘СЂС‚РІС‹Р№ РєРѕРґ СѓРґР°Р»РµРЅС‹

##  NeoPaint
- **Layout**: `activity_neopaint.xml` вЂ” РїРѕС‡РёРЅРµРЅРѕ РїРѕР·РёС†РёРѕРЅРёСЂРѕРІР°РЅРёРµ (action_bar/toolbar/layers_panel/property_bar)
- **Save**: `saveAndReturn()` вЂ” РїСЂРё `picturePath == null` СЃРѕС…СЂР°РЅСЏРµС‚ РІРѕ РІСЂРµРјРµРЅРЅС‹Р№ С„Р°Р№Р» Рё РІРѕР·РІСЂР°С‰Р°РµС‚ `RESULT_OK`
- **UI**: `setupToolbars()` вЂ” ImageButton + setSelected() + РїРѕРґСЃРІРµС‚РєР° Р°РєС‚РёРІРЅРѕРіРѕ РёРЅСЃС‚СЂСѓРјРµРЅС‚Р°
- **Icons**: 17 vector drawable РёРєРѕРЅРѕРє + selector'С‹ (tool_button_bg, action_button_bg)
- **Labels**: РґРѕР±Р°РІР»РµРЅС‹ `lbl_brush_size` / `lbl_opacity`
- **State**: `onSaveInstanceState`/`onRestoreInstanceState` РґР»СЏ РїРѕРІРѕСЂРѕС‚Р° СЌРєСЂР°РЅР°
- **DrawingView**: СѓРґР°Р»РµРЅС‹ РґСѓР±Р»РёСЂСѓСЋС‰РёРµ `max()`/`min()`, `smudgeSrc = null` РІ `ACTION_UP`, PorterDuff.Mode.CLEAR в†’ BlendModeColorFilter (API 29+) СЃ fallback
- **Dialog**: `text_dialog` вЂ” AlertDialog.setPositiveButton() (СѓСЃС‚Р°СЂРµРІС€РµРµ create().apply)

##  РўРµСЃС‚С‹ (21 С„Р°Р№Р» РґР»СЏ 11 РЅРѕРІС‹С… Р±Р»РѕРєРѕРІ)
### Brick tests (РІРµСЂРёС„РёРєР°С†РёСЏ addActionToSequence):
1. `PutFileIntoFolderBrickTest.java`
2. `PutFileIntoPathBrickTest.java`
3. `SendNotificationBrickTest.java`
4. `ShowScheduledNotificationBrickTest.java`
5. `NotificationActionBrickTest.java`
6. `PrepareNotificationBrickTest.java`
6a. `SetRagdollBrickTest.java` вЂ” 12 С‚РµСЃС‚РѕРІ (brick wiring, action, formula)
7. `TouchDirectionBrickTest.java`
8. `DeleteCloneByNumberBrickTest.java`
9. `ExecuteForCloneNumberBrickTest.java` (+ composite brick С‚РµСЃС‚С‹)
10. `AttachToCameraWithOffsetBrickTest.java`

### Action tests (unit + PowerMock):
1. `PrepareNotificationActionTest.kt` вЂ” РїСЂРѕРІРµСЂРєР° NotificationStorage
2. `NotificationActionActionTest.kt` вЂ” РїСЂРѕРІРµСЂРєР° addAction/execution guard
3. `ExecuteForCloneNumberActionTest.java` вЂ” cloneIndex matching/restart
4. `TouchDirectionActionTest.java` вЂ” PowerMock(TouchUtil), 8 С‚РµСЃС‚РѕРІ
5. `DeleteCloneByNumberActionTest.kt` вЂ” PowerMock(StageActivity)
6. `AttachToCameraWithOffsetActionTest.java` вЂ” PowerMock(StageActivity, SceneManager)
7. `PutFileIntoFolderActionTest.java` вЂ” PowerMock(Environment, TemporaryFolder)
8. `PutFileIntoPathActionTest.java` вЂ” PowerMock(Environment, TemporaryFolder + path traversal)
9. `SendNotificationActionTest.kt` вЂ” PowerMock(StageActivity, NotificationStorage)
10. `ShowScheduledNotificationActionTest.kt` вЂ” PowerMock(StageActivity, NotificationStorage)

##  РСЃРїСЂР°РІР»РµРЅРёРµ pre-existing РѕС€РёР±РѕРє РєРѕРјРїРёР»СЏС†РёРё
### Java (main):
- `ShowColorPickerFormulaEditorStrategy.java` вЂ” РґРѕР±Р°РІР»РµРЅ РёРјРїРѕСЂС‚ `FragmentManager`, `value -> { ... return null; }` РґР»СЏ Kotlin `Unit`
- `FormulaEditorFragment.java` вЂ” С‚Рѕ Р¶Рµ РёСЃРїСЂР°РІР»РµРЅРёРµ lambda return
- `UiUtils.java` вЂ” РґРѕР±Р°РІР»РµРЅ `R.string.menu_rate_us` (РѕС‚СЃСѓС‚СЃС‚РІРѕРІР°Р»)
### Kotlin (tests):
- `ObjectDetectorOnSuccessListener` вЂ” СЃРѕР·РґР°РЅ РЅРµРґРѕСЃС‚Р°СЋС‰РёР№ РєР»Р°СЃСЃ РІ `camera/mlkitdetectors/`
- `DetectedObject` stub вЂ” РґРѕР±Р°РІР»РµРЅС‹ РєРѕРЅСЃС‚СЂСѓРєС‚РѕСЂС‹ `(Rect, Int, List<Label>)` + `Label(String, Float, Int)`
- РњРѕРё 20 С‚РµСЃС‚РѕРІ вЂ” РёСЃРїСЂР°РІР»РµРЅС‹: `ScriptSequenceAction(null)` в†’ mock, missing imports, `SequenceAction` в†’ `ScriptSequenceAction`

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
- XStreamFormulaElementConverter: fixed SECOND_FACE_Y_POSITION в†’ FACE_Y sensor mapping

## РџСЂРѕС‡РµРµ
- ActionFactory: `RunShellAction()` в†’ `runShellAction()` (Java naming convention)
- PanoramicConverter: uncommented `fbo.dispose()`
- ErrorInterceptor.kt: `response.body?.toString()` в†’ `response.body?.string()`
- Removed duplicate commented `package` lines from 14 action files
- Removed duplicate `createDeleteCloneByNumberAction` from ActionFactory
- Р”РѕР±Р°РІР»РµРЅС‹ missing resources: `cancel_button_text`, `import_step_prepare`, `menu_rate_us`, `ic_pocketpaint_tool_resize_adjust`

##  РћР±РЅРѕРІР»РµРЅРёРµ Р·Р°РІРёСЃРёРјРѕСЃС‚РµР№ (2026-07)
| Р—Р°РІРёСЃРёРјРѕСЃС‚СЊ | Р‘С‹Р»Рѕ | РЎС‚Р°Р»Рѕ |
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
| **РќРµ РѕР±РЅРѕРІР»РµРЅРѕ** (high risk): Koin 2.1.6, CameraX 1.0.0-beta07, Mockito 3.12.4 (Р·Р°Р±Р»РѕРєРёСЂРѕРІР°РЅ PowerMock)

---

# Windows Desktop Player (build_exe)

## РњРѕРґСѓР»Рё

- `:core` вЂ” JVM-РјРѕРґСѓР»СЊ СЃ РїРѕСЂС‚Р°С‚РёРІРЅС‹РјРё seam-РёРЅС‚РµСЂС„РµР№СЃР°РјРё (RuntimeServices, AudioService, MidiService, TextService, NotificationService) Рё holder-Р°РјРё.
- `:desktop-runtime` вЂ” JVM-РјРѕРґСѓР»СЊ СЃ Desktop-СЂРµР°Р»РёР·Р°С†РёСЏРјРё seam (DesktopAudioService, DesktopMidiService, DesktopTextService, DesktopNotificationService, DesktopRuntimeServices) Рё С‚РѕС‡РєРѕР№ РІС…РѕРґР° DesktopStage.
- `:catroid` (Android) вЂ” СЃРѕРґРµСЂР¶РёС‚ РєРЅРѕРїРєСѓ **В«РЎРѕР±СЂР°С‚СЊ EXEВ»** РІ ProjectOptionsFragment.

## UI: РљРЅРѕРїРєР° "РЎРѕР±СЂР°С‚СЊ EXE" (ProjectOptionsFragment)

- **Layout**: `fragment_project_options.xml` в†’ `@id/project_options_build_exe`
- **Kotlin**: `setupBuildExeOption()` (СѓР¶Рµ Р±С‹Р»Р°) + `buildExe()` (СЂРµР°Р»РёР·РѕРІР°РЅР° 2026-07)
- **Р§С‚Рѕ РґРµР»Р°РµС‚**:
  1. РЎРѕС…СЂР°РЅСЏРµС‚ РїСЂРѕРµРєС‚
  2. РџР°РєСѓРµС‚ РїСЂРѕРµРєС‚ РІ `{projectName}.zip`
  3. **РЁРёС„СЂСѓРµС‚** zip С‚РµРј Р¶Рµ `ProjectCrypto.encrypt` (AES-256-GCM + PBKDF2, РїР°СЂРѕР»СЊ `ProtectedProjectPayload.PASSWORD` вЂ” РєР°Рє РІ Baked APK) в†’ `{projectName}.enc`
  4. РќР°С…РѕРґРёС‚ РёРєРѕРЅРєСѓ РїСЂРѕРµРєС‚Р° (`manual_screenshot.png` РёР»Рё `automatic_screenshot.png`)
  5. Р”РѕР±Р°РІР»СЏРµС‚ РІ `{projectName}_win.zip`: Р·Р°С€РёС„СЂРѕРІР°РЅРЅС‹Р№ РїСЂРѕРµРєС‚ (entry `project.zip`), `template_win.zip` (РёР· assets), `build_exe.bat` (РёР· assets), `icon.png`, README_WINDOWS.txt
  6. РЎРѕР·РґР°С‘С‚ РёС‚РѕРіРѕРІС‹Р№ `{projectName}_win.zip` Рё РѕС‚РєСЂС‹РІР°РµС‚ share-РґРёР°Р»РѕРі
- **РџРµСЂРµРІРѕРґС‹**: `project_options_build_exe` (values + values-ru)

## Gradle: copyDesktopTemplate

- `catroid/build.gradle` вЂ” Р·Р°РґР°С‡Р° `copyDesktopTemplate`, РєРѕРїРёСЂСѓРµС‚ `template_win.zip` Рё `build_exe.bat` РёР· `desktop-runtime/` РІ `catroid/src/main/assets/`.
- РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё Р·Р°РїСѓСЃРєР°РµС‚СЃСЏ РїРµСЂРµРґ mergeAssets.

## build_exe.bat (Windows, launch4j)

- Р›РµР¶РёС‚ РІ `desktop-runtime/build_exe.bat`
- **РЁРёС„СЂРѕРІР°РЅРёРµ РїСЂРѕРµРєС‚Р°**: С‚РµР»РµС„РѕРЅ РєР»Р°РґС‘С‚ СѓР¶Рµ Р·Р°С€РёС„СЂРѕРІР°РЅРЅС‹Р№ `project.zip` (AES-256-GCM, РјР°РіРёСЏ `NCPP`). `DesktopStage.extractPayload()` РїСЂРё СЃС‚Р°СЂС‚Рµ РїСЂРѕРІРµСЂСЏРµС‚ РјР°РіРёСЋ `NCPP` Рё СЂР°СЃС€РёС„СЂРѕРІС‹РІР°РµС‚ С‚РµРј Р¶Рµ РїР°СЂРѕР»РµРј `ProtectedProjectPayload.PASSWORD`; РµСЃР»Рё РјР°РіРёРё РЅРµС‚ вЂ” РіСЂСѓР·РёС‚ РєР°Рє РѕР±С‹С‡РЅС‹Р№ zip (РѕР±СЂР°С‚РЅР°СЏ СЃРѕРІРјРµСЃС‚РёРјРѕСЃС‚СЊ СЃРѕ СЃС‚Р°СЂС‹РјРё/РЅРµС€РёС„СЂРѕРІР°РЅРЅС‹РјРё РїСЂРѕРµРєС‚Р°РјРё).
- **РђРІС‚РѕРЅРѕРјРЅРѕСЃС‚СЊ**: `template_win.zip` СЃРѕР±РёСЂР°РµС‚СЃСЏ СЃ **РІС€РёС‚С‹Рј JRE** (`jre/`) Рё **РІС€РёС‚С‹Рј launch4j** (`launch4j/`). `build_exe.bat` РёС‰РµС‚ launch4j РІ РїРѕСЂСЏРґРєРµ `%LAUNCH4J_HOME%` в†’ `%ROOT%launch4j\` в†’ СЂР°СЃРїР°РєРѕРІР°РЅРЅС‹Р№ С€Р°Р±Р»РѕРЅ `build\win-dist\bundle\launch4j\`, РїРѕСЌС‚РѕРјСѓ РєРѕРЅРµС‡РЅРѕРјСѓ РїРѕР»СЊР·РѕРІР°С‚РµР»СЋ РЅРёС‡РµРіРѕ РїРѕРґРєР»Р°РґС‹РІР°С‚СЊ РЅРµ РЅР°РґРѕ.
- РЎРѕР±РёСЂР°РµС‚ `player.jar` (РёР»Рё Р±РµСЂС‘С‚ РёР· С€Р°Р±Р»РѕРЅР°) в†’ РІСЃС‚СЂР°РёРІР°РµС‚ `project.zip` РєР°Рє NEOCAT01-footer в†’ РєРѕРЅРІРµСЂС‚РёСЂСѓРµС‚ PNG РІ ICO в†’ РїСЂРё РЅР°Р»РёС‡РёРё launch4j РІ С€Р°Р±Р»РѕРЅРµ СЃРѕР·РґР°С‘С‚ `NeoCatroid.exe` (СЃ Р±Р°РЅРґР»РѕРј `jre/`), РёРЅР°С‡Рµ `NeoCatroid.bat`.
- РЁР°Рі СѓРїР°РєРѕРІРєРё С€Р°Р±Р»РѕРЅР° (`template_win.zip`) РєРѕРїРёСЂСѓРµС‚ `launch4j/` РёР· `desktop-runtime\launch4j\` РІ bundle, С‡С‚РѕР±С‹ launch4j РїРѕРїР°Р» РІ Р°СЃСЃРµС‚С‹ Android-РїР°РєРµС‚Р°.

###  РР·РІРµСЃС‚РЅС‹Р№ СЂРµРіСЂРµСЃСЃРёРѕРЅРЅС‹Р№ Р±Р°Рі СЃР±РѕСЂРєРё EXE (2026-07)
`git`-РІРµСЂСЃРёСЏ `desktop-runtime/launch4j/` **РќР•РџРћР›РќРђРЇ** вЂ” РІ РЅРµР№ РЅРµС‚ `lib/` (xstream.jar Рё С‚.Рї.),
`bin/` (windres.exe/ld.exe), `head/` (guihead.o, head.o) Рё `w32api/` (crt2.o Рё MinGW .a).
РџРѕСЌС‚РѕРјСѓ `launch4jc.exe`/`launch4j.exe` РїР°РґР°СЋС‚ (NoClassDefFoundError в†’ Р·Р°С‚РµРј
"cannot find crt2.o / guihead.o"). РџРѕР»РЅС‹Р№ `launch4j` Р»РµР¶РёС‚ РІ `template_win.zip`
(`build/win-dist/bundle/launch4j/`) Рё РІ `C:\Users\ivanp\Downloads\launch4j-3.50-win32\launch4j\`.
**Р РµС€РµРЅРёРµ**: СЃРєРѕРїРёСЂРѕРІР°С‚СЊ `lib/`,`bin/`,`head/`,`w32api/` РёР· РїРѕР»РЅРѕРіРѕ launch4j РІ `desktop-runtime/launch4j/`.
Р”РѕРїРѕР»РЅРёС‚РµР»СЊРЅРѕ: `build_exe.bat` РЅР° С€Р°РіРµ staging (6a) СѓРґР°Р»СЏРµС‚ Р’РЎР• РїР°РїРєРё РІ РєРѕСЂРЅРµ `desktop-runtime`,
РєСЂРѕРјРµ `icon`/`jre` вЂ” РІ С‚.С‡. СѓРґР°Р»СЏРµС‚ СЃР°Рј `launch4j` Рё `build`. Р—Р°РїСѓСЃРєР°С‚СЊ РїРѕРІС‚РѕСЂРЅРѕ С‚РѕР»СЊРєРѕ РџРћРЎР›Р•
РІРѕСЃСЃС‚Р°РЅРѕРІР»РµРЅРёСЏ `launch4j` (РЅР°РїСЂРёРјРµСЂ, `git checkout -- desktop-runtime/launch4j` + РґРѕР»РёС‚СЊ РЅРµРґРѕСЃС‚Р°СЋС‰РёРµ РїР°РїРєРё).
Р”Р»СЏ headless-СЃР±РѕСЂРєРё РёСЃРїРѕР»СЊР·РѕРІР°С‚СЊ РљРћРќРЎРћР›Р¬РќР«Р™ `launch4jc.exe`, Р° РЅРµ GUI `launch4j.exe`
(GUI РјРѕР¶РµС‚ РЅРµ РїРѕРґРЅСЏС‚СЊСЃСЏ Р±РµР· desktop-СЃРµСЃСЃРёРё). РРєРѕРЅРєСѓ РІ `launch4j.xml` Р·Р°РґР°РІР°С‚СЊ РђР‘РЎРћР›Р®РўРќР«Рњ РїСѓС‚С‘Рј
(launch4j СЂРµР·РѕР»РІРёС‚ icon РѕС‚РЅРѕСЃРёС‚РµР»СЊРЅРѕ С„Р°Р№Р»Р° xml, Р° РЅРµ exe).

## Windows Desktop Player V2 вЂ” РµРґРёРЅС‹Р№ EXE С‡РµСЂРµР· WebView2 (2026-08)

РђР»СЊС‚РµСЂРЅР°С‚РёРІР° build_exe: РќР• РЅСѓР¶РµРЅ launch4j/JRE/batch-РїР°Р№РїР»Р°Р№РЅ. РћРґРёРЅ `NeoCatroid.exe`
(~176 РљР‘, C#/.NET Framework 4.8 stub) + footer `NEOCAT01` в†’ WebView2 РѕС‚РєСЂС‹РІР°РµС‚
`app.html` Рё РёСЃРїРѕР»РЅСЏРµС‚ РїСЂРѕРµРєС‚ РІ Р±СЂР°СѓР·РµСЂРµ. РЎР±РѕСЂРєР° С†РµР»РёРєРѕРј РЅР° С‚РµР»РµС„РѕРЅРµ, Р±РµР· Windows.

### Р¤РѕСЂРјР°С‚ С„Р°Р№Р»Р°
```
[NeoCatroid.exe (stub)] [web.zip] [size: Int64 LE (8 Р±Р°Р№С‚)] [NEOCAT01 (8 Р±Р°Р№С‚)]
```
- Footer С‡РёС‚Р°РµС‚ `NeoCatroidStub.cs` (`ReadFooterPayload`, РґРёР°РіРЅРѕСЃС‚РёРєР° `--check-footer <file>`).
- `web.zip`: `app.html`, `player.js`, `title.txt` (РёРјСЏ РїСЂРѕРµРєС‚Р°), `project.pak`.
- `project.pak` = **NCPW**-РєРѕРЅС‚РµР№РЅРµСЂ (`NCPW` + len(BE) + password(UTF-8) + РІР»РѕР¶РµРЅРЅС‹Р№ РїРѕС‚РѕРє)
  СЃРѕ РЎР›РЈР§РђР™РќР«Рњ РїР°СЂРѕР»РµРј РЅР° РєР°Р¶РґСѓСЋ СЃР±РѕСЂРєСѓ (`ProjectCrypto.generateRandomPassword()`);
  РІРЅСѓС‚СЂРё вЂ” **NCPP** (AES-256-GCM + PBKDF2 100000) РѕС‚ zip-РїСЂРѕРµРєС‚Р° (code.xml + images/ + sounds/,
  `undo_code.xml` РёСЃРєР»СЋС‡Р°РµС‚СЃСЏ). РЎС‚Р°СЂС‹Р№ `ProtectedProjectPayload.PASSWORD` РќР• РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ.

### Р¤Р°Р№Р»С‹
```
desktop-runtime/webview2_stub/
  NeoCatroidStub.cs        вЂ” C# stub (WinForms + WebView2), С‚РµРјР° РїСЂРёР»РѕР¶РµРЅРёСЏ = title.txt
  build_stub.bat           вЂ” СЃР±РѕСЂРєР° csc.exe (РЅСѓР¶РµРЅ С‚РѕР»СЊРєРѕ .NET Framework 4.8)
  NeoCatroid.exe           вЂ” РіРѕС‚РѕРІС‹Р№ stub (~176 РљР‘)
  lib/                     вЂ” Microsoft.Web.WebView2 (net462 + WebView2Loader.dll РІ СЂРµСЃСѓСЂСЃР°С…)
  player/
    player.js              вЂ” WebView2-СЂР°РЅС‚Р°Р№Рј: NCXml/NCZip/NCCrypto/NCEngine/NCBoot (~63 РљР‘)
    app.html               вЂ” bootstrap
    player_test.js         вЂ” 61 Node-С‚РµСЃС‚ (node player_test.js)
catroid/src/main/assets/exe_v2/   вЂ” NeoCatroid.exe, player.js, app.html (РєРѕРїРёРё РґР»СЏ Android)
catroid/src/main/java/org/catrobat/catroid/exebuildv2/ExeBuilderV2.kt вЂ” СЃР±РѕСЂРєР° РЅР° С‚РµР»РµС„РѕРЅРµ
catroid/.../ui/fragment/ProjectOptionsFragment.kt вЂ” РїСѓРЅРєС‚ В«Windows EXE v2В» РІ export-РјРµРЅСЋ (РёРЅРґРµРєСЃ 5)
catroid/src/test/.../robolectric/exebuildv2/ExeBuilderV2FooterTest.kt вЂ” 3 Robolectric-С‚РµСЃС‚Р°
```

### player.js (WebView2-СЂР°РЅС‚Р°Р№Рј)
- **NCXml** вЂ” XML-РїР°СЂСЃРµСЂ (attr/children/text, РґРµРєР»Р°СЂР°С†РёРё, CDATA; CP1251-fallback РґРµРєРѕРґРёСЂСѓРµС‚
  С‚РѕР»СЊРєРѕ РїСЂРё РѕС€РёР±РєРµ UTF-8).
- **NCZip** вЂ” zip-СЂРёРґРµСЂ РїРѕ С†РµРЅС‚СЂР°Р»СЊРЅРѕРјСѓ РєР°С‚Р°Р»РѕРіСѓ (РјРµС‚РѕРґС‹ 0/store Рё 8/deflate).
- **NCCrypto** вЂ” NCPW-РєРѕРЅС‚РµР№РЅРµСЂ + NCPP/NCPX (РѕРґРёРЅРѕС‡РЅС‹Р№) Рё **NCPS** (СЃРµРіРјРµРЅС‚РЅС‹Р№ РїРѕС‚РѕРє,
  Р·РµСЂРєР°Р»Рѕ `ChunkedGcmOutputStream`: `NCPS` + salt + ivPrefix(8), СЃРµРіРјРµРЅС‚С‹
  `len(BE) + ct`, iv = ivPrefix + BE32-РёРЅРґРµРєСЃ). WebCrypto (PBKDF2 100000, AES-GCM 256).
- **NCEngine** вЂ” РїР°СЂСЃРµСЂ code.xml (РІР»РѕР¶РµРЅРЅС‹Рµ `scenes/scene/objectList/object`, XStream
  `formulaList/entry/key|value` Рё legacy `formula category`, РєРѕРЅС‚РµР№РЅРµСЂС‹ С‡РµСЂРµР·
  `loopBricks`/`ifBricks`/`elseBricks`) + РёРЅС‚РµСЂРїСЂРµС‚Р°С‚РѕСЂ:
  stack-С„СЂРµР№РјС‹ (repeat/forever/if/broadcast_wait/wait/glide), СЃРµРЅСЃРѕСЂС‹, РїРµСЂРµРјРµРЅРЅС‹Рµ.
- **Boot**: `fetch('project.pak')` в†’ NCPWв†’РґРµС€РёС„СЂРѕРІРєР° в†’ zip в†’ `code.xml` + images/ (ImageBitmap)
  + sounds/ (Audio-СЌР»РµРјРµРЅС‚С‹), СЂРµРЅРґРµСЂ РІ canvas (letterbox FitViewport), pointer-РІРІРѕРґ.

### Scenes РІ player.js (2026-08)

РџРѕР»РЅРѕС†РµРЅРЅР°СЏ СЃС†РµРЅРѕРІР°СЏ РјРѕРґРµР»СЊ (РїРѕ РѕР±СЂР°Р·С†Сѓ Android StageListener):

- `loadXml` РіСЂСѓРїРїРёСЂСѓРµС‚ РѕР±СЉРµРєС‚С‹ РїРѕ `<scene name="...">` РІ `this.scenes` (fallback: РїР»РѕСЃРєРёР№
  `<objectList>`/`<object>` = РѕРґРЅР° СЃС†РµРЅР° СЃ РїСѓСЃС‚С‹Рј РёРјРµРЅРµРј). РђРєС‚РёРІРЅР° РўРћР›Р¬РљРћ РїРµСЂРІР°СЏ СЃС†РµРЅР°:
  `this.sprites = buildSceneSprites(0)` вЂ” РѕР±СЉРµРєС‚С‹ РѕСЃС‚Р°Р»СЊРЅС‹С… СЃС†РµРЅ РЅРµ СЃРѕР·РґР°СЋС‚СЃСЏ Рё РЅРµ РёРіСЂР°СЋС‚.
- `switchScene(name, additive)` вЂ” РїРµСЂРµРєР»СЋС‡РµРЅРёРµ: fire `sceneExited`-СЃРєСЂРёРїС‚РѕРІ СЃРѕРІРїР°РґР°СЋС‰РµР№ СЃС†РµРЅС‹
  (СЃ РёС… sync-РІС‹РїРѕР»РЅРµРЅРёРµРј С‡РµСЂРµР· `stepInstance`), kill РІСЃРµС… РёРЅСЃС‚Р°РЅСЃРѕРІ/РєР»РѕРЅРѕРІ/joints/rays,
  РѕС‡РёСЃС‚РєР° edge-triggers Рё `texts`, `stopAllSounds()` РµСЃР»Рё `this.stopSounds`, СЃР±СЂРѕСЃ
  `cloneCounter`/`sceneTime`, push СЃС‚Р°СЂРѕР№ СЃС†РµРЅС‹ РІ `sceneBackStack`, РїРµСЂРµСЃРѕР·РґР°РЅРёРµ СЃРїСЂР°Р№С‚РѕРІ
  СЃС†РµРЅС‹ С‡РµСЂРµР· `parseObject` (СЃРІРµР¶РµРµ СЃРѕСЃС‚РѕСЏРЅРёРµ, РєР°Рє `create()` РІ Android) Рё СЃС‚Р°СЂС‚
  start/`scene_start`-СЃРєСЂРёРїС‚РѕРІ. `additive=true` вЂ” append СЃРїСЂР°Р№С‚РѕРІ СЃС†РµРЅС‹ Рє Р°РєС‚РёРІРЅС‹Рј.
- Р‘СЂРёРєРё: `SceneStartBrick`/`SceneTransitionBrick` в†’ `scene_switch` (textContent
  `sceneToStart`/`sceneForTransition`), `SceneBackBrick` в†’ `scene_back` (pop backStack).
  РџРѕСЃР»Рµ РїРµСЂРµРєР»СЋС‡РµРЅРёСЏ С‚РµРєСѓС‰РёР№ РёРЅСЃС‚Р°РЅСЃ Р·Р°РІРµСЂС€Р°РµС‚СЃСЏ (`stop`). `SceneIdBrick`/`ClearScene`/
  `SetSaveScenes`/`SetStopSounds`/`SetPreloading`/`LaunchProject`/`ReturnToPreviousProject`
  РѕСЃС‚Р°СЋС‚СЃСЏ `scene_op` (no-op).
- `WhenSceneExitedScript` РїР°СЂСЃРёС‚СЃСЏ РІ `sceneExited` c `param = <sceneName>`;
  `WhenSceneLaunchedScript` вЂ” `scene_start` (Р·Р°РїСѓСЃРєР°РµС‚СЃСЏ РєР°Рє start РїСЂРё РІС…РѕРґРµ РІ СЃС†РµРЅСѓ).
- РЎРµРЅСЃРѕСЂС‹: `CURRENT_SCENE_NAME` вЂ” `this.currentSceneName`; `SCENE_TIME` вЂ” `this.sceneTime`
  (СЃР±СЂР°СЃС‹РІР°РµС‚СЃСЏ РїСЂРё РєР°Р¶РґРѕРј РІС…РѕРґРµ РІ СЃС†РµРЅСѓ).
- **Р“Р»РѕР±Р°Р»СЊРЅР°СЏ СЃС†РµРЅР°**: РѕР±СЉРµРєС‚С‹ `<globalScene>` Рё СЃРїСЂР°Р№С‚С‹ СЃ С„Р»Р°РіРѕРј `isGlobal/global` (РІ С‚.С‡.
  legacy РІРЅСѓС‚СЂРё СЃС†РµРЅ) СЃРѕР±РёСЂР°СЋС‚СЃСЏ РІ `this.globalSprites`, РґРѕР±Р°РІР»СЏСЋС‚СЃСЏ Рє Р°РєС‚РёРІРЅРѕР№ СЃС†РµРЅРµ РїСЂРё
  СЃС‚Р°СЂС‚Рµ Рё РџР•Р Р•Р–РР’РђР®Рў РїРµСЂРµРєР»СЋС‡РµРЅРёРµ СЃС†РµРЅ (СЃРєСЂРёРїС‚С‹/РёРЅСЃС‚Р°РЅСЃС‹ РЅРµ СѓР±РёРІР°СЋС‚СЃСЏ, СЃРѕСЃС‚РѕСЏРЅРёРµ
  СЃРѕС…СЂР°РЅСЏРµС‚СЃСЏ). `isBackgroundSprite` вЂ” РїРµСЂРІС‹Р№ СЃРїСЂР°Р№С‚ СЃС†РµРЅС‹ (РіР»РѕР±Р°Р»СЊРЅС‹Рµ РґРѕР±Р°РІР»СЏСЋС‚СЃСЏ РїРѕСЃР»Рµ).
- РўРµСЃС‚С‹: `player_test.js` вЂ” СЃРµРєС†РёСЏ `scenes suite` (8 С‚РµСЃС‚РѕРІ: РёР·РѕР»СЏС†РёСЏ СЃС†РµРЅ, transition,
  exit-СЃРєСЂРёРїС‚С‹, РѕСЃС‚Р°РЅРѕРІРєР° С„РѕРЅРѕРІС‹С… СЃС†РµРЅ, start, back, globalScene + isGlobal-СЃРїСЂР°Р№С‚).

### File РІ player.js (2026-08)

РџРѕР»РЅР°СЏ РєР°С‚РµРіРѕСЂРёСЏ **File** (~50 Р±Р»РѕРєРѕРІ РёР· `CategoryBricksFactory.setupFileCategoryList`):

- **Р’РёСЂС‚СѓР°Р»СЊРЅР°СЏ Р¤РЎ (VFS)**: `engine.fileWrites` (Map nameв†’Uint8Array, Р·Р°РїРёСЃС‹РІР°РµРјС‹Р№ СЃР»РѕР№ РїРѕРІРµСЂС…
  read-only `engine.files` РёР· РїСЂРѕРµРєС‚Р°). РҐРµР»РїРµСЂС‹ `vfsRead/vfsWrite/vfsDelete/vfsList`,
  `normFileWrite` (РґРѕР±Р°РІР»СЏРµС‚ `.txt` РїСЂРё РѕС‚СЃСѓС‚СЃС‚РІРёРё СЂР°СЃС€РёСЂРµРЅРёСЏ), `normFileRead`, `readZipSync`
  (СЃРёРЅС…СЂРѕРЅРЅС‹Р№ store-СЂРёРґРµСЂ, Р·РµСЂРєР°Р»Рѕ `NCZip.read` Р±РµР· async-inflate).
- **NCZip.write** (store-РјРµС‚РѕРґ, СЃРѕРІРјРµСЃС‚РёРј СЃ `NCZip.read`): `crc32` + Р»РѕРєР°Р»СЊРЅС‹Рµ/С†РµРЅС‚СЂР°Р»СЊРЅС‹Рµ
  Р·Р°РіРѕР»РѕРІРєРё + EOCD; РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ `ZipBrick` РґР»СЏ СЃРѕР·РґР°РЅРёСЏ zip РІ VFS.
- **Р РµР°Р»СЊРЅС‹Р№ РїР°СЂСЃРёРЅРі+РёСЃРїРѕР»РЅРµРЅРёРµ** (portable): WriteVariableToFile, ReadVariableFromFile
  (СЃРїРёРЅРЅРµСЂ KEEP/DELETE), WriteToFiles, ReadFromFiles, DeleteFiles, Zip/Unzip/UnzipProjectFiles,
  GetZipFileNames, OpenFile, MoveFiles/MoveDownloads (rename РїРѕ `>`), CopyProjectFile(+ToFolder/
  ToPath), PutFileIntoFolder/Path, Create/DeleteFolder(+ByPath), SaveToInternalStorage,
  LoadFromInternalStorage (no-op), Base64ToFile.
- **РЎРµРјР°РЅС‚РёРєР°** РїРѕРІС‚РѕСЂСЏРµС‚ Android-`*Action.kt`: VALUE/WRITE_FILENAME = РёРјСЏ С„Р°Р№Р»Р° (var = РєРѕРЅС‚РµРЅС‚);
  С‡С‚РµРЅРёРµ РєР»Р°РґС‘С‚ С‚РµРєСЃС‚ РІ userVariable; zip СЃРѕР±РёСЂР°РµС‚ РїРµСЂРµС‡РёСЃР»РµРЅРЅС‹Рµ С‡РµСЂРµР· Р·Р°РїСЏС‚СѓСЋ С„Р°Р№Р»С‹;
  unzip СЂР°СЃРєР»Р°РґС‹РІР°РµС‚ entry РѕР±СЂР°С‚РЅРѕ РІ VFS.
- **Android-only Р·Р°РіР»СѓС€РєРё** (`file_noop`, РїСЂРѕСЃС‚Рѕ `return 'advance'`): FileUrl, FilesUrl,
  DownloadZippedLooks, DownloadFile, UploadFile, ChooseFile, ExportProjectFile, CreateVideo,
  LoadNN, ResizeImg, GrayscaleImg, NormalizeImg, ApplyShaderToImage, LoadNativeModule,
  LoadPythonLibrary, RunVm2, RunVM, GenerateKey, SignApk, UpdateManifest, ExtractFile,
  AddFileToApk, DeleteFromApk, ZipProjectFiles, HttpSaveFile, HttpAttachFile (СЃРµС‚Рё/Р’Рњ/APK/
  РёР·РѕР±СЂР°Р¶РµРЅРёСЏ/UI вЂ” РЅРµРґРѕСЃС‚СѓРїРЅС‹ РІ Р±СЂР°СѓР·РµСЂРµ).
- **РўРµСЃС‚С‹**: `player_test.js` РґРѕР±Р°РІР»РµРЅС‹ 9 File-С‚РµСЃС‚РѕРІ (VFS state, write/read roundtrip,
  delete, zip+unzip roundtrip, get_zip_names, base64, create_folder + СЃС‚Р°Р±С‹). Р’СЃРµРіРѕ 61 С‚РµСЃС‚.

### Firebase РІ player.js (2026-08)

Firebase Storage (upload/download/delete/list) + `WhenFirebaseChangedScript` (Realtime Database
polling) вЂ” Р±РµР· Firebase SDK, С‡РµСЂРµР· `fetch()` (WebView2):

- **РџР°СЂСЃРёРЅРі** (`parseBrickRec`): `UploadFileToFirebaseBrick` в†’ `firebase_upload` (FIREBASE_BUCKET,
  FIREBASE_STORAGE_PATH, FILE), `DownloadFileFromFirebaseBrick` в†’ `firebase_download`
  (+DOWNLOAD_PATH, var), `ListFirebaseFilesBrick` в†’ `firebase_list` (+var), `DeleteFirebaseFileBrick`
  в†’ `firebase_delete`. `WriteBaseBrick` в†’ `firebase_db_write` (FIREBASE_ID, FIREBASE_KEY,
  FIREBASE_VALUE + wait-С„Р»Р°Рі РёР· `waitForResponseSelection`), `ReadBaseBrick` в†’ `firebase_db_read`
  (FIREBASE_ID, FIREBASE_KEY, var + wait-С„Р»Р°Рі), `DeleteBaseBrick` в†’ `firebase_db_delete`
  (FIREBASE_ID, FIREBASE_KEY + wait-С„Р»Р°Рі). `waitForResponseSelection=0` (Wait) в†’ Р±Р»РѕРє Р¶РґС‘С‚ РѕС‚РІРµС‚Р°.
- **RTDB REST** (`{baseUrl}/{key}.json`, key С‡РµСЂРµР· encodeURIComponent РїРѕ СЃРµРіРјРµРЅС‚Р°Рј):
  write = `PUT` СЃ `JSON.stringify(value)` (СЃС‚СЂРѕРєРё РІ РєР°РІС‹С‡РєР°С…, РєР°Рє Android `setValue(String)`);
  read = `GET` в†’ JSON-РїР°СЂСЃ в†’ РїРµСЂРµРјРµРЅРЅР°СЏ (С‡РёСЃР»Рѕ РіРѕР»РѕРµ, СЃС‚СЂРѕРєР° Р±РµР· РєР°РІС‹С‡РµРє, `null` в†’ `"No data"`,
  РѕР±СЉРµРєС‚ в†’ JSON-СЃС‚СЂРѕРєР°; `setVar` РєРѕРЅРІРµСЂС‚РёСЂСѓРµС‚ С‡РёСЃР»РѕРІС‹Рµ СЃС‚СЂРѕРєРё РІ С‡РёСЃР»Р°); delete = `DELETE`.
- **РћР¶РёРґР°РЅРёРµ РѕС‚РІРµС‚Р°**: `waitForResponse=true` в†’ execBlock РІРѕР·РІСЂР°С‰Р°РµС‚ `'waiting'`, РЅР° С„СЂРµР№Рј
  РІРµС€Р°РµС‚СЃСЏ `frame._asyncPending = { done, promise }`; `stepInstance` РІ РЅР°С‡Р°Р»Рµ РїСЂРѕРІРµСЂСЏРµС‚ РµРіРѕ
  (done в†’ `ip++`, РёРЅР°С‡Рµ return). РЎР±СЂРѕСЃ `_asyncPending` РїСЂРё РІС‹С…РѕРґРµ С„СЂРµР№РјР° вЂ” РЅРµ РЅСѓР¶РµРЅ (С„СЂРµР№РјС‹
  РѕРґРЅРѕСЂР°Р·РѕРІС‹Рµ).
- **Storage REST** (`https://firebasestorage.googleapis.com/v0/b/{bucket}/o...`):
  upload = `POST ?name={path}` (С‚РµР»Рѕ вЂ” Р±Р°Р№С‚С‹ С„Р°Р№Р»Р° РёР· VFS, `application/octet-stream`), download =
  `GET {path}?alt=media` в†’ `vfsWrite(dest)` + РїРµСЂРµРјРµРЅРЅР°СЏ = РїСѓС‚СЊ (РёР»Рё `"ERROR"`), delete = `DELETE`,
  list = `GET ?maxResults=1000&prefix=` в†’ РёРјРµРЅР° С‡РµСЂРµР· `"name":"` в†’ РїРµСЂРµРјРµРЅРЅР°СЏ = `join(", ")`.
  РЎРµРјР°РЅС‚РёРєР° РїРѕРІС‚РѕСЂСЏРµС‚ `FireBaseStorageManager.kt` + `*FirebaseAction.kt` Android.
- **РђСѓС‚РµРЅС‚РёС„РёРєР°С†РёСЏ (lazy)**: `_firebaseAuthHeaders()` вЂ” API key РёР· РїРµСЂРµРјРµРЅРЅРѕР№ РїСЂРѕРµРєС‚Р°
  `FIREBASE_API_KEY` (UserVariable/UserList); РїСЂРё РїРµСЂРІРѕРј РІС‹Р·РѕРІРµ Р·Р°РїСѓСЃРєР°РµС‚ anonymous auth
  (`identitytoolkit.googleapis.com/v1/accounts:signInAnonymously?key=...`), `idToken` РєСЌС€РёСЂСѓРµС‚СЃСЏ
  РІ `this.firebase.token`; РїРѕСЃР»РµРґСѓСЋС‰РёРµ Р·Р°РїСЂРѕСЃС‹ РёРґСѓС‚ СЃ `Authorization: Bearer {token}`. Р‘РµР· РєР»СЋС‡Р° вЂ”
  Р·Р°РїСЂРѕСЃС‹ Р±РµР· auth (РїСѓР±Р»РёС‡РЅС‹Рµ РїСЂР°РІРёР»Р°).
- **WhenFirebaseChangedScript** (`parseScript`): `st='firebase'`, `param = { bucket, path }` вЂ”
  С„РѕСЂРјСѓР»С‹ `FIREBASE_TRIGGER_BUCKET`/`FIREBASE_TRIGGER_PATH` РёР· `<formulaMap>` СЃРєСЂРёРїС‚Р° (РјРµС‚РѕРґ
  `getFormula` СЂР°СЃС€РёСЂРµРЅ: С‡РёС‚Р°РµС‚ Рё `formulaList`, Рё `formulaMap`). bucket = URL RTDB
  (`https://...firebaseio.com`), path = РєР»СЋС‡.
- **Polling**: `update()` РєР°Р¶РґС‹Рµ 2СЃ (`_fbPollTimer`) в†’ `_firebasePoll()`: GET
  `{bucket}/{path.split('/').map(encodeURIComponent)}.json` (cache: no-store); РїРµСЂРІС‹Р№ РѕС‚РІРµС‚ вЂ”
  baseline (РЅРµ С‚СЂРёРіРіРµСЂРёС‚), РїСЂРё РёР·РјРµРЅРµРЅРёРё С‚РµРєСЃС‚Р° вЂ” `startScript(sp, s)` (edge-trigger, РєР»СЋС‡
  `sp.name#index`, `_fbPrev`); `_fbPrev.clear()` РїСЂРё `switchScene`/`loadXml`. Guard РЅР° РѕС‚СЃСѓС‚СЃС‚РІРёРµ
  fetch/РїСѓСЃС‚С‹Рµ С„РѕСЂРјСѓР»С‹.
- **РЎРѕСЃС‚РѕСЏРЅРёРµ**: `this.firebase = { token, listeners, config }`, `_fbPrev`, `_fbPollTimer`,
  `_fbInitStarted` (РІ constructor).
- **РўРµСЃС‚С‹**: `player_test.js` вЂ” СЃРµРєС†РёСЏ `firebase suite` (16 С‚РµСЃС‚РѕРІ, РїРѕСЃР»РµРґРѕРІР°С‚РµР»СЊРЅС‹Р№ Р·Р°РїСѓСЃРє
  РїРѕСЃР»Рµ `await Promise.all(pending)`, РјРѕРє `global.fetch`): РїР°СЂСЃРёРЅРі 4 Р±Р»РѕРєРѕРІ Storage + 3 Р±Р»РѕРєРѕРІ
  RTDB (РІРєР». wait-С„Р»Р°РіРё РёР· СЃРїРёРЅРЅРµСЂР°), upload POST С‚РµР»Р° РёР· VFS, no-op Р±РµР· fetch/С„Р°Р№Р»Р°,
  download в†’ VFS+РїРµСЂРµРјРµРЅРЅР°СЏ, ERROR РЅР° HTTP-СЃР±РѕР№, list в†’ join, delete DELETE, РїР°СЂСЃРёРЅРі
  WhenFirebaseChangedScript, poll baseline/change/no-refire/refire, auth: anonymous sign-in +
  Bearer РІ РїРѕСЃР»РµРґСѓСЋС‰РёС… Р·Р°РїСЂРѕСЃР°С…, db_write PUT JSON + Р±Р»РѕРєРёСЂРѕРІРєР° РґРѕ РѕС‚РІРµС‚Р°, db_read (С‡РёСЃР»Рѕ/
  СЃС‚СЂРѕРєР°/`No data`/РѕР±СЉРµРєС‚), db_delete DELETE.

### Motion/Physics РІ player.js (2026-08)

РџРѕР»РЅР°СЏ РєР°С‚РµРіРѕСЂРёСЏ **Motion** (~48 Р±Р»РѕРєРѕРІ, РІРєР»СЋС‡Р°СЏ С„РёР·РёРєСѓ Рё С€Р°СЂРЅРёСЂС‹):

- **РњРµС…Р°РЅРёРєР°**: `sprite.ph = {type (0 NONE/1 DYNAMIC/2 FIXED), mass, vx, vy, omega, fx, fy,
  torque, linDamping, angDamping, bounce, friction, ragdoll}` вЂ” РёРЅРёС†РёР°Р»РёР·РёСЂСѓРµС‚СЃСЏ РІ `parseObject`.
  `stepPhysics(dt)` (РІ `update()` РїРµСЂРµРґ С€Р°РіРѕРј РёРЅСЃС‚Р°РЅСЃРѕРІ): РіСЂР°РІРёС‚Р°С†РёСЏ `this.gravity`
  (РґРµС„РѕР»С‚ `{x:0, y:-980}` px/sВІ) + РЅР°РєРѕРїР»РµРЅРЅС‹Рµ СЃРёР»С‹/РјРѕРјРµРЅС‚С‹ в†’ РёРЅС‚РµРіСЂР°С†РёСЏ РїРѕР·РёС†РёРё/СѓРіР»Р°,
  РґРµРјРїС„РёСЂРѕРІР°РЅРёРµ, joint-РѕРіСЂР°РЅРёС‡РµРЅРёСЏ, РєРѕР»Р»РёР·РёРё DYNAMICв†”FIXED (РІС‹С‚Р°Р»РєРёРІР°РЅРёРµ РїРѕ РјРµРЅСЊС€РµР№ РѕСЃРё +
  РѕС‚СЂР°Р¶РµРЅРёРµ `vx/vy Г— bounce`) Рё СЃС‚РµРЅС‹ СЃС†РµРЅС‹ (В±VW/2, В±VH/2 в€’ РїРѕР»РѕРІРёРЅРЅС‹Р№ С…РёС‚Р±РѕРєСЃ).
- **Р’РµРєС‚РѕСЂС‹ РґРѕ -980**: SetVelocity/ApplyForce/Impulse, Torque/AngularImpulse,
  TurnLeft/RightSpeed (omega), SetGravity/SetMass/SetDamping/SetBounce(0..1, РІ С„РѕСЂРјСѓР»Рµ %)/SetFriction.
  Р’СЃРµ force/velocity-Р±Р»РѕРєРё РїСЂРѕРјРѕС‚РёСЂСѓСЋС‚ NONEв†’DYNAMIC, РЅРѕ РќР• РїРµСЂРµР±РёРІР°СЋС‚ СЏРІРЅС‹Р№ FIXED.
- **SetPhysicsObjectTypeBrick** вЂ” `<type>` РёР· XML (DYNAMIC/FIXED/NONE). **SetRagdollBrick** вЂ”
  С„Р»Р°Рі `ph.ragdoll`. **SetHitboxBrick** вЂ” `sprite.hitbox {w,h}` РїРµСЂРµРѕРїСЂРµРґРµР»СЏРµС‚ AABB
  (РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ РІ aabbFor в†’ РєРѕР»Р»РёР·РёРё/raycast/С‚СЂРёРіРіРµСЂС‹).
- **PointToBrick** вЂ” `pointedObject` СЃ `reference="/.../object[N]"` (XStream) СЂРµР·РѕР»РІРёС‚СЃСЏ РІ
  `resolveTargetName('ref:вЂ¦')` **РЅР° СЌС‚Р°РїРµ РёСЃРїРѕР»РЅРµРЅРёСЏ** (РїСЂРё РїР°СЂСЃРёРЅРіРµ `this.sprites` РµС‰С‘ РїСѓСЃС‚);
  РїСѓСЃС‚РѕР№ target в†’ СЃР»СѓС‡Р°Р№РЅРѕРµ РЅР°РїСЂР°РІР»РµРЅРёРµ. GoToBrick `destinationSprite` вЂ” С‚Р° Р¶Рµ РјРµС…Р°РЅРёРєР°.
- **PerformRayCastBrick** вЂ” Liang-Barsky `segmentAABB` РїРѕ С…РёС‚Р±РѕРєСЃР°Рј, СЂРµР·СѓР»СЊС‚Р°С‚ РІ
  `engine.rays.get(id)` = `{sprite, t}` (Р±Р»РёР¶Р°Р№С€РµРµ РїРµСЂРµСЃРµС‡РµРЅРёРµ) РёР»Рё null.
- **РЁР°СЂРЅРёСЂС‹**: `create_joint` Р·Р°РїРёСЃС‹РІР°РµС‚ РІ `this.joints` `{type, body, target, length?}`;
  СЂРµР°Р»СЊРЅРѕ СЂР°Р±РѕС‚Р°СЋС‚ **distance** (РїРѕР·РёС†РёРѕРЅРЅС‹Р№ constraint, СЃС…РѕРґРёС‚СЃСЏ Рє JOINT_LENGTH) Рё
  **weld/revolute** (С„РёРєСЃР°С†РёСЏ РѕС‚РЅРѕСЃРёС‚РµР»СЊРЅРѕРіРѕ СЃРјРµС‰РµРЅРёСЏ); prismatic/pulley/gear Р·Р°РїРёСЃС‹РІР°СЋС‚СЃСЏ
  (inert), DestroyJoint СѓРґР°Р»СЏРµС‚ РїРѕ JOINT_ID.
- **VibrationBrick** вЂ” `navigator.vibrate` (guard РІ Node). **SetCameraFocusPointBrick**
  (+HORIZONTAL/VERTICAL_FLEXIBILITY) Рё **FadeParticleEffectBrick** вЂ” no-op.
- **WhenBounceOffScript** в†’ С‚РёРї `bounce`, param = `spriteToBounceOffName` (РїСѓСЃС‚Рѕ = Р»СЋР±РѕР№).
  `checkBounceTriggers()` (РІ `update()` Р”Рћ `stepPhysics`, РёРЅР°С‡Рµ РІС‹С‚Р°Р»РєРёРІР°РЅРёРµ РіР°СЃРёС‚ СЃРѕР±С‹С‚РёРµ):
  edge-trigger РїРѕ AABB-РїРµСЂРµРєСЂС‹С‚РёСЋ РїР°СЂ, РіРґРµ С…РѕС‚СЏ Р±С‹ РѕРґРёРЅ DYNAMIC; fire РЅР° РѕР±Р° СЃРїСЂР°Р№С‚Р°;
  РїРѕРІС‚РѕСЂРЅС‹Р№ fire С‚РѕР»СЊРєРѕ РїРѕСЃР»Рµ СЂР°СЃС…РѕР¶РґРµРЅРёСЏ РїР°СЂС‹ (`_bouncePrev: Set`).

### РСЃРїСЂР°РІР»РµРЅРЅС‹Рµ Р±Р°РіРё РїСЂРё С‚РµСЃС‚РёСЂРѕРІР°РЅРёРё (2026-08)
- NCXml: СЃР°РјРѕР·Р°РєСЂС‹РІР°СЋС‰РёРµСЃСЏ С‚РµРіРё (`<look .../>`) РєР»Р°Р»РёСЃСЊ РІ СЃС‚РµРє в†’ siblings РІРєР»Р°РґС‹РІР°Р»РёСЃСЊ;
  `stack.push` С‚РѕР»СЊРєРѕ РїСЂРё `!selfClosed`.
- NCZip: С‚РµСЃС‚РѕРІС‹Р№ builder Р±РµР· РїРѕР»СЏ date в†’ С†РµРЅС‚СЂР°Р»СЊРЅС‹Р№ РєР°С‚Р°Р»РѕРі СЃРґРІРёРЅСѓС‚ РЅР° 2 Р±Р°Р№С‚Р°
  (Z_BUF_ERROR РЅР° inflate); С…РµРґРµСЂС‹ РїСЂРёРІРµРґРµРЅС‹ Рє СЃС‚Р°РЅРґР°СЂС‚Сѓ.
- РРЅС‚РµСЂРїСЂРµС‚Р°С‚РѕСЂ: РїРѕРІС‚РѕСЂ РєРѕРЅС‚РµР№РЅРµСЂР° РёРЅРєСЂРµРјРµРЅС‚РёСЂРѕРІР°Р» ip РєР°Р¶РґС‹Р№ РїСЂРѕС…РѕРґ в†’ restart СЃ
  `blocks[parent.ip]`; `parent.ip++` С‚РѕР»СЊРєРѕ РєРѕРіРґР° repeat РёСЃС‡РµСЂРїР°РЅ.
- Р“Р»РёРґ СЃС‡РёС‚Р°Р» `performance.now()` в†’ РґРµС‚РµСЂРјРёРЅРёСЂРѕРІР°РЅРЅС‹Р№ `elapsed += dt`.
- `B()`/`N()` С…РµР»РїРµСЂС‹ РЅРµ Р±С‹Р»Рё РѕРїСЂРµРґРµР»РµРЅС‹ (ReferenceError) вЂ” РґРѕР±Р°РІР»РµРЅС‹ РІ parseBrickRec;
  Р±Р»РѕРєРё РїРёС€СѓС‚СЃСЏ РєР°Рє `{t, args, kids?}` (РёРЅС‚РµСЂРїСЂРµС‚Р°С‚РѕСЂ С‡РёС‚Р°РµС‚ `b.args`).
- `SetYPositionBrick` РѕС‚СЃСѓС‚СЃС‚РІРѕРІР°Р» РІ РїР°СЂСЃРµСЂРµ (skip) вЂ” РґРѕР±Р°РІР»РµРЅ Р°Р»РёР°СЃ РЅР° `set_y`.
- spriteAt С‚СЂРµР±РѕРІР°Р» `l.img` в†’ fallback-СЂР°Р·РјРµСЂ 80 (РґРµС„РѕР»С‚ Catroid) РґР»СЏ Р±РµР·-РёР·РѕР±СЂР°Р¶РµРЅРёР№.

### РџСЂРѕРІРµСЂРєР°
```
cd desktop-runtime/webview2_stub/player && node player_test.js        # 61 С‚РµСЃС‚ (node player_test.js, РІРєР». Motion/physics/File)
./gradlew :catroid:testCatroidDebugUnitTest --tests "*ExeBuilderV2FooterTest*"   # 3 Robolectric
```
Р­РєСЃРїРѕСЂС‚ РІ РїСЂРёР»РѕР¶РµРЅРёРё: РџСЂРѕРµРєС‚ в†’ в‹® в†’ Export в†’ В«Windows EXE v2 (single file)В».
РЎС‚Р°СЂС‹Р№ `buildExe` (V1, launch4j-РїР°Р№РїР»Р°Р№РЅ) РќР• С‚СЂРѕРЅСѓС‚ вЂ” РѕРЅ РѕСЃС‚Р°С‘С‚СЃСЏ РЅРµРёСЃРїРѕР»СЊР·СѓРµРјС‹Рј.

## РџРµСЂРµРЅРѕСЃРёРјС‹Рµ seam (РІ `:core`)

| Seam | РРЅС‚РµСЂС„РµР№СЃ | Holder |
|---|---|---|
| Runtime | `RuntimeServices` | `RuntimeServicesHolder` |
| Audio | `AudioService` | `AudioServiceHolder` |
| Midi | `MidiService` | `MidiServiceHolder` |
| Text | `TextService` | `TextServiceHolder` |
| Notification | `NotificationService` | `NotificationServiceHolder` |

StageListenerHolder: `object StageListenerHolder { var listener: StageListener? = null }` (РІ `:core`).

## РСЃРїСЂР°РІР»РµРЅРёСЏ Р±Р°РіРѕРІ (2026-07-13)

### DesktopMidiService
- `playSoundFile()` / `playSoundFileWithStartTime()` вЂ” Р±С‹Р»Рё РїСѓСЃС‚С‹РјРё Р·Р°РіР»СѓС€РєР°РјРё в†’ С‚РµРїРµСЂСЊ РґРµР»РµРіРёСЂСѓСЋС‚ `AudioServiceHolder`.

### DesktopSprite
- Р”РѕР±Р°РІР»РµРЅРѕ РїРѕР»Рµ `visible: Boolean = true`.

### DesktopPhysicsWorld
- Р”РѕР±Р°РІР»РµРЅ РјРµС‚РѕРґ `getBody(sprite): Body?` (Р±С‹Р» РїСЂРёРІР°С‚РЅС‹Рј).

### DesktopSprite
- Р”РѕР±Р°РІР»РµРЅС‹ РїРѕР»СЏ: `transparency`, `brightness`, `color` (РіСЂР°С„РёС‡РµСЃРєРёРµ СЌС„С„РµРєС‚С‹); `width`, `height` (РїРµСЂРµРѕРїСЂРµРґРµР»РµРЅРёРµ СЂР°Р·РјРµСЂР°); `penDown`, `penSize`, `penColorRed/Green/Blue` (РїРµСЂРѕ); `rotationStyle` (0/1/2); `lookWidth`, `lookHeight` (computed).

### DesktopStageListener
- РќРµРІРёРґРёРјС‹Рµ СЃРїСЂР°Р№С‚С‹ (`!sprite.visible`) РїСЂРѕРїСѓСЃРєР°СЋС‚СЃСЏ РїСЂРё СЂРµРЅРґРµСЂРµ.

### DesktopInput (РѕР±РЅРѕРІР»С‘РЅ 2026-07-13)
- Р”РѕР±Р°РІР»РµРЅС‹: `mouseDeltaX`, `mouseDeltaY` (РґР»СЏ СЃРµРЅСЃРѕСЂРѕРІ MOUSE_DELTA), `fingerX`, `fingerY`, `isTouched` (Р·РµСЂРєР°Р»Рѕ РјС‹С€Рё РґР»СЏ СЃРµРЅСЃРѕСЂРѕРІ РєР°СЃР°РЅРёСЏ).

### DesktopScriptEngine вЂ” РїРѕР»РЅР°СЏ РїРµСЂРµСЂР°Р±РѕС‚РєР° (2026-07-13)
- **РЎС‚РµРєРѕРІР°СЏ РјР°С€РёРЅР°**: РєР°Р¶РґС‹Р№ СЃРєСЂРёРїС‚ = СЃРІРѕР№ `ScriptState` СЃРѕ СЃС‚РµРєРѕРј С„СЂРµР№РјРѕРІ.
- Р¤СЂРµР№Рј: `{blocks, ip, repeatRemaining, waitTimer, glideState}`.
- **RuntimeFormula**: С„РѕСЂРјСѓР»С‹ СЃ СЃРµРЅСЃРѕСЂР°РјРё/РїРµСЂРµРјРµРЅРЅС‹РјРё РІС‹С‡РёСЃР»СЏСЋС‚СЃСЏ РїСЂРё РєР°Р¶РґРѕРј РїСЂРѕС…РѕРґРµ (Р° РЅРµ РїСЂРё РїР°СЂСЃРёРЅРіРµ).

#### РџРѕРґРґРµСЂР¶РёРІР°РµРјС‹Рµ РєРѕРЅС‚РµР№РЅРµСЂРЅС‹Рµ Р±СЂРёРєРё
- **ForeverBrick** в†’ `repeatRemaining = -1`, СЃР±СЂРѕСЃ ip РЅР° 0 РїСЂРё Р·Р°РІРµСЂС€РµРЅРёРё.
- **RepeatBrick** в†’ `repeatRemaining = N`, РґРµРєСЂРµРјРµРЅС‚, СЃР±СЂРѕСЃ ip.
- **RepeatUntilBrick** в†’ forever + wait_until condition.
- **IfLogicBeginBrick** в†’ then-branch / else-branch (IfLogicElseBrick).
- **IfThenLogicBeginBrick** в†’ if Р±РµР· else.
- **ForVariableFromToBrick** в†’ repeat СЃ РІС‹С‡РёСЃР»РµРЅРЅС‹Рј С‡РёСЃР»РѕРј РёС‚РµСЂР°С†РёР№.
- **ScheduleBrick** в†’ wait + РІС‹РїРѕР»РЅРµРЅРёРµ РґРµС‚РµР№.
- **ExecuteForCloneNumberBrick** в†’ repeat 1 СЃ РґРµС‚СЊРјРё.
- **RunAsSpriteBrick / RunOnUiThreadBrick** в†’ inline-РІС‹РїРѕР»РЅРµРЅРёРµ РґРµС‚РµР№.
- **BroadcastBrick, BroadcastWaitBrick** в†’ СЃРѕР±С‹С‚РёРµ broadcast.
- **StopScriptBrick** в†’ frame.ip = blocks.size (РІС‹С…РѕРґ).

#### РџРѕРґРґРµСЂР¶РёРІР°РµРјС‹Рµ Р»РёСЃС‚РѕРІС‹Рµ Р±СЂРёРєРё (~60 С‚РёРїРѕРІ)
- **Motion**: MoveNSteps, TurnLeft/Right, SetX/Y, ChangeX/Y, GoTo, PlaceAt, PointInDirection, SetSizeTo, GlideTo, IfOnEdgeBounce, ComeToFront, GoNStepsBack, SetRotationStyle, TouchDirection
- **Looks**: Show, Hide, Next/Previous Look, SetLook(byIndex), SetBackground(byIndex), SetSizeTo, ChangeSize, Set/Change Transparency/Brightness/Color, ClearEffects, Set/Change Width/Height
- **Sound**: PlaySound, PlaySoundAndWait, PlaySoundAt, SoundFile, SoundFiles, PrepareSound, PlayPrepared, StopSound, StopSoundBrick2, StopAllSounds, Sound_StopAll, SetVolume, ChangeVolume, SetSoundVolume, SetGlobalSoundVolume, SetGameVolume, SetStopSounds, Speak, SpeakAndWait, SpeakWithRate, AskSpeech, StartListening, SetListeningLanguage, ListenMicro, StartRecording, StopRecording, SetPan, SetPitchOnly, AudioFadeIn, AudioFadeOut, EqualizerSetBand, PlayTone
- **Music**: PlayNoteForBeats, PlayDrumForBeats, SetInstrument, SetTempo, ChangeTempo, PauseForBeats
- **Pen**: PenDown/Up, SetPenSize, SetPenColor, Stamp, ClearBackground
- **Control**: Wait (СЃ runtime-С„РѕСЂРјСѓР»Р°РјРё), WaitUntil, Note (РєРѕРјРјРµРЅС‚Р°СЂРёР№), FinishStage, ExitStage
- **Variables**: SetVariable, ChangeVariable (СЃ runtime-С„РѕСЂРјСѓР»Р°РјРё), ShowText, HideText
- **Web**: WebRequestBrick (GET), PostWebRequestBrick, PutWebRequestBrick, DeleteWebRequestBrick
- **Data**: WriteVariableOnDevice, ReadVariableFromDevice
- **Sensing**: ResetTimer

#### Р РµРєСѓСЂСЃРёРІРЅС‹Р№ РІС‹С‡РёСЃР»РёС‚РµР»СЊ С„РѕСЂРјСѓР»
Р—Р°РјРµРЅСЏРµС‚ СЃС‚Р°С‚РёС‡РµСЃРєРёР№ `extractFormulaValue`. РџРѕРґРґРµСЂР¶РёРІР°РµС‚:

| РўРёРї | Р—РЅР°С‡РµРЅРёРµ | РџРѕРґРґРµСЂР¶РєР° |
|-----|----------|-----------|
| `NUMBER` | `value.toDouble()` | РґР°|
| `STRING` | СЃС‚СЂРѕРєРѕРІР°СЏ РєРѕРЅСЃС‚Р°РЅС‚Р° | РґР° |
| `OPERATOR` | PLUS, MINUS, MULT, DIVIDE, MOD, POW, EQUAL, NOT_EQUAL, SMALLER_THAN, GREATER_THAN, SMALLER_OR_EQUAL, GREATER_OR_EQUAL, LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT |  РІСЃРµ |
| `FUNCTION` | РњР°С‚РµРјР°С‚РёС‡РµСЃРєРёРµ: SIN, COS, TAN, LN, LOG, SQRT, ABS, ROUND, FLOOR, CEIL, PI, TRUE, FALSE, RAND, MAX, MIN, POWER, MOD, ARCSIN, ARCCOS, ARCTAN, ARCTAN2, EXP, ROUNDTO, CLAMP | РґР° |
| `FUNCTION` | РЎС‚СЂРѕРєРѕРІС‹Рµ: LENGTH, LETTER, SUBTEXT, UPPER, LOWER, JOIN, JOIN3, REVERSE | РґР° |
| `FUNCTION` | РЎРёСЃС‚РµРјРЅС‹Рµ: SCREEN_WIDTH, SCREEN_HEIGHT, DEVICE_NAME | РґР° |
| `SENSOR` | OBJECT_X, OBJECT_Y, OBJECT_SIZE, OBJECT_WIDTH, OBJECT_HEIGHT, OBJECT_DIRECTION, MOTION_DIRECTION, LOOK_DIRECTION, OBJECT_TRANSPARENCY, OBJECT_BRIGHTNESS, OBJECT_COLOR, OBJECT_LOOK_NUMBER, OBJECT_NUMBER_OF_LOOKS, OBJECT_X_VELOCITY, OBJECT_Y_VELOCITY, STAGE_WIDTH, STAGE_HEIGHT | РґР° |
| `SENSOR` | MOUSE_X, MOUSE_Y, MOUSE_DELTA_X, MOUSE_DELTA_Y, FINGER_X, FINGER_Y, FINGER_TOUCHED, NUMBER_CURRENT_TOUCHES, INDEX_CURRENT_TOUCH | РґР°|
| `SENSOR` | DATE_YEAR, DATE_MONTH, DATE_DAY, DATE_WEEKDAY, TIME_HOUR, TIME_MINUTE, TIME_SECOND | РґР° |
| `SENSOR` | X_ACCELERATION, Y_ACCELERATION, Z_ACCELERATION, COMPASS_DIRECTION, LATITUDE, LONGITUDE (Р·Р°РіР»СѓС€РєРё = 0) | РґР° Р·Р°РіР»СѓС€РєРё |
| `USER_VARIABLE` | lookup(name) РІ `variables[name]` (РІРѕР·РІСЂР°С‰Р°РµС‚ `Any`, РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ `0f`) | РґР°|
| `USER_LIST` | РІРѕР·РІСЂР°С‰Р°РµС‚ "" | РґР° Р·Р°РіР»СѓС€РєР° |
| `BRACKET` | РІС‹С‡РёСЃР»СЏРµС‚ rightChild | РґР° |
| `COLLISION_FORMULA` | РІРѕР·РІСЂР°С‰Р°РµС‚ value РєР°Рє Double | РґР° |

### DesktopNetworkService (РЅРѕРІС‹Р№ seam, 2026-07-13)
- `NetworkService` (РёРЅС‚РµСЂС„РµР№СЃ, `:core`): `httpGet(url)`, `httpPost(url, body)`, `httpPut(url, body)`, `httpDelete(url)` (4 РјРµС‚РѕРґР°).
- `NetworkServiceHolder` (РѕР±СЉРµРєС‚, `:core`): С‚РѕС‡РєР° РёРЅСЉРµРєС†РёРё.
- `DesktopNetworkService` (`:desktop-runtime`): СЂРµР°Р»РёР·Р°С†РёСЏ С‡РµСЂРµР· `java.net.HttpURLConnection` СЃ 10s С‚Р°Р№РјР°СѓС‚Р°РјРё.
- Р—Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅ РІ `DesktopStage.main()`.

## Desktop-СЂРµР°Р»РёР·Р°С†РёРё seam

| РњРѕРґСѓР»СЊ | Р¤Р°Р№Р» | РњРµС‚РѕРґС‹ | РЎС‚Р°С‚СѓСЃ |
|--------|------|--------|--------|
| :core | RuntimeServices (7 РјРµС‚РѕРґРѕРІ) | DesktopRuntimeServices |  РІСЃРµ |
| :core | AudioService (18 РјРµС‚РѕРґРѕРІ) | DesktopAudioService |  РІСЃРµ |
| :core | MidiService (16 РјРµС‚РѕРґРѕРІ) | DesktopMidiService | РІСЃРµ |
| :core | TextService (1 РјРµС‚РѕРґ) | DesktopTextService | СѓСЃРµ  |
| :core | NotificationService (4 РјРµС‚РѕРґР°) | DesktopNotificationService |  РІСЃРµ |

## РСЃРїСЂР°РІР»РµРЅРёСЏ Р±Р°РіРѕРІ СЂР°РЅС‚Р°Р№РјР° (2026-07-13)
- **DesktopInput**: `isMouseJustPressed` РІСЃРµРіРґР° Р±С‹Р» `false` РёР·-Р·Р° `wasMouseDown = isMouseDown` (С‚РµРєСѓС‰РµРµ СЃРѕСЃС‚РѕСЏРЅРёРµ РІРјРµСЃС‚Рѕ РїСЂРµРґС‹РґСѓС‰РµРіРѕ) вЂ” РїРµСЂРµРґРµР»Р°РЅРѕ РЅР° РґРІСѓС…РєР°РґСЂРѕРІС‹Р№ С‚СЂРµРєРёРЅРі С‡РµСЂРµР· `previousMouseDown`.
- **DesktopInput**: `update()` РІС‹Р·С‹РІР°Р»СЃСЏ РґРІР°Р¶РґС‹ Р·Р° РєР°РґСЂ (РІ `DesktopScriptEngine.update()` + `DesktopScriptRunner.updateInput()`) вЂ” РІС‹Р·РѕРІ РїРµСЂРµРЅРµСЃС‘РЅ РћР”РРќ СЂР°Р· РІ `DesktopStageListener.render()`.
- **DesktopProjectManager**: РёРјСЏ СЃРїСЂР°Р№С‚Р° (`"sprite$i"`) Рё `direction` РЅРµ С‡РёС‚Р°Р»РёСЃСЊ РёР· `code.xml` вЂ” С‚РµРїРµСЂСЊ С‡РёС‚Р°СЋС‚СЃСЏ.
- **DesktopMidiService**: `playNote` РІС‹Р·С‹РІР°Р» `Thread.sleep` РЅР° render-РїРѕС‚РѕРєРµ вЂ” РІС‹РЅРµСЃРµРЅ РІ daemon-РїРѕС‚РѕРє (isDaemon = true).
- **DesktopPhysicsWorld**: `syncSpritesFromPhysics` РґРµР»Р°Р» Р±РµСЃСЃРјС‹СЃР»РµРЅРЅС‹Р№ `body.setTransform` РїРѕСЃР»Рµ С‡С‚РµРЅРёСЏ РїРѕР·РёС†РёРё вЂ” СѓРґР°Р»С‘РЅ.
- **DesktopStage**: `extractPayload` РЅРµ РѕС‡РёС‰Р°Р» temp-РґРёСЂРµРєС‚РѕСЂРёСЋ вЂ” `walkTopDown().forEach { it.deleteOnExit() }`.

РЎР±РѕСЂРєР°: `./gradlew :core:compileKotlin --offline -q` вЂ” **BUILD SUCCESSFUL**.
РЎР±РѕСЂРєР° desktop-runtime: `./gradlew :desktop-runtime:compileKotlin --offline -q` вЂ” **BUILD SUCCESSFUL**.

## РџРѕР»РЅС‹Р№ DesktopScriptEngine (2026-07-13)

РџРѕР»РЅР°СЏ РїРµСЂРµСЂР°Р±РѕС‚РєР° `DesktopScriptEngine.kt` (960 СЃС‚СЂРѕРє):
- **Р РµРєСѓСЂСЃРёРІРЅС‹Р№ РІС‹С‡РёСЃР»РёС‚РµР»СЊ С„РѕСЂРјСѓР»** вЂ” `evaluateFormulaNode()` РѕР±СЂР°Р±Р°С‚С‹РІР°РµС‚ 10 С‚РёРїРѕРІ (NUMBER, STRING, OPERATOR, FUNCTION, SENSOR, USER_VARIABLE, USER_LIST, BRACKET, COLLISION_FORMULA) СЃ СЂРµРєСѓСЂСЃРёРІРЅС‹Рј РѕР±С…РѕРґРѕРј leftChild/rightChild/additionalChildren.
- **15 РѕРїРµСЂР°С‚РѕСЂРѕРІ**: PLUS, MINUS, MULT, DIVIDE, MOD, POW, EQUAL, NOT_EQUAL, SMALLER_THAN, GREATER_THAN, SMALLER_OR_EQUAL, GREATER_OR_EQUAL, LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT.
- **30+ С„СѓРЅРєС†РёР№**: SIN, COS, TAN, SQRT, RAND, ABS, ROUND, FLOOR, CEIL, PI, TRUE, FALSE, MAX, MIN, POWER, MOD, ARCSIN, ARCCOS, ARCTAN, ARCTAN2, EXP, ROUNDTO, CLAMP, LENGTH, LETTER, SUBTEXT, UPPER, LOWER, JOIN, JOIN3, REVERSE, SCREEN_WIDTH, SCREEN_HEIGHT, DEVICE_NAME.
- **40+ СЃРµРЅСЃРѕСЂРѕРІ**: OBJECT_X/Y/SIZE/WIDTH/HEIGHT/TRANSPARENCY/BRIGHTNESS/COLOR/LOOK_NUMBER/VELOCITY, MOUSE_X/Y/DELTA, FINGER_X/Y/TOUCHED, РґР°С‚Р°/РІСЂРµРјСЏ, Р·Р°РіР»СѓС€РєРё РґР»СЏ Р°РєСЃРµР»РµСЂРѕРјРµС‚СЂР°/РєРѕРјРїР°СЃР°/GPS.
- **~89 С‚РёРїРѕРІ Р±СЂРёРєРѕРІ**: Motion (14), Looks (14), Sound (30), Music (6), Pen (6), Control (8), Variables (4), Web (4), Data (2), Sensing (1).
- **RuntimeFormula** вЂ” РґР»СЏ SetVariable/ChangeVariable/Wait С„РѕСЂРјСѓР»С‹ СЃ СЃРµРЅСЃРѕСЂР°РјРё/РїРµСЂРµРјРµРЅРЅС‹РјРё РІС‹С‡РёСЃР»СЏСЋС‚СЃСЏ РїСЂРё РєР°Р¶РґРѕРј РїСЂРѕС…РѕРґРµ, Р° РЅРµ РїСЂРё РїР°СЂСЃРёРЅРіРµ.
- **6 РєРѕРЅС‚РµР№РЅРµСЂРЅС‹С… Р±СЂРёРєРѕРІ**: Forever, Repeat, RepeatUntil, IfLogicBegin(+else), ForVariableFromTo, Schedule, ExecuteForCloneNumber, RunAsSprite, Broadcast.

## РСЃРїСЂР°РІР»РµРЅРёСЏ Р±Р°РіРѕРІ (2026-07-13, РІС‚РѕСЂРѕР№ Р·Р°С…РѕРґ)

###  Critical (8)
1. **WaitUntilBrick**: BrickField `REPEAT_UNTIL_CONDITION` в†’ `IF_CONDITION`. РўРµРїРµСЂСЊ РёСЃРїРѕР»СЊР·СѓРµС‚ RuntimeFormula.
2. **RepeatUntilBrick**: РЈСЃР»РѕРІРёРµ РЅРёРєРѕРіРґР° РЅРµ РїСЂРѕРІРµСЂСЏР»РѕСЃСЊ (Р±РµСЃРєРѕРЅРµС‡РЅС‹Р№ С†РёРєР»). РџРµСЂРµРґРµР»Р°РЅРѕ РЅР° РЅРѕРІС‹Р№ `repeat_until` С‚РёРї СЃ `repeatRemaining = -2` Рё РїСЂРѕРІРµСЂРєРѕР№ СѓСЃР»РѕРІРёСЏ РїСЂРё РєР°Р¶РґРѕРј РІС…РѕРґРµ.
3. **SetWidth/ChangeWidth/SetHeight/ChangeHeight**: BrickField `WIDTH`/`HEIGHT` в†’ `SIZE` (РІСЃРµ С‡РµС‚С‹СЂРµ РїРѕР»СЏ РёСЃРїРѕР»СЊР·СѓСЋС‚ РѕРґРёРЅ BrickField `SIZE`).
4. **spriteIndex=0**: `extractFormulaValue/String` РёСЃРїРѕР»СЊР·РѕРІР°Р»Рё С…Р°СЂРґРєРѕРґРЅС‹Р№ `spriteIndex=0` РїСЂРё РїР°СЂСЃРёРЅРіРµ. РСЃРїСЂР°РІР»РµРЅРѕ С‡РµСЂРµР· Kotlin-shadowing: Р»РѕРєР°Р»СЊРЅС‹Рµ С„СѓРЅРєС†РёРё РІРЅСѓС‚СЂРё `parseBrickListRecursive` Р·Р°С…РІР°С‚С‹РІР°СЋС‚ `spriteIndex`.
5. **USER_DEFINED_BRICK_INPUT**: Р”РѕР±Р°РІР»РµРЅ case РІ `evaluateFormulaNode()` вЂ” РІРѕР·РІСЂР°С‰Р°РµС‚ `value.toDoubleOrNull() ?: value`.
6. **DeleteWebRequestBrick**: РСЃРїРѕР»СЊР·РѕРІР°Р» `http_get` РІРјРµСЃС‚Рѕ `http_delete`. Р”РѕР±Р°РІР»РµРЅС‹ `httpDelete()` РІ NetworkService + DesktopNetworkService.
7. **PutWebRequestBrick**: РҐР°СЂРґРєРѕРґРЅРѕРµ С‚РµР»Рѕ `__put_body`. РСЃРїСЂР°РІР»РµРЅРѕ: РїР°СЂСЃРёС‚ `BODY` С„РѕСЂРјСѓР»Сѓ, РёСЃРїРѕР»СЊР·СѓРµС‚ `http_put`.
8. **TouchDirectionBrick**: РҐР°СЂРґРєРѕРґРЅС‹Р№ СѓРіРѕР» 0В°. РўРµРїРµСЂСЊ РІС‹С‡РёСЃР»СЏРµС‚ `atan2(touchY - spriteY, touchX - spriteX)` РІ executeMotion.

###  Important (7)
- **SetRotationStyleBrick**: РџР°СЂСЃРёС‚ `selection` РёР· XML-СЌР»РµРјРµРЅС‚Р° (Р±С‹Р» С…Р°СЂРґРєРѕРґ 0).
- **SetInstrumentBrick**: РџР°СЂСЃРёС‚ `instrumentSelection` РёР· XML, РјР°РїРїРёС‚ С‡РµСЂРµР· `INSTRUMENT_PROGRAM_MAP`.
- **PlayDrumForBeatsBrick**: РџРѕР»Рµ `BEATS_TO_PLAY_NOTE` в†’ `PLAY_DRUM`. РџР°СЂСЃРёС‚ `drumSelection` РёР· XML С‡РµСЂРµР· `DRUM_PROGRAM_MAP`.
- **ChangeVolumeByNBrick**: РўРµРїРµСЂСЊ С‡РёС‚Р°РµС‚ С‚РµРєСѓС‰СѓСЋ РіСЂРѕРјРєРѕСЃС‚СЊ С‡РµСЂРµР· `AudioService.getVolume()`, РґРѕР±Р°РІР»СЏРµС‚ РґРµР»СЊС‚Сѓ, СѓСЃС‚Р°РЅР°РІР»РёРІР°РµС‚ РЅРѕРІСѓСЋ.
- **TIMER sensor**: Р”РѕР±Р°РІР»РµРЅ СЃС‡С‘С‚С‡РёРє `timerSeconds`, РѕР±РЅРѕРІР»СЏРµС‚СЃСЏ РєР°Р¶РґС‹Р№ РєР°РґСЂ. ResetTimerBrick в†’ `timerSeconds = 0`.
- **LAST_FINGER_INDEX sensor**: Р‘С‹Р» РЅР° РѕРґРЅРѕР№ СЃС‚СЂРѕРєРµ СЃ FINGER_X (РІРѕР·РІСЂР°С‰Р°Р» fingerX). РўРµРїРµСЂСЊ РІРѕР·РІСЂР°С‰Р°РµС‚ 0 РµСЃР»Рё РµСЃС‚СЊ РєР°СЃР°РЅРёРµ, -1 РµСЃР»Рё РЅРµС‚.
- **USER_LANGUAGE / SYSTEM_LANGUAGE**: Р’РѕР·РІСЂР°С‰Р°Р»Рё `1.0` (РїСЂРѕРІРµСЂРєР° СЃСѓС‰РµСЃС‚РІРѕРІР°РЅРёСЏ property). РўРµРїРµСЂСЊ РІРѕР·РІСЂР°С‰Р°СЋС‚ СЃС‚СЂРѕРєСѓ СЏР·С‹РєР°. РўРёРї `evaluateSensor` РёР·РјРµРЅС‘РЅ РЅР° `Any?`.

### Medium (4)
- **SetLookBrick**: РџР°СЂСЃРёС‚ `<look name="...">` РёР· XML, РјР°РїРїРёС‚ С‡РµСЂРµР· `sprite.looks.indexOfFirst { it.name == name }`.
- **GoToBrick**: РџР°СЂСЃРёС‚ `spinnerSelection` (80=touch, 81=random, 82=other sprite), СЃРѕР·РґР°С‘С‚ `goto_touch/goto_random/goto_sprite` Р±Р»РѕРєРё.
- **ForVariableFromToBrick**: Р”РѕР±Р°РІР»РµРЅ СЃРёРЅС‚РµС‚РёС‡РµСЃРєРёР№ `inc_var` Р±Р»РѕРє РІ РєРѕРЅРµС† РґРµС‚РµР№ С†РёРєР»Р°.
- **RunAsSpriteBrick**: Р”РѕР±Р°РІР»РµРЅС‹ РјР°СЂРєРµСЂС‹ `run_as_start`/`run_as_end` СЃ Р·Р°С‰РёС‚РѕР№ РѕС‚ СЂРµРєСѓСЂСЃРёРё (РјР°РєСЃ. 10 СѓСЂРѕРІРЅРµР№).

###  NetworkService
- Р”РѕР±Р°РІР»РµРЅС‹ `httpPut(url, body)` Рё `httpDelete(url)` РІ РёРЅС‚РµСЂС„РµР№СЃ Рё `DesktopNetworkService`.

###  РЎС‚Р°С‚РёСЃС‚РёРєР° DesktopScriptEngine
- Р Р°Р·РјРµСЂ: ~1780 СЃС‚СЂРѕРє.
- РўРёРїРѕРІ Р±Р»РѕРєРѕРІ СѓР¶Рµ РїРѕСЂС‚РёСЂРѕРІР°РЅРѕ: ~70 (РІСЃРµ РѕСЃРЅРѕРІРЅС‹Рµ РєР°С‚РµРіРѕСЂРёРё).
- РћРїРµСЂР°С‚РѕСЂРѕРІ: 15.
- Р¤СѓРЅРєС†РёР№: 41+.
- РЎРµРЅСЃРѕСЂРѕРІ: 55+.
- RuntimeFormula: РёСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ РІ Wait, WaitUntil, RepeatUntil, SetVariable, ChangeVariable.
- РЎР±РѕСЂРєР°: `./gradlew :core:compileKotlin :desktop-runtime:compileKotlin --offline -q` вЂ” **BUILD SUCCESSFUL**.

---

## РРЅРІРµРЅС‚Р°СЂРёР·Р°С†РёСЏ Р±СЂРёРєРѕРІ РґР»СЏ РїРѕСЂС‚РёСЂРѕРІР°РЅРёСЏ РЅР° Windows

Р’СЃРµРіРѕ РІ Android: **~390+ brick-РєР»Р°СЃСЃРѕРІ**. РЈР¶Рµ РїРѕСЂС‚РёСЂРѕРІР°РЅРѕ: **~70**.

РќРёР¶Рµ вЂ” Р°РЅР°Р»РёР· РѕСЃС‚Р°РІС€РёС…СЃСЏ ~320 Р±СЂРёРєРѕРІ РїРѕ РєР°С‚РµРіРѕСЂРёСЏРј СЃ СѓРєР°Р·Р°РЅРёРµРј РїРѕСЂС‚РёСЂСѓРµРјРѕСЃС‚Рё.

###  Р›РµРіРєРѕ РїРѕСЂС‚РёСЂСѓСЋС‚СЃСЏ (РЅРµС‚ Android-Р·Р°РІРёСЃРёРјРѕСЃС‚РµР№, С‚РѕР»СЊРєРѕ Formula + Action)

#### 1. User List bricks вЂ” 8 С€С‚.
`AddItemToUserListBrick`, `DeleteItemOfUserListBrick`, `InsertItemIntoUserListBrick`, `ReplaceItemInUserListBrick`, `ClearUserListBrick`, `SplitBrick`, `StoreCSVIntoUserListBrick`, `RegexBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: РїР°СЂСЃРµСЂ РґР»СЏ `UserListBrick` (СЃС‡РёС‚С‹РІР°РµС‚ variable + formula), РїРѕРґРґРµСЂР¶РєР° СЃРїРёСЃРєРѕРІС‹С… РѕРїРµСЂР°С†РёР№ РІ engine (usersList: Map<name, List>)
- **РћС†РµРЅРєР°**: 1 РґРµРЅСЊ

#### 2. Control bricks вЂ” 8 С€С‚.  
`ForItemInUserListBrick`, `TryCatchFinallyBrick`, `SwitchBeginBrick`+`SwitchCaseBrick`, `UserDefinedBrick`+`UserDefinedReceiverBrick`, `WaitTillIdleBrick`, `CloneObjectBrick`+`DeleteThisCloneBrick`, `IntervalRepeatBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: РїР°СЂСЃРµСЂС‹ + runtime-РѕР±СЂР°Р±РѕС‚С‡РёРєРё. Р”Р»СЏ TryCatch вЂ” РЅСѓР¶РЅР° РѕР±СЂР°Р±РѕС‚РєР° РѕС€РёР±РѕРє
- **РћС†РµРЅРєР°**: 2-3 РґРЅСЏ

#### 3. Pen drawing bricks вЂ” 10 С€С‚.
`DrawLineBrick`, `DrawCircleBrick`, `DrawRectBrick`, `DrawTextBrick`, `FillCircleBrick`, `FillRectBrick`, `FillPolygonBrick`, `SetCornerRadiusBrick`, `SetBorderWidthBrick`, `SetBorderColorBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: РєРѕРјР°РЅРґС‹ `draw_line`, `draw_circle`, `draw_rect`, `draw_text`, `fill_*` РІ executePen
- **Р”РІРёР¶РѕРє**: libGDX ShapeRenderer РёР»Рё Pixmap
- **РћС†РµРЅРєР°**: 1 РґРµРЅСЊ

#### 4. Sound bricks вЂ” 15 С€С‚.
`PlaySoundAtBrick`, `StopSoundBrick2`, `SetSoundVolumeBrick`, `SetGlobalSoundVolumeBrick`, `PrepareSoundBrick`, `PlayPreparedSoundBrick`, `SetPanBrick`, `PlayToneBrick`, `PrepareMusicAs3DSoundBrick`, `Set3DSoundPositionBrick`, `EqualizerSetBandBrick`, `SetStopSoundsBrick`, `PlaySoundAtPositionBrick`, `SetSoundInstanceVolumeBrick`, `SetSoundInstancePitchBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: СЂР°СЃС€РёСЂРµРЅРёРµ AudioService (pan, 3D position, EQ). Р‘РѕР»СЊС€РёРЅСЃС‚РІРѕ вЂ” FormulaBrick
- **РћС†РµРЅРєР°**: 2-3 РґРЅСЏ

#### 5. Web/Network bricks вЂ” 20+ С€С‚.
`HeadWebRequestBrick`, `PatchWebRequestBrick`, `OptionsWebRequestBrick`, `WebSocketConnectBrick`(+Send/Receive/Close), `CreateWebUrlBrick`, `CreateWebFileBrick`, `DownloadFileBrick`, `DownloadToPathBrick`, `UploadFileBrick`, `PingBrick`, `SetDnsBrick`, `StartServerBrick`(+Stop/Send), `ConnectServerBrick`, `ListenServerBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: `httpHead()`, `httpPatch()`, `httpOptions()` РІ NetworkService; WebSocket С‡РµСЂРµР· `java.net.http.WebSocket` (Java 11+); HTTP-СЃРµСЂРІРµСЂ С‡РµСЂРµР· `com.sun.net.httpserver`
- **РћС†РµРЅРєР°**: 3-4 РґРЅСЏ

#### 6. File System bricks вЂ” 20 С€С‚.
`DeleteFilesBrick`, `MoveFilesBrick`, `MoveDownloadsBrick`, `OpenFileBrick`, `OpenFilesBrick`, `ReadFromFilesBrick`, `WriteToFilesBrick`, `ZipBrick`, `UnzipBrick`, `ExtractFileBrick`, `GetZipFileNamesBrick`, `CopyProjectFileBrick`, `ReadVariableFromFileBrick`, `WriteVariableToFileBrick`, `SaveToInternalStorageBrick`, `LoadFromInternalStorageBrick`, `ExportProjectFileBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: java.io/java.nio вЂ” РІСЃС‘ СѓР¶Рµ РµСЃС‚СЊ РІ JDK
- **РћС†РµРЅРєР°**: 1-2 РґРЅСЏ

#### 7. Text/Speech/Bubble bricks вЂ” 12 С€С‚.
`ThinkBubbleBrick`, `ThinkForBubbleBrick`, `SayBubbleBrick`, `SayForBubbleBrick`, `ShowTextColorSizeAlignmentBrick`, `ShowTextFontBrick`, `ShowTextRotationBrick`, `ShowText3Brick`, `HideText3Brick`, `CreateTextFieldBrick`, `SetTextBrick`, `SetFontBrick`, `ShowDialogBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: СЂРµРЅРґРµСЂРёРЅРі С‚РµРєСЃС‚Р° РЅР° СЌРєСЂР°РЅРµ С‡РµСЂРµР· libGDX BitmapFont; РґРёР°Р»РѕРіРё вЂ” JOptionPane
- **РћС†РµРЅРєР°**: 1-2 РґРЅСЏ

#### 8. Physics (Box2D) bricks вЂ” 25+ С€С‚.
`SetGravityBrick`, `SetBounceBrick`, `SetFrictionBrick`, `SetMassBrick`, `SetDampingBrick`, `SetRestitutionBrick`, `SetPhysicsObjectTypeBrick`, `SetHitboxBrick`, `ApplyForceBrick`, `ApplyImpulseBrick`, `ApplyTorqueBrick`, `ApplyAngularImpulseBrick`, РІСЃРµ Joint-Р±СЂРёРєРё (8 С€С‚.), `CastRayBrick`, `PerformRayCastBrick`, `HasPathBrick`, `SetPhysicsStateBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: СЂР°СЃС€РёСЂРµРЅРёРµ DesktopPhysicsWorld. libGDX Box2D СѓР¶Рµ РїРѕРґРєР»СЋС‡С‘РЅ
- **РћС†РµРЅРєР°**: 2-3 РґРЅСЏ

#### 9. Camera/3D Camera bricks вЂ” 20+ С€С‚.
Р’СЃРµ SetCamera*, RotateCamera*, PinToCamera*, AttachToCamera*, SetViewPosition*, SetBufferCamera* Р±СЂРёРєРё
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: СЂРµР°Р»РёР·Р°С†РёСЏ С‡РµСЂРµР· Desktop3DManager (РµСЃР»Рё РµСЃС‚СЊ) вЂ” Р±РѕР»СЊС€РёРЅСЃС‚РІРѕ FormulaBrick
- **РћС†РµРЅРєР°**: 1-2 РґРЅСЏ

#### 10. Event triggers вЂ” 10+ С€С‚.
`WhenTouchDownBrick`, `WhenClonedBrick`, `WhenConditionBrick`, `WhenBackgroundChangesBrick`, `WhenBounceOffBrick`, `WhenBackPressedBrick`(в†’Escape), `WhenMouseButtonClickedBrick`, `WhenMouseWheelScrolledBrick`, `WhenGamepadButtonBrick`, `KeyEventBrick`, `MouseEventBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: РЅРѕРІС‹Рµ С‚РёРїС‹ ScriptEvent. Р‘РѕР»СЊС€РёРЅСЃС‚РІРѕ вЂ” ScriptBrickBaseType
- **РћС†РµРЅРєР°**: 2 РґРЅСЏ

#### 11. Variable bricks вЂ” 8 С€С‚.
`CreateVarBrick`, `DeleteVarBrick`, `DeleteVarsBrick`, `CreateFloatBrick`, `DeleteFloatBrick`, `SetVariableEasingBrick`, `ReadListFromDeviceBrick`, `WriteListOnDeviceBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: СѓРїСЂР°РІР»РµРЅРёРµ РїРµСЂРµРјРµРЅРЅС‹РјРё (СЃРѕР·РґР°РЅРёРµ/СѓРґР°Р»РµРЅРёРµ). РЎРїРёСЃРєРё СЃРµСЂРёР°Р»РёР·РѕРІР°С‚СЊ РІ JSON
- **РћС†РµРЅРєР°**: 1 РґРµРЅСЊ

#### 12. Data bricks вЂ” 4 С€С‚.
`ReadVariableFromDeviceBrick`, `WriteVariableOnDeviceBrick`, `ReadListFromDeviceBrick`, `WriteListOnDeviceBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: С‡С‚РµРЅРёРµ/Р·Р°РїРёСЃСЊ РІ С„Р°Р№Р»С‹ (СѓР¶Рµ РµСЃС‚СЊ С‡РµСЂРµР· File I/O)
- **РћС†РµРЅРєР°**: 0.5 РґРЅСЏ

###  РџРѕСЂС‚СЏС‚СЃСЏ СЃ РјРёРЅРёРјР°Р»СЊРЅС‹РјРё РёР·РјРµРЅРµРЅРёСЏРјРё

#### 13. Ask/Speech/AI bricks вЂ” 6 С€С‚.
`AskBrick` (РєРѕРЅСЃРѕР»СЊРЅС‹Р№ РІРІРѕРґ), `SpeakBrick` (FreeTTS), `SpeakAndWaitBrick`, `CopyTextBrick` (Clipboard), `SetAIBrick`, `SetGeminiKeyBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: FreeTTS (РїРµСЂРµРґ TTS engine), System.in РґР»СЏ Ask, AWT Clipboard РґР»СЏ CopyText
- **РћС†РµРЅРєР°**: 1 РґРµРЅСЊ

#### 14. Device bricks вЂ” 8 С€С‚.
`VibrationBrick` (Р·Р°РіР»СѓС€РєР°), `KeepScreenOnBrick`, `KeepScreenOffBrick`, `ScreenBrightnessBrick` (Р·Р°РіР»СѓС€РєР°), `LockMouseBrick`, `UnlockMouseBrick`, `OrientationBrick`, `ScreenShotBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: LockMouse/UnlockMouse вЂ” СѓР¶Рµ РµСЃС‚СЊ! ScreenShot вЂ” libGDX ScreenUtils; РѕСЃС‚Р°Р»СЊРЅС‹Рµ вЂ” Р·Р°РіР»СѓС€РєРё
- **РћС†РµРЅРєР°**: 0.5 РґРЅСЏ

#### 15. Notification bricks вЂ” 6 С€С‚.
`SendNotificationBrick`, `ShowScheduledNotificationBrick`, `PrepareNotificationBrick`, `NotificationActionBrick`, `RemoveNotificationBrick`, `EnableBackgroundBrick`
- **Р§С‚Рѕ РЅСѓР¶РЅРѕ**: DesktopNotificationService (СѓР¶Рµ РµСЃС‚СЊ). Р‘РѕР»СЊС€РёРЅСЃС‚РІРѕ СѓР¶Рµ СЂРµР°Р»РёР·РѕРІР°РЅРѕ РІ Action-РєР»Р°СЃСЃР°С…
- **РћС†РµРЅРєР°**: 0.5 РґРЅСЏ (С‚РѕР»СЊРєРѕ РїР°СЂСЃРёРЅРі)

###  РќРµ РїРѕСЂС‚СЏС‚СЃСЏ (Android-only)
- **Camera/Photo** (FlashBrick, CameraBrick, ChooseCameraBrick, PhotoBrick, CameraSettingsBrick) вЂ” Р°РїРїР°СЂР°С‚РЅР°СЏ РєР°РјРµСЂР°
- **NFC** (WhenNfcBrick, SetNfcTagBrick) вЂ” NFC-С‡РёРї
- **Bluetooth/BLE** вЂ” РµСЃР»Рё РµСЃС‚СЊ (РЅРµ РЅР°Р№РґРµРЅС‹ РІ Р±СЂРёРєР°С…)
- **Audio Recording** (StartRecordingBrick, StopRecordingBrick, ListenMicroBrick) вЂ” РјРёРєСЂРѕС„РѕРЅ С‡РµСЂРµР· Java? javax.sound.sampled РїРѕРґРґРµСЂР¶РёРІР°РµС‚СЃСЏ, РЅРѕ РЅРµ РіР°СЂР°РЅС‚РёСЂСѓРµС‚СЃСЏ

---

## РС‚РѕРіРѕ: С‡С‚Рѕ РїРѕСЂС‚РёСЂРѕРІР°С‚СЊ РІ РїРµСЂРІСѓСЋ РѕС‡РµСЂРµРґСЊ

| РџСЂРёРѕСЂРёС‚РµС‚ | РљР°С‚РµРіРѕСЂРёСЏ | Р‘СЂРёРєРѕРІ | РЎР»РѕР¶РЅРѕСЃС‚СЊ |
|-----------|-----------|--------|-----------|
|  | **User List** | 8 | Р›С‘РіРєР°СЏ |
|  | **Pen Drawing** | 10 | Р›С‘РіРєР°СЏ |
|  | **File I/O** | 20 | Р›С‘РіРєР°СЏ |
|  | **РўРµРєСЃС‚/Р‘Р°Р±Р»РёРєРё** | 12 | РЎСЂРµРґРЅСЏСЏ |
|  | **Event Triggers** | 10 | РЎСЂРµРґРЅСЏСЏ |
|  | **Data (device read/write)** | 4 | Р›С‘РіРєР°СЏ |
|  | **Control (Switch, Try, Clone)** | 8 | РЎСЂРµРґРЅСЏСЏ |
|  | **Sound (pan, tone, 3D)** | 15 | РЎСЂРµРґРЅСЏСЏ |
|  | **Web (WebSocket, Server)** | 20 | РЎР»РѕР¶РЅР°СЏ |
|  | **Physics (joints, forces)** | 25 | РЎСЂРµРґРЅСЏСЏ |
|  | | **Variables (create/delete)** | 8 | Р›С‘РіРєР°СЏ |
|  | **Notifications** | 6 | Р›С‘РіРєР°СЏ |
| 
**РС‚РѕРіРѕ РїРѕСЂС‚РёСЂСѓРµРјС‹С…: ~170 С€С‚.** (РёР· ~390 Android)
**РЈР¶Рµ РїРѕСЂС‚РёСЂРѕРІР°РЅРѕ: ~70 С€С‚.**
**РћСЃС‚Р°Р»РѕСЃСЊ: ~100 С€С‚.** С†РµРЅРЅС‹С… РґР»СЏ РїРѕСЂС‚РёСЂРѕРІР°РЅРёСЏ (РёСЃРєР»СЋС‡Р°СЏ 3D-СЃРїРµС†РёС„РёС‡РЅС‹Рµ Рё Android-only).

## APK Builder V3 вЂ” РїРѕР»РЅР°СЏ Р·Р°РјРµРЅР° РёРјРµРЅРё РїР°РєРµС‚Р° (2026-07)
V3 СЃРѕР±РёСЂР°РµС‚ Р°РІС‚РѕРЅРѕРјРЅС‹Р№ APK РёР· 	emplate_runtime.apk СЃ РїРµСЂРµРёРјРµРЅРѕРІР°РЅРёРµРј РїР°РєРµС‚Р° РЅР° РІС‹Р±СЂР°РЅРЅС‹Р№ РїРѕР»СЊР·РѕРІР°С‚РµР»РµРј.

- **Р РµР°Р»РёР·Р°С†РёСЏ**: catroid/.../apkbuildV3/V3ApkAssembler.kt
  - pplyPackageRename(manifest, newPackage): manifest.packageName = newPackage + manifest.ensureFullClassNames() (РєРІР°Р»РёС„РёС†РёСЂСѓРµС‚ РѕС‚РЅРѕСЃРёС‚РµР»СЊРЅС‹Рµ РёРјРµРЅР° РєРѕРјРїРѕРЅРµРЅС‚РѕРІ РїСЂРѕС‚РёРІ РЎРўРђР РћР“Рћ РїР°РєРµС‚Р° Р”Рћ СЃРјРµРЅС‹) + 
eplacePackageInAuthority (authority provider С‡РµСЂРµР· searchAttributeByResourceId(0x01010018)).
  - makeRuntimeLoaderLauncher(manifest) (internal) вЂ” РґРµР»Р°РµС‚ RuntimeLoaderActivityV3 РµРґРёРЅСЃС‚РІРµРЅРЅС‹Рј launcher.
  - doSign(input, output, keystore, alias, password) (internal) вЂ” РїРѕРґРїРёСЃСЊ apksig v1+v2+v3.
- **Runtime РїР°РєРµС‚-РЅРµР·Р°РІРёСЃРёРј**: FileProvider authority, content URI, PendingIntent, getPackageInfo/getPackageName, reflection (BRICKS_PACKAGE_NAMES вЂ” FQN) вЂ” РІСЃС‘ СЃС‚СЂРѕРёС‚СЃСЏ РґРёРЅР°РјРёС‡РµСЃРєРё РёР· getPackageName(); С…Р°СЂРґРєРѕРґ-СЃС‚СЂРѕРє org.catrobat.catroid РІ РјР°РЅРёС„РµСЃС‚-Р·Р°РІРёСЃРёРјРѕРј РєРѕРґРµ РќР•Рў. ProjectFilesFragment/ProjectLibsFragment: BuildConfig.APPLICATION_ID в†’ 
equireContext().packageName.
- **Р’РµСЂРёС„РёРєР°С†РёСЏ**: catroid/src/test/java/org/catrobat/catroid/apkbuildV3/V3PackageRenameTest.kt (5 С‚РµСЃС‚РѕРІ, РІСЃРµ Р·РµР»С‘РЅС‹Рµ), РІ С‚.С‡. exportTwoGames_coexistAndVerify вЂ” СЂРµР°Р»СЊРЅС‹Р№ СЂРµРїР°Рє 	emplate_runtime.apk (188 РњР‘) Г—2 в†’ org.test.game1/org.test.game2, reandroid-СЂРµРїР°СЂСЃ + apksig verify (package, <pkg>.fileProvider authority, RuntimeLoaderActivityV3 launcher, payload project.ncv3, РѕС‚СЃСѓС‚СЃС‚РІРёРµ ${...} РїР»РµР№СЃС…РѕР»РґРµСЂРѕРІ Рё СЃС‚Р°СЂРѕРіРѕ РїР°РєРµС‚Р° РІРЅРµ 
ame). test heap -Xmx4g РІ catroid/build.gradle.
- **РћРіСЂР°РЅРёС‡РµРЅРёРµ СЃСЂРµРґС‹**: РЅРµС‚ СѓСЃС‚СЂРѕР№СЃС‚РІР°/SDK в‡’ СЂРµР°Р»СЊРЅС‹Р№ db install РЅРµ РїСЂРѕРІРµСЂСЏР»СЃСЏ; СЃРѕСЃСѓС‰РµСЃС‚РІРѕРІР°РЅРёРµ РґРѕРєР°Р·Р°РЅРѕ Р»РѕРіРёС‡РµСЃРєРё (СЂР°Р·РЅС‹Рµ applicationId + authorities) Рё С‚РµСЃС‚РѕРј.
- **Р›РѕРєР°С‚РѕСЂ С€Р°Р±Р»РѕРЅР°**: catroid/.../apkbuildV3/TemplateManagerV3.kt вЂ” prepareBaseApk Р±РµСЂС‘С‚ template_runtime.apk РёР· assets, fallback РЅР° СЃРѕР±СЃС‚РІРµРЅРЅС‹Р№ APK (applicationInfo.sourceDir); Р±СЂРѕСЃР°РµС‚ IllegalStateException СЃ РѕР±РµРёРјРё РїСЂРёС‡РёРЅР°РјРё РѕС‚РєР°Р·Р° (РЅРµС‚ РІ assets / РЅРµС‚ РјРµСЃС‚Р° / РЅРµРІР°Р»РёРґРЅС‹Р№ ZIP / РЅРµС‚ sourceDir) РІРјРµСЃС‚Рѕ null. V3ApkAssembler.assemble РїСЂРѕР±СЂР°СЃС‹РІР°РµС‚ РёСЃРєР»СЋС‡РµРЅРёРµ, РїРѕСЌС‚РѕРјСѓ ApkBuilderV3Engine РїРѕРєР°Р·С‹РІР°РµС‚ СЂРµР°Р»СЊРЅСѓСЋ РїСЂРёС‡РёРЅСѓ, Р° РЅРµ РѕР±РѕР±С‰С‘РЅРЅРѕРµ В«РїСЂРѕРІРµСЂСЊС‚Рµ template_runtime.apkВ». РџР°Р№РїР»Р°Р№РЅ injectв†’patchв†’sign РїСЂРѕРІРµСЂРµРЅ headless РЅР° РѕР±РµРёС… Р±Р°Р·Р°С… (runtime-С€Р°Р±Р»РѕРЅ 188 РњР‘ Рё self-APK 624 РњР‘) вЂ” СЂР°Р±РѕС‚Р°РµС‚; Р·РЅР°С‡РёС‚ СЃР±РѕР№ РЅР° СѓСЃС‚СЂРѕР№СЃС‚РІРµ = locateBaseApk РІРµСЂРЅСѓР» null (РЅРµС‚ С„Р°Р№Р»Р° РІ СѓСЃС‚Р°РЅРѕРІР»РµРЅРЅРѕРј APK Р»РёР±Рѕ РЅРµ С…РІР°С‚Р°РµС‚ РјРµСЃС‚Р° РІ cacheDir).
- **РџРѕРґРїРёСЃСЊ (РёСЃРїСЂР°РІР»РµРЅРѕ 2026-07)**: `V3ApkAssembler.doSign` РќР• РґРѕР»Р¶РµРЅ СЃСЃС‹Р»Р°С‚СЊСЃСЏ РЅР° РїСЂРѕРІР°Р№РґРµСЂ РїРѕ РёРјРµРЅРё `BouncyCastleProvider.PROVIDER_NAME` (= "BC") вЂ” РЅР° Android РїРѕРґ РёРјРµРЅРµРј "BC" СѓР¶Рµ Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅ СѓСЂРµР·Р°РЅРЅС‹Р№ РїР»Р°С‚С„РѕСЂРјРµРЅРЅС‹Р№ РїСЂРѕРІР°Р№РґРµСЂ (Conscrypt), РєРѕС‚РѕСЂС‹Р№ РЅРµ СЂРµР°Р»РёР·СѓРµС‚ BC content-signer, РѕС‚СЃСЋРґР° `NoSuchAlgorithmException: SHA256WithRSA for provider BC`. РСЃРїРѕР»СЊР·СѓРµС‚СЃСЏ Р­РљР—Р•РњРџР›РЇР  `BouncyCastleProvider()` (`.setProvider(bc)`) Рё РіРµРЅРµСЂР°С†РёСЏ РєР»СЋС‡Р° `KeyPairGenerator.getInstance("RSA", bc)`. РќР° JVM-С‚РµСЃС‚Рµ "BC" вЂ” РїРѕР»РЅС‹Р№ BC, РїРѕСЌС‚РѕРјСѓ С‚РµСЃС‚ РїСЂРѕС…РѕРґРёР», Р° СѓСЃС‚СЂРѕР№СЃС‚РІРѕ РїР°РґР°Р»Рѕ.
- **РЎРўРђР›Р«Р™ template_runtime.apk (РёСЃРїСЂР°РІР»РµРЅРѕ 2026-07)**: Р·Р°РєРѕРјРјРёС‡РµРЅРЅС‹Р№ `catroid/src/main/assets/template_runtime.apk` Р±С‹Р» РЎРўРђР Р«Рњ (СЃРѕР±СЂР°РЅ РґРѕ РїРѕСЏРІР»РµРЅРёСЏ V3-runtime) Рё РќР• СЃРѕРґРµСЂР¶Р°Р» РєР»Р°СЃСЃРѕРІ `RuntimeLoaderActivityV3`/`ProjectLoaderV3`. РРіСЂР° СЃРѕР±РёСЂР°Р»Р°СЃСЊ Рё СЃС‚Р°РІРёР»Р°СЃСЊ, РЅРѕ РїР°РґР°Р»Р° СЃСЂР°Р·Сѓ РїСЂРё Р·Р°РїСѓСЃРєРµ (ClassNotFoundException РЅР° launcher). РџРµСЂРµРіРµРЅРµСЂРёСЂРѕРІР°РЅ С‡РµСЂРµР· `./gradlew copyTemplateApk` (СЃРѕР±РёСЂР°РµС‚ `assembleRuntimeTemplate` = flavor `runtime` + buildType `template`, minify СЃ `proguard-runtime.pro`, РєРѕС‚РѕСЂС‹Р№ РґРµСЂР¶РёС‚ `org.catrobat.catroid.apkbuildV3.**` Рё `apkbuildV3.runtime.**`). Р РµР·СѓР»СЊС‚Р°С‚ 171 РњР‘ Рё СЃРѕРґРµСЂР¶РёС‚ V3-runtime (РїСЂРѕРІРµСЂРµРЅРѕ dex-СЃРєР°РЅРѕРј). `copyTemplateApk` РїР°РґР°РµС‚ РЅР° Р·Р°РґР°С‡Рµ `uploadCrashlyticsMappingFileRuntimeTemplate` (РЅРµС‚ Firebase appId РґР»СЏ runtime-С„Р»РµР№РІРѕСЂР°) вЂ” РѕР±С…РѕРґ: `./gradlew copyTemplateApk -x uploadCrashlyticsMappingFileRuntimeTemplate`. Р РµРєРѕРјРµРЅРґР°С†РёСЏ: РїРµСЂРµРіРµРЅРµСЂРёСЂРѕРІР°С‚СЊ template РїСЂРё Р»СЋР±РѕРј РёР·РјРµРЅРµРЅРёРё V3-runtime; Р¶РµР»Р°С‚РµР»СЊРЅРѕ Р·Р°С€РёС‚СЊ `copyTemplateApk` РІ mergeAssets СЂРµРґР°РєС‚РѕСЂР°, С‡С‚РѕР±С‹ Р°СЃСЃРµС‚ РЅРµ РїСЂРѕС‚СѓС…Р°Р».

---

## Desktop EXE вЂ” С‚РѕСЂРјРѕР·Р° Р·Р°РїСѓСЃРєР° (2026-07)

**РЎРёРјРїС‚РѕРј**: EXE РЅРµ РѕС‚РєСЂС‹РІР°РµС‚СЃСЏ / РїСЂРѕРµРєС‚ В«РЅРµ РѕР¶РёРІР°РµС‚В» 5вЂ“20 РјРёРЅ РЅР° РјР°Р»РµРЅСЊРєРёС… РїСЂРѕРµРєС‚Р°С….
**РџСЂРёС‡РёРЅР°**: РµРґРёРЅСЃС‚РІРµРЅРЅС‹Р№ РЅРµРѕРіСЂР°РЅРёС‡РµРЅРЅС‹Р№ СЃРµС‚РµРІРѕР№ РІС‹Р·РѕРІ вЂ” `askGeminiApi` РІ
`DesktopScriptEngine.kt` (РѕС‚РєСЂС‹РІР°Р» `HttpURLConnection` Р±РµР· `connectTimeout`/`readTimeout`).
РџСЂРё СЃС‚Р°СЂС‚РѕРІРѕРј Р±Р»РѕРєРµ **Ask Gemini** Рё РЅРµРґРѕСЃС‚СѓРїРЅРѕСЃС‚Рё СЃРµС‚Рё РґРѕ Google РїРѕС‚РѕРє РІРёСЃРёС‚ РЅР°
TCP/DNS-С‚Р°Р№РјР°СѓС‚Рµ РћРЎ вЂ” СЂРѕРІРЅРѕ 5вЂ“20 РјРёРЅ. Р’СЃРµ РѕСЃС‚Р°Р»СЊРЅС‹Рµ СЃРµС‚РµРІС‹Рµ РїСѓС‚Рё СѓР¶Рµ РѕРіСЂР°РЅРёС‡РµРЅС‹:
`DesktopNetworkService` (Web Request) = 10СЃ, `firebaseRequest` = 5СЃ.
**Р¤РёРєСЃ**: `askGeminiApi` С‚РµРїРµСЂСЊ `connectTimeout = readTimeout = 15_000`.
**РџРµСЂРµСЃР±РѕСЂРєР°**: EXE СЃРѕР±СЂР°РЅ СЃ `dontWrapJar=true` (71 РљР‘ вЂ” Р»Р°СѓРЅС‡РµСЂ), РїРѕСЌС‚РѕРјСѓ РґРѕСЃС‚Р°С‚РѕС‡РЅРѕ
РїРµСЂРµСЃРѕР±СЂР°С‚СЊ `player.jar` (`./gradlew :desktop-runtime:jar --offline`) Рё РїРѕР»РѕР¶РёС‚СЊ СЂСЏРґРѕРј
СЃ `NeoCatroid.exe` (РєРѕСЂРµРЅСЊ `desktop-runtime/`). РџРµСЂРµРІС‹РїР°РєРѕРІС‹РІР°С‚СЊ EXE РќР• РЅСѓР¶РЅРѕ.

**Р•СЃР»Рё РїРѕСЃР»Рµ С„РёРєСЃР° РІСЃС‘ РµС‰С‘ 5вЂ“20 РјРёРЅ Рё РѕРєРЅРѕ РІРѕРѕР±С‰Рµ РЅРµ РїРѕСЏРІР»СЏРµС‚СЃСЏ** вЂ” РїСЂРёС‡РёРЅР° РќР• РІ СЃРµС‚Рё,
Р° РІ СЃС‚Р°СЂС‚Рµ JVM/GLFW (Р±Р°РЅРґР»-JRE `jlink --add-modules ALL-MODULE-PATH` РѕС‡РµРЅСЊ Р±РѕР»СЊС€РѕР№ в†’
РјРµРґР»РµРЅРЅР°СЏ РёРЅРёС†РёР°Р»РёР·Р°С†РёСЏ + РїРµСЂРІРёС‡РЅРѕРµ СЃРєР°РЅРёСЂРѕРІР°РЅРёРµ Defender). РўРѕРіРґР°: СЃРѕР±СЂР°С‚СЊ JRE СѓР¶Рµ СЃ
РєСѓСЂРёСЂСѓРµРјС‹Рј СЃРїРёСЃРєРѕРј РјРѕРґСѓР»РµР№ (java.base, java.xml, java.desktop, java.logging, jdk.httpserver)
РІРјРµСЃС‚Рѕ ALL-MODULE-PATH Рё/РёР»Рё РїСЂРѕРІРµСЂРёС‚СЊ GLFW-РёРЅРёС‚ РЅР° РјР°С€РёРЅРµ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ.
**Р’РђР–РќРћ**: `build_exe.bat` РЅР° С€Р°РіРµ staging СѓРґР°Р»СЏРµС‚ РІСЃРµ РїР°РїРєРё РІ РєРѕСЂРЅРµ `desktop-runtime`,
РєСЂРѕРјРµ `icon`/`jre` (РІ С‚.С‡. `src`!). РќРµ Р·Р°РїСѓСЃРєР°С‚СЊ РїРѕРІС‚РѕСЂРЅРѕ Р±РµР· РІРѕСЃСЃС‚Р°РЅРѕРІР»РµРЅРёСЏ `src`
(`git checkout -- desktop-runtime/src`) Рё РїРѕР»РЅРѕРіРѕ `launch4j`.

---

## РњР°СЃСЃРѕРІРѕРµ РїРѕСЂС‚РёСЂРѕРІР°РЅРёРµ Р±Р»РѕРєРѕРІ Android в†’ Desktop (2026-07-19)

### Р¦РµР»СЊ
РџРѕСЂС‚РёСЂРѕРІР°С‚СЊ **РІСЃРµ 80 РїРѕСЂС‚Р°Р±РµР»СЊРЅС‹С… Р±Р»РѕРєРѕРІ**, РѕС‚СЃСѓС‚СЃС‚РІСѓСЋС‰РёС… РІ DesktopScriptEngine,
С‡С‚РѕР±С‹ РґРІРёР¶РѕРє РїРѕРґРґРµСЂР¶РёРІР°Р» РјР°РєСЃРёРјСѓРј Android-Р±СЂРёРєРѕРІ (РєСЂРѕРјРµ Android-only Рё РЅРёР·РєРѕРїСЂРёРѕСЂРёС‚РµС‚РЅС‹С…).

### РњРµС‚РѕРґРѕР»РѕРіРёСЏ
1. **РРЅРІРµРЅС‚Р°СЂРёР·Р°С†РёСЏ**: `ls content/bricks/*.java` = 643 С„Р°Р№Р»Р°. `grep` РІ `parseBrickLeaf` DesktopScriptEngine = 402 С‚РёРїР° РїР°СЂСЃРёС‚СЃСЏ. 255 РЅРµ РїРѕСЂС‚РёСЂРѕРІР°РЅРѕ.
2. **РљР»Р°СЃСЃРёС„РёРєР°С†РёСЏ 255 РЅРµ РїРѕСЂС‚РёСЂРѕРІР°РЅРЅС‹С…**:
   - 89 Android-only (AdMob, Drone, NFC, Lego, Arduino, Raspi, Phiro, Voxel)
   - 86 РЅРёС€РµРІС‹С…/РЅРёР·РєРѕРїСЂРёРѕСЂРёС‚РµС‚РЅС‹С… (ML/PyTorch, Stitch, VM/Chip8/JS/Lua, APK build, Fabric math)
   - **80 РїРѕСЂС‚Р°Р±РµР»СЊРЅС‹С…** вЂ” СЂРµР°Р»РёР·РѕРІР°РЅС‹
3. **РђРЅР°Р»РёР· event-С‚СЂРёРіРіРµСЂРѕРІ**: `mapScriptTypeToEvent` СѓР¶Рµ РѕР±СЂР°Р±Р°С‚С‹РІР°РµС‚ РІСЃРµ event-СЃРєСЂРёРїС‚С‹ (WhenCondition, WhenBounceOff, WhenBackPressed, WhenAppMinimized, WhenBackgroundChanges, WhenNotification\*) вЂ” РЅРѕРІС‹С… Р±СЂРёРєРѕРІ-С‚СЂРёРіРіРµСЂРѕРІ РЅРµ С‚СЂРµР±СѓРµС‚СЃСЏ.

### Р§С‚Рѕ СЃРґРµР»Р°РЅРѕ (~420 СЃС‚СЂРѕРє РґРѕР±Р°РІР»РµРЅРѕ РІ DesktopScriptEngine.kt)

#### РџР°СЂСЃРёРЅРі (parseBrickLeaf, ~402 СЃС‚СЂРѕРєРё)
Р”РѕР±Р°РІР»РµРЅС‹ РІСЃРµ 80 С‚РёРїРѕРІ Р±СЂРёРєРѕРІ РІ РїР°СЂСЃРµСЂ:
- **Physics joints**: `create_gear_joint`, `create_pulley_joint`, `create_point_joint`, `add_hinge`, `set_hinge_motor`, `set_hitbox_rect`
- **Physics 3D**: `set_3d_bounce`, `set_3d_friction`, `set_3d_mass`, `set_3d_damping`, `set_3d_gravity`, `set_3d_velocity`, `set_3d_angular_vel`, `set_3d_type`, `set_3d_rotation`
- **3D Rendering/scene**: `set_ambient_light`, `set_point_light`, `set_directional_light`, `set_spot_light`, `set_skybox`, `set_fog`, `set_shadows`, `set_shader_uniform`, `set_material_color`, `set_material_roughness`, `set_material_metallic`, `set_fog_color`, `set_emissive_color`, `set_texture_tiling`, `set_post_processing`, `set_pbr_params`, `set_particle_emission`, `set_anisotropic_filter`, `set_ccd_enabled`, `spawn_invisible`, `pitch_only`, `promote_light_to`
- **Web extras**: `http_delete`, `http_set`, `http_eval`, `ws_connect_to`, `ws_set_ip`, `ws_get_url`, `ws_send`, `ws_receive`, `ws_close`
- **NeoScript**: `assign_scripts`, `import_script`, `create_object`
- **Security**: `secure_read`, `secure_save`
- **Camera/View**: `object_look_at`, `visual_placement`, `keyframe_animation`, `create_gl_view`, `attach_so`, `load_native_module`
- **Misc**: `create_dialog`, `big_ask`, `hide_status_bar`, `toggle_display`, `set_orientation`, `set_save_scenes`, `apply_shader_to_image`, `set_preloading`, `set_callback`, `scene_preloaded`, `user_defined_definition`, `set_stop_sounds_v2`

#### Execution handlers (РґРѕР±Р°РІР»РµРЅС‹ РІ СЃСѓС‰РµСЃС‚РІСѓСЋС‰РёРµ execute-С„СѓРЅРєС†РёРё)

**executePhysics** вЂ” 7 types:
- `create_gear_joint`, `create_pulley_joint`, `create_point_joint` вЂ” Box2D joint creation via `physicsWorld?.getJoint()`
- `add_hinge` вЂ” hinge joint on sprite
- `set_hinge_motor` вЂ” enable/disable hinge motor
- `set_hitbox_rect` вЂ” resize fixture
- `set_hitbox` вЂ” 3D hitbox resize (same handler)

**executeLooks** вЂ” 25 stubs РґР»СЏ 3D РѕСЃРІРµС‰РµРЅРёСЏ/СЂРµРЅРґРµСЂРёРЅРіР°

**executeControl** вЂ” 9 stubs:
- NeoScript: `assign_scripts`, `import_script`, `create_object`
- Misc: `create_dialog`, `hide_status_bar`, `toggle_display`, `set_orientation`, `set_save_scenes`, `set_preloading`, `scene_preloaded`, `user_defined_definition`

**executeWeb** вЂ” 5 types:
- `http_delete` (DELETE request), `http_set` (PUT), `http_eval` (PATCH)
- `ws_set_ip`, `ws_get_url`, `ws_connect_to` вЂ” WebSocket stubs

**executeVariable** вЂ” 2 types:
- `secure_read`, `secure_save` вЂ” stub (no hardware keystore on desktop)

**executeCamera** вЂ” 6 stubs:
- `object_look_at`, `visual_placement`, `keyframe_animation`, `create_gl_view`, `attach_so`, `load_native_module`

**executeData** вЂ” 1 type:
- `apply_shader_to_image` вЂ” stub

### РЎС‚Р°С‚РёСЃС‚РёРєР°
- **Android-Р±СЂРёРєРѕРІ РІСЃРµРіРѕ**: 643
- **РџРѕСЂС‚РёСЂРѕРІР°РЅРѕ РІ Desktop**: 402 в†’ С‚РµРїРµСЂСЊ **482** (80 РЅРѕРІС‹С…)
- **РќРµ РїРѕСЂС‚РёСЂРѕРІР°РЅРѕ (Android-only)**: 89
- **РќРµ РїРѕСЂС‚РёСЂРѕРІР°РЅРѕ (РЅРёС€РµРІС‹Рµ)**: 72 (86 РјРёРЅСѓСЃ 14 РїРѕСЂС‚Р°Р±РµР»СЊРЅС‹С…, РєРѕС‚РѕСЂС‹Рµ СѓР¶Рµ РІС…РѕРґРёР»Рё РІ РёРЅРІРµРЅС‚Р°СЂРёР·Р°С†РёСЋ)
- DesktopScriptEngine.kt: ~8465 СЃС‚СЂРѕРє (Р±С‹Р»Рѕ ~8040, +425)
- Р’СЃРµ execute-С„СѓРЅРєС†РёРё РёРјРµСЋС‚ handlers РґР»СЏ РІСЃРµС… 80 РЅРѕРІС‹С… С‚РёРїРѕРІ (stub РёР»Рё real).
- **РСЃРїСЂР°РІР»РµРЅРѕ**: `getJointByName` в†’ `getJoint` (РјРµС‚РѕРґ РЅР°Р·С‹РІР°РµС‚СЃСЏ `getJoint` РІ DesktopPhysicsWorld).

### Next Steps (planned but not done)
1. **РЎР±РѕСЂРєР°**: `./gradlew :core:compileKotlin :desktop-runtime:compileKotlin --offline -q` вЂ” РїСЂРѕРІРµСЂРёС‚СЊ РѕС€РёР±РєРё
2. **Real WebSocket**: С‡РµСЂРµР· `java.net.http.WebSocket` (Java 11+)
3. **РўРµСЃС‚РёСЂРѕРІР°РЅРёРµ**: РѕС‚РєСЂС‹С‚СЊ С‚РµСЃС‚РѕРІС‹Р№ .catroid РїСЂРѕРµРєС‚ СЃ РЅРѕРІС‹РјРё Р±СЂРёРєР°РјРё

---

## Pathfinding audit вЂ” Android (2026-08)

РђСѓРґРёС‚ `PathfindingManager.kt` + `MoveToObjectAction`/`HasPathAction` РїРѕСЃР»Рµ РїСЂР°РІРѕРє РґРёРЅР°РјРёС‡РµСЃРєРёС… РїСЂРµРїСЏС‚СЃС‚РІРёР№ Рё СЃС‚СЂР°С‚РµРіРёР№ РїСѓС‚Рё. РСЃРїСЂР°РІР»РµРЅРѕ:

- **Data race (CRITICAL)**: A* (`findPath`/`smoothPath`/`hasLineOfSight`) С‡РёС‚Р°Р» `navGrid.walkable` РёР· `pathExecutor`-РїРѕС‚РѕРєР°, РїРѕРєР° render-РїРѕС‚РѕРє РјСѓС‚РёСЂРѕРІР°Р» РµРіРѕ РІ `updateObstaclesDynamic` в†’ В«СЂРІР°РЅС‹РµВ» РїСѓС‚Рё. Р¤РёРєСЃ: `snapshotGrid()` РєРѕРїРёСЂСѓРµС‚ `walkable` РІ РЅР°С‡Р°Р»Рµ `findPath`/`smoothPath`, РІСЃРµ РїСЂРѕРІРµСЂРєРё РІРЅСѓС‚СЂРё СЂР°Р±РѕС‚Р°СЋС‚ СЃРѕ СЃРЅР°РїС€РѕС‚РѕРј; `hasLineOfSight(from,to,grid,scm,sw,sh)` С‚РµРїРµСЂСЊ РїСЂРёРЅРёРјР°РµС‚ grid РїР°СЂР°РјРµС‚СЂРѕРј (РІС‹Р·РѕРІС‹ РёР· render-РєРѕРЅС‚РµРєСЃС‚Р° РїРµСЂРµРґР°СЋС‚ СЃРІРµР¶РёР№ СЃРЅР°РїС€РѕС‚).
- **addObstacle Р±РµР· СЃРµС‚РєРё (HIGH)**: `step = navGrid?.cellSize ?: 1f` в†’ РґРѕ 100 000 С‚РѕС‡РµРє РЅР° РїСЂРµРїСЏС‚СЃС‚РІРёРµ (РІС‹Р·С‹РІР°Р»РѕСЃСЊ РґРѕ РїСЂРѕРІРµСЂРєРё `navGrid == null` РІ `MoveToObjectAction`). Р¤РёРєСЃ: СЂР°РЅРЅРёР№ `return` РїСЂРё `navGrid == null` + РѕС‡РµСЂРµРґСЊ `pendingObstacleNames`; `createGrid()` РїРѕСЃР»Рµ РїРѕСЃС‚СЂРѕРµРЅРёСЏ РїРµСЂРµСЃРєР°РЅРёСЂСѓРµС‚ РѕС‚Р»РѕР¶РµРЅРЅС‹Рµ РёРјРµРЅР°.
- **HasPathAction (HIGH)**: СЃРёРЅС…СЂРѕРЅРЅС‹Р№ A* (РґРѕ 50 000 РёС‚РµСЂР°С†РёР№) РЅР° render-РїРѕС‚РѕРєРµ РІ РєР°Р¶РґРѕРј РєР°РґСЂРµ. Р¤РёРєСЃ: `findPathToObjectAsync` + РєРѕР»Р±СЌРє; Р±Р»РѕРє Р·Р°РІРµСЂС€Р°РµС‚СЃСЏ СЃСЂР°Р·Сѓ, СЂРµР·СѓР»СЊС‚Р°С‚ РїРёС€РµС‚СЃСЏ РІ РїРµСЂРµРјРµРЅРЅСѓСЋ РїРѕ РїСЂРёС…РѕРґСѓ (РЅР° РєР°РґСЂ РїРѕР·Р¶Рµ).
- **update() (MEDIUM)**: Р»РёРЅРµР№РЅС‹Р№ РїРѕРёСЃРє СЃРїСЂР°Р№С‚Р° РїРѕ РёРјРµРЅРё РЅР° РєР°Р¶РґС‹Р№ С„РѕР»Р»РѕРІРµСЂ РєР°Р¶РґС‹Р№ РєР°РґСЂ в†’ `HashMap<name, Sprite>` СЃС‚СЂРѕРёС‚СЃСЏ РѕРґРёРЅ СЂР°Р· Р·Р° РєР°РґСЂ.
- **Р’РµС‡РЅС‹Р№ replan (MEDIUM)**: РїСЂРё РЅРµРґРѕСЃС‚РёР¶РёРјРѕР№ С†РµР»Рё Рё `enableDynamicReplanning` С„РѕР»Р»РѕРІРµСЂ РїРµСЂРµРїР»Р°РЅРёСЂРѕРІР°Р» РїСѓС‚СЊ РґРѕ РєРѕРЅС†Р° РІСЂРµРјРµРЅ вЂ” С†РёРєР» РЅРµ СЃС…РѕРґРёР»СЃСЏ. Р¤РёРєСЃ: stalePath-РїСЂРѕРІРµСЂРєР° РІРѕ РІСЃРµС… С‚СЂС‘С… replan-РєРѕР»Р±СЌРєР°С… вЂ” РµСЃР»Рё РїРѕСЃР»РµРґРЅСЏСЏ С‚РѕС‡РєР° РЅРѕРІРѕРіРѕ РїСѓС‚Рё СЃРѕРІРїР°РґР°РµС‚ СЃРѕ СЃС‚Р°СЂРѕР№ (< 1f), РїСѓС‚СЊ РЅРµ РѕР±РЅРѕРІР»СЏРµС‚СЃСЏ; С„РѕР»Р»РѕРІРµСЂ Р·Р°РІРµСЂС€Р°РµС‚СЃСЏ (REACHED РґР»СЏ end-replan, onPathBlocked+IDLE РґР»СЏ waypoint-replan).
- **MAX_ITERATIONS**: 50000 в†’ 200000 (РїСѓС‚Рё РЅР° Р±РѕР»СЊС€РёС… СЃРµС‚РєР°С… РЅРµ РЅР°С…РѕРґРёР»РёСЃСЊ; A* СЂР°Р±РѕС‚Р°РµС‚ РЅР° executor-РїРѕС‚РѕРєРµ).
- **РњС‘СЂС‚РІС‹Р№ РєРѕРґ СѓРґР°Р»С‘РЅ**: `updateObstacles()`, `createObstaclesFromBackground()`, `rebuildGrid()`, `findPathToObject()` (sync), `findPathWithSmoothing()`, `findPathToObjectWithSmoothing()`, `setPathForFollowerWithSmoothing()`, `getGridInfo()`, `getObstacleCount()`, `getFollowerCount()`, `getFollowerInfo()`, `isPathWalkable()`, `getNearestWalkablePoint()`, `debugPrintGrid()`, `getWalkableAreaPercentage()` вЂ” РЅРёРіРґРµ РЅРµ РІС‹Р·С‹РІР°Р»РёСЃСЊ (РїСЂРѕРІРµСЂРµРЅРѕ grep РїРѕ РІСЃРµРјСѓ СЂРµРїРѕ).

## Р РµРґР°РєС‚РѕСЂС‹: РёРЅРІРµРЅС‚Р°СЂРёР·Р°С†РёСЏ + РїРѕРґРєР»СЋС‡РµРЅРёРµ ParticleEditor (2026-08)

Р’ РїСЂРѕРµРєС‚Рµ 9 СЂРµРґР°РєС‚РѕСЂРѕРІ. 4 Р±С‹Р»Рё В«СЃРїСЏС‰РёРјРёВ» (РІ РјР°РЅРёС„РµСЃС‚Рµ, РЅРѕ Р±РµР· С‚РѕС‡РµРє РІС…РѕРґР°): TilemapEditor, DialogueEditor, ParticleEditor, NeoPaint. РџРѕРґРєР»СЋС‡С‘РЅ **ParticleEditor** (РїРѕР»РЅРѕСЌРєСЂР°РЅРЅС‹Р№ СЂРµРґР°РєС‚РѕСЂ 3D-С‡Р°СЃС‚РёС†, Unity-СЃС‚РёР»СЊ, `editor/ParticleEditorActivity.kt`):

- **РўРѕС‡РєР° РІС…РѕРґР°**: РєРЅРѕРїРєР° `btn_quick_particles` РІ quick-actions РїР°РЅРµР»Рё 3D-СЂРµРґР°РєС‚РѕСЂР° (`editor_activity.xml`, РёРєРѕРЅРєР° `drawable/ic_particles.xml`, РјРµР¶РґСѓ РёРЅСЃРїРµРєС‚РѕСЂРѕРј Рё СЂР°Р·РґРµР»РёС‚РµР»РµРј).
- `EditorActivity.setupUI()`: РµСЃР»Рё Сѓ РІС‹Р±СЂР°РЅРЅРѕРіРѕ РѕР±СЉРµРєС‚Р° РµСЃС‚СЊ `ParticleSystem3DComponent` в†’ `ParticleEditorActivity.Companion.launch(this, id, false)`; РёРЅР°С‡Рµ AlertDialog В«Р”РѕР±Р°РІРёС‚СЊ Particle System 3D?В» в†’ `go.addComponent(ps3d)` + `sceneManager.engine.createParticleProxy(id)` + `updateParticleEffect3D(...)` (С‚РѕС‚ Р¶Рµ РєРѕРґ, С‡С‚Рѕ case 8 РІ `InspectorManager.showAddComponentDialog`) в†’ launch.
- РР· Java companion-РјРµС‚РѕРґС‹ Kotlin РІС‹Р·С‹РІР°СЋС‚СЃСЏ РєР°Рє `Companion.launch(context, id, false)` (3 Р°СЂРіСѓРјРµРЅС‚Р°, РµСЃС‚СЊ РґРµС„РѕР»С‚РЅС‹Р№ `useUi2`).
- РЎС‚СЂРѕРєРё: `editor_3d_particles`, `editor_3d_particles_add_prompt` (en + ru).

РќРµ РїРѕРґРєР»СЋС‡РµРЅС‹ (РѕСЃС‚Р°Р»РѕСЃСЊ РЅР° РїРѕС‚РѕРј): TilemapEditor (СЂР°РЅС‚Р°Р№Рј РµСЃС‚СЊ: `SetTilemapSolidBrick`, С„РѕСЂРјСѓР»С‹ `tilemap_width/height`; СЃС‚СЂРѕРєРё `look_new_tilemap`/`look_edit_tilemap` СѓР¶Рµ РµСЃС‚СЊ), DialogueEditor (СЂР°РЅС‚Р°Р№Рј РµСЃС‚СЊ: `DialogueRunner`, `StartDialogueBrick`), NeoPaint (РѕСЃРЅРѕРІРЅРѕР№ С„Р»РѕСѓ РёРґС‘С‚ РІ PocketPaint).

РРґРµРё РЅРѕРІС‹С… СЂРµРґР°РєС‚РѕСЂРѕРІ (РєРѕРјРїРѕРЅРµРЅС‚С‹ РґР»СЏ РїРµСЂРµРёСЃРїРѕР»СЊР·РѕРІР°РЅРёСЏ): Level Designer (`TilemapEditorView`+`SceneEditorView`+`writePosition`), Animation/Keyframe (`CurveEditorView`), Physics Shape Editor (СЂР°СЃС€РёСЂРµРЅРёРµ `HitboxEditorView` РґРѕ РїРѕР»РёРіРѕРЅРѕРІ/РѕРєСЂСѓР¶РЅРѕСЃС‚РµР№), UI/HUD Editor (ShowText + WhenTouchDown СЃ СЏРєРѕСЂСЏРјРё), Path Editor (GlideTo waypoints), Atlas Cutter (РЅР°СЂРµР·РєР° СЃРїСЂР°Р№С‚-Р»РёСЃС‚РѕРІ), Skeleton/Bone Editor (С„СѓРЅРґР°РјРµРЅС‚ вЂ” ragdoll-СЂРµР¶РёРј 2 `PhysicsLook.updateRagdollFollow`), Game State Editor (UserVariable/UserList), AI/Behavior FSM Editor (РєР°Рє DialogueEditor, РЅРѕ РґР»СЏ РР: СЃРѕСЃС‚РѕСЏРЅРёСЏ+РїРµСЂРµС…РѕРґС‹ в†’ WhenCondition/Switch), Input Mapper (РіРµР№РјРїР°Рґ/РєР»Р°РІРёС€Рё в†’ WhenGamepadButton/KeyEvent), Variable Watch (РѕС‚Р»Р°РґРєР° Р·РЅР°С‡РµРЅРёР№ РїРµСЂРµРјРµРЅРЅС‹С… РІ СЂР°РЅС‚Р°Р№РјРµ).

## Physics collision fix вЂ” Desktop + Android (2026-07-19)

### Desktop (DesktopPhysicsWorld.kt)
- **Р‘Р°Рі**: `createBodyForSprite()` СЃРѕР·РґР°РІР°Р» Р’РЎР• С‚РµР»Р° СЃ `CircleShape`. РЎС‚Р°С‚РёС‡РµСЃРєРёРµ СЃС‚РµРЅС‹ РґРѕР»Р¶РЅС‹ Р±С‹С‚СЊ `PolygonShape` (РїСЂСЏРјРѕСѓРіРѕР»СЊРЅРёРє), РёРЅР°С‡Рµ РѕР±СЉРµРєС‚С‹ РїСЂРѕС…РѕРґСЏС‚ СЃРєРІРѕР·СЊ РїСЂРё РЅР°РєР»РѕРЅРЅРѕРј СѓРґР°СЂРµ.
- **Р¤РёРєСЃ**: static в†’ `PolygonShape.setAsBox(halfWidth, halfHeight)`, dynamic в†’ `CircleShape` + `setBullet(true)` (CCD).
- **setBodyType**: РїСЂРё СЃРјРµРЅРµ РЅР° Static РїРµСЂРµСЃРѕР·РґР°С‘С‚ С„РёРєСЃС‚СѓСЂСѓ РІ Polygon; РїСЂРё СЃРјРµРЅРµ РЅР° Dynamic вЂ” Circle + bullet.
- **setHitbox**: Р±РѕР»СЊС€Рµ РЅРµ no-op вЂ” РІС‹Р·С‹РІР°РµС‚ `physicsWorld.setHitbox()`.
- **Р”РѕР±Р°РІР»РµРЅРѕ**: `customHitboxSprites` (Set), `clearAllBodies()`.
- **РўРµСЃС‚С‹**: `DesktopPhysicsWorldCollisionTest.kt` вЂ” 36 С‚РµСЃС‚РѕРІ, РІСЃРµ РїСЂРѕР№РґРµРЅС‹.

### Android (PhysicsObject.java)
- **Р‘Р°Рі**: `setType(Type.FIXED)` Рё `setType(Type.NONE)` РёСЃРїРѕР»СЊР·РѕРІР°Р»Рё `BodyType.KinematicBody`. FIXED-РѕР±СЉРµРєС‚С‹ (СЃС‚РµРЅС‹, РїРѕР») РґРѕР»Р¶РЅС‹ Р±С‹С‚СЊ `StaticBody` вЂ” truly immovable. KinematicBody РјРѕР¶РµС‚ РїСЂРѕС‚Р°Р»РєРёРІР°С‚СЊ РґРёРЅР°РјРёС‡РµСЃРєРёРµ С‚РµР»Р° РёРЅР°С‡Рµ, С‡РµРј StaticBody, С‡С‚Рѕ РїСЂРёРІРѕРґРёС‚ Рє РЅРµРєРѕСЂСЂРµРєС‚РЅРѕР№ РєРѕР»Р»РёР·РёРё РїСЂРё РЅР°РєР»РѕРЅРЅС‹С… СѓРґР°СЂР°С….
- **Р¤РёРєСЃ**:
  - `FIXED` в†’ `BodyType.StaticBody` + `gravityScale(0.0f)`
  - `NONE` в†’ `BodyType.StaticBody` + `gravityScale(0.0f)`
  - `body.setBullet(false)` РїСЂРё РІС‹С…РѕРґРµ РёР· DYNAMIC (CCD РЅРµ РЅСѓР¶РµРЅ РЅР° СЃС‚Р°С‚РёРєРµ)
- **PhysicsBoundaryBox**: СѓР¶Рµ `StaticBody` + `PolygonShape` вЂ” С‚СЂРѕРіР°С‚СЊ РЅРµ РїСЂРёС€Р»РѕСЃСЊ.
- **РўРµСЃС‚С‹**: `PhysicsObjectTest` вЂ” РѕР±РЅРѕРІР»С‘РЅ `testSetType()` (KinematicBodyв†’StaticBody), РґРѕР±Р°РІР»РµРЅ `testSetTypeBulletTransitions()` (7 РїРµСЂРµС…РѕРґРѕРІ bullet=true/false).

---

# 3D-РјРѕРґРµР»Рё: СѓСЃС‚РѕР№С‡РёРІРѕСЃС‚СЊ Р·Р°РіСЂСѓР·РєРё GLB/GLTF (2026-08)

РџСЂРѕР±Р»РµРјР°: РЅРµРєРѕС‚РѕСЂС‹Рµ GLB-РјРѕРґРµР»Рё (РЅР°РїСЂ. Sketchfab) РґР°РІР°Р»Рё В«СЃРёРЅРёР№ СЌРєСЂР°РЅВ» РІ 3D-СЂРµРґР°РєС‚РѕСЂРµ Рё Р»РѕРјР°Р»Рё 3D-РёРіСЂСѓ.

## РђРЅР°Р»РёР· РїСЂРѕР±Р»РµРјРЅРѕР№ РјРѕРґРµР»Рё (vanessa_-_fnaf_security_breach.glb, Sketchfab)

- 9.5 РњР‘, glTF 2.0, Р‘Р•Р— extensionsUsed/Required (РЅРµС‚ Draco) вЂ” СЃС‚СЂСѓРєС‚СѓСЂРЅРѕ РІР°Р»РёРґРЅР° РґР»СЏ gdx-gltf.
- 12 СѓР·Р»РѕРІ, 10 РјРµС€РµР№, 10 РјР°С‚РµСЂРёР°Р»РѕРІ, 19 С‚РµРєСЃС‚СѓСЂ (JPEG+PNG), 0 Р°РЅРёРјР°С†РёР№/СЃРєРёРЅРѕРІ.
- РРЅРґРµРєСЃС‹ UNSIGNED_INT (componentType 5125), РґРѕ 57Рљ РёРЅРґРµРєСЃРѕРІ РЅР° РјРµС€; TEXCOORD_0..4 (gdx-gltf С‡РёС‚Р°РµС‚ С‚РѕР»СЊРєРѕ texcoord0, Р»РёС€РЅРёРµ РёРіРЅРѕСЂРёСЂСѓРµС‚).
- РњР°С‚РµСЂРёР°Р»С‹: alphaMode=BLEND (СЂРµСЃРЅРёС†С‹/РІРѕР»РѕСЃС‹/СЃС‚РµРєР»Рѕ), normal/emissive С‚РµРєСЃС‚СѓСЂС‹, doubleSided.
- РќРµРґРѕС‡С‘С‚ РјРѕРґРµР»Рё: `emissiveFactor: [1,0,1,0,1,0]` (6 Р·РЅР°С‡РµРЅРёР№ РІРјРµСЃС‚Рѕ 3) Сѓ `MI_GregFlashlight_00` вЂ” gdx-gltf С‡РёС‚Р°РµС‚ РїРµСЂРІС‹Рµ 3, РЅРµ РїР°РґР°РµС‚.
- Р РµР°Р»СЊРЅС‹Рµ РїСЂРёС‡РёРЅС‹ В«СЃРёРЅРµРіРѕ СЌРєСЂР°РЅР°В» РјРѕРіСѓС‚ Р±С‹С‚СЊ: OOM/GL-OOM РїСЂРё РґРµРєРѕРґРёСЂРѕРІР°РЅРёРё 19 С‚РµРєСЃС‚СѓСЂ (Error, РЅРµ Exception вЂ” СЂР°РЅСЊС€Рµ РќР• Р»РѕРІРёР»СЃСЏ), РЅР°С‚РёРІРЅС‹Р№ GL-РєСЂР°С€ РїСЂРё upload (С‚РµРєСЃС‚СѓСЂС‹ > maxTextureSize), Р»РёР±Рѕ РёСЃРєР»СЋС‡РµРЅРёРµ СЂРµРЅРґРµСЂР° СЃС†РµРЅС‹ (realisticMode в†’ gdx-gltf SceneManager).

## Р§С‚Рѕ РёСЃРїСЂР°РІР»РµРЅРѕ

- **`ThreeDManager.createObject()`** Рё **`replaceModel()`**: `catch (Exception)` в†’ `catch (Throwable)` (OOM/Error Р±РѕР»СЊС€Рµ РЅРµ СѓР±РёРІР°СЋС‚ РїСЂРёР»РѕР¶РµРЅРёРµ), РІ Р»РѕРі РїРёС€РµС‚СЃСЏ РїРѕР»РЅС‹Р№ СЃС‚РµРєС‚СЂРµР№СЃ СЃ РїСѓС‚С‘Рј РјРѕРґРµР»Рё.
- **Preflight-РїСЂРѕРІРµСЂРєР° GLB**: `hasUnsupportedGltfExtensions(FileHandle)` вЂ” С‡РёС‚Р°РµС‚ JSON-chunk Р±РёРЅР°СЂРЅРѕРіРѕ GLB (`readGltfJsonChunk`, magic glTF + chunk0) Рё РѕС‚РєР»РѕРЅСЏРµС‚ РјРѕРґРµР»Рё СЃ `KHR_draco_mesh_compression` / `EXT_meshopt_compression` / `KHR_texture_basisu` (gdx-gltf 2.2.1 РёС… РЅРµ СѓРјРµРµС‚) СЃ РїРѕРЅСЏС‚РЅС‹Рј СЃРѕРѕР±С‰РµРЅРёРµРј РІ Р»РѕРіРµ.
- **Р—Р°С‰РёС‚Р° СЂРµРЅРґРµСЂ-С†РёРєР»Р°** `renderColorsOnly()`:
  - non-realistic: `modelBatch.render()` РєР°Р¶РґРѕРіРѕ РѕР±СЉРµРєС‚Р° РѕР±С‘СЂРЅСѓС‚ РІ try/catch(Throwable); СѓРїР°РІС€РёР№ РёРЅСЃС‚Р°РЅСЃ СѓРґР°Р»СЏРµС‚СЃСЏ РёР· `sceneObjects`/`gltfObjectIds`/`animationControllers` РїРѕСЃР»Рµ `modelBatch.end()` (Р±РµР· ConcurrentModificationException) вЂ” РёРіСЂР° РїСЂРѕРґРѕР»Р¶Р°РµС‚ СЂР°Р±РѕС‚Р°С‚СЊ.
  - realistic: `sceneManager.renderMirror()/renderTransmission()/renderColors()` РєР°Р¶РґС‹Р№ РІ СЃРІРѕС‘Рј try/catch(Throwable) СЃ Р»РѕРіРѕРј.
- **`SceneManager.rebuildGameObject_internal()`**: РµСЃР»Рё `engine.createObject()` РІРµСЂРЅСѓР» false вЂ” РІРјРµСЃС‚Рѕ В«РїСѓСЃС‚РѕРіРѕВ» РѕР±СЉРµРєС‚Р° СЃС‚Р°РІРёС‚СЃСЏ РєСѓР±-РїСЂРёРјРёС‚РёРІ (`engine.createCube`) + Р»РѕРі СЃ РїСЂРёС‡РёРЅРѕР№; СЂРµРґР°РєС‚РѕСЂ Р±РѕР»СЊС€Рµ РЅРµ РїРѕРєР°Р·С‹РІР°РµС‚ РїСѓСЃС‚РѕС‚Сѓ (В«СЃРёРЅРёР№ СЌРєСЂР°РЅВ»). РЎСѓС‰РµСЃС‚РІСѓСЋС‰РёР№ РѕР±СЉРµРєС‚ РїСЂРё СЌС‚РѕРј РЅРµ С‚СЂРѕРіР°РµС‚СЃСЏ (guard `containsKey` РІ createCube).

## РџСЂРѕРІРµСЂРєР°

`./gradlew :catroid:compileCatroidDebugJavaWithJavac --offline -q` вЂ” BUILD SUCCESSFUL (С‚РѕР»СЊРєРѕ СЃС‚Р°РЅРґР°СЂС‚РЅС‹Рµ Note).
Р”РёР°РіРЅРѕСЃС‚РёРєР° РїСЂРёС‡РёРЅС‹ Сѓ РїРѕР»СЊР·РѕРІР°С‚РµР»СЏ: logcat-С‚РµРіРё `3DManager_PBR` / `3DManager` / `SceneManager` вЂ” С‚Р°Рј С‚РµРїРµСЂСЊ СЃС‚РµРєС‚СЂРµР№СЃ РїСЂРёС‡РёРЅС‹ РѕС‚РєР°Р·Р°.
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