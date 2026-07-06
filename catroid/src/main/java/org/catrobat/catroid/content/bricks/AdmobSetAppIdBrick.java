package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class AdmobSetAppIdBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public AdmobSetAppIdBrick() {
        addAllowedBrickField(BrickField.AD_APP_ID, R.id.brick_admob_app_id_edit);
    }

    public AdmobSetAppIdBrick(String appId) {
        this(new Formula(appId));
    }

    public AdmobSetAppIdBrick(Formula appId) {
        this();
        setFormulaWithBrickField(BrickField.AD_APP_ID, appId);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_admob_set_app_id;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createAdmobSetAppIdAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.AD_APP_ID)));
    }
}
