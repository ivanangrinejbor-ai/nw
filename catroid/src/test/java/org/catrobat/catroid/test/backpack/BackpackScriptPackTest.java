/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.test.backpack;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.badlogic.gdx.utils.GdxNativesLoader;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.common.Backpack;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ChangeVariableBrick;
import org.catrobat.catroid.content.bricks.PlaySoundAndWaitBrick;
import org.catrobat.catroid.content.bricks.PlaySoundBrick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.content.bricks.UserVariableBrickWithFormula;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.ui.controller.BackpackListManager;
import org.catrobat.catroid.ui.recyclerview.controller.ScriptController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GdxNativesLoader.class, BackpackListManager.class, ProjectManager.class})
public class BackpackScriptPackTest {

	private Project project;
	private Sprite sprite;
	private ScriptController scriptController;
	private BackpackListManager backpackListManager;
	private Backpack backpack;

	@Before
	public void setUp() {
		PowerMockito.mockStatic(GdxNativesLoader.class);
		Context context = Mockito.mock(Context.class);
		project = new Project(context, "TestProject");
		sprite = new Sprite("testSprite");
		project.getDefaultScene().addSprite(sprite);

		ProjectManager projectManager = Mockito.mock(ProjectManager.class);
		Mockito.when(projectManager.getCurrentProject()).thenReturn(project);
		Mockito.when(projectManager.getCurrentSprite()).thenReturn(sprite);
		Mockito.when(projectManager.getCurrentlyEditedScene()).thenReturn(project.getDefaultScene());
		PowerMockito.mockStatic(ProjectManager.class);
		when(ProjectManager.getInstance()).thenReturn(projectManager);

		backpack = new Backpack();
		backpackListManager = Mockito.mock(BackpackListManager.class);
		Mockito.when(backpackListManager.getBackpack()).thenReturn(backpack);
		Mockito.when(backpackListManager.getBackpackedScriptSounds()).thenReturn(backpack.backpackedScriptSounds);
		Mockito.when(backpackListManager.getBackpackedVariableValues()).thenReturn(backpack.backpackedVariableValues);
		Mockito.when(backpackListManager.getBackpackedListValues()).thenReturn(backpack.backpackedListValues);
		PowerMockito.mockStatic(BackpackListManager.class);
		when(BackpackListManager.getInstance()).thenReturn(backpackListManager);

		scriptController = new ScriptController();
	}

	@After
	public void tearDown() {
		backpack.backpackedScripts.clear();
		backpack.backpackedScriptSounds.clear();
		backpack.backpackedVariableValues.clear();
		backpack.backpackedListValues.clear();
		backpack.backpackedUserVariables.clear();
		backpack.backpackedUserLists.clear();
	}

