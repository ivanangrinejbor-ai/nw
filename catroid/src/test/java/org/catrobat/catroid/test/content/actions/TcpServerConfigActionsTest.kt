package org.catrobat.catroid.test.content.actions

import org.catrobat.catroid.content.LocalServer
import org.catrobat.catroid.content.actions.SetTcpServerClientLimitAction
import org.catrobat.catroid.content.actions.SetTcpServerTimeoutAction
import org.catrobat.catroid.formulaeditor.Formula
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TcpServerConfigActionsTest {

	@After
	fun tearDown() {
		LocalServer.clientLimit = 10
		LocalServer.serverTimeoutSeconds = 30
	}

	private fun applyLimit(formula: Formula?) {
		val action = SetTcpServerClientLimitAction()
		action.limit = formula
		action.act(1f)
	}

	private fun applyTimeout(formula: Formula?) {
		val action = SetTcpServerTimeoutAction()
		action.timeout = formula
		action.act(1f)
	}

	@Test
	fun testDefaultClientLimitIsTen() {
		assertEquals(10, LocalServer.clientLimit)
	}

	@Test
	fun testValidLimitsAppliedAsIs() {
		for (value in intArrayOf(1, 2, 3, 5, 7, 10, 42, 100, 1000, 65535)) {
			applyLimit(Formula(value.toString()))
			assertEquals("лимит $value", value, LocalServer.clientLimit)
		}
	}

	@Test
	fun testZeroCoercedToOne() {
		applyLimit(Formula("0"))
		assertEquals(1, LocalServer.clientLimit)
	}

	@Test
	fun testNegativeLimitsCoercedToOne() {
		for (value in intArrayOf(-1, -5, -100, -2147483648)) {
			applyLimit(Formula(value.toString()))
			assertEquals("лимит $value", 1, LocalServer.clientLimit)
		}
	}

	@Test
	fun testHugeLimitAppliedAsIs() {
		applyLimit(Formula("2147483647"))
		assertEquals(2147483647, LocalServer.clientLimit)
	}

	@Test
	fun testOverflowingLimitIgnored() {
		applyLimit(Formula("2147483648"))
		assertEquals(10, LocalServer.clientLimit)
	}

	@Test
	fun testNonNumericLimitIgnored() {
		val invalid = arrayOf("abc", "", " 5", "5.5", "0x5", "NaN", "null", "ten", "1e2", "--5", "+5")
		for (value in invalid) {
			applyLimit(Formula(value))
			assertEquals("лимит '$value' не применяется", 10, LocalServer.clientLimit)
		}
	}

	@Test
	fun testNullLimitIgnored() {
		applyLimit(null)
		assertEquals(10, LocalServer.clientLimit)
	}

	@Test
	fun testLimitFormulaWithExpression() {
		applyLimit(Formula("2 + 3"))
		assertEquals(5, LocalServer.clientLimit)
	}

	@Test
	fun testLimitSequentialApplicationsLastWins() {
		applyLimit(Formula("3"))
		applyLimit(Formula("0"))
		applyLimit(Formula("7"))
		assertEquals(7, LocalServer.clientLimit)
	}

	@Test
	fun testLimitRepeatedSameValue() {
		for (i in 0 until 10) {
			applyLimit(Formula("4"))
		}
		assertEquals(4, LocalServer.clientLimit)
	}

	@Test
	fun testDefaultTimeoutIsThirty() {
		assertEquals(30, LocalServer.serverTimeoutSeconds)
	}

	@Test
	fun testValidTimeoutsAppliedAsIs() {
		for (value in intArrayOf(1, 2, 5, 10, 30, 60, 120, 300, 3600)) {
			applyTimeout(Formula(value.toString()))
			assertEquals("таймаут $value", value, LocalServer.serverTimeoutSeconds)
		}
	}

	@Test
	fun testZeroTimeoutCoercedToOne() {
		applyTimeout(Formula("0"))
		assertEquals(1, LocalServer.serverTimeoutSeconds)
	}

	@Test
	fun testNegativeTimeoutsCoercedToOne() {
		for (value in intArrayOf(-1, -30, -2147483648)) {
			applyTimeout(Formula(value.toString()))
			assertEquals("таймаут $value", 1, LocalServer.serverTimeoutSeconds)
		}
	}

	@Test
	fun testHugeTimeoutAppliedAsIs() {
		applyTimeout(Formula("2147483647"))
		assertEquals(2147483647, LocalServer.serverTimeoutSeconds)
	}

	@Test
	fun testOverflowingTimeoutIgnored() {
		applyTimeout(Formula("2147483648"))
		assertEquals(30, LocalServer.serverTimeoutSeconds)
	}

	@Test
	fun testNonNumericTimeoutIgnored() {
		val invalid = arrayOf("abc", "", " 30", "30.5", "0x30", "NaN", "null", "thirty", "1e1", "--30")
		for (value in invalid) {
			applyTimeout(Formula(value))
			assertEquals("таймаут '$value' не применяется", 30, LocalServer.serverTimeoutSeconds)
		}
	}

	@Test
	fun testNullTimeoutIgnored() {
		applyTimeout(null)
		assertEquals(30, LocalServer.serverTimeoutSeconds)
	}

	@Test
	fun testTimeoutFormulaWithExpression() {
		applyTimeout(Formula("10 * 3"))
		assertEquals(30, LocalServer.serverTimeoutSeconds)
	}

	@Test
	fun testTimeoutSequentialApplicationsLastWins() {
		applyTimeout(Formula("5"))
		applyTimeout(Formula("0"))
		applyTimeout(Formula("15"))
		assertEquals(15, LocalServer.serverTimeoutSeconds)
	}

	@Test
	fun testTimeoutRepeatedSameValue() {
		for (i in 0 until 10) {
			applyTimeout(Formula("8"))
		}
		assertEquals(8, LocalServer.serverTimeoutSeconds)
	}

	@Test
	fun testLimitAndTimeoutIndependent() {
		applyLimit(Formula("2"))
		applyTimeout(Formula("60"))
		assertEquals(2, LocalServer.clientLimit)
		assertEquals(60, LocalServer.serverTimeoutSeconds)
		applyLimit(Formula("abc"))
		applyTimeout(Formula("xyz"))
		assertEquals(2, LocalServer.clientLimit)
		assertEquals(60, LocalServer.serverTimeoutSeconds)
	}

	@Test
	fun testLimitAppliedWhileServerRunning() {
		applyLimit(Formula("1"))
		assertEquals(1, LocalServer.clientLimit)
		LocalServer.stop()
	}
}