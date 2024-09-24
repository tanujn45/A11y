package com.tanujn45.a11y;

import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;
import com.tanujn45.a11y.CSVEditor.CSVFile;
import com.tanujn45.a11y.KMeans.KMeans;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


//Todo: Fix and optimize AAC activity
public class AccessibleActivity extends AppCompatActivity implements CardAdapter.OnItemClickListener, AccItem.OnItemRemovedListener {
    ArrayList<AccItem> accItems = new ArrayList<>();
    LinearLayout scrollableLayout;
    private TextToSpeech textToSpeech;
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String PATH = "/Meas/IMU6/";
    private static final String RATE = "52";
    private MdsSubscription mdsSubscription;
    private String connectedSerial;
    Spinner modelSpinner;
    private List<CardData> cardDataList;
    RecyclerView recyclerView;
    SwitchCompat toggleRecognition;
    KMeans kMeans;
    CSVFile masterFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessible);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File master = new File(directory, "master.csv");
        try {
            masterFile = new CSVFile(master);
        } catch (Exception e) {
            e.printStackTrace();
        }

        kMeans = new KMeans(this);

        modelSpinner = findViewById(R.id.modelSpinner);
        recyclerView = findViewById(R.id.recyclerView);
        toggleRecognition = findViewById(R.id.enableRecognitionSwitch);
        scrollableLayout = findViewById(R.id.scrollableLayout);

        connectedSerial = getConnectedSerial();
        cardDataList = new ArrayList<>();

        initModelSpinner();
        initToggleRecognition();
        populateGridLayout();


//        for (int i = 0; i < gridLayout.getChildCount(); i++) {
//            gridLayout.getChildAt(i).setOnClickListener(this);
//        }
    }

    private void initToggleRecognition() {
        toggleRecognition.setChecked(false);
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

    private void initModelSpinner() {
        modelSpinner = findViewById(R.id.modelSpinner);
        modelSpinner.setEnabled(false);
        List<String> modelNames = kMeans.getModelNames();

        // Remove the temporary cache model from the spinner
        int i;
        boolean found = false;
        for (i = 0; i < modelNames.size(); i++) {
            if (modelNames.get(i).startsWith("tempModelCacheA11y")) {
                found = true;
                break;
            }
        }
        if (found) {
            modelNames.remove(i);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                unsubscribe();
                String selectedModel = modelSpinner.getSelectedItem().toString();
                kMeans.setModel(selectedModel);
                subscribeToSensor(connectedSerial);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    private void populateGridLayout() {
        List<String[]> data = masterFile.getCSVData();
        boolean firstRow = true;
        for (String[] row : data) {
            if (firstRow) {
                firstRow = false;
                continue;
            }
            if (Objects.equals(row[0], "Rest")) {
                continue;
            }
            CardData cardData = new CardData();
            cardData.setName(row[0]);
            cardData.setTextToSpeak(row[1]);
            cardDataList.add(cardData);
        }

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        CardAdapter adapter = new CardAdapter(cardDataList, (CardAdapter.OnItemClickListener) this);
        recyclerView.setAdapter(adapter);
    }

    private String getConnectedSerial() {
        return getIntent().getStringExtra("serial");
    }

    private void subscribeToSensor(String connectedSerial) {
        if (mdsSubscription != null) {
            unsubscribe();
        }

        connectedSerial = MainActivity.connectedSerial;

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

                    String res = kMeans.performKMeans(timestamp, arrAcc[i].getX(), arrAcc[i].getY(), arrAcc[i].getZ(), arrGyro[i].getX(), arrGyro[i].getY(), arrGyro[i].getZ());
                    if (res != null) {
                        System.out.println(res);
                    }

                    // System.out.println(sensorMsgStr);

                    if (res != null) {
                        addAccItem(res);
                        System.out.println(sensorMsgStr + " " + res);
                    }
                }
            }

            @Override
            public void onError(MdsException error) {
                unsubscribe();
            }
        });
    }

    private void addAccItem(String res) {
        if (res.equals("Rest")) {
            return;
        }
        AccItem accItem = new AccItem(this);
        accItem.setOnItemRemovedListener(this);

        accItem.setText1(res);
        for (int i = 0; i < cardDataList.size(); i++) {
//            System.out.println(cardDataList.get(i).getName() + " test " + res);
            if (cardDataList.get(i).getName().toLowerCase().equals(res.toLowerCase())) {
                accItem.setText2(cardDataList.get(i).getTextToSpeak());
                textToSpeech.speak(cardDataList.get(i).getTextToSpeak(), TextToSpeech.QUEUE_FLUSH, null, null);
                break;
            }
        }

        scrollableLayout.addView(accItem);
        accItems.add(accItem);
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

    @Override
    public void onItemClick(View view, int position) {
        CardData cardData = cardDataList.get(position);
        String name = cardData.getName();
        String textToSpeak = cardData.getTextToSpeak();

        AccItem accItem = new AccItem(this);
        accItem.setOnItemRemovedListener(this);
        accItem.setText1(name);
        accItem.setText2(textToSpeak);

        LinearLayout scrollableLayout = findViewById(R.id.scrollableLayout);
        scrollableLayout.addView(accItem);
        accItems.add(accItem);
    }
}