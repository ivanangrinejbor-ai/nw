package org.catrobat.catroid.test.content.bricks;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.CheckPortBrick;
import org.catrobat.catroid.content.bricks.GetFromPastebinBrick;
import org.catrobat.catroid.content.bricks.ListenTcpServerBrick;
import org.catrobat.catroid.content.bricks.SendToTcpServerBrick;
import org.catrobat.catroid.content.bricks.SetTcpServerClientLimitBrick;
import org.catrobat.catroid.content.bricks.SetTcpServerTimeoutBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class TcpNetworkBricksTest {

	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "Project");
		Scene currentlyPlayingScene = new Scene("Currently playing scene", project);
		sprite = new Sprite("Sprite");
		currentlyPlayingScene.addSprite(sprite);
		project.addScene(currentlyPlayingScene);
		ProjectManager.getInstance().setCurrentProject(project);
		ProjectManager.getInstance().setCurrentlyEditedScene(new Scene());
		ProjectManager.getInstance().setCurrentlyPlayingScene(currentlyPlayingScene);
	}

	@Test
	public void testGetFromPastebinBrickCreatesAction() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);
		GetFromPastebinBrick brick = new GetFromPastebinBrick("https://pastebin.com/raw/abc");
		brick.setUserVariable(new UserVariable("content"));

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		verify(actionFactory).createGetFromPastebinAction(eq(sprite),
				any(SequenceAction.class), any(Formula.class), any(UserVariable.class));
	}

	@Test
	public void testCheckPortBrickCreatesAction() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);
		CheckPortBrick brick = new CheckPortBrick("8888");
		brick.setUserVariable(new UserVariable("portInUse"));

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		verify(actionFactory).createCheckPortAction(eq(sprite),
				any(SequenceAction.class), any(Formula.class), any(UserVariable.class));
	}

	@Test
	public void testSetTcpServerClientLimitBrickCreatesAction() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);
		SetTcpServerClientLimitBrick brick = new SetTcpServerClientLimitBrick(10);

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		verify(actionFactory).createSetTcpServerClientLimitAction(eq(sprite),
				any(SequenceAction.class), any(Formula.class));
	}

	@Test
	public void testSetTcpServerTimeoutBrickCreatesAction() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);
		SetTcpServerTimeoutBrick brick = new SetTcpServerTimeoutBrick(30);

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		verify(actionFactory).createSetTcpServerTimeoutAction(eq(sprite),
				any(SequenceAction.class), any(Formula.class));
	}

	@Test
	public void testSendToTcpServerBrickCreatesAction() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);
		SendToTcpServerBrick brick = new SendToTcpServerBrick("okay");

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		verify(actionFactory).createSendToTcpServerAction(eq(sprite),
				any(SequenceAction.class), anyList());
	}

	@Test
	public void testListenTcpServerBrickCreatesAction() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);
		ListenTcpServerBrick brick = new ListenTcpServerBrick(new UserVariable("received"));

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		verify(actionFactory).createListenTcpServerAction(eq(sprite),
				any(SequenceAction.class), anyList());
	}

	@Test
	public void testSendToTcpServerVisibleFields() {
		SendToTcpServerBrick brick = new SendToTcpServerBrick("a");
		assertEquals(1, brick.getVisibleFields());
		brick.setVisibleFields(3);
		assertEquals(3, brick.getVisibleFields());
	}

	@Test
	public void testListenTcpServerVisibleVariables() {
		ListenTcpServerBrick brick = new ListenTcpServerBrick();
		assertEquals(1, brick.getVisibleVariables());
		brick.setVisibleVariables(2);
		assertEquals(2, brick.getVisibleVariables());
	}

	@Test
	public void testSendToTcpServerClone() throws CloneNotSupportedException {
		SendToTcpServerBrick brick = new SendToTcpServerBrick("okay");
		brick.setVisibleFields(4);
		SendToTcpServerBrick clone = (SendToTcpServerBrick) brick.clone();
		assertNotNull(clone);
		assertEquals(4, clone.getVisibleFields());
	}

	@Test
	public void testListenTcpServerClone() throws CloneNotSupportedException {
		ListenTcpServerBrick brick = new ListenTcpServerBrick(new UserVariable("received"));
		brick.setVisibleVariables(3);
		ListenTcpServerBrick clone = (ListenTcpServerBrick) brick.clone();
		assertNotNull(clone);
		assertEquals(3, clone.getVisibleVariables());
	}
}