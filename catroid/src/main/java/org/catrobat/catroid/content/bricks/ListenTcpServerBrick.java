package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Nameable;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.NewOption;
import org.catrobat.catroid.content.bricks.brickspinner.UserVariableBrickTextInputDialogBuilder;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.ui.UiUtils;
import org.catrobat.catroid.utils.LockUtils;
import org.catrobat.catroid.utils.ToastUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ListenTcpServerBrick extends FormulaBrick implements UserVariableBrickInterface {

	private static final long serialVersionUID = 1L;

	private UserVariable userVariable;
	private List<UserVariable> extraVariables = new ArrayList<>();
	private int visibleVariables = 1;

	private transient Map<Integer, BrickSpinner<UserVariable>> spinners = new HashMap<>();
	private transient Map<Integer, Integer> spinnerSlots = new HashMap<>();

	public ListenTcpServerBrick() {
	}

	public ListenTcpServerBrick(UserVariable userVariable) {
		this();
		this.userVariable = userVariable;
	}

	@Override
	public UserVariable getUserVariable() {
		return userVariable;
	}

	@Override
	public void setUserVariable(UserVariable userVariable) {
		this.userVariable = userVariable;
	}

	public UserVariable getVariable(int index) {
		if (index == 0) {
			return userVariable;
		}
		int extraIndex = index - 1;
		if (extraIndex < extraVariables.size()) {
			return extraVariables.get(extraIndex);
		}
		return null;
	}

	public void setVariable(int index, UserVariable variable) {
		if (index == 0) {
			userVariable = variable;
			return;
		}
		int extraIndex = index - 1;
		while (extraVariables.size() <= extraIndex) {
			extraVariables.add(null);
		}
		extraVariables.set(extraIndex, variable);
	}

	public int getVisibleVariables() {
		return visibleVariables;
	}

	public void setVisibleVariables(int visibleVariables) {
		this.visibleVariables = visibleVariables;
	}

	private List<UserVariable> getVariables() {
		List<UserVariable> variables = new ArrayList<>();
		for (int i = 0; i < visibleVariables; i++) {
			variables.add(getVariable(i));
		}
		return variables;
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

		int[] spinnerIds = {
				R.id.listen_tcp_spinner1,
				R.id.listen_tcp_spinner2,
				R.id.listen_tcp_spinner3,
				R.id.listen_tcp_spinner4,
				R.id.listen_tcp_spinner5,
				R.id.listen_tcp_spinner6,
				R.id.listen_tcp_spinner7,
				R.id.listen_tcp_spinner8,
				R.id.listen_tcp_spinner9,
				R.id.listen_tcp_spinner10,
				R.id.listen_tcp_spinner11,
				R.id.listen_tcp_spinner12
		};

		for (int i = 0; i < spinnerIds.length; i++) {
			View spinnerView = view.findViewById(spinnerIds[i]);
			if (i < visibleVariables) {
				spinnerView.setVisibility(View.VISIBLE);
				BrickSpinner<UserVariable> spinner = new BrickSpinner<>(spinnerIds[i], view, items);
				spinner.setOnItemSelectedListener(this);
				spinner.setSelection(getVariable(i));
				spinners.put(spinnerIds[i], spinner);
				spinnerSlots.put(spinnerIds[i], i);
			} else {
				spinnerView.setVisibility(View.GONE);
			}
		}

		View addButton = view.findViewById(R.id.brick_listen_tcp_add);
		if (addButton != null) {
			addButton.setOnClickListener(click -> {
				if (visibleVariables < spinnerIds.length) {
					visibleVariables++;
					int nextId = spinnerIds[visibleVariables - 1];
					View nextView = view.findViewById(nextId);
					if (nextView != null) {
						nextView.setVisibility(View.VISIBLE);
						// init spinner for newly visible slot
						BrickSpinner<UserVariable> spinner = new BrickSpinner<>(nextId, view, items);
						spinner.setOnItemSelectedListener(ListenTcpServerBrick.this);
						spinner.setSelection(getVariable(visibleVariables - 1));
						spinners.put(nextId, spinner);
						spinnerSlots.put(nextId, visibleVariables - 1);
					}
					if (visibleVariables >= spinnerIds.length) {
						click.setVisibility(View.GONE);
					}
					ElseIfSeparatorBrick.refreshScriptList(view);
				}
			});
			addButton.setVisibility(visibleVariables >= spinnerIds.length ? View.GONE : View.VISIBLE);
		}
		return view;
	}

	@Override
	public void onNewOptionSelected(Integer spinnerId) {
		AppCompatActivity activity = UiUtils.getActivityFromView(view);
		if (activity == null) {
			return;
		}
		BrickSpinner<UserVariable> spinner = spinners.get(spinnerId);
		int slot = spinnerSlots.containsKey(spinnerId) ? spinnerSlots.get(spinnerId) : 0;
		UserVariable current = getVariable(slot);
		Project currentProject = ProjectManager.getInstance().getCurrentProject();
		Sprite currentSprite = ProjectManager.getInstance().getCurrentSprite();

		UserVariableBrickTextInputDialogBuilder builder =
				new UserVariableBrickTextInputDialogBuilder(currentProject, currentSprite, current, activity, spinner);
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
		int slot = spinnerSlots.containsKey(spinnerId) ? spinnerSlots.get(spinnerId) : 0;
		UserVariable previous = getVariable(slot);
		if (previous != null && previous.isLocked()) {
			AppCompatActivity activity = UiUtils.getActivityFromView(view);
			if (activity != null) {
				LockUtils.requestPassword(activity, R.string.variable_locked_enter_password, password -> {
					if (previous.verifyLock(password)) {
						setVariable(slot, item);
					} else {
						ToastUtil.showError(activity, R.string.brick_wrong_password);
						BrickSpinner<UserVariable> spinner = spinners.get(spinnerId);
						if (spinner != null) {
							spinner.setSelection(previous);
						}
					}
				});
			}
			return;
		}
		setVariable(slot, item);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_listen_tcp_server;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createListenTcpServerAction(sprite, sequence, getVariables()));
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		ListenTcpServerBrick clone = (ListenTcpServerBrick) super.clone();
		clone.spinners = new HashMap<>();
		clone.spinnerSlots = new HashMap<>();
		return clone;
	}
}