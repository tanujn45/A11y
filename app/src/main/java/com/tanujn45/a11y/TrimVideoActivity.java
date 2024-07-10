package com.tanujn45.a11y;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;

import com.tanujn45.a11y.CSVEditor.CSVFile;
import com.tanujn45.a11y.VideoTrimmer.VideoTrimmer;
import com.tanujn45.a11y.VideoTrimmer.interfaces.OnTrimVideoListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Objects;

//Todo: Implement the on Trim cancel function to go back to the Instance activity
//Todo: Implement a preloader while trim is happening

public class TrimVideoActivity extends AppCompatActivity implements OnTrimVideoListener {
    private VideoTrimmer mVideoTrimmer;
    private File directory;
    private String videoPath, gestureName, folderName, path;
    CSVFile master, subMaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trim_video);

        videoPath = getIntent().getStringExtra("videoPath");
        gestureName = getIntent().getStringExtra("gestureCategoryName");

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        folderName = gestureName.replace(" ", "_").toLowerCase();
        path = directory.getAbsolutePath() + "/trimmedData/" + folderName;
        File trimmedVideosDir = new File(path);

        try {
            master = new CSVFile(new File(directory, "master.csv").getAbsolutePath());
            subMaster = new CSVFile(new File(path + "/master_" + folderName + ".csv").getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        mVideoTrimmer = findViewById(R.id.videoTrimmer);
        if (mVideoTrimmer != null) {
            mVideoTrimmer.setMaxDuration(60);
            mVideoTrimmer.setOnTrimVideoListener(this);
            mVideoTrimmer.setOnTrimVideoListener(this);
            mVideoTrimmer.setDestinationPath(trimmedVideosDir.getPath() + "/");
            mVideoTrimmer.setVideoURI(Uri.parse(videoPath));
        }
    }

    private int getInstanceCount() {
        String[] row = master.getRowWithData(gestureName);
        return Integer.parseInt(row[row.length - 1]);
    }

    private void incrementInstanceCount() {
        String[] row = master.getRowWithData(gestureName);
        int count = Integer.parseInt(row[row.length - 1]);
        count++;
        row[row.length - 1] = String.valueOf(count);
        if(master.editRowWithData(gestureName, row)) {
            master.save();
        }
    }

    @Override
    public void onTrimStarted() {

    }

    @Override
    public void getResult(Uri uri, int startPostion, int endPostion) {
        File file = new File(Objects.requireNonNull(uri.getPath()));
        String fileName = folderName + "_" + (getInstanceCount() + 1);
        File newFile = new File(path + "/" + fileName + ".mp4");

        if (file.renameTo(newFile)) {
            System.out.println("File is moved successful!");
            incrementInstanceCount();

        } else {
            System.out.println("File is failed to move!");
            // Handle this case
        }

        // Trim the csv and apply the filters
        String csvPath = videoPath.replace(".mp4", ".csv").replace("Videos", "Data");
        File rawCSV = new File(csvPath);
        File newCSV = new File(path + "/" + fileName + ".csv");

        try {
            Path sourcePath = Paths.get(rawCSV.getAbsolutePath());
            Path destinationPath = Paths.get(newCSV.getAbsolutePath());
            Files.copy(sourcePath, destinationPath);

            System.out.println("File copied successfully!");

        } catch (IOException e) {
            System.out.println("Failed to copy file: " + e.getMessage());
        }

        try {
            CSVFile csvFile = new CSVFile(newCSV.getAbsolutePath());

            csvFile.printData(0);
            csvFile.printData(2);

            csvFile.applyTime();
            csvFile.printData(0);
            csvFile.printData(2);

            csvFile.trimData(startPostion, endPostion);
            csvFile.applyMovingAverage();
            csvFile.applyDifferentiation();
            csvFile.save();
            double startTimeInSeconds = (double) startPostion / 1000.0;
            double endTimeInSeconds = (double) endPostion / 1000.0;
            subMaster.addRow(new String[]{fileName, String.valueOf(startTimeInSeconds), String.valueOf(endTimeInSeconds)});
            subMaster.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        Intent intent = new Intent(this, GestureInstanceActivity.class);
        intent.putExtra("gestureCategoryName", gestureName);
        startActivity(intent);
        finish();
    }

    @Override
    public void cancelAction() {

    }

    @Override
    public void onError(String s) {

    }
}
