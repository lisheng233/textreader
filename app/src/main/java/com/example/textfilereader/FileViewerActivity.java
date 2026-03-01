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
        String fileName = getIntent().getStringExtra("file_name");
        
        if (fileName != null) {
            setTitle(fileName);
        }
        
        if (filePath != null) {
            loadFileContent(filePath);
        } else {
            Toast.makeText(this, "文件路径错误", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void loadFileContent(String filePath) {
        File file = new File(filePath);
        
        Toast.makeText(this, "正在加载文件...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            StringBuilder content = new StringBuilder();
            int lineCount = 0;
            int maxLines = 5000; // 限制显示行数，防止内存溢出
            final int[] finalLineCount = {0}; // 使用数组来存储可变的lineCount
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                
                String line;
                while ((line = reader.readLine()) != null && lineCount < maxLines) {
                    content.append(line).append("\n");
                    lineCount++;
                }
                
                finalLineCount[0] = lineCount; // 保存lineCount的值
                
                if (lineCount >= maxLines) {
                    content.append("\n\n... (文件过大，只显示前5000行)");
                }
                
                String finalContent = content.toString();
                mainHandler.post(() -> {
                    tvContent.setText(finalContent);
                    scrollView.scrollTo(0, 0);
                    // 使用finalLineCount[0]而不是lineCount
                    Toast.makeText(FileViewerActivity.this, 
                                  "加载完成，共 " + finalLineCount[0] + " 行", 
                                  Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> 
                    Toast.makeText(FileViewerActivity.this, 
                                  "读取文件失败: " + e.getMessage(), 
                                  Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
