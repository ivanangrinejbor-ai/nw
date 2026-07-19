package org.catrobat.catroid.ai.analysis

import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick

object ScriptReader {

    data class ScriptDescription(
        val index: Int,
        val type: String,
        val bricks: List<BrickDescription>,
        val summary: String
    )

    data class BrickDescription(
        val index: Int,
        val type: String,
        val fields: Map<String, String>
    )

    fun readScript(script: Script, index: Int): ScriptDescription {
        val type = script::class.java.simpleName
        val bricks = script.getBrickList().mapIndexed { i, brick -> describeBrick(brick, i) }

        val summary = buildString {
            append("Script $index: $type")
            if (bricks.isNotEmpty()) {
                append(" (${bricks.size} bricks)")
                val categories = bricks.groupBy { it.type }
                for ((cat, list) in categories) {
                    append("\n  $cat: ${list.size}")
                }
            }
        }

        return ScriptDescription(index, type, bricks, summary)
    }

    fun readAllScripts(sprite: Sprite): List<ScriptDescription> {
        return sprite.scriptList.mapIndexed { i, script -> readScript(script, i) }
    }

    private fun describeBrick(brick: Brick, index: Int): BrickDescription {
        val type = brick::class.java.simpleName
        val fields = mutableMapOf<String, String>()
        try {
            for (field in brick.javaClass.declaredFields) {
                field.isAccessible = true
                val name = field.name
                if (name.contains("serialVersionUID") || name.contains("$")) continue
                val value = try { field.get(brick)?.toString() ?: "null" } catch (_: Exception) { "N/A" }
                if (value.length < 100) fields[name] = value
            }
        } catch (_: Exception) {}
        return BrickDescription(index, type, fields)
    }

    fun createReadableScriptSummary(sprite: Sprite): String {
        val scripts = readAllScripts(sprite)
        return scripts.joinToString("\n\n") { it.summary }
    }

    fun getBrickCategory(brick: Brick): String {
        val pkg = brick::class.java.`package`?.name ?: ""
        return when {
            pkg.contains("physics") -> "Physics"
            brick::class.java.simpleName.contains("Motion") ||
                brick::class.java.simpleName.contains("Move") ||
                brick::class.java.simpleName.contains("Turn") ||
                brick::class.java.simpleName.contains("Set") && !brick::class.java.simpleName.contains("Variable") -> "Motion"
            brick::class.java.simpleName.contains("Look") ||
                brick::class.java.simpleName.contains("Show") ||
                brick::class.java.simpleName.contains("Hide") ||
                brick::class.java.simpleName.contains("Background") -> "Looks"
            brick::class.java.simpleName.contains("Sound") ||
                brick::class.java.simpleName.contains("Play") -> "Sound"
            brick::class.java.simpleName.contains("Control") ||
                brick::class.java.simpleName.contains("Wait") ||
                brick::class.java.simpleName.contains("If") ||
                brick::class.java.simpleName.contains("Repeat") ||
                brick::class.java.simpleName.contains("Forever") -> "Control"
            brick::class.java.simpleName.contains("Variable") ||
                brick::class.java.simpleName.contains("List") -> "Data"
            else -> "Other"
        }
    }
}
