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
public class SetHeightActionTest {

	private static final float HEIGHT = 75.0f;
	private final Formula height = new Formula(HEIGHT);
	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		initializeStaticSingletonMethods();
		sprite = new Sprite("testSprite");
	}

	@Test
	public void testSetHeight() {
		assertEquals(1f, sprite.look.getScaleY());

		sprite.getActionFactory().createSetHeightAction(sprite, new SequenceAction(), height).act(1.0f);
		assertEquals(HEIGHT / 100, sprite.look.getScaleY());
	}

	@Test
	public void testSetHeightZero() {
		sprite.getActionFactory().createSetHeightAction(sprite, new SequenceAction(), new Formula(0)).act(1.0f);
		assertEquals(0f, sprite.look.getScaleY());
	}

	@Test(expected = NullPointerException.class)
	public void testNullSprite() {
		new Sprite(null);
	}
}
