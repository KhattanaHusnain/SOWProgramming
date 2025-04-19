package com.android.nexcode.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.android.nexcode.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignUp extends AppCompatActivity {

    private ImageButton backButton;
    private TextInputEditText nameInput, emailInput, passwordInput, confirmPasswordInput;
    private CheckBox termsCheckbox;
    private AppCompatButton signupButton;
    private CardView googleButton;
    private TextView loginLink;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI components
        initializeViews();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        // Find all views by their IDs
        backButton = findViewById(R.id.back_button);
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        termsCheckbox = findViewById(R.id.terms_checkbox);
        signupButton = findViewById(R.id.signup_button);
        googleButton = findViewById(R.id.google_button);
        loginLink = findViewById(R.id.login_link);
    }

    private void setupClickListeners() {
        // Back button - return to previous screen
        backButton.setOnClickListener(v -> finish());

        // Sign Up button - validate and create account
        signupButton.setOnClickListener(v -> validateAndCreateAccount());

        // Google sign-in button
        googleButton.setOnClickListener(v -> {
            // Implement Google Sign In functionality here
            Toast.makeText(SignUp.this, "Google Sign In coming soon", Toast.LENGTH_SHORT).show();
        });

        // Login link - go to sign in screen
        loginLink.setOnClickListener(v -> {
            // Navigate to login activity
            Intent intent = new Intent(SignUp.this, Login.class);
            startActivity(intent);
            finish();
        });
    }

    private void validateAndCreateAccount() {
        // Get input values
        String fullName = Objects.requireNonNull(nameInput.getText()).toString().trim();
        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(confirmPasswordInput.getText()).toString().trim();

        // Validate inputs
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms and Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress indicator (you might want to add a progress bar in your layout)
        signupButton.setEnabled(false);

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();

                        // Set user display name
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build();

                        if (user != null) {
                            // Update profile
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        // Save user data to Firestore
                                        saveUserToFirestore(user.getUid(), fullName, email);
                                    });
                        }
                    } else {
                        // If sign up fails, display a message to the user
                        Toast.makeText(SignUp.this,
                                Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();

                        // Re-enable the signup button
                        signupButton.setEnabled(true);
                    }
                });
    }

    private void saveUserToFirestore(String userId, String fullName, String email) {
        // Create a new user with the provided data
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());

        // Add a new document with the user ID
        firestore.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    // Navigate to main activity after successful data saving
                    Intent intent = new Intent(SignUp.this, Main.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    Toast.makeText(SignUp.this, "Welcome " + fullName, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    // If Firestore operation fails
                    Toast.makeText(SignUp.this,
                            "Error saving user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Re-enable the signup button
                    signupButton.setEnabled(true);
                });
    }
}