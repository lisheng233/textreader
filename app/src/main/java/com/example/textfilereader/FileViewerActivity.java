package com.example.textfilereader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class FileViewerActivity extends AppCompatActivity {
    
    private TextView tvContent;
    private ScrollView scrollView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer);
        
        tvContent = findViewById(R.id.tvContent);
        scrollView = findViewById(R.id.scrollView);
        
        String filePath = getIntent().getStringExtra("file_path");
        if (filePath != null) {
            loadFileContent(filePath);
        } else {
            Toast.makeText(this, "文件路径错误", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void loadFileContent(String filePath) {
        File file = new File(filePath);
        setTitle(file.getName());
        
        new Thread(() -> {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                
                mainHandler.post(() -> {
                    tvContent.setText(content.toString());
                    scrollView.scrollTo(0, 0);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> 
                    Toast.makeText(this, "读取文件失败: " + e.getMessage(), 
                                  Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}