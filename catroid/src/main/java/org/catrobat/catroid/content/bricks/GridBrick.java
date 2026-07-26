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

import android.content.Intent;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.visualplacement.VisualPlacementActivity;

public class GridBrick extends VisualPlacementBrick {
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
    public BrickField getXBrickField() {
        return BrickField.POSX;
    }

    @Override
    public BrickField getYBrickField() {
        return BrickField.POSY;
    }

    @Override
    public int getXEditTextId() {
        return R.id.brick_grid_edit_x;
    }

    @Override
    public int getYEditTextId() {
        return R.id.brick_grid_edit_y;
    }

    @Override
    public Intent generateIntentForVisualPlacement(BrickField brickFieldX, BrickField brickFieldY) {
        Intent intent = super.generateIntentForVisualPlacement(brickFieldX, brickFieldY);
        // Вместо спрайта в редакторе таскается миниатюрная сетка-превью реального размера.
        int columns = 32;
        int rows = 32;
        try {
            ProjectManager projectManager = ProjectManager.getInstance();
            Scope scope = new Scope(projectManager.getCurrentProject(),
                    projectManager.getCurrentSprite(), null);
            columns = getFormulaWithBrickField(BrickField.SIZE_X).interpretInteger(scope);
            rows = getFormulaWithBrickField(BrickField.SIZE_Y).interpretInteger(scope);
        } catch (InterpretationException interpretationException) {
            // Формулы размера не числовые — показываем дефолтную сетку 32×32.
        }
        columns = Math.max(1, Math.min(columns, 2000));
        rows = Math.max(1, Math.min(rows, 2000));
        intent.putExtra(VisualPlacementActivity.EXTRA_GRID_COLUMNS, columns);
        intent.putExtra(VisualPlacementActivity.EXTRA_GRID_ROWS, rows);
        // Сетка не вращается — угол спрайта не нужен.
        intent.putExtra(SpriteActivity.EXTRA_ROTATION, 0f);
        return intent;
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
