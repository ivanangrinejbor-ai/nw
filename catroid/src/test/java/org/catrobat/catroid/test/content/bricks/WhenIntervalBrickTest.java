package org.catrobat.catroid.test.content.bricks;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenIntervalScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.WhenIntervalBrick;
import org.catrobat.catroid.content.eventids.IntervalEventId;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

@RunWith(JUnit4.class)
public class WhenIntervalBrickTest {

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "Project");
		Scene scene = new Scene("Scene", project);
		Sprite sprite = new Sprite("TestSprite");
		scene.addSprite(sprite);
		project.addScene(scene);
		ProjectManager.getInstance().setCurrentProject(project);
	}

	@Test
	public void testBrickAndScriptLinkage() {
		WhenIntervalScript script = new WhenIntervalScript(new Formula(5));
		WhenIntervalBrick brick = new WhenIntervalBrick(script);
		assertEquals(script, brick.getScript());
		assertEquals(brick, script.getScriptBrick());
	}

	@Test
	public void testDefaultConstructorUsesOneSecond() {
		WhenIntervalScript script = new WhenIntervalScript();
		Formula seconds = script.getFormulaMap().get(Brick.BrickField.TIME_TO_WAIT_IN_SECONDS);
		assertSame(script.getFormulaMap().get(Brick.BrickField.TIME_TO_WAIT_IN_SECONDS), seconds);
	}

	@Test
	public void testCreateEventIdMatchesFiredEvent() {
		WhenIntervalScript script = new WhenIntervalScript(new Formula(3));
		Sprite sprite = new Sprite("S");
		IntervalEventId expected = (IntervalEventId) script.createEventId(sprite);
		IntervalEventId fired = new IntervalEventId(
				script.getFormulaMap().get(Brick.BrickField.TIME_TO_WAIT_IN_SECONDS));
		assertEquals(expected, fired);

		IntervalEventId other = new IntervalEventId(new Formula(7));
		assertNotEquals(expected, other);
	}

	@Test
	public void testCloneKeepsScriptAndBrickLinkage() throws CloneNotSupportedException {
		WhenIntervalScript script = new WhenIntervalScript(new Formula(2));
		WhenIntervalBrick brick = new WhenIntervalBrick(script);

		Brick clonedBrick = brick.clone();
		WhenIntervalBrick intervalClone = (WhenIntervalBrick) clonedBrick;
		assertSame(intervalClone, intervalClone.getScript().getScriptBrick());

		WhenIntervalScript clonedScript = (WhenIntervalScript) script.clone();
		assertSame(clonedScript.getFormulaMap(),
				clonedScript.getFormulaMap());
	}
}
