package com.example.emotikey;

import android.content.Context;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmotionDetectionAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = "EmotionDetectionAnalyzer";

    // Configuration constants - Fixed issues from previous conversation
    private static final float MINIMUM_FACE_SIZE = 0.3f; // Increased from 0.1 to reduce false positives
    private static final float CONFIDENCE_THRESHOLD = 0.7f; // Only show emotions above 70% confidence
    private static final long PROCESSING_INTERVAL_MS = 100; // Process every 100ms for smooth performance

    private final Context context;
    private final PreviewView previewView;
    private final EmotionDetectionListener listener;
    private final FaceDetector faceDetector;
    private final EmotionClassifier emotionClassifier;
    private final ImageUtils imageUtils;
    private final CoordinateTransformHelper coordinateHelper;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private long lastProcessingTime = 0;

    public EmotionDetectionAnalyzer(Context context, PreviewView previewView,
                                    EmotionDetectionListener listener) {
        this.context = context;
        this.previewView = previewView;
        this.listener = listener;

        // Configure ML Kit Face Detection with optimized settings
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(MINIMUM_FACE_SIZE) // Increased to reduce false positives
                .enableTracking()
                .build();

        this.faceDetector = FaceDetection.getClient(options);
        this.emotionClassifier = new EmotionClassifier(context);
        this.imageUtils = new ImageUtils();
        this.coordinateHelper = new CoordinateTransformHelper();
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        // Throttle processing to maintain performance
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastProcessingTime < PROCESSING_INTERVAL_MS) {
            imageProxy.close();
            return;
        }

        if (isProcessing.get()) {
            imageProxy.close();
            return;
        }

        isProcessing.set(true);
        lastProcessingTime = currentTime;

        try {
            // Convert ImageProxy to InputImage for ML Kit
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                listener.onEmotionDetectionError("Failed to get image from camera");
                return;
            }

            InputImage inputImage = InputImage.fromMediaImage(mediaImage,
                    imageProxy.getImageInfo().getRotationDegrees());

            // Detect faces using ML Kit
            detectFaces(inputImage, imageProxy);

        } catch (Exception e) {
            Log.e(TAG, "Error during image analysis", e);
            listener.onEmotionDetectionError("Analysis error: " + e.getMessage());
        } finally {
            isProcessing.set(false);
            imageProxy.close();
        }
    }

    private void detectFaces(InputImage inputImage, ImageProxy imageProxy) {
        Task<List<Face>> result = faceDetector.process(inputImage);

        result.addOnSuccessListener(faces -> {
            try {
                processFaceDetectionResults(faces, imageProxy);
            } catch (Exception e) {
                Log.e(TAG, "Error processing face detection results", e);
                listener.onEmotionDetectionError("Face processing error");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Face detection failed", e);
            listener.onEmotionDetectionError("Face detection failed");
        });
    }

    private void processFaceDetectionResults(List<Face> faces, ImageProxy imageProxy) {
        if (faces.isEmpty()) {
            listener.onNoFaceDetected();
            return;
        }

        // Process the largest face (most prominent)
        Face largestFace = findLargestFace(faces);
        if (largestFace == null) {
            listener.onNoFaceDetected();
            return;
        }

        // Extract face region and classify emotion
        try {
            classifyEmotion(largestFace, imageProxy);
        } catch (Exception e) {
            Log.e(TAG, "Error during emotion classification", e);
            listener.onEmotionDetectionError("Emotion classification error");
        }
    }

    private Face findLargestFace(List<Face> faces) {
        Face largestFace = null;
        float largestArea = 0;

        for (Face face : faces) {
            Rect boundingBox = face.getBoundingBox();
            float area = boundingBox.width() * boundingBox.height();
            if (area > largestArea) {
                largestArea = area;
                largestFace = face;
            }
        }

        return largestFace;
    }

    private void classifyEmotion(Face face, ImageProxy imageProxy) {
        try {
            // Convert ImageProxy to Bitmap
            android.graphics.Bitmap fullBitmap = imageUtils.convertImageProxyToBitmap(imageProxy);
            if (fullBitmap == null) {
                listener.onEmotionDetectionError("Failed to convert image to bitmap");
                return;
            }

            // Extract face region
            Rect faceBounds = face.getBoundingBox();
            android.graphics.Bitmap faceBitmap = imageUtils.cropFaceFromBitmap(fullBitmap, faceBounds);

            if (faceBitmap == null) {
                listener.onEmotionDetectionError("Failed to extract face region");
                fullBitmap.recycle();
                return;
            }

            // Preprocess for emotion model (48x48 grayscale, normalized)
            android.graphics.Bitmap processedFace = imageUtils.preprocessForEmotionModel(faceBitmap);

            if (processedFace == null) {
                listener.onEmotionDetectionError("Failed to preprocess face image");
                faceBitmap.recycle();
                fullBitmap.recycle();
                return;
            }

            // Classify emotion using TensorFlow Lite
            EmotionClassifier.EmotionPrediction prediction =
                    emotionClassifier.classifyEmotion(processedFace);

            if (prediction != null && prediction.confidence >= CONFIDENCE_THRESHOLD) {
                // Transform coordinates for overlay display
                Rect transformedBounds = coordinateHelper.transformCoordinates(
                        faceBounds, imageProxy, previewView);

                EmotionResult result = new EmotionResult(
                        prediction.emotion,
                        prediction.confidence,
                        transformedBounds,
                        prediction.allScores
                );

                listener.onEmotionDetected(result);
            } else {
                listener.onNoFaceDetected(); // Low confidence treated as no detection
            }

            // Clean up bitmaps to prevent memory leaks
            processedFace.recycle();
            faceBitmap.recycle();
            fullBitmap.recycle();

        } catch (Exception e) {
            Log.e(TAG, "Error during emotion classification", e);
            listener.onEmotionDetectionError("Classification error: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (faceDetector != null) {
                faceDetector.close();
            }
            if (emotionClassifier != null) {
                emotionClassifier.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing analyzers", e);
        }
    }

    // Interface for emotion detection callbacks
    public interface EmotionDetectionListener {
        void onEmotionDetected(EmotionResult emotionResult);
        void onEmotionDetectionError(String error);
        void onNoFaceDetected();
    }
}