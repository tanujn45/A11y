package com.tanujn45.a11y;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.tanujn45.a11y.KMeans.KMeans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//Todo: Update the UI to not have a separate set filter activity

public class VisualizationActivity extends AppCompatActivity {
    List<String> gestureNamesList = new ArrayList<>();
    HashMap<String, String> gestureNameToPath = new HashMap<>();
    HashMap<String, String> instanceNameToGestureName = new HashMap<>();
    VideoView videoView1, videoView2;
    File directory;
    ConstraintLayout instanceOneVideoLayout, instanceTwoVideoLayout;
    Spinner gestureOneSpinner, gestureTwoSpinner;
    ImageButton playBothVideosButton;
    ImageView gestureOneThumbnail, gestureTwoThumbnail;
    ArrayAdapter<String> adapterOne, adapterTwo;
    String gestureOneVideoPath, gestureTwoVideoPath;
    String gestureOneCategory, gestureTwoCategory;
    String gestureOneInstance, gestureTwoInstance;
    boolean videoOneIsFinished;
    boolean videoTwoIsFinished;
    boolean arePlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visualization);

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        gestureOneSpinner = findViewById(R.id.gestureOneSpinner);
        gestureTwoSpinner = findViewById(R.id.gestureTwoSpinner);
        gestureOneThumbnail = findViewById(R.id.videoThumbnailOne);
        gestureTwoThumbnail = findViewById(R.id.videoThumbnailTwo);
        playBothVideosButton = findViewById(R.id.playBothVideos);
        instanceOneVideoLayout = findViewById(R.id.instanceOneVideoLayout);
        instanceTwoVideoLayout = findViewById(R.id.instanceTwoVideoLayout);
        videoView1 = findViewById(R.id.videoView1);
        videoView2 = findViewById(R.id.videoView2);

        initGestureDict();
        initSpinners();
        initVideoViews();
        setPlayBothVideosButtonWidth();

        videoOneIsFinished = false;
        videoTwoIsFinished = false;
        arePlaying = false;
    }

    public void setPlayBothVideosButtonWidth() {
        playBothVideosButton.post(new Runnable() {
            @Override
            public void run() {
                int height = playBothVideosButton.getHeight();

                ViewGroup.LayoutParams params = playBothVideosButton.getLayoutParams();
                params.width = height;
                playBothVideosButton.setLayoutParams(params);
            }
        });
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

            String dirName = dir.getName();

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
                        gestureNamesList.add(name.replace("_", " ").trim());
                    }

                    gestureNameToPath.put(name, path);
                    instanceNameToGestureName.put(name, dirName);
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
                selectedItem = selectedItem.replace(" ", "_").toLowerCase();
                gestureOneInstance = selectedItem;
                gestureOneCategory = instanceNameToGestureName.get(selectedItem);
                String path = gestureNameToPath.get(selectedItem);
                gestureOneVideoPath = path.replace(".csv", ".mp4");
                videoView1.setVideoPath(gestureOneVideoPath);
                resetVideos();
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
                selectedItem = selectedItem.replace(" ", "_").toLowerCase();
                gestureTwoInstance = selectedItem;
                gestureTwoCategory = instanceNameToGestureName.get(selectedItem);
                String path = gestureNameToPath.get(selectedItem);
                gestureTwoVideoPath = path.replace(".csv", ".mp4");
                videoView2.setVideoPath(gestureTwoVideoPath);
                resetVideos();
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

    private void initVideoViews() {
        videoView1.setOnPreparedListener(mp -> {
            mp.setVolume(0, 0);
        });

        videoView2.setOnPreparedListener(mp -> {
            mp.setVolume(0, 0);
        });

        videoView1.setOnCompletionListener(mp -> {
            videoOneIsFinished = true;
            videoView1.seekTo(0);
            startVideos();
        });

        videoView2.setOnCompletionListener(mp -> {
            videoTwoIsFinished = true;
            videoView2.seekTo(0);
            startVideos();
        });
    }

    private void startVideos() {
        if (videoOneIsFinished && videoTwoIsFinished) {
            videoView1.start();
            videoView2.start();
            videoOneIsFinished = false;
            videoTwoIsFinished = false;
        }
    }

    private void resetVideos() {
        videoView1.pause();
        videoView2.pause();
        videoView1.seekTo(0);
        videoView2.seekTo(0);
        videoView1.start();
        videoView2.start();
        videoOneIsFinished = false;
        videoTwoIsFinished = false;
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
        Intent intent = new Intent(this, InstanceDataActivity.class);
        intent.putExtra("instanceName", gestureOneInstance);
        intent.putExtra("gestureCategoryName", gestureOneCategory);
        intent.putExtra("notFromInstanceGestureIntent", true);
        intent.putExtra("videoPath", gestureOneVideoPath);
        startActivity(intent);
    }

    public void playInstanceVideoTwo(View view) {
        Intent intent = new Intent(this, InstanceDataActivity.class);
        intent.putExtra("instanceName", gestureTwoInstance);
        intent.putExtra("gestureCategoryName", gestureTwoCategory);
        intent.putExtra("notFromInstanceGestureIntent", true);
        intent.putExtra("videoPath", gestureTwoVideoPath);
        startActivity(intent);
    }

    public void playBothVideos(View view) {
        if (!arePlaying) {
            videoView1.start();
            videoView2.start();
            arePlaying = true;
            gestureOneThumbnail.setVisibility(View.INVISIBLE);
            gestureTwoThumbnail.setVisibility(View.INVISIBLE);
            instanceOneVideoLayout.setVisibility(View.INVISIBLE);
            instanceTwoVideoLayout.setVisibility(View.INVISIBLE);
            playBothVideosButton.setImageResource(R.drawable.pause);
        } else {
            videoView1.stopPlayback();
            videoView2.stopPlayback();
            arePlaying = false;
            gestureOneThumbnail.setVisibility(View.VISIBLE);
            gestureTwoThumbnail.setVisibility(View.VISIBLE);
            instanceOneVideoLayout.setVisibility(View.VISIBLE);
            instanceTwoVideoLayout.setVisibility(View.VISIBLE);
            playBothVideosButton.setImageResource(R.drawable.play);
        }
    }
}