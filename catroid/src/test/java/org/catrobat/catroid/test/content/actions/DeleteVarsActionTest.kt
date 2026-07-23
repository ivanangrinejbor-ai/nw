package org.catrobat.catroid.test.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.UserVarsManager
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.test.StaticSingletonInitializer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DeleteVarsActionTest {

	private lateinit var sprite: Sprite

	@Before
	fun setUp() {
		StaticSingletonInitializer.initializeStaticSingletonMethods()
		sprite = Sprite("testSprite")
	}

	@Test
	fun testDeleteVarsAction() {
		sprite.getActionFactory().createDeleteVarsAction(sprite, SequenceAction()).act(1.0f)
	}
}
