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

public class UploadFileToFirebaseBrick extends FormulaBrick {

    private static final long serialVersionUID = 1L;

    public UploadFileToFirebaseBrick() {
        addAllowedBrickField(BrickField.FIREBASE_BUCKET, R.id.brick_upload_firebase_bucket);
        addAllowedBrickField(BrickField.FIREBASE_STORAGE_PATH, R.id.brick_upload_firebase_path);
        addAllowedBrickField(BrickField.FILE, R.id.brick_upload_firebase_file);
    }

    public UploadFileToFirebaseBrick(String bucket, String path, String localFile) {
        this(new Formula(bucket), new Formula(path), new Formula(localFile));
    }

    public UploadFileToFirebaseBrick(Formula bucket, Formula path, Formula localFile) {
        this();
        setFormulaWithBrickField(BrickField.FIREBASE_BUCKET, bucket);
        setFormulaWithBrickField(BrickField.FIREBASE_STORAGE_PATH, path);
        setFormulaWithBrickField(BrickField.FILE, localFile);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_upload_firebase;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createUploadFileToFirebaseAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.FIREBASE_BUCKET),
                getFormulaWithBrickField(BrickField.FIREBASE_STORAGE_PATH),
                getFormulaWithBrickField(BrickField.FILE)));
    }
}
