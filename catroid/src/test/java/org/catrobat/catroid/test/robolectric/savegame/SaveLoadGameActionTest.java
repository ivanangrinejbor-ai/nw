package org.catrobat.catroid.test.robolectric.savegame;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.actions.LoadGameAction;
import org.catrobat.catroid.content.actions.SaveGameAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SaveLoadGameActionTest {

	private Project project;
	private Scene scene;
	private Sprite sprite;
	private UserVariable score;
	private UserList inventory;

	@Before
	public void setUp() throws Exception {
		project = new Project(MockUtil.mockContextForProject(), "SaveProject");
		scene = new Scene("Scene", project);
		sprite = new Sprite("Hero");
		score = new UserVariable("score", 42.0);
		inventory = new UserList("inventory", new ArrayList<>());
		inventory.addListItem("sword");
		sprite.addUserVariable(score);
		sprite.getUserLists().add(inventory);
		scene.addSprite(sprite);
		project.addScene(scene);
		ProjectManager.getInstance().setCurrentProject(project);
		ProjectManager.getInstance().setCurrentlyEditedScene(scene);
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);
	}

	private Scope scope() {
		Scope scope = Mockito.mock(Scope.class);
		Mockito.when(scope.getSprite()).thenReturn(sprite);
		return scope;
	}

	@Test
	public void testGameFileNameContainsSlot() {
		assertTrue(SaveGameAction.gameFile(1).getName().equals("savegame_slot1.json"));
		assertEquals(SaveGameAction.gameFile(-5).getName(), SaveGameAction.gameFile(1).getName());
		assertEquals(SaveGameAction.gameFile(500).getName(), SaveGameAction.gameFile(99).getName());
	}

	@Test
	public void testSaveThenLoadRestoresVariables() {
		SaveGameAction save = new SaveGameAction();
		save.setScope(scope());
		save.setSlot(new Formula(2));
		save.act(1f);

		score.setValue(1000.0);
		inventory.getValue().clear();

		LoadGameAction load = new LoadGameAction();
		load.setScope(scope());
		load.setSlot(new Formula(2));
		load.act(1f);

		assertEquals(42.0, ((Number) score.getValue()).doubleValue(), 0.0001);
		assertEquals(1, inventory.getValue().size());
		assertEquals("sword", inventory.getValue().get(0));
	}
}
