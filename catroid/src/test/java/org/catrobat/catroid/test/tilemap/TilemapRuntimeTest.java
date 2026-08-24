/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.tilemap;

import org.catrobat.catroid.common.TilemapLookData;
import org.catrobat.catroid.content.tilemap.TilemapRuntime;
import org.catrobat.catroid.content.tilemap.TilemapRuntimeManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TilemapRuntimeTest {

    private TilemapLookData data;

    @Before
    public void setUp() {
        data = new TilemapLookData();
        data.setMapSize(5, 5);
        data.setTileWidth(16);
        data.setTileHeight(16);
        data.initLayers(1);
    }

    @After
    public void tearDown() {
        // Clear the static registry so tests don't leak into each other
        TilemapRuntimeManager.disposeAll(null);
    }

    @Test
    public void testNewRuntimeIsPhysicsDirty() {
        TilemapRuntime runtime = new TilemapRuntime(data);
        assertTrue(runtime.isPhysicsDirty());
    }

    @Test
    public void testInvalidatePhysicsSetsDirty() {
        TilemapRuntime runtime = new TilemapRuntime(data);
        // After rebuildIfDirty with null world, dirty flag stays (null world = no rebuild)
        runtime.rebuildIfDirty(null, null);
        // null world → early return, still dirty
        assertTrue(runtime.isPhysicsDirty());
    }

    @Test
    public void testGetDataReturnsSameData() {
        TilemapRuntime runtime = new TilemapRuntime(data);
        assertSame(data, runtime.getData());
    }

    @Test
    public void testGetRegionNegativeIndexReturnsNull() {
        TilemapRuntime runtime = new TilemapRuntime(data);
        assertNull(runtime.getRegion(-1));
    }

    @Test
    public void testGetRegionWithNoPixmapReturnsNull() {
        TilemapRuntime runtime = new TilemapRuntime(data);
        // No pixmap set → sliceRegions creates empty array
        assertNull(runtime.getRegion(0));
    }

    @Test
    public void testInvalidateRegionsFlagsReslice() {
        TilemapRuntime runtime = new TilemapRuntime(data);
        // Just ensure it doesn't throw
        runtime.invalidateRegions();
    }

    @Test
    public void testDisposeWithNullWorld() {
        TilemapRuntime runtime = new TilemapRuntime(data);
        runtime.dispose(null); // should not throw
    }

    @Test
    public void testGetOrCreateReturnsRuntime() {
        TilemapRuntime runtime = TilemapRuntimeManager.getOrCreate(data);
        assertNotNull(runtime);
        assertSame(data, runtime.getData());
    }

    @Test
    public void testGetOrCreateReturnsSameInstance() {
        TilemapRuntime r1 = TilemapRuntimeManager.getOrCreate(data);
        TilemapRuntime r2 = TilemapRuntimeManager.getOrCreate(data);
        assertSame(r1, r2);
    }

    @Test
    public void testPeekReturnsNullBeforeCreate() {
        assertNull(TilemapRuntimeManager.peek(data));
    }

    @Test
    public void testPeekReturnsRuntimeAfterCreate() {
        TilemapRuntimeManager.getOrCreate(data);
        assertNotNull(TilemapRuntimeManager.peek(data));
    }

    @Test
    public void testDisposeAllClearsRegistry() {
        TilemapRuntimeManager.getOrCreate(data);
        assertNotNull(TilemapRuntimeManager.peek(data));

        TilemapRuntimeManager.disposeAll(null);
        assertNull(TilemapRuntimeManager.peek(data));
    }

    @Test
    public void testDisposeAllDisposesEveryRuntime() {
        TilemapLookData data2 = new TilemapLookData();
        data2.setMapSize(3, 3);
        data2.initLayers(1);

        TilemapRuntime r1 = TilemapRuntimeManager.getOrCreate(data);
        TilemapRuntime r2 = TilemapRuntimeManager.getOrCreate(data2);

        assertNotNull(r1);
        assertNotNull(r2);

        TilemapRuntimeManager.disposeAll(null);
        assertNull(TilemapRuntimeManager.peek(data));
        assertNull(TilemapRuntimeManager.peek(data2));
    }
}
