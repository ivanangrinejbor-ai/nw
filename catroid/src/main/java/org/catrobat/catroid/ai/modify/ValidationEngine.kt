package org.catrobat.catroid.ai.modify

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.ai.tool.ChangeType
import org.catrobat.catroid.ai.tool.ProjectChange
import org.catrobat.catroid.content.Project

object ValidationEngine {

    data class ValidationResult(
        val isValid: Boolean,
        val warnings: List<String>,
        val errors: List<String>
    )

    fun validateChanges(project: Project, changes: List<ProjectChange>): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        for (change in changes) {
            val result = validateChange(project, change)
            errors.addAll(result.errors)
            warnings.addAll(result.warnings)
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            warnings = warnings,
            errors = errors
        )
    }

    private fun validateChange(project: Project, change: ProjectChange): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val data = change.data
        val sceneName = data["scene"] as? String
        val objectName = data["object"] as? String
        val name = (data["name"] as? String)?.trim().orEmpty()
        val scene = sceneName?.let { project.sceneList.find { s -> s.name == it } }
        val sprite = if (scene != null && objectName != null) {
            scene.spriteList.find { it.name == objectName }
        } else null

        when (change.type) {
            ChangeType.CREATE_OBJECT -> {
                if (sceneName.isNullOrBlank()) errors.add("createObject: 'scene' is required")
                else if (scene == null) errors.add("createObject: scene '$sceneName' not found")
                else if (name.isEmpty()) errors.add("createObject: 'name' is required")
                else if (scene.spriteList.any { it.name == name }) errors.add("createObject: object '$name' already exists in '$sceneName'")
            }
            ChangeType.DELETE_OBJECT -> {
                if (sceneName.isNullOrBlank()) errors.add("deleteObject: 'scene' is required")
                else if (scene == null) errors.add("deleteObject: scene '$sceneName' not found")
                else if (name.isEmpty()) errors.add("deleteObject: 'name' is required")
                else if (scene.spriteList.none { it.name == name }) errors.add("deleteObject: object '$name' not found in '$sceneName'")
            }
            ChangeType.CREATE_SCENE -> {
                if (name.isEmpty()) errors.add("createScene: 'name' is required")
                else if (project.sceneList.any { it.name == name }) errors.add("createScene: scene '$name' already exists")
            }
            ChangeType.DELETE_SCENE -> {
                if (name.isEmpty()) errors.add("deleteScene: 'name' is required")
                else if (project.sceneList.none { it.name == name }) errors.add("deleteScene: scene '$name' not found")
                else if (project.sceneList.size <= 1) errors.add("deleteScene: cannot delete the only scene")
            }
            ChangeType.REPLACE_SCRIPT, ChangeType.APPEND_SCRIPT, ChangeType.DELETE_SCRIPT -> {
                if (sceneName.isNullOrBlank()) errors.add("${change.type}: 'scene' is required")
                else if (scene == null) errors.add("${change.type}: scene '$sceneName' not found")
                else if (objectName.isNullOrBlank()) errors.add("${change.type}: 'object' is required")
                else if (sprite == null) errors.add("${change.type}: object '$objectName' not found in '$sceneName'")
                if (change.type != ChangeType.APPEND_SCRIPT && sprite != null) {
                    val index = (data["index"] as? Number)?.toInt()
                    if (index == null) errors.add("${change.type}: 'index' is required")
                    else if (index < 0 || index >= sprite.scriptList.size) {
                        errors.add("${change.type}: script index $index out of range (0..${sprite.scriptList.size - 1})")
                    }
                }
            }
            ChangeType.CREATE_VARIABLE -> {
                if (name.isEmpty()) errors.add("createVariable: 'name' is required")
                val scope = (data["scope"] as? String)?.lowercase() ?: "project"
                if (scope !in setOf("project", "global", "multiplayer", "object", "local")) {
                    errors.add("createVariable: unknown scope '$scope' (use project|multiplayer|object)")
                } else {
                    val exists = when (scope) {
                        "project", "global" -> project.userVariables.any { it.name == name }
                        "multiplayer" -> project.multiplayerVariables.any { it.name == name }
                        else -> sprite != null && sprite.userVariables.any { it.name == name }
                    }
                    if (exists) errors.add("createVariable: variable '$name' already exists in scope '$scope'")
                    if (scope in setOf("object", "local") && sprite == null) {
                        errors.add("createVariable: 'object' is required for a local variable")
                    }
                }
            }
            ChangeType.DELETE_VARIABLE -> {
                if (name.isEmpty()) errors.add("deleteVariable: 'name' is required")
                val scope = (data["scope"] as? String)?.lowercase() ?: "project"
                val exists = when (scope) {
                    "project", "global" -> project.userVariables.any { it.name == name }
                    "multiplayer" -> project.multiplayerVariables.any { it.name == name }
                    else -> sprite != null && sprite.userVariables.any { it.name == name }
                }
                if (!exists) errors.add("deleteVariable: variable '$name' not found in scope '$scope'")
            }
            ChangeType.MODIFY_BRICK -> {
                if (sceneName.isNullOrBlank()) errors.add("modifyBrick: 'scene' is required")
                else if (scene == null) errors.add("modifyBrick: scene '$sceneName' not found")
                else if (objectName.isNullOrBlank()) errors.add("modifyBrick: 'object' is required")
                else if (sprite == null) errors.add("modifyBrick: object '$objectName' not found in '$sceneName'")
                if (sprite != null) {
                    val scriptIndex = (data["scriptIndex"] as? Number)?.toInt()
                    val brickIndex = (data["brickIndex"] as? Number)?.toInt()
                    if (scriptIndex == null) errors.add("modifyBrick: 'scriptIndex' is required")
                    else if (scriptIndex < 0 || scriptIndex >= sprite.scriptList.size) {
                        errors.add("modifyBrick: script index $scriptIndex out of range")
                    } else if (brickIndex == null) {
                        errors.add("modifyBrick: 'brickIndex' is required")
                    } else {
                        val bricks = sprite.scriptList[scriptIndex].getBrickList()
                        if (brickIndex < 0 || brickIndex >= bricks.size) {
                            errors.add("modifyBrick: brick index $brickIndex out of range (0..${bricks.size - 1})")
                        }
                    }
                    if ((data["newType"] as? String).isNullOrBlank()) {
                        errors.add("modifyBrick: 'newType' is required")
                    }
                }
            }
            ChangeType.CREATE_BROADCAST -> {
                if (name.isEmpty()) errors.add("createBroadcast: 'name' is required")
                else if (project.broadcastMessageContainer == null) {
                    errors.add("createBroadcast: no broadcast container on project")
                }
            }
            ChangeType.WIRE_LOCALIZATION_SWITCH -> {
                val lang = (data["language"] as? String)?.take(2)?.lowercase()
                if (lang.isNullOrBlank()) errors.add("wireLocalizationSwitch: 'language' is required")
            }
        }

        return ValidationResult(errors.isEmpty(), warnings, errors)
    }

    fun validateProjectIntegrity(project: Project): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (project.sceneList.isEmpty()) {
            errors.add("Project has no scenes")
        }

        for (scene in project.sceneList) {
            if (scene.spriteList.isEmpty()) {
                warnings.add("Scene '${scene.name}' has no objects")
            }
            for (sprite in scene.spriteList) {
                if (sprite.scriptList.isEmpty()) {
                    warnings.add("Object '${sprite.name}' in '${scene.name}' has no scripts")
                }
            }
        }

        return ValidationResult(errors.isEmpty(), warnings, errors)
    }

    fun analyzePerformance(project: Project): List<String> {
        val issues = mutableListOf<String>()
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                if (sprite.scriptList.size > 20) {
                    issues.add("${sprite.name} has ${sprite.scriptList.size} scripts - consider organizing")
                }
                for (script in sprite.scriptList) {
                    val bricks = script.getBrickList()
                    if (bricks != null && bricks.size > 50) {
                        issues.add("Script in ${sprite.name} has ${bricks.size} bricks - consider splitting")
                    }
                }
            }
        }
        return issues
    }
}
