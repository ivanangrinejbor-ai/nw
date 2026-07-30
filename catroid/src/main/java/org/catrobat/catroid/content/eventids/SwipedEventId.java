/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

import com.google.common.base.Objects;

public class SwipedEventId extends EventId {

	// 0 = Up, 1 = Down, 2 = Left, 3 = Right, 4 = Any
	public static final int UP = 0;
	public static final int DOWN = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;
	public static final int ANY = 4;

	private final int direction;

	public SwipedEventId(int direction) {
		super(EventId.SWIPED);
		this.direction = direction;
	}

	public int getDirection() {
		return direction;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SwipedEventId)) {
			return false;
		}
		SwipedEventId that = (SwipedEventId) o;
		return direction == that.direction;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), direction);
	}
}
