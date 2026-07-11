/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.actions;

import com.badlogic.gdx.utils.GdxNativesLoader;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.AttachToCameraWithOffsetAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.raptor.SceneManager;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.stage.StageListener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GdxNativesLoader.class, StageActivity.class})
public class AttachToCameraWithOffsetActionTest {

    private Sprite sprite;
    private Scope scope;
    private SceneManager sceneManager;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(GdxNativesLoader.class);
        PowerMockito.mockStatic(StageActivity.class);

        sprite = new Sprite("testSprite");
        scope = new Scope(null, sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        sceneManager = Mockito.mock(SceneManager.class);
        StageListener stageListener = Mockito.mock(StageListener.class);
        stageListener.sceneManager = sceneManager;
        PowerMockito.when(StageActivity.getActiveStageListener()).thenReturn(stageListener);
    }

    @Test
    public void testAttachToCameraWithOffsets() {
        AttachToCameraWithOffsetAction action = new AttachToCameraWithOffsetAction();
        action.setScope(scope);
        action.setObjectName(new Formula("myObject"));
        action.setOffsetX(new Formula(1.5f));
        action.setOffsetY(new Formula(2.5f));
        action.setOffsetZ(new Formula(3.5f));

        action.act(1.0f);

        Mockito.verify(sceneManager).attachObjectToCamera("myObject", 1.5f, 2.5f, 3.5f);
    }

    @Test
    public void testNullObjectNameDoesNothing() {
        AttachToCameraWithOffsetAction action = new AttachToCameraWithOffsetAction();
        action.setScope(scope);
        action.setObjectName(new Formula(""));

        action.act(1.0f);

        Mockito.verify(sceneManager, Mockito.never())
                .attachObjectToCamera(anyString(), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    public void testNullScopeDoesNothing() {
        AttachToCameraWithOffsetAction action = new AttachToCameraWithOffsetAction();
        action.act(1.0f);
        // Should not throw NPE
    }

    @Test
    public void testDefaultOffsetsWhenNull() {
        AttachToCameraWithOffsetAction action = new AttachToCameraWithOffsetAction();
        action.setScope(scope);
        action.setObjectName(new Formula("obj"));

        action.act(1.0f);

        Mockito.verify(sceneManager).attachObjectToCamera("obj", 0f, 0f, 0f);
    }

    @Test
    public void testNullStageListenerDoesNothing() {
        PowerMockito.when(StageActivity.getActiveStageListener()).thenReturn(null);

        AttachToCameraWithOffsetAction action = new AttachToCameraWithOffsetAction();
        action.setScope(scope);
        action.setObjectName(new Formula("obj"));

        action.act(1.0f);

        Mockito.verify(sceneManager, Mockito.never())
                .attachObjectToCamera(anyString(), anyFloat(), anyFloat(), anyFloat());
    }
}
