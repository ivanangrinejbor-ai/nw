package org.catrobat.catroid.stage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class SceneSelectorTest {

    private fun parse(xml: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val doc = factory.newDocumentBuilder().parse(org.xml.sax.InputSource(StringReader(xml)))
        doc.documentElement.normalize()
        return doc
    }

    private fun names(objs: List<Element>): List<String> = objs.map { it.getAttribute("name") }

    private val twoScenesWithGlobal = """
        <program>
          <scenes>
            <scene><name>SceneA</name><objectList>
              <object name="a1"/><object name="a2"/>
            </objectList></scene>
            <scene><name>SceneB</name><objectList>
              <object name="b1"/>
            </objectList></scene>
          </scenes>
          <globalScene><name>Global</name><objectList>
            <object name="g1"/>
          </objectList></globalScene>
        </program>
    """.trimIndent()

    @Test
    fun defaultSelectsFirstSceneWithGlobalPrefix() {
        val sel = SceneSelector.selectScene(parse(twoScenesWithGlobal), null)
        assertEquals("SceneA", sel.activeSceneName)
        assertTrue(sel.hasGlobal)
        assertEquals(1, sel.globalCount)
        assertEquals(listOf("g1", "a1", "a2"), names(sel.objectEls))
    }

    @Test
    fun sceneNamesListsAllRegularScenesExcludingGlobal() {
        val sel = SceneSelector.selectScene(parse(twoScenesWithGlobal), null)
        assertEquals(listOf("SceneA", "SceneB"), sel.sceneNames)
    }

    @Test
    fun namedSceneIsSelectedWithGlobalPrefix() {
        val sel = SceneSelector.selectScene(parse(twoScenesWithGlobal), "SceneB")
        assertEquals("SceneB", sel.activeSceneName)
        assertEquals(1, sel.globalCount)
        assertEquals(listOf("g1", "b1"), names(sel.objectEls))
    }

    @Test
    fun unknownSceneFallsBackToFirst() {
        val sel = SceneSelector.selectScene(parse(twoScenesWithGlobal), "DoesNotExist")
        assertEquals("SceneA", sel.activeSceneName)
        assertEquals(listOf("g1", "a1", "a2"), names(sel.objectEls))
    }

    @Test
    fun projectWithoutGlobalSceneHasZeroGlobalCount() {
        val xml = """
            <program><scenes>
              <scene><name>Only</name><objectList>
                <object name="o1"/><object name="o2"/>
              </objectList></scene>
            </scenes></program>
        """.trimIndent()
        val sel = SceneSelector.selectScene(parse(xml), null)
        assertFalse(sel.hasGlobal)
        assertEquals(0, sel.globalCount)
        assertEquals("Only", sel.activeSceneName)
        assertEquals(listOf("o1", "o2"), names(sel.objectEls))
    }

    @Test
    fun onlyActiveSceneObjectsAreSelectedNotAllScenes() {
        val sel = SceneSelector.selectScene(parse(twoScenesWithGlobal), "SceneA")
        val allNames = names(sel.objectEls)
        assertFalse("SceneB object must not be loaded when SceneA is active", allNames.contains("b1"))
        assertEquals(listOf("g1", "a1", "a2"), allNames)
    }

    @Test
    fun emptyProjectYieldsEmptySelection() {
        val sel = SceneSelector.selectScene(parse("<program><scenes></scenes></program>"), null)
        assertEquals(0, sel.globalCount)
        assertEquals(0, sel.objectEls.size)
        assertEquals(emptyList<String>(), sel.sceneNames)
    }
}
