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

package org.catrobat.catroid.ui.recyclerview.dialog;

import android.content.Context;
import android.content.DialogInterface;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Scene;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public final class PlaySceneDialog extends AlertDialog {

	private PlaySceneDialog(Context context) {
		super(context);
	}

	public static class Builder extends AlertDialog.Builder {

		private int checkedIndex = 0;
		private final Scene defaultScene;
		private final Scene currentScene;
		private final boolean currentIsGlobal;
		private final ProjectManager projectManager;

		public Builder(@NonNull Context context) {
			super(context);

			projectManager = ProjectManager.getInstance();
			currentScene = projectManager.getCurrentlyEditedScene();
			defaultScene = projectManager.getCurrentProject().getDefaultScene();
			currentIsGlobal = currentScene != null && currentScene.isGlobalScene();

			if (currentIsGlobal) {
				String[] dialogOptions = new String[] {
						String.format(context.getString(R.string.play_scene_dialog_default), defaultScene.getName())
				};
				setTitle(R.string.play_scene_dialog_title);
				setSingleChoiceItems(dialogOptions, 0, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						checkedIndex = 0;
					}
				});
				return;
			}

			String[] dialogOptions = new String[] {
					String.format(context.getString(R.string.play_scene_dialog_default), defaultScene.getName()),
					String.format(context.getString(R.string.play_scene_dialog_current), currentScene.getName())
			};

			setTitle(R.string.play_scene_dialog_title);

			setSingleChoiceItems(dialogOptions, 0, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					checkedIndex = which;
				}
			});
		}

		public void applySceneSelection() {
			if (currentIsGlobal || checkedIndex == 0) {
				projectManager.setCurrentlyPlayingScene(defaultScene);
				projectManager.setStartScene(defaultScene);
				return;
			}
			switch (checkedIndex) {
				case 1:
					projectManager.setCurrentlyPlayingScene(currentScene);
					projectManager.setStartScene(currentScene);
					break;
				default:
					break;
			}
		}
	}
}
