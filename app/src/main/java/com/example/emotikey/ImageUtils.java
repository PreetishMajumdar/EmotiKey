package com.example.emotikey;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageUtils {

    public static Bitmap cropFaceFromImage(ImageProxy imageProxy, Rect boundingBox) {
        try {
            // Convert ImageProxy to Bitmap
            Bitmap bitmap = imageProxyToBitmap(imageProxy);

            if (bitmap == null) return null;

            // Crop the face region
            int left = Math.max(0, boundingBox.left);
            int top = Math.max(0, boundingBox.top);
            int right = Math.min(bitmap.getWidth(), boundingBox.right);
            int bottom = Math.min(bitmap.getHeight(), boundingBox.bottom);

            if (left >= right || top >= bottom) return null;

            return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                imageProxy.getWidth(), imageProxy.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()),
                100, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
}
