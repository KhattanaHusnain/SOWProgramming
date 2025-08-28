package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sowp.user.R;
import com.sowp.user.models.User;
import com.sowp.user.repositories.firebase.UserRepository;

public class Authentication extends AppCompatActivity {

    private Button loginButton;
    private Button signupButton;
    private ImageButton googleLoginButton;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        // Initialize views
        initialize();

        // Set click listeners
        setUpClickListeners();

    }

    private void initialize() {
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.signup_button);
        googleLoginButton = findViewById(R.id.google_login);
        userRepository = new UserRepository(this);
    }

    private void setUpClickListeners() {
        loginButton.setOnClickListener(v -> navigateToLogin());
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToSignup();
            }
        });
        googleLoginButton.setOnClickListener(v -> {
            userRepository.signInWithGoogle(new UserRepository.GoogleSignInCallback() {

                @Override
                public void onSuccess(User user) {
                    navigateToMain();
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(Authentication.this, "Google Sign In Failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        finish();
    }

    private void navigateToSignup() {
        Intent intent = new Intent(this, SignUp.class);
        startActivity(intent);
        finish();
    }

    private void navigateToMain() {
        Intent intent = new Intent(Authentication.this, Main.class);
        startActivity(intent);
        finish();
    }

}