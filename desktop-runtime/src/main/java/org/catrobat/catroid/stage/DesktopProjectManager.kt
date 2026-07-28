package org.catrobat.catroid.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object DesktopProjectManager {

    private var currentProject: DesktopProject? = null

    fun getInstance(): DesktopProjectManager = this

    fun loadProject(projectDir: File): DesktopProject? {
        // TODO: also search for code.xml in subdirectories (e.g. projectName/code.xml)
        val codeXml = File(projectDir, "code.xml")
        if (!codeXml.exists()) {
            Gdx.app.log(TAG, "code.xml not found in ${projectDir.absolutePath}")
            return null
        }

        val project = DesktopProject(name = projectDir.name, projectDir = projectDir)
            val imagesDir = findDir(projectDir, "images") ?: File(projectDir, "images")
        val soundsDir = findDir(projectDir, "sounds") ?: File(projectDir, "sounds")
        project.imagesDir = imagesDir
        project.soundsDir = soundsDir

        // Diagnostic: dump the ACTUAL image filenames on disk so we can compare them to the
        // look <fileName> values in code.xml (which currently resolve to "not found").
        run {
            val sample = mutableListOf<String>()
            val imageDirs = mutableListOf<String>()
            fun walk(d: File, depth: Int) {
                if (depth > 4 || sample.size >= 40) return
                val entries = d.listFiles() ?: return
                for (f in entries) {
                    if (sample.size >= 40) break
                    if (f.isDirectory) {
                        if (f.name.equals("images", true)) imageDirs.add(f.absolutePath)
                        walk(f, depth + 1)
                    } else if (f.name.endsWith(".png", true) || f.name.endsWith(".jpg", true)) {
                        sample.add(f.name)
                    }
                }
            }
            walk(projectDir, 0)
            Gdx.app.log(TAG, "imagesDir resolved to: ${imagesDir.absolutePath} exists=${imagesDir.isDirectory}")
            Gdx.app.log(TAG, "images/ dirs found (first ${imageDirs.size}): $imageDirs")
            Gdx.app.log(TAG, "actual image files (sample ${sample.size}): $sample")
        }

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            val builder = factory.newDocumentBuilder()
            // code.xml declares UTF-8 but may actually be CP1251 (legacy editor exports).
            // Decode with a strict UTF-8 decoder (REPORT) and fall back to CP1251,
            // instead of the platform-default charset (CP1251 on Windows would corrupt UTF-8 files).
            val rawBytes = codeXml.readBytes()
            val xmlText = try {
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(rawBytes)).toString()
            } catch (e: Exception) {
                Gdx.app.log(TAG, "code.xml is not valid UTF-8, decoding as CP1251")
                String(rawBytes, charset("windows-1251"))
            }
            val doc = builder.parse(org.xml.sax.InputSource(java.io.StringReader(xmlText)))
            doc.documentElement.normalize()

            val nameNodes = doc.getElementsByTagName("name")
            if (nameNodes.length > 0) {
                project.name = nameNodes.item(0).textContent.trim()
            }

            val headerNodes = doc.getElementsByTagName("header")
            if (headerNodes.length > 0) {
                val headerEl = headerNodes.item(0) as Element
                headerEl.getAttribute("screenWidth").toIntOrNull()?.let { project.stageWidth = it }
                headerEl.getAttribute("screenHeight").toIntOrNull()?.let { project.stageHeight = it }
            }

            val objects = doc.getElementsByTagName("object")
            // Diagnostic: dump the scene structure so we can see how many scenes exist,
            // their names, and object counts (multi-scene projects are currently flattened).
            run {
                val sceneNodes = doc.getElementsByTagName("scene")
                Gdx.app.log(TAG, "scenes found: ${sceneNodes.length}; total <object> in doc: ${objects.length}")
                for (si in 0 until sceneNodes.length) {
                    val sEl = sceneNodes.item(si) as? Element ?: continue
                    val sName = sEl.getElementsByTagName("name").let { if (it.length > 0) it.item(0).textContent.trim() else "?" }
                    val olEl = sEl.getElementsByTagName("objectList").let { if (it.length > 0) it.item(0) as? Element else null }
                    var objCnt = 0
                    if (olEl != null) {
                        val ch = olEl.childNodes
                        for (k in 0 until ch.length) if (ch.item(k).nodeName == "object") objCnt++
                    }
                    Gdx.app.log(TAG, "  scene[$si] '$sName' directObjects=$objCnt")
                }
            }
            for (i in 0 until objects.length) {
                val objNode = objects.item(i)
                if (objNode.nodeType != Node.ELEMENT_NODE) continue
                val objEl = objNode as Element

                val spriteName = attrOrText(objEl, "name")?.trim() ?: "sprite$i"
                val sprite = DesktopSprite(name = spriteName)

                textOf(objEl, "x")?.toFloatOrNull()?.let { sprite.x = it }
                textOf(objEl, "y")?.toFloatOrNull()?.let { sprite.y = it }
                textOf(objEl, "size")?.toFloatOrNull()?.let { sprite.size = it }
                textOf(objEl, "direction")?.toFloatOrNull()?.let { sprite.direction = it }

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
                        val hitboxNodes = lookEl.getElementsByTagName("hitbox")
                        for (k in 0 until hitboxNodes.length) {
                            val hbNode = hitboxNodes.item(k)
                            if (hbNode.nodeType != Node.ELEMENT_NODE) continue
                            val hbEl = hbNode as Element
                            val hw = attrOrText(hbEl, "width")?.toFloatOrNull() ?: 0f
                            val hh = attrOrText(hbEl, "height")?.toFloatOrNull() ?: 0f
                            if (hw > 0f && hh > 0f) {
                                look.hitboxes.add(
                                    DesktopHitbox(
                                        x = attrOrText(hbEl, "x")?.toFloatOrNull() ?: 0f,
                                        y = attrOrText(hbEl, "y")?.toFloatOrNull() ?: 0f,
                                        width = hw,
                                        height = hh,
                                        rotation = attrOrText(hbEl, "rotation")?.toFloatOrNull() ?: 0f
                                    )
                                )
                            }
                        }
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

    private fun attrOrText(el: Element, tag: String): String? {
        val a = el.getAttribute(tag)
        if (!a.isNullOrBlank()) return a
        return textOf(el, tag)?.trim()
    }

    fun loadTextureLazy(fileName: String): Texture? {
        val project = currentProject ?: return null
        // Look files live in the scene's images dir (projectDir/<scene>/images), which
        // findDir() located at load time. The old code hard-coded projectDir/images and
        // therefore never found scene-nested images -> white screen + endless "not found".
        project.imagesDir?.takeIf { it.isDirectory }?.let { dir ->
            loadTextureFrom(dir, fileName)?.let { return it }
        }
        project.projectDir?.let { root ->
            loadTextureFrom(File(root, "images"), fileName)?.let { return it }
            // Last resort: multi-scene / unexpected nesting - find the file anywhere.
            findFileRecursive(root, fileName, 6)?.let { f -> loadTextureFile(f)?.let { return it } }
            // Fuzzy fallback: a look <fileName> in code.xml often carries a project-global
            // "_#N" index that does NOT match the per-scene file on disk (which uses a local
            // index or no suffix). Retry by the base name (strip the trailing _#digits).
            val base = fileName.replace(Regex("_#\\d+(?=\\.[^.]+$)"), "")
            if (base != fileName) {
                findFileRecursive(root, base, 6)?.let { f -> loadTextureFile(f)?.let { return it } }
            }
        }
        if (loggedMissing.add(fileName)) {
            Gdx.app.log(TAG, "Image not found (after base-name fallback): $fileName")
        }
        return null
    }

    private fun loadTextureFrom(imagesDir: File, fileName: String): Texture? {
        val file = File(imagesDir, fileName)
        if (!file.exists()) return null
        return loadTextureFile(file)
    }

    private fun loadTextureFile(file: File): Texture? {
        return try {
            Texture(Gdx.files.absolute(file.absolutePath))
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Failed to load texture ${file.name}", e)
            null
        }
    }

    private fun findFileRecursive(root: File, name: String, maxDepth: Int): File? {
        if (!root.isDirectory || maxDepth < 0) return null
        root.listFiles()?.forEach { f -> if (f.isFile && f.name == name) return f }
        root.listFiles()?.forEach { f ->
            if (f.isDirectory) findFileRecursive(f, name, maxDepth - 1)?.let { return it }
        }
        return null
    }

    // TODO: limit search depth to prevent unbounded recursion on deeply-nested trees
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
    private val loggedMissing = HashSet<String>()
}