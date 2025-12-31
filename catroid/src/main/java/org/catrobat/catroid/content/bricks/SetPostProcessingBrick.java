package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetPostProcessingBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    private int effectIndex = 0;
    private int paramIndex = 0;

    public SetPostProcessingBrick() {
        addAllowedBrickField(BrickField.VALUE, R.id.brick_pp_value_edit);
    }

    public SetPostProcessingBrick(int effectIdx, int paramIdx, Formula value) {
        this();
        this.effectIndex = effectIdx;
        this.paramIndex = paramIdx;
        setFormulaWithBrickField(BrickField.VALUE, value);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_pp_effect;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner effectSpinner = view.findViewById(R.id.brick_pp_effect_spinner);
        Spinner paramSpinner = view.findViewById(R.id.brick_pp_param_spinner);

        ArrayAdapter<CharSequence> effectAdapter = ArrayAdapter.createFromResource(context, R.array.pp_effect_types, android.R.layout.simple_spinner_item);
        effectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        effectSpinner.setAdapter(effectAdapter);

        ArrayAdapter<CharSequence> paramAdapter = ArrayAdapter.createFromResource(context, R.array.pp_effect_params, android.R.layout.simple_spinner_item);
        paramAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paramSpinner.setAdapter(paramAdapter);

        effectSpinner.setSelection(effectIndex);
        paramSpinner.setSelection(paramIndex);

        effectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                effectIndex = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        paramSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                paramIndex = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSetPostProcessingAction(sprite, sequence,
                        effectIndex,
                        paramIndex,
                        getFormulaWithBrickField(BrickField.VALUE)
                ));
    }
}