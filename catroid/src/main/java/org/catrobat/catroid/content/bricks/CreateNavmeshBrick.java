package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CreateNavmeshBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateNavmeshBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_create_navmesh_edit_1);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_create_navmesh_edit_2);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_create_navmesh_edit_3);
    }

    public CreateNavmeshBrick(Integer value1, Integer value2, Integer value3) {
        this(new Formula(value1), new Formula(value2), new Formula(value3));
    }

    public CreateNavmeshBrick(Formula formula1, Formula formula2, Formula formula3) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, formula1);
        setFormulaWithBrickField(BrickField.VALUE_2, formula2);
        setFormulaWithBrickField(BrickField.VALUE_3, formula3);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_navmesh;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createCreateNavmeshAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE_1),
                        getFormulaWithBrickField(BrickField.VALUE_2),
                        getFormulaWithBrickField(BrickField.VALUE_3)));
    }
}
