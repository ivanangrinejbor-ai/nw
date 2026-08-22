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

package org.catrobat.catroid.content;

import android.util.Log;

import com.google.firebase.firestore.ListenerRegistration;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.eventids.WhenFirestoreChangedEventId;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class FirestoreChangedTrigger {
	private static final String TAG = FirestoreChangedTrigger.class.getSimpleName();

	private final Formula path;
	private final Formula base;
	private final Sprite sprite;

	private ListenerRegistration registration;

	private volatile boolean firstValueReceived = false;
	private volatile boolean pendingFire = false;

	FirestoreChangedTrigger(Formula path, Formula base, Sprite sprite) {
		this.path = path;
		this.base = base;
		this.sprite = sprite;
	}

	void startListening() {
		if (sprite == null) {
			return;
		}
		try {
			Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, null);
			String pathStr = path.interpretString(scope);
			String baseStr = base != null ? base.interpretString(scope) : "";

			if (pathStr.isBlank()) {
				return;
			}

			boolean isDocument = pathStr.split("/").length % 2 == 0;

			if (isDocument) {
				registration = FirestoreManager.INSTANCE.observeDocument(baseStr, pathStr, (snapshot, error) -> {
					if (error != null) {
						Log.e(TAG, "Firestore document listener error: " + error.getMessage());
						return;
					}
					onSnapshotReceived();
				});
			} else {
				registration = FirestoreManager.INSTANCE.observeCollection(baseStr, pathStr, (snapshot, error) -> {
					if (error != null) {
						Log.e(TAG, "Firestore collection listener error: " + error.getMessage());
						return;
					}
					onSnapshotReceived();
				});
			}
		} catch (InterpretationException e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}

	private void onSnapshotReceived() {
		if (!firstValueReceived) {
			firstValueReceived = true;
			return;
		}
		pendingFire = true;
	}

	void evaluateAndTriggerActions() {
		if (pendingFire && sprite.look != null) {
			pendingFire = false;
			EventWrapper eventWrapper = new EventWrapper(
					new WhenFirestoreChangedEventId(path, base), false);
			sprite.look.fire(eventWrapper);
		}
	}

	void stopListening() {
		if (registration != null) {
			registration.remove();
		}
		registration = null;
		firstValueReceived = false;
		pendingFire = false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FirestoreChangedTrigger)) {
			return false;
		}
		FirestoreChangedTrigger that = (FirestoreChangedTrigger) o;
		return path.equals(that.path) && (base == null ? that.base == null : base.equals(that.base));
	}

	@Override
	public int hashCode() {
		int result = path.hashCode();
		result = 31 * result + (base != null ? base.hashCode() : 0);
		return result;
	}
}