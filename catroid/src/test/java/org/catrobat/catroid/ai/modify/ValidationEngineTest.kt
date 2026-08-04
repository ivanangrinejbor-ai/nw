package org.catrobat.catroid.ai.modify

import org.catrobat.catroid.ai.tool.ChangeType
import org.catrobat.catroid.ai.tool.ProjectChange
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.StartScript
import org.catrobat.catroid.test.MockUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationEngineTest {

    private fun newProject(name: String = "TestProject"): Project =
        Project(MockUtil.mockContextForProject(), name)

    private fun namedScene(project: Project): org.catrobat.catroid.content.Scene {
        val scene = project.getDefaultScene()
        scene.name = "Scene 1"
        return scene
    }

    private fun projectChange(type: ChangeType, data: Map<String, Any>): ProjectChange =
        ProjectChange(type, "test change", data)


    @Test
    fun emptyChanges_isValid() {
        val project = newProject()
        val result = ValidationEngine.validateChanges(project, emptyList())
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }


    @Test
    fun createObject_missingScene_fails() {
        val project = newProject()
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_OBJECT, mapOf("name" to "Cat")))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("'scene' is required") })
    }

    @Test
    fun createObject_sceneNotFound_fails() {
        val project = newProject()
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_OBJECT, mapOf("scene" to "NoSuchScene", "name" to "Cat")))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("scene 'NoSuchScene' not found") })
    }

    @Test
    fun createObject_missingName_fails() {
        val project = newProject()
        val sceneName = namedScene(project).name
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_OBJECT, mapOf("scene" to sceneName)))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("'name' is required") })
    }

    @Test
    fun createObject_duplicateName_fails() {
        val project = newProject()
        val scene = namedScene(project)
        scene.addSprite(org.catrobat.catroid.content.Sprite("Cat"))
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_OBJECT, mapOf("scene" to scene.name, "name" to "Cat")))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("already exists") })
    }

    @Test
    fun createObject_valid_succeeds() {
        val project = newProject()
        val scene = namedScene(project)
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_OBJECT, mapOf("scene" to scene.name, "name" to "NewCat")))
        )
        assertTrue(result.isValid)
    }


    @Test
    fun deleteObject_existing_succeeds() {
        val project = newProject()
        val scene = namedScene(project)
        scene.addSprite(org.catrobat.catroid.content.Sprite("Cat"))
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.DELETE_OBJECT, mapOf("scene" to scene.name, "name" to "Cat")))
        )
        assertTrue(result.isValid)
    }

    @Test
    fun deleteObject_missing_fails() {
        val project = newProject()
        val scene = namedScene(project)
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.DELETE_OBJECT, mapOf("scene" to scene.name, "name" to "Ghost")))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("object 'Ghost' not found") })
    }


    @Test
    fun createScene_duplicate_fails() {
        val project = newProject()
        val sceneName = namedScene(project).name
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_SCENE, mapOf("name" to sceneName)))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("already exists") })
    }

    @Test
    fun createScene_new_succeeds() {
        val project = newProject()
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_SCENE, mapOf("name" to "Level 2")))
        )
        assertTrue(result.isValid)
    }

    @Test
    fun deleteScene_onlyScene_fails() {
        val project = newProject()
        val sceneName = namedScene(project).name
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.DELETE_SCENE, mapOf("name" to sceneName)))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("cannot delete the only scene") })
    }


    @Test
    fun replaceScript_indexOutOfRange_fails() {
        val project = newProject()
        val scene = namedScene(project)
        scene.addSprite(org.catrobat.catroid.content.Sprite("Cat"))
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(
                ChangeType.REPLACE_SCRIPT,
                mapOf("scene" to scene.name, "object" to "Cat", "index" to 5)
            ))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("index 5 out of range") })
    }

    @Test
    fun replaceScript_withinRange_succeeds() {
        val project = newProject()
        val scene = namedScene(project)
        val sprite = org.catrobat.catroid.content.Sprite("Cat")
        sprite.addScript(StartScript())
        scene.addSprite(sprite)
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(
                ChangeType.REPLACE_SCRIPT,
                mapOf("scene" to scene.name, "object" to "Cat", "index" to 0)
            ))
        )
        assertTrue(result.isValid)
    }

    @Test
    fun appendScript_noIndexNeeded_succeeds() {
        val project = newProject()
        val scene = namedScene(project)
        scene.addSprite(org.catrobat.catroid.content.Sprite("Cat"))
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(
                ChangeType.APPEND_SCRIPT,
                mapOf("scene" to scene.name, "object" to "Cat")
            ))
        )
        assertTrue(result.isValid)
    }

    @Test
    fun deleteScript_missingObject_fails() {
        val project = newProject()
        val scene = namedScene(project)
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(
                ChangeType.DELETE_SCRIPT,
                mapOf("scene" to scene.name, "index" to 0)
            ))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("'object' is required") })
    }


    @Test
    fun createVariable_duplicateProjectScope_fails() {
        val project = newProject()
        project.addUserVariable(org.catrobat.catroid.formulaeditor.UserVariable("score"))
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_VARIABLE, mapOf("name" to "score")))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("already exists") })
    }

    @Test
    fun createVariable_localWithoutObject_fails() {
        val project = newProject()
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_VARIABLE, mapOf("name" to "lives", "scope" to "object")))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("'object' is required for a local variable") })
    }

    @Test
    fun createVariable_unknownScope_fails() {
        val project = newProject()
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.CREATE_VARIABLE, mapOf("name" to "lives", "scope" to "weird")))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("unknown scope") })
    }

    @Test
    fun deleteVariable_missing_fails() {
        val project = newProject()
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.DELETE_VARIABLE, mapOf("name" to "ghostVar")))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("not found") })
    }


    @Test
    fun modifyBrick_missingScriptIndex_fails() {
        val project = newProject()
        val scene = namedScene(project)
        scene.addSprite(org.catrobat.catroid.content.Sprite("Cat"))
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(
                ChangeType.MODIFY_BRICK,
                mapOf("scene" to scene.name, "object" to "Cat", "newType" to "WaitBrick")
            ))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("'scriptIndex' is required") })
    }


    @Test
    fun wireLocalizationSwitch_noLanguage_fails() {
        val project = newProject()
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.WIRE_LOCALIZATION_SWITCH, emptyMap()))
        )
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("'language' is required") })
    }

    @Test
    fun wireLocalizationSwitch_validLanguage_succeeds() {
        val project = newProject()
        val result = ValidationEngine.validateChanges(
            project,
            listOf(projectChange(ChangeType.WIRE_LOCALIZATION_SWITCH, mapOf("language" to "ru")))
        )
        assertTrue(result.isValid)
    }


    @Test
    fun multipleInvalidChanges_collectAllErrors() {
        val project = newProject()
        val sceneName = namedScene(project).name
        val result = ValidationEngine.validateChanges(
            project,
            listOf(
                projectChange(ChangeType.CREATE_OBJECT, mapOf("scene" to "MissingScene", "name" to "Cat")),
                projectChange(ChangeType.CREATE_SCENE, mapOf("name" to sceneName))
            )
        )
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
    }


    @Test
    fun integrity_emptySceneList_fails() {
        val project = newProject()
        project.sceneList.removeAt(0)
        val result = ValidationEngine.validateProjectIntegrity(project)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("no scenes") })
    }

    @Test
    fun integrity_scriptlessObject_warns() {
        val project = newProject()
        val scene = namedScene(project)
        scene.addSprite(org.catrobat.catroid.content.Sprite("IdleCat"))
        val result = ValidationEngine.validateProjectIntegrity(project)
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("IdleCat") })
    }
}
