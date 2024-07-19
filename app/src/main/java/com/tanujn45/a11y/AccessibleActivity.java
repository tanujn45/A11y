package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;
import com.tanujn45.a11y.KMeans.KMeans;

import java.util.ArrayList;
import java.util.Locale;

import weka.core.Attribute;


//Todo: Fix and optimize AAC activity
public class AccessibleActivity extends AppCompatActivity implements View.OnClickListener, AccItem.OnItemRemovedListener {
    ArrayList<AccItem> accItems = new ArrayList<>();
    LinearLayout scrollableLayout;
    private TextToSpeech textToSpeech;
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String PATH = "/Meas/IMU6/";
    private static final String RATE = "52";
    private MdsSubscription mdsSubscription;
    private String connectedSerial;
    Spinner modelSpinner;
    GridLayout gridLayout;
    SwitchCompat toggleRecognition;
    KMeans kMeans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessible);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        kMeans = new KMeans(this);

        modelSpinner = findViewById(R.id.modelSpinner);
        modelSpinner.setEnabled(false);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, kMeans.getModelNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        modelSpinner.setOnItemSelectedListener(itemSelectedListener);

        gridLayout = findViewById(R.id.gridLayout);

        toggleRecognition = findViewById(R.id.enableRecognitionSwitch);

        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            gridLayout.getChildAt(i).setOnClickListener(this);
        }

        scrollableLayout = findViewById(R.id.scrollableLayout);

        connectedSerial = getConnectedSerial();

        toggleRecognition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                modelSpinner.setEnabled(true);
                subscribeToSensor(connectedSerial);
            } else {
                modelSpinner.setEnabled(false);
                unsubscribe();
            }
        });
    }

    private String getConnectedSerial() {
        return getIntent().getStringExtra("serial");
    }

    private ArrayList<Attribute> createAttributes() {
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("timestamp"));
        attributes.add(new Attribute("acc_x"));
        attributes.add(new Attribute("acc_y"));
        attributes.add(new Attribute("acc_z"));
        attributes.add(new Attribute("gyro_x"));
        attributes.add(new Attribute("gyro_y"));
        attributes.add(new Attribute("gyro_z"));
        attributes.add(new Attribute("magn_x"));
        attributes.add(new Attribute("magn_y"));
        attributes.add(new Attribute("magn_z"));
        attributes.add(new Attribute("acc_diff_x"));
        attributes.add(new Attribute("acc_diff_y"));
        attributes.add(new Attribute("acc_diff_z"));
        attributes.add(new Attribute("acc_ma_x"));
        attributes.add(new Attribute("acc_ma_y"));
        attributes.add(new Attribute("acc_ma_z"));
        return attributes;
    }


    /**
     * Listener for the model spinner
     */
    private AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            unsubscribe();
            String selectedModel = modelSpinner.getSelectedItem().toString();

            kMeans.setModel(selectedModel);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parentView) {
        }
    };

    private void subscribeToSensor(String connectedSerial) {
        if (mdsSubscription != null) {
            unsubscribe();
        }

        String strContract = "{\"Uri\": \"" + connectedSerial + PATH + RATE + "\"}";

        mdsSubscription = this.getMds().builder().build(this).subscribe(URI_EVENTLISTENER, strContract, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                ImuModel imuModel = new Gson().fromJson(data, ImuModel.class);

                if (imuModel == null) {
                    return;
                }

                ImuModel.ArrayAcc[] arrAcc = imuModel.getBody().getArrayAcc();
                ImuModel.ArrayGyro[] arrGyro = imuModel.getBody().getArrayGyro();
                long timestamp = imuModel.getBody().getTimestamp();

                if (arrAcc.length == 0 || arrGyro.length == 0) {
                    return;
                }

                for (int i = 0; i < arrAcc.length; i++) {
                    String sensorMsgStr =
                            "x: " +
                                    Math.round(arrAcc[i].getX() * 100) / 100.0 +
                                    "  y: " +
                                    Math.round(arrAcc[i].getY() * 100) / 100.0 +
                                    "  z: " +
                                    Math.round(arrAcc[i].getZ() * 100) / 100.0;

                    kMeans.performKMeans(
                            timestamp,
                            arrAcc[i].getX(),
                            arrAcc[i].getY(),
                            arrAcc[i].getZ(),
                            arrGyro[i].getX(),
                            arrGyro[i].getY(),
                            arrGyro[i].getZ()
                    );
                }
            }

            @Override
            public void onError(MdsException error) {
                unsubscribe();
            }
        });
    }


    /**
     * Unsubscribe from the sensor
     */
    private void unsubscribe() {
        if (mdsSubscription != null) {
            mdsSubscription.unsubscribe();
            mdsSubscription = null;
        }
    }


    /**
     * Get the MDS instance
     *
     * @return MDS instance
     */
    private Mds getMds() {
        return MainActivity.mMds;
    }


    @Override
    public void onClick(View v) {
        LinearLayout layout = (LinearLayout) v;
        TextView textView1 = (TextView) layout.getChildAt(0);
        TextView textView2 = (TextView) layout.getChildAt(1);
        String text1 = textView1.getText().toString();
        String text2 = textView2.getText().toString();

        AccItem accItem = new AccItem(this);
        accItem.setOnItemRemovedListener(this);
        accItem.setText1(text1);
        accItem.setText2(text2);
        scrollableLayout.addView(accItem);
        accItems.add(accItem);
    }


    public void playButtonClicked(View view) {
        speakText();

        scrollableLayout.removeAllViews();
        accItems.clear();
    }

    private void speakText() {
        StringBuilder text = new StringBuilder();
        for (AccItem accItem : accItems) {
            text.append(accItem.getText2()).append(" ");
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }


    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();

        unsubscribe();
    }


    @Override
    public void onItemRemoved(AccItem accItem) {
        accItems.remove(accItem);
    }

    public void editDialogButtonClicked(View view) {
        toggleRecognition.setChecked(false);
        Intent intent = new Intent(AccessibleActivity.this, DialogActivity.class);
        startActivity(intent);
        finish();
    }
}