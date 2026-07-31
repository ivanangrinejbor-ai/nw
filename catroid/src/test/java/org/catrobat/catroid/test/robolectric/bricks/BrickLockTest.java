/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.robolectric.bricks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.DeleteCloneByNumberBrick;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BrickLockTest {

	@Test
	public void testInitiallyUnlocked() {
		Brick brick = new DeleteCloneByNumberBrick(1);
		assertFalse(brick.isLocked());
	}

	@Test
	public void testSetLockAndVerify() {
		Brick brick = new DeleteCloneByNumberBrick(1);
		brick.setLock("secret");
		assertTrue(brick.isLocked());
		assertTrue(brick.verifyLock("secret"));
		assertFalse(brick.verifyLock("wrong"));
	}

	@Test
	public void testClearLock() {
		Brick brick = new DeleteCloneByNumberBrick(1);
		brick.setLock("secret");
		brick.clearLock();
		assertFalse(brick.isLocked());
		assertTrue(brick.verifyLock("anything"));
	}

	@Test
	public void testCloneInheritsLock() throws Exception {
		DeleteCloneByNumberBrick brick = new DeleteCloneByNumberBrick(1);
		brick.setLock("secret");
		DeleteCloneByNumberBrick clone = (DeleteCloneByNumberBrick) brick.clone();
		assertNotNull(clone);
		assertTrue(clone.isLocked());
		assertTrue(clone.verifyLock("secret"));
	}
}
