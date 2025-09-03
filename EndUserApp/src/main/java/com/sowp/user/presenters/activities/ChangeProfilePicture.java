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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.ContentLoadingProgressBar;

import com.sowp.user.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChangeProfilePicture extends AppCompatActivity {

    private static final String TAG = "ChangeProfilePicture";
    private static final int IMAGE_QUALITY = 80;
    private static final int MAX_IMAGE_SIZE = 400;

    // UI Components

    private ImageView backButton;
    private CircleImageView profileImage;
    private FloatingActionButton btnChangePhoto;
    private MaterialButton btnTakePhoto, btnChooseGallery, btnSave, btnCancel;
    private MaterialTextView statusText;
    private ContentLoadingProgressBar progressBar;
    private View actionButtonsContainer;

    // Data
    private String profileImageBase64 = "";
    private String originalImageBase64 = "";
    private boolean imageChanged = false;
    private Uri photoUri;

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
        initializeActivityResultLaunchers();
        setupClickListeners();
        loadCurrentProfileImage();
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.btn_back);
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
        backButton.setOnClickListener(v -> finish());
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
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Picture"));
    }

    private void openCamera() {
        try {
            photoUri = createImageUri();
            cameraLauncher.launch(photoUri);
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
            showError("Camera not available");
        }
    }

    private Uri createImageUri() throws IOException {
        File imageFile = new File(getExternalFilesDir(null), "profile_temp.jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
    }

    private void handleSelectedImage(Uri imageUri) {
        showLoading(true);

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                // Handle image rotation
                bitmap = handleImageRotation(imageUri, bitmap);
                processAndDisplayImage(bitmap);
            } else {
                showError("Failed to load image");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image: " + e.getMessage());
            showError("Error processing image. Please try again.");
        } finally {
            showLoading(false);
        }
    }

    private Bitmap handleImageRotation(Uri imageUri, Bitmap bitmap) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ExifInterface exif = new ExifInterface(inputStream);
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            int rotationInDegrees = exifToDegrees(rotation);

            if (rotationInDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationInDegrees);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
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
        // Resize bitmap for better performance and storage
        Bitmap resizedBitmap = resizeBitmapMaintainAspect(bitmap, MAX_IMAGE_SIZE);

        // Animate the change
        profileImage.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        // Set the image
        profileImage.setImageBitmap(resizedBitmap);

        // Convert to Base64
        profileImageBase64 = bitmapToBase64(resizedBitmap);
        imageChanged = true;

        // Update UI
        updateUIAfterImageSelection();
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
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error saving profile picture: " + e.getMessage());
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
        statusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        actionButtonsContainer.setVisibility(View.GONE);
    }

    private void loadCurrentProfileImage() {
        if (auth.getCurrentUser() == null) {
            showError("User not authenticated");
            return;
        }

        showLoading(true);

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
                            } catch (Exception e) {
                                Log.e(TAG, "Error decoding stored image: " + e.getMessage());
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error loading profile image: " + e.getMessage());
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
        // Clean up temporary file if exists
        if (photoUri != null) {
            try {
                File file = new File(photoUri.getPath());
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not delete temporary file: " + e.getMessage());
            }
        }
    }
}