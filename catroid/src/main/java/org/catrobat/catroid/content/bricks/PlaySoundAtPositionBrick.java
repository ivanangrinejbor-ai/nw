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

public class PlaySoundAtPositionBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int loopSelection = 0; // 0 = No, 1 = Yes

    public PlaySoundAtPositionBrick() {
        addAllowedBrickField(BrickField.SOUND_NAME, R.id.brick_play_sound_3d_name_edit);
        addAllowedBrickField(BrickField.INSTANCE_NAME, R.id.brick_play_sound_3d_instance_edit);
        addAllowedBrickField(BrickField.POS_X, R.id.brick_play_sound_3d_x_edit);
        addAllowedBrickField(BrickField.POS_Y, R.id.brick_play_sound_3d_y_edit);
        addAllowedBrickField(BrickField.POS_Z, R.id.brick_play_sound_3d_z_edit);
        addAllowedBrickField(BrickField.VOLUME, R.id.brick_play_sound_3d_volume_edit);
        addAllowedBrickField(BrickField.PITCH, R.id.brick_play_sound_3d_pitch_edit);
    }

    public PlaySoundAtPositionBrick(String soundName, String instanceName) {
        this();
        setFormulaWithBrickField(BrickField.SOUND_NAME, new Formula(soundName));
        setFormulaWithBrickField(BrickField.INSTANCE_NAME, new Formula(instanceName));
        setFormulaWithBrickField(BrickField.POS_X, new Formula(0));
        setFormulaWithBrickField(BrickField.POS_Y, new Formula(10));
        setFormulaWithBrickField(BrickField.POS_Z, new Formula(-10));
        setFormulaWithBrickField(BrickField.VOLUME, new Formula(100));
        setFormulaWithBrickField(BrickField.PITCH, new Formula(100));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_play_sound_at_pos;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);
        Spinner loopSpinner = view.findViewById(R.id.brick_play_sound_3d_loop_spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.simple_spinner_item_white_text, new String[]{"No", "Yes"});
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        loopSpinner.setAdapter(adapter);
        loopSpinner.setSelection(loopSelection);
        loopSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loopSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createPlaySoundAtPositionAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.SOUND_NAME),
                getFormulaWithBrickField(BrickField.INSTANCE_NAME),
                getFormulaWithBrickField(BrickField.POS_X),
                getFormulaWithBrickField(BrickField.POS_Y),
                getFormulaWithBrickField(BrickField.POS_Z),
                getFormulaWithBrickField(BrickField.VOLUME),
                getFormulaWithBrickField(BrickField.PITCH),
                loopSelection == 1
        ));
    }
}