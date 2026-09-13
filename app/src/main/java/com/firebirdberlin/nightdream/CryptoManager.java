package com.firebirdberlin.nightdream;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * A simple utility to encrypt and decrypt strings using the Android Keystore System.
 * This is zero-dependency and GMS-free.
 */
public class CryptoManager {
    private static final String TAG = "CryptoManager";
    private static final String KEY_ALIAS = "nightdream_secret_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private KeyStore keyStore;

    public CryptoManager() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize KeyStore", e);
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build();
        keyGenerator.init(spec);
        return keyGenerator.generateKey();
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) return null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to the encrypted data so we can use it during decryption
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        }
    }

    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) return null;
        try {
            byte[] combined = Base64.decode(encryptedBase64, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            // AES/GCM/NoPadding uses a 12-byte IV by default in Android
            int ivLength = 12;
            byte[] iv = new byte[ivLength];
            byte[] encrypted = new byte[combined.length - ivLength];
            System.arraycopy(combined, 0, iv, 0, ivLength);
            System.arraycopy(combined, ivLength, encrypted, 0, encrypted.length);

            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec);

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            return null;
        }
    }
}
