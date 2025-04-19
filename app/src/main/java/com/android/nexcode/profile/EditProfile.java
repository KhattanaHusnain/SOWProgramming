package com.android.nexcode.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.nexcode.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfile extends AppCompatActivity {
    private static final String TAG = "EditProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI Components
    private CircleImageView profileImageView;
    private FloatingActionButton btnChangePhoto;
    private TextInputEditText editUsername, editEmail, editFullName, editPhone, editBirthdate;
    private SwitchMaterial switchNotifications, switchPrivacy;
    private MaterialButton btnSave;
    private LinearProgressIndicator loadingIndicator;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    // Data
    private String currentProfileImageBase64;
    private boolean isPhotoChanged = false;
    private boolean isNewUser = false;

    // Date formatter
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
    private SimpleDateFormat storageDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile); // Updated to match the XML filename

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "No user is logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initViews();
        setupClickListeners();

        // Load user data from Firestore
        loadUserData();
    }

    private void initViews() {
        // Updated view IDs to match the XML layout
        profileImageView = findViewById(R.id.profile_image);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
        editUsername = findViewById(R.id.edit_username);
        editEmail = findViewById(R.id.edit_email);
        editFullName = findViewById(R.id.edit_full_name);
        editPhone = findViewById(R.id.edit_phone);
        editBirthdate = findViewById(R.id.edit_birthdate);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchPrivacy = findViewById(R.id.switch_privacy);
        btnSave = findViewById(R.id.btn_save);
        loadingIndicator = findViewById(R.id.loading_indicator);
    }

    private void setupClickListeners() {
        // Change profile photo
        btnChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
        });

        // Date picker for birthdate
        editBirthdate.setOnClickListener(v -> showDatePickerDialog());

        // Save button
        btnSave.setOnClickListener(v -> validateAndSaveData());
    }

    private void showDatePickerDialog() {
        // Get current date or saved date if available
        Calendar calendar = Calendar.getInstance();
        Date selectedDate = null;

        try {
            if (editBirthdate.getText() != null && !editBirthdate.getText().toString().isEmpty()) {
                selectedDate = displayDateFormat.parse(editBirthdate.getText().toString());
                calendar.setTime(selectedDate);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date", e);
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(selectedYear, selectedMonth, selectedDay);
                    editBirthdate.setText(displayDateFormat.format(calendar.getTime()));
                },
                year, month, day);

        // Set max date to current date (no future dates)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void loadUserData() {
        showLoading(true);
        String userId = currentUser.getUid();

        firestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            populateFormWithUserData(document);
                        } else {
                            Log.d(TAG, "No such user document - creating a new one");
                            isNewUser = true;
                            createEmptyUserDocument(userId);
                            initializeFormWithDefaultValues();
                        }
                    } else {
                        Log.w(TAG, "Error loading user data", task.getException());
                        Toast.makeText(this, "Error loading profile: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createEmptyUserDocument(String userId) {
        Map<String, Object> userData = new HashMap<>();
        // Initialize with default values from Firebase Auth if available
        if (currentUser.getEmail() != null) {
            userData.put("email", currentUser.getEmail());
        }
        if (currentUser.getDisplayName() != null) {
            userData.put("displayName", currentUser.getDisplayName());
            userData.put("fullName", currentUser.getDisplayName());
        }

        // Default settings
        userData.put("emailNotifications", false);
        userData.put("profilePublic", false);
        userData.put("createdAt", new Date());

        showLoading(true);
        firestore.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Log.d(TAG, "Empty user document created successfully");
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error creating user document", e);
                    Toast.makeText(this, "Error creating user profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void initializeFormWithDefaultValues() {
        // Set default values for a new user
        if (currentUser.getEmail() != null) {
            editEmail.setText(currentUser.getEmail());
        }
        if (currentUser.getDisplayName() != null) {
            editUsername.setText(currentUser.getDisplayName());
            editFullName.setText(currentUser.getDisplayName());
        }

        // Default image
        profileImageView.setImageResource(R.drawable.ic_profile);

        // Default switches
        switchNotifications.setChecked(false);
        switchPrivacy.setChecked(false);

        Toast.makeText(this, "Welcome! Please complete your profile", Toast.LENGTH_LONG).show();
    }

    private void populateFormWithUserData(DocumentSnapshot document) {
        // Basic info
        String userName = document.getString("displayName");
        String email = document.getString("email");
        String fullName = document.getString("fullName");
        String phone = document.getString("phone");
        String birthdate = document.getString("birthdate");
        Boolean notifications = document.getBoolean("emailNotifications");
        Boolean privacy = document.getBoolean("profilePublic");

        // Profile image
        currentProfileImageBase64 = document.getString("profileImageBase64");

        // Update UI
        if (userName != null) editUsername.setText(userName);
        if (email != null) editEmail.setText(email);
        if (fullName != null) editFullName.setText(fullName);
        if (phone != null) editPhone.setText(phone);

        // Format and set birthdate
        if (birthdate != null && !birthdate.isEmpty()) {
            try {
                Date date = storageDateFormat.parse(birthdate);
                if (date != null) {
                    editBirthdate.setText(displayDateFormat.format(date));
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing birthdate", e);
                editBirthdate.setText(birthdate); // Fallback
            }
        }

        // Set switches
        switchNotifications.setChecked(notifications != null ? notifications : false);
        switchPrivacy.setChecked(privacy != null ? privacy : false);

        // Set profile image
        if (currentProfileImageBase64 != null && !currentProfileImageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(currentProfileImageBase64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                profileImageView.setImageBitmap(decodedBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding profile image", e);
                profileImageView.setImageResource(R.drawable.ic_profile);
            }
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile);
        }
    }

    private void validateAndSaveData() {
        // Get values from form
        String username = editUsername.getText() != null ? editUsername.getText().toString().trim() : "";
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String fullName = editFullName.getText() != null ? editFullName.getText().toString().trim() : "";
        String phone = editPhone.getText() != null ? editPhone.getText().toString().trim() : "";
        String birthdate = editBirthdate.getText() != null ? editBirthdate.getText().toString().trim() : "";
        boolean notifications = switchNotifications.isChecked();
        boolean privacy = switchPrivacy.isChecked();

        // Validate required fields
        if (username.isEmpty()) {
            editUsername.setError("Username is required");
            return;
        }

        if (email.isEmpty()) {
            editEmail.setError("Email is required");
            return;
        }

        // Prepare data for saving
        Map<String, Object> userData = new HashMap<>();
        userData.put("displayName", username);
        userData.put("email", email);
        userData.put("fullName", fullName);
        userData.put("phone", phone);
        userData.put("emailNotifications", notifications);
        userData.put("profilePublic", privacy);

        // If this is a new user or this field was updated, add last updated timestamp
        userData.put("lastUpdated", new Date());

        // Convert and store birthdate in standard format
        if (!birthdate.isEmpty()) {
            try {
                Date date = displayDateFormat.parse(birthdate);
                if (date != null) {
                    userData.put("birthdate", storageDateFormat.format(date));
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing birthdate", e);
                // Use as-is if parsing fails
                userData.put("birthdate", birthdate);
            }
        }

        // Add profile image if changed
        if (isPhotoChanged && currentProfileImageBase64 != null) {
            userData.put("profileImageBase64", currentProfileImageBase64);
        }

        // Save to Firestore
        saveUserData(userData);
    }

    private void saveUserData(Map<String, Object> userData) {
        if (currentUser == null) return;

        showLoading(true);
        String userId = currentUser.getUid();

        // For new users use set(), for existing users use update()
        if (isNewUser) {
            firestore.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
                        showLoading(false);
                        Toast.makeText(EditProfile.this, "Profile created successfully", Toast.LENGTH_SHORT).show();

                        // Update Firebase Auth display name
                        String newDisplayName = (String) userData.get("displayName");
                        if (newDisplayName != null && !newDisplayName.isEmpty()) {
                            updateDisplayNameInAuth(newDisplayName);
                        }

                        // If email has been changed, update Firebase Auth email
                        String newEmail = (String) userData.get("email");
                        if (newEmail != null && !newEmail.equals(currentUser.getEmail())) {
                            updateEmailInAuth(newEmail);
                        } else {
                            finish(); // Close activity and return to profile
                        }
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Error creating profile", e);
                        Toast.makeText(EditProfile.this, "Error creating profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            firestore.collection("users").document(userId)
                    .update(userData)
                    .addOnSuccessListener(aVoid -> {
                        showLoading(false);
                        Toast.makeText(EditProfile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                        // If email has been changed, update Firebase Auth email
                        String newEmail = (String) userData.get("email");
                        if (newEmail != null && !newEmail.equals(currentUser.getEmail())) {
                            updateEmailInAuth(newEmail);
                        } else {
                            finish(); // Close activity and return to profile
                        }
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Error updating profile", e);
                        Toast.makeText(EditProfile.this, "Error updating profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateDisplayNameInAuth(String newDisplayName) {
        currentUser.updateProfile(new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(newDisplayName)
                        .build())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Display name updated in Auth");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating display name in Auth", e);
                });
    }

    private void updateEmailInAuth(String newEmail) {
        showLoading(true);
        currentUser.updateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(EditProfile.this, "Email updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error updating email in Auth", e);
                    Toast.makeText(EditProfile.this,
                            "Profile saved but email update failed. You may need to re-authenticate.",
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                // Update UI
                profileImageView.setImageBitmap(bitmap);

                // Convert to Base64
                currentProfileImageBase64 = encodeImageToBase64(bitmap);
                isPhotoChanged = true;

            } catch (IOException e) {
                Log.e(TAG, "Error loading image", e);
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        // Scale down the image if it's too large
        Bitmap resizedBitmap = scaleBitmapToMaxDimension(bitmap, 500);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap scaleBitmapToMaxDimension(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleRatio = Math.min(
                (float) maxDimension / width,
                (float) maxDimension / height
        );

        // Only scale down, not up
        if (scaleRatio >= 1) {
            return bitmap;
        }

        int targetWidth = Math.round(width * scaleRatio);
        int targetHeight = Math.round(height * scaleRatio);

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }

    private void showLoading(boolean show) {
        // Use the LinearProgressIndicator instead of loadingOverlay
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}