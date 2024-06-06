package com.tanujn45.a11y;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

public class AccessibleActivity extends AppCompatActivity implements View.OnClickListener, AccItem.OnItemRemovedListener {
    ArrayList<AccItem> accItems = new ArrayList<>();
    LinearLayout scrollableLayout;
    private TextToSpeech textToSpeech;
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String URI_TIME = "suunto://{0}/Time";
    private static final String PATH = "/Meas/IMU9/";
    private static final String RATE = "52";
    private MdsSubscription mdsSubscription;
    private static String trimmedCsvFolderPath;
    private static String modelCsvFolderPath;
    private String connectedSerial;
    private File directory;
    private String[] prefixes;
    private double[] weights;
    private String[] csvFiles;
    private int nClusters = 20;
    private boolean globalIsChecked = false;
    Spinner modelSpinner;
    List<String> gestures, dialog;
    ArrayList<SimpleKMeans> kmeans;
    Instances combinedDataGestures;
    Instances sensorInstances;
    GridLayout gridLayout;
    SwitchCompat toggleRecognition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessible);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        trimmedCsvFolderPath = directory + "/trimmedData";
        modelCsvFolderPath = directory + "/models";

        List<String> trimmedFileNames = getCSVFileNames(trimmedCsvFolderPath);
        List<String> modelFileNames = getCSVFileNames(modelCsvFolderPath);
        if (trimmedFileNames.isEmpty() || modelFileNames.isEmpty()) {
            // Navigate to a different screen because no files are found
            Intent intent = new Intent(AccessibleActivity.this, RecordActivity.class);
            startActivity(intent);
            finish();
        }

        modelSpinner = findViewById(R.id.modelSpinner);
        modelSpinner.setEnabled(false);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getCSVFileNames(modelCsvFolderPath));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);
        modelSpinner.setOnItemSelectedListener(itemSelectedListener);

        gridLayout = findViewById(R.id.gridLayout);

        toggleRecognition = findViewById(R.id.enableRecognitionSwitch);

        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            gridLayout.getChildAt(i).setOnClickListener(this);
        }

        scrollableLayout = findViewById(R.id.scrollableLayout);
        sensorInstances = new Instances("sensorData", createAttributes(), 0);

        connectedSerial = getConnectedSerial();

        toggleRecognition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                modelSpinner.setEnabled(true);
                globalIsChecked = true;
                subscribeToSensor(connectedSerial);
            } else {
                modelSpinner.setEnabled(false);
                globalIsChecked = false;
                unsubscribe();
            }
        });

        setDialog();

        readAndCreateViewsFromFile();
    }

    private void setDialog() {
        List<String> fileNames = getCSVFileNames(trimmedCsvFolderPath);
        Set<String> fileNameSet = new HashSet<>();
        for (String fileName : fileNames) {
            String trimmedName = fileName.replaceAll("_", " ").replaceAll("[0-9]", "").trim();
            fileNameSet.add(trimmedName);
        }


        File file = new File(directory + "/dialogs.txt");
        if (file.exists() && file.length() > 0) {
            gestures = new ArrayList<>();
            dialog = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        gestures.add(parts[0]);
                        dialog.add(parts[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
            }

        } else {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(fos);
                for (String fileName : fileNameSet) {
                    writer.append(fileName).append(":").append(fileName).append("\n");
                }

                writer.flush();
                writer.close();
                fos.close();
//                Toast.makeText(this, "Dialogs file created", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readAndCreateViewsFromFile() {
        File file = new File(directory, "/dialogs.txt");
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String mainLabel = parts[0];
                    if (mainLabel.startsWith("rest")) {
                        continue;
                    }
                    String smallerLabel = parts[1];

                    // Create LinearLayout for the block
                    LinearLayout linearLayout = new LinearLayout(this);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setGravity(Gravity.CENTER);
                    linearLayout.setPadding(10, 10, 10, 10);
                    linearLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corner));

                    GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
                    layoutParams.width = 0;
                    layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 140, getResources().getDisplayMetrics());
                    layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                    linearLayout.setLayoutParams(layoutParams);
                    linearLayout.setOnClickListener(this);

                    // Create TextView for main label
                    TextView mainTextView = new TextView(this);
                    mainTextView.setText(mainLabel);
                    mainTextView.setTextSize(22f);
                    mainTextView.setTextColor(Color.WHITE);
                    mainTextView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    mainTextView.setGravity(Gravity.CENTER);

                    // Create TextView for smaller label
                    TextView smallerTextView = new TextView(this);
                    smallerTextView.setText(smallerLabel);
                    smallerTextView.setTextSize(13f);
                    smallerTextView.setTextColor(Color.WHITE);
                    LinearLayout.LayoutParams smallerTextParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    smallerTextParams.topMargin = 5;
                    smallerTextView.setLayoutParams(smallerTextParams);
                    smallerTextView.setGravity(Gravity.CENTER);
                    smallerTextView.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);

                    // Adding TextViews to LinearLayout
                    linearLayout.addView(mainTextView);
                    linearLayout.addView(smallerTextView);

                    // Adding LinearLayout to GridLayout
                    gridLayout.addView(linearLayout);
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create attributes for the sensor data
     *
     * @return List of attributes
     */
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
     * Get all the ML models in the models folder
     *
     * @param folderPath Path to the models folder
     * @return List of model names
     */
    private List<String> getCSVFileNames(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));
        List<String> csvFiles = new ArrayList<>();
        for (File file : files) {
            csvFiles.add(file.getName().replace(".csv", ""));
        }
        return csvFiles;
    }


    /**
     * Listener for the model spinner
     */
    private AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            unsubscribe();
            String selectedModel = modelSpinner.getSelectedItem().toString();
            String modelFilePath = modelCsvFolderPath + "/" + selectedModel + ".csv";
            try {
                CSVLoader loader = new CSVLoader();
                loader.setSource(new File(modelFilePath));
                Instances data = loader.getDataSet();

                prefixes = new String[data.numInstances()];
                weights = new double[data.numInstances()];

                for (int i = 0; i < data.numInstances(); i++) {
                    prefixes[i] = data.instance(i).stringValue(0);
                    //prefixes[i] = "acc";
                    weights[i] = data.instance(i).value(1);
                }

                getKMeans();
                if (globalIsChecked) {
                    subscribeToSensor(connectedSerial);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parentView) {
        }
    };


    /**
     * Get KMeans clusters for each prefix
     *
     * @throws Exception if can't read the data
     */
    public void getKMeans() throws Exception {
        combinedDataGestures = combineData();

        kmeans = new ArrayList<>();

        for (int i = combinedDataGestures.numInstances() - 1; i >= 0; i--) {
            if (hasMissingValues(combinedDataGestures.instance(i))) {
                combinedDataGestures.delete(i);
            }
        }

        for (String prefix : prefixes) {
            List<String> columns = new ArrayList<>();

            columns.add(prefix + "_x");
            columns.add(prefix + "_y");
            columns.add(prefix + "_z");

            Instances kmeansColumns = getColumnData(combinedDataGestures, columns);


            SimpleKMeans kmeansModel = new SimpleKMeans();
            kmeansModel.setNumClusters(nClusters);
            kmeansModel.buildClusterer(kmeansColumns);

            Attribute clusterIdAttr = new Attribute("cluster_id_" + prefix);
            combinedDataGestures.insertAttributeAt(clusterIdAttr, combinedDataGestures.numAttributes());

            // Assign cluster IDs to instances in combinedDataGestures
            for (int i = 0; i < kmeansColumns.numInstances(); i++) {
                Instance instance = kmeansColumns.instance(i);
                int clusterId = kmeansModel.clusterInstance(instance);
                combinedDataGestures.instance(i).setValue(combinedDataGestures.numAttributes() - 1, clusterId);
            }

            kmeans.add(kmeansModel);
        }
    }


    /**
     * Combine all the data from the trimmedData folder
     *
     * @return Combined data
     * @throws Exception if can't read the data
     */
    public Instances combineData() throws Exception {
        List<File> csvFiles = getCSVFiles(trimmedCsvFolderPath);

        Instances combinedData = null;
        for (File file : csvFiles) {
            Instances data = loadData(file);
            assert data != null;
            if (combinedData == null) {
                combinedData = new Instances(data);
            } else {
                combinedData.addAll(data);
            }
        }

        return combinedData;
    }


    /**
     * Load data from a CSV file
     *
     * @param file File to load data from
     * @return Data from the file
     */
    private Instances loadData(File file) {
        try {
            CSVLoader loader = new CSVLoader();
            loader.setSource(file);
            return loader.getDataSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Get all the CSV files in a folder
     *
     * @param folderPath Path to the folder
     * @return List of CSV files
     */
    private List<File> getCSVFiles(String folderPath) {
        List<File> csvFiles = new ArrayList<>();
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".csv")) {
                    csvFiles.add(file);
                }
            }
        }
        return csvFiles;
    }


    /**
     * Check if an instance has missing values
     *
     * @param instance Instance to check
     * @return True if the instance has missing values, false otherwise
     */
    private boolean hasMissingValues(Instance instance) {
        for (int i = 0; i < instance.numAttributes(); i++) {
            if (instance.isMissing(i)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Get the data for the specified columns
     *
     * @param data    Data to get columns from
     * @param columns Columns to get
     * @return Data with only the specified columns
     */
    private Instances getColumnData(Instances data, List<String> columns) {

        Instances newData = new Instances(data);
        newData.delete();

        // Create a mapping of attribute names to indices for faster lookup
        Map<String, Integer> attributeIndices = new HashMap<>();
        for (int i = 0; i < data.numAttributes(); i++) {
            Attribute attribute = data.attribute(i);
            attributeIndices.put(attribute.name(), i);
        }

        // Remove attributes not in the specified list
        for (int j = newData.numAttributes() - 1; j >= 0; j--) {
            Attribute attribute = newData.attribute(j);
            if (attribute != null && !columns.contains(attribute.name())) {
                newData.deleteAttributeAt(j);
            }
        }

        // Copy instances
        for (int j = 0; j < data.size(); j++) {
            Instance instance = data.get(j);
            Instance newDataInstance = new DenseInstance(newData.numAttributes());
            for (int k = 0; k < newData.numAttributes(); k++) {
                Attribute attr = newData.attribute(k);
                Integer indexInUnknownData = attributeIndices.get(attr.name());
                if (indexInUnknownData != null) {
                    newDataInstance.setValue(attr, instance.value(indexInUnknownData));
                }
            }
            newData.add(newDataInstance);
        }

        return newData;
    }


    /**
     * Get the serial number of the connected device
     *
     * @return Serial number of the connected device
     */
    private String getConnectedSerial() {
        return MainActivity.connectedSerial;
    }


    /**
     * Subscribe to the sensor
     *
     * @param connectedSerial Serial number of the connected device
     */
    private void subscribeToSensor(String connectedSerial) {
        if (mdsSubscription != null) {
            unsubscribe();
        }

        String strContract = "{\"Uri\": \"" + connectedSerial + PATH + RATE + "\"}";

        mdsSubscription = getMds().builder().build(this).subscribe(URI_EVENTLISTENER, strContract, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                ImuModel imuModel = new Gson().fromJson(data, ImuModel.class);

                if (imuModel != null && imuModel.getBody().getArrayAcc().length > 0 && imuModel.getBody().getArrayGyro().length > 0) {
                    for (int i = 0; i < imuModel.getBody().getArrayAcc().length; i++) {

                        String sensorMsgStr = "x: " + Math.round(imuModel.getBody().getArrayAcc()[i].getX() * 100) / 100.0 + "  y: " + Math.round(imuModel.getBody().getArrayAcc()[i].getY() * 100) / 100.0 + "  z: " + Math.round(imuModel.getBody().getArrayAcc()[i].getZ() * 100) / 100.0;

                        DenseInstance instance = createInstance(imuModel.getBody().getTimestamp(), imuModel.getBody().getArrayAcc()[i].getX(), imuModel.getBody().getArrayAcc()[i].getY(), imuModel.getBody().getArrayAcc()[i].getZ(), imuModel.getBody().getArrayGyro()[i].getX(), imuModel.getBody().getArrayGyro()[i].getY(), imuModel.getBody().getArrayGyro()[i].getZ(), imuModel.getBody().getArrayMag()[i].getX(), imuModel.getBody().getArrayMag()[i].getY(), imuModel.getBody().getArrayMag()[i].getZ());

                        sensorInstances.add(instance);
                        if (sensorInstances.size() == 100) {
                            performKMeans(sensorInstances);
                            sensorInstances.clear();
                        }

                    }
                }

            }

            @Override
            public void onError(MdsException error) {
                unsubscribe();
            }
        });
    }


    /**
     * Perform KMeans clustering
     *
     * @param unknownData Data to perform KMeans on
     */
    private void performKMeans(Instances unknownData) {
        try {
            csvFiles = getCSVFileNames(trimmedCsvFolderPath).toArray(new String[0]);
            Arrays.sort(csvFiles);

            double[] result = new double[csvFiles.length];
            for (int i = 0; i < prefixes.length; i++) {
                List<String> columns = new ArrayList<>();

                columns.add(prefixes[i] + "_x");
                columns.add(prefixes[i] + "_y");
                columns.add(prefixes[i] + "_z");

                if (Objects.equals(prefixes[i], "acc_diff")) {
                    for (int j = 0; j < unknownData.numInstances() - 1; j++) {
                        unknownData.instance(j).setValue(10, unknownData.instance(j + 1).value(unknownData.attribute("acc_x")) - unknownData.instance(j).value(unknownData.attribute("acc_x")));
                        unknownData.instance(j).setValue(11, unknownData.instance(j + 1).value(unknownData.attribute("acc_y")) - unknownData.instance(j).value(unknownData.attribute("acc_y")));
                        unknownData.instance(j).setValue(12, unknownData.instance(j + 1).value(unknownData.attribute("acc_z")) - unknownData.instance(j).value(unknownData.attribute("acc_z")));
                    }
                }

                if (Objects.equals(prefixes[i], "acc_ma")) {
                    for (int j = 0; j < unknownData.numInstances() - 1; j++) {
                        unknownData.instance(j).setValue(13, (unknownData.instance(j).value(unknownData.attribute("acc_x")) + unknownData.instance(j + 1).value(unknownData.attribute("acc_x"))) / 2);
                        unknownData.instance(j).setValue(14, (unknownData.instance(j).value(unknownData.attribute("acc_y")) + unknownData.instance(j + 1).value(unknownData.attribute("acc_y"))) / 2);
                        unknownData.instance(j).setValue(15, (unknownData.instance(j).value(unknownData.attribute("acc_z")) + unknownData.instance(j + 1).value(unknownData.attribute("acc_z"))) / 2);
                    }
                }

                Instances data = getColumnData(unknownData, columns);

                // Make a copy of data
                Instances dataCopy = new Instances(data);

                SimpleKMeans kmeansModel = kmeans.get(i);

                data.setClassIndex(data.numAttributes() - 1);

                Attribute clusterIdAttr = new Attribute("cluster_id_" + prefixes[i]);
                data.insertAttributeAt(clusterIdAttr, data.numAttributes());

                for (int j = 0; j < data.numInstances(); j++) {
                    int clusterId = kmeansModel.clusterInstance(dataCopy.instance(j));
                    data.instance(j).setValue(data.numAttributes() - 1, clusterId);
                }

                double[] resultCurr = processData(combinedDataGestures, data, prefixes[i]);
                for (int j = 0; j < result.length; j++) {
                    result[j] += weights[i] * resultCurr[j];
                }
            }

            for (int i = 0; i < result.length; i++) {
                result[i] = Math.round(result[i] * 100.0) / 100.0;
            }

            int maxIndex = 0;
            for (int i = 1; i < result.length; i++) {
                if (result[i] > result[maxIndex]) {
                    maxIndex = i;
                }
            }

            String resultFinal = csvFiles[maxIndex].replaceAll("_", " ").replaceAll("[0-9]", "").trim();

            // check if a string has a substring
            if (!resultFinal.startsWith("rest")) {
                AccItem accItem = new AccItem(this);
                accItem.setOnItemRemovedListener(this);

                accItem.setText1(resultFinal);
                for (int i = 0; i < gestures.size(); i++) {
                    if (gestures.get(i).equals(resultFinal)) {
                        accItem.setText2(dialog.get(i));
                        textToSpeech.speak(dialog.get(i), TextToSpeech.QUEUE_FLUSH, null, null);
                        break;
                    }
                }

                scrollableLayout.addView(accItem);
                accItems.add(accItem);

            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /**
     * Create an instance
     *
     * @param timestamp Timestamp of the instance
     * @param acc_x     X-axis acceleration
     * @param acc_y     Y-axis acceleration
     * @param acc_z     Z-axis acceleration
     * @param gyro_x    X-axis gyroscope
     * @param gyro_y    Y-axis gyroscope
     * @param gyro_z    Z-axis gyroscope
     * @param magn_x    X-axis magnetometer
     * @param magn_y    Y-axis magnetometer
     * @param magn_z    Z-axis magnetometer
     * @return Instance
     */
    private DenseInstance createInstance(double timestamp, double acc_x, double acc_y, double acc_z, double gyro_x, double gyro_y, double gyro_z, double magn_x, double magn_y, double magn_z) {
        DenseInstance instance = new DenseInstance(16);

        // Set attribute values
        instance.setValue(0, timestamp);
        instance.setValue(1, acc_x);
        instance.setValue(2, acc_y);
        instance.setValue(3, acc_z);
        instance.setValue(4, gyro_x);
        instance.setValue(5, gyro_y);
        instance.setValue(6, gyro_z);
        instance.setValue(7, magn_x);
        instance.setValue(8, magn_y);
        instance.setValue(9, magn_z);
        instance.setValue(10, acc_x);
        instance.setValue(11, acc_y);
        instance.setValue(12, acc_z);
        instance.setValue(13, acc_x);
        instance.setValue(14, acc_y);
        instance.setValue(15, acc_z);

        return instance;
    }


    /**
     * Process the data
     *
     * @param combinedData Combined data
     * @param unknownData  Unknown data
     * @param currPrefix   Current prefix
     * @return Processed data
     */
    private double[] processData(Instances combinedData, Instances unknownData, String currPrefix) {
        double[] result = new double[csvFiles.length];

        for (int i = 0; i < csvFiles.length; i++) {
            // String gestureId = csvFiles[i] + ".csv";

            List<Integer> cluster1 = new ArrayList<>();
            for (int j = 0; j < combinedData.numInstances(); j++) {
                int currGestureId = (int) combinedData.instance(j).value(combinedData.attribute("gesture_id"));
                if (i == currGestureId && combinedData.attribute("cluster_id_" + currPrefix) != null) {
                    cluster1.add((int) combinedData.instance(j).value(combinedData.attribute("cluster_id_" + currPrefix)));
                }
            }

            List<Integer> cluster2 = new ArrayList<>();
            for (int j = 0; j < unknownData.numInstances(); j++) {
                cluster2.add((int) unknownData.instance(j).value(unknownData.attribute("cluster_id_" + currPrefix)));
            }

            result[i] = normSim(cluster1, cluster2);
        }

        return result;
    }


    /**
     * Calculate the normalized similarity between two clusters
     *
     * @param cluster1 First cluster
     * @param cluster2 Second cluster
     * @return Normalized similarity
     */
    private double normSim(List<Integer> cluster1, List<Integer> cluster2) {
        double longestCommonSubsequence = longestCommonSubsequence(cluster1, cluster2);
        return longestCommonSubsequence / Math.max(cluster1.size(), cluster2.size());
    }


    /**
     * Calculate the longest common subsequence between two clusters
     *
     * @param cluster1 First cluster
     * @param cluster2 Second cluster
     * @return Longest common subsequence
     */
    private int longestCommonSubsequence(List<Integer> cluster1, List<Integer> cluster2) {
        int m = cluster1.size();
        int n = cluster2.size();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (cluster1.get(i - 1).equals(cluster2.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        return dp[m][n];
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