	@Test
	public void packScriptWithSounds() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound = createSoundInfo("meow.mp3");
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, false);

		assertTrue("Script should be packed", backpack.backpackedScripts.containsKey("testGroup"));
		assertTrue("Script sounds should be packed", backpack.backpackedScriptSounds.containsKey("testGroup"));
		assertEquals("Should have 1 sound", 1, backpack.backpackedScriptSounds.get("testGroup").size());
		assertEquals("Sound name should match", "meow.mp3", backpack.backpackedScriptSounds.get("testGroup").get(0).name);
	}

	@Test
	public void packScriptWithoutSounds() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound = createSoundInfo("meow.mp3");
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, false);

		assertTrue("Script should be packed", backpack.backpackedScripts.containsKey("testGroup"));
		assertFalse("Script sounds should NOT be packed", backpack.backpackedScriptSounds.containsKey("testGroup"));
	}

	@Test
	public void packScriptDeduplicatesSoundsByName() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound1 = createSoundInfo("meow.mp3");
		SoundInfo sound2 = createSoundInfo("meow.mp3");
		PlaySoundBrick brick1 = new PlaySoundBrick();
		brick1.setSound(sound1);
		PlaySoundBrick brick2 = new PlaySoundBrick();
		brick2.setSound(sound2);
		script.addBrick(brick1);
		script.addBrick(brick2);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, false);

		assertEquals("Duplicate sounds should be deduped", 1, backpack.backpackedScriptSounds.get("testGroup").size());
	}

	@Test
	public void packScriptWithMultipleDifferentSounds() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound1 = createSoundInfo("meow.mp3");
		SoundInfo sound2 = createSoundInfo("woof.mp3");
		PlaySoundBrick brick1 = new PlaySoundBrick();
		brick1.setSound(sound1);
		PlaySoundAndWaitBrick brick2 = new PlaySoundAndWaitBrick();
		brick2.setSound(sound2);
		script.addBrick(brick1);
		script.addBrick(brick2);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, false);

		assertEquals("Should have 2 different sounds", 2, backpack.backpackedScriptSounds.get("testGroup").size());
	}

	@Test
	public void packPlaySoundAndWaitBrick() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound = createSoundInfo("wait.mp3");
		PlaySoundAndWaitBrick soundBrick = new PlaySoundAndWaitBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, false);

		assertTrue("PlaySoundAndWait sound should be packed", backpack.backpackedScriptSounds.containsKey("testGroup"));
		assertEquals("Sound name should match", "wait.mp3", backpack.backpackedScriptSounds.get("testGroup").get(0).name);
	}

	@Test
	public void packSoundWithNullFile() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound = new SoundInfo("broken.mp3", null);
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, false);

		assertFalse("Sound with null file should not be packed", backpack.backpackedScriptSounds.containsKey("testGroup"));
	}

	@Test
	public void packSoundFileNotExists() throws Exception {
		StartScript script = new StartScript();
		File nonExistentFile = new File("/non/existent/file.mp3");
		SoundInfo sound = new SoundInfo("ghost.mp3", nonExistentFile);
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, false);

		assertFalse("Sound with non-existent file should not be packed", backpack.backpackedScriptSounds.containsKey("testGroup"));
	}

	@Test
	public void packScriptWithVariableValues() throws Exception {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable("score", 42.0);
		sprite.addUserVariable(variable);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertTrue("Variable values should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
		assertNotNull("Variable value should exist", backpack.backpackedVariableValues.get("testGroup").get("score"));
	}

	@Test
	public void packScriptWithoutVariableValues() throws Exception {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable("score", 42.0);
		sprite.addUserVariable(variable);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, false);

		assertFalse("Variable values should NOT be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
	}

	@Test
	public void packVariableWithNullValue() throws Exception {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable("empty");
		variable.setValue(null);
		sprite.addUserVariable(variable);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertTrue("Variable with null value should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
		assertEquals("Null value should be empty string", "", backpack.backpackedVariableValues.get("testGroup").get("empty"));
	}

	@Test
	public void packWithNegativeNumberValue() throws Exception {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable("negative", -42.5);
		sprite.addUserVariable(variable);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertTrue("Negative value should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
		assertEquals("Negative value should be preserved", "-42.5", backpack.backpackedVariableValues.get("testGroup").get("negative"));
	}

	@Test
	public void packWithZeroValue() throws Exception {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable("zero", 0.0);
		sprite.addUserVariable(variable);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertTrue("Zero value should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
		assertEquals("Zero value should be preserved", "0.0", backpack.backpackedVariableValues.get("testGroup").get("zero"));
	}

	@Test
	public void packWithSpecialCharactersInVariableName() throws Exception {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable("var-with-dash", 99.0);
		sprite.addUserVariable(variable);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertTrue("Variable with special chars should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
		assertEquals("Value should match", "99.0", backpack.backpackedVariableValues.get("testGroup").get("var-with-dash"));
	}

	@Test
	public void packScriptWithBothSoundsAndValues() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound = createSoundInfo("meow.mp3");
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		UserVariable variable = new UserVariable("lives", 3.0);
		sprite.addUserVariable(variable);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, true);

		assertTrue("Script sounds should be packed", backpack.backpackedScriptSounds.containsKey("testGroup"));
		assertTrue("Variable values should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
		assertEquals("Should have 1 sound", 1, backpack.backpackedScriptSounds.get("testGroup").size());
		assertEquals("Variable value should be 3.0", "3.0", backpack.backpackedVariableValues.get("testGroup").get("lives"));
	}

	@Test
	public void packScriptNoSoundsNoValues() throws Exception {
		StartScript script = new StartScript();
		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, false);

		assertTrue("Script should be packed", backpack.backpackedScripts.containsKey("testGroup"));
		assertFalse("No sounds expected", backpack.backpackedScriptSounds.containsKey("testGroup"));
		assertFalse("No values expected", backpack.backpackedVariableValues.containsKey("testGroup"));
	}

	@Test
	public void packMultipleScriptsInGroup() throws Exception {
		StartScript script1 = new StartScript();
		SoundInfo sound1 = createSoundInfo("sound1.mp3");
		PlaySoundBrick brick1 = new PlaySoundBrick();
		brick1.setSound(sound1);
		script1.addBrick(brick1);

		StartScript script2 = new StartScript();
		SoundInfo sound2 = createSoundInfo("sound2.mp3");
		PlaySoundBrick brick2 = new PlaySoundBrick();
		brick2.setSound(sound2);
		script2.addBrick(brick2);

		List<Brick> bricks = new ArrayList<>();
		bricks.addAll(flattenScript(script1));
		bricks.addAll(flattenScript(script2));

		scriptController.pack("multiGroup", bricks, true, false);

		assertEquals("Should have 2 sounds from 2 scripts", 2, backpack.backpackedScriptSounds.get("multiGroup").size());
	}

	@Test
	public void packWithNullGroupName() throws Exception {
		StartScript script = new StartScript();
		List<Brick> bricks = flattenScript(script);

		scriptController.pack(null, bricks, false, false);

		assertTrue("Script with null group name should be packed", backpack.backpackedScripts.containsKey(null));
	}

	@Test
	public void packWithEmptyBrickList() throws Exception {
		List<Brick> bricks = new ArrayList<>();

		scriptController.pack("emptyGroup", bricks, true, true);

		assertTrue("Empty group should be packed", backpack.backpackedScripts.containsKey("emptyGroup"));
		assertFalse("No sounds for empty group", backpack.backpackedScriptSounds.containsKey("emptyGroup"));
		assertFalse("No values for empty group", backpack.backpackedVariableValues.containsKey("emptyGroup"));
	}

	@Test
	public void unpackSoundExistsInDestination() throws Exception {
		StartScript script = new StartScript();
		SoundInfo backpackSound = createSoundInfo("meow.mp3");
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(backpackSound);
		script.addBrick(soundBrick);

		SoundInfo existingSound = createSoundInfo("meow.mp3");
		sprite.getSoundList().add(existingSound);

		List<SoundInfo> packedSounds = new ArrayList<>();
		packedSounds.add(createSoundInfo("meow.mp3"));
		backpack.backpackedScriptSounds.put("testGroup", packedSounds);

		scriptController.unpack("testGroup", script, sprite);

		assertEquals("Destination should still have 1 sound", 1, sprite.getSoundList().size());
	}

	@Test
	public void unpackSoundNotInBackpackNorDestination() throws Exception {
		StartScript script = new StartScript();
		SoundInfo originalSound = createSoundInfo("missing.mp3");
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(originalSound);
		script.addBrick(soundBrick);

		backpack.backpackedScriptSounds.put("testGroup", new ArrayList<>());

		scriptController.unpack("testGroup", script, sprite);

		assertEquals("Destination should have 0 sounds", 0, sprite.getSoundList().size());
	}

	@Test
	public void unpackMultipleBricksSameSound() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound1 = createSoundInfo("click.mp3");
		SoundInfo sound2 = createSoundInfo("click.mp3");
		PlaySoundBrick brick1 = new PlaySoundBrick();
		brick1.setSound(sound1);
		PlaySoundBrick brick2 = new PlaySoundBrick();
		brick2.setSound(sound2);
		script.addBrick(brick1);
		script.addBrick(brick2);

		List<SoundInfo> packedSounds = new ArrayList<>();
		packedSounds.add(createSoundInfo("click.mp3"));
		backpack.backpackedScriptSounds.put("testGroup", packedSounds);

		scriptController.unpack("testGroup", script, sprite);

		assertEquals("Destination should have 1 sound", 1, sprite.getSoundList().size());
	}

	@Test
	public void unpackSoundNotIncludedInBackpack() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound = createSoundInfo("orphan.mp3");
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		backpack.backpackedScriptSounds.put("testGroup", new ArrayList<>());

		scriptController.unpack("testGroup", script, sprite);

		assertEquals("No sound should be added to destination", 0, sprite.getSoundList().size());
	}

	@Test
	public void unpackWithNullScriptName() throws Exception {
		StartScript script = new StartScript();

		scriptController.unpack(null, script, sprite);
	}

	// ==================== UNPACK: VALUES ====================

	@Test
	public void unpackVariableValueNumber() throws Exception {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable("score");
		sprite.addUserVariable(variable);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		HashMap<String, String> values = new HashMap<>();
		values.put("score", "100.0");
		backpack.backpackedVariableValues.put("testGroup", values);

		scriptController.unpack("testGroup", script, sprite);

		assertEquals("Variable value should be restored", "100.0", variable.getValue().toString());
	}

	@Test
	public void unpackVariableValueString() throws Exception {
		StartScript script = new StartScript();
		UserVariable variable = new UserVariable("name");
		sprite.addUserVariable(variable);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		HashMap<String, String> values = new HashMap<>();
		values.put("name", "Player1");
		backpack.backpackedVariableValues.put("testGroup", values);

		scriptController.unpack("testGroup", script, sprite);

		assertEquals("Variable string value should be restored", "Player1", variable.getValue());
	}

	@Test
	public void backpackScriptSoundsPerGroup() {
		List<SoundInfo> sounds1 = new ArrayList<>();
		sounds1.add(createSoundInfo("a.mp3"));
		List<SoundInfo> sounds2 = new ArrayList<>();
		sounds2.add(createSoundInfo("b.mp3"));

		backpack.backpackedScriptSounds.put("group1", sounds1);
		backpack.backpackedScriptSounds.put("group2", sounds2);

		assertEquals("Group 1 should have 1 sound", 1, backpack.backpackedScriptSounds.get("group1").size());
		assertEquals("Group 2 should have 1 sound", 1, backpack.backpackedScriptSounds.get("group2").size());
		assertEquals("Group 1 sound should be a.mp3", "a.mp3", backpack.backpackedScriptSounds.get("group1").get(0).name);
	}

	@Test
	public void backpackVariableValuesPerGroup() {
		HashMap<String, String> values1 = new HashMap<>();
		values1.put("x", "10");
		HashMap<String, String> values2 = new HashMap<>();
		values2.put("y", "20");

		backpack.backpackedVariableValues.put("group1", values1);
		backpack.backpackedVariableValues.put("group2", values2);

		assertEquals("Group 1 should have x=10", "10", backpack.backpackedVariableValues.get("group1").get("x"));
		assertEquals("Group 2 should have y=20", "20", backpack.backpackedVariableValues.get("group2").get("y"));
	}

	@Test
	public void backpackListValuesPerGroup() {
		HashMap<String, String> values1 = new HashMap<>();
		values1.put("list1", "a,b,c");

		backpack.backpackedListValues.put("group1", values1);

		assertEquals("List should be CSV", "a,b,c", backpack.backpackedListValues.get("group1").get("list1"));
	}

	@Test
	public void removeItemRemovesAllData() {
		backpack.backpackedScripts.put("group1", new ArrayList<>());
		backpack.backpackedScriptSounds.put("group1", new ArrayList<>());
		backpack.backpackedVariableValues.put("group1", new HashMap<>());
		backpack.backpackedListValues.put("group1", new HashMap<>());
		backpack.backpackedUserVariables.put("group1", new HashMap<>());
		backpack.backpackedUserLists.put("group1", new HashMap<>());

		backpackListManager.removeItemFromScriptBackPack("group1");

		assertFalse("Scripts should be removed", backpack.backpackedScripts.containsKey("group1"));
		assertFalse("Script sounds should be removed", backpack.backpackedScriptSounds.containsKey("group1"));
		assertFalse("Variable values should be removed", backpack.backpackedVariableValues.containsKey("group1"));
		assertFalse("List values should be removed", backpack.backpackedListValues.containsKey("group1"));
		assertFalse("User variables should be removed", backpack.backpackedUserVariables.containsKey("group1"));
		assertFalse("User lists should be removed", backpack.backpackedUserLists.containsKey("group1"));
	}

	@Test
	public void backpackEmptyWithOnlyScriptSounds() {
		backpack.backpackedScriptSounds.put("group1", new ArrayList<>());
		backpack.backpackedScripts.put("group1", new ArrayList<>());

		assertFalse("Backpack with script sounds should not be empty", backpack.backpackedScripts.isEmpty());
	}

	@Test
	public void backpackNewFieldsInitialized() {
		assertNotNull("backpackedScriptSounds should be initialized", backpack.backpackedScriptSounds);
		assertNotNull("backpackedVariableValues should be initialized", backpack.backpackedVariableValues);
		assertNotNull("backpackedListValues should be initialized", backpack.backpackedListValues);
		assertTrue("backpackedScriptSounds should be empty", backpack.backpackedScriptSounds.isEmpty());
		assertTrue("backpackedVariableValues should be empty", backpack.backpackedVariableValues.isEmpty());
		assertTrue("backpackedListValues should be empty", backpack.backpackedListValues.isEmpty());
	}

	@Test
	public void packWithMaximalValues() throws Exception {
		StartScript script = new StartScript();
		UserVariable var1 = new UserVariable("var1", Double.MAX_VALUE);
		sprite.addUserVariable(var1);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertTrue("Max value should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
	}

	@Test
	public void packWithMinimalValues() throws Exception {
		StartScript script = new StartScript();
		UserVariable var1 = new UserVariable("var1", Double.MIN_VALUE);
		sprite.addUserVariable(var1);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertTrue("Min value should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
	}

	@Test
	public void packWithLongStringValue() throws Exception {
		StartScript script = new StartScript();
		UserVariable var1 = new UserVariable("long", "This is a very long string value that should be preserved correctly");
		sprite.addUserVariable(var1);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertTrue("Long string value should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
		assertEquals("Long string should be preserved",
			"This is a very long string value that should be preserved correctly",
			backpack.backpackedVariableValues.get("testGroup").get("long"));
	}

	@Test
	public void packMultipleVariablesInSameScript() throws Exception {
		StartScript script = new StartScript();
		UserVariable var1 = new UserVariable("a", 1.0);
		UserVariable var2 = new UserVariable("b", 2.0);
		UserVariable var3 = new UserVariable("c", 3.0);
		sprite.addUserVariable(var1);
		sprite.addUserVariable(var2);
		sprite.addUserVariable(var3);

		SetVariableBrick brick1 = new SetVariableBrick(0);
		SetVariableBrick brick2 = new SetVariableBrick(0);
		SetVariableBrick brick3 = new SetVariableBrick(0);
		script.addBrick(brick1);
		script.addBrick(brick2);
		script.addBrick(brick3);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertEquals("Should have 3 variable values", 3, backpack.backpackedVariableValues.get("testGroup").size());
	}

	@Test
	public void packWithBooleanTrueValue() throws Exception {
		StartScript script = new StartScript();
		UserVariable var1 = new UserVariable("flag", true);
		sprite.addUserVariable(var1);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertEquals("Boolean true should be packed as string", "true", backpack.backpackedVariableValues.get("testGroup").get("flag"));
	}

	@Test
	public void packWithBooleanFalseValue() throws Exception {
		StartScript script = new StartScript();
		UserVariable var1 = new UserVariable("flag", false);
		sprite.addUserVariable(var1);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertEquals("Boolean false should be packed as string", "false", backpack.backpackedVariableValues.get("testGroup").get("flag"));
	}

	@Test
	public void packSoundAndValueIndependently() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound = createSoundInfo("test.mp3");
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		UserVariable var = new UserVariable("v", 5.0);
		sprite.addUserVariable(var);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, false);

		assertTrue("Sounds should be packed", backpack.backpackedScriptSounds.containsKey("testGroup"));
		assertFalse("Values should NOT be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
	}

	@Test
	public void packValueAndSoundIndependently() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound = createSoundInfo("test.mp3");
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		UserVariable var = new UserVariable("v", 5.0);
		sprite.addUserVariable(var);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertFalse("Sounds should NOT be packed", backpack.backpackedScriptSounds.containsKey("testGroup"));
		assertTrue("Values should be packed", backpack.backpackedVariableValues.containsKey("testGroup"));
	}

	@Test
	public void packTwoGroupsIndependently() throws Exception {
		StartScript script1 = new StartScript();
		SoundInfo sound1 = createSoundInfo("group1_sound.mp3");
		PlaySoundBrick brick1 = new PlaySoundBrick();
		brick1.setSound(sound1);
		script1.addBrick(brick1);

		StartScript script2 = new StartScript();
		SoundInfo sound2 = createSoundInfo("group2_sound.mp3");
		PlaySoundBrick brick2 = new PlaySoundBrick();
		brick2.setSound(sound2);
		script2.addBrick(brick2);

		scriptController.pack("group1", flattenScript(script1), true, false);
		scriptController.pack("group2", flattenScript(script2), true, false);

		assertEquals("Group 1 should have its sound", "group1_sound.mp3", backpack.backpackedScriptSounds.get("group1").get(0).name);
		assertEquals("Group 2 should have its sound", "group2_sound.mp3", backpack.backpackedScriptSounds.get("group2").get(0).name);
	}

	@Test
	public void unpackDoesNotModifyOtherGroups() throws Exception {
		List<SoundInfo> sounds1 = new ArrayList<>();
		sounds1.add(createSoundInfo("existing.mp3"));
		backpack.backpackedScriptSounds.put("group1", sounds1);

		StartScript script = new StartScript();
		scriptController.unpack("group2", script, sprite);

		assertEquals("Group 1 sounds should remain", 1, backpack.backpackedScriptSounds.get("group1").size());
	}

	@Test
	public void packWithUnicodeSoundName() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound = createSoundInfo("звук.mp3");
		PlaySoundBrick soundBrick = new PlaySoundBrick();
		soundBrick.setSound(sound);
		script.addBrick(soundBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, false);

		assertEquals("Unicode sound name should be preserved", "звук.mp3", backpack.backpackedScriptSounds.get("testGroup").get(0).name);
	}

	@Test
	public void packPreservesSoundOrder() throws Exception {
		StartScript script = new StartScript();
		SoundInfo sound1 = createSoundInfo("first.mp3");
		SoundInfo sound2 = createSoundInfo("second.mp3");
		SoundInfo sound3 = createSoundInfo("third.mp3");

		PlaySoundBrick brick1 = new PlaySoundBrick();
		brick1.setSound(sound1);
		PlaySoundBrick brick2 = new PlaySoundBrick();
		brick2.setSound(sound2);
		PlaySoundBrick brick3 = new PlaySoundBrick();
		brick3.setSound(sound3);

		script.addBrick(brick1);
		script.addBrick(brick2);
		script.addBrick(brick3);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, true, false);

		assertEquals("First sound should be first.mp3", "first.mp3", backpack.backpackedScriptSounds.get("testGroup").get(0).name);
		assertEquals("Second sound should be second.mp3", "second.mp3", backpack.backpackedScriptSounds.get("testGroup").get(1).name);
		assertEquals("Third sound should be third.mp3", "third.mp3", backpack.backpackedScriptSounds.get("testGroup").get(2).name);
	}

	@Test
	public void unpackWithNoBricksInScript() throws Exception {
		StartScript script = new StartScript();
		scriptController.unpack("testGroup", script, sprite);
		assertEquals("Script should be added to sprite", 1, sprite.getScriptList().size());
	}

	@Test
	public void packGettersReturnCorrectMapReferences() {
		List<SoundInfo> sounds = backpackListManager.getBackpackedScriptSounds().get("test");
		assertNull("Non-existent group should return null", sounds);

		backpackListManager.getBackpackedScriptSounds().put("test", new ArrayList<>());
		assertNotNull("After put, getter should return list", backpackListManager.getBackpackedScriptSounds().get("test"));
	}

	@Test
	public void backpackSerializedFieldsAreSerializable() {
		backpack.backpackedScriptSounds.put("g1", new ArrayList<>());
		backpack.backpackedVariableValues.put("g1", new HashMap<>());
		backpack.backpackedListValues.put("g1", new HashMap<>());

		assertTrue("backpackedScriptSounds should be HashMap", backpack.backpackedScriptSounds instanceof HashMap);
		assertTrue("backpackedVariableValues should be HashMap", backpack.backpackedVariableValues instanceof HashMap);
		assertTrue("backpackedListValues should be HashMap", backpack.backpackedListValues instanceof HashMap);
	}

	@Test
	public void packWithEmptyStringValue() throws Exception {
		StartScript script = new StartScript();
		UserVariable var = new UserVariable("empty_str", "");
		sprite.addUserVariable(var);
		SetVariableBrick varBrick = new SetVariableBrick(0);
		script.addBrick(varBrick);

		List<Brick> bricks = flattenScript(script);

		scriptController.pack("testGroup", bricks, false, true);

		assertEquals("Empty string should be preserved", "", backpack.backpackedVariableValues.get("testGroup").get("empty_str"));
	}

	private SoundInfo createSoundInfo(String name) {
		File mockFile = Mockito.mock(File.class);
		Mockito.when(mockFile.exists()).thenReturn(true);
		Mockito.when(mockFile.getName()).thenReturn(name);
		return new SoundInfo(name, mockFile);
	}

	private List<Brick> flattenScript(Script script) {
		List<Brick> bricks = new ArrayList<>();
		bricks.add(script.getScriptBrick());
		bricks.addAll(script.getBrickList());
		return bricks;
	}

}
