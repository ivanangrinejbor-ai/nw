package org.catrobat.catroid.test.content.actions

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.ServerSocket
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
class SocketTimeoutProbeTest {

	@Test
	fun testServerSocketSoTimeoutThrows() {
		val server = ServerSocket(0)
		try {
			server.soTimeout = 200
			val start = System.currentTimeMillis()
			var threw = false
			try {
				server.accept()
			} catch (e: SocketTimeoutException) {
				threw = true
			}
			val elapsed = System.currentTimeMillis() - start
			assertTrue("accept должен бросить SocketTimeoutException за ~200ms, прошло $elapsed ms, threw=$threw", threw)
		} finally {
			server.close()
		}
	}

	@Test
	fun testPlainServerSocketWorks() {
		val server = ServerSocket(0)
		try {
			assertTrue(server.localPort > 0)
		} finally {
			server.close()
		}
	}
}