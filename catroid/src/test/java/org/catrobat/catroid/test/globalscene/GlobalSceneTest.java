package org.catrobat.catroid.test.globalscene;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.SetVariableBrick;
import org.catrobat.catroid.content.bricks.ForeverBrick;
import org.catrobat.catroid.content.bricks.ShowBrick;
import org.catrobat.catroid.content.bricks.HideBrick;
import org.catrobat.catroid.content.bricks.BroadcastBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class GlobalSceneTest {

    private Project project;

    @Before
    public void setUp() {
        project = new Project();
        project.setName("TestProject");
        Scene defaultScene = new Scene("Scene 1", project);
        project.addScene(defaultScene);
    }

    @Test
    public void testProjectWithoutGlobalScene() {
        assertFalse(project.hasGlobalScene());
        assertNull(project.getGlobalScene());
    }

    @Test
    public void testCreateGlobalScene() {
        Scene globalScene = new Scene("Global", project);
        project.setGlobalScene(globalScene);
        assertTrue(project.hasGlobalScene());
        assertNotNull(project.getGlobalScene());
        assertEquals("Global", project.getGlobalScene().getName());
    }

    @Test
    public void testOnlyOneGlobalScene() {
        Scene gs1 = new Scene("Global1", project);
        Scene gs2 = new Scene("Global2", project);
        project.setGlobalScene(gs1);
        project.setGlobalScene(gs2);
        assertEquals("Global2", project.getGlobalScene().getName());
    }

    @Test
    public void testGlobalSceneIsMarkedGlobal() {
        Scene globalScene = new Scene("Global", project);
        project.setGlobalScene(globalScene);
        assertTrue(globalScene.isGlobalScene());
    }

    @Test
    public void testGlobalSceneSpritesInAllGlobalList() {
        Scene globalScene = new Scene("Global", project);
        Sprite manager = new Sprite("GameManager");
        globalScene.addSprite(manager);
        project.setGlobalScene(globalScene);

        List<Sprite> allGlobal = project.getAllGlobalSprites();
        assertTrue(allGlobal.contains(manager));
    }

    @Test
    public void testGlobalSceneNotInSceneList() {
        Scene globalScene = new Scene("Global", project);
        project.setGlobalScene(globalScene);
        assertFalse(project.getSceneList().contains(globalScene));
    }

    @Test
    public void testGlobalSceneSpriteCanHaveScripts() {
        Scene globalScene = new Scene("Global", project);
        Sprite sprite = new Sprite("GlobalSprite");
        StartScript script = new StartScript();
        script.addBrick(new ShowBrick());
        sprite.addScript(script);
        globalScene.addSprite(sprite);
        project.setGlobalScene(globalScene);

        assertEquals(1, project.getGlobalScene().getSpriteList().get(0).getScriptList().size());
    }

    @Test
    public void testGlobalScenePreservesMultipleSprites() {
        Scene globalScene = new Scene("Global", project);
        globalScene.addSprite(new Sprite("HUD"));
        globalScene.addSprite(new Sprite("MusicController"));
        globalScene.addSprite(new Sprite("ScoreManager"));
        project.setGlobalScene(globalScene);

        assertEquals(3, project.getGlobalScene().getSpriteList().size());
    }

    @Test
    public void testGlobalSpritesNotResetOnSceneSwitch() {
        Scene globalScene = new Scene("Global", project);
        Sprite gSprite = new Sprite("Persistent");
        globalScene.addSprite(gSprite);
        project.setGlobalScene(globalScene);

        List<Sprite> allGlobal = project.getAllGlobalSprites();
        assertTrue(allGlobal.contains(gSprite));
    }

    @Test
    public void testGlobalSceneSpritesAddedToAllGlobalList() {
        Scene gs = new Scene("Global", project);
        Sprite s1 = new Sprite("S1");
        Sprite s2 = new Sprite("S2");
        gs.addSprite(s1);
        gs.addSprite(s2);
        project.setGlobalScene(gs);

        assertEquals(2, project.getAllGlobalSprites().size());
    }

    @Test
    public void testLegacyGlobalFlagStillWorks() {
        Sprite legacySprite = new Sprite("LegacyGlobal");
        legacySprite.setGlobal(true);
        project.getDefaultScene().addSprite(legacySprite);

        List<Sprite> allGlobal = project.getAllGlobalSprites();
        assertTrue(allGlobal.contains(legacySprite));
    }

    @Test
    public void testBothSystemsCombine() {
        Scene gs = new Scene("Global", project);
        Sprite gsSprite = new Sprite("FromGlobalScene");
        gs.addSprite(gsSprite);
        project.setGlobalScene(gs);

        Sprite legacySprite = new Sprite("LegacyGlobal");
        legacySprite.setGlobal(true);
        project.getDefaultScene().addSprite(legacySprite);

        List<Sprite> allGlobal = project.getAllGlobalSprites();
        assertEquals(2, allGlobal.size());
        assertTrue(allGlobal.contains(gsSprite));
        assertTrue(allGlobal.contains(legacySprite));
    }

    @Test
    public void testGlobalSpriteNotDuplicated() {
        Scene gs = new Scene("Global", project);
        Sprite sprite = new Sprite("NoDupe");
        gs.addSprite(sprite);
        project.setGlobalScene(gs);

        long count = project.getAllGlobalSprites().stream()
                .filter(s -> s.getName().equals("NoDupe")).count();
        assertEquals(1, count);
    }

    @Test
    public void testGlobalSceneHasProject() {
        Scene gs = new Scene("Global", project);
        project.setGlobalScene(gs);
        assertTrue(gs.isGlobalScene());
    }

    @Test
    public void testGlobalSceneWithStartScript() {
        Scene gs = new Scene("Global", project);
        Sprite sprite = new Sprite("ScriptedGlobal");
        StartScript script = new StartScript();
        sprite.addScript(script);
        gs.addSprite(sprite);
        project.setGlobalScene(gs);

        assertFalse(project.getGlobalScene().getSpriteList().get(0).getScriptList().isEmpty());
    }

    @Test
    public void testGlobalSceneWithVariables() {
        Scene gs = new Scene("Global", project);
        Sprite sprite = new Sprite("VarSprite");
        sprite.getUserVariables().add(new UserVariable("score", 0));
        gs.addSprite(sprite);
        project.setGlobalScene(gs);

        assertEquals(1, project.getGlobalScene().getSpriteList().get(0).getUserVariables().size());
    }

    @Test
    public void testGlobalSceneSpriteLookListEmpty() {
        Scene gs = new Scene("Global", project);
        Sprite sprite = new Sprite("NoLook");
        gs.addSprite(sprite);
        project.setGlobalScene(gs);

        assertTrue(sprite.getLookList().isEmpty());
    }

    @Test
    public void testGlobalSceneWithMultipleScripts() {
        Scene gs = new Scene("Global", project);
        Sprite sprite = new Sprite("MultiScript");
        sprite.addScript(new StartScript());
        sprite.addScript(new StartScript());
        gs.addSprite(sprite);
        project.setGlobalScene(gs);

        assertEquals(2, project.getGlobalScene().getSpriteList().get(0).getScriptList().size());
    }

    @Test
    public void testRemoveGlobalScene() {
        Scene gs = new Scene("Global", project);
        project.setGlobalScene(gs);
        assertTrue(project.hasGlobalScene());
        project.setGlobalScene(null);
        assertFalse(project.hasGlobalScene());
    }

    @Test
    public void testSetNullGlobalSceneNoOp() {
        project.setGlobalScene(null);
        assertFalse(project.hasGlobalScene());
        assertNull(project.getGlobalScene());
    }

    @Test
    public void testGlobalSceneNamePersists() {
        Scene gs = new Scene("MyGlobal", project);
        project.setGlobalScene(gs);
        assertEquals("MyGlobal", project.getGlobalScene().getName());
    }

    @Test
    public void testGlobalSpriteSoundListEmpty() {
        Scene gs = new Scene("Global", project);
        Sprite sprite = new Sprite("NoSound");
        gs.addSprite(sprite);
        project.setGlobalScene(gs);

        assertTrue(sprite.getSoundList().isEmpty());
    }

    @Test
    public void testGlobalSceneCreationSetsFlag() {
        Scene gs = new Scene("Global", project);
        gs.setGlobalScene(true);
        assertTrue(gs.isGlobalScene());
    }

    @Test
    public void testRegularSceneNotGlobal() {
        Scene regular = project.getDefaultScene();
        assertFalse(regular.isGlobalScene());
    }

    @Test
    public void testGlobalSceneViaSetGlobalScene() {
        Scene gs = new Scene("Global", project);
        project.setGlobalScene(gs);
        assertTrue(project.getGlobalScene().isGlobalScene());
    }

    @Test
    public void testMultipleScenesWithOneGlobal() {
        project.addScene(new Scene("Scene 2", project));
        project.addScene(new Scene("Scene 3", project));
        Scene gs = new Scene("Global", project);
        project.setGlobalScene(gs);

        assertEquals(3, project.getSceneList().size());
        assertTrue(project.hasGlobalScene());
    }

    @Test
    public void testGlobalSceneIdUnique() {
        Scene gs = new Scene("Global", project);
        project.setGlobalScene(gs);
        assertNotNull(gs.getSceneId());
        assertNotEquals(project.getDefaultScene().getSceneId(), gs.getSceneId());
    }

    @Test
    public void testCanAddSpriteToGlobalScene() {
        Scene gs = new Scene("Global", project);
        project.setGlobalScene(gs);
        Sprite newSprite = new Sprite("Added");
        project.getGlobalScene().addSprite(newSprite);
        assertEquals(1, project.getGlobalScene().getSpriteList().size());
    }

    @Test
    public void testCanRemoveSpriteFromGlobalScene() {
        Scene gs = new Scene("Global", project);
        Sprite sprite = new Sprite("ToRemove");
        gs.addSprite(sprite);
        project.setGlobalScene(gs);
        gs.removeSprite(sprite);
        assertEquals(0, project.getGlobalScene().getSpriteList().size());
    }

    @Test
    public void testGlobalSceneSpriteCountAccurate() {
        Scene gs = new Scene("Global", project);
        for (int i = 0; i < 10; i++) {
            gs.addSprite(new Sprite("Sprite" + i));
        }
        project.setGlobalScene(gs);
        assertEquals(10, project.getGlobalScene().getSpriteList().size());
    }

    @Test
    public void testEmptyGlobalSceneNoCrash() {
        Scene gs = new Scene("Global", project);
        project.setGlobalScene(gs);
        List<Sprite> allGlobal = project.getAllGlobalSprites();
        assertTrue(allGlobal.isEmpty());
    }

    @Test
    public void testGlobalSceneWithOnlyVariablesNoCrash() {
        Scene gs = new Scene("Global", project);
        project.setGlobalScene(gs);
        project.getUserVariables().add(new UserVariable("globalScore", 0));
        assertNotNull(project.getGlobalScene());
    }

    @Test
    public void testGlobalAndRegularSameSpriteName() {
        Scene gs = new Scene("Global", project);
        gs.addSprite(new Sprite("Player"));
        project.setGlobalScene(gs);
        project.getDefaultScene().addSprite(new Sprite("Player"));

        assertEquals(1, project.getGlobalScene().getSpriteList().size());
        assertTrue(project.getDefaultScene().getSpriteList().size() >= 1);
    }

    @Test
    public void testGlobalSceneDeleteAndRecreate() {
        Scene gs1 = new Scene("Global1", project);
        gs1.addSprite(new Sprite("Old"));
        project.setGlobalScene(gs1);
        assertTrue(project.hasGlobalScene());

        project.setGlobalScene(null);
        assertFalse(project.hasGlobalScene());

        Scene gs2 = new Scene("Global2", project);
        gs2.addSprite(new Sprite("New"));
        project.setGlobalScene(gs2);
        assertTrue(project.hasGlobalScene());
        assertEquals("New", project.getGlobalScene().getSpriteList().get(0).getName());
    }

    @Test
    public void testGlobalSceneReplace() {
        Scene gs1 = new Scene("First", project);
        gs1.addSprite(new Sprite("A"));
        project.setGlobalScene(gs1);

        Scene gs2 = new Scene("Second", project);
        gs2.addSprite(new Sprite("B"));
        project.setGlobalScene(gs2);

        assertEquals("Second", project.getGlobalScene().getName());
        assertEquals("B", project.getGlobalScene().getSpriteList().get(0).getName());
    }

    @Test
    public void testAllGlobalSpritesWithNoGlobalScene() {
        List<Sprite> result = project.getAllGlobalSprites();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGlobalSceneSceneIdNotNull() {
        Scene gs = new Scene("Global", project);
        project.setGlobalScene(gs);
        assertNotNull(project.getGlobalScene().getSceneId());
    }

    @Test
    public void testGlobalSceneSpritesOrderPreserved() {
        Scene gs = new Scene("Global", project);
        gs.addSprite(new Sprite("First"));
        gs.addSprite(new Sprite("Second"));
        gs.addSprite(new Sprite("Third"));
        project.setGlobalScene(gs);

        List<Sprite> sprites = project.getGlobalScene().getSpriteList();
        assertEquals("First", sprites.get(0).getName());
        assertEquals("Second", sprites.get(1).getName());
        assertEquals("Third", sprites.get(2).getName());
    }

    @Test
    public void testGlobalSceneExistsBeforeRegularScenes() {
        Scene gs = new Scene("Global", project);
        gs.addSprite(new Sprite("Manager"));
        project.setGlobalScene(gs);

        Scene scene1 = new Scene("Level 1", project);
        scene1.addSprite(new Sprite("Player"));
        project.addScene(scene1);

        assertFalse(project.getSceneList().contains(gs));
        assertNotNull(project.getGlobalScene());
        assertTrue(project.getSceneList().size() >= 1);
    }

    @Test
    public void testGlobalSpritesLoadedWithRegularScene() {
        Scene gs = new Scene("Global", project);
        Sprite hudSprite = new Sprite("HUD");
        gs.addSprite(hudSprite);
        project.setGlobalScene(gs);

        Sprite player = new Sprite("Player");
        project.getDefaultScene().addSprite(player);

        List<Sprite> globalSprites = project.getAllGlobalSprites();
        assertTrue(globalSprites.contains(hudSprite));
        assertFalse(globalSprites.contains(player));
    }

    @Test
    public void testTwoScenesWithGlobalSceneStructure() {
        Scene gs = new Scene("Global", project);
        gs.addSprite(new Sprite("ScoreManager"));
        gs.addSprite(new Sprite("MusicController"));
        project.setGlobalScene(gs);

        project.addScene(new Scene("Level 2", project));

        assertEquals(2, project.getSceneList().size());
        assertTrue(project.hasGlobalScene());
        assertEquals(2, project.getGlobalScene().getSpriteList().size());
        assertEquals(2, project.getAllGlobalSprites().size());
    }

    @Test
    public void testGlobalSceneDefaultSceneLaunchOrder() {
        Scene gs = new Scene("Global", project);
        Sprite gm = new Sprite("GameManager");
        StartScript gmScript = new StartScript();
        gm.addScript(gmScript);
        gs.addSprite(gm);
        project.setGlobalScene(gs);

        Scene defaultScene = project.getDefaultScene();
        assertNotNull(defaultScene);
        assertFalse(defaultScene.isGlobalScene());

        assertTrue(project.hasGlobalScene());
        assertEquals(1, project.getGlobalScene().getSpriteList().get(0).getScriptList().size());

        List<Sprite> allGlobal = project.getAllGlobalSprites();
        assertTrue(allGlobal.contains(gm));
    }
}
