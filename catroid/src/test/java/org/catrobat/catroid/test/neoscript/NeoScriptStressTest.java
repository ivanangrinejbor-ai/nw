package org.catrobat.catroid.test.neoscript;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
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

@RunWith(JUnit4.class)
public class NeoScriptStressTest {

	private Project project;
	private Sprite sprite;

	@Before
	public void setUp() {
		project = new Project(MockUtil.mockContextForProject(), "NeoScriptStressProject");
		sprite = new Sprite("StressTarget");
		project.getDefaultScene().addSprite(sprite);
	}

	@Test
	public void testSixtyStartScriptsStress() throws Exception {
		NeoScriptFile file = new NeoScriptFile();
		for (int s = 0; s < 60; s++) {
			StartScript startScript = new StartScript();
			for (int b = 0; b < 10; b++) {
				UserVariable variable = new UserVariable("var_" + s + "_" + b);
				startScript.addBrick(new SetVariableBrick(new Formula(b), variable));
			}
			file.getScripts().add(startScript);
		}

		String xml = NeoScriptSerializer.serializeToString(file);
		assertNotNull(xml);
		
		NeoScriptFile restored = NeoScriptSerializer.deserializeFromString(xml);
		assertEquals(60, restored.getScripts().size());

		for (int s = 0; s < 60; s++) {
			Script script = restored.getScripts().get(s);
			assertEquals(10, script.getBrickList().size());
			for (int b = 0; b < 10; b++) {
				Brick brick = script.getBrickList().get(b);
				assertTrue(brick instanceof SetVariableBrick);
				SetVariableBrick setVar = (SetVariableBrick) brick;
				assertEquals("var_" + s + "_" + b, setVar.getUserVariable().getName());
			}
		}

		NeoScriptImporter.ImportResult appendResult =
				NeoScriptImporter.importScripts(restored, project, sprite, NeoScriptImporter.ImportStrategy.APPEND_ALL);
		assertEquals(60, appendResult.added.size());
		assertEquals(60, sprite.getScriptList().size());

		for (int s = 0; s < 60; s++) {
			Script script = sprite.getScriptList().get(s);
			assertEquals(10, script.getBrickList().size());
			for (int b = 0; b < 10; b++) {
				Brick brick = script.getBrickList().get(b);
				SetVariableBrick setVar = (SetVariableBrick) brick;
				assertEquals("var_" + s + "_" + b, setVar.getUserVariable().getName());
			}
		}
	}

	@Test
	public void testSixtyStartScriptsWithSkipDuplicates() throws Exception {
		NeoScriptFile file = new NeoScriptFile();
		for (int s = 0; s < 60; s++) {
			StartScript startScript = new StartScript();
			for (int b = 0; b < 10; b++) {
				UserVariable variable = new UserVariable("energy_" + s + "_" + b);
				startScript.addBrick(new SetVariableBrick(new Formula(b), variable));
			}
			file.getScripts().add(startScript);
		}

		NeoScriptImporter.ImportResult skipResult =
				NeoScriptImporter.importScripts(file, project, sprite, NeoScriptImporter.ImportStrategy.SKIP_DUPLICATES);
		
		assertEquals(60, skipResult.added.size());
		assertEquals(0, skipResult.skipped.size());
		assertEquals(60, sprite.getScriptList().size());
	}

	@Test
	public void testSixtyStartScriptsWithReplaceDuplicates() throws Exception {
		StartScript existing = new StartScript();
		existing.addBrick(new SetVariableBrick(new Formula(99), new UserVariable("old")));
		sprite.addScript(existing);

		NeoScriptFile file = new NeoScriptFile();
		for (int s = 0; s < 60; s++) {
			StartScript startScript = new StartScript();
			for (int b = 0; b < 10; b++) {
				UserVariable variable = new UserVariable("new_" + s + "_" + b);
				startScript.addBrick(new SetVariableBrick(new Formula(b), variable));
			}
			file.getScripts().add(startScript);
		}

		NeoScriptImporter.ImportResult replaceResult =
				NeoScriptImporter.importScripts(file, project, sprite, NeoScriptImporter.ImportStrategy.REPLACE_DUPLICATES);

		assertEquals(60, replaceResult.added.size());
		assertEquals(60, replaceResult.replaced.size());
		assertEquals(60, sprite.getScriptList().size());

		for (Script s : sprite.getScriptList()) {
			for (Brick b : s.getBrickList()) {
				SetVariableBrick setVar = (SetVariableBrick) b;
				assertTrue(!setVar.getUserVariable().getName().equals("old"));
			}
		}
	}
}
