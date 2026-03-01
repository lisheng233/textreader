package com.example.textfilereader;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private Button btnSelectFile;
    private Button btnKeyManagement;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化视图
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnKeyManagement = findViewById(R.id.btnKeyManagement);
        
        // 设置点击事件
        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FilePickerActivity.class);
            startActivity(intent);
        });
        
        btnKeyManagement.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, KeyManagementActivity.class);
            startActivity(intent);
        });
    }
}