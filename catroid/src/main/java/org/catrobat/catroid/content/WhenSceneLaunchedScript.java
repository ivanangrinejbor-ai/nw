package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.WhenSceneLaunchedBrick;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.SceneStartedEventId;

/**
 * Script triggered when a specific scene starts.
 * Only available in the Global Scene.
 */
public class WhenSceneLaunchedScript extends Script {

	private static final long serialVersionUID = 1L;

	private String sceneName;

	public WhenSceneLaunchedScript() {
	}

	public WhenSceneLaunchedScript(String sceneName) {
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
			scriptBrick = new WhenSceneLaunchedBrick(this);
		}
		return scriptBrick;
	}

	@Override
	public EventId createEventId(Sprite sprite) {
		return new SceneStartedEventId(sceneName);
	}
}
