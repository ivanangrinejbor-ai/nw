package org.catrobat.catroid.test.content.actions

import org.catrobat.catroid.content.LocalServer
import org.catrobat.catroid.content.actions.ListenTcpServerAction
import org.catrobat.catroid.formulaeditor.UserVariable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.ServerSocket
import java.net.Socket

@RunWith(RobolectricTestRunner::class)
class ListenTcpServerActionTest {

	private fun freePort(): Int {
		val socket = ServerSocket(0)
		val port = socket.localPort
		socket.close()
		return port
	}

	private fun await(timeoutMillis: Long = 8000, condition: () -> Boolean) {
		val deadline = System.currentTimeMillis() + timeoutMillis
		while (System.currentTimeMillis() < deadline) {
			if (condition()) {
				return
			}
			Thread.sleep(20)
		}
		fail("condition not met within ${timeoutMillis}ms")
	}

	private fun startServerWithMessages(vararg messages: String) {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		for (message in messages) {
			Socket("127.0.0.1", port).use { socket ->
				socket.getOutputStream().write((message + "\n").toByteArray(Charsets.UTF_8))
				socket.getOutputStream().flush()
			}
		}
	}

	private fun runAction(variables: List<UserVariable?>?): Boolean {
		val action = ListenTcpServerAction()
		action.variables = variables as List<UserVariable>?
		return action.act(1f)
	}

	@Before
	fun setUp() {
		LocalServer.clientLimit = 10
		LocalServer.serverTimeoutSeconds = 30
	}

	@After
	fun tearDown() {
		ListenTcpServerAction.stopAll()
		LocalServer.stop()
	}

	@Test
	fun testNullVariablesReturnsTrue() {
		assertTrue(runAction(null))
	}

	@Test
	fun testEmptyVariablesReturnsTrue() {
		assertTrue(runAction(emptyList()))
	}

	@Test
	fun testListWithNullVariableDoesNotCrash() {
		startServerWithMessages("a")
		await { LocalServer.getValue() == "a" }
		assertTrue(runAction(listOf(null)))
		Thread.sleep(150)
	}

	@Test
	fun testListWithNullInMiddleDoesNotCrash() {
		startServerWithMessages("a", "b", "c")
		await { LocalServer.getMessages().size == 3 }
		assertTrue(runAction(listOf(UserVariable("first"), null, UserVariable("third"))))
		Thread.sleep(150)
	}

	@Test
	fun testActionReturnsTrueImmediately() {
		assertTrue(runAction(listOf(UserVariable("v"))))
	}

	@Test
	fun testSingleVariableGetsLastMessage() {
		startServerWithMessages("one", "two", "three")
		await { LocalServer.getMessages().size == 3 }
		val variable = UserVariable("v")
		runAction(listOf(variable))
		await { variable.value == "three" }
	}

	@Test
	fun testTwoVariablesGetLastTwoMessagesInOrder() {
		startServerWithMessages("one", "two", "three", "four")
		await { LocalServer.getMessages().size == 4 }
		val first = UserVariable("v1")
		val second = UserVariable("v2")
		runAction(listOf(first, second))
		await { second.value == "four" }
		await { first.value == "three" }
	}

	@Test
	fun testThreeVariablesGetLastThreeMessagesInOrder() {
		startServerWithMessages("1", "2", "3", "4", "5")
		await { LocalServer.getMessages().size == 5 }
		val v1 = UserVariable("v1")
		val v2 = UserVariable("v2")
		val v3 = UserVariable("v3")
		runAction(listOf(v1, v2, v3))
		await { v3.value == "5" }
		await { v2.value == "4" }
		await { v1.value == "3" }
	}

	@Test
	fun testFourVariablesWithExactlyFourMessages() {
		startServerWithMessages("a", "b", "c", "d")
		await { LocalServer.getMessages().size == 4 }
		val vars = (1..4).map { UserVariable("v$it") }.toMutableList()
		runAction(vars)
		await { vars[3].value == "d" }
		await { vars[0].value == "a" }
	}

