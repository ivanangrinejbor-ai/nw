package org.catrobat.catroid.content.eventids;

import com.google.common.base.Objects;

public class KeyPressedEventId extends EventId {
	private final int keycode;

	public KeyPressedEventId(int keycode) {
		super(EventId.KEY_PRESSED);
		this.keycode = keycode;
	}

	public int getKeycode() {
		return keycode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof KeyPressedEventId)) {
			return false;
		}
		KeyPressedEventId that = (KeyPressedEventId) o;
		return keycode == that.keycode;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(super.hashCode(), keycode);
	}
}
