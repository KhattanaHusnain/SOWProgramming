package com.android.nexcode.authentication;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.nexcode.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailInput;
    private MaterialButton resetButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        emailInputLayout = findViewById(R.id.email_input_layout);
        emailInput = findViewById(R.id.email_input);
        resetButton = findViewById(R.id.reset_button);

        // Set up click listeners
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        resetButton.setOnClickListener(v -> {
            resetPassword();
        });
    }

    private void resetPassword() {
        // Clear previous errors
        emailInputLayout.setError(null);

        // Get input value
        String email = emailInput.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            emailInputLayout.setError("Email is required");
            return;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address");
            return;
        }

        // Show loading state
        resetButton.setEnabled(false);
        resetButton.setText("Sending...");

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    resetButton.setEnabled(true);
                    resetButton.setText("Reset Password");

                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPassword.this,
                                "Password reset email sent. Check your inbox.",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            String error = task.getException().getMessage();
                            if (error.contains("no user record")) {
                                emailInputLayout.setError("No account found with this email");
                            } else {
                                Toast.makeText(ForgotPassword.this,
                                        "Failed to send reset email: " + error,
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ForgotPassword.this,
                                    "Failed to send reset email",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}