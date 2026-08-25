package org.catrobat.catroid.test.content.bricks;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.JsonStore;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.actions.JsonGetAction;
import org.catrobat.catroid.content.actions.JsonSetAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.JsonGetBrick;
import org.catrobat.catroid.content.bricks.JsonSetBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class JsonGetSetBrickTest {

	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "Project");
		Scene scene = new Scene("Scene", project);
		sprite = new Sprite("TestSprite");
		scene.addSprite(sprite);
		project.addScene(scene);
		ProjectManager.getInstance().setCurrentProject(project);
		JsonStore.INSTANCE.clearAll();
	}

	@Test
	public void testJsonGetBrickDelegatesToActionFactory() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);

		UserVariable variable = new UserVariable("result");
		JsonGetBrick brick = new JsonGetBrick(new Formula("data"), new Formula("player.name"), variable);
		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		Mockito.verify(actionFactory).createJsonGetAction(
				Mockito.eq(sprite),
				Mockito.any(ScriptSequenceAction.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class),
				Mockito.eq(variable));
	}

	@Test
	public void testJsonSetBrickDelegatesToActionFactory() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);

		JsonSetBrick brick = new JsonSetBrick("data", "score", "100");
		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		Mockito.verify(actionFactory).createJsonSetAction(
				Mockito.eq(sprite),
				Mockito.any(ScriptSequenceAction.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class),
				Mockito.any(Formula.class));
	}

	@Test
	public void testJsonSetThenGetRoundTrip() {
		JsonSetAction setAction = new JsonSetAction();
		setAction.setScope(mockScope());
		setAction.setNameFormula(new Formula("data"));
		setAction.setKeyFormula(new Formula("score"));
		setAction.setValueFormula(new Formula("42"));
		setAction.act(1f);

		UserVariable result = new UserVariable("out");
		JsonGetAction getAction = new JsonGetAction();
		getAction.setScope(mockScope());
		getAction.setNameFormula(new Formula("data"));
		getAction.setKeyFormula(new Formula("score"));
		getAction.setUserVariable(result);
		getAction.act(1f);

		assertEquals(42.0, ((Number) result.getValue()).doubleValue(), 0.0001);
	}

	@Test
	public void testJsonGetStringValue() {
		org.catrobat.catroid.content.JsonStore.INSTANCE.parse("cfg", "{\"name\":\"Neo\"}");
		UserVariable result = new UserVariable("out");

		JsonGetAction getAction = new JsonGetAction();
		getAction.setScope(mockScope());
		getAction.setNameFormula(new Formula("cfg"));
		getAction.setKeyFormula(new Formula("name"));
		getAction.setUserVariable(result);
		getAction.act(1f);

		assertEquals("Neo", result.getValue());
	}

	@Test
	public void testJsonGetMissingReturnsEmpty() {
		UserVariable result = new UserVariable("out");
		result.setValue("INITIAL");

		JsonGetAction getAction = new JsonGetAction();
		getAction.setScope(mockScope());
		getAction.setNameFormula(new Formula("nope"));
		getAction.setKeyFormula(new Formula("key"));
		getAction.setUserVariable(result);
		getAction.act(1f);

		assertEquals("", result.getValue());
	}

	private Scope mockScope() {
		Scope scope = Mockito.mock(Scope.class);
		Mockito.when(scope.getSprite()).thenReturn(sprite);
		return scope;
	}
}
