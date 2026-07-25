package org.catrobat.catroid.content.bricks

import android.content.Context
import android.view.View
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Nameable
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner
import org.catrobat.catroid.content.bricks.brickspinner.NewOption
import org.catrobat.catroid.ui.SpriteActivity
import org.catrobat.catroid.ui.UiUtils
import org.catrobat.catroid.ui.recyclerview.dialog.dialoginterface.NewItemInterface

class ResumeSoundBrick : BrickBaseType(),
    BrickSpinner.OnItemSelectedListener<SoundInfo>, NewItemInterface<SoundInfo> {

    var sound: SoundInfo? = null
    @Transient private lateinit var spinner: BrickSpinner<SoundInfo>

    override fun getViewResource() = R.layout.brick_resume_sound

    override fun getView(context: Context): View {
        super.getView(context)
        val items = mutableListOf<Nameable>(NewOption(context.getString(R.string.new_option)))
        items.addAll(ProjectManager.getInstance().currentSprite.soundList)
        spinner = BrickSpinner(R.id.brick_resume_sound_spinner, view, items)
        spinner.setOnItemSelectedListener(this)
        spinner.setSelection(sound)
        return view
    }

    override fun onNewOptionSelected(spinnerId: Int) {
        (UiUtils.getActivityFromView(view) as? SpriteActivity)?.apply {
            registerOnNewSoundListener(this@ResumeSoundBrick)
            handleAddSoundButton()
        }
    }
    override fun onEditOptionSelected(spinnerId: Int) = Unit
    override fun addItem(item: SoundInfo) { spinner.add(item); spinner.setSelection(item) }
    override fun onStringOptionSelected(spinnerId: Int, string: String) = Unit
    override fun onItemSelected(spinnerId: Int, item: SoundInfo?) { sound = item }

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createResumeSoundAction(sprite, sound))
    }
}
