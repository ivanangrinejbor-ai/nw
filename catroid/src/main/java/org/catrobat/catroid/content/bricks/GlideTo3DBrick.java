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

public class GlideTo3DBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    private int typeSelectionIndex = 0;

    public GlideTo3DBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_glide_3d_object_id);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_glide_3d_x);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_glide_3d_y);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_glide_3d_z);
        addAllowedBrickField(BrickField.DURATION_IN_SECONDS, R.id.brick_glide_3d_duration);
    }

    public GlideTo3DBrick(String id, double x, double y, double z, double durationInSeconds) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, new Formula(id));
        setFormulaWithBrickField(BrickField.VALUE_2, new Formula(x));
        setFormulaWithBrickField(BrickField.VALUE_3, new Formula(y));
        setFormulaWithBrickField(BrickField.VALUE_4, new Formula(z));
        setFormulaWithBrickField(BrickField.DURATION_IN_SECONDS, new Formula(durationInSeconds));
    }

    public GlideTo3DBrick(Formula id, Formula x, Formula y, Formula z, Formula durationInSeconds) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, id);
        setFormulaWithBrickField(BrickField.VALUE_2, x);
        setFormulaWithBrickField(BrickField.VALUE_3, y);
        setFormulaWithBrickField(BrickField.VALUE_4, z);
        setFormulaWithBrickField(BrickField.DURATION_IN_SECONDS, durationInSeconds);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_glide_to_3d;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner typeSpinner = view.findViewById(R.id.brick_glide_3d_type_spinner);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                context,
                R.array.brick_easing_types,
                android.R.layout.simple_spinner_item
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        typeSpinner.setSelection(typeSelectionIndex);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                typeSelectionIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createGlideTo3DAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.VALUE_1),
                getFormulaWithBrickField(BrickField.VALUE_2),
                getFormulaWithBrickField(BrickField.VALUE_3),
                getFormulaWithBrickField(BrickField.VALUE_4),
                getFormulaWithBrickField(BrickField.DURATION_IN_SECONDS),
                typeSelectionIndex));
    }
}
