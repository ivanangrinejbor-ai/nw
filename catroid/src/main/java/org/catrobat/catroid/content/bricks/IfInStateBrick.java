package org.catrobat.catroid.content.bricks;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.IfInStateAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

public class IfInStateBrick extends FormulaBrick implements CompositeBrick {
    private static final long serialVersionUID = 1L;

    private transient EndBrick endBrick = new EndBrick(this);

    protected List<Brick> stateBranchBricks = new ArrayList<>();

    public IfInStateBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_if_in_state_machine_edit);
        addAllowedBrickField(BrickField.STRING, R.id.brick_if_in_state_value_edit);
    }

    public IfInStateBrick(String machine, String state) {
        this(new Formula(machine), new Formula(state));
    }

    public IfInStateBrick(Formula machine, Formula state) {
        this();
        setFormulaWithBrickField(BrickField.NAME, machine);
        setFormulaWithBrickField(BrickField.STRING, state);
    }

    public void addBrickToStateBranch(Brick brick) {
        stateBranchBricks.add(brick);
    }

    public List<Brick> getStateBranchBricks() {
        return stateBranchBricks;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_if_in_state;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        ActionFactory factory = sprite.getActionFactory();

        ScriptSequenceAction stateSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
        for (Brick brick : stateBranchBricks) {
            brick.addActionToSequence(sprite, stateSequence);
        }

        Action action = factory.createIfInStateAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.STRING));
        ((IfInStateAction) action).setStateAction(stateSequence);
        sequence.addAction(action);
    }

    @Override
    public boolean consistsOfMultipleParts() {
        return true;
    }

    @Override
    public boolean hasSecondaryList() {
        return false;
    }

    @Override
    public List<Brick> getNestedBricks() {
        return stateBranchBricks;
    }

    @Override
    public List<Brick> getSecondaryNestedBricks() {
        return null;
    }

    @Override
    public List<Brick> getAllParts() {
        List<Brick> parts = new ArrayList<>();
        parts.add(this);
        parts.add(endBrick);
        return parts;
    }

    @Override
    public void addToFlatList(List<Brick> bricks) {
        bricks.add(this);
        for (Brick brick : stateBranchBricks) {
            brick.addToFlatList(bricks);
        }
        bricks.add(endBrick);
    }

    @Override
    public void setParent(Brick parentBrick) {
        super.setParent(parentBrick);
        for (Brick brick : stateBranchBricks) {
            brick.setParent(this);
        }
    }

    @Override
    public boolean removeChild(Brick brick) {
        if (stateBranchBricks.remove(brick)) {
            return true;
        }
        for (Brick child : stateBranchBricks) {
            if (child instanceof CompositeBrick && ((CompositeBrick) child).removeChild(brick)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Brick> getDragAndDropTargetList() {
        return stateBranchBricks;
    }

    @Override
    public void setCommentedOut(boolean commentedOut) {
        super.setCommentedOut(commentedOut);
        for (Brick brick : stateBranchBricks) {
            brick.setCommentedOut(commentedOut);
        }
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        IfInStateBrick clone = (IfInStateBrick) super.clone();
        clone.stateBranchBricks = new ArrayList<>();
        for (Brick brick : stateBranchBricks) {
            Brick clonedBrick = brick.clone();
            clonedBrick.setParent(clone);
            clone.stateBranchBricks.add(clonedBrick);
        }
        clone.endBrick = new EndBrick(clone);
        return clone;
    }

    @Override
    public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
        super.addRequiredResources(requiredResourcesSet);
        for (Brick brick : stateBranchBricks) {
            brick.addRequiredResources(requiredResourcesSet);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof IfInStateBrick)) return false;
        IfInStateBrick other = (IfInStateBrick) obj;
        return stateBranchBricks.equals(other.stateBranchBricks);
    }

    @Override
    public int hashCode() {
        return stateBranchBricks.hashCode();
    }
}
