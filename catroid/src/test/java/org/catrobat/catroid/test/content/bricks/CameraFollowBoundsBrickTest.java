package org.catrobat.catroid.test.content.bricks;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.CameraBoundsBrick;
import org.catrobat.catroid.content.bricks.CameraFollowBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class CameraFollowBoundsBrickTest {

	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "Project");
		Scene scene = new Scene("Scene", project);
		sprite = new Sprite("TestSprite");
		scene.addSprite(sprite);
		project.addScene(scene);
		ProjectManager.getInstance().setCurrentProject(project);
	}

	@Test
	public void testCameraFollowDelegatesToActionFactory() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);

		CameraFollowBrick brick = new CameraFollowBrick("player", 30f);
		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		Mockito.verify(actionFactory).createCameraFollowAction(
				Mockito.eq(sprite),
				Mockito.any(ScriptSequenceAction.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class));
	}

	@Test
	public void testCameraBoundsDelegatesToActionFactory() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);

		CameraBoundsBrick brick = new CameraBoundsBrick(-100f, -200f, 300f, 400f);
		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		Mockito.verify(actionFactory).createCameraBoundsAction(
				Mockito.eq(sprite),
				Mockito.any(ScriptSequenceAction.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class));
	}
}
