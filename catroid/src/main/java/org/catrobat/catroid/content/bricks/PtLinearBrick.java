/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PtLinearBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public PtLinearBrick() {
        addAllowedBrickField(BrickField.NAME, R.id.brick_pt_linear_name);
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_pt_linear_input);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_pt_linear_output);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_pt_linear_in_features);
        addAllowedBrickField(BrickField.VALUE_4, R.id.brick_pt_linear_out_features);
    }

    public PtLinearBrick(String layerName, String input, String output, int inFeatures, int outFeatures) {
        this(new Formula(layerName), new Formula(input), new Formula(output),
                new Formula(inFeatures), new Formula(outFeatures));
    }

    public PtLinearBrick(Formula layerName, Formula input, Formula output, Formula inFeatures, Formula outFeatures) {
        this();
        setFormulaWithBrickField(BrickField.NAME, layerName);
        setFormulaWithBrickField(BrickField.VALUE_1, input);
        setFormulaWithBrickField(BrickField.VALUE_2, output);
        setFormulaWithBrickField(BrickField.VALUE_3, inFeatures);
        setFormulaWithBrickField(BrickField.VALUE_4, outFeatures);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_pt_linear;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createPtLinearAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.VALUE_1),
                getFormulaWithBrickField(BrickField.VALUE_2),
                getFormulaWithBrickField(BrickField.VALUE_3),
                getFormulaWithBrickField(BrickField.VALUE_4)
        ));
    }
}
