package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.FormulaSpannableStringBuilder;
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.ui.UiUtils;
import org.catrobat.catroid.ui.fragment.FormulaEditorFragment;

import java.util.List;
import java.util.UUID;

import androidx.fragment.app.FragmentActivity;

/**
 * Маркер-брик "Иначе если" — отображается в плоском списке скриптов.
 * Не сериализуется отдельно (transient в IfLogicBeginBrick).
 * Хранит ссылку на родительский IfLogicBeginBrick и индекс ветки.
 */
public class ElseIfBrick extends BrickBaseType implements View.OnClickListener {

	private final IfLogicBeginBrick ifBrick;
	private final int branchIndex;

	public ElseIfBrick(IfLogicBeginBrick ifBrick, int branchIndex) {
		this.ifBrick = ifBrick;
		this.branchIndex = branchIndex;
		parent = ifBrick;
	}

	public int getBranchIndex() {
		return branchIndex;
	}

	public Formula getCondition() {
		return ifBrick.getElseIfCondition(branchIndex);
	}

	@Override
	public boolean isCommentedOut() {
		return ifBrick.isCommentedOut();
	}

	@Override
	public boolean consistsOfMultipleParts() {
		return true;
	}

	@Override
	public List<Brick> getAllParts() {
		return ifBrick.getAllParts();
	}

	@Override
	public void addToFlatList(List<Brick> bricks) {
		ifBrick.addToFlatList(bricks);
	}

	@Override
	public List<Brick> getDragAndDropTargetList() {
		return ifBrick.getElseIfBranch(branchIndex);
	}

	@Override
	public int getPositionInDragAndDropTargetList() {
		return -1;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_else_if;
	}

	@Override
	public View getView(Context context) {
		super.getView(context);
		TextView formulaView = view.findViewById(R.id.brick_else_if_edit_text);
		Formula condition = getCondition();
		if (condition != null) {
			String text = condition.clone().getTrimmedFormulaString(context);
			formulaView.setText(
					FormulaSpannableStringBuilder.buildSpannableFormulaString(
							context, text, formulaView.getTextSize()),
					TextView.BufferType.SPANNABLE);
		}
		formulaView.setOnClickListener(this);
		return view;
	}

	@Override
	public void onClick(View v) {
		// Открыть FormulaEditor для условия этой else-if ветки
		if (v.getId() == R.id.brick_else_if_edit_text) {
			FragmentActivity activity = (FragmentActivity) UiUtils.getActivityFromView(view);
			if (activity != null) {
				FormulaEditorFragment.showFragment(activity, ifBrick, BrickField.IF_CONDITION);
			}
		}
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		// Маркер — не добавляет action самостоятельно
	}

	@Override
	public UUID getBrickID() {
		return ifBrick.getBrickID();
	}
}
