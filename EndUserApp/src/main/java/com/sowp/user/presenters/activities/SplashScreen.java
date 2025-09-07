package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.sowp.user.R;
import com.sowp.user.services.UserAuthenticationUtils;

public class SplashScreen extends AppCompatActivity {
    private UserAuthenticationUtils userAuthenticationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.SplashTheme);
        setContentView(R.layout.activity_splash_screen);
        userAuthenticationUtils = new UserAuthenticationUtils(this);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                goToNextScreen();
            }
        }, 2000);
    }


    private void goToNextScreen() {
            Intent intent = userAuthenticationUtils.isUserLoggedIn()
                    ? new Intent(this, Main.class)
                    : new Intent(this, Authentication.class);
            startActivity(intent);
            finish();
    }
}