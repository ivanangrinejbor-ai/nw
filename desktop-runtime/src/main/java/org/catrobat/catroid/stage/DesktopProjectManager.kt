package org.catrobat.catroid.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Десктопный менеджер проектов. Загружает проект NeoCatroid/Catrobat из
 * директории (формат: code.xml + images/).
 *
 * Поддерживает:
 *  - имя проекта (из code.xml <program> <name>)
 *  - спрайты (каждый <object>), их позицию (<x>, <y>), размер (<size>)
 *  - список look'ов (<looks> <look> <fileName>)
 *
 * Использует стандартный JDK DOM-парсер (javax.xml) — без Android-зависимостей.
 */
object DesktopProjectManager {

    private var currentProject: DesktopProject? = null

    fun getInstance(): DesktopProjectManager = this

    fun loadProject(projectDir: File): DesktopProject? {
        val codeXml = File(projectDir, "code.xml")
        if (!codeXml.exists()) {
            Gdx.app.log(TAG, "code.xml not found in ${projectDir.absolutePath}")
            return null
        }

        val project = DesktopProject(name = projectDir.name, projectDir = projectDir)
        val imagesDir = File(projectDir, "images")

        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(codeXml)
            doc.documentElement.normalize()

            // Имя проекта
            val nameNodes = doc.getElementsByTagName("name")
            if (nameNodes.length > 0) {
                project.name = nameNodes.item(0).textContent.trim()
            }

            // Спрайты — <object>
            val objects = doc.getElementsByTagName("object")
            for (i in 0 until objects.length) {
                val objNode = objects.item(i)
                if (objNode.nodeType != Node.ELEMENT_NODE) continue
                val objEl = objNode as Element

                val spriteName = textOf(objEl, "name")?.trim() ?: "sprite$i"
                val sprite = DesktopSprite(name = spriteName)

                // Позиция, размер и направление
                textOf(objEl, "x")?.toFloatOrNull()?.let { sprite.x = it }
                textOf(objEl, "y")?.toFloatOrNull()?.let { sprite.y = it }
                textOf(objEl, "size")?.toFloatOrNull()?.let { sprite.size = it }
                textOf(objEl, "direction")?.toFloatOrNull()?.let { sprite.direction = it }

                // Look'и
                val looks = objEl.getElementsByTagName("look")
                for (j in 0 until looks.length) {
                    val lookNode = looks.item(j)
                    if (lookNode.nodeType != Node.ELEMENT_NODE) continue
                    val lookEl = lookNode as Element
                    val fileName = textOf(lookEl, "fileName")?.trim()
                    if (!fileName.isNullOrEmpty()) {
                        val look = DesktopLook(
                            name = textOf(lookEl, "name")?.trim() ?: fileName,
                            fileName = fileName
                        )
                        loadTexture(imagesDir, fileName)?.let { look.texture = it }
                        sprite.looks.add(look)
                    }
                }

                project.sprites.add(sprite)
            }

            currentProject = project
            Gdx.app.log(TAG, "Loaded project '${project.name}' with ${project.sprites.size} sprites")
            return project
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Failed to parse project", e)
            return null
        }
    }

    private fun textOf(parent: Element, tag: String): String? {
        val nodes = parent.getElementsByTagName(tag)
        if (nodes.length == 0) return null
        return nodes.item(0).textContent
    }

    private fun loadTexture(imagesDir: File, fileName: String): Texture? {
        val file = File(imagesDir, fileName)
        if (!file.exists()) {
            Gdx.app.log(TAG, "Image not found: ${file.absolutePath}")
            return null
        }
        return try {
            Texture(file.absolutePath)
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Failed to load texture $fileName", e)
            null
        }
    }

    fun getCurrentProject(): DesktopProject? = currentProject

    fun clear() {
        currentProject?.sprites?.forEach { sprite ->
            sprite.looks.forEach { it.texture?.dispose() }
        }
        currentProject = null
    }

    private const val TAG = "DesktopProjectManager"
}