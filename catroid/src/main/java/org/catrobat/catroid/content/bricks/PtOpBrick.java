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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PtOpBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int opSelection = 0;

    public PtOpBrick() {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_pt_op_res);
        addAllowedBrickField(BrickField.VALUE_2, R.id.brick_pt_op_a);
        addAllowedBrickField(BrickField.VALUE_3, R.id.brick_pt_op_b);
    }

    public PtOpBrick(String res, String a, String b) {
        this(new Formula(res), new Formula(a), new Formula(b));
    }

    public PtOpBrick(Formula res, Formula a, Formula b) {
        this();
        setFormulaWithBrickField(BrickField.VALUE_1, res);
        setFormulaWithBrickField(BrickField.VALUE_2, a);
        setFormulaWithBrickField(BrickField.VALUE_3, b);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_pt_op;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);
        Spinner spinner = view.findViewById(R.id.brick_pt_op_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.pt_ops_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(opSelection);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                opSelection = position;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        String[] ops = CatroidApplication.getAppContext().getResources().getStringArray(R.array.pt_ops_array);
        sequence.addAction(sprite.getActionFactory().createPtOpAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE_1),
                getFormulaWithBrickField(BrickField.VALUE_2),
                getFormulaWithBrickField(BrickField.VALUE_3),
                ops[opSelection]
        ));
    }
}