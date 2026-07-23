package org.catrobat.catroid.formulaeditor;

import android.app.Activity;
import android.os.Environment;
import org.catrobat.catroid.runtime.RuntimeServicesHolder;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.content.FloatArrayManager;
import org.catrobat.catroid.common.TilemapLookData;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StateMachineManager;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.TableManager;
import org.catrobat.catroid.content.UserVarsManager;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.fast2d.Fast2DFormulaBridge;
import org.catrobat.catroid.fast2d.FastTwoDManager;
import org.catrobat.catroid.formulaeditor.common.Conversions;
import org.catrobat.catroid.formulaeditor.function.ArduinoFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.BinaryFunction;
import org.catrobat.catroid.formulaeditor.function.FormulaFunction;
import org.catrobat.catroid.formulaeditor.function.FunctionProvider;
import org.catrobat.catroid.formulaeditor.function.MathFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.ObjectDetectorFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.RaspiFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.TernaryFunction;
import org.catrobat.catroid.formulaeditor.function.TextBlockFunctionProvider;
import org.catrobat.catroid.formulaeditor.function.TouchFunctionProvider;
import org.catrobat.catroid.libraries.LibraryManager;
import org.catrobat.catroid.libraries.LoadedLibrary;
import org.catrobat.catroid.ml.MLBridge;
import org.catrobat.catroid.physics.PhysicsWorld;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.sensing.CollisionDetection;
import org.catrobat.catroid.sensing.ColorAtXYDetection;
import org.catrobat.catroid.sensing.ColorCollisionDetection;
import org.catrobat.catroid.sensing.ColorEqualsColor;
import org.catrobat.catroid.admob.AdMobManager;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.stage.StageListener;
import org.catrobat.catroid.utils.lunoscript.Interpreter;
import org.catrobat.catroid.utils.lunoscript.LunoRuntimeError;
import org.catrobat.catroid.utils.lunoscript.LunoValue;
import org.catrobat.catroid.utils.lunoscript.Token;
import org.catrobat.catroid.utils.lunoscript.TokenType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

import static org.catrobat.catroid.formulaeditor.InternTokenType.BRACKET_CLOSE;
import static org.catrobat.catroid.formulaeditor.InternTokenType.BRACKET_OPEN;
import static org.catrobat.catroid.formulaeditor.InternTokenType.COLLISION_FORMULA;
import static org.catrobat.catroid.formulaeditor.InternTokenType.FUNCTION_NAME;
import static org.catrobat.catroid.formulaeditor.InternTokenType.FUNCTION_PARAMETERS_BRACKET_CLOSE;
import static org.catrobat.catroid.formulaeditor.InternTokenType.FUNCTION_PARAMETERS_BRACKET_OPEN;
import static org.catrobat.catroid.formulaeditor.InternTokenType.FUNCTION_PARAMETER_DELIMITER;
import static org.catrobat.catroid.formulaeditor.InternTokenType.NUMBER;
import static org.catrobat.catroid.formulaeditor.InternTokenType.OPERATOR;
import static org.catrobat.catroid.formulaeditor.InternTokenType.SENSOR;
import static org.catrobat.catroid.formulaeditor.InternTokenType.STRING;
import static org.catrobat.catroid.formulaeditor.InternTokenType.USER_DEFINED_BRICK_INPUT;
import static org.catrobat.catroid.formulaeditor.InternTokenType.USER_LIST;
import static org.catrobat.catroid.formulaeditor.InternTokenType.USER_VARIABLE;
import static org.catrobat.catroid.formulaeditor.common.Conversions.FALSE;
import static org.catrobat.catroid.formulaeditor.common.Conversions.TRUE;
import static org.catrobat.catroid.formulaeditor.common.Conversions.booleanToDouble;
import static org.catrobat.catroid.formulaeditor.common.Conversions.convertArgumentToDouble;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretOperatorEqual;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretSensor;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretUserDefinedBrickInput;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretUserList;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.interpretUserVariable;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.isInteger;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.normalizeDegeneratedDoubleValues;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.tryInterpretCollision;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.tryInterpretDoubleValue;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.tryInterpretElementRecursive;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementOperations.tryParseIntFromObject;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementResources.addFunctionResources;
import static org.catrobat.catroid.formulaeditor.common.FormulaElementResources.addSensorsResources;
import static org.catrobat.catroid.utils.NumberFormats.trimTrailingCharacters;

import org.mozilla.javascript.ContextFactory;

import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;
import com.danvexteam.lunoscript_annotations.LunoClass;

@LunoClass
public class FormulaElement implements Serializable {

    private static final long serialVersionUID = 1L;
    private static boolean rhinoInitialized = false;
    private static final String TAG_FORMULA_ELEMENT = "FormulaElement";

    private transient org.catrobat.catroid.formulaeditor.UserVariable cachedUserVariable = null;
    private transient org.catrobat.catroid.formulaeditor.UserList cachedUserList = null;
    private transient org.catrobat.catroid.content.Scope cachedScope = null;

    private transient org.catrobat.catroid.formulaeditor.UserData cachedBrickInput = null;
    private transient com.badlogic.gdx.scenes.scene2d.actions.SequenceAction cachedSequence = null;

    private transient Double cachedDoubleValue = null;

    private static org.luaj.vm2.Globals luaGlobals = null;
    private static long cpuPrevTotal = 0;
    private static long cpuPrevIdle = 0;


    public enum ElementType {
        OPERATOR, FUNCTION, NUMBER, SENSOR, USER_VARIABLE, USER_LIST, USER_DEFINED_BRICK_INPUT, BRACKET, STRING, COLLISION_FORMULA
    }

    private static void ensureRhinoInitialized() {
        if (!ContextFactory.hasExplicitGlobal()) {
            ContextFactory.initGlobal(new ContextFactory());
        }
    }

    public ElementType type;
    private String value;
    public FormulaElement leftChild = null;
    public FormulaElement rightChild = null;
    public List<FormulaElement> additionalChildren;
    private transient FormulaElement parent;
    private transient Map<Functions, FormulaFunction> formulaFunctions;
    private transient TextBlockFunctionProvider textBlockFunctionProvider;

    protected FormulaElement() {
        textBlockFunctionProvider = new TextBlockFunctionProvider();
        List<FunctionProvider> functionProviders = Arrays.asList(
                new ArduinoFunctionProvider(),
                new RaspiFunctionProvider(),
                new MathFunctionProvider(),
                new TouchFunctionProvider(),
                textBlockFunctionProvider,
                new ObjectDetectorFunctionProvider()
        );

        formulaFunctions = new EnumMap<>(Functions.class);
        initFunctionMap(functionProviders, formulaFunctions);
        additionalChildren = new ArrayList<>();
    }

    public FormulaElement(ElementType type, String value, FormulaElement parent) {
        this();
        this.type = type;
        this.value = value;
        this.parent = parent;
    }

    public FormulaElement(ElementType type, String value, FormulaElement parent, FormulaElement leftChild,
                          FormulaElement rightChild) {
        this(type, value, parent);
        this.leftChild = leftChild;
        this.rightChild = rightChild;

        if (leftChild != null) {
            this.leftChild.parent = this;
        }
        if (rightChild != null) {
            this.rightChild.parent = this;
        }
    }

    public FormulaElement(ElementType type, String value, FormulaElement parent, FormulaElement leftChild,
                          FormulaElement rightChild, List<FormulaElement> additionalChildren) {
        this(type, value, parent, leftChild, rightChild);
        for (FormulaElement child : additionalChildren) {
            addAdditionalChild(child);
        }
    }

    private void initFunctionMap(List<FunctionProvider> functionProviders, Map<Functions, FormulaFunction> formulaFunctions) {
        for (FunctionProvider functionProvider : functionProviders) {
            functionProvider.addFunctionsToMap(formulaFunctions);
        }

        formulaFunctions.put(Functions.RAND, new BinaryFunction(this::interpretFunctionRand));
        formulaFunctions.put(Functions.IF_THEN_ELSE, new TernaryFunction(this::interpretFunctionIfThenElse));
    }

    public ElementType getElementType() {
        return type;
    }

    private FastTwoDManager getFastTwoDManager() {
        StageActivity activity = StageActivity.activeStageActivity.get();
        if (activity != null && activity.stageListener != null) {
            return activity.stageListener.fastTwoDManager;
        }
        return null;
    }

    public String getValue() {
        return trimTrailingCharacters(value);
    }

    public List<InternToken> getInternTokenList() {
        List<InternToken> tokens = new LinkedList<>();

        switch (type) {
            case BRACKET:
                addBracketTokens(tokens, rightChild);
                break;
            case OPERATOR:
                addOperatorTokens(tokens, value);
                break;
            case FUNCTION:
                addFunctionTokens(tokens, value, leftChild, rightChild);
                break;
            case USER_VARIABLE:
                addToken(tokens, USER_VARIABLE, value);
                break;
            case USER_LIST:
                addToken(tokens, USER_LIST, value);
                break;
            case USER_DEFINED_BRICK_INPUT:
                addToken(tokens, USER_DEFINED_BRICK_INPUT, value);
                break;
            case NUMBER:
                addToken(tokens, NUMBER, trimTrailingCharacters(value));
                break;
            case SENSOR:
                addToken(tokens, SENSOR, value);
                break;
            case STRING:
                addToken(tokens, STRING, value);
                break;
            case COLLISION_FORMULA:
                addToken(tokens, COLLISION_FORMULA, value);
                break;
        }
        return tokens;
    }

    private void addToken(List<InternToken> tokens, InternTokenType tokenType) {
        tokens.add(new InternToken(tokenType));
    }

    private void addToken(List<InternToken> tokens, InternTokenType tokenType, String value) {
        tokens.add(new InternToken(tokenType, value));
    }

    private void addBracketTokens(List<InternToken> internTokenList, FormulaElement element) {
        addToken(internTokenList, BRACKET_OPEN);
        tryAddInternTokens(internTokenList, element);
        addToken(internTokenList, BRACKET_CLOSE);
    }

    private void addOperatorTokens(List<InternToken> tokens, String value) {
        tryAddInternTokens(tokens, leftChild);
        addToken(tokens, OPERATOR, value);
        tryAddInternTokens(tokens, rightChild);
    }

    private void addFunctionTokens(List<InternToken> tokens, String value, FormulaElement leftChild, FormulaElement rightChild) {
        addToken(tokens, FUNCTION_NAME, value);
        boolean functionHasParameters = false;
        if (leftChild != null) {
            addToken(tokens, FUNCTION_PARAMETERS_BRACKET_OPEN);
            functionHasParameters = true;
            tokens.addAll(leftChild.getInternTokenList());
        }
        if (rightChild != null) {
            addToken(tokens, FUNCTION_PARAMETER_DELIMITER);
            tokens.addAll(rightChild.getInternTokenList());
        }
        for (FormulaElement child : additionalChildren) {
            if (child != null) {
                addToken(tokens, FUNCTION_PARAMETER_DELIMITER);
                tokens.addAll(child.getInternTokenList());
            }
        }
        if (functionHasParameters) {
            addToken(tokens, FUNCTION_PARAMETERS_BRACKET_CLOSE);
        }
    }

    private void tryAddInternTokens(List<InternToken> tokens, FormulaElement child) {
        if (child != null) {
            tokens.addAll(child.getInternTokenList());
        }
    }

    public FormulaElement getRoot() {
        FormulaElement root = this;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return root;
    }

    public void updateElementByName(String oldName, String newName, ElementType type) {
        tryUpdateElementByName(leftChild, oldName, newName, type);
        tryUpdateElementByName(rightChild, oldName, newName, type);

        for (FormulaElement child : additionalChildren) {
            tryUpdateElementByName(child, oldName, newName, type);
        }

        if (matchesTypeAndName(type, oldName)) {
            value = newName;
        }
    }

    private void tryUpdateElementByName(FormulaElement element, String oldName, String newName,
                                        ElementType type) {
        if (element != null) {
            element.updateElementByName(oldName, newName, type);
        }
    }

    public final boolean containsSpriteInCollision(String name) {
        if (containsSpriteInCollision(leftChild, name) || containsSpriteInCollision(rightChild, name)) {
            return true;
        }
        for (FormulaElement child : additionalChildren) {
            if (containsSpriteInCollision(child, name)) {
                return true;
            }
        }
        return matchesTypeAndName(ElementType.COLLISION_FORMULA, name);
    }

    private boolean containsSpriteInCollision(FormulaElement element, String name) {
        return element != null && element.containsSpriteInCollision(name);
    }

    public final void insertFlattenForAllUserLists(FormulaElement element, FormulaElement parent) {
        if (element.leftChild != null) {
            insertFlattenForAllUserLists(element.leftChild, element);
        }
        if (element.rightChild != null) {
            insertFlattenForAllUserLists(element.rightChild, element);
        }
        for (FormulaElement child : element.additionalChildren) {
            if (child != null) {
                insertFlattenForAllUserLists(child, element);
            }
        }
        if (element.type == ElementType.USER_LIST && isNotUserListFunction(parent)) {
            insertFlattenBetweenParentAndElement(parent, element);
        }
    }

