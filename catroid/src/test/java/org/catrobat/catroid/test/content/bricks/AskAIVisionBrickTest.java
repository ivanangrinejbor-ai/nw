package org.catrobat.catroid.test.content.bricks;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.AskAIVisionBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class AskAIVisionBrickTest {

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
	public void testBrickDelegatesToActionFactory() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);

		UserVariable variable = new UserVariable("answer");
		AskAIVisionBrick brick = new AskAIVisionBrick("What is this?");
		brick.setUserVariable(variable);
		LookData lookData = new LookData();
		lookData.setName("face");
		setSelectedLook(brick, lookData);

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		Mockito.verify(actionFactory).createAskAIVisionAction(
				Mockito.eq(sprite),
				Mockito.any(ScriptSequenceAction.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class),
				Mockito.anyString(),
				Mockito.eq(lookData),
				Mockito.eq(variable));
	}

	@Test
	public void testConstructorSetsPrompt() {
		AskAIVisionBrick brick = new AskAIVisionBrick("Describe this");
		org.junit.Assert.assertNotNull(brick.getFormulaWithBrickField(
				org.catrobat.catroid.content.bricks.Brick.BrickField.TEXT));
		org.junit.Assert.assertEquals("gemini", brick.getProviderSelection());
	}

	private void setSelectedLook(AskAIVisionBrick brick, LookData lookData) {
		try {
			java.lang.reflect.Field field = AskAIVisionBrick.class.getDeclaredField("lookData");
			field.setAccessible(true);
			field.set(brick, lookData);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
