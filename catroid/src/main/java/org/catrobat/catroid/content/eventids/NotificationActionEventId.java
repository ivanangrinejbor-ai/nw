/*
 * NeoCatroid: On-device visual programming system
 * Notification action event id — fires when a specific notification action was clicked.
 */
package org.catrobat.catroid.content.eventids;

import com.google.common.base.Objects;

public class NotificationActionEventId extends EventId {

	private final String actionId;

	public NotificationActionEventId(String actionId) {
		super(EventId.NOTIFICATION_ACTION_CLICKED);
		this.actionId = actionId != null ? actionId : "";
	}

	public String getActionId() {
		return actionId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof NotificationActionEventId)) {
			return false;
		}
		NotificationActionEventId that = (NotificationActionEventId) o;
		return actionId.equals(that.actionId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), actionId);
	}
}