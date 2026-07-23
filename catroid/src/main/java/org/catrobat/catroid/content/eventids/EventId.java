/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.eventids;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

public class EventId {
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({TAP, TAP_BACKGROUND, START, START_AS_CLONE, ANY_NFC, OTHER, PROJECT_EXIT, BACK_PRESSED, MOUSE_BUTTON_CLICKED, MOUSE_WHEEL_SCROLLED, APP_MINIMIZED, APP_RESTORED, BATTERY_CONNECTED, BATTERY_DISCONNECTED, BATTERY_LOW, BATTERY_FULL, BATTERY_SAVER_ON, BATTERY_SAVER_OFF, INTERNET_AVAILABLE, INTERNET_LOST, WIFI_CONNECTED, WIFI_DISCONNECTED, NETWORK_TYPE_CHANGED, APP_FIRST_LAUNCH,
		ADMOB_INITIALIZED, ADMOB_INIT_FAILED, ADMOB_BANNER_LOADED, ADMOB_BANNER_FAILED, ADMOB_BANNER_SHOWN, ADMOB_BANNER_HIDDEN,
		ADMOB_INTERSTITIAL_LOADED, ADMOB_INTERSTITIAL_FAILED, ADMOB_INTERSTITIAL_SHOWN, ADMOB_INTERSTITIAL_CLOSED,
		ADMOB_REWARDED_LOADED, ADMOB_REWARDED_FAILED, ADMOB_REWARDED_SHOWN, ADMOB_REWARDED_REWARD, ADMOB_REWARDED_CLOSED,
		ADMOB_APP_OPEN_LOADED, ADMOB_APP_OPEN_SHOWN, ADMOB_APP_OPEN_CLOSED,
		NOTIFICATION_ACTION_CLICKED, NOTIFICATION_REPLY_SENT, NOTIFICATION_SHOWN, NOTIFICATION_DISMISSED,
		BEFORE_UPDATE, AFTER_UPDATE, SPRITE_RELEASED, FINGER_MOVED_OVER_SPRITE, FINGER_MOVED_ON_SCREEN, WINDOW_RESIZED, NOTIFICATION_CLICKED, USER_CONCAT,
		SHAKE, AMBIENT_LIGHT, NOISE_DETECTED, KEY_PRESSED, EDGE_SWIPED})
	public @interface EventType {
	}

	public static final int OTHER = 0;
	public static final int TAP = 1;
	public static final int TAP_BACKGROUND = 2;
	public static final int START = 3;
	public static final int START_AS_CLONE = 4;
	public static final int ANY_NFC = 5;

	public static final int SHAKE = 101;
	public static final int AMBIENT_LIGHT = 102;
	public static final int NOISE_DETECTED = 103;
	public static final int KEY_PRESSED = 104;
	public static final int EDGE_SWIPED = 105;

	public static final int PROJECT_EXIT = 6;
	public static final int BACK_PRESSED = 7;
	public static final int MOUSE_BUTTON_CLICKED = 8;
	public static final int MOUSE_WHEEL_SCROLLED = 9;

	public static final int APP_MINIMIZED = 10;
	public static final int APP_RESTORED = 11;
	public static final int BATTERY_CONNECTED = 12;
	public static final int BATTERY_DISCONNECTED = 13;
	public static final int BATTERY_LOW = 14;
	public static final int BATTERY_FULL = 15;
	public static final int BATTERY_SAVER_ON = 16;
	public static final int BATTERY_SAVER_OFF = 17;
	public static final int INTERNET_AVAILABLE = 18;
	public static final int INTERNET_LOST = 19;
	public static final int WIFI_CONNECTED = 20;
	public static final int WIFI_DISCONNECTED = 21;
	public static final int NETWORK_TYPE_CHANGED = 22;
	public static final int APP_FIRST_LAUNCH = 23;

	public static final int ADMOB_INITIALIZED = 24;
	public static final int ADMOB_INIT_FAILED = 25;
	public static final int ADMOB_BANNER_LOADED = 26;
	public static final int ADMOB_BANNER_FAILED = 27;
	public static final int ADMOB_BANNER_SHOWN = 28;
	public static final int ADMOB_BANNER_HIDDEN = 29;
	public static final int ADMOB_INTERSTITIAL_LOADED = 30;
	public static final int ADMOB_INTERSTITIAL_FAILED = 31;
	public static final int ADMOB_INTERSTITIAL_SHOWN = 32;
	public static final int ADMOB_INTERSTITIAL_CLOSED = 33;
	public static final int ADMOB_REWARDED_LOADED = 34;
	public static final int ADMOB_REWARDED_FAILED = 35;
	public static final int ADMOB_REWARDED_SHOWN = 36;
	public static final int ADMOB_REWARDED_REWARD = 37;
	public static final int ADMOB_REWARDED_CLOSED = 38;
	public static final int ADMOB_APP_OPEN_LOADED = 39;
	public static final int ADMOB_APP_OPEN_SHOWN = 40;
	public static final int ADMOB_APP_OPEN_CLOSED = 41;

	public static final int NOTIFICATION_ACTION_CLICKED = 42;
	public static final int NOTIFICATION_REPLY_SENT = 43;
	public static final int NOTIFICATION_SHOWN = 44;
	public static final int NOTIFICATION_DISMISSED = 45;

	public static final int BEFORE_UPDATE = 46;
	public static final int AFTER_UPDATE = 47;
	public static final int SPRITE_RELEASED = 48;
	public static final int FINGER_MOVED_OVER_SPRITE = 49;
	public static final int FINGER_MOVED_ON_SCREEN = 50;
	public static final int WINDOW_RESIZED = 51;
	public static final int NOTIFICATION_CLICKED = 52;
	public static final int USER_CONCAT = 53;

	@EventType
	private final int type;

	public EventId(@EventType int type) {
		this.type = type;
	}

	protected EventId() {
		this.type = OTHER;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof EventId)) {
			return false;
		}

		EventId eventId = (EventId) o;

		return type == eventId.type;
	}

	@Override
	public int hashCode() {
		return type;
	}
}
