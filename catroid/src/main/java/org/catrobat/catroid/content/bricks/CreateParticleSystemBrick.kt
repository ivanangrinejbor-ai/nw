package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.content.bricks.Brick.BrickField

class CreateParticleSystemBrick : FormulaBrick() {
    companion object {
        @JvmField val serialVersionUID: Long = 1L
    }

    constructor() {
        addAllowedBrickField(BrickField.PARTICLE_ID, R.id.brick_create_particle_system_edit_id)
        addAllowedBrickField(BrickField.PARTICLE_MAX_COUNT, R.id.brick_create_particle_system_edit_count)
        addAllowedBrickField(BrickField.PARTICLE_LIFETIME, R.id.brick_create_particle_system_edit_lifetime)
        addAllowedBrickField(BrickField.PARTICLE_SPEED, R.id.brick_create_particle_system_edit_speed)
    }

    constructor(id: String, maxCount: Double, lifetime: Double, speed: Double) : this() {
        setFormulaWithBrickField(BrickField.PARTICLE_ID, Formula(id))
        setFormulaWithBrickField(BrickField.PARTICLE_MAX_COUNT, Formula(maxCount))
        setFormulaWithBrickField(BrickField.PARTICLE_LIFETIME, Formula(lifetime))
        setFormulaWithBrickField(BrickField.PARTICLE_SPEED, Formula(speed))
    }

    override fun getViewResource(): Int = R.layout.brick_create_particle_system

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createParticleSystemAction(
            sprite, sequence,
            getFormulaWithBrickField(BrickField.PARTICLE_ID),
            getFormulaWithBrickField(BrickField.PARTICLE_MAX_COUNT),
            getFormulaWithBrickField(BrickField.PARTICLE_LIFETIME),
            getFormulaWithBrickField(BrickField.PARTICLE_SPEED)
        ))
    }
}
