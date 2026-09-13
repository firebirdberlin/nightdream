package com.firebirdberlin.nightdream;

import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;

import java.util.HashSet;
import java.util.Set;

/**
 * A PreferenceDataStore that manually encrypts sensitive keys before saving them
 * to the standard SharedPreferences.
 */
public class SecurePreferenceDataStore extends PreferenceDataStore {
    private final SharedPreferences prefs;
    private final CryptoManager cryptoManager;
    private final Set<String> secureKeys;

    public SecurePreferenceDataStore(SharedPreferences prefs) {
        this.prefs = prefs;
        this.cryptoManager = new CryptoManager();
        this.secureKeys = new HashSet<>();
        this.secureKeys.add("smart_home_avm_password");
    }

    @Override
    public void putString(String key, @Nullable String value) {
        if (secureKeys.contains(key)) {
            String encrypted = cryptoManager.encrypt(value);
            prefs.edit().putString(key, encrypted).apply();
        } else {
            prefs.edit().putString(key, value).apply();
        }
    }

    @Override
    @Nullable
    public String getString(String key, @Nullable String defValue) {
        String value = prefs.getString(key, null);
        if (value == null) return defValue;

        if (secureKeys.contains(key)) {
            String decrypted = cryptoManager.decrypt(value);
            // If decryption fails, it might be that the value is still in plaintext (migration)
            if (decrypted == null) {
                // Return original value and attempt to encrypt it for next time
                // This handles migration from plaintext to encrypted
                String encrypted = cryptoManager.encrypt(value);
                if (encrypted != null) {
                    prefs.edit().putString(key, encrypted).apply();
                }
                return value;
            }
            return decrypted;
        }
        return value;
    }

    // Proxy other methods to standard prefs
    @Override
    public void putBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return prefs.getBoolean(key, defValue);
    }

    @Override
    public void putInt(String key, int value) {
        prefs.edit().putInt(key, value).apply();
    }

    @Override
    public int getInt(String key, int defValue) {
        return prefs.getInt(key, defValue);
    }

    @Override
    public void putLong(String key, long value) {
        prefs.edit().putLong(key, value).apply();
    }

    @Override
    public long getLong(String key, long defValue) {
        return prefs.getLong(key, defValue);
    }

    @Override
    public void putFloat(String key, float value) {
        prefs.edit().putFloat(key, value).apply();
    }

    @Override
    public float getFloat(String key, float defValue) {
        return prefs.getFloat(key, defValue);
    }
}
