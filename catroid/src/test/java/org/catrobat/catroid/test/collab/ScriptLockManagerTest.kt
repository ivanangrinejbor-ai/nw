package org.catrobat.catroid.test.collab

import org.catrobat.catroid.collab.FakeScriptLockBackend
import org.catrobat.catroid.collab.LockHeartbeat
import org.catrobat.catroid.collab.LockIdentity
import org.catrobat.catroid.collab.ManualLockHeartbeat
import org.catrobat.catroid.collab.ScriptLock
import org.catrobat.catroid.collab.ScriptLockBackend
import org.catrobat.catroid.collab.ScriptLockManager
import org.catrobat.catroid.collab.ScriptLockPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScriptLockManagerTest {

    private lateinit var backend: FakeScriptLockBackend
    private lateinit var heartbeat: ManualLockHeartbeat
    private var now = 1_000_000L
    private val me = LockIdentity("uid-me", "Petya", 10f)
    private var session: LockIdentity? = me

    private var savedBackend: ScriptLockBackend? = null
    private var savedNow: (() -> Long)? = null
    private var savedSession: (() -> LockIdentity?)? = null
    private var savedHeartbeat: LockHeartbeat? = null

    @Before
    fun setUp() {
        savedBackend = ScriptLockManager.backend
        savedNow = ScriptLockManager.nowProvider
        savedSession = ScriptLockManager.sessionProvider
        savedHeartbeat = ScriptLockManager.heartbeatDriver
        backend = FakeScriptLockBackend()
        heartbeat = ManualLockHeartbeat()
        ScriptLockManager.backend = backend
        ScriptLockManager.nowProvider = { now }
        ScriptLockManager.sessionProvider = { session }
        ScriptLockManager.heartbeatDriver = heartbeat
        ScriptLockManager.start("sid-test")
    }

    @After
    fun tearDown() {
        ScriptLockManager.stop()
        ScriptLockManager.backend = savedBackend!!
        ScriptLockManager.nowProvider = savedNow!!
        ScriptLockManager.sessionProvider = savedSession!!
        ScriptLockManager.heartbeatDriver = savedHeartbeat!!
    }

    private fun foreignClaim(scriptId: String, at: Long = now) {
        var done = false
        backend.claim("sid-test", scriptId, ScriptLock("uid-anya", "Anya", 20f, at), at) { done = it }
        assertTrue(done)
    }

    @Test
    fun inactiveSessionAllowsEverythingWithoutBackend() {
        session = null
        assertTrue(ScriptLockManager.canEdit("s1"))
        assertTrue(ScriptLockManager.claimMine("s1"))
        assertEquals(0, backend.claims)
    }

    @Test
    fun claimFreeScript() {
        assertTrue(ScriptLockManager.claimMine("s1"))
        assertTrue(ScriptLockManager.isHeldByMe("s1"))
        assertEquals("uid-me", backend.docs["s1"]?.uid)
        assertEquals(now, backend.docs["s1"]?.at)
    }

    @Test
    fun foreignFreshLockDeniesClaimAndEdit() {
        foreignClaim("s2")
        assertFalse(ScriptLockManager.claimMine("s2"))
        assertFalse(ScriptLockManager.canEdit("s2"))
        assertEquals("Anya", ScriptLockManager.lockerOf("s2")?.name)
    }

    @Test
    fun expiredForeignLockCanBeStolen() {
        foreignClaim("s2", now - ScriptLockPolicy.LOCK_TTL_MS - 1L)
        assertTrue(ScriptLockManager.canEdit("s2"))
        assertTrue(ScriptLockManager.claimMine("s2"))
        assertEquals("uid-me", backend.docs["s2"]?.uid)
    }

    @Test
    fun locksArePerScript() {
        foreignClaim("s2")
        assertTrue(ScriptLockManager.claimMine("s1"))
        assertTrue(ScriptLockManager.canEdit("s1"))
        assertFalse(ScriptLockManager.canEdit("s2"))
        assertNull(ScriptLockManager.lockerOf("s1"))
    }

    @Test
    fun backendDoubleClaimByTwoUsers() {
        var first = false
        var second = false
        backend.claim("sid-test", "x", ScriptLock("uid-me", "Petya", 10f, now), now) { first = it }
        backend.claim("sid-test", "x", ScriptLock("uid-anya", "Anya", 20f, now), now) { second = it }
        assertTrue(first)
        assertFalse(second)
        assertEquals("uid-me", backend.docs["x"]?.uid)
    }

    @Test
    fun releaseRemovesOwnLock() {
        ScriptLockManager.claimMine("s1")
        ScriptLockManager.releaseMine("s1")
        assertFalse(ScriptLockManager.isHeldByMe("s1"))
        assertTrue(backend.docs["s1"] == null)
    }

    @Test
    fun releaseDoesNotRemoveForeignLock() {
        foreignClaim("s2")
        val releasesBefore = backend.releases
        ScriptLockManager.releaseMine("s2")
        assertEquals(releasesBefore + 1, backend.releases)
        assertEquals("uid-anya", backend.docs["s2"]?.uid)
        assertFalse(ScriptLockManager.canEdit("s2"))
    }

    @Test
    fun heartbeatRefreshesHeldLocks() {
        ScriptLockManager.claimMine("s1")
        now += 20_000L
        heartbeat.fire()
        assertEquals(now, backend.docs["s1"]?.at)
        assertTrue(ScriptLockManager.isHeldByMe("s1"))
    }

    @Test
    fun observerNotifiedOnRemoteChange() {
        var notifications = 0
        ScriptLockManager.addObserver("test") { notifications++ }
        try {
            foreignClaim("s9")
            assertEquals(1, notifications)
        } finally {
            ScriptLockManager.removeObserver("test")
        }
    }

    @Test
    fun emptyScriptIdAlwaysEditable() {
        assertTrue(ScriptLockManager.canEdit(null))
        assertTrue(ScriptLockManager.canEdit(""))
        assertTrue(ScriptLockManager.claimMine(""))
        assertEquals(0, backend.claims)
    }

    @Test
    fun stopReleasesAllMine() {
        ScriptLockManager.claimMine("s1")
        ScriptLockManager.claimMine("s2")
        ScriptLockManager.stop()
        assertTrue(backend.docs.isEmpty())
        assertFalse(ScriptLockManager.isHeldByMe("s1"))
    }
}
