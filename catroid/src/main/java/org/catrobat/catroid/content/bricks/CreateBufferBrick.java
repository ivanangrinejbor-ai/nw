package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R; import org.catrobat.catroid.content.Sprite; import org.catrobat.catroid.content.actions.ScriptSequenceAction; import org.catrobat.catroid.formulaeditor.Formula;

public class CreateBufferBrick extends FormulaBrick {
    public CreateBufferBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_buffer_name);
        addAllowedBrickField(BrickField.WIDTH, R.id.brick_buffer_width);
        addAllowedBrickField(BrickField.HEIGHT, R.id.brick_buffer_height);
    }
    public CreateBufferBrick(String n, String w, String h) { this(new Formula(n), new Formula(w), new Formula(h)); }
    public CreateBufferBrick(Formula n, Formula w, Formula h) {
        this();
        setFormulaWithBrickField(BrickField.NAME, n); setFormulaWithBrickField(BrickField.WIDTH, w); setFormulaWithBrickField(BrickField.HEIGHT, h);
    }
    @Override public int getViewResource() { return R.layout.brick_create_buffer; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createBufferAction(sprite, sequence, getFormulaWithBrickField(BrickField.NAME), getFormulaWithBrickField(BrickField.WIDTH), getFormulaWithBrickField(BrickField.HEIGHT)));
    }
}