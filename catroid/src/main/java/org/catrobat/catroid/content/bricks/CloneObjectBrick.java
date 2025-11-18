package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CloneObjectBrick extends FormulaBrick {
    public CloneObjectBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_clone_source_edit);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_clone_new_name_edit);
    }

    public CloneObjectBrick(String sourceName, String newName) {
        this(new Formula(sourceName), new Formula(newName));
    }

    public CloneObjectBrick(Formula source, Formula newName) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, source);
        setFormulaWithBrickField(BrickField.VALUE_2, newName);
    }

    @Override
    public int getViewResource() { return R.layout.brick_clone_object; }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createCloneObjectAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE_1),
                getFormulaWithBrickField(BrickField.VALUE_2)
        ));
    }
}