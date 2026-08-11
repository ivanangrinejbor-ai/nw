package org.catrobat.catroid.content.bricks

import org.catrobat.catroid.R
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.bricks.Brick.BrickField
import org.catrobat.catroid.content.bricks.Brick.ResourcesSet
import org.catrobat.catroid.formulaeditor.Formula

class DownloadZippedLooksBrick constructor() : FormulaBrick() {

    constructor(url: String, prefix: String) : this() {
        setFormulaWithBrickField(BrickField.DOWNLOAD_URL, Formula(url))
        setFormulaWithBrickField(BrickField.NAME, Formula(prefix))
    }

    constructor(url: Formula, prefix: Formula) : this() {
        setFormulaWithBrickField(BrickField.DOWNLOAD_URL, url)
        setFormulaWithBrickField(BrickField.NAME, prefix)
    }

    init {
        addAllowedBrickField(BrickField.DOWNLOAD_URL, R.id.brick_download_zipped_looks_url_edit)
        addAllowedBrickField(BrickField.NAME, R.id.brick_download_zipped_looks_prefix_edit)
    }

    override fun getViewResource(): Int = R.layout.brick_download_zipped_looks

    override fun addRequiredResources(requiredResourcesSet: ResourcesSet) {
        requiredResourcesSet.add(Brick.NETWORK_CONNECTION)
        super.addRequiredResources(requiredResourcesSet)
    }

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(
            sprite.actionFactory.createDownloadZippedLooksAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.DOWNLOAD_URL),
                getFormulaWithBrickField(BrickField.NAME)
            )
        )
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}