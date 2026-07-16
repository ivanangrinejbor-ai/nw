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

public class DeleteFirebaseFileBrick extends FormulaBrick {

    private static final long serialVersionUID = 1L;

    public DeleteFirebaseFileBrick() {
        addAllowedBrickField(BrickField.FIREBASE_BUCKET, R.id.brick_delete_firebase_bucket);
        addAllowedBrickField(BrickField.FIREBASE_STORAGE_PATH, R.id.brick_delete_firebase_path);
    }

    public DeleteFirebaseFileBrick(String bucket, String path) {
        this(new Formula(bucket), new Formula(path));
    }

    public DeleteFirebaseFileBrick(Formula bucket, Formula path) {
        this();
        setFormulaWithBrickField(BrickField.FIREBASE_BUCKET, bucket);
        setFormulaWithBrickField(BrickField.FIREBASE_STORAGE_PATH, path);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_delete_firebase;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createDeleteFirebaseFileAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.FIREBASE_BUCKET),
                getFormulaWithBrickField(BrickField.FIREBASE_STORAGE_PATH)));
    }
}
