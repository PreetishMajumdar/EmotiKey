package com.example.emotikey;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageUtils {
    private static final String TAG = "ImageUtils";

    /**
     * Convert ImageProxy to Bitmap - handles both YUV420 and JPEG formats
     * Fixed implementation to avoid the coordinate mapping issues
     */
    public Bitmap convertImageProxyToBitmap(ImageProxy imageProxy) {
        try {
            Image image = imageProxy.getImage();
            if (image == null) {
                Log.e(TAG, "Image is null");
                return null;
            }

            // Handle different image formats
            int format = image.getFormat();

            if (format == ImageFormat.YUV_420_888) {
                return yuv420ToBitmap(image);
            } else if (format == ImageFormat.JPEG) {
                return jpegToBitmap(imageProxy);
            } else {
                Log.w(TAG, "Unsupported image format: " + format);
                return yuv420ToBitmap(image); // Fallback to YUV conversion
            }

        } catch (Exception e) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap", e);
            return null;
        }
    }

    /**
     * Convert YUV420_888 format to Bitmap
     * Optimized implementation for better performance
     */
    private Bitmap yuv420ToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            // U and V are swapped for NV21 format
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                    image.getWidth(), image.getHeight(), null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()),
                    85, out); // Use 85% quality for balance between speed and quality

            byte[] imageBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        } catch (Exception e) {
            Log.e(TAG, "Error converting YUV420 to Bitmap", e);
            return null;
        }
    }

    /**
     * Convert JPEG format ImageProxy to Bitmap
     */
    private Bitmap jpegToBitmap(ImageProxy imageProxy) {
        try {
            ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting JPEG to Bitmap", e);
            return null;
        }
    }

    /**
     * Crop face region from full bitmap
     * Includes safety checks to prevent crashes
     */
    public Bitmap cropFaceFromBitmap(Bitmap fullBitmap, Rect faceBounds) {
        if (fullBitmap == null || faceBounds == null) {
            return null;
        }

        try {
            // Ensure bounds are within bitmap dimensions
            int left = Math.max(0, faceBounds.left);
            int top = Math.max(0, faceBounds.top);
            int right = Math.min(fullBitmap.getWidth(), faceBounds.right);
            int bottom = Math.min(fullBitmap.getHeight(), faceBounds.bottom);

            int width = right - left;
            int height = bottom - top;

            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Invalid face bounds: " + faceBounds);
                return null;
            }

            return Bitmap.createBitmap(fullBitmap, left, top, width, height);

        } catch (Exception e) {
            Log.e(TAG, "Error cropping face from bitmap", e);
            return null;
        }
    }

    /**
     * Preprocess face bitmap for emotion model
     * - Resize to 48x48
     * - Convert to grayscale
     * - Apply normalization
     */
    public Bitmap preprocessForEmotionModel(Bitmap faceBitmap) {
        if (faceBitmap == null) {
            return null;
        }

        try {
            // Step 1: Resize to 48x48
            Bitmap resized = Bitmap.createScaledBitmap(faceBitmap, 48, 48, true);

            // Step 2: Convert to grayscale
            Bitmap grayscale = convertToGrayscale(resized);

            // Clean up intermediate bitmap
            if (resized != grayscale) {
                resized.recycle();
            }

            return grayscale;

        } catch (Exception e) {
            Log.e(TAG, "Error preprocessing face bitmap", e);
            return null;
        }
    }

    /**
     * Convert bitmap to grayscale using ColorMatrix
     * More efficient than manual pixel manipulation
     */
    private Bitmap convertToGrayscale(Bitmap originalBitmap) {
        try {
            Bitmap grayscaleBitmap = Bitmap.createBitmap(
                    originalBitmap.getWidth(),
                    originalBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(grayscaleBitmap);
            Paint paint = new Paint();

            // Create grayscale color matrix
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);

            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            paint.setColorFilter(filter);

            canvas.drawBitmap(originalBitmap, 0, 0, paint);

            return grayscaleBitmap;

        } catch (Exception e) {
            Log.e(TAG, "Error converting to grayscale", e);
            return originalBitmap; // Return original if conversion fails
        }
    }

    /**
     * Rotate bitmap by specified degrees
     * Useful for handling camera orientation
     */
    public Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        if (bitmap == null || degrees == 0) {
            return bitmap;
        }

        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);

            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        } catch (Exception e) {
            Log.e(TAG, "Error rotating bitmap", e);
            return bitmap;
        }
    }

    /**
     * Flip bitmap horizontally (for front camera mirror effect)
     */
    public Bitmap flipBitmapHorizontally(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        try {
            Matrix matrix = new Matrix();
            matrix.preScale(-1.0f, 1.0f);

            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        } catch (Exception e) {
            Log.e(TAG, "Error flipping bitmap", e);
            return bitmap;
        }
    }

    /**
     * Get bitmap from asset file
     * Used for loading emoji images
     */
    public Bitmap loadBitmapFromAssets(android.content.Context context, String fileName) {
        try {
            return BitmapFactory.decodeStream(context.getAssets().open(fileName));
        } catch (Exception e) {
            Log.e(TAG, "Error loading bitmap from assets: " + fileName, e);
            return null;
        }
    }

    /**
     * Check if bitmap is valid and not recycled
     */
    public boolean isValidBitmap(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled() && bitmap.getWidth() > 0 && bitmap.getHeight() > 0;
    }

    /**
     * Safe bitmap recycling with null check
     */
    public void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}