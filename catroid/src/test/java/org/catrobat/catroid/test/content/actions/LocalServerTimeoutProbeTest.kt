package org.catrobat.catroid.test.content.actions

import org.catrobat.catroid.content.LocalServer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.ServerSocket

@RunWith(RobolectricTestRunner::class)
class LocalServerTimeoutProbeTest {

	@Test
	fun testProbe() {
		val socket = ServerSocket(0)
		val port = socket.localPort
		socket.close()
		LocalServer.serverTimeoutSeconds = 1
		LocalServer.startOrJoin(null, port.toString())
		Thread.sleep(3000)
		println("PROBE getPort=${LocalServer.getPort()} getIP=${LocalServer.getIP()}")
		assertEquals("NaN", LocalServer.getPort())
		LocalServer.stop()
	}
}