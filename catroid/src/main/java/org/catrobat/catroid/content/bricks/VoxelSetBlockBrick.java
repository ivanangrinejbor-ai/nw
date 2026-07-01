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

public class VoxelSetBlockBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VoxelSetBlockBrick() {
        super();
        addAllowedBrickField(BrickField.NAME, R.id.brick_voxel_set_edit_world);
        addAllowedBrickField(BrickField.X, R.id.brick_voxel_set_edit_x);
        addAllowedBrickField(BrickField.Y, R.id.brick_voxel_set_edit_y);
        addAllowedBrickField(BrickField.Z, R.id.brick_voxel_set_edit_z);
        addAllowedBrickField(BrickField.VALUE, R.id.brick_voxel_set_edit_type);
        addAllowedBrickField(BrickField.DATA, R.id.brick_voxel_set_edit_data);
    }

    public VoxelSetBlockBrick(Formula worldId, Formula x, Formula y, Formula z, Formula type, Formula data) {
        this();
        setFormulaWithBrickField(BrickField.NAME, worldId);
        setFormulaWithBrickField(BrickField.X, x);
        setFormulaWithBrickField(BrickField.Y, y);
        setFormulaWithBrickField(BrickField.Z, z);
        setFormulaWithBrickField(BrickField.VALUE, type);
        setFormulaWithBrickField(BrickField.DATA, data);
    }

    public VoxelSetBlockBrick(String worldId, double x, double y, double z, int type, int data) {
        this(new Formula(worldId), new Formula(x), new Formula(y), new Formula(z), new Formula(type), new Formula(data));
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_set_block;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createVoxelSetBlockAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y),
                getFormulaWithBrickField(BrickField.Z),
                getFormulaWithBrickField(BrickField.VALUE),
                getFormulaWithBrickField(BrickField.DATA)
        ));
    }
}
