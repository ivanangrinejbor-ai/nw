package org.catrobat.catroid.test.collab

import org.catrobat.catroid.collab.CollabAccess
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CollabAccessTest {

    @Test
    fun revokedOnlyOnPermissionDenied() {
        assertTrue(CollabAccess.isRevoked("PERMISSION_DENIED"))
    }

    @Test
    fun transientErrorsAreNotRevocation() {
        assertFalse(CollabAccess.isRevoked("UNAVAILABLE"))
        assertFalse(CollabAccess.isRevoked("ABORTED"))
        assertFalse(CollabAccess.isRevoked("NOT_FOUND"))
        assertFalse(CollabAccess.isRevoked(null))
        assertFalse(CollabAccess.isRevoked(""))
    }
}
