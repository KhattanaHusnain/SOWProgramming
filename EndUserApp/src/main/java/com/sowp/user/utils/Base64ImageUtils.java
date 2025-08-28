package com.sowp.user.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class for handling Base64 image operations
 * Includes methods for downloading images from URLs and converting them to Base64
 */
public class Base64ImageUtils {
    private static final String TAG = "Base64ImageUtils";
    private static final int MAX_IMAGE_SIZE = 1024; // Max width/height for resized image
    private static final int JPEG_QUALITY = 80; // JPEG compression quality (0-100)

    public interface Base64ConversionCallback {
        void onSuccess(String base64String);
        void onFailure(String error);
    }

    /**
     * Downloads an image from URL and converts it to Base64 string
     * @param imageUrl The URL of the image to download
     * @param callback Callback to handle success/failure
     */
    public static void downloadAndConvertToBase64(String imageUrl, Base64ConversionCallback callback) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            callback.onFailure("Image URL is empty or null");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to download image from URL: " + imageUrl, e);
                // Switch to main thread for callback
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onFailure("Failed to download image: " + e.getMessage())
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] imageBytes = response.body().bytes();

                        // Optimize image size before converting to Base64
                        byte[] optimizedBytes = optimizeImageSize(imageBytes);
                        String base64String = Base64.encodeToString(optimizedBytes, Base64.NO_WRAP);

                        // Switch back to main thread for callback
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onSuccess(base64String)
                        );

                    } catch (Exception e) {
                        Log.e(TAG, "Failed to convert image to Base64", e);
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onFailure("Failed to process image: " + e.getMessage())
                        );
                    }
                } else {
                    Log.e(TAG, "Failed to download image. Response code: " + response.code());
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure("HTTP error: " + response.code())
                    );
                }

                if (response.body() != null) {
                    response.body().close();
                }
            }
        });
    }

    /**
     * Converts a Bitmap to Base64 string
     * @param bitmap The bitmap to convert
     * @param compressFormat The compression format (JPEG, PNG, etc.)
     * @param quality Compression quality (0-100, only for JPEG)
     * @return Base64 encoded string
     */
    public static String bitmapToBase64(Bitmap bitmap, Bitmap.CompressFormat compressFormat, int quality) {
        if (bitmap == null) {
            return "";
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // Resize bitmap if it's too large
            Bitmap resizedBitmap = resizeBitmapIfNeeded(bitmap);

            resizedBitmap.compress(compressFormat, quality, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert bitmap to Base64", e);
            return "";
        }
    }

    /**
     * Converts Base64 string to Bitmap
     * @param base64String The Base64 encoded image string
     * @return Bitmap object or null if conversion fails
     */
    public static Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert Base64 to bitmap", e);
            return null;
        }
    }

    /**
     * Optimizes image size by resizing if necessary
     * @param imageBytes Original image bytes
     * @return Optimized image bytes
     */
    private static byte[] optimizeImageSize(byte[] imageBytes) {
        try {
            // Decode the original image
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (originalBitmap == null) {
                return imageBytes; // Return original if decoding fails
            }

            // Resize if needed
            Bitmap optimizedBitmap = resizeBitmapIfNeeded(originalBitmap);

            // Convert back to bytes with compression
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);

            // Clean up
            if (optimizedBitmap != originalBitmap) {
                optimizedBitmap.recycle();
            }
            originalBitmap.recycle();

            return outputStream.toByteArray();

        } catch (Exception e) {
            Log.e(TAG, "Failed to optimize image size", e);
            return imageBytes; // Return original on failure
        }
    }

    /**
     * Resizes bitmap if it exceeds maximum dimensions
     * @param bitmap Original bitmap
     * @return Resized bitmap or original if resizing not needed
     */
    private static Bitmap resizeBitmapIfNeeded(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Check if resizing is needed
        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap;
        }

        // Calculate new dimensions maintaining aspect ratio
        float aspectRatio = (float) width / height;
        int newWidth, newHeight;

        if (width > height) {
            newWidth = MAX_IMAGE_SIZE;
            newHeight = Math.round(MAX_IMAGE_SIZE / aspectRatio);
        } else {
            newHeight = MAX_IMAGE_SIZE;
            newWidth = Math.round(MAX_IMAGE_SIZE * aspectRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Validates if a string is a valid Base64 encoded image
     * @param base64String The string to validate
     * @return true if valid Base64 image, false otherwise
     */
    public static boolean isValidBase64Image(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return false;
        }

        try {
            byte[] decoded = Base64.decode(base64String, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            boolean isValid = bitmap != null;
            if (bitmap != null) {
                bitmap.recycle();
            }
            return isValid;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the estimated file size of a Base64 encoded image
     * @param base64String The Base64 encoded image
     * @return Estimated file size in bytes
     */
    public static long getBase64ImageSize(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return 0;
        }

        // Base64 encoding increases size by approximately 33%
        // So actual size = (base64 length * 3) / 4
        return (base64String.length() * 3L) / 4L;
    }
}