package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;

import java.io.File;

public class GestureActivity extends AppCompatActivity {
    private static final String LOG_TAG = GestureActivity.class.getSimpleName();
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    static private final String URI_MEAS_ACC_13 = "/Meas/Acc/13";
    public static final String SERIAL = "serial";
    String connectedSerial;
    private MdsSubscription mdsSubscription;

    private Mds getMds() {
        return MainActivity.mMds;
    }

    ImageButton settingsButton;
    Button gestureButton;
    TextView sensorMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);

        Intent intent = getIntent();
        connectedSerial = intent.getStringExtra(SERIAL);

        settingsButton = findViewById(R.id.settingsButton);
        gestureButton = findViewById(R.id.addGestureButton);
        sensorMsg = findViewById(R.id.sensorMsg);

        subscribeToSensor(connectedSerial);
    }

    private void subscribeToSensor(String connectedSerial) {
        if (mdsSubscription != null) {
            unsubscribe();
        }

        String strContract = "{\"Uri\": \"" + connectedSerial + URI_MEAS_ACC_13 + "\"}";
        Log.d(LOG_TAG, strContract);

        mdsSubscription = getMds().builder().build(this).subscribe(URI_EVENTLISTENER,
                strContract, new MdsNotificationListener() {
                    @Override
                    public void onNotification(String data) {
                        Log.d(LOG_TAG, "onNotification(): " + data);


                        AccDataResponse accResponse = new Gson().fromJson(data, AccDataResponse.class);
                        if (accResponse != null && accResponse.body.array.length > 0) {

                            String accStr =
                                    String.format("%.02f, %.02f, %.02f", accResponse.body.array[0].x, accResponse.body.array[0].y, accResponse.body.array[0].z);

                            sensorMsg.setText(accStr);
                        }
                    }

                    @Override
                    public void onError(MdsException error) {
                        Log.e(LOG_TAG, "subscription onError(): ", error);
                        unsubscribe();
                    }
                });

    }

    private void unsubscribe() {
        if (mdsSubscription != null) {
            mdsSubscription.unsubscribe();
            mdsSubscription = null;
        }
    }

    public void settingsButtonClicked(View view) {
        Intent intent = new Intent(GestureActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void addGestureButtonClicked(View view) {
        unsubscribe();
        Intent intent = new Intent(GestureActivity.this, RegisterActivity.class);
        intent.putExtra(RegisterActivity.SERIAL, connectedSerial);
        startActivity(intent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}