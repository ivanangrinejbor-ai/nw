package org.catrobat.catroid.test.globalscene;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.WhenSceneLaunchedScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ChangeXByNBrick;
import org.catrobat.catroid.content.bricks.SetXBrick;
import org.catrobat.catroid.content.bricks.WhenSceneLaunchedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.SceneStartedEventId;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * 40 tests for the "When scene starts" event brick (Global Scene only).
 * Covers: SceneStartedEventId, WhenSceneLaunchedScript, WhenSceneLaunchedBrick,
 * event matching (scene A vs B), serialization contracts and edge cases.
 */
public class WhenSceneLaunchedTest {

    private Project project;
    private Scene globalScene;
    private Scene sceneA;
    private Scene sceneB;

    @Before
    public void setUp() {
        project = new Project();
        project.setName("TestProject");

        sceneA = new Scene("Scene A", project);
        sceneB = new Scene("Scene B", project);
        project.addScene(sceneA);
        project.addScene(sceneB);

        globalScene = new Scene("Global", project);
        globalScene.setGlobalScene(true);
        project.setGlobalScene(globalScene);
    }

    // ═══════════════════════════════════════
    // 1-8: SceneStartedEventId
    // ═══════════════════════════════════════

    @Test
    public void test01_EventIdCreation() {
        SceneStartedEventId id = new SceneStartedEventId("Scene B");
        assertEquals("Scene B", id.sceneName);
    }

    @Test
    public void test02_EventIdEqualsSameScene() {
        assertEquals(new SceneStartedEventId("Scene B"), new SceneStartedEventId("Scene B"));
    }

    @Test
    public void test03_EventIdNotEqualsDifferentScene() {
        assertNotEquals(new SceneStartedEventId("Scene A"), new SceneStartedEventId("Scene B"));
    }

    @Test
    public void test04_EventIdHashCodeConsistent() {
        assertEquals(new SceneStartedEventId("X").hashCode(), new SceneStartedEventId("X").hashCode());
    }

    @Test
    public void test05_EventIdNullSceneName() {
        SceneStartedEventId id = new SceneStartedEventId(null);
        assertNull(id.sceneName);
        assertEquals(0, id.hashCode());
    }

    @Test
    public void test06_EventIdNullEqualsNull() {
        assertEquals(new SceneStartedEventId(null), new SceneStartedEventId(null));
    }

    @Test
    public void test07_EventIdNotEqualsPlainEventId() {
        SceneStartedEventId sceneId = new SceneStartedEventId("Scene A");
        EventId plain = new EventId(EventId.START);
        assertNotEquals(sceneId, plain);
    }

    @Test
    public void test08_EventIdNotEqualsNull() {
        assertNotEquals(new SceneStartedEventId("Scene A"), null);
    }

    // ═══════════════════════════════════════
    // 9-16: WhenSceneLaunchedScript
    // ═══════════════════════════════════════

