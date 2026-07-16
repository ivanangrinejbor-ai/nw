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
import org.catrobat.catroid.content.actions.DownloadFileFromFirebaseAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class DownloadFileFromFirebaseActionTest {

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
        DownloadFileFromFirebaseAction action = new DownloadFileFromFirebaseAction();
        action.setScope(scope);
        action.setBucket(null);
        action.setPath(null);
        action.setDestFile(null);
        action.setVariable(new UserVariable("result"));
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testNullVariableDoesNotCrash() {
        DownloadFileFromFirebaseAction action = new DownloadFileFromFirebaseAction();
        action.setScope(scope);
        action.setBucket(new Formula("bucket"));
        action.setPath(new Formula("images/a.jpg"));
        action.setDestFile(new Formula("local.png"));
        action.setVariable(null);
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testNullScopeDoesNotCrash() {
        DownloadFileFromFirebaseAction action = new DownloadFileFromFirebaseAction();
        action.setScope(null);
        action.setBucket(new Formula("bucket"));
        action.setPath(new Formula("images/a.jpg"));
        action.setDestFile(new Formula("local.png"));
        action.setVariable(new UserVariable("result"));
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testAllNullDoesNotCrash() {
        DownloadFileFromFirebaseAction action = new DownloadFileFromFirebaseAction();
        action.setScope(null);
        action.setBucket(null);
        action.setPath(null);
        action.setDestFile(null);
        action.setVariable(null);
        assertTrue(action.act(1.0f));
    }
}
