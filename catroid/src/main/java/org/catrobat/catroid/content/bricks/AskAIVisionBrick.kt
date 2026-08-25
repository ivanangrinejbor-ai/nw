/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.bricks

import android.content.Context
import android.view.View
import org.catrobat.catroid.R
import org.catrobat.catroid.ai.model.AiProvider
import org.catrobat.catroid.common.Nameable
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.bricks.Brick.BrickField
import org.catrobat.catroid.content.bricks.Brick.ResourcesSet
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner
import org.catrobat.catroid.content.bricks.brickspinner.StringOption
import org.catrobat.catroid.formulaeditor.Formula

class AskAIVisionBrick() : UserVariableBrickWithFormula() {

    private var providerSelection = AiProvider.GEMINI.id

    @Transient
    private var lookData: org.catrobat.catroid.common.LookData? = null

    fun getProviderSelection(): String = providerSelection

    fun getSelectedLookData(): org.catrobat.catroid.common.LookData? = lookData

    constructor(prompt: String) : this() {
        setFormulaWithBrickField(BrickField.TEXT, Formula(prompt))
    }

    init {
        addAllowedBrickField(BrickField.TEXT, R.id.brick_ask_ai_vision_edit_prompt)
        addAllowedBrickField(BrickField.BODY, R.id.brick_ask_ai_vision_edit_system)
        addAllowedBrickField(BrickField.MODEL, R.id.brick_ask_ai_vision_edit_model)
    }

    override fun getViewResource(): Int = R.layout.brick_ask_ai_vision

    override fun getView(context: Context): View {
        super.getView(context)

        val providerItems: MutableList<Nameable> = ArrayList()
        for (provider in AiProvider.values()) {
            providerItems.add(StringOption(provider.id))
        }
        val providerSpinner = BrickSpinner<StringOption>(
            R.id.brick_ask_ai_vision_spinner, view, providerItems
        )
        providerSpinner.setOnItemSelectedListener(object :
                BrickSpinner.OnItemSelectedListener<StringOption> {
                override fun onNewOptionSelected(spinnerId: Int?) {}
                override fun onEditOptionSelected(spinnerId: Int?) {}
                override fun onStringOptionSelected(spinnerId: Int?, string: String?) {
                    providerSelection = string ?: AiProvider.GEMINI.id
                }
                override fun onItemSelected(spinnerId: Int?, item: StringOption?) {
                    providerSelection = item?.name ?: AiProvider.GEMINI.id
                }
            })
        providerSpinner.setSelection(providerSelection)

        val lookItems: MutableList<Nameable> = ArrayList()
        val currentSprite = org.catrobat.catroid.ProjectManager.getInstance().currentSprite
        if (currentSprite != null) {
            lookItems.addAll(currentSprite.lookList)
        }
        val lookSpinner = BrickSpinner<org.catrobat.catroid.common.LookData>(
            R.id.brick_ask_ai_vision_look_spinner, view, lookItems
        )
        lookSpinner.setOnItemSelectedListener(object :
                BrickSpinner.OnItemSelectedListener<org.catrobat.catroid.common.LookData> {
                override fun onNewOptionSelected(spinnerId: Int?) {}
                override fun onEditOptionSelected(spinnerId: Int?) {}
                override fun onStringOptionSelected(spinnerId: Int?, string: String?) {}
                override fun onItemSelected(spinnerId: Int?, item: org.catrobat.catroid.common.LookData?) {
                    lookData = item
                }
            })
        if (lookData != null) {
            lookSpinner.setSelection(lookData)
        }

        return view
    }

    override fun addActionToSequence(sprite: Sprite, sequence: ScriptSequenceAction) {
        sequence.addAction(
            sprite.actionFactory.createAskAIVisionAction(
                sprite, sequence,
                getFormulaWithBrickField(BrickField.TEXT),
                getFormulaWithBrickField(BrickField.BODY),
                getFormulaWithBrickField(BrickField.MODEL),
                providerSelection,
                lookData,
                userVariable
            )
        )
    }

    override fun addRequiredResources(requiredResourcesSet: ResourcesSet) {
        requiredResourcesSet.add(Brick.NETWORK_CONNECTION)
        super.addRequiredResources(requiredResourcesSet)
    }

    override fun getSpinnerId(): Int = R.id.brick_ask_ai_vision_user_variable_spinner
}
