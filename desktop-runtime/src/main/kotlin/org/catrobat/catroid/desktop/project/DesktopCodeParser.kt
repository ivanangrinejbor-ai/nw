package org.catrobat.catroid.desktop.project

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Парсер code.xml (XStream-формат, который экспортирует редактор Catroid).
 * Поддерживает: вложенные scenes/scene/objectList/object, brickList, legacy
 * <formulaList><formula category="..."> и XStream <formulaMap>, контейнеры
 * loopBricks/ifBricks/elseBricks, переменные (вложенные или через reference).
 */
class DesktopCodeParser {

    fun parse(codeXml: File): DesktopProject {
        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isIgnoringComments = true
        }.newDocumentBuilder().parse(codeXml)

        val root = doc.documentElement // <program>
        val header = firstChild(root, "header")

        val name = childText(firstChild(root, "header"), "programName") ?: "Project"
        val screenWidth = text(firstChild(header, "screenWidth"))?.toIntOrNull() ?: 1280
        val screenHeight = text(firstChild(header, "screenHeight"))?.toIntOrNull() ?: 720
        val landscapeMode = text(firstChild(header, "landscapeMode")) == "true"
        val screenMode = text(firstChild(header, "screenMode")) ?: "STRETCH"

        val scenes = mutableListOf<DesktopScene>()
        for (sceneEl in children(firstChild(root, "scenes"), "scene")) {
            scenes += parseScene(sceneEl)
        }
        if (scenes.isEmpty()) {
            // flat fallback: <objectList> прямо в program
            val flat = children(root, "objectList")
            if (flat.isNotEmpty()) {
                scenes += parseSceneFromObjects(flat, "")
            }
        }

        val globalVars = parseVariableList(root, "programVariableList")
        val globalLists = parseVariableList(root, "programListOfLists")