	@Test
	fun testVariableNotOverwrittenWhenNoMessages() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val variable = UserVariable("v").apply { value = "INITIAL" }
		runAction(listOf(variable))
		Thread.sleep(200)
		assertEquals("INITIAL", variable.value)
	}

	@Test
	fun testNewMessagesKeepUpdatingVariable() {
		startServerWithMessages("first")
		await { LocalServer.getValue() == "first" }
		val variable = UserVariable("v")
		runAction(listOf(variable))
		await { variable.value == "first" }
		val port = LocalServer.getPort().toInt()
		sendLine(port, "second")
		await { variable.value == "second" }
	}

	@Test
	fun testSecondActionDoesNotCancelFirst() {
		startServerWithMessages("a", "b", "c")
		await { LocalServer.getMessages().size == 3 }
		val oldVar = UserVariable("old")
		runAction(listOf(oldVar))
		await { oldVar.value == "c" }
		val newVar = UserVariable("new")
		runAction(listOf(newVar))
		await { newVar.value == "c" }
		val port = LocalServer.getPort().toInt()
		sendLine(port, "d")
		await { newVar.value == "d" }
		await { oldVar.value == "d" }
	}

	@Test
	fun testPackedMessageSplitsIntoVariablesInOrder() {
		val sep = LocalServer.VALUE_SEPARATOR
		startServerWithMessages("5$sep" + "4$sep" + "0$sep" + "5")
		await { LocalServer.getMessages().isNotEmpty() }
		val vars = (1..4).map { UserVariable("v$it") }.toMutableList()
		runAction(vars)
		await { vars[0].value == "5" }
		await { vars[1].value == "4" }
		await { vars[2].value == "0" }
		await { vars[3].value == "5" }
	}

	@Test
	fun testPackedMessageRepeatsDeliverSameValues() {
		// эхо loopback доставляет копии пакета — переменные не должны «поехать»
		val sep = LocalServer.VALUE_SEPARATOR
		startServerWithMessages("5$sep" + "4$sep" + "0$sep" + "5")
		await { LocalServer.getMessages().isNotEmpty() }
		val vars = (1..4).map { UserVariable("v$it") }.toMutableList()
		runAction(vars)
		sendLine(LocalServer.getPort().toInt(), "5$sep" + "4$sep" + "0$sep" + "5")
		sendLine(LocalServer.getPort().toInt(), "5$sep" + "4$sep" + "0$sep" + "5")
		Thread.sleep(300)
		assertEquals("5", vars[0].value)
		assertEquals("4", vars[1].value)
		assertEquals("0", vars[2].value)
		assertEquals("5", vars[3].value)
	}

	@Test
	fun testPackedMessageWithFewerPartsLeavesRestUntouched() {
		val sep = LocalServer.VALUE_SEPARATOR
		startServerWithMessages("a$sep" + "b")
		await { LocalServer.getMessages().isNotEmpty() }
		val first = UserVariable("v1")
		val second = UserVariable("v2")
		val third = UserVariable("v3").apply { value = "KEEP" }
		runAction(listOf(first, second, third))
		await { first.value == "a" }
		await { second.value == "b" }
		Thread.sleep(150)
		assertEquals("третьей части нет — переменная не трогается", "KEEP", third.value)
	}

	@Test
	fun testPackedMessageWithMorePartsIgnoresExtras() {
		val sep = LocalServer.VALUE_SEPARATOR
		startServerWithMessages("1$sep" + "2$sep" + "3$sep" + "4$sep" + "5")
		await { LocalServer.getMessages().isNotEmpty() }
		val first = UserVariable("v1")
		val second = UserVariable("v2")
		runAction(listOf(first, second))
		await { first.value == "1" }
		await { second.value == "2" }
	}

	@Test
	fun testStopAllStopsUpdates() {
		startServerWithMessages("first")
		await { LocalServer.getValue() == "first" }
		val variable = UserVariable("v")
		runAction(listOf(variable))
		await { variable.value == "first" }
		ListenTcpServerAction.stopAll()
		val port = LocalServer.getPort().toInt()
		sendLine(port, "second")
		Thread.sleep(300)
		assertEquals("после stopAll переменная не обновляется", "first", variable.value)
	}

	@Test
	fun testStopAllThenNewActionWorks() {
		startServerWithMessages("x")
		await { LocalServer.getValue() == "x" }
		ListenTcpServerAction.stopAll()
		val variable = UserVariable("v")
		runAction(listOf(variable))
		await { variable.value == "x" }
	}

	@Test
	fun testStopAllWhenNeverStartedIsSafe() {
		ListenTcpServerAction.stopAll()
		ListenTcpServerAction.stopAll()
	}

	@Test
	fun testUnicodeMessagesPropagateToVariables() {
		startServerWithMessages("привет")
		await { LocalServer.getValue() == "привет" }
		val variable = UserVariable("v")
		runAction(listOf(variable))
		await { variable.value == "привет" }
	}

	@Test
	fun testManyActionsSequentiallyDoNotLeakTasks() {
		startServerWithMessages("m")
		await { LocalServer.getValue() == "m" }
		for (i in 0 until 5) {
			val variable = UserVariable("v$i")
			runAction(listOf(variable))
			await { variable.value == "m" }
		}
		ListenTcpServerAction.stopAll()
	}

	private fun sendLine(port: Int, line: String) {
		Socket("127.0.0.1", port).use { socket ->
			socket.getOutputStream().write((line + "\n").toByteArray(Charsets.UTF_8))
			socket.getOutputStream().flush()
		}
	}
}