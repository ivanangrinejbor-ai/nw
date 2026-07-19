package org.catrobat.catroid.ai.modify

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.ai.tool.ChangeType
import org.catrobat.catroid.ai.tool.ProjectChange
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite

object ProjectModifier {

    sealed class ModificationResult {
        data class Success(val message: String) : ModificationResult()
        data class Failure(val error: String) : ModificationResult()
    }

    fun applyChanges(changes: List<ProjectChange>): List<ModificationResult> {
        return changes.map { applyChange(it) }
    }

    private fun applyChange(change: ProjectChange): ModificationResult {
        val project = ProjectManager.getInstance().currentProject
            ?: return ModificationResult.Failure("No project open")

        return try {
            when (change.type) {
                ChangeType.CREATE_OBJECT -> createObject(project, change)
                ChangeType.DELETE_OBJECT -> deleteObject(project, change)
                ChangeType.CREATE_SCENE -> createScene(project, change)
                ChangeType.DELETE_SCENE -> deleteScene(project, change)
                ChangeType.REPLACE_SCRIPT -> replaceScript(project, change)
                ChangeType.APPEND_SCRIPT -> appendScript(project, change)
                ChangeType.DELETE_SCRIPT -> deleteScript(project, change)
                ChangeType.CREATE_VARIABLE -> createVariable(project, change)
                ChangeType.DELETE_VARIABLE -> deleteVariable(project, change)
                ChangeType.CREATE_BROADCAST -> createBroadcast(project, change)
                ChangeType.MODIFY_BRICK -> modifyBrick(project, change)
            }
        } catch (e: Exception) {
            ModificationResult.Failure("Error applying ${change.type}: ${e.message}")
        }
    }

    private fun getScene(project: Project, change: ProjectChange): Scene? {
        val sceneName = change.data["scene"] as? String ?: return null
        return project.sceneList.find { it.name == sceneName }
    }

    private fun getSprite(project: Project, change: ProjectChange): Sprite? {
        val sceneName = change.data["scene"] as? String ?: return null
        val objectName = change.data["object"] as? String ?: return null
        val scene = project.sceneList.find { it.name == sceneName } ?: return null
        return scene.spriteList.find { it.name == objectName }
    }

    private fun createObject(project: Project, change: ProjectChange): ModificationResult {
        val scene = getScene(project, change) ?: return ModificationResult.Failure("Scene not found")
        val name = change.data["name"] as? String ?: return ModificationResult.Failure("Name required")
        if (scene.spriteList.any { it.name == name }) {
            return ModificationResult.Failure("Object '$name' already exists")
        }
        val sprite = Sprite(name)
        scene.addSprite(sprite)
        return ModificationResult.Success("Created object '$name'")
    }

    private fun deleteObject(project: Project, change: ProjectChange): ModificationResult {
        val scene = getScene(project, change) ?: return ModificationResult.Failure("Scene not found")
        val name = change.data["name"] as? String ?: return ModificationResult.Failure("Name required")
        val sprite = scene.spriteList.find { it.name == name }
            ?: return ModificationResult.Failure("Object '$name' not found")
        scene.spriteList.remove(sprite)
        return ModificationResult.Success("Deleted object '$name'")
    }

    private fun createScene(project: Project, change: ProjectChange): ModificationResult {
        val name = change.data["name"] as? String ?: return ModificationResult.Failure("Name required")
        if (project.sceneList.any { it.name == name }) {
            return ModificationResult.Failure("Scene '$name' already exists")
        }
        val scene = Scene()
        scene.name = name
        scene.setProject(project)
        project.sceneList.add(scene)
        return ModificationResult.Success("Created scene '$name'")
    }

    private fun deleteScene(project: Project, change: ProjectChange): ModificationResult {
        val name = change.data["name"] as? String ?: return ModificationResult.Failure("Name required")
        if (project.sceneList.size <= 1) return ModificationResult.Failure("Cannot delete the only scene")
        val scene = project.sceneList.find { it.name == name }
            ?: return ModificationResult.Failure("Scene '$name' not found")
        project.sceneList.remove(scene)
        return ModificationResult.Success("Deleted scene '$name'")
    }

    private fun replaceScript(project: Project, change: ProjectChange): ModificationResult {
        val sprite = getSprite(project, change) ?: return ModificationResult.Failure("Object not found")
        val index = (change.data["index"] as? Number)?.toInt()
            ?: return ModificationResult.Failure("Script index required")
        if (index < 0 || index >= sprite.scriptList.size) {
            return ModificationResult.Failure("Script index $index out of range")
        }
        return ModificationResult.Success("Script $index marked for replacement - needs brick construction")
    }

    private fun appendScript(project: Project, change: ProjectChange): ModificationResult {
        val sprite = getSprite(project, change) ?: return ModificationResult.Failure("Object not found")
        return ModificationResult.Success("Script append requested - needs brick construction")
    }

    private fun deleteScript(project: Project, change: ProjectChange): ModificationResult {
        val sprite = getSprite(project, change) ?: return ModificationResult.Failure("Object not found")
        val index = (change.data["index"] as? Number)?.toInt()
            ?: return ModificationResult.Failure("Script index required")
        if (index < 0 || index >= sprite.scriptList.size) {
            return ModificationResult.Failure("Script index $index out of range")
        }
        sprite.scriptList.removeAt(index)
        return ModificationResult.Success("Deleted script $index")
    }

    private fun createVariable(project: Project, change: ProjectChange): ModificationResult {
        return ModificationResult.Success("Variable creation - needs integration with project variable system")
    }

    private fun deleteVariable(project: Project, change: ProjectChange): ModificationResult {
        return ModificationResult.Success("Variable deletion - needs integration with project variable system")
    }

    private fun createBroadcast(project: Project, change: ProjectChange): ModificationResult {
        return ModificationResult.Success("Broadcast creation - needs integration")
    }

    private fun modifyBrick(project: Project, change: ProjectChange): ModificationResult {
        return ModificationResult.Success("Brick modification - needs brick construction")
    }
}
