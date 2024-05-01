package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

public class GestureActivity extends AppCompatActivity {
    private static final String LOG_TAG = GestureActivity.class.getSimpleName();
    public static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private static final String URI_TIME = "suunto://{0}/Time";
    private static final String PATH = "/Meas/IMU9/";
    private static final String RATE = "52";
    private MdsSubscription mdsSubscription;
    private static String trimmedCsvFolderPath;
    private static String modelCsvFolderPath;
    private File directory;
    private String[] prefixes;
    private double[] weights;
    private String[] csvFiles;
    private int nClusters = 20;
    Button recordScreenButton;
    Spinner modelSpinner;
    TextView sensorMsg, outputMsg;
    ArrayList<SimpleKMeans> kmeans;
    Instances combinedDataGestures;
    Instances sensorInstances;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);

        // UI elements
        recordScreenButton = findViewById(R.id.recordScreenButton);
        modelSpinner = findViewById(R.id.modelSpinner);
        sensorMsg = findViewById(R.id.sensorMsg);
        outputMsg = findViewById(R.id.outputMsg);


        // Set paths
        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        trimmedCsvFolderPath = directory + "/trimmedData";
        modelCsvFolderPath = directory + "/models";


        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getCSVFileNames(modelCsvFolderPath));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(adapter);

        modelSpinner.setOnItemSelectedListener(itemSelectedListener);

        sensorInstances = new Instances("sensorData", createAttributes(), 0);

        subscribeToSensor(getConnectedSerial());
    }

    private Mds getMds() {
        return MainActivity.mMds;
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
        return attributes;
    }


    /**
     * Get the connected serial from MainActivity
     *
     * @return String
     */
    private String getConnectedSerial() {
        return MainActivity.connectedSerial;
    }

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
                        sensorMsg.setText(sensorMsgStr);

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
                //Log.e(LOG_TAG, "subscription onError(): ", error);
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

    private DenseInstance createInstance(double timestamp, double acc_x, double acc_y, double acc_z, double gyro_x, double gyro_y, double gyro_z, double magn_x, double magn_y, double magn_z) {
        DenseInstance instance = new DenseInstance(10);

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

        return instance;
    }

    private AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
            String selectedModel = modelSpinner.getSelectedItem().toString();
            String modelFilePath = modelCsvFolderPath + "/" + selectedModel + ".csv";
            try {
                CSVLoader loader = new CSVLoader();
                loader.setSource(new File(modelFilePath));
                Instances data = loader.getDataSet();

                prefixes = new String[data.numInstances()];
                weights = new double[data.numInstances()];

                for (int i = 0; i < data.numInstances(); i++) {
//                    prefixes[i] = data.instance(i).stringValue(0);
                    prefixes[i] = "acc";
                    weights[i] = data.instance(i).value(1);
                }

                getKMeans();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parentView) {
        }
    };

    private List<String> getCSVFileNames(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));
        List<String> csvFiles = new ArrayList<>();
        for (File file : files) {
            csvFiles.add(file.getName().replace(".csv", ""));
        }
        return csvFiles;
    }

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

    private boolean hasMissingValues(Instance instance) {
        for (int i = 0; i < instance.numAttributes(); i++) {
            if (instance.isMissing(i)) {
                return true;
            }
        }
        return false;
    }

    private double normSim(List<Integer> cluster1, List<Integer> cluster2) {
        double longestCommonSubsequence = longestCommonSubsequence(cluster1, cluster2);
        return longestCommonSubsequence / Math.max(cluster1.size(), cluster2.size());
    }

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

    private void performKMeans(Instances unknownData) {
        try {
            csvFiles = getCSVFileNames(trimmedCsvFolderPath).toArray(new String[0]);
            Arrays.sort(csvFiles);

            double[] result = new double[csvFiles.length];
            for (int i = 0; i < prefixes.length; i++) {
                System.out.println("Prefix: " + prefixes[i] + ", Weight: " + weights[i]);
                List<String> columns = new ArrayList<>();

                columns.add(prefixes[i] + "_x");
                columns.add(prefixes[i] + "_y");
                columns.add(prefixes[i] + "_z");

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

            outputMsg.setText("Predicted gesture ID: " + csvFiles[maxIndex]);
            // System.out.println("Predicted gesture ID: " + csvFiles[maxIndex].split("\\.")[0]);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void recordScreenButtonClicked(View view) {
        Intent intent = new Intent(GestureActivity.this, RecordActivity.class);
        startActivity(intent);
    }
}