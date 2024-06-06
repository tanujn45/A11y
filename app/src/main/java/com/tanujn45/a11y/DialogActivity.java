package com.tanujn45.a11y;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class DialogActivity extends AppCompatActivity {
    File directory;

    LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        container = findViewById(R.id.dialogContainer);

        readDialogFile();
    }

    private void readDialogFile() {
        File file = new File(directory, "/dialogs.txt");

        if (!file.exists()) {
            Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    if (parts[0].contains("rest")) {
                        continue;
                    }
                    LinearLayout linearLayout = new LinearLayout(this);
                    LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    linearLayoutParams.setMargins(0, 0, 0, 40);
                    linearLayout.setLayoutParams(linearLayoutParams);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.setPadding(55, 40, 55, 40);
                    linearLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_corner));

                    TextView textView = new TextView(this);
                    textView.setText(parts[0]);
                    LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    textViewParams.setMarginStart(10);
                    textView.setLayoutParams(textViewParams);
                    textView.setTextColor(ContextCompat.getColor(this, R.color.white));
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    linearLayout.addView(textView);

                    EditText editText = new EditText(this);
                    editText.setHint(parts[1]);
                    LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    editTextParams.topMargin = 5;
                    editText.setLayoutParams(editTextParams);
                    editText.setInputType(InputType.TYPE_CLASS_TEXT);
                    editText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                    editText.setTextColor(ContextCompat.getColor(this, R.color.white));
                    editText.setHintTextColor(Color.parseColor("#8E8E8E"));
                    editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    linearLayout.addView(editText);

                    container.addView(linearLayout);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }

    }

    private void saveChanges() {
        File file = new File(directory, "/dialogs.txt");

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
            for (int i = 0; i < container.getChildCount(); i++) {
                View view = container.getChildAt(i);
                if (view instanceof LinearLayout) {
                    LinearLayout linearLayout = (LinearLayout) view;
                    TextView tv = null;
                    EditText et = null;

                    for (int j = 0; j < linearLayout.getChildCount(); j++) {
                        View childView = linearLayout.getChildAt(j);
                        if (childView instanceof EditText) {
                            et = (EditText) childView;
                        } else if (childView instanceof TextView) {
                            tv = (TextView) childView;
                        }
                    }

                    if (tv != null && et != null) {
                        String entry = tv.getText().toString() + ":" + et.getText().toString();
                        writer.write(entry);
                        writer.newLine();
                    }
                }
            }
            writer.write("rest:rest");
            writer.newLine();
            Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
        }
    }


    public void saveButtonClicked(View view) {
        saveChanges();
        Intent intent = new Intent(DialogActivity.this, AccessibleActivity.class);
        startActivity(intent);
        finish();
    }
}