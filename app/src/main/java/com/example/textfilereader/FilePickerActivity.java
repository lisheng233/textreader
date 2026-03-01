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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class FilePickerActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private ListView listView;
    private TextView tvCurrentPath;
    private File currentDir;
    private ArrayList<File> fileList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    
    // 支持的文���类型 - 扩展更多格式
    private final String[] supportedExtensions = {
        ".txt", ".log", ".md", ".json", ".xml", ".html", ".htm", ".csv",
        ".properties", ".conf", ".cfg", ".ini", ".bat", ".sh", ".js", ".css",
        ".java", ".kt", ".py", ".cpp", ".c", ".h", ".php", ".asp", ".aspx",
        ".jsp", ".rb", ".pl", ".sql", ".yaml", ".yml", ".toml"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);
        
        listView = findViewById(R.id.listView);
        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        
        // 设置标题
        setTitle("选择文件");
        
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
        // 尝试多个可能的起始目录
        File startDir = null;
        
        // 首先尝试外部存储
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            startDir = Environment.getExternalStorageDirectory();
        }
        
        // 如果外部存储不可用，尝试内部存储
        if (startDir == null || !startDir.exists()) {
            startDir = Environment.getRootDirectory();
        }
        
        // 如果还不行，使用应用的文件目录
        if (startDir == null || !startDir.exists()) {
            startDir = getFilesDir();
        }
        
        currentDir = startDir;
        fillFileList(currentDir);
    }
    
    private void fillFileList(File dir) {
        fileList.clear();
        
        // 检查目录是否可读
        if (dir == null || !dir.exists() || !dir.canRead()) {
            tvCurrentPath.setText("无法访问: " + (dir != null ? dir.getAbsolutePath() : "未知路径"));
            updateList();
            Toast.makeText(this, "无法读取目录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        File[] files = dir.listFiles();
        
        tvCurrentPath.setText("当前路径: " + dir.getAbsolutePath());
        
        if (files != null && files.length > 0) {
            // 转换为ArrayList以便排序
            ArrayList<File> fileArrayList = new ArrayList<>(Arrays.asList(files));
            
            // 按名称排序（忽略大小写）
            Collections.sort(fileArrayList, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });
            
            // 添加上级目录（如果不是根目录）
            if (dir.getParentFile() != null && dir.getParentFile().canRead()) {
                fileList.add(dir.getParentFile());
            }
            
            // 先添加所有文件夹
            for (File file : fileArrayList) {
                if (file.isDirectory() && file.canRead()) {
                    fileList.add(file);
                }
            }
            
            // 再添加所有支持的文件
            for (File file : fileArrayList) {
                if (file.isFile() && file.canRead()) {
                    if (isSupportedFile(file.getName())) {
                        fileList.add(file);
                    }
                }
            }
            
            // 调试信息
            System.out.println("找到 " + fileArrayList.size() + " 个项目");
            System.out.println("显示 " + fileList.size() + " 个项目（包含返回上级）");
        } else {
            Toast.makeText(this, "目录为空或无法读取", Toast.LENGTH_SHORT).show();
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
        
        if (fileList.isEmpty()) {
            names.add("📁 此文件夹为空");
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(null); // 禁用点击
            return;
        }
        
        for (File file : fileList) {
            if (file.isDirectory()) {
                // 判断是否是上级目录
                if (file.getParentFile() != null && 
                    currentDir.getParentFile() != null && 
                    file.getAbsolutePath().equals(currentDir.getParentFile().getAbsolutePath())) {
                    names.add("🔙 .. (返回上级)");
                } else {
                    names.add("📁 " + file.getName());
                }
            } else {
                // 显示文件大小
                String fileSize = getFileSize(file.length());
                names.add("📄 " + file.getName() + " (" + fileSize + ")");
            }
        }
        
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (fileList.isEmpty()) return;
                
                File selectedFile = fileList.get(position);
                
                // 处理点击事件
                if (selectedFile.isDirectory()) {
                    // 如果是目录，进入该目录
                    if (selectedFile.canRead()) {
                        currentDir = selectedFile;
                        fillFileList(selectedFile);
                    } else {
                        Toast.makeText(FilePickerActivity.this, "无法读取该目录", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 如果是文件，打开它
                    openFile(selectedFile);
                }
            }
        });
    }
    
    private String getFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    private void openFile(File file) {
        if (file.length() > 50 * 1024 * 1024) { // 增大到50MB限制
            Toast.makeText(this, "文件太大（超过50MB），无法打开", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, FileViewerActivity.class);
        intent.putExtra("file_path", file.getAbsolutePath());
        intent.putExtra("file_name", file.getName());
        startActivity(intent);
    }
    
    @Override
    public void onBackPressed() {
        // 返回上级目录，而不是退出Activity
        if (currentDir != null && currentDir.getParentFile() != null && 
            currentDir.getParentFile().canRead()) {
            currentDir = currentDir.getParentFile();
            fillFileList(currentDir);
        } else {
            super.onBackPressed();
        }
    }
}
