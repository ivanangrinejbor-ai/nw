package org.catrobat.catroid.ai.modify

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.ai.tool.BrickFactory
import org.catrobat.catroid.ai.tool.ChangeType
import org.catrobat.catroid.ai.tool.ProjectChange
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.StartScript
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick
import org.catrobat.catroid.content.bricks.SetLookBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable

object ProjectModifier {

    sealed class ModificationResult {
        data class Success(val message: String, val card: ChangeCard? = null) : ModificationResult()
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
                ChangeType.WIRE_LOCALIZATION_SWITCH -> wireLocalizationSwitch(project, change)
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
        return ModificationResult.Success(
            "Created object '$name' in scene '${scene.name}'",
            ChangeCard("Created object '$name'", objectName = name, sceneName = scene.name, added = 1)
        )
    }

    private fun deleteObject(project: Project, change: ProjectChange): ModificationResult {
        val scene = getScene(project, change) ?: return ModificationResult.Failure("Scene not found")
        val name = change.data["name"] as? String ?: return ModificationResult.Failure("Name required")
        val sprite = scene.spriteList.find { it.name == name }
            ?: return ModificationResult.Failure("Object '$name' not found")
        scene.removeSprite(sprite)
        return ModificationResult.Success(
            "Deleted object '$name' from scene '${scene.name}'",
            ChangeCard("Deleted object '$name'", objectName = name, sceneName = scene.name, removed = 1)
        )
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
        return ModificationResult.Success(
            "Created scene '$name'",
            ChangeCard("Created scene '$name'", sceneName = name, added = 1)
        )
    }

    private fun deleteScene(project: Project, change: ProjectChange): ModificationResult {
        val name = change.data["name"] as? String ?: return ModificationResult.Failure("Name required")
        if (project.sceneList.size <= 1) return ModificationResult.Failure("Cannot delete the only scene")
        val scene = project.sceneList.find { it.name == name }
            ?: return ModificationResult.Failure("Scene '$name' not found")
        project.sceneList.remove(scene)
        return ModificationResult.Success(
            "Deleted scene '$name'",
            ChangeCard("Deleted scene '$name'", sceneName = name, removed = 1)
        )
    }

    private fun replaceScript(project: Project, change: ProjectChange): ModificationResult {
        val sprite = getSprite(project, change) ?: return ModificationResult.Failure("Object not found")
        val index = (change.data["index"] as? Number)?.toInt()
            ?: return ModificationResult.Failure("Script index required")
        if (index < 0 || index >= sprite.scriptList.size) {
            return ModificationResult.Failure("Script index $index out of range")
        }
        val scriptType = change.data["scriptType"] as? String ?: "StartScript"
        val bricksText = change.data["bricks"] as? String ?: ""
        val newScript = BrickFactory.createScript(scriptType)
        val specs = BrickFactory.parseBrickSpecs(bricksText)
            ?: return ModificationResult.Failure("Syntax error in brick spec")
        val buildResult = BrickFactory.buildBricks(specs)
        if (buildResult.bricks.isEmpty()) {
            return ModificationResult.Failure(
                "No bricks were created:\n" + buildResult.errors.joinToString("\n") { "  - $it" }
            )
        }
        val oldBrickCount = sprite.scriptList[index].getBrickList().size
        for (b in buildResult.bricks) newScript.addBrick(b)
        sprite.scriptList[index] = newScript
        val msg = "Replaced script $index of '${sprite.name}' with ${newScript::class.java.simpleName} " +
            "(${buildResult.bricks.size} top-level brick(s))"
        val card = ChangeCard(
            "Replaced script $index in '${sprite.name}'",
            objectName = sprite.name,
            sceneName = change.data["scene"] as? String,
            added = buildResult.bricks.size,
            removed = oldBrickCount
        )
        return ModificationResult.Success(
            (if (buildResult.errors.isEmpty()) msg
            else "$msg\nWarnings:\n" + buildResult.errors.joinToString("\n") { "  - $it" }),
            card
        )
    }

