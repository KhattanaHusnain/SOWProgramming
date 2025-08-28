package com.sowp.user.presenters.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sowp.user.R;
import com.sowp.user.utils.UserAuthenticationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPassword extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputEditText emailInput;
    private MaterialButton resetButton;
    private ImageButton backButton;
    private UserAuthenticationUtils userAuthenticationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        initialize();
        setUpClickListeners();
    }

    private void initialize() {
        emailInputLayout = findViewById(R.id.email_input_layout);
        emailInput = findViewById(R.id.email_input);
        resetButton = findViewById(R.id.reset_button);
        backButton = findViewById(R.id.back_button);
        userAuthenticationUtils = new UserAuthenticationUtils(this);
    }

    private void setUpClickListeners() {
        backButton.setOnClickListener(v -> finish());
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
        userAuthenticationUtils.sendResetPasswordLink(email, new UserAuthenticationUtils.Callback() {
            @Override
            public void onSuccess() {
                resetButton.setEnabled(true);
                resetButton.setText("Reset Password");
                Toast.makeText(ForgotPassword.this,
                        "Password reset email sent. Check your inbox.",
                        Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onFailure(String message) {
                resetButton.setEnabled(true);
                resetButton.setText("Reset Password");
                if (message.contains("no user record")) {
                    emailInputLayout.setError("No account found with this email");
                } else {
                    Toast.makeText(ForgotPassword.this,
                            "Failed to send reset email: " + message,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}