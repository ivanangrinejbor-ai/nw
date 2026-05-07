package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R; import org.catrobat.catroid.content.Sprite; import org.catrobat.catroid.content.actions.ScriptSequenceAction; import org.catrobat.catroid.formulaeditor.Formula;
public class SetBufferAutoUpdateBrick extends FormulaBrick {
    public SetBufferAutoUpdateBrick() { addAllowedBrickField(BrickField.NAME, R.id.brick_buffer_name); addAllowedBrickField(BrickField.TIME, R.id.brick_buffer_state); }
    public SetBufferAutoUpdateBrick(Formula n, Formula s) { this(); setFormulaWithBrickField(BrickField.NAME, n); setFormulaWithBrickField(BrickField.TIME, s); }
    @Override public int getViewResource() { return R.layout.brick_buffer_auto_update; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) { sequence.addAction(sprite.getActionFactory().createSetBufferAutoUpdateAction(sprite, sequence, getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.TIME))); }
}