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

public class VoxelCreateWorldBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VoxelCreateWorldBrick() {
        super();
        addAllowedBrickField(BrickField.NAME, R.id.brick_voxel_world_id);

        addAllowedBrickField(BrickField.X, R.id.brick_voxel_size_x);
        addAllowedBrickField(BrickField.Y, R.id.brick_voxel_size_y);
        addAllowedBrickField(BrickField.Z, R.id.brick_voxel_size_z);

        addAllowedBrickField(BrickField.X_1, R.id.brick_voxel_world_x);
        addAllowedBrickField(BrickField.Y_1, R.id.brick_voxel_world_y);
        addAllowedBrickField(BrickField.Z_1, R.id.brick_voxel_world_z);
    }

    public VoxelCreateWorldBrick(Formula id, Formula sx, Formula sy, Formula sz, Formula wx, Formula wy, Formula wz) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.X, sx);
        setFormulaWithBrickField(BrickField.Y, sy);
        setFormulaWithBrickField(BrickField.Z, sz);
        setFormulaWithBrickField(BrickField.X_1, wx);
        setFormulaWithBrickField(BrickField.Y_1, wy);
        setFormulaWithBrickField(BrickField.Z_1, wz);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_create_world;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createVoxelCreateWorldAction(
                sprite,
                sequence,
                getFormulaWithBrickField(BrickField.NAME),
                getFormulaWithBrickField(BrickField.X),
                getFormulaWithBrickField(BrickField.Y),
                getFormulaWithBrickField(BrickField.Z),
                getFormulaWithBrickField(BrickField.X_1),
                getFormulaWithBrickField(BrickField.Y_1),
                getFormulaWithBrickField(BrickField.Z_1)
        ));
    }
}
