package com.tanujn45.a11y.filters;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.Slider;
import com.tanujn45.a11y.CSVEditor.CSVFile;
import com.tanujn45.a11y.KMeans.KMeans;
import com.tanujn45.a11y.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Filters extends ConstraintLayout {
    Slider acc, accMa, gyro;
    EditText accEditText, accMAEditText, gyroEditText;
    TextView totalValue;
    Button saveModelButton;
    Button updateModelButton;
    Button deleteModelButton;
    Button similarityButton;
    Button heatmapButton;
    LinearLayout updateDeleteLayout;
    private float value;
    Spinner modelSpinner;
    GridLayout gridLayout;
    ArrayAdapter<String> modelAdapter;
    List<String> models = new ArrayList<>();
    String modelPath;
    String[] prefixes;
    double[] weights;
    HashMap<String, Slider> prefixToSlider = new HashMap<>();
    HashMap<Slider, String> sliderToPrefix = new HashMap<>();
    HashMap<String, String> prefixToWeight = new HashMap<>();
    double[][] heatmap;

    public Filters(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Filters(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.filters_card, this, true);

        acc = findViewById(R.id.accSlider);
        accMa = findViewById(R.id.accMASlider);
        gyro = findViewById(R.id.gyroSlider);
        accEditText = findViewById(R.id.accEditText);
        accMAEditText = findViewById(R.id.accMAEditText);
        gyroEditText = findViewById(R.id.gyroEditText);
        totalValue = findViewById(R.id.totalValue);
        modelSpinner = findViewById(R.id.modelSpinner);
        saveModelButton = findViewById(R.id.saveModelButton);
        updateModelButton = findViewById(R.id.updateModelButton);
        deleteModelButton = findViewById(R.id.deleteModelButton);
        updateDeleteLayout = findViewById(R.id.updateDeleteLayout);
        similarityButton = findViewById(R.id.similarityButton);
        heatmapButton = findViewById(R.id.heatmapButton);
        gridLayout = findViewById(R.id.gridLayout);

        updateDeleteLayout.setVisibility(View.INVISIBLE);
        saveModelButton.setVisibility(View.VISIBLE);

        value = 0;

        File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        modelPath = directory.getAbsolutePath() + "/models";

        setSlider(acc, accEditText);
        setSlider(accMa, accMAEditText);
        setSlider(gyro, gyroEditText);

        prefixToSlider.put("acc", acc);
        prefixToSlider.put("accMa", accMa);
        prefixToSlider.put("gyro", gyro);

        sliderToPrefix.put(acc, "acc");
        sliderToPrefix.put(accMa, "accMa");
        sliderToPrefix.put(gyro, "gyro");

        prefixToWeight.put("acc", "0");
        prefixToWeight.put("accMa", "0");
        prefixToWeight.put("gyro", "0");

        loadModels();
        initSpinners();

        updateModelButton.setOnClickListener(v -> updateModel());
        saveModelButton.setOnClickListener(v -> saveModel());
        deleteModelButton.setOnClickListener(v -> deleteModel());
        similarityButton.setOnClickListener(v -> getSimilarity());
        heatmapButton.setOnClickListener(v -> generateHeatmap());

        // set heatmap to random values between 0 and 1
//        int val = 5;
//        heatmap = new double[val][val];
//        for (int i = 0; i < heatmap.length; i++) {
//            for (int j = 0; j < heatmap[0].length; j++) {
//                heatmap[i][j] = Math.random();
//            }
//        }

        heatmap = new double[][]{{1.00, 0.23, 0.78, 0.65, 0.49}, {0.54, 1.00, 0.31, 0.90, 0.72}, {0.87, 0.44, 1.00, 0.55, 0.92}, {0.12, 0.86, 0.77, 1.00, 0.68}, {0.33, 0.29, 0.48, 0.71, 1.00}};
    }

    // Load all the models from the models directory
    private void loadModels() {
        models.clear();
        models.add("Choose a model");
        File modelDir = new File(modelPath);
        if (modelDir.exists()) {
            File[] modelFiles = modelDir.listFiles();
            if (modelFiles == null) {
                return;
            }
            for (File modelFile : modelFiles) {
                String modelName = modelFile.getName();
                System.out.println(modelName);
                if (modelName.startsWith("tempModelCacheA11y")) {
                    continue;
                }

                if (modelName.endsWith(".csv")) {
                    modelName = modelName.replace(".csv", "");
                }
                models.add(modelName);
            }
        }

        modelAdapter = new ArrayAdapter<>(this.getContext(), android.R.layout.simple_spinner_item, models);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modelSpinner.setAdapter(modelAdapter);
    }

    // Reset all sliders to 0
    private void setSlidersToZero() {
        moveSlider(acc, 0);
        moveSlider(accMa, 0);
        moveSlider(gyro, 0);
    }

    // Initialize the spinner with the models
    private void initSpinners() {
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = (String) parent.getItemAtPosition(position);
                if (selectedModel.equals("Choose a model")) {
                    setSlidersToZero();
                    moveSlider(acc, (float) 100);
                    accEditText.setText("100");
                    updateTotal(acc);
                    saveModelButton.setVisibility(View.VISIBLE);
                    updateDeleteLayout.setVisibility(View.INVISIBLE);
                } else {
                    selectedModel = selectedModel + ".csv";
                    saveModelButton.setVisibility(View.INVISIBLE);
                    updateDeleteLayout.setVisibility(View.VISIBLE);
                    setPrefixesAndWeights(new File(modelPath + "/" + selectedModel));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    // Set sliders range, step size and init the Listener
    private void setSlider(Slider slider, EditText editText) {
        slider.setValueFrom(0);
        slider.setValueTo(100);
        slider.setStepSize(1);
        slider.setValue(0);
        editText.setText("0");

        slider.addOnChangeListener((slider1, value, fromUser) -> {
            editText.setText(String.valueOf((int) value));
            updateTotal(slider1);
        });

        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(getContext(), editText);
                String text = editText.getText().toString();
                try {
                    int currValue = 0;
                    if (!text.isEmpty()) {
                        currValue = Integer.parseInt(text);
                        if (currValue < 0) {
                            currValue = 0;
                        } else if (currValue > 100) {
                            currValue = 100;
                        }
                    }
                    slider.setValue(currValue);
                    updateTotal(slider);

                    // This is purely to remove focus from editText
                    similarityButton.requestFocus();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String text = editText.getText().toString();
                try {
                    int value = Integer.parseInt(text);
                    if (value < 0) {
                        editText.setText("0");
                        moveSlider(slider, 0);
                    } else if (value > 100) {
                        editText.setText("100");
                        moveSlider(slider, 100);
                    } else {
                        moveSlider(slider, value);
                    }
                } catch (NumberFormatException e) {
                }
            }
        });
    }

    public static void hideKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
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
        // Setting the weight for Slider that changed
        String currFilter = sliderToPrefix.get(slider);
        double currWeight = slider.getValue();
        prefixToWeight.put(currFilter, String.valueOf(currWeight));

        value = acc.getValue() + accMa.getValue() + gyro.getValue();
        if (value == 100.0) {
            totalValue.setTextColor(ContextCompat.getColor(this.getContext(), R.color.green));

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
                    updateModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme));
                    updateModelButton.setEnabled(true);
                } else {
                    updateModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme2));
                    updateModelButton.setEnabled(false);
                }
            } else {
                saveModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme));
                saveModelButton.setEnabled(true);
            }
        } else {
            totalValue.setTextColor(ContextCompat.getColor(this.getContext(), R.color.red));

            saveModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme2));
            updateModelButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.theme2));

            saveModelButton.setEnabled(false);
            updateModelButton.setEnabled(false);
        }
        totalValue.setText(String.valueOf(value));
    }

    private void saveModel() {
        View dialogView = LayoutInflater.from(this.getContext()).inflate(R.layout.custom_alert_dialog, null);

        TextView dialogTitle = dialogView.findViewById(R.id.alertTitle);
        EditText modelName = dialogView.findViewById(R.id.fileNameEditText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);
        deleteButton.setVisibility(View.INVISIBLE);

        dialogTitle.setText("Enter the model name");
        modelName.setHint("Model name");

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String name = modelName.getText().toString();
            if (name.isEmpty()) {
                Toast.makeText(this.getContext(), "Model name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (models.contains(name + ".csv")) {
                Toast.makeText(this.getContext(), "Model with the same name already exists", Toast.LENGTH_SHORT).show();
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
        View dialogView = LayoutInflater.from(this.getContext()).inflate(R.layout.custom_alert_dialog, null);

        TextView dialogTitle = dialogView.findViewById(R.id.alertTitle);
        EditText modelName = dialogView.findViewById(R.id.fileNameEditText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button deleteButton = dialogView.findViewById(R.id.deleteButton);

        deleteButton.setVisibility(View.INVISIBLE);
        saveButton.setText("Update");
        modelName.setVisibility(View.INVISIBLE);
        dialogTitle.setText("Overwrite the model?");

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
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

    private void deleteModel() {
        File model = new File(modelPath + "/" + modelSpinner.getSelectedItem().toString() + ".csv");
        if (model.delete()) {
            Toast.makeText(this.getContext(), "Model deleted", Toast.LENGTH_SHORT).show();
            loadModels();
        } else {
            Toast.makeText(this.getContext(), "Failed to delete model", Toast.LENGTH_SHORT).show();
        }
    }

    private void createModelFile(String name) {
        try {
            CSVFile modelCSV = new CSVFile(new String[]{"prefix", "weight"}, modelPath + "/" + name + ".csv");
            prefixToWeight.forEach((prefix, weight) -> {
                if (Double.parseDouble(weight) == 0.0) {
                    return;
                }
                double weightValue = Double.parseDouble(weight) / 100;
                modelCSV.addRow(new String[]{prefix, String.valueOf(weightValue)});
            });
            modelCSV.save();
            loadModels();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getSimilarity() {
        if (value != 100.0) {
            Toast.makeText(this.getContext(), "Total value should be 100", Toast.LENGTH_SHORT).show();
            return;
        }
        createModelFile("tempModelCacheA11y");
    }

    private void generateHeatmap() {
        int numRows = heatmap.length;
        int numCols = heatmap[0].length;

        gridLayout.setRowCount(numRows);
        gridLayout.setColumnCount(numCols);

        int cellSize = 150;  // Size in pixels for each cell

        for (int i = 0; i < heatmap.length; i++) {
            for (int j = 0; j < heatmap[i].length; j++) {
                TextView textView = new TextView(this.getContext());
                double rounded = Math.round(heatmap[i][j] * 100.0) / 100.0;
                textView.setText(String.valueOf(rounded));
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(18);
                textView.setBackgroundColor(getColorForValue((float) heatmap[i][j]));

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(8, 8, 8, 8);
                textView.setLayoutParams(params);

                gridLayout.addView(textView);
            }
        }
    }

    public int getColorForValue(float value) {
        // Ensure the value is within the range 0 to 1
        value = Math.max(0, Math.min(1, value));

        // Define the start and end colors for the gradient
        int colorStart = Color.rgb(0, 0, 255); // Blue for value 0
        int colorEnd = Color.rgb(255, 0, 0);   // Red for value 1

        // Interpolate between the start and end colors
        int r = (int) (Color.red(colorStart) * (1 - value) + Color.red(colorEnd) * value);
        int g = (int) (Color.green(colorStart) * (1 - value) + Color.green(colorEnd) * value);
        int b = (int) (Color.blue(colorStart) * (1 - value) + Color.blue(colorEnd) * value);

        return Color.rgb(r, g, b);
    }

    private double[][] getHeatmapData() {
        String currModel;
        if (modelSpinner.getSelectedItem().toString().equals("Choose a model")) {
            currModel = "tempModelCacheA11y.csv";
        } else {
            currModel = modelSpinner.getSelectedItem().toString();
        }

        KMeans kMeans = new KMeans(this.getContext());
        kMeans.setModel(currModel);

        heatmap = kMeans.performKMeans();
        for (double[] doubles : heatmap) {
            for (double aDouble : doubles) {
                System.out.print(aDouble + " ");
            }
            System.out.println();
        }

        return heatmap;
    }
}
