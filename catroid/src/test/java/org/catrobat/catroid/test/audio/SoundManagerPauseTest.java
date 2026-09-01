package org.catrobat.catroid.test.audio;

import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.io.SoundManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SoundManagerPauseTest {

	private SoundManager soundManager;

	@Before
	public void setUp() {
		soundManager = SoundManager.getInstance();
		soundManager.clear();
	}

	@After
	public void tearDown() {
		soundManager.clear();
	}

	@Test
	public void testPauseAndResumeState() {
		assertFalse(soundManager.isPaused());
		soundManager.pause();
		assertTrue(soundManager.isPaused());
		soundManager.resume();
		assertFalse(soundManager.isPaused());
	}

	@Test
	public void testClearResetsPausedState() {
		soundManager.pause();
		assertTrue(soundManager.isPaused());
		soundManager.clear();
		assertFalse(soundManager.isPaused());
	}
}
