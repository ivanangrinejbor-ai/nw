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
public class SetWidthActionTest {

	private static final float WIDTH = 50.0f;
	private final Formula width = new Formula(WIDTH);
	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		initializeStaticSingletonMethods();
		sprite = new Sprite("testSprite");
	}

	@Test
	public void testSetWidth() {
		assertEquals(1f, sprite.look.getScaleX());

		sprite.getActionFactory().createSetWidthAction(sprite, new SequenceAction(), width).act(1.0f);
		assertEquals(WIDTH / 100, sprite.look.getScaleX());
	}

	@Test
	public void testSetWidthZero() {
		sprite.getActionFactory().createSetWidthAction(sprite, new SequenceAction(), new Formula(0)).act(1.0f);
		assertEquals(0f, sprite.look.getScaleX());
	}

	@Test(expected = NullPointerException.class)
	public void testNullSprite() {
		new Sprite(null);
	}
}
