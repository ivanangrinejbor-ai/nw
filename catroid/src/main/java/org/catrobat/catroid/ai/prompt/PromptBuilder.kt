package org.catrobat.catroid.ai.prompt

import org.catrobat.catroid.ai.analysis.ProjectAnalyzer
import org.catrobat.catroid.ai.context.ContextManager
import org.catrobat.catroid.ai.context.MemoryManager
import org.catrobat.catroid.ai.tool.ToolCallingEngine

object PromptBuilder {

    fun buildSystemPrompt(analysis: ProjectAnalyzer.AnalysisResult?): String {
        return buildString {
            appendLine("You are NeoCatroid AI Agent — an intelligent assistant for the NeoCatroid visual programming system.")
            appendLine()
            appendLine("## Your Capabilities")
            appendLine("- Analyze NeoCatroid projects with scenes, objects, sprites, scripts and bricks")
            appendLine("- Understand visual programming blocks and their logic")
            appendLine("- Create, read, update and delete project elements via tool calling")
            appendLine("- Optimize game logic and find errors")
            appendLine("- Generate new game mechanics")
            appendLine()
            appendLine("## Working with the Project")
            appendLine("- You use Tool Calling to interact with the project")
            appendLine("- Always analyze before making changes")
            appendLine("- Never assume project structure without checking")
            appendLine("- Use cached information when available")
            appendLine()
            appendLine("## Tool Calling Format")
            appendLine("To call a tool, use this XML format:")
            appendLine("<tool_call>")
            appendLine("  <name>toolName</name>")
            appendLine("  <args>")
            appendLine("    <param1>value1</param1>")
            appendLine("    <param2>value2</param2>")
            appendLine("  </args>")
            appendLine("</tool_call>")
            appendLine()

            val toolsDesc = ToolCallingEngine.getToolsDescription()
            if (toolsDesc.isNotBlank()) {
                appendLine("## Available Tools")
                appendLine(toolsDesc)
                appendLine()
            }

            appendLine("## Available Bricks (Block Types)")
            appendLine("- Motion: MoveNSteps, TurnLeft, TurnRight, SetX, SetY, ChangeX, ChangeY, GoTo, PlaceAt, PointInDirection, SetSizeTo, GlideTo, SetRotationStyle, TouchDirection, IfOnEdgeBounce")
            appendLine("- Looks: Show, Hide, NextLook, PreviousLook, SetLook, SetBackground, SetSizeTo, ChangeSize, SetTransparency, SetBrightness, SetColor, ClearEffects")
            appendLine("- Sound: PlaySound, PlaySoundAndWait, StopSound, StopAllSounds, SetVolume, ChangeVolume")
            appendLine("- Control: Wait, WaitUntil, Repeat, Forever, IfLogic, IfThenLogic, ForVariable, Broadcast, BroadcastWait, Note")
            appendLine("- Data: SetVariable, ChangeVariable, ShowText, HideText")
            appendLine("- Events: StartScript, WhenScript, BroadcastScript")
            appendLine("- Pen: PenDown, PenUp, SetPenSize, SetPenColor, Stamp, ClearBackground")
            appendLine()

            if (analysis != null) {
                appendLine("## Current Project Analysis")
                appendLine(analysis.summary)
                appendLine()
            }

            val memories = MemoryManager.getSummary()
            if (memories.isNotEmpty()) {
                appendLine("## Stored Memories")
                appendLine(memories)
                appendLine()
            }

            appendLine("## Response Guidelines")
            appendLine("- Analyze the project before making changes")
            appendLine("- Use tools to read data, never invent project contents")
            appendLine("- Explain changes before applying them")
            appendLine("- Format code blocks with markdown")
            appendLine("- Be concise and practical")
            appendLine("- If you need more data, call the appropriate tool")
            appendLine("- When user asks to create a game, plan the architecture first")
        }
    }

    fun buildUserPrompt(userInput: String, analysis: ProjectAnalyzer.AnalysisResult?): String {
        return buildString {
            appendLine("## User Request")
            appendLine(userInput)
            appendLine()
            if (analysis != null) {
                appendLine("## Available Project Context")
                appendLine("Project: ${analysis.projectName}")
                appendLine("Scenes: ${analysis.totalScenes}")
                appendLine("Objects: ${analysis.totalObjects}")
                appendLine("Scripts: ${analysis.totalScripts}")
                appendLine("Bricks: ${analysis.totalBricks}")
            }
        }
    }

    fun assembleFullPrompt(
        systemPrompt: String,
        conversationHistory: List<ContextManager.ConversationEntry>,
        userPrompt: String
    ): String {
        val sb = StringBuilder()
        sb.appendLine(systemPrompt)
        sb.appendLine()

        if (conversationHistory.isNotEmpty()) {
            sb.appendLine("## Conversation History")
            for (entry in conversationHistory) {
                sb.appendLine("User: ${entry.userMessage}")
                sb.appendLine("Assistant: ${entry.aiResponse}")
                sb.appendLine()
            }
        }

        sb.appendLine(userPrompt)
        sb.appendLine()
        sb.appendLine("## Assistant Response:")
        return sb.toString()
    }

    fun buildToolResultPrompt(toolName: String, result: String): String {
        return buildString {
            appendLine("## Tool Call Result")
            appendLine("Tool: $toolName")
            appendLine("Result: $result")
            appendLine()
            appendLine("Continue with your analysis based on this result.")
        }
    }
}
