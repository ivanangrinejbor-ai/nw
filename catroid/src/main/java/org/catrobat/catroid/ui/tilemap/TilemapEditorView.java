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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.catrobat.catroid.common.TilemapLookData;

/**
 * Custom view that renders the tilemap grid and handles single-finger draw/erase plus
 * two-finger pan/zoom. A sibling {@link PaletteView} shows the tileset palette.
 *
 * <p>One drag stroke = one undo batch (see {@link TilemapEditHistory}).</p>
 */
public class TilemapEditorView extends View {

	/** Listener notified on every edit, so the activity can update the hint and button states. */
	public interface OnChangeListener {
		void onChanged();
	}

	/** Listener for palette tile taps. */
	public interface OnPaletteTileListener {
		void onPaletteTilePicked(int tileIndex);
	}

	private TilemapLookData data;
	private final TilemapEditHistory history;

	private Bitmap tilesetBitmap;
	private int tileW = 16;
	private int tileH = 16;
	private int tilesetColumns = 0;
	private int tilesetRows = 0;

	private int currentTile = 0;
	private boolean eraserMode = false;

	private float panOffsetX = 0f;
	private float panOffsetY = 0f;
	private float zoom = 1f;
	private static final float MIN_ZOOM = 0.25f;
	private static final float MAX_ZOOM = 8f;

	private boolean isDrawing = false;
	private int lastDrawCol = -1;
	private int lastDrawRow = -1;
	private boolean strokeHadEffect = false;

	private float pinchStartDistance = 0f;
	private float pinchStartZoom = 1f;
	private float panStartX = 0f;
	private float panStartY = 0f;
	private float panStartOffsetX = 0f;
	private float panStartOffsetY = 0f;

	private final Paint gridPaint = new Paint();
	private final Paint selectionPaint = new Paint();
	private final Paint emptyPaint = new Paint();

	private OnChangeListener changeListener;
	private OnPaletteTileListener paletteListener;

	public TilemapEditorView(Context context) {
		this(context, null);
	}

	public TilemapEditorView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TilemapEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		history = new TilemapEditHistory();

		gridPaint.setColor(Color.GRAY);
		gridPaint.setStrokeWidth(1f);
		gridPaint.setStyle(Paint.Style.STROKE);

		selectionPaint.setColor(Color.argb(128, 255, 255, 0));
		selectionPaint.setStyle(Paint.Style.FILL);

