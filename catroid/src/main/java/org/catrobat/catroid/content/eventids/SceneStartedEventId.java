package org.catrobat.catroid.content.eventids;

import java.util.Objects;

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
