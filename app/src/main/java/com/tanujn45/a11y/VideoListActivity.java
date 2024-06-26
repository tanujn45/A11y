package com.tanujn45.a11y;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VideoListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private List<Video> videoList;
    private File directory;
    private String gestureName;
    ActivityResultLauncher<Intent> trimForResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        File trimmedVideosDir = new File(directory, "trimmedVideos");

        if (!trimmedVideosDir.exists()) {
            trimmedVideosDir.mkdirs();
        }

        try {
            videoList = getVideosFromFolder();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        videoAdapter = new VideoAdapter(videoList, video -> {
            String videoPath = video.getPath();
            System.out.println("Video path:: " + videoPath);
            startTrimActivityWithDialog(videoPath);
        });
        recyclerView.setAdapter(videoAdapter);
    }


    private void moveFile(Uri sourceUri, File destFile) {
        String sourceFilePath = sourceUri.getPath();

        assert sourceFilePath != null;
        File sourceFile = new File(sourceFilePath);

        if (!sourceFile.exists()) {
            Log.e("moveFile", "Source file does not exist: " + sourceFilePath);
            return;
        }

        if (destFile.exists()) {
            Log.e("moveFile", "Destination file already exists: " + destFile.getAbsolutePath());
            return;
        }

        try {
            if (!destFile.getParentFile().exists()) {
                destFile.getParentFile().mkdirs();
            }

            boolean success = sourceFile.renameTo(destFile);

            if (!success) {
                Log.e("moveFile", "Failed to move file");
            }
        } catch (Exception e) {
            Log.e("moveFile", "Error moving file: " + e.getMessage());
        }
    }

    private List<Video> getVideosFromFolder() throws IOException {
        List<Video> videos = new ArrayList<>();

        File rawVideos = new File(directory, "rawVideos");

        File[] files = rawVideos.listFiles();

        if (files != null) {
            for (File file : files) {
                if (isVideoFile(file)) {
                    Bitmap thumbnail = generateThumbnail(file);
                    String title = file.getName().replaceFirst("[.][^.]+$", "");
                    videos.add(new Video(file.getPath(), thumbnail, title));
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
                // Do something here with the gesture name
                Intent intent = new Intent(VideoListActivity.this, TrimVideoActivity.class);
                intent.putExtra("videoPath", videoPath);
                intent.putExtra("gestureName", gestureName);
                startActivity(intent);
                finish();
                alertDialog.dismiss();
            } else {
                Toast.makeText(VideoListActivity.this, "Gesture name is required", Toast.LENGTH_SHORT).show();
            }
        });

        alertDialog.show();
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
}