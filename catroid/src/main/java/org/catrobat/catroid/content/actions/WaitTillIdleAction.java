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

package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.stage.StageListener;

public class WaitTillIdleAction extends Action {

	private static final float TIMEOUT = 5.0f; // 5 seconds timeout
	private float totalTime = 0f;

	@Override
	public boolean act(float delta) {
		totalTime += delta;
		
		// Timeout protection: stop waiting after 5 seconds
		if (totalTime >= TIMEOUT) {
			return true;
		}
		
		return allActorsIdle();
	}

	private boolean allActorsIdle() {
		StageListener stageListener = StageActivity.getActiveStageListener();
		if (stageListener == null || stageListener.getStage() == null) {
			return false;
		}
		
		com.badlogic.gdx.scenes.scene2d.Stage stage = stageListener.getStage();
		int totalActors = stage.getActors().size;
		if (totalActors == 0) {
			return true; // No actors = idle
		}

		int idleActors = 0;
		int actorsWithOnlyThisScript = 0;

		for (Actor actor : stage.getActors()) {
			Array<Action> actions = actor.getActions();
			
			// Actor is idle if it has no actions
			if (actions.size == 0) {
				idleActors++;
				continue;
			}
			
			// Check if actor has only ScriptSequenceAction containing this WaitTillIdleAction
			boolean hasOnlyThisScript = false;
			for (Action action : actions) {
				if (action instanceof ScriptSequenceAction) {
					ScriptSequenceAction sequenceAction = (ScriptSequenceAction) action;
					if (sequenceAction.getActions().contains(this, true)) {
						// This actor is running the script that contains this WaitTillIdleAction
						// Check if it has no OTHER actions besides this sequence
						if (actions.size == 1) {
							hasOnlyThisScript = true;
						}
					}
				}
			}
			
			if (hasOnlyThisScript) {
				actorsWithOnlyThisScript++;
			}
		}
		
		// All actors are idle if: all have 0 actions, OR only the current script is running
		return (idleActors + actorsWithOnlyThisScript) >= totalActors;
	}
	
	@Override
	public void restart() {
		totalTime = 0f;
		super.restart();
	}
}
