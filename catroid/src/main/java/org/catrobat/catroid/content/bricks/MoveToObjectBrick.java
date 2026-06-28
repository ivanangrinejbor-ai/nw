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
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoveToObjectBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<Sprite> {
    private static final long serialVersionUID = 1L;
    private String targetObject;
    private transient BrickSpinner<Sprite> spinner;
    private transient Set<String> avoidSet = new HashSet<>();

    public MoveToObjectBrick() {
        addAllowedBrickField(BrickField.SPRITE, R.id.brick_move_to_avoid_edit);
        addAllowedBrickField(BrickField.SPEED, R.id.brick_move_to_speed_edit);
    }

    public MoveToObjectBrick(String targetObject, String avoidObjects, double speed) {
        this();
        this.targetObject = targetObject;
        setFormulaWithBrickField(BrickField.SPRITE, new Formula(avoidObjects));
        setFormulaWithBrickField(BrickField.SPEED, new Formula(speed));
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        MoveToObjectBrick clone = (MoveToObjectBrick) super.clone();
        clone.spinner = null;
        return clone;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_move_to_object;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        List<Nameable> items = new ArrayList<>();
        items.addAll(ProjectManager.getInstance().getCurrentlyEditedScene().getSpriteList());
        items.remove(ProjectManager.getInstance().getCurrentlyEditedScene().getBackgroundSprite());
        items.remove(ProjectManager.getInstance().getCurrentSprite());

        spinner = new BrickSpinner<>(R.id.brick_move_to_spinner, view, items);
        spinner.setOnItemSelectedListener(this);
        if (targetObject != null) {
            spinner.setSelection(targetObject);
        }

        TextView avoidEdit = view.findViewById(R.id.brick_move_to_avoid_edit);
        if (avoidEdit != null) {
            avoidEdit.setText(String.join(", ", avoidSet));
            avoidEdit.setEnabled(false);
        }

        Button avoidBtn = view.findViewById(R.id.brick_move_to_avoid_select);
        if (avoidBtn != null) {
            avoidBtn.setOnClickListener(v -> showAvoidDialog(context));
        }
        return view;
    }

    private void showAvoidDialog(Context context) {
        List<Sprite> sprites = new ArrayList<>(ProjectManager.getInstance().getCurrentlyEditedScene().getSpriteList());
        sprites.remove(ProjectManager.getInstance().getCurrentlyEditedScene().getBackgroundSprite());
        sprites.remove(ProjectManager.getInstance().getCurrentSprite());

        String[] names = new String[sprites.size()];
        boolean[] checked = new boolean[sprites.size()];
        for (int i = 0; i < sprites.size(); i++) {
            names[i] = sprites.get(i).getName();
            checked[i] = avoidSet.contains(names[i]);
        }

        new AlertDialog.Builder(context)
            .setTitle(R.string.pathfinder_avoid)
            .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                if (isChecked) {
                    avoidSet.add(names[which]);
                } else {
                    avoidSet.remove(names[which]);
                }
            })
            .setPositiveButton(R.string.ok, (dialog, which) -> {
                TextView avoidEdit = view.findViewById(R.id.brick_move_to_avoid_edit);
                if (avoidEdit != null) {
                    avoidEdit.setText(String.join(", ", avoidSet));
                }
                setFormulaWithBrickField(BrickField.SPRITE, new Formula(String.join(",", avoidSet)));
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
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
                .createMoveToObjectAction(sprite, sequence,
                        targetObject != null ? targetObject : "",
                        getFormulaWithBrickField(BrickField.SPRITE),
                        getFormulaWithBrickField(BrickField.SPEED)));
    }
}
