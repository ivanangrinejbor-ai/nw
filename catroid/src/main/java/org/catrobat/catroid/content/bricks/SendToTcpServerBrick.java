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
		addAllowedBrickField(BrickField.VALUE_5, R.id.brick_send_tcp_edit5);
		addAllowedBrickField(BrickField.VALUE_6, R.id.brick_send_tcp_edit6);
		addAllowedBrickField(BrickField.VALUE_7, R.id.brick_send_tcp_edit7);
		addAllowedBrickField(BrickField.VALUE_8, R.id.brick_send_tcp_edit8);
		addAllowedBrickField(BrickField.VALUE_9, R.id.brick_send_tcp_edit9);
		addAllowedBrickField(BrickField.VALUE_10, R.id.brick_send_tcp_edit10);
		addAllowedBrickField(BrickField.VALUE_11, R.id.brick_send_tcp_edit11);
		addAllowedBrickField(BrickField.VALUE_12, R.id.brick_send_tcp_edit12);
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
		int[] editIds = {
				R.id.brick_send_tcp_edit2, R.id.brick_send_tcp_edit3, R.id.brick_send_tcp_edit4,
				R.id.brick_send_tcp_edit5, R.id.brick_send_tcp_edit6, R.id.brick_send_tcp_edit7,
				R.id.brick_send_tcp_edit8, R.id.brick_send_tcp_edit9, R.id.brick_send_tcp_edit10,
				R.id.brick_send_tcp_edit11, R.id.brick_send_tcp_edit12
		};
		for (int i = 0; i < editIds.length; i++) {
			View editView = view.findViewById(editIds[i]);
			if (editView != null) {
				editView.setVisibility(visibleFields >= (i + 2) ? View.VISIBLE : View.GONE);
			}
		}
		View addButton = view.findViewById(R.id.brick_send_tcp_add);
		if (addButton != null) {
			addButton.setOnClickListener(click -> {
				if (visibleFields < 12) {
					visibleFields++;
					int nextId = editIds[visibleFields - 2];
					View nextView = view.findViewById(nextId);
					if (nextView != null) {
						nextView.setVisibility(View.VISIBLE);
						try { setClickListeners(); } catch (Throwable ignored) {}
					}
					if (visibleFields >= 12) {
						click.setVisibility(View.GONE);
					}
					ElseIfSeparatorBrick.refreshScriptList(view);
				}
			});
			addButton.setVisibility(visibleFields >= 12 ? View.GONE : View.VISIBLE);
		}
		return view;
	}

	private List<Formula> getValues() {
		List<Formula> values = new ArrayList<>();
		BrickField[] fields = {
				BrickField.VALUE, BrickField.VALUE_2, BrickField.VALUE_3, BrickField.VALUE_4,
				BrickField.VALUE_5, BrickField.VALUE_6, BrickField.VALUE_7, BrickField.VALUE_8,
				BrickField.VALUE_9, BrickField.VALUE_10, BrickField.VALUE_11, BrickField.VALUE_12
		};
		for (int i = 0; i < visibleFields && i < fields.length; i++) {
			values.add(getFormulaWithBrickField(fields[i]));
		}
		return values;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createSendToTcpServerAction(sprite, sequence, getValues()));
	}
}