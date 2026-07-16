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

import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class PasswordHash {

	private static final String TAG = PasswordHash.class.getSimpleName();
	private static final String ALGORITHM = "SHA-256";
	private static final int SALT_BYTES = 16;
	private static final int BASE64_FLAGS = Base64.NO_WRAP;

	private PasswordHash() {
	}

	public static String generateSalt() {
		byte[] salt = new byte[SALT_BYTES];
		new SecureRandom().nextBytes(salt);
		return Base64.encodeToString(salt, BASE64_FLAGS);
	}

	public static String hash(String password, String saltBase64) {
		try {
			byte[] salt = Base64.decode(saltBase64, BASE64_FLAGS);
			MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
			digest.update(salt);
			byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			return Base64.encodeToString(hash, BASE64_FLAGS);
		} catch (Exception e) {
			Log.e(TAG, Log.getStackTraceString(e));
			return null;
		}
	}

	public static boolean verify(String password, String saltBase64, String hashBase64) {
		if (password == null || saltBase64 == null || hashBase64 == null) {
			return false;
		}
		String computed = hash(password, saltBase64);
		return computed != null && computed.equals(hashBase64);
	}
}
