/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.bricks;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.ai.model.AiProvider;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.AskAIBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(JUnit4.class)
public class AskAIBrickTest {

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
	public void testAskAIBrickCreatesActionWithProvider() {
		ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
		sprite.setActionFactory(actionFactory);
		AskAIBrick brick = new AskAIBrick("Hello!", AiProvider.GEMINI.getId());

		brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

		verify(actionFactory).createAskAIAction(eq(sprite),
				any(SequenceAction.class), any(Formula.class), any(Formula.class),
				any(Formula.class), eq(AiProvider.GEMINI.getId()), any());
	}

	@Test
	public void testDefaultProviderIsGemini() {
		AskAIBrick brick = new AskAIBrick("Hello!");
		assertEquals(AiProvider.GEMINI.getId(), brick.getProviderSelection());
	}

	@Test
	public void testProviderConstructorStoresProvider() {
		AskAIBrick brick = new AskAIBrick("Hello!", "deepseek");
		assertEquals("deepseek", brick.getProviderSelection());
	}

	@Test
	public void testClonePreservesProvider() throws CloneNotSupportedException {
		AskAIBrick brick = new AskAIBrick("Hello!", "openai");
		AskAIBrick clone = (AskAIBrick) brick.clone();
		assertNotNull(clone);
		assertEquals("openai", clone.getProviderSelection());
	}
}
