package com.tanujn45.a11y;

import android.content.Intent;
import android.graphics.Color;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsHeader;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsResponseListener;
import com.movesense.mds.MdsSubscription;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GestureActivity extends AppCompatActivity implements TextToSpeech.OnInitListener, AdapterView.OnItemSelectedListener, SurfaceHolder.Callback {
    private static final String LOG_TAG = GestureActivity.class.getSimpleName();
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String URI_TIME = "suunto://{0}/Time";
    private static final String PATH = "/Meas/IMU9/";
    private static String RATE = "52";
    private static int currentRate = 2;
    private static final String RECORDING = "Recording";
    private static final String RECORD = "Record";
    public static final String FILE_TYPE = ".csv";
    String connectedSerial;
    private String fileNameSave;
    private List<CardData> cardDataList;
    private RecyclerView recyclerView;
    private CardAdapter adapter;
    private MdsSubscription mdsSubscription;
    private TextToSpeech tts;
    private MediaRecorder mMediaRecorder;
    private SurfaceHolder mSurfaceHolder;

    File directory;
    boolean isRecording;
    File file;
    FileOutputStream fos;
    OutputStreamWriter writer;

    private Mds getMds() {
        return MainActivity.mMds;
    }

    private String getConnectedSerial() {
        return MainActivity.connectedSerial;
    }

    ImageButton settingsButton;
    Button gestureButton;
    Button recordButton;
    TextView sensorMsg;
    LineChart accChart, gyroChart, magChart;
    SwitchCompat accSwitch, gyroSwitch, magSwitch;
    Spinner refreshRate;

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
        recordButton = findViewById(R.id.recordWholeButton);
        refreshRate = findViewById(R.id.refreshRate);
        refreshRate.setOnItemSelectedListener(this);
        refreshRate.setSelection(currentRate);

        accChart = findViewById(R.id.imuChartAcc);
        accChart.setData(new LineData());
        accChart.getDescription().setText("Linear Acc");
        accChart.setTouchEnabled(false);
        accChart.setAutoScaleMinMaxEnabled(true);
        accChart.invalidate();

        gyroChart = findViewById(R.id.imuChartGyro);
        gyroChart.setData(new LineData());
        gyroChart.getDescription().setText("Gyroscope");
        gyroChart.setTouchEnabled(false);
        gyroChart.setAutoScaleMinMaxEnabled(true);
        gyroChart.invalidate();

        magChart = findViewById(R.id.imuChartMag);
        magChart.setData(new LineData());
        magChart.getDescription().setText("Magnetometer");
        magChart.setTouchEnabled(false);
        magChart.setAutoScaleMinMaxEnabled(true);
        magChart.invalidate();

        accSwitch = findViewById(R.id.accSwitch);
        accSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    accChart.setVisibility(View.VISIBLE);
                } else {
                    accChart.setVisibility(View.GONE);
                }
            }
        });

        gyroSwitch = findViewById(R.id.gyroSwitch);
        gyroSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    gyroChart.setVisibility(View.VISIBLE);
                } else {
                    gyroChart.setVisibility(View.GONE);
                }
            }
        });

        magSwitch = findViewById(R.id.magSwitch);
        magSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    magChart.setVisibility(View.VISIBLE);
                } else {
                    magChart.setVisibility(View.GONE);
                }
            }
        });

        cardDataList = new ArrayList<>();
        tts = new TextToSpeech(this, this);

        adapter = new CardAdapter(cardDataList);
        recyclerView.setAdapter(adapter);
        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        fileNameSave = "";
        isRecording = false;
        setCurrentTimeToSensor(connectedSerial);

