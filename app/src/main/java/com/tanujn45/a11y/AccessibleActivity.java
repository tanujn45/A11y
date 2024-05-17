package com.tanujn45.a11y;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class AccessibleActivity extends AppCompatActivity implements View.OnClickListener, AccItem.OnItemRemovedListener {

    ArrayList<AccItem> accItems = new ArrayList<>();
    LinearLayout scrollableLayout, btn1, btn2, btn3, btn4;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accessible);

        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        scrollableLayout = findViewById(R.id.scrollableLayout);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);

        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        System.out.println("Clicked");
        if (v.getId() == R.id.btn1) {
            AccItem accItem = new AccItem(this);
            accItem.setOnItemRemovedListener(this);
            accItem.setImage(R.drawable.image);
            accItem.setText("Wave");
            scrollableLayout.addView(accItem);
            accItems.add(accItem);
        } else if (v.getId() == R.id.btn2) {
            AccItem accItem = new AccItem(this);
            accItem.setOnItemRemovedListener(this);
            accItem.setImage(R.drawable.image);
            accItem.setText("I want");
            scrollableLayout.addView(accItem);
            accItems.add(accItem);
        } else if (v.getId() == R.id.btn3) {
            AccItem accItem = new AccItem(this);
            accItem.setOnItemRemovedListener(this);
            accItem.setImage(R.drawable.image);
            accItem.setText("I don't like");
            scrollableLayout.addView(accItem);
            accItems.add(accItem);
        } else if (v.getId() == R.id.btn4) {
            AccItem accItem = new AccItem(this);
            accItem.setOnItemRemovedListener(this);
            accItem.setImage(R.drawable.image);
            accItem.setText("Come here");
            scrollableLayout.addView(accItem);
            accItems.add(accItem);
        }
    }

    public void playButtonClicked(View view) {
        StringBuilder text = new StringBuilder();
        for (AccItem accItem : accItems) {
            text.append(accItem.getText()).append(" ");
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
    }

    @Override
    public void onItemRemoved(AccItem accItem) {
        accItems.remove(accItem);
    }
}