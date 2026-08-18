package org.catrobat.catroid.desktop

import org.catrobat.catroid.desktop.project.DesktopBrick
import org.catrobat.catroid.desktop.project.DesktopFormula
import org.catrobat.catroid.desktop.project.DesktopFormulaNode
import org.catrobat.catroid.desktop.project.DesktopProject
import org.catrobat.catroid.desktop.project.DesktopScene
import org.catrobat.catroid.desktop.project.DesktopScript
import org.catrobat.catroid.desktop.project.DesktopSprite
import org.catrobat.catroid.desktop.project.DesktopVariableRef
import org.catrobat.catroid.desktop.stage.DesktopEngine
import java.io.File

fun main() {
    var ok = true
    fun check(cond: Boolean, msg: String) {
        if (!cond) { ok = false; println("FAIL: $msg") }
    }
    fun formulaNum(v: String) = DesktopFormula("NUMBER", v, null, null, emptyList())
    fun formulaVar(name: String) = DesktopFormula("USER_VARIABLE", name, null, null, emptyList())
    fun op(name: String, l: DesktopFormulaNode, r: DesktopFormulaNode) =
        DesktopFormula("OPERATOR", name, l, r, emptyList())
    fun setVar(varName: String, f: DesktopFormula) = DesktopBrick(
        "SetVariableBrick", fields = mapOf("VARIABLE" to f),
        variableRefs = mapOf("userVariable" to varName)
    )
    fun changeVar(varName: String, f: DesktopFormula) = DesktopBrick(
        "ChangeVariableBrick", fields = mapOf("VARIABLE_CHANGE" to f),
        variableRefs = mapOf("userVariable" to varName)
    )
    fun projectWith(sprite: DesktopSprite): DesktopProject = DesktopProject(
        "t", 100, 100, false, "STRETCH",
        listOf(DesktopScene("", listOf(sprite))), emptyList(), emptyList()
    )
    fun engineFor(project: DesktopProject): DesktopEngine {
        val e = DesktopEngine(project, File(System.getProperty("java.io.tmpdir")))
        e.start()
        return e
    }
    fun runUntilDone(e: DesktopEngine, maxSec: Float = 5f) {
        var t = 0f
        while (t < maxSec) {
            e.tick(0.01f)
            t += 0.01f
            if (e.variables["done"] != null) return
        }
    }

    // 1. WaitWhileBrick: ждать пока flag==1, потом выполнить SetVariable
    run {
        val brick = DesktopBrick("WaitWhileBrick", fields = mapOf("IF_CONDITION" to formulaVar("flag")))
        val done = setVar("done", formulaNum("1"))
        val s = DesktopSprite("S1", false, emptyList(), emptyList(),
            listOf(DesktopScript("StartScript", listOf(brick, done))), emptyList(), emptyList())
        val e = engineFor(projectWith(s))
        e.variables["flag"] = 1.0
        e.tick(0.1f); e.tick(0.1f)
        check(e.variables["done"] == null, "WaitWhile: выполнился пока flag=1")
        e.variables["flag"] = 0.0
        e.tick(0.01f)
        check(e.variables["done"] == 1.0, "WaitWhile: не продолжил после flag=0, done=${e.variables["done"]}")
    }

    // 2. RepeatWhileBrick: пока count<3 → инкремент; ожидание count=3
    run {
        val cond = formulaVar("count")
        val body = listOf(changeVar("count", formulaNum("1")))
        val b = DesktopBrick("RepeatWhileBrick",
            fields = mapOf("REPEAT_UNTIL_CONDITION" to cond), children = mapOf("loopBricks" to body))
        val s = DesktopSprite("S1", false, emptyList(), emptyList(),
            listOf(DesktopScript("StartScript", listOf(b))), emptyList(), emptyList())
        val e = engineFor(projectWith(s))
        e.variables["count"] = 0.0
        runUntilDone(e)
        check(e.variables["count"] == 3.0, "RepeatWhile: count=${e.variables["count"]} (ожидалось 3)")
    }

    // 3. AsyncRepeatBrick: 3 параллельные итерации тела
    run {
        val body = listOf(changeVar("count", formulaNum("1")))
        val b = DesktopBrick("AsyncRepeatBrick",
            fields = mapOf("TIMES_TO_REPEAT" to formulaNum("3")),
            simpleValues = mapOf("isLoopDelay" to "false"),
            children = mapOf("loopBricks" to body))
        val done = setVar("done", formulaNum("1"))
        val s = DesktopSprite("S1", false, emptyList(), emptyList(),
            listOf(DesktopScript("StartScript", listOf(b, done))), emptyList(), emptyList())
        val e = engineFor(projectWith(s))
        e.variables["count"] = 0.0
        runUntilDone(e)
        check(e.variables["done"] == 1.0, "AsyncRepeat: done=${e.variables["done"]}")
        check(e.variables["count"] == 3.0, "AsyncRepeat: count=${e.variables["count"]} (ожидалось 3)")
    }

    // 4. IntervalRepeatBrick: 3 итерации с интервалом 0.1с
    run {
        val body = listOf(changeVar("count", formulaNum("1")))
        val b = DesktopBrick("IntervalRepeatBrick",
            fields = mapOf("TIMES_TO_REPEAT" to formulaNum("3"), "INTERVAL" to formulaNum("0.1")),
            children = mapOf("loopBricks" to body))
        val done = setVar("done", formulaNum("1"))
        val s = DesktopSprite("S1", false, emptyList(), emptyList(),
            listOf(DesktopScript("StartScript", listOf(b, done))), emptyList(), emptyList())
        val e = engineFor(projectWith(s))
        e.variables["count"] = 0.0
        e.tick(0.08f)
        check(e.variables["count"] == 0.0, "IntervalRepeat: стартовая пауза не сработала, count=${e.variables["count"]}")
        runUntilDone(e)
        check(e.variables["count"] == 3.0, "IntervalRepeat: count=${e.variables["count"]} (ожидалось 3)")
        check(e.variables["done"] == 1.0, "IntervalRepeat: done=${e.variables["done"]}")
    }

    // 5. ForItemInUserListBrick: элементы a,b,c → last=c
    run {
        val body = listOf(setVar("last", formulaVar("item")))
        val b = DesktopBrick("ForItemInUserListBrick",
            variableRefs = mapOf("userList" to "items", "userVariable" to "item"),
            children = mapOf("loopBricks" to body))
        val done = setVar("done", formulaNum("1"))
        val s = DesktopSprite("S1", false, emptyList(), emptyList(),
            listOf(DesktopScript("StartScript", listOf(b, done))),
            emptyList(), listOf(DesktopVariableRef("items", "UserList")))
        val e = engineFor(projectWith(s))
        e.lists["items"]!!.addAll(listOf("a", "b", "c"))
        runUntilDone(e)
        check(e.variables["last"] == "c", "ForItem: last=${e.variables["last"]} (ожидалось c)")
        check(e.variables["done"] == 1.0, "ForItem: done=${e.variables["done"]}")
    }

    println(if (ok) "CONTROL_SMOKE_OK" else "CONTROL_SMOKE_FAIL")
}
