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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.eventids.WhenFirebaseChangedEventId;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class FirebaseChangedTrigger {
	private static final String TAG = FirebaseChangedTrigger.class.getSimpleName();

	private final Formula bucket;
	private final Formula path;
	private final Sprite sprite;

	private DatabaseReference ref;
	private ValueEventListener listener;

	private volatile boolean firstValueReceived = false;
	private volatile boolean pendingFire = false;

	FirebaseChangedTrigger(Formula bucket, Formula path, Sprite sprite) {
		this.bucket = bucket;
		this.path = path;
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

			ValueEventListener valueListener = new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot snapshot) {
					if (!firstValueReceived) {
						firstValueReceived = true;
						return;
					}
					pendingFire = true;
				}

				@Override
				public void onCancelled(DatabaseError error) {
					Log.e(TAG, "Firebase value listener cancelled: " + error.getMessage());
				}
			};
			listener = valueListener;
			ref = FireBaseManager.INSTANCE.observeValue(url, key, valueListener);
		} catch (InterpretationException e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
	}

	void evaluateAndTriggerActions() {
		if (pendingFire && sprite.look != null) {
			pendingFire = false;
			EventWrapper eventWrapper = new EventWrapper(
					new WhenFirebaseChangedEventId(bucket, path), false);
			sprite.look.fire(eventWrapper);
		}
	}

	void stopListening() {
		if (ref != null && listener != null) {
			ref.removeEventListener(listener);
		}
		ref = null;
		listener = null;
		firstValueReceived = false;
		pendingFire = false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FirebaseChangedTrigger)) {
			return false;
		}
		FirebaseChangedTrigger that = (FirebaseChangedTrigger) o;
		return bucket.equals(that.bucket) && path.equals(that.path);
	}

	@Override
	public int hashCode() {
		return 31 * bucket.hashCode() + path.hashCode();
	}
}
