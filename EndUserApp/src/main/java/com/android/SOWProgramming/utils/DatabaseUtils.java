package com.android.SOWProgramming.utils;

import android.content.Context;

import net.sqlcipher.database.SupportFactory;

public class DatabaseUtils {

    public static SupportFactory getEncryptedFactory(Context context) throws Exception {
        DatabaseKeyManager.init(context);
        byte[] passphrase = DatabaseKeyManager.getDecryptedPassphrase(context);
        return new SupportFactory(passphrase);
    }
}
