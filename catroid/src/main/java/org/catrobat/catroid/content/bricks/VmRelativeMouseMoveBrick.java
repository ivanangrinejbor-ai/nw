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

public class VmRelativeMouseMoveBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public VmRelativeMouseMoveBrick() {
        addAllowedBrickField(BrickField.X_POSITION, R.id.brick_vm_mouse_edit_dx);
        addAllowedBrickField(BrickField.Y_POSITION, R.id.brick_vm_mouse_edit_dy);
        addAllowedBrickField(BrickField.MASK, R.id.brick_vm_mouse_mask);
    }

    public VmRelativeMouseMoveBrick(int dx, int dy, int mask) {
        this(new Formula(dx), new Formula(dy), new Formula(mask));
    }

    public VmRelativeMouseMoveBrick(Formula dx, Formula dy, Formula mask) {
        this();
        setFormulaWithBrickField(BrickField.X_POSITION, dx);
        setFormulaWithBrickField(BrickField.Y_POSITION, dy);
        setFormulaWithBrickField(BrickField.MASK, mask);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_vm_mouse_relative;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createVmRelativeMouseMoveAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.X_POSITION),
                        getFormulaWithBrickField(BrickField.Y_POSITION),
                        getFormulaWithBrickField(BrickField.MASK)));
    }
}