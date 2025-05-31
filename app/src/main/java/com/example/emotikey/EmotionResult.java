package com.example.emotikey;

import android.graphics.Rect;

/**
 * Data class to hold emotion detection results
 */
public class EmotionResult {
    private final String topEmotion;
    private final float topConfidence;
    private final Rect boundingBox;
    private final float[] allConfidences;
    private final long timestamp;

    public EmotionResult(String topEmotion, float topConfidence, Rect boundingBox, float[] allConfidences) {
        this.topEmotion = topEmotion;
        this.topConfidence = topConfidence;
        this.boundingBox = boundingBox;
        this.allConfidences = allConfidences != null ? allConfidences.clone() : new float[0];
        this.timestamp = System.currentTimeMillis();
    }

    public String getTopEmotion() {
        return topEmotion;
    }

    public float getTopConfidence() {
        return topConfidence;
    }

    public Rect getBoundingBox() {
        return boundingBox;
    }

    public float[] getAllConfidences() {
        return allConfidences.clone();
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Get confidence for a specific emotion
     */
    public float getConfidenceForEmotion(String emotion) {
        String[] emotions = EmotionClassifier.getEmotionLabels();
        for (int i = 0; i < emotions.length && i < allConfidences.length; i++) {
            if (emotions[i].equals(emotion)) {
                return allConfidences[i];
            }
        }
        return 0.0f;
    }

    /**
     * Check if this is a high confidence detection
     */
    public boolean isHighConfidence() {
        return topConfidence >= 0.8f;
    }

    /**
     * Get emoji character for the detected emotion
     */
    public String getEmoji() {
        switch (topEmotion.toLowerCase()) {
            case "angry":
                return "😠";
            case "disgust":
                return "🤢";
            case "fear":
                return "😨";
            case "happy":
                return "😊";
            case "neutral":
                return "😐";
            case "sad":
                return "😢";
            case "surprise":
                return "😲";
            default:
                return "🤔";
        }
    }

    /**
     * Get color for emotion display
     */
    public int getEmotionColor() {
        switch (topEmotion.toLowerCase()) {
            case "happy":
                return 0xFF4CAF50; // Green
            case "angry":
                return 0xFFF44336; // Red
            case "sad":
                return 0xFF2196F3; // Blue
            case "surprise":
                return 0xFFFF9800; // Orange
            case "fear":
                return 0xFF9C27B0; // Purple
            case "disgust":
                return 0xFF795548; // Brown
            case "neutral":
            default:
                return 0xFF757575; // Gray
        }
    }

    @Override
    public String toString() {
        return String.format("EmotionResult{emotion='%s', confidence=%.3f, box=%s}",
                topEmotion, topConfidence, boundingBox.toString());
    }
}