    public boolean isNotUserListFunction(FormulaElement element) {
        return element == null
                || element.type != ElementType.FUNCTION
                || (!element.value.equals(Functions.CONTAINS.name())
                && !element.value.equals(Functions.NUMBER_OF_ITEMS.name())
                && !element.value.equals(Functions.LIST_ITEM.name())
                && !element.value.equals(Functions.INDEX_OF_ITEM.name())
                && !element.value.equals(Functions.FLATTEN.name())
                && !element.value.equals(Functions.CONNECT.name())
                && !element.value.equals(Functions.FIND.name()));
    }

    public void insertFlattenBetweenParentAndElement(FormulaElement parent,
                                                     FormulaElement element) {
        FormulaElement flatten = new FormulaElement(ElementType.FUNCTION,
                Functions.FLATTEN.name(), parent);
        insertElementBeforeChildInFormulaTree(parent, element, flatten);
    }

    private void insertElementBeforeChildInFormulaTree(FormulaElement parent, FormulaElement child,
                                                       FormulaElement elementToInsert) {
        if (child == null || elementToInsert == null) {
            return;
        }

        child.parent = elementToInsert;
        elementToInsert.setLeftChild(child);

        if (parent == null) {
            return;
        }

        if (parent.leftChild == child) {
            parent.leftChild = elementToInsert;
        } else if (parent.rightChild == child) {
            parent.rightChild = elementToInsert;
        } else {
            for (int i = 0; i < parent.additionalChildren.size(); i++) {
                if (parent.additionalChildren.get(i) == child) {
                    parent.additionalChildren.set(i, elementToInsert);
                }
            }
        }
    }

    private boolean matchesTypeAndName(ElementType queriedType, String name) {
        return type == queriedType && value.equals(name);
    }

    public void updateCollisionFormulaToVersion(Project currentProject) {
        tryUpdateCollisionFormulaToVersion(leftChild, currentProject);
        tryUpdateCollisionFormulaToVersion(rightChild, currentProject);
        for (FormulaElement child : additionalChildren) {
            tryUpdateCollisionFormulaToVersion(child, currentProject);
        }
        if (type == ElementType.COLLISION_FORMULA) {
            String secondSpriteName = CollisionDetection.getSecondSpriteNameFromCollisionFormulaString(value, currentProject);
            if (secondSpriteName != null) {
                value = secondSpriteName;
            }
        }
    }

    private void tryUpdateCollisionFormulaToVersion(FormulaElement element, Project currentProject) {
        if (element != null) {
            element.updateCollisionFormulaToVersion(currentProject);
        }
    }

    public Object interpretRecursive(Scope scope) {
        Object rawReturnValue = rawInterpretRecursive(scope);
        return normalizeDegeneratedDoubleValues(rawReturnValue);
    }

    private Object rawInterpretRecursive(Scope scope) {
        if (scope == null) {
            return FALSE;
        }
        org.catrobat.catroid.utils.PerformanceTracker.formulaEvaluations.incrementAndGet();

        switch (type) {
            case BRACKET:
                if (additionalChildren.size() != 0) {
                    return additionalChildren.get(additionalChildren.size() - 1).interpretRecursive(scope);
                }
                return rightChild != null ? rightChild.interpretRecursive(scope) : 0.0;
            case NUMBER:
                if (cachedDoubleValue != null) return cachedDoubleValue;
                try {
                    cachedDoubleValue = Double.valueOf(value);
                    return cachedDoubleValue;
                } catch (NumberFormatException e) {
                    return value;
                }
            case STRING:
                return value;
            case OPERATOR:
                return tryInterpretOperator(scope, value);
            case FUNCTION:
                return interpretFunction(value, scope);
            case SENSOR:
                ProjectManager pm1 = ProjectManager.getInstance();
                Scene currentlyEditedScene = pm1 != null ? pm1.getCurrentlyEditedScene() : null;
                Project currentProject = pm1 != null ? pm1.getCurrentProject() : null;
                Sprite sprite = scope.getSprite();
                if (sprite == null) return 0.0;
                return interpretSensor(sprite, currentlyEditedScene, currentProject, value);
            case USER_VARIABLE:
                if (cachedUserVariable == null || cachedScope != scope || (cachedScope != null && cachedScope.getSprite() != scope.getSprite())) {
                    cachedUserVariable = UserDataWrapper.getUserVariable(value, scope);
                    cachedScope = scope;
                }
                return interpretUserVariable(cachedUserVariable);

            case USER_LIST:
                if (cachedUserList == null || cachedScope != scope || (cachedScope != null && cachedScope.getSprite() != scope.getSprite())) {
                    cachedUserList = UserDataWrapper.getUserList(value, scope);
                    cachedScope = scope;
                }
                return interpretUserList(cachedUserList);

            case USER_DEFINED_BRICK_INPUT:
                if (cachedBrickInput == null || cachedSequence != scope.getSequence()) {
                    cachedBrickInput = UserDataWrapper.getUserDefinedBrickInput(value, scope.getSequence());
                    cachedSequence = scope.getSequence();
                }
                return interpretUserDefinedBrickInput(cachedBrickInput);
            case COLLISION_FORMULA:
                ProjectManager pm2 = ProjectManager.getInstance();
                Scene currentlyPlayingScene = pm2 != null ? pm2.getCurrentlyPlayingScene() : null;
                StageListener stageListener = StageActivity.getActiveStageListener();
                Sprite collisionSprite = scope.getSprite();
                if (collisionSprite == null || collisionSprite.look == null) return FALSE;
                return tryInterpretCollision(collisionSprite.look, value, currentlyPlayingScene,
                        stageListener);
        }
        return FALSE;
    }

    @NotNull
    private Object tryInterpretOperator(Scope scope, String value) {
        Operators operator = Operators.getOperatorByValue(value);
        if (operator == null) {
            return false;
        }
        return interpretOperator(operator, scope);
    }

    private Object interpretFunction(String name, Scope scope) {
        Functions standardFunction = Functions.getFunctionByValue(name);

        if (standardFunction != null) {
            return interpretFunction(standardFunction, scope);
        } else {
            CustomFormula customFormula = CustomFormulaManager.INSTANCE.getFormulaByUniqueName(name);
            if (customFormula != null) {
                List<Object> actualArguments = new ArrayList<>();
                if (leftChild != null) actualArguments.add(tryInterpretRecursive(leftChild, scope));
                if (rightChild != null) actualArguments.add(tryInterpretRecursive(rightChild, scope));
                for (int i = 0; i < additionalChildren.size(); i++) {
                    actualArguments.add(tryInterpretRecursive(additionalChildren.get(i), scope));
                }
                return interpretCustomLunoFunction(customFormula, actualArguments, scope);
            } else {
                return FALSE;
            }
        }
    }

    private Object interpretFunction(Functions function, Scope scope) {
        if (scope == null) {
            return FALSE;
        }
        if (function == Functions.IF_THEN_ELSE) {
            Double condition = convertArgumentToDouble(tryInterpretRecursive(leftChild, scope));

            if (condition == null || condition.isNaN()) return Double.NaN;

            FormulaElement branch = (condition != 0) ? rightChild : (additionalChildren.isEmpty() ? null : additionalChildren.get(0));
            Object result = tryInterpretRecursive(branch, scope);

            if (result instanceof String) return result;
            Double resultDouble = convertArgumentToDouble(result);
            return resultDouble != null ? resultDouble : 0.0;
        }

        Object arg0 = tryInterpretRecursive(leftChild, scope);
        Object arg1 = tryInterpretRecursive(rightChild, scope);
        Object arg2 = additionalChildren.isEmpty() ? null : tryInterpretRecursive(additionalChildren.get(0), scope);
        Object arg3 = additionalChildren.size() > 1 ? tryInterpretRecursive(additionalChildren.get(1), scope) : null;

        switch (function) {
            case CLAMP:
                return interpretFunctionClamp(arg0, arg1, arg2);
            case DISTAN:
                return interpretFunctionDistan(arg0, arg1, arg2, arg3);
            case LERP:
                return interpretFunctionLerp(arg0, arg1, arg2);
            case MAP_RANGE: {
                Object arg4 = additionalChildren.size() > 2 ? tryInterpretRecursive(additionalChildren.get(2), scope) : null;
                return interpretFunctionMapRange(arg0, arg1, arg2, arg3, arg4);
            }
            case RGB:
                return interpretFunctionRgb(arg0, arg1, arg2);
            case HSV:
                return interpretFunctionHsv(arg0, arg1, arg2);
            case MIX_COLOR:
                return interpretFunctionMixColor(arg0, arg1, arg2);
            case CURRENT_STATE:
                return StateMachineManager.getState(scope.getSprite(), String.valueOf(arg0));
            case STATE_TIME:
                return StateMachineManager.getStateTimeSeconds(scope.getSprite(), String.valueOf(arg0));
            case LETTER:
                return interpretFunctionLetter(arg0, arg1);
            case SUBTEXT:
                return interpretFunctionSubtext(arg0, arg1, arg2);
            case FILE: {
                String fileName = String.valueOf(arg0);
                if (scope.getProject() == null) return false;
                try {
                    File file = scope.getProject().getFile(fileName);
                    return file != null && file.exists() && file.isFile() && file.canRead();
                } catch (Exception e) {
                    return false;
                }
            }
            case FILE_READ_STRING: {
                if (scope.getProject() == null) return "";
                return readLineFromFile(scope.getProject().getFilesDir(), String.valueOf(arg0), String.valueOf(arg1));
            }
            case FILES_PATH: {
                Project currentProject1 = ProjectManager.getInstance().getCurrentProject();
                return currentProject1 != null ? getFileListString(currentProject1.getFilesDir()) : "";
            }
            case ALL_FILES: {
                Project currentProject2 = ProjectManager.getInstance().getCurrentProject();
                return currentProject2 != null ? currentProject2.getFilesDir().getAbsolutePath() : "";
            }
            case LUA: {
                // SECURITY: use standardGlobals() — unlike debugGlobals() it does NOT install
                // the debug library, so the sandbox cannot be escaped via
                // debug.getupvalue/setmetatable even after io/os are stripped.
                if (luaGlobals == null) {
                    luaGlobals = org.luaj.vm2.lib.jse.JsePlatform.standardGlobals();
                    // Sandbox: remove dangerous libraries
                    luaGlobals.set("io", org.luaj.vm2.LuaValue.NIL);
                    luaGlobals.set("os", org.luaj.vm2.LuaValue.NIL);
                    luaGlobals.set("require", org.luaj.vm2.LuaValue.NIL);
                    luaGlobals.set("dofile", org.luaj.vm2.LuaValue.NIL);
                    luaGlobals.set("loadfile", org.luaj.vm2.LuaValue.NIL);
                }
                String luaScript = String.valueOf(arg0);
                try {
                    return luaGlobals.load(luaScript).call().tojstring();
                } catch (org.luaj.vm2.LuaError e) {
                    Log.e(TAG_FORMULA_ELEMENT, "LUA script error", e);
                    return "LUA ERROR: " + e.getMessage();
                } catch (Exception e) {
                    Log.e(TAG_FORMULA_ELEMENT, "LUA unexpected error", e);
                    return "";
                }
            }
            case FILE_SIZE: {
                Project currentProject3 = ProjectManager.getInstance().getCurrentProject();
                if (currentProject3 == null) return 0.0;
                // Use double (long-safe): (int) cast would overflow for files > 2 GB
                return (double) getFileSize(currentProject3.getFile(String.valueOf(arg0)));
            }
            case FILE_PROJECT_SIZE: {
                Project currentProject4 = ProjectManager.getInstance().getCurrentProject();
                if (currentProject4 == null) return 0.0;
                // Use double (long-safe): (int) cast would overflow for files > 2 GB
                return (double) getFileSize(currentProject4.getFile(String.valueOf(arg0)));
            }
            case FILE_SIZE_IN_DIR: {
                String fileName = String.valueOf(arg0);
                String dirName = String.valueOf(arg1);
                try {
                    File downloadsDir = new File(RuntimeServicesHolder.services.getDownloadsDir());
                    File dir = new File(downloadsDir, dirName);
                    File file = new File(dir, fileName);
                    return (double) getFileSize(file);
                } catch (Exception e) { return 0; }
            }
            case FILE_SIZE_AT_PATH: {
                String fileName = String.valueOf(arg0);
                String path = String.valueOf(arg1);
                try {
                    File downloadsDir = new File(RuntimeServicesHolder.services.getDownloadsDir());
                    File dir = new File(downloadsDir, path);
                    File file = new File(dir, fileName);
                    return (double) getFileSize(file);
                } catch (Exception e) { return 0; }
            }
            case TO_HEX: {
                Integer decimal = tryParseIntFromObject(arg0);
                return decimal != null ? Integer.toHexString(decimal).toUpperCase() : "0";
            }
            case TO_DEC: {
                try {
                    return Integer.parseInt(String.valueOf(arg0), 16);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            case IS_MOUSE_BUTTON_DOWN: {
                Integer buttonCode = tryParseIntFromObject(arg0);
                if (buttonCode == null) return Conversions.FALSE;
                return Conversions.booleanToDouble(Gdx.input.isButtonPressed(buttonCode));
            }
            case RANDOM_STR: {
                String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
                Integer length = tryParseIntFromObject(arg0);
                if (length == null || length <= 0) return "";
                StringBuilder result2 = new StringBuilder(length);
                java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
                for (int i = 0; i < length; i++) {
                    result2.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
                }
                return result2.toString();
            }
            case GET_DIRECTION_X:
                return (double) com.badlogic.gdx.math.MathUtils.cosDeg((float) tryInterpretDoubleValue(arg0 != null ? arg0 : 0.0));
            case GET_DIRECTION_Y:
                return (double) com.badlogic.gdx.math.MathUtils.sinDeg((float) tryInterpretDoubleValue(arg0 != null ? arg0 : 0.0));
            case GET_ANGLE:
                return (double) com.badlogic.gdx.math.MathUtils.atan2Deg((float) tryInterpretDoubleValue(arg1 != null ? arg1 : 0.0), (float) tryInterpretDoubleValue(arg0 != null ? arg0 : 0.0));
            case GET_3D_VELOCITY_X: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getVelocity(String.valueOf(arg0)).x : 0.0;
            }
            case GET_3D_VELOCITY_Y: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getVelocity(String.valueOf(arg0)).y : 0.0;
            }
            case GET_3D_VELOCITY_Z: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getVelocity(String.valueOf(arg0)).z : 0.0;
            }
            case GET_3D_POSITION_X: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 pos = manager.getPosition(String.valueOf(arg0));
                return (pos != null) ? pos.x : 0.0;
            }
            case PT_ARGMAX:
                return MLBridge.nativeArgMax(String.valueOf(arg0));
            case F2D_IS_TOUCHED:
                return Fast2DFormulaBridge.isTouched(String.valueOf(arg0));
            case F2D_IS_TOUCHED_INDEX: {
                Integer fingerOpt = tryParseIntFromObject(arg1);
                return Fast2DFormulaBridge.isTouched(String.valueOf(arg0), fingerOpt != null ? fingerOpt : 0);
            }
            case F2D_X:
                return Fast2DFormulaBridge.getX(String.valueOf(arg0));
            case F2D_Y:
                return Fast2DFormulaBridge.getY(String.valueOf(arg0));
            case F2D_ROTATION:
                return Fast2DFormulaBridge.getRotation(String.valueOf(arg0));
            case F2D_SCALE_X:
                return Fast2DFormulaBridge.getScaleX(String.valueOf(arg0));
            case F2D_SCALE_Y:
                return Fast2DFormulaBridge.getScaleY(String.valueOf(arg0));
            case F2D_COLOR_R:
                return Fast2DFormulaBridge.getColorR(String.valueOf(arg0));
            case F2D_COLOR_G:
                return Fast2DFormulaBridge.getColorG(String.valueOf(arg0));
            case F2D_COLOR_B:
                return Fast2DFormulaBridge.getColorB(String.valueOf(arg0));
            case F2D_ALPHA:
                return Fast2DFormulaBridge.getAlpha(String.valueOf(arg0));
            case F2D_TEXTURE:
                return Fast2DFormulaBridge.getTexture(String.valueOf(arg0));
            case F2D_CAM_X:
                return Fast2DFormulaBridge.getCamX();
            case F2D_CAM_Y:
                return Fast2DFormulaBridge.getCamY();
            case F2D_CAM_ZOOM:
                return Fast2DFormulaBridge.getCamZoom();
            case PT_DUMP:
                return MLBridge.nativeGetTensorAsString(String.valueOf(arg0));
            case PT_TOTALSIZE:
                return MLBridge.nativeGetTotalSize(String.valueOf(arg0));
            case PT_VALUE: {
                Integer ptIdx = tryParseIntFromObject(arg1);
                return MLBridge.nativeGetTensorValueByIndex(String.valueOf(arg0), ptIdx != null ? ptIdx : 0);
            }
            case PT_SHAPE:
                return MLBridge.nativeGetShape(String.valueOf(arg0));
            case PT_VALUEND:
                return MLBridge.nativeGetValueND(String.valueOf(arg0), String.valueOf(arg1));
            case INTERSECT_LIST: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? manager.getIntersectionCollisionsList(String.valueOf(arg0)) : "";
            }
            case COLLISION_LIST: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? manager.getPhysicsCollisionsList(String.valueOf(arg0)) : "";
            }
            case DELTA: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? String.valueOf(manager.getDeltaTime()) : "-1";
            }
            case OBJECT_TOUCHES_OBJECT: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? Conversions.booleanToDouble(manager.checkCollision(String.valueOf(arg0), String.valueOf(arg1))) : Conversions.FALSE;
            }
            case OBJECT_INTERSECTS_OBJECT: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? Conversions.booleanToDouble(manager.checkIntersection(String.valueOf(arg0), String.valueOf(arg1))) : Conversions.FALSE;
            }
            case ID_OF_DETECTED_OBJECT: {
                // Wired to the real object-detection source (ObjectDetectorResults, populated by
                // ObjectDetectorFunctionProvider) so this is intentional, not a silent default fall-through.
                return interpretFormulaFunction(function, arg0, arg1, arg2);
            }
            case OBJECT_WITH_ID_VISIBLE: {
                return interpretFormulaFunction(function, arg0, arg1, arg2);
            }
            case GET_CAMERA_ROTATION_YAW: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraRotation().y : 0.0;
            }
            case GET_CAMERA_ROTATION_PITCH: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraRotation().x : 0.0;
            }
            case GET_CAMERA_ROTATION_ROLL: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraRotation().z : 0.0;
            }
            case GET_3D_POSITION_Y: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 pos = manager.getPosition(String.valueOf(arg0));
                return (pos != null) ? pos.y : 0.0;
            }
            case GET_3D_POSITION_Z: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 pos = manager.getPosition(String.valueOf(arg0));
                return (pos != null) ? pos.z : 0.0;
            }
            case GET_CAMERA_POS_X: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraPosition().x : 0.0;
            }
            case GET_CAMERA_POS_Y: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraPosition().y : 0.0;
            }
            case GET_CAMERA_POS_Z: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraPosition().z : 0.0;
            }
            case GET_CAMERA_DIR_X: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraDirection().x : 0.0;
            }
            case GET_CAMERA_DIR_Y: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraDirection().y : 0.0;
            }
            case GET_CAMERA_DIR_Z: {
                ThreeDManager manager = getThreeDManager();
                return (manager != null) ? (double) manager.getCameraDirection().z : 0.0;
            }
            case GET_3D_ROTATION_YAW: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 rot = manager.getRotation(String.valueOf(arg0));
                return (rot != null) ? rot.y : 0.0;
            }
            case GET_3D_ROTATION_PITCH: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 rot = manager.getRotation(String.valueOf(arg0));
                return (rot != null) ? rot.x : 0.0;
            }
            case GET_3D_ROTATION_ROLL: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 rot = manager.getRotation(String.valueOf(arg0));
                return (rot != null) ? rot.z : 0.0;
            }
            case GET_3D_SCALE_X: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 scale = manager.getScale(String.valueOf(arg0));
                return (scale != null) ? scale.x : 0.0;
            }
            case GET_3D_SCALE_Y: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 scale = manager.getScale(String.valueOf(arg0));
                return (scale != null) ? scale.y : 0.0;
            }
            case GET_3D_SCALE_Z: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Vector3 scale = manager.getScale(String.valueOf(arg0));
                return (scale != null) ? scale.z : 0.0;
            }
            case GET_3D_DISTANCE: {
                ThreeDManager manager = getThreeDManager();
                if (manager == null) return 0.0;
                Float dist = manager.getDistance(String.valueOf(arg0), String.valueOf(arg1));
                return (dist != null) ? dist : 0.0;
            }
            case RAY_DID_HIT2: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return FALSE;
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                return booleanToDouble(result9 != null && result9.hasHit);
            }
            case RAY_HIT_SPRITE_NAME: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return "";
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                if (result9 != null && result9.hasHit && result9.hitSprite != null) {
                    return result9.hitSprite.getName();
                }
                return "";
            }
            case RAY_HIT_X: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return 0.0;
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                if (result9 != null && result9.hasHit) {
                    return (double) result9.hitPoint.x;
                }
                return 0.0;
            }
            case RAY_HIT_Y: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return 0.0;
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                if (result9 != null && result9.hasHit) {
                    return (double) result9.hitPoint.y;
                }
                return 0.0;
            }
            case RAY_HIT_DISTANCE: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return 0.0;
                PhysicsWorld.RayCastResult result9 = scene.getPhysicsWorld().getRayCastResult(String.valueOf(arg0));
                if (result9 != null && result9.hasHit) {
                    return (double) result9.hitPoint.dst(scope.getSprite().look.getX(), scope.getSprite().look.getY());
                }
                return 0.0;
            }
            case JSON_GET:
                return interpretFunctionJsonGet(arg0, arg1);
            case JSON_SET:
                return interpretFunctionJsonSet(arg0, arg1, arg2);
            case JSON_IS_VALID:
                return interpretFunctionJsonIsValid(arg0);
            case REPEAT: {
                Integer timesOpt = tryParseIntFromObject(arg1);
                String sourceStr = arg0 != null ? String.valueOf(arg0) : "";
                return sourceStr.repeat(Math.max(0, timesOpt != null ? timesOpt : 0));
            }
            case REPLACE:
                return (arg0 != null ? String.valueOf(arg0) : "").replace(String.valueOf(arg1), String.valueOf(arg2));
            case CONTAINS_STR:
                return (arg0 != null ? String.valueOf(arg0) : "").contains(String.valueOf(arg1));
            case TABLE_X:
                return TableManager.Companion.getTableXSize(String.valueOf(arg0));
            case VIEW_X: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return activity == null ? 0 : activity.getViewX(String.valueOf(arg0));
            }
            case VIEW_Y: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return activity == null ? 0 : activity.getViewY(String.valueOf(arg0));
            }
            case VIEW_WIDTH: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return activity == null ? 0 : activity.getViewWidth(String.valueOf(arg0));
            }
            case VIEW_HEIGHT: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return activity == null ? 0 : activity.getViewHeight(String.valueOf(arg0));
            }
            case RAY_DID_HIT: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? Conversions.booleanToDouble(manager.getRayDidHit(String.valueOf(arg0))) : Conversions.FALSE;
            }
            case GET_RAY_HIT_X: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitPointX(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_Y: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitPointY(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_Z: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitPointZ(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_NORMAL_X: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitNormalX(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_NORMAL_Y: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitNormalY(String.valueOf(arg0)) : 0.0;
            }
            case GET_RAY_HIT_NORMAL_Z: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRayHitNormalZ(String.valueOf(arg0)) : 0.0;
            }
            case VIDEO_PLAYING: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return activity != null && activity.isVideoPlaying(String.valueOf(arg0));
            }
            case VIDEO_TIME: {
                StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                return activity != null ? activity.getVideoCurrentTime(String.valueOf(arg0)) : 0;
            }
            case GET_RAY_DISTANCE: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? (double) manager.getRaycastDistance(String.valueOf(arg0)) : -1.0;
            }
            case GET_RAY_HIT_OBJECT: {
                ThreeDManager manager = getThreeDManager();
                return manager != null ? manager.getRaycastHitObjectId(String.valueOf(arg0)) : "";
            }
            case VOXEL_GET_ID: {
                String worldId = String.valueOf(arg0);
                int x = (int) Math.floor(tryInterpretDoubleValue(arg1));
                int y = (int) Math.floor(tryInterpretDoubleValue(arg2));
                int z = (int) Math.floor(tryInterpretDoubleValue(arg3));

                var buffer = org.catrobat.catroid.raptor.VoxelManager.Companion.getBuffer(worldId);
                return (double) (buffer != null ? buffer.get(x, y, z) : 0.0);
            }
            case VOXEL_GET_DATA: {
                String worldId = String.valueOf(arg0);
                int x = (int) Math.floor(tryInterpretDoubleValue(arg1));
                int y = (int) Math.floor(tryInterpretDoubleValue(arg2));
                int z = (int) Math.floor(tryInterpretDoubleValue(arg3));

                var buffer = org.catrobat.catroid.raptor.VoxelManager.Companion.getBuffer(worldId);
                return (double) (buffer != null ? buffer.getData(x, y, z) : 0);
            }
            case FLOATARRAY:
                return FloatArrayManager.INSTANCE.getArraySize(String.valueOf(arg0));
            case TABLE_Y:
                return TableManager.Companion.getTableYSize(String.valueOf(arg0));
            case TABLE_ELEMENT: {
                Integer tabX = tryParseIntFromObject(arg1);
                Integer tabY = tryParseIntFromObject(arg2);
                return TableManager.Companion.getElementValue(String.valueOf(arg0), tabX != null ? tabX : 0, tabY != null ? tabY : 0);
            }
            case TABLE_JOIN:
                return TableManager.Companion.tableToString(String.valueOf(arg0), String.valueOf(arg1), String.valueOf(arg2));
            case UPPER:
                return arg0 != null ? String.valueOf(arg0).toUpperCase() : "";
            case LOWER:
                return arg0 != null ? String.valueOf(arg0).toLowerCase() : "";
            case REVERSE:
                return new StringBuilder(arg0 != null ? String.valueOf(arg0) : "").reverse().toString();
            case VAR:
                return UserVarsManager.INSTANCE.getVar(String.valueOf(arg0));
            case VARNAME: {
                Integer nameId = tryParseIntFromObject(arg0);
                return UserVarsManager.INSTANCE.getVarName(nameId != null ? nameId : -1);
            }
            case VARVALUE: {
                Integer valId = tryParseIntFromObject(arg0);
                return UserVarsManager.INSTANCE.getVarValue(valId != null ? valId : -1);
            }
            case LENGTH:
                return interpretFunctionLength(arg0, scope);
            case JOIN:
                return interpretFunctionJoin(scope, leftChild, rightChild);
            case JOIN3:
                return interpretFunctionJoin3(scope, leftChild, rightChild, additionalChildren);
            case DISTANCE:
                return interpretFunctionDistance(scope, arg0, arg1);
            case JOINNUMBER:
                return interpretFunctionJoinNumber(scope, leftChild, rightChild);
            case REGEX:
                return tryInterpretFunctionRegex(scope, leftChild, rightChild);
            case LIST_ITEM:
                return interpretFunctionListItem(arg0, scope);
            case CONTAINS:
                return interpretFunctionContains(arg1, scope);
            case NUMBER_OF_ITEMS:
                return interpretFunctionNumberOfItems(arg0, scope);
            case INDEX_OF_ITEM:
                return interpretFunctionIndexOfItem(arg0, scope);
            case FLATTEN:
                return interpretFunctionFlatten(scope, leftChild);
            case CONNECT:
                return interpretFunctionConnect(arg1, scope);
            case FIND:
                return interpretFunctionFind(arg1, scope);
            case COLLIDES_WITH_COLOR:
                return booleanToDouble(new ColorCollisionDetection(scope, StageActivity.getActiveStageListener())
                        .tryInterpretFunctionTouchesColor(arg0));
            case COLOR_TOUCHES_COLOR:
                return booleanToDouble(new ColorCollisionDetection(scope, StageActivity.getActiveStageListener())
                        .tryInterpretFunctionColorTouchesColor(arg0, arg1));
            case COLOR_AT_XY:
                return new ColorAtXYDetection(scope, StageActivity.getActiveStageListener())
                        .tryInterpretFunctionColorAtXY(arg0, arg1);
            case TOUCHES_OBJECT_BY_NAME: {
                StageListener stageListener = StageActivity.getActiveStageListener();
                if (stageListener == null) return FALSE;
                Sprite touchSprite = scope.getSprite();
                if (touchSprite == null || touchSprite.look == null) return FALSE;
                return tryInterpretCollision(
                        touchSprite.look,
                        String.valueOf(arg0),
                        ProjectManager.getInstance().getCurrentlyPlayingScene(),
                        stageListener
                );
            }
            case TEXT_BLOCK_FROM_CAMERA:
                return textBlockFunctionProvider.interpretFunctionTextBlock(tryInterpretDoubleValue(arg0));
            case TEXT_BLOCK_LANGUAGE_FROM_CAMERA:
                return textBlockFunctionProvider.interpretFunctionTextBlockLanguage(tryInterpretDoubleValue(arg0));
            case COLOR_EQUALS_COLOR:
                return booleanToDouble(new ColorEqualsColor().tryInterpretFunctionColorEqualsColor(arg0, arg1, arg2));
            case FILE_EXISTS: {
                String path = String.valueOf(arg0);
                Project project = ProjectManager.getInstance().getCurrentProject();
                if (project == null) return FALSE;
                try {
                    File file = new File(project.getDirectory(), path);
                    return booleanToDouble(file.exists() && file.isFile());
                } catch (Exception e) { return FALSE; }
            }
            case FILE_PROJECT_EXISTS: {
                String fileName = String.valueOf(arg0);
                Project project = ProjectManager.getInstance().getCurrentProject();
                if (project == null) return FALSE;
                try {
                    File file = project.getFile(fileName);
                    return booleanToDouble(file.exists() && file.isFile());
                } catch (Exception e) { return FALSE; }
            }
            case FILE_EXISTS_IN_DIR: {
                String fileName = String.valueOf(arg0);
                String dirName = String.valueOf(arg1);
                try {
                    File downloadsDir = new File(RuntimeServicesHolder.services.getDownloadsDir());
                    File dir = new File(downloadsDir, dirName);
                    File file = new File(dir, fileName);
                    return booleanToDouble(file.exists() && file.isFile());
                } catch (Exception e) { return FALSE; }
            }
            case FILE_EXISTS_AT_PATH: {
                String fileName = String.valueOf(arg0);
                String path = String.valueOf(arg1);
                try {
                    File downloadsDir = new File(RuntimeServicesHolder.services.getDownloadsDir());
                    File dir = new File(downloadsDir, path);
                    File file = new File(dir, fileName);
                    return booleanToDouble(file.exists() && file.isFile());
                } catch (Exception e) { return FALSE; }
            }
            case DEVICE_NAME:
                return android.os.Build.MODEL != null ? android.os.Build.MODEL : "Unknown";
            case DEVICE_MANUFACTURER:
                return android.os.Build.MANUFACTURER != null ? android.os.Build.MANUFACTURER : "Unknown";
            case ANDROID_VERSION:
                return android.os.Build.VERSION.RELEASE != null ? android.os.Build.VERSION.RELEASE : "Unknown";
            case API_LEVEL:
                return (double) android.os.Build.VERSION.SDK_INT;
            case SYSTEM_LANGUAGE: {
                try {
                    java.util.Locale locale = android.content.res.Resources.getSystem().getConfiguration().getLocales().get(0);
                    return locale != null ? locale.getDisplayLanguage() : "Unknown";
                } catch (Exception e) { return "Unknown"; }
            }
            case SYSTEM_THEME: {
                try {
                    int nightMode = CatroidApplication.getAppContext().getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                    return (nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) ? "Dark" : "Light";
                } catch (Exception e) { return "Unknown"; }
            }
            case CPU_NAME: {
                // Use try-with-resources to guarantee RandomAccessFile is always closed
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.FileReader("/proc/cpuinfo"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains("hardware")) {
                            String[] parts = line.split(":");
                            return parts.length > 1 ? parts[1].trim() : "Unknown";
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG_FORMULA_ELEMENT, "Failed to read CPU info", e);
                }
                return "Unknown";
            }
            case CPU_ARCHITECTURE:
                return android.os.Build.SUPPORTED_ABIS != null && android.os.Build.SUPPORTED_ABIS.length > 0
                    ? android.os.Build.SUPPORTED_ABIS[0] : "Unknown";
            case CPU_CORES:
                return (double) Runtime.getRuntime().availableProcessors();
            case CPU_FREQUENCY: {
                try (java.io.RandomAccessFile reader = new java.io.RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r")) {
                    String line = reader.readLine();
                    long khz = Long.parseLong(line != null ? line.trim() : "0");
                    return khz / 1000.0;
                } catch (Exception e) { return 0.0; }
            }
            case CPU_FREQUENCY_MIN: {
                try (java.io.RandomAccessFile reader = new java.io.RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq", "r")) {
                    String line = reader.readLine();
                    long khz = Long.parseLong(line != null ? line.trim() : "0");
                    return khz / 1000.0;
                } catch (Exception e) { return 0.0; }
            }
            case CPU_USAGE: {
                // /proc/stat values are cumulative (since boot), not instantaneous.
                // Correct CPU% requires delta between two snapshots: (deltaWork / deltaTotal) * 100.
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.FileReader("/proc/stat"))) {
                    String line = reader.readLine();
                    if (line == null) return 0.0;
                    String[] fields = line.trim().split("\\s+");
                    if (fields.length < 5) return 0.0;
                    long idle = Long.parseLong(fields[4]);
                    long total = 0;
                    for (int i = 1; i < fields.length; i++) total += Long.parseLong(fields[i]);
                    long deltaTotal = total - cpuPrevTotal;
                    long deltaIdle = idle - cpuPrevIdle;
                    cpuPrevTotal = total;
                    cpuPrevIdle = idle;
                    // First call: no delta available yet, return 0
                    if (deltaTotal <= 0) return 0.0;
                    return (double) (deltaTotal - deltaIdle) * 100.0 / deltaTotal;
                } catch (Exception e) { return 0.0; }
            }
            case TOTAL_RAM: {
                try {
                    android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
                    android.app.ActivityManager activityManager = (android.app.ActivityManager)
                        CatroidApplication.getAppContext().getSystemService(android.content.Context.ACTIVITY_SERVICE);
                    activityManager.getMemoryInfo(mi);
                    return mi.totalMem / (1024.0 * 1024.0);
                } catch (Exception e) { return 0.0; }
            }
            case FREE_RAM: {
                try {
                    android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
                    android.app.ActivityManager activityManager = (android.app.ActivityManager)
                        CatroidApplication.getAppContext().getSystemService(android.content.Context.ACTIVITY_SERVICE);
                    activityManager.getMemoryInfo(mi);
                    return mi.availMem / (1024.0 * 1024.0);
                } catch (Exception e) { return 0.0; }
            }
            case USED_RAM: {
                try {
                    android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
                    android.app.ActivityManager activityManager = (android.app.ActivityManager)
                        CatroidApplication.getAppContext().getSystemService(android.content.Context.ACTIVITY_SERVICE);
                    activityManager.getMemoryInfo(mi);
                    return (mi.totalMem - mi.availMem) / (1024.0 * 1024.0);
                } catch (Exception e) { return 0.0; }
            }
            case TOTAL_STORAGE: {
                try {
                    android.os.StatFs stat = new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());
                    return stat.getBlockCountLong() * stat.getBlockSizeLong() / (1024.0 * 1024.0);
                } catch (Exception e) { return 0.0; }
            }
            case FREE_STORAGE: {
                try {
                    android.os.StatFs stat = new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());
                    return stat.getAvailableBlocksLong() * stat.getBlockSizeLong() / (1024.0 * 1024.0);
                } catch (Exception e) { return 0.0; }
            }
            case USED_STORAGE: {
                try {
                    android.os.StatFs stat = new android.os.StatFs(android.os.Environment.getDataDirectory().getPath());
                    long total = stat.getBlockCountLong() * stat.getBlockSizeLong();
                    long free = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
                    return (total - free) / (1024.0 * 1024.0);
                } catch (Exception e) { return 0.0; }
            }
            case BATTERY_PERCENT: {
                try {
                    android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
                    android.content.Intent batteryStatus = CatroidApplication.getAppContext().registerReceiver(null, filter);
                    if (batteryStatus == null) return 0.0;
                    int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                    return (level > 0 && scale > 0) ? (double) (level * 100 / scale) : 0.0;
                } catch (Exception e) { return 0.0; }
            }
            case BATTERY_CHARGING: {
                try {
                    android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
                    android.content.Intent batteryStatus = CatroidApplication.getAppContext().registerReceiver(null, filter);
                    if (batteryStatus == null) return FALSE;
                    int status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                    boolean charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING
                        || status == android.os.BatteryManager.BATTERY_STATUS_FULL;
                    return booleanToDouble(charging);
                } catch (Exception e) { return FALSE; }
            }
            case BATTERY_TEMP: {
                try {
                    android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
                    android.content.Intent batteryStatus = CatroidApplication.getAppContext().registerReceiver(null, filter);
                    if (batteryStatus == null) return 0.0;
                    int temp = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0);
                    return (double) (temp / 10.0);
                } catch (Exception e) { return 0.0; }
            }
            case BATTERY_VOLTAGE: {
                try {
                    android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
                    android.content.Intent batteryStatus = CatroidApplication.getAppContext().registerReceiver(null, filter);
                    if (batteryStatus == null) return 0.0;
                    int voltage = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0);
                    return (double) (voltage / 1000.0);
                } catch (Exception e) { return 0.0; }
            }
            case BATTERY_STATE: {
                try {
                    android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
                    android.content.Intent batteryStatus = CatroidApplication.getAppContext().registerReceiver(null, filter);
                    if (batteryStatus == null) return "Unknown";
                    int status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                    switch (status) {
                        case android.os.BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
                        case android.os.BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
                        case android.os.BatteryManager.BATTERY_STATUS_FULL: return "Full";
                        case android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not charging";
                        default: return "Unknown";
                    }
                } catch (Exception e) { return "Unknown"; }
            }
            case INTERNET_CONNECTED: {
                try {
                    android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                        CatroidApplication.getAppContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.net.Network activeNetwork = cm.getActiveNetwork();
                        if (activeNetwork == null) return FALSE;
                        android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(activeNetwork);
                        boolean connected = nc != null && (nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                            || nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                            || nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
                            || nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN));
                        return booleanToDouble(connected);
                    } else {
                        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        return booleanToDouble(activeNetwork != null && activeNetwork.isConnected());
                    }
                } catch (Exception e) { return FALSE; }
            }
            case INTERNET_TYPE: {
                try {
                    android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                        CatroidApplication.getAppContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.net.Network activeNetwork = cm.getActiveNetwork();
                        if (activeNetwork == null) return "None";
                        android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(activeNetwork);
                        if (nc == null) return "None";
                        if (nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
                        if (nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)) return "Mobile";
                        if (nc.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
                        return "Other";
                    } else {
                        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        if (activeNetwork == null || !activeNetwork.isConnected()) return "None";
                        switch (activeNetwork.getType()) {
                            case android.net.ConnectivityManager.TYPE_WIFI: return "Wi-Fi";
                            case android.net.ConnectivityManager.TYPE_MOBILE: return "Mobile";
                            case android.net.ConnectivityManager.TYPE_ETHERNET: return "Ethernet";
                            default: return "Other";
                        }
                    }
                } catch (Exception e) { return "Unknown"; }
            }
            case INTERNET_SPEED: {
                try {
                    android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                        CatroidApplication.getAppContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.net.Network activeNetwork = cm.getActiveNetwork();
                        if (activeNetwork == null) return 0.0;
                        android.net.NetworkCapabilities nc = cm.getNetworkCapabilities(activeNetwork);
                        if (nc != null) {
                            int down = nc.getLinkDownstreamBandwidthKbps();
                            return (double) (down / 1000); // Mbps
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG_FORMULA_ELEMENT, "Failed to read network bandwidth", e);
                }
                return 0.0;
            }
            case LOCAL_IP: {
                try {
                    java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        java.net.NetworkInterface iface = interfaces.nextElement();
                        if (iface.isLoopback() || !iface.isUp()) continue;
                        java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            java.net.InetAddress addr = addresses.nextElement();
                            if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                                return addr.getHostAddress();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG_FORMULA_ELEMENT, "Failed to read local IP", e);
                }
                return "Unknown";
            }
            case SCREEN_WIDTH: {
                try {
                    android.util.DisplayMetrics metrics = CatroidApplication.getAppContext()
                        .getResources().getDisplayMetrics();
                    return (double) metrics.widthPixels;
                } catch (Exception e) { return 0.0; }
            }
            case SCREEN_HEIGHT: {
                try {
                    android.util.DisplayMetrics metrics = CatroidApplication.getAppContext()
                        .getResources().getDisplayMetrics();
                    return (double) metrics.heightPixels;
                } catch (Exception e) { return 0.0; }
            }
            case SCREEN_DPI: {
                try {
                    android.util.DisplayMetrics metrics = CatroidApplication.getAppContext()
                        .getResources().getDisplayMetrics();
                    return (double) metrics.densityDpi;
                } catch (Exception e) { return 0.0; }
            }
            case SCREEN_REFRESH: {
                try {
                    android.view.WindowManager wm = (android.view.WindowManager)
                        CatroidApplication.getAppContext().getSystemService(android.content.Context.WINDOW_SERVICE);
                    android.view.Display display = wm.getDefaultDisplay();
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.view.Display.Mode mode = display.getMode();
                        return (double) mode.getRefreshRate();
                    } else {
                        return (double) display.getRefreshRate();
                    }
                } catch (Exception e) { return 0.0; }
            }
            case GET_STATUS_CODE: {
                try {
                    return (double) org.catrobat.catroid.content.actions.DownloadState.INSTANCE.getLastStatusCode();
                } catch (Exception e) { return 0.0; }
            }
            case DOWNLOAD_PROGRESS: {
                try {
                    int prog = org.catrobat.catroid.content.actions.DownloadState.INSTANCE.getProgress();
                    return prog >= 0 ? (double) prog : 0.0;
                } catch (Exception e) { return 0.0; }
            }
            case SCREEN_ORIENTATION: {
                try {
                    int orientation = CatroidApplication.getAppContext().getResources()
                        .getConfiguration().orientation;
                    return (orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                        ? "Landscape" : "Portrait";
                } catch (Exception e) { return "Unknown"; }
            }
            case VOLUME_LEVEL: {
                try {
                    android.media.AudioManager audio = (android.media.AudioManager)
                        CatroidApplication.getAppContext().getSystemService(android.content.Context.AUDIO_SERVICE);
                    return (double) audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
                } catch (Exception e) { return 0.0; }
            }
            case SCREEN_BRIGHTNESS: {
                try {
                    int brightness = android.provider.Settings.System.getInt(
                            CatroidApplication.getAppContext().getContentResolver(),
                            android.provider.Settings.System.SCREEN_BRIGHTNESS, 255);
                    return (double) (brightness * 100.0 / 255.0);
                } catch (Exception e) { return 0.0; }
            }
            case IS_IN_FOREGROUND: {
                try {
                    StageActivity activity = StageActivity.activeStageActivity != null ? StageActivity.activeStageActivity.get() : null;
                    return booleanToDouble(activity != null && !activity.isFinishing());
                } catch (Exception e) { return FALSE; }
            }
            case IS_PC: {
                return 0.0; // On Android, this is always false
            }
            case IS_MOBILE: {
                return 1.0; // On Android, this is always true
            }
            case GPU_NAME: {
                try {
                    return com.badlogic.gdx.Gdx.graphics.getGLVersion().getRendererString();
                } catch (Exception e) { return ""; }
            }
            case OPENGL_VERSION: {
                try {
                    return com.badlogic.gdx.Gdx.graphics.getGLVersion().getVersionString();
                } catch (Exception e) { return ""; }
            }
            case VULKAN_SUPPORTED: {
                try {
                    android.content.pm.PackageManager pm = CatroidApplication.getAppContext().getPackageManager();
                    boolean hasVulkan = pm.hasSystemFeature("android.hardware.vulkan");
                    return booleanToDouble(hasVulkan);
                } catch (Exception e) { return FALSE; }
            }
            case SPRITE_EXISTS: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return FALSE;
                return booleanToDouble(scene.getSprite(String.valueOf(arg0)) != null);
            }
            case SPRITE_X: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null && s.look != null) ? (double) s.look.getXInUserInterfaceDimensionUnit() : 0.0;
            }
            case SPRITE_Y: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null && s.look != null) ? (double) s.look.getYInUserInterfaceDimensionUnit() : 0.0;
            }
            case SPRITE_SIZE: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null && s.look != null) ? (double) s.look.getSizeInUserInterfaceDimensionUnit() : 0.0;
            }
            case SPRITE_WIDTH: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null && s.look != null) ? (double) s.look.getWidthInUserInterfaceDimensionUnit() : 0.0;
            }
            case SPRITE_HEIGHT: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null && s.look != null) ? (double) s.look.getHeightInUserInterfaceDimensionUnit() : 0.0;
            }
            case SPRITE_DIRECTION: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null && s.look != null) ? (double) s.look.getLookDirectionInUserInterfaceDimensionUnit() : 0.0;
            }
            case SPRITE_VISIBLE: {
                Sprite s = findSprite(String.valueOf(arg0));
                return booleanToDouble(s != null && s.look != null && s.look.isLookVisible());
            }
            case SPRITE_TRANSPARENCY: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null && s.look != null) ? (double) s.look.getTransparencyInUserInterfaceDimensionUnit() : 0.0;
            }
            case SPRITE_LAYER: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return 0.0;
                java.util.List<Sprite> list = scene.getSpriteList();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getName().equals(String.valueOf(arg0))) return (double) (list.size() - i);
                }
                return 0.0;
            }
            case SPRITE_NAME_GET: {
                Sprite s = findSprite(String.valueOf(arg0));
                return s != null ? s.getName() : "";
            }
            case SPRITE_INDEX_GET: {
                Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
                if (scene == null) return 0.0;
                java.util.List<Sprite> list = scene.getSpriteList();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getName().equals(String.valueOf(arg0))) return (double) (i + 1);
                }
                return 0.0;
            }
            case SPRITE_UUID: {
                Sprite s = findSprite(String.valueOf(arg0));
                return s != null ? s.getSpriteId() : "";
            }
            case SPRITE_CLONE_COUNT: {
                StageListener listener = StageActivity.getActiveStageListener();
                if (listener == null) return 0.0;
                String name = String.valueOf(arg0);
                int count = 0;
                for (Sprite sp : listener.getSpritesFromStage()) {
                    if (sp.isClone && name.equals(sp.getName())) count++;
                }
                return (double) count;
            }
            case SPRITE_LOOK_COUNT: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null) ? (double) s.getLookList().size() : 0.0;
            }
            case SPRITE_SOUND_COUNT: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null) ? (double) s.getSoundList().size() : 0.0;
            }
            case SPRITE_VARIABLE_COUNT: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null) ? (double) s.getUserVariables().size() : 0.0;
            }
            case SPRITE_LIST_COUNT: {
                Sprite s = findSprite(String.valueOf(arg0));
                return (s != null) ? (double) s.getUserLists().size() : 0.0;
            }
            case SPRITE_DISTANCE: {
                Sprite s1 = findSprite(String.valueOf(arg0));
                Sprite s2 = findSprite(String.valueOf(arg1));
                if (s1 == null || s2 == null || s1.look == null || s2.look == null) return 0.0;
                float dx = s1.look.getXInUserInterfaceDimensionUnit() - s2.look.getXInUserInterfaceDimensionUnit();
                float dy = s1.look.getYInUserInterfaceDimensionUnit() - s2.look.getYInUserInterfaceDimensionUnit();
                return (double) (float) Math.sqrt(dx * dx + dy * dy);
            }
            case SPRITE_TOUCHING: {
                Sprite s1 = findSprite(String.valueOf(arg0));
                Sprite s2 = findSprite(String.valueOf(arg1));
                if (s1 == null || s2 == null || s1.look == null || s2.look == null) return FALSE;
                StageListener listener = StageActivity.getActiveStageListener();
                if (listener == null) return FALSE;
                return tryInterpretCollision(s1.look, s2.getName(), ProjectManager.getInstance().getCurrentlyPlayingScene(), listener);
            }
            case SPRITE_ANGLE_TO: {
                Sprite s1 = findSprite(String.valueOf(arg0));
                Sprite s2 = findSprite(String.valueOf(arg1));
                if (s1 == null || s2 == null || s1.look == null || s2.look == null) return 0.0;
                float dx = s2.look.getXInUserInterfaceDimensionUnit() - s1.look.getXInUserInterfaceDimensionUnit();
                float dy = s2.look.getYInUserInterfaceDimensionUnit() - s1.look.getYInUserInterfaceDimensionUnit();
                return (double) Math.toDegrees(Math.atan2(dy, dx));
            }
            case ADMOB_IS_INITIALIZED:
                return AdMobManager.INSTANCE.isInitialized();
            case ADMOB_IS_TEST_MODE:
                return AdMobManager.INSTANCE.isTestMode();
            case ADMOB_IS_BANNER_LOADED:
                return AdMobManager.INSTANCE.isBannerLoaded();
            case ADMOB_IS_INTERSTITIAL_LOADED:
                return AdMobManager.INSTANCE.isInterstitialLoaded();
            case ADMOB_IS_REWARDED_LOADED:
                return AdMobManager.INSTANCE.isRewardedLoaded();
            case ADMOB_IS_APP_OPEN_LOADED:
                return AdMobManager.INSTANCE.isAppOpenLoaded();
            case ADMOB_LAST_ERROR_CODE:
                return (double) AdMobManager.INSTANCE.getLastErrorCode();
            case ADMOB_LAST_ERROR_MESSAGE:
                return AdMobManager.INSTANCE.getLastErrorMessage();
            case ADMOB_IS_GOOGLE_PLAY_SERVICES_AVAILABLE: {
                if (StageActivity.activeStageActivity != null) {
                    Activity activity = StageActivity.activeStageActivity.get();
                    if (activity != null) {
                        return AdMobManager.INSTANCE.isGooglePlayServicesAvailable(activity);
                    }
                }
                return false;
            }

            // -- Cryptography ---
            case SHA_224:
                return CryptoFormulaHelper.sha("SHA-224", String.valueOf(arg0));
            case SHA_256:
                return CryptoFormulaHelper.sha("SHA-256", String.valueOf(arg0));
            case SHA_384:
                return CryptoFormulaHelper.sha("SHA-384", String.valueOf(arg0));
            case SHA_512:
                return CryptoFormulaHelper.sha("SHA-512", String.valueOf(arg0));
            case HASH_BYTES:
                return CryptoFormulaHelper.hashBytes(String.valueOf(arg0));
            case HASH_FILE:
                return CryptoFormulaHelper.hashFile(String.valueOf(arg0));
            case AES_ENCRYPT:
                return CryptoFormulaHelper.aesEncrypt(String.valueOf(arg0), String.valueOf(arg1));
            case AES_DECRYPT:
                return CryptoFormulaHelper.aesDecrypt(String.valueOf(arg0), String.valueOf(arg1));
            case CHACHA20_ENCRYPT:
                return CryptoFormulaHelper.chaCha20Encrypt(String.valueOf(arg0), String.valueOf(arg1), String.valueOf(arg2));
            case CHACHA20_DECRYPT:
                return CryptoFormulaHelper.chaCha20Decrypt(String.valueOf(arg0), String.valueOf(arg1), String.valueOf(arg2));
            case PBKDF2: {
                Integer iters = tryParseIntFromObject(arg2);
                return CryptoFormulaHelper.pbkdf2(String.valueOf(arg0), String.valueOf(arg1), iters != null ? iters : 10000);
            }
            case GENERATE_SALT: {
                Integer len = tryParseIntFromObject(arg0);
                return CryptoFormulaHelper.generateSalt(len != null ? len : 16);
            }
            case DERIVE_KEY:
                return CryptoFormulaHelper.deriveKey(String.valueOf(arg0));
            case GENERATE_AES_KEY: {
                Integer bits = tryParseIntFromObject(arg0);
                return CryptoFormulaHelper.generateAesKey(bits != null ? bits : 256);
            }
            case GENERATE_RANDOM_BYTES: {
                Integer len = tryParseIntFromObject(arg0);
                return CryptoFormulaHelper.generateRandomBytes(len != null ? len : 16);
            }
            case GENERATE_PASSWORD: {
                Integer len = tryParseIntFromObject(arg0);
                return CryptoFormulaHelper.generatePassword(len != null ? len : 16);
            }
            case GENERATE_UUID:
                return CryptoFormulaHelper.generateUuid();
            case RANDOM_HEX: {
                Integer len = tryParseIntFromObject(arg0);
                return CryptoFormulaHelper.randomHex(len != null ? len : 16);
            }
            case RANDOM_BASE64: {
                Integer len = tryParseIntFromObject(arg0);
                return CryptoFormulaHelper.randomBase64(len != null ? len : 16);
            }
            case RANDOM_INT_SECURE:
                return CryptoFormulaHelper.randomIntSecure(tryInterpretDoubleValue(arg0), tryInterpretDoubleValue(arg1));
            case RANDOM_STRING_SECURE: {
                Integer len = tryParseIntFromObject(arg0);
                return CryptoFormulaHelper.randomStringSecure(len != null ? len : 16);
            }
            case BASE64_ENCODE:
                return CryptoFormulaHelper.base64Encode(String.valueOf(arg0));
            case BASE64_DECODE:
                return CryptoFormulaHelper.base64Decode(String.valueOf(arg0));
            case HEX_ENCODE:
                return CryptoFormulaHelper.hexEncode(String.valueOf(arg0));
            case HEX_DECODE:
                return CryptoFormulaHelper.hexDecode(String.valueOf(arg0));
            case COMPARE_HASH:
                return CryptoFormulaHelper.compareHash(String.valueOf(arg0), String.valueOf(arg1));
            case IS_BASE64:
                return CryptoFormulaHelper.isBase64(String.valueOf(arg0));
            case IS_HEX:
                return CryptoFormulaHelper.isHex(String.valueOf(arg0));
            case HMAC_SHA_256:
                return CryptoFormulaHelper.hmacSha256(String.valueOf(arg0), String.valueOf(arg1));
            case HMAC_SHA_512:
                return CryptoFormulaHelper.hmacSha512(String.valueOf(arg0), String.valueOf(arg1));
            case RSA_GENERATE_KEY_PAIR:
                return CryptoFormulaHelper.rsaGenerateKeyPair();
            case RSA_ENCRYPT:
                return CryptoFormulaHelper.rsaEncrypt(String.valueOf(arg0), String.valueOf(arg1));
            case RSA_DECRYPT:
                return CryptoFormulaHelper.rsaDecrypt(String.valueOf(arg0), String.valueOf(arg1));
            case RSA_SIGN:
                return CryptoFormulaHelper.rsaSign(String.valueOf(arg0), String.valueOf(arg1));
            case RSA_VERIFY:
                return CryptoFormulaHelper.rsaVerify(String.valueOf(arg0), String.valueOf(arg1), String.valueOf(arg2));

            case TILE_AT_POSITION: {
                Sprite tileSprite = scope.getSprite();
                if (tileSprite != null && tileSprite.look != null) {
                    LookData ld = tileSprite.look.getLookData();
                    if (ld instanceof TilemapLookData) {
                        TilemapLookData tmd = (TilemapLookData) ld;
                        int col = (int) Math.round(Double.parseDouble(String.valueOf(arg0)));
                        int row = (int) Math.round(Double.parseDouble(String.valueOf(arg1)));
                        return (double) tmd.getTile(col, row);
                    }
                }
                return 0.0;
            }
            case IS_SOLID_TILE_AT: {
                Sprite tileSprite2 = scope.getSprite();
                if (tileSprite2 != null && tileSprite2.look != null) {
                    LookData ld2 = tileSprite2.look.getLookData();
                    if (ld2 instanceof TilemapLookData) {
                        TilemapLookData tmd2 = (TilemapLookData) ld2;
                        int col2 = (int) Math.round(Double.parseDouble(String.valueOf(arg0)));
                        int row2 = (int) Math.round(Double.parseDouble(String.valueOf(arg1)));
                        short tile = tmd2.getTile(col2, row2);
                        return booleanToDouble(tmd2.getSolidTiles().contains((int) tile));
                    }
                }
                return 0.0;
            }
            case TILEMAP_WIDTH: {
                Sprite tileSprite3 = scope.getSprite();
                if (tileSprite3 != null && tileSprite3.look != null) {
                    LookData ld3 = tileSprite3.look.getLookData();
                    if (ld3 instanceof TilemapLookData) {
                        return (double) ((TilemapLookData) ld3).getMapColumns();
                    }
                }
                return 0.0;
            }
            case TILEMAP_HEIGHT: {
                Sprite tileSprite4 = scope.getSprite();
                if (tileSprite4 != null && tileSprite4.look != null) {
                    LookData ld4 = tileSprite4.look.getLookData();
                    if (ld4 instanceof TilemapLookData) {
                        return (double) ((TilemapLookData) ld4).getMapRows();
                    }
                }
                return 0.0;
            }
            case TILE_SIZE: {
                Sprite tileSprite5 = scope.getSprite();
                if (tileSprite5 != null && tileSprite5.look != null) {
                    LookData ld5 = tileSprite5.look.getLookData();
                    if (ld5 instanceof TilemapLookData) {
                        return (double) ((TilemapLookData) ld5).getTileWidth();
                    }
                }
                return 0.0;
            }

            default:
                return interpretFormulaFunction(function, arg0, arg1, arg2);
        }
    }

    private Object interpretCustomLunoFunction(CustomFormula customFormula, List<Object> arguments, Scope scope) {
        LoadedLibrary library = LibraryManager.INSTANCE.getLoadedLibrary(customFormula.getOwnerLibraryId());
        if (library == null) {
            Log.e(TAG_FORMULA_ELEMENT, "Библиотека " + customFormula.getOwnerLibraryId() + " не загружена для функции " + customFormula.getUniqueName());
            return "LIB NOT FOUND";
        }

        Interpreter interpreter = library.getInterpreter();

        try {
            LunoValue functionValue = interpreter.getGlobals().get(new Token(TokenType.IDENTIFIER, customFormula.getLunoFunctionName(), null, -1, -1));

            if (!(functionValue instanceof LunoValue.Callable)) {
                Log.e(TAG_FORMULA_ELEMENT, "Функция " + customFormula.getLunoFunctionName() + " не найдена или не является функцией в библиотеке " + library.getId());
                return "FUNC NOT FOUND";
            }
            LunoValue.Callable lunoFunction = (LunoValue.Callable) functionValue;

            List<LunoValue> lunoArgs = new ArrayList<>();
            for (Object arg : arguments) {
                lunoArgs.add(LunoValue.Companion.fromKotlin(arg));
            }

            LunoValue result = lunoFunction.call(interpreter, lunoArgs, new Token(TokenType.EOF, "", null, -1, -1));

            Object resultJava;
            if (result instanceof LunoValue.Number) {
                resultJava = ((LunoValue.Number) result).getValue();
            } else if (result instanceof LunoValue.String) {
                resultJava = ((LunoValue.String) result).getValue();
            } else if (result instanceof LunoValue.Boolean) {
                resultJava = ((LunoValue.Boolean) result).getValue() ? Conversions.TRUE : Conversions.FALSE;
            } else {
                resultJava = interpreter.lunoValueToString(result, true);
            }

            return resultJava;

        } catch (LunoRuntimeError e) {
            Log.e(TAG_FORMULA_ELEMENT, "Ошибка выполнения LunoScript для функции " + customFormula.getUniqueName(), e);
            return "LUNO ERROR";
        }
    }

    @Nullable
    private Sprite findSprite(String name) {
        if (name == null || name.isEmpty()) return null;
        Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
        return scene != null ? scene.getSprite(name) : null;
    }

    @Nullable
    private Object tryInterpretRecursive(FormulaElement element, Scope scope) {
        if (element == null) {
            return null;
        }
        return element.interpretRecursive(scope);
    }

    private Object interpretFunctionIfThenElseObject(Double condition, Object thenValue, Object elseValue) {
        if (Double.isNaN(condition)) return Double.NaN;
        if (condition != 0) return thenValue;
        return elseValue;
    }

    private double interpretFunctionIfThenElse(double condition, double thenValue, double elseValue) {
        if (Double.isNaN(condition)) return Double.NaN;
        if (condition != 0) return thenValue;
        return elseValue;
    }

    private Object interpretFormulaFunction(Functions function, Object arg0, Object arg1, Object arg2) {
        FormulaFunction formulaFunction = formulaFunctions.get(function);
        if (formulaFunction == null) {
            return FALSE;
        }

        Double val0 = convertArgumentToDouble(arg0);
        Double val1 = convertArgumentToDouble(arg1);

        double d0 = (val0 != null) ? val0 : 0.0;
        double d1 = (val1 != null) ? val1 : 0.0;

        if (arg2 == null) {
            return formulaFunction.execute(d0, d1);
        } else {
            Double val2 = convertArgumentToDouble(arg2);
            double d2 = (val2 != null) ? val2 : 0.0;
            return formulaFunction.execute(d0, d1, d2);
        }
    }

    private Object interpretFunctionNumberOfItems(Object left, Scope scope) {
        if (leftChild != null && leftChild.type == ElementType.USER_LIST) {
            UserList userList = UserDataWrapper.getUserList(leftChild.value, scope);
            return (double) handleNumberOfItemsOfUserListParameter(userList);
        }
        return interpretFunctionLength(left, scope);
    }

    private int handleNumberOfItemsOfUserListParameter(UserList userList) {
        if (userList == null) {
            return 0;
        }
        return userList.getValue().size();
    }

    private Object interpretFunctionContains(Object right, Scope scope) {
        UserList userList = getUserListOfChild(leftChild, scope);
        if (userList == null) {
            return FALSE;
        }
        for (Object userListElement : userList.getValue()) {
            if (interpretOperatorEqual(userListElement, right)) {
                return TRUE;
            }
        }
        return FALSE;
    }

    private Object interpretFunctionIndexOfItem(Object left, Scope scope) {
        if (rightChild != null && rightChild.getElementType() == ElementType.USER_LIST) {
            UserList userList = UserDataWrapper.getUserList(rightChild.value, scope);
            if (userList == null) {
                return 0.0;
            }
            return (double) (userList.getIndexOf(left) + 1);
        }
        return FALSE;
    }

    private Object interpretFunctionListItem(Object left, Scope scope) {
        if (left == null) return "";
        UserList userList = getUserListOfChild(rightChild, scope);
        if (userList == null) return "";

        Integer idxOpt = tryParseIntFromObject(left);
        if (idxOpt == null) return "";

        int index = idxOpt - 1;
        if (index < 0 || index >= userList.getValue().size()) return "";
        return userList.getValue().get(index);
    }

    private Object interpretFunctionConnect(Object right, Scope scope) {
        UserList userlist = getUserListOfChild(leftChild, scope);
        if (userlist == null) return "";

        List<Object> list = userlist.getValue();
        StringBuilder strBuilder = new StringBuilder();
        String comma = String.valueOf(right);

        boolean first = true;
        for (Object object : list) {
            if (object != null) {
                if (!first) {
                    strBuilder.append(comma);
                }
                strBuilder.append(String.valueOf(object));
                first = false;
            }
        }
        return strBuilder.toString();
    }

    private Object interpretFunctionFind(Object right, Scope scope) {
        UserList userlist = getUserListOfChild(leftChild, scope);
        if (userlist == null) return "";

        List<Object> list = userlist.getValue();
        for (int i = 0; i < list.size(); i++) {
            Object object = list.get(i);
            if (object != null && String.valueOf(object).equals(String.valueOf(right))) {
                return i + 1;
            }
        }
        return "";
    }

    @Nullable
    private UserList getUserListOfChild(FormulaElement child, Scope scope) {
        if (child == null || child.getElementType() != ElementType.USER_LIST) {
            return null;
        }
        return UserDataWrapper.getUserList(child.value, scope);
    }

    private static String interpretFunctionJoin(Scope scope, FormulaElement leftChild, FormulaElement rightChild) {
        return interpretFunctionString(leftChild, scope).concat(interpretFunctionString(rightChild, scope));
    }

    private static String interpretFunctionJoinNumber(Scope scope, FormulaElement leftChild, FormulaElement rightChild) {
        String numb1 = interpretFunctionString(leftChild, scope);
        String numb2 = interpretFunctionString(rightChild, scope);
        return numb1 + numb2;
    }

    private static String interpretFunctionJoin3(Scope scope, FormulaElement leftChild, FormulaElement rightChild, List<FormulaElement> additionalChildren) {
        String third = (additionalChildren != null && !additionalChildren.isEmpty())
            ? interpretFunctionString(additionalChildren.get(0), scope) : "";
        return interpretFunctionString(leftChild, scope).concat(interpretFunctionString(rightChild,
                scope).concat(third));
    }

    private static String interpretFunctionFlatten(Scope scope, FormulaElement leftChild) {
        return interpretFunctionString(leftChild, scope);
    }

    private static String tryInterpretFunctionRegex(Scope scope, FormulaElement leftChild, FormulaElement rightChild) {
        try {
            String left = interpretFunctionString(leftChild, scope);
            String right = interpretFunctionString(rightChild, scope);
            return interpretFunctionRegex(left, right);
        } catch (IllegalArgumentException exception) {
            return exception.getLocalizedMessage();
        }
    }

    private static String interpretFunctionRegex(String patternString, String matcherString) {
        Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(matcherString);
        if (matcher.find()) {
            int groupIndex = matcher.groupCount() == 0 ? 0 : 1;
            return matcher.group(groupIndex);
        } else {
            return "";
        }
    }

    private static String interpretFunctionString(FormulaElement child, Scope scope) {
        String parameterInterpretation = "";
        if (child != null) {
            Object objectInterpretation = child.interpretRecursive(scope);
            switch (child.getElementType()) {
                case STRING:
                    parameterInterpretation = child.getValue();
                    break;
                case NUMBER:
                    parameterInterpretation = formatNumberString(child.getValue());
                    break;
                default:
                    parameterInterpretation += objectInterpretation;
                    parameterInterpretation = trimTrailingCharacters(parameterInterpretation);
            }
        }
        return parameterInterpretation;
    }

    private static String formatNumberString(String numberString) {
        try {
            double number = Double.parseDouble(numberString);
            String formattedNumberString = "";
            if (!Double.isNaN(number)) {
                formattedNumberString += isInteger(number) ? (int) number : number;
            }
            return trimTrailingCharacters(formattedNumberString);
        } catch (NumberFormatException e) {
            // Corrupt/malformed NUMBER token: return the raw string instead of throwing.
            return trimTrailingCharacters(numberString);
        }
    }

    private Object interpretFunctionLength(Object left, Scope scope) {
        if (leftChild == null) {
            return FALSE;
        }
        switch (leftChild.type) {
            case NUMBER:
            case STRING:
                return (double) leftChild.value.length();
            case USER_VARIABLE:
                UserVariable userVariable = UserDataWrapper.getUserVariable(leftChild.value, scope);
                return (double) calculateUserVariableLength(userVariable);
            case USER_LIST:
                UserList userList = UserDataWrapper.getUserList(leftChild.value, scope);
                return calculateUserListLength(userList, left, scope);
            default:
                if (left instanceof Double && ((Double) left).isNaN()) {
                    return 0d;
                }
                return (double) (String.valueOf(left)).length();
        }
    }

    private int calculateUserVariableLength(UserVariable userVariable) {
        if (userVariable == null) {
            return 0;
        }
        Object userVariableValue = userVariable.getValue();
        if (userVariableValue == null) {
            return 0;
        }
        if (userVariableValue instanceof String) {
            return ((String) userVariableValue).length();
        }
        if (userVariableValue instanceof Boolean) {
            return 1;
        }
        String strVal = userVariableValue.toString();
        if (strVal.equals("true") || strVal.equals("false")) {
            return 1;
        }
        if (userVariableValue instanceof Number) {
            double doubleVal = ((Number) userVariableValue).doubleValue();
            if (Double.isNaN(doubleVal) || Double.isInfinite(doubleVal)) {
                return 0;
            }
            if (isInteger(doubleVal)) {
                return Integer.toString((int) doubleVal).length();
            } else {
                return Double.toString(doubleVal).length();
            }
        }
        return strVal.length();
    }

    private double calculateUserListLength(UserList userList, Object left, Scope scope) {
        if (userList == null || userList.getValue().isEmpty()) {
            return FALSE;
        }

        Object interpretedList = leftChild.interpretRecursive(scope);
        if (interpretedList instanceof Double) {
            Double interpretedListDoubleValue = (Double) interpretedList;
            if (interpretedListDoubleValue.isNaN() || interpretedListDoubleValue.isInfinite()) {
                return FALSE;
            }
            return String.valueOf(interpretedListDoubleValue.intValue()).length();
        }
        if (interpretedList instanceof String) {
            return ((String) interpretedList).length();
        }
        if (left instanceof Double && ((Double) left).isNaN()) {
            return FALSE;
        }
        return String.valueOf(left).length();
    }

    private Object interpretFunctionLetter(Object left, Object right) {
        if (left == null || right == null) return "";

        Integer idxOpt = tryParseIntFromObject(left);
        if (idxOpt == null) return "";

        int index = idxOpt - 1;
        String stringValueOfRight = String.valueOf(right);

        if (index < 0 || index >= stringValueOfRight.length()) {
            return "";
        }
        return String.valueOf(stringValueOfRight.charAt(index));
    }

    public static int levenshtain(String str1, String str2) {
        int lenStr1 = str1.length();
        int lenStr2 = str2.length();

        int[][] dp = new int[lenStr1 + 1][lenStr2 + 1];

        for (int i = 0; i <= lenStr1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= lenStr2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= lenStr1; i++) {
            for (int j = 1; j <= lenStr2; j++) {
                int cost = (str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[lenStr1][lenStr2];
    }

    private Object interpretFunctionDistance(Scope scope, Object val1, Object val2) {
        var value1 = val1.toString();
        var value2 = val2.toString();

        return(levenshtain(value1, value2));
    }

    private Object interpretFunctionJsonIsValid(Object arg) {
        String jsonString = String.valueOf(arg);
        try {
            String trimmed = jsonString.trim();
            if (trimmed.startsWith("{")) {
                new JSONObject(jsonString);
            } else if (trimmed.startsWith("[")) {
                new JSONArray(jsonString);
            } else {
                return Conversions.FALSE;
            }
        } catch (JSONException e) {
            return Conversions.FALSE;
        }
        return Conversions.TRUE;
    }

    private Object interpretFunctionJsonGet(Object jsonArg, Object pathArg) {
        String jsonString = String.valueOf(jsonArg);
        String path = String.valueOf(pathArg);
        try {
            Object current;
            String trimmed = jsonString.trim();
            if (trimmed.startsWith("[")) {
                current = new JSONArray(jsonString);
            } else {
                current = new JSONObject(jsonString);
            }
            String[] keys = path.isEmpty() ? new String[0] : path.split("\\.");
            for (int i = 0; i < keys.length; i++) {
                if (current == null || current == JSONObject.NULL) {
                    return "";
                }
                String key = keys[i];
                if (current instanceof JSONObject) {
                    current = ((JSONObject) current).opt(key);
                } else if (current instanceof JSONArray) {
                    JSONArray arr = (JSONArray) current;
                    try {
                        int idx = Integer.parseInt(key);
                        if (idx >= 0 && idx < arr.length()) {
                            current = arr.opt(idx);
                        } else {
                            return "";
                        }
                    } catch (NumberFormatException e) {
                        return "";
                    }
                } else {
                    return "";
                }
            }
            if (current == null || current == JSONObject.NULL) {
                return "";
            }
            if (current instanceof JSONObject || current instanceof JSONArray) {
                return current.toString();
            }
            return current;
        } catch (JSONException e) {
            return "";
        }
    }

    private Object interpretFunctionJsonSet(Object jsonArg, Object pathArg, Object valueArg) {
        String jsonString = String.valueOf(jsonArg);
        String path = String.valueOf(pathArg);
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String[] keys = path.split("\\.");
            JSONObject current = jsonObject;
            for (int i = 0; i < keys.length - 1; i++) {
                String key = keys[i];
                JSONObject next = current.optJSONObject(key);
                if (next == null) {
                    next = new JSONObject();
                    current.put(key, next);
                }
                current = next;
            }

            String finalKey = keys[keys.length - 1];
            try {
                if (valueArg instanceof String) {
                    String valStr = (String) valueArg;
                    if (valStr.contains(".")) {
                        current.put(finalKey, Double.parseDouble(valStr));
                    } else {
                        current.put(finalKey, Integer.parseInt(valStr));
                    }
                } else {
                    current.put(finalKey, valueArg);
                }
            } catch (NumberFormatException e) {
                current.put(finalKey, valueArg);
            }

            return jsonObject.toString();
        } catch (JSONException e) {
            return jsonString;
        }
    }

    private Object interpretFunctionSubtext(Object leftArg, Object rightArg, Object stringObj) {
        if (leftArg == null || rightArg == null) return "";

        Integer startOpt = tryParseIntFromObject(leftArg);
        Integer endOpt = tryParseIntFromObject(rightArg);

        if (startOpt == null || endOpt == null) return "";

        int start = startOpt - 1;
        int end = endOpt;
        String stringValueOfString = String.valueOf(stringObj);

        if (start < 0 || end < 0 || start > end || end > stringValueOfString.length()) {
            return "";
        }

        return stringValueOfString.substring(start, end);
    }

    private static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private Object interpretFunctionDistan(Object x1, Object y1, Object x2, Object y2) {
        double x1s = tryInterpretDoubleValue(x1), y1s = tryInterpretDoubleValue(y1);
        double x2s = tryInterpretDoubleValue(x2), y2s = tryInterpretDoubleValue(y2);
        return calculateDistance(x1s, y1s, x2s, y2s);
    }

    private Object interpretFunctionClamp(Object valueArg, Object leftArg, Object rightArg) {
        if (valueArg == null || leftArg == null || rightArg == null) return 0.0;

        double value = tryInterpretDoubleValue(valueArg);
        double min = tryInterpretDoubleValue(leftArg);
        double max = tryInterpretDoubleValue(rightArg);

        double start = Math.min(min, max);
        double end = Math.max(min, max);

        if (value < start) return start;
        if (value > end) return end;

        return value;
    }

    private Object interpretFunctionLerp(Object fromArg, Object toArg, Object tArg) {
        if (fromArg == null || toArg == null || tArg == null) return 0.0;
        double from = tryInterpretDoubleValue(fromArg);
        double to = tryInterpretDoubleValue(toArg);
        double t = tryInterpretDoubleValue(tArg);
        return from + (to - from) * t;
    }

    private Object interpretFunctionMapRange(Object valueArg, Object inMinArg, Object inMaxArg,
            Object outMinArg, Object outMaxArg) {
        if (valueArg == null || inMinArg == null || inMaxArg == null
                || outMinArg == null || outMaxArg == null) {
            return 0.0;
        }
        double value = tryInterpretDoubleValue(valueArg);
        double inMin = tryInterpretDoubleValue(inMinArg);
        double inMax = tryInterpretDoubleValue(inMaxArg);
        double outMin = tryInterpretDoubleValue(outMinArg);
        double outMax = tryInterpretDoubleValue(outMaxArg);

        if (inMax == inMin) {
            return outMin;
        }
        return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
    }

    private static int clampColorComponent(double component) {
        if (component < 0) return 0;
        if (component > 255) return 255;
        return (int) Math.round(component);
    }

    private Object interpretFunctionRgb(Object rArg, Object gArg, Object bArg) {
        int r = clampColorComponent(tryInterpretDoubleValue(rArg));
        int g = clampColorComponent(tryInterpretDoubleValue(gArg));
        int b = clampColorComponent(tryInterpretDoubleValue(bArg));
        return String.format(java.util.Locale.US, "#%02X%02X%02X", r, g, b);
    }

    private Object interpretFunctionHsv(Object hArg, Object sArg, Object vArg) {
        double h = tryInterpretDoubleValue(hArg);
        double s = tryInterpretDoubleValue(sArg) / 100.0;
        double v = tryInterpretDoubleValue(vArg) / 100.0;

        h = ((h % 360) + 360) % 360;
        s = Math.max(0, Math.min(1, s));
        v = Math.max(0, Math.min(1, v));

        double c = v * s;
        double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
        double m = v - c;

        double rp;
        double gp;
        double bp;
        if (h < 60) {
            rp = c; gp = x; bp = 0;
        } else if (h < 120) {
            rp = x; gp = c; bp = 0;
        } else if (h < 180) {
            rp = 0; gp = c; bp = x;
        } else if (h < 240) {
            rp = 0; gp = x; bp = c;
        } else if (h < 300) {
            rp = x; gp = 0; bp = c;
        } else {
            rp = c; gp = 0; bp = x;
        }

        int r = clampColorComponent((rp + m) * 255);
        int g = clampColorComponent((gp + m) * 255);
        int b = clampColorComponent((bp + m) * 255);
        return String.format(java.util.Locale.US, "#%02X%02X%02X", r, g, b);
    }

    private static int[] parseHexColor(String hex) {
        if (hex == null) return null;
        String value = hex.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() == 3) {
            StringBuilder expanded = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                char digit = value.charAt(i);
                expanded.append(digit).append(digit);
            }
            value = expanded.toString();
        }
        if (value.length() != 6) return null;
        try {
            int r = Integer.parseInt(value.substring(0, 2), 16);
            int g = Integer.parseInt(value.substring(2, 4), 16);
            int b = Integer.parseInt(value.substring(4, 6), 16);
            return new int[] {r, g, b};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Object interpretFunctionMixColor(Object color1Arg, Object color2Arg, Object tArg) {
        int[] c1 = parseHexColor(color1Arg == null ? null : String.valueOf(color1Arg));
        int[] c2 = parseHexColor(color2Arg == null ? null : String.valueOf(color2Arg));
        if (c1 == null || c2 == null) {
            return "#000000";
        }
        double t = tArg == null ? 0.5 : tryInterpretDoubleValue(tArg);
        t = Math.max(0, Math.min(1, t));

        int r = clampColorComponent(c1[0] + (c2[0] - c1[0]) * t);
        int g = clampColorComponent(c1[1] + (c2[1] - c1[1]) * t);
        int b = clampColorComponent(c1[2] + (c2[2] - c1[2]) * t);
        return String.format(java.util.Locale.US, "#%02X%02X%02X", r, g, b);
    }

    private double interpretFunctionRand(double from, double to) {
        double low = Math.min(from, to);
        double high = Math.max(from, to);

        if (low == high) {
            return low;
        }

        if (isInteger(low) && isInteger(high)
                && !isNumberWithDecimalPoint(leftChild) && !isNumberWithDecimalPoint(rightChild)) {
            return Math.floor(Math.random() * ((high + 1) - low)) + low;
        } else {
            return (Math.random() * (high - low)) + low;
        }
    }

    private static boolean isNumberWithDecimalPoint(FormulaElement element) {
        return element != null && element.type == ElementType.NUMBER && element.value.contains(".");
    }

    private Object interpretOperator(@NotNull Operators operator, Scope scope) {
        if (leftChild != null) {
            return interpretBinaryOperator(operator, scope);
        } else {
            return interpretUnaryOperator(operator, scope);
        }
    }

    private double interpretUnaryOperator(@NotNull Operators operator, Scope scope) {
        Object rightObject = tryInterpretElementRecursive(rightChild, scope);
        double right = tryInterpretDoubleValue(rightObject);

        switch (operator) {
            case MINUS:
                return -right;
            case LOGICAL_NOT:
                return booleanToDouble(right == FALSE);
            default:
                return FALSE;
        }
    }

    private Object interpretBinaryOperator(@NotNull Operators operator, Scope scope) {
        Object leftObject = tryInterpretElementRecursive(leftChild, scope);
        Object rightObject = tryInterpretElementRecursive(rightChild, scope);

        // Bug #3: string concatenation for '+'. A String operand means a non-numeric value,
        // so concatenate instead of producing a NaN that later throws InterpretationException.
        if (operator == Operators.PLUS
                && (leftObject instanceof String || rightObject instanceof String)) {
            return String.valueOf(leftObject) + String.valueOf(rightObject);
        }

        Double leftDouble = tryInterpretDoubleValue(leftObject);
        Double rightDouble = tryInterpretDoubleValue(rightObject);

        // Bug #3/#4: coerce an unparseable-string (NaN) operand to 0.0 so it can never become
        // a NaN result (which would abort the action) nor count as "true" in logical ops.
        double left = (leftDouble != null && !Double.isNaN(leftDouble)) ? leftDouble : 0.0;
        double right = (rightDouble != null && !Double.isNaN(rightDouble)) ? rightDouble : 0.0;

        switch (operator) {
            case PLUS:
                return left + right;
            case MINUS:
                return left - right;
            case MULT:
                return left * right;
            case DIVIDE:
                return (right == 0.0) ? Double.NaN : (left / right);
            case MOD:
                return (right == 0.0) ? Double.NaN : (left % right);
            case POW:
                return Math.pow(left, right);
            case EQUAL:
                return booleanToDouble(interpretOperatorEqual(leftObject, rightObject));
            case NOT_EQUAL:
                return booleanToDouble(!(interpretOperatorEqual(leftObject, rightObject)));
            case GREATER_THAN:
                return booleanToDouble(left > right);
            case GREATER_OR_EQUAL:
                return booleanToDouble(left >= right);
            case SMALLER_THAN:
                return booleanToDouble(left < right);
            case SMALLER_OR_EQUAL:
                return booleanToDouble(left <= right);
            case LOGICAL_AND:
                // Bug #4: a NaN operand is coerced to 0.0 above, so it is treated as false here.
                return booleanToDouble(left != FALSE && right != FALSE);
            case LOGICAL_OR:
                return booleanToDouble(left != FALSE || right != FALSE);
            default:
                return FALSE;
        }
    }

    public FormulaElement getParent() {
        return parent;
    }

    public void setRightChild(FormulaElement rightChild) {
        this.rightChild = rightChild;
        if (this.rightChild != null) {
            this.rightChild.parent = this;
        }
    }

    public void setLeftChild(FormulaElement leftChild) {
        this.leftChild = leftChild;
        if (this.leftChild != null) {
            this.leftChild.parent = this;
        }
    }

    public void addAdditionalChild(FormulaElement child) {
        additionalChildren.add(child);
        if (child != null) {
            child.parent = this;
        }
    }

    public void replaceElement(FormulaElement current) {
        parent = current.parent;
        leftChild = current.leftChild;
        rightChild = current.rightChild;
        for (int index = 0; index < current.additionalChildren.size(); index++) {
            if (index < additionalChildren.size()) {
                additionalChildren.set(index, current.additionalChildren.get(index));
            } else {
                additionalChildren.add(current.additionalChildren.get(index));
            }
        }
        value = current.value;
        type = current.type;

        if (leftChild != null) {
            leftChild.parent = this;
        }
        if (rightChild != null) {
            rightChild.parent = this;
        }
        for (FormulaElement child : additionalChildren) {
            if (child != null) {
                child.parent = this;
            }
        }
    }

    public void replaceElement(ElementType type, String value) {
        this.value = value;
        this.type = type;
    }

    public void replaceWithSubElement(String operator, FormulaElement rightChild) {
        FormulaElement cloneThis = new FormulaElement(ElementType.OPERATOR, operator, this.getParent(), this,
                rightChild);

        cloneThis.parent.rightChild = cloneThis;
    }

    public boolean isBoolean(Scope scope) {
        if (type == ElementType.USER_VARIABLE) {
            return isUserVariableBoolean(scope);
        } else if (type == ElementType.USER_LIST) {
            return isUserListBoolean(scope);
        } else if (type == ElementType.USER_DEFINED_BRICK_INPUT) {
            return isUserDefinedBrickInputBoolean(scope);
        } else {
            return isOtherBooleanFormulaElement();
        }
    }

    private boolean isUserVariableBoolean(Scope scope) {
        UserVariable userVariable = UserDataWrapper.getUserVariable(value, scope);
        return userVariable != null && userVariable.getValue() instanceof Boolean;
    }

    private boolean isUserListBoolean(Scope scope) {
        UserList ul = UserDataWrapper.getUserList(value, scope);
        if (ul == null) return false;
        List<Object> listValues = ul.getValue();
        if (listValues.size() != 1) {
            return false;
        }
        return listValues.get(0) instanceof Boolean;
    }

    private boolean isUserDefinedBrickInputBoolean(Scope scope) {
        UserData userData = UserDataWrapper.getUserDefinedBrickInput(value, scope.getSequence());
        if (userData != null && userData.getValue() instanceof Formula) {
            return ((Formula) userData.getValue()).getRoot().isBoolean(scope);
        } else {
            return false;
        }
    }

    private boolean isOtherBooleanFormulaElement() {
        return (type == ElementType.FUNCTION
                && Functions.isBoolean(Functions.getFunctionByValue(value)))
                || (type == ElementType.SENSOR
                && Sensors.isBoolean(Sensors.getSensorByValue(value)))
                || (type == ElementType.OPERATOR
                && Operators.getOperatorByValue(value).isLogicalOperator)
                || type == ElementType.COLLISION_FORMULA;
    }

    public boolean containsElement(ElementType elementType) {
        if (type.equals(elementType)
                || (leftChild != null && leftChild.containsElement(elementType))
                || (rightChild != null && rightChild.containsElement(elementType))) {
            return true;
        }
        for (FormulaElement child : additionalChildren) {
            if (child != null && child.containsElement(elementType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isNumber() {
        if (type == ElementType.OPERATOR) {
            Operators operator = Operators.getOperatorByValue(value);
            return (operator == Operators.MINUS) && (leftChild == null) && rightChild.isNumber();
        }
        return type == ElementType.NUMBER;
    }

    @Override
    public FormulaElement clone() {
        FormulaElement leftChildClone = tryCloneElement(leftChild);
        FormulaElement rightChildClone = tryCloneElement(rightChild);
        List<FormulaElement> additionalChildrenClones = new ArrayList<>();
        for (FormulaElement child : additionalChildren) {
            additionalChildrenClones.add(tryCloneElement(child));
        }
        String valueClone = value == null ? "" : value;
        return new FormulaElement(type, valueClone, null, leftChildClone, rightChildClone,
                additionalChildrenClones);
    }

    private FormulaElement tryCloneElement(FormulaElement element) {
        return element == null ? null : element.clone();
    }

    public void addRequiredResources(final Set<Integer> requiredResourcesSet) {
        tryAddRequiredResources(requiredResourcesSet, leftChild);
        tryAddRequiredResources(requiredResourcesSet, rightChild);

        for (FormulaElement child : additionalChildren) {
            tryAddRequiredResources(requiredResourcesSet, child);
        }

        switch (type) {
            case FUNCTION:
                addFunctionResources(requiredResourcesSet, Functions.getFunctionByValue(value));
                break;
            case SENSOR:
                addSensorsResources(requiredResourcesSet, Sensors.getSensorByValue(value));
                break;
            case COLLISION_FORMULA:
                requiredResourcesSet.add(Brick.COLLISION);
                break;
            default:
        }
    }

    private void tryAddRequiredResources(Set<Integer> resourceSet, FormulaElement element) {
        if (element != null) {
            element.addRequiredResources(resourceSet);
        }
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<String> getUserDataRecursive(ElementType type) {
        ArrayList<String> userDataNames = new ArrayList<>();

        if (this.type == type) {
            userDataNames.add(this.value);
        }

        if (this.leftChild != null) {
            userDataNames.addAll(leftChild.getUserDataRecursive(type));
        }

        if (this.rightChild != null) {
            userDataNames.addAll(rightChild.getUserDataRecursive(type));
        }

        for (FormulaElement child : additionalChildren) {
            if (child != null) {
                userDataNames.addAll(child.getUserDataRecursive(type));
            }
        }

        return userDataNames;
    }

    public String getFileListString(File directory) {
        if (directory == null || !directory.isDirectory()) return "";

        File[] files = directory.listFiles();
        if (files == null) return "";

        StringBuilder builder = new StringBuilder();
        for (File file : files) {
            builder.append(file.getName()).append("\n");
        }
        return builder.toString();
    }

    static String readLineFromFile(File filesDir, String lineStr, String fileName) {
        if (filesDir == null) return "";
        File file = new File(filesDir, fileName);
        if (!file.exists() || !file.isFile() || !file.canRead()) return "";
        try {
            int lineNo = Integer.parseInt(lineStr.trim());
            if (lineNo < 1) return "";
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            if (lineNo > lines.size()) return "";
            return lines.get(lineNo - 1);
        } catch (Exception e) {
            return "";
        }
    }

    private org.catrobat.catroid.raptor.ThreeDManager getThreeDManager() {
        org.catrobat.catroid.stage.StageListener listener = org.catrobat.catroid.stage.StageActivity.getActiveStageListener();
        if (listener != null) {
            return listener.getThreeDManager();
        }
        return null;
    }

    public long getFileSize(File file) {
        if (file == null) {
            return 0;
        }

        if (file.exists() && file.isFile()) {
            return file.length();
        }

        return 0;
    }
}
