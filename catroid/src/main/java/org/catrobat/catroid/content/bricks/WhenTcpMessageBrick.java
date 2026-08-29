package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenTcpMessageScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.NewOption;
import org.catrobat.catroid.content.bricks.brickspinner.UserVariableBrickTextInputDialogBuilder;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WhenTcpMessageBrick extends FormulaBrick implements ScriptBrick, UserVariableBrickInterface,
		BrickSpinner.OnItemSelectedListener<UserVariable> {

	private static final long serialVersionUID = 1L;

	private UserVariable userVariable;
	private WhenTcpMessageScript script;

	private transient BrickSpinner<UserVariable> spinner;

	public WhenTcpMessageBrick() {
		this(new WhenTcpMessageScript());
	}

	public WhenTcpMessageBrick(WhenTcpMessageScript script) {
		script.setScriptBrick(this);
		commentedOut = script.isCommentedOut();
		this.script = script;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		WhenTcpMessageBrick clone = (WhenTcpMessageBrick) super.clone();
		clone.script = (WhenTcpMessageScript) script.clone();
		clone.script.setScriptBrick(clone);
		clone.spinner = null;
		return clone;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_when_tcp_message;
	}

	@Override
	public View getView(Context context) {
		View view = super.getView(context);

		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		List<Nameable> items = new ArrayList<>();
		items.add(new NewOption(context.getString(R.string.new_option)));
		if (sprite != null) {
			items.addAll(sprite.getUserVariables());
		}
		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project != null) {
			items.addAll(project.getUserVariables());
			items.addAll(project.getMultiplayerVariables());
		}

		spinner = new BrickSpinner<>(R.id.brick_when_tcp_message_spinner, view, items);
		spinner.setOnItemSelectedListener(this);
		spinner.setSelection(userVariable);

		return view;
	}

	@Override
	public void onNewOptionSelected(Integer spinnerId) {
		AppCompatActivity activity = UiUtils.getActivityFromView(view);
		if (activity == null) {
			return;
		}
		Project currentProject = ProjectManager.getInstance().getCurrentProject();
		Sprite currentSprite = ProjectManager.getInstance().getCurrentSprite();
		UserVariableBrickTextInputDialogBuilder builder =
				new UserVariableBrickTextInputDialogBuilder(currentProject, currentSprite, userVariable, activity, spinner);
		builder.show();
	}

	@Override
	public void onEditOptionSelected(Integer spinnerId) {
	}

	@Override
	public void onStringOptionSelected(Integer spinnerId, String string) {
	}

	@Override
	public void onItemSelected(Integer spinnerId, @Nullable UserVariable item) {
		userVariable = item;
	}

	@Override
	public UserVariable getUserVariable() {
		return userVariable;
	}

	@Override
	public void setUserVariable(UserVariable userVariable) {
		this.userVariable = userVariable;
	}

	public WhenTcpMessageScript getWhenTcpMessageScript() {
		return script;
	}

	@Override
	public Script getScript() {
		return script;
	}

	@Override
	public int getPositionInScript() {
		return -1;
	}

	@Override
	public void addToFlatList(List<Brick> bricks) {
		super.addToFlatList(bricks);
		for (Brick brick : getScript().getBrickList()) {
			brick.addToFlatList(bricks);
		}
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return getScript().getBrickList();
	}

	@Override
	public int getPositionInDragAndDropTargetList() {
		return -1;
	}

	@Override
	public void setCommentedOut(boolean commentedOut) {
		super.setCommentedOut(commentedOut);
		getScript().setCommentedOut(commentedOut);
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
	}
}
