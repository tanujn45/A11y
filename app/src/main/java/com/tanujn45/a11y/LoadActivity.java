package com.tanujn45.a11y;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LoadActivity extends AppCompatActivity  {
    private CardView loadModelButton, loadGestureButton;
    String destinationFolder;
    private ActivityResultLauncher<String> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load);

        loadModelButton = findViewById(R.id.loadModelCardView);
        loadGestureButton = findViewById(R.id.loadGestureCardView);

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                selectedFileUri -> {
                    if (selectedFileUri != null) {
                        copyFileToAppDirectory(selectedFileUri);
                    }
                });

        loadModelButton.setOnClickListener(v -> {
            destinationFolder = "models";
            openFilePicker();
        });

        loadGestureButton.setOnClickListener(v -> {
            destinationFolder = "trimmedData";
            openFilePicker();
        });
    }

    private void openFilePicker() {
        filePickerLauncher.launch("*/*");
    }

    private void copyFileToAppDirectory(Uri selectedFileUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            String fileName = getFileName(selectedFileUri);
            if (!fileName.contains(".csv"))  {
                Toast.makeText(this,"Invalid file format. Please select a .csv file", Toast.LENGTH_SHORT).show();
                return;
            }
            File destFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS + "/" + destinationFolder), fileName);
            OutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Toast.makeText(this, "File copied successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error copying file", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}