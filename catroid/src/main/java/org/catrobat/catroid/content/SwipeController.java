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

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.SwipedEventId;

import java.util.ArrayList;
import java.util.List;

public class SwipeController {

	private enum State { IDLE, DRAGGING, FLYING, SPRINGING }

	private static final float SWIPE_THRESHOLD = 40f;
	private static final float TAP_THRESHOLD = 8f;
	private static final float FLY_DURATION = 0.35f;
	private static final float SPRING_DURATION = 0.2f;

	private final Sprite parentSprite;

	private State state = State.IDLE;
	private float startFingerX;
	private float startFingerY;
	private int committedDirection = SwipedEventId.UP;

	private final List<Look> groupLooks = new ArrayList<>();
	private final List<float[]> dragStart = new ArrayList<>();
	private final List<float[]> tweenFrom = new ArrayList<>();
	private final List<float[]> tweenTo = new ArrayList<>();
	private float tweenProgress;
	private float tweenDuration;

	public SwipeController(Sprite parentSprite) {
		this.parentSprite = parentSprite;
	}

	public boolean onTouchDown(float fingerX, float fingerY) {
		if (state != State.IDLE || parentSprite == null || parentSprite.look == null) {
			return false;
		}
		groupLooks.clear();
		dragStart.clear();
		addLookToGroup(parentSprite.look);
		for (Look childLook : resolveAttachedLooks()) {
			addLookToGroup(childLook);
		}
		startFingerX = fingerX;
		startFingerY = fingerY;
		state = State.DRAGGING;
		return true;
	}

	public void onTouchDragged(float fingerX, float fingerY) {
		if (state != State.DRAGGING) {
			return;
		}
		float dx = fingerX - startFingerX;
		float dy = fingerY - startFingerY;
		for (int i = 0; i < groupLooks.size(); i++) {
			float[] start = dragStart.get(i);
			groupLooks.get(i).setPositionInUserInterfaceDimensionUnit(start[0] + dx, start[1] + dy);
		}
	}

	public void onTouchUp(float fingerX, float fingerY) {
		if (state != State.DRAGGING) {
			return;
		}
		float dx = fingerX - startFingerX;
		float dy = fingerY - startFingerY;
		float distance = Math.max(Math.abs(dx), Math.abs(dy));

		if (distance < TAP_THRESHOLD) {
			resetGroupToStart();
			state = State.IDLE;
			fireTap();
			return;
		}

		if (distance >= SWIPE_THRESHOLD) {
			committedDirection = dominantDirection(dx, dy);
			startFlyTween();
		} else {
			startSpringTween();
		}
	}

	public void update(float delta) {
		if (state != State.FLYING && state != State.SPRINGING) {
			return;
		}
		if (tweenDuration <= 0f) {
			tweenProgress = 1f;
		} else {
			tweenProgress += delta / tweenDuration;
		}
		float p = Math.min(1f, tweenProgress);
		for (int i = 0; i < groupLooks.size(); i++) {
			float[] from = tweenFrom.get(i);
			float[] to = tweenTo.get(i);
			groupLooks.get(i).setPositionInUserInterfaceDimensionUnit(
					from[0] + (to[0] - from[0]) * p,
					from[1] + (to[1] - from[1]) * p);
		}
		if (p >= 1f) {
			boolean wasFlying = state == State.FLYING;
			state = State.IDLE;
			if (wasFlying) {
				fireSwiped(committedDirection);
			}
		}
	}

	private void addLookToGroup(Look look) {
		if (look == null) {
			return;
		}
		groupLooks.add(look);
		dragStart.add(new float[] {
				look.getXInUserInterfaceDimensionUnit(),
				look.getYInUserInterfaceDimensionUnit()
		});
	}

