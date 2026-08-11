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

import com.google.common.base.Objects;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.eventids.TouchingSpriteEventId;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.stage.StageListener;

import java.util.List;

public class TouchingSpriteTrigger {

	static final int TRIGGER_NOW = 0;
	static final int ALREADY_TRIGGERED = 1;

	private final String touchedSpriteName;
	private final boolean reactToBackground;

	private int status = TRIGGER_NOW;

	TouchingSpriteTrigger(String touchedSpriteName, boolean reactToBackground) {
		this.touchedSpriteName = touchedSpriteName == null ? "" : touchedSpriteName;
		this.reactToBackground = reactToBackground;
	}

	void evaluateAndTriggerActions(Sprite sprite) {
		if (isTouching(sprite)) {
			if (status == TRIGGER_NOW) {
				EventWrapper eventWrapper = new EventWrapper(new TouchingSpriteEventId(sprite, touchedSpriteName), false);
				sprite.look.fire(eventWrapper);
				status = ALREADY_TRIGGERED;
			}
		} else {
			status = TRIGGER_NOW;
		}
	}

	private boolean isTouching(Sprite self) {
		if (self == null || self.look == null || !self.look.isVisible()) {
			return false;
		}

		Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
		if (scene == null) {
			return false;
		}
		Sprite background = scene.getBackgroundSprite();

		for (Sprite other : getStageSprites()) {
			if (other == null || other == self || other.look == null || !other.look.isVisible()) {
				continue;
			}
			if (!reactToBackground && touchedSpriteName.isEmpty() && other == background) {
				continue;
			}
			if (!matchesTargetName(other)) {
				continue;
			}
			if (overlap(self.look, other.look)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesTargetName(Sprite other) {
		if (touchedSpriteName.isEmpty()) {
			return true;
		}
		if (touchedSpriteName.equals(other.getName())) {
			return true;
		}
		return other.isClone && other.myOriginal != null && touchedSpriteName.equals(other.myOriginal.getName());
	}

	private boolean overlap(Look selfLook, Look otherLook) {
		float dx = Math.abs(selfLook.getXInUserInterfaceDimensionUnit() - otherLook.getXInUserInterfaceDimensionUnit());
		float dy = Math.abs(selfLook.getYInUserInterfaceDimensionUnit() - otherLook.getYInUserInterfaceDimensionUnit());
		float halfWidths = (selfLook.getWidthInUserInterfaceDimensionUnit() + otherLook.getWidthInUserInterfaceDimensionUnit()) / 2f;
		float halfHeights = (selfLook.getHeightInUserInterfaceDimensionUnit() + otherLook.getHeightInUserInterfaceDimensionUnit()) / 2f;
		return dx < halfWidths && dy < halfHeights;
	}

	private List<Sprite> getStageSprites() {
		StageActivity stageActivity = StageActivity.activeStageActivity != null
				? StageActivity.activeStageActivity.get() : null;
		if (stageActivity != null) {
			StageListener stageListener = stageActivity.stageListener;
			if (stageListener != null) {
				return stageListener.getSpritesFromStage();
			}
		}
		return ProjectManager.getInstance().getCurrentProject().getSpriteListWithClones();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TouchingSpriteTrigger)) {
			return false;
		}
		TouchingSpriteTrigger that = (TouchingSpriteTrigger) o;
		return reactToBackground == that.reactToBackground
				&& Objects.equal(touchedSpriteName, that.touchedSpriteName);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(touchedSpriteName, reactToBackground);
	}
}
