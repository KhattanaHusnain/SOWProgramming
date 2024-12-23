package com.android.nexcode;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class Login extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    Button loginButton;
    FirebaseAuth mAuth;
    EditText email_input, password_input;
    ImageButton back_button;
    ImageView facebook, google;
    SignInClient oneTapClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginButton = findViewById(R.id.login_button);
        email_input = findViewById(R.id.email_input);
        password_input = findViewById(R.id.password_input);
        back_button = findViewById(R.id.back_button);
        facebook = findViewById(R.id.facebook);
        google = findViewById(R.id.google);

        mAuth = FirebaseAuth.getInstance();

        // Initialize Credential Manager
        oneTapClient = Identity.getSignInClient(this);

        google.setOnClickListener(v -> signInWithGoogle());

        loginButton.setOnClickListener(v -> {
            String email = email_input.getText().toString().trim();
            String password = password_input.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(Login.this, Home.class);
                    startActivity(intent);
                    finish(); // Optional: Prevent returning to the login screen
                } else {
                    Log.w(TAG, "signInWithEmailAndPassword:failure", task.getException());
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        back_button.setOnClickListener(v -> finish());
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
                                0,
                                0,
                                0
                        );
                    } catch (Exception e) {
                        Log.e(TAG, "Could not start sign-in intent", e);
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Google Sign-In failed", e);
                    Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();

                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken);
                } else {
                    Log.e(TAG, "No ID token!");
                }
            } catch (Exception e) {
                Log.e(TAG, "Google Sign-In failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Login.this, Home.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
        }
    }
}
