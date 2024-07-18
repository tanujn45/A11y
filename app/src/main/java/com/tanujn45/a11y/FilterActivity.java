package com.tanujn45.a11y;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.Slider;

public class FilterActivity extends AppCompatActivity {
    Slider acc, accMa, accDiff, gyro;
    TextView totalValue;
    Button doneButton;
    private float value;

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

        value = 0;

        setSlider(acc);
        setSlider(accMa);
        setSlider(accDiff);
        setSlider(gyro);
    }
    
    private void setSlider(Slider slider) {
        slider.setValueFrom(0);
        slider.setValueTo(100);
        slider.setStepSize(1);
        slider.setValue(0);
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            updateTotal();
        });
    }

    private void updateTotal() {
        value = acc.getValue() + accMa.getValue() + accDiff.getValue() + gyro.getValue();
        if (value == 100.0) {
            totalValue.setTextColor(ContextCompat.getColor(this, R.color.green));
            doneButton.setBackgroundColor(ContextCompat.getColor(this,R.color.theme));
            doneButton.setEnabled(true);
        } else {
            totalValue.setTextColor(ContextCompat.getColor(this, R.color.red));
            doneButton.setBackgroundColor(ContextCompat.getColor(this,R.color.grey));
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