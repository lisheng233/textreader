package com.example.textfilereader;

import android.util.Base64;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCrypt {
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final String AES_MODE = "AES/GCM/NoPadding";
    
    // 使用存储的密钥加密文件
    public static void encryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(AES_MODE);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            // 先写入IV
            fos.write(iv);
            
            // 加密并写入文件内容
            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) > 0) {
                    cos.write(buffer, 0, count);
                }
            }
        }
    }
    
    // 使用存储的密钥解密文件
    public static void decryptFile(File inputFile, File outputFile, SecretKey key) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            // 读取IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            if (fis.read(iv) != GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid file format");
            }
            
            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = cis.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
            }
        }
    }
    
    // 使用密码加密文件
    public static void encryptFileWithPassword(File inputFile, File outputFile, String password) throws Exception {
        byte[] salt = KeyManager.generateSalt();
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        // 从密码派生密钥
        SecretKey key = deriveKeyFromPassword(password, salt);
        
        Cipher cipher = Cipher.getInstance(AES_MODE);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            // 写入盐和IV
            fos.write(salt);
            fos.write(iv);
            
            // 加密并写入文件内容
            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) > 0) {
                    cos.write(buffer, 0, count);
                }
            }
        }
    }
    
    // 使用密码解密文件
    public static void decryptFileWithPassword(File inputFile, File outputFile, String password) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            // 读取盐和IV
            byte[] salt = new byte[16];
            byte[] iv = new byte[GCM_IV_LENGTH];
            
            if (fis.read(salt) != 16 || fis.read(iv) != GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid file format");
            }
            
            // 从密码派生密钥
            SecretKey key = deriveKeyFromPassword(password, salt);
            
            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = cis.read(buffer)) > 0) {
                    fos.write(buffer, 0, count);
                }
            }
        }
    }
    
    private static SecretKey deriveKeyFromPassword(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}