//        try {
//            fetchDataFromFiles();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        subscribeToSensor(connectedSerial);
    }

    private void showFileNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View customLayout = getLayoutInflater().inflate(R.layout.custom_alert_dialog, null);
        builder.setView(customLayout);

        EditText input = customLayout.findViewById(R.id.fileNameEditText);
        Button save = customLayout.findViewById(R.id.saveButton);
        Button cancel = customLayout.findViewById(R.id.cancelButton);

        final AlertDialog alertDialog = builder.create();

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fileName = input.getText().toString();
                if (!fileName.isEmpty()) {
                    if (isFileExists(fileName)) {
                        Toast.makeText(GestureActivity.this, "File name already exists!", Toast.LENGTH_SHORT).show();
                    } else {
                        File newFile = new File(directory, fileName + FILE_TYPE);
                        file.renameTo(newFile);
                        alertDialog.dismiss();
                    }
                } else {
                    Toast.makeText(GestureActivity.this, "Please enter a file name", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                file.delete();
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private boolean isFileExists(String fileName) {
        File file = new File(directory, fileName + ".txt");
        return file.exists();
    }

    public void recordGestureButtonClicked(View view) throws IOException {
        isRecording = !isRecording;
        if (isRecording) {
            recordButton.setText(RECORDING);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
            String currentDateTime = sdf.format(new Date());
            fileNameSave = currentDateTime + FILE_TYPE;
            file = new File(directory, fileNameSave);
            fos = new FileOutputStream(file);
            writer = new OutputStreamWriter(fos);
            writer.append("Timestamp,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,magn_x,magn_y,magn_z").append("\n");
        } else {
            writer.flush();
            writer.close();
            fos.close();
            Toast.makeText(GestureActivity.this, "File saved as " + fileNameSave, Toast.LENGTH_SHORT).show();
            recordButton.setText(RECORD);
        }
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

    private void startRecording() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        mMediaRecorder.setOutputFile(getOutputMediaFile().getPath());
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            isRecording = true;
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (isRecording) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRecording = false;
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void subscribeToSensor(String connectedSerial) {
        if (mdsSubscription != null) {
            unsubscribe();
        }

        /*
        final LineData mLineDataAcc = accChart.getData();

        ILineDataSet xSetAcc = mLineDataAcc.getDataSetByIndex(0);
        ILineDataSet ySetAcc = mLineDataAcc.getDataSetByIndex(1);
        ILineDataSet zSetAcc = mLineDataAcc.getDataSetByIndex(2);

        if (xSetAcc == null) {
            xSetAcc = createSet("Data x", getResources().getColor(android.R.color.holo_red_dark));
            ySetAcc = createSet("Data y", getResources().getColor(android.R.color.holo_green_dark));
            zSetAcc = createSet("Data z", getResources().getColor(android.R.color.holo_blue_dark));
            mLineDataAcc.addDataSet(xSetAcc);
            mLineDataAcc.addDataSet(ySetAcc);
            mLineDataAcc.addDataSet(zSetAcc);
        }

        final LineData mLineDataGyro = gyroChart.getData();

        ILineDataSet xSetGyro = mLineDataGyro.getDataSetByIndex(0);
        ILineDataSet ySetGyro = mLineDataGyro.getDataSetByIndex(1);
        ILineDataSet zSetGyro = mLineDataGyro.getDataSetByIndex(2);

        if (xSetGyro == null) {
            xSetGyro = createSet("Data x", getResources().getColor(android.R.color.holo_red_dark));
            ySetGyro = createSet("Data y", getResources().getColor(android.R.color.holo_green_dark));
            zSetGyro = createSet("Data z", getResources().getColor(android.R.color.holo_blue_dark));
            mLineDataGyro.addDataSet(xSetGyro);
            mLineDataGyro.addDataSet(ySetGyro);
            mLineDataGyro.addDataSet(zSetGyro);
        }

        final LineData mLineDataMag = magChart.getData();

        ILineDataSet xSetMag = mLineDataMag.getDataSetByIndex(0);
        ILineDataSet ySetMag = mLineDataMag.getDataSetByIndex(1);
        ILineDataSet zSetMag = mLineDataMag.getDataSetByIndex(2);

        if (xSetMag == null) {
            xSetMag = createSet("Data x", getResources().getColor(android.R.color.holo_red_dark));
            ySetMag = createSet("Data y", getResources().getColor(android.R.color.holo_green_dark));
            zSetMag = createSet("Data z", getResources().getColor(android.R.color.holo_blue_dark));
            mLineDataMag.addDataSet(xSetMag);
            mLineDataMag.addDataSet(ySetMag);
            mLineDataMag.addDataSet(zSetMag);
        }
        */

        String strContract = "{\"Uri\": \"" + connectedSerial + PATH + RATE + "\"}";

        mdsSubscription = getMds().builder().build(this).subscribe(URI_EVENTLISTENER, strContract, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                ImuModel imuModel = new Gson().fromJson(data, ImuModel.class);

                if (imuModel != null && imuModel.getBody().getArrayAcc().length > 0 && imuModel.getBody().getArrayGyro().length > 0) {
                    for (int i = 0; i < imuModel.getBody().getArrayAcc().length; i++) {
                        String resultStrRecord = String.format(Locale.getDefault(), "%d,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f", (imuModel.getBody().getTimestamp() + i * 20L), imuModel.getBody().getArrayAcc()[i].getX(), imuModel.getBody().getArrayAcc()[i].getY(), imuModel.getBody().getArrayAcc()[i].getZ(), imuModel.getBody().getArrayGyro()[i].getX(), imuModel.getBody().getArrayGyro()[i].getY(), imuModel.getBody().getArrayGyro()[i].getZ(), imuModel.getBody().getArrayMag()[i].getX(), imuModel.getBody().getArrayMag()[i].getY(), imuModel.getBody().getArrayMag()[i].getZ());
                        sensorMsg.setText(resultStrRecord);
                        if (isRecording) {
                            try {
                                writer.append(resultStrRecord).append("\n");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        /*
                        mLineDataAcc.addEntry(new Entry((imuModel.getBody().getTimestamp() + i * 20L) / 100, (float) imuModel.getBody().getArrayAcc()[i].getX()), 0);
                        mLineDataAcc.addEntry(new Entry((imuModel.getBody().getTimestamp() + i * 20L) / 100, (float) imuModel.getBody().getArrayAcc()[i].getY()), 1);
                        mLineDataAcc.addEntry(new Entry((imuModel.getBody().getTimestamp() + i * 20L) / 100, (float) imuModel.getBody().getArrayAcc()[i].getZ()), 2);
                        mLineDataAcc.notifyDataChanged();

                        accChart.notifyDataSetChanged();
                        accChart.setVisibleXRangeMaximum(50);
                        accChart.moveViewToX((imuModel.getBody().getTimestamp() + i * 20L) / 100);

                        mLineDataGyro.addEntry(new Entry((imuModel.getBody().getTimestamp() + i * 20L) / 100, (float) imuModel.getBody().getArrayGyro()[i].getX()), 0);
                        mLineDataGyro.addEntry(new Entry((imuModel.getBody().getTimestamp() + i * 20L) / 100, (float) imuModel.getBody().getArrayGyro()[i].getY()), 1);
                        mLineDataGyro.addEntry(new Entry((imuModel.getBody().getTimestamp() + i * 20L) / 100, (float) imuModel.getBody().getArrayGyro()[i].getZ()), 2);
                        mLineDataGyro.notifyDataChanged();

                        gyroChart.notifyDataSetChanged();
                        gyroChart.setVisibleXRangeMaximum(50);
                        gyroChart.moveViewToX((imuModel.getBody().getTimestamp() + i * 20L) / 100);

                        if (imuModel.getBody().getArrayMag() != null) {
                            mLineDataMag.addEntry(new Entry((imuModel.getBody().getTimestamp() + i * 20L) / 100, (float) imuModel.getBody().getArrayMag()[i].getX()), 0);
                            mLineDataMag.addEntry(new Entry((imuModel.getBody().getTimestamp() + i * 20L) / 100, (float) imuModel.getBody().getArrayMag()[i].getY()), 1);
                            mLineDataMag.addEntry(new Entry((imuModel.getBody().getTimestamp() + i * 20L) / 100, (float) imuModel.getBody().getArrayMag()[i].getZ()), 2);
                            mLineDataMag.notifyDataChanged();

                            magChart.notifyDataSetChanged();
                            magChart.setVisibleXRangeMaximum(50);
                            magChart.moveViewToX((imuModel.getBody().getTimestamp() + i * 20L) / 100);
                        }
                        */
                    }
                }
            }

            @Override
            public void onError(MdsException error) {
                //Log.e(LOG_TAG, "subscription onError(): ", error);
                unsubscribe();
            }
        });
    }

    private LineDataSet createSet(String name, int color) {
        LineDataSet set = new LineDataSet(null, name);
        set.setLineWidth(2.5f);
        set.setColor(color);
        set.setDrawCircleHole(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setValueTextSize(0f);

        return set;
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

    private void setCurrentTimeToSensor(String serial) {
        String timeUri = MessageFormat.format(URI_TIME, serial);
        String payload = "{\"value\":" + (new Date().getTime() * 1000) + "}";
        getMds().put(timeUri, payload, new MdsResponseListener() {
            @Override
            public void onSuccess(String data, MdsHeader header) {
                Log.i(LOG_TAG, "PUT /Time successful: " + data);
            }

            @Override
            public void onError(MdsException e) {
                Log.e(LOG_TAG, "PUT /Time returned error: " + e);
            }
        });

    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (!isRecording) {
            RATE = parent.getItemAtPosition(position).toString();
            //System.out.println(RATE);
            unsubscribe();
            currentRate = position;
            subscribeToSensor(connectedSerial);
        } else {
            refreshRate.setSelection(currentRate);
            Toast.makeText(GestureActivity.this, "Cannot change refresh rate while recording!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

    }
}