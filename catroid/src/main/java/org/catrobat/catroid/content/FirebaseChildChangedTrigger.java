/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits)
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

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.eventids.WhenFirebaseChildChangedEventId;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class FirebaseChildChangedTrigger {
	private static final String TAG = FirebaseChildChangedTrigger.class.getSimpleName();

	public static final int EVENT_ADDED = 0;
	public static final int EVENT_CHANGED = 1;
	public static final int EVENT_REMOVED = 2;
	public static final int EVENT_ANY = 3;

	private final Formula bucket;
	private final Formula path;
	private final int eventType;
	private final Sprite sprite;

	private DatabaseReference ref;
	private ChildEventListener listener;

	private volatile boolean firstEventReceived = false;
	private volatile boolean pendingFire = false;

	FirebaseChildChangedTrigger(Formula bucket, Formula path, int eventType, Sprite sprite) {
		this.bucket = bucket;
		this.path = path;
		this.eventType = eventType;
		this.sprite = sprite;
	}

	void startListening() {
		if (sprite == null) {
			return;
		}
		try {
			Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, null);
			String url = bucket.interpretString(scope);
			String key = path.interpretString(scope);

			ChildEventListener childListener = new ChildEventListener() {
				@Override
				public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
					onChildEvent(EVENT_ADDED);
				}

				@Override
				public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
					onChildEvent(EVENT_CHANGED);
				}

				@Override
				public void onChildRemoved(DataSnapshot snapshot) {
					onChildEvent(EVENT_REMOVED);
				}

				@Override
				public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
				}

				@Override
				public void onCancelled(DatabaseError error) {
					Log.e(TAG, "Firebase child listener cancelled: " + error.getMessage());
				}
			};
			listener = childListener;
			ref = FireBaseManager.INSTANCE.observeChild(url, key, childListener);
		} catch (InterpretationException e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}

	private void onChildEvent(int event) {
		if (!matches(event)) {
			return;
		}
		if (!firstEventReceived) {
			firstEventReceived = true;
			return;
		}
		pendingFire = true;
	}

	private boolean matches(int event) {
		return eventType == EVENT_ANY || eventType == event;
	}

	void evaluateAndTriggerActions() {
		if (pendingFire && sprite.look != null) {
			pendingFire = false;
			EventWrapper eventWrapper = new EventWrapper(
					new WhenFirebaseChildChangedEventId(bucket, path, eventType), false);
			sprite.look.fire(eventWrapper);
		}
	}

	void stopListening() {
		if (ref != null && listener != null) {
			ref.removeEventListener(listener);
		}
		ref = null;
		listener = null;
		firstEventReceived = false;
		pendingFire = false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FirebaseChildChangedTrigger)) {
			return false;
		}
		FirebaseChildChangedTrigger that = (FirebaseChildChangedTrigger) o;
		return bucket.equals(that.bucket) && path.equals(that.path) && eventType == that.eventType;
	}

	@Override
	public int hashCode() {
		return 31 * (31 * bucket.hashCode() + path.hashCode()) + eventType;
	}
}