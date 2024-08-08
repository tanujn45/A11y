package com.tanujn45.a11y;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.Slider;
import com.tanujn45.a11y.CSVEditor.CSVFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FilterActivity extends AppCompatActivity {
    Slider acc, accMa, accDiff, gyro;
    TextView totalValue;
    Button doneButton;
    Button saveModelButton;
    Button updateModelButton;
    private float value;
    Spinner modelSpinner;
    ArrayAdapter<String> modelAdapter;
    List<String> models = new ArrayList<>();
    String modelPath;
    String[] prefixes;
    double[] weights;
    HashMap<String, Slider> prefixToSlider = new HashMap<>();

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
        saveModelButton = findViewById(R.id.saveModelButton);
        updateModelButton = findViewById(R.id.updateModelButton);

        value = 0;

        File directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        modelPath = directory.getAbsolutePath() + "/models";

        setSlider(acc);
        setSlider(accMa);
        setSlider(accDiff);
        setSlider(gyro);

        prefixToSlider.put("acc", acc);
        prefixToSlider.put("accMa", accMa);
        prefixToSlider.put("accDiff", accDiff);
        prefixToSlider.put("gyro", gyro);

        loadModels();
        initSpinners();
    }

    // Load all the models from the models directory
    private void loadModels() {
        models.add("Choose a model");
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

    // Reset all sliders to 0
    private void setSlidersToZero() {
        moveSlider(acc, 0);
        moveSlider(accMa, 0);
        moveSlider(accDiff, 0);
        moveSlider(gyro, 0);
    }

    // Initialize the spinner with the models
    private void initSpinners() {
        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, models);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = (String) parent.getItemAtPosition(position);
                if (selectedModel.equals("Choose a model")) {
                    setSlidersToZero();
                    saveModelButton.setVisibility(View.VISIBLE);
                    updateModelButton.setVisibility(View.INVISIBLE);
                } else {
                    saveModelButton.setVisibility(View.INVISIBLE);
                    updateModelButton.setVisibility(View.VISIBLE);
                    setPrefixesAndWeights(new File(modelPath + "/" + selectedModel));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    // Set sliders range, step size and init the Listener
    private void setSlider(Slider slider) {
        slider.setValueFrom(0);
        slider.setValueTo(100);
        slider.setStepSize(1);
        slider.setValue(0);
        slider.addOnChangeListener((slider1, value, fromUser) -> updateTotal(slider1));
    }

    // Move the slider to the value
    private void moveSlider(Slider slider, double value) {
        slider.setValue((float) value);
    }

    // Set the prefixes and weights from the model file
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

        this.setSlidersToZero();
        for (int i = 0; i < this.prefixes.length; i++) {
            Slider slider = this.prefixToSlider.get(this.prefixes[i]);
            assert slider != null;
            double weight = this.weights[i] * 100;
            this.moveSlider(slider, weight);
        }
    }

    private void updateTotal(Slider slider) {
        value = acc.getValue() + accMa.getValue() + accDiff.getValue() + gyro.getValue();
        if (value == 100.0) {
            totalValue.setTextColor(ContextCompat.getColor(this, R.color.green));
            doneButton.setBackgroundColor(ContextCompat.getColor(this, R.color.theme));
            doneButton.setEnabled(true);

            if (!modelSpinner.getSelectedItem().toString().equals("Choose a model")) {
                boolean isModelChanged = false;
                for (int i = 0; i < this.prefixes.length; i++) {
                    Slider currSlider = this.prefixToSlider.get(this.prefixes[i]);
                    double weight = currSlider.getValue() / 100;
                    weight = Math.round(weight * 100.0) / 100.0;
                    if (weight != this.weights[i]) {
                        isModelChanged = true;
                    }
                }

                if (isModelChanged) {
                    updateModelButton.setBackgroundColor(ContextCompat.getColor(this, R.color.theme));
                    updateModelButton.setEnabled(true);
                } else {
                    updateModelButton.setBackgroundColor(ContextCompat.getColor(this, R.color.theme2));
                    updateModelButton.setEnabled(false);
                }
            } else {
                saveModelButton.setBackgroundColor(ContextCompat.getColor(this, R.color.theme));
                saveModelButton.setEnabled(true);
            }
        } else {
            totalValue.setTextColor(ContextCompat.getColor(this, R.color.red));

            saveModelButton.setBackgroundColor(ContextCompat.getColor(this, R.color.theme2));
            updateModelButton.setBackgroundColor(ContextCompat.getColor(this, R.color.theme2));
            doneButton.setBackgroundColor(ContextCompat.getColor(this, R.color.theme2));

            saveModelButton.setEnabled(false);
            updateModelButton.setEnabled(false);
            doneButton.setEnabled(false);
        }
        totalValue.setText(String.valueOf(value));
    }

    private void saveModel() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_dialog, null);

        TextView dialogTitle = dialogView.findViewById(R.id.alertTitle);
        EditText modelName = dialogView.findViewById(R.id.fileNameEditText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        dialogTitle.setText("Enter the model name");
        modelName.setHint("Model name");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String name = modelName.getText().toString();
            if (name.isEmpty()) {
                Toast.makeText(FilterActivity.this, "Model name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (models.contains(name + ".csv")) {
                Toast.makeText(FilterActivity.this, "Model with the same name already exists", Toast.LENGTH_SHORT).show();
                return;
            }
            createModelFile(name);
            alertDialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private void updateModel() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_alert_dialog, null);

        TextView dialogTitle = dialogView.findViewById(R.id.alertTitle);
        EditText modelName = dialogView.findViewById(R.id.fileNameEditText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        modelName.setVisibility(View.INVISIBLE);

        dialogTitle.setText("Overwrite the model?");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        saveButton.setOnClickListener(v -> {
            try {
                CSVFile modelCSV = new CSVFile(modelPath + "/" + modelSpinner.getSelectedItem().toString());
                modelCSV.clearData();
                modelCSV.addRow(new String[]{"prefix", "weight"});
                for (String prefix : prefixes) {
                    Slider slider = prefixToSlider.get(prefix);
                    double weight = slider.getValue() / 100;
                    weight = Math.round(weight * 100.0) / 100.0;
                    if (weight == 0.0) {
                        continue;
                    }
                    modelCSV.addRow(new String[]{prefix, String.valueOf(weight)});
                }
                modelCSV.save();
            } catch (Exception e) {
                e.printStackTrace();
            }

            alertDialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            alertDialog.dismiss();
        });

        alertDialog.show();
    }


    private void createModelFile(String name) {
        try {
            CSVFile modelCSV = new CSVFile(modelPath + "/" + name + ".csv");
            modelCSV.addRow(new String[]{"prefix", "weight"});
            for (String prefix : prefixes) {
                Slider slider = prefixToSlider.get(prefix);
                double weight = slider.getValue() / 100;
                weight = Math.round(weight * 100.0) / 100.0;
                if (weight == 0.0) {
                    continue;
                }
                modelCSV.addRow(new String[]{prefix, String.valueOf(weight)});
            }
            modelCSV.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doneWithFilterButtonClicked(View view) {
        // Pass the value as intent to Visualization activity
        finish();
    }

    public void saveModelButtonClicked(View view) {
        saveModel();
    }
}