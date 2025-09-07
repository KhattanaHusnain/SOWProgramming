package com.sowp.user.services;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sowp.user.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class for handling Base64 image operations including camera/gallery access,
 * permissions management, image processing, and full-screen viewing.
 */
public class ImageService {
    private static final String TAG = "Base64ImageUtils";

    // Configuration constants
    private static final int MAX_IMAGE_SIZE = 1024;
    private static final int JPEG_QUALITY = 80;
    private static final String FILE_PROVIDER_AUTHORITY = ".fileprovider";

    // Permission arrays
    private static final String[] CAMERA_PERMISSIONS = {Manifest.permission.CAMERA};
    private static final String[] STORAGE_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? new String[]{Manifest.permission.READ_MEDIA_IMAGES}
            : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    // Callback interfaces
    public interface Base64ConversionCallback {
        void onSuccess(String base64String);
        void onFailure(String error);
    }

    public interface ImageSelectionCallback {
        void onImageSelected(String base64String, Bitmap bitmap);
        void onError(String error);
        void onPermissionDenied();
    }

    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    /**
     * Handles image selection from camera or gallery with proper permission management
     */
    public static class ImageSelector {
        private final AppCompatActivity activity;
        private final ImageSelectionCallback callback;

        private ActivityResultLauncher<Intent> galleryLauncher;
        private ActivityResultLauncher<Uri> cameraLauncher;
        private ActivityResultLauncher<String[]> permissionLauncher;

        private Uri photoUri;
        private File currentPhotoFile;

        public ImageSelector(AppCompatActivity activity, ImageSelectionCallback callback) {
            this.activity = activity;
            this.callback = callback;
            initializeLaunchers();
        }

        private void initializeLaunchers() {
            initializeGalleryLauncher();
            initializeCameraLauncher();
            initializePermissionLauncher();
        }

