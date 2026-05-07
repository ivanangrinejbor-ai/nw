package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R; import org.catrobat.catroid.content.Sprite; import org.catrobat.catroid.content.actions.ScriptSequenceAction; import org.catrobat.catroid.formulaeditor.Formula;

public class ApplyBufferLookBrick extends FormulaBrick {
    public ApplyBufferLookBrick() { addAllowedBrickField(BrickField.NAME, R.id.brick_buffer_name); }
    public ApplyBufferLookBrick(String n) { this(new Formula(n)); }
    public ApplyBufferLookBrick(Formula n) { this(); setFormulaWithBrickField(BrickField.NAME, n); }
    @Override public int getViewResource() { return R.layout.brick_apply_buffer; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createApplyBufferLookAction(sprite, sequence, getFormulaWithBrickField(BrickField.NAME)));
    }
}