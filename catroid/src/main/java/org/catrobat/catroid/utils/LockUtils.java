/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
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

package org.catrobat.catroid.utils;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.CompositeBrick;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helpers for the per-brick password protection ("lock") feature.
 *
 * NOTE: this is client-side obfuscation, not real security — the hash lives in the
 * project file and can be removed by editing it. It protects against accidental
 * deletion / tampering by a casual user, not a determined attacker.
 */
public final class LockUtils {

	private LockUtils() {
	}

	/** All locked bricks of a sprite (every script, including nested composite bricks). */
	public static List<Brick> getLockedBricks(Sprite sprite) {
		List<Brick> result = new ArrayList<>();
		if (sprite == null) {
			return result;
		}
		for (Script script : sprite.getScriptList()) {
			collectLocked(script.getBrickList(), result);
		}
		return result;
	}

	/** All locked bricks of a scene (every sprite, every script). */
	public static List<Brick> getLockedBricks(Scene scene) {
		List<Brick> result = new ArrayList<>();
		if (scene == null) {
			return result;
		}
		for (Sprite sprite : scene.getSpriteList()) {
			result.addAll(getLockedBricks(sprite));
		}
		return result;
	}

	private static void collectLocked(List<Brick> bricks, List<Brick> out) {
		if (bricks == null) {
			return;
		}
		for (Brick brick : bricks) {
			if (brick == null) {
				continue;
			}
			if (brick.isLocked()) {
				out.add(brick);
			}
			if (brick instanceof CompositeBrick) {
				CompositeBrick composite = (CompositeBrick) brick;
				collectLocked(composite.getNestedBricks(), out);
				if (composite.hasSecondaryList()) {
					collectLocked(composite.getSecondaryNestedBricks(), out);
				}
			}
		}
	}

	/** True only if the password verifies every locked brick in the list. */
	public static boolean verify(List<Brick> lockedBricks, String password) {
		if (lockedBricks == null) {
			return true;
		}
		for (Brick brick : lockedBricks) {
			if (brick.isLocked() && !brick.verifyLock(password)) {
				return false;
			}
		}
		return true;
	}

	/** Shows a password prompt; on a non-empty OK it passes the entered password to {@code onOk}. */
	public static void requestPassword(Context context, int titleRes, Consumer<String> onOk) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(titleRes);
		final EditText input = new EditText(context);
		input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		builder.setView(input);
		builder.setPositiveButton(R.string.ok, (dialog, which) -> {
			String password = input.getText().toString();
			if (password.isEmpty()) {
				ToastUtil.showError(context, R.string.brick_password_empty);
				return;
			}
			onOk.accept(password);
		});
		builder.setNegativeButton(R.string.cancel, null);
		builder.show();
	}
}
