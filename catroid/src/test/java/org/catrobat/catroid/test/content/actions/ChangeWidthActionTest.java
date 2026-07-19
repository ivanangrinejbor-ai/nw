package org.catrobat.catroid.test.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertEquals;

import static org.catrobat.catroid.test.StaticSingletonInitializer.initializeStaticSingletonMethods;

@RunWith(JUnit4.class)
public class ChangeWidthActionTest {

	private static final float DELTA = 25.0f;
	private final Formula delta = new Formula(DELTA);
	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		initializeStaticSingletonMethods();
		sprite = new Sprite("testSprite");
	}

	@Test
	public void testChangeWidth() {
		assertEquals(1f, sprite.look.getScaleX());

		sprite.getActionFactory().createChangeWidthAction(sprite, new SequenceAction(), delta).act(1.0f);
		assertEquals(1f + DELTA / 100, sprite.look.getScaleX());
	}

	@Test
	public void testChangeWidthNegative() {
		sprite.getActionFactory().createChangeWidthAction(sprite, new SequenceAction(), new Formula(-10)).act(1.0f);
		assertEquals(0.9f, sprite.look.getScaleX());
	}

	@Test(expected = NullPointerException.class)
	public void testNullSprite() {
		new Sprite(null);
	}
}
