package org.catrobat.catroid.test.formulaeditor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserDataWrapper;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;

public class SceneUserVariableTest {

	private Project project;
	private Scene sceneA;
	private Scene sceneB;
	private Sprite spriteInA;
	private Sprite spriteInB;
	private UserVariable sceneVarA;
	private UserVariable sceneVarB;
	private UserVariable globalVar;
	private UserVariable spriteVar;

	@Before
	public void setUp() {
		project = new Project(MockUtil.mockContextForProject(), "test", false);
		sceneA = new Scene("SceneA", project);
		sceneB = new Scene("SceneB", project);
		project.getSceneList().add(sceneA);
		project.getSceneList().add(sceneB);

		spriteInA = new Sprite("SpriteA");
		spriteInB = new Sprite("SpriteB");
		sceneA.addSprite(spriteInA);
		sceneB.addSprite(spriteInB);

		globalVar = new UserVariable("global");
		project.addUserVariable(globalVar);
		spriteVar = new UserVariable("spriteVar");
		spriteInA.addUserVariable(spriteVar);

		sceneVarA = new UserVariable("sceneVar");
		sceneA.addSceneVariable(sceneVarA);
		sceneVarB = new UserVariable("sceneVar");
		sceneB.addSceneVariable(sceneVarB);
	}

	private Scope scopeFor(Sprite sprite) {
		return new Scope(project, sprite, null);
	}

	@Test
	public void testAddAndGetSceneVariable() {
		assertSame(sceneVarA, sceneA.getSceneVariable("sceneVar"));
		assertSame(sceneVarB, sceneB.getSceneVariable("sceneVar"));
		assertEquals(1, sceneA.getSceneVariables().size());
	}

	@Test
	public void testRemoveSceneVariable() {
		sceneA.removeSceneVariable("sceneVar");
		assertNull(sceneA.getSceneVariable("sceneVar"));
		assertTrue(sceneA.getSceneVariables().isEmpty());
		assertSame(sceneVarB, sceneB.getSceneVariable("sceneVar"));
	}

	@Test
	public void testResetSceneVariables() {
		sceneVarA.setValue(42.0);
		sceneVarB.setValue(42.0);
		sceneA.resetSceneVariables();
		assertEquals(0.0, sceneA.getSceneVariable("sceneVar").getValue());
		assertEquals(42.0, sceneVarB.getValue());
	}

	@Test
	public void testGetUserVariableSceneLookup() {
		assertSame(sceneVarA, UserDataWrapper.getUserVariable("sceneVar", scopeFor(spriteInA)));
		assertSame(sceneVarB, UserDataWrapper.getUserVariable("sceneVar", scopeFor(spriteInB)));
		assertSame(globalVar, UserDataWrapper.getUserVariable("global", scopeFor(spriteInA)));
		assertSame(spriteVar, UserDataWrapper.getUserVariable("spriteVar", scopeFor(spriteInA)));
		assertNull(UserDataWrapper.getUserVariable("spriteVar", scopeFor(spriteInB)));
		assertNull(UserDataWrapper.getUserVariable("unknown", scopeFor(spriteInA)));
	}

	@Test
	public void testGetUserListStillWorks() {
		UserList globalList = new UserList("globalList");
		project.addUserList(globalList);
		assertSame(globalList, UserDataWrapper.getUserList("globalList", scopeFor(spriteInA)));
	}

	@Test
	public void testSceneVariablesNotVisibleInOtherSceneForSpriteLookup() {
		UserVariable onlyInA = new UserVariable("onlyInA");
		sceneA.addSceneVariable(onlyInA);
		assertSame(onlyInA, UserDataWrapper.getUserVariable("onlyInA", scopeFor(spriteInA)));
		assertNull(UserDataWrapper.getUserVariable("onlyInA", scopeFor(spriteInB)));
	}

	@Test
	public void testSceneVariableHidesGlobalVariable() {
		UserVariable global = new UserVariable("shared");
		project.addUserVariable(global);
		UserVariable sceneOnly = new UserVariable("shared");
		sceneA.addSceneVariable(sceneOnly);
		assertSame(sceneOnly, UserDataWrapper.getUserVariable("shared", scopeFor(spriteInA)));
	}

	@Test
	public void testSceneVariableHidesMultiplayerFallback() {
		UserVariable multi = new UserVariable("sceneVar");
		project.addMultiplayerVariable(multi);
		assertSame(sceneVarA, UserDataWrapper.getUserVariable("sceneVar", scopeFor(spriteInA)));
		assertSame(sceneVarB, UserDataWrapper.getUserVariable("sceneVar", scopeFor(spriteInB)));
	}

	@Test
	public void testResetAllUserDataResetsSceneVariables() {
		sceneVarA.setValue(5.0);
		sceneVarB.setValue(7.0);
		globalVar.setValue(9.0);
		UserDataWrapper.resetAllUserData(project);
		assertEquals(0.0, sceneVarA.getValue());
		assertEquals(0.0, sceneVarB.getValue());
		assertEquals(0.0, globalVar.getValue());
	}
}
