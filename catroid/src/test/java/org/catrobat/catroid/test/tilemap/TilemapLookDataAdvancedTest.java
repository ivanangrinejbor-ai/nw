/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.tilemap;

import org.catrobat.catroid.common.TilemapLookData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TilemapLookDataAdvancedTest {

    // ======================= setMapSize resize =======================

    @Test
    public void testResizePreservesOverlappingTiles() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(5, 5);
        data.initLayers(1);
        data.setTile(0, 0, (short) 1);
        data.setTile(4, 4, (short) 2);

        // Shrink to 3x3 — (0,0) preserved, (4,4) lost
        data.setMapSize(3, 3);
        assertEquals(1, data.getTile(0, 0));
        assertEquals(3, data.getMapColumns());
        assertEquals(3, data.getMapRows());
    }

    @Test
    public void testResizeGrowFillsWithEmpty() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(2, 2);
        data.initLayers(1);
        data.setTile(0, 0, (short) 5);

        data.setMapSize(4, 4);
        assertEquals(5, data.getTile(0, 0));
        assertEquals(TilemapLookData.EMPTY, data.getTile(3, 3));
        assertEquals(4, data.getMapColumns());
    }

    @Test
    public void testResizeToZero() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(3, 3);
        data.initLayers(1);
        data.setTile(0, 0, (short) 1);

        data.setMapSize(0, 0);
        assertEquals(0, data.getMapColumns());
        assertEquals(0, data.getMapRows());
        assertEquals(TilemapLookData.EMPTY, data.getTile(0, 0));
    }

    // ======================= Tile properties =======================

    @Test
    public void testSetTileReturnsTrueOnChange() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(3, 3);
        data.initLayers(1);
        assertTrue(data.setTile(0, 0, (short) 1));
    }

    @Test
    public void testSetTileReturnsFalseWhenSameValue() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(3, 3);
        data.initLayers(1);
        data.setTile(0, 0, (short) 1);
        assertFalse(data.setTile(0, 0, (short) 1)); // same value
    }

    @Test
    public void testSetTileReturnsFalseOutOfBounds() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(3, 3);
        data.initLayers(1);
        assertFalse(data.setTile(10, 10, (short) 1));
    }

    // ======================= Solid tiles =======================

    @Test
    public void testMultipleSolidTiles() {
        TilemapLookData data = new TilemapLookData();
        data.setTileSolid(1, true);
        data.setTileSolid(3, true);
        data.setTileSolid(7, true);

        assertTrue(data.isSolidTile(1));
        assertFalse(data.isSolidTile(2));
        assertTrue(data.isSolidTile(3));
        assertTrue(data.isSolidTile(7));
        assertEquals(3, data.getSolidTiles().size());
    }

    @Test
    public void testRemoveSolidTile() {
        TilemapLookData data = new TilemapLookData();
        data.setTileSolid(5, true);
        assertTrue(data.isSolidTile(5));

        data.setTileSolid(5, false);
        assertFalse(data.isSolidTile(5));
        assertEquals(0, data.getSolidTiles().size());
    }

    // ======================= Tileset geometry =======================

    @Test
    public void testTilesetColumnsFromImageWidth() {
        TilemapLookData data = new TilemapLookData();
        data.setTileWidth(32);
        data.setTileHeight(32);
        data.setMargin(0);
        data.setSpacing(0);
        // 256px wide / 32px = 8 columns
        assertEquals(8, data.getTilesetColumns(256));
    }

    @Test
    public void testTilesetColumnsWithSpacing() {
        TilemapLookData data = new TilemapLookData();
        data.setTileWidth(32);
        data.setSpacing(2);
        data.setMargin(0);
        // (256 + 2) / (32 + 2) = 258 / 34 = 7
        assertEquals(7, data.getTilesetColumns(256));
    }

    @Test
    public void testTilesetColumnsWithMargin() {
        TilemapLookData data = new TilemapLookData();
        data.setTileWidth(32);
        data.setSpacing(0);
        data.setMargin(4);
        // (256 - 2*4) / 32 = 248 / 32 = 7
        assertEquals(7, data.getTilesetColumns(256));
    }

    @Test
    public void testTilesetRowsFromImageHeight() {
        TilemapLookData data = new TilemapLookData();
        data.setTileHeight(16);
        data.setMargin(0);
        data.setSpacing(0);
        // 128px / 16px = 8 rows
        assertEquals(8, data.getTilesetRows(128));
    }

    // ======================= Layer access =======================

    @Test
    public void testGetLayerOutOfBoundsReturnsNull() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(3, 3);
        data.initLayers(1);
        assertNull(data.getLayer(5));
        assertNull(data.getLayer(-1));
    }

    @Test
    public void testGetLayerZeroReturnsArray() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(3, 3);
        data.initLayers(1);
        assertNotNull(data.getLayer(0));
        assertEquals(9, data.getLayer(0).length); // 3*3
    }

    // ======================= Tile properties map =======================

    @Test
    public void testTilePropertiesInitiallyEmpty() {
        TilemapLookData data = new TilemapLookData();
        assertNotNull(data.getTileProperties());
        assertTrue(data.getTileProperties().isEmpty());
    }

    // ======================= EMPTY constant =======================

    @Test
    public void testEmptyConstantIsMinusOne() {
        assertEquals(-1, TilemapLookData.EMPTY);
    }

    // ======================= Tile width/height setters =======================

    @Test
    public void testTileWidthMinimumIsOne() {
        TilemapLookData data = new TilemapLookData();
        data.setTileWidth(0);
        assertEquals(1, data.getTileWidth());
        data.setTileWidth(-5);
        assertEquals(1, data.getTileWidth());
    }

    @Test
    public void testTileHeightMinimumIsOne() {
        TilemapLookData data = new TilemapLookData();
        data.setTileHeight(0);
        assertEquals(1, data.getTileHeight());
    }

    @Test
    public void testMarginCannotBeNegative() {
        TilemapLookData data = new TilemapLookData();
        data.setMargin(-3);
        assertEquals(0, data.getMargin());
    }

    @Test
    public void testSpacingCannotBeNegative() {
        TilemapLookData data = new TilemapLookData();
        data.setSpacing(-3);
        assertEquals(0, data.getSpacing());
    }
}
