package com.example.emotikey;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.Surface;

import androidx.camera.core.ImageProxy;
import androidx.camera.view.PreviewView;

/**
 * Helper class to handle coordinate transformation between ImageAnalysis and PreviewView
 * Fixes the coordinate mapping issues mentioned in the previous conversation
 */
public class CoordinateTransformHelper {
    private static final String TAG = "CoordinateTransformHelper";

    /**
     * Transform coordinates from ImageAnalysis space to PreviewView space
     * This implementation avoids the CameraX coordinate mapping bugs
     */
    public Rect transformCoordinates(Rect sourceBounds, ImageProxy imageProxy, PreviewView previewView) {
        try {
            // Get source and target dimensions
            int imageWidth = imageProxy.getWidth();
            int imageHeight = imageProxy.getHeight();
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

            int previewWidth = previewView.getWidth();
            int previewHeight = previewView.getHeight();

            if (previewWidth == 0 || previewHeight == 0) {
                Log.w(TAG, "PreviewView dimensions not available");
                return sourceBounds;
            }

            // Create transformation matrix
            Matrix transformMatrix = createTransformMatrix(
                    imageWidth, imageHeight,
                    previewWidth, previewHeight,
                    rotationDegrees,
                    previewView.getScaleType()
            );

            // Apply transformation
            RectF sourceRectF = new RectF(sourceBounds);
            RectF transformedRectF = new RectF();
            transformMatrix.mapRect(transformedRectF, sourceRectF);

            // Convert back to Rect
            return new Rect(
                    Math.round(transformedRectF.left),
                    Math.round(transformedRectF.top),
                    Math.round(transformedRectF.right),
                    Math.round(transformedRectF.bottom)
            );

        } catch (Exception e) {
            Log.e(TAG, "Error transforming coordinates", e);
            return sourceBounds; // Return original if transformation fails
        }
    }

    /**
     * Create transformation matrix from image space to preview space
     * Handles rotation, scaling, and translation
     */
    private Matrix createTransformMatrix(int imageWidth, int imageHeight,
                                         int previewWidth, int previewHeight,
                                         int rotationDegrees,
                                         PreviewView.ScaleType scaleType) {

        Matrix matrix = new Matrix();

        // Step 1: Handle rotation
        boolean isRotated = rotationDegrees == 90 || rotationDegrees == 270;
        int rotatedImageWidth = isRotated ? imageHeight : imageWidth;
        int rotatedImageHeight = isRotated ? imageWidth : imageHeight;

        if (rotationDegrees != 0) {
            // Rotate around center
            matrix.postRotate(rotationDegrees, imageWidth / 2f, imageHeight / 2f);

            // Translate to correct position after rotation
            switch (rotationDegrees) {
                case 90:
                    matrix.postTranslate(0, -imageWidth);
                    break;
                case 180:
                    matrix.postTranslate(-imageWidth, -imageHeight);
                    break;
                case 270:
                    matrix.postTranslate(-imageHeight, 0);
                    break;
            }
        }

        // Step 2: Handle scaling based on ScaleType
        float scaleX, scaleY;
        float translateX = 0, translateY = 0;

        switch (scaleType) {
            case FILL_CENTER:
                // Scale to fill the preview, may crop
                scaleX = (float) previewWidth / rotatedImageWidth;
                scaleY = (float) previewHeight / rotatedImageHeight;
                float scale = Math.max(scaleX, scaleY);
                scaleX = scaleY = scale;

                // Center the image
                translateX = (previewWidth - rotatedImageWidth * scale) / 2f;
                translateY = (previewHeight - rotatedImageHeight * scale) / 2f;
                break;

            case FIT_CENTER:
                // Scale to fit entirely within preview
                scaleX = (float) previewWidth / rotatedImageWidth;
                scaleY = (float) previewHeight / rotatedImageHeight;
                scale = Math.min(scaleX, scaleY);
                scaleX = scaleY = scale;

                // Center the image
                translateX = (previewWidth - rotatedImageWidth * scale) / 2f;
                translateY = (previewHeight - rotatedImageHeight * scale) / 2f;
                break;

            case FILL_START:
            case FILL_END:
            default:
                // Default to FILL_CENTER behavior
                scaleX = (float) previewWidth / rotatedImageWidth;
                scaleY = (float) previewHeight / rotatedImageHeight;
                scale = Math.max(scaleX, scaleY);
                scaleX = scaleY = scale;
                break;
        }

        // Apply scaling and translation
        matrix.postScale(scaleX, scaleY);
        matrix.postTranslate(translateX, translateY);

        return matrix;
    }

