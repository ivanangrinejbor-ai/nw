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

public class VoxelBuildBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VoxelBuildBrick() {
        super();
        addAllowedBrickField(BrickField.NAME, R.id.brick_voxel_world_id);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_voxel_atlas_text);
        addAllowedBrickField(BrickField.X, R.id.brick_voxel_atlas_width);
        addAllowedBrickField(BrickField.Y, R.id.brick_voxel_atlas_height);
    }

    public VoxelBuildBrick(Formula id, Formula atlas, Formula w, Formula h) {
        this();
        setFormulaWithBrickField(BrickField.NAME, id);
        setFormulaWithBrickField(BrickField.TEXT, atlas);
        setFormulaWithBrickField(BrickField.X, w);
        setFormulaWithBrickField(BrickField.Y, h);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_voxel_build;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createVoxelBuildAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NAME),
                        getFormulaWithBrickField(BrickField.TEXT),
                        getFormulaWithBrickField(BrickField.X),
                        getFormulaWithBrickField(BrickField.Y)));
    }
}
