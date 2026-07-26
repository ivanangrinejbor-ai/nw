package org.catrobat.catroid.content.bricks;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.IfCustomParamEqualsAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

public class IfCustomParamEqualsBrick extends FormulaBrick implements CompositeBrick {
    private static final long serialVersionUID = 2L;

    private transient EndBrick endBrick = new EndBrick(this);
    protected List<Brick> ifBranchBricks = new ArrayList<>();

    // XStream не вызывает конструкторы — transient endBrick будет null после десериализации.
    private Object readResolve() {
        if (endBrick == null) {
            endBrick = new EndBrick(this);
        }
        if (ifBranchBricks == null) {
            ifBranchBricks = new ArrayList<>();
        }
        return this;
    }

    public IfCustomParamEqualsBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_if_param_name);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_if_param_val);
    }

    public IfCustomParamEqualsBrick(String paramName, String expectedValue) {
        this(new Formula(paramName), new Formula(expectedValue));
    }

    public IfCustomParamEqualsBrick(Formula paramName, Formula expectedValue) {
        this();
        setFormulaWithBrickField(BrickField.NAME, paramName);
        setFormulaWithBrickField(BrickField.VALUE_1, expectedValue);
    }

    public void addBrickToIfBranch(Brick brick) {
        ifBranchBricks.add(brick);
    }

    public List<Brick> getIfBranchBricks() {
        return ifBranchBricks;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_if_custom_param_equals;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        ActionFactory factory = sprite.getActionFactory();

        ScriptSequenceAction ifSequence = (ScriptSequenceAction) ActionFactory.createScriptSequenceAction(sequence.getScript());
        for (Brick brick : ifBranchBricks) {
            brick.addActionToSequence(sprite, ifSequence);
        }

        Action action = factory.createIfCustomParamEqualsAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.VALUE_1));
        ((IfCustomParamEqualsAction) action).setAction(ifSequence);
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
        return ifBranchBricks;
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
        for (Brick brick : ifBranchBricks) {
            brick.addToFlatList(bricks);
        }
        bricks.add(endBrick);
    }

    @Override
    public void setParent(Brick parentBrick) {
        super.setParent(parentBrick);
        for (Brick brick : ifBranchBricks) {
            brick.setParent(this);
        }
    }

    @Override
    public boolean removeChild(Brick brick) {
        if (ifBranchBricks.remove(brick)) {
            return true;
        }
        for (Brick child : ifBranchBricks) {
            if (child instanceof CompositeBrick && ((CompositeBrick) child).removeChild(brick)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Brick> getDragAndDropTargetList() {
        return ifBranchBricks;
    }

    @Override
    public void setCommentedOut(boolean commentedOut) {
        super.setCommentedOut(commentedOut);
        for (Brick brick : ifBranchBricks) {
            brick.setCommentedOut(commentedOut);
        }
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        IfCustomParamEqualsBrick clone = (IfCustomParamEqualsBrick) super.clone();
        clone.ifBranchBricks = new ArrayList<>();
        for (Brick brick : ifBranchBricks) {
            Brick clonedBrick = brick.clone();
            clonedBrick.setParent(clone);
            clone.ifBranchBricks.add(clonedBrick);
        }
        clone.endBrick = new EndBrick(clone);
        return clone;
    }

    @Override
    public void addRequiredResources(final ResourcesSet requiredResourcesSet) {
        super.addRequiredResources(requiredResourcesSet);
        for (Brick brick : ifBranchBricks) {
            brick.addRequiredResources(requiredResourcesSet);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof IfCustomParamEqualsBrick)) return false;
        IfCustomParamEqualsBrick other = (IfCustomParamEqualsBrick) obj;
        return ifBranchBricks.equals(other.ifBranchBricks);
    }

    @Override
    public int hashCode() {
        return ifBranchBricks.hashCode();
    }
}
