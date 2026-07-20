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
        // Catrobat projects may nest media under a project-name folder
        // (e.g. <project>/images, <project>/sounds), so resolve recursively.
        val imagesDir = findDir(projectDir, "images") ?: File(projectDir, "images")
        val soundsDir = findDir(projectDir, "sounds") ?: File(projectDir, "sounds")
        project.imagesDir = imagesDir
        project.soundsDir = soundsDir

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
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

                val spriteName = attrOrText(objEl, "name")?.trim() ?: "sprite$i"
                val sprite = DesktopSprite(name = spriteName)

                // Позиция, размер и направление
                textOf(objEl, "x")?.toFloatOrNull()?.let { sprite.x = it }
                textOf(objEl, "y")?.toFloatOrNull()?.let { sprite.y = it }
                textOf(objEl, "size")?.toFloatOrNull()?.let { sprite.size = it }
                textOf(objEl, "direction")?.toFloatOrNull()?.let { sprite.direction = it }

                // Look'и (lazy — textures loaded on first access)
                val looks = objEl.getElementsByTagName("look")
                for (j in 0 until looks.length) {
                    val lookNode = looks.item(j)
                    if (lookNode.nodeType != Node.ELEMENT_NODE) continue
                    val lookEl = lookNode as Element
                    val fileName = attrOrText(lookEl, "fileName")?.trim()
                    if (!fileName.isNullOrEmpty()) {
                        val look = DesktopLook(
                            name = attrOrText(lookEl, "name")?.trim() ?: fileName,
                            fileName = fileName
                        )
                        // Texture will be loaded lazily on first render access
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

    /**
     * Catrobat's code.xml stores many fields BOTH as XML attributes and/or as
     * child elements depending on the exporter/version. Read the attribute first,
     * then fall back to the child-element text, so look/sprite metadata
     * (e.g. <look fileName="...">, <object name="...">) resolves either way.
     */
    private fun attrOrText(el: Element, tag: String): String? {
        val a = el.getAttribute(tag)
        if (!a.isNullOrBlank()) return a
        return textOf(el, tag)?.trim()
    }

    /**
     * Lazily load a texture by fileName from the current project's images directory.
     * Called by [DesktopLook.texture] getter on first access.
     */
    fun loadTextureLazy(fileName: String): Texture? {
        val dir = currentProject?.projectDir ?: return null
        val imagesDir = File(dir, "images")
        return loadTexture(imagesDir, fileName)
    }

    private fun loadTexture(imagesDir: File, fileName: String): Texture? {
        val file = File(imagesDir, fileName)
        if (!file.exists()) {
            Gdx.app.log(TAG, "Image not found: ${file.absolutePath}")
            return null
        }
        return try {
            Texture(Gdx.files.absolute(file.absolutePath))
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Failed to load texture $fileName", e)
            null
        }
    }

    /**
     * Depth-limited recursive search for a subdirectory by [name] under [root].
     * Catrobat zips can place media under a project-name folder, so a flat
     * `File(root, "images")` lookup misses it.
     */
    private fun findDir(root: File, name: String, maxDepth: Int = 6): File? {
        if (!root.isDirectory || maxDepth < 0) return null
        root.listFiles()?.forEach { f ->
            if (f.isDirectory && f.name.equals(name, ignoreCase = true)) return f
        }
        root.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                val found = findDir(f, name, maxDepth - 1)
                if (found != null) return found
            }
        }
        return null
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