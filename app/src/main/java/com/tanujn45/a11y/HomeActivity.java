package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    LinearLayout recordDataCardView, recognizeGestureCardView, AccessibleGesturesCardView, LoadModelDataCardView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recordDataCardView = findViewById(R.id.recordDataCardView);
        recognizeGestureCardView = findViewById(R.id.recognizeGesturesCardView);
        AccessibleGesturesCardView = findViewById(R.id.accessibleGesturesCardView);
        LoadModelDataCardView = findViewById(R.id.loadModelDataCardView);

        recordDataCardView.setOnClickListener(this);
        recognizeGestureCardView.setOnClickListener(this);
        AccessibleGesturesCardView.setOnClickListener(this);
        LoadModelDataCardView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i;
        if (v.getId() == R.id.recordDataCardView) {
            i = new Intent(this, RecordActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.recognizeGesturesCardView) {
            i = new Intent(this, GestureActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.accessibleGesturesCardView) {
            i = new Intent(this, AccessibleActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.loadModelDataCardView) {
            i = new Intent(this, LoadActivity.class);
            startActivity(i);
        }
    }
}