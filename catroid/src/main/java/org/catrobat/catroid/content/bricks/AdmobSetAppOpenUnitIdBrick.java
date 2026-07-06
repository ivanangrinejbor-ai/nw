package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class AdmobSetAppOpenUnitIdBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public AdmobSetAppOpenUnitIdBrick() {
        addAllowedBrickField(BrickField.AD_UNIT_ID, R.id.brick_admob_unit_id_edit);
    }

    public AdmobSetAppOpenUnitIdBrick(String unitId) {
        this(new Formula(unitId));
    }

    public AdmobSetAppOpenUnitIdBrick(Formula unitId) {
        this();
        setFormulaWithBrickField(BrickField.AD_UNIT_ID, unitId);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_admob_set_app_open_unit_id;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createAdmobSetAppOpenUnitIdAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.AD_UNIT_ID)));
    }
}
