/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits)
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

public class WhenFirebaseChangedEventId extends EventId {
	final Formula bucket;
	final Formula path;

	public WhenFirebaseChangedEventId(Formula bucket, Formula path) {
		this.bucket = bucket;
		this.path = path;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof WhenFirebaseChangedEventId)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		WhenFirebaseChangedEventId that = (WhenFirebaseChangedEventId) o;

		String thisBucket = bucket != null ? bucket.getTrimmedFormulaString(null) : "";
		String thatBucket = that.bucket != null ? that.bucket.getTrimmedFormulaString(null) : "";
		String thisPath = path != null ? path.getTrimmedFormulaString(null) : "";
		String thatPath = that.path != null ? that.path.getTrimmedFormulaString(null) : "";

		return thisBucket.equals(thatBucket) && thisPath.equals(thatPath);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		String thisBucket = bucket != null ? bucket.getTrimmedFormulaString(null) : "";
		String thisPath = path != null ? path.getTrimmedFormulaString(null) : "";
		result = 31 * result + thisBucket.hashCode();
		result = 31 * result + thisPath.hashCode();
		return result;
	}
}
