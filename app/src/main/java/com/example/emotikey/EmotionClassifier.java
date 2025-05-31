package com.example.emotikey;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;

public class EmotionClassifier {
    private static final String TAG = "EmotionClassifier";

    // Model configuration - matches your Python training setup
    private static final String MODEL_FILE = "emotion_model.tflite";
    private static final int INPUT_SIZE = 48; // 48x48 input size from your model
    private static final int NUM_CLASSES = 7; // 7 emotion classes
    private static final int PIXEL_SIZE = 1; // Grayscale
    private static final int INPUT_SIZE_BYTES = INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE * 4; // float32

    // Emotion labels matching your training data
    private static final String[] EMOTION_LABELS = {
            "angry", "disgust", "fear", "happy", "neutral", "sad", "surprise"
    };

    private Interpreter tflite;
    private ByteBuffer inputBuffer;
    private float[][] outputBuffer;
    private boolean isInitialized = false;

    public EmotionClassifier(Context context) {
        try {
            initializeModel(context);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize emotion classifier", e);
        }
    }

    private void initializeModel(Context context) throws IOException {
        // Load the TensorFlow Lite model
        MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE);

        // Configure interpreter options for better performance
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4); // Use multiple threads for better performance
        options.setUseXNNPACK(true); // Enable XNNPACK delegate for CPU optimization

        tflite = new Interpreter(modelBuffer, options);

        // Allocate input and output buffers
        inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE_BYTES);
        inputBuffer.order(ByteOrder.nativeOrder());

        outputBuffer = new float[1][NUM_CLASSES];

        isInitialized = true;
        Log.d(TAG, "TensorFlow Lite model initialized successfully");
    }

    public EmotionPrediction classifyEmotion(Bitmap faceBitmap) {
        if (!isInitialized) {
            Log.e(TAG, "Model not initialized");
            return null;
        }

        if (faceBitmap == null) {
            Log.e(TAG, "Input bitmap is null");
            return null;
        }

        try {
            // Preprocess the image
            preprocessImage(faceBitmap);

            // Run inference
            tflite.run(inputBuffer, outputBuffer);

            // Process results
            return processResults();

        } catch (Exception e) {
            Log.e(TAG, "Error during emotion classification", e);
            return null;
        }
    }

    private void preprocessImage(Bitmap bitmap) {
        // Ensure the bitmap is 48x48 grayscale
        if (bitmap.getWidth() != INPUT_SIZE || bitmap.getHeight() != INPUT_SIZE) {
            Log.w(TAG, "Input bitmap size mismatch. Expected 48x48, got " +
                    bitmap.getWidth() + "x" + bitmap.getHeight());
        }

        // Reset buffer position
        inputBuffer.rewind();

        // Extract pixel values and normalize to [0, 1]
        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int i = 0; i < pixels.length; i++) {
            // Extract grayscale value (assuming already converted to grayscale)
            int pixel = pixels[i];
            float grayscaleValue = (pixel & 0xFF) / 255.0f; // Normalize to [0, 1]
            inputBuffer.putFloat(grayscaleValue);
        }
    }

    private EmotionPrediction processResults() {
        float[] scores = outputBuffer[0];

        // Find the emotion with highest confidence
        int maxIndex = 0;
        float maxConfidence = scores[0];

        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > maxConfidence) {
                maxConfidence = scores[i];
                maxIndex = i;
            }
        }

        // Apply softmax to get proper probabilities
        float[] probabilities = applySoftmax(scores);

        String predictedEmotion = EMOTION_LABELS[maxIndex];
        float confidence = probabilities[maxIndex];

        Log.d(TAG, "Predicted emotion: " + predictedEmotion +
                " with confidence: " + String.format("%.3f", confidence));

        // Return prediction with all scores for debugging
        return new EmotionPrediction(predictedEmotion, confidence, probabilities.clone());
    }

    private float[] applySoftmax(float[] scores) {
        float[] probabilities = new float[scores.length];
        float sum = 0.0f;

        // Find max for numerical stability
        float max = scores[0];
        for (float score : scores) {
            if (score > max) max = score;
        }

        // Calculate exp(x - max) and sum
        for (int i = 0; i < scores.length; i++) {
            probabilities[i] = (float) Math.exp(scores[i] - max);
            sum += probabilities[i];
        }

        // Normalize
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= sum;
        }

        return probabilities;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        isInitialized = false;
        Log.d(TAG, "Emotion classifier closed");
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public static String[] getEmotionLabels() {
        return EMOTION_LABELS.clone();
    }

    // Result class for emotion predictions
    public static class EmotionPrediction {
        public final String emotion;
        public final float confidence;
        public final float[] allScores;

        public EmotionPrediction(String emotion, float confidence, float[] allScores) {
            this.emotion = emotion;
            this.confidence = confidence;
            this.allScores = allScores;
        }

        @Override
        public String toString() {
            return String.format("EmotionPrediction{emotion='%s', confidence=%.3f}",
                    emotion, confidence);
        }
    }
}