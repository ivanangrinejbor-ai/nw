/*
 * NeoCatroid
 * Copyright (C) 2026 The NeoCatroid Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */

package org.catrobat.catroid.test.neoscript;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.neoscript.NeoScriptException;
import org.catrobat.catroid.neoscript.NeoScriptExporter;
import org.catrobat.catroid.neoscript.NeoScriptFile;
import org.catrobat.catroid.neoscript.NeoScriptImporter;
import org.catrobat.catroid.neoscript.NeoScriptSerializer;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class NeoScriptModuleTest {

	private Project project;
	private Sprite sprite;

	@Before
	public void setUp() {
		project = new Project(MockUtil.mockContextForProject(), "NeoScriptTestProject");
		sprite = new Sprite("TargetObject");
		project.getDefaultScene().addSprite(sprite);
	}

	private NeoScriptFile buildSampleFile(String variableName) {
		org.catrobat.catroid.content.BroadcastScript script =
				new org.catrobat.catroid.content.BroadcastScript("msg_" + variableName);
		UserVariable variable = new UserVariable(variableName);
		script.addBrick(new SetVariableBrick(new Formula(0), variable));
		NeoScriptFile file = new NeoScriptFile();
		file.getScripts().add(script);
		return file;
	}

	@Test
	public void testSingleScriptExportRoundTrip() throws Exception {
		NeoScriptFile file = buildSampleFile("score");
		String xml = NeoScriptSerializer.serializeToString(file);
		assertNotNull(xml);
		assertTrue(xml.contains("<neoscript>"));

		NeoScriptFile restored = NeoScriptSerializer.deserializeFromString(xml);
		assertEquals(1, restored.getScripts().size());

		Script restoredScript = restored.getScripts().get(0);
		assertEquals(1, restoredScript.getBrickList().size());
		assertTrue(restoredScript.getBrickList().get(0) instanceof SetVariableBrick);

		SetVariableBrick brick = (SetVariableBrick) restoredScript.getBrickList().get(0);
		assertEquals("score", brick.getUserVariable().getName());
	}

	@Test
	public void testMultipleScriptExport() throws Exception {
		NeoScriptFile file = new NeoScriptFile();
		file.getScripts().add(new StartScript());
		file.getScripts().add(new StartScript());
		assertEquals(2, file.getScripts().size());

		String xml = NeoScriptSerializer.serializeToString(file);
		NeoScriptFile restored = NeoScriptSerializer.deserializeFromString(xml);
		assertEquals(2, restored.getScripts().size());
	}

	@Test
	public void testImportAddsScriptsAndVariables() throws Exception {
		NeoScriptFile file = buildSampleFile("health");
		NeoScriptImporter.ImportResult result =
				NeoScriptImporter.importScripts(file, project, sprite, false);

		assertEquals(1, result.added.size());
		assertEquals(1, sprite.getScriptList().size());
		assertNotNull(sprite.getUserVariable("health"));
	}

	@Test
	public void testImportSkipsDuplicatesWhenOverwriteFalse() throws Exception {
		NeoScriptFile file = buildSampleFile("energy");

		NeoScriptImporter.importScripts(file, project, sprite, false);
		NeoScriptImporter.ImportResult second =
				NeoScriptImporter.importScripts(file, project, sprite, false);

		assertEquals(1, second.skipped.size());
		assertEquals(0, second.added.size());
		assertEquals(1, sprite.getScriptList().size());
	}

	@Test
	public void testImportReplacesDuplicatesWhenOverwriteTrue() throws Exception {
		NeoScriptFile file = buildSampleFile("mana");

		NeoScriptImporter.importScripts(file, project, sprite, false);
		NeoScriptImporter.ImportResult second =
				NeoScriptImporter.importScripts(file, project, sprite, true);

		assertEquals(1, second.replaced.size());
		assertEquals(1, second.added.size());
		assertEquals(1, sprite.getScriptList().size());
	}

	@Test
	public void testImportGeneratesFreshIds() throws Exception {
		NeoScriptFile file = buildSampleFile("idtest");
		NeoScriptImporter.ImportResult result =
				NeoScriptImporter.importScripts(file, project, sprite, false);

		Script imported = result.added.get(0);
		assertNotSame(file.getScripts().get(0).getScriptId(), imported.getScriptId());
	}

	@Test
	public void testInvalidFileWrongRootElement() {
		try {
			NeoScriptSerializer.deserializeFromString("<program></program>");
			fail("Expected NeoScriptException");
		} catch (NeoScriptException e) {
			assertTrue(e.getMessage().contains("Wrong file type"));
		}
	}

	@Test
	public void testEmptyFileRejected() {
		try {
			NeoScriptSerializer.deserializeFromString("");
			fail("Expected NeoScriptException");
		} catch (NeoScriptException e) {
			assertTrue(e.getMessage().contains("empty"));
		}
	}

	@Test
	public void testCorruptedDataRejected() {
		try {
			NeoScriptSerializer.deserializeFromString("<neoscript><script type=\"StartScript\">");
			fail("Expected NeoScriptException");
		} catch (NeoScriptException e) {
			assertTrue(e.getMessage().contains("Corrupted"));
		}
	}

	@Test
	public void testWrongExtensionRejected() throws Exception {
		File wrongFile = File.createTempFile("module", ".txt");
		wrongFile.deleteOnExit();
		java.nio.file.Files.write(wrongFile.toPath(), "data".getBytes());
		try {
			NeoScriptSerializer.deserializeFromFile(wrongFile);
			fail("Expected NeoScriptException");
		} catch (NeoScriptException e) {
			assertTrue(e.getMessage().contains("extension"));
		}
	}

	@Test
	public void testFutureVersionRejected() throws Exception {
		NeoScriptFile file = buildSampleFile("future");
		String xml = NeoScriptSerializer.serializeToString(file)
				.replace("<formatVersion>1</formatVersion>", "<formatVersion>999</formatVersion>");
		try {
			NeoScriptSerializer.deserializeFromString(xml);
			fail("Expected NeoScriptException");
		} catch (NeoScriptException e) {
			assertTrue(e.getMessage().contains("Future unsupported"));
		}
	}

	@Test
	public void testOldVersionRejected() throws Exception {
		NeoScriptFile file = buildSampleFile("old");
		String xml = NeoScriptSerializer.serializeToString(file)
				.replace("<formatVersion>1</formatVersion>", "<formatVersion>0</formatVersion>");
		try {
			NeoScriptSerializer.deserializeFromString(xml);
			fail("Expected NeoScriptException");
		} catch (NeoScriptException e) {
			assertTrue(e.getMessage().contains("Old incompatible"));
		}
	}

	@Test
	public void testLargeScriptLoadsWithoutFreeze() throws Exception {
		NeoScriptFile file = new NeoScriptFile();
		StartScript script = new StartScript();
		List<UserVariable> variables = new ArrayList<>();
		for (int i = 0; i < 300; i++) {
			UserVariable variable = new UserVariable("var" + i);
			variables.add(variable);
			script.addBrick(new SetVariableBrick(new Formula(i), variable));
		}
		file.getScripts().add(script);

		long start = System.currentTimeMillis();
		String xml = NeoScriptSerializer.serializeToString(file);
		NeoScriptFile restored = NeoScriptSerializer.deserializeFromString(xml);
		long duration = System.currentTimeMillis() - start;

		assertEquals(300, restored.getScripts().get(0).getBrickList().size());
		assertTrue("Large script load took too long: " + duration + "ms", duration < 30000);
	}

	@Test
	public void testExporterCollectsReferencedVariables() {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable("exportedVar");
		script.addBrick(new SetVariableBrick(new Formula(0), variable));
		List<Script> scripts = new ArrayList<>();
		scripts.add(script);
		NeoScriptFile file = NeoScriptExporter.buildFromScripts(scripts, project, sprite);
		assertEquals(1, file.getScripts().size());
		assertFalse(file.getUserVariables().isEmpty());
		assertEquals("exportedVar", file.getUserVariables().get(0).getName());
	}

	@Test
	public void testUndoRedoStateModel() throws Exception {
		NeoScriptFile file = buildSampleFile("undoVar");

		NeoScriptImporter.ImportResult first =
				NeoScriptImporter.importScripts(file, project, sprite, false);
		assertEquals(1, first.added.size());

		sprite.getScriptList().removeAll(first.added);

		NeoScriptImporter.ImportResult redo =
				NeoScriptImporter.importScripts(file, project, sprite, false);
		assertEquals(1, redo.added.size());
		assertEquals(1, sprite.getScriptList().size());
		assertNull(sprite.getUserVariable("neverCreated"));
	}

	@Test
	public void ns07a_multipleStartScripts_importedIntoEmptySprite_allAdded()
			throws NeoScriptException {
		NeoScriptFile file = new NeoScriptFile();
		for (int i = 0; i < 3; i++) {
			StartScript s = new StartScript();
			s.addBrick(new org.catrobat.catroid.content.bricks.NoteBrick("Block " + i));
			file.getScripts().add(s);
		}

		NeoScriptImporter.ImportResult result =
				NeoScriptImporter.importScripts(file, project, sprite,
						NeoScriptImporter.ImportStrategy.SKIP_DUPLICATES);

		assertEquals("All 3 StartScripts must be added", 3, result.added.size());
		assertEquals("Sprite must have 3 scripts", 3, sprite.getScriptList().size());
		assertEquals("Nothing should be skipped", 0, result.skipped.size());
	}

	@Test
	public void ns07b_multipleStartScripts_importedIntoSpriteWithExisting_allAdded()
			throws NeoScriptException {
		StartScript existing = new StartScript();
		existing.addBrick(new org.catrobat.catroid.content.bricks.NoteBrick("Existing"));
		sprite.addScript(existing);

		NeoScriptFile file = new NeoScriptFile();
		for (int i = 0; i < 3; i++) {
			StartScript s = new StartScript();
			s.addBrick(new org.catrobat.catroid.content.bricks.NoteBrick("Imported " + i));
			file.getScripts().add(s);
		}

		NeoScriptImporter.ImportResult result =
				NeoScriptImporter.importScripts(file, project, sprite,
						NeoScriptImporter.ImportStrategy.SKIP_DUPLICATES);

		assertEquals("All 3 imported StartScripts must be added", 3, result.added.size());
		assertEquals("Sprite must have 4 scripts total (1 existing + 3 new)", 4,
				sprite.getScriptList().size());
		assertEquals("Nothing should be skipped", 0, result.skipped.size());
	}

	@Test
	public void ns08_replaceDuplicates_multipleStartScripts_existingPreserved()
			throws NeoScriptException {
		StartScript existing = new StartScript();
		existing.addBrick(new org.catrobat.catroid.content.bricks.NoteBrick("Existing"));
		sprite.addScript(existing);

		NeoScriptFile file = new NeoScriptFile();
		for (int i = 0; i < 2; i++) {
			StartScript s = new StartScript();
			s.addBrick(new org.catrobat.catroid.content.bricks.NoteBrick("New " + i));
			file.getScripts().add(s);
		}

		NeoScriptImporter.ImportResult result =
				NeoScriptImporter.importScripts(file, project, sprite,
						NeoScriptImporter.ImportStrategy.REPLACE_DUPLICATES);

		assertTrue("Existing StartScript must be preserved",
				sprite.getScriptList().contains(existing));
		assertEquals("2 new StartScripts added", 2, result.added.size());
		assertEquals("Sprite has 3 scripts total", 3, sprite.getScriptList().size());
	}

	@Test
	public void ns07_sanity_broadcastScriptWithSameMessage_skippedCorrectly()
			throws NeoScriptException {
		org.catrobat.catroid.content.BroadcastScript existing =
				new org.catrobat.catroid.content.BroadcastScript();
		existing.setBroadcastMessage("hello");
		sprite.addScript(existing);

		NeoScriptFile file = new NeoScriptFile();
		org.catrobat.catroid.content.BroadcastScript duplicate =
				new org.catrobat.catroid.content.BroadcastScript();
		duplicate.setBroadcastMessage("hello");
		file.getScripts().add(duplicate);

		NeoScriptImporter.ImportResult result =
				NeoScriptImporter.importScripts(file, project, sprite,
						NeoScriptImporter.ImportStrategy.SKIP_DUPLICATES);

		assertEquals("Duplicate BroadcastScript('hello') must be skipped", 1, result.skipped.size());
		assertEquals("Sprite should still have only 1 script", 1, sprite.getScriptList().size());
	}
}

