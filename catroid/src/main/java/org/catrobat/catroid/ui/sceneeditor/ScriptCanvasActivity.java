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

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.FormulaBrick;
import org.catrobat.catroid.content.bricks.SubCategoryHeaderBrick;
import org.catrobat.catroid.io.XstreamSerializer;
import org.catrobat.catroid.ui.fragment.CategoryBricksFactory;

import java.util.ArrayList;
import java.util.List;

public class ScriptCanvasActivity extends AppCompatActivity {

	public static final String EXTRA_SPRITE_NAME = "extra_sprite_name";
	private static final int TAB_SCRIPTS = 0;

	private Project project;
	private Scene scene;
	private Sprite sprite;
	private FrameLayout canvasContainer;
	private ScriptCanvasView canvas;
	private View palettePanel;
	private LinearLayout paletteCategories;
	private LinearLayout paletteBricks;
	private FrameLayout dragLayer;
	private View dragGhost;
	private boolean isBackground;
	private boolean paletteBuilt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_script_canvas);
		if (getSupportActionBar() != null) {
			getSupportActionBar().hide();
		}

		project = ProjectManager.getInstance().getCurrentProject();
		scene = ProjectManager.getInstance().getCurrentlyEditedScene();
		sprite = ProjectManager.getInstance().getCurrentSprite();

		if (getIntent() != null && getIntent().hasExtra(EXTRA_SPRITE_NAME)) {
			String spriteName = getIntent().getStringExtra(EXTRA_SPRITE_NAME);
			if (scene != null) {
				for (Sprite s : scene.getSpriteList()) {
					if (s.getName().equals(spriteName)) {
						sprite = s;
						break;
					}
				}
			}
		}

		if (project == null || scene == null || sprite == null) {
			finish();
			return;
		}

		TextView title = findViewById(R.id.script_canvas_title);
		title.setText(sprite.getName());
		setupObjectTabs();

		ImageButton close = findViewById(R.id.script_canvas_btn_close);
		close.setOnClickListener(v -> finish());

		canvasContainer = findViewById(R.id.script_canvas_container);
		TextView empty = findViewById(R.id.script_canvas_empty);
		empty.setVisibility(sprite.getScriptList().isEmpty() ? TextView.VISIBLE : TextView.GONE);

		canvas = new ScriptCanvasView(this);
		canvas.setSprite(sprite);
		canvasContainer.addView(canvas, 0);

		isBackground = scene.getBackgroundSprite() == sprite;
		palettePanel = findViewById(R.id.script_palette);
		paletteCategories = findViewById(R.id.script_palette_categories);
		paletteBricks = findViewById(R.id.script_palette_bricks);
		dragLayer = findViewById(R.id.script_canvas_drag_layer);
		ImageButton undoButton = findViewById(R.id.script_canvas_btn_undo);
		if (undoButton != null) {
			undoButton.setEnabled(false);
			undoButton.setAlpha(0.4f);
			undoButton.setOnClickListener(v -> canvas.undo());
		}
		ImageButton redoButton = findViewById(R.id.script_canvas_btn_redo);
		if (redoButton != null) {
			redoButton.setEnabled(false);
			redoButton.setAlpha(0.4f);
			redoButton.setOnClickListener(v -> canvas.redo());
		}
		canvas.setUndoRedoListener(this::updateUndoRedoButtons);

		ImageButton dataButton = findViewById(R.id.script_canvas_btn_data);
		if (dataButton != null) {
			dataButton.setOnClickListener(v -> showDataManagerDialog());
		}

		ImageButton importNeoButton = findViewById(R.id.script_canvas_btn_import_neoscript);
		if (importNeoButton != null) {
			importNeoButton.setOnClickListener(v -> handleImportNeoScript());
		}

		ImageButton backpackButton = findViewById(R.id.script_canvas_btn_backpack);
		backpackButton.setOnClickListener(v -> {
			Intent intent = new Intent(this, org.catrobat.catroid.ui.recyclerview.backpack.BackpackActivity.class);
			intent.putExtra(org.catrobat.catroid.ui.recyclerview.backpack.BackpackActivity.EXTRA_FRAGMENT_POSITION,
					org.catrobat.catroid.ui.recyclerview.backpack.BackpackActivity.FRAGMENT_SCRIPTS);
			startActivity(intent);
		});

		ImageButton paletteButton = findViewById(R.id.script_canvas_btn_palette);
		if (paletteButton != null) {
			paletteButton.setOnClickListener(v -> togglePalette());
		}

		EditText searchEdit = findViewById(R.id.script_palette_search);
		if (searchEdit != null) {
			searchEdit.addTextChangedListener(new android.text.TextWatcher() {
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
				@Override
				public void afterTextChanged(android.text.Editable s) {
					filterBricksBySearch(s.toString().trim());
				}
			});
		}
	}

	private void setupObjectTabs() {
		TextView scripts = findViewById(R.id.script_canvas_tab_scripts);
		TextView looks = findViewById(R.id.script_canvas_tab_looks);
		TextView sounds = findViewById(R.id.script_canvas_tab_sounds);
		if (scripts == null || looks == null || sounds == null) return;
		selectObjectTab(scripts, looks, sounds);
		scripts.setOnClickListener(v -> selectObjectTab(scripts, looks, sounds));
		looks.setOnClickListener(v -> openLegacyObjectTab(1));
		sounds.setOnClickListener(v -> openLegacyObjectTab(2));
	}

	private void selectObjectTab(TextView scripts, TextView looks, TextView sounds) {
		scripts.setTextColor(0xFFFFFFFF);
		looks.setTextColor(0xFF94A3B8);
		sounds.setTextColor(0xFF94A3B8);
	}

	private void openLegacyObjectTab(int fragmentPosition) {
		ProjectManager.getInstance().setCurrentSprite(sprite);
		Intent intent = new Intent(this, org.catrobat.catroid.ui.SpriteActivity.class);
		intent.putExtra(org.catrobat.catroid.ui.SpriteActivity.EXTRA_FRAGMENT_POSITION, fragmentPosition);
		intent.putExtra(org.catrobat.catroid.ui.SpriteActivity.EXTRA_FORCE_LEGACY_EDITOR, true);
		startActivity(intent);
	}

	private static final int[] PALETTE_CATEGORIES = {
			R.string.category_event, R.string.category_control, R.string.category_motion,
			R.string.category_looks, R.string.category_sound, R.string.category_pen,
			R.string.category_data, R.string.category_device, R.string.category_file,
			R.string.category_neoscript, R.string.category_pathfinder, R.string.category_preload,
			R.string.category_admob, R.string.category_transitions,
			R.string.category_threed, R.string.category_json, R.string.category_internet,
			R.string.category_user_bricks, R.string.category_lego_nxt, R.string.category_lego_ev3,
			R.string.category_arduino, R.string.category_drone, R.string.category_jumping_sumo,
			R.string.category_phiro, R.string.category_cast, R.string.category_raspi,
			R.string.category_embroidery, R.string.category_plot, R.string.category_neural,
			R.string.pocketensor, R.string.fast2d, R.string.category_assertions,
			R.string.category_libraries
	};

	private void togglePalette() {
		if (palettePanel.getVisibility() == View.VISIBLE) {
			palettePanel.setVisibility(View.GONE);
			return;
		}
		if (!paletteBuilt) {
			buildPaletteCategories();
			paletteBuilt = true;
			showCategoryBricks(PALETTE_CATEGORIES[0]);
		}
		palettePanel.setVisibility(View.VISIBLE);
	}

	private void buildPaletteCategories() {
		paletteCategories.removeAllViews();
		for (int categoryRes : PALETTE_CATEGORIES) {
			Button chip = new Button(this);
			chip.setText(categoryRes);
			chip.setAllCaps(false);
			final int res = categoryRes;
			chip.setOnClickListener(v -> showCategoryBricks(res));
			paletteCategories.addView(chip);
		}
	}

	private final android.os.Handler paletteHandler = new android.os.Handler(android.os.Looper.getMainLooper());
	private Runnable paletteLoadingRunnable;

	private void showCategoryBricks(int categoryRes) {
		if (paletteLoadingRunnable != null) {
			paletteHandler.removeCallbacks(paletteLoadingRunnable);
			paletteLoadingRunnable = null;
		}
		paletteBricks.removeAllViews();
		android.widget.ScrollView paletteScroll = findViewById(R.id.script_palette_scroll);
		if (paletteScroll != null) {
			paletteScroll.scrollTo(0, 0);
		}
		List<Brick> bricks;
		try {
			bricks = new CategoryBricksFactory().getBricks(getString(categoryRes), isBackground, this);
		} catch (Exception e) {
			return;
		}

		final List<Brick> validBricks = new ArrayList<>();
		for (Brick b : bricks) {
			if (!(b instanceof SubCategoryHeaderBrick)) {
				validBricks.add(b);
			}
		}

		final int batchSize = 4;
		paletteLoadingRunnable = new Runnable() {
			private int currentIndex = 0;

			@Override
			public void run() {
				if (isFinishing() || isDestroyed()) {
					return;
				}
				int limit = Math.min(currentIndex + batchSize, validBricks.size());
				for (int i = currentIndex; i < limit; i++) {
					Brick brick = validBricks.get(i);
					try {
						View itemView = brick.getPrototypeView(ScriptCanvasActivity.this);
						itemView.setOnTouchListener(new PaletteDragListener(brick));
						itemView.setAlpha(0f);
						itemView.setTranslationY(dp(12));

					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					params.bottomMargin = dp(8);
					paletteBricks.addView(itemView, params);

					itemView.animate()
								.alpha(1f)
								.translationY(0f)
								.setDuration(180)
								.start();
					} catch (Exception ignored) {
					}
				}
				currentIndex = limit;
				if (currentIndex < validBricks.size()) {
					paletteHandler.postDelayed(this, 16);
				} else {
					paletteLoadingRunnable = null;
				}
			}
		};
		paletteHandler.post(paletteLoadingRunnable);
	}

	private void startGhost(Brick prototype, float rawX, float rawY) {
		removeGhost();
		View ghost;
		try {
			ghost = prototype.getPrototypeView(this);
		} catch (Exception e) {
			return;
		}
		ghost.setAlpha(0.85f);
		dragGhost = ghost;
		dragLayer.addView(ghost, new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT));
		moveGhost(rawX, rawY);
	}

	private void moveGhost(float rawX, float rawY) {
		if (dragGhost == null) {
			return;
		}
		int[] location = new int[2];
		dragLayer.getLocationOnScreen(location);
		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) dragGhost.getLayoutParams();
		params.leftMargin = Math.round(rawX - location[0] - dp(24));
		params.topMargin = Math.round(rawY - location[1] - dp(16));
		dragGhost.setLayoutParams(params);
	}

	private void removeGhost() {
		if (dragGhost != null) {
			dragLayer.removeView(dragGhost);
			dragGhost = null;
		}
	}

	private int dp(float value) {
		return Math.round(value * getResources().getDisplayMetrics().density);
	}

	private class PaletteDragListener implements View.OnTouchListener {
		private final Brick prototype;

		PaletteDragListener(Brick prototype) {
			this.prototype = prototype;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					startGhost(prototype, event.getRawX(), event.getRawY());
					return true;
				case MotionEvent.ACTION_MOVE:
					moveGhost(event.getRawX(), event.getRawY());
					return true;
				case MotionEvent.ACTION_UP:
					removeGhost();
					if (canvas.dropPrototypeAtScreen(prototype, event.getRawX(), event.getRawY())) {
						palettePanel.setVisibility(View.GONE);
					}
					return true;
				case MotionEvent.ACTION_CANCEL:
					removeGhost();
					return true;
				default:
					return false;
			}
		}
	}

	private static final int REQUEST_NEO_SCRIPT = 9021;

	private void handleImportNeoScript() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(Intent.createChooser(intent, "Выберите модуль .neoscript"), REQUEST_NEO_SCRIPT);
	}

	private FormulaBrick activeFormulaBrick = null;
	private Brick.FormulaField activeFormulaField = null;

	public void setActiveEditFormula(FormulaBrick brick, Brick.FormulaField field) {
		this.activeFormulaBrick = brick;
		this.activeFormulaField = field;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 8899 && resultCode == RESULT_OK && data != null) {
			String res = data.getStringExtra(org.catrobat.catroid.ui.formulaeditor.FormulaEditor2Activity.EXTRA_RESULT_FORMULA_STRING);
			if (res != null && activeFormulaBrick != null && activeFormulaField != null) {
				canvas.snapshot();
				activeFormulaBrick.setFormulaWithBrickField(activeFormulaField, new org.catrobat.catroid.formulaeditor.Formula(res));
				canvas.rebuild();
				Toast.makeText(this, "Формула обновлена!", Toast.LENGTH_SHORT).show();
			}
		} else if (requestCode == REQUEST_NEO_SCRIPT && resultCode == RESULT_OK && data != null && data.getData() != null) {
			try {
				java.io.InputStream is = getContentResolver().openInputStream(data.getData());
				java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
				byte[] chunk = new byte[8192];
				int read;
				while ((read = is.read(chunk)) != -1) {
					buffer.write(chunk, 0, read);
				}
				is.close();
				String xml = buffer.toString("UTF-8");
				org.catrobat.catroid.neoscript.NeoScriptFile file = org.catrobat.catroid.neoscript.NeoScriptSerializer.deserializeFromString(xml);
				org.catrobat.catroid.neoscript.NeoScriptImporter.importScripts(file, project, sprite, org.catrobat.catroid.neoscript.NeoScriptImporter.ImportStrategy.SKIP_DUPLICATES);
				canvas.rebuild();
				Toast.makeText(this, "Модуль NeoScript успешно импортирован!", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				Toast.makeText(this, "Ошибка импорта NeoScript: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void filterBricksBySearch(String query) {
		if (query.isEmpty()) {
			showCategoryBricks(PALETTE_CATEGORIES[0]);
			return;
		}
		String lowerQuery = query.toLowerCase();
		paletteBricks.removeAllViews();
		List<Brick> allMatched = new ArrayList<>();
		for (int catRes : PALETTE_CATEGORIES) {
			try {
				List<Brick> bricks = new CategoryBricksFactory().getBricks(getString(catRes), isBackground, this);
				for (Brick b : bricks) {
					if (!(b instanceof SubCategoryHeaderBrick)) {
						// Fast path: match by class name
						if (b.getClass().getSimpleName().toLowerCase().contains(lowerQuery)) {
							allMatched.add(b);
							continue;
						}
						// Slow path: match by localized text from prototype view
						try {
							String brickText = extractAllText(b.getPrototypeView(this)).toLowerCase();
							if (brickText.contains(lowerQuery)) {
								allMatched.add(b);
							}
						} catch (Exception ignored) {}
					}
				}
			} catch (Exception ignored) {}
		}
		for (Brick brick : allMatched) {
			try {
				View itemView = brick.getPrototypeView(this);
				itemView.setOnTouchListener(new PaletteDragListener(brick));
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params.bottomMargin = dp(8);
			paletteBricks.addView(itemView, params);
			} catch (Exception ignored) {}
		}
	}

	private void showDataManagerDialog() {
		float density = getResources().getDisplayMetrics().density;
		int dp8 = Math.round(8 * density);
		int dp10 = Math.round(10 * density);
		int dp12 = Math.round(12 * density);

		ScrollView scrollView = new ScrollView(this);
		LinearLayout container = new LinearLayout(this);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setPadding(dp12, dp12, dp12, dp12);
		scrollView.addView(container);

		AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Менеджер Данных 2.0")
				.setView(scrollView)
				.setNegativeButton(android.R.string.cancel, null)
				.create();

		addDataBtn(container, "➕ Переменная проекта (глобальная)", 0xFF38BDF8, dp10, dp8,
				() -> { dialog.dismiss(); createVariableDialog(); });
		addDataBtn(container, "➕ Переменная объекта «" + sprite.getName() + "»", 0xFF34D399, dp10, dp8,
				() -> { dialog.dismiss(); createSpriteVariableDialog(); });
		addDataBtn(container, "➕ Список проекта (глобальный)", 0xFFFBBF24, dp10, dp8,
				() -> { dialog.dismiss(); createListDialog(); });
		addDataBtn(container, "➕ Список объекта «" + sprite.getName() + "»", 0xFFF97316, dp10, dp8,
				() -> { dialog.dismiss(); createSpriteListDialog(); });

		addSectionTitle(container, "📌 Переменные проекта", 0xFFF8FAFC, dp10);
		List<org.catrobat.catroid.formulaeditor.UserVariable> projectVars = project.getUserVariables();
		if (projectVars == null || projectVars.isEmpty()) {
			addSmallLabel(container, "  Нет глобальных переменных", dp8);
		} else {
			for (org.catrobat.catroid.formulaeditor.UserVariable uv : new ArrayList<>(projectVars)) {
				addDeletableRow(container, uv.getName(), 0xFF94A3B8, dp10, dp8,
						() -> { projectVars.remove(uv); dialog.dismiss(); showDataManagerDialog(); });
			}
		}

		addSectionTitle(container, "🎮 Переменные: " + sprite.getName(), 0xFF6EE7B7, dp10);
		List<org.catrobat.catroid.formulaeditor.UserVariable> spriteVars = sprite.getUserVariables();
		if (spriteVars == null || spriteVars.isEmpty()) {
			addSmallLabel(container, "  Нет локальных переменных", dp8);
		} else {
			for (org.catrobat.catroid.formulaeditor.UserVariable uv : new ArrayList<>(spriteVars)) {
				addDeletableRow(container, uv.getName(), 0xFF6EE7B7, dp10, dp8,
						() -> { spriteVars.remove(uv); dialog.dismiss(); showDataManagerDialog(); });
			}
		}

		addSectionTitle(container, "📋 Списки проекта", 0xFFF8FAFC, dp10);
		List<org.catrobat.catroid.formulaeditor.UserList> projectLists = project.getUserLists();
		if (projectLists == null || projectLists.isEmpty()) {
			addSmallLabel(container, "  Нет глобальных списков", dp8);
		} else {
			for (org.catrobat.catroid.formulaeditor.UserList ul : new ArrayList<>(projectLists)) {
				addDeletableRow(container, ul.getName(), 0xFFFBBF24, dp10, dp8,
						() -> { projectLists.remove(ul); dialog.dismiss(); showDataManagerDialog(); });
			}
		}

		addSectionTitle(container, "📋 Списки: " + sprite.getName(), 0xFFFDA4AF, dp10);
		List<org.catrobat.catroid.formulaeditor.UserList> spriteLists = sprite.getUserLists();
		if (spriteLists == null || spriteLists.isEmpty()) {
			addSmallLabel(container, "  Нет локальных списков", dp8);
		} else {
			for (org.catrobat.catroid.formulaeditor.UserList ul : new ArrayList<>(spriteLists)) {
				addDeletableRow(container, ul.getName(), 0xFFFDA4AF, dp10, dp8,
						() -> { spriteLists.remove(ul); dialog.dismiss(); showDataManagerDialog(); });
			}
		}

		dialog.show();
	}

	private void addDataBtn(LinearLayout container, String label, int color, int pad, int margin, Runnable action) {
		TextView btn = new TextView(this);
		btn.setText(label);
		btn.setTextColor(color);
		btn.setTextSize(13f);
		btn.setTypeface(null, android.graphics.Typeface.BOLD);
		btn.setPadding(pad, pad, pad, pad);
		btn.setBackgroundResource(R.drawable.bg_object_card_cube);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		lp.bottomMargin = margin;
		btn.setLayoutParams(lp);
		btn.setOnClickListener(v -> action.run());
		container.addView(btn);
	}

	private void addSectionTitle(LinearLayout container, String text, int color, int pad) {
		TextView hdr = new TextView(this);
		hdr.setText(text);
		hdr.setTextColor(color);
		hdr.setTextSize(13f);
		hdr.setTypeface(null, android.graphics.Typeface.BOLD);
		hdr.setPadding(0, pad, 0, Math.round(4 * getResources().getDisplayMetrics().density));
		container.addView(hdr);
	}

	private void addSmallLabel(LinearLayout container, String text, int pad) {
		TextView tv = new TextView(this);
		tv.setText(text);
		tv.setTextColor(0xFF475569);
		tv.setTextSize(12f);
		tv.setPadding(pad, pad, pad, pad);
		container.addView(tv);
	}

	private void addDeletableRow(LinearLayout container, String name, int nameColor, int pad, int margin, Runnable onDelete) {
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		row.setBackgroundResource(R.drawable.bg_object_card_cube);
		row.setPadding(pad, pad, pad, pad);
		row.setGravity(android.view.Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		rowLp.bottomMargin = margin;
		row.setLayoutParams(rowLp);
		TextView nameTv = new TextView(this);
		nameTv.setText(name);
		nameTv.setTextColor(nameColor);
		nameTv.setTextSize(13f);
		LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		nameTv.setLayoutParams(nameLp);
		row.addView(nameTv);
		TextView delBtn = new TextView(this);
		delBtn.setText("🗑");
		delBtn.setTextSize(16f);
		delBtn.setPadding(pad, 0, 0, 0);
		delBtn.setOnClickListener(v ->
				new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
						.setTitle("Удалить «" + name + "»?")
						.setPositiveButton(android.R.string.ok, (d, w) -> onDelete.run())
						.setNegativeButton(android.R.string.cancel, null)
						.show());
		row.addView(delBtn);
		container.addView(row);
	}

	private void createVariableDialog() {
		EditText input = new EditText(this);
		input.setHint("Имя переменной");
		int p = dp(16);
		input.setPadding(p, p, p, p);

		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Новая переменная")
				.setView(input)
				.setPositiveButton("Создать", (dialog, which) -> {
					String name = input.getText().toString().trim();
					if (!name.isEmpty()) {
						org.catrobat.catroid.formulaeditor.UserVariable uv = new org.catrobat.catroid.formulaeditor.UserVariable(name);
						project.getUserVariables().add(uv);
						Toast.makeText(this, "Переменная " + name + " создана!", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("Отмена", null)
				.show();
	}

	private void createListDialog() {
		EditText input = new EditText(this);
		input.setHint("Имя списка");
		int p = dp(16);
		input.setPadding(p, p, p, p);

		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Новый список")
				.setView(input)
				.setPositiveButton("Создать", (dialog, which) -> {
					String name = input.getText().toString().trim();
					if (!name.isEmpty()) {
						org.catrobat.catroid.formulaeditor.UserList ul = new org.catrobat.catroid.formulaeditor.UserList(name);
						project.getUserLists().add(ul);
						Toast.makeText(this, "Список " + name + " создан!", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("Отмена", null)
				.show();
	}

	private void createSpriteVariableDialog() {
		EditText input = new EditText(this);
		input.setHint("Имя переменной объекта");
		int p = dp(16);
		input.setPadding(p, p, p, p);
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Переменная объекта «" + sprite.getName() + "»")
				.setView(input)
				.setPositiveButton("Создать", (dialog, which) -> {
					String name = input.getText().toString().trim();
					if (!name.isEmpty()) {
						sprite.getUserVariables().add(new org.catrobat.catroid.formulaeditor.UserVariable(name));
						Toast.makeText(this, "Переменная " + name + " создана!", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("Отмена", null)
				.show();
	}

	private void createSpriteListDialog() {
		EditText input = new EditText(this);
		input.setHint("Имя списка объекта");
		int p = dp(16);
		input.setPadding(p, p, p, p);
		new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
				.setTitle("Список объекта «" + sprite.getName() + "»")
				.setView(input)
				.setPositiveButton("Создать", (dialog, which) -> {
					String name = input.getText().toString().trim();
					if (!name.isEmpty()) {
						sprite.getUserLists().add(new org.catrobat.catroid.formulaeditor.UserList(name));
						Toast.makeText(this, "Список " + name + " создан!", Toast.LENGTH_SHORT).show();
					}
				})
				.setNegativeButton("Отмена", null)
				.show();
	}

	private void updateUndoRedoButtons() {
		ImageButton undoBtn = findViewById(R.id.script_canvas_btn_undo);
		ImageButton redoBtn = findViewById(R.id.script_canvas_btn_redo);
		if (undoBtn != null) {
			undoBtn.setEnabled(canvas.canUndo());
			undoBtn.setAlpha(canvas.canUndo() ? 1f : 0.4f);
		}
		if (redoBtn != null) {
			redoBtn.setEnabled(canvas.canRedo());
			redoBtn.setAlpha(canvas.canRedo() ? 1f : 0.4f);
		}
	}

	private String extractAllText(View view) {
		if (view instanceof TextView) {
			CharSequence text = ((TextView) view).getText();
			return text != null ? text.toString() : "";
		}
		if (view instanceof ViewGroup) {
			StringBuilder sb = new StringBuilder();
			ViewGroup vg = (ViewGroup) view;
			for (int i = 0; i < vg.getChildCount(); i++) {
				String child = extractAllText(vg.getChildAt(i));
				if (!child.isEmpty()) sb.append(child).append(' ');
			}
			return sb.toString().trim();
		}
		return "";
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (canvas != null) {
			outState.putFloat("canvas_pan_x", canvas.getPanX());
			outState.putFloat("canvas_pan_y", canvas.getPanY());
			outState.putFloat("canvas_scale", canvas.getScale());
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (canvas != null) {
			canvas.restorePanAndScale(
					savedInstanceState.getFloat("canvas_pan_x", 0f),
					savedInstanceState.getFloat("canvas_pan_y", 0f),
					savedInstanceState.getFloat("canvas_scale", 1f));
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (project != null) {
			new Thread(() -> {
				try {
					XstreamSerializer.getInstance().saveProject(project);
				} catch (Exception ignored) {
				}
			}, "script-canvas-save").start();
		}
	}
}
