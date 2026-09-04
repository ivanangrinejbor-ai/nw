package org.catrobat.catroid.test.io

import org.catrobat.catroid.io.asynctask.copyStreamWithProgressAndHash
import org.catrobat.catroid.io.asynctask.verifyStreamHash
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import kotlin.random.Random

@RunWith(JUnit4::class)
class ExportVerificationTest {

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    @Test
    fun copyReturnsCorrectHashAndIdenticalBytes() {
        val data = Random(42).nextBytes(200 * 1024)
        val out = ByteArrayOutputStream()
        val progress = mutableListOf<Pair<Long, Long>>()

        val hash = copyStreamWithProgressAndHash(
            ByteArrayInputStream(data), out, data.size.toLong()
        ) { done, total -> progress.add(done to total) }

        assertArrayEquals(data, out.toByteArray())
        assertArrayEquals(sha256(data), hash)
        assertTrue(progress.isNotEmpty())
        assertEquals(data.size.toLong() to data.size.toLong(), progress.last())
        progress.zipWithNext { a, b -> assertTrue(b.first >= a.first) }
    }

    @Test
    fun copyEmptyStreamYieldsEmptyHash() {
        val out = ByteArrayOutputStream()
        val hash = copyStreamWithProgressAndHash(
            ByteArrayInputStream(ByteArray(0)), out, 0L
        ) { _, _ -> }

        assertEquals(0, out.size())
        assertArrayEquals(sha256(ByteArray(0)), hash)
    }

    @Test
    fun verifyAcceptsIdenticalStream() {
        val data = Random(7).nextBytes(1024)
        val ok = verifyStreamHash(ByteArrayInputStream(data), sha256(data)) { _, _ -> }
        assertTrue(ok)
    }

    @Test
    fun verifyRejectsTamperedStream() {
        val data = Random(7).nextBytes(1024)
        val tampered = data.copyOf().also { it[512] = (it[512] + 1).toByte() }
        val ok = verifyStreamHash(ByteArrayInputStream(tampered), sha256(data)) { _, _ -> }
        assertFalse(ok)
    }

    @Test
    fun verifyRejectsTruncatedStream() {
        val data = Random(7).nextBytes(1024)
        val truncated = data.copyOf(100)
        val ok = verifyStreamHash(ByteArrayInputStream(truncated), sha256(data)) { _, _ -> }
        assertFalse(ok)
    }

    @Test
    fun fullRoundTripCopyThenVerify() {
        val data = Random(99).nextBytes(300 * 1024)
        val stored = ByteArrayOutputStream()
        val sourceHash = copyStreamWithProgressAndHash(
            ByteArrayInputStream(data), stored, data.size.toLong()
        ) { _, _ -> }

        val ok = verifyStreamHash(ByteArrayInputStream(stored.toByteArray()), sourceHash) { _, _ -> }
        assertTrue(ok)
    }
}
