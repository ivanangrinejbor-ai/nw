package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenSceneExitedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.SceneExitedEventId;

/**
 * Script triggered when leaving a specific scene.
 * Only available in the Global Scene.
 */
public class WhenSceneExitedScript extends Script {

	private static final long serialVersionUID = 1L;

	private String sceneName;

	public WhenSceneExitedScript() {
	}

	public WhenSceneExitedScript(String sceneName) {
		this.sceneName = sceneName;
	}

	public String getSceneName() {
		return sceneName;
	}

	public void setSceneName(String sceneName) {
		this.sceneName = sceneName;
	}

	@Override
	public ScriptBrick getScriptBrick() {
		if (scriptBrick == null) {
			scriptBrick = new WhenSceneExitedBrick(this);
		}
		return scriptBrick;
	}

	@Override
	public EventId createEventId(Sprite sprite) {
		return new SceneExitedEventId(sceneName);
	}
}
