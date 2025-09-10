package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.sowp.user.R;
import com.sowp.user.models.User;
import com.sowp.user.repositories.UserRepository;

public class Authentication extends AppCompatActivity {

    private MaterialButton loginButton, signupButton;
    private ImageButton googleLoginButton;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.signup_button);
        googleLoginButton = findViewById(R.id.google_login);
        userRepository = new UserRepository(this);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToLogin();
            }
        });
        signupButton.setOnClickListener(v -> navigateToSignup());
        googleLoginButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        userRepository.signInWithGoogle(new UserRepository.GoogleSignInCallback() {
            @Override
            public void onSuccess(User user) {
                navigateToMain();
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() ->
                        Toast.makeText(Authentication.this, message, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, Login.class));
        finish();
    }

    private void navigateToSignup() {
        startActivity(new Intent(this, SignUp.class));
        finish();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, Main.class));
        finish();
    }
}