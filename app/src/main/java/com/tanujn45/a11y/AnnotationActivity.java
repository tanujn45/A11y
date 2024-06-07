package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class AnnotationActivity extends AppCompatActivity {
    Button annotateNewGestureButton;
    File directory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annotation);

        annotateNewGestureButton = findViewById(R.id.annotateNewGestureButton);

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
    }


    public void annotateNewGestureButtonClicked(View view) {
        Intent intent = new Intent(AnnotationActivity.this, VideoListActivity.class);
        startActivity(intent);
    }
}