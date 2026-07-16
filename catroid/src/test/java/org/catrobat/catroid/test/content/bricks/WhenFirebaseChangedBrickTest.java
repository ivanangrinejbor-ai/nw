/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.bricks;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenFirebaseChangedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.WhenFirebaseChangedBrick;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class WhenFirebaseChangedBrickTest {

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
	public void testBrickHasCorrectViewResource() {
		WhenFirebaseChangedBrick brick = new WhenFirebaseChangedBrick();
		assertEquals(R.layout.brick_when_firebase_changed, brick.getViewResource());
	}

	@Test
	public void testBrickCreatesWhenFirebaseChangedScript() {
		WhenFirebaseChangedBrick brick = new WhenFirebaseChangedBrick();
		assertTrue(brick.getScript() instanceof WhenFirebaseChangedScript);
	}

	@Test
	public void testAddActionToSequenceDoesNotThrow() {
		WhenFirebaseChangedBrick brick = new WhenFirebaseChangedBrick();
		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));
		assertNotNull(brick.getScript());
	}
}
