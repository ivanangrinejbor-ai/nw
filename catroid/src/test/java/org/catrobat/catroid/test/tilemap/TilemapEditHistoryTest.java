/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.tilemap;

import org.catrobat.catroid.ui.tilemap.TilemapEditHistory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TilemapEditHistoryTest {

    private TilemapEditHistory history;

    @Before
    public void setUp() {
        history = new TilemapEditHistory();
    }

    @Test
    public void testEmptyHistory() {
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
        assertNull(history.undo());
        assertNull(history.redo());
    }

    @Test
    public void testSingleBatchUndoRedo() {
        history.beginBatch();
        history.recordCellChange(0, 1, 2, (short) -1, (short) 3);
        history.recordCellChange(0, 1, 3, (short) -1, (short) 3);
        history.commitBatch();

        assertTrue(history.canUndo());
        assertFalse(history.canRedo());

        List<Object> batch = history.undo();
        assertNotNull(batch);
        assertEquals(2, batch.size());
        assertTrue(batch.get(0) instanceof TilemapEditHistory.CellChange);

        assertFalse(history.canUndo());
        assertTrue(history.canRedo());

        List<Object> redone = history.redo();
        assertNotNull(redone);
        assertEquals(2, redone.size());
        assertTrue(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    public void testEmptyBatchNotCommitted() {
        history.beginBatch();
        history.commitBatch();
        assertFalse(history.canUndo());
    }

    @Test
    public void testNoOpChangesNotRecorded() {
        history.beginBatch();
        history.recordCellChange(0, 1, 2, (short) 3, (short) 3); // same tile
        history.recordSolidChange(1, true, true); // same solid state
        history.commitBatch();
        assertFalse(history.canUndo());
    }

    @Test
    public void testNewBatchClearsRedo() {
        history.beginBatch();
        history.recordCellChange(0, 0, 0, (short) -1, (short) 1);
        history.commitBatch();

        history.undo();
        assertTrue(history.canRedo());

        history.beginBatch();
        history.recordCellChange(0, 1, 1, (short) -1, (short) 2);
        history.commitBatch();
        assertFalse(history.canRedo());
    }

    @Test
    public void testSolidChangeRecorded() {
        history.beginBatch();
        history.recordSolidChange(5, false, true);
        history.commitBatch();

        assertTrue(history.canUndo());
        List<Object> batch = history.undo();
        assertNotNull(batch);
        assertEquals(1, batch.size());
        assertTrue(batch.get(0) instanceof TilemapEditHistory.SolidChange);
        TilemapEditHistory.SolidChange change = (TilemapEditHistory.SolidChange) batch.get(0);
        assertEquals(5, change.tileIndex);
        assertFalse(change.oldSolid);
        assertTrue(change.newSolid);
    }

    @Test
    public void testClear() {
        history.beginBatch();
        history.recordCellChange(0, 0, 0, (short) -1, (short) 1);
        history.commitBatch();
        history.undo();

        history.clear();
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
    }
}
