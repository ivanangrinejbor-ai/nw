package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import java.util.ArrayList;
import java.util.List;

public class SendToTcpServerBrick extends FormulaBrick {

	private static final long serialVersionUID = 1L;

	private int visibleFields = 1;

	public SendToTcpServerBrick() {
		addAllowedBrickField(BrickField.VALUE, R.id.brick_send_tcp_edit);
		addAllowedBrickField(BrickField.VALUE_2, R.id.brick_send_tcp_edit2);
		addAllowedBrickField(BrickField.VALUE_3, R.id.brick_send_tcp_edit3);
		addAllowedBrickField(BrickField.VALUE_4, R.id.brick_send_tcp_edit4);
	}

	public SendToTcpServerBrick(String value) {
		this(new Formula(value));
	}

	public SendToTcpServerBrick(Formula value) {
		this();
		setFormulaWithBrickField(BrickField.VALUE, value);
	}

	public int getVisibleFields() {
		return visibleFields;
	}

	public void setVisibleFields(int visibleFields) {
		this.visibleFields = visibleFields;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_send_to_tcp_server;
	}

	@Override
	public View getView(Context context) {
		View view = super.getView(context);
		view.findViewById(R.id.brick_send_tcp_edit2)
				.setVisibility(visibleFields >= 2 ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.brick_send_tcp_edit3)
				.setVisibility(visibleFields >= 3 ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.brick_send_tcp_edit4)
				.setVisibility(visibleFields >= 4 ? View.VISIBLE : View.GONE);
		View addButton = view.findViewById(R.id.brick_send_tcp_add);
		if (addButton != null) {
			addButton.setOnClickListener(click -> {
				if (visibleFields < 4) {
					visibleFields++;
					ElseIfSeparatorBrick.refreshScriptList(view);
				}
			});
		}
		return view;
	}

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
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSendToTcpServerAction(sprite, sequence, getValues()));
	}
}