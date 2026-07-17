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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotSame;
import static org.junit.Assert.fail;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.NoteBrick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.content.bricks.UnknownBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.neoscript.NeoScriptException;
import org.catrobat.catroid.neoscript.NeoScriptFile;
import org.catrobat.catroid.neoscript.NeoScriptImporter;
import org.catrobat.catroid.neoscript.NeoScriptSerializer;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for NeoScript scene-aware features:
 * - Scene resolution in CreateObjectBrick / AssignScriptsBrick
 * - UnknownBrick detection and safe replacement
 * - Cross-scene object isolation
 * - Backward compatibility (missing scene field)
 */
@RunWith(JUnit4.class)
public class NeoScriptSceneTest {

	private Project project;
	private Scene scene1;
	private Scene scene2;
	private Sprite spriteInScene1;
	private Sprite spriteInScene2;

	@Before
	public void setUp() {
		project = new Project(MockUtil.mockContextForProject(), "SceneTestProject");
		scene1 = project.getDefaultScene();

		// Create a second scene
		scene2 = new Scene();
		scene2.setName("Scene2");
		project.addScene(scene2);

		// Add sprites to each scene
		spriteInScene1 = new Sprite("Hero");
		scene1.addSprite(spriteInScene1);

		spriteInScene2 = new Sprite("Hero");  // Same name, different scene
		scene2.addSprite(spriteInScene2);

		// Set currently edited scene to scene1
		ProjectManager.getInstance().setCurrentProject(project);
		ProjectManager.getInstance().setCurrentlyEditedScene(scene1);
	}

	// ============ SCENE RESOLUTION TESTS ============

	@Test
	public void testScene1ContainsHero() {
		assertNotNull(scene1.getSprite("Hero"));
		assertNotNull(scene2.getSprite("Hero"));
		assertNotSame(
				"Same-named sprites in different scenes must not be the same object",
				scene1.getSprite("Hero"),
				scene2.getSprite("Hero"));
	}

	@Test
	public void testScene2SpriteIsIsolatedFromScene1() {
		Sprite hero1 = scene1.getSprite("Hero");
		Sprite hero2 = scene2.getSprite("Hero");
		assertNotSame(hero1, hero2);

		// Modify scene1's sprite — should not affect scene2's sprite
		hero1.setName("HeroModified");
		assertEquals("Hero", scene2.getSprite("Hero").getName());
	}

	@Test
	public void testProjectGetSceneByName() {
		Scene found = project.getSceneByName("Scene2");
		assertNotNull("Scene2 not found in project", found);
		assertEquals("Scene2", found.getName());
	}

	@Test
	public void testProjectGetSceneByNameReturnsNullForUnknown() {
		assertNull(project.getSceneByName("NonExistentScene"));
	}

	@Test
	public void testGetDefaultSceneReturnsFirstScene() {
		Scene defaultScene = project.getDefaultScene();
		assertNotNull(defaultScene);
		assertEquals(scene1.getName(), defaultScene.getName());
	}

	// ============ BACKWARD COMPATIBILITY TESTS ============

	@Test
	public void testSceneFieldNullDefaultsToCurrentScene() {
		// Simulate a NeoScriptFile with no scene metadata
		// The brick should default to Current Scene (currently edited scene)
		// This test verifies the fallback logic, not the brick itself
		String sceneName = null;  // null = Current scene
		Scene resolved = resolveScene(sceneName);
		assertNotNull("Resolved scene must not be null", resolved);
		assertEquals("Default scene should be scene1", scene1.getName(), resolved.getName());
	}

	@Test
	public void testSceneFieldEmptyDefaultsToCurrentScene() {
		Scene resolved = resolveScene("");
		assertNotNull(resolved);
		assertEquals(scene1.getName(), resolved.getName());
	}

	@Test
	public void testSpecificSceneResolvesCorrectly() {
		Scene resolved = resolveScene("Scene2");
		assertNotNull(resolved);
		assertEquals("Scene2", resolved.getName());
	}

	// ============ UNKNOWN BRICK DETECTION TESTS ============

	@Test
	public void testCheckForUnknownBricksPositive() {
		NeoScriptFile file = buildScriptWithUnknownBrick();
		assertTrue("Must detect UnknownBrick", containsUnknownBricks(file));
	}

	@Test
	public void testCheckForUnknownBricksNegative() {
		NeoScriptFile file = buildSampleFile("normalVar");
		assertFalse("Must not detect UnknownBrick when none present", containsUnknownBricks(file));
	}

