/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.actions;

import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.TilemapLookData;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ClearTileAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.SetTileAction;
import org.catrobat.catroid.content.actions.SetTilemapSolidAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class TilemapActionTest {

    private Sprite sprite;
    private TilemapLookData tilemap;

    @Before
    public void setUp() {
        sprite = new Sprite("testSprite");
        tilemap = new TilemapLookData();
        tilemap.setMapSize(10, 10);
        tilemap.setTileWidth(16);
        tilemap.setTileHeight(16);
        tilemap.initLayers(1);
        sprite.look.setLookData(tilemap);
    }

    private Scope makeScope() {
        return new Scope(null, sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));
    }

    // ======================= SetTileAction =======================

    @Test
    public void testSetTileSetsTile() {
        SetTileAction action = new SetTileAction();
        action.setScope(makeScope());
        action.setColumn(new Formula(3));
        action.setRow(new Formula(4));
        action.setTileIndex(new Formula(7));
        action.act(1.0f);

        assertEquals(7, tilemap.getTile(3, 4));
    }

    @Test
    public void testSetTileMultipleCells() {
        // First tile
        SetTileAction action1 = new SetTileAction();
        action1.setScope(makeScope());
        action1.setColumn(new Formula(0));
        action1.setRow(new Formula(0));
        action1.setTileIndex(new Formula(1));
        action1.act(1.0f);

        // Second tile (new action instance — TemporalAction is one-shot)
        SetTileAction action2 = new SetTileAction();
        action2.setScope(makeScope());
        action2.setColumn(new Formula(5));
        action2.setRow(new Formula(5));
        action2.setTileIndex(new Formula(2));
        action2.act(1.0f);

        assertEquals(1, tilemap.getTile(0, 0));
        assertEquals(2, tilemap.getTile(5, 5));
        assertEquals(TilemapLookData.EMPTY, tilemap.getTile(3, 3));
    }

    @Test
    public void testSetTileNoOpWhenNotTilemap() {
        sprite.look.setLookData(Mockito.mock(LookData.class));
        SetTileAction action = new SetTileAction();
        action.setScope(makeScope());
        action.setColumn(new Formula(0));
        action.setRow(new Formula(0));
        action.setTileIndex(new Formula(1));
        action.act(1.0f);
        // Should not throw — just no-op
    }

    @Test
    public void testSetTileNoOpWhenScopeNull() {
        SetTileAction action = new SetTileAction();
        // scope is null by default
        action.setColumn(new Formula(0));
        action.setRow(new Formula(0));
        action.setTileIndex(new Formula(1));
        action.act(1.0f);
        // Should not throw
    }

    @Test
    public void testSetTileOutOfBoundsIsHarmless() {
        SetTileAction action = new SetTileAction();
        action.setScope(makeScope());
        action.setColumn(new Formula(999));
        action.setRow(new Formula(999));
        action.setTileIndex(new Formula(1));
        action.act(1.0f);
        // Out of bounds → setTile returns false, no crash
    }

    // ======================= ClearTileAction =======================

    @Test
    public void testClearTileSetsToEmpty() {
        tilemap.setTile(2, 3, (short) 5);
        assertEquals(5, tilemap.getTile(2, 3));

        ClearTileAction action = new ClearTileAction();
        action.setScope(makeScope());
        action.setColumn(new Formula(2));
        action.setRow(new Formula(3));
        action.act(1.0f);

        assertEquals(TilemapLookData.EMPTY, tilemap.getTile(2, 3));
    }

    @Test
    public void testClearTileNoOpWhenNotTilemap() {
        sprite.look.setLookData(Mockito.mock(LookData.class));
        ClearTileAction action = new ClearTileAction();
        action.setScope(makeScope());
        action.setColumn(new Formula(0));
        action.setRow(new Formula(0));
        action.act(1.0f);
    }

    @Test
    public void testClearTileAlreadyEmpty() {
        
        ClearTileAction action = new ClearTileAction();
        action.setScope(makeScope());
        action.setColumn(new Formula(0));
        action.setRow(new Formula(0));
        action.act(1.0f);
        assertEquals(TilemapLookData.EMPTY, tilemap.getTile(0, 0));
    }

    

    @Test
    public void testSetSolidTrue() {
        SetTilemapSolidAction action = new SetTilemapSolidAction();
        action.setScope(makeScope());
        action.setTileIndex(new Formula(3));
        action.setSolid(new Formula(1));
        action.act(1.0f);

        assertTrue(tilemap.isSolidTile(3));
    }

    @Test
    public void testSetSolidFalse() {
        tilemap.setTileSolid(3, true);
        assertTrue(tilemap.isSolidTile(3));

        SetTilemapSolidAction action = new SetTilemapSolidAction();
        action.setScope(makeScope());
        action.setTileIndex(new Formula(3));
        action.setSolid(new Formula(0));
        action.act(1.0f);

        assertFalse(tilemap.isSolidTile(3));
    }

    @Test
    public void testSetSolidNonZeroIsTrue() {
        SetTilemapSolidAction action = new SetTilemapSolidAction();
        action.setScope(makeScope());
        action.setTileIndex(new Formula(5));
        action.setSolid(new Formula(42)); // any non-zero = true
        action.act(1.0f);

        assertTrue(tilemap.isSolidTile(5));
    }

    @Test
    public void testSetSolidNoOpWhenNotTilemap() {
        sprite.look.setLookData(Mockito.mock(LookData.class));
        SetTilemapSolidAction action = new SetTilemapSolidAction();
        action.setScope(makeScope());
        action.setTileIndex(new Formula(1));
        action.setSolid(new Formula(1));
        action.act(1.0f);
    }

    @Test
    public void testSetSolidNoChangeDoesNotInvalidate() {
        // Already solid → setting solid again should be a no-op
        tilemap.setTileSolid(2, true);

        SetTilemapSolidAction action = new SetTilemapSolidAction();
        action.setScope(makeScope());
        action.setTileIndex(new Formula(2));
        action.setSolid(new Formula(1)); // already solid
        action.act(1.0f);

        assertTrue(tilemap.isSolidTile(2)); // still solid
    }
}
