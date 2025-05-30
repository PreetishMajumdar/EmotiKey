package com.example.emotikey;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class EmotionClassifier {
    private static final String TAG = "EmotionClassifier";
    private static final String MODEL_FILENAME = "emotion_model.tflite";
    private static final int INPUT_SIZE = 48;
    private static final int NUM_CLASSES = 7;

    private final String[] emotionLabels = {
            "angry", "disgust", "fear", "happy", "neutral", "sad", "surprise"
    };

    private Interpreter interpreter;
    private ByteBuffer inputBuffer;
    private float[][] outputArray;

    public EmotionClassifier(Context context) {
        try {
            MappedByteBuffer model = loadModelFile(context);
            interpreter = new Interpreter(model);

            // Initialize input buffer
            inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 1 * 4);
            inputBuffer.order(ByteOrder.nativeOrder());

            // Initialize output array
            outputArray = new float[1][NUM_CLASSES];

            Log.d(TAG, "Emotion classifier initialized successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize emotion classifier", e);
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_FILENAME);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public String classifyEmotion(Bitmap faceBitmap) {
        if (interpreter == null) {
            return "neutral";
        }

        // Preprocess the bitmap
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true);

        // Convert to grayscale and normalize
        convertBitmapToByteBuffer(resizedBitmap);

        // Run inference
        interpreter.run(inputBuffer, outputArray);

        // Get the emotion with highest confidence
        return getEmotionFromOutput(outputArray[0]);
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (inputBuffer == null) {
            return;
        }

        inputBuffer.rewind();

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixel : pixels) {
            // Convert to grayscale and normalize to [0,1]
            int gray = (int) (0.299 * ((pixel >> 16) & 0xFF) +
                    0.587 * ((pixel >> 8) & 0xFF) +
                    0.114 * (pixel & 0xFF));
            float normalizedPixel = gray / 255.0f;
            inputBuffer.putFloat(normalizedPixel);
        }
    }

    private String getEmotionFromOutput(float[] output) {
        int maxIndex = 0;
        float maxValue = output[0];

        for (int i = 1; i < output.length; i++) {
            if (output[i] > maxValue) {
                maxValue = output[i];
                maxIndex = i;
            }
        }

        Log.d(TAG, "Predicted emotion: " + emotionLabels[maxIndex] +
                " with confidence: " + maxValue);

        return emotionLabels[maxIndex];
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
