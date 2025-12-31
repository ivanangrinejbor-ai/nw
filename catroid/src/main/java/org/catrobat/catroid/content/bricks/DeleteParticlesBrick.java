package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import java.util.List;

public class DeleteParticlesBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public DeleteParticlesBrick() {
        addAllowedBrickField(BrickField.PARTICLE_ID, R.id.brick_particle_id_edit);
    }

    public DeleteParticlesBrick(String particleId) {
        this(new Formula(particleId));
    }

    public DeleteParticlesBrick(Formula particleId) {
        this();
        setFormulaWithBrickField(BrickField.PARTICLE_ID, particleId);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_delete_particles;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createDeleteParticlesAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.PARTICLE_ID)
        ));
    }
}