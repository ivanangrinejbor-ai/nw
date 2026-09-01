package org.catrobat.catroid.test.content.bricks;

import com.badlogic.gdx.scenes.scene2d.Action;

import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.actions.ApplyLookColorProfileAction;
import org.catrobat.catroid.content.actions.CaptureLookColorProfileAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.ApplyLookColorProfileBrick;
import org.catrobat.catroid.content.bricks.CaptureLookColorProfileBrick;
import org.catrobat.catroid.test.StaticSingletonInitializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LookColorProfileBrickTest {

	@Before
	public void setUp() {
		StaticSingletonInitializer.initializeStaticSingletonMethods();
	}

	@Test
	public void captureBrickCreatesCaptureActionAndKeepsPhysicsState() {
		Sprite sprite = new Sprite("sprite");
		sprite.look.setX(42f);
		sprite.look.setY(-17f);
		sprite.look.setRotation(33f);
		sprite.ragdollMode = 2;
		ScriptSequenceAction sequence = new ScriptSequenceAction(Mockito.mock(Script.class));

		new CaptureLookColorProfileBrick().addActionToSequence(sprite, sequence);

		Action action = sequence.getActions().first();
		assertTrue(action instanceof CaptureLookColorProfileAction);
		assertEquals(42f, sprite.look.getX(), 0.001f);
		assertEquals(-17f, sprite.look.getY(), 0.001f);
		assertEquals(33f, sprite.look.getRotation(), 0.001f);
		assertEquals(2, sprite.ragdollMode);
	}

	@Test
	public void applyBrickCreatesApplyActionForSelectedLook() {
		Sprite sprite = new Sprite("sprite");
		ScriptSequenceAction sequence = new ScriptSequenceAction(Mockito.mock(Script.class));
		ApplyLookColorProfileBrick brick = new ApplyLookColorProfileBrick();
		LookData selectedLook = new LookData("target");
		brick.onItemSelected(0, selectedLook);

		brick.addActionToSequence(sprite, sequence);

		Action action = sequence.getActions().first();
		assertTrue(action instanceof ApplyLookColorProfileAction);
		assertSame(selectedLook, brick.getTargetLook());
	}
}
