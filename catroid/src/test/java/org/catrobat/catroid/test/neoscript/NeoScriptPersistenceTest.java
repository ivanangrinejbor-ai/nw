package org.catrobat.catroid.test.neoscript;

import com.thoughtworks.xstream.XStream;

import org.catrobat.catroid.content.BroadcastScript;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.actions.AssignScriptsAction;
import org.catrobat.catroid.content.actions.CreateObjectAction;
import org.catrobat.catroid.content.bricks.AssignScriptsBrick;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.CreateObjectBrick;
import org.catrobat.catroid.content.bricks.NoteBrick;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.content.bricks.UnknownBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.io.XstreamSerializer;
import org.catrobat.catroid.neoscript.NeoScriptFile;
import org.catrobat.catroid.neoscript.NeoScriptSerializer;
import org.catrobat.catroid.test.MockUtil;

import java.io.File;
import java.nio.file.Files;

import junit.framework.TestCase;

/**
 * Persistence tests for the NeoScript object/script bricks.
 *
 * Two axes are verified:
 *   1. Behavioural (runtime side of persistence): the action mutates the canonical
 *      {@link Project} (the model that Catroid later serialises to disk). Whether the
 *      flag is on or off, the object/script is added to the live project model.
 *   2. Flag serialisation: the user's "persist" choice round-trips through the very
 *      same XStream configuration used by {@code ProjectSaver}/{@code XstreamSerializer}
 *      when the project is written to disk. A missing field (older .neoscript / project
 *      files) deserialises to the safe runtime-only default.
 *
 * NOTE: A full Project save+reload (code.xml) cannot run in this offline unit harness
 * because Catroid's Project serialisation touches Android/device-only classes. The on-disk
 * write path itself is Catroid's standard, on-device {@code ProjectSaver.saveProjectAsync}
 * (exercised by the editor's normal save flow); the tests below verify the two pieces that
 * ARE observable offline: the canonical model mutation and the persistence flag serialisation.
 */
public class NeoScriptPersistenceTest extends TestCase {

