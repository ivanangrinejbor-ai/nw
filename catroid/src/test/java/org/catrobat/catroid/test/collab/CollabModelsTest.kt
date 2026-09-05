package org.catrobat.catroid.test.collab

import org.catrobat.catroid.collab.CollabInvite
import org.catrobat.catroid.collab.CollabMember
import org.catrobat.catroid.collab.CollabMeta
import org.catrobat.catroid.collab.CollabRequest
import org.catrobat.catroid.collab.CollabRoles
import org.catrobat.catroid.collab.MemberPresence
import org.catrobat.catroid.collab.ScriptLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CollabModelsTest {

    @Test
    fun metaRoundTrip() {
        val original = CollabMeta("uid1", "Petya", "Game", "owner", "repo", true, 123L)
        val restored = CollabMeta.fromMap(original.toMap())
        assertEquals(original, restored)
    }

    @Test
    fun metaDefaultsOnMissingKeys() {
        val restored = CollabMeta.fromMap(emptyMap())
        assertEquals(CollabMeta(), restored)
    }

    @Test
    fun memberToleratesNumberTypes() {
        val restored = CollabMember.fromMap(mapOf(
            "role" to CollabRoles.EDITOR,
            "githubUsername" to "anya",
            "colorHue" to 42,
            "name" to "Anya",
            "joinedAt" to 99.0
        ))
        assertEquals(CollabRoles.EDITOR, restored.role)
        assertEquals(42f, restored.colorHue)
        assertEquals(99L, restored.joinedAt)
    }

    @Test
    fun memberRoundTrip() {
        val original = CollabMember(CollabRoles.HOST, "host", 10.5f, "Host", 7L)
        assertEquals(original, CollabMember.fromMap(original.toMap()))
    }

    @Test
    fun inviteNullOnNullMap() {
        assertNull(CollabInvite.fromMap(null))
    }

    @Test
    fun inviteRoundTrip() {
        val original = CollabInvite(CollabRoles.VIEWER, 555L, "uid9")
        assertEquals(original, CollabInvite.fromMap(original.toMap()))
    }

    @Test
    fun requestNullOnNullMap() {
        assertNull(CollabRequest.fromMap(null))
    }

    @Test
    fun requestRoundTrip() {
        val original = CollabRequest("petya", "Petya", 200f, 11L)
        assertEquals(original, CollabRequest.fromMap(original.toMap()))
    }

    @Test
    fun presenceFromSnapshot() {
        val presence = MemberPresence.fromSnapshot(
            "uid1",
            mapOf("name" to "Petya", "colorHue" to 10, "role" to "editor",
                "sceneId" to "sc", "spriteId" to "sp", "tab" to "scripts", "detail" to ""),
            12345L
        )
        assertEquals("uid1", presence.uid)
        assertEquals("Petya", presence.name)
        assertEquals(10f, presence.colorHue)
        assertEquals(12345L, presence.updatedAt)
    }

    @Test
    fun lockRoundTrip() {
        val original = ScriptLock("uid1", "Petya", 33f, 777L)
        assertEquals(original, ScriptLock.fromMap(original.toMap()))
    }

    @Test
    fun lockNullOnNullMap() {
        assertNull(ScriptLock.fromMap(null))
    }

    @Test
    fun lockToleratesIntTimestamp() {
        val restored = ScriptLock.fromMap(mapOf("uid" to "u", "name" to "n", "colorHue" to 5, "at" to 100))
        assertEquals(100L, restored?.at)
        assertTrue(restored?.colorHue == 5f)
    }
}
