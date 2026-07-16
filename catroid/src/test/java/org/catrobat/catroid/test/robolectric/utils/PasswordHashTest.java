/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.robolectric.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import org.catrobat.catroid.utils.PasswordHash;

import android.util.Base64;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PasswordHashTest {

	@Test
	public void testGenerateSaltIsUniqueAndValid() {
		String salt1 = PasswordHash.generateSalt();
		String salt2 = PasswordHash.generateSalt();
		assertNotNull(salt1);
		assertNotNull(salt2);
		assertNotEquals("salts must differ", salt1, salt2);
		byte[] decoded = Base64.decode(salt1, Base64.NO_WRAP);
		assertEquals(16, decoded.length);
	}

	@Test
	public void testHashIsDeterministic() {
		String salt = PasswordHash.generateSalt();
		String h1 = PasswordHash.hash("secret", salt);
		String h2 = PasswordHash.hash("secret", salt);
		assertNotNull(h1);
		assertEquals(h1, h2);
	}

	@Test
	public void testHashDiffersForDifferentPasswords() {
		String salt = PasswordHash.generateSalt();
		String h1 = PasswordHash.hash("secret", salt);
		String h2 = PasswordHash.hash("other", salt);
		assertNotEquals(h1, h2);
	}

	@Test
	public void testVerifyAcceptsCorrectAndRejectsWrong() {
		String salt = PasswordHash.generateSalt();
		String hash = PasswordHash.hash("secret", salt);
		assertTrue(PasswordHash.verify("secret", salt, hash));
		assertFalse(PasswordHash.verify("wrong", salt, hash));
	}

	@Test
	public void testVerifyRejectsNulls() {
		assertFalse(PasswordHash.verify(null, "s", "h"));
		assertFalse(PasswordHash.verify("p", null, "h"));
		assertFalse(PasswordHash.verify("p", "s", null));
	}
}
