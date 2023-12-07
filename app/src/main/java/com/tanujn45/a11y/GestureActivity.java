package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GestureActivity extends AppCompatActivity {
    private static final String LOG_TAG = GestureActivity.class.getSimpleName();
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String PATH = "/Meas/IMU6/";
    private static final String RATE = "104";
    String connectedSerial;
    private List<CardData> cardDataList;
    private RecyclerView recyclerView;
    private CardAdapter adapter;
    private MdsSubscription mdsSubscription;

    private Mds getMds() {
        return MainActivity.mMds;
    }

    private String getConnectedSerial() {
        return MainActivity.connectedSerial;
    }

    ImageButton settingsButton;
    Button gestureButton;
    TextView sensorMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);


        connectedSerial = getConnectedSerial();

        settingsButton = findViewById(R.id.settingsButton);
        gestureButton = findViewById(R.id.addGestureButton);
        sensorMsg = findViewById(R.id.sensorMsg);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        cardDataList = new ArrayList<>();

        adapter = new CardAdapter(cardDataList);
        recyclerView.setAdapter(adapter);

        try {
            fetchDataFromFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        subscribeToSensor(connectedSerial);
    }

    private void fetchDataFromFiles() throws IOException {
        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (file.isFile() && file.getName().startsWith("info_")) {

                    String name = "";
                    String textToSpeak = "";
                    String description = "";
                    int numOfGestures = 0;

                    BufferedReader reader = new BufferedReader(new FileReader(file));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("########## Name ##########")) {
                            name = reader.readLine();
                        } else if (line.startsWith("########## Text To Speak ##########")) {
                            textToSpeak = reader.readLine();
                        } else if (line.startsWith("########## Description ##########")) {
                            description = reader.readLine();
                        } else if (line.startsWith("########## Number of Gestures ##########")) {
                            numOfGestures = Integer.parseInt(reader.readLine());
                        }
                    }
                    reader.close();

                    CardData cardData = new CardData();
                    cardData.setName(name);
                    cardData.setTextToSpeak(textToSpeak);
                    cardData.setDescription(description);
                    cardData.setNumOfGestures(numOfGestures);

                    cardDataList.add(cardData);
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void subscribeToSensor(String connectedSerial) {
        if (mdsSubscription != null) {
            unsubscribe();
        }

        String strContract = "{\"Uri\": \"" + connectedSerial + PATH + RATE + "\"}";
        Log.d(LOG_TAG, strContract);

        mdsSubscription = getMds().builder().build(this).subscribe(URI_EVENTLISTENER, strContract, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                Log.d(LOG_TAG, "onNotification(): " + data);

                ImuModel imuModel = new Gson().fromJson(data, ImuModel.class);

                if (imuModel != null && imuModel.getBody().getArrayAcc().length > 0 && imuModel.getBody().getArrayGyro().length > 0) {

                    String resultStr = String.format(Locale.getDefault(), "%.2f %.2f %.2f\n%.2f %.2f %.2f", imuModel.getBody().getArrayAcc()[0].getX(), imuModel.getBody().getArrayAcc()[0].getY(), imuModel.getBody().getArrayAcc()[0].getZ(), imuModel.getBody().getArrayGyro()[0].getX(), imuModel.getBody().getArrayGyro()[0].getY(), imuModel.getBody().getArrayGyro()[0].getZ());

                    sensorMsg.setText(resultStr);
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
        startActivity(intent);
    }
}