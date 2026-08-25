package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class CameraBoundsBrick extends FormulaBrick {
	private static final long serialVersionUID = 1L;

	public CameraBoundsBrick() {
		addAllowedBrickField(BrickField.X1, R.id.brick_camera_bounds_min_x_edit);
		addAllowedBrickField(BrickField.Y1, R.id.brick_camera_bounds_min_y_edit);
		addAllowedBrickField(BrickField.X2, R.id.brick_camera_bounds_max_x_edit);
		addAllowedBrickField(BrickField.Y2, R.id.brick_camera_bounds_max_y_edit);
	}

	public CameraBoundsBrick(float minX, float minY, float maxX, float maxY) {
		this(new Formula(minX), new Formula(minY), new Formula(maxX), new Formula(maxY));
	}

	public CameraBoundsBrick(Formula minX, Formula minY, Formula maxX, Formula maxY) {
		this();
		setFormulaWithBrickField(BrickField.X1, minX);
		setFormulaWithBrickField(BrickField.Y1, minY);
		setFormulaWithBrickField(BrickField.X2, maxX);
		setFormulaWithBrickField(BrickField.Y2, maxY);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_camera_bounds;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createCameraBoundsAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.X1),
				getFormulaWithBrickField(BrickField.Y1),
				getFormulaWithBrickField(BrickField.X2),
				getFormulaWithBrickField(BrickField.Y2)));
	}
}
