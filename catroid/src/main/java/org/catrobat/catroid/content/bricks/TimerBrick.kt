package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.formulaeditor.Formula

class TimerBrick() : FormulaBrick() {
    companion object { @JvmField val serialVersionUID: Long = 1L }

    init { addFields() }
    private fun addFields() {
        addAllowedBrickField(Brick.BrickField.NAME, R.id.brick_timer_edit_name)
        addAllowedBrickField(Brick.BrickField.DURATION_IN_SECONDS, R.id.brick_timer_edit_duration)
        addAllowedBrickField(Brick.BrickField.VARIABLE, R.id.brick_timer_edit_variable)
    }

    constructor(name: String, duration: Double, variableName: String) : this() {
        setFormulaWithBrickField(Brick.BrickField.NAME, Formula(name))
        setFormulaWithBrickField(Brick.BrickField.DURATION_IN_SECONDS, Formula(duration))
        setFormulaWithBrickField(Brick.BrickField.VARIABLE, Formula(variableName))
    }

    override fun getViewResource(): Int = R.layout.brick_timer
    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createTimerAction(sprite, sequence,
            getFormulaWithBrickField(Brick.BrickField.NAME),
            getFormulaWithBrickField(Brick.BrickField.DURATION_IN_SECONDS),
            getFormulaWithBrickField(Brick.BrickField.VARIABLE)))
    }
}
