package org.catrobat.catroid.test.content.actions

import org.catrobat.catroid.content.LocalServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

@RunWith(RobolectricTestRunner::class)
class LocalServerTest {

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

	private fun sendLine(port: Int, line: String) {
		Socket("127.0.0.1", port).use { socket ->
			socket.getOutputStream().write((line + "\n").toByteArray(Charsets.UTF_8))
			socket.getOutputStream().flush()
		}
	}

	@Before
	fun setUp() {
		LocalServer.clientLimit = 10
		LocalServer.serverTimeoutSeconds = 30
	}

	@After
	fun tearDown() {
		LocalServer.stop()
		LocalServer.clientLimit = 10
		LocalServer.serverTimeoutSeconds = 30
	}

	// ======================== isPortInUse ========================

	@Test
	fun testIsPortInUseFreePortReturnsFalse() {
		val port = freePort()
		assertFalse("свободный порт должен быть свободен", LocalServer.isPortInUse(port))
	}

	@Test
	fun testIsPortInUseOccupiedPortReturnsTrue() {
		val socket = ServerSocket(0)
		try {
			assertTrue("занятый порт должен определяться как занятый", LocalServer.isPortInUse(socket.localPort))
		} finally {
			socket.close()
		}
	}

	@Test
	fun testIsPortInUseFreedPortReturnsFalse() {
		val socket = ServerSocket(0)
		val port = socket.localPort
		socket.close()
		await { !LocalServer.isPortInUse(port) }
	}

	@Test
	fun testIsPortInUseZeroPortIsFree() {
		assertFalse("порт 0 = эфемерный, всегда свободен", LocalServer.isPortInUse(0))
	}

	@Test
	fun testIsPortInUseNegativePortDoesNotCrash() {
		assertTrue("отрицательный порт не биндится → занят/ошибка", LocalServer.isPortInUse(-1))
		assertTrue(LocalServer.isPortInUse(-65535))
		assertTrue(LocalServer.isPortInUse(Int.MIN_VALUE))
	}

	@Test
	fun testIsPortInUseOutOfRangeDoesNotCrash() {
		assertTrue(LocalServer.isPortInUse(65536))
		assertTrue(LocalServer.isPortInUse(70000))
		assertTrue(LocalServer.isPortInUse(Int.MAX_VALUE))
	}

	@Test
	fun testIsPortInUseBoundaryPorts() {
		val low = ServerSocket(0)
		try {
			assertTrue(LocalServer.isPortInUse(low.localPort))
		} finally {
			low.close()
		}
		val high = ServerSocket(0)
		try {
			assertTrue(LocalServer.isPortInUse(high.localPort))
		} finally {
			high.close()
		}
	}

	@Test
	fun testIsPortInUseManySequentialCallsAreStable() {
		val port = freePort()
		for (i in 0 until 20) {
			assertFalse(LocalServer.isPortInUse(port))
		}
	}

	// ======================== server lifecycle ========================

