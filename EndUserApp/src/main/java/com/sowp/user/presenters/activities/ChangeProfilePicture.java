package com.sowp.user.presenters.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.sowp.user.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChangeProfilePicture extends AppCompatActivity {

    private static final String TAG = "ChangeProfilePicture";
    private static final int IMAGE_QUALITY = 80;
    private static final int MAX_IMAGE_SIZE = 400;
    private static final String FILE_PROVIDER_AUTHORITY = ".fileprovider";

    // UI Components
    private Toolbar toolbar;
    private CircleImageView profileImage;
    private FloatingActionButton btnChangePhoto;
    private MaterialButton btnTakePhoto, btnChooseGallery, btnSave, btnCancel;
    private TextView statusText;
    private ProgressBar progressBar;
    private View actionButtonsContainer;

    // Data
    private String profileImageBase64 = "";
    private String originalImageBase64 = "";
    private boolean imageChanged = false;
    private Uri photoUri;
    private File currentPhotoFile;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    // Activity result launchers
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_profile_picture);

        initializeFirebase();
        initializeViews();
        setupToolbar();
        initializeActivityResultLaunchers();
        setupClickListeners();
        loadCurrentProfileImage();
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        profileImage = findViewById(R.id.profile_image);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnChooseGallery = findViewById(R.id.btn_choose_gallery);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        statusText = findViewById(R.id.status_text);
        progressBar = findViewById(R.id.progress_bar);
        actionButtonsContainer = findViewById(R.id.action_buttons_container);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeActivityResultLaunchers() {
        // Modern gallery picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            handleSelectedImage(imageUri);
                        }
                    }
                }
        );

        // Modern camera launcher using URI
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && photoUri != null) {
                        handleSelectedImage(photoUri);
                    } else {
                        if (currentPhotoFile != null && currentPhotoFile.exists()) {
                            currentPhotoFile.delete();
                        }
                        showError("Failed to capture photo");
                    }
                }
        );

        // Multiple permissions launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean cameraGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.CAMERA));
                    boolean storageGranted = isStoragePermissionGranted(permissions);

                    if (cameraGranted && storageGranted) {
                        showImageSourceDialog();
                    } else {
                        showPermissionDeniedMessage();
                    }
                }
        );
    }

    private boolean isStoragePermissionGranted(java.util.Map<String, Boolean> permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_MEDIA_IMAGES));
        } else {
            return Boolean.TRUE.equals(permissions.get(Manifest.permission.READ_EXTERNAL_STORAGE));
        }
    }

    private void setupClickListeners() {
        btnChangePhoto.setOnClickListener(v -> checkPermissionsAndShowDialog());
        btnTakePhoto.setOnClickListener(v -> openCamera());
        btnChooseGallery.setOnClickListener(v -> openImagePicker());
        btnSave.setOnClickListener(v -> saveProfileImage());
        btnCancel.setOnClickListener(v -> cancelChanges());
    }

    private void checkPermissionsAndShowDialog() {
        String[] permissions = getRequiredPermissions();

        if (hasAllPermissions(permissions)) {
            showImageSourceDialog();
        } else {
            permissionLauncher.launch(permissions);
        }
    }

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
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void showImageSourceDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Choose Photo Source")
                .setMessage("How would you like to update your profile picture?")
                .setPositiveButton("Camera", (dialog, which) -> openCamera())
                .setNegativeButton("Gallery", (dialog, which) -> openImagePicker())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void openImagePicker() {
        try {
            Intent intent;

            // Try modern approach first (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
            } else {
                // Fallback for older versions
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
            }

            // Alternative: Use ACTION_GET_CONTENT if ACTION_PICK fails
            if (intent.resolveActivity(getPackageManager()) == null) {
                intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
            }

            if (intent.resolveActivity(getPackageManager()) != null) {
                imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Picture"));
            } else {
                showError("No gallery app available");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening gallery: " + e.getMessage(), e);
            showError("Failed to open gallery");
        }
    }

    private void openCamera() {
        try {
            // Clean up any existing photo file
            cleanupTempFile();

            // Create new photo file and URI
            photoUri = createImageUri();

            if (photoUri != null) {
                cameraLauncher.launch(photoUri);
            } else {
                showError("Failed to create photo file");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage(), e);
            showError("Camera not available");
        }
    }

    private Uri createImageUri() throws IOException {
        // Create an image file name with timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "PROFILE_" + timeStamp + "_";

        File storageDir = new File(getExternalFilesDir(null), "profile_photos");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        currentPhotoFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Get the authority from your package name + .fileprovider
        String authority = getPackageName() + FILE_PROVIDER_AUTHORITY;

        return FileProvider.getUriForFile(this, authority, currentPhotoFile);
    }

    private void handleSelectedImage(Uri imageUri) {
        showLoading(true);
        statusText.setText("Processing image...");

        // Use background thread for image processing
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showError("Failed to read image file");
                    });
                    return;
                }

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (bitmap != null) {
                    // Handle image rotation
                    bitmap = handleImageRotation(imageUri, bitmap);

                    // Process on main thread
                    Bitmap finalBitmap = bitmap;
                    runOnUiThread(() -> processAndDisplayImage(finalBitmap));
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showError("Failed to load image");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing image: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Error processing image. Please try again.");
                });
            }
        }).start();
    }

    private Bitmap handleImageRotation(Uri imageUri, Bitmap bitmap) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                ExifInterface exif = new ExifInterface(inputStream);
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                inputStream.close();

                int rotationInDegrees = exifToDegrees(rotation);

                if (rotationInDegrees != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationInDegrees);
                    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not rotate image: " + e.getMessage());
        }

        return bitmap;
    }

    private int exifToDegrees(int exifOrientation) {
        switch (exifOrientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: return 90;
            case ExifInterface.ORIENTATION_ROTATE_180: return 180;
            case ExifInterface.ORIENTATION_ROTATE_270: return 270;
            default: return 0;
        }
    }

    private void processAndDisplayImage(Bitmap bitmap) {
        try {
            // Resize bitmap for better performance and storage
            Bitmap resizedBitmap = resizeBitmapMaintainAspect(bitmap, MAX_IMAGE_SIZE);

            // Animate the change
            profileImage.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

            // Set the image
            profileImage.setImageBitmap(resizedBitmap);

            // Convert to Base64 in background
            new Thread(() -> {
                String base64 = bitmapToBase64(resizedBitmap);

                runOnUiThread(() -> {
                    profileImageBase64 = base64;
                    imageChanged = true;
                    showLoading(false);
                    updateUIAfterImageSelection();
                });
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Error processing and displaying image: " + e.getMessage(), e);
            showLoading(false);
            showError("Error processing image");
        }
    }

    private void updateUIAfterImageSelection() {
        statusText.setText("Image selected! Save to apply changes.");
        statusText.setTextColor(ContextCompat.getColor(this, R.color.primary));

        // Show action buttons with animation
        actionButtonsContainer.setVisibility(View.VISIBLE);
        actionButtonsContainer.startAnimation(
                AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        );
    }

    private void saveProfileImage() {
        if (!imageChanged) {
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            showError("User not authenticated");
            return;
        }

        if (profileImageBase64.isEmpty()) {
            showError("No image data to save");
            return;
        }

        showLoading(true);
        statusText.setText("Saving profile picture...");

        firestore.collection("User")
                .document(auth.getCurrentUser().getEmail())
                .update("photo", profileImageBase64)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    imageChanged = false;
                    originalImageBase64 = profileImageBase64;

                    statusText.setText("Profile picture updated successfully!");
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.primary));
                    actionButtonsContainer.setVisibility(View.GONE);

                    Snackbar.make(findViewById(android.R.id.content),
                                    "Profile picture saved successfully!",
                                    Snackbar.LENGTH_LONG)
                            .setAction("DONE", v -> finish())
                            .show();

                    // Clean up temp file after successful save
                    cleanupTempFile();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error saving profile picture: " + e.getMessage(), e);
                    showError("Failed to save profile picture. Please try again.");
                });
    }

    private void cancelChanges() {
        if (imageChanged) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Discard Changes?")
                    .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                    .setPositiveButton("Discard", (dialog, which) -> resetToOriginalImage())
                    .setNegativeButton("Keep Editing", null)
                    .show();
        } else {
            finish();
        }
    }

    private void resetToOriginalImage() {
        if (!originalImageBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(originalImageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                profileImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error restoring original image: " + e.getMessage());
                profileImage.setImageResource(R.drawable.ic_profile);
            }
        } else {
            profileImage.setImageResource(R.drawable.ic_profile);
        }

        profileImageBase64 = originalImageBase64;
        imageChanged = false;
        statusText.setText("Tap the camera icon to change your photo");
        statusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        actionButtonsContainer.setVisibility(View.GONE);

        // Clean up temp file
        cleanupTempFile();
    }

    private void loadCurrentProfileImage() {
        if (auth.getCurrentUser() == null) {
            showError("User not authenticated");
            return;
        }

        showLoading(true);
        statusText.setText("Loading profile picture...");

        firestore.collection("User")
                .document(auth.getCurrentUser().getEmail())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists() && documentSnapshot.contains("photo")) {
                        String photoBase64 = documentSnapshot.getString("photo");
                        if (photoBase64 != null && !photoBase64.isEmpty()) {
                            originalImageBase64 = photoBase64;
                            profileImageBase64 = photoBase64;

                            try {
                                byte[] decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                profileImage.setImageBitmap(bitmap);
                                statusText.setText("Tap the camera icon to change your photo");
                            } catch (Exception e) {
                                Log.e(TAG, "Error decoding stored image: " + e.getMessage());
                                statusText.setText("Tap the camera icon to change your photo");
                            }
                        } else {
                            statusText.setText("Tap the camera icon to change your photo");
                        }
                    } else {
                        statusText.setText("Tap the camera icon to change your photo");
                    }
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading profile image: " + e.getMessage());
                    statusText.setText("Tap the camera icon to change your photo");
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        setButtonsEnabled(!show);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnChangePhoto.setEnabled(enabled);
        btnTakePhoto.setEnabled(enabled);
        btnChooseGallery.setEnabled(enabled);
        btnSave.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
    }

    private void showError(String message) {
        statusText.setText(message);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.error));
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showPermissionDeniedMessage() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs camera and storage permissions to change your profile picture. " +
                        "Please grant permissions in app settings.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    // Open app settings
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Bitmap resizeBitmapMaintainAspect(Bitmap bitmap, int maxSize) {
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

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void cleanupTempFile() {
        if (currentPhotoFile != null && currentPhotoFile.exists()) {
            try {
                currentPhotoFile.delete();
                currentPhotoFile = null;
                photoUri = null;
            } catch (Exception e) {
                Log.w(TAG, "Could not delete temporary file: " + e.getMessage());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (imageChanged) {
            cancelChanges();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupTempFile();
    }
}