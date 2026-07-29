package org.catrobat.catroid.test.robolectric.io;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import org.catrobat.catroid.io.AssetConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AssetConverterTest {

	@Test
	public void testIsAudioFileWav() {
		assertTrue(isAudioFile("sound.wav"));
		assertTrue(isAudioFile("SOUND.WAV"));
	}

	@Test
	public void testIsAudioFileMp3() {
		assertTrue(isAudioFile("music.mp3"));
		assertTrue(isAudioFile("MUSIC.MP3"));
	}

	@Test
	public void testIsAudioFileOgg() {
		assertTrue(isAudioFile("effect.ogg"));
	}

	@Test
	public void testIsAudioFileAac() {
		assertTrue(isAudioFile("voice.aac"));
	}

	@Test
	public void testIsAudioFileFlac() {
		assertTrue(isAudioFile("hifi.flac"));
	}

	@Test
	public void testIsAudioFileM4a() {
		assertTrue(isAudioFile("track.m4a"));
	}

	@Test
	public void testIsAudioFileOpus() {
		assertTrue(isAudioFile("speech.opus"));
	}

	@Test
	public void testIsAudioFileAmr() {
		assertTrue(isAudioFile("recording.amr"));
	}

	@Test
	public void testIsAudioFileWma() {
		assertTrue(isAudioFile("old.wma"));
	}

	@Test
	public void testIsNotAudioFilePng() {
		assertFalse(isAudioFile("image.png"));
	}

	@Test
	public void testIsNotAudioFileJpg() {
		assertFalse(isAudioFile("photo.jpg"));
	}

	@Test
	public void testIsNotAudioFileXml() {
		assertFalse(isAudioFile("code.xml"));
	}

	@Test
	public void testImageFormatKeep() {
		assertEquals(AssetConverter.ImageFormat.KEEP, AssetConverter.ImageFormat.KEEP);
	}

	@Test
	public void testImageFormatWebpLossy() {
		assertEquals(AssetConverter.ImageFormat.WEBP_LOSSY, AssetConverter.ImageFormat.WEBP_LOSSY);
	}

	@Test
	public void testImageFormatWebpLossless() {
		assertEquals(AssetConverter.ImageFormat.WEBP_LOSSLESS, AssetConverter.ImageFormat.WEBP_LOSSLESS);
	}

	@Test
	public void testAudioFormatM4a96() {
		assertEquals(AssetConverter.AudioFormat.M4A_96, AssetConverter.AudioFormat.M4A_96);
	}

	@Test
	public void testAudioFormatM4a128() {
		assertEquals(AssetConverter.AudioFormat.M4A_128, AssetConverter.AudioFormat.M4A_128);
	}

	@Test
	public void testAudioFormatM4a192() {
		assertEquals(AssetConverter.AudioFormat.M4A_192, AssetConverter.AudioFormat.M4A_192);
	}

	@Test
	public void testAudioFormatM4a256() {
		assertEquals(AssetConverter.AudioFormat.M4A_256, AssetConverter.AudioFormat.M4A_256);
	}

	@Test
	public void testAudioFormatKeep() {
		assertEquals(AssetConverter.AudioFormat.KEEP, AssetConverter.AudioFormat.KEEP);
	}

	private boolean isAudioFile(String name) {
		name = name.toLowerCase();
		return name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".ogg") ||
			name.endsWith(".aac") || name.endsWith(".flac") || name.endsWith(".wma") ||
			name.endsWith(".opus") || name.endsWith(".m4a") || name.endsWith(".amr");
	}
}
