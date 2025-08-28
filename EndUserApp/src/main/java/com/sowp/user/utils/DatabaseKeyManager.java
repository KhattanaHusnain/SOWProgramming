package com.sowp.user.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.RequiresApi;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

public class DatabaseKeyManager {
    private static final String KEY_ALIAS = "NexCodeAESKey";
    private static final String PREF_NAME = "db_prefs";
    private static final String PREF_KEY = "encrypted_passphrase";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void init(Context context) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        // Generate AES key if not present
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            keyGenerator.init(
                    new KeyGenParameterSpec.Builder(KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setRandomizedEncryptionRequired(true)
                            .build()
            );

            keyGenerator.generateKey();
        }

        // Store random DB passphrase if not present
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(PREF_KEY)) {
            byte[] randomPass = new byte[32]; // 256-bit key
            new SecureRandom().nextBytes(randomPass);

            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(randomPass);

            String encryptedData = Base64.getEncoder().encodeToString(iv) + ":" +
                    Base64.getEncoder().encodeToString(encrypted);

            prefs.edit().putString(PREF_KEY, encryptedData).apply();
        }
    }

    private static SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    public static byte[] getDecryptedPassphrase(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String encryptedData = prefs.getString(PREF_KEY, null);

        if (encryptedData == null) {
            throw new IllegalStateException("Passphrase not initialized");
        }

        String[] parts = encryptedData.split(":");
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] encrypted = Base64.getDecoder().decode(parts[1]);

        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

        return cipher.doFinal(encrypted);
    }
}
