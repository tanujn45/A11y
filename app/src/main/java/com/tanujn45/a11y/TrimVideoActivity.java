package com.tanujn45.a11y;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;

import com.tanujn45.a11y.VideoTrimmer.VideoTrimmer;
import com.tanujn45.a11y.VideoTrimmer.interfaces.OnTrimVideoListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class TrimVideoActivity extends AppCompatActivity  implements OnTrimVideoListener {
    private VideoTrimmer mVideoTrimmer;
    private File directory;
    private String path;
    private String gestureName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trim_video);

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File trimmedVideosDir = new File(directory, "trimmedVideos");
        if (!trimmedVideosDir.exists()) {
            trimmedVideosDir.mkdirs();
        }

        path = getIntent().getStringExtra("videoPath");
        gestureName = getIntent().getStringExtra("gestureName");

        File file = new File(path);
        try {
            FileInputStream fis = new FileInputStream(file);
            System.out.println(fis.getChannel());
            fis.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }





        mVideoTrimmer = findViewById(R.id.videoTrimmer);
        if (mVideoTrimmer != null) {
            mVideoTrimmer.setMaxDuration(60);
            mVideoTrimmer.setOnTrimVideoListener(this);
            mVideoTrimmer.setOnTrimVideoListener(this);
            mVideoTrimmer.setDestinationPath(trimmedVideosDir.getPath() + "/");
            mVideoTrimmer.setVideoURI(Uri.parse(path));
        }

        int startPosition = mVideoTrimmer.getStartPosition();
        int endPosition = mVideoTrimmer.getEndPosition();
        System.out.println("Start position: " + startPosition);
        System.out.println("End position: " + endPosition);
    }

    @Override
    public void onTrimStarted() {

    }

    @Override
    public void getResult(Uri uri, int startPostion, int endPostion) {
        Intent intent = new Intent(this, VideoListActivity.class);
        intent.putExtra("videoPath", uri.toString());
        intent.putExtra("gestureName", gestureName);
        intent.putExtra("startPosition", startPostion);
        intent.putExtra("endPosition", endPostion);
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