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
package org.catrobat.catroid.ui.tilemap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.TilemapLookData;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.io.StorageOperations;
import org.catrobat.catroid.io.XstreamSerializer;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public class TilemapEditorActivity extends Activity {

	public static final String EXTRA_LOOK_INDEX = "extra_look_index";
	public static final String EXTRA_NEW_TILEMAP = "extra_new_tilemap";
	public static final int REQUEST_PICK_TILESET = 1001;

	private TilemapEditorView editorView;
	private TilemapEditorView.PaletteView paletteView;
	private TextView hintView;
	private ImageButton btnUndo;
	private ImageButton btnRedo;
	private ImageButton btnSave;
	private android.widget.CompoundButton solidToggle;

	private TilemapLookData tilemapData;
	private int lookIndex = -1;
	private boolean isNewTilemap = false;
	private boolean hasUnsavedChanges = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tilemap_editor);

		editorView = findViewById(R.id.tilemap_canvas);
		paletteView = findViewById(R.id.tilemap_palette);
		hintView = findViewById(R.id.tilemap_hint);
		btnUndo = findViewById(R.id.tilemap_btn_undo);
		btnRedo = findViewById(R.id.tilemap_btn_redo);
		btnSave = findViewById(R.id.tilemap_btn_save);
		solidToggle = findViewById(R.id.tilemap_solid_toggle);
		ImageButton btnBack = findViewById(R.id.tilemap_btn_back);
		TextView title = findViewById(R.id.tilemap_title);
		TextView btnMapSize = findViewById(R.id.tilemap_btn_map_size);
		TextView btnTileSize = findViewById(R.id.tilemap_btn_tile_size);
		TextView btnTileset = findViewById(R.id.tilemap_btn_tileset);

		lookIndex = getIntent().getIntExtra(EXTRA_LOOK_INDEX, -1);
		isNewTilemap = getIntent().getBooleanExtra(EXTRA_NEW_TILEMAP, false);
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		if (sprite == null) {
			finish();
			return;
		}

		if (isNewTilemap) {
			tilemapData = new TilemapLookData("tilemap_" + UUID.randomUUID().toString().substring(0, 6));
			tilemapData.setMapSize(16, 12);
			sprite.getLookList().add(tilemapData);
			lookIndex = sprite.getLookList().size() - 1;
		} else if (lookIndex >= 0 && lookIndex < sprite.getLookList().size()) {
			LookData look = sprite.getLookList().get(lookIndex);
			if (look instanceof TilemapLookData) {
				tilemapData = (TilemapLookData) look;
			} else {
				finish();
				return;
			}
		} else {
			finish();
			return;
		}

		title.setText(getString(R.string.tilemap_editor_title) + ": " + tilemapData.getName());
		editorView.setData(tilemapData);
		paletteView.setEditor(editorView);

		editorView.setOnChangeListener(() -> {
			hasUnsavedChanges = true;
			hintView.setText(R.string.tilemap_hint_modified);
			updateUndoRedoButtons();
		});
		editorView.setOnPaletteTileListener(tileIndex -> {
			solidToggle.setChecked(tilemapData.isSolidTile(tileIndex));
		});

		btnBack.setOnClickListener(v -> confirmDiscardAndFinish());
		btnUndo.setOnClickListener(v -> performUndo());
		btnRedo.setOnClickListener(v -> performRedo());
		btnSave.setOnClickListener(v -> saveAndExit());

		solidToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
			int tile = editorView.getCurrentTile();
			if (tile >= 0) {
				boolean wasSolid = tilemapData.isSolidTile(tile);
				if (wasSolid != isChecked) {
					tilemapData.setTileSolid(tile, isChecked);
					editorView.getHistory().beginBatch();
					editorView.getHistory().recordSolidChange(tile, wasSolid, isChecked);
					editorView.getHistory().commitBatch();
					hasUnsavedChanges = true;
					hintView.setText(R.string.tilemap_hint_modified);
					updateUndoRedoButtons();
				}
			}
		});

		btnMapSize.setOnClickListener(v -> showMapSizeDialog());
		btnTileSize.setOnClickListener(v -> showTileSizeDialog());
		btnTileset.setOnClickListener(v -> showTilesetPicker());

		updateUndoRedoButtons();
	}

	private void updateUndoRedoButtons() {
		btnUndo.setEnabled(editorView.getHistory().canUndo());
		btnRedo.setEnabled(editorView.getHistory().canRedo());
	}

	private void performUndo() {
		List<Object> batch = editorView.getHistory().undo();
		if (batch != null) {
			editorView.applyBatch(batch, false);
			updateUndoRedoButtons();
		}
	}

	private void performRedo() {
		List<Object> batch = editorView.getHistory().redo();
		if (batch != null) {
			editorView.applyBatch(batch, true);
			updateUndoRedoButtons();
		}
	}

	private void showMapSizeDialog() {
		EditText colsInput = new EditText(this);
		colsInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
		colsInput.setText(String.valueOf(tilemapData.getMapColumns()));
		EditText rowsInput = new EditText(this);
		rowsInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
		rowsInput.setText(String.valueOf(tilemapData.getMapRows()));

		android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
		layout.setOrientation(android.widget.LinearLayout.VERTICAL);
		layout.setPadding(32, 16, 32, 16);
		TextView colsLabel = new TextView(this);
		colsLabel.setText(R.string.tilemap_columns);
		layout.addView(colsLabel);
		layout.addView(colsInput);
		TextView rowsLabel = new TextView(this);
		rowsLabel.setText(R.string.tilemap_rows);
		layout.addView(rowsLabel);
		layout.addView(rowsInput);

		new AlertDialog.Builder(this)
				.setTitle(R.string.tilemap_map_size)
				.setView(layout)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					try {
						int cols = Integer.parseInt(colsInput.getText().toString().trim());
						int rows = Integer.parseInt(rowsInput.getText().toString().trim());
						if (cols > 0 && rows > 0 && cols <= 200 && rows <= 200) {
							tilemapData.setMapSize(cols, rows);
							editorView.setData(tilemapData);
							hasUnsavedChanges = true;
						}
					} catch (NumberFormatException ignored) {
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showTileSizeDialog() {
		EditText wInput = new EditText(this);
		wInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
		wInput.setText(String.valueOf(tilemapData.getTileWidth()));
		EditText hInput = new EditText(this);
		hInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
		hInput.setText(String.valueOf(tilemapData.getTileHeight()));

		android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
		layout.setOrientation(android.widget.LinearLayout.VERTICAL);
		layout.setPadding(32, 16, 32, 16);
		TextView wLabel = new TextView(this);
		wLabel.setText(R.string.tilemap_tile_width);
		layout.addView(wLabel);
		layout.addView(wInput);
		TextView hLabel = new TextView(this);
		hLabel.setText(R.string.tilemap_tile_height);
		layout.addView(hLabel);
		layout.addView(hInput);

		new AlertDialog.Builder(this)
				.setTitle(R.string.tilemap_tile_size)
				.setView(layout)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					try {
						int w = Integer.parseInt(wInput.getText().toString().trim());
						int h = Integer.parseInt(hInput.getText().toString().trim());
						if (w > 0 && h > 0) {
							tilemapData.setTileWidth(w);
							tilemapData.setTileHeight(h);
							editorView.setData(tilemapData);
							hasUnsavedChanges = true;
						}
					} catch (NumberFormatException ignored) {
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void showTilesetPicker() {
		Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
		if (sprite == null) {
			return;
		}
		java.util.ArrayList<String> names = new java.util.ArrayList<>();
		java.util.ArrayList<LookData> looks = new java.util.ArrayList<>();
		for (LookData look : sprite.getLookList()) {
			if (look != null && !(look instanceof TilemapLookData) && look.getFile() != null) {
				names.add(look.getName());
				looks.add(look);
			}
		}
		names.add(getString(R.string.tilemap_import_image));

		new AlertDialog.Builder(this)
				.setTitle(R.string.tilemap_select_tileset)
				.setItems(names.toArray(new String[0]), (dialog, which) -> {
					if (which == looks.size()) {
						pickImageFromGallery();
					} else {
						applyTilesetFromLook(looks.get(which));
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
	}

	private void pickImageFromGallery() {
		Intent intent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, REQUEST_PICK_TILESET);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_PICK_TILESET && resultCode == RESULT_OK && data != null) {
			Uri uri = data.getData();
			if (uri != null) {
				applyTilesetFromUri(uri);
			}
		}
	}

	private void applyTilesetFromUri(Uri uri) {
		try {
			Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
			if (sprite == null) {
				return;
			}
			File imagesDir = new File(ProjectManager.getInstance().getCurrentProject().getDirectory(),
					"images");
			if (!imagesDir.exists()) {
				imagesDir.mkdirs();
			}
			String newName = "tileset_" + UUID.randomUUID().toString() + ".png";
			File dest;
			try (InputStream in = getContentResolver().openInputStream(uri)) {
				if (in == null) {
					return;
				}
				dest = StorageOperations.copyStreamToDir(in, imagesDir, newName);
			}

			LookData newLook = new LookData("tileset", dest);
			applyTilesetFromLook(newLook);
		} catch (Exception e) {
			hintView.setText(getString(R.string.tilemap_error_tileset) + ": " + e.getMessage());
		}
	}

	private void applyTilesetFromLook(LookData look) {
		if (look == null || look.getFile() == null) {
			return;
		}
		tilemapData.setName(look.getName());
		try {
			File copy = StorageOperations.duplicateFile(look.getFile());
			tilemapData = replaceTilemapFile(tilemapData, copy);
			editorView.setData(tilemapData);
			editorView.reloadTileset();
			hasUnsavedChanges = true;
		} catch (Exception e) {
			hintView.setText(getString(R.string.tilemap_error_tileset) + ": " + e.getMessage());
		}
	}

	private TilemapLookData replaceTilemapFile(TilemapLookData data, File newFile) {
		data.setFile(newFile);
		try {
			java.lang.reflect.Field pixmapField = LookData.class.getDeclaredField("pixmap");
			pixmapField.setAccessible(true);
			Object oldPixmap = pixmapField.get(data);
			if (oldPixmap instanceof com.badlogic.gdx.graphics.Pixmap) {
				((com.badlogic.gdx.graphics.Pixmap) oldPixmap).dispose();
			}
			pixmapField.set(data, null);
		} catch (Exception ignored) {
		}
		return data;
	}

	private void saveAndExit() {
		btnSave.setEnabled(false);
		hintView.setText(R.string.tilemap_saving);
		new Thread(() -> {
			Exception saveError = null;
			try {
				XstreamSerializer.getInstance().saveProject(
						ProjectManager.getInstance().getCurrentProject());
			} catch (Exception e) {
				saveError = e;
			}
			final Exception finalError = saveError;
			runOnUiThread(() -> {
				if (finalError != null) {
					String msg = finalError.getMessage();
					if (msg == null) {
						msg = finalError.getClass().getSimpleName();
					}
					hintView.setText(getString(R.string.tilemap_save_error) + ": " + msg);
					btnSave.setEnabled(true);
				} else {
					setResult(RESULT_OK);
					finish();
				}
			});
		}, "tilemap-save").start();
	}

	private void cancelNewTilemap() {
		if (isNewTilemap && tilemapData != null) {
			Sprite sprite = ProjectManager.getInstance().getCurrentSprite();
			if (sprite != null) {
				sprite.getLookList().remove(tilemapData);
			}
		}
	}

	private void confirmDiscardAndFinish() {
		if (hasUnsavedChanges) {
			new AlertDialog.Builder(this)
					.setTitle(R.string.tilemap_unsaved_title)
					.setMessage(R.string.tilemap_unsaved_message)
					.setPositiveButton(R.string.tilemap_save, (d, w) -> saveAndExit())
					.setNegativeButton(R.string.tilemap_discard, (d, w) -> {
						cancelNewTilemap();
						setResult(RESULT_CANCELED);
						finish();
					})
					.setNeutralButton(android.R.string.cancel, null)
					.show();
		} else {
			cancelNewTilemap();
			setResult(RESULT_CANCELED);
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		confirmDiscardAndFinish();
	}
}
