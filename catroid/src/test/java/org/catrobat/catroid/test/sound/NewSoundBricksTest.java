package org.catrobat.catroid.test.sound;

import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.*;
import org.catrobat.catroid.content.GlobalManager;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for 7 new sound bricks:
 * PauseSound, ResumeSound, SetSoundLoop, CrossFadeSound,
 * PlaySoundWithSpeed, SetGameVolume + master volume override
 */
public class NewSoundBricksTest {

    @Before
    public void setUp() {
        GlobalManager.Companion.setGameVolume(null);
    }

    @After
    public void tearDown() {
        GlobalManager.Companion.setGameVolume(null);
    }

    // --- PauseSoundBrick ---

    @Test
    public void testPauseSoundBrickCreation() {
        PauseSoundBrick brick = new PauseSoundBrick();
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_pause_sound, brick.getViewResource());
    }

    @Test
    public void testPauseSoundBrickSoundNull() {
        PauseSoundBrick brick = new PauseSoundBrick();
        assertNull(brick.getSound());
    }

    // --- ResumeSoundBrick ---

    @Test
    public void testResumeSoundBrickCreation() {
        ResumeSoundBrick brick = new ResumeSoundBrick();
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_resume_sound, brick.getViewResource());
    }

    @Test
    public void testResumeSoundBrickSoundNull() {
        ResumeSoundBrick brick = new ResumeSoundBrick();
        assertNull(brick.getSound());
    }

    // --- SetSoundLoopBrick ---

    @Test
    public void testSetSoundLoopBrickCreation() {
        SetSoundLoopBrick brick = new SetSoundLoopBrick();
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_set_sound_loop, brick.getViewResource());
    }

    @Test
    public void testSetSoundLoopBrickDefaultLoop() {
        SetSoundLoopBrick brick = new SetSoundLoopBrick();
        assertEquals(1, brick.getLoopEnabled()); // default = Yes (1)
    }

    // --- CrossFadeSoundBrick ---

    @Test
    public void testCrossFadeSoundBrickCreation() {
        CrossFadeSoundBrick brick = new CrossFadeSoundBrick(2.0);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_cross_fade_sound, brick.getViewResource());
    }

    @Test
    public void testCrossFadeSoundBrickFormula() {
        CrossFadeSoundBrick brick = new CrossFadeSoundBrick(new Formula(1.5));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.DURATION));
    }

    @Test
    public void testCrossFadeSoundBrickClone() throws CloneNotSupportedException {
        CrossFadeSoundBrick brick = new CrossFadeSoundBrick(3.0);
        Brick clone = brick.clone();
        assertNotNull(clone);
    }

    // --- PlaySoundWithSpeedBrick ---

    @Test
    public void testPlaySoundWithSpeedBrickCreation() {
        PlaySoundWithSpeedBrick brick = new PlaySoundWithSpeedBrick();
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_play_sound_with_speed, brick.getViewResource());
    }

    @Test
    public void testPlaySoundWithSpeedBrickFormula() {
        PlaySoundWithSpeedBrick brick = new PlaySoundWithSpeedBrick(2.0);
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PLAYBACK_SPEED));
    }

    // --- SetGameVolumeBrick ---

    @Test
    public void testSetGameVolumeBrickCreation() {
        SetGameVolumeBrick brick = new SetGameVolumeBrick(50.0);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_set_game_volume, brick.getViewResource());
    }

    @Test
    public void testSetGameVolumeBrickFormula() {
        SetGameVolumeBrick brick = new SetGameVolumeBrick(new Formula(75));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.VOLUME));
    }

    @Test
    public void testSetGameVolumeBrickClone() throws CloneNotSupportedException {
        SetGameVolumeBrick brick = new SetGameVolumeBrick(100.0);
        Brick clone = brick.clone();
        assertNotNull(clone);
        assertTrue(clone instanceof SetGameVolumeBrick);
    }

    // --- Master Volume Override Logic ---

    @Test
    public void testGameVolumeInitiallyNull() {
        assertNull(GlobalManager.Companion.getGameVolume());
    }

    @Test
    public void testGameVolumeCanBeSet() {
        GlobalManager.Companion.setGameVolume(50);
        assertEquals(Integer.valueOf(50), GlobalManager.Companion.getGameVolume());
    }

    @Test
    public void testGameVolumeCanBeCleared() {
        GlobalManager.Companion.setGameVolume(75);
        GlobalManager.Companion.setGameVolume(null);
        assertNull(GlobalManager.Companion.getGameVolume());
    }

    @Test
    public void testGameVolumeOverrideActiveWhenSet() {
        GlobalManager.Companion.setGameVolume(30);
        assertNotNull(GlobalManager.Companion.getGameVolume());
    }

    @Test
    public void testGameVolumeOverrideInactiveWhenNull() {
        GlobalManager.Companion.setGameVolume(null);
        assertNull(GlobalManager.Companion.getGameVolume());
    }

    @Test
    public void testGameVolumeRange0() {
        GlobalManager.Companion.setGameVolume(0);
        assertEquals(Integer.valueOf(0), GlobalManager.Companion.getGameVolume());
    }

    @Test
    public void testGameVolumeRange100() {
        GlobalManager.Companion.setGameVolume(100);
        assertEquals(Integer.valueOf(100), GlobalManager.Companion.getGameVolume());
    }

    // --- All bricks have no-arg constructor ---

    @Test
    public void testAllNewSoundBricksNoArgConstructor() {
        assertNotNull(new PauseSoundBrick());
        assertNotNull(new ResumeSoundBrick());
        assertNotNull(new SetSoundLoopBrick());
        assertNotNull(new CrossFadeSoundBrick());
        assertNotNull(new PlaySoundWithSpeedBrick());
        assertNotNull(new SetGameVolumeBrick());
    }
}
