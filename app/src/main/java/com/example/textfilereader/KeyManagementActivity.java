package com.example.textfilereader;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyManagementActivity extends AppCompatActivity {
    
    private ListView listView;
    private TextView tvEmpty;
    private Button btnGenerateKey;
    private Button btnImportKey;
    private KeyManager keyManager;
    private List<String> keyNames;
    private ArrayAdapter<String> adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_management);
        
        // 初始化视图
        listView = findViewById(R.id.listView);
        tvEmpty = findViewById(R.id.tvEmpty);
        btnGenerateKey = findViewById(R.id.btnGenerateKey);
        btnImportKey = findViewById(R.id.btnImportKey);
        
        // 设置标题
        setTitle("密钥管理");
        
        keyManager = KeyManager.getInstance(this);
        
        // 设置按钮点击事件
        btnGenerateKey.setOnClickListener(v -> showGenerateKeyDialog());
        btnImportKey.setOnClickListener(v -> showImportKeyDialog());
        
        // 加载密钥列表
        loadKeyList();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadKeyList();
    }
    
    private void loadKeyList() {
        keyNames = keyManager.getKeyNames();
        
        if (keyNames.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            
            adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_list_item_1, keyNames);
            listView.setAdapter(adapter);
            
            listView.setOnItemClickListener((parent, view, position, id) -> {
                String keyName = keyNames.get(position);
                showKeyOptionsDialog(keyName);
            });
        }
    }
    
    private void showGenerateKeyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_generate_key, null);
        EditText etKeyName = view.findViewById(R.id.etKeyName);
        
        builder.setView(view)
               .setTitle("生成新密钥")
               .setPositiveButton("生成", (dialog, which) -> {
                   String keyName = etKeyName.getText().toString().trim();
                   if (keyName.isEmpty()) {
                       Toast.makeText(this, "请输入密钥名称", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   String result = keyManager.generateRandomKey(keyName);
                   if (result != null) {
                       Toast.makeText(this, "密钥生成成功: " + keyName, Toast.LENGTH_SHORT).show();
                       loadKeyList();
                   } else {
                       Toast.makeText(this, "密钥生成失败", Toast.LENGTH_LONG).show();
                   }
               })
               .setNegativeButton("取消", null)
               .show();
    }
    
    private void showKeyOptionsDialog(String keyName) {
        new AlertDialog.Builder(this)
            .setTitle("密钥操作")
            .setMessage("密钥名称: " + keyName)
            .setPositiveButton("导出", (dialog, which) -> exportKey(keyName))
            .setNegativeButton("删除", (dialog, which) -> showDeleteKeyDialog(keyName))
            .show();
    }
    
    private void showDeleteKeyDialog(String keyName) {
        new AlertDialog.Builder(this)
            .setTitle("删除密钥")
            .setMessage("确定要删除密钥 \"" + keyName + "\" 吗？")
            .setPositiveButton("删除", (dialog, which) -> {
                keyManager.deleteKey(keyName);
                Toast.makeText(this, "密钥已删除", Toast.LENGTH_SHORT).show();
                loadKeyList();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void exportKey(String keyName) {
        SecretKey key = keyManager.getKey(keyName);
        if (key == null) {
            Toast.makeText(this, "密钥不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 将密钥转换为Base64
        String keyBase64 = android.util.Base64.encodeToString(key.getEncoded(), android.util.Base64.DEFAULT);
        
        // 创建导出文件到下载目录
        File exportDir = new File(getExternalFilesDir(null), "keys");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        
        File exportFile = new File(exportDir, keyName + ".key");
        
        try (FileOutputStream fos = new FileOutputStream(exportFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            
            osw.write("# TextReader 导出密钥\n");
            osw.write("# 密钥名称: " + keyName + "\n");
            osw.write("# 导出时间: " + new java.util.Date().toString() + "\n");
            osw.write("# 密钥格式: AES-256 Base64\n");
            osw.write(keyBase64.trim() + "\n");
            
            Toast.makeText(this, "密钥已导出到: " + exportFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            
            // 提示用户文件位置
            new AlertDialog.Builder(this)
                .setTitle("导出成功")
                .setMessage("密钥已保存到:\n" + exportFile.getAbsolutePath())
                .setPositiveButton("确定", null)
                .show();
                
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showImportKeyDialog() {
        // 选择导入方式
        new AlertDialog.Builder(this)
            .setTitle("导入密钥")
            .setItems(new String[]{"从文件导入", "手动输入"}, (dialog, which) -> {
                if (which == 0) {
                    // 从文件导入
                    importKeyFromFile();
                } else {
                    // 手动输入
                    showManualImportDialog();
                }
            })
            .show();
    }
    
    private void importKeyFromFile() {
        File importDir = new File(getExternalFilesDir(null), "keys");
        if (!importDir.exists()) {
            importDir.mkdirs();
        }
        
        File[] files = importDir.listFiles((dir, name) -> name.endsWith(".key"));
        
        if (files == null || files.length == 0) {
            Toast.makeText(this, "未找到.key文件", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示找到的文件列表
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName();
        }
        
        new AlertDialog.Builder(this)
            .setTitle("选择要导入的密钥文件")
            .setItems(fileNames, (dialog, which) -> {
                File selectedFile = files[which];
                importKeyFromFile(selectedFile);
            })
            .show();
    }
    
    private void importKeyFromFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder content = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                content.append(line.trim());
            }
            
            String keyBase64 = content.toString();
            if (keyBase64.isEmpty()) {
                Toast.makeText(this, "无效的密钥文件", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 解码密钥
            byte[] keyBytes = android.util.Base64.decode(keyBase64, android.util.Base64.DEFAULT);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            
            // 生成密钥名称
            String keyName = file.getName().replace(".key", "");
            
            // 保存密钥
            String savedName = keyManager.saveImportedKey(keyName, key);
            Toast.makeText(this, "密钥导入成功: " + savedName, Toast.LENGTH_SHORT).show();
            loadKeyList();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showManualImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_import_key, null);
        EditText etKeyName = view.findViewById(R.id.etKeyName);
        EditText etKeyBase64 = view.findViewById(R.id.etKeyBase64);
        
        builder.setView(view)
               .setTitle("手动导入密钥")
               .setPositiveButton("导入", (dialog, which) -> {
                   String keyName = etKeyName.getText().toString().trim();
                   String keyBase64 = etKeyBase64.getText().toString().trim();
                   
                   if (keyName.isEmpty()) {
                       Toast.makeText(this, "请输入密钥名称", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   if (keyBase64.isEmpty()) {
                       Toast.makeText(this, "请输入密钥Base64", Toast.LENGTH_SHORT).show();
                       return;
                   }
                   
                   try {
                       byte[] keyBytes = android.util.Base64.decode(keyBase64, android.util.Base64.DEFAULT);
                       SecretKey key = new SecretKeySpec(keyBytes, "AES");
                       
                       String savedName = keyManager.saveImportedKey(keyName, key);
                       Toast.makeText(this, "密钥导入成功: " + savedName, Toast.LENGTH_SHORT).show();
                       loadKeyList();
                   } catch (Exception e) {
                       e.printStackTrace();
                       Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                   }
               })
               .setNegativeButton("取消", null)
               .show();
    }
}