    private fun appendScript(project: Project, change: ProjectChange): ModificationResult {
        val sprite = getSprite(project, change) ?: return ModificationResult.Failure("Object not found")
        val scriptType = change.data["scriptType"] as? String ?: "StartScript"
        val bricksText = change.data["bricks"] as? String ?: ""
        val newScript = BrickFactory.createScript(scriptType)
        val specs = BrickFactory.parseBrickSpecs(bricksText)
            ?: return ModificationResult.Failure("Syntax error in brick spec")
        val buildResult = BrickFactory.buildBricks(specs)
        if (buildResult.bricks.isEmpty()) {
            return ModificationResult.Failure(
                "No bricks were created:\n" + buildResult.errors.joinToString("\n") { "  - $it" }
            )
        }
        for (b in buildResult.bricks) newScript.addBrick(b)
        sprite.addScript(newScript)
        val msg = "Appended ${newScript::class.java.simpleName} to '${sprite.name}' " +
            "(${buildResult.bricks.size} top-level brick(s))"
        val card = ChangeCard(
            "Added ${newScript::class.java.simpleName} to '${sprite.name}'",
            objectName = sprite.name,
            sceneName = change.data["scene"] as? String,
            added = buildResult.bricks.size
        )
        return ModificationResult.Success(
            (if (buildResult.errors.isEmpty()) msg
            else "$msg\nWarnings:\n" + buildResult.errors.joinToString("\n") { "  - $it" }),
            card
        )
    }

    private fun deleteScript(project: Project, change: ProjectChange): ModificationResult {
        val sprite = getSprite(project, change) ?: return ModificationResult.Failure("Object not found")
        val index = (change.data["index"] as? Number)?.toInt()
            ?: return ModificationResult.Failure("Script index required")
        if (index < 0 || index >= sprite.scriptList.size) {
            return ModificationResult.Failure("Script index $index out of range")
        }
        val removedBrickCount = sprite.scriptList[index].getBrickList().size
        val removed = sprite.scriptList.removeAt(index)
        return ModificationResult.Success(
            "Deleted ${removed::class.java.simpleName} (index $index) from '${sprite.name}'",
            ChangeCard(
                "Deleted ${removed::class.java.simpleName} from '${sprite.name}'",
                objectName = sprite.name,
                sceneName = change.data["scene"] as? String,
                removed = removedBrickCount
            )
        )
    }

    private fun createVariable(project: Project, change: ProjectChange): ModificationResult {
        val name = change.data["name"] as? String ?: return ModificationResult.Failure("Variable name required")
        val scope = change.data["scope"] as? String ?: "project"
        val initialValue: Any = parseInitialValue(change.data["value"] as? String)
        return when (scope.lowercase()) {
            "project", "global" -> {
                if (project.userVariables.any { it.name == name }) {
                    return ModificationResult.Failure("Global variable '$name' already exists")
                }
                project.addUserVariable(UserVariable(name, initialValue))
                ModificationResult.Success(
                    "Created global variable '$name' (value=$initialValue)",
                    ChangeCard("Created global variable '$name'", added = 1)
                )
            }
            "multiplayer" -> {
                if (project.multiplayerVariables.any { it.name == name }) {
                    return ModificationResult.Failure("Multiplayer variable '$name' already exists")
                }
                project.addMultiplayerVariable(UserVariable(name, initialValue))
                ModificationResult.Success(
                    "Created multiplayer variable '$name' (value=$initialValue)",
                    ChangeCard("Created multiplayer variable '$name'", added = 1)
                )
            }
            "object", "local" -> {
                val sprite = getSprite(project, change)
                    ?: return ModificationResult.Failure("Object required for local variable")
                if (sprite.userVariables.any { it.name == name }) {
                    return ModificationResult.Failure("Local variable '$name' already exists on '${sprite.name}'")
                }
                sprite.addUserVariable(UserVariable(name, initialValue))
                ModificationResult.Success(
                    "Created local variable '$name' on object '${sprite.name}' (value=$initialValue)",
                    ChangeCard(
                        "Created local variable '$name'",
                        objectName = sprite.name,
                        sceneName = change.data["scene"] as? String,
                        added = 1
                    )
                )
            }
            else -> ModificationResult.Failure("Unknown scope '$scope'; use project|multiplayer|object")
        }
    }

