package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserList;

public class ListFilesInFolderBrick extends UserListBrick {

    private static final long serialVersionUID = 1L;

    public ListFilesInFolderBrick() {
        addAllowedBrickField(BrickField.VALUE, R.id.brick_list_files_in_folder_edit_text);
    }

    public ListFilesInFolderBrick(String folder) {
        this(new Formula(folder));
    }

    public ListFilesInFolderBrick(Formula folder) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, folder);
    }

    public ListFilesInFolderBrick(Formula folder, UserList userList) {
        this(folder);
        this.userList = userList;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_list_files_in_folder;
    }

    @Override
    protected int getSpinnerId() {
        return R.id.brick_list_files_in_folder_spinner;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createListFilesInFolderAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VALUE), userList));
    }
}
