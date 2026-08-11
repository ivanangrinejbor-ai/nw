/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.stage;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

public class VolumeButtonState {

	private static volatile VolumeButtonState instance;

	private boolean volumeUpHeld = false;
	private boolean volumeDownHeld = false;
	private long volumeUpHoldStart = 0L;
	private long volumeDownHoldStart = 0L;
	private boolean upEventSent = false;
	private boolean downEventSent = false;

	private final List<VolumeButtonHoldListener> holdListeners = new ArrayList<>();

	public interface VolumeButtonHoldListener {
		void onVolumeButtonHeld(int keyCode, float duration);
	}

	private VolumeButtonState() {
	}

	public static VolumeButtonState getInstance() {
		if (instance == null) {
			synchronized (VolumeButtonState.class) {
				if (instance == null) {
					instance = new VolumeButtonState();
				}
			}
		}
		return instance;
	}

	public static boolean isVolumeUpHeld() {
		return getInstance().volumeUpHeld;
	}

	public static boolean isVolumeDownHeld() {
		return getInstance().volumeDownHeld;
	}

	public static float getVolumeUpHeldSeconds() {
		VolumeButtonState s = getInstance();
		if (!s.volumeUpHeld) return 0f;
		return (SystemClock.elapsedRealtime() - s.volumeUpHoldStart) / 1000f;
	}

	public static float getVolumeDownHeldSeconds() {
		VolumeButtonState s = getInstance();
		if (!s.volumeDownHeld) return 0f;
		return (SystemClock.elapsedRealtime() - s.volumeDownHoldStart) / 1000f;
	}

	public static void onVolumeUpPressed() {
		VolumeButtonState s = getInstance();
		s.volumeUpHeld = true;
		s.volumeUpHoldStart = SystemClock.elapsedRealtime();
		s.upEventSent = false;
	}

	public static void onVolumeUpReleased() {
		VolumeButtonState s = getInstance();
		s.volumeUpHeld = false;
		s.volumeUpHoldStart = 0L;
		s.upEventSent = false;
	}

	public static void onVolumeDownPressed() {
		VolumeButtonState s = getInstance();
		s.volumeDownHeld = true;
		s.volumeDownHoldStart = SystemClock.elapsedRealtime();
		s.downEventSent = false;
	}

	public static void onVolumeDownReleased() {
		VolumeButtonState s = getInstance();
		s.volumeDownHeld = false;
		s.volumeDownHoldStart = 0L;
		s.downEventSent = false;
	}

	public static boolean isUpEventSent() {
		return getInstance().upEventSent;
	}

	public static void setUpEventSent(boolean sent) {
		getInstance().upEventSent = sent;
	}

	public static boolean isDownEventSent() {
		return getInstance().downEventSent;
	}

	public static void setDownEventSent(boolean sent) {
		getInstance().downEventSent = sent;
	}

	public static void addHoldListener(VolumeButtonHoldListener listener) {
		getInstance().holdListeners.add(listener);
	}

	public static void removeHoldListener(VolumeButtonHoldListener listener) {
		getInstance().holdListeners.remove(listener);
	}

	public static void notifyHoldListeners(int keyCode, float duration) {
		for (VolumeButtonHoldListener l : new ArrayList<>(getInstance().holdListeners)) {
			l.onVolumeButtonHeld(keyCode, duration);
		}
	}

	public static void reset() {
		VolumeButtonState s = getInstance();
		s.volumeUpHeld = false;
		s.volumeDownHeld = false;
		s.volumeUpHoldStart = 0L;
		s.volumeDownHoldStart = 0L;
		s.upEventSent = false;
		s.downEventSent = false;
		s.holdListeners.clear();
	}
}
