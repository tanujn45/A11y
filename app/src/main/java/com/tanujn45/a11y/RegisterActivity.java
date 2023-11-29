package com.tanujn45.a11y;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class RegisterActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String LOG_TAG = RegisterActivity.class.getSimpleName();
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String URI_MEAS_ACC_13 = "/Meas/Acc/13";
    public static final String SERIAL = "serial";
    private static final String RECORDING = "Recording";
    private static final String RECORD = "Record";

    String connectedSerial;
    int gestureNumber;
    private MdsSubscription mdsSubscription;
    File directory, file;
    FileOutputStream fos;
    OutputStreamWriter writer;
    boolean isRecording = false;
    private TextToSpeech tts;

    private Mds getMds() {
        return MainActivity.mMds;
    }

    EditText nameEntry;
    EditText ttsEntry;
    EditText descriptionEntry;
    Button recordButton;
    ImageButton playButton;
    TextView recordNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Intent intent = getIntent();
        connectedSerial = intent.getStringExtra(SERIAL);
        gestureNumber = 0;
        tts = new TextToSpeech(this, this);

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        file = new File(directory, "temp.txt");
        try {
            fos = new FileOutputStream(file);
            writer = new OutputStreamWriter(fos);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        recordButton = findViewById(R.id.recordButton);
        nameEntry = findViewById(R.id.nameEntry);
        playButton = findViewById(R.id.playButton);
        ttsEntry = findViewById(R.id.ttsEntry);
        descriptionEntry = findViewById(R.id.descriptionEntry);
        recordNumber = findViewById(R.id.recordNumber);

        recordButton.setText(RECORD);
    }

    private void subscribeToSensor(String connectedSerial) {
        if (mdsSubscription != null) {
            unsubscribe();
        }

        String strContract = "{\"Uri\": \"" + connectedSerial + URI_MEAS_ACC_13 + "\"}";
        Log.d(LOG_TAG, strContract);

        mdsSubscription = getMds().builder().build(this).subscribe(URI_EVENTLISTENER, strContract, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                Log.d(LOG_TAG, "onNotification(): " + data);


                AccDataResponse accResponse = new Gson().fromJson(data, AccDataResponse.class);
                if (accResponse != null && accResponse.body.array.length > 0) {

                    @SuppressLint("DefaultLocale") String accStr = String.format("%.02f, %.02f, %.02f", accResponse.body.array[0].x, accResponse.body.array[0].y, accResponse.body.array[0].z);
                    try {
                        writer.append(accStr).append("\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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

    public void recordGestureButtonClicked(View view) throws IOException {
        isRecording = !isRecording;
        if (isRecording) {
            recordButton.setText(RECORDING);
            writer.append("########## Gesture ").append(String.valueOf(gestureNumber + 1)).append(" ##########\n");
            subscribeToSensor(connectedSerial);
        } else {
            unsubscribe();
            gestureNumber++;
            recordNumber.setText("Number of records: " + gestureNumber);
            recordButton.setText(RECORD);
        }
    }

    public void saveButtonClicked(View view) throws IOException {
        String name = nameEntry.getText().toString();
        if (TextUtils.isEmpty(name)) {
            nameEntry.setError("This field cannot be empty!");
            return;
        }

        String ttsStr = ttsEntry.getText().toString();
        if (TextUtils.isEmpty(ttsStr)) {
            ttsEntry.setError("This field cannot be empty!");
            return;
        }

        String description = descriptionEntry.getText().toString();
        if (TextUtils.isEmpty(description)) {
            descriptionEntry.setError("This field cannot be empty!");
            return;
        }

        if (gestureNumber <= 1) {
            Toast.makeText(getApplicationContext(), "Please record gestures!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = name.replaceAll("[^a-zA-Z]", "").replaceAll("\\s", "") + ".txt";

        createDataFile(fileName);
        createInfoFile(fileName, name, ttsStr, description, gestureNumber);

        Intent intent = new Intent(RegisterActivity.this, GestureActivity.class);
        intent.putExtra(SERIAL, connectedSerial);
        startActivity(intent);
    }

    public void createDataFile(String fileName) throws IOException {
        String dataFileName = "data_" + fileName;
        File rfile = new File(directory, dataFileName);
        writer.flush();
        writer.close();
        fos.close();
        if (file.exists()) {
            file.renameTo(rfile);
        }

        Log.d(LOG_TAG, "File path: " + rfile.getAbsolutePath());
    }

    public void createInfoFile(String fileName, String name, String ttsText, String description, int nGestures) throws IOException {
        String infoFileName = "info_" + fileName;
        File iFile = new File(directory, infoFileName);

        FileOutputStream iFos = new FileOutputStream(iFile);
        OutputStreamWriter iWriter = new OutputStreamWriter(iFos);

        iWriter.append("########## Name ##########\n");
        iWriter.append(name);
        iWriter.append("\n########## Text To Speak ##########\n");
        iWriter.append(ttsText);
        iWriter.append("\n########## Description ##########\n");
        iWriter.append(description);
        iWriter.append("\n########## Number of Gestures ##########\n");
        iWriter.append(String.valueOf(nGestures));

        iWriter.flush();
        iWriter.close();
        iFos.close();

        Log.d(LOG_TAG, "File path: " + iFile.getAbsolutePath());
    }

    public void playButtonClicked(View view) {
        String ttsStr = ttsEntry.getText().toString();
        if (TextUtils.isEmpty(ttsStr)) {
            ttsEntry.setError("Add text to play!");
            return;
        }
        tts.speak(ttsStr, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
        }
    }
}
