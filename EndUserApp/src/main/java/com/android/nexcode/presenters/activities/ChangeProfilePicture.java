package com.android.nexcode.presenters.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import com.android.nexcode.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChangeProfilePicture extends AppCompatActivity {

    // UI Components
    private CircleImageView profileImage;
    private FloatingActionButton btnChangePhoto;
    private MaterialButton btnTakePhoto, btnChooseGallery, btnSave, btnCancel;
    private MaterialTextView statusText;
    private CircularProgressIndicator progressIndicator;
    private View actionButtonsContainer;

    // Data
    private String profileImageBase64 = "";
    private String originalImageBase64 = "";
    private boolean imageChanged = false;

    // Activity result launchers
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> storagePermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_profile_picture);

        initializeViews();
        initializeActivityResultLaunchers();
        setupClickListeners();
        loadCurrentProfileImage();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profile_image);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnChooseGallery = findViewById(R.id.btn_choose_gallery);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        statusText = findViewById(R.id.status_text);
        progressIndicator = findViewById(R.id.progress_indicator);
        actionButtonsContainer = findViewById(R.id.action_buttons_container);
    }

    private void initializeActivityResultLaunchers() {
        // Gallery image picker launcher
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

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            if (imageBitmap != null) {
                                handleCapturedImage(imageBitmap);
                            }
                        }
                    }
                }
        );

        // Storage permission launcher
        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        showPermissionDeniedMessage("storage access");
                    }
                }
        );

        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        showPermissionDeniedMessage("camera access");
                    }
                }
        );
    }

    private void setupClickListeners() {
        btnChangePhoto.setOnClickListener(v -> showImageSourceDialog());
        btnTakePhoto.setOnClickListener(v -> checkCameraPermissionAndOpen());
        btnChooseGallery.setOnClickListener(v -> checkStoragePermissionAndOpenGallery());
        btnSave.setOnClickListener(v -> saveProfileImage());
        btnCancel.setOnClickListener(v -> cancelChanges());
    }

    private void showImageSourceDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Choose Photo Source")
                .setMessage("How would you like to add your profile picture?")
                .setPositiveButton("Camera", (dialog, which) -> checkCameraPermissionAndOpen())
                .setNegativeButton("Gallery", (dialog, which) -> checkStoragePermissionAndOpenGallery())
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void checkStoragePermissionAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Picture"));
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSelectedImage(Uri imageUri) {
        try {
            showLoading(true);
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                processAndDisplayImage(bitmap);
            }
        } catch (FileNotFoundException e) {
            Log.e("ImageSelection", "Error loading image: " + e.getMessage());
            showError("Error loading image. Please try again.");
        } finally {
            showLoading(false);
        }
    }

    private void handleCapturedImage(Bitmap bitmap) {
        showLoading(true);
        processAndDisplayImage(bitmap);
        showLoading(false);
    }

    private void processAndDisplayImage(Bitmap bitmap) {
        // Resize bitmap to reduce size and improve performance
        Bitmap resizedBitmap = resizeBitmap(bitmap, 400, 400);

        // Add subtle animation
        profileImage.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        // Set the image to ImageView
        profileImage.setImageBitmap(resizedBitmap);
        profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Convert to Base64
        profileImageBase64 = bitmapToBase64(resizedBitmap);
        imageChanged = true;

        // Update UI
        updateUIAfterImageSelection();
    }

    private void updateUIAfterImageSelection() {
        statusText.setText("Image selected! Save to apply changes.");
        statusText.setTextColor(ContextCompat.getColor(this, R.color.progress_color));

        // Show save/cancel buttons with animation
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

        showLoading(true);
        statusText.setText("Saving profile picture...");

        FirebaseFirestore.getInstance()
                .collection("User")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .update("photo", profileImageBase64)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    imageChanged = false;
                    originalImageBase64 = profileImageBase64;

                    // Update UI
                    statusText.setText("Profile picture updated successfully!");
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.progress_color));
                    actionButtonsContainer.setVisibility(View.GONE);

                    // Show success snackbar
                    Snackbar.make(findViewById(android.R.id.content),
                                    "Profile picture saved successfully!",
                                    Snackbar.LENGTH_LONG)
                            .setAction("DONE", v -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e("Firebase", "Error updating profile picture: " + e.getMessage());
                    showError("Failed to save profile picture. Please try again.");
                });
    }

    private void cancelChanges() {
        if (imageChanged) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Discard Changes?")
                    .setMessage("You have unsaved changes. Are you sure you want to discard them?")
                    .setPositiveButton("Discard", (dialog, which) -> {
                        resetToOriginalImage();
                    })
                    .setNegativeButton("Keep Editing", null)
                    .show();
        } else {
            finish();
        }
    }

    private void resetToOriginalImage() {
        // Reset to original image or placeholder
        if (!originalImageBase64.isEmpty()) {
            byte[] decodedBytes = Base64.decode(originalImageBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            profileImage.setImageBitmap(bitmap);
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
        showLoading(true);

        FirebaseFirestore.getInstance()
                .collection("User")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists() && documentSnapshot.contains("photo")) {
                        String photoBase64 = documentSnapshot.getString("photo");
                        if (photoBase64 != null && !photoBase64.isEmpty()) {
                            originalImageBase64 = photoBase64;
                            profileImageBase64 = photoBase64;

                            // Decode and display the image
                            byte[] decodedBytes = Base64.decode(photoBase64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            profileImage.setImageBitmap(bitmap);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e("Firebase", "Error loading profile image: " + e.getMessage());
                });
    }

    private void showLoading(boolean show) {
        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        btnChangePhoto.setEnabled(!show);
        btnTakePhoto.setEnabled(!show);
        btnChooseGallery.setEnabled(!show);
        btnSave.setEnabled(!show);
    }

    private void showError(String message) {
        statusText.setText(message);
        statusText.setTextColor(ContextCompat.getColor(this, R.color.error_color));
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showPermissionDeniedMessage(String permissionType) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs " + permissionType + " permission to change your profile picture. " +
                        "Please grant permission in app settings.")
                .setPositiveButton("OK", null)
                .show();
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
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

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
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
}