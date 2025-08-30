package com.sowp.admin;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";
    private static final int IMAGE_PICK_REQUEST = 1000;

    // UI Components
    private ImageView ivProfileLarge, ivProfileVerification;
    private TextInputEditText etProfileName, etBirthdate, etDegree, etSemester;
    private AutoCompleteTextView spinnerRole, spinnerGender;
    private TextView tvProfileEmail;
    private TextView tvEnrolledCount, tvAssignmentAvg, tvQuizAvg;
    private TextView tvCreatedDate;
    private MaterialButton btnSaveChanges, btnDeleteUser;
    private FloatingActionButton fabEditProfileImage;

    // Firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    // Data
    private String userEmail;
    private User currentUser;
    private String selectedImageBase64;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);


        // Get user email from intent
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Invalid user email", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupFirebase();
        setupActivityResultLaunchers();
        setupListeners();
        setupDropdowns();
        loadUserData();
    }

    private void initializeViews() {
        // Profile header
        ivProfileLarge = findViewById(R.id.iv_profile_large);
        ivProfileVerification = findViewById(R.id.iv_profile_verification);
        etProfileName = findViewById(R.id.et_profile_name);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        spinnerRole = findViewById(R.id.spinner_role);
        fabEditProfileImage = findViewById(R.id.fab_edit_profile_image);

        // Personal information
        etBirthdate = findViewById(R.id.et_birthdate);
        spinnerGender = findViewById(R.id.spinner_gender);

        // Academic information
        etDegree = findViewById(R.id.et_degree);
        etSemester = findViewById(R.id.et_semester);

        // Statistics
        tvEnrolledCount = findViewById(R.id.tv_enrolled_count);
        tvAssignmentAvg = findViewById(R.id.tv_assignment_avg);
        tvQuizAvg = findViewById(R.id.tv_quiz_avg);

        // Account info
        tvCreatedDate = findViewById(R.id.tv_created_date);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        btnDeleteUser = findViewById(R.id.btn_delete_user);
    }

    private void setupFirebase() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private void setupActivityResultLaunchers() {
        // Image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                                // Resize bitmap if too large
                                bitmap = resizeBitmap(bitmap, 300, 300);
                                selectedImageBase64 = bitmapToBase64(bitmap);
                                ivProfileLarge.setImageBitmap(bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void setupListeners() {
        // Profile image click for full screen view
        ivProfileLarge.setOnClickListener(v -> showFullScreenImage());

        // Edit profile image FAB
        fabEditProfileImage.setOnClickListener(v -> selectProfileImage());

        // Birth date picker
        etBirthdate.setOnClickListener(v -> showDatePicker());

        // Action buttons
        btnSaveChanges.setOnClickListener(v -> saveChanges());
        btnDeleteUser.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void setupDropdowns() {
        // Role dropdown
        String[] roles = {"User", "Instructor", "Admin"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, roles);
        spinnerRole.setAdapter(roleAdapter);

        // Gender dropdown
        String[] genders = {"Male", "Female", "Other", "Prefer not to say"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genders);
        spinnerGender.setAdapter(genderAdapter);
    }

    private void showFullScreenImage() {
        // Create full screen dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fullscreen_image, null);
        ImageView fullScreenImage = dialogView.findViewById(R.id.iv_fullscreen_image);

        // Set the same image as profile
        if (selectedImageBase64 != null && !selectedImageBase64.isEmpty()) {
            try {
                String cleanBase64 = selectedImageBase64;
                if (selectedImageBase64.contains(",")) {
                    cleanBase64 = selectedImageBase64.substring(selectedImageBase64.indexOf(",") + 1);
                }
                byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                fullScreenImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                fullScreenImage.setImageResource(R.drawable.ic_person);
            }
        } else if (currentUser != null && currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
            setProfileImageFromBase64(fullScreenImage, currentUser.getPhoto());
        } else {
            fullScreenImage.setImageResource(R.drawable.ic_person);
        }

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Close on image click
        fullScreenImage.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void selectProfileImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Parse current date if exists
        String currentDate = etBirthdate.getText().toString();
        if (!currentDate.equals("Not provided") && !currentDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                calendar.setTime(sdf.parse(currentDate));
            } catch (Exception e) {
                // Use current date if parsing fails
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etBirthdate.setText(sdf.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void saveChanges() {
        if (currentUser == null) return;

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");

        // Get updated values from UI
        String updatedName = etProfileName.getText().toString().trim();
        String updatedRole = spinnerRole.getText().toString().trim();
        String updatedBirthdate = etBirthdate.getText().toString().trim();
        String updatedGender = spinnerGender.getText().toString().trim();
        String updatedDegree = etDegree.getText().toString().trim();
        String updatedSemester = etSemester.getText().toString().trim();

        // Validate required fields
        if (updatedName.isEmpty()) {
            etProfileName.setError("Name is required");
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setText("Save Changes");
            return;
        }

        // Create update map
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", updatedName);
        updates.put("role", updatedRole);
        updates.put("birthdate", updatedBirthdate.equals("Not provided") ? "" : updatedBirthdate);
        updates.put("gender", updatedGender.equals("Not specified") ? "" : updatedGender);
        updates.put("degree", updatedDegree.equals("Not specified") ? "" : updatedDegree);

        // Handle semester - store as string to match User model
        if (!updatedSemester.equals("Not specified") && !updatedSemester.isEmpty()) {
            updates.put("semester", updatedSemester);
        } else {
            updates.put("semester", "");
        }

        // Add image if changed
        if (selectedImageBase64 != null) {
            updates.put("photo", selectedImageBase64);
        }

        updates.put("lastModified", System.currentTimeMillis());

        // Update in Firestore
        firestore.collection("User")
                .document(userEmail)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");

                    // Update current user object
                    updateCurrentUserObject(updates);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                });
    }

    private void updateCurrentUserObject(Map<String, Object> updates) {
        if (currentUser != null) {
            if (updates.containsKey("fullName")) {
                currentUser.setFullName((String) updates.get("fullName"));
            }
            if (updates.containsKey("role")) {
                currentUser.setRole((String) updates.get("role"));
            }
            if (updates.containsKey("birthdate")) {
                currentUser.setBirthdate((String) updates.get("birthdate"));
            }
            if (updates.containsKey("gender")) {
                currentUser.setGender((String) updates.get("gender"));
            }
            if (updates.containsKey("degree")) {
                currentUser.setDegree((String) updates.get("degree"));
            }
            if (updates.containsKey("semester")) {
                // Convert to string since User model expects string
                Object semesterObj = updates.get("semester");
                if (semesterObj instanceof Integer) {
                    currentUser.setSemester(String.valueOf(semesterObj));
                } else if (semesterObj instanceof String) {
                    currentUser.setSemester((String) semesterObj);
                }
            }
            if (updates.containsKey("photo")) {
                currentUser.setPhoto((String) updates.get("photo"));
            }

            checkEmailVerificationStatus();
        }
    }

    private void loadUserData() {
        firestore.collection("User")
                .document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = User.fromDocument(documentSnapshot);
                        if (currentUser != null) {
                            populateUserData();
                            checkEmailVerificationStatus();
                        } else {
                            showError("Failed to parse user data");
                        }
                    } else {
                        showError("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Failed to load user data: " + e.getMessage());
                });
    }

    private void checkEmailVerificationStatus() {
        boolean isVerified = !currentUser.getFullName().isEmpty() &&
                !currentUser.getDisplayPhone().isEmpty();
        currentUser.setEmailVerified(isVerified);
        updateVerificationStatus();
    }

    private void populateUserData() {
        // Profile header
        etProfileName.setText(currentUser.getDisplayName());
        tvProfileEmail.setText(currentUser.getEmail());
        spinnerRole.setText(currentUser.getRole(), false);

        // Personal information
        etBirthdate.setText(currentUser.getDisplayBirthdate());
        spinnerGender.setText(currentUser.getDisplayGender(), false);

        // Academic information
        etDegree.setText(currentUser.getDisplayDegree());
        etSemester.setText(currentUser.getDisplaySemester());

        // Statistics
        tvEnrolledCount.setText(String.valueOf(currentUser.getEnrolledCoursesCount()));
        tvAssignmentAvg.setText(String.format("%.1f", currentUser.getAssignmentAvg()));
        tvQuizAvg.setText(String.format("%.1f", currentUser.getQuizzesAvg()));

        // Account info
        tvCreatedDate.setText(currentUser.getFormattedCreatedDate());

        // Profile image
        setProfileImage(currentUser.getPhoto());
    }

    private void updateVerificationStatus() {
        if (currentUser.isEmailVerified()) {
            ivProfileVerification.setVisibility(View.VISIBLE);
            ivProfileVerification.setImageResource(R.drawable.ic_verified);
        } else {
            ivProfileVerification.setVisibility(View.GONE);
        }
    }

    private void setProfileImage(String base64Image) {
        setProfileImageFromBase64(ivProfileLarge, base64Image);
    }

    private void setProfileImageFromBase64(ImageView imageView, String base64Image) {
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                String cleanBase64 = base64Image;
                if (base64Image.contains(",")) {
                    cleanBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
                }

                byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(R.drawable.ic_person);
                }
            } catch (Exception e) {
                e.printStackTrace();
                imageView.setImageResource(R.drawable.ic_person);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_person);
        }
    }

    private void showDeleteConfirmation() {
        if (currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete User Account");
        builder.setMessage("Are you sure you want to delete " + currentUser.getDisplayName() + "'s account?\n\n" +
                "This will:\n" +
                "• Delete the user from Firebase Authentication\n" +
                "• Remove all user data from Firestore\n" +
                "• Use stored credentials for authentication\n\n" +
                "This action cannot be undone!");
        builder.setIcon(R.drawable.ic_delete);

        builder.setPositiveButton("Delete Account", (dialog, which) -> deleteUserAccount());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.setCancelable(true);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize button colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                getResources().getColor(android.R.color.holo_red_dark, null));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                getResources().getColor(android.R.color.holo_blue_dark, null));
    }

    private void deleteUserAccount() {
        btnDeleteUser.setEnabled(false);
        btnDeleteUser.setText("Deleting...");

        // Get the stored password from Firestore
        getUserPasswordAndDelete();
    }

    private void getUserPasswordAndDelete() {
        firestore.collection("User")
                .document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedPassword = documentSnapshot.getString("password");

                        if (storedPassword != null && !storedPassword.isEmpty()) {
                            // Use the stored password to sign in and delete
                            signInUserAndDelete(storedPassword);
                        } else {
                            // No password stored, show error
                            btnDeleteUser.setEnabled(true);
                            btnDeleteUser.setText("Delete User");
                            Toast.makeText(this, "No password found for this user. Cannot delete from Authentication.",
                                    Toast.LENGTH_LONG).show();

                            // Offer to delete data only
                            showDataOnlyDeletionOption();
                        }
                    } else {
                        btnDeleteUser.setEnabled(true);
                        btnDeleteUser.setText("Delete User");
                        Toast.makeText(this, "User document not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnDeleteUser.setEnabled(true);
                    btnDeleteUser.setText("Delete User");
                    Toast.makeText(this, "Failed to retrieve user password: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showDataOnlyDeletionOption() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Authentication Deletion Not Possible");
        builder.setMessage("No password is stored for this user, so they cannot be deleted from Firebase Authentication.\n\n" +
                "You can still remove their data from Firestore. The authentication account will remain active.\n\n" +
                "Do you want to proceed with data removal only?");

        builder.setPositiveButton("Remove Data Only", (dialog, which) -> deleteFromFirestore());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void signInUserAndDelete(String password) {
        // Temporarily sign in as the user to be deleted
        auth.signInWithEmailAndPassword(userEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser userToDelete = authResult.getUser();
                    if (userToDelete != null) {
                        // Delete the user account
                        userToDelete.delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User deleted from Authentication successfully");
                                    // Now delete from Firestore
                                    deleteFromFirestore();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to delete user from Authentication", e);
                                    handleAuthDeletionFailure(e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sign in as user for deletion", e);
                    btnDeleteUser.setEnabled(true);
                    btnDeleteUser.setText("Delete User");

                    if (e.getMessage() != null && e.getMessage().contains("password")) {
                        Toast.makeText(this, "Stored password is incorrect or expired.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleAuthDeletionFailure(Exception e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Authentication Deletion Failed");
        builder.setMessage("Could not delete user from Firebase Authentication.\n\n" +
                "Error: " + e.getMessage() + "\n\n" +
                "The user data will be removed from Firestore, but the authentication account will remain active. " +
                "You may need to manually disable the account or ask the user to delete it themselves.\n\n" +
                "Do you want to proceed with removing user data only?");

        builder.setPositiveButton("Remove Data Only", (dialog, which) -> deleteFromFirestore());
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            btnDeleteUser.setEnabled(true);
            btnDeleteUser.setText("Delete User");
        });

        builder.show();
    }

    private void deleteFromFirestore() {
        firestore.collection("User")
                .document(userEmail)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Also create a record of deleted users for audit purposes
                    createDeletedUserRecord();

                    Toast.makeText(this, "User account deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnDeleteUser.setEnabled(true);
                    btnDeleteUser.setText("Delete User");
                    Toast.makeText(this, "Failed to delete user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void createDeletedUserRecord() {
        // Create an audit record of the deletion
        Map<String, Object> deletedRecord = new HashMap<>();
        deletedRecord.put("deletedEmail", userEmail);
        deletedRecord.put("deletedName", currentUser.getDisplayName());
        deletedRecord.put("deletedBy", "Admin"); // Since this is from admin app
        deletedRecord.put("deletedAt", System.currentTimeMillis());
        deletedRecord.put("reason", "Admin deletion");
        deletedRecord.put("deletedFromAuth", true); // Mark that user was deleted from Authentication
        deletedRecord.put("deletedFromFirestore", true); // Mark that user was deleted from Firestore

        firestore.collection("deleted_users")
                .add(deletedRecord)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Deletion audit record created");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to create deletion audit record", e);
                });
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap resizeBitmap(Bitmap original, int maxWidth, int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();

        float aspectRatio = (float) width / height;

        if (width > height) {
            width = maxWidth;
            height = (int) (width / aspectRatio);
        } else {
            height = maxHeight;
            width = (int) (height * aspectRatio);
        }

        return Bitmap.createScaledBitmap(original, width, height, true);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}