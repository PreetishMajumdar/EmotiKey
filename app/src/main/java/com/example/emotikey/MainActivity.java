package com.example.emotikey;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements EmotionDetectionAnalyzer.EmotionDetectionListener {
    private static final String TAG = "MainActivity";

    private PreviewView previewView;
    private OverlayView overlayView;
    private TextView statusText;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private EmotionDetectionAnalyzer emotionAnalyzer;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required for emotion detection",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupCamera();
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        statusText = findViewById(R.id.statusText);

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
                updateStatus("Camera initialized successfully");
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
                updateStatus("Camera initialization failed");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        // Preview use case
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image Analysis use case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        try {
            emotionAnalyzer = new EmotionDetectionAnalyzer(this, previewView, this);
            imageAnalysis.setAnalyzer(cameraExecutor, emotionAnalyzer);

            // Select front camera
            CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

            // Unbind all use cases before rebinding
            cameraProvider.unbindAll();

            // Bind use cases to lifecycle
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            updateStatus("Emotion detection ready");

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            updateStatus("Failed to start emotion detection");
        }
    }

    private void updateStatus(String message) {
        runOnUiThread(() -> {
            statusText.setText(message);
            Log.d(TAG, message);
        });
    }

    @Override
    public void onEmotionDetected(EmotionResult emotionResult) {
        runOnUiThread(() -> {
            overlayView.updateEmotions(emotionResult);
            updateStatus("Detected: " + emotionResult.getTopEmotion() +
                    " (" + String.format("%.1f", emotionResult.getTopConfidence() * 100) + "%)");
        });
    }

    @Override
    public void onEmotionDetectionError(String error) {
        runOnUiThread(() -> {
            updateStatus("Error: " + error);
            overlayView.clearEmotions();
        });
    }

    @Override
    public void onNoFaceDetected() {
        runOnUiThread(() -> {
            updateStatus("No face detected");
            overlayView.clearEmotions();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (emotionAnalyzer != null) {
            emotionAnalyzer.close();
        }
    }
}