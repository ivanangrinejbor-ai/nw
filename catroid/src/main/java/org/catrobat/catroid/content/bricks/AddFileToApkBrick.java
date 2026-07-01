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

public class AddFileToApkBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public AddFileToApkBrick() {
        addAllowedBrickField(BrickField.TEXT_1, R.id.brick_add_file_apk_edit_apk);
        addAllowedBrickField(BrickField.TEXT_2, R.id.brick_add_file_apk_edit_src);
        addAllowedBrickField(BrickField.TEXT_3, R.id.brick_add_file_apk_edit_dest);
    }

    public AddFileToApkBrick(String apk, String src, String dest) {
        this(new Formula(apk), new Formula(src), new Formula(dest));
    }

    public AddFileToApkBrick(Formula apk, Formula src, Formula dest) {
        this();
        setFormulaWithBrickField(BrickField.TEXT_1, apk);
        setFormulaWithBrickField(BrickField.TEXT_2, src);
        setFormulaWithBrickField(BrickField.TEXT_3, dest);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_add_file_apk;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createAddFileToApkAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.TEXT_1),
                        getFormulaWithBrickField(BrickField.TEXT_2),
                        getFormulaWithBrickField(BrickField.TEXT_3)));
    }
}