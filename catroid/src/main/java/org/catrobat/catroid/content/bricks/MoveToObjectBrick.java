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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoveToObjectBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<Sprite> {
    private static final long serialVersionUID = 1L;
    private String targetObject;
    private int moveMode = 0;
    private int sizeCheckMode = 0; // 0 = ignore size, 1 = check fit
    private int blockedPathAction = 0; // 0 = don't go, 1 = stop where blocked
    private transient BrickSpinner<Sprite> spinner;
    private Set<String> avoidSet = new HashSet<>();

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

    public MoveToObjectBrick(String targetObject, String avoidObjects, double speed, int moveMode, int sizeCheckMode, int blockedPathAction) {
        this(targetObject, avoidObjects, speed);
        this.moveMode = moveMode;
        this.sizeCheckMode = sizeCheckMode;
        this.blockedPathAction = blockedPathAction;
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

        Spinner modeSpinner = view.findViewById(R.id.brick_move_to_mode_spinner);
        if (modeSpinner != null) {
            String[] modeItems = new String[] {
                context.getString(R.string.pathfinder_mode_precise),
                context.getString(R.string.pathfinder_mode_touch)
            };
            ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, modeItems);
            modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            modeSpinner.setAdapter(modeAdapter);
            modeSpinner.setSelection(moveMode);
            modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    moveMode = position;
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        Spinner sizeSpinner = view.findViewById(R.id.brick_move_to_size_spinner);
        if (sizeSpinner != null) {
            String[] sizeItems = new String[] {
                context.getString(R.string.pathfinder_size_ignore),
                context.getString(R.string.pathfinder_size_check_fit)
            };
            ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, sizeItems);
            sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sizeSpinner.setAdapter(sizeAdapter);
            sizeSpinner.setSelection(sizeCheckMode);
            sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    sizeCheckMode = position;
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        Spinner blockedSpinner = view.findViewById(R.id.brick_move_to_blocked_spinner);
        if (blockedSpinner != null) {
            String[] blockedItems = new String[] {
                context.getString(R.string.pathfinder_blocked_dont_go),
                context.getString(R.string.pathfinder_blocked_stop_there)
            };
            ArrayAdapter<String> blockedAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, blockedItems);
            blockedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            blockedSpinner.setAdapter(blockedAdapter);
            blockedSpinner.setSelection(blockedPathAction);
            blockedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    blockedPathAction = position;
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
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

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(avoidSet.size());
        for (String s : avoidSet) {
            out.writeObject(s);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int size = in.readInt();
        avoidSet = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            avoidSet.add((String) in.readObject());
        }
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createMoveToObjectAction(sprite, sequence,
                        targetObject != null ? targetObject : "",
                        getFormulaWithBrickField(BrickField.SPRITE),
                        getFormulaWithBrickField(BrickField.SPEED),
                        moveMode,
                        sizeCheckMode,
                        blockedPathAction));
    }
}
