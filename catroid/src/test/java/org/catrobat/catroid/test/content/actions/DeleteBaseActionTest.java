/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.DeleteBaseAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class DeleteBaseActionTest {

    private Sprite sprite;
    private Scope scope;

    @Before
    public void setUp() throws Exception {
        Project project = new Project(MockUtil.mockContextForProject(), "Project");
        Scene scene = new Scene("test", project);
        sprite = new Sprite("testSprite");
        scene.addSprite(sprite);
        project.addScene(scene);
        ProjectManager.getInstance().setCurrentProject(project);
        scope = new Scope(project, sprite, null);
    }

    @Test
    public void testNullFormulasDoNotCrash() {
        DeleteBaseAction action = new DeleteBaseAction();
        action.setScope(scope);
        action.setBase(null);
        action.setKey(null);
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testNullScopeDoesNotCrash() {
        DeleteBaseAction action = new DeleteBaseAction();
        action.setScope(null);
        action.setBase(new Formula("https://test.firebaseio.com"));
        action.setKey(new Formula("key"));
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testAllNullDoesNotCrash() {
        DeleteBaseAction action = new DeleteBaseAction();
        action.setScope(null);
        action.setBase(null);
        action.setKey(null);
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testEmptyStringsDoNotCrash() {
        DeleteBaseAction action = new DeleteBaseAction();
        action.setScope(scope);
        action.setBase(new Formula(""));
        action.setKey(new Formula(""));
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testValidFormulasViaFactoryDoNotCrash() {
        assertTrue(sprite.getActionFactory()
                .createDeleteBaseAction(sprite, new SequenceAction(),
                        new Formula("https://test.firebaseio.com"),
                        new Formula("myKey"))
                .act(1.0f));
    }
}
