/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2025 The Catrobat Team
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

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenTouchingSpriteByNameBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.TouchingSpriteEventId;

public class WhenTouchingSpriteByNameScript extends Script {

	private static final long serialVersionUID = 1L;

	private String spriteToTouchName = "";
	private boolean reactToBackground = false;

	public WhenTouchingSpriteByNameScript() {
	}

	public WhenTouchingSpriteByNameScript(String spriteToTouchName) {
		setSpriteToTouchName(spriteToTouchName);
	}

	public WhenTouchingSpriteByNameScript(String spriteToTouchName, boolean reactToBackground) {
		setSpriteToTouchName(spriteToTouchName);
		this.reactToBackground = reactToBackground;
	}

	public String getSpriteToTouchName() {
		return spriteToTouchName;
	}

	public void setSpriteToTouchName(String spriteToTouchName) {
		if (spriteToTouchName == null) {
			this.spriteToTouchName = "";
		} else {
			this.spriteToTouchName = spriteToTouchName;
		}
	}

	public boolean isReactToBackground() {
		return reactToBackground;
	}

	public void setReactToBackground(boolean reactToBackground) {
		this.reactToBackground = reactToBackground;
	}

	@Override
	public Script clone() throws CloneNotSupportedException {
		WhenTouchingSpriteByNameScript clone = (WhenTouchingSpriteByNameScript) super.clone();
		clone.spriteToTouchName = spriteToTouchName;
		clone.reactToBackground = reactToBackground;
		return clone;
	}

	@Override
	public ScriptBrick getScriptBrick() {
		if (scriptBrick == null) {
			scriptBrick = new WhenTouchingSpriteByNameBrick(this);
		}
		return scriptBrick;
	}

	@Override
	public EventId createEventId(Sprite sprite) {
		return new TouchingSpriteEventId(sprite, spriteToTouchName);
	}
}
