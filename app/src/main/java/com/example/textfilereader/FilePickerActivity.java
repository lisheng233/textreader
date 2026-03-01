package com.example.textfilereader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FilePickerActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private ListView listView;
    private TextView tvCurrentPath;
    private File currentDir;
    private ArrayList<File> fileList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    
    private final String[] supportedExtensions = {".txt", ".log", ".md", ".json", ".xml", ".html", ".csv"};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);
        
        listView = findViewById(R.id.listView);
        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        
        checkPermissions();
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                loadFileList();
            }
        } else {
            loadFileList();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFileList();
            } else {
                Toast.makeText(this, "需要存储权限才能读取文件", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void loadFileList() {
        currentDir = Environment.getExternalStorageDirectory();
        fillFileList(currentDir);
    }
    
    private void fillFileList(File dir) {
        fileList.clear();
        File[] files = dir.listFiles();
        
        tvCurrentPath.setText("当前路径: " + dir.getAbsolutePath());
        
        if (files != null) {
            // 按名称排序
            Arrays.sort(files, Comparator.comparing(File::getName));
            
            // 添加上级目录
            if (dir.getParentFile() != null) {
                fileList.add(dir.getParentFile());
            }
            
            // 添加文件夹
            for (File file : files) {
                if (file.isDirectory()) {
                    fileList.add(file);
                }
            }
            
            // 添加支持的文件
            for (File file : files) {
                if (file.isFile() && isSupportedFile(file.getName())) {
                    fileList.add(file);
                }
            }
        }
        
        updateList();
    }
    
    private boolean isSupportedFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        for (String ext : supportedExtensions) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    private void updateList() {
        ArrayList<String> names = new ArrayList<>();
        for (File file : fileList) {
            if (file.isDirectory()) {
                if (file.getParentFile() != null && file.equals(file.getParentFile())) {
                    names.add(".. (返回上级)");
                } else {
                    names.add("📁 " + file.getName());
                }
            } else {
                names.add("📄 " + file.getName());
            }
        }
        
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            File selectedFile = fileList.get(position);
            if (selectedFile.isDirectory()) {
                currentDir = selectedFile;
                fillFileList(selectedFile);
            } else {
                openFile(selectedFile);
            }
        });
    }
    
    private void openFile(File file) {
        if (file.length() > 20 * 1024 * 1024) { // 20MB限制
            Toast.makeText(this, "文件太大，无法打开", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, FileViewerActivity.class);
        intent.putExtra("file_path", file.getAbsolutePath());
        startActivity(intent);
    }
}