package com.tanujn45.a11y.VideoTrimmer.interfaces;

import android.net.Uri;

public interface OnTrimVideoListener {

    void onTrimStarted();

    void getResult(final Uri uri, final int startPostion, final int endPostion) throws Exception;

    void cancelAction();

    void onError(final String message);
}
