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

package org.catrobat.catroid.common;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BroadcastMessageContainer {

	private final List<String> broadcastMessages;
	private final Map<String, Set<String>> customCreatedMessagesByScene = new HashMap<>();
	private final Map<String, BroadcastMessageScope> messageScopes = new HashMap<>();
	private transient Scene lastUpdatedScene;

	public BroadcastMessageContainer() {
		this.broadcastMessages = new ArrayList<>();
	}

	public void update() {
		Scene editedScene = ProjectManager.getInstance().getCurrentlyEditedScene();
		broadcastMessages.clear();
		lastUpdatedScene = editedScene;
		if (editedScene == null) {
			return;
		}
		Set<String> all = new LinkedHashSet<>();
		if (editedScene.isGlobalScene()) {
			all.addAll(editedScene.getBroadcastMessagesInUse());
			Project project = ProjectManager.getInstance().getCurrentProject();
			if (project != null) {
				for (Scene scene : project.getSceneList()) {
					all.addAll(scene.getBroadcastMessagesInUse());
				}
				if (project.getGlobalScene() != null && project.getGlobalScene() != editedScene) {
					all.addAll(project.getGlobalScene().getBroadcastMessagesInUse());
				}
			}
		} else {
			all.addAll(editedScene.getBroadcastMessagesInUse());
		}
		if (editedScene.isGlobalScene()) {
			for (Set<String> messages : customCreatedMessagesByScene.values()) {
				all.addAll(messages);
			}
		} else {
			all.addAll(customCreatedMessagesByScene.getOrDefault(editedScene.getName(), new LinkedHashSet<>()));
		}
		broadcastMessages.addAll(all);
	}

	public boolean addBroadcastMessage(String messageToAdd) {
		if (messageToAdd != null && !messageToAdd.isEmpty()) {
			Scene editedScene = ProjectManager.getInstance().getCurrentlyEditedScene();
			String sceneName = editedScene == null ? "" : editedScene.getName();
			customCreatedMessagesByScene
					.computeIfAbsent(sceneName, ignored -> new LinkedHashSet<>())
					.add(messageToAdd);
			if (!broadcastMessages.contains(messageToAdd)) {
				broadcastMessages.add(messageToAdd);
			}
			return true;
		}
		return false;
	}

	public boolean removeBroadcastMessage(String messageToRemove) {
		if (messageToRemove != null && !messageToRemove.isEmpty()) {
			for (Set<String> messages : customCreatedMessagesByScene.values()) {
				messages.remove(messageToRemove);
			}
			return broadcastMessages.remove(messageToRemove);
		}
		return false;
	}

	public List<String> getBroadcastMessages() {
		Scene editedScene = ProjectManager.getInstance().getCurrentlyEditedScene();
		if (broadcastMessages.size() == 0 || lastUpdatedScene != editedScene) {
			update();
		}
		return broadcastMessages;
	}

	public BroadcastMessageScope getScope(String message) {
		return messageScopes.get(message);
	}

	public void setScope(String message, List<String> sceneNames) {
		if (message == null || message.isEmpty()) return;
		if (sceneNames == null || sceneNames.isEmpty()) {
			messageScopes.remove(message);
		} else {
			messageScopes.put(message, new BroadcastMessageScope(message, sceneNames));
		}
	}

	public boolean isMessageVisibleInScene(String message, String sceneName) {
		BroadcastMessageScope scope = messageScopes.get(message);
		if (scope == null) return true;
		return scope.isAllowedInScene(sceneName);
	}

	public List<String> getMessagesVisibleInScene(String sceneName) {
		List<String> result = new ArrayList<>();
		for (String msg : getBroadcastMessages()) {
			if (isMessageVisibleInScene(msg, sceneName)) {
				result.add(msg);
			}
		}
		return result;
	}

	public void onSceneRemoved(String sceneName) {
		for (BroadcastMessageScope scope : messageScopes.values()) {
			scope.removeScene(sceneName);
		}
	}

	public void onSceneRenamed(String oldName, String newName) {
		for (BroadcastMessageScope scope : messageScopes.values()) {
			scope.renameScene(oldName, newName);
		}
	}
}