	@Test
	fun testStartServerAcceptsMessageFromClient() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		sendLine(port, "hello")
		await { LocalServer.getValue() == "hello" }
	}

	@Test
	fun testServerStoresMessagesInOrder() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		try {
			val out = client.getOutputStream()
			out.write("one\n".toByteArray(Charsets.UTF_8))
			out.write("two\n".toByteArray(Charsets.UTF_8))
			out.write("three\n".toByteArray(Charsets.UTF_8))
			out.flush()
			await { LocalServer.getMessages().size == 3 }
			assertEquals(listOf("one", "two", "three"), LocalServer.getMessages())
		} finally {
			client.close()
		}
	}

	@Test
	fun testServerStoresEmptyMessage() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		sendLine(port, "")
		await { LocalServer.getMessages().isNotEmpty() }
		assertEquals("", LocalServer.getValue())
	}

	@Test
	fun testServerStoresUnicodeMessage() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		sendLine(port, "привет мир 🌍")
		await { LocalServer.getValue() == "привет мир 🌍" }
	}

	@Test
	fun testServerStoresLongMessage() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val long = "x".repeat(10_000)
		sendLine(port, long)
		await { LocalServer.getValue() == long }
	}

	@Test
	fun testServerBufferTruncatesTo50() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		try {
			val out = client.getOutputStream()
			for (i in 1..60) {
				out.write("msg$i\n".toByteArray(Charsets.UTF_8))
			}
			out.flush()
			await { LocalServer.getMessages().size == 50 }
			val messages = LocalServer.getMessages()
			assertEquals("msg11", messages.first())
			assertEquals("msg60", messages.last())
		} finally {
			client.close()
		}
	}

	@Test
	fun testServerIgnoresMessageWithoutNewline() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		try {
			client.getOutputStream().write("no-newline".toByteArray(Charsets.UTF_8))
			client.getOutputStream().flush()
			Thread.sleep(300)
			assertTrue(
				"сообщение без \\n не должно попасть в буфер, пока сокет открыт",
				LocalServer.getMessages().isEmpty(),
			)
		} finally {
			client.close()
		}
	}

	@Test
	fun testServerAcceptMultipleClients() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		sendLine(port, "from-a")
		sendLine(port, "from-b")
		await { LocalServer.getMessages().size == 2 }
		assertEquals(
			"порядок сообщений от разных клиентов не гарантирован",
			setOf("from-a", "from-b"),
			LocalServer.getMessages().toSet(),
		)
	}

	@Test
	fun testGetPortBeforeStartIsNaN() {
		assertEquals("NaN", LocalServer.getPort())
	}

	@Test
	fun testGetIPBeforeStartIsNaN() {
		assertEquals("NaN", LocalServer.getIP())
	}

	@Test
	fun testGetIPAfterStartIsNotNaN() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		await { LocalServer.getIP() != "NaN" }
	}

	@Test
	fun testStartServerOnOccupiedPortDoesNotCrash() {
		val occupied = ServerSocket(0)
		try {
			LocalServer.startOrJoin(null, occupied.localPort.toString())
			Thread.sleep(300)
			val port = freePort()
			LocalServer.startOrJoin(null, port.toString())
			await { LocalServer.getPort() == port.toString() }
			sendLine(port, "still-works")
			await { LocalServer.getValue() == "still-works" }
		} finally {
			occupied.close()
		}
	}

	@Test
	fun testStartServerWithNonNumericPortDoesNotCrash() {
		LocalServer.startOrJoin(null, "abc")
		Thread.sleep(300)
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
	}

	@Test
	fun testStartServerWithOutOfRangePortDoesNotCrash() {
		LocalServer.startOrJoin(null, "99999")
		Thread.sleep(300)
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
	}

	@Test
	fun testStartServerWithEmptyPortDoesNotCrash() {
		LocalServer.startOrJoin(null, "")
		Thread.sleep(300)
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
	}

	@Test
	fun testRestartServerOnSamePort() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		sendLine(port, "first")
		await { LocalServer.getValue() == "first" }
		LocalServer.stop()
		Thread.sleep(200)
		LocalServer.startOrJoin(null, port.toString())
		await {
			val reopened = runCatching { Socket("127.0.0.1", port) }.isSuccess
			if (reopened) {
				LocalServer.stop()
			}
			reopened
		}
	}

	@Test
	fun testStopClosesClientSockets() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		client.getOutputStream().write("before-stop\n".toByteArray())
		await { LocalServer.getValue() == "before-stop" }
		LocalServer.stop()
		await {
			val closed = runCatching {
				client.getInputStream().read() == -1
			}.getOrDefault(true)
			closed
		}
		client.close()
	}

	@Test
	fun testStopWhenNotRunningIsSafe() {
		LocalServer.stop()
		LocalServer.stop()
		LocalServer.stop()
	}

	@Test
	fun testMessagesPersistAfterStop() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		sendLine(port, "kept")
		await { LocalServer.getValue() == "kept" }
		LocalServer.stop()
		assertEquals("kept", LocalServer.getValue())
	}

	@Test
	fun testSendWithoutConnectionDoesNotCrash() {
		LocalServer.send("nobody-listens")
		Thread.sleep(200)
		LocalServer.send("")
		Thread.sleep(200)
	}

	@Test
	fun testSendAfterStopDoesNotCrash() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		LocalServer.stop()
		LocalServer.send("after-stop")
		Thread.sleep(200)
	}

	@Test
	fun testSendUnicodeAndNewlines() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		client.soTimeout = 8000
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		Thread.sleep(200)
		LocalServer.send("line-with-юникод")
		assertEquals("line-with-юникод", reader.readLine())
		LocalServer.send("second")
		assertEquals("second", reader.readLine())
		client.close()
	}

	@Test
	fun testSendMultilineValueKeepsInnerNewline() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		client.soTimeout = 8000
		val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
		Thread.sleep(200)
		LocalServer.send("a\nb")
		assertEquals("a", reader.readLine())
		assertEquals("b", reader.readLine())
		client.close()
	}

	@Test
	fun testSendWithMultipleClientsDeliversToAll() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client1 = Socket("127.0.0.1", port)
		client1.soTimeout = 8000
		val reader1 = BufferedReader(InputStreamReader(client1.getInputStream(), Charsets.UTF_8))
		val client2 = Socket("127.0.0.1", port)
		client2.soTimeout = 8000
		val reader2 = BufferedReader(InputStreamReader(client2.getInputStream(), Charsets.UTF_8))
		Thread.sleep(300)
		LocalServer.send("broadcast")
		assertEquals("broadcast", reader1.readLine())
		assertEquals("broadcast", reader2.readLine())
		client1.close()
		client2.close()
	}

	@Test
	fun testServerTimeoutStopsServer() {
		LocalServer.serverTimeoutSeconds = 1
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		await(5000) {
			LocalServer.getPort() == "NaN" && LocalServer.getIP() == "NaN"
		}
	}

	@Test
	fun testServerClientTimeoutClosesIdleClient() {
		LocalServer.serverTimeoutSeconds = 1
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client = Socket("127.0.0.1", port)
		client.getOutputStream().write("i-am-here\n".toByteArray())
		await { LocalServer.getValue() == "i-am-here" }
		await(5000) {
			runCatching {
				client.getInputStream().read() == -1
			}.getOrDefault(true)
		}
		client.close()
	}

	// ======================== client mode ========================

	@Test
	fun testConnectToServerReceivesMessages() {
		val server = ServerSocket(0)
		try {
			LocalServer.startOrJoin("127.0.0.1", server.localPort.toString())
			await { LocalServer.getPort() == server.localPort.toString() }
			val accepted = server.accept()
			accepted.getOutputStream().write("from-server\n".toByteArray())
			accepted.getOutputStream().flush()
			await { LocalServer.getValue() == "from-server" }
			accepted.close()
		} finally {
			server.close()
		}
	}

	@Test
	fun testConnectToRefusedPortStopsCleanly() {
		val port = freePort()
		LocalServer.startOrJoin("127.0.0.1", port.toString())
		await(5000) {
			runCatching { Socket("127.0.0.1", port) }.isFailure
		}
		LocalServer.stop()
	}

	@Test
	fun testConnectToInvalidIPDoesNotCrash() {
		LocalServer.startOrJoin("999.999.999.999", "80")
		Thread.sleep(500)
		LocalServer.stop()
	}

	@Test
	fun testConnectToNonNumericPortDoesNotCrash() {
		LocalServer.startOrJoin("127.0.0.1", "notaport")
		Thread.sleep(300)
	}

	// ======================== client limit ========================

	@Test
	fun testClientLimitOneRejectsSecondClient() {
		LocalServer.clientLimit = 1
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client1 = Socket("127.0.0.1", port)
		client1.getOutputStream().write("accepted\n".toByteArray())
		client1.getOutputStream().flush()
		await { LocalServer.getValue() == "accepted" }
		runCatching {
			val client2 = Socket("127.0.0.1", port)
			client2.getOutputStream().write("rejected\n".toByteArray())
			client2.getOutputStream().flush()
			client2.close()
		}
		Thread.sleep(500)
		assertTrue("отклонённое сообщение не должно попасть в буфер", LocalServer.getMessages().none { it == "rejected" })
		client1.close()
	}

	@Test
	fun testClientLimitTwoAcceptsTwoRejectsThird() {
		LocalServer.clientLimit = 2
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client1 = Socket("127.0.0.1", port)
		client1.getOutputStream().write("m1\n".toByteArray(Charsets.UTF_8))
		client1.getOutputStream().flush()
		val client2 = Socket("127.0.0.1", port)
		client2.getOutputStream().write("m2\n".toByteArray(Charsets.UTF_8))
		client2.getOutputStream().flush()
		await { LocalServer.getMessages().size == 2 }
		runCatching {
			val client3 = Socket("127.0.0.1", port)
			client3.getOutputStream().write("m3\n".toByteArray(Charsets.UTF_8))
			client3.getOutputStream().flush()
			client3.close()
		}
		Thread.sleep(300)
		assertEquals(2, LocalServer.getMessages().size)
		client1.close()
		client2.close()
	}

	@Test
	fun testClientLimitDefaultIsTen() {
		assertEquals(10, LocalServer.clientLimit)
	}

	@Test
	fun testClientLimitAfterClientDisconnectsFreesSlot() {
		LocalServer.clientLimit = 1
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val client1 = Socket("127.0.0.1", port)
		client1.getOutputStream().write("first\n".toByteArray())
		await { LocalServer.getValue() == "first" }
		client1.close()
		await(5000) {
			val secondAccepted = runCatching {
				val second = Socket("127.0.0.1", port)
				second.getOutputStream().write("second\n".toByteArray())
				second.getOutputStream().flush()
				second.close()
				true
			}.getOrDefault(false)
			secondAccepted
		}
		await { LocalServer.getValue() == "second" }
	}

	// ======================== concurrency ========================

	@Test
	fun testConcurrentSendsDoNotCrash() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val threads = (1..8).map { i ->
			Thread {
				repeat(10) {
					LocalServer.send("t$i-$it")
				}
			}
		}
		threads.forEach { it.start() }
		threads.forEach { it.join() }
		Thread.sleep(300)
		LocalServer.stop()
	}

	@Test
	fun testConcurrentStartStopDoesNotCrash() {
		val port = freePort()
		val threads = (1..6).map {
			Thread {
				LocalServer.startOrJoin(null, port.toString())
				Thread.sleep(50)
				LocalServer.stop()
			}
		}
		threads.forEach { it.start() }
		threads.forEach { it.join() }
		LocalServer.stop()
	}

	@Test
	fun testManySequentialRestarts() {
		for (i in 0 until 5) {
			val port = freePort()
			LocalServer.startOrJoin(null, port.toString())
			await { LocalServer.getPort() == port.toString() }
			sendLine(port, "run$i")
			await { LocalServer.getValue() == "run$i" }
			LocalServer.stop()
			Thread.sleep(100)
		}
	}

	@Test
	fun testClientModeThenServerModeOnSameInstance() {
		val server = ServerSocket(0)
		try {
			LocalServer.startOrJoin("127.0.0.1", server.localPort.toString())
			await { LocalServer.getPort() == server.localPort.toString() }
			val accepted = server.accept()
			accepted.getOutputStream().write("x\n".toByteArray())
			await { LocalServer.getValue() == "x" }
			accepted.close()
			LocalServer.stop()
		} finally {
			server.close()
		}
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		sendLine(port, "y")
		await { LocalServer.getValue() == "y" }
		LocalServer.stop()
	}

	@Test
	fun testSwitchServerToClientWhileRunning() {
		val port = freePort()
		LocalServer.startOrJoin(null, port.toString())
		await { LocalServer.getPort() == port.toString() }
		val server = ServerSocket(0)
		try {
			LocalServer.startOrJoin("127.0.0.1", server.localPort.toString())
			await { LocalServer.getPort() == server.localPort.toString() }
			val accepted = server.accept()
			accepted.getOutputStream().write("switched\n".toByteArray())
			await { LocalServer.getValue() == "switched" }
			accepted.close()
		} finally {
			server.close()
		}
	}
}