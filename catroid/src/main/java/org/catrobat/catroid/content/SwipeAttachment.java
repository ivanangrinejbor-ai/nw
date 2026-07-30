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
package org.catrobat.catroid.content;

import java.io.Serializable;

/**
 * A child sprite attached to a swipeable parent sprite. The offset is measured in Catroid
 * user-interface dimension units relative to the parent's centre; attached children follow the
 * parent while it is dragged and fly off together with it.
 */
public class SwipeAttachment implements Serializable {

	private static final long serialVersionUID = 1L;

	private String childSpriteName;
	private float offsetX;
	private float offsetY;

	public SwipeAttachment() {
	}

	public SwipeAttachment(String childSpriteName, float offsetX, float offsetY) {
		this.childSpriteName = childSpriteName;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
	}

	public String getChildSpriteName() {
		return childSpriteName;
	}

	public void setChildSpriteName(String childSpriteName) {
		this.childSpriteName = childSpriteName;
	}

	public float getOffsetX() {
		return offsetX;
	}

	public void setOffsetX(float offsetX) {
		this.offsetX = offsetX;
	}

	public float getOffsetY() {
		return offsetY;
	}

	public void setOffsetY(float offsetY) {
		this.offsetY = offsetY;
	}
}
