package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class AddHingeBrick extends FormulaBrick {
    public AddHingeBrick() {
        addAllowedBrickField(BrickField.CONSTRAINT_ID, R.id.brick_hinge_id);
        addAllowedBrickField(BrickField.OBJECT_A, R.id.brick_hinge_obja);
        addAllowedBrickField(BrickField.OBJECT_B, R.id.brick_hinge_objb);

        addAllowedBrickField(BrickField.PIVOT_A_X, R.id.brick_hinge_pax);
        addAllowedBrickField(BrickField.PIVOT_A_Y, R.id.brick_hinge_pay);
        addAllowedBrickField(BrickField.PIVOT_A_Z, R.id.brick_hinge_paz);

        addAllowedBrickField(BrickField.AXIS_A_X, R.id.brick_hinge_aax);
        addAllowedBrickField(BrickField.AXIS_A_Y, R.id.brick_hinge_aay);
        addAllowedBrickField(BrickField.AXIS_A_Z, R.id.brick_hinge_aaz);

        addAllowedBrickField(BrickField.PIVOT_B_X, R.id.brick_hinge_pbx);
        addAllowedBrickField(BrickField.PIVOT_B_Y, R.id.brick_hinge_pby);
        addAllowedBrickField(BrickField.PIVOT_B_Z, R.id.brick_hinge_pbz);

        addAllowedBrickField(BrickField.AXIS_B_X, R.id.brick_hinge_abx);
        addAllowedBrickField(BrickField.AXIS_B_Y, R.id.brick_hinge_aby);
        addAllowedBrickField(BrickField.AXIS_B_Z, R.id.brick_hinge_abz);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_threed_hinge;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createAddHingeAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.CONSTRAINT_ID),
                getFormulaWithBrickField(BrickField.OBJECT_A),
                getFormulaWithBrickField(BrickField.OBJECT_B),
                getFormulaWithBrickField(BrickField.PIVOT_A_X), getFormulaWithBrickField(BrickField.PIVOT_A_Y), getFormulaWithBrickField(BrickField.PIVOT_A_Z),
                getFormulaWithBrickField(BrickField.AXIS_A_X),  getFormulaWithBrickField(BrickField.AXIS_A_Y),  getFormulaWithBrickField(BrickField.AXIS_A_Z),
                getFormulaWithBrickField(BrickField.PIVOT_B_X), getFormulaWithBrickField(BrickField.PIVOT_B_Y), getFormulaWithBrickField(BrickField.PIVOT_B_Z),
                getFormulaWithBrickField(BrickField.AXIS_B_X),  getFormulaWithBrickField(BrickField.AXIS_B_Y),  getFormulaWithBrickField(BrickField.AXIS_B_Z)
        ));
    }
}