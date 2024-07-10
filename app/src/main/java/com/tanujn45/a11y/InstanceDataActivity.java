package com.tanujn45.a11y;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.tanujn45.a11y.CSVEditor.CSVFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


//Todo: Implement Video player
//Todo: Implement deletion of the instance
//Todo: Fix the Diff and MA plots

public class InstanceDataActivity extends AppCompatActivity {
    private LineChart accChart, accMAChart, accDiffChart, gyroChart;
    private TextView instanceNameTextView;
    private ImageView instanceImageView;
    private File directory;
    private String instanceName, gestureName, path, folderName, videoPath;
    private CSVFile subMaster, instanceData;
    private float startTime, endTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_data);

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        Intent intent = getIntent();
        instanceName = intent.getStringExtra("instanceName");
        gestureName = intent.getStringExtra("gestureCategoryName");

        folderName = gestureName.replace(" ", "_").toLowerCase();
        path = directory.getAbsolutePath() + "/trimmedData/" + folderName;
        videoPath = directory.getAbsolutePath() + "/trimmedData/" + folderName + "/" + instanceName + ".mp4";

        File subMasterFile = new File(path, "master_" + folderName + ".csv");

        try {
            subMaster = new CSVFile(subMasterFile.getAbsolutePath());
            instanceData = new CSVFile(new File(path, instanceName + ".csv").getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        getStartEndTime();

        instanceNameTextView = findViewById(R.id.instanceName);
        instanceNameTextView.setText(instanceName);
        instanceImageView = findViewById(R.id.videoThumbnail);

        Bitmap thumbnail;
        try {
            thumbnail = getVideoThumbnail(videoPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (thumbnail != null) {
            instanceImageView.setImageBitmap(thumbnail);
        }

        accChart = findViewById(R.id.accLineChart);
        accMAChart = findViewById(R.id.accMALineChart);
        accDiffChart = findViewById(R.id.accDiffLineChart);
        gyroChart = findViewById(R.id.gyroLineChart);

        setChart(accChart, "acc");
        setChart(accMAChart, "acc_ma");
        setChart(accDiffChart, "acc_diff");
        setChart(gyroChart, "gyro");
    }

    private Bitmap getVideoThumbnail(String videopath) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(videopath);
            return retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            retriever.release();
        }
    }

    private void getStartEndTime() {
        String[] row = subMaster.getRowWithData(instanceName);
        startTime = Float.parseFloat(row[1]);
        endTime = Float.parseFloat(row[2]);
    }

    private void setChart(LineChart chart, String filter) {
        List<Entry> xEntries = new ArrayList<>();
        List<Entry> yEntries = new ArrayList<>();
        List<Entry> zEntries = new ArrayList<>();

        List<String> xData = instanceData.getColumnData(filter + "_x", startTime, endTime);
        List<String> yData = instanceData.getColumnData(filter + "_y", startTime, endTime);
        List<String> zData = instanceData.getColumnData(filter + "_z", startTime, endTime);

        for(int i = 0; i < xData.size(); i++) {
            float x = Float.parseFloat(xData.get(i));
            float y = Float.parseFloat(yData.get(i));
            float z = Float.parseFloat(zData.get(i));

            xEntries.add(new Entry(i, x));
            yEntries.add(new Entry(i, y));
            zEntries.add(new Entry(i, z));
        }

        LineDataSet xDataSet = new LineDataSet(xEntries, "X");
        xDataSet.setColor(Color.RED);
        xDataSet.setDrawCircles(false);
        LineDataSet yDataSet = new LineDataSet(yEntries, "Y");
        yDataSet.setColor(Color.GREEN);
        yDataSet.setDrawCircles(false);
        LineDataSet zDataSet = new LineDataSet(zEntries, "Z");
        zDataSet.setColor(Color.BLUE);
        zDataSet.setDrawCircles(false);

        LineData lineData = new LineData(xDataSet, yDataSet, zDataSet);
        chart.setData(lineData);
        chart.getDescription().setEnabled(false);
        chart.setDrawMarkers(false);

        chart.invalidate();
    }
}
