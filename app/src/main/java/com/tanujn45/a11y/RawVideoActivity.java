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
import java.util.List;

public class RawVideoActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private VideoAdapter videoAdapter;
    private List<Video> videoList;
    private File directory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_raw_video);

        recyclerView = findViewById(R.id.recycler_view);
        emptyTextView = findViewById(R.id.empty_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

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
            editRawVideoDialog(videoPath);
        });
        recyclerView.setAdapter(videoAdapter);
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

    private void editRawVideoDialog(String videoPath) {
        // Inflate the custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_dialog, null);

        // Find views in the custom layout
        TextView titleTextView = dialogView.findViewById(R.id.alertTitle);
        EditText fileNameEditText = dialogView.findViewById(R.id.fileNameEditText);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);
        Button renameButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        titleTextView.setText("Enter new video title");
        deleteButton.setText("Delete");
        renameButton.setText("Rename");

        // Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        deleteButton.setOnClickListener(v -> {
            deleteData(videoPath);
            Intent intent = getIntent();
            finish();
            startActivity(intent);
            alertDialog.dismiss();
        });

        renameButton.setOnClickListener(v -> {
            String renamedFileName = fileNameEditText.getText().toString();

            if (renamedFileName.isEmpty()) {
                Toast.makeText(this, "Please enter a valid file name", Toast.LENGTH_SHORT).show();
                return;
            }

            renamedFileName = renamedFileName.replace(" ", "_");
            renameData(videoPath, renamedFileName);
            Intent intent = getIntent();
            finish();
            startActivity(intent);
            alertDialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> alertDialog.dismiss());

        alertDialog.show();
    }

    private void deleteData(String videoPath) {
        String csvPath = videoPath.replace("rawVideos", "rawData").replace(".mp4", ".csv");
        File videoFile = new File(videoPath);
        File csvFile = new File(csvPath);

        if (!videoFile.exists() || !csvFile.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (videoFile.delete() && csvFile.delete()) {
            Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
        }
    }

    private void renameData(String videoPath, String newFileName) {
        String csvPath = videoPath.replace("rawVideos", "rawData").replace(".mp4", ".csv");
        File videoFile = new File(videoPath);
        File csvFile = new File(csvPath);

        if (!videoFile.exists() || !csvFile.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        File newVideoFile = new File(videoFile.getParent(), newFileName + ".mp4");
        File newCsvFile = new File(csvFile.getParent(), newFileName + ".csv");

        if (newVideoFile.exists() || newCsvFile.exists()) {
            Toast.makeText(this, "File with the same name already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        if (videoFile.renameTo(newVideoFile) && csvFile.renameTo(newCsvFile)) {
            Toast.makeText(this, "File renamed successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to rename file", Toast.LENGTH_SHORT).show();
        }
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