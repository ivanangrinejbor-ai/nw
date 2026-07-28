package org.catrobat.catroid.test.dialogue

import org.catrobat.catroid.dialogue.*
import org.junit.Test
import org.junit.Assert.*

class DialogueSerializerTest {

    @Test
    fun `serialize and deserialize simple dialogue`() {
        val tree = DialogueTree(name = "Test")
        tree.nodes.add(DialogueNode.StartNode("start", 0f, 0f, "n1"))
        tree.nodes.add(DialogueNode.DialogueLine("n1", 100f, 100f, "", "NPC", "", "Hello {player}!", next = "n2"))
        tree.nodes.add(DialogueNode.EndNode("n2", 200f, 200f))

        val json = DialogueSerializer.serialize(tree)
        val loaded = DialogueSerializer.deserialize(json)

        assertEquals(3, loaded.nodes.size)
        assertTrue(loaded.nodes.any { it is DialogueNode.StartNode })
        assertTrue(loaded.nodes.any { it is DialogueNode.DialogueLine && it.text == "Hello {player}!" })
        assertTrue(loaded.nodes.any { it is DialogueNode.EndNode })
    }

    @Test
    fun `serialize and deserialize full dialogue tree`() {
        val tree = DialogueTree(name = "FullTest")
        tree.nodes.add(DialogueNode.StartNode("s1", 0f, 0f, "d1"))
        tree.nodes.add(DialogueNode.DialogueLine("d1", 100f, 100f, "", "NPC", "portrait.png",
            "Hello {player.name}!", "voice.mp3", 0.08f, "bg.jpg", "c1"))
        tree.nodes.add(DialogueNode.ChoiceNode("c1", 200f, 200f, mutableListOf(
            DialogueNode.Choice("Buy sword", "a1"),
            DialogueNode.Choice("Leave", "e1")
        )))
        tree.nodes.add(DialogueNode.ActionNode("a1", 300f, 300f, mutableListOf(
            DialogueNode.ActionEntry("setVariable", "coins", "+100"),
            DialogueNode.ActionEntry("giveItem", "Sword", "")
        ), "c2"))
        tree.nodes.add(DialogueNode.ConditionNode("c2", 400f, 400f, "coins >= 200", "d2", "e2"))
        tree.nodes.add(DialogueNode.DialogueLine("d2", 500f, 500f, "", "NPC", "", "You are rich!"))
        tree.nodes.add(DialogueNode.EndNode("e1", 300f, 100f))
        tree.nodes.add(DialogueNode.EndNode("e2", 500f, 200f))
        tree.nodes.add(DialogueNode.CommentNode("cm1", 50f, 300f, "Test comment", "#808080"))

        val json = DialogueSerializer.serialize(tree)
        val loaded = DialogueSerializer.deserialize(json)

        assertEquals(9, loaded.nodes.size)
        val loadedChoice = loaded.nodes.find { it is DialogueNode.ChoiceNode } as? DialogueNode.ChoiceNode
        assertNotNull(loadedChoice)
        assertEquals(2, loadedChoice!!.choices.size)
        assertEquals("Buy sword", loadedChoice.choices[0].text)
        assertEquals("a1", loadedChoice.choices[0].next)

        val loadedAction = loaded.nodes.find { it is DialogueNode.ActionNode } as? DialogueNode.ActionNode
        assertNotNull(loadedAction)
        assertEquals(2, loadedAction!!.actions.size)
        assertEquals("setVariable", loadedAction.actions[0].type)
        assertEquals("coins", loadedAction.actions[0].name)
        assertEquals("+100", loadedAction.actions[0].value)

        val loadedCond = loaded.nodes.find { it is DialogueNode.ConditionNode } as? DialogueNode.ConditionNode
        assertNotNull(loadedCond)
        assertEquals("coins >= 200", loadedCond!!.expression)
        assertEquals("d2", loadedCond.trueNext)
        assertEquals("e2", loadedCond.falseNext)
    }

    @Test
    fun `validate dialogue without start node returns error`() {
        val tree = DialogueTree(name = "NoStart")
        tree.nodes.add(DialogueNode.DialogueLine("d1", 0f, 0f, "", "NPC", "", "Hello"))
        val errors = tree.validate()
        assertTrue(errors.any { it.contains("No Start node") })
    }

    @Test
    fun `validate dialogue with broken reference returns error`() {
        val tree = DialogueTree(name = "BrokenRef")
        tree.nodes.add(DialogueNode.StartNode("start", 0f, 0f, "nonexistent"))
        val errors = tree.validate()
        assertTrue(errors.any { it.contains("references missing node") })
    }

    @Test
    fun `validate dialogue with unreachable node`() {
        val tree = DialogueTree(name = "UnreachableTest")
        tree.nodes.add(DialogueNode.StartNode("start", 0f, 0f, "d1"))
        tree.nodes.add(DialogueNode.DialogueLine("d1", 100f, 100f, "", "NPC", "", "Hello", next = "e1"))
        tree.nodes.add(DialogueNode.EndNode("e1", 200f, 200f))
        tree.nodes.add(DialogueNode.DialogueLine("orphan", 400f, 400f, "", "Ghost", "", "No one sees me"))
        val errors = tree.validate()
        assertTrue(errors.any { it.contains("unreachable") })
    }

    @Test
    fun `validate valid dialogue passes`() {
        val tree = DialogueTree(name = "Valid")
        tree.nodes.add(DialogueNode.StartNode("start", 0f, 0f, "d1"))
        tree.nodes.add(DialogueNode.DialogueLine("d1", 100f, 100f, "", "NPC", "", "Hello"))
        tree.nodes.add(DialogueNode.EndNode("e1", 200f, 200f))
        val errors = tree.validate()
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `dialogue node with textId is valid even with empty text`() {
        val tree = DialogueTree(name = "LocalizedTest")
        tree.nodes.add(DialogueNode.StartNode("start", 0f, 0f, "d1"))
        tree.nodes.add(DialogueNode.DialogueLine("d1", 100f, 100f, textId = "greeting.hello", speaker = "NPC", text = ""))
        tree.nodes.add(DialogueNode.EndNode("e1", 200f, 200f))
        val errors = tree.validate()
        val dialogueErrors = errors.filter { it.contains("empty text") }
        assertTrue("textId should allow empty text", dialogueErrors.isEmpty())
    }
}