	private List<Look> resolveAttachedLooks() {
		List<Look> looks = new ArrayList<>();
		Scene scene = ProjectManager.getInstance().getCurrentlyPlayingScene();
		if (scene == null) {
			return looks;
		}
		for (SwipeAttachment attachment : parentSprite.getSwipeAttachments()) {
			if (attachment == null || attachment.getChildSpriteName() == null) {
				continue;
			}
			Sprite child = scene.getSprite(attachment.getChildSpriteName());
			if (child != null && child.look != null) {
				looks.add(child.look);
			}
		}
		return looks;
	}

	private int dominantDirection(float dx, float dy) {
		if (Math.abs(dx) >= Math.abs(dy)) {
			return dx >= 0 ? SwipedEventId.RIGHT : SwipedEventId.LEFT;
		}
		return dy >= 0 ? SwipedEventId.UP : SwipedEventId.DOWN;
	}

	private void startFlyTween() {
		tweenFrom.clear();
		tweenTo.clear();
		float[] offset = offScreenDelta(parentSprite.look, committedDirection);
		for (Look look : groupLooks) {
			float fromX = look.getXInUserInterfaceDimensionUnit();
			float fromY = look.getYInUserInterfaceDimensionUnit();
			tweenFrom.add(new float[] {fromX, fromY});
			tweenTo.add(new float[] {fromX + offset[0], fromY + offset[1]});
		}
		tweenProgress = 0f;
		tweenDuration = FLY_DURATION;
		state = State.FLYING;
	}

	private void startSpringTween() {
		tweenFrom.clear();
		tweenTo.clear();
		for (int i = 0; i < groupLooks.size(); i++) {
			Look look = groupLooks.get(i);
			tweenFrom.add(new float[] {
					look.getXInUserInterfaceDimensionUnit(),
					look.getYInUserInterfaceDimensionUnit()
			});
			float[] start = dragStart.get(i);
			tweenTo.add(new float[] {start[0], start[1]});
		}
		tweenProgress = 0f;
		tweenDuration = SPRING_DURATION;
		state = State.SPRINGING;
	}

	private float[] offScreenDelta(Look parentLook, int direction) {
		float screenWidth = 480f;
		float screenHeight = 800f;
		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project != null && project.getXmlHeader() != null) {
			screenWidth = project.getXmlHeader().getVirtualScreenWidth();
			screenHeight = project.getXmlHeader().getVirtualScreenHeight();
		}
		float halfWidth = screenWidth / 2f;
		float halfHeight = screenHeight / 2f;
		float objectHalfWidth = parentLook.getWidthInUserInterfaceDimensionUnit() / 2f;
		float objectHalfHeight = parentLook.getHeightInUserInterfaceDimensionUnit() / 2f;
		float currentX = parentLook.getXInUserInterfaceDimensionUnit();
		float currentY = parentLook.getYInUserInterfaceDimensionUnit();
		switch (direction) {
			case SwipedEventId.LEFT:
				return new float[] {(-halfWidth - objectHalfWidth) - currentX, 0f};
			case SwipedEventId.RIGHT:
				return new float[] {(halfWidth + objectHalfWidth) - currentX, 0f};
			case SwipedEventId.DOWN:
				return new float[] {0f, (-halfHeight - objectHalfHeight) - currentY};
			case SwipedEventId.UP:
			default:
				return new float[] {0f, (halfHeight + objectHalfHeight) - currentY};
		}
	}

	private void resetGroupToStart() {
		for (int i = 0; i < groupLooks.size(); i++) {
			float[] start = dragStart.get(i);
			groupLooks.get(i).setPositionInUserInterfaceDimensionUnit(start[0], start[1]);
		}
	}

	private void fireTap() {
		if (parentSprite.look != null) {
			parentSprite.look.fire(new EventWrapper(new EventId(EventId.TAP), false));
		}
	}

	private void fireSwiped(int direction) {
		if (parentSprite.look == null) {
			return;
		}
		parentSprite.look.fire(new EventWrapper(new SwipedEventId(direction), false));
		parentSprite.look.fire(new EventWrapper(new SwipedEventId(SwipedEventId.ANY), false));
	}
}