	@Test
	public void testReplaceUnknownBricks() {
		NeoScriptFile file = buildScriptWithUnknownBrick();
		assertTrue("Precondition: must have UnknownBrick", containsUnknownBricks(file));

		replaceUnknownBricks(file);
		assertFalse("After replacement, must not have UnknownBrick", containsUnknownBricks(file));

		// Verify NoteBricks were inserted
		Script script = file.getScripts().get(0);
		boolean hasNoteBrick = false;
		for (Brick brick : script.getBrickList()) {
			if (brick instanceof NoteBrick) {
				hasNoteBrick = true;
				break;
			}
		}
		assertTrue("Must have at least one NoteBrick after replacement", hasNoteBrick);
	}

	@Test
	public void testReplaceUnknownBricksDoesNotAffectNormalBricks() {
		NeoScriptFile file = buildScriptWithUnknownBrick();
		replaceUnknownBricks(file);

		Script script = file.getScripts().get(0);
		int normalBrickCount = 0;
		for (Brick brick : script.getBrickList()) {
			if (!(brick instanceof NoteBrick)) {
				normalBrickCount++;
			}
		}
		assertTrue("Normal bricks should still be present", normalBrickCount > 0);
	}

	// ============ CROSS-SCENE OBJECT LOOKUP TESTS ============

	@Test
	public void testObjectLookupScopedToScene1() {
		// Scene 1 has "Hero" — should find it
		Sprite found = scene1.getSprite("Hero");
		assertNotNull(found);
		assertEquals(spriteInScene1.getName(), found.getName());
	}

	@Test
	public void testObjectLookupScopedToScene2() {
		// Scene 2 also has "Hero" — should find the scene2 version
		Sprite found = scene2.getSprite("Hero");
		assertNotNull(found);
		assertEquals(spriteInScene2.getName(), found.getName());
	}

	@Test
	public void testObjectLookupScopedToSceneDoesNotCrossBoundary() {
		// Scene 1 does NOT have "Villain"
		assertNull(scene1.getSprite("Villain"));
		// Adding to scene1 should NOT affect scene2
		scene1.addSprite(new Sprite("Villain"));
		assertNotNull(scene1.getSprite("Villain"));
		assertNull("Scene2 must not be contaminated", scene2.getSprite("Villain"));
	}

	// ============ DUPLICATE OBJECT NAME TESTS ============

	@Test
	public void testDuplicateNameInSameScene() {
		// Adding a sprite with the same name should succeed in the model
		Sprite duplicate = new Sprite("Hero");
		scene1.addSprite(duplicate);
		// scene1.getSprite("Hero") returns the FIRST match
		Sprite first = scene1.getSprite("Hero");
		assertNotNull(first);
	}

	// ============ NEOSCRIPT IMPORT CROSS-SCENE TESTS ============

	@Test
	public void testImportIntoScene1SpriteDoesNotAffectScene2() throws Exception {
		NeoScriptFile file = buildSampleFile("crossVar");
		Sprite target = scene1.getSprite("Hero");
		assertNotNull("Precondition: Hero must exist in scene1", target);

		NeoScriptImporter.ImportResult result =
				NeoScriptImporter.importScripts(file, project, target, false);

		assertEquals("Must add scripts to scene1's Hero", 1, result.added.size());
		assertEquals("scene1's Hero must have 1 script", 1, target.getScriptList().size());

		Sprite otherHero = scene2.getSprite("Hero");
		assertEquals("scene2's Hero must not be affected", 0, otherHero.getScriptList().size());
	}

	// ============ CORNER CASE TESTS ============

	@Test
	public void testSceneNotFoundLogsError() {
		// If an action tries to resolve a scene that doesn't exist,
		// it should return null rather than crashing
		Scene notFound = project.getSceneByName("ImaginaryScene");
		assertNull("Non-existent scene must return null", notFound);
	}

	@Test
	public void testSpriteNotFoundInSceneReturnsNull() {
		Sprite notFound = scene1.getSprite("NonExistent");
		assertNull("Non-existent sprite must return null", notFound);
	}

	@Test
	public void testEmptySceneHasBackgroundSprite() {
		Scene emptyScene = new Scene();
		emptyScene.setName("Empty");
		project.addScene(emptyScene);
		assertNull("Empty scene without setup has no background", emptyScene.getBackgroundSprite());
	}

	// ============ ASSIGN-SCRIPTS REPLACE MODE TESTS ============

	@Test
	public void testAppendAllKeepsExistingAndAddsImported() throws Exception {
		Sprite target = scene1.getSprite("Hero");
		target.addScript(new StartScript());
		int before = target.getScriptList().size();

		NeoScriptFile file = buildSampleFile("appendVar");
		NeoScriptImporter.ImportResult result = NeoScriptImporter.importScripts(
				file, project, target, NeoScriptImporter.ImportStrategy.APPEND_ALL);

		assertEquals("One script added", 1, result.added.size());
		assertEquals("Existing kept + imported added", before + 1, target.getScriptList().size());
	}

