package com.example.emotikey;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom view for drawing face detection overlays and emotion emojis
 * Handles the display of bounding boxes and emotion indicators
 */
public class OverlayView extends View {
    private static final String TAG = "OverlayView";

    // Drawing configuration
    private static final float BBOX_STROKE_WIDTH = 4.0f;
    private static final float TEXT_SIZE = 24.0f;
    private static final float EMOJI_SIZE = 60.0f;
    private static final int TEXT_BACKGROUND_ALPHA = 128;

    // Paints for different drawing elements
    private Paint bboxPaint;
    private Paint textPaint;
    private Paint textBackgroundPaint;
    private Paint emojiPaint;

    // Current emotion result
    private EmotionResult currentEmotion;

    // Emoji bitmaps cache
    private Map<String, Bitmap> emojiCache;
    private ImageUtils imageUtils;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize paints
        setupPaints();

        // Initialize emoji cache and utilities
        emojiCache = new HashMap<>();
        imageUtils = new ImageUtils();

        // Load emoji bitmaps
        loadEmojiAssets();

        // Set view as clickable to receive touch events if needed
        setClickable(false);
        setFocusable(false);
    }

    private void setupPaints() {
        // Bounding box paint
        bboxPaint = new Paint();
        bboxPaint.setStyle(Paint.Style.STROKE);
        bboxPaint.setStrokeWidth(BBOX_STROKE_WIDTH);
        bboxPaint.setAntiAlias(true);

        // Text paint for emotion labels
        textPaint = new Paint();
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(0xFFFFFFFF); // White text

        // Text background paint
        textBackgroundPaint = new Paint();
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setAntiAlias(true);

        // Emoji paint
        emojiPaint = new Paint();
        emojiPaint.setAntiAlias(true);
        emojiPaint.setFilterBitmap(true);
    }

    private void loadEmojiAssets() {
        // Load emoji images from assets
        String[] emotions = {"angry", "disgust", "fear", "happy", "neutral", "sad", "surprise"};

        for (String emotion : emotions) {
            try {
                String fileName = "emojis/" + emotion + ".png";
                Bitmap emoji = imageUtils.loadBitmapFromAssets(getContext(), fileName);
                if (emoji != null) {
                    // Scale emoji to desired size
                    Bitmap scaledEmoji = Bitmap.createScaledBitmap(emoji,
                            (int) EMOJI_SIZE, (int) EMOJI_SIZE, true);
                    emojiCache.put(emotion, scaledEmoji);

                    // Recycle original if different
                    if (emoji != scaledEmoji) {
                        emoji.recycle();
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to load emoji for emotion: " + emotion, e);
            }
        }
    }

    /**
     * Update the emotion display with new detection results
     */
    public void updateEmotions(EmotionResult emotionResult) {
        this.currentEmotion = emotionResult;

        // Trigger redraw on UI thread
        post(this::invalidate);
    }

    /**
     * Clear all emotion displays
     */
    public void clearEmotions() {
        this.currentEmotion = null;

        // Trigger redraw on UI thread
        post(this::invalidate);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentEmotion == null) {
            return;
        }

        try {
            drawEmotionOverlay(canvas, currentEmotion);
        } catch (Exception e) {
            Log.e(TAG, "Error drawing emotion overlay", e);
        }
    }

    private void drawEmotionOverlay(Canvas canvas, EmotionResult emotion) {
        Rect bbox = emotion.getBoundingBox();
        String emotionText = emotion.getTopEmotion();
        float confidence = emotion.getTopConfidence();

        // Set colors based on emotion and confidence
        int emotionColor = emotion.getEmotionColor();
        bboxPaint.setColor(emotionColor);
        textBackgroundPaint.setColor(emotionColor | (TEXT_BACKGROUND_ALPHA << 24));

        // Draw bounding box
        drawBoundingBox(canvas, bbox);

        // Draw emotion text with confidence
        String displayText = emotionText + " (" + String.format("%.0f", confidence * 100) + "%)";
        drawEmotionText(canvas, displayText, bbox);

        // Draw emoji above the bounding box
        drawEmotionEmoji(canvas, emotionText, bbox);
    }

    private void drawBoundingBox(Canvas canvas, Rect bbox) {
        // Draw main bounding box
        canvas.drawRect(bbox, bboxPaint);

        // Draw corner decorations for better visibility
        drawCornerDecorations(canvas, bbox);
    }

    private void drawCornerDecorations(Canvas canvas, Rect bbox) {
        float cornerLength = 20f;
        Paint cornerPaint = new Paint(bboxPaint);
        cornerPaint.setStrokeWidth(BBOX_STROKE_WIDTH + 2);

        // Top-left corner
        canvas.drawLine(bbox.left, bbox.top, bbox.left + cornerLength, bbox.top, cornerPaint);
        canvas.drawLine(bbox.left, bbox.top, bbox.left, bbox.top + cornerLength, cornerPaint);

        // Top-right corner
        canvas.drawLine(bbox.right, bbox.top, bbox.right - cornerLength, bbox.top, cornerPaint);
        canvas.drawLine(bbox.right, bbox.top, bbox.right, bbox.top + cornerLength, cornerPaint);

        // Bottom-left corner
        canvas.drawLine(bbox.left, bbox.bottom, bbox.left + cornerLength, bbox.bottom, cornerPaint);
        canvas.drawLine(bbox.left, bbox.bottom, bbox.left, bbox.bottom - cornerLength, cornerPaint);

        // Bottom-right corner
        canvas.drawLine(bbox.right, bbox.bottom, bbox.right - cornerLength, bbox.bottom, cornerPaint);
        canvas.drawLine(bbox.right, bbox.bottom, bbox.right, bbox.bottom - cornerLength, cornerPaint);
    }

    private void drawEmotionText(Canvas canvas, String text, Rect bbox) {
        // Calculate text bounds
        Rect textBounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), textBounds);

        // Position text below the bounding box
        float textX = bbox.left;
        float textY = bbox.bottom + textBounds.height() + 10;

        // Ensure text is within view bounds
        if (textX + textBounds.width() > getWidth()) {
            textX = getWidth() - textBounds.width() - 10;
        }
        if (textY > getHeight() - 10) {
            textY = bbox.top - 10; // Move above if no space below
        }

        // Draw text background
        float padding = 8f;
        Rect backgroundRect = new Rect(
                (int) (textX - padding),
                (int) (textY - textBounds.height() - padding),
                (int) (textX + textBounds.width() + padding),
                (int) (textY + padding)
        );
        canvas.drawRect(backgroundRect, textBackgroundPaint);

        // Draw text
        canvas.drawText(text, textX, textY, textPaint);
    }

    private void drawEmotionEmoji(Canvas canvas, String emotion, Rect bbox) {
        // Try to draw bitmap emoji first
        Bitmap emojiBitmap = emojiCache.get(emotion.toLowerCase());
        if (emojiBitmap != null && !emojiBitmap.isRecycled()) {
            drawBitmapEmoji(canvas, emojiBitmap, bbox);
        } else {
            // Fallback to text emoji
            drawTextEmoji(canvas, emotion, bbox);
        }
    }

    private void drawBitmapEmoji(Canvas canvas, Bitmap emoji, Rect bbox) {
        // Position emoji above the bounding box
        float emojiX = bbox.left + (bbox.width() - EMOJI_SIZE) / 2f;
        float emojiY = bbox.top - EMOJI_SIZE - 10;

        // Ensure emoji is within view bounds
        emojiX = Math.max(0, Math.min(emojiX, getWidth() - EMOJI_SIZE));
        emojiY = Math.max(0, emojiY);

        // Draw emoji bitmap
        canvas.drawBitmap(emoji, emojiX, emojiY, emojiPaint);
    }

    private void drawTextEmoji(Canvas canvas, String emotion, Rect bbox) {
        // Fallback to unicode emoji characters
        String emojiChar;
        switch (emotion.toLowerCase()) {
            case "angry":
                emojiChar = "😠";
                break;
            case "disgust":
                emojiChar = "🤢";
                break;
            case "fear":
                emojiChar = "😨";
                break;
            case "happy":
                emojiChar = "😊";
                break;
            case "neutral":
                emojiChar = "😐";
                break;
            case "sad":
                emojiChar = "😢";
                break;
            case "surprise":
                emojiChar = "😲";
                break;
            default:
                emojiChar = "🤔";
                break;
        }

        // Setup emoji text paint
        Paint emojiTextPaint = new Paint();
        emojiTextPaint.setTextSize(EMOJI_SIZE);
        emojiTextPaint.setAntiAlias(true);

        // Position emoji above the bounding box
        float emojiX = bbox.left + (bbox.width()) / 2f;
        float emojiY = bbox.top - 10;

        // Draw emoji character
        canvas.drawText(emojiChar, emojiX, emojiY, emojiTextPaint);
    }

    /**
     * Check if a point is within the current emotion's bounding box
     * Useful for touch interaction if needed
     */
    public boolean isPointInEmotionBounds(float x, float y) {
        if (currentEmotion == null) {
            return false;
        }

        Rect bbox = currentEmotion.getBoundingBox();
        return bbox.contains((int) x, (int) y);
    }

    /**
     * Get current emotion result
     */
    public EmotionResult getCurrentEmotion() {
        return currentEmotion;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Clean up emoji bitmaps
        for (Bitmap bitmap : emojiCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        emojiCache.clear();
    }
}