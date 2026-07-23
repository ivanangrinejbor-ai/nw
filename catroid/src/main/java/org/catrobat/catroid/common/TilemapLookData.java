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

import com.badlogic.gdx.graphics.Pixmap;

import org.catrobat.catroid.io.StorageOperations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;

/**
 * A special sprite costume ({@link LookData}) that stores a tile map instead of a single image.
 *
 * <p><b>Model only.</b> This class holds pure data: tile size, map size, tile indices per layer
 * and which tile indices are solid (for collisions). Rendering, texture slicing and Box2D bodies
 * live in the runtime ({@code content.tilemap.TilemapRuntime}) — never here — so a future desktop
 * runtime or custom physics can reuse this same model.</p>
 *
 * <p>The tileset spritesheet is the costume's own image file (see {@link #getFile()} /
 * {@link #getPixmap()}); tiles are the cells of that sheet cut into a grid by
 * {@link #tileWidth}/{@link #tileHeight} (plus optional {@link #margin}/{@link #spacing}).
 * The number of tileset columns/rows is NOT stored — it is derived lazily from the image size so
 * replacing the image can never desync it.</p>
 */
public class TilemapLookData extends LookData {

	private static final long serialVersionUID = 1L;

	/** Empty cell marker. */
	public static final short EMPTY = -1;

	private int tileWidth = 16;
	private int tileHeight = 16;

	/** Reserved for future Tiled import; always 0 in v2.3. */
	private int margin = 0;
	private int spacing = 0;

	private int mapColumns = 0;
	private int mapRows = 0;

	/**
	 * Layers, bottom-most first. Each layer is a flat {@code short[]} of length
	 * {@code mapColumns * mapRows}, row-major, with {@link #EMPTY} for empty cells.
	 * A {@link List} (not {@code short[][]}) so layers can be added/removed later without
	 * recreating the outer array. v2.3 UI edits only {@code layers.get(0)}.
	 */
	private List<short[]> layers = new ArrayList<>();

	/** Tileset indices that are solid (generate collision bodies). */
	private Set<Integer> solidTiles = new HashSet<>();

	/**
	 * Forward-compat hook (empty in v2.3): tileIndex → property bitmask/value.
	 * Reserved for future autotiles and tile animation so the format need not change.
	 */
	private Map<Integer, Integer> tileProperties = new HashMap<>();

	public TilemapLookData() {
		super();
	}

	public TilemapLookData(String name) {
		super(name);
	}

	public TilemapLookData(String name, @NonNull File file) {
		super(name, file);
	}

	/** Allocates (or reallocates) layer storage for the current map size, keeping at least one layer. */
	public void initLayers(int layerCount) {
		layers = new ArrayList<>();
		int count = Math.max(1, layerCount);
		for (int i = 0; i < count; i++) {
			layers.add(newEmptyLayer());
		}
	}

	private short[] newEmptyLayer() {
		short[] layer = new short[Math.max(0, mapColumns * mapRows)];
		java.util.Arrays.fill(layer, EMPTY);
		return layer;
	}

	private void ensureAtLeastOneLayer() {
		if (layers == null) {
			layers = new ArrayList<>();
		}
		if (layers.isEmpty()) {
			layers.add(newEmptyLayer());
		}
	}

	public int getLayerCount() {
		ensureAtLeastOneLayer();
		return layers.size();
	}

	public List<short[]> getLayers() {
		ensureAtLeastOneLayer();
		return layers;
	}

	public short[] getLayer(int layerIndex) {
		ensureAtLeastOneLayer();
		if (layerIndex < 0 || layerIndex >= layers.size()) {
			return null;
		}
		return layers.get(layerIndex);
	}

	private boolean inBounds(int column, int row) {
		return column >= 0 && column < mapColumns && row >= 0 && row < mapRows;
	}

	private int cellIndex(int column, int row) {
		return row * mapColumns + column;
	}

	/** @return tile index at layer/column/row, or {@link #EMPTY} if out of range. */
	public short getTile(int layerIndex, int column, int row) {
		short[] layer = getLayer(layerIndex);
		if (layer == null || !inBounds(column, row)) {
			return EMPTY;
		}
		return layer[cellIndex(column, row)];
	}

	/** @return tile index on layer 0. */
	public short getTile(int column, int row) {
		return getTile(0, column, row);
	}

	/**
	 * Sets a tile on the given layer.
	 * @return true if the cell actually changed (useful for undo diffing).
	 */
	public boolean setTile(int layerIndex, int column, int row, short tileIndex) {
		short[] layer = getLayer(layerIndex);
		if (layer == null || !inBounds(column, row)) {
			return false;
		}
		int idx = cellIndex(column, row);
		if (layer[idx] == tileIndex) {
			return false;
		}
		layer[idx] = tileIndex;
		return true;
	}

