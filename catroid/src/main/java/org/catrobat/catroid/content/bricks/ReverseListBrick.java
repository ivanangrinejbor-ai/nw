package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.NewOption;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.ui.UiUtils;
import org.catrobat.catroid.ui.recyclerview.dialog.TextInputDialog;
import org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment;
import org.catrobat.catroid.utils.AddUserListDialog;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ReverseListBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<UserList> {

    private static final long serialVersionUID = 1L;

    private UserList userList;
    private transient BrickSpinner<UserList> listSpinner;

    public ReverseListBrick() {
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_reverse_list;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);
        Sprite sprite = ProjectManager.getInstance().getCurrentSprite();

        List<Nameable> listItems = new ArrayList<>();
        listItems.add(new NewOption(context.getString(R.string.new_option)));
        if (sprite != null) {
            listItems.addAll(sprite.getUserLists());
        }
        listItems.addAll(ProjectManager.getInstance().getCurrentProject().getUserLists());

        listSpinner = new BrickSpinner<>(R.id.reverse_list_spinner, view, listItems);
        listSpinner.setOnItemSelectedListener(this);
        listSpinner.setSelection(userList);

        return view;
    }

    @Override
    public void onNewOptionSelected(Integer spinnerId) {
        final AppCompatActivity activity = UiUtils.getActivityFromView(view);
        if (activity == null) return;

        TextInputDialog.Builder builder = new TextInputDialog.Builder(activity);
        AddUserListDialog userListDialog = new AddUserListDialog(builder, listSpinner);
        userListDialog.show(activity.getString(R.string.data_label), activity.getString(R.string.ok), new AddUserListDialog.Callback() {
            @Override
            public void onPositiveButton(DialogInterface dialog, String textInput) {
                UserList userList = new UserList(textInput);
                userListDialog.addUserList(dialog, userList,
                        ProjectManager.getInstance().getCurrentProject().getUserLists(),
                        ProjectManager.getInstance().getCurrentSprite().getUserLists());
                listSpinner.add(userList);
                listSpinner.setSelection(userList);
                ReverseListBrick.this.userList = userList;
                ScriptFragment parentFragment = (ScriptFragment) activity.getSupportFragmentManager().findFragmentByTag(ScriptFragment.TAG);
                if (parentFragment != null) parentFragment.notifyDataSetChanged();
            }

            @Override
            public void onNegativeButton() {
                listSpinner.setSelection(ReverseListBrick.this.userList);
            }
        });
    }

    @Override
    public void onEditOptionSelected(Integer spinnerId) {
    }

    @Override
    public void onStringOptionSelected(Integer spinnerId, String string) {
    }

    @Override
    public void onItemSelected(Integer spinnerId, @Nullable UserList item) {
        userList = item;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createReverseListAction(userList));
    }
}
