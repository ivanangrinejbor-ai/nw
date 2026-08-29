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

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.PlaceAtBrick;
import org.catrobat.catroid.content.bricks.SetWidthBrick;
import org.catrobat.catroid.content.bricks.SetHeightBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.io.StorageOperations;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.ui.ImportFromPocketPaintLauncher;
import org.catrobat.catroid.ui.ImportFromCameraLauncher;
import org.catrobat.catroid.ui.ImportFormMediaLibraryLauncher;
import org.catrobat.catroid.editor.EditorActivity;
import org.catrobat.catroid.ui.ProjectActivity;
import org.catrobat.catroid.ui.recyclerview.controller.SceneController;
import org.catrobat.catroid.ui.recyclerview.controller.SpriteController;
import org.catrobat.catroid.ui.SpriteActivity;
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider;
import org.catrobat.catroid.ui.SettingsActivity;
import org.catrobat.catroid.soundrecorder.SoundRecorderActivity;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.ui.controller.BackpackListManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SceneEditorActivity extends AppCompatActivity implements SceneEditorView.Listener,
		FloatingObjectWindow.Callback {

	public static final String EXTRA_SCENE_NAME = "extra_scene_name";
	public static final String EXTRA_OPEN_OBJECT_NAME = "extra_open_object_name";
	public static final String EXTRA_OPEN_OBJECT_TAB = "extra_open_object_tab";
	public static final int TAB_SPRITES = 0;
	public static final int TAB_LOOKS = 1;
	public static final int TAB_SOUNDS = 2;

	private static final int MAX_BITMAP_DIM = 1024;
	private static final int REQUEST_POCKET_PAINT_LOOK = 8021;
	private static final int REQUEST_LOOK_FILE = 8022;
	private static final int REQUEST_LOOK_CAMERA = 8023;
	private static final int REQUEST_SOUND_FILE = 8024;
	private static final int REQUEST_SOUND_RECORD = 8025;
private static final int REQUEST_OBJECT_LIBRARY = 8026;
	private static final String PAINT_TARGET_SPRITE_ID = "paintTargetSpriteId";
	private static final String PAINT_TARGET_LOOK_INDEX = "paintTargetLookIndex";

	private SceneEditorView canvas;
	private TextView hintView;
	private TextView title;
	private ImageButton btnSave;
	private FrameLayout windowContainer;
	private View objectDock;
	private LinearLayout objectDockList;
	private final List<FloatingObjectWindow> windows = new ArrayList<>();
	private final Map<Sprite, int[]> pendingMoves = new HashMap<>();
	private List<SceneEditorView.SceneObject> sceneObjects = new ArrayList<>();
	private Scene scene;
	private Project project;
	private Sprite paintTargetSprite;
	private LookData paintTargetLook;
private Sprite pendingLookSprite;
	private Sprite pendingSoundSprite;
	private boolean initialResumeConsumed = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scene_editor);

		canvas = findViewById(R.id.scene_editor_canvas);
		hintView = findViewById(R.id.scene_editor_hint);
		title = findViewById(R.id.scene_editor_title);
		ImageButton btnBack = findViewById(R.id.scene_editor_btn_back);
		ImageButton btnScenes = findViewById(R.id.scene_editor_btn_scenes);
		ImageButton btnAdd = findViewById(R.id.scene_editor_btn_add);
		ImageButton btnPlay = findViewById(R.id.scene_editor_btn_play);
		ImageButton btnStop = findViewById(R.id.scene_editor_btn_stop);
		ImageButton btnObjects = findViewById(R.id.scene_editor_btn_objects);
		ImageButton btnMore = findViewById(R.id.scene_editor_btn_more);
		btnSave = findViewById(R.id.scene_editor_btn_save);
		btnSave.setVisibility(View.GONE);
		windowContainer = findViewById(R.id.scene_editor_window_container);
		objectDock = findViewById(R.id.scene_editor_object_dock);
		objectDockList = findViewById(R.id.scene_editor_object_dock_list);

		project = ProjectManager.getInstance().getCurrentProject();
		scene = ProjectManager.getInstance().getCurrentlyEditedScene();
		if (scene == null && project != null) {
			scene = project.getDefaultScene();
			ProjectManager.getInstance().setCurrentlyEditedScene(scene);
		}
		if (project == null || scene == null) {
			finish();
			return;
		}
		restorePaintTarget(savedInstanceState);

		title.setText(getString(R.string.scene_editor_title) + ": " + scene.getName());
		if (project.getXmlHeader() != null) {
			canvas.setVirtualSize(project.getXmlHeader().getVirtualScreenWidth(),
					project.getXmlHeader().getVirtualScreenHeight());
		}
		canvas.setListener(this);
		refreshObjects(true);

		btnBack.setOnClickListener(v -> finish());
		btnScenes.setOnClickListener(v -> {
			if (isPlayingInWindow) return;
			showSceneSwitcher();
		});
		btnAdd.setOnClickListener(v -> {
			if (isPlayingInWindow) return;
			showCreateObjectSourceDialog();
		});
		ImageButton btnPause = findViewById(R.id.scene_editor_btn_pause);
		ImageButton btnDebug = findViewById(R.id.scene_editor_btn_debug);

		btnPlay.setOnClickListener(v -> startInWindowPlayback());
		if (btnPause != null) {
			btnPause.setOnClickListener(v -> togglePauseInWindowPlayback());
		}
		btnStop.setOnClickListener(v -> stopInWindowPlayback());
		if (btnDebug != null) {
			btnDebug.setOnClickListener(v -> toggleDebugWindow());
		}
		btnObjects.setOnClickListener(v -> {
			if (isPlayingInWindow) return;
			toggleObjectDock();
		});
		btnMore.setOnClickListener(v -> {
			if (isPlayingInWindow) return;
			showMoreMenu();
		});

		if (getIntent().hasExtra(EXTRA_OPEN_OBJECT_TAB)) {
			canvas.post(this::openRequestedObjectTab);
		}
	}

	private void openRequestedObjectTab() {
		String objectName = getIntent().getStringExtra(EXTRA_OPEN_OBJECT_NAME);
		Sprite target = null;
		for (Sprite candidate : scene.getSpriteList()) {
			if (candidate.getName().equals(objectName)) {
				target = candidate;
				break;
			}
		}
		if (target == null) return;
		toggleObjectDock();
		int tab = getIntent().getIntExtra(EXTRA_OPEN_OBJECT_TAB, TAB_SPRITES);
		if (tab == TAB_LOOKS) {
			showLooksDialog(target);
		} else if (tab == TAB_SOUNDS) {
			showSoundsDialog(target);
		}
	}

	private boolean isPlayingInWindow = false;
	private boolean isPausedInWindow = false;
	private FloatingDebugWindow activeDebugWindow = null;

	private void toggleDebugWindow() {
		if (activeDebugWindow != null && activeDebugWindow.getParent() != null) {
			activeDebugWindow.close();
			activeDebugWindow = null;
			Toast.makeText(this, "Отладчик закрыт", Toast.LENGTH_SHORT).show();
		} else {
			activeDebugWindow = new FloatingDebugWindow(this);
			float density = getResources().getDisplayMetrics().density;
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
					Math.round(300 * density),
					ViewGroup.LayoutParams.WRAP_CONTENT
			);
			params.gravity = Gravity.TOP | Gravity.START;
			params.topMargin = Math.round(70 * density);
			params.leftMargin = Math.round(16 * density);
			windowContainer.addView(activeDebugWindow, params);
			Toast.makeText(this, "Отладчик 2.0 открыт!", Toast.LENGTH_SHORT).show();
		}
	}

	private void startInWindowPlayback() {
		if (project == null || isFinishing()) return;
		org.catrobat.catroid.content.GlobalManager.setStopSounds(false);
		new Thread(() -> {
			try {
				if (!ProjectSaveCoordinator.saveBlocking(project)) {
					throw new IllegalStateException("Project save failed");
				}
				runOnUiThread(() -> {
					org.catrobat.catroid.ProjectManager pm = org.catrobat.catroid.ProjectManager.getInstance();
					Scene playing = pm.getCurrentlyPlayingScene();
					if (playing == null || playing.isGlobalScene()) {
						Scene fallback = project.getDefaultScene();
						if (fallback != null) {
							pm.setCurrentlyPlayingScene(fallback);
							pm.setStartScene(fallback);
						}
					}
					Intent intent = new Intent(this, StageActivity.class);
					startActivityForResult(intent, StageActivity.REQUEST_START_STAGE);
				});
			} catch (Exception e) {
				runOnUiThread(() -> Toast.makeText(this,
						"Не удалось сохранить проект перед запуском", Toast.LENGTH_LONG).show());
			}
		}, "scene-editor-launch-save").start();
	}

	private void togglePauseInWindowPlayback() {
		if (!isPlayingInWindow) return;
		isPausedInWindow = !isPausedInWindow;
		ImageButton btnPlay = findViewById(R.id.scene_editor_btn_play);
		ImageButton btnPause = findViewById(R.id.scene_editor_btn_pause);
		if (isPausedInWindow) {
			if (btnPlay != null) btnPlay.setVisibility(View.VISIBLE);
			if (btnPause != null) btnPause.setVisibility(View.GONE);
			hintView.setText("Пауза. Выберите действие на панели управления.");
		} else {
			if (btnPlay != null) btnPlay.setVisibility(View.GONE);
			if (btnPause != null) btnPause.setVisibility(View.VISIBLE);
			hintView.setText("Воспроизведение возобновлено.");
		}
	}

	private void stopInWindowPlayback() {
		isPlayingInWindow = false;
		isPausedInWindow = false;
		ImageButton btnPlay = findViewById(R.id.scene_editor_btn_play);
		ImageButton btnPause = findViewById(R.id.scene_editor_btn_pause);
		if (btnPlay != null) btnPlay.setVisibility(View.VISIBLE);
		if (btnPause != null) btnPause.setVisibility(View.GONE);

		org.catrobat.catroid.content.GlobalManager.setStopSounds(true);
		canvas.setPlayingMode(false);
		refreshObjects(true);
		hintView.setText("Воспроизведение остановлено. Редактор разблокирован.");
	}

	@Override
	public void onObjectMoved(Sprite sprite, int x, int y) {
		pendingMoves.put(sprite, new int[] {x, y});
		writePosition(sprite, x, y);
		persistProjectAsync();
		hintView.setText(getString(R.string.scene_editor_hint_moved, sprite.getName(), x, y));
	}

	private String formatLastModified(long timestamp) {
		if (timestamp <= 0) return "—";
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault());
		return sdf.format(new java.util.Date(timestamp));
	}

	private String formatBytes(long totalBytes) {
		if (totalBytes <= 0) return "0 KB";
		if (totalBytes < 1024) return totalBytes + " B";
		if (totalBytes < 1024 * 1024) return String.format(java.util.Locale.getDefault(), "%.1f KB", totalBytes / 1024.0f);
		return String.format(java.util.Locale.getDefault(), "%.1f MB", totalBytes / (1024.0f * 1024.0f));
	}

	private void showSceneSwitcher() {
		final List<Scene> scenes = project.getSceneList();
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		int dp16 = Math.round(16 * getResources().getDisplayMetrics().density);
		int dp10 = Math.round(10 * getResources().getDisplayMetrics().density);
		int dp12 = Math.round(12 * getResources().getDisplayMetrics().density);
		container.setPadding(dp16, dp16, dp16, dp16);

		android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
		scrollView.addView(container);

		AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(R.string.scene_editor_scenes)
				.setView(scrollView)
				.setPositiveButton(R.string.scene_editor_new_scene, (d, which) -> createScene())
				.setNeutralButton(R.string.scene_editor_project_files, (d, which) -> openProjectPanel("project_files"))
				.setNegativeButton(android.R.string.cancel, null)
				.create();

		for (Scene sceneItem : scenes) {
			LinearLayout card = new LinearLayout(this);
			card.setOrientation(LinearLayout.HORIZONTAL);
			card.setBackgroundResource(R.drawable.bg_object_card_cube);
			card.setGravity(Gravity.CENTER_VERTICAL);
			card.setPadding(dp10, dp10, dp10, dp10);

			LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			cardParams.bottomMargin = dp10;
			card.setLayoutParams(cardParams);

			android.widget.ImageView preview = new android.widget.ImageView(this);
			int thumbSize = Math.round(52 * getResources().getDisplayMetrics().density);
			LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(thumbSize, thumbSize);
			thumbParams.rightMargin = dp10;
			preview.setLayoutParams(thumbParams);
			preview.setBackgroundResource(R.drawable.bg_object_thumb_cube);
			preview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);

			File screenshot = new File(sceneItem.getDirectory(), "manual_screenshot.png");
			if (!screenshot.exists()) {
				screenshot = new File(sceneItem.getDirectory(), "automatic_screenshot.png");
			}
			if (screenshot.exists()) {
				Bitmap bm = loadBitmap(screenshot.getAbsolutePath());
				if (bm != null) preview.setImageBitmap(bm);
			}

			card.addView(preview);

			LinearLayout textLayout = new LinearLayout(this);
			textLayout.setOrientation(LinearLayout.VERTICAL);

			TextView nameView = new TextView(this);
			nameView.setText(sceneItem.getName());
			nameView.setTextColor(0xFFF8FAFC);
			nameView.setTextSize(16f);
			nameView.setTypeface(null, android.graphics.Typeface.BOLD);
			textLayout.addView(nameView);

			long lastMod = 0;
			long totalSize = 0;
			File dir = sceneItem.getDirectory();
			if (dir != null && dir.exists()) {
				File[] files = dir.listFiles();
				if (files != null) {
					for (File f : files) {
						lastMod = Math.max(lastMod, f.lastModified());
						totalSize += f.length();
					}
				}
			}

			TextView subView = new TextView(this);
			subView.setText("Изменено: " + formatLastModified(lastMod) + " • " + formatBytes(totalSize));
			subView.setTextColor(0xFF94A3B8);
			subView.setTextSize(12f);
			textLayout.addView(subView);

			card.addView(textLayout);

			card.setOnClickListener(v -> {
				dialog.dismiss();
				switchScene(sceneItem);
			});
			card.setOnLongClickListener(v -> {
				dialog.dismiss();
				showSceneActions(sceneItem);
				return true;
			});

			container.addView(card);
		}

		TextView createSceneBtn = new TextView(this);
		createSceneBtn.setText("Создать новую сцену");
		createSceneBtn.setTextColor(0xFF94A3B8);
		createSceneBtn.setTextSize(15f);
		createSceneBtn.setTypeface(null, android.graphics.Typeface.BOLD);
		createSceneBtn.setPadding(dp12, dp12, dp12, dp12);
		createSceneBtn.setBackgroundResource(R.drawable.bg_object_card_cube);
		LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		createParams.bottomMargin = dp10;
		createSceneBtn.setLayoutParams(createParams);
		createSceneBtn.setOnClickListener(v -> {
			dialog.dismiss();
			createScene();
		});
		container.addView(createSceneBtn, 0);

		dialog.show();
	}

	@Override
	public void onOpenLayering(Sprite sprite) {
		showLayeringDialog(sprite);
	}

	@Override
	public void onOpenInspector(Sprite sprite) {
		showInspectorDialog(sprite);
	}

	private void showLayeringDialog(Sprite targetSprite) {
		String[] options = {
				"На самый передний план",
				"Поднять на слой выше",
				"Опустить на слой ниже",
				"На самый задний план"
		};
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Порядок слоёв: " + targetSprite.getName())
				.setItems(options, (dialog, which) -> {
					List<Sprite> list = scene.getSpriteList();
					int currIdx = list.indexOf(targetSprite);
					if (currIdx < 0) return;
					if (which == 0) {
						list.remove(targetSprite);
						list.add(targetSprite);
					} else if (which == 1) {
						if (currIdx < list.size() - 1) {
							java.util.Collections.swap(list, currIdx, currIdx + 1);
						}
					} else if (which == 2) {
						if (currIdx > 0) {
							java.util.Collections.swap(list, currIdx, currIdx - 1);
						}
					} else if (which == 3) {
						list.remove(targetSprite);
						list.add(0, targetSprite);
					}
					refreshAfterModelChange();
					Toast.makeText(this, "Порядок слоёв обновлён!", Toast.LENGTH_SHORT).show();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showInspectorDialog(Sprite targetSprite) {
		float density = getResources().getDisplayMetrics().density;
		int dp10 = Math.round(10 * density);
		int dp16 = Math.round(16 * density);

		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setPadding(dp16, dp16, dp16, dp16);

		TextView xLabel = new TextView(this);
		xLabel.setText("Координата X:");
		xLabel.setTextColor(0xFF94A3B8);
		container.addView(xLabel);

		EditText inputX = new EditText(this);
		boolean foundInScript = false;
		int posX = 0, posY = 0;
		outer:
		for (org.catrobat.catroid.content.Script script : targetSprite.getScriptList()) {
			for (org.catrobat.catroid.content.bricks.Brick b : script.getBrickList()) {
				if (b instanceof org.catrobat.catroid.content.bricks.PlaceAtBrick) {
					try {
						posX = Math.round(Float.parseFloat(((org.catrobat.catroid.content.bricks.PlaceAtBrick) b)
								.getFormulaWithBrickField(Brick.BrickField.X_POSITION).interpretString(null)));
						posY = Math.round(Float.parseFloat(((org.catrobat.catroid.content.bricks.PlaceAtBrick) b)
								.getFormulaWithBrickField(Brick.BrickField.Y_POSITION).interpretString(null)));
						foundInScript = true;
						break outer;
					} catch (Exception ignored) {}
				}
			}
		}
		if (!foundInScript && sceneObjects != null) {
			for (SceneEditorView.SceneObject obj : sceneObjects) {
				if (obj.sprite == targetSprite) {
					posX = Math.round(obj.x);
					posY = Math.round(obj.y);
					break;
				}
			}
		}
		inputX.setText(String.valueOf(posX));
		inputX.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
		container.addView(inputX);

		TextView yLabel = new TextView(this);
		yLabel.setText("Координата Y:");
		yLabel.setTextColor(0xFF94A3B8);
		container.addView(yLabel);

		EditText inputY = new EditText(this);
		inputY.setText(String.valueOf(posY));
		inputY.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
		container.addView(inputY);

		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Инспектор свойств: " + targetSprite.getName())
				.setView(container)
				.setPositiveButton("Применить", (dialog, which) -> {
					try {
						int nx = Integer.parseInt(inputX.getText().toString().trim());
						int ny = Integer.parseInt(inputY.getText().toString().trim());
						writePosition(targetSprite, nx, ny);
						refreshAfterModelChange();
						Toast.makeText(this, "Свойства применены!", Toast.LENGTH_SHORT).show();
					} catch (Exception e) {
						Toast.makeText(this, "Ошибка ввода чисел", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("Отмена", null)
				.show();
	}


	@Override
	public void onOpenLooks(Sprite sprite) {
		showLooksDialog(sprite);
	}

	@Override
	public void onOpenSounds(Sprite sprite) {
		showSoundsDialog(sprite);
	}

	private void showLooksDialog(Sprite targetSprite) {
		float density = getResources().getDisplayMetrics().density;
		int dp10 = Math.round(10 * density);
		int dp12 = Math.round(12 * density);

		ScrollView scrollView = new ScrollView(this);
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setPadding(dp12, dp12, dp12, dp12);
		scrollView.addView(container);

		AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Образы: " + targetSprite.getName())
				.setView(scrollView)
				.setNegativeButton(android.R.string.cancel, null)
				.create();

		TextView addPaintBtn = new TextView(this);
		addPaintBtn.setText("Добавить образ");
		addPaintBtn.setTextColor(0xFF94A3B8);
		addPaintBtn.setTextSize(14f);
		addPaintBtn.setTypeface(null, android.graphics.Typeface.BOLD);
		addPaintBtn.setPadding(dp10, dp10, dp10, dp10);
		addPaintBtn.setBackgroundResource(R.drawable.bg_object_card_cube);
		LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		p1.bottomMargin = dp10;
		addPaintBtn.setLayoutParams(p1);
		addPaintBtn.setOnClickListener(v -> {
			dialog.dismiss();
			showLookSourceDialog(targetSprite);
		});
		container.addView(addPaintBtn);

		if (targetSprite.getLookList().isEmpty()) {
			TextView emptyTv = new TextView(this);
			emptyTv.setText("У объекта пока нет образов");
			emptyTv.setTextColor(0xFF94A3B8);
			emptyTv.setPadding(dp10, dp10, dp10, dp10);
			container.addView(emptyTv);
		} else {
			for (LookData look : targetSprite.getLookList()) {
				LinearLayout card = new LinearLayout(this);
				card.setOrientation(LinearLayout.HORIZONTAL);
				card.setBackgroundResource(R.drawable.bg_object_card_cube);
				card.setGravity(Gravity.CENTER_VERTICAL);
				card.setPadding(dp10, dp10, dp10, dp10);
				LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				cp.bottomMargin = dp10;
				card.setLayoutParams(cp);

				ImageView preview = new ImageView(this);
				int thumbSize = Math.round(40 * density);
				LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(thumbSize, thumbSize);
				tp.rightMargin = dp10;
				preview.setLayoutParams(tp);
				preview.setBackgroundResource(R.drawable.bg_object_thumb_cube);
				preview.setScaleType(ImageView.ScaleType.CENTER_CROP);

				if (look.getFile() != null && look.getFile().exists()) {
					Bitmap bm = loadBitmap(look.getFile().getAbsolutePath());
					if (bm != null) preview.setImageBitmap(bm);
				}
				card.addView(preview);

				LinearLayout textL = new LinearLayout(this);
				textL.setOrientation(LinearLayout.VERTICAL);
				TextView nameTv = new TextView(this);
				nameTv.setText(look.getName());
				nameTv.setTextColor(0xFFF8FAFC);
				nameTv.setTextSize(14f);
				nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
				textL.addView(nameTv);

				card.addView(textL);
				card.setOnClickListener(v -> startPocketPaintForLook(targetSprite, look));
				card.setOnLongClickListener(v -> {
					String[] options = {"Удалить образ"};
					new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
							.setTitle(look.getName())
							.setItems(options, (d, w) -> {
								targetSprite.getLookList().remove(look);
								refreshAfterModelChange();
								dialog.dismiss();
								showLooksDialog(targetSprite);
							})
							.show();
					return true;
				});
				container.addView(card);
			}
		}
		dialog.show();
	}

	private void showSoundsDialog(Sprite targetSprite) {
		float density = getResources().getDisplayMetrics().density;
		int dp10 = Math.round(10 * density);
		int dp12 = Math.round(12 * density);

		ScrollView scrollView = new ScrollView(this);
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setPadding(dp12, dp12, dp12, dp12);
		scrollView.addView(container);

		AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Звуки: " + targetSprite.getName())
				.setView(scrollView)
				.setNegativeButton(android.R.string.cancel, null)
				.create();

		TextView addSoundBtn = new TextView(this);
		addSoundBtn.setText("Добавить звук");
		addSoundBtn.setTextColor(0xFF94A3B8);
		addSoundBtn.setTextSize(14f);
		addSoundBtn.setTypeface(null, android.graphics.Typeface.BOLD);
		addSoundBtn.setPadding(dp10, dp10, dp10, dp10);
		addSoundBtn.setBackgroundResource(R.drawable.bg_object_card_cube);
		addSoundBtn.setOnClickListener(v -> showSoundSourceDialog(targetSprite));
		container.addView(addSoundBtn, 0);

		if (targetSprite.getSoundList().isEmpty()) {
			TextView emptyTv = new TextView(this);
			emptyTv.setText("У объекта пока нет звуков");
			emptyTv.setTextColor(0xFF94A3B8);
			emptyTv.setPadding(dp10, dp10, dp10, dp10);
			container.addView(emptyTv);
		} else {
			for (org.catrobat.catroid.common.SoundInfo sound : targetSprite.getSoundList()) {
				LinearLayout card = new LinearLayout(this);
				card.setOrientation(LinearLayout.HORIZONTAL);
				card.setBackgroundResource(R.drawable.bg_object_card_cube);
				card.setGravity(Gravity.CENTER_VERTICAL);
				card.setPadding(dp10, dp10, dp10, dp10);
				LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
				cp.bottomMargin = dp10;
				card.setLayoutParams(cp);

				TextView iconTv = new TextView(this);
				iconTv.setText("");
				iconTv.setTextSize(20f);
				iconTv.setPadding(0, 0, dp10, 0);
				card.addView(iconTv);

				TextView nameTv = new TextView(this);
				nameTv.setText(sound.getName());
				nameTv.setTextColor(0xFFF8FAFC);
				nameTv.setTextSize(14f);
				nameTv.setTypeface(null, android.graphics.Typeface.BOLD);
				card.addView(nameTv);

				card.setOnLongClickListener(v -> {
					String[] options = {"Удалить звук"};
					new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
							.setTitle(sound.getName())
							.setItems(options, (d, w) -> {
								targetSprite.getSoundList().remove(sound);
								refreshAfterModelChange();
								dialog.dismiss();
								showSoundsDialog(targetSprite);
							})
							.show();
					return true;
				});
				container.addView(card);
			}
		}
		dialog.show();
	}

	private void showLookSourceDialog(Sprite targetSprite) {
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Добавить образ")
				.setItems(new String[] {"Pocket Paint", "Галерея", "Камера"}, (dialog, which) -> {
					pendingLookSprite = targetSprite;
					try {
						if (which == 0) {
							startPocketPaintForNewLook(targetSprite);
						} else if (which == 1) {
							Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
									.setType("image/*")
									.addCategory(Intent.CATEGORY_OPENABLE);
							startActivityForResult(intent, REQUEST_LOOK_FILE);
						} else {
							new ImportFromCameraLauncher(this).startActivityForResult(REQUEST_LOOK_CAMERA);
						}
					} catch (Exception e) {
						pendingLookSprite = null;
						Toast.makeText(this, "Источник изображения недоступен", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	@Override
	public void onObjectResized(Sprite sprite, int width, int height) {
		if (sprite == null || sprite.getLookList().isEmpty()) return;
		String lookPath = firstLookPath(sprite);
		int[] natural = lookPath == null ? new int[] {1, 1} : readImageBounds(lookPath);
		float widthPercent = Math.max(1f, width * 100f / Math.max(1, natural[0]));
		float heightPercent = Math.max(1f, height * 100f / Math.max(1, natural[1]));
		SetWidthBrick widthBrick = null;
		SetHeightBrick heightBrick = null;
		for (Script script : sprite.getScriptList()) {
			for (Brick brick : script.getBrickList()) {
				if (brick instanceof SetWidthBrick) widthBrick = (SetWidthBrick) brick;
				if (brick instanceof SetHeightBrick) heightBrick = (SetHeightBrick) brick;
			}
		}
		Script script = null;
		for (Script candidate : sprite.getScriptList()) {
			if (candidate instanceof StartScript) {
				script = candidate;
				break;
			}
		}
		if (script == null) {
			script = new StartScript();
			sprite.addScript(script);
		}
		if (widthBrick == null) {
			widthBrick = new SetWidthBrick(widthPercent);
			script.addBrick(widthBrick);
		} else {
			widthBrick.setFormulaWithBrickField(Brick.BrickField.WIDTH, new Formula(widthPercent));
		}
		if (heightBrick == null) {
			heightBrick = new SetHeightBrick(heightPercent);
			script.addBrick(heightBrick);
		} else {
			heightBrick.setFormulaWithBrickField(Brick.BrickField.HEIGHT, new Formula(heightPercent));
		}
		persistProjectAsync();
	}

	private void showSoundSourceDialog(Sprite targetSprite) {
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Добавить звук")
				.setItems(new String[] {"Файл", "Диктофон"}, (dialog, which) -> {
					pendingSoundSprite = targetSprite;
					try {
						if (which == 0) {
							Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
									.setType("audio/*")
									.addCategory(Intent.CATEGORY_OPENABLE);
							startActivityForResult(intent, REQUEST_SOUND_FILE);
						} else {
							startActivityForResult(new Intent(this, SoundRecorderActivity.class), REQUEST_SOUND_RECORD);
						}
					} catch (Exception e) {
						pendingSoundSprite = null;
						Toast.makeText(this, "Источник звука недоступен", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void populateObjectDock() {
		objectDockList.removeAllViews();
		float density = getResources().getDisplayMetrics().density;
		int dp10 = Math.round(10 * density);
		int dp12 = Math.round(12 * density);

		TextView filesItem = new TextView(this);
		filesItem.setText(getString(R.string.scene_editor_project_files));
		filesItem.setTextColor(0xFF94A3B8);
		filesItem.setTextSize(15f);
		filesItem.setTypeface(null, android.graphics.Typeface.BOLD);
		filesItem.setPadding(dp12, dp12, dp12, dp12);
		filesItem.setBackgroundResource(R.drawable.bg_object_card_cube);
		LinearLayout.LayoutParams filesParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		filesParams.bottomMargin = dp10;
		filesItem.setLayoutParams(filesParams);
		filesItem.setOnClickListener(v -> openProjectPanel("project_files"));
		objectDockList.addView(filesItem);

		for (Sprite dockSprite : scene.getSpriteList()) {
			LinearLayout card = new LinearLayout(this);
			card.setOrientation(LinearLayout.HORIZONTAL);
			card.setBackgroundResource(R.drawable.bg_object_card_cube);
			card.setGravity(Gravity.CENTER_VERTICAL);
			card.setPadding(dp10, dp10, dp10, dp10);

			LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			cardParams.bottomMargin = dp10;
			card.setLayoutParams(cardParams);

			android.widget.ImageView preview = new android.widget.ImageView(this);
			int thumbSize = Math.round(44 * density);
			LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(thumbSize, thumbSize);
			thumbParams.rightMargin = dp10;
			preview.setLayoutParams(thumbParams);
			preview.setBackgroundResource(R.drawable.bg_object_thumb_cube);
			preview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);

			String firstLook = firstLookPath(dockSprite);
			if (firstLook != null) {
				Bitmap bm = loadBitmap(firstLook);
				if (bm != null) preview.setImageBitmap(bm);
			}
			card.addView(preview);

			LinearLayout textLayout = new LinearLayout(this);
			textLayout.setOrientation(LinearLayout.VERTICAL);

			TextView nameView = new TextView(this);
			nameView.setText(dockSprite.getName());
			nameView.setTextColor(0xFFF8FAFC);
			nameView.setTextSize(15f);
			nameView.setTypeface(null, android.graphics.Typeface.BOLD);
			textLayout.addView(nameView);

			long lastMod = 0;
			long totalSize = 0;
			for (LookData look : dockSprite.getLookList()) {
				if (look.getFile() != null && look.getFile().exists()) {
					lastMod = Math.max(lastMod, look.getFile().lastModified());
					totalSize += look.getFile().length();
				}
			}
			for (org.catrobat.catroid.common.SoundInfo sound : dockSprite.getSoundList()) {
				if (sound.getFile() != null && sound.getFile().exists()) {
					lastMod = Math.max(lastMod, sound.getFile().lastModified());
					totalSize += sound.getFile().length();
				}
			}

			TextView subView = new TextView(this);
			subView.setText("Изменено: " + formatLastModified(lastMod) + "\nВес: " + formatBytes(totalSize));
			subView.setTextColor(0xFF94A3B8);
			subView.setTextSize(11f);
			textLayout.addView(subView);

			card.addView(textLayout);

			final Sprite target = dockSprite;
			card.setOnClickListener(v -> openScriptCanvas(target));
			card.setOnLongClickListener(v -> {
				showObjectActions(target);
				return true;
			});
			objectDockList.addView(card);
		}
	}

	private void openScriptCanvas(Sprite sprite) {
		ProjectManager.getInstance().setCurrentlyEditedScene(scene);
		ProjectManager.getInstance().setCurrentSprite(sprite);
		startActivity(new Intent(this, ScriptCanvasActivity.class));
	}

	private void showMoreMenu() {
		List<String> items = new ArrayList<>();
		List<Runnable> actions = new ArrayList<>();

		items.add("AI Помощник (Gemini Chat)");
		actions.add(() -> {
			Intent intent = new Intent(this, org.catrobat.catroid.ai.chat.ChatActivity.class);
			intent.putExtra(org.catrobat.catroid.ai.chat.ChatActivity.EXTRA_SCOPE_PROJECT, project.getName());
			startActivity(intent);
		});

		items.add("Файлы проекта 2.0");
		actions.add(() -> openProjectPanel("project_files"));

		items.add("Опции проекта 2.0");
		actions.add(() -> openProjectPanel("project_options"));

		items.add("Рюкзак");
		actions.add(() -> {
			Intent intent = new Intent(this, org.catrobat.catroid.ui.recyclerview.backpack.BackpackActivity.class);
			startActivity(intent);
		});

		items.add("3D Редактор");
		actions.add(() -> startActivity(new Intent(this, EditorActivity.class)));

		items.add("Управление сценами");
		actions.add(this::showSceneSwitcher);

		items.add("Настройки приложения");
		actions.add(() -> startActivity(new Intent(this, SettingsActivity.class)));

		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(R.string.scene_editor_more)
				.setItems(items.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void openProjectPanel(String panel) {
		Intent intent = new Intent(this, Ui2PanelActivity.class);
		intent.putExtra(Ui2PanelActivity.EXTRA_PANEL, panel);
		startActivity(intent);
	}

	private void showObjectActions(Sprite sprite) {
		boolean isBackground = scene.getBackgroundSprite() == sprite;
		List<String> items = new ArrayList<>();
		List<Runnable> actions = new ArrayList<>();

		items.add(getString(R.string.scene_editor_open_scripts));
		actions.add(() -> openScriptCanvas(sprite));

		items.add("Управление Образами");
		actions.add(() -> showLooksDialog(sprite));

		items.add("Управление Звуками");
		actions.add(() -> showSoundsDialog(sprite));

		items.add("Порядок слоёв (Z-index)");
		actions.add(() -> showLayeringDialog(sprite));

		items.add("Инспектор свойств (X, Y)");
		actions.add(() -> showInspectorDialog(sprite));

		items.add("Положить объект в Рюкзак");
		actions.add(() -> {
			try {
				BackpackListManager.getInstance().getSprites().add(new SpriteController().pack(sprite));
				BackpackListManager.getInstance().saveBackpack();
				Toast.makeText(this, "Объект добавлен в Рюкзак!", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(this, "Не удалось добавить объект в Рюкзак", Toast.LENGTH_SHORT).show();
			}
		});

		items.add(getString(R.string.scene_editor_rename));
		actions.add(() -> renameObject(sprite));

		if (!isBackground) {
			items.add(getString(R.string.scene_editor_duplicate));
			actions.add(() -> duplicateObject(sprite));

			items.add(getString(R.string.scene_editor_delete));
			actions.add(() -> deleteObject(sprite));
		}

		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(sprite.getName())
				.setItems(items.toArray(new String[0]), (dialog, which) -> actions.get(which).run())
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void renameObject(Sprite sprite) {
		EditText input = new EditText(this);
		input.setText(sprite.getName());
		int p = Math.round(16 * getResources().getDisplayMetrics().density);
		input.setPadding(p, p, p, p);
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(R.string.scene_editor_rename)
				.setView(input)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					String typed = input.getText().toString().trim();
					if (typed.isEmpty()) {
						return;
					}
					String name = new UniqueNameProvider().getUniqueNameInNameables(typed, scene.getSpriteList());
					sprite.rename(name);
					refreshAfterModelChange();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void duplicateObject(Sprite sprite) {
		try {
			Sprite copy = new SpriteController().copy(sprite, project, scene);
			if (!scene.getSpriteList().contains(copy)) {
				scene.addSprite(copy);
			}
			refreshAfterModelChange();
		} catch (Exception e) {
			hintView.setText(R.string.scene_editor_action_failed);
		}
	}

	private void deleteObject(Sprite sprite) {
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(R.string.scene_editor_delete)
				.setMessage(sprite.getName())
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					try {
						new SpriteController().delete(sprite);
					} catch (Exception ignored) {
					}
					scene.getSpriteList().remove(sprite);
					refreshAfterModelChange();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void createScene() {
		String name = new UniqueNameProvider().getUniqueNameInNameables(
				getString(R.string.default_scene_name), project.getSceneList());
		Scene newScene = new Scene(name, project);
		newScene.addSprite(new Sprite(getString(R.string.background)));
		project.addScene(newScene);
		switchScene(newScene);
	}

	private void showSceneActions(Scene targetScene) {
		String[] items = {
				getString(R.string.scene_editor_switch_to),
				getString(R.string.scene_editor_rename),
				getString(R.string.scene_editor_delete)
		};
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(targetScene.getName())
				.setItems(items, (dialog, which) -> {
					if (which == 0) {
						switchScene(targetScene);
					} else if (which == 1) {
						renameScene(targetScene);
					} else {
						deleteScene(targetScene);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void renameScene(Scene targetScene) {
		EditText input = new EditText(this);
		input.setText(targetScene.getName());
		int p = Math.round(16 * getResources().getDisplayMetrics().density);
		input.setPadding(p, p, p, p);
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(R.string.scene_editor_rename)
				.setView(input)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					String typed = input.getText().toString().trim();
					if (typed.isEmpty()) {
						return;
					}
					boolean renamed = new SceneController().rename(targetScene, typed);
					if (!renamed) {
						Toast.makeText(this, "Сцена с таким именем уже существует", Toast.LENGTH_SHORT).show();
						return;
					}
					persistProjectAsync();
					if (targetScene == scene) {
						title.setText(getString(R.string.scene_editor_title) + ": " + scene.getName());
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void deleteScene(Scene targetScene) {
		if (ProjectManager.getInstance().getCurrentProject().isProtectedProject()) {
			Toast.makeText(this, R.string.protected_project_cannot_edit, Toast.LENGTH_SHORT).show();
			return;
		}
		if (project.getSceneList().size() <= 1) {
			hintView.setText(R.string.scene_editor_cant_delete_scene);
			return;
		}
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(R.string.scene_editor_delete)
				.setMessage(targetScene.getName())
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					try {
						new SceneController().delete(targetScene);
					} catch (Exception e) {
						Toast.makeText(this, "Не удалось удалить сцену", Toast.LENGTH_SHORT).show();
						return;
					}
					if (targetScene == scene) {
						switchScene(project.getDefaultScene());
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void refreshAfterModelChange() {
		refreshObjects(false);
		if (objectDock != null && objectDock.getVisibility() == View.VISIBLE) {
			populateObjectDock();
		}
	}

	private void writePosition(Sprite sprite, float x, float y) {
		if (sprite == null) return;
		if (ProjectManager.getInstance().getCurrentProject().isProtectedProject()) {
			Toast.makeText(this, R.string.protected_project_cannot_edit, Toast.LENGTH_SHORT).show();
			return;
		}
		for (org.catrobat.catroid.content.Script script : sprite.getScriptList()) {
			for (org.catrobat.catroid.content.bricks.Brick b : script.getBrickList()) {
				if (b instanceof org.catrobat.catroid.content.bricks.PlaceAtBrick) {
					org.catrobat.catroid.content.bricks.PlaceAtBrick pab = (org.catrobat.catroid.content.bricks.PlaceAtBrick) b;
					pab.setFormulaWithBrickField(Brick.BrickField.X_POSITION, new org.catrobat.catroid.formulaeditor.Formula(Math.round(x)));
					pab.setFormulaWithBrickField(Brick.BrickField.Y_POSITION, new org.catrobat.catroid.formulaeditor.Formula(Math.round(y)));
					return;
				}
			}
		}
		org.catrobat.catroid.content.bricks.PlaceAtBrick newPab = new org.catrobat.catroid.content.bricks.PlaceAtBrick(Math.round(x), Math.round(y));
		if (!sprite.getScriptList().isEmpty()) {
			sprite.getScriptList().get(0).addBrick(0, newPab);
		} else {
			org.catrobat.catroid.content.StartScript ss = new org.catrobat.catroid.content.StartScript();
			ss.addBrick(newPab);
			sprite.addScript(ss);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isPlayingInWindow) {
			stopInWindowPlayback();
		}
		for (Map.Entry<Sprite, int[]> entry : pendingMoves.entrySet()) {
			writePosition(entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
		}
		ProjectSaveCoordinator.saveAsync(ProjectManager.getInstance().getCurrentProject());
	}

	private void saveAndExit() {
		for (Map.Entry<Sprite, int[]> entry : pendingMoves.entrySet()) {
			writePosition(entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
		}
		btnSave.setEnabled(false);
		hintView.setText(R.string.scene_editor_hint_saving);
		new Thread(() -> {
			try {
				if (!ProjectSaveCoordinator.saveBlocking(ProjectManager.getInstance().getCurrentProject())) {
					throw new IllegalStateException("Project save failed");
				}
			} catch (Exception ignored) {
			}
			if (!isDestroyed() && !isFinishing()) {
				runOnUiThread(this::finish);
			}
		}, "scene-editor-save").start();
		setResult(RESULT_OK);
	}

private void refreshObjects(boolean full) {
		List<Bitmap> recycledBitmaps = new ArrayList<>();
		for (SceneEditorView.SceneObject previous : sceneObjects) {
			if (previous.bitmap != null && !previous.bitmap.isRecycled()) {
				recycledBitmaps.add(previous.bitmap);
			}
		}
		sceneObjects.clear();
		Bitmap background = null;
		for (Sprite sprite : scene.getSpriteList()) {
			int posX = 0, posY = 0;
			for (Script script : sprite.getScriptList()) {
				for (Brick b : script.getBrickList()) {
					if (b instanceof PlaceAtBrick) {
						try {
							posX = Math.round(Float.parseFloat(((PlaceAtBrick) b)
									.getFormulaWithBrickField(Brick.BrickField.X_POSITION).interpretString(null)));
							posY = Math.round(Float.parseFloat(((PlaceAtBrick) b)
									.getFormulaWithBrickField(Brick.BrickField.Y_POSITION).interpretString(null)));
						} catch (Exception ignored) {}
					}
				}
			}
			String lookPath = firstLookPath(sprite);
			Bitmap bitmap = lookPath != null ? loadBitmap(lookPath) : null;
			int[] bounds = lookPath != null ? readImageBounds(lookPath) : new int[] {80, 80};
			float widthPercent = readDimensionPercent(sprite, Brick.BrickField.WIDTH);
			float heightPercent = readDimensionPercent(sprite, Brick.BrickField.HEIGHT);
			SceneEditorView.SceneObject object = new SceneEditorView.SceneObject(
					sprite, posX, posY,
					Math.max(1, bounds[0] * widthPercent / 100f),
					Math.max(1, bounds[1] * heightPercent / 100f));
			object.bitmap = bitmap;
			object.isBackground = sprite == scene.getBackgroundSprite();
			sceneObjects.add(object);
			if (sprite == scene.getBackgroundSprite()) {
				background = bitmap;
			}
		}
		canvas.setBackgroundBitmap(background);
		if (full) {
			canvas.setObjects(sceneObjects);
		} else {
			canvas.updateObjects(sceneObjects);
		}
		for (Bitmap old : recycledBitmaps) {
			if (!old.isRecycled()) {
				old.recycle();
			}
		}
		if (full) {
			populateObjectDock();
		}
	}

	private void showCreateObjectSourceDialog() {
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(getString(R.string.scene_editor_new_object))
				.setItems(new String[] {getString(R.string.scene_editor_create_empty_sprite), getString(R.string.scene_editor_from_library)}, (dialog, which) -> {
					if (which == 1) {
						new ImportFormMediaLibraryLauncher(this,
								org.catrobat.catroid.common.FlavoredConstants.LIBRARY_LOOKS_URL)
								.startActivityForResult(REQUEST_OBJECT_LIBRARY);
					} else {
						showCreateObjectDialog();
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}


	private float readDimensionPercent(Sprite sprite, Brick.BrickField field) {
		for (Script script : sprite.getScriptList()) {
			for (Brick brick : script.getBrickList()) {
				try {
					if (field == Brick.BrickField.WIDTH && brick instanceof SetWidthBrick) {
						return Float.parseFloat(((SetWidthBrick) brick).getFormulaWithBrickField(field).interpretString(null));
					}
					if (field == Brick.BrickField.HEIGHT && brick instanceof SetHeightBrick) {
						return Float.parseFloat(((SetHeightBrick) brick).getFormulaWithBrickField(field).interpretString(null));
					}
				} catch (Exception ignored) {
					return 100f;
				}
			}
		}
		return 100f;
	}

private void showCreateObjectDialog() {
		EditText input = new EditText(this);
		int p = Math.round(16 * getResources().getDisplayMetrics().density);
		input.setPadding(p, p, p, p);
		input.setHint("Имя объекта");
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle(R.string.scene_editor_new_object)
				.setView(input)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					String typed = input.getText().toString().trim();
					if (typed.isEmpty()) {
						return;
					}
					String name = new UniqueNameProvider().getUniqueNameInNameables(typed, scene.getSpriteList());
					Sprite sprite = new Sprite(name);
					scene.addSprite(sprite);
					refreshAfterModelChange();
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void toggleObjectDock() {
		if (objectDock == null) {
			return;
		}
		if (objectDock.getVisibility() == View.VISIBLE) {
			objectDock.setVisibility(View.GONE);
		} else {
			populateObjectDock();
			objectDock.setVisibility(View.VISIBLE);
		}
	}

	private void persistProjectAsync() {
		ProjectSaveCoordinator.saveAsync(ProjectManager.getInstance().getCurrentProject());
	}

	private void restorePaintTarget(Bundle savedInstanceState) {
		if (savedInstanceState == null || project == null) return;
		String spriteId = savedInstanceState.getString(PAINT_TARGET_SPRITE_ID);
		int lookIndex = savedInstanceState.getInt(PAINT_TARGET_LOOK_INDEX, -1);
		if (spriteId == null || lookIndex < 0) return;
		List<Scene> scenes = new ArrayList<>(project.getSceneList());
		if (project.hasGlobalScene()) scenes.add(project.getGlobalScene());
		for (Scene candidateScene : scenes) {
			for (Sprite candidateSprite : candidateScene.getSpriteList()) {
				if (spriteId.equals(candidateSprite.getSpriteId())
						&& lookIndex < candidateSprite.getLookList().size()) {
					paintTargetSprite = candidateSprite;
					paintTargetLook = candidateSprite.getLookList().get(lookIndex);
					return;
				}
			}
		}
	}

	private void switchScene(Scene targetScene) {
		scene = targetScene;
		ProjectManager.getInstance().setCurrentlyEditedScene(targetScene);
		title.setText(getString(R.string.scene_editor_title) + ": " + scene.getName());
		refreshObjects(true);
	}

	private Bitmap loadBitmap(String path) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
			int sample = 1;
			while (Math.max(options.outWidth, options.outHeight) / sample > MAX_BITMAP_DIM) {
				sample *= 2;
			}
			options.inJustDecodeBounds = false;
			options.inSampleSize = sample;
			return BitmapFactory.decodeFile(path, options);
		} catch (Exception e) {
			return null;
		}
	}

	private int[] readImageBounds(String path) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		return new int[] {options.outWidth, options.outHeight};
	}

	private String firstLookPath(Sprite sprite) {
		if (sprite.getLookList().isEmpty()) {
			return null;
		}
		LookData look = sprite.getLookList().get(0);
		if (look.getFile() != null && look.getFile().exists()) {
			return look.getFile().getAbsolutePath();
		}
		return null;
	}

	@Override
	public void onObjectTapped(Sprite sprite) {
		showInspectorDialog(sprite);
	}

@Override
	public void onObjectPaintRequested(Sprite sprite) {
		LookData existing = firstLook(sprite);
		if (existing != null) {
			startPocketPaintForLook(sprite, existing);
			return;
		}
		startPocketPaintForNewLook(sprite);
	}

	private LookData firstLook(Sprite sprite) {
		if (sprite == null || sprite.getLookList().isEmpty()) return null;
		return sprite.getLookList().get(0);
	}

	private void startPocketPaintForNewLook(Sprite sprite) {
		paintTargetSprite = sprite;
		paintTargetLook = null;
		try {
			new ImportFromPocketPaintLauncher(this).startActivityForResult(REQUEST_POCKET_PAINT_LOOK);
		} catch (Exception e) {
			paintTargetSprite = null;
			paintTargetLook = null;
			Toast.makeText(this, "Не удалось открыть Pocket Paint", Toast.LENGTH_LONG).show();
		}
	}

	private void startPocketPaintForLook(Sprite sprite, LookData look) {
		if (sprite == null || look == null || look.getFile() == null || !look.getFile().isFile()) {
			Toast.makeText(this, "У объекта нет доступного образа для редактирования", Toast.LENGTH_LONG).show();
			return;
		}
		paintTargetSprite = sprite;
		paintTargetLook = look;
		try {
			new ImportFromPocketPaintLauncher(this).startActivityForExistingImage(
					look.getFile(), REQUEST_POCKET_PAINT_LOOK);
		} catch (Exception e) {
			paintTargetSprite = null;
			paintTargetLook = null;
			Toast.makeText(this, "Не удалось открыть образ в Pocket Paint", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onOpenBlocks(Sprite sprite) {
		openScriptCanvas(sprite);
	}

	@Override
	public void onClosed(FloatingObjectWindow window) {
		windows.remove(window);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (paintTargetSprite != null) {
			outState.putString(PAINT_TARGET_SPRITE_ID, paintTargetSprite.getSpriteId());
			outState.putInt(PAINT_TARGET_LOOK_INDEX,
					paintTargetLook == null ? -1 : paintTargetSprite.getLookList().indexOf(paintTargetLook));
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == StageActivity.REQUEST_START_STAGE) {
			org.catrobat.catroid.content.GlobalManager.setStopSounds(true);
			canvas.setPlayingMode(false);
			refreshObjects(true);
			hintView.setText(R.string.scene_editor_hint_stop);
			return;
		}
if (resultCode != RESULT_OK && (requestCode == REQUEST_LOOK_FILE
				|| requestCode == REQUEST_LOOK_CAMERA)) {
			pendingLookSprite = null;
			return;
		}
		if (resultCode != RESULT_OK && (requestCode == REQUEST_SOUND_FILE
				|| requestCode == REQUEST_SOUND_RECORD)) {
			pendingSoundSprite = null;
			return;
		}
		if (resultCode == RESULT_OK && requestCode == REQUEST_OBJECT_LIBRARY && data != null) {
			addObjectFromLibrary(data);
			return;
		}
		if (resultCode == RESULT_OK && pendingLookSprite != null
				&& (requestCode == REQUEST_LOOK_FILE || requestCode == REQUEST_LOOK_CAMERA)) {
			try {
				Uri uri = requestCode == REQUEST_LOOK_CAMERA
						? new ImportFromCameraLauncher(this).getCacheCameraUri() : (data != null ? data.getData() : null);
				addLookFromUri(pendingLookSprite, uri);
				Toast.makeText(this, "Образ добавлен!", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(this, "Не удалось добавить образ", Toast.LENGTH_SHORT).show();
			} finally {
				pendingLookSprite = null;
			}
			return;
		}
		if (resultCode == RESULT_OK && pendingSoundSprite != null
				&& (requestCode == REQUEST_SOUND_FILE || requestCode == REQUEST_SOUND_RECORD)) {
			try {
				addSoundFromUri(pendingSoundSprite, data != null ? data.getData() : null);
				Toast.makeText(this, "Звук добавлен!", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(this, "Не удалось добавить звук", Toast.LENGTH_SHORT).show();
			} finally {
				pendingSoundSprite = null;
			}
			return;
		}
		if (requestCode == REQUEST_POCKET_PAINT_LOOK && resultCode == RESULT_OK && paintTargetSprite != null) {
			try {
				Uri uri = new ImportFromPocketPaintLauncher(this).getPocketPaintCacheUri();
				if (paintTargetLook != null && paintTargetLook.getFile() != null) {
					File editedFile = paintTargetLook.getFile();
					StorageOperations.copyUriToDir(getContentResolver(), uri,
							editedFile.getParentFile(), editedFile.getName());
					paintTargetLook.getCollisionInformation().calculate();
					refreshAfterModelChange();
					persistProjectAsync();
					Toast.makeText(this, "Образ обновлён!", Toast.LENGTH_SHORT).show();
					paintTargetLook = null;
					paintTargetSprite = null;
					return;
				}
				if (uri == null) throw new IOException("Изображение Pocket Paint не найдено");
				String name = new UniqueNameProvider().getUniqueNameInNameables(
						paintTargetSprite.getName() + "_paint", paintTargetSprite.getLookList());
				File imageDirectory = new File(scene.getDirectory(), Constants.IMAGE_DIRECTORY_NAME);
				if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
					throw new IOException("Не удалось создать папку образов");
				}
				File file = StorageOperations.copyUriToDir(getContentResolver(), uri, imageDirectory, name + ".png");
				if (file == null || !file.exists()) throw new IOException("Файл образа не скопирован");
				LookData look = new LookData(name, file);
				paintTargetSprite.getLookList().add(look);
				look.getCollisionInformation().calculate();
				refreshAfterModelChange();
				persistProjectAsync();
				Toast.makeText(this, "Образ добавлен!", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(this, "Не удалось добавить образ", Toast.LENGTH_SHORT).show();
			}
			paintTargetSprite = null;
			paintTargetLook = null;
		}
	}

	private void addObjectFromLibrary(Intent data) {
		Sprite imported = null;
		try {
			String path = data.getStringExtra(org.catrobat.catroid.ui.WebViewActivity.MEDIA_FILE_PATH);
			if (path == null) throw new IOException("Library file is missing");
			File source = new File(path);
			String baseName = source.getName();
			int dot = baseName.lastIndexOf('.');
			if (dot > 0) baseName = baseName.substring(0, dot);
			String name = new UniqueNameProvider().getUniqueNameInNameables(baseName, scene.getSpriteList());
			imported = new Sprite(name);
			scene.addSprite(imported);
			File imageDirectory = new File(scene.getDirectory(), Constants.IMAGE_DIRECTORY_NAME);
			if (!imageDirectory.exists() && !imageDirectory.mkdirs()) throw new IOException("Image directory unavailable");
			File file = StorageOperations.copyUriToDir(getContentResolver(), Uri.fromFile(source), imageDirectory, name + ".png");
			if (file == null || !file.exists()) throw new IOException("Library image copy failed");
			LookData look = new LookData(name, file);
			imported.getLookList().add(look);
			look.getCollisionInformation().calculate();
			refreshAfterModelChange();
			Toast.makeText(this, getString(R.string.scene_editor_object_imported), Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			if (imported != null) scene.getSpriteList().remove(imported);
			Toast.makeText(this, getString(R.string.scene_editor_import_failed), Toast.LENGTH_SHORT).show();
		}
	}

	private void addLookFromUri(Sprite targetSprite, Uri uri) throws IOException {
		if (uri == null) throw new IOException("URI изображения отсутствует");
		String name = new UniqueNameProvider().getUniqueNameInNameables(
				targetSprite.getName() + "_look", targetSprite.getLookList());
		File imageDirectory = new File(scene.getDirectory(), Constants.IMAGE_DIRECTORY_NAME);
		if (!imageDirectory.exists() && !imageDirectory.mkdirs()) throw new IOException("Нет папки образов");
		File file = StorageOperations.copyUriToDir(getContentResolver(), uri, imageDirectory, name + ".png");
		if (file == null || !file.exists()) throw new IOException("Образ не скопирован");
		LookData look = new LookData(name, file);
		targetSprite.getLookList().add(look);
		look.getCollisionInformation().calculate();
		refreshAfterModelChange();
		persistProjectAsync();
	}

	private void addSoundFromUri(Sprite targetSprite, Uri uri) throws IOException {
		if (uri == null) throw new IOException("URI звука отсутствует");
		String name = new UniqueNameProvider().getUniqueNameInNameables(
				targetSprite.getName() + "_sound", targetSprite.getSoundList());
		File soundDirectory = new File(scene.getDirectory(), Constants.SOUND_DIRECTORY_NAME);
		if (!soundDirectory.exists() && !soundDirectory.mkdirs()) throw new IOException("Нет папки звуков");
		File file = StorageOperations.copyUriToDir(getContentResolver(), uri, soundDirectory, name + ".mp3");
		if (file == null || !file.exists()) throw new IOException("Звук не скопирован");
		targetSprite.getSoundList().add(new SoundInfo(name, file));
		refreshAfterModelChange();
		persistProjectAsync();
	}
}
