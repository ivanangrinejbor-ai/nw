package org.catrobat.catroid.stage

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import org.w3c.dom.Document
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
                val rawW = attrOrText(headerEl, "screenWidth")
                val rawH = attrOrText(headerEl, "screenHeight")
                Gdx.app.log(TAG, "header raw screenWidth='$rawW' screenHeight='$rawH'")
                rawW?.toIntOrNull()?.let { project.stageWidth = it }
                rawH?.toIntOrNull()?.let { project.stageHeight = it }
            }

            // Scene-aware object selection (mirrors Android): start on the FIRST regular scene
            // (sceneList[0]); a <globalScene>'s sprites go FIRST so their indices stay stable
            // across scene switches. Only the ACTIVE scene is loaded, not all scenes flattened.
            val sel = SceneSelector.selectScene(doc, project.activeSceneName)
            project.sceneNames.clear()
            project.sceneNames.addAll(sel.sceneNames)
            project.hasGlobalScene = sel.hasGlobal
            project.globalSpriteCount = sel.globalCount
            project.activeSceneName = sel.activeSceneName
            Gdx.app.log(TAG, "scenes=${sel.sceneNames.size} active='${sel.activeSceneName}'"
                + " globalSprites=${sel.globalCount} activeObjects=${sel.objectEls.size}")
            // Point media dirs at the ACTIVE scene's own images/sounds (per-scene folders) so a
            // look's exact fileName resolves to THIS scene's image, not another scene's.
            resolveSceneMediaDir(projectDir, sel.activeSceneName, "images")?.let { project.imagesDir = it }
            resolveSceneMediaDir(projectDir, sel.activeSceneName, "sounds")?.let { project.soundsDir = it }
            Gdx.app.log(TAG, "active scene imagesDir=${project.imagesDir?.absolutePath}")
            Gdx.app.log(TAG, "active imagesDir files: ${project.imagesDir?.listFiles()?.map { it.name }?.sorted()?.take(40)}")
            for ((i, objEl) in sel.objectEls.withIndex()) {
                project.sprites.add(parseSpriteElement(objEl, i))
            }

            currentProject = project
            Gdx.app.log(TAG, "Loaded project '${project.name}' with ${project.sprites.size} sprites")
            return project
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Failed to parse project", e)
            return null
        }
    }

    // Parses one <object> element into a DesktopSprite (shared by initial load + scene switch).
    private fun parseSpriteElement(objEl: Element, index: Int): DesktopSprite {
        val spriteName = attrOrText(objEl, "name")?.trim() ?: "sprite$index"
        val sprite = DesktopSprite(name = spriteName)
        attrOrText(objEl, "x")?.toFloatOrNull()?.let { sprite.x = it }
        attrOrText(objEl, "y")?.toFloatOrNull()?.let { sprite.y = it }
        attrOrText(objEl, "size")?.toFloatOrNull()?.let { sprite.size = it }
        attrOrText(objEl, "direction")?.toFloatOrNull()?.let { sprite.direction = it }
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
        return sprite
    }

    /**
     * Switches the project's ACTIVE scene to [sceneName] (mirrors Android startScene): keeps the
     * global-scene sprite prefix (indices < globalSpriteCount) and their runtime state, and
     * replaces the scene-local tail with the requested scene's sprites. Returns the FULL ordered
     * object elements of the newly active scene (global first, then scene-local) so the caller
     * (script engine) can rebuild scripts with matching indices, or null if it cannot be loaded.
     */
    fun activateScene(project: DesktopProject, sceneName: String?): List<Element>? {
        val dir = project.projectDir ?: return null
        val doc = parseCodeXml(dir) ?: return null
        val sel = SceneSelector.selectScene(doc, sceneName)
        val keep = project.globalSpriteCount.coerceIn(0, project.sprites.size)
        while (project.sprites.size > keep) {
            val removed = project.sprites.removeAt(project.sprites.size - 1)
            removed.looks.forEach { it.texture?.dispose() }
        }
        val sceneLocalEls = sel.objectEls.drop(sel.globalCount)
        for ((offset, el) in sceneLocalEls.withIndex()) {
            project.sprites.add(parseSpriteElement(el, keep + offset))
        }
        project.activeSceneName = sel.activeSceneName
        resolveSceneMediaDir(dir, sel.activeSceneName, "images")?.let { project.imagesDir = it }
        resolveSceneMediaDir(dir, sel.activeSceneName, "sounds")?.let { project.soundsDir = it }
        loggedMissing.clear()
        Gdx.app.log(TAG, "activateScene -> '${sel.activeSceneName}' sceneLocal=${sceneLocalEls.size} globalPrefix=$keep imagesDir=${project.imagesDir?.absolutePath}")
        return sel.objectEls
    }

    // Parses code.xml into a DOM (UTF-8 with CP1251 fallback for legacy exports).
    fun parseCodeXml(projectDir: File): Document? {
        val codeXml = File(projectDir, "code.xml")
        if (!codeXml.exists()) return null
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            val builder = factory.newDocumentBuilder()
            val rawBytes = codeXml.readBytes()
            val xmlText = try {
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(rawBytes)).toString()
            } catch (e: Exception) {
                String(rawBytes, charset("windows-1251"))
            }
            val doc = builder.parse(org.xml.sax.InputSource(java.io.StringReader(xmlText)))
            doc.documentElement.normalize()
            doc
        } catch (e: Exception) {
            Gdx.app.error(TAG, "parseCodeXml failed", e)
            null
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
            val t = Texture(Gdx.files.absolute(file.absolutePath))
            if (loggedLoaded.add(file.name)) Gdx.app.log(TAG, "texture OK: ${file.name} (${t.width}x${t.height}) glHandle=${t.textureObjectHandle}")
            t
        } catch (e: Exception) {
            if (loggedLoaded.add("FAIL:" + file.name)) Gdx.app.error(TAG, "texture FAIL: ${file.absolutePath} -> ${e.message}")
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

    // Resolves a scene's own media dir: projectDir/<sceneFolder>/<media>. The scene folder is the
    // scene name, possibly sanitized during zip extraction (trailing space/dots + illegal chars ->
    // '_'). Tries exact + sanitized, then a sanitized-name scan of direct subdirs. null if absent.
    fun resolveSceneMediaDir(projectDir: File, sceneName: String?, media: String): File? {
        if (sceneName.isNullOrEmpty()) return null
        for (c in linkedSetOf(sceneName, sanitizeFolder(sceneName))) {
            val d = File(File(projectDir, c), media)
            if (d.isDirectory) return d
        }
        projectDir.listFiles()?.forEach { sub ->
            if (sub.isDirectory && sanitizeFolder(sub.name) == sanitizeFolder(sceneName)) {
                val d = File(sub, media)
                if (d.isDirectory) return d
            }
        }
        return null
    }

    // Mirrors the zip-extraction sanitizer for a single folder segment (see DesktopStage).
    private fun sanitizeFolder(name: String): String {
        val cleaned = buildString(name.length) {
            for (c in name) append(
                if (c.code < 0x20 || c == '<' || c == '>' || c == ':' || c == '"' ||
                    c == '|' || c == '?' || c == '*' || c == '\\' || c == '/'
                ) '_' else c
            )
        }.trimEnd(' ', '.')
        return if (cleaned.isEmpty()) "_" else cleaned
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
    private val loggedLoaded = HashSet<String>()
}

/**
 * Selects the objects to load for the ACTIVE scene, mirroring Android's scene model:
 * a project has an ordered list of regular <scene> elements (sceneList) plus an optional
 * <globalScene>. Only ONE regular scene is active at a time; its objects are combined with
 * the global scene's objects (global FIRST, so global sprite indices stay stable across
 * scene switches). Both the sprite loader and the script engine call this so their object
 * ordering — and therefore sprite indices — match exactly.
 */
object SceneSelector {
    class Selection(
        val objectEls: List<Element>,
        val globalCount: Int,
        val activeSceneName: String,
        val sceneNames: List<String>,
        val hasGlobal: Boolean
    )

    fun selectScene(doc: Document, requestedSceneName: String?): Selection {
        val regularScenes = regularSceneEls(doc)
        val sceneNames = regularScenes.map { sceneName(it) }
        val globalEl = globalSceneEl(doc)
        val globalObjs = directObjects(globalEl)
        val activeEl = requestedSceneName?.let { req -> regularScenes.firstOrNull { sceneName(it) == req } }
            ?: regularScenes.firstOrNull()
        val activeName = activeEl?.let { sceneName(it) } ?: (requestedSceneName ?: "")
        val sceneObjs = directObjects(activeEl)
        val all = ArrayList<Element>(globalObjs.size + sceneObjs.size)
        all.addAll(globalObjs)
        all.addAll(sceneObjs)
        return Selection(all, globalObjs.size, activeName, sceneNames, globalEl != null)
    }

    // Regular <scene> elements (excludes anything nested under <globalScene>), document order.
    private fun regularSceneEls(doc: Document): List<Element> {
        val out = ArrayList<Element>()
        val nodes = doc.getElementsByTagName("scene")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            if (isInsideGlobalScene(el)) continue
            out.add(el)
        }
        return out
    }

    private fun isInsideGlobalScene(el: Element): Boolean {
        var p: Node? = el.parentNode
        while (p != null) {
            if (p.nodeName == "globalScene") return true
            p = p.parentNode
        }
        return false
    }

    private fun globalSceneEl(doc: Document): Element? {
        val nodes = doc.getElementsByTagName("globalScene")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            if (directChild(el, "objectList") != null || directChild(el, "name") != null) return el
        }
        return null
    }

    private fun sceneName(sceneEl: Element?): String {
        sceneEl ?: return ""
        return directChild(sceneEl, "name")?.textContent?.trim() ?: ""
    }

    // Direct <object> children of a scene's (or global scene's) <objectList>.
    private fun directObjects(sceneEl: Element?): List<Element> {
        sceneEl ?: return emptyList()
        val objectList = directChild(sceneEl, "objectList") ?: return emptyList()
        val out = ArrayList<Element>()
        val ch = objectList.childNodes
        for (i in 0 until ch.length) {
            val n = ch.item(i)
            if (n.nodeType == Node.ELEMENT_NODE && n.nodeName == "object") out.add(n as Element)
        }
        return out
    }

    private fun directChild(parent: Element, tag: String): Element? {
        val ch = parent.childNodes
        for (i in 0 until ch.length) {
            val n = ch.item(i)
            if (n.nodeType == Node.ELEMENT_NODE && n.nodeName == tag) return n as Element
        }
        return null
    }
}