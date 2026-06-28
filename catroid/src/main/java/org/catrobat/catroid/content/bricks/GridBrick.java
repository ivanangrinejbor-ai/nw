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

public class GridBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public GridBrick() {
        addAllowedBrickField(BrickField.POSX, R.id.brick_grid_edit_x);
        addAllowedBrickField(BrickField.POSY, R.id.brick_grid_edit_y);
        addAllowedBrickField(BrickField.SIZE_X, R.id.brick_grid_edit_width);
        addAllowedBrickField(BrickField.SIZE_Y, R.id.brick_grid_edit_height);
    }

    public GridBrick(double x, double y, double w, double h) {
        this(new Formula(x), new Formula(y), new Formula(w), new Formula(h));
    }

    public GridBrick(Formula x, Formula y, Formula w, Formula h) {
        this();
        setFormulaWithBrickField(BrickField.POSX, x);
        setFormulaWithBrickField(BrickField.POSY, y);
        setFormulaWithBrickField(BrickField.SIZE_X, w);
        setFormulaWithBrickField(BrickField.SIZE_Y, h);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_grid;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createGridAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.POSX),
                        getFormulaWithBrickField(BrickField.POSY),
                        getFormulaWithBrickField(BrickField.SIZE_X),
                        getFormulaWithBrickField(BrickField.SIZE_Y)));
    }
}
