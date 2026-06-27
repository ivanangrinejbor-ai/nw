package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.NewOption;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class MoveToObjectBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<Sprite> {

    private static final long serialVersionUID = 1L;

    private Sprite targetObject;
    private transient BrickSpinner<Sprite> spinner;

    public MoveToObjectBrick() {
        addAllowedBrickField(BrickField.SPEED, R.id.brick_move_to_object_speed_edit);
    }

    public MoveToObjectBrick(Sprite target, Formula speed) {
        this();
        this.targetObject = target;
        setFormulaWithBrickField(BrickField.SPEED, speed);
    }

    public MoveToObjectBrick(String targetName, String speed) {
        this(null, new Formula(speed));
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        MoveToObjectBrick clone = (MoveToObjectBrick) super.clone();
        clone.spinner = null;
        return clone;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_move_to_object;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        List<Nameable> items = new ArrayList<>();
        items.add(new NewOption(context.getString(R.string.new_option)));
        items.addAll(ProjectManager.getInstance().getCurrentlyEditedScene().getSpriteList());
        items.remove(ProjectManager.getInstance().getCurrentSprite());

        spinner = new BrickSpinner<>(R.id.brick_move_to_object_spinner, view, items);
        spinner.setOnItemSelectedListener(this);
        spinner.setSelection(targetObject);

        return view;
    }

    @Override
    public void onNewOptionSelected(Integer spinnerId) {
    }

    @Override
    public void onEditOptionSelected(Integer spinnerId) {
    }

    @Override
    public void onStringOptionSelected(Integer spinnerId, String string) {
    }

    @Override
    public void onItemSelected(Integer spinnerId, @Nullable Sprite item) {
        targetObject = item;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        String targetName = (targetObject != null) ? targetObject.getName() : "";
        sequence.addAction(sprite.getActionFactory()
                .createMoveToObjectAction(sprite, sequence,
                        new Formula(targetName),
                        getFormulaWithBrickField(BrickField.SPEED)));
    }
}
