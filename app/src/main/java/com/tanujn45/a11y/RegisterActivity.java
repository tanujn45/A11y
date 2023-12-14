package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RegisterActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String LOG_TAG = RegisterActivity.class.getSimpleName();
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    public static final String FILE_TYPE = ".csv";
    private static final String PATH = "/Meas/IMU6/";
    private static final String RATE = "104";
    private static final String RECORDING = "Recording";
    private static final String RECORD = "Record";

    String connectedSerial;
    int gestureNumber;
    long prevUpdateTimestamp;
    private MdsSubscription mdsSubscription;
    File directory, file;
    FileOutputStream fos;
    OutputStreamWriter writer;
    boolean isRecording = false;
    private TextToSpeech tts;
    boolean isSetRecordingMessage;

    private Mds getMds() {
        return MainActivity.mMds;
    }

    private String getConnectedSerial() {
        return MainActivity.connectedSerial;
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

        connectedSerial = getConnectedSerial();

        gestureNumber = 0;
        prevUpdateTimestamp = 0;

        tts = new TextToSpeech(this, this);

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        recordButton = findViewById(R.id.recordButton);
        nameEntry = findViewById(R.id.nameEntry);
        playButton = findViewById(R.id.playButton);
        ttsEntry = findViewById(R.id.ttsEntry);
        descriptionEntry = findViewById(R.id.descriptionEntry);
        recordNumber = findViewById(R.id.recordNumber);

        recordButton.setText(RECORD);
    }

    private void subscribeToSensor(String connectedSerial) throws IOException {
        if (mdsSubscription != null) {
            unsubscribe();
        }

        String strContract = "{\"Uri\": \"" + connectedSerial + PATH + RATE + "\"}";
        Log.d(LOG_TAG, strContract);
        writer.append("Timestamp,AccX,AccY,AccZ,GyroX,GyroY,GyroZ\n");
        isSetRecordingMessage = true;

        mdsSubscription = getMds().builder().build(this).subscribe(URI_EVENTLISTENER, strContract, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                Log.d(LOG_TAG, "onNotification(): " + data);

                ImuModel imuModel = new Gson().fromJson(data, ImuModel.class);
                if(isSetRecordingMessage) {
                    recordButton.setText(RECORDING);
                    isSetRecordingMessage = false;
                }

                if (imuModel != null && imuModel.getBody().getArrayAcc().length > 0 && imuModel.getBody().getArrayGyro().length > 0) {

                    if (prevUpdateTimestamp == 0) {
                        prevUpdateTimestamp = imuModel.getBody().getTimestamp();
                    }

                    String resultStr = String.format(Locale.getDefault(), "%.6f,%.6f,%.6f,%.6f,%.6f,%.6f", imuModel.getBody().getArrayAcc()[0].getX(), imuModel.getBody().getArrayAcc()[0].getY(), imuModel.getBody().getArrayAcc()[0].getZ(), imuModel.getBody().getArrayGyro()[0].getX(), imuModel.getBody().getArrayGyro()[0].getY(), imuModel.getBody().getArrayGyro()[0].getZ());


                    Date date = new Date((imuModel.getBody().getTimestamp() - prevUpdateTimestamp) * 1000);
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String formattedDate = sdf.format(date);

                    Log.d(LOG_TAG, "Timestamp value: " + (imuModel.getBody().getTimestamp() - prevUpdateTimestamp));
                    resultStr = formattedDate + "," + resultStr;

                    try {
                        writer.append(resultStr).append("\n");
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
            String fileName = "temp_" + gestureNumber + FILE_TYPE;
            file = new File(directory, fileName);
            fos = new FileOutputStream(file);
            writer = new OutputStreamWriter(fos);

            // writer.append("########## Gesture ").append(String.valueOf(gestureNumber + 1)).append(" ##########\n");
            subscribeToSensor(connectedSerial);
        } else {
            unsubscribe();
            isSetRecordingMessage = false;
            writer.flush();
            writer.close();
            fos.close();
            gestureNumber++;
            recordNumber.setText("Number of records: " + gestureNumber);
            recordButton.setText(RECORD);
        }
    }

    public void createDataFile(String fileName) throws IOException {
        fileRename(fileName);
    }

    public void createInfoFile(String fileName, String name, String ttsText, String description, int nGestures) throws IOException {
        String infoFileName = "info_" + fileName + ".txt";
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

    public void fileRename(String newName) {
        newName = "gesture_" + newName;
        File[] files = directory.listFiles();
        String currentName = "temp";

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith(currentName)) {
                    String oldFileName = file.getName();
                    String newFileName = newName + oldFileName.substring(currentName.length());

                    File newFile = new File(directory, newFileName);
                    boolean isRenamed = file.renameTo(newFile);

                    if (isRenamed) {
                        System.out.println("File " + oldFileName + " renamed to " + newFileName);
                    } else {
                        System.out.println("Failed to rename " + oldFileName);
                    }
                }
            }
        }
    }

    public void fileDelete(String prefix) {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith(prefix)) {
                    file.delete();
                }
            }
        }
    }

    public void playButtonClicked(View view) {
        String ttsStr = ttsEntry.getText().toString();
        if (TextUtils.isEmpty(ttsStr)) {
            ttsEntry.setError("Add text to play!");
            return;
        }
        tts.speak(ttsStr, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void cancelButtonClicked(View view) throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
        if (fos != null)
            fos.close();
        fileDelete("temp");

        Intent intent = new Intent(RegisterActivity.this, GestureActivity.class);
        startActivity(intent);
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

        String fileName = name.replaceAll("[^a-zA-Z]", "").replaceAll("\\s", "");

        createDataFile(fileName);
        createInfoFile(fileName, name, ttsStr, description, gestureNumber);

        Intent intent = new Intent(RegisterActivity.this, GestureActivity.class);
        startActivity(intent);
    }

    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteButtonClicked(View view) throws IOException {
        gestureNumber = 0;
        recordNumber.setText("Number of records: " + gestureNumber);
        fos.getChannel().truncate(0);

    }
}
