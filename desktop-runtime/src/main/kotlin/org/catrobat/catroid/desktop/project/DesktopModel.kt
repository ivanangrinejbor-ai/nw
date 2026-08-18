package org.catrobat.catroid.desktop.project

import java.io.File

data class DesktopProject(
    val name: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val landscapeMode: Boolean,
    val screenMode: String,
    val scenes: List<DesktopScene>,
    val globalVariables: List<DesktopVariableRef>,
    val globalLists: List<DesktopVariableRef>
) {
    fun startScene(): DesktopScene = scenes.firstOrNull() ?: DesktopScene("", emptyList())
}

data class DesktopScene(
    val name: String,
    val sprites: List<DesktopSprite>
)

data class DesktopSprite(
    val name: String,
    val isBackground: Boolean,
    val looks: List<DesktopLook>,
    val sounds: List<DesktopSound>,
    val scripts: List<DesktopScript>,
    val userVariables: List<DesktopVariableRef>,
    val userLists: List<DesktopVariableRef>
)

data class DesktopLook(
    val name: String,
    val fileName: String
)

data class DesktopSound(
    val name: String,
    val fileName: String
)

data class DesktopScript(
    val type: String,
    val brickList: List<DesktopBrick>,
    val broadcastMessage: String? = null,
    val touchedSpriteName: String? = null,
    val conditionType: String? = null,
    val gamepadButton: String? = null,
    val triggerFormulas: Map<String, DesktopFormula> = emptyMap(),
    val values: Map<String, String> = emptyMap(),
    val variableName: String? = null,
    val sceneName: String? = null
)

data class DesktopVariableRef(
    val name: String,
    val type: String = "UserVariable"
)

data class DesktopBrick(
    val type: String,
    val commentedOut: Boolean = false,
    val fields: Map<String, DesktopFormula> = emptyMap(),
    val simpleValues: Map<String, String> = emptyMap(),
    val children: Map<String, List<DesktopBrick>> = emptyMap(),
    val variableRefs: Map<String, String> = emptyMap(),
    val lookRefs: Map<String, String> = emptyMap(),
    val soundRefs: Map<String, String> = emptyMap(),
    val scriptRefs: Map<String, String> = emptyMap()
) {
    fun field(category: String): DesktopFormula? = fields[category]

    fun value(category: String): String? = simpleValues[category]
}

sealed class DesktopFormulaNode {
    data class Num(val v: Double) : DesktopFormulaNode()
    data class Str(val s: String) : DesktopFormulaNode()
    data class Var(val name: String) : DesktopFormulaNode()
    data class ListRef(val name: String) : DesktopFormulaNode()
    data class Sensor(val sensor: String) : DesktopFormulaNode()
    data class Func(val func: String, val args: List<DesktopFormulaNode>) : DesktopFormulaNode()
    data class Op(val op: String, val l: DesktopFormulaNode?, val r: DesktopFormulaNode?) : DesktopFormulaNode()
    object Null : DesktopFormulaNode()
}

data class DesktopFormula(
    val type: String,
    val value: String,
    val leftChild: DesktopFormulaNode?,
    val rightChild: DesktopFormulaNode?,
    val additionalChildren: List<DesktopFormulaNode>
)