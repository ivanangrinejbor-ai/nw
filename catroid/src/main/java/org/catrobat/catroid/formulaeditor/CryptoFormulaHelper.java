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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.formulaeditor;

import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper for the cryptography formula functions. All methods return either a
 * String (text result) or a double (numeric / boolean result). Errors are
 * handled gracefully by returning an empty string or 0.0 / 1.0.
 */
public final class CryptoFormulaHelper {
    private static final String TAG = CryptoFormulaHelper.class.getSimpleName();
    private static final String HEX = "0123456789abcdef";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoFormulaHelper() {
    }

    // ------------------------------------------------------------------
    // Hashing
    // ------------------------------------------------------------------

    public static String sha(String algorithm, String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(getUtf8Bytes(text));
            return toHex(hash);
        } catch (Exception e) {
            Log.e(TAG, "sha failed", e);
            return "";
        }
    }

    public static String hashFile(String path) {
        try {
            File file = new File(path);
            if (!file.exists() || !file.isFile()) {
                return "";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return toHex(digest.digest());
        } catch (Exception e) {
            Log.e(TAG, "hashFile failed", e);
            return "";
        }
    }

    public static String hashBytes(String input) {
        try {
            byte[] data = decodeBytes(input);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(data));
        } catch (Exception e) {
            Log.e(TAG, "hashBytes failed", e);
            return "";
        }
    }

    // ------------------------------------------------------------------
    // AES (GCM)
    // ------------------------------------------------------------------

    public static String aesEncrypt(String text, String password) {
        try {
            byte[] key = deriveAesKey(password);
            byte[] iv = new byte[12];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(getUtf8Bytes(text));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "aesEncrypt failed", e);
            return "";
        }
    }

    public static String aesDecrypt(String data, String password) {
        try {
            byte[] key = deriveAesKey(password);
            byte[] raw = Base64.decode(data, Base64.NO_WRAP);
            byte[] iv = new byte[12];
            System.arraycopy(raw, 0, iv, 0, iv.length);
            byte[] encrypted = new byte[raw.length - iv.length];
            System.arraycopy(raw, iv.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "aesDecrypt failed", e);
            return "";
        }
    }

    // ------------------------------------------------------------------
    // ChaCha20 (API 28+)
    // ------------------------------------------------------------------

    public static String chaCha20Encrypt(String text, String key, String nonce) {
        try {
            byte[] keyBytes = normalizeKey(key, 32);
            byte[] nonceBytes = normalizeNonce(nonce, 12);
            Cipher cipher = Cipher.getInstance("ChaCha20");
            AlgorithmParameterSpec spec = new javax.crypto.spec.ChaCha20ParameterSpec(nonceBytes, 0);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "ChaCha20"), spec);
            byte[] encrypted = cipher.doFinal(getUtf8Bytes(text));
            return Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "chaCha20Encrypt failed", e);
            return "";
        }
    }

    public static String chaCha20Decrypt(String data, String key, String nonce) {
        try {
            byte[] keyBytes = normalizeKey(key, 32);
            byte[] nonceBytes = normalizeNonce(nonce, 12);
            byte[] raw = Base64.decode(data, Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance("ChaCha20");
            AlgorithmParameterSpec spec = new javax.crypto.spec.ChaCha20ParameterSpec(nonceBytes, 0);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "ChaCha20"), spec);
            return new String(cipher.doFinal(raw), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "chaCha20Decrypt failed", e);
            return "";
        }
    }

    // ------------------------------------------------------------------
    // Key derivation
    // ------------------------------------------------------------------

    public static String pbkdf2(String password, String salt, int iterations) {
        try {
            byte[] saltBytes = decodeBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, iterations, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return toHex(factory.generateSecret(spec).getEncoded());
        } catch (Exception e) {
            Log.e(TAG, "pbkdf2 failed", e);
            return "";
        }
    }

    public static String generateSalt(int length) {
        if (length <= 0) {
            length = 16;
        }
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static String deriveKey(String password) {
        String salt = generateSalt(16);
        String key = pbkdf2(password, salt, 10000);
        return (key.isEmpty()) ? "" : (salt + ":" + key);
    }

    // ------------------------------------------------------------------
    // Key / data generation
    // ------------------------------------------------------------------

    public static String generateAesKey(int bits) {
        int keyBits = (bits == 128 || bits == 192) ? bits : 256;
        byte[] key = new byte[keyBits / 8];
        SECURE_RANDOM.nextBytes(key);
        return Base64.encodeToString(key, Base64.NO_WRAP);
    }

    public static String generateRandomBytes(int length) {
        if (length <= 0) {
            length = 16;
        }
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    public static String generatePassword(int length) {
        if (length <= 0) {
            length = 16;
        }
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*?";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String generateUuid() {
        return java.util.UUID.randomUUID().toString();
    }

    // ------------------------------------------------------------------
    // Random
    // ------------------------------------------------------------------

    public static String randomHex(int length) {
        if (length <= 0) {
            length = 16;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(HEX.charAt(SECURE_RANDOM.nextInt(HEX.length())));
        }
        return sb.toString();
    }

    public static String randomBase64(int length) {
        if (length <= 0) {
            length = 16;
        }
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static double randomIntSecure(double min, double max) {
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        double range = hi - lo;
        if (range <= 0) {
            return lo;
        }
        return lo + (Math.floor(SECURE_RANDOM.nextDouble() * (range + 1)));
    }

    public static String randomStringSecure(int length) {
        if (length <= 0) {
            length = 16;
        }
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Encoding
    // ------------------------------------------------------------------

    public static String base64Encode(String text) {
        try {
            return Base64.encodeToString(getUtf8Bytes(text), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "base64Encode failed", e);
            return "";
        }
    }

    public static String base64Decode(String text) {
        try {
            return new String(Base64.decode(text, Base64.NO_WRAP), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "base64Decode failed", e);
            return "";
        }
    }

    public static String hexEncode(String input) {
        try {
            return toHex(decodeBytes(input));
        } catch (Exception e) {
            Log.e(TAG, "hexEncode failed", e);
            return "";
        }
    }

    public static String hexDecode(String input) {
        try {
            return new String(fromHex(input.trim()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "hexDecode failed", e);
            return "";
        }
    }

    // ------------------------------------------------------------------
    // Verification
    // ------------------------------------------------------------------

    public static double compareHash(String text, String hash) {
        if (text == null || hash == null) {
            return 0.0;
        }
        String computed = sha("SHA-256", text);
        if (computed.isEmpty()) {
            return 0.0;
        }
        String lowerHash = hash.trim().toLowerCase(java.util.Locale.US);
        return lowerHash.equals(computed) ? 1.0 : 0.0;
    }

    public static double isBase64(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        try {
            byte[] decoded = Base64.decode(text, Base64.NO_WRAP);
            return (decoded.length > 0) ? 1.0 : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static double isHex(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        String t = text.trim().toLowerCase(java.util.Locale.US);
        if ((t.length() % 2) != 0) {
            return 0.0;
        }
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return 0.0;
            }
        }
        return 1.0;
    }

    // ------------------------------------------------------------------
    // HMAC
    // ------------------------------------------------------------------

    public static String hmacSha256(String text, String key) {
        return hmac("HmacSHA256", text, key);
    }

    public static String hmacSha512(String text, String key) {
        return hmac("HmacSHA512", text, key);
    }

    private static String hmac(String algorithm, String text, String key) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(getUtf8Bytes(key), algorithm));
            return toHex(mac.doFinal(getUtf8Bytes(text)));
        } catch (Exception e) {
            Log.e(TAG, "hmac failed", e);
            return "";
        }
    }

    // ------------------------------------------------------------------
    // RSA
    // ------------------------------------------------------------------

    public static String rsaGenerateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048, SECURE_RANDOM);
            KeyPair pair = generator.generateKeyPair();
            String publicKey = Base64.encodeToString(pair.getPublic().getEncoded(), Base64.NO_WRAP);
            String privateKey = Base64.encodeToString(pair.getPrivate().getEncoded(), Base64.NO_WRAP);
            return "{\"public\":\"" + publicKey + "\",\"private\":\"" + privateKey + "\"}";
        } catch (Exception e) {
            Log.e(TAG, "rsaGenerateKeyPair failed", e);
            return "";
        }
    }

    public static String rsaEncrypt(String text, String publicKey) {
        try {
            java.security.PublicKey key = java.security.KeyFactory.getInstance("RSA")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.decode(publicKey, Base64.NO_WRAP)));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.encodeToString(cipher.doFinal(getUtf8Bytes(text)), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "rsaEncrypt failed", e);
            return "";
        }
    }

    public static String rsaDecrypt(String data, String privateKey) {
        try {
            java.security.PrivateKey key = java.security.KeyFactory.getInstance("RSA")
                    .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.NO_WRAP)));
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "rsaDecrypt failed", e);
            return "";
        }
    }

    public static String rsaSign(String text, String privateKey) {
        try {
            java.security.PrivateKey key = java.security.KeyFactory.getInstance("RSA")
                    .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(Base64.decode(privateKey, Base64.NO_WRAP)));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key, SECURE_RANDOM);
            signature.update(getUtf8Bytes(text));
            return Base64.encodeToString(signature.sign(), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "rsaSign failed", e);
            return "";
        }
    }

    public static double rsaVerify(String text, String signature, String publicKey) {
        try {
            java.security.PublicKey key = java.security.KeyFactory.getInstance("RSA")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.decode(publicKey, Base64.NO_WRAP)));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(key);
            sig.update(getUtf8Bytes(text));
            return sig.verify(Base64.decode(signature, Base64.NO_WRAP)) ? 1.0 : 0.0;
        } catch (Exception e) {
            Log.e(TAG, "rsaVerify failed", e);
            return 0.0;
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static byte[] deriveAesKey(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(getUtf8Bytes(password));
    }

    private static byte[] normalizeKey(String key, int size) throws Exception {
        byte[] bytes = decodeBytes(key);
        if (bytes.length == size) {
            return bytes;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        byte[] out = new byte[size];
        System.arraycopy(hash, 0, out, 0, Math.min(size, hash.length));
        if (size > hash.length) {
            byte[] hash2 = digest.digest(hash);
            System.arraycopy(hash2, 0, out, hash.length, Math.min(size - hash.length, hash2.length));
        }
        return out;
    }

    private static byte[] normalizeNonce(String nonce, int size) throws Exception {
        byte[] bytes = decodeBytes(nonce);
        if (bytes.length == size) {
            return bytes;
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        byte[] out = new byte[size];
        System.arraycopy(hash, 0, out, 0, Math.min(size, hash.length));
        return out;
    }

    private static byte[] getUtf8Bytes(String text) {
        return (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] decodeBytes(String input) {
        if (input == null || input.isEmpty()) {
            return new byte[0];
        }
        String trimmed = input.trim();
        // Try hex
        if ((trimmed.length() % 2) == 0 && isHexString(trimmed)) {
            try {
                return fromHex(trimmed);
            } catch (Exception ignored) {
                // fall through
            }
        }
        // Try base64
        try {
            return Base64.decode(trimmed, Base64.NO_WRAP);
        } catch (Exception ignored) {
            // fall through
        }
        // Fall back to utf-8
        return getUtf8Bytes(trimmed);
    }

    private static boolean isHexString(String text) {
        String t = text.toLowerCase(java.util.Locale.US);
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            sb.append(HEX.charAt(b & 0x0f));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String text) {
        int len = text.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(text.charAt(i), 16) << 4)
                    + Character.digit(text.charAt(i + 1), 16));
        }
        return data;
    }
}
