package com.example.textfilereader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class FileViewerActivity extends AppCompatActivity {
    
    private TextView tvContent;
    private ScrollView scrollView;
    private LinearLayout paginationLayout;
    private Button btnPrevPage;
    private Button btnNextPage;
    private TextView tvPageInfo;
    
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // 分页相关变量
    private List<Long> pagePositions = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 0;
    private String filePath;
    private static final int PAGE_SIZE = 100; // 每页显示行数
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer);
        
        tvContent = findViewById(R.id.tvContent);
        scrollView = findViewById(R.id.scrollView);
        paginationLayout = findViewById(R.id.paginationLayout);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        
        filePath = getIntent().getStringExtra("file_path");
        String fileName = getIntent().getStringExtra("file_name");
        
        if (fileName != null) {
            setTitle(fileName);
        }
        
        if (filePath != null) {
            // 直接分析文件并建立索引
            analyzeFile();
        } else {
            Toast.makeText(this, "文件路径错误", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        // 设置翻页按钮点击事件
        btnPrevPage.setOnClickListener(v -> goToPage(currentPage - 1));
        btnNextPage.setOnClickListener(v -> goToPage(currentPage + 1));
    }
    
    private void analyzeFile() {
        new Thread(() -> {
            try {
                pagePositions.clear();
                pagePositions.add(0L); // 第一页从文件开头开始
                
                RandomAccessFile raf = new RandomAccessFile(filePath, "r");
                long fileLength = raf.length();
                int lineCount = 0;
                
                while (raf.getFilePointer() < fileLength) {
                    String line = raf.readLine();
                    if (line == null) break;
                    
                    lineCount++;
                    
                    // 每PAGE_SIZE行记录一个分页位置
                    if (lineCount % PAGE_SIZE == 0) {
                        pagePositions.add(raf.getFilePointer());
                    }
                }
                
                raf.close();
                
                totalPages = pagePositions.size();
                
                mainHandler.post(() -> {
                    if (totalPages > 0) {
                        paginationLayout.setVisibility(View.VISIBLE);
                        loadPage(0);
                    } else {
                        tvContent.setText("文件为空");
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> 
                    Toast.makeText(FileViewerActivity.this, 
                                  "分析文件失败: " + e.getMessage(), 
                                  Toast.LENGTH_LONG).show());
            }
        }).start();
    }
    
    private void loadPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= totalPages) return;
        
        currentPage = pageIndex;
        updatePageInfo();
        
        new Thread(() -> {
            try {
                long startPosition = pagePositions.get(pageIndex);
                long endPosition = (pageIndex < totalPages - 1) ? 
                                   pagePositions.get(pageIndex + 1) : -1;
                
                StringBuilder content = new StringBuilder();
                int linesLoaded = 0;
                
                RandomAccessFile raf = new RandomAccessFile(filePath, "r");
                raf.seek(startPosition);
                
                while (linesLoaded < PAGE_SIZE) {
                    String line = raf.readLine();
                    if (line == null) break;
                    
                    // 处理编码问题
                    byte[] bytes = line.getBytes("ISO-8859-1");
                    String decodedLine = new String(bytes, "UTF-8");
                    
                    content.append(decodedLine).append("\n");
                    linesLoaded++;
                    
                    // 检查是否到达本页末尾
                    if (endPosition > 0 && raf.getFilePointer() >= endPosition) {
                        break;
                    }
                }
                
                raf.close();
                
                String finalContent = content.toString();
                
                mainHandler.post(() -> {
                    tvContent.setText(finalContent);
                    scrollView.scrollTo(0, 0);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> 
                    Toast.makeText(FileViewerActivity.this, 
                                  "加载页面失败: " + e.getMessage(), 
                                  Toast.LENGTH_LONG).show());
            }
        }).start();
    }
    
    private void goToPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < totalPages) {
            loadPage(pageIndex);
        }
    }
    
    private void updatePageInfo() {
        tvPageInfo.setText("第 " + (currentPage + 1) + " 页 / 共 " + totalPages + " 页");
        btnPrevPage.setEnabled(currentPage > 0);
        btnNextPage.setEnabled(currentPage < totalPages - 1);
    }
}