	public boolean setTile(int column, int row, short tileIndex) {
		return setTile(0, column, row, tileIndex);
	}

	public synchronized boolean isSolidTile(int tileIndex) {
		return solidTiles.contains(tileIndex);
	}

	public synchronized void setTileSolid(int tileIndex, boolean solid) {
		if (solid) {
			solidTiles.add(tileIndex);
		} else {
			solidTiles.remove(tileIndex);
		}
	}

	public synchronized Set<Integer> getSolidTiles() {
		return new HashSet<>(solidTiles);
	}

	public Map<Integer, Integer> getTileProperties() {
		return tileProperties;
	}

	public int getTileWidth() {
		return tileWidth;
	}

	public void setTileWidth(int tileWidth) {
		this.tileWidth = Math.max(1, tileWidth);
	}

	public int getTileHeight() {
		return tileHeight;
	}

	public void setTileHeight(int tileHeight) {
		this.tileHeight = Math.max(1, tileHeight);
	}

	public int getMargin() {
		return margin;
	}

	public void setMargin(int margin) {
		this.margin = Math.max(0, margin);
	}

	public int getSpacing() {
		return spacing;
	}

	public void setSpacing(int spacing) {
		this.spacing = Math.max(0, spacing);
	}

	public int getMapColumns() {
		return mapColumns;
	}

	public int getMapRows() {
		return mapRows;
	}

	/** Resizes the map, preserving overlapping tiles. Reallocates every layer. */
	public void setMapSize(int columns, int rows) {
		int newColumns = Math.max(0, columns);
		int newRows = Math.max(0, rows);
		ensureAtLeastOneLayer();
		List<short[]> resized = new ArrayList<>();
		for (short[] oldLayer : layers) {
			short[] newLayer = new short[newColumns * newRows];
			java.util.Arrays.fill(newLayer, EMPTY);
			for (int r = 0; r < Math.min(rows, mapRows); r++) {
				for (int c = 0; c < Math.min(columns, mapColumns); c++) {
					newLayer[r * newColumns + c] = oldLayer[r * mapColumns + c];
				}
			}
			resized.add(newLayer);
		}
		layers = resized;
		mapColumns = newColumns;
		mapRows = newRows;
	}

	public int getMapPixelWidth() {
		return mapColumns * tileWidth;
	}

	public int getMapPixelHeight() {
		return mapRows * tileHeight;
	}

	/** Tileset columns, derived from image width (never stored, so it can't desync). */
	public int getTilesetColumns(int imageWidth) {
		int denom = tileWidth + spacing;
		if (denom <= 0) {
			return 0;
		}
		return Math.max(0, (imageWidth - 2 * margin + spacing) / denom);
	}

	public int getTilesetRows(int imageHeight) {
		int denom = tileHeight + spacing;
		if (denom <= 0) {
			return 0;
		}
		return Math.max(0, (imageHeight - 2 * margin + spacing) / denom);
	}

	public int getTilesetColumns() {
		Pixmap pixmap = getPixmap();
		return pixmap != null ? getTilesetColumns(pixmap.getWidth()) : 0;
	}

	public int getTilesetRows() {
		Pixmap pixmap = getPixmap();
		return pixmap != null ? getTilesetRows(pixmap.getHeight()) : 0;
	}

	/** Backward-compat: some legacy/streamed maps may lack layer allocation; normalise on load. */
	private Object readResolve() {
		if (solidTiles == null) {
			solidTiles = new HashSet<>();
		}
		if (tileProperties == null) {
			tileProperties = new HashMap<>();
		}
		ensureAtLeastOneLayer();
		return this;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public TilemapLookData clone() {
		TilemapLookData copy;
		try {
			File duplicated = getFile() != null ? StorageOperations.duplicateFile(getFile()) : null;
			copy = duplicated != null ? new TilemapLookData(getName(), duplicated)
					: new TilemapLookData(getName());
		} catch (IOException e) {
			throw new RuntimeException("TilemapLookData: could not copy tileset file", e);
		}
		copy.setLookId(UUID.randomUUID().toString());
		copy.tileWidth = tileWidth;
		copy.tileHeight = tileHeight;
		copy.margin = margin;
		copy.spacing = spacing;
		copy.mapColumns = mapColumns;
		copy.mapRows = mapRows;
		copy.layers = new ArrayList<>();
		for (short[] layer : getLayers()) {
			copy.layers.add(layer.clone());
		}
		copy.solidTiles = new HashSet<>(solidTiles);
		copy.tileProperties = new HashMap<>(tileProperties);
		copy.setHitboxMode(getHitboxMode());
		return copy;
	}
}
