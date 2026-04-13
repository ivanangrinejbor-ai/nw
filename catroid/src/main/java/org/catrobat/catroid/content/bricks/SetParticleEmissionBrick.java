package org.catrobat.catroid.content.bricks;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetParticleEmissionBrick extends FormulaBrick {
    public SetParticleEmissionBrick() {
        addAllowedBrickField(BrickField.OBJECT_ID, R.id.particle_obj_id);
        addAllowedBrickField(BrickField.EMISSION_RATE, R.id.particle_rate2);
    }
    public SetParticleEmissionBrick(String id, float rate) {
        this(new Formula(id), new Formula(rate));
    }
    public SetParticleEmissionBrick(Formula id, Formula rate) {
        this();
        setFormulaWithBrickField(BrickField.OBJECT_ID, id);
        setFormulaWithBrickField(BrickField.EMISSION_RATE, rate);
    }
    @Override public int getViewResource() { return R.layout.brick_set_particle_emission; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetParticleEmissionAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.OBJECT_ID), getFormulaWithBrickField(BrickField.EMISSION_RATE)));
    }
}