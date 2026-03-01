package com.example.textfilereader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.fragment.app.DialogFragment;
import java.io.File;
import java.util.List;

public class DecryptDialog extends DialogFragment {
    
    private File inputFile;
    private File outputFile;
    private KeyManager keyManager;
    private OnDecryptCompleteListener listener;
    
    public interface OnDecryptCompleteListener {
        void onDecryptComplete(boolean success, String message);
    }
    
    public DecryptDialog(File inputFile, File outputFile, OnDecryptCompleteListener listener) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.listener = listener;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        keyManager = KeyManager.getInstance(getActivity());
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_decrypt, null);
        
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        Spinner keySpinner = view.findViewById(R.id.keySpinner);
        EditText passwordInput = view.findViewById(R.id.passwordInput);
        
        // 加载已有密钥
        List<String> keyNames = keyManager.getKeyNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), 
            android.R.layout.simple_spinner_item, keyNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keySpinner.setAdapter(adapter);
        
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioKey) {
                keySpinner.setEnabled(true);
                passwordInput.setEnabled(false);
            } else if (checkedId == R.id.radioPassword) {
                keySpinner.setEnabled(false);
                passwordInput.setEnabled(true);
            }
        });
        
        builder.setView(view)
               .setTitle("解密文件")
               .setPositiveButton("解密", null)
               .setNegativeButton("取消", (dialog, which) -> {});
        
        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                
                try {
                    if (selectedId == R.id.radioKey) {
                        // 使用密钥解密
                        String keyName = (String) keySpinner.getSelectedItem();
                        if (keyName == null) {
                            Toast.makeText(getActivity(), "请选择密钥", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        javax.crypto.SecretKey key = keyManager.getKey(keyName);
                        AESCrypt.decryptFile(inputFile, outputFile, key);
                        listener.onDecryptComplete(true, "解密成功，使用密钥: " + keyName);
                        dialog.dismiss();
                        
                    } else if (selectedId == R.id.radioPassword) {
                        // 使用密码解密
                        String password = passwordInput.getText().toString();
                        
                        if (password.isEmpty()) {
                            Toast.makeText(getActivity(), "请输入密码", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        AESCrypt.decryptFileWithPassword(inputFile, outputFile, password);
                        listener.onDecryptComplete(true, "解密成功，使用密码");
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onDecryptComplete(false, "解密失败: " + e.getMessage());
                    dialog.dismiss();
                }
            });
        });
        
        return dialog;
    }
}