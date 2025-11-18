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

public class SetActiveBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    // 0 = true, 1 = false.
    private int activeStateSelection = 0;

    public SetActiveBrick() {
        addAllowedBrickField(BrickField.OBJECT_NAME, R.id.brick_set_active_object_name);
    }

    public SetActiveBrick(String objectName, boolean isActive) {
        this();
        setFormulaWithBrickField(BrickField.OBJECT_NAME, new Formula(objectName));
        this.activeStateSelection = isActive ? 0 : 1;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_active;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner stateSpinner = view.findViewById(R.id.brick_set_active_state_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.brick_set_active_states, R.layout.simple_spinner_item_white_text); // Используем стиль для белого текста
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        stateSpinner.setAdapter(adapter);

        stateSpinner.setSelection(activeStateSelection);

        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                activeStateSelection = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        boolean isActive = (activeStateSelection == 0);

        sequence.addAction(sprite.getActionFactory().createSetActiveAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.OBJECT_NAME),
                isActive
        ));
    }
}