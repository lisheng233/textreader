package com.example.textfilereader;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {
    private static final String PREFS_NAME = "secure_prefs";
    private static final String KEY_ALIAS_PREFIX = "aes_key_";
    private static final String KEY_NAMES_SET = "key_names";
    private static KeyManager instance;
    private Context context;
    private SharedPreferences sharedPreferences;
    private List<String> keyNames = new ArrayList<>();

    private KeyManager(Context context) {
        this.context = context.getApplicationContext();
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            loadKeyNames();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
    }

    public static synchronized KeyManager getInstance(Context context) {
        if (instance == null) {
            instance = new KeyManager(context);
        }
        return instance;
    }

    private void loadKeyNames() {
        Set<String> names = sharedPreferences.getStringSet(KEY_NAMES_SET, new HashSet<>());
        keyNames.clear();
        keyNames.addAll(names);
    }

    private void saveKeyNames() {
        sharedPreferences.edit().putStringSet(KEY_NAMES_SET, new HashSet<>(keyNames)).apply();
    }

    public String generateRandomKey(String keyName) throws GeneralSecurityException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();
        
        String keyBase64 = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
        sharedPreferences.edit().putString(KEY_ALIAS_PREFIX + keyName, keyBase64).apply();
        
        if (!keyNames.contains(keyName)) {
            keyNames.add(keyName);
            saveKeyNames();
        }
        
        return keyName;
    }

    public SecretKey getKey(String keyName) {
        String keyBase64 = sharedPreferences.getString(KEY_ALIAS_PREFIX + keyName, null);
        if (keyBase64 == null) return null;
        
        byte[] keyBytes = Base64.decode(keyBase64, Base64.DEFAULT);
        return new SecretKeySpec(keyBytes, "AES");
    }

    public void deleteKey(String keyName) {
        sharedPreferences.edit().remove(KEY_ALIAS_PREFIX + keyName).apply();
        keyNames.remove(keyName);
        saveKeyNames();
    }

    public List<String> getKeyNames() {
        return new ArrayList<>(keyNames);
    }

    public String saveImportedKey(String keyName, SecretKey key) {
        // 检查密钥名是否已存在
        if (keyNames.contains(keyName)) {
            // 添加后缀
            int counter = 1;
            String newName;
            do {
                newName = keyName + "_" + counter;
                counter++;
            } while (keyNames.contains(newName));
            keyName = newName;
        }
        
        String keyBase64 = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
        sharedPreferences.edit().putString(KEY_ALIAS_PREFIX + keyName, keyBase64).apply();
        
        keyNames.add(keyName);
        saveKeyNames();
        
        return keyName;
    }

    public void renameKey(String oldName, String newName) throws GeneralSecurityException {
        if (oldName.equals(newName)) return;
        
        if (keyNames.contains(newName)) {
            throw new GeneralSecurityException("密钥名称已存在");
        }
        
        SecretKey key = getKey(oldName);
        if (key == null) {
            throw new GeneralSecurityException("密钥不存在");
        }
        
        // 保存到新名称
        String keyBase64 = Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
        sharedPreferences.edit().putString(KEY_ALIAS_PREFIX + newName, keyBase64).apply();
        
        // 删除旧名称
        sharedPreferences.edit().remove(KEY_ALIAS_PREFIX + oldName).apply();
        
        keyNames.remove(oldName);
        keyNames.add(newName);
        saveKeyNames();
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public SecretKey deriveKeyFromPassword(String password, byte[] salt) throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}