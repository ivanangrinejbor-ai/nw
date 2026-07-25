package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.NewOption;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.ui.UiUtils;
import org.catrobat.catroid.ui.recyclerview.dialog.dialoginterface.NewItemInterface;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class CrossFadeSoundBrick extends FormulaBrick implements
        BrickSpinner.OnItemSelectedListener<SoundInfo>, NewItemInterface<SoundInfo> {

    private static final long serialVersionUID = 1L;
    private SoundInfo soundFrom;
    private SoundInfo soundTo;
    private transient BrickSpinner<SoundInfo> spinnerFrom;
    private transient BrickSpinner<SoundInfo> spinnerTo;

    public CrossFadeSoundBrick() {
        addAllowedBrickField(BrickField.DURATION, R.id.brick_cross_fade_duration_edit);
    }

    public CrossFadeSoundBrick(double duration) {
        this(new Formula(duration));
    }

    public CrossFadeSoundBrick(Formula duration) {
        this();
        setFormulaWithBrickField(BrickField.DURATION, duration);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_cross_fade_sound;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);
        List<Nameable> items = new ArrayList<>();
        items.add(new NewOption(context.getString(R.string.new_option)));
        items.addAll(ProjectManager.getInstance().getCurrentSprite().getSoundList());

        spinnerFrom = new BrickSpinner<>(R.id.brick_cross_fade_from_spinner, view, new ArrayList<>(items));
        spinnerFrom.setOnItemSelectedListener(this);
        spinnerFrom.setSelection(soundFrom);

        spinnerTo = new BrickSpinner<>(R.id.brick_cross_fade_to_spinner, view, new ArrayList<>(items));
        spinnerTo.setOnItemSelectedListener(this);
        spinnerTo.setSelection(soundTo);

        return view;
    }

    @Override
    public void onNewOptionSelected(Integer spinnerId) {
        AppCompatActivity activity = UiUtils.getActivityFromView(view);
        if (activity instanceof SpriteActivity) {
            ((SpriteActivity) activity).registerOnNewSoundListener(this);
            ((SpriteActivity) activity).handleAddSoundButton();
        }
    }

    @Override
    public void addItem(SoundInfo item) {
        if (spinnerFrom != null) { spinnerFrom.add(item); spinnerFrom.setSelection(item); }
        if (spinnerTo != null) { spinnerTo.add(item); }
    }

    @Override public void onEditOptionSelected(Integer spinnerId) {}
    @Override public void onStringOptionSelected(Integer spinnerId, String string) {}

    @Override
    public void onItemSelected(Integer spinnerId, @Nullable SoundInfo item) {
        if (spinnerId == R.id.brick_cross_fade_from_spinner) {
            soundFrom = item;
        } else if (spinnerId == R.id.brick_cross_fade_to_spinner) {
            soundTo = item;
        }
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createCrossFadeSoundAction(
                sprite, sequence, soundFrom, soundTo,
                getFormulaWithBrickField(BrickField.DURATION)));
    }
}
