package org.catrobat.catroid.content.eventids;

import java.util.Objects;

/**
 * Event fired when a scene is started/switched to.
 * Used by "When scene starts" scripts in the Global Scene.
 */
public class SceneStartedEventId extends EventId {

	public final String sceneName;

	public SceneStartedEventId(String sceneName) {
		this.sceneName = sceneName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SceneStartedEventId)) {
			return false;
		}
		SceneStartedEventId that = (SceneStartedEventId) o;
		return Objects.equals(sceneName, that.sceneName);
	}

	@Override
	public int hashCode() {
		return sceneName != null ? sceneName.hashCode() : 0;
	}
}
