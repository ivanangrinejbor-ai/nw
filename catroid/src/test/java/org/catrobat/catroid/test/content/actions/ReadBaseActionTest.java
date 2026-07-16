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
import org.catrobat.catroid.content.actions.ReadBaseAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ReadBaseActionTest {

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
        ReadBaseAction action = new ReadBaseAction();
        action.setScope(scope);
        action.setBase(null);
        action.setKey(null);
        action.setVariable(new UserVariable("result"));
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testNullVariableDoesNotCrash() {
        ReadBaseAction action = new ReadBaseAction();
        action.setScope(scope);
        action.setBase(new Formula("https://test.firebaseio.com"));
        action.setKey(new Formula("myKey"));
        action.setVariable(null);
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testNullScopeDoesNotCrash() {
        ReadBaseAction action = new ReadBaseAction();
        action.setScope(null);
        action.setBase(new Formula("https://test.firebaseio.com"));
        action.setKey(new Formula("myKey"));
        action.setVariable(new UserVariable("result"));
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testAllNullDoesNotCrash() {
        ReadBaseAction action = new ReadBaseAction();
        action.setScope(null);
        action.setBase(null);
        action.setKey(null);
        action.setVariable(null);
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testEmptyStringsDoNotCrash() {
        ReadBaseAction action = new ReadBaseAction();
        action.setScope(scope);
        action.setBase(new Formula(""));
        action.setKey(new Formula(""));
        action.setVariable(new UserVariable("result"));
        assertTrue(action.act(1.0f));
    }

    @Test
    public void testValidFormulasViaFactoryDoNotCrash() {
        UserVariable variable = new UserVariable("result");
        assertTrue(sprite.getActionFactory()
                .createReadBaseAction(sprite, new SequenceAction(),
                        new Formula("https://test.firebaseio.com"),
                        new Formula("myKey"),
                        variable)
                .act(1.0f));
    }
}