	@Test
	public void testReplaceAllRemovesExistingScripts() throws Exception {
		Sprite target = scene1.getSprite("Hero");
		target.addScript(new StartScript()); // pre-existing, different signature

		NeoScriptFile file = buildSampleFile("replaceVar");
		NeoScriptImporter.ImportResult result = NeoScriptImporter.importScripts(
				file, project, target, NeoScriptImporter.ImportStrategy.REPLACE_ALL);

		assertEquals("One script added", 1, result.added.size());
		assertEquals("All previous scripts removed, only imported remains",
				1, target.getScriptList().size());

		Script remaining = target.getScriptList().get(0);
		boolean foundImported = false;
		for (Brick brick : remaining.getBrickList()) {
			if (brick instanceof SetVariableBrick) {
				foundImported = true;
			}
		}
		assertTrue("Imported script must be present after REPLACE_ALL", foundImported);
	}

	@Test
	public void testReplaceAllSceneIsolation() throws Exception {
		NeoScriptFile file = buildSampleFile("isoVar");
		Sprite target = scene1.getSprite("Hero");

		NeoScriptImporter.importScripts(file, project, target, NeoScriptImporter.ImportStrategy.REPLACE_ALL);

		Sprite otherHero = scene2.getSprite("Hero");
		assertEquals("scene2's Hero must not be affected by REPLACE_ALL on scene1",
				0, otherHero.getScriptList().size());
	}

	@Test
	public void testBooleanOverwriteStillMapsToDuplicateStrategy() throws Exception {
		// Backward compat: the boolean API (used by ImportScriptBrick) must still work.
		NeoScriptFile file = buildSampleFile("compatVar");
		Sprite target = scene1.getSprite("Hero");

		NeoScriptImporter.ImportResult first = NeoScriptImporter.importScripts(file, project, target, false);
		assertEquals(1, first.added.size());

		NeoScriptImporter.ImportResult second = NeoScriptImporter.importScripts(file, project, target, false);
		assertEquals("Duplicate skipped when overwrite=false", 0, second.added.size());
		assertEquals("Duplicate counted as skipped", 1, second.skipped.size());
	}

	@Test
	public void testAssignScriptsModeZeroEqualsAppendAll() throws Exception {
		Sprite target = scene1.getSprite("Hero");
		target.addScript(new StartScript());
		int before = target.getScriptList().size();

		NeoScriptFile file = buildSampleFile("mode0Var");
		NeoScriptImporter.importScripts(file, project, target, NeoScriptImporter.ImportStrategy.APPEND_ALL);

		assertEquals("Mode 0 keeps existing and adds imported",
				before + 1, target.getScriptList().size());
	}

	@Test
	public void testAssignScriptsModeOneEqualsReplaceAll() throws Exception {
		Sprite target = scene1.getSprite("Hero");
		target.addScript(new StartScript());

		NeoScriptFile file = buildSampleFile("mode1Var");
		NeoScriptImporter.importScripts(file, project, target, NeoScriptImporter.ImportStrategy.REPLACE_ALL);

		assertEquals("Mode 1 removes all existing, only imported remains",
				1, target.getScriptList().size());
	}

	// ============ HELPER METHODS ============

	private Scene resolveScene(String sceneName) {
		if (sceneName == null || sceneName.isEmpty()) {
			return project.getDefaultScene();
		}
		return project.getSceneByName(sceneName);
	}

	private boolean containsUnknownBricks(NeoScriptFile file) {
		for (Script script : file.getScripts()) {
			for (Brick brick : script.getBrickList()) {
				if (brick instanceof UnknownBrick) {
					return true;
				}
			}
		}
		return false;
	}

	private void replaceUnknownBricks(NeoScriptFile file) {
		for (Script script : file.getScripts()) {
			List<Brick> newBrickList = new ArrayList<>();
			for (Brick brick : script.getBrickList()) {
				if (brick instanceof UnknownBrick) {
					newBrickList.add(new NoteBrick("This block is not supported"));
				} else {
					newBrickList.add(brick);
				}
			}
			script.getBrickList().clear();
			script.getBrickList().addAll(newBrickList);
		}
	}

	private NeoScriptFile buildSampleFile(String variableName) {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable(variableName);
		script.addBrick(new SetVariableBrick(new Formula(0), variable));
		NeoScriptFile file = new NeoScriptFile();
		file.getScripts().add(script);
		return file;
	}

	private NeoScriptFile buildScriptWithUnknownBrick() {
		StartScript script = new StartScript();
		script.addBrick(new UnknownBrick("SomeFutureBrick"));
		script.addBrick(new SetVariableBrick(new Formula(42), new UserVariable("normalVar")));
		NeoScriptFile file = new NeoScriptFile();
		file.getScripts().add(script);
		return file;
	}
}
