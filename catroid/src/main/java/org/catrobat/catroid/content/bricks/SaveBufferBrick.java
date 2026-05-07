package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R; import org.catrobat.catroid.content.Sprite; import org.catrobat.catroid.content.actions.ScriptSequenceAction; import org.catrobat.catroid.formulaeditor.Formula;
public class SaveBufferBrick extends FormulaBrick {
    public SaveBufferBrick() { addAllowedBrickField(BrickField.NAME, R.id.brick_buffer_name); addAllowedBrickField(BrickField.STRING_VALUE, R.id.brick_buffer_file); }
    public SaveBufferBrick(Formula n, Formula f) { this(); setFormulaWithBrickField(BrickField.NAME, n); setFormulaWithBrickField(BrickField.STRING_VALUE, f); }
    @Override public int getViewResource() { return R.layout.brick_save_buffer; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) { sequence.addAction(sprite.getActionFactory().createSaveBufferAction(sprite, sequence, getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.STRING_VALUE))); }
}