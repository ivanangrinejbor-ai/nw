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

public class VoxelConfigBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    // Empty constructor for XStream (serialization)
    public VoxelConfigBrick() {
        super();
        addAllowedBrickField(BrickField.NAME, R.id.brick_voxel_config_edit_id);
        addAllowedBrickField(BrickField.X, R.id.brick_voxel_config_edit_shape);
        addAllowedBrickField(BrickField.Y, R.id.brick_voxel_config_edit_tx);
        addAllowedBrickField(BrickField.Z, R.id.brick_voxel_config_edit_ty);
    }

    public VoxelConfigBrick(Formula id, Formula shape, Formula tx, Formula ty) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X, shape);
        setFormulaWithBrickField(BrickField.Y, tx);
        setFormulaWithBrickField(BrickField.Z, ty);
    }

    public VoxelConfigBrick(String id, int shape, int tx, int ty) {
        this(new Formula(id), new Formula(shape), new Formula(tx), new Formula(ty));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_config;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createVoxelConfigAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y),
                getFormulaWithBrickField(BrickField.Z)
        ));
    }
}
