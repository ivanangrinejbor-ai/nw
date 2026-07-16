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

public class DownloadFileFromFirebaseBrick extends UserVariableBrickWithFormula {

    private static final long serialVersionUID = 1L;

    public DownloadFileFromFirebaseBrick() {
        addAllowedBrickField(BrickField.FIREBASE_BUCKET, R.id.brick_download_firebase_bucket);
        addAllowedBrickField(BrickField.FIREBASE_STORAGE_PATH, R.id.brick_download_firebase_path);
        addAllowedBrickField(BrickField.DOWNLOAD_PATH, R.id.brick_download_firebase_dest);
    }

    public DownloadFileFromFirebaseBrick(String bucket, String path, String dest) {
        this(new Formula(bucket), new Formula(path), new Formula(dest));
    }

    public DownloadFileFromFirebaseBrick(Formula bucket, Formula path, Formula dest, UserVariable variable) {
        this(bucket, path, dest);
        this.userVariable = variable;
    }

    private DownloadFileFromFirebaseBrick(Formula bucket, Formula path, Formula dest) {
        this();
        setFormulaWithBrickField(BrickField.FIREBASE_BUCKET, bucket);
        setFormulaWithBrickField(BrickField.FIREBASE_STORAGE_PATH, path);
        setFormulaWithBrickField(BrickField.DOWNLOAD_PATH, dest);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_download_firebase;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_download_firebase_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createDownloadFileFromFirebaseAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.FIREBASE_BUCKET),
                getFormulaWithBrickField(BrickField.FIREBASE_STORAGE_PATH),
                getFormulaWithBrickField(BrickField.DOWNLOAD_PATH),
                userVariable));
    }
}
