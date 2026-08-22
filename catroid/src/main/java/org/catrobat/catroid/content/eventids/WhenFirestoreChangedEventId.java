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

package org.catrobat.catroid.content.eventids;

import org.catrobat.catroid.formulaeditor.Formula;

public class WhenFirestoreChangedEventId extends EventId {
	final Formula path;
	final Formula base;

	public WhenFirestoreChangedEventId(Formula path) {
		this(path, null);
	}

	public WhenFirestoreChangedEventId(Formula path, Formula base) {
		this.path = path;
		this.base = base;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof WhenFirestoreChangedEventId)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		WhenFirestoreChangedEventId that = (WhenFirestoreChangedEventId) o;

		String thisPath = path != null ? path.getTrimmedFormulaString(null) : "";
		String thatPath = that.path != null ? that.path.getTrimmedFormulaString(null) : "";
		if (!thisPath.equals(thatPath)) {
			return false;
		}

		String thisBase = base != null ? base.getTrimmedFormulaString(null) : "";
		String thatBase = that.base != null ? that.base.getTrimmedFormulaString(null) : "";
		return thisBase.equals(thatBase);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		String thisPath = path != null ? path.getTrimmedFormulaString(null) : "";
		result = 31 * result + thisPath.hashCode();
		String thisBase = base != null ? base.getTrimmedFormulaString(null) : "";
		result = 31 * result + thisBase.hashCode();
		return result;
	}
}