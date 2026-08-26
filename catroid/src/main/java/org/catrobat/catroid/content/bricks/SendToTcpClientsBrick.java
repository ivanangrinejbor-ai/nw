package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.brickspinner.BrickSpinner;
import org.catrobat.catroid.content.bricks.brickspinner.StringOption;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

public class SendToTcpClientsBrick extends FormulaBrick implements BrickSpinner.OnItemSelectedListener<StringOption> {

	private static final long serialVersionUID = 1L;

	private int visibleFields = 1;
	private int echoMode = 0;

	private transient BrickSpinner<StringOption> echoSpinner;

	public SendToTcpClientsBrick() {
		addAllowedBrickField(BrickField.VALUE, R.id.brick_send_tcp_clients_edit);
		addAllowedBrickField(BrickField.VALUE_2, R.id.brick_send_tcp_clients_edit2);
		addAllowedBrickField(BrickField.VALUE_3, R.id.brick_send_tcp_clients_edit3);
		addAllowedBrickField(BrickField.VALUE_4, R.id.brick_send_tcp_clients_edit4);
	}

	public SendToTcpClientsBrick(String value) {
		this(new Formula(value));
	}

	public SendToTcpClientsBrick(Formula value) {
		this();
		setFormulaWithBrickField(BrickField.VALUE, value);
	}

	public int getVisibleFields() {
		return visibleFields;
	}

	public void setVisibleFields(int visibleFields) {
		this.visibleFields = visibleFields;
	}

	public int getEchoMode() {
		return echoMode;
	}

	public void setEchoMode(int echoMode) {
		this.echoMode = echoMode;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_send_to_tcp_clients;
	}

	@Override
	public View getView(Context context) {
		View view = super.getView(context);
		view.findViewById(R.id.brick_send_tcp_clients_edit2)
				.setVisibility(visibleFields >= 2 ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.brick_send_tcp_clients_edit3)
				.setVisibility(visibleFields >= 3 ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.brick_send_tcp_clients_edit4)
				.setVisibility(visibleFields >= 4 ? View.VISIBLE : View.GONE);
		View addButton = view.findViewById(R.id.brick_send_tcp_clients_add);
		if (addButton != null) {
			addButton.setOnClickListener(click -> {
				if (visibleFields < 4) {
					visibleFields++;
					ElseIfSeparatorBrick.refreshScriptList(view);
				}
			});
		}
		List<org.catrobat.catroid.common.Nameable> echoItems = new ArrayList<>();
		echoItems.add(new StringOption(context.getString(R.string.brick_send_to_tcp_clients_echo_no)));
		echoItems.add(new StringOption(context.getString(R.string.brick_send_to_tcp_clients_echo_yes)));
		echoSpinner = new BrickSpinner<>(R.id.brick_send_tcp_clients_echo_spinner, view, echoItems);
		echoSpinner.setOnItemSelectedListener(this);
		echoSpinner.setSelection(echoMode == 1 ? 1 : 0);
		return view;
	}

	@Override
	public void onNewOptionSelected(Integer spinnerId) {}

	@Override
	public void onEditOptionSelected(Integer spinnerId) {}

	@Override
	public void onStringOptionSelected(Integer spinnerId, String string) {
		if (string == null || view == null) return;
		Context ctx = view.getContext();
		if (ctx.getString(R.string.brick_send_to_tcp_clients_echo_yes).equals(string)) {
			echoMode = 1;
		} else {
			echoMode = 0;
		}
	}

	@Override
	public void onItemSelected(Integer spinnerId, StringOption item) {}

	private List<Formula> getValues() {
		List<Formula> values = new ArrayList<>();
		values.add(getFormulaWithBrickField(BrickField.VALUE));
		if (visibleFields >= 2) {
			values.add(getFormulaWithBrickField(BrickField.VALUE_2));
		}
		if (visibleFields >= 3) {
			values.add(getFormulaWithBrickField(BrickField.VALUE_3));
		}
		if (visibleFields >= 4) {
			values.add(getFormulaWithBrickField(BrickField.VALUE_4));
		}
		return values;
	}

	@Override
	public Brick clone() throws CloneNotSupportedException {
		SendToTcpClientsBrick clone = (SendToTcpClientsBrick) super.clone();
		clone.echoSpinner = null;
		return clone;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSendToTcpClientsAction(sprite, sequence, getValues(), echoMode));
	}
}