        return DesktopProject(name, screenWidth, screenHeight, landscapeMode, screenMode, scenes, globalVars, globalLists)
    }

    private fun parseScene(sceneEl: Element): DesktopScene {
        val name = text(firstChild(sceneEl, "name")) ?: ""
        val objects = children(firstChild(sceneEl, "objectList"), "object")
        return parseSceneFromObjects(objects, name)
    }

    private fun parseSceneFromObjects(objectEls: List<Element>, sceneName: String): DesktopScene {
        val sprites = mutableListOf<DesktopSprite>()
        var index = 0
        for (obj in objectEls) {
            val type = obj.getAttribute("type")
            if (type.isNotEmpty() && type != "Sprite") continue
            val spriteName = obj.getAttribute("name").ifEmpty { "sprite${index}" }
            sprites += parseSprite(obj, spriteName, isBackground = index == 0)
            index++
        }
        return DesktopScene(sceneName, sprites)
    }

    private fun parseSprite(obj: Element, name: String, isBackground: Boolean): DesktopSprite {
        val looks = mutableListOf<DesktopLook>()
        val lookListEl = firstChild(obj, "lookList")
        for (lookEl in children(lookListEl, "look")) {
            val lookName = text(firstChild(lookEl, "name")) ?: "look${looks.size}"
            val fileName = text(firstChild(lookEl, "fileName")) ?: ""
            looks += DesktopLook(lookName, fileName)
        }

        val sounds = mutableListOf<DesktopSound>()
        val soundListEl = firstChild(obj, "soundList")
        for (soundEl in children(soundListEl, "sound")) {
            val soundName = text(firstChild(soundEl, "name")) ?: "sound${sounds.size}"
            val fileName = text(firstChild(soundEl, "fileName")) ?: ""
            sounds += DesktopSound(soundName, fileName)
        }

        val scripts = mutableListOf<DesktopScript>()
        for (scriptEl in children(firstChild(obj, "scriptList"), "script")) {
            scripts += parseScript(scriptEl)
        }

        val userVariables = parseVariableList(obj, "userVariables")
        val userLists = parseVariableList(obj, "userLists")

        return DesktopSprite(name, isBackground, looks, sounds, scripts, userVariables, userLists)
    }

    private fun parseScript(scriptEl: Element): DesktopScript = parseScriptElement(scriptEl)

    fun parseScriptElement(scriptEl: Element): DesktopScript {
        val type = scriptEl.getAttribute("type")
        val bricks = mutableListOf<DesktopBrick>()
        for (brickEl in children(firstChild(scriptEl, "brickList"), "brick")) {
            bricks += parseBrick(brickEl)
        }

        var broadcastMessage: String? = null
        val broadcastEl = firstChild(scriptEl, "broadcastMessage")
        if (broadcastEl != null) broadcastMessage = text(broadcastEl)

        var touchedSprite: String? = null
        val touchedEl = firstChild(scriptEl, "spriteToTouchName")
        if (touchedEl != null) touchedSprite = text(touchedEl)

        val triggerFormulas = mutableMapOf<String, DesktopFormula>()
        for (formulaEl in children(firstChild(scriptEl, "formulaList"), "formula")) {
            val category = formulaEl.getAttribute("category")
            if (category.isNotEmpty()) triggerFormulas[category] = parseFormula(formulaEl)
        }
        val formulaMapEl = firstChild(scriptEl, "formulaMap")
        if (formulaMapEl != null) {
            for (entry in children(formulaMapEl, "entry")) {
                val key = text(firstChild(entry, "key")) ?: continue
                val valueEl = firstChild(entry, "value")
                val formulaEl = firstChild(valueEl, "formula") ?: valueEl
                if (formulaEl == null || triggerFormulas.containsKey(key)) continue
                triggerFormulas[key] = parseFormula(formulaEl)
            }
        }

        val values = mutableMapOf<String, String>()
        for (child in directChildren(scriptEl)) {
            if (child !is Element) continue
            val tag = child.tagName
            if (tag in SCRIPT_STRUCTURAL_TAGS) continue
            if (child.childNodes.length == 1 && child.firstChild.nodeType == Node.TEXT_NODE) {
                val v = child.textContent
                if (v.isNotEmpty()) values[tag] = v
            }
        }

        val sceneName = values["sceneName"]

        var variableName: String? = null
        val userVarEl = firstChild(scriptEl, "userVariable")
        if (userVarEl != null) {
            variableName = parseVariableName(userVarEl, scriptEl)
        }

        return DesktopScript(
            type = type,
            brickList = bricks,
            broadcastMessage = broadcastMessage,
            touchedSpriteName = touchedSprite,
            triggerFormulas = triggerFormulas,
            values = values,
            variableName = variableName,
            sceneName = sceneName
        )
    }

    private fun parseBrick(brickEl: Element): DesktopBrick {
        val type = brickEl.getAttribute("type")
        val commentedOut = text(firstChild(brickEl, "commentedOut")) == "true"

        val fields = mutableMapOf<String, DesktopFormula>()
        for (formulaEl in children(firstChild(brickEl, "formulaList"), "formula")) {
            val category = formulaEl.getAttribute("category")
            if (category.isNotEmpty()) {
                fields[category] = parseFormula(formulaEl)
            }
        }

        // XStream <formulaMap><entry><key>..</key><value><formula>..</formula></value></entry>
        val formulaMapEl = firstChild(brickEl, "formulaMap")
        if (formulaMapEl != null) {
            for (entry in children(formulaMapEl, "entry")) {
                val key = text(firstChild(entry, "key")) ?: continue
                val valueEl = firstChild(entry, "value")
                val formulaEl = firstChild(valueEl, "formula") ?: valueEl
                if (formulaEl == null || fields.containsKey(key)) continue
                fields[key] = parseFormula(formulaEl)
            }
        }

        val simpleValues = mutableMapOf<String, String>()
        for (child in directChildren(brickEl)) {
            if (child !is Element) continue
            val tag = child.tagName
            if (tag in STRUCTURAL_TAGS || tag == "commentedOut" || tag == "brickId") continue
            if (child.childNodes.length == 1 && child.firstChild.nodeType == Node.TEXT_NODE) {
                val v = child.textContent
                if (v.isNotEmpty()) simpleValues[tag] = v
            }
        }

        val childrenMap = mutableMapOf<String, List<DesktopBrick>>()
        for (child in directChildren(brickEl)) {
            if (child !is Element) continue
            val tag = child.tagName
            if (tag.endsWith("Bricks") && tag !in SKIP_CONTAINER_TAGS) {
                val list = children(child, "brick").map { parseBrick(it) }
                if (list.isNotEmpty() || tag == "loopBricks") {
                    childrenMap[tag] = list
                }
            }
        }

        val varRefs = mutableMapOf<String, String>()
        val lookRefs = mutableMapOf<String, String>()
        val soundRefs = mutableMapOf<String, String>()
        val scriptRefs = mutableMapOf<String, String>()

        for (child in directChildren(brickEl)) {
            if (child !is Element) continue
            when (child.tagName) {
                "userVariable" -> {
                    val ref = child.getAttribute("reference")
                    val name = parseVariableName(child, brickEl)
                    if (name != null) varRefs["userVariable"] = name
                    else if (ref.isNotEmpty()) varRefs["userVariable"] = "@ref:$ref"
                }
                "userList" -> {
                    val ref = child.getAttribute("reference")
                    val name = parseVariableName(child, brickEl)
                    if (name != null) varRefs["userList"] = name
                    else if (ref.isNotEmpty()) varRefs["userList"] = "@ref:$ref"
                }
                "look", "lookRef", "lookData" -> {
                    val name = parseLookName(child, brickEl)
                    if (name != null) lookRefs[child.tagName] = name
                }
                "sound", "soundRef", "soundInfo" -> {
                    val name = parseSoundName(child, brickEl)
                    if (name != null) soundRefs[child.tagName] = name
                }
                "scriptRef", "userDefinedBrick" -> {
                    val ref = child.getAttribute("reference")
                    scriptRefs[child.tagName] = if (ref.isNotEmpty()) "@ref:$ref" else (text(child) ?: "")
                }
            }
        }

        return DesktopBrick(
            type = type,
            commentedOut = commentedOut,
            fields = fields,
            simpleValues = simpleValues,
            children = childrenMap,
            variableRefs = varRefs,
            lookRefs = lookRefs,
            soundRefs = soundRefs,
            scriptRefs = scriptRefs
        )
    }

    private fun parseVariableName(el: Element, context: Element): String? {
        // <userVariable type="UserVariable" serialization="custom"><userVariable><default><name>..</name></default></userVariable></userVariable>
        val nestedName = firstChild(firstChild(firstChild(el, "userVariable"), "default"), "name")
        if (nestedName != null) {
            val n = text(nestedName)
            if (!n.isNullOrEmpty()) return n
        }
        val ref = el.getAttribute("reference")
        if (ref.isNotEmpty()) {
            val resolved = resolveReference(context, ref)
            val n = resolved?.let { parseVariableName(it, resolved) }
            if (n != null) return n
        }
        return null
    }

    private fun parseLookName(el: Element, context: Element): String? {
        val ref = el.getAttribute("reference")
        if (ref.isNotEmpty()) {
            val resolved = resolveReference(context, ref)
            if (resolved != null) return text(firstChild(resolved, "name"))
        }
        val nameEl = firstChild(el, "name")
        if (nameEl != null) return text(nameEl)
        return null
    }

    private fun parseSoundName(el: Element, context: Element): String? {
        val ref = el.getAttribute("reference")
        if (ref.isNotEmpty()) {
            val resolved = resolveReference(context, ref)
            if (resolved != null) return text(firstChild(resolved, "name"))
        }
        val nameEl = firstChild(el, "name")
        if (nameEl != null) return text(nameEl)
        return null
    }

    private fun parseFormula(formulaEl: Element): DesktopFormula {
        val type = text(firstChild(formulaEl, "type")) ?: "NULL"
        val value = text(firstChild(formulaEl, "value")) ?: ""

        val leftChild = parseChildFormula(formulaEl, "leftChild")
        val rightChild = parseChildFormula(formulaEl, "rightChild")
        val additional = mutableListOf<DesktopFormulaNode>()
        val additionalEl = firstChild(formulaEl, "additionalChildren")
        if (additionalEl != null) {
            for (f in children(additionalEl, "formula")) {
                additional += parseFormula(f).toNode()
            }
        }

        return DesktopFormula(type, value, leftChild, rightChild, additional)
    }

    private fun parseChildFormula(parent: Element, tag: String): DesktopFormulaNode? {
        val childEl = firstChild(parent, tag)
        if (childEl == null) return null
        val formulaEl = firstChild(childEl, "formula") ?: childEl
        return parseFormula(formulaEl).toNode()
    }

    private fun DesktopFormula.toNode(): DesktopFormulaNode {
        return when (type) {
            "NUMBER" -> DesktopFormulaNode.Num(value.toDoubleOrNull() ?: 0.0)
            "STRING" -> DesktopFormulaNode.Str(value)
            "OPERATOR" -> DesktopFormulaNode.Op(value, leftChild, rightChild)
            "FUNCTION" -> DesktopFormulaNode.Func(
                value,
                listOfNotNull(leftChild, rightChild) + additionalChildren
            )
            "SENSOR" -> DesktopFormulaNode.Sensor(value)
            "USER_VARIABLE" -> DesktopFormulaNode.Var(value)
            "USER_LIST" -> DesktopFormulaNode.ListRef(value)
            "BRACKET" -> rightChild ?: leftChild ?: DesktopFormulaNode.Null
            "COLLISION_FORMULA" -> DesktopFormulaNode.Num(value.toDoubleOrNull() ?: 0.0)
            "USER_DEFINED_BRICK_INPUT", "USER_DEFINED_BRICK_ARGUMENT" -> DesktopFormulaNode.Var(value)
            "STRING_NUMBER_CONVERSION" -> leftChild ?: DesktopFormulaNode.Null
            else -> DesktopFormulaNode.Null
        }
    }

    private fun parseVariableList(container: Element?, tag: String): List<DesktopVariableRef> {
        val result = mutableListOf<DesktopVariableRef>()
        val listEl = firstChild(container, tag)
        if (listEl == null) return result
        for (child in directChildren(listEl)) {
            if (child !is Element) continue
            val ref = child.getAttribute("reference")
            if (ref.isNotEmpty()) {
                val resolved = resolveReference(listEl, ref)
                val name = resolved?.let { parseVariableName(it, resolved) }
                if (name != null) result += DesktopVariableRef(name, child.tagName)
            } else {
                val name = text(firstChild(child, "name")) ?: parseVariableName(child, child)
                if (name != null) result += DesktopVariableRef(name, child.tagName)
            }
        }
        return result
    }

    private fun resolveReference(from: Element, ref: String): Element? {
        var current: Node? = from
        val segments = ref.split("/")
        for (seg in segments) {
            if (seg == ".") continue
            if (seg == "..") {
                current = current?.parentNode
                if (current is Element) continue else return null
            }
            if (seg.isEmpty()) continue
            val found = children(current as? Element, seg)
            if (found.isEmpty()) return null
            current = found.first()
        }
        return current as? Element
    }

    private fun children(el: Element?, tag: String): List<Element> {
        if (el == null) return emptyList()
        return directChildren(el).filterIsInstance<Element>().filter { it.tagName == tag }
    }

    private fun directChildren(el: Element?): List<Node> {
        if (el == null) return emptyList()
        val nodes = mutableListOf<Node>()
        var child = el.firstChild
        while (child != null) {
            nodes += child
            child = child.nextSibling
        }
        return nodes
    }

    private fun firstChild(el: Element?, tag: String): Element? {
        return children(el, tag).firstOrNull()
    }

    private fun childText(el: Element?, tag: String): String? = text(firstChild(el, tag))

    private fun text(el: Element?): String? {
        if (el == null) return null
        val t = el.textContent?.trim()
        return if (t.isNullOrEmpty()) null else t
    }

    companion object {
        private val STRUCTURAL_TAGS = setOf(
            "formulaList", "formulaMap", "userVariable", "userList", "look", "lookRef",
            "lookData", "sound", "soundRef", "soundInfo", "scriptRef", "loopBricks",
            "ifBricks", "elseBricks", "userDefinedBrick", "setVariable", "listRef"
        )
        private val SKIP_CONTAINER_TAGS = setOf("userDefinedBrick")
        private val SCRIPT_STRUCTURAL_TAGS = setOf(
            "brickList", "formulaList", "formulaMap", "userVariable", "userList",
            "broadcastMessage", "spriteToTouchName", "commentedOut",
            "scriptId", "condition"
        )
    }
}

/**
 * Парсит .neoscript модуль: XStream-XML с корнем <neoscript>,
 * внутри <scripts><script type="...">...</script></scripts>.
 * Возвращает список скриптов (тот же формат, что в code.xml).
 */
fun parseNeoScripts(file: File): List<DesktopScript> {
    val doc = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        isIgnoringComments = true
    }.newDocumentBuilder().parse(file)
    val root = doc.documentElement // <neoscript>
    val scripts = mutableListOf<DesktopScript>()
    val parser = DesktopCodeParser()
    for (i in 0 until root.childNodes.length) {
        val child = root.childNodes.item(i)
        if (child !is Element) continue
        if (child.tagName != "scripts") continue
        for (j in 0 until child.childNodes.length) {
            val scriptChild = child.childNodes.item(j)
            if (scriptChild !is Element || scriptChild.tagName != "script") continue
            scripts += parser.parseScriptElement(scriptChild)
        }
    }
    return scripts
}