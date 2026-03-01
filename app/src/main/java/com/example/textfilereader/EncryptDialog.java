package com.example.textfilereader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
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

public class EncryptDialog extends DialogFragment {
    
    private File inputFile;
    private File outputFile;
    private KeyManager keyManager;
    private OnEncryptCompleteListener listener;
    
    public interface OnEncryptCompleteListener {
        void onEncryptComplete(boolean success, String message);
    }
    
    public EncryptDialog(File inputFile, File outputFile, OnEncryptCompleteListener listener) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.listener = listener;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        keyManager = KeyManager.getInstance(getActivity());
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_encrypt, null);
        
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        Spinner keySpinner = view.findViewById(R.id.keySpinner);
        EditText passwordInput = view.findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);
        
        // 加载已有密钥
        List<String> keyNames = keyManager.getKeyNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), 
            android.R.layout.simple_spinner_item, keyNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keySpinner.setAdapter(adapter);
        
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioExistingKey) {
                keySpinner.setEnabled(true);
                passwordInput.setEnabled(false);
                confirmPasswordInput.setEnabled(false);
            } else if (checkedId == R.id.radioNewKey) {
                keySpinner.setEnabled(false);
                passwordInput.setEnabled(true);
                confirmPasswordInput.setEnabled(true);
            } else if (checkedId == R.id.radioPassword) {
                keySpinner.setEnabled(false);
                passwordInput.setEnabled(true);
                confirmPasswordInput.setEnabled(true);
            }
        });
        
        builder.setView(view)
               .setTitle("加密文件")
               .setPositiveButton("加密", null)
               .setNegativeButton("取消", (dialog, which) -> {});
        
        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                
                try {
                    if (selectedId == R.id.radioExistingKey) {
                        // 使用已有密钥
                        String keyName = (String) keySpinner.getSelectedItem();
                        if (keyName == null) {
                            Toast.makeText(getActivity(), "请选择密钥", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        javax.crypto.SecretKey key = keyManager.getKey(keyName);
                        AESCrypt.encryptFile(inputFile, outputFile, key);
                        listener.onEncryptComplete(true, "加密成功，使用密钥: " + keyName);
                        dialog.dismiss();
                        
                    } else if (selectedId == R.id.radioNewKey) {
                        // 生成新密钥
                        String keyName = "key_" + System.currentTimeMillis();
                        keyManager.generateRandomKey(keyName);
                        javax.crypto.SecretKey key = keyManager.getKey(keyName);
                        AESCrypt.encryptFile(inputFile, outputFile, key);
                        listener.onEncryptComplete(true, "加密成功，生成新密钥: " + keyName);
                        dialog.dismiss();
                        
                    } else if (selectedId == R.id.radioPassword) {
                        // 使用密码
                        String password = passwordInput.getText().toString();
                        String confirmPassword = confirmPasswordInput.getText().toString();
                        
                        if (password.isEmpty()) {
                            Toast.makeText(getActivity(), "请输入密码", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        if (!password.equals(confirmPassword)) {
                            Toast.makeText(getActivity(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        AESCrypt.encryptFileWithPassword(inputFile, outputFile, password);
                        listener.onEncryptComplete(true, "加密成功，使用密码");
                        dialog.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onEncryptComplete(false, "加密失败: " + e.getMessage());
                    dialog.dismiss();
                }
            });
        });
        
        return dialog;
    }
}