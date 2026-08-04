package org.catrobat.catroid.ai.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallingEngineParseTest {

    @Test
    fun xml_basic_call_with_args() {
        val calls = ToolCallingEngine.parseToolCalls(
            "<tool_call><name>readObject</name><args><object>Cat</object><scene>Scene 1</scene></args></tool_call>"
        )
        assertEquals(1, calls.size)
        assertEquals("readObject", calls[0].name)
        assertEquals("Cat", calls[0].args["object"])
        assertEquals("Scene 1", calls[0].args["scene"])
    }

    @Test
    fun xml_multiple_calls_kept_in_order() {
        val calls = ToolCallingEngine.parseToolCalls(
            "<tool_call><name>createScene</name><args><name>Level 2</name></args></tool_call>" +
                "<tool_call><name>createObject</name><args><scene>Level 2</scene><name>Bird</name></args></tool_call>"
        )
        assertEquals(2, calls.size)
        assertEquals("createScene", calls[0].name)
        assertEquals("createObject", calls[1].name)
        assertEquals("Level 2", calls[1].args["scene"])
        assertEquals("Bird", calls[1].args["name"])
    }

    @Test
    fun xml_embedded_in_narrative_text() {
        val calls = ToolCallingEngine.parseToolCalls(
            "I will inspect the project first.\n\n<tool_call><name>projectInventory</name></tool_call>\n\n" +
                "Now I can see the full structure."
        )
        assertEquals(1, calls.size)
        assertEquals("projectInventory", calls[0].name)
    }

    @Test
    fun xml_attribute_shorthand() {
        val calls = ToolCallingEngine.parseToolCalls(
            """<tool_call name="createVariable" var="score" scope="project"/>"""
        )
        assertEquals(1, calls.size)
        assertEquals("createVariable", calls[0].name)
        assertEquals("score", calls[0].args["var"])
        assertEquals("project", calls[0].args["scope"])
    }

    @Test
    fun xml_without_args() {
        val calls = ToolCallingEngine.parseToolCalls("<tool_call><name>listScenes</name></tool_call>")
        assertEquals(1, calls.size)
        assertEquals("listScenes", calls[0].name)
        assertTrue(calls[0].args.isEmpty())
    }

    @Test
    fun json_array_with_args_object() {
        val calls = ToolCallingEngine.parseToolCalls(
            """[{"name":"listObjects","args":{"scene":"Scene 1"}}]"""
        )
        assertEquals(1, calls.size)
        assertEquals("listObjects", calls[0].name)
        assertEquals("Scene 1", calls[0].args["scene"])
    }

    @Test
    fun json_single_object_with_arguments() {
        val calls = ToolCallingEngine.parseToolCalls(
            """{"name":"buildScript","arguments":{"object":"Cat","scene":"Scene 1","scriptType":"StartScript"}}"""
        )
        assertEquals(1, calls.size)
        assertEquals("buildScript", calls[0].name)
        assertEquals("Cat", calls[0].args["object"])
        assertEquals("StartScript", calls[0].args["scriptType"])
    }

    @Test
    fun json_with_stringified_arguments() {
        val calls = ToolCallingEngine.parseToolCalls(
            """{"tool":"readFile","arguments":"{\"path\":\"level1.txt\"}"}"""
        )
        assertEquals(1, calls.size)
        assertEquals("readFile", calls[0].name)
        assertEquals("level1.txt", calls[0].args["path"])
    }

    @Test
    fun json_with_parameters() {
        val calls = ToolCallingEngine.parseToolCalls(
            """[{"name":"createVariable","parameters":{"name":"lives","scope":"object","object":"Bird"}}]"""
        )
        assertEquals(1, calls.size)
        assertEquals("createVariable", calls[0].name)
        assertEquals("lives", calls[0].args["name"])
        assertEquals("object", calls[0].args["scope"])
        assertEquals("Bird", calls[0].args["object"])
    }

    @Test
    fun json_inside_markdown_fence() {
        val calls = ToolCallingEngine.parseToolCalls(
            "```json\n[{\"name\":\"openProject\",\"args\":{\"name\":\"MyGame\"}}]\n```"
        )
        assertEquals(1, calls.size)
        assertEquals("openProject", calls[0].name)
        assertEquals("MyGame", calls[0].args["name"])
    }

    @Test
    fun json_inside_plain_fence() {
        val calls = ToolCallingEngine.parseToolCalls(
            "```\n[{\"name\":\"listVariables\"}]\n```"
        )
        assertEquals(1, calls.size)
        assertEquals("listVariables", calls[0].name)
    }

    @Test
    fun plain_text_returns_no_calls() {
        val calls = ToolCallingEngine.parseToolCalls(
            "I found 3 objects in your project. The bird moves 10 steps when tapped."
        )
        assertTrue(calls.isEmpty())
    }

    @Test
    fun empty_response_returns_no_calls() {
        assertTrue(ToolCallingEngine.parseToolCalls("").isEmpty())
        assertTrue(ToolCallingEngine.parseToolCalls("   ").isEmpty())
    }

    @Test
    fun malformed_xml_returns_no_calls() {
        val calls = ToolCallingEngine.parseToolCalls(
            "<tool_call><name>readObject</name>" // unclosed
        )
        assertTrue(calls.isEmpty())
    }

    @Test
    fun json_malformed_returns_no_calls() {
        val calls = ToolCallingEngine.parseToolCalls("[{\"name\":\"listScenes\"")
        assertTrue(calls.isEmpty())
    }

    @Test
    fun json_empty_array_returns_no_calls() {
        assertTrue(ToolCallingEngine.parseToolCalls("[]").isEmpty())
    }
}
