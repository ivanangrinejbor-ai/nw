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

import android.content.Context;
import android.view.View;

import androidx.annotation.Nullable;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

public class HasPathBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<Sprite> {
    private static final long serialVersionUID = 1L;
    private String targetObject;
    private transient BrickSpinner<Sprite> spinner;

    public HasPathBrick() {
        addAllowedBrickField(BrickField.VARIABLE, R.id.brick_has_path_var_edit);
    }

    public HasPathBrick(String targetObject, String variableName) {
        this();
        this.targetObject = targetObject;
        setFormulaWithBrickField(BrickField.VARIABLE, new Formula(variableName));
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        HasPathBrick clone = (HasPathBrick) super.clone();
        clone.spinner = null;
        return clone;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_has_path;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        List<Nameable> items = new ArrayList<>();
        items.addAll(ProjectManager.getInstance().getCurrentlyEditedScene().getSpriteList());
        items.remove(ProjectManager.getInstance().getCurrentlyEditedScene().getBackgroundSprite());
        items.remove(ProjectManager.getInstance().getCurrentSprite());

        spinner = new BrickSpinner<>(R.id.brick_has_path_spinner, view, items);
        spinner.setOnItemSelectedListener(this);
        if (targetObject != null) {
            spinner.setSelection(targetObject);
        }
        return view;
    }

    @Override
    public void onNewOptionSelected(Integer spinnerId) {
    }

    @Override
    public void onEditOptionSelected(Integer spinnerId) {
    }

    @Override
    public void onStringOptionSelected(Integer spinnerId, String string) {
    }

    @Override
    public void onItemSelected(Integer spinnerId, @Nullable Sprite item) {
        if (item != null) {
            targetObject = item.getName();
        }
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createHasPathAction(sprite, sequence,
                        targetObject != null ? targetObject : "",
                        getFormulaWithBrickField(BrickField.VARIABLE)));
    }
}
