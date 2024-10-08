package com.tanujn45.a11y;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class VideoListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private VideoAdapter videoAdapter;
    private List<Video> videoList;
    private File directory;
    private String gestureName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        recyclerView = findViewById(R.id.recycler_view);
        emptyTextView = findViewById(R.id.empty_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        Intent intent = getIntent();
        gestureName = intent.getStringExtra("gestureCategoryName");

        File trimmedVideosDir = new File(directory, "trimmedVideos");

        if (!trimmedVideosDir.exists()) {
            trimmedVideosDir.mkdirs();
        }

        try {
            videoList = getVideosFromFolder();
            if (videoList.isEmpty()) {
                emptyTextView.setVisibility(View.VISIBLE);
            } else {
                emptyTextView.setVisibility(View.GONE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        videoAdapter = new VideoAdapter(videoList, video -> {
            String videoPath = video.getPath();
            System.out.println("Video path:: " + videoPath);
            // startTrimActivityWithDialog(videoPath);
            startTrimActivity(videoPath);
        });
        recyclerView.setAdapter(videoAdapter);
    }

    private List<Video> getVideosFromFolder() throws IOException {
        List<Video> videos = new ArrayList<>();

        File rawVideos = new File(directory, "rawVideos");

        File[] files = rawVideos.listFiles();

        if (files != null) {
            // sort files by name
            Arrays.sort(files, Comparator.comparing(File::getName));

            for (File file : files) {
                if (isVideoFile(file)) {
                    Bitmap thumbnail = generateThumbnail(file);
                    String title = file.getName().replaceFirst("[.][^.]+$", "");
                    videos.add(new Video(file.getPath(), thumbnail, title, false, false));
                }
            }
        }

        return videos;
    }


    private void startTrimActivityWithDialog(String videoPath) {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_dialog, null);

        // Find views in the custom layout
        EditText fileNameEditText = dialogView.findViewById(R.id.fileNameEditText);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        cancelButton.setOnClickListener(v -> {
            Toast.makeText(VideoListActivity.this, "Trimming cancelled", Toast.LENGTH_SHORT).show();
            alertDialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            gestureName = fileNameEditText.getText().toString();
            if (!gestureName.isEmpty()) {
                startTrimActivity(videoPath);
                alertDialog.dismiss();
            } else {
                Toast.makeText(VideoListActivity.this, "Gesture name is required", Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.show();
    }

    private void startTrimActivity(String videoPath) {
        Intent intent = new Intent(VideoListActivity.this, TrimVideoActivity.class);
        intent.putExtra("videoPath", videoPath);
        intent.putExtra("gestureCategoryName", gestureName);
        startActivity(intent);
        finish();
    }

    private boolean isVideoFile(File file) {
        return file.getName().toLowerCase().endsWith(".mp4");
    }

    private Bitmap generateThumbnail(File videoFile) throws IOException {
        Bitmap thumbnail = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videoFile.getPath());
            thumbnail = retriever.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return thumbnail;
    }

    public void backButtonClicked(View view) {
        finish();
    }
}