package com.android.nexcode.authentication;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.nexcode.R;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class Login extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private static final int RC_FACEBOOK_SIGN_IN = 101;
    private static final int RC_APPLE_SIGN_IN = 102;

    // UI Elements
    private TextInputLayout emailInputLayout, passwordInputLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
    private TextView forgotPassword, signUp, contactUs;
    private MaterialCardView googleCard, facebookCard, appleCard;

    // Firebase
    private FirebaseAuth mAuth;
    private SignInClient oneTapClient;

    // Loading state
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize Google Sign-In
        oneTapClient = Identity.getSignInClient(this);

        // Initialize UI components
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        // Find all views
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        forgotPassword = findViewById(R.id.forget_password);
        signUp = findViewById(R.id.sign_up);
        contactUs = findViewById(R.id.contact_us);
        googleCard = findViewById(R.id.google_card);
    }

    private void setupClickListeners() {
        // Back button
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        // Login button
        loginButton.setOnClickListener(v -> {
            if (!isLoading) {
                validateAndLogin();
            }
        });

        // Forgot password
        forgotPassword.setOnClickListener(v -> {
            // Navigate to forgot password screen
            Intent intent = new Intent(Login.this, ForgotPassword.class);
            startActivity(intent);
        });

        // Sign up
        signUp.setOnClickListener(v -> {
            // Navigate to sign up screen
            Intent intent = new Intent(Login.this, SignUp.class);
            startActivity(intent);
        });

        // Contact us
        contactUs.setOnClickListener(v -> {
            // Navigate to contact screen or show contact dialog
            Toast.makeText(this, "Contact support at support@nexcode.com", Toast.LENGTH_LONG).show();
        });

        // Social login buttons
        googleCard.setOnClickListener(v -> {
            if (!isLoading) {
                setLoading(true);
                signInWithGoogle();
            }
        });




    }

    private void validateAndLogin() {
        // Clear previous errors
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);

        // Get input values
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate email
        if (email.isEmpty()) {
            emailInputLayout.setError("Email is required");
            return;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.setError("Please enter a valid email address");
            return;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordInputLayout.setError("Password is required");
            return;
        } else if (password.length() < 6) {
            passwordInputLayout.setError("Password must be at least 6 characters");
            return;
        }

        // All validations passed, proceed with login
        setLoading(true);
        signInWithEmailPassword(email, password);
    }

    private void signInWithEmailPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // Sign in fails
                        Log.w(TAG, "signInWithEmail:failure", task.getException());

                        // Determine error type and show appropriate message
                        String errorMessage = "Authentication failed";
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("password")) {
                                    passwordInputLayout.setError("Incorrect password");
                                    errorMessage = "Incorrect password";
                                } else if (exceptionMessage.contains("user")) {
                                    emailInputLayout.setError("User not found");
                                    errorMessage = "User not found";
                                }
                            }
                        }

                        Toast.makeText(Login.this, errorMessage, Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void signInWithGoogle() {
        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                .setSupported(true)
                                .setServerClientId(getString(R.string.default_web_client_id))
                                .setFilterByAuthorizedAccounts(false)
                                .build())
                .build();

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                RC_SIGN_IN,
                                null,
                                0, 0, 0
                        );
                    } catch (Exception e) {
                        Log.e(TAG, "Could not start Google sign-in intent", e);
                        setLoading(false);
                        Toast.makeText(this, "Google Sign-In failed to start", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Google Sign-In failed", e);
                    setLoading(false);
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void signInWithFacebook() {
        // Facebook login implementation
        // This is a placeholder - you would need to implement Facebook login with Firebase
        // using the Facebook SDK

        // For now, we'll just show a message
        setLoading(false);
        Toast.makeText(this, "Facebook login will be implemented soon", Toast.LENGTH_SHORT).show();
    }

    private void signInWithApple() {
        // Apple login implementation
        // This is a placeholder - you would need to implement Apple login with Firebase

        // For now, we'll just show a message
        setLoading(false);
        Toast.makeText(this, "Apple login will be implemented soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();

                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken);
                } else {
                    Log.e(TAG, "No ID token!");
                    setLoading(false);
                    Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Google Sign-In failed", e);
                setLoading(false);
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // Show welcome message with user's name if available
            String displayName = user.getDisplayName();
            String welcomeMessage = displayName != null && !displayName.isEmpty()
                    ? "Welcome " + displayName
                    : "Login successful";

            Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show();

            // Navigate to main activity
            Intent intent = new Intent(Login.this, Main.class);
            startActivity(intent);
            finish(); // Close login activity
        }
    }

    private void setLoading(boolean loading) {
        isLoading = loading;

        // Update UI to show loading state
        if (loading) {
            loginButton.setText("Logging in...");
            loginButton.setEnabled(false);
            emailInput.setEnabled(false);
            passwordInput.setEnabled(false);
        } else {
            loginButton.setText("Login");
            loginButton.setEnabled(true);
            emailInput.setEnabled(true);
            passwordInput.setEnabled(true);
        }
    }
}