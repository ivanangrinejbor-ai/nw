package org.catrobat.catroid.content.eventids;

import java.util.Objects;

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
