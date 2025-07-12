package com.example.aiapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.InputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    EditText promptEditText;
    Button generateButton;
    ImageView resultImageView;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.ai_image_generator);
        }

        promptEditText = findViewById(R.id.promptEditText);
        generateButton = findViewById(R.id.generateButton);
        resultImageView = findViewById(R.id.resultImageView);
        progressBar = findViewById(R.id.progressBar);

        generateButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            if (prompt.isEmpty()) {
                Toast.makeText(this, "Enter a prompt", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Generating image...", Toast.LENGTH_SHORT).show();
            resultImageView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            StableHordeClient.generateImage(prompt, new StableHordeClient.ImageCallback() {
                @Override
                public void onSuccess(Bitmap Image) {
                    runOnUiThread(() -> {
                        resultImageView.setImageBitmap(Image);
                        resultImageView.setVisibility(ImageView.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
}
