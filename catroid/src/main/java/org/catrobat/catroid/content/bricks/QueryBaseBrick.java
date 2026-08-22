/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;

public class QueryBaseBrick extends UserVariableBrickWithFormula {

	private static final long serialVersionUID = 1L;

	public QueryBaseBrick() {
		addAllowedBrickField(BrickField.FIREBASE_ID, R.id.brick_query_base_edit_base);
		addAllowedBrickField(BrickField.FIREBASE_KEY, R.id.brick_query_base_edit_key);
		addAllowedBrickField(BrickField.FIREBASE_QUERY_ORDER, R.id.brick_query_base_edit_order);
		addAllowedBrickField(BrickField.FIREBASE_QUERY_LIMIT, R.id.brick_query_base_edit_limit);
		addAllowedBrickField(BrickField.FIREBASE_QUERY_EQUAL, R.id.brick_query_base_edit_equal);
	}

	public QueryBaseBrick(String base, String key, String orderBy, String limit, String equalTo) {
		this(new Formula(base), new Formula(key), new Formula(orderBy), new Formula(limit), new Formula(equalTo));
	}

	public QueryBaseBrick(Formula base, Formula key, Formula orderBy, Formula limit, Formula equalTo) {
		this();
		setFormulaWithBrickField(BrickField.FIREBASE_ID, base);
		setFormulaWithBrickField(BrickField.FIREBASE_KEY, key);
		setFormulaWithBrickField(BrickField.FIREBASE_QUERY_ORDER, orderBy);
		setFormulaWithBrickField(BrickField.FIREBASE_QUERY_LIMIT, limit);
		setFormulaWithBrickField(BrickField.FIREBASE_QUERY_EQUAL, equalTo);
	}

	@Override
	public int getViewResource() {
		return R.layout.brick_query_base;
	}

	@Override
	protected int getSpinnerId() {
		return R.id.brick_query_base_spinner;
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		sequence.addAction(sprite.getActionFactory().createQueryBaseAction(sprite, sequence,
				getFormulaWithBrickField(BrickField.FIREBASE_ID), getFormulaWithBrickField(BrickField.FIREBASE_KEY),
				getFormulaWithBrickField(BrickField.FIREBASE_QUERY_ORDER),
				getFormulaWithBrickField(BrickField.FIREBASE_QUERY_LIMIT),
				getFormulaWithBrickField(BrickField.FIREBASE_QUERY_EQUAL), userVariable));
	}
}