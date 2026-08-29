package org.catrobat.catroid.content.eventids;

public class TcpDisconnectEventId extends EventId {
	private static final long serialVersionUID = 1L;

	private final String uniqueId = java.util.UUID.randomUUID().toString();

	public TcpDisconnectEventId() {
		super(EventId.USER_CONCAT);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TcpDisconnectEventId)) return false;
		return uniqueId.equals(((TcpDisconnectEventId) o).uniqueId);
	}

	@Override
	public int hashCode() {
		return uniqueId.hashCode();
	}
}
