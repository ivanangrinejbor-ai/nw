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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Undo/redo stack for the tilemap editor. Each entry is a batch of cell changes (one stroke =
 * one batch), so undoing reverts an entire drag in a single step.
 *
 * <p>Only the active layer (layer 0 in v2.3) is tracked. Solid-tile toggles are stored as a
 * separate batch type so they can be undone independently.</p>
 */
public final class TilemapEditHistory {

	/** A single cell change: layer/column/row went from {@code oldTile} to {@code newTile}. */
	public static final class CellChange {
		public final int layerIndex;
		public final int column;
		public final int row;
		public final short oldTile;
		public final short newTile;

		public CellChange(int layerIndex, int column, int row, short oldTile, short newTile) {
			this.layerIndex = layerIndex;
			this.column = column;
			this.row = row;
			this.oldTile = oldTile;
			this.newTile = newTile;
		}
	}

	/** A single solid-tile toggle: tileIndex went from {@code oldSolid} to {@code newSolid}. */
	public static final class SolidChange {
		public final int tileIndex;
		public final boolean oldSolid;
		public final boolean newSolid;

		public SolidChange(int tileIndex, boolean oldSolid, boolean newSolid) {
			this.tileIndex = tileIndex;
			this.oldSolid = oldSolid;
			this.newSolid = newSolid;
		}
	}

	private static final int MAX_DEPTH = 100;

	private final Deque<List<Object>> undoStack = new ArrayDeque<>();
	private final Deque<List<Object>> redoStack = new ArrayDeque<>();

	private List<Object> currentBatch;

	/** Begin collecting changes for a new batch (e.g. at the start of a drag stroke). */
	public void beginBatch() {
		currentBatch = new ArrayList<>();
	}

	/** Record a cell change into the current batch. No-op if no batch is open. */
	public void recordCellChange(int layerIndex, int column, int row, short oldTile, short newTile) {
		if (currentBatch == null || oldTile == newTile) {
			return;
		}
		currentBatch.add(new CellChange(layerIndex, column, row, oldTile, newTile));
	}

	/** Record a solid-tile toggle into the current batch. */
	public void recordSolidChange(int tileIndex, boolean oldSolid, boolean newSolid) {
		if (currentBatch == null || oldSolid == newSolid) {
			return;
		}
		currentBatch.add(new SolidChange(tileIndex, oldSolid, newSolid));
	}

	/**
	 * Commit the current batch to the undo stack. Empty batches are discarded; any pending redo
	 * history is cleared (a new action invalidates the redo timeline).
	 */
	public void commitBatch() {
		if (currentBatch == null || currentBatch.isEmpty()) {
			currentBatch = null;
			return;
		}
		undoStack.push(currentBatch);
		if (undoStack.size() > MAX_DEPTH) {
			// Drop the oldest entry (bottom of the deque).
			((ArrayDeque<List<Object>>) undoStack).removeLast();
		}
		redoStack.clear();
		currentBatch = null;
	}

	public boolean canUndo() {
		return !undoStack.isEmpty();
	}

	public boolean canRedo() {
		return !redoStack.isEmpty();
	}

	/** Pop the most recent batch from the undo stack and push it onto the redo stack. */
	public List<Object> undo() {
		if (undoStack.isEmpty()) {
			return null;
		}
		List<Object> batch = undoStack.pop();
		redoStack.push(batch);
		return batch;
	}

	/** Pop the most recent batch from the redo stack and push it back onto the undo stack. */
	public List<Object> redo() {
		if (redoStack.isEmpty()) {
			return null;
		}
		List<Object> batch = redoStack.pop();
		undoStack.push(batch);
		return batch;
	}

	/** Drop all history. */
	public void clear() {
		undoStack.clear();
		redoStack.clear();
		currentBatch = null;
	}
}
