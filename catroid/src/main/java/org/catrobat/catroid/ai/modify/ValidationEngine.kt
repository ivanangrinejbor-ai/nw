package org.catrobat.catroid.ai.modify

import org.catrobat.catroid.ProjectManager
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

        // TODO: implement actual validation logic for each ProjectChange type
        return ValidationResult(true, warnings, errors)
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
