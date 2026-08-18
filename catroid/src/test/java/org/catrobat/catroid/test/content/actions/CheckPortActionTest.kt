package org.catrobat.catroid.test.content.actions

import org.catrobat.catroid.content.actions.CheckPortAction
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.ServerSocket

@RunWith(JUnit4::class)
class CheckPortActionTest {

	private val occupiedSockets = mutableListOf<ServerSocket>()

	private fun occupyPort(): Int {
		val socket = ServerSocket(0)
		occupiedSockets.add(socket)
		return socket.localPort
	}

	private fun freePort(): Int {
		val socket = ServerSocket(0)
		val port = socket.localPort
		socket.close()
		return port
	}

	private fun runAction(portFormula: Formula?, variable: UserVariable?) {
		val action = CheckPortAction()
		action.port = portFormula
		action.variable = variable
		action.act(1f)
	}

	private fun assertResult(portFormula: Formula?, expected: String, variable: UserVariable) {
		runAction(portFormula, variable)
		assertEquals(expected, variable.value)
	}

	@After
	fun tearDown() {
		occupiedSockets.forEach { runCatching { it.close() } }
	}

	// ======================== valid ports (free) ========================

	@Test
	fun testFreePortReturnsFalse() {
		assertResult(Formula(freePort().toString()), "false", UserVariable("v"))
	}

	@Test
	fun testBoundaryPort1ReturnsFalseWhenFree() {
		val port = freePort()
		if (port == 1) {
			return
		}
		assertResult(Formula("1"), if (1 == port) "false" else "false", UserVariable("v"))
	}

	@Test
	fun testBoundaryPort65535ReturnsFalseWhenFree() {
		assertResult(Formula("65535"), "false", UserVariable("v"))
	}

	@Test
	fun testManyFreePortsReturnFalse() {
		val variable = UserVariable("v")
		for (i in 0 until 25) {
			val port = freePort()
			runAction(Formula(port.toString()), variable)
			assertEquals("порт $port должен быть свободен", "false", variable.value)
		}
	}

	@Test
	fun testMiddleRangePortsAreChecked() {
		val variable = UserVariable("v")
		for (i in 0 until 14) {
			val port = freePort()
			runAction(Formula(port.toString()), variable)
			assertEquals("порт $port должен быть свободен", "false", variable.value)
		}
	}

	@Test
	fun testPortWithSpacesParsesOk() {
		val variable = UserVariable("v")
		runAction(Formula(" 8080"), variable)
		assertEquals("false", variable.value)
	}

	@Test
	fun testPortWithTrailingSpaceParsesOk() {
		val variable = UserVariable("v")
		runAction(Formula("8080 "), variable)
		assertEquals("false", variable.value)
	}

	@Test
	fun testPortWithLeadingZerosParsesOk() {
		val variable = UserVariable("v")
		runAction(Formula("00080"), variable)
		assertEquals("false", variable.value)
	}

	// ======================== occupied ports ========================

	@Test
	fun testOccupiedPortReturnsTrue() {
		val port = occupyPort()
		assertResult(Formula(port.toString()), "true", UserVariable("v"))
	}

	@Test
	fun testManyOccupiedPortsReturnTrue() {
		val variable = UserVariable("v")
		for (i in 0 until 15) {
			val port = occupyPort()
			runAction(Formula(port.toString()), variable)
			assertEquals("порт $port должен быть занят", "true", variable.value)
		}
	}

	@Test
	fun testPortFreedAfterSocketClosedReturnsFalse() {
		val port = occupyPort()
		val variable = UserVariable("v")
		runAction(Formula(port.toString()), variable)
		assertEquals("true", variable.value)
		occupiedSockets.removeAt(occupiedSockets.lastIndex).close()
		Thread.sleep(50)
		runAction(Formula(port.toString()), variable)
		assertEquals("false", variable.value)
	}

	@Test
	fun testSamePortCheckedTwiceIsStable() {
		val port = occupyPort()
		val variable = UserVariable("v")
		for (i in 0 until 5) {
			runAction(Formula(port.toString()), variable)
			assertEquals("true", variable.value)
		}
	}

	// ======================== invalid values ========================

	@Test
	fun testPortZeroReturnsError() {
		assertResult(Formula("0"), "ERROR", UserVariable("v"))
	}

	@Test
	fun testNegativePortReturnsError() {
		assertResult(Formula("-1"), "ERROR", UserVariable("v"))
		assertResult(Formula("-8080"), "ERROR", UserVariable("v"))
		assertResult(Formula("-65535"), "ERROR", UserVariable("v"))
	}

	@Test
	fun testTooLargePortReturnsError() {
		assertResult(Formula("65536"), "ERROR", UserVariable("v"))
		assertResult(Formula("99999"), "ERROR", UserVariable("v"))
		assertResult(Formula("1000000000"), "ERROR", UserVariable("v"))
	}

	@Test
	fun testNonNumericPortReturnsError() {
		val variable = UserVariable("v")
		for (value in arrayOf("abc", "", "12.5", "0x50", "NaN", "null", "true", "1,2", "80-", "--80", "1e3")) {
			runAction(Formula(value), variable)
			assertEquals("порт '$value' → ERROR", "ERROR", variable.value)
		}
	}

	@Test
	fun testIntegerOverflowPortReturnsError() {
		assertResult(Formula("2147483647"), "ERROR", UserVariable("v"))
		assertResult(Formula("2147483648"), "ERROR", UserVariable("v"))
		assertResult(Formula("-2147483649"), "ERROR", UserVariable("v"))
	}

	@Test
	fun testNullPortFormulaLeavesVariableUntouched() {
		val variable = UserVariable("INITIAL")
		runAction(null, variable)
		assertEquals("INITIAL", variable.value)
	}

	@Test
	fun testErrorDoesNotOverwriteWhenVariableNull() {
		runAction(Formula("notaport"), null)
		runAction(null, null)
	}

	@Test
	fun testValidResultWithVariableNullDoesNotCrash() {
		runAction(Formula("8080"), null)
	}

	@Test
	fun testFormulaWithFunctionReturnsError() {
		val variable = UserVariable("v")
		runAction(Formula("JOIN(80,80)"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testFormulaResultingInNumberWorks() {
		val variable = UserVariable("v")
		runAction(Formula("80 + 0"), variable)
		assertEquals("false", variable.value)
	}

	@Test
	fun testFormulaResultingInNumberForOccupiedPortWorks() {
		val port = occupyPort()
		val variable = UserVariable("v")
		runAction(Formula("$port + 0"), variable)
		assertEquals("true", variable.value)
	}
}