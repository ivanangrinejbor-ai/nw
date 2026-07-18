/*
 * NeoCatroid
 * Copyright (C) 2026 The NeoCatroid Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class SecureReadVariableBrick extends UserVariableBrick {
	private static final long serialVersionUID = 1L;

	public SecureReadVariableBrick() {
	}

	public SecureReadVariableBrick(UserVariable userVariable) {
		this();
		this.userVariable = userVariable;
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_secure_read_variable;
	}

	@Override
	protected int getSpinnerId() {
		return R.id.secure_read_variable_spinner;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		if (userVariable == null || userVariable.getName() == null) {
			return;
		}
		sequence.addAction(sprite.getActionFactory().createSecureReadVariableAction(userVariable));
	}
}
