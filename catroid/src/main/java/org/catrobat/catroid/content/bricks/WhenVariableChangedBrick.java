package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenVariableChangedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.NewOption;
import org.catrobat.catroid.formulaeditor.UserVariable;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.utils.LoopUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

public class WhenVariableChangedBrick extends BrickBaseType implements CompositeBrick, ScriptBrick {

    private static final long serialVersionUID = 1L;
    private transient EndBrick endBrick = new EndBrick(this);

    private WhenVariableChangedScript script;
    private List<Brick> loopBricks = new ArrayList<>();
    private transient BrickSpinner<UserVariable> varSpinner;

    public WhenVariableChangedBrick() {
        this(new WhenVariableChangedScript());
    }

    public WhenVariableChangedBrick(WhenVariableChangedScript script) {
        script.setScriptBrick(this);
        commentedOut = script.isCommentedOut();
        this.script = script;
    }

    private Object readResolve() {
        if (endBrick == null) {
            endBrick = new EndBrick(this);
        }
        if (loopBricks == null) {
            loopBricks = new ArrayList<>();
        }
        return this;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        WhenVariableChangedBrick clone = (WhenVariableChangedBrick) super.clone();
        clone.endBrick = new EndBrick(clone);
        clone.script = (WhenVariableChangedScript) script.clone();
        clone.script.setScriptBrick(clone);
        clone.loopBricks = new ArrayList<>();
        for (Brick brick : loopBricks) {
            clone.addBrick(brick.clone());
        }
        return clone;
    }

    @Override
    public boolean hasSecondaryList() {
        return false;
    }

    @Override
    public List<Brick> getNestedBricks() {
        return loopBricks;
    }

    @Override
    public List<Brick> getSecondaryNestedBricks() {
        return null;
    }

    public boolean addBrick(Brick brick) {
        return loopBricks.add(brick);
    }

    @Override
    public void setCommentedOut(boolean commentedOut) {
        super.setCommentedOut(commentedOut);
        for (Brick brick : loopBricks) {
            brick.setCommentedOut(commentedOut);
        }
    }

    @Override
    public boolean consistsOfMultipleParts() {
        return true;
    }

    @Override
    public List<Brick> getAllParts() {
        List<Brick> bricks = new ArrayList<>();
        bricks.add(this);
        bricks.add(endBrick);
        return bricks;
    }

    @Override
    public void addToFlatList(List<Brick> bricks) {
        super.addToFlatList(bricks);
        for (Brick brick : loopBricks) {
            brick.addToFlatList(bricks);
        }
        bricks.add(endBrick);
    }

    @Override
    public void setParent(Brick parent) {
        super.setParent(parent);
        for (Brick brick : loopBricks) {
            brick.setParent(this);
        }
    }

    @Override
    public List<Brick> getDragAndDropTargetList() {
        return loopBricks;
    }

    @Override
    public boolean removeChild(Brick brick) {
        if (loopBricks.remove(brick)) {
            return true;
        }
        for (Brick childBrick : loopBricks) {
            if (childBrick.removeChild(brick)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_when_variable_changed;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);
        Sprite sprite = ProjectManager.getInstance().getCurrentSprite();

        List<Nameable> varItems = new ArrayList<>();
        varItems.add(new NewOption(context.getString(R.string.new_option)));
        if (sprite != null) {
            varItems.addAll(sprite.getUserVariables());
        }
        varItems.addAll(ProjectManager.getInstance().getCurrentProject().getUserVariables());

        varSpinner = new BrickSpinner<>(R.id.when_var_changed_spinner, view, varItems);
        varSpinner.setSelection(script.getMonitoredVariable());
        varSpinner.setOnItemSelectedListener(new BrickSpinner.OnItemSelectedListener<UserVariable>() {
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
            public void onItemSelected(Integer spinnerId, @Nullable UserVariable item) {
                script.setMonitoredVariable(item);
            }
        });

        return view;
    }

    @Override
    public Script getScript() {
        return script;
    }

    @Override
    public int getPositionInScript() {
        return -1;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        ScriptSequenceAction repeatSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
        boolean isLoopDelay = LoopUtil.checkLoopBrickForLoopDelay(this, sequence.getScript());

        Action action = sprite.getActionFactory().createWhenVariableChangedAction(sprite, sequence, repeatSequence, script.getMonitoredVariable(), isLoopDelay);

        sprite.getActionFactory().pushLoopControl((org.catrobat.catroid.content.actions.LoopControl) action);
        for (Brick brick : loopBricks) {
            if (!brick.isCommentedOut()) {
                brick.addActionToSequence(sprite, repeatSequence);
            }
        }
        sprite.getActionFactory().popLoopControl();

        sequence.addAction(action);
    }

    @Override
    public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
        super.addRequiredResources(requiredResourcesSet);
        for (Brick brick : loopBricks) {
            brick.addRequiredResources(requiredResourcesSet);
        }
    }
}