        private void initializeGalleryLauncher() {
            galleryLauncher = activity.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null
                                && result.getData().getData() != null) {
                            processImage(result.getData().getData());
                        }
                    }
            );
        }

        private void initializeCameraLauncher() {
            cameraLauncher = activity.registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success && photoUri != null) {
                            processImage(photoUri);
                        } else {
                            cleanup();
                            callback.onError("Failed to capture photo");
                        }
                    }
            );
        }

        private void initializePermissionLauncher() {
            permissionLauncher = activity.registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::handlePermissionResult
            );
        }

        private void handlePermissionResult(java.util.Map<String, Boolean> permissions) {
            boolean hasCameraPermission = hasPermissions(CAMERA_PERMISSIONS);
            boolean hasStoragePermission = hasPermissions(STORAGE_PERMISSIONS);

            if (hasCameraPermission && hasStoragePermission) {
                showImageSourceDialog();
            } else if (hasCameraPermission) {
                openCamera();
            } else if (hasStoragePermission) {
                openGallery();
            } else {
                callback.onPermissionDenied();
            }
        }

        /**
         * Starts image selection process
         */
        public void selectImage() {
            boolean hasCameraPermission = hasPermissions(CAMERA_PERMISSIONS);
            boolean hasStoragePermission = hasPermissions(STORAGE_PERMISSIONS);

            if (hasCameraPermission && hasStoragePermission) {
                showImageSourceDialog();
            } else if (hasCameraPermission) {
                openCamera();
            } else if (hasStoragePermission) {
                openGallery();
            } else {
                requestPermissions();
            }
        }

        private void requestPermissions() {
            java.util.Set<String> permissionsToRequest = new java.util.HashSet<>();

            addMissingPermissions(permissionsToRequest, CAMERA_PERMISSIONS);
            addMissingPermissions(permissionsToRequest, STORAGE_PERMISSIONS);

            if (!permissionsToRequest.isEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
            }
        }

        private void addMissingPermissions(java.util.Set<String> permissionsSet, String[] permissions) {
            for (String permission : permissions) {
                if (!hasPermission(permission)) {
                    permissionsSet.add(permission);
                }
            }
        }

        private boolean hasPermissions(String[] permissions) {
            for (String permission : permissions) {
                if (!hasPermission(permission)) {
                    return false;
                }
            }
            return true;
        }

        private boolean hasPermission(String permission) {
            return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
        }

        private void showImageSourceDialog() {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("Select Image Source")
                    .setMessage("Choose how you want to select an image:")
                    .setPositiveButton("Camera", (dialog, which) -> openCamera())
                    .setNegativeButton("Gallery", (dialog, which) -> openGallery())
                    .setNeutralButton("Cancel", null)
                    .show();
        }

        /**
         * Opens camera for photo capture
         */
        public void openCamera() {
            if (!hasPermissions(CAMERA_PERMISSIONS)) {
                callback.onPermissionDenied();
                return;
            }

            try {
                photoUri = createPhotoUri();
                if (photoUri != null) {
                    cameraLauncher.launch(photoUri);
                } else {
                    callback.onError("Failed to create photo file");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening camera", e);
                callback.onError("Camera not available: " + e.getMessage());
            }
        }

        /**
         * Opens gallery for image selection
         */
        public void openGallery() {
            if (!hasPermissions(STORAGE_PERMISSIONS)) {
                callback.onPermissionDenied();
                return;
            }

            try {
                Intent intent = createGalleryIntent();
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    galleryLauncher.launch(intent);
                } else {
                    callback.onError("No gallery app available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening gallery", e);
                callback.onError("Failed to open gallery: " + e.getMessage());
            }
        }

        private Intent createGalleryIntent() {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            return intent;
        }

        private Uri createPhotoUri() throws IOException {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "IMG_" + timeStamp + "_";

            File storageDir = new File(activity.getExternalFilesDir(null), "camera_photos");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            currentPhotoFile = File.createTempFile(fileName, ".jpg", storageDir);
            String authority = activity.getPackageName() + FILE_PROVIDER_AUTHORITY;

            return FileProvider.getUriForFile(activity, authority, currentPhotoFile);
        }

        private void processImage(Uri imageUri) {
            new Thread(() -> {
                try {
                    Bitmap bitmap = loadBitmap(imageUri);
                    if (bitmap != null) {
                        bitmap = processLoadedBitmap(imageUri, bitmap);
                        String base64String = bitmapToBase64(bitmap, Bitmap.CompressFormat.JPEG, JPEG_QUALITY);

                        Bitmap finalBitmap = bitmap;
                        activity.runOnUiThread(() -> {
                            if (!base64String.isEmpty() && !finalBitmap.isRecycled()) {
                                callback.onImageSelected(base64String, finalBitmap);
                                cleanup();
                            } else {
                                callback.onError("Failed to process image");
                            }
                        });
                    } else {
                        activity.runOnUiThread(() -> callback.onError("Failed to load image"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing image", e);
                    activity.runOnUiThread(() -> callback.onError("Error processing image: " + e.getMessage()));
                }
            }).start();
        }

        private Bitmap loadBitmap(Uri imageUri) throws IOException {
            if ("file".equals(imageUri.getScheme()) && imageUri.getPath() != null) {
                return loadBitmapFromFile(imageUri.getPath());
            } else {
                return loadBitmapFromContentUri(imageUri);
            }
        }

        private Bitmap loadBitmapFromFile(String filePath) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            if (options.outWidth > 0 && options.outHeight > 0) {
                options.inJustDecodeBounds = false;
                options.inSampleSize = calculateSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
                return BitmapFactory.decodeFile(filePath, options);
            }
            return null;
        }

        private Bitmap loadBitmapFromContentUri(Uri imageUri) throws IOException {
            try (InputStream inputStream = activity.getContentResolver().openInputStream(imageUri)) {
                if (inputStream == null) return null;

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);

                if (options.outWidth > 0 && options.outHeight > 0) {
                    try (InputStream secondStream = activity.getContentResolver().openInputStream(imageUri)) {
                        options.inJustDecodeBounds = false;
                        options.inSampleSize = calculateSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
                        return BitmapFactory.decodeStream(secondStream, null, options);
                    }
                }
            }
            return null;
        }

        private Bitmap processLoadedBitmap(Uri imageUri, Bitmap bitmap) {
            bitmap = handleRotation(imageUri, bitmap);
            return resizeBitmapIfNeeded(bitmap);
        }

        private Bitmap handleRotation(Uri imageUri, Bitmap bitmap) {
            try {
                int rotation = getImageRotation(imageUri);
                int degrees = exifToDegrees(rotation);

                if (degrees != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(degrees);
                    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not rotate image: " + e.getMessage());
            }
            return bitmap;
        }

        private int getImageRotation(Uri imageUri) throws IOException {
            if ("file".equals(imageUri.getScheme()) && imageUri.getPath() != null) {
                ExifInterface exif = new ExifInterface(imageUri.getPath());
                return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            } else {
                try (InputStream inputStream = activity.getContentResolver().openInputStream(imageUri)) {
                    if (inputStream != null) {
                        ExifInterface exif = new ExifInterface(inputStream);
                        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    }
                }
            }
            return ExifInterface.ORIENTATION_NORMAL;
        }

        private int exifToDegrees(int exifOrientation) {
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        }

        private int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            return inSampleSize;
        }

        private void cleanup() {
            if (currentPhotoFile != null && currentPhotoFile.exists()) {
                try {
                    currentPhotoFile.delete();
                    currentPhotoFile = null;
                    photoUri = null;
                } catch (Exception e) {
                    Log.w(TAG, "Could not delete temporary file", e);
                }
            }
        }

        /**
         * Clean up resources when activity is destroyed
         */
        public void onDestroy() {
            cleanup();
        }
    }

    /**
     * Full-screen image viewer with zoom and pan functionality
     */
    public static class FullScreenImageViewer {

        public static void showFullScreenImage(Context context, Bitmap bitmap, String title) {
            if (bitmap == null) {
                Toast.makeText(context, "Image not available", Toast.LENGTH_SHORT).show();
                return;
            }

            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_fullscreen_image, null);
            setupDialogView(dialogView, bitmap, title);

            AlertDialog dialog = createFullScreenDialog(context, dialogView);
            dialog.show();
        }

        private static void setupDialogView(View dialogView, Bitmap bitmap, String title) {
            ImageView imageView = dialogView.findViewById(R.id.ivFullscreenImage);
            TextView titleView = dialogView.findViewById(R.id.tvFullImageTitle);
            ImageView closeButton = dialogView.findViewById(R.id.btnCloseFullscreen);

            imageView.setImageBitmap(bitmap);
            setupZoomableImageView(imageView);

            if (title != null && !title.isEmpty()) {
                titleView.setText(title);
                titleView.setVisibility(View.VISIBLE);
            } else {
                titleView.setVisibility(View.GONE);
            }

            closeButton.setOnClickListener(v -> {
                if (v.getContext() instanceof Activity) {
                    ((Activity) v.getContext()).onBackPressed();
                }
            });
        }

        private static AlertDialog createFullScreenDialog(Context context, View dialogView) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            builder.setView(dialogView);

            AlertDialog dialog = builder.create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
                );
                dialog.getWindow().setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                );
            }

            return dialog;
        }

        private static void setupZoomableImageView(ImageView imageView) {
            imageView.setScaleType(ImageView.ScaleType.MATRIX);

            ZoomTouchHandler zoomHandler = new ZoomTouchHandler();
            imageView.setOnTouchListener(zoomHandler);
        }

        public static void showFullScreenImage(Context context, String base64String, String title) {
            Bitmap bitmap = base64ToBitmap(base64String);
            showFullScreenImage(context, bitmap, title);
        }

        /**
         * Handles zoom and pan gestures for ImageView
         */
        private static class ZoomTouchHandler implements View.OnTouchListener {
            private static final float MIN_SCALE = 0.5f;
            private static final float MAX_SCALE = 5f;
            private static final int DOUBLE_TAP_TIMEOUT = 300;

            private final Matrix matrix = new Matrix();
            private final float[] lastTouchPoint = new float[2];
            private final float[] startTouchPoint = new float[2];

            private boolean isDragging = false;
            private boolean isZooming = false;
            private float lastDistance = 0f;
            private float currentScale = 1f;
            private long lastTapTime = 0;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                ImageView imageView = (ImageView) v;

                switch (event.getAction() & android.view.MotionEvent.ACTION_MASK) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        handleActionDown(event);
                        break;
                    case android.view.MotionEvent.ACTION_POINTER_DOWN:
                        handlePointerDown(event);
                        break;
                    case android.view.MotionEvent.ACTION_MOVE:
                        handleMove(event, imageView);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                        handleActionUp(event, imageView);
                        break;
                    case android.view.MotionEvent.ACTION_POINTER_UP:
                        handlePointerUp();
                        break;
                }
                return true;
            }

            private void handleActionDown(android.view.MotionEvent event) {
                lastTouchPoint[0] = event.getX();
                lastTouchPoint[1] = event.getY();
                startTouchPoint[0] = event.getX();
                startTouchPoint[1] = event.getY();
                isDragging = false;
            }

            private void handlePointerDown(android.view.MotionEvent event) {
                if (event.getPointerCount() == 2) {
                    lastDistance = calculateDistance(event);
                    isZooming = true;
                }
            }

            private void handleMove(android.view.MotionEvent event, ImageView imageView) {
                if (event.getPointerCount() == 2 && isZooming) {
                    handlePinchZoom(event, imageView);
                } else if (event.getPointerCount() == 1 && currentScale > 1f) {
                    handlePan(event, imageView);
                }
            }

            private void handlePinchZoom(android.view.MotionEvent event, ImageView imageView) {
                float newDistance = calculateDistance(event);
                if (lastDistance > 0) {
                    float scale = newDistance / lastDistance;
                    float newScale = currentScale * scale;

                    if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
                        matrix.postScale(scale, scale,
                                (event.getX(0) + event.getX(1)) / 2,
                                (event.getY(0) + event.getY(1)) / 2);
                        currentScale = newScale;
                        imageView.setImageMatrix(matrix);
                    }
                }
                lastDistance = newDistance;
            }

            private void handlePan(android.view.MotionEvent event, ImageView imageView) {
                if (!isDragging && isSignificantMovement(event)) {
                    isDragging = true;
                }

                if (isDragging) {
                    float dx = event.getX() - lastTouchPoint[0];
                    float dy = event.getY() - lastTouchPoint[1];
                    matrix.postTranslate(dx, dy);
                    imageView.setImageMatrix(matrix);
                }

                lastTouchPoint[0] = event.getX();
                lastTouchPoint[1] = event.getY();
            }

            private boolean isSignificantMovement(android.view.MotionEvent event) {
                return Math.abs(event.getX() - startTouchPoint[0]) > 10 ||
                        Math.abs(event.getY() - startTouchPoint[1]) > 10;
            }

            private void handleActionUp(android.view.MotionEvent event, ImageView imageView) {
                if (isDoubleTap(event)) {
                    resetZoom(imageView);
                }
                isDragging = false;
            }

            private boolean isDoubleTap(android.view.MotionEvent event) {
                if (isDragging || isZooming || isSignificantMovement(event)) {
                    return false;
                }

                long currentTime = System.currentTimeMillis();
                boolean isDoubleTap = currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT;
                lastTapTime = currentTime;
                return isDoubleTap;
            }

            private void resetZoom(ImageView imageView) {
                matrix.reset();
                currentScale = 1f;
                imageView.setImageMatrix(matrix);
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView.setScaleType(ImageView.ScaleType.MATRIX);
            }

            private void handlePointerUp() {
                isZooming = false;
                lastDistance = 0;
            }

            private float calculateDistance(android.view.MotionEvent event) {
                if (event.getPointerCount() < 2) return 0;
                float dx = event.getX(0) - event.getX(1);
                float dy = event.getY(0) - event.getY(1);
                return (float) Math.sqrt(dx * dx + dy * dy);
            }
        }
    }

    /**
     * Permission handling utilities
     */
    public static class PermissionHelper {

        public static boolean hasCameraPermissions(Context context) {
            return hasAllPermissions(context, CAMERA_PERMISSIONS);
        }

        public static boolean hasStoragePermissions(Context context) {
            return hasAllPermissions(context, STORAGE_PERMISSIONS);
        }

        private static boolean hasAllPermissions(Context context, String[] permissions) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }

        public static void showPermissionSettingsDialog(Context context) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Permissions Required")
                    .setMessage("This app needs camera and storage permissions to function properly. " +
                            "Please grant permissions in app settings.")
                    .setPositiveButton("Settings", (dialog, which) -> openAppSettings(context))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private static void openAppSettings(Context context) {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        }
    }

    // =========================
    // STATIC UTILITY METHODS
    // =========================

    /**
     * Downloads image from URL and converts to Base64
     */
    public static void downloadAndConvertToBase64(String imageUrl, Base64ConversionCallback callback) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            callback.onFailure("Image URL is empty");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(imageUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onFailure("Failed to download image: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] imageBytes = response.body().bytes();
                        byte[] optimizedBytes = optimizeImageBytes(imageBytes);
                        String base64String = Base64.encodeToString(optimizedBytes, Base64.NO_WRAP);

                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(base64String));
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onFailure("Failed to process image: " + e.getMessage()));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onFailure("HTTP error: " + response.code()));
                }

                if (response.body() != null) {
                    response.body().close();
                }
            }
        });
    }

    /**
     * Converts Bitmap to Base64 string
     */
    public static String bitmapToBase64(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        if (bitmap == null) return "";

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Bitmap resizedBitmap = resizeBitmapIfNeeded(bitmap);
            resizedBitmap.compress(format, quality, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert bitmap to Base64", e);
            return "";
        }
    }

    /**
     * Converts Base64 string to Bitmap
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
     * Resizes bitmap if it exceeds maximum dimensions
     */
    public static Bitmap resizeBitmapIfNeeded(Bitmap bitmap) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap;
        }

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
     * Validates if string is valid Base64 encoded image
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
     * Gets estimated file size of Base64 encoded image
     */
    public static long getBase64ImageSize(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return 0;
        }
        return (base64String.length() * 3L) / 4L;
    }

    /**
     * Formats file size to human readable format
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Resizes bitmap to specific dimensions while maintaining aspect ratio
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxWidth;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxHeight;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    // =========================
    // PRIVATE HELPER METHODS
    // =========================

    private static byte[] optimizeImageBytes(byte[] imageBytes) {
        try {
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (originalBitmap == null) {
                return imageBytes;
            }

            Bitmap optimizedBitmap = resizeBitmapIfNeeded(originalBitmap);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream);

                if (optimizedBitmap != originalBitmap) {
                    optimizedBitmap.recycle();
                }
                originalBitmap.recycle();

                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to optimize image size", e);
            return imageBytes;
        }
    }
}