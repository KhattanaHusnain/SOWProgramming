package com.sowp.user.services;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageService {

    private AppCompatActivity activity;
    private ImageCallback callback;
    private Uri photoUri;
    private File currentPhotoFile;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    public interface ImageCallback {
        void onImageSelected(String base64String);
        void onError(String error);
        void onPermissionDenied();
    }

    public ImageService(AppCompatActivity activity, ImageCallback callback) {
        this.activity = activity;
        this.callback = callback;
        initializeLaunchers();
    }

    private void initializeLaunchers() {
        // Gallery launcher
        galleryLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == activity.RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {
                        processImage(result.getData().getData());
                    }
                }
        );

        // Camera launcher
        cameraLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        processImage(photoUri);
                    } else {
                        cleanupTempFile();
                        callback.onError("Failed to capture photo");
                    }
                }
        );

        // Permission launcher
        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean cameraGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA));
                    boolean storageGranted = isStoragePermissionGranted(permissions);

                    if (cameraGranted && storageGranted) {
                        // Both permissions granted, let user choose
                    } else if (cameraGranted) {
                        openCamera();
                    } else if (storageGranted) {
                        openGallery();
                    } else {
                        callback.onPermissionDenied();
                    }
                }
        );
    }

    // Check and request permissions
    public void requestPermissions() {
        String[] permissions = getRequiredPermissions();

        if (hasAllPermissions(permissions)) {
            showImageSourceDialog();
        } else {
            permissionLauncher.launch(permissions);
        }
    }

    // Show dialog to choose between camera and gallery
    private void showImageSourceDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle("Select Photo Source")
                .setMessage("Choose how you want to add your photo:")
                .setPositiveButton("Camera", (dialog, which) -> openCamera())
                .setNegativeButton("Gallery", (dialog, which) -> openGallery())
                .setNeutralButton("Cancel", null)
                .show();
    }

    // Open camera
    public void openCamera() {
        if (!hasCameraPermission()) {
            callback.onPermissionDenied();
            return;
        }

        try {
            cleanupTempFile();
            photoUri = createImageUri();
            if (photoUri != null) {
                cameraLauncher.launch(photoUri);
            } else {
                callback.onError("Failed to create photo file");
            }
        } catch (Exception e) {
            callback.onError("Camera not available");
        }
    }

    // Open gallery
    public void openGallery() {
        if (!hasStoragePermission()) {
            callback.onPermissionDenied();
            return;
        }

        try {
            Intent intent;

            // Try different approaches for better compatibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
            } else {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
            }

            // Fallback if first intent doesn't work
            if (intent.resolveActivity(activity.getPackageManager()) == null) {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
            }

            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                galleryLauncher.launch(Intent.createChooser(intent, "Select Profile Picture"));
            } else {
                callback.onError("No gallery app available");
            }
        } catch (Exception e) {
            callback.onError("Failed to open gallery");
        }
    }

    // Convert image to Base64
    private void processImage(Uri imageUri) {
        new Thread(() -> {
            try {
                InputStream inputStream = activity.getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    activity.runOnUiThread(() -> callback.onError("Failed to read image"));
                    return;
                }

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (bitmap != null) {
                    // Resize bitmap if too large
                    bitmap = resizeBitmap(bitmap, 1024);
                    String base64 = bitmapToBase64(bitmap);

                    activity.runOnUiThread(() -> {
                        if (!base64.isEmpty()) {
                            callback.onImageSelected(base64);
                        } else {
                            callback.onError("Failed to convert image");
                        }
                    });
                } else {
                    activity.runOnUiThread(() -> callback.onError("Failed to load image"));
                }
            } catch (Exception e) {
                activity.runOnUiThread(() -> callback.onError("Error processing image"));
            }
        }).start();
    }

    // Convert Bitmap to Base64 string
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            outputStream.close();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            return "";
        }
    }

    // Convert Base64 string to Bitmap
    public static Bitmap base64ToBitmap(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    // Helper methods
    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            return new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }

    private boolean hasAllPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean isStoragePermissionGranted(java.util.Map<String, Boolean> permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_MEDIA_IMAGES));
        } else {
            return Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_EXTERNAL_STORAGE));
        }
    }

    private Uri createImageUri() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";

        File storageDir = new File(activity.getExternalFilesDir(null), "camera_photos");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        currentPhotoFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        String authority = activity.getPackageName() + ".fileprovider";

        return FileProvider.getUriForFile(activity, authority, currentPhotoFile);
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void cleanupTempFile() {
        if (currentPhotoFile != null && currentPhotoFile.exists()) {
            try {
                currentPhotoFile.delete();
                currentPhotoFile = null;
                photoUri = null;
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    // Call this when activity is destroyed
    public void onDestroy() {
        cleanupTempFile();
    }
}