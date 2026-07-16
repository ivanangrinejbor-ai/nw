/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class ListFirebaseFilesBrick extends UserVariableBrickWithFormula {

    private static final long serialVersionUID = 1L;

    public ListFirebaseFilesBrick() {
        addAllowedBrickField(BrickField.FIREBASE_BUCKET, R.id.brick_list_firebase_bucket);
        addAllowedBrickField(BrickField.FIREBASE_STORAGE_PATH, R.id.brick_list_firebase_prefix);
    }

    public ListFirebaseFilesBrick(String bucket, String prefix) {
        this(new Formula(bucket), new Formula(prefix));
    }

    public ListFirebaseFilesBrick(Formula bucket, Formula prefix, UserVariable variable) {
        this(bucket, prefix);
        this.userVariable = variable;
    }

    private ListFirebaseFilesBrick(Formula bucket, Formula prefix) {
        this();
        setFormulaWithBrickField(BrickField.FIREBASE_BUCKET, bucket);
        setFormulaWithBrickField(BrickField.FIREBASE_STORAGE_PATH, prefix);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_list_firebase;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_list_firebase_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createListFirebaseFilesAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.FIREBASE_BUCKET),
                getFormulaWithBrickField(BrickField.FIREBASE_STORAGE_PATH),
                userVariable));
    }
}
