package com.tanujn45.a11y;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.Slider;
import com.tanujn45.a11y.CSVEditor.CSVFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilterActivity extends AppCompatActivity {
    Slider acc, accMa, accDiff, gyro;
    TextView totalValue;
    Button doneButton;
    private float value;
    Spinner modelSpinner;
    ArrayAdapter<String> modelAdapter;
    List<String> models = new ArrayList<>();
    String modelPath;
    String[] prefixes;
    double[] weights;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        
        acc = findViewById(R.id.accSlider);
        accMa = findViewById(R.id.accMASlider);
        accDiff = findViewById(R.id.accDiffSlider);
        gyro = findViewById(R.id.gyroSlider);
        totalValue = findViewById(R.id.totalValue);
        doneButton = findViewById(R.id.doneButton);
        modelSpinner = findViewById(R.id.modelSpinner);

        value = 0;

        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        modelPath = directory.getAbsolutePath() + "/models";

        setSlider(acc);
        setSlider(accMa);
        setSlider(accDiff);
        setSlider(gyro);

        loadModels();
        initSpinners();
    }

    private void loadModels() {
        File modelDir = new File(modelPath);
        if (modelDir.exists()) {
            File[] modelFiles = modelDir.listFiles();
            if (modelFiles == null) {
                return;
            }
            for (File modelFile : modelFiles) {
                String modelName = modelFile.getName();
                models.add(modelName);
            }
        }
    }

    private void initSpinners() {
        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, models);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = (String) parent.getItemAtPosition(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    
    private void setSlider(Slider slider) {
        slider.setValueFrom(0);
        slider.setValueTo(100);
        slider.setStepSize(1);
        slider.setValue(0);
        slider.addOnChangeListener((slider1, value, fromUser) -> updateTotal());
    }

    private void setPrefixesAndWeights(File model) {
        CSVFile modelCSV;
        try {
            modelCSV = new CSVFile(model.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        prefixes = new String[modelCSV.getCSVData().size() - 1];
        weights = new double[modelCSV.getCSVData().size() - 1];

        List<String[]> data = modelCSV.getCSVData();
        for (int i = 1; i < data.size(); i++) {
            this.prefixes[i - 1] = data.get(i)[0];
            this.weights[i - 1] = Double.parseDouble(data.get(i)[1]);
        }
    }

    private void updateTotal() {
        value = acc.getValue() + accMa.getValue() + accDiff.getValue() + gyro.getValue();
        if (value == 100.0) {
            totalValue.setTextColor(ContextCompat.getColor(this, R.color.green));
            doneButton.setBackgroundColor(ContextCompat.getColor(this,R.color.theme));
            doneButton.setEnabled(true);
        } else {
            totalValue.setTextColor(ContextCompat.getColor(this, R.color.red));
            doneButton.setBackgroundColor(ContextCompat.getColor(this,R.color.theme2));
            doneButton.setEnabled(false);
        }
        totalValue.setText(String.valueOf(value));
    }

    public void doneWithFilterButtonClicked(View view) {
        if (value == 100.0) {
            finish();
        } else {
            Toast.makeText(FilterActivity.this, "The total filter value should be 100", Toast.LENGTH_SHORT).show();
        }
    }
}