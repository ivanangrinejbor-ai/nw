package org.catrobat.catroid.test.content.bricks;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.GlobalManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.SetGameTimeScaleAction;
import org.catrobat.catroid.content.actions.SetSeedAction;
import org.catrobat.catroid.content.bricks.SetGameTimeScaleBrick;
import org.catrobat.catroid.content.bricks.SetSeedBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class GameTimeAndSeedBrickTest {

	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "Project");
		Scene scene = new Scene("Scene", project);
		sprite = new Sprite("TestSprite");
		scene.addSprite(sprite);
		project.addScene(scene);
		ProjectManager.getInstance().setCurrentProject(project);
		GlobalManager.Companion.setGameTimeScale(1f);
		GlobalManager.Companion.clearRandomSeed();
	}

	@Test
	public void testTimeScaleBrickDelegatesToActionFactory() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);

		SetGameTimeScaleBrick brick = new SetGameTimeScaleBrick(50f);
		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		Mockito.verify(actionFactory).createSetGameTimeScaleAction(
				Mockito.eq(sprite),
				Mockito.any(ScriptSequenceAction.class),
				Mockito.any(Formula.class));
	}

	@Test
	public void testTimeScaleActionSetsGlobalManager() {
		SetGameTimeScaleAction action = new SetGameTimeScaleAction();
		action.setScope(mockScope());
		action.setScale(new Formula(25));

		action.act(1.0f);

		assertEquals(25f, GlobalManager.Companion.getGameTimeScale(), 0.001f);
	}

	@Test
	public void testTimeScaleNegativeClampedToZero() {
		SetGameTimeScaleAction action = new SetGameTimeScaleAction();
		action.setScope(mockScope());
		action.setScale(new Formula(-5));

		action.act(1.0f);

		assertEquals(0f, GlobalManager.Companion.getGameTimeScale(), 0.001f);
	}

	@Test
	public void testSeedBrickDelegatesToActionFactory() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);

		SetSeedBrick brick = new SetSeedBrick(42.0);
		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		Mockito.verify(actionFactory).createSetSeedAction(
				Mockito.eq(sprite),
				Mockito.any(ScriptSequenceAction.class),
				Mockito.any(Formula.class));
	}

	@Test
	public void testSeedActionSetsGlobalSeed() {
		SetSeedAction action = new SetSeedAction();
		action.setScope(mockScope());
		action.setSeed(new Formula(1234567));

		action.act(1.0f);

		assertEquals(Long.valueOf(1234567L), GlobalManager.Companion.getRandomSeed());
	}

	@Test
	public void testSeededRandomIsDeterministic() {
		GlobalManager.Companion.setRandomSeed(999L);
		double first = GlobalManager.Companion.nextRandom();
		GlobalManager.Companion.setRandomSeed(999L);
		double second = GlobalManager.Companion.nextRandom();

		assertEquals(first, second, 0.0000001);
	}

	@Test
	public void testUnseededRandomReturnsSomething() {
		GlobalManager.Companion.clearRandomSeed();
		double value = GlobalManager.Companion.nextRandom();
		org.junit.Assert.assertTrue(value >= 0.0 && value < 1.0);
	}

	private Scope mockScope() {
		Scope scope = Mockito.mock(Scope.class);
		Mockito.when(scope.getSprite()).thenReturn(sprite);
		return scope;
	}
}
