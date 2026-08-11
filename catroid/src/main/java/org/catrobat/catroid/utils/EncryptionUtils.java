/*
 * NeoCatroid
 * Copyright (C) 2026 The NeoCatroid Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package org.catrobat.catroid.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class EncryptionUtils {
	private static final String TAG = EncryptionUtils.class.getSimpleName();
	private static final String PREF_NAME = "secure_variable_preferences";
	private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

	private EncryptionUtils() {
	}

	private static final String PREF_FALLBACK_ID = "encryption_fallback_device_id";

	private static String getOrCreateFallbackId(Context context) {
		android.content.SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		String id = prefs.getString(PREF_FALLBACK_ID, null);
		if (id == null || id.isEmpty()) {
			id = java.util.UUID.randomUUID().toString();
			prefs.edit().putString(PREF_FALLBACK_ID, id).apply();
		}
		return id;
	}

	private static SecretKeySpec getSecretKey(Context context) throws Exception {
		String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
		if (androidId == null || androidId.isEmpty()) {
			androidId = getOrCreateFallbackId(context);
		}
		String rawKey = androidId + "_" + context.getPackageName() + "_secure_var_salt";
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] keyBytes = digest.digest(rawKey.getBytes("UTF-8"));
		return new SecretKeySpec(keyBytes, "AES");
	}

	public static String encrypt(Context context, String plainText) {
		try {
			SecretKeySpec secretKey = getSecretKey(context);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] iv = cipher.getIV();
			byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));

			String ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP);
			String encryptedBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP);
			return ivBase64 + ":" + encryptedBase64;
		} catch (Exception e) {
			Log.e(TAG, "Encryption failed: " + e.getMessage(), e);
			return null;
		}
	}

	public static String decrypt(Context context, String cipherText) {
		try {
			if (cipherText == null || !cipherText.contains(":")) {
				return null;
			}
			String[] parts = cipherText.split(":");
			if (parts.length != 2) {
				return null;
			}
			byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
			byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);

			SecretKeySpec secretKey = getSecretKey(context);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
			byte[] decrypted = cipher.doFinal(encrypted);
			return new String(decrypted, "UTF-8");
		} catch (Exception e) {
			Log.e(TAG, "Decryption failed: " + e.getMessage(), e);
			return null;
		}
	}

	public static void saveSecureValue(Context context, String key, String value) {
		String encrypted = encrypt(context, value);
		if (encrypted != null) {
			SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
			prefs.edit().putString(key, encrypted).apply();
		}
	}

	public static String readSecureValue(Context context, String key, String defaultValue) {
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		String encrypted = prefs.getString(key, null);
		if (encrypted == null) {
			return defaultValue;
		}
		String decrypted = decrypt(context, encrypted);
		return decrypted != null ? decrypted : defaultValue;
	}
}
