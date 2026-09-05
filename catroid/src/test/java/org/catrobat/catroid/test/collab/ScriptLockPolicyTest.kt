package org.catrobat.catroid.test.collab

import org.catrobat.catroid.collab.ScriptLock
import org.catrobat.catroid.collab.ScriptLockPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScriptLockPolicyTest {

    private val now = 1_000_000L
    private val fresh = ScriptLock("uid-anya", "Anya", 20f, now - 1_000L)
    private val expired = ScriptLock("uid-anya", "Anya", 20f, now - ScriptLockPolicy.LOCK_TTL_MS - 1L)
    private val edge = ScriptLock("uid-anya", "Anya", 20f, now - ScriptLockPolicy.LOCK_TTL_MS)
    private val mine = ScriptLock("uid-me", "Petya", 10f, now - 1_000L)

    @Test
    fun freshIsFresh() {
        assertTrue(ScriptLockPolicy.isFresh(fresh, now))
    }

    @Test
    fun expiredIsNotFresh() {
        assertFalse(ScriptLockPolicy.isFresh(expired, now))
    }

    @Test
    fun edgeTtlIsStillFresh() {
        assertTrue(ScriptLockPolicy.isFresh(edge, now))
    }

    @Test
    fun nullOrEmptyUidIsNotFresh() {
        assertFalse(ScriptLockPolicy.isFresh(null, now))
        assertFalse(ScriptLockPolicy.isFresh(ScriptLock("", "x", 0f, now), now))
    }

    @Test
    fun futureTimestampIsNotFresh() {
        assertFalse(ScriptLockPolicy.isFresh(ScriptLock("u", "x", 0f, now + 5_000L), now))
    }

    @Test
    fun claimFreeOrMineOrExpired() {
        assertTrue(ScriptLockPolicy.canClaim(null, "uid-me", now))
        assertTrue(ScriptLockPolicy.canClaim(mine, "uid-me", now))
        assertTrue(ScriptLockPolicy.canClaim(expired, "uid-me", now))
    }

    @Test
    fun claimFreshForeignDenied() {
        assertFalse(ScriptLockPolicy.canClaim(fresh, "uid-me", now))
    }

    @Test
    fun lockedByOtherOnlyForFreshForeign() {
        assertEqualsLock("Anya", ScriptLockPolicy.lockedByOther(fresh, "uid-me", now)?.name)
        assertNull(ScriptLockPolicy.lockedByOther(expired, "uid-me", now))
        assertNull(ScriptLockPolicy.lockedByOther(mine, "uid-me", now))
        assertNull(ScriptLockPolicy.lockedByOther(null, "uid-me", now))
    }

    private fun assertEqualsLock(expected: String, actual: String?) {
        assertTrue("expected=$expected actual=$actual", expected == actual)
    }
}
