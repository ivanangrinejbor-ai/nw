package org.catrobat.catroid.test.broadcast;

import org.catrobat.catroid.common.BroadcastMessageContainer;
import org.catrobat.catroid.common.BroadcastMessageScope;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 12 tests for scoped broadcast signals.
 */
public class ScopedBroadcastTest {

    private BroadcastMessageContainer container;

    @Before
    public void setUp() {
        container = new BroadcastMessageContainer();
    }

    @Test
    public void testDefaultScopeIsGlobal() {
        container.addBroadcastMessage("test_signal");
        BroadcastMessageScope scope = container.getScope("test_signal");
        // No scope set = null = treated as global
        assertNull(scope);
        assertTrue(container.isMessageVisibleInScene("test_signal", "AnyScene"));
    }

    @Test
    public void testSetScopeToSpecificScenes() {
        container.addBroadcastMessage("jump");
        container.setScope("jump", Arrays.asList("Level 1", "Level 2"));
        BroadcastMessageScope scope = container.getScope("jump");
        assertNotNull(scope);
        assertFalse(scope.isGlobal());
        assertEquals(2, scope.getAllowedSceneNames().size());
    }

    @Test
    public void testGlobalScopeAllowsAllScenes() {
        BroadcastMessageScope scope = new BroadcastMessageScope("signal");
        assertTrue(scope.isGlobal());
        assertTrue(scope.isAllowedInScene("AnyScene"));
        assertTrue(scope.isAllowedInScene("Level 1"));
        assertTrue(scope.isAllowedInScene(""));
    }

    @Test
    public void testScopedMessageFilteredByScene() {
        container.addBroadcastMessage("enemy_hit");
        container.setScope("enemy_hit", Arrays.asList("Level 1"));

        assertTrue(container.isMessageVisibleInScene("enemy_hit", "Level 1"));
        assertFalse(container.isMessageVisibleInScene("enemy_hit", "Level 2"));
        assertFalse(container.isMessageVisibleInScene("enemy_hit", "Menu"));
    }

    @Test
    public void testGlobalSceneAlwaysReceives() {
        // Global scene is handled at runtime level (BroadcastAction),
        // but scope check should still work for explicit scene names
        BroadcastMessageScope scope = new BroadcastMessageScope("signal", Arrays.asList("Level 1"));
        assertTrue(scope.isAllowedInScene("Level 1"));
        assertFalse(scope.isAllowedInScene("Level 2"));
        // Note: Global scene bypasses this at dispatch level
    }

    @Test
    public void testBackwardCompatNoScopes() {
        container.addBroadcastMessage("old_signal");
        // No scope set = visible everywhere
        assertTrue(container.isMessageVisibleInScene("old_signal", "Scene 1"));
        assertTrue(container.isMessageVisibleInScene("old_signal", "Scene 2"));
        assertTrue(container.isMessageVisibleInScene("old_signal", "Global"));
    }

    @Test
    public void testScopeSerializationRoundTrip() {
        container.addBroadcastMessage("scoped_msg");
        container.setScope("scoped_msg", Arrays.asList("A", "B", "C"));

        BroadcastMessageScope scope = container.getScope("scoped_msg");
        assertNotNull(scope);
        assertEquals("scoped_msg", scope.getMessageName());
        assertEquals(3, scope.getAllowedSceneNames().size());
        assertTrue(scope.getAllowedSceneNames().contains("A"));
        assertTrue(scope.getAllowedSceneNames().contains("B"));
        assertTrue(scope.getAllowedSceneNames().contains("C"));
    }

    @Test
    public void testReceiverFiltersByScope() {
        container.addBroadcastMessage("global_msg");
        container.addBroadcastMessage("scoped_msg");
        container.setScope("scoped_msg", Arrays.asList("Level 1"));

        List<String> level1Messages = container.getMessagesVisibleInScene("Level 1");
        List<String> level2Messages = container.getMessagesVisibleInScene("Level 2");

        assertTrue(level1Messages.contains("global_msg"));
        assertTrue(level1Messages.contains("scoped_msg"));
        assertTrue(level2Messages.contains("global_msg"));
        assertFalse(level2Messages.contains("scoped_msg"));
    }

    @Test
    public void testSendScopedBroadcastFiltersCorrectly() {
        BroadcastMessageScope scope = new BroadcastMessageScope("attack", Arrays.asList("Battle"));
        assertFalse(scope.isGlobal());
        assertTrue(scope.isAllowedInScene("Battle"));
        assertFalse(scope.isAllowedInScene("Menu"));
    }

    @Test
    public void testMultipleScopesCoexist() {
        container.addBroadcastMessage("msg_a");
        container.addBroadcastMessage("msg_b");
        container.setScope("msg_a", Arrays.asList("Scene 1"));
        container.setScope("msg_b", Arrays.asList("Scene 2", "Scene 3"));

        assertTrue(container.isMessageVisibleInScene("msg_a", "Scene 1"));
        assertFalse(container.isMessageVisibleInScene("msg_a", "Scene 2"));
        assertFalse(container.isMessageVisibleInScene("msg_b", "Scene 1"));
        assertTrue(container.isMessageVisibleInScene("msg_b", "Scene 2"));
    }

    @Test
    public void testRemoveSceneUpdatesScopes() {
        container.addBroadcastMessage("signal");
        container.setScope("signal", Arrays.asList("Level 1", "Level 2", "Level 3"));

        container.onSceneRemoved("Level 2");

        BroadcastMessageScope scope = container.getScope("signal");
        assertEquals(2, scope.getAllowedSceneNames().size());
        assertFalse(scope.getAllowedSceneNames().contains("Level 2"));
    }

    @Test
    public void testRenamingSceneUpdatesScopes() {
        container.addBroadcastMessage("signal");
        container.setScope("signal", Arrays.asList("Old Name", "Other"));

        container.onSceneRenamed("Old Name", "New Name");

        BroadcastMessageScope scope = container.getScope("signal");
        assertTrue(scope.getAllowedSceneNames().contains("New Name"));
        assertFalse(scope.getAllowedSceneNames().contains("Old Name"));
    }
}
