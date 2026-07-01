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

public class UpdateManifestBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public UpdateManifestBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_manif_edit_apk);
        addAllowedBrickField(BrickField.TEXT_2, R.id.brick_manif_edit_pkg);
        addAllowedBrickField(BrickField.TEXT_3, R.id.brick_manif_edit_name);
        addAllowedBrickField(BrickField.TEXT_4, R.id.brick_manif_edit_vcode);
        addAllowedBrickField(BrickField.TEXT_5, R.id.brick_manif_edit_vname);
        addAllowedBrickField(BrickField.TEXT_6, R.id.brick_manif_edit_min);
        addAllowedBrickField(BrickField.TEXT_7, R.id.brick_manif_edit_target);
        addAllowedBrickField(BrickField.TEXT_8, R.id.brick_manif_edit_debug);
        addAllowedBrickField(BrickField.TEXT_9, R.id.brick_manif_edit_p_add);
        addAllowedBrickField(BrickField.TEXT_10, R.id.brick_manif_edit_p_rem);
    }

    public UpdateManifestBrick(Formula apk, Formula pkg, Formula name, Formula vCode, Formula vName,
                               Formula min, Formula target, Formula debug, Formula pAdd, Formula pRem) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, apk);
        setFormulaWithBrickField(BrickField.TEXT_2, pkg);
        setFormulaWithBrickField(BrickField.TEXT_3, name);
        setFormulaWithBrickField(BrickField.TEXT_4, vCode);
        setFormulaWithBrickField(BrickField.TEXT_5, vName);
        setFormulaWithBrickField(BrickField.TEXT_6, min);
        setFormulaWithBrickField(BrickField.TEXT_7, target);
        setFormulaWithBrickField(BrickField.TEXT_8, debug);
        setFormulaWithBrickField(BrickField.TEXT_9, pAdd);
        setFormulaWithBrickField(BrickField.TEXT_10, pRem);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_update_manifest;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createUpdateManifestAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1),
                        getFormulaWithBrickField(BrickField.TEXT_2),
                        getFormulaWithBrickField(BrickField.TEXT_3),
                        getFormulaWithBrickField(BrickField.TEXT_4),
                        getFormulaWithBrickField(BrickField.TEXT_5),
                        getFormulaWithBrickField(BrickField.TEXT_6),
                        getFormulaWithBrickField(BrickField.TEXT_7),
                        getFormulaWithBrickField(BrickField.TEXT_8),
                        getFormulaWithBrickField(BrickField.TEXT_9),
                        getFormulaWithBrickField(BrickField.TEXT_10)
                ));
    }
}