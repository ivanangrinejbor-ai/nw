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
package org.catrobat.catroid.content.tilemap;

import org.catrobat.catroid.common.TilemapLookData;
import org.catrobat.catroid.physics.PhysicsWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class TilemapRuntimeManager {

	private static final Map<TilemapLookData, TilemapRuntime> RUNTIMES = new WeakHashMap<>();

	private TilemapRuntimeManager() {
	}

	public static synchronized TilemapRuntime getOrCreate(TilemapLookData data) {
		TilemapRuntime runtime = RUNTIMES.get(data);
		if (runtime == null) {
			runtime = new TilemapRuntime(data);
			RUNTIMES.put(data, runtime);
		}
		return runtime;
	}

	public static synchronized TilemapRuntime peek(TilemapLookData data) {
		return RUNTIMES.get(data);
	}

	public static synchronized void disposeAll(PhysicsWorld physicsWorld) {
		List<TilemapRuntime> snapshot = new ArrayList<>(RUNTIMES.values());
		for (TilemapRuntime runtime : snapshot) {
			runtime.dispose(physicsWorld);
		}
		RUNTIMES.clear();
	}
}