    private fun deleteVariable(project: Project, change: ProjectChange): ModificationResult {
        val name = change.data["name"] as? String ?: return ModificationResult.Failure("Variable name required")
        val scope = change.data["scope"] as? String ?: "project"
        return when (scope.lowercase()) {
            "project", "global" -> {
                if (project.removeUserVariable(name)) {
                    ModificationResult.Success(
                        "Deleted global variable '$name'",
                        ChangeCard("Deleted global variable '$name'", removed = 1)
                    )
                } else ModificationResult.Failure("Global variable '$name' not found")
            }
            "multiplayer" -> {
                val mp = project.multiplayerVariables.find { it.name == name }
                if (mp != null && project.multiplayerVariables.remove(mp)) {
                    ModificationResult.Success(
                        "Deleted multiplayer variable '$name'",
                        ChangeCard("Deleted multiplayer variable '$name'", removed = 1)
                    )
                } else ModificationResult.Failure("Multiplayer variable '$name' not found")
            }
            "object", "local" -> {
                val sprite = getSprite(project, change)
                    ?: return ModificationResult.Failure("Object required for local variable")
                val v = sprite.userVariables.find { it.name == name }
                if (v != null && sprite.userVariables.remove(v)) {
                    ModificationResult.Success(
                        "Deleted local variable '$name' from '${sprite.name}'",
                        ChangeCard(
                            "Deleted local variable '$name'",
                            objectName = sprite.name,
                            sceneName = change.data["scene"] as? String,
                            removed = 1
                        )
                    )
                } else ModificationResult.Failure("Local variable '$name' not found on '${sprite.name}'")
            }
            else -> ModificationResult.Failure("Unknown scope '$scope'")
        }
    }

    private fun createBroadcast(project: Project, change: ProjectChange): ModificationResult {
        val name = change.data["name"] as? String ?: return ModificationResult.Failure("Broadcast name required")
        val container = project.broadcastMessageContainer
        if (container == null) return ModificationResult.Failure("No broadcast container on project")
        val added = container.addBroadcastMessage(name)
        return if (added) ModificationResult.Success(
            "Created broadcast message '$name'",
            ChangeCard("Created broadcast '$name'", added = 1)
        )
        else ModificationResult.Success("Broadcast message '$name' already exists")
    }

    private fun modifyBrick(project: Project, change: ProjectChange): ModificationResult {
        val sprite = getSprite(project, change) ?: return ModificationResult.Failure("Object not found")
        val scriptIndex = (change.data["scriptIndex"] as? Number)?.toInt()
            ?: return ModificationResult.Failure("scriptIndex required")
        val brickIndex = (change.data["brickIndex"] as? Number)?.toInt()
            ?: return ModificationResult.Failure("brickIndex required")
        if (scriptIndex < 0 || scriptIndex >= sprite.scriptList.size) {
            return ModificationResult.Failure("Script index $scriptIndex out of range")
        }
        val script = sprite.scriptList[scriptIndex]
        val bricks = script.getBrickList()
        if (brickIndex < 0 || brickIndex >= bricks.size) {
            return ModificationResult.Failure("Brick index $brickIndex out of range")
        }
        val newType = change.data["newType"] as? String
            ?: return ModificationResult.Failure("newType (brick class name) required")
        val fields = change.data["fields"] as? String ?: ""
        val spec = "$newType($fields)"
        val parsed = BrickFactory.parseBrickSpecs(spec)
            ?: return ModificationResult.Failure("Syntax error in brick spec")
        val first = parsed.firstOrNull() ?: return ModificationResult.Failure("Empty spec")
        val validationError = BrickFactory.validateBrickSpec(first)
        if (validationError != null) return ModificationResult.Failure(validationError)
        val newBrick = BrickFactory.buildBrick(first)
            ?: return ModificationResult.Failure("Failed to build brick '$newType'")
        bricks[brickIndex] = newBrick
        return ModificationResult.Success(
            "Replaced brick $brickIndex in script $scriptIndex of '${sprite.name}' with $newType",
            ChangeCard(
                "Swapped brick to $newType in '${sprite.name}'",
                objectName = sprite.name,
                sceneName = change.data["scene"] as? String,
                added = 1,
                removed = 1
            )
        )
    }