	private File workDir;
	private File moduleFile;
	private Project project;
	private Sprite hero1;
	private StartScript heroScript;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		workDir = Files.createTempDirectory("neoscript_persist_").toFile();
		project = new Project(MockUtil.mockContextForProject(), "NeoScriptPersistenceTest");
		if (project.getSceneList().isEmpty()) {
			project.addScene(new Scene("Scene1", project));
		}
		Scene scene = project.getDefaultScene();
		hero1 = new Sprite("Hero1");
		heroScript = new StartScript();
		heroScript.addBrick(new SetVariableBrick(new Formula(1.0), new UserVariable("Hero")));
		hero1.addScript(heroScript);
		scene.getSpriteList().add(hero1);
		moduleFile = helperMakeModuleFile();
	}

	@Override
	protected void tearDown() throws Exception {
		if (workDir != null) {
			deleteRecursively(workDir);
		}
		super.tearDown();
	}

	// ======================= module builders =======================

	private File helperMakeModuleFile() throws Exception {
		NeoScriptFile file = new NeoScriptFile();
		StartScript s = new StartScript();
		s.addBrick(new SetVariableBrick(new Formula(7.0), new UserVariable("X")));
		file.getScripts().add(s);
		File f = new File(workDir, "module.neoscript");
		NeoScriptSerializer.serializeToFile(file, f);
		return f;
	}

	private File helperMakeLargeModuleFile(int count) throws Exception {
		NeoScriptFile file = new NeoScriptFile();
		for (int i = 0; i < count; i++) {
			BroadcastScript s = new BroadcastScript("msg" + i);
			s.addBrick(new SetVariableBrick(new Formula((double) i), new UserVariable("V" + i)));
			file.getScripts().add(s);
		}
		File f = new File(workDir, "large.neoscript");
		NeoScriptSerializer.serializeToFile(file, f);
		return f;
	}

	// ======================= action runners =======================

	private void runCreateObject(String name, String scene, boolean persist) {
		CreateObjectAction action = new CreateObjectAction();
		action.setScope(new Scope(project, hero1, null));
		action.setObjectName(new Formula(name));
		action.setTargetSceneName(scene);
		action.setPersist(persist);
		action.act(1.0f);
	}

	private void runAssignScripts(String path, String objectName, String scene, boolean replace, boolean save) {
		AssignScriptsAction action = new AssignScriptsAction();
		action.setScope(new Scope(project, hero1, null));
		action.setFilePath(new Formula(path));
		action.setObjectName(new Formula(objectName));
		action.setTargetSceneName(scene);
		action.setReplaceExistingScripts(replace);
		action.setSavePersistent(save);
		action.act(1.0f);
	}

	// ======================= helpers =======================

	private XStream xstream() {
		return XstreamSerializer.getInstance().getXstream();
	}

	private static void setIntField(Object obj, String name, int value) throws Exception {
		java.lang.reflect.Field f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private boolean containsSetVariableBrick(Sprite sprite) {
		for (Script s : sprite.getScriptList()) {
			for (Brick b : s.getBrickList()) {
				if (b instanceof SetVariableBrick) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean containsNoteBrick(Sprite sprite) {
		for (Script s : sprite.getScriptList()) {
			for (Brick b : s.getBrickList()) {
				if (b instanceof NoteBrick) {
					return true;
				}
			}
		}
		return false;
	}

	private void deleteRecursively(File dir) {
		File[] children = dir.listFiles();
		if (children != null) {
			for (File c : children) {
				if (c.isDirectory()) {
					deleteRecursively(c);
				} else {
					c.delete();
				}
			}
		}
		dir.delete();
	}

	// ======================= behavioural: canonical model mutation =======================

	public void testCreateObjectAddsSpriteToCanonicalProject() {
		runCreateObject("Enemy", null, false);
		assertNotNull(project.getDefaultScene().getSprite("Enemy"));
	}

	public void testCreateObjectPersistTrueStillMutatesCanonicalProject() {
		// The persist flag must not change runtime behaviour: the object is still added.
		runCreateObject("Enemy", null, true);
		assertNotNull(project.getDefaultScene().getSprite("Enemy"));
	}

	public void testCreateObjectTargetsSpecificScene() {
		project.addScene(new Scene("Scene2", project));
		runCreateObject("Enemy", "Scene2", false);
		assertNotNull(project.getSceneByName("Scene2").getSprite("Enemy"));
		assertNull(project.getDefaultScene().getSprite("Enemy"));
	}

	public void testAssignScriptsAddsScriptsToCanonicalProject() {
		int before = hero1.getScriptList().size();
		runAssignScripts(moduleFile.getAbsolutePath(), "Hero1", null, false, false);
		assertEquals(before + 1, hero1.getScriptList().size());
		assertTrue(containsSetVariableBrick(hero1));
	}

	public void testAssignScriptsPersistTrueStillMutatesCanonicalProject() {
		runAssignScripts(moduleFile.getAbsolutePath(), "Hero1", null, false, true);
		assertTrue(containsSetVariableBrick(hero1));
	}

	public void testAssignScriptsAppendKeepsExisting() {
		runAssignScripts(moduleFile.getAbsolutePath(), "Hero1", null, false, false);
		assertEquals(2, hero1.getScriptList().size());
	}

	public void testAssignScriptsReplaceRemovesExisting() {
		runAssignScripts(moduleFile.getAbsolutePath(), "Hero1", null, true, false);
		assertEquals(1, hero1.getScriptList().size());
	}

	public void testAssignScriptsTargetScene() {
		project.addScene(new Scene("Scene2", project));
		Sprite s2hero = new Sprite("S2Hero");
		s2hero.addScript(new StartScript());
		project.getSceneByName("Scene2").getSpriteList().add(s2hero);
		int beforeS2 = s2hero.getScriptList().size();

		runAssignScripts(moduleFile.getAbsolutePath(), "S2Hero", "Scene2", false, false);

		assertEquals(beforeS2 + 1, s2hero.getScriptList().size());
		assertTrue(containsSetVariableBrick(s2hero));
		// Hero1 in Scene1 is untouched by the Scene2 assignment.
		assertEquals(1, hero1.getScriptList().size());
	}

	public void testAssignScriptsUnknownBrickBecomesNote() throws Exception {
		NeoScriptFile file = new NeoScriptFile();
		StartScript s = new StartScript();
		s.addBrick(new UnknownBrick("com.example.ghost.MissingBrick"));
		file.getScripts().add(s);
		File unknownFile = new File(workDir, "unknown.neoscript");
		NeoScriptSerializer.serializeToFile(file, unknownFile);

		runAssignScripts(unknownFile.getAbsolutePath(), "Hero1", null, false, false);

		assertTrue(containsNoteBrick(hero1));
	}

	public void testLargeScriptImportAppendsAll() throws Exception {
		File large = helperMakeLargeModuleFile(50);
		int before = hero1.getScriptList().size();
		runAssignScripts(large.getAbsolutePath(), "Hero1", null, false, false);
		assertEquals(before + 50, hero1.getScriptList().size());
	}

	// ======================= persistence flag serialisation =======================

	public void testCreateObjectBrickDefaultNotPersistent() {
		assertFalse(new CreateObjectBrick().isPersistent());
	}

	public void testCreateObjectBrickPersistentRoundTrip() throws Exception {
		CreateObjectBrick b = new CreateObjectBrick();
		setIntField(b, "persistentSelection", 1);
		CreateObjectBrick roundTripped = (CreateObjectBrick) xstream().fromXML(xstream().toXML(b));
		assertTrue(roundTripped.isPersistent());
	}

	public void testCreateObjectBrickBackwardCompatMissingField() {
		String xml = xstream().toXML(new CreateObjectBrick());
		String stripped = xml.replaceAll("<persistentSelection>\\d+</persistentSelection>", "");
		CreateObjectBrick roundTripped = (CreateObjectBrick) xstream().fromXML(stripped);
		assertFalse(roundTripped.isPersistent());
	}

	public void testAssignScriptsBrickDefaultNotSavePersistent() {
		assertFalse(new AssignScriptsBrick().isSavePersistent());
	}

	public void testAssignScriptsBrickSavePersistentRoundTrip() throws Exception {
		AssignScriptsBrick b = new AssignScriptsBrick();
		setIntField(b, "savePersistentSelection", 1);
		AssignScriptsBrick roundTripped = (AssignScriptsBrick) xstream().fromXML(xstream().toXML(b));
		assertTrue(roundTripped.isSavePersistent());
	}

	public void testAssignScriptsBrickBackwardCompatMissingField() {
		String xml = xstream().toXML(new AssignScriptsBrick());
		String stripped = xml.replaceAll("<savePersistentSelection>\\d+</savePersistentSelection>", "");
		AssignScriptsBrick roundTripped = (AssignScriptsBrick) xstream().fromXML(stripped);
		assertFalse(roundTripped.isSavePersistent());
	}
}
