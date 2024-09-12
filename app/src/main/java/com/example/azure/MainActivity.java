package com.example.azure;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private MediaRecorder recorder = null;
    private String fileName = null;

    private Button recordButton;
    private TextView transcriptionTextView;
    private TextView statusTextView;
    private TextView timerTextView;
    private TextView aiTextView;
    private boolean isRecording = false;
    private long startTime = 0L;
    private Handler timerHandler = new Handler();
    private Handler uiHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = SystemClock.elapsedRealtime() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            timerTextView.setText(String.format("%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (!permissionToRecordAccepted) finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Request permissions
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        // Set up the record button and text view
        recordButton = findViewById(R.id.button3);
        transcriptionTextView = findViewById(R.id.textView2);
        statusTextView = findViewById(R.id.statusTextView);
        timerTextView = findViewById(R.id.timerTextView);
        aiTextView = findViewById(R.id.aitext);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }
                isRecording = !isRecording;
            }
        });

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        recorder.start();
        startTime = SystemClock.elapsedRealtime();
        timerHandler.postDelayed(timerRunnable, 0);
        recordButton.setText("Stop Recording");
        statusTextView.setText("Status: Recording...");
        statusTextView.setTextColor(Color.parseColor("#0B6623"));
        Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
        timerHandler.removeCallbacks(timerRunnable);
        recordButton.setText("Start Recording");
        statusTextView.setText("Status: Processing...");
        timerTextView.setText("00:00");
        Toast.makeText(this, "Recording Stopped", Toast.LENGTH_SHORT).show();
        convert3gpToWav(fileName);
    }

    private void convert3gpToWav(String filePath) {
        String wavFilePath = filePath.replace(".3gp", ".wav");
        try {
            String[] command = {"-y", "-i", filePath, "-ar", "16000", wavFilePath}; // Convert to 16000 Hz
            FFmpeg.executeAsync(command, (executionId, returnCode) -> {
                if (returnCode == Config.RETURN_CODE_SUCCESS) {
                    runOnUiThread(() -> {
//                        Toast.makeText(MainActivity.this, "Conversion Successful", Toast.LENGTH_SHORT).show();
                        transcribeAudio(wavFilePath);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Conversion Failed", Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void transcribeAudio(String wavFilePath) {
        new Thread(() -> {
            try {
                SpeechConfig speechConfig = SpeechConfig.fromSubscription("4c74aec3e2864ae683cb7cf2d401d526", "uksouth");
                AudioConfig audioConfig = AudioConfig.fromWavFileInput(wavFilePath);
                SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

                recognizer.recognized.addEventListener((s, e) -> {
                    String transcription = e.getResult().getText();
                    runOnUiThread(() -> {
                        transcriptionTextView.setText(transcription);
                        statusTextView.setText("Status: Transcription Successful!");
                        Toast.makeText(MainActivity.this, "Transcription Successful", Toast.LENGTH_LONG).show();
                        uiHandler.postDelayed(() -> {
                            statusTextView.setText("Status: Generating records...");
                            getDetailedDescription(transcription);
                        }, 2000);
                    });
                });

                recognizer.recognizeOnceAsync().get();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    statusTextView.setText("Status: Transcription Failed");
                    Toast.makeText(MainActivity.this, "Failed to transcribe audio: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void getDetailedDescription(String rawText) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build();

                MediaType mediaType = MediaType.parse("application/json");
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("messages", new JSONArray().put(new JSONObject()
                        .put("role", "user")
                        .put("content", "Convert the following clinical conversation into a FHIR-enabled record with SNOMED CT codes and and provide only the FHIR records in JSON format without the prefix statement. only show the json file foramt" + rawText)));
                jsonBody.put("max_tokens", 4000);

                RequestBody body = RequestBody.create(jsonBody.toString(), mediaType);

                Request request = new Request.Builder()
                        .url("https://2ndversion.openai.azure.com/openai/deployments/gpt-4o/chat/completions?api-version=2024-02-15-preview")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("api-key", "b951fee60d8d458d854e34870c4d7f5a")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject responseJson = new JSONObject(responseBody);
                        String detailedDescription = responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

                        runOnUiThread(() -> {
                            aiTextView.setText(detailedDescription);
                            statusTextView.setText("Status: Generate FHIR records successful!");
                            Toast.makeText(MainActivity.this, "Detailed description generated", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> {
                            statusTextView.setText("Status: Failed to generate description");
                            Toast.makeText(MainActivity.this, "Failed to get detailed description", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    statusTextView.setText("Status: Failed to generate description");
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }
}
