package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetBufferOnlyBrick extends FormulaBrick {
    public SetBufferOnlyBrick() {
        addAllowedBrickField(BrickField.TIME, R.id.brick_buffer_state);
    }
    public SetBufferOnlyBrick(Formula s) { this(); setFormulaWithBrickField(BrickField.TIME, s); }
    @Override public int getViewResource() { return R.layout.brick_buffer_only; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetBufferOnlyAction(sprite, sequence, getFormulaWithBrickField(BrickField.TIME)));
    }
}