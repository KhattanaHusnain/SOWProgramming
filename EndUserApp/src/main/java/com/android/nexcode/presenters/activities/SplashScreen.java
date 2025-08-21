package com.android.nexcode.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.nexcode  .R;
import com.android.nexcode.utils.NetworkUtils;
import com.android.nexcode.utils.UserAuthenticationUtils;

public class SplashScreen extends AppCompatActivity {
    // UI elements
    private ImageView logo;
    private TextView appName;
    private TextView tagline;
    private TextView versionText;
    private ProgressBar loadingIndicator;
    private UserAuthenticationUtils userAuthenticationUtils;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply the splash screen theme
        setTheme(R.style.SplashTheme);
        setContentView(R.layout.activity_splash_screen);
        // Initialize UI elements
        initialize();
        // Check and load data in background

        // Navigate after delay

        new Thread(() -> {
            try {
                Thread.sleep(3000); // Increased to 3 seconds to give more time for data loading
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            goToNextScreen();
        }).start();
    }
    private void initialize() {
        logo = findViewById(R.id.logo);
        appName = findViewById(R.id.app_name);
        tagline = findViewById(R.id.tagline);
        versionText = findViewById(R.id.version_text);
        loadingIndicator = findViewById(R.id.loading_indicator);
        userAuthenticationUtils = new UserAuthenticationUtils(this);
    }
    private void goToNextScreen() {
        // Only proceed if we haven't already navigated away
        if(!NetworkUtils.isNetworkAvailable(this)) {
            Intent intent = new Intent(this, OfflineCourseActivity.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = userAuthenticationUtils.isUserLoggedIn()
                    ? new Intent(this, Main.class)
                    : new Intent(this, Authentication.class);
            startActivity(intent);
            finish();
        }
    }
}