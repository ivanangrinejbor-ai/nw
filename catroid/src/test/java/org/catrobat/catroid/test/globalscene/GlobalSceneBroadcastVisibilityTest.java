package org.catrobat.catroid.test.globalscene;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.BroadcastScript;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.BroadcastBrick;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GlobalSceneBroadcastVisibilityTest {

    private Project project;
    private Scene globalScene;
    private Scene sceneA;
    private Scene sceneB;
    private ProjectManager projectManager;

    @Before
    public void setUp() {
        new ProjectManager(null);
        projectManager = ProjectManager.getInstance();

        project = new Project();
        project.setName("SignalTestProject");

        sceneA = new Scene("Scene A", project);
        sceneB = new Scene("Scene B", project);
        project.addScene(sceneA);
        project.addScene(sceneB);

        globalScene = new Scene("Global", project);
        globalScene.setGlobalScene(true);
        project.setGlobalScene(globalScene);

        projectManager.setCurrentProject(project);
    }

    private void addReceiverTo(Scene scene, String spriteName, String signal) {
        Sprite sprite = new Sprite(spriteName);
        sprite.addScript(new BroadcastScript(signal));
        scene.addSprite(sprite);
    }

    private void addSenderTo(Scene scene, String spriteName, String signal) {
        Sprite sprite = new Sprite(spriteName);
        StartScript script = new StartScript();
        script.addBrick(new BroadcastBrick(signal));
        sprite.addScript(script);
        scene.addSprite(sprite);
    }

    private List<String> messagesWhileEditing(Scene scene) {
        projectManager.setCurrentlyEditedScene(scene);
        project.getBroadcastMessageContainer().update();
        return project.getBroadcastMessageContainer().getBroadcastMessages();
    }

    @Test
    public void testSignalFromRegularSceneVisibleInGlobalScene() {
        addReceiverTo(sceneA, "Player", "jump_signal");

        List<String> messages = messagesWhileEditing(globalScene);
        assertTrue("Сигнал из Scene A должен быть виден в глобальной сцене",
                messages.contains("jump_signal"));
    }

    @Test
    public void testSenderSignalFromRegularSceneVisibleInGlobalScene() {
        addSenderTo(sceneA, "Enemy", "attack_signal");

        List<String> messages = messagesWhileEditing(globalScene);
        assertTrue("Сигнал-отправка из Scene A должен быть виден в глобальной сцене",
                messages.contains("attack_signal"));
    }

    @Test
    public void testSignalsFromMultipleScenesAllVisibleInGlobal() {
        addReceiverTo(sceneA, "A1", "signal_a");
        addReceiverTo(sceneB, "B1", "signal_b");

        List<String> messages = messagesWhileEditing(globalScene);
        assertTrue(messages.contains("signal_a"));
        assertTrue(messages.contains("signal_b"));
    }

    @Test
    public void testGlobalSceneOwnSignalsAlsoIncluded() {
        addReceiverTo(globalScene, "Manager", "global_signal");
        addReceiverTo(sceneA, "Player", "scene_signal");

        List<String> messages = messagesWhileEditing(globalScene);
        assertTrue(messages.contains("global_signal"));
        assertTrue(messages.contains("scene_signal"));
    }

    @Test
    public void testRegularSceneSeesOnlyOwnSignals() {
        addReceiverTo(sceneA, "A1", "signal_a");
        addReceiverTo(sceneB, "B1", "signal_b");

        List<String> messages = messagesWhileEditing(sceneA);
        assertTrue(messages.contains("signal_a"));
        assertFalse("Обычная сцена НЕ должна видеть сигналы другой сцены",
                messages.contains("signal_b"));
    }

    @Test
    public void testRegularSceneDoesNotSeeGlobalOnlySignals() {
        addReceiverTo(globalScene, "Manager", "global_only");

        List<String> messages = messagesWhileEditing(sceneA);
        assertFalse(messages.contains("global_only"));
    }

    @Test
    public void testDuplicateSignalAppearsOnce() {
        addReceiverTo(sceneA, "A1", "shared");
        addReceiverTo(sceneB, "B1", "shared");
        addReceiverTo(globalScene, "G1", "shared");

        List<String> messages = messagesWhileEditing(globalScene);
        int count = 0;
        for (String m : messages) {
            if ("shared".equals(m)) {
                count++;
            }
        }
        assertEquals("Дубликат сигнала должен схлопнуться в один", 1, count);
    }

    @Test
    public void testEmptyProjectNoSignals() {
        List<String> messages = messagesWhileEditing(globalScene);
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testProjectWithoutGlobalSceneStillWorks() {
        Project plain = new Project();
        plain.setName("Plain");
        Scene only = new Scene("Only", plain);
        plain.addScene(only);
        Sprite s = new Sprite("S");
        s.addScript(new BroadcastScript("msg"));
        only.addSprite(s);

        projectManager.setCurrentProject(plain);
        projectManager.setCurrentlyEditedScene(only);
        plain.getBroadcastMessageContainer().update();

        assertTrue(plain.getBroadcastMessageContainer().getBroadcastMessages().contains("msg"));
    }

    @Test
    public void testSwitchingEditedSceneRefreshesList() {
        addReceiverTo(sceneA, "A1", "signal_a");
        addReceiverTo(sceneB, "B1", "signal_b");

        List<String> inA = messagesWhileEditing(sceneA);
        assertTrue(inA.contains("signal_a"));
        assertFalse(inA.contains("signal_b"));

        List<String> inB = messagesWhileEditing(sceneB);
        assertTrue(inB.contains("signal_b"));
        assertFalse(inB.contains("signal_a"));

        List<String> inGlobal = messagesWhileEditing(globalScene);
        assertTrue(inGlobal.contains("signal_a"));
        assertTrue(inGlobal.contains("signal_b"));
    }
}
