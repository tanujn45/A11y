package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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

public class GestureActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String LOG_TAG = GestureActivity.class.getSimpleName();
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String PATH = "/Meas/IMU6/";
    private static final String RATE = "104";
    String connectedSerial;
    private List<CardData> cardDataList;
    private RecyclerView recyclerView;
    private CardAdapter adapter;
    private MdsSubscription mdsSubscription;
    private double currentTimeSeconds = System.currentTimeMillis() / 1000.0;
    private double previousTime = System.currentTimeMillis() / 1000.0;
    private TextToSpeech tts;
    String circleClockwise = "gesture_shake_0.csv";
    String circleAnticlockwise = "gesture_zigzag_0.csv";
    double clockwiseDistance;
    double anticlockwiseDistance;
    File directory;
    float[] accTemplate1, accTemplate2;

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
        tts = new TextToSpeech(this, this);

        adapter = new CardAdapter(cardDataList);
        recyclerView.setAdapter(adapter);
        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        accTemplate1 = getDatafromCSV("gesture_clockwise_4.csv", 0);
        accTemplate2 = getDatafromCSV("gesture_anticlockwise_4.csv", 0);

        try {
            fetchDataFromFiles();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        subscribeToSensor(connectedSerial);
    }

    private void fetchDataFromFiles() throws IOException {


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
        //Log.d(LOG_TAG, strContract);

        List<Float> accX = new ArrayList<>();
        List<Float> accY = new ArrayList<>();
        List<Float> accZ = new ArrayList<>();

        mdsSubscription = getMds().builder().build(this).subscribe(URI_EVENTLISTENER, strContract, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                //Log.d(LOG_TAG, "onNotification(): " + data);

                ImuModel imuModel = new Gson().fromJson(data, ImuModel.class);

                if (imuModel != null && imuModel.getBody().getArrayAcc().length > 0 && imuModel.getBody().getArrayGyro().length > 0) {

                    String resultStr = String.format(Locale.getDefault(), "%.2f %.2f %.2f\n%.2f %.2f %.2f", imuModel.getBody().getArrayAcc()[0].getX(), imuModel.getBody().getArrayAcc()[0].getY(), imuModel.getBody().getArrayAcc()[0].getZ(), imuModel.getBody().getArrayGyro()[0].getX(), imuModel.getBody().getArrayGyro()[0].getY(), imuModel.getBody().getArrayGyro()[0].getZ());

                    currentTimeSeconds = System.currentTimeMillis() / 1000.0;

                    accX.add((float) imuModel.getBody().getArrayAcc()[0].getX());
                    accY.add((float) imuModel.getBody().getArrayAcc()[0].getY());
                    accZ.add((float) imuModel.getBody().getArrayAcc()[0].getZ());

                    double elapsedTime = currentTimeSeconds - previousTime;
                    if (elapsedTime >= 2.0) {

                        //float[][] acc = new float[3][accX.size()];

                        float[] accM = new float[accX.size()];

                        for (int i = 0; i < accX.size(); i++) {
                            accM[i] = (float) Math.sqrt(
                                    accX.get(i) * accX.get(i) +
                                            accY.get(i) * accY.get(i) +
                                            accZ.get(i) * accZ.get(i)
                            );
                            /*
                            acc[0][i] = accX.get(i);
                            acc[1][i] = accY.get(i);
                            acc[2][i] = accZ.get(i);
                             */
                        }
                        checkForGesture(accM);
                        previousTime = currentTimeSeconds;
                    }

                    sensorMsg.setText(resultStr);
                }
            }

            @Override
            public void onError(MdsException error) {
                //Log.e(LOG_TAG, "subscription onError(): ", error);
                unsubscribe();
            }
        });
    }

    private void checkForGesture(float[] acc) {
        clockwiseDistance = getAvgDistance(accTemplate1, acc);
        anticlockwiseDistance = getAvgDistance(accTemplate2, acc);


        String textToSpeak;
        /*
        if (clockwiseDistance > 2 && anticlockwiseDistance > 2) {
            return;
        }*/
        if (clockwiseDistance < anticlockwiseDistance) {
            textToSpeak = "zigzag!";
        } else {
            textToSpeak = "shake!";
        }
        Log.i(LOG_TAG, "zigzag Distance: " + clockwiseDistance + "\nshake Distance: " + anticlockwiseDistance +"\nDecision: :" + textToSpeak);

        tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public double getAvgDistance(float[] preRecordedData, float[] dataRecorded) {
        double avgDistance = 0;
        //System.out.println(Arrays.toString(dataRecorded[0]));

        DTW dtw = new DTW();

        DTW.Result result = dtw.compute(preRecordedData, dataRecorded);

        //int[][] warpingPath = result.getWarpingPath();
        double distance = result.getDistance();
        System.out.println(distance);

        return distance;

        /*
        for (int i = 1; i < 4; i++) {
            DTW dtw = new DTW();

            DTW.Result result = dtw.compute(getDatafromCSV(fileToCompare, i), dataRecorded[i - 1]);

            int[][] warpingPath = result.getWarpingPath();
            double distance = result.getDistance();
            System.out.println(distance);

            avgDistance += (distance * distance);
        }
        */

        //return avgDistance;
    }

    public float[] getDatafromCSV(String fileName, int dataType) {
        List<Float> accX = new ArrayList<>();
        List<Float> accY = new ArrayList<>();
        List<Float> accZ = new ArrayList<>();

        File file = new File(directory, fileName);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean skipFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (skipFirstLine) {
                    skipFirstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length >= 7) {

                    try {
                        accX.add(Float.parseFloat(data[1]));
                        accY.add(Float.parseFloat(data[2]));
                        accZ.add(Float.parseFloat(data[3]));
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        float[] acc = new float[accX.size()];

        for (int i = 0; i < accX.size(); i++) {
            acc[i] = (float) Math.sqrt(
                    Math.pow(accX.get(i), 2) +
                    Math.pow(accY.get(i), 2) +
                    Math.pow(accZ.get(i), 2)
            );
        }

        return acc;
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

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
        }
    }
}