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

package org.catrobat.catroid.ui.recyclerview.adapter;

import android.view.View;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.io.ProjectAndSceneScreenshotLoader;
import org.catrobat.catroid.ui.recyclerview.viewholder.ExtendedViewHolder;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class SceneAdapter extends ExtendedRVAdapter<Scene> {

	public SceneAdapter(List<Scene> items) {
		super(items);
	}

	@Override
	public void onBindViewHolder(ExtendedViewHolder holder, int position) {
		int thumbnailWidth = holder.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.project_thumbnail_width);
		int thumbnailHeight = holder.itemView.getContext().getResources().getDimensionPixelSize(R.dimen.project_thumbnail_height);
		ProjectAndSceneScreenshotLoader loader = new ProjectAndSceneScreenshotLoader(thumbnailWidth, thumbnailHeight);
		Scene item = items.get(position);

		File projectDir = ProjectManager.getInstance().getCurrentProject().getDirectory();
		if (item.isGlobalScene()) {
			holder.title.setText("\uD83C\uDF10 " + item.getName());
			holder.details.setText(holder.itemView.getContext().getString(R.string.global_scene_badge));
			holder.details.setVisibility(View.VISIBLE);
		} else {
			holder.title.setText(item.getName());
		}

		loader.loadAndShowScreenshot(projectDir.getName(), item.getDirectory().getName(), false, holder.image);

		if (showDetails && !item.isGlobalScene()) {
			holder.details.setText(String.format(Locale.getDefault(),
					holder.itemView.getContext().getString(R.string.scene_details),
					item.getSpriteList().size(),
					getLookCount(item),
					getSoundCount(item)));
			holder.details.setVisibility(View.VISIBLE);
		} else if (!item.isGlobalScene()) {
			holder.details.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onItemMove(int sourcePosition, int targetPosition) {
		boolean moved = super.onItemMove(sourcePosition, targetPosition);
		try {
			org.catrobat.catroid.content.Project project = org.catrobat.catroid.ProjectManager.getInstance().getCurrentProject();
			if (project != null) {
				java.util.List<org.catrobat.catroid.content.Scene> newOrder = new java.util.ArrayList<>();
				for (org.catrobat.catroid.content.Scene s : items) {
					if (s != null && !s.isGlobalScene()) {
						newOrder.add(s);
					}
				}
				java.util.List<org.catrobat.catroid.content.Scene> projectScenes = project.getSceneList();
				projectScenes.clear();
				projectScenes.addAll(newOrder);
				try {
					org.catrobat.catroid.io.XstreamSerializer.getInstance().saveProject(project);
				} catch (Exception e) {
					android.util.Log.e("SceneAdapter", "Failed to save scene order", e);
				}
			}
		} catch (Exception e) {
			android.util.Log.e("SceneAdapter", "onItemMove failed", e);
		}
		try {
			if (!items.isEmpty()) {
				org.catrobat.catroid.content.Scene first = items.get(0);
				if (first != null && first.isGlobalScene() && items.size() > 1) {
					first = items.get(1);
				}
				ProjectManager.getInstance().setCurrentlyEditedScene(first);
			}
		} catch (Exception ignored) {}
		return moved;
	}

	private int getLookCount(Scene scene) {
		int lookCount = 0;
		for (Sprite sprite : scene.getSpriteList()) {
			lookCount += sprite.getLookList().size();
		}
		return lookCount;
	}

	private int getSoundCount(Scene scene) {
		int soundCount = 0;
		for (Sprite sprite : scene.getSpriteList()) {
			soundCount += sprite.getSoundList().size();
		}
		return soundCount;
	}
}
