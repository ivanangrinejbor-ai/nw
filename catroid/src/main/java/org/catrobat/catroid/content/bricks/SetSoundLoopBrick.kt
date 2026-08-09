package org.catrobat.catroid.content.bricks

import android.content.Context
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
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

class SetSoundLoopBrick : BrickBaseType(),
    BrickSpinner.OnItemSelectedListener<SoundInfo>, NewItemInterface<SoundInfo> {

    var sound: SoundInfo? = null
    var loopEnabled: Int = 1
    @Transient private lateinit var spinner: BrickSpinner<SoundInfo>

    override fun getViewResource() = R.layout.brick_set_sound_loop

    override fun getView(context: Context): View {
        super.getView(context)
        val items = mutableListOf<Nameable>(NewOption(context.getString(R.string.new_option)))
        items.addAll(ProjectManager.getInstance().currentSprite.soundList)
        spinner = BrickSpinner(R.id.brick_set_sound_loop_spinner, view, items)
        spinner.setOnItemSelectedListener(this)
        spinner.setSelection(sound)

        val toggleSpinner = view.findViewById<Spinner>(R.id.brick_set_sound_loop_toggle)
        val adapter = ArrayAdapter(context, R.layout.simple_spinner_item_white_text,
            arrayOf(context.getString(R.string.no), context.getString(R.string.yes)))
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text)
        toggleSpinner.adapter = adapter
        toggleSpinner.setSelection(loopEnabled)
        toggleSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) { loopEnabled = pos }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        return view
    }

    override fun onNewOptionSelected(spinnerId: Int) {
        (UiUtils.getActivityFromView(view) as? SpriteActivity)?.apply {
            registerOnNewSoundListener(this@SetSoundLoopBrick)
            handleAddSoundButton()
        }
    }
    override fun onEditOptionSelected(spinnerId: Int) = Unit
    override fun addItem(item: SoundInfo) { spinner.add(item); spinner.setSelection(item) }
    override fun onStringOptionSelected(spinnerId: Int, string: String) = Unit
    override fun onItemSelected(spinnerId: Int, item: SoundInfo?) { sound = item }

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(sprite.actionFactory.createSetSoundLoopAction(sprite, sound, loopEnabled == 1))
    }
}
