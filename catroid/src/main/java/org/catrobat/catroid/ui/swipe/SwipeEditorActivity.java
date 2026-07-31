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
package org.catrobat.catroid.ui.swipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.GroupSprite;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.SwipeAttachment;
import org.catrobat.catroid.io.XstreamSerializer;

import java.util.ArrayList;
import java.util.List;

public class SwipeEditorActivity extends Activity {

	public static final String EXTRA_SPRITE_NAME = "extra_sprite_name";

	private SwipeEditorView canvas;
	private TextView hintView;
	private ImageButton btnSave;
	private Switch swipeToggle;
	private Sprite sprite;
	private Scene scene;
	private volatile boolean finishingFlag = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_swipe_editor);

		canvas = findViewById(R.id.swipe_canvas);
		hintView = findViewById(R.id.swipe_hint);
		swipeToggle = findViewById(R.id.swipe_toggle);
		ImageButton btnBack = findViewById(R.id.swipe_btn_back);
		ImageButton btnAdd = findViewById(R.id.swipe_btn_add);
		ImageButton btnDelete = findViewById(R.id.swipe_btn_delete);
		btnSave = findViewById(R.id.swipe_btn_save);
		btnSave.setVisibility(View.GONE);
		TextView title = findViewById(R.id.swipe_title);

		String spriteName = getIntent().getStringExtra(EXTRA_SPRITE_NAME);
		scene = ProjectManager.getInstance().getCurrentlyEditedScene();
		if (scene == null || spriteName == null) {
			finish();
			return;
		}
		sprite = scene.getSprite(spriteName);
		if (sprite == null) {
			finish();
			return;
		}

		title.setText(getString(R.string.swipe_editor_title) + ": " + sprite.getName());
		swipeToggle.setChecked(sprite.isSwipeable());

		swipeToggle.setOnCheckedChangeListener((btn, isChecked) -> {
			sprite.setSwipeable(isChecked);
			autoSaveState();
		});

		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project != null && project.getXmlHeader() != null) {
			canvas.setVirtualSize(project.getXmlHeader().getVirtualScreenWidth(),
					project.getXmlHeader().getVirtualScreenHeight());
		}

		canvas.setParentImage(firstLookPath(sprite));

		for (SwipeAttachment attachment : sprite.getSwipeAttachments()) {
			if (attachment == null || attachment.getChildSpriteName() == null) {
				continue;
			}
			Sprite child = scene.getSprite(attachment.getChildSpriteName());
			canvas.loadExistingAttachment(attachment.getChildSpriteName(), firstLookPath(child),
					attachment.getOffsetX(), attachment.getOffsetY());
		}

		canvas.setOnChangeListener(() -> {
			hintView.setText(R.string.swipe_editor_hint_modified);
			autoSaveState();
		});

		btnBack.setOnClickListener(v -> finish());
		btnAdd.setOnClickListener(v -> showAddDialog());
		btnDelete.setOnClickListener(v -> {
			canvas.deleteSelected();
			hintView.setText(R.string.swipe_editor_hint_modified);
			autoSaveState();
		});
	}

	private String firstLookPath(Sprite candidate) {
		if (candidate != null && !candidate.getLookList().isEmpty()
				&& candidate.getLookList().get(0).getFile() != null) {
			return candidate.getLookList().get(0).getFile().getAbsolutePath();
		}
		return null;
	}

	private void showAddDialog() {
		final List<String> names = new ArrayList<>();
		for (Sprite other : scene.getSpriteList()) {
			if (other == sprite || other instanceof GroupSprite) {
				continue;
			}
			names.add(other.getName());
		}
		if (names.isEmpty()) {
			hintView.setText(R.string.swipe_editor_no_objects);
			return;
		}
		final String[] items = names.toArray(new String[0]);
		new AlertDialog.Builder(this)
				.setTitle(R.string.swipe_editor_add_object)
				.setItems(items, (dialog, which) -> {
					String name = items[which];
					canvas.addAttachment(name, firstLookPath(scene.getSprite(name)));
					hintView.setText(R.string.swipe_editor_hint_modified);
					autoSaveState();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void autoSaveState() {
		if (sprite == null) return;
		sprite.setSwipeable(swipeToggle.isChecked());
		List<SwipeAttachment> attachments = new ArrayList<>();
		for (SwipeEditorView.Attachment attachment : canvas.getAttachments()) {
			attachments.add(new SwipeAttachment(attachment.childName, attachment.offsetX, attachment.offsetY));
		}
		sprite.setSwipeAttachments(attachments);

		new Thread(() -> {
			try {
				XstreamSerializer.getInstance().saveProject(
						ProjectManager.getInstance().getCurrentProject());
			} catch (Exception ignored) {
			}
		}, "swipe-autosave").start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		autoSaveState();
	}

	@Override
	public void onBackPressed() {
		autoSaveState();
		setResult(RESULT_OK);
		super.onBackPressed();
	}
}
