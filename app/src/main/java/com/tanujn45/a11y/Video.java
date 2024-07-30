package com.tanujn45.a11y;

import android.graphics.Bitmap;

public class Video {
    private String path;
    private Bitmap thumbnail;
    private String title;
    private boolean showEditButton;

    public Video(String path, Bitmap thumbnail, String title) {
        this.path = path;
        this.thumbnail = thumbnail;
        this.title = title;
        this.showEditButton = true;
    }

    public Video(String path, Bitmap thumbnail, String title, boolean showEditButton) {
        this.path = path;
        this.thumbnail = thumbnail;
        this.title = title;
        this.showEditButton = showEditButton;
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

    public boolean getShowEditButton() {
        return showEditButton;
    }
}
