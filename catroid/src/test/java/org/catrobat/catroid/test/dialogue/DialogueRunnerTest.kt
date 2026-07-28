package org.catrobat.catroid.test.dialogue

import org.catrobat.catroid.dialogue.*
import org.junit.Test
import org.junit.Assert.*

class DialogueRunnerTest {

    @Test
    fun `runner starts and processes dialogue flow`() {
        val tree = DialogueTree(name = "TestFlow")
        tree.nodes.add(DialogueNode.StartNode("start", 0f, 0f, "d1"))
        tree.nodes.add(DialogueNode.DialogueLine("d1", 0f, 0f, "", "NPC", "", "Hello {player}!", next = "e1"))
        tree.nodes.add(DialogueNode.EndNode("e1", 0f, 0f))

        val state = DialogueState()
        val runner = DialogueRunner(state)
        var shownSpeaker = ""
        var shownText = ""
        var dialogueEnded = false

        runner.onShowDialogue = { speaker, text, _, _ ->
            shownSpeaker = speaker
            shownText = text
        }
        runner.onDialogueEnd = { dialogueEnded = true }

        state.variables["player"] = "Ivan"
        runner.load(tree)
        runner.start()

        assertEquals("NPC", shownSpeaker)
        assertEquals("Hello Ivan!", shownText)
        assertTrue(runner.isRunning())

        runner.continueAfterDialogue()
        assertFalse(runner.isRunning())
        assertTrue(dialogueEnded)
    }

    @Test
    fun `choice node returns selected choice`() {
        val tree = DialogueTree(name = "ChoiceTest")
        tree.nodes.add(DialogueNode.StartNode("s", 0f, 0f, "c1"))
        tree.nodes.add(DialogueNode.ChoiceNode("c1", 0f, 0f, mutableListOf(
            DialogueNode.Choice("Option A", "e1"),
            DialogueNode.Choice("Option B", "e2")
        )))
        tree.nodes.add(DialogueNode.DialogueLine("e1", 0f, 0f, "", "NPC", "", "You chose A"))
        tree.nodes.add(DialogueNode.EndNode("e2", 0f, 0f))

        val runner = DialogueRunner(DialogueState())
        runner.load(tree)
        runner.start()

        assertTrue(runner.isRunning())

        runner.selectChoice(0)
        assertEquals("Option A", runner.getSelectedChoice())
        assertEquals("You chose A", runner.getCurrentText())
    }

    @Test
    fun `condition node evaluates correctly`() {
        val tree = DialogueTree(name = "CondTest")
        tree.nodes.add(DialogueNode.StartNode("s", 0f, 0f, "cond"))
        tree.nodes.add(DialogueNode.ConditionNode("cond", 0f, 0f, "coins >= 100", "rich", "poor"))
        tree.nodes.add(DialogueNode.DialogueLine("rich", 0f, 0f, "", "NPC", "", "Rich!"))
        tree.nodes.add(DialogueNode.DialogueLine("poor", 0f, 0f, "", "NPC", "", "Poor!"))

        val state = DialogueState()
        val runner = DialogueRunner(state)

        state.variables["coins"] = 150
        runner.load(tree)
        runner.start()
        assertEquals("Rich!", runner.getCurrentText())

        state.variables["coins"] = 50
        runner.start()
        assertEquals("Poor!", runner.getCurrentText())
    }

    @Test
    fun `action node modifies variables`() {
        val tree = DialogueTree(name = "ActionTest")
        tree.nodes.add(DialogueNode.StartNode("s", 0f, 0f, "a1"))
        tree.nodes.add(DialogueNode.ActionNode("a1", 0f, 0f, mutableListOf(
            DialogueNode.ActionEntry("setVariable", "coins", "+100"),
            DialogueNode.ActionEntry("giveItem", "Sword", "")
        ), "e1"))
        tree.nodes.add(DialogueNode.EndNode("e1", 0f, 0f))

        val state = DialogueState()
        val runner = DialogueRunner(state)
        state.variables["coins"] = 50

        runner.load(tree)
        runner.start()

        assertFalse(runner.isRunning())
        assertEquals(150.0, state.variables["coins"])
        assertEquals(true, state.variables["inventory_Sword"])
    }

    @Test
    fun `jump to specific node`() {
        val tree = DialogueTree(name = "JumpTest")
        tree.nodes.add(DialogueNode.StartNode("s", 0f, 0f, "d1"))
        tree.nodes.add(DialogueNode.DialogueLine("d1", 0f, 0f, "", "NPC", "", "First"))
        tree.nodes.add(DialogueNode.DialogueLine("d2", 0f, 0f, "", "NPC", "", "Second"))

        val runner = DialogueRunner(DialogueState())
        runner.load(tree)
        runner.start()
        assertEquals("First", runner.getCurrentText())

        runner.jumpTo("d2")
        assertEquals("Second", runner.getCurrentText())
    }

    @Test
    fun `variable resolver replaces nested variables`() {
        val variables = mapOf<String, Any>(
            "player" to mapOf("name" to "Ivan", "level" to 5),
            "coins" to 100,
            "health" to 80
        )
        val result = DialogueVariableResolver.resolve(
            "{player.name} has {coins} coins and {health} HP",
            variables
        )
        assertEquals("Ivan has 100 coins and 80 HP", result)
    }

    @Test
    fun `variable resolver supports array index access`() {
        val variables = mapOf<String, Any>(
            "enemies" to listOf(mapOf("name" to "Goblin"), mapOf("name" to "Orc")),
            "party" to listOf("Warrior", "Mage", "Rogue")
        )
        assertEquals("Goblin", DialogueVariableResolver.resolve("{enemies[0].name}", variables))
        assertEquals("Mage", DialogueVariableResolver.resolve("{party[1]}", variables))
    }

    @Test
    fun `variable resolver escapes double braces`() {
        val result = DialogueVariableResolver.resolve("Hello {{world}}", emptyMap())
        assertEquals("Hello {world}", result)
    }

    @Test
    fun `unknown variable remains unchanged`() {
        val result = DialogueVariableResolver.resolve("Hello {unknown}", emptyMap())
        assertEquals("Hello {unknown}", result)
    }
}
