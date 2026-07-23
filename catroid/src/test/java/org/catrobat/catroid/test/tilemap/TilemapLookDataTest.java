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
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TilemapLookDataTest {

    @Test
    public void testDefaultConstruction() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(10, 10);
        data.setTileWidth(32);
        data.setTileHeight(32);
        assertEquals(10, data.getMapColumns());
        assertEquals(10, data.getMapRows());
        assertEquals(32, data.getTileWidth());
    }

    @Test
    public void testSetAndGetTile() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(5, 5);
        data.setTileWidth(16);
        data.setTileHeight(16);
        data.initLayers(1);
        // Default tile should be EMPTY (-1)
        assertEquals(TilemapLookData.EMPTY, data.getTile(0, 0));
        // Set tile
        data.setTile(0, 0, (short) 3);
        assertEquals(3, data.getTile(0, 0));
        // Out of bounds should return EMPTY
        assertEquals(TilemapLookData.EMPTY, data.getTile(-1, 0));
        assertEquals(TilemapLookData.EMPTY, data.getTile(100, 0));
    }

    @Test
    public void testSolidTiles() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(5, 5);
        data.initLayers(1);
        assertFalse(data.getSolidTiles().contains(1));
        data.setTileSolid(1, true);
        assertTrue(data.getSolidTiles().contains(1));
        data.setTileSolid(1, false);
        assertFalse(data.getSolidTiles().contains(1));
    }

    @Test
    public void testMapPixelDimensions() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(8, 6);
        data.setTileWidth(32);
        data.setTileHeight(32);
        assertEquals(8 * 32, data.getMapPixelWidth());
        assertEquals(6 * 32, data.getMapPixelHeight());
    }

    @Test
    public void testMultipleLayers() {
        TilemapLookData data = new TilemapLookData();
        data.setMapSize(4, 4);
        data.setTileWidth(16);
        data.setTileHeight(16);
        data.initLayers(1);
        assertEquals(1, data.getLayerCount());
        data.initLayers(2);
        assertEquals(2, data.getLayerCount());
        // Set tile on layer 1
        data.setTile(1, 0, 0, (short) 5);
        assertEquals(5, data.getTile(1, 0, 0));
        // Layer 0 should still be EMPTY
        assertEquals(TilemapLookData.EMPTY, data.getTile(0, 0, 0));
        // getTile(col, row) reads from layer 0
        assertEquals(TilemapLookData.EMPTY, data.getTile(0, 0));
    }
}
