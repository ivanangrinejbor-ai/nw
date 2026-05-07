package org.catrobat.catroid.content.bricks;
import android.view.View; import android.widget.TextView;
import org.catrobat.catroid.R; import org.catrobat.catroid.content.Sprite; import org.catrobat.catroid.content.actions.RemoveFromBufferAction; import org.catrobat.catroid.content.actions.ScriptSequenceAction; import org.catrobat.catroid.formulaeditor.Formula;
public class RemoveFromBufferBrick extends FormulaBrick {
    public RemoveFromBufferBrick() { addAllowedBrickField(BrickField.NAME, R.id.brick_buffer_name); }
    public RemoveFromBufferBrick(Formula n) { this(); setFormulaWithBrickField(BrickField.NAME, n); }
    @Override public int getViewResource() { return R.layout.brick_remove_from_buffer; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) { sequence.addAction(sprite.getActionFactory().removeBufferAction(sprite, sequence, getFormulaWithBrickField(BrickField.NAME))); }
}