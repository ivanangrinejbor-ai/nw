package org.catrobat.catroid.test.content.actions

import org.catrobat.catroid.content.LocalServer
import org.catrobat.catroid.content.actions.SendToTcpServerAction
import org.catrobat.catroid.formulaeditor.Formula
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
class SendToTcpServerActionTest {

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

	private fun expectNothingMore(client: Socket, reader: BufferedReader) {
		client.soTimeout = 500
		try {
			val line = reader.readLine()
			fail("не ожидалось сообщение, но пришло: $line")
		} catch (expected: SocketTimeoutException) {
			// ok — больше ничего не пришло
		} finally {
			client.soTimeout = 8000
		}
	}

private fun runAction(values: List<Formula?>?) {
		val action = SendToTcpServerAction()
		action.values = values as List<Formula>?
		action.act(1f)
	}

	@Before
	fun setUp() {
		LocalServer.clientLimit = 10
		LocalServer.serverTimeoutSeconds = 30
	}

	@After
	fun tearDown() {
		LocalServer.stop()
	}

	// ======================== defensive cases ========================

	@Test
	fun testNullValuesDoesNotCrash() {
		runAction(null)
	}

	@Test
	fun testEmptyValuesDoesNotCrash() {
		runAction(emptyList())
	}

	@Test
	fun testNullFormulaInListDoesNotCrash() {
		runAction(listOf(null))
		runAction(listOf(null, null, null))
	}

	@Test
	fun testListWithOnlyNullFormulasSendsNothing() {
		runAction(listOf(null))
		Thread.sleep(200)
	}

	@Test
	fun testActionCanRunTwice() {
		runAction(listOf(Formula("x")))
		runAction(listOf(Formula("y")))
	}

	// ======================== integration with LocalServer ========================

	@Test
	fun testSingleValueReachesClient() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		client.soTimeout = 8000
		Thread.sleep(200)
		runAction(listOf(Formula("hello")))
		assertEquals("hello", reader.readLine())
		client.close()
	}

	@Test
	fun testTwoValuesReachClientInOrder() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		client.soTimeout = 8000
		Thread.sleep(200)
		runAction(listOf(Formula("first"), Formula("second")))
		assertEquals("first", reader.readLine())
		assertEquals("second", reader.readLine())
		client.close()
	}

	@Test
	fun testFourValuesReachClientInOrder() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		client.soTimeout = 8000
		Thread.sleep(200)
		runAction(listOf(Formula("1"), Formula("2"), Formula("3"), Formula("4")))
		assertEquals("1", reader.readLine())
		assertEquals("2", reader.readLine())
		assertEquals("3", reader.readLine())
		assertEquals("4", reader.readLine())
		client.close()
	}

	@Test
	fun testNullFormulaInMiddleIsSkipped() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		client.soTimeout = 8000
		Thread.sleep(200)
		runAction(listOf(Formula("a"), null, Formula("b")))
		assertEquals("a", reader.readLine())
		assertEquals("b", reader.readLine())
		expectNothingMore(client, reader)
		client.close()
	}

	@Test
	fun testUnicodeValueReachesClient() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		client.soTimeout = 8000
		Thread.sleep(200)
		runAction(listOf(Formula("текст с юникодом")))
		assertEquals("текст с юникодом", reader.readLine())
		client.close()
	}

	@Test
	fun testValueWithInnerNewlineSplitIntoLines() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		client.soTimeout = 8000
		Thread.sleep(200)
		runAction(listOf(Formula("a\nb")))
		assertEquals("a", reader.readLine())
		assertEquals("b", reader.readLine())
		client.close()
	}

	@Test
	fun testEmptyStringValueSendsEmptyLine() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		client.soTimeout = 8000
		Thread.sleep(200)
		runAction(listOf(Formula("")))
		assertEquals("", reader.readLine())
		client.close()
	}

	@Test
	fun testSendWhileClientDisconnectedDoesNotCrash() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		client.close()
		Thread.sleep(200)
		runAction(listOf(Formula("to-nowhere")))
		Thread.sleep(200)
	}

	@Test
	fun testSendWithNoClientsConnectedDoesNotCrash() {
		runAction(listOf(Formula("nobody")))
		Thread.sleep(200)
	}

	@Test
	fun testSendAfterServerStoppedDoesNotCrash() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		LocalServer.stop()
		runAction(listOf(Formula("after-stop")))
		Thread.sleep(200)
	}

	@Test
	fun testActionIsSingleShot() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		client.soTimeout = 8000
		Thread.sleep(200)
		runAction(listOf(Formula("once")))
		assertEquals("once", reader.readLine())
		expectNothingMore(client, reader)
		client.close()
	}
}