		emptyPaint.setColor(Color.argb(40, 128, 128, 128));
		emptyPaint.setStyle(Paint.Style.FILL);
	}

	public void setData(TilemapLookData data) {
		this.data = data;
		if (data != null) {
			tileW = data.getTileWidth();
			tileH = data.getTileHeight();
			loadTilesetBitmap();
		}
		invalidate();
	}

	public TilemapLookData getData() {
		return data;
	}

	public TilemapEditHistory getHistory() {
		return history;
	}

	public void setOnChangeListener(OnChangeListener listener) {
		this.changeListener = listener;
	}

	public void setOnPaletteTileListener(OnPaletteTileListener listener) {
		this.paletteListener = listener;
	}

	public int getCurrentTile() {
		return currentTile;
	}

	public void setCurrentTile(int tileIndex) {
		this.currentTile = Math.max(-1, tileIndex);
		this.eraserMode = (currentTile == -1);
		invalidate();
	}

	public boolean isEraserMode() {
		return eraserMode;
	}

	public void setEraserMode(boolean eraser) {
		this.eraserMode = eraser;
		if (eraser) {
			this.currentTile = -1;
		}
		invalidate();
	}

	private void loadTilesetBitmap() {
		if (data == null || data.getFile() == null || !data.getFile().exists()) {
			tilesetBitmap = null;
			tilesetColumns = 0;
			tilesetRows = 0;
			return;
		}
		Bitmap raw = BitmapFactory.decodeFile(data.getFile().getAbsolutePath());
		if (raw == null) {
			tilesetBitmap = null;
			tilesetColumns = 0;
			tilesetRows = 0;
			return;
		}
		// Scale up 3× so small pixel-art tilesets are visible on modern screens.
		int scaledW = raw.getWidth() * 3;
		int scaledH = raw.getHeight() * 3;
		tilesetBitmap = Bitmap.createScaledBitmap(raw, scaledW, scaledH, true);
		if (tilesetBitmap != raw) {
			raw.recycle();
		}
		tilesetColumns = data.getTilesetColumns();
		tilesetRows = data.getTilesetRows();
	}

	/** Called by the activity after the user picks a new tileset image. */
	public void reloadTileset() {
		loadTilesetBitmap();
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.rgb(30, 30, 46));
		if (data == null) {
			return;
		}
		int columns = data.getMapColumns();
		int rows = data.getMapRows();
		if (columns <= 0 || rows <= 0) {
			return;
		}

		float scaledTileW = tileW * zoom;
		float scaledTileH = tileH * zoom;

		short[] layer = data.getLayer(0);

		// Empty-cell background.
		canvas.drawRect(panOffsetX, panOffsetY,
				panOffsetX + columns * scaledTileW,
				panOffsetY + rows * scaledTileH, emptyPaint);

		// Tile content.
		if (layer != null) {
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < columns; col++) {
					int idx = row * columns + col;
					if (idx >= layer.length) {
						continue;
					}
					short tile = layer[idx];
					if (tile == TilemapLookData.EMPTY || tile < 0) {
						continue;
					}
					if (tilesetBitmap != null && tilesetColumns > 0 && tilesetRows > 0
							&& tile < tilesetColumns * tilesetRows) {
						int srcCol = tile % tilesetColumns;
						int srcRow = tile / tilesetColumns;
						Rect src = new Rect(
								srcCol * tileW, srcRow * tileH,
								(srcCol + 1) * tileW, (srcRow + 1) * tileH);
						Rect dst = new Rect(
								(int) (panOffsetX + col * scaledTileW),
								(int) (panOffsetY + row * scaledTileH),
								(int) (panOffsetX + (col + 1) * scaledTileW),
								(int) (panOffsetY + (row + 1) * scaledTileH));
						canvas.drawBitmap(tilesetBitmap, src, dst, null);
					} else {
						// Fallback: solid colour block when no tileset is loaded.
						Paint p = new Paint();
						p.setColor(Color.argb(200, 80 + (tile * 37) % 175,
								80 + (tile * 53) % 175, 80 + (tile * 71) % 175));
						p.setStyle(Paint.Style.FILL);
						canvas.drawRect(
								panOffsetX + col * scaledTileW,
								panOffsetY + row * scaledTileH,
								panOffsetX + (col + 1) * scaledTileW,
								panOffsetY + (row + 1) * scaledTileH, p);
					}
				}
			}
		}

		// Grid lines.
		Paint gp = new Paint(gridPaint);
		gp.setColor(Color.argb(80, 200, 200, 200));
		for (int c = 0; c <= columns; c++) {
			float x = panOffsetX + c * scaledTileW;
			canvas.drawLine(x, panOffsetY, x, panOffsetY + rows * scaledTileH, gp);
		}
		for (int r = 0; r <= rows; r++) {
			float y = panOffsetY + r * scaledTileH;
			canvas.drawLine(panOffsetX, y, panOffsetX + columns * scaledTileW, y, gp);
		}

		// Highlight the currently selected palette tile on the grid.
		if (!eraserMode && currentTile >= 0 && layer != null) {
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < columns; col++) {
					int idx = row * columns + col;
					if (idx < layer.length && layer[idx] == currentTile) {
						canvas.drawRect(
								panOffsetX + col * scaledTileW,
								panOffsetY + row * scaledTileH,
								panOffsetX + (col + 1) * scaledTileW,
								panOffsetY + (row + 1) * scaledTileH, selectionPaint);
					}
				}
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int pointerCount = event.getPointerCount();
		int action = event.getActionMasked();

		switch (action) {
			case MotionEvent.ACTION_DOWN:
				if (pointerCount == 1) {
					isDrawing = true;
					strokeHadEffect = false;
					history.beginBatch();
					int[] cell = screenToCell(event.getX(), event.getY());
					applyTool(cell[0], cell[1]);
				} else {
					// Switching to pinch: cancel the in-progress stroke.
					if (isDrawing) {
						isDrawing = false;
						// Drop the half-built batch so undo doesn't see a partial stroke.
						history.beginBatch();
					}
					pinchStartDistance = fingerDistance(event);
					pinchStartZoom = zoom;
					panStartX = event.getX();
					panStartY = event.getY();
					panStartOffsetX = panOffsetX;
					panStartOffsetY = panOffsetY;
				}
				return true;

			case MotionEvent.ACTION_MOVE:
				if (isDrawing && pointerCount == 1) {
					int[] cell = screenToCell(event.getX(), event.getY());
					if (cell[0] != lastDrawCol || cell[1] != lastDrawRow) {
						applyTool(cell[0], cell[1]);
					}
				} else if (!isDrawing && pointerCount >= 2) {
					float newDist = fingerDistance(event);
					if (pinchStartDistance > 0f) {
						float newZoom = pinchStartZoom * (newDist / pinchStartDistance);
						zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
					}
					panOffsetX = panStartOffsetX + (event.getX() - panStartX);
					panOffsetY = panStartOffsetY + (event.getY() - panStartY);
					invalidate();
				}
				return true;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (isDrawing) {
					history.commitBatch();
					isDrawing = false;
					lastDrawCol = -1;
					lastDrawRow = -1;
					if (strokeHadEffect && changeListener != null) {
						changeListener.onChanged();
					}
				}
				return true;

			default:
				return super.onTouchEvent(event);
		}
	}

	private void applyTool(int col, int row) {
		if (data == null || col < 0 || row < 0
				|| col >= data.getMapColumns() || row >= data.getMapRows()) {
			lastDrawCol = col;
			lastDrawRow = row;
			return;
		}
		if (eraserMode) {
			short oldTile = data.getTile(col, row);
			if (data.setTile(col, row, TilemapLookData.EMPTY)) {
				history.recordCellChange(0, col, row, oldTile, TilemapLookData.EMPTY);
				strokeHadEffect = true;
				invalidate();
			}
		} else if (currentTile >= 0) {
			short oldTile = data.getTile(col, row);
			short newTile = (short) currentTile;
			if (oldTile != newTile && data.setTile(col, row, newTile)) {
				history.recordCellChange(0, col, row, oldTile, newTile);
				strokeHadEffect = true;
				invalidate();
			}
		}
		lastDrawCol = col;
		lastDrawRow = row;
	}

	private int[] screenToCell(float screenX, float screenY) {
		float scaledTileW = tileW * zoom;
		float scaledTileH = tileH * zoom;
		int col = (int) Math.floor((screenX - panOffsetX) / scaledTileW);
		int row = (int) Math.floor((screenY - panOffsetY) / scaledTileH);
		return new int[]{col, row};
	}

	private float fingerDistance(MotionEvent event) {
		if (event.getPointerCount() < 2) {
			return 0f;
		}
		float dx = event.getX(0) - event.getX(1);
		float dy = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	/** Apply a batch of changes (from undo/redo) to the model. */
	public void applyBatch(java.util.List<Object> batch, boolean forward) {
		if (batch == null || data == null) {
			return;
		}
		for (Object change : batch) {
			if (change instanceof TilemapEditHistory.CellChange) {
				TilemapEditHistory.CellChange cc = (TilemapEditHistory.CellChange) change;
				data.setTile(cc.layerIndex, cc.column, cc.row, forward ? cc.newTile : cc.oldTile);
			} else if (change instanceof TilemapEditHistory.SolidChange) {
				TilemapEditHistory.SolidChange sc = (TilemapEditHistory.SolidChange) change;
				data.setTileSolid(sc.tileIndex, forward ? sc.newSolid : sc.oldSolid);
			}
		}
		invalidate();
		if (changeListener != null) {
			changeListener.onChanged();
		}
	}

	/**
	 * Horizontal palette of tileset cells. Tap to pick the active tile; the currently selected
	 * tile is drawn with a yellow highlight.
	 */
	public static class PaletteView extends View {

		private TilemapEditorView editor;

		public PaletteView(Context context) {
			this(context, null);
		}

		public PaletteView(Context context, AttributeSet attrs) {
			this(context, attrs, 0);
		}

		public PaletteView(Context context, AttributeSet attrs, int defStyleAttr) {
			super(context, attrs, defStyleAttr);
		}

		public void setEditor(TilemapEditorView editor) {
			this.editor = editor;
			invalidate();
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int height = MeasureSpec.getSize(heightMeasureSpec);
			int width;
			if (editor == null || editor.tilesetBitmap == null) {
				width = height;
			} else {
				float cell = height;
				int tiles = Math.min(editor.tilesetColumns * editor.tilesetRows, 128);
				width = (int) (tiles * cell);
			}
			setMeasuredDimension(Math.max(width, 1), height);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			if (editor == null || editor.tilesetBitmap == null
					|| editor.tilesetColumns <= 0 || editor.tilesetRows <= 0) {
				canvas.drawColor(Color.argb(60, 128, 128, 128));
				Paint textPaint = new Paint();
				textPaint.setColor(Color.WHITE);
				textPaint.setTextSize(28f);
				textPaint.setTextAlign(Paint.Align.CENTER);
				canvas.drawText("Tap 'Select tileset' to choose an image",
						getWidth() / 2f, getHeight() / 2f, textPaint);
				return;
			}
			float cellSize = getHeight();
			int totalTiles = Math.min(editor.tilesetColumns * editor.tilesetRows, 128);
			for (int i = 0; i < totalTiles; i++) {
				int srcCol = i % editor.tilesetColumns;
				int srcRow = i / editor.tilesetColumns;
				Rect src = new Rect(
						srcCol * editor.tileW, srcRow * editor.tileH,
						(srcCol + 1) * editor.tileW, (srcRow + 1) * editor.tileH);
				Rect dst = new Rect(
						(int) (i * cellSize), 0,
						(int) ((i + 1) * cellSize), (int) cellSize);
				canvas.drawBitmap(editor.tilesetBitmap, src, dst, null);

				if (i == editor.currentTile) {
					Paint sel = new Paint();
					sel.setColor(Color.YELLOW);
					sel.setStyle(Paint.Style.STROKE);
					sel.setStrokeWidth(4f);
					canvas.drawRect(dst, sel);
				}
			}
			// Subtle separator lines between cells.
			Paint linePaint = new Paint();
			linePaint.setColor(Color.argb(60, 255, 255, 255));
			for (int i = 1; i < totalTiles; i++) {
				float x = i * cellSize;
				canvas.drawLine(x, 0, x, cellSize, linePaint);
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (editor == null || editor.tilesetColumns <= 0) {
				return super.onTouchEvent(event);
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				float cellSize = getHeight();
				int index = (int) (event.getX() / cellSize);
				int total = Math.min(editor.tilesetColumns * editor.tilesetRows, 128);
				if (index >= 0 && index < total) {
					editor.setCurrentTile(index);
					if (editor.paletteListener != null) {
						editor.paletteListener.onPaletteTilePicked(index);
					}
					invalidate();
					editor.invalidate();
				}
				return true;
			}
			return super.onTouchEvent(event);
		}
	}
}
