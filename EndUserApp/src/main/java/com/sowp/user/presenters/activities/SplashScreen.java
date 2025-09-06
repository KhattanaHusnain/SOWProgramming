package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sowp.user.R;
import com.sowp.user.utils.NetworkUtils;
import com.sowp.user.utils.UserAuthenticationUtils;

public class SplashScreen extends AppCompatActivity {
    private ImageView logo;
    private TextView appName;
    private TextView tagline;
    private TextView versionText;
    private ProgressBar loadingIndicator;
    private UserAuthenticationUtils userAuthenticationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.SplashTheme);
        setContentView(R.layout.activity_splash_screen);

        initialize();

        new Thread(() -> {
            try {
                Thread.sleep(3000);
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
        if(!NetworkUtils.isNetworkAvailable(this)) {
        } else {
            Intent intent = userAuthenticationUtils.isUserLoggedIn()
                    ? new Intent(this, Main.class)
                    : new Intent(this, Authentication.class);
            startActivity(intent);
            finish();
        }
    }
}