    @Test
    public void test09_ScriptDefaultConstructor() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript();
        assertNull(script.getSceneName());
    }

    @Test
    public void test10_ScriptSceneNameConstructor() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene B");
        assertEquals("Scene B", script.getSceneName());
    }

    @Test
    public void test11_ScriptSetSceneName() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript();
        script.setSceneName("Scene A");
        assertEquals("Scene A", script.getSceneName());
    }

    @Test
    public void test12_ScriptCreateEventId() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene B");
        Sprite sprite = new Sprite("Manager");
        EventId eventId = script.createEventId(sprite);
        assertTrue(eventId instanceof SceneStartedEventId);
        assertEquals("Scene B", ((SceneStartedEventId) eventId).sceneName);
    }

    @Test
    public void test13_ScriptGetScriptBrickCreatesBrick() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene A");
        assertNotNull(script.getScriptBrick());
        assertTrue(script.getScriptBrick() instanceof WhenSceneLaunchedBrick);
    }

    @Test
    public void test14_ScriptGetScriptBrickCached() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene A");
        assertSame(script.getScriptBrick(), script.getScriptBrick());
    }

    @Test
    public void test15_ScriptCloneKeepsSceneName() throws CloneNotSupportedException {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene B");
        WhenSceneLaunchedScript clone = (WhenSceneLaunchedScript) script.clone();
        assertEquals("Scene B", clone.getSceneName());
        assertNotSame(script, clone);
    }

    @Test
    public void test16_ScriptBrickListIndependentAfterClone() throws CloneNotSupportedException {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene B");
        script.addBrick(new SetXBrick());
        WhenSceneLaunchedScript clone = (WhenSceneLaunchedScript) script.clone();
        clone.addBrick(new ChangeXByNBrick());
        assertEquals(1, script.getBrickList().size());
        assertEquals(2, clone.getBrickList().size());
    }

    // ═══════════════════════════════════════
    // 17-24: WhenSceneLaunchedBrick
    // ═══════════════════════════════════════

    @Test
    public void test17_BrickDefaultConstructor() {
        WhenSceneLaunchedBrick brick = new WhenSceneLaunchedBrick();
        assertNotNull(brick.getScript());
        assertNull(brick.getSceneName());
    }

    @Test
    public void test18_BrickScriptBinding() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene A");
        WhenSceneLaunchedBrick brick = new WhenSceneLaunchedBrick(script);
        assertSame(script, brick.getScript());
        assertSame(brick, script.getScriptBrick());
    }

    @Test
    public void test19_BrickSetSceneName() {
        WhenSceneLaunchedBrick brick = new WhenSceneLaunchedBrick();
        brick.setSceneName("Scene B");
        assertEquals("Scene B", brick.getSceneName());
    }

    @Test
    public void test20_BrickViewResource() {
        WhenSceneLaunchedBrick brick = new WhenSceneLaunchedBrick();
        assertEquals(org.catrobat.catroid.R.layout.brick_when_scene_launched, brick.getViewResource());
    }

    @Test
    public void test21_BrickClone() throws CloneNotSupportedException {
        WhenSceneLaunchedBrick brick = new WhenSceneLaunchedBrick();
        brick.setSceneName("Scene B");
        WhenSceneLaunchedBrick clone = (WhenSceneLaunchedBrick) brick.clone();
        assertEquals("Scene B", clone.getSceneName());
        assertNotSame(brick, clone);
        assertNotSame(brick.getScript(), clone.getScript());
    }

    @Test
    public void test22_BrickCloneScriptBinding() throws CloneNotSupportedException {
        WhenSceneLaunchedBrick brick = new WhenSceneLaunchedBrick();
        WhenSceneLaunchedBrick clone = (WhenSceneLaunchedBrick) brick.clone();
        assertSame(clone, clone.getScript().getScriptBrick());
    }

    @Test
    public void test23_BrickCommentedOutPropagates() {
        WhenSceneLaunchedBrick brick = new WhenSceneLaunchedBrick();
        brick.setCommentedOut(true);
        assertTrue(brick.getScript().isCommentedOut());
    }

    @Test
    public void test24_BrickAddActionToSequenceIsNoOp() {
        WhenSceneLaunchedBrick brick = new WhenSceneLaunchedBrick();
        // Event bricks don't add actions; must not throw
        brick.addActionToSequence(new Sprite("S"), null);
    }

    // ═══════════════════════════════════════
    // 25-32: Event matching — scene A vs B (КЛЮЧЕВЫЕ)
    // ═══════════════════════════════════════

    @Test
    public void test25_ScriptForSceneBMatchesSceneBEvent() {
        // Глобальная сцена: блок настроен на сцену Б
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene B");
        Sprite manager = new Sprite("Manager");
        globalScene.addSprite(manager);
        manager.addScript(script);

        // Запускается сцена Б → событие
        SceneStartedEventId fired = new SceneStartedEventId("Scene B");
        EventId scriptEventId = script.createEventId(manager);

        assertEquals(fired, scriptEventId); // Триггерится!
    }

    @Test
    public void test26_ScriptForSceneBDoesNotMatchSceneAEvent() {
        // Блок на сцену Б НЕ должен сработать при запуске сцены А
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene B");
        Sprite manager = new Sprite("Manager");
        globalScene.addSprite(manager);
        manager.addScript(script);

        SceneStartedEventId firedA = new SceneStartedEventId("Scene A");
        EventId scriptEventId = script.createEventId(manager);

        assertNotEquals(firedA, scriptEventId); // НЕ триггерится
    }

    @Test
    public void test27_TwoScriptsDifferentScenes() {
        // Менеджер с двумя скриптами: один на А, другой на Б
        Sprite manager = new Sprite("Manager");
        globalScene.addSprite(manager);
        WhenSceneLaunchedScript scriptA = new WhenSceneLaunchedScript("Scene A");
        WhenSceneLaunchedScript scriptB = new WhenSceneLaunchedScript("Scene B");
        manager.addScript(scriptA);
        manager.addScript(scriptB);

        SceneStartedEventId firedB = new SceneStartedEventId("Scene B");
        assertNotEquals(firedB, scriptA.createEventId(manager)); // A не сработал
        assertEquals(firedB, scriptB.createEventId(manager));    // B сработал
    }

    @Test
    public void test28_MultipleManagersSameScene() {
        // Два глобальных объекта слушают одну сцену — оба должны получить событие
        Sprite m1 = new Sprite("MusicManager");
        Sprite m2 = new Sprite("HudManager");
        globalScene.addSprite(m1);
        globalScene.addSprite(m2);
        WhenSceneLaunchedScript s1 = new WhenSceneLaunchedScript("Scene B");
        WhenSceneLaunchedScript s2 = new WhenSceneLaunchedScript("Scene B");
        m1.addScript(s1);
        m2.addScript(s2);

        SceneStartedEventId fired = new SceneStartedEventId("Scene B");
        assertEquals(fired, s1.createEventId(m1));
        assertEquals(fired, s2.createEventId(m2));
    }

    @Test
    public void test29_EventCaseSensitiveSceneNames() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("scene b");
        SceneStartedEventId fired = new SceneStartedEventId("Scene B");
        assertNotEquals(fired, script.createEventId(new Sprite("M")));
    }

    @Test
    public void test30_EventWithRenamedScene() {
        // После переименования сцены скрипт со старым именем не триггерится
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene B");
        sceneB.setName("Level 2");
        SceneStartedEventId fired = new SceneStartedEventId(sceneB.getName());
        assertNotEquals(fired, script.createEventId(new Sprite("M")));
    }

    @Test
    public void test31_ScriptWithNullSceneNeverMatchesRealScene() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript();
        SceneStartedEventId fired = new SceneStartedEventId("Scene A");
        assertNotEquals(fired, script.createEventId(new Sprite("M")));
    }

    @Test
    public void test32_EventForGlobalSceneNameItself() {
        // Скрипт слушающий "Global" — сработает только если событие с "Global"
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Global");
        assertEquals(new SceneStartedEventId("Global"), script.createEventId(new Sprite("M")));
        assertNotEquals(new SceneStartedEventId("Scene A"), script.createEventId(new Sprite("M")));
    }

    // ═══════════════════════════════════════
    // 33-40: Модель проекта + структура
    // ═══════════════════════════════════════

    @Test
    public void test33_GlobalSceneHoldsBrickScript() {
        Sprite manager = new Sprite("Manager");
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene B");
        manager.addScript(script);
        globalScene.addSprite(manager);

        assertEquals(1, project.getGlobalScene().getSpriteList().size());
        Script stored = project.getGlobalScene().getSpriteList().get(0).getScriptList().get(0);
        assertTrue(stored instanceof WhenSceneLaunchedScript);
        assertEquals("Scene B", ((WhenSceneLaunchedScript) stored).getSceneName());
    }

    @Test
    public void test34_ScriptHoldsChildBricks() {
        WhenSceneLaunchedScript script = new WhenSceneLaunchedScript("Scene A");
        script.addBrick(new SetXBrick());
        script.addBrick(new ChangeXByNBrick());
        assertEquals(2, script.getBrickList().size());
    }

    @Test
    public void test35_SceneAAndBBothExist() {
        assertEquals(2, project.getSceneList().size());
        assertNotNull(project.getSceneByName("Scene A"));
        assertNotNull(project.getSceneByName("Scene B"));
    }

    @Test
    public void test36_GlobalSceneNotInSceneList() {
        assertFalse(project.getSceneList().contains(globalScene));
        assertTrue(project.hasGlobalScene());
    }

    @Test
    public void test37_ManagerSpriteInGlobalSprites() {
        Sprite manager = new Sprite("Manager");
        globalScene.addSprite(manager);
        assertTrue(project.getAllGlobalSprites().contains(manager));
    }

    @Test
    public void test38_EventIdsForAllScenesDistinct() {
        SceneStartedEventId idA = new SceneStartedEventId("Scene A");
        SceneStartedEventId idB = new SceneStartedEventId("Scene B");
        SceneStartedEventId idG = new SceneStartedEventId("Global");
        assertNotEquals(idA, idB);
        assertNotEquals(idB, idG);
        assertNotEquals(idA, idG);
    }

    @Test
    public void test39_ScriptListSurvivesMultipleClones() throws CloneNotSupportedException {
        WhenSceneLaunchedBrick brick = new WhenSceneLaunchedBrick();
        brick.setSceneName("Scene B");
        Brick c1 = brick.clone();
        Brick c2 = c1.clone();
        assertEquals("Scene B", ((WhenSceneLaunchedBrick) c2).getSceneName());
    }

    @Test
    public void test40_FullScenario_GlobalManagerTriggersOnlyOnSceneB() {
        // ПОЛНЫЙ СЦЕНАРИЙ: глобальная сцена + сцены А и Б.
        // Менеджер в глобальной сцене слушает "Scene B".
        Sprite manager = new Sprite("SceneWatcher");
        WhenSceneLaunchedScript watchB = new WhenSceneLaunchedScript("Scene B");
        watchB.addBrick(new SetXBrick());
        manager.addScript(watchB);
        globalScene.addSprite(manager);

        // Также обычный StartScript — не должен конфликтовать
        manager.addScript(new StartScript());

        // 1) Запуск сцены А → событие А
        SceneStartedEventId eventA = new SceneStartedEventId("Scene A");
        // 2) Запуск сцены Б → событие Б
        SceneStartedEventId eventB = new SceneStartedEventId("Scene B");

        EventId watcherEventId = watchB.createEventId(manager);

        // Скрипт должен сработать ТОЛЬКО на сцену Б
        assertNotEquals("Не должен триггериться на сцену А", eventA, watcherEventId);
        assertEquals("Должен триггериться на сцену Б", eventB, watcherEventId);

        // Модель целостна
        assertEquals(2, manager.getScriptList().size());
        assertTrue(project.getAllGlobalSprites().contains(manager));
        assertEquals(1, watchB.getBrickList().size());
    }
}
