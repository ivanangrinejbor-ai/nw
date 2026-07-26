package org.catrobat.catroid.content.eventids;

import java.util.Objects;

/**
 * Event fired when leaving a scene (before switching to another one).
 * Used by "When leaving scene" scripts in the Global Scene.
 */
public class SceneExitedEventId extends EventId {

	public final String sceneName;

	public SceneExitedEventId(String sceneName) {
		this.sceneName = sceneName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SceneExitedEventId)) {
			return false;
		}
		SceneExitedEventId that = (SceneExitedEventId) o;
		return Objects.equals(sceneName, that.sceneName);
	}

	@Override
	public int hashCode() {
		return sceneName != null ? sceneName.hashCode() * 31 : 0;
	}
}
