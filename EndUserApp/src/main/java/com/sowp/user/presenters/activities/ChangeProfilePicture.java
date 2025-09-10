package com.sowp.user.presenters.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.sowp.user.R;
import com.sowp.user.services.ImageService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChangeProfilePicture extends AppCompatActivity implements ImageService.ImageCallback {

    private CircleImageView profileImage;
    private FloatingActionButton btnChangePhoto;
    private MaterialButton btnTakePhoto, btnChooseGallery, btnSave, btnCancel;
    private TextView statusText;
    private ProgressBar progressBar;
    private View actionButtonsContainer;

    private String profileImageBase64 = "";
    private String originalImageBase64 = "";
    private boolean imageChanged = false;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ImageService imageService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_profile_picture);

        initializeFirebase();
        initializeViews();
        setupToolbar();
        initializeImageService();
        setupClickListeners();
        loadCurrentProfileImage();
    }

    private void initializeFirebase() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeImageService() {
        imageService = new ImageService(this, this);
    }

    private void setupClickListeners() {
        btnChangePhoto.setOnClickListener(v -> imageService.requestPermissions());
        btnTakePhoto.setOnClickListener(v -> imageService.openCamera());
        btnChooseGallery.setOnClickListener(v -> imageService.openGallery());
        btnSave.setOnClickListener(v -> saveProfileImage());
        btnCancel.setOnClickListener(v -> cancelChanges());
    }

    // ImageService.ImageCallback implementation
    @Override
    public void onImageSelected(String base64String) {
        showLoading(true);
        statusText.setText("Processing image...");

        new Thread(() -> {
            try {
                // Convert base64 to bitmap for display
                Bitmap bitmap = ImageService.base64ToBitmap(base64String);

                if (bitmap != null) {
                    runOnUiThread(() -> {
                        profileImage.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
                        profileImage.setImageBitmap(bitmap);

                        profileImageBase64 = base64String;
                        imageChanged = true;
                        showLoading(false);
                        updateUIAfterImageSelection();
                    });
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showError("Failed to process image");
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Error processing image");
                });
            }
        }).start();
    }

    @Override
    public void onError(String error) {
        showError(error);
    }

    @Override
    public void onPermissionDenied() {
        showPermissionDeniedMessage();
    }

    private void updateUIAfterImageSelection() {
        statusText.setText("Image selected! Save to apply changes.");
        statusText.setTextColor(ContextCompat.getColor(this, R.color.primary));

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
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
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
            Bitmap bitmap = ImageService.base64ToBitmap(originalImageBase64);
            if (bitmap != null) {
                profileImage.setImageBitmap(bitmap);
            } else {
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

                            Bitmap bitmap = ImageService.base64ToBitmap(photoBase64);
                            if (bitmap != null) {
                                profileImage.setImageBitmap(bitmap);
                            }
                            statusText.setText("Tap the camera icon to change your photo");
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
                    android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        if (imageService != null) {
            imageService.onDestroy();
        }
    }
}