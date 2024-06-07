package com.tanujn45.a11y;

import android.graphics.Bitmap;

public class Video {
    private String path;
    private Bitmap thumbnail;
    private String title;

    public Video(String path, Bitmap thumbnail, String title) {
        this.path = path;
        this.thumbnail = thumbnail;
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public String getTitle() {
        return title;
    }
}
