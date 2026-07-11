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
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ExecuteForCloneNumberBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class ExecuteForCloneNumberBrickTest {

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
    public void testExecuteForCloneNumberBrickCreatesActionWithCorrectSprite() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        ExecuteForCloneNumberBrick brick = new ExecuteForCloneNumberBrick(1);

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createExecuteForCloneNumberAction(eq(sprite),
                any(SequenceAction.class), any(Formula.class));
    }

    @Test
    public void testCompositeBrickProperties() {
        ExecuteForCloneNumberBrick brick = new ExecuteForCloneNumberBrick(3);
        assertTrue(brick.consistsOfMultipleParts());
        assertFalse(brick.hasSecondaryList());
        assertNotNull(brick.getAllParts());
        assertEquals(2, brick.getAllParts().size()); // this + endBrick
        assertNotNull(brick.getNestedBricks());
        assertTrue(brick.getNestedBricks().isEmpty());
    }

    @Test
    public void testAddBrickToCloneBranch() {
        ExecuteForCloneNumberBrick brick = new ExecuteForCloneNumberBrick(3);
        Brick mockBrick = Mockito.mock(Brick.class);
        brick.addBrickToCloneBranch(mockBrick);

        List<Brick> branchBricks = brick.getCloneBranchBricks();
        assertEquals(1, branchBricks.size());
        assertEquals(mockBrick, branchBricks.get(0));
    }

    @Test
    public void testClonePreservesBranchBricks() throws Exception {
        ExecuteForCloneNumberBrick brick = new ExecuteForCloneNumberBrick(3);
        Brick mockBrick = Mockito.mock(Brick.class);
        Mockito.when(mockBrick.clone()).thenReturn(Mockito.mock(Brick.class));
        brick.addBrickToCloneBranch(mockBrick);

        ExecuteForCloneNumberBrick cloned = (ExecuteForCloneNumberBrick) brick.clone();
        assertEquals(1, cloned.getCloneBranchBricks().size());
    }

    @Test
    public void testSetCommentedOutPropagatesToChildren() {
        ExecuteForCloneNumberBrick brick = new ExecuteForCloneNumberBrick(3);
        Brick mockBrick = Mockito.mock(Brick.class);
        brick.addBrickToCloneBranch(mockBrick);

        brick.setCommentedOut(true);
        Mockito.verify(mockBrick).setCommentedOut(true);
    }

    @Test
    public void testSetParentPropagatesToChildren() {
        ExecuteForCloneNumberBrick brick = new ExecuteForCloneNumberBrick(3);
        Brick parent = Mockito.mock(Brick.class);
        Brick mockChild = Mockito.mock(Brick.class);
        brick.addBrickToCloneBranch(mockChild);

        brick.setParent(parent);
        Mockito.verify(mockChild).setParent(brick);
    }

    @Test
    public void testRemoveChildFromBranch() {
        ExecuteForCloneNumberBrick brick = new ExecuteForCloneNumberBrick(3);
        Brick mockBrick = Mockito.mock(Brick.class);
        brick.addBrickToCloneBranch(mockBrick);

        assertTrue(brick.removeChild(mockBrick));
        assertTrue(brick.getCloneBranchBricks().isEmpty());
    }

    @Test
    public void testAddToFlatListIncludesBricksAndEndBrick() {
        ExecuteForCloneNumberBrick brick = new ExecuteForCloneNumberBrick(3);
        java.util.ArrayList<Brick> flatList = new java.util.ArrayList<>();
        brick.addToFlatList(flatList);

        assertEquals(2, flatList.size());
        assertEquals(brick, flatList.get(0));
    }
}
