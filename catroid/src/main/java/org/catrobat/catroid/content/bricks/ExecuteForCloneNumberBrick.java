package org.catrobat.catroid.content.bricks;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ExecuteForCloneNumberAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

public class ExecuteForCloneNumberBrick extends FormulaBrick implements CompositeBrick {
    private static final long serialVersionUID = 1L;

    private transient EndBrick endBrick = new EndBrick(this);

    protected List<Brick> cloneBranchBricks = new ArrayList<>();

    public ExecuteForCloneNumberBrick() {
        addAllowedBrickField(BrickField.NUMBER, R.id.brick_execute_for_clone_number_edit);
    }

    public ExecuteForCloneNumberBrick(int cloneNumber) {
        this(new Formula(cloneNumber));
    }

    public ExecuteForCloneNumberBrick(Formula cloneNumber) {
        this();
        setFormulaWithBrickField(BrickField.NUMBER, cloneNumber);
    }

    private Object readResolve() {
        if (endBrick == null) {
            endBrick = new EndBrick(this);
        }
        if (cloneBranchBricks == null) {
            cloneBranchBricks = new ArrayList<>();
        }
        return this;
    }

    public void addBrickToCloneBranch(Brick brick) {
        cloneBranchBricks.add(brick);
    }

    public List<Brick> getCloneBranchBricks() {
        return cloneBranchBricks;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_execute_for_clone_number;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        ActionFactory factory = sprite.getActionFactory();

        ScriptSequenceAction cloneSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
        for (Brick brick : cloneBranchBricks) {
            if (!brick.isCommentedOut()) {
                brick.addActionToSequence(sprite, cloneSequence);
            }
        }

        Action action = factory.createExecuteForCloneNumberAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NUMBER));
        ((ExecuteForCloneNumberAction) action).setCloneAction(cloneSequence);
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
        return cloneBranchBricks;
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
        for (Brick brick : cloneBranchBricks) {
            brick.addToFlatList(bricks);
        }
        bricks.add(endBrick);
    }

    @Override
    public void setParent(Brick parentBrick) {
        super.setParent(parentBrick);
        for (Brick brick : cloneBranchBricks) {
            brick.setParent(this);
        }
    }

    @Override
    public boolean removeChild(Brick brick) {
        if (cloneBranchBricks.remove(brick)) {
            return true;
        }
        for (Brick child : cloneBranchBricks) {
            if (child instanceof CompositeBrick && ((CompositeBrick) child).removeChild(brick)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Brick> getDragAndDropTargetList() {
        return cloneBranchBricks;
    }

    @Override
    public void setCommentedOut(boolean commentedOut) {
        super.setCommentedOut(commentedOut);
        for (Brick brick : cloneBranchBricks) {
            brick.setCommentedOut(commentedOut);
        }
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        ExecuteForCloneNumberBrick clone = (ExecuteForCloneNumberBrick) super.clone();
        clone.cloneBranchBricks = new ArrayList<>();
        for (Brick brick : cloneBranchBricks) {
            Brick clonedBrick = brick.clone();
            clonedBrick.setParent(clone);
            clone.cloneBranchBricks.add(clonedBrick);
        }
        clone.endBrick = new EndBrick(clone);
        return clone;
    }

    @Override
    public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
        super.addRequiredResources(requiredResourcesSet);
        for (Brick brick : cloneBranchBricks) {
            brick.addRequiredResources(requiredResourcesSet);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ExecuteForCloneNumberBrick)) return false;
        ExecuteForCloneNumberBrick other = (ExecuteForCloneNumberBrick) obj;
        return cloneBranchBricks.equals(other.cloneBranchBricks);
    }

    @Override
    public int hashCode() {
        return cloneBranchBricks.hashCode();
    }
}
