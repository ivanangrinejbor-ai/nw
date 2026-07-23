package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.ui.BrickLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserDefinedBrickV2 extends FormulaBrick {
    private static final long serialVersionUID = 2L;

    private String blockName = "MyBlockV2";
    private List<String> parameterNames = new ArrayList<>();
    private UUID userDefinedBrickID;
    private boolean isCallingBrick = false;

    public transient BiMap<FormulaField, TextView> formulaFieldToTextViewMap = HashBiMap.create(5);

    public UserDefinedBrickV2() {
        this("MyBlockV2", new ArrayList<>());
    }

    public UserDefinedBrickV2(String blockName, List<String> parameterNames) {
        this.blockName = blockName;
        this.parameterNames = parameterNames != null ? parameterNames : new ArrayList<>();
        this.userDefinedBrickID = UUID.randomUUID();
        initFormulaFields();
    }

    public UserDefinedBrickV2(UserDefinedBrickV2 other) {
        this.blockName = other.blockName;
        this.parameterNames = new ArrayList<>(other.parameterNames);
        this.userDefinedBrickID = other.userDefinedBrickID;
        this.isCallingBrick = other.isCallingBrick;
        initFormulaFields();
        for (Map.Entry<FormulaField, Formula> entry : other.formulaMap.entrySet()) {
            this.formulaMap.put(entry.getKey(), entry.getValue() != null ? new Formula(entry.getValue().getFormulaTree()) : new Formula(0));
        }
    }

    private void initFormulaFields() {
        for (int i = 0; i < parameterNames.size(); i++) {
            FormulaField field = getParamFormulaField(i);
            if (!formulaMap.containsKey(field)) {
                formulaMap.put(field, new Formula(0));
            }
        }
    }

    public FormulaField getParamFormulaField(int index) {
        switch (index) {
            case 0: return BrickField.VALUE_1;
            case 1: return BrickField.VALUE_2;
            case 2: return BrickField.VALUE_3;
            case 3: return BrickField.VALUE_4;
            case 4: return BrickField.VALUE_5;
            default: return BrickField.VALUE_1;
        }
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public UUID getUserDefinedBrickID() {
        return userDefinedBrickID;
    }

    public void setCallingBrick(boolean callingBrick) {
        isCallingBrick = callingBrick;
    }

    public boolean isCallingBrick() {
        return isCallingBrick;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        return new UserDefinedBrickV2(this);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_user_brick;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);
        BrickLayout layout = view.findViewById(R.id.brick_user_brick);
        int spacing = context.getResources().getDimensionPixelOffset(R.dimen.material_design_spacing_small);

        // Block Title
        TextView titleView = new TextView(new ContextThemeWrapper(context, R.style.BrickText));
        titleView.setText("✦ " + blockName);
        layout.addView(titleView);
        ((BrickLayout.LayoutParams) titleView.getLayoutParams()).setHorizontalSpacing(spacing);

        if (formulaFieldToTextViewMap == null) {
            formulaFieldToTextViewMap = HashBiMap.create(5);
        }
        formulaFieldToTextViewMap.clear();

        // Parameters
        for (int i = 0; i < parameterNames.size(); i++) {
            String paramName = parameterNames.get(i);

            TextView paramLabel = new TextView(new ContextThemeWrapper(context, R.style.BrickText));
            paramLabel.setText(paramName + ":");
            layout.addView(paramLabel);
            ((BrickLayout.LayoutParams) paramLabel.getLayoutParams()).setHorizontalSpacing(spacing);

            TextView editView = new TextView(context, null, 0, R.style.BrickEditText);
            FormulaField field = getParamFormulaField(i);
            addAllowedBrickField(field, editView);
            Formula f = getFormulaWithBrickField(field);
            editView.setText(f != null ? f.getTrimmedFormulaString(context) : "0");
            layout.addView(editView);
            ((BrickLayout.LayoutParams) editView.getLayoutParams()).setEditText(true);
            ((BrickLayout.LayoutParams) editView.getLayoutParams()).setHorizontalSpacing(spacing);
        }

        return view;
    }

    protected void addAllowedBrickField(FormulaField formulaField, TextView textView) {
        formulaMap.putIfAbsent(formulaField, new Formula(0));
        formulaFieldToTextViewMap.put(formulaField, textView);
    }

    @Override
    public TextView getTextView(FormulaField formulaField) {
        if (formulaFieldToTextViewMap != null && formulaFieldToTextViewMap.containsKey(formulaField)) {
            return formulaFieldToTextViewMap.get(formulaField);
        }
        return super.getTextView(formulaField);
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        Map<String, Formula> paramFormulas = new HashMap<>();
        for (int i = 0; i < parameterNames.size(); i++) {
            FormulaField field = getParamFormulaField(i);
            Formula f = getFormulaWithBrickField(field);
            paramFormulas.put(parameterNames.get(i), f);
            paramFormulas.put("$" + (i + 1), f);
        }
        sequence.addAction(sprite.getActionFactory().createUserBrickV2Action(sprite, sequence, userDefinedBrickID, blockName, paramFormulas));
    }
}
