package com.tanujn45.a11y;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//Todo: Update the UI to not have a separate set filter activity

public class VisualizationActivity extends AppCompatActivity {
    List<String> gestureNamesList = new ArrayList<>();
    HashMap<String, String> gestureNameToPath = new HashMap<>();

    File directory;
    Spinner gestureOneSpinner, gestureTwoSpinner;
    ImageView gestureOneThumbnail, gestureTwoThumbnail;
    ArrayAdapter<String> adapterOne, adapterTwo;

    String gestureOneVideoPath, gestureTwoVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization);

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        gestureOneSpinner = findViewById(R.id.gestureOneSpinner);
        gestureTwoSpinner = findViewById(R.id.gestureTwoSpinner);
        gestureOneThumbnail = findViewById(R.id.videoThumbnailOne);
        gestureTwoThumbnail = findViewById(R.id.videoThumbnailTwo);

        initGestureDict();
        initSpinners();
    }

    private void initGestureDict() {
        String pathToTrimmedData = directory.getAbsolutePath() + "/trimmedData";
        File trimmedData = new File(pathToTrimmedData);

        if (!trimmedData.exists() || !trimmedData.isDirectory()) {
            throw new RuntimeException("Trimmed data directory does not exist");
        }

        File[] gestureDirectories = trimmedData.listFiles();

        if (gestureDirectories == null) {
            return;
        }

        for (File dir : gestureDirectories) {
            if (!dir.isDirectory()) {
                continue;
            }

            File[] gestureFiles = dir.listFiles();
            if (gestureFiles == null) {
                continue;
            }

            for (File gesture : gestureFiles) {
                if (gesture.isDirectory()) {
                    continue;
                }

                String currGesture = gesture.getName();
                if (currGesture.startsWith("master")) {
                    continue;
                }

                if (currGesture.endsWith(".csv")) {
                    String path = gesture.getAbsolutePath();
                    String name = "";

                    int lastDotIndex = currGesture.lastIndexOf(".");
                    if (lastDotIndex > 0) {
                        name = currGesture.substring(0, lastDotIndex);
                    }

                    gestureNamesList.add(name);
                    gestureNameToPath.put(name, path);
                }
            }
        }
    }

    private void initSpinners() {
        adapterOne = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gestureNamesList);
        adapterOne.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gestureOneSpinner.setAdapter(adapterOne);
        gestureOneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                String path = gestureNameToPath.get(selectedItem);
                gestureOneVideoPath = path.replace(".csv", ".mp4");
                setVideoThumbnail(gestureOneThumbnail, gestureOneVideoPath);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        adapterTwo = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, gestureNamesList);
        adapterTwo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gestureTwoSpinner.setAdapter(adapterTwo);
        gestureTwoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                String path = gestureNameToPath.get(selectedItem);
                gestureTwoVideoPath = path.replace(".csv", ".mp4");
                setVideoThumbnail(gestureTwoThumbnail, gestureTwoVideoPath);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private Bitmap getVideoThumbnail(String videoPath) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoPath);
            return retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            retriever.release();
        }
    }

    private void setVideoThumbnail(ImageView imageView, String videoPath) {
        try {
            Bitmap thumbnail = getVideoThumbnail(videoPath);
            imageView.setImageBitmap(thumbnail);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void playInstanceVideoOne(View view) {
        Intent intent = new Intent(this, VideoPlayer.class);
        intent.putExtra("videoPath", gestureOneVideoPath);
        startActivity(intent);
    }

    public void playInstanceVideoTwo(View view) {
        String videoPath = "";
        Intent intent = new Intent(this, VideoPlayer.class);
        intent.putExtra("videoPath", gestureTwoVideoPath);
        startActivity(intent);
    }

    public void setFiltersButtonClicked(View view) {
        Intent intent = new Intent(this, FilterActivity.class);
        startActivity(intent);
    }
}