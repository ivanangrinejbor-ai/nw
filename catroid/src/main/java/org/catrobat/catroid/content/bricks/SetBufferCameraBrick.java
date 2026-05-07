package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R; import org.catrobat.catroid.content.Sprite; import org.catrobat.catroid.content.actions.ScriptSequenceAction; import org.catrobat.catroid.formulaeditor.Formula;

public class SetBufferCameraBrick extends FormulaBrick {
    public SetBufferCameraBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_buffer_name);
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_buffer_x);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_buffer_y);
        addAllowedBrickField(BrickField.ZOOM, R.id.brick_buffer_zoom);
        addAllowedBrickField(BrickField.DEGREES, R.id.brick_buffer_rot);
    }
    public SetBufferCameraBrick(String n, String x, String y, String z, String r) { this(new Formula(n), new Formula(x), new Formula(y), new Formula(z), new Formula(r)); }
    public SetBufferCameraBrick(Formula n, Formula x, Formula y, Formula z, Formula r) {
        this();
        setFormulaWithBrickField(BrickField.NAME, n); setFormulaWithBrickField(BrickField.X_POSITION, x); setFormulaWithBrickField(BrickField.Y_POSITION, y); setFormulaWithBrickField(BrickField.ZOOM, z); setFormulaWithBrickField(BrickField.DEGREES, r);
    }
    @Override public int getViewResource() { return R.layout.brick_set_buffer_cam; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetBufferCameraAction(sprite, sequence, getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.X_POSITION), getFormulaWithBrickField(BrickField.Y_POSITION), getFormulaWithBrickField(BrickField.ZOOM), getFormulaWithBrickField(BrickField.DEGREES)));
    }
}