package com.android.nexcode.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.nexcode.R;

public class Authorization extends AppCompatActivity implements View.OnClickListener {

    private Button loginButton;
    private Button signupButton;
    private ImageButton googleLoginButton;
    private RelativeLayout authContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);

        // Initialize views
        initViews();

        // Set click listeners
        setClickListeners();

        // Add animation for the auth container
        animateAuthContainer();
    }

    private void initViews() {
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.signup_button);
        googleLoginButton = findViewById(R.id.google_login);
        authContainer = findViewById(R.id.auth_container);
    }

    private void setClickListeners() {
        loginButton.setOnClickListener(this);
        signupButton.setOnClickListener(this);
        googleLoginButton.setOnClickListener(this);
    }

    private void animateAuthContainer() {
        // Fade in animation
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500);
        fadeIn.setStartOffset(300);

        // Slide up animation
        TranslateAnimation slideUp = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.1f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        slideUp.setDuration(500);
        slideUp.setStartOffset(300);

        // Apply animations
        authContainer.startAnimation(fadeIn);
        authContainer.startAnimation(slideUp);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();

        if (viewId == R.id.login_button) {
            navigateToLogin();
        } else if (viewId == R.id.signup_button) {
            navigateToSignup();
        } else if (viewId == R.id.google_login) {
            handleSocialLogin("Google");
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void navigateToSignup() {
        Intent intent = new Intent(this, SignUp.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void handleSocialLogin(String provider) {
        // This would be replaced with actual social login implementation
        Toast.makeText(this, provider + " login clicked", Toast.LENGTH_SHORT).show();

        // For demonstration purposes, we'll show a toast
        // In a real app, you would implement the OAuth flow and handle authentication
    }
}