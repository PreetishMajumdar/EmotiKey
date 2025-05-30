package com.example.emotikey;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class OverlayView extends View {
    private Paint paint;
    private Rect faceRect;
    private String currentEmotion;
    private Map<String, Bitmap> emojiMap;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);

        // Load emoji bitmaps
        loadEmojis(context);
    }

    private void loadEmojis(Context context) {
        emojiMap = new HashMap<>();
        String[] emotions = {"angry", "disgust", "fear", "happy", "neutral", "sad", "surprise"};

        for (String emotion : emotions) {
            try {
                InputStream is = context.getAssets().open(emotion + ".png");
                Bitmap emoji = BitmapFactory.decodeStream(is);
                emojiMap.put(emotion, emoji);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateFaceData(Rect boundingBox, String emotion) {
        this.faceRect = boundingBox;
        this.currentEmotion = emotion;
        invalidate(); // Trigger redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (faceRect != null) {
            // Draw bounding box
            canvas.drawRect(faceRect, paint);

            // Draw emoji above the face
            if (currentEmotion != null && emojiMap.containsKey(currentEmotion)) {
                Bitmap emoji = emojiMap.get(currentEmotion);
                if (emoji != null) {
                    float emojiSize = faceRect.width() * 0.8f;
                    float left = faceRect.left + (faceRect.width() - emojiSize) / 2;
                    float top = faceRect.top - emojiSize - 20;

                    Rect destRect = new Rect((int)left, (int)top,
                            (int)(left + emojiSize), (int)(top + emojiSize));
                    canvas.drawBitmap(emoji, null, destRect, null);
                }
            }

            // Draw emotion text
            if (currentEmotion != null) {
                paint.setTextSize(40);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawText(currentEmotion.toUpperCase(),
                        faceRect.left, faceRect.bottom + 50, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.GREEN);
            }
        }
    }
}
