package org.catrobat.catroid.test.content.actions;

import java.lang.reflect.Field;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;

import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.FadeParticleEffectAction;
import org.catrobat.catroid.test.StaticSingletonInitializer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class FadeParticleEffectActionTest {

	@Before
	public void setUp() {
		StaticSingletonInitializer.initializeStaticSingletonMethods();
	}

	@Test
	public void fadeInInitializesParticleEffectForLookDraw() throws Exception {
		Sprite sprite = new Sprite("particleSprite");
		FadeParticleEffectAction action = new FadeParticleEffectAction();
		action.setSprite(sprite);
		action.setFadeIn(true);

		action.act(0f);

		Field particleEffectField = sprite.look.getClass().getDeclaredField("particleEffect");
		particleEffectField.setAccessible(true);
		assertNotNull((ParticleEffect) particleEffectField.get(sprite.look));
	}
}
