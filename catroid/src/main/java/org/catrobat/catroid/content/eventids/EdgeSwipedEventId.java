package org.catrobat.catroid.content.eventids;

import com.google.common.base.Objects;

public class EdgeSwipedEventId extends EventId {
	private final int edgeDirection; // 0 = Left, 1 = Right, 2 = Top, 3 = Bottom

	public EdgeSwipedEventId(int edgeDirection) {
		super(EventId.EDGE_SWIPED);
		this.edgeDirection = edgeDirection;
	}

	public int getEdgeDirection() {
		return edgeDirection;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof EdgeSwipedEventId)) {
			return false;
		}
		EdgeSwipedEventId that = (EdgeSwipedEventId) o;
		return edgeDirection == that.edgeDirection;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), edgeDirection);
	}
}