    /**
     * Wire automatic language switching for the localized costumes created by
     * `localizeSprites`. For every sprite that has a costume named "<orig> (<lang>)",
     * adds a `When scene starts` script: If (language = '<lang>') switch to the localized
     * costume, else the original. Creates the global `language` variable if missing.
     * Idempotent: a sprite already carrying such a switch for the same language is skipped.
     */
    private fun wireLocalizationSwitch(project: Project, change: ProjectChange): ModificationResult {
        val langCode = (change.data["language"] as? String)?.take(2)?.lowercase()
            ?: return ModificationResult.Failure("language required")

        var createdVar = false
        if (project.userVariables.none { it.name == "language" } &&
            project.multiplayerVariables.none { it.name == "language" }) {
            project.addUserVariable(UserVariable("language", langCode))
            createdVar = true
        }

        val suffix = " ($langCode)"
        var wired = 0
        for (scene in project.sceneList) {
            for (sprite in scene.spriteList) {
                if (spriteHasLanguageSwitch(sprite, langCode)) continue
                val looks = sprite.lookList.toList()
                for (localized in looks) {
                    if (!localized.name.endsWith(suffix)) continue
                    val original = looks.firstOrNull { it.name == localized.name.removeSuffix(suffix) }
                        ?: continue

                    val condition = Formula(
                        FormulaElement(
                            FormulaElement.ElementType.OPERATOR,
                            org.catrobat.catroid.formulaeditor.Operators.EQUAL.name, null,
                            FormulaElement(FormulaElement.ElementType.USER_VARIABLE, "language", null),
                            FormulaElement(FormulaElement.ElementType.STRING, langCode, null)
                        )
                    )
                    val ifBrick = IfLogicBeginBrick(condition)
                    ifBrick.addBrickToIfBranch(SetLookBrick().apply { setLook(localized) })
                    ifBrick.addBrickToElseBranch(SetLookBrick().apply { setLook(original) })

                    val script = StartScript()
                    script.addBrick(ifBrick)
                    sprite.addScript(script)
                    wired++
                }
            }
        }

        if (wired == 0) {
            return ModificationResult.Failure(
                "No localized costumes '… ($langCode)' found to wire (or already wired). Run localizeSprites first."
            )
        }
        val varNote = if (createdVar) " Created global variable 'language' (= '$langCode')." else ""
        return ModificationResult.Success(
            "Wired language switching for $wired sprite(s): When scene starts → " +
                "If (language = '$langCode') use localized costume, else original.$varNote",
            ChangeCard("Wired language switch ($langCode)", added = wired)
        )
    }

    /** True if [sprite] already has an If-brick condition of the form `language = '<langCode>'`. */
    private fun spriteHasLanguageSwitch(sprite: Sprite, langCode: String): Boolean {
        for (s in sprite.scriptList) {
            for (b in s.brickList) {
                if (b !is IfLogicBeginBrick) continue
                val root = b.getFormulaWithBrickField(
                    org.catrobat.catroid.content.bricks.Brick.BrickField.IF_CONDITION
                )?.root ?: continue
                if (root.elementType == FormulaElement.ElementType.OPERATOR &&
                    root.value == org.catrobat.catroid.formulaeditor.Operators.EQUAL.name &&
                    root.leftChild?.elementType == FormulaElement.ElementType.USER_VARIABLE &&
                    root.leftChild?.value == "language" &&
                    root.rightChild?.value == langCode
                ) return true
            }
        }
        return false
    }

    /** Parse "BrickName(a, b)" -> ("BrickName", ["a", "b"]). */
    private fun parseBrickSpec(spec: String): Pair<String, List<String>> {
        val open = spec.indexOf('(')
        if (open < 0 || !spec.endsWith(")")) return spec.trim() to emptyList()
        val className = spec.substring(0, open).trim()
        val inner = spec.substring(open + 1, spec.length - 1)
        if (inner.isBlank()) return className to emptyList()
        return className to parseBrickArgs(inner)
    }

    private fun parseBrickArgs(inner: String): List<String> {
        val args = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        for (c in inner) {
            when (c) {
                '(' -> { depth++; current.append(c) }
                ')' -> { depth--; current.append(c) }
                ',' -> if (depth == 0) {
                    args.add(current.toString().trim()); current.setLength(0)
                } else current.append(c)
                else -> current.append(c)
            }
        }
        if (current.isNotBlank()) args.add(current.toString().trim())
        return args
    }

    private fun parseInitialValue(raw: String?): Any {
        if (raw == null) return 0.0
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return 0.0
        trimmed.toDoubleOrNull()?.let { return it }
        trimmed.toLongOrNull()?.let { return it.toDouble() }
        if (trimmed.equals("true", true)) return 1.0
        if (trimmed.equals("false", true)) return 0.0
        return trimmed
    }
}