    /**
     * Alternative transformation method using manual calculation
     * Use this if the matrix-based approach has issues
     */
    public Rect transformCoordinatesManual(Rect sourceBounds, ImageProxy imageProxy, PreviewView previewView) {
        try {
            int imageWidth = imageProxy.getWidth();
            int imageHeight = imageProxy.getHeight();
            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

            int previewWidth = previewView.getWidth();
            int previewHeight = previewView.getHeight();

            if (previewWidth == 0 || previewHeight == 0) {
                return sourceBounds;
            }

            // Handle rotation
            Rect rotatedBounds = applyRotation(sourceBounds, imageWidth, imageHeight, rotationDegrees);

            // Calculate scale factors
            boolean isRotated = rotationDegrees == 90 || rotationDegrees == 270;
            int effectiveImageWidth = isRotated ? imageHeight : imageWidth;
            int effectiveImageHeight = isRotated ? imageWidth : imageHeight;

            float scaleX = (float) previewWidth / effectiveImageWidth;
            float scaleY = (float) previewHeight / effectiveImageHeight;

            // Use the larger scale to fill the preview (FILL_CENTER behavior)
            float scale = Math.max(scaleX, scaleY);

            // Apply scaling
            int scaledLeft = Math.round(rotatedBounds.left * scale);
            int scaledTop = Math.round(rotatedBounds.top * scale);
            int scaledRight = Math.round(rotatedBounds.right * scale);
            int scaledBottom = Math.round(rotatedBounds.bottom * scale);

            // Apply centering offset
            float offsetX = (previewWidth - effectiveImageWidth * scale) / 2f;
            float offsetY = (previewHeight - effectiveImageHeight * scale) / 2f;

            return new Rect(
                    scaledLeft + Math.round(offsetX),
                    scaledTop + Math.round(offsetY),
                    scaledRight + Math.round(offsetX),
                    scaledBottom + Math.round(offsetY)
            );

        } catch (Exception e) {
            Log.e(TAG, "Error in manual coordinate transformation", e);
            return sourceBounds;
        }
    }

    /**
     * Apply rotation to rectangle coordinates
     */
    private Rect applyRotation(Rect rect, int imageWidth, int imageHeight, int rotationDegrees) {
        switch (rotationDegrees) {
            case 0:
                return rect;
            case 90:
                return new Rect(
                        rect.top,
                        imageWidth - rect.right,
                        rect.bottom,
                        imageWidth - rect.left
                );
            case 180:
                return new Rect(
                        imageWidth - rect.right,
                        imageHeight - rect.bottom,
                        imageWidth - rect.left,
                        imageHeight - rect.top
                );
            case 270:
                return new Rect(
                        imageHeight - rect.bottom,
                        rect.left,
                        imageHeight - rect.top,
                        rect.right
                );
            default:
                Log.w(TAG, "Unsupported rotation: " + rotationDegrees);
                return rect;
        }
    }

    /**
     * Check if the transformed coordinates are within preview bounds
     */
    public boolean isWithinPreviewBounds(Rect rect, PreviewView previewView) {
        return rect.left >= 0 && rect.top >= 0 &&
                rect.right <= previewView.getWidth() &&
                rect.bottom <= previewView.getHeight();
    }

    /**
     * Clamp rectangle to preview bounds
     */
    public Rect clampToPreviewBounds(Rect rect, PreviewView previewView) {
        int left = Math.max(0, rect.left);
        int top = Math.max(0, rect.top);
        int right = Math.min(previewView.getWidth(), rect.right);
        int bottom = Math.min(previewView.getHeight(), rect.bottom);

        return new Rect(left, top, right, bottom);
    }
}