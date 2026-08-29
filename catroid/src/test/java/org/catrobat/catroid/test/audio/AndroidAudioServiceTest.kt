package org.catrobat.catroid.test.audio

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.audio.AndroidAudioService
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.test.MockUtil
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

import org.catrobat.catroid.test.StaticSingletonInitializer

class AndroidAudioServiceTest {

    private lateinit var project: Project

    @Before
    fun setUp() {
        StaticSingletonInitializer.initializeStaticSingletonMethods()
        project = Project(MockUtil.mockContextForProject(), "testProject")
        val scene = Scene("Main", project)
        project.addScene(scene)
        ProjectManager.getInstance().setCurrentProject(project)
        ProjectManager.getInstance().setCurrentlyPlayingScene(scene)
    }

    @Test
    fun resolveSpriteFindsSpriteFromGlobalScene() {
        val globalScene = Scene("Global", project)
        val globalSprite = Sprite("GlobalSoundController")
        globalScene.addSprite(globalSprite)
        project.setGlobalScene(globalScene)

        assertSame(globalSprite, AndroidAudioService().resolveSprite("GlobalSoundController"))
    }
}
