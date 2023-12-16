package com.tanujn45.a11y;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.movesense.mds.Mds;

public class SettingsActivity extends AppCompatActivity {
    private Mds getMds() {
        return MainActivity.mMds;
    }
    private String getConnectedSerial() {
        return MainActivity.connectedSerial;
    }

    Button disconnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        disconnectButton = findViewById(R.id.disconnectButton);

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMds().disconnect(getConnectedSerial());
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}