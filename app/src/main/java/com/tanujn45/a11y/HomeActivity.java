package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout recordDataCardView, accessibleGesturesCardView, annotateGestureCardView, visualizeGestureCardView, rawVideoActivityCardView, getHelpCardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        recordDataCardView = findViewById(R.id.recordDataCardView);
        accessibleGesturesCardView = findViewById(R.id.accessibleGesturesCardView);
        annotateGestureCardView = findViewById(R.id.annotateGestureCardView);
        visualizeGestureCardView = findViewById(R.id.visualizeGestureCardView);
        rawVideoActivityCardView = findViewById(R.id.rawVideoCardView);
        getHelpCardView = findViewById(R.id.getHelpCardView);


        recordDataCardView.setOnClickListener(this);
        accessibleGesturesCardView.setOnClickListener(this);
        annotateGestureCardView.setOnClickListener(this);
        visualizeGestureCardView.setOnClickListener(this);
        rawVideoActivityCardView.setOnClickListener(this);
        getHelpCardView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i;
        if (v.getId() == R.id.recordDataCardView) {
            i = new Intent(this, RecordActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.accessibleGesturesCardView) {
            i = new Intent(this, AccessibleActivity.class);
            startActivity(i);
        }  else if (v.getId() == R.id.annotateGestureCardView) {
            i = new Intent(this, GestureCategoryActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.visualizeGestureCardView) {
            i = new Intent(this, VisualizationActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.rawVideoCardView) {
            i = new Intent(this, RawVideoActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.getHelpCardView) {
            i = new Intent(this, HelpActivity.class);
            startActivity(i);
        }
    }
}