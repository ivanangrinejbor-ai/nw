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
package org.catrobat.catroid.ui.sceneeditor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.PlaceAtBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.io.XstreamSerializer;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.ui.SpriteActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * UI 2.0 scene editor: shows the whole scene with a grid, infinite pan/zoom and drag-to-place for
 * every object. Object positions are stored Catroid-native in each sprite's StartScript PlaceAtBrick.
 * Launched from ProjectActivity when the scene-editor mode is enabled. Saving persists the project
 * off the UI thread, mirroring HitboxEditorActivity.
 */
public class SceneEditorActivity extends Activity implements SceneEditorView.Listener,
		FloatingObjectWindow.Callback {

	public static final String EXTRA_SCENE_NAME = "extra_scene_name";

	private static final int MAX_BITMAP_DIM = 1024;

	private SceneEditorView canvas;
	private TextView hintView;
	private ImageButton btnSave;
	private FrameLayout windowContainer;
	private final List<FloatingObjectWindow> windows = new ArrayList<>();
	private Scene scene;
	private Project project;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scene_editor);

		canvas = findViewById(R.id.scene_editor_canvas);
		hintView = findViewById(R.id.scene_editor_hint);
		TextView title = findViewById(R.id.scene_editor_title);
		ImageButton btnBack = findViewById(R.id.scene_editor_btn_back);
		ImageButton btnPlay = findViewById(R.id.scene_editor_btn_play);
		ImageButton btnStop = findViewById(R.id.scene_editor_btn_stop);
		btnSave = findViewById(R.id.scene_editor_btn_save);
		windowContainer = findViewById(R.id.scene_editor_window_container);

		project = ProjectManager.getInstance().getCurrentProject();
		scene = ProjectManager.getInstance().getCurrentlyEditedScene();
		if (project == null || scene == null) {
			finish();
			return;
		}

		title.setText(getString(R.string.scene_editor_title) + ": " + scene.getName());
		canvas.setVirtualSize(project.getXmlHeader().getVirtualScreenWidth(),
				project.getXmlHeader().getVirtualScreenHeight());
		canvas.setListener(this);
		canvas.setObjects(buildObjects());

		btnBack.setOnClickListener(v -> finish());
		btnPlay.setOnClickListener(v -> StageActivity.handlePlayButton(ProjectManager.getInstance(), this));
		btnStop.setOnClickListener(v -> hintView.setText(R.string.scene_editor_hint_stop));
		btnSave.setOnClickListener(v -> saveAndExit());
	}

	private List<SceneEditorView.SceneObject> buildObjects() {
		List<SceneEditorView.SceneObject> result = new ArrayList<>();
		Sprite background = scene.getBackgroundSprite();
		for (Sprite sprite : scene.getSpriteList()) {
			if (sprite == background) {
				continue;
			}
			Bitmap bitmap = firstLookBitmap(sprite);
			int[] position = readPosition(sprite);
			float widthUnits = bitmap != null ? bitmap.getWidth() : 0f;
			float heightUnits = bitmap != null ? bitmap.getHeight() : 0f;
			result.add(new SceneEditorView.SceneObject(sprite, bitmap, position[0], position[1],
					widthUnits, heightUnits));
		}
		return result;
	}

	private Bitmap firstLookBitmap(Sprite sprite) {
		if (sprite.getLookList().isEmpty()) {
			return null;
		}
		LookData look = sprite.getLookList().get(0);
		if (look.getFile() == null) {
			return null;
		}
		return loadBitmap(look.getFile().getAbsolutePath());
	}

	private Bitmap loadBitmap(String path) {
		if (path == null) {
			return null;
		}
		BitmapFactory.Options bounds = new BitmapFactory.Options();
		bounds.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, bounds);
		int sample = 1;
		if (bounds.outWidth > 0 && bounds.outHeight > 0) {
			while (Math.max(bounds.outWidth, bounds.outHeight) / sample > MAX_BITMAP_DIM) {
				sample *= 2;
			}
		}
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inSampleSize = sample;
		return BitmapFactory.decodeFile(path, opts);
	}

	private int[] readPosition(Sprite sprite) {
		PlaceAtBrick placeAt = findPlaceAtBrick(sprite);
		if (placeAt == null) {
			return new int[] {0, 0};
		}
		int x = interpretInt(sprite, placeAt.getFormulaWithBrickField(Brick.BrickField.X_POSITION));
		int y = interpretInt(sprite, placeAt.getFormulaWithBrickField(Brick.BrickField.Y_POSITION));
		return new int[] {x, y};
	}

	private int interpretInt(Sprite sprite, Formula formula) {
		if (formula == null) {
			return 0;
		}
		try {
			Integer value = formula.interpretInteger(new Scope(project, sprite, null));
			return value != null ? value : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	private PlaceAtBrick findPlaceAtBrick(Sprite sprite) {
		for (Script script : sprite.getScriptList()) {
			if (script instanceof StartScript) {
				for (Brick brick : script.getBrickList()) {
					if (brick instanceof PlaceAtBrick) {
						return (PlaceAtBrick) brick;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void onObjectMoved(Sprite sprite, int x, int y) {
		writePosition(sprite, x, y);
		hintView.setText(getString(R.string.scene_editor_hint_moved, sprite.getName(), x, y));
	}

	@Override
	public void onObjectTapped(Sprite sprite) {
		for (FloatingObjectWindow existing : windows) {
			if (existing.getSprite() == sprite) {
				existing.bringToFront();
				return;
			}
		}
		FloatingObjectWindow window = new FloatingObjectWindow(this, sprite, this);
		float density = getResources().getDisplayMetrics().density;
		int widthPx = Math.round(260 * density);
		int heightPx = Math.round(220 * density);
		int margin = Math.round(16 * density);
		int offset = windows.size() * Math.round(24 * density);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(widthPx, heightPx);
		params.gravity = Gravity.TOP | Gravity.START;
		params.topMargin = margin + offset;
		params.leftMargin = offset;
		window.setLayoutParams(params);
		windowContainer.addView(window);
		windows.add(window);
		windowContainer.post(() -> {
			FrameLayout.LayoutParams laidOut = (FrameLayout.LayoutParams) window.getLayoutParams();
			laidOut.leftMargin = Math.max(0, windowContainer.getWidth() - window.getWidth() - margin) - offset;
			window.setLayoutParams(laidOut);
		});
	}

	@Override
	public void onOpenBlocks(Sprite sprite) {
		ProjectManager.getInstance().setCurrentSprite(sprite);
		Intent intent = new Intent(this, SpriteActivity.class);
		intent.putExtra(SpriteActivity.EXTRA_FRAGMENT_POSITION, SpriteActivity.FRAGMENT_SCRIPTS);
		startActivity(intent);
	}

	@Override
	public void onClosed(FloatingObjectWindow window) {
		windows.remove(window);
	}

	private void writePosition(Sprite sprite, int x, int y) {
		PlaceAtBrick placeAt = findPlaceAtBrick(sprite);
		if (placeAt != null) {
			placeAt.setFormulaWithBrickField(Brick.BrickField.X_POSITION, new Formula(x));
			placeAt.setFormulaWithBrickField(Brick.BrickField.Y_POSITION, new Formula(y));
			return;
		}
		StartScript startScript = null;
		for (Script script : sprite.getScriptList()) {
			if (script instanceof StartScript) {
				startScript = (StartScript) script;
				break;
			}
		}
		if (startScript == null) {
			startScript = new StartScript();
			sprite.addScript(startScript);
		}
		startScript.addBrick(0, new PlaceAtBrick(x, y));
		startScript.setParents();
	}

	private void saveAndExit() {
		btnSave.setEnabled(false);
		hintView.setText(R.string.scene_editor_hint_saving);
		new Thread(() -> {
			try {
				XstreamSerializer.getInstance().saveProject(ProjectManager.getInstance().getCurrentProject());
			} catch (Exception ignored) {
				// Best effort — positions are already in memory.
			}
			if (!isDestroyed() && !isFinishing()) {
				runOnUiThread(() -> {
					setResult(RESULT_OK);
					finish();
				});
			}
		}, "scene-editor-save").start();
		setResult(RESULT_OK);
	}
}
