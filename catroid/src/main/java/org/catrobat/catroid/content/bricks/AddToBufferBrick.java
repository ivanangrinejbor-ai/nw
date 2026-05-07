package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R; import org.catrobat.catroid.content.Sprite; import org.catrobat.catroid.content.actions.ScriptSequenceAction; import org.catrobat.catroid.formulaeditor.Formula;

public class AddToBufferBrick extends FormulaBrick {
    public AddToBufferBrick() { addAllowedBrickField(BrickField.NAME, R.id.brick_buffer_name); }
    public AddToBufferBrick(String n) { this(new Formula(n)); }
    public AddToBufferBrick(Formula n) { this(); setFormulaWithBrickField(BrickField.NAME, n); }
    @Override public int getViewResource() { return R.layout.brick_add_to_buffer; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createAddToBufferAction(sprite, sequence, getFormulaWithBrickField(BrickField.NAME)));
    }
}