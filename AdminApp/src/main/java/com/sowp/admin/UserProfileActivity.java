package com.sowp.admin;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";

    // UI Components
    private ImageView ivProfileLarge, ivProfileVerification;
    private TextView tvProfileName, tvProfileEmail, tvProfileRole;
    private TextView tvPhone, tvBirthdate, tvGenderDetail;
    private TextView tvDegreeDetail, tvSemesterDetail;
    private TextView tvEnrolledCount, tvAssignmentAvg, tvQuizAvg;
    private TextView tvCreatedDate;
    private Button btnDeleteUser;

    // Firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    // Data
    private String userEmail;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
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
        setupListeners();
        loadUserData();
    }

    private void initializeViews() {
        // Profile header
        ivProfileLarge = findViewById(R.id.iv_profile_large);
        ivProfileVerification = findViewById(R.id.iv_profile_verification);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileEmail = findViewById(R.id.tv_profile_email);
        tvProfileRole = findViewById(R.id.tv_profile_role);

        // Personal information
        tvPhone = findViewById(R.id.tv_phone);
        tvBirthdate = findViewById(R.id.tv_birthdate);
        tvGenderDetail = findViewById(R.id.tv_gender_detail);

        // Academic information
        tvDegreeDetail = findViewById(R.id.tv_degree_detail);
        tvSemesterDetail = findViewById(R.id.tv_semester_detail);

        // Statistics
        tvEnrolledCount = findViewById(R.id.tv_enrolled_count);
        tvAssignmentAvg = findViewById(R.id.tv_assignment_avg);
        tvQuizAvg = findViewById(R.id.tv_quiz_avg);

        // Account info
        tvCreatedDate = findViewById(R.id.tv_created_date);
        btnDeleteUser = findViewById(R.id.btn_delete_user);
    }

    private void setupFirebase() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private void setupListeners() {
        btnDeleteUser.setOnClickListener(v -> showDeleteConfirmation());
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
        tvProfileName.setText(currentUser.getDisplayName());
        tvProfileEmail.setText(currentUser.getEmail());
        tvProfileRole.setText(currentUser.getRole());

        // Set role background
        setRoleBackground(currentUser.getRole());

        // Personal information
        tvPhone.setText(currentUser.getDisplayPhone());
        tvBirthdate.setText(currentUser.getDisplayBirthdate());
        tvGenderDetail.setText(currentUser.getDisplayGender());

        // Academic information
        tvDegreeDetail.setText(currentUser.getDisplayDegree());
        tvSemesterDetail.setText(currentUser.getDisplaySemester());

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

    private void setRoleBackground(String role) {
        int backgroundColor;
        switch (role.toLowerCase()) {
            case "admin":
                backgroundColor = getResources().getColor(R.color.role_admin, null);
                break;
            case "instructor":
                backgroundColor = getResources().getColor(R.color.role_instructor, null);
                break;
            case "user":
            default:
                backgroundColor = getResources().getColor(R.color.role_user, null);
                break;
        }
        tvProfileRole.setBackgroundColor(backgroundColor);
    }

    private void setProfileImage(String base64Image) {
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                String cleanBase64 = base64Image;
                if (base64Image.contains(",")) {
                    cleanBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
                }

                byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                if (bitmap != null) {
                    ivProfileLarge.setImageBitmap(bitmap);
                } else {
                    ivProfileLarge.setImageResource(R.drawable.ic_person);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ivProfileLarge.setImageResource(R.drawable.ic_person);
            }
        } else {
            ivProfileLarge.setImageResource(R.drawable.ic_person);
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
                getResources().getColor(R.color.error_color, null));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                getResources().getColor(R.color.primary_color, null));
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

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}