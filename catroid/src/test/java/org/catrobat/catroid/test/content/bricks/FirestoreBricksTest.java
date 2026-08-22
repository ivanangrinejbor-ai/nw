/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.bricks;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenFirestoreChangedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.AddFirestoreDocumentBrick;
import org.catrobat.catroid.content.bricks.DeleteFirestoreBrick;
import org.catrobat.catroid.content.bricks.QueryFirestoreBrick;
import org.catrobat.catroid.content.bricks.ReadFirestoreBrick;
import org.catrobat.catroid.content.bricks.UpdateFirestoreBrick;
import org.catrobat.catroid.content.bricks.WhenFirestoreChangedBrick;
import org.catrobat.catroid.content.bricks.WriteFirestoreBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class FirestoreBricksTest {

    private Sprite sprite;

    @Before
    public void setUp() throws Exception {
        Project project = new Project(MockUtil.mockContextForProject(), "Project");
        Scene currentlyPlayingScene = new Scene("Currently playing scene", project);
        sprite = new Sprite("Sprite");
        currentlyPlayingScene.addSprite(sprite);
        project.addScene(currentlyPlayingScene);
        ProjectManager.getInstance().setCurrentProject(project);
        ProjectManager.getInstance().setCurrentlyEditedScene(new Scene());
        ProjectManager.getInstance().setCurrentlyPlayingScene(currentlyPlayingScene);
    }

    @Test
    public void testWriteFirestoreBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        WriteFirestoreBrick brick = new WriteFirestoreBrick("collection/docId", "{\"score\": 10}");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createWriteFirestoreAction(eq(sprite),
                any(SequenceAction.class), any(Formula.class), any(Formula.class), any(Formula.class), any(Boolean.class));
    }

    @Test
    public void testUpdateFirestoreBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        UpdateFirestoreBrick brick = new UpdateFirestoreBrick("collection/docId", "{\"score\": 11}");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createUpdateFirestoreAction(eq(sprite),
                any(SequenceAction.class), any(Formula.class), any(Formula.class), any(Formula.class), any(Boolean.class));
    }

    @Test
    public void testAddFirestoreDocumentBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        AddFirestoreDocumentBrick brick = new AddFirestoreDocumentBrick("collection", "{\"score\": 10}");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createAddFirestoreDocumentAction(eq(sprite),
                any(SequenceAction.class), any(Formula.class), any(Formula.class), any(Formula.class), any(), any(Boolean.class));
    }

    @Test
    public void testReadFirestoreBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        ReadFirestoreBrick brick = new ReadFirestoreBrick("collection/docId");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createReadFirestoreAction(eq(sprite),
                any(SequenceAction.class), any(Formula.class), any(Formula.class), any(), any(Boolean.class));
    }

    @Test
    public void testDeleteFirestoreBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        DeleteFirestoreBrick brick = new DeleteFirestoreBrick("collection/docId");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createDeleteFirestoreAction(eq(sprite),
                any(SequenceAction.class), any(Formula.class), any(Formula.class), any(Boolean.class));
    }

    @Test
    public void testQueryFirestoreBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        QueryFirestoreBrick brick = new QueryFirestoreBrick("collection", "score", ">", "5", "10");

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createQueryFirestoreAction(eq(sprite),
                any(SequenceAction.class), any(Formula.class), any(Formula.class), any(Formula.class), eq(">"),
                any(Formula.class), any(Formula.class), any(), any(Boolean.class));
    }

    @Test
    public void testWhenFirestoreChangedBrickScriptLinkage() {
        WhenFirestoreChangedBrick brick = new WhenFirestoreChangedBrick();
        Script script = brick.getScript();
        assert (script instanceof WhenFirestoreChangedScript);
        assert (script.getScriptBrick() == brick);
    }

    @Test
    public void testWhenFirestoreChangedBrickClone() throws CloneNotSupportedException {
        WhenFirestoreChangedBrick brick = new WhenFirestoreChangedBrick();
        WhenFirestoreChangedBrick clone = (WhenFirestoreChangedBrick) brick.clone();
        assert (clone.getScript() != brick.getScript());
        assert (clone.getScript().getScriptBrick() == clone);
    }

    @Test
    public void testWriteFirestoreBrickConstructors() {
        WriteFirestoreBrick brick = new WriteFirestoreBrick("a/b", "{\"x\": 1}");
        assert brick.getFormulaWithBrickField(WriteFirestoreBrick.BrickField.FIRESTORE_PATH) != null;
        assert brick.getFormulaWithBrickField(WriteFirestoreBrick.BrickField.FIRESTORE_VALUE) != null;

        WriteFirestoreBrick waitBrick = new WriteFirestoreBrick("a/b", "{\"x\": 1}", 1);
        assert waitBrick != null;
    }
}