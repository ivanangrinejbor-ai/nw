package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class AdmobShowBannerBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public AdmobShowBannerBrick() {
        addAllowedBrickField(BrickField.AD_POSITION, R.id.brick_admob_position_edit);
    }

    public AdmobShowBannerBrick(Formula position) {
        this();
        setFormulaWithBrickField(BrickField.AD_POSITION, position);
    }

    public AdmobShowBannerBrick(int position) {
        this(new Formula(position));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_admob_show_banner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createAdmobShowBannerAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.AD_POSITION)));
    }
}
