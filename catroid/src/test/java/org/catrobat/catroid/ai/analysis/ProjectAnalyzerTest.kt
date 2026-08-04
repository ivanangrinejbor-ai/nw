package org.catrobat.catroid.ai.analysis

import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.StartScript
import org.catrobat.catroid.content.WhenScript
import org.catrobat.catroid.content.bricks.BroadcastBrick
import org.catrobat.catroid.content.bricks.MoveNStepsBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.test.MockUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectAnalyzerTest {

    private fun newProject(): Project {
        val project = Project(MockUtil.mockContextForProject(), "TestProject")
        project.getDefaultScene().name = "Scene 1"
        return project
    }

    private fun spriteWithScript(project: Project, script: org.catrobat.catroid.content.Script) {
        val scene = project.getDefaultScene()
        val sprite = org.catrobat.catroid.content.Sprite("Cat")
        sprite.addScript(script)
        scene.addSprite(sprite)
    }

    @Test
    fun unusedProjectVariable_reported() {
        val project = newProject()
        project.userVariables.add(UserVariable("score"))
        assertEquals(listOf("score"), ProjectAnalyzer.findUnusedVariables(project))
    }

    @Test
    fun usedProjectVariable_notReported() {
        val project = newProject()
        project.userVariables.add(UserVariable("score"))
        val varRef = Formula(FormulaElement(FormulaElement.ElementType.USER_VARIABLE, "score", null))
        spriteWithScript(project, StartScript().also { it.addBrick(MoveNStepsBrick(varRef)) })
        assertTrue(ProjectAnalyzer.findUnusedVariables(project).isEmpty())
    }

    @Test
    fun unusedSpriteVariable_reported() {
        val project = newProject()
        val scene = project.getDefaultScene()
        val sprite = org.catrobat.catroid.content.Sprite("Cat")
        sprite.addUserVariable(UserVariable("lives"))
        scene.addSprite(sprite)
        assertEquals(listOf("lives"), ProjectAnalyzer.findUnusedVariables(project))
    }

    @Test
    fun usedSpriteVariable_notReported() {
        val project = newProject()
        val scene = project.getDefaultScene()
        val sprite = org.catrobat.catroid.content.Sprite("Cat")
        sprite.addUserVariable(UserVariable("lives"))
        val varRef = Formula(FormulaElement(FormulaElement.ElementType.USER_VARIABLE, "lives", null))
        sprite.addScript(StartScript().also { it.addBrick(MoveNStepsBrick(varRef)) })
        scene.addSprite(sprite)
        assertTrue(ProjectAnalyzer.findUnusedVariables(project).isEmpty())
    }

    @Test
    fun unusedBroadcast_reported() {
        val project = newProject()
        project.broadcastMessageContainer.addBroadcastMessage("go")
        assertEquals(listOf("go"), ProjectAnalyzer.findUnusedBroadcasts(project))
    }

    @Test
    fun usedBroadcast_notReported() {
        val project = newProject()
        project.broadcastMessageContainer.addBroadcastMessage("go")
        spriteWithScript(project, StartScript().also { it.addBrick(BroadcastBrick("go")) })
        assertTrue(ProjectAnalyzer.findUnusedBroadcasts(project).isEmpty())
    }

    @Test
    fun partiallyUsedBroadcasts() {
        val project = newProject()
        project.broadcastMessageContainer.addBroadcastMessage("go")
        project.broadcastMessageContainer.addBroadcastMessage("stop")
        spriteWithScript(project, StartScript().also { it.addBrick(BroadcastBrick("go")) })
        assertEquals(listOf("stop"), ProjectAnalyzer.findUnusedBroadcasts(project))
    }

    @Test
    fun duplicatedWhenScripts_reported() {
        val project = newProject()
        val sprite = org.catrobat.catroid.content.Sprite("Cat")
        sprite.addScript(WhenScript().also { it.addBrick(MoveNStepsBrick(10.0)) })
        sprite.addScript(WhenScript().also { it.addBrick(MoveNStepsBrick(10.0)) })
        project.getDefaultScene().addSprite(sprite)
        val duplicates = ProjectAnalyzer.findDuplicatedScripts(project)
        assertEquals(1, duplicates.size)
        assertTrue(duplicates[0].contains("duplicate"))
    }

    @Test
    fun startScriptDuplicates_ignored() {
        val project = newProject()
        val sprite = org.catrobat.catroid.content.Sprite("Cat")
        sprite.addScript(StartScript().also { it.addBrick(MoveNStepsBrick(10.0)) })
        sprite.addScript(StartScript().also { it.addBrick(MoveNStepsBrick(10.0)) })
        project.getDefaultScene().addSprite(sprite)
        assertTrue(ProjectAnalyzer.findDuplicatedScripts(project).isEmpty())
    }

    @Test
    fun differentScripts_notDuplicates() {
        val project = newProject()
        val sprite = org.catrobat.catroid.content.Sprite("Cat")
        sprite.addScript(WhenScript().also { it.addBrick(MoveNStepsBrick(10.0)) })
        sprite.addScript(WhenScript().also { it.addBrick(org.catrobat.catroid.content.bricks.TurnLeftBrick(90.0)) })
        project.getDefaultScene().addSprite(sprite)
        assertTrue(ProjectAnalyzer.findDuplicatedScripts(project).isEmpty())
    }

    @Test
    fun identicalScriptsAcrossSprites_reported() {
        val project = newProject()
        val scene = project.getDefaultScene()
        val cat = org.catrobat.catroid.content.Sprite("Cat")
        cat.addScript(WhenScript().also { it.addBrick(MoveNStepsBrick(10.0)) })
        scene.addSprite(cat)
        val bird = org.catrobat.catroid.content.Sprite("Bird")
        bird.addScript(WhenScript().also { it.addBrick(MoveNStepsBrick(10.0)) })
        scene.addSprite(bird)
        assertEquals(1, ProjectAnalyzer.findDuplicatedScripts(project).size)
    }
}
