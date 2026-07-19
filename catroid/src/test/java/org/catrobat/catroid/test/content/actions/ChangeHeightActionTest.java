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
public class ChangeHeightActionTest {

	private static final float DELTA = 30.0f;
	private final Formula delta = new Formula(DELTA);
	private Sprite sprite;

	@Before
	public void setUp() throws Exception {
		initializeStaticSingletonMethods();
		sprite = new Sprite("testSprite");
	}

	@Test
	public void testChangeHeight() {
		assertEquals(1f, sprite.look.getScaleY());

		sprite.getActionFactory().createChangeHeightAction(sprite, new SequenceAction(), delta).act(1.0f);
		assertEquals(1f + DELTA / 100, sprite.look.getScaleY());
	}

	@Test
	public void testChangeHeightNegative() {
		sprite.getActionFactory().createChangeHeightAction(sprite, new SequenceAction(), new Formula(-10)).act(1.0f);
		assertEquals(0.9f, sprite.look.getScaleY());
	}

	@Test(expected = NullPointerException.class)
	public void testNullSprite() {
		new Sprite(null);
	}
}
