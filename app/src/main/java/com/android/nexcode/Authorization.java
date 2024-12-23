package com.android.nexcode;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Authorization extends AppCompatActivity {

    Button loginButton;
    Button signupButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorization);
        loginButton = findViewById(R.id.login_button);
        signupButton = findViewById(R.id.signup_button);

        loginButton.setOnClickListener(v -> {
            // Handle login button click
            // You can navigate to the login activity or perform other actions here
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
        });
        signupButton.setOnClickListener(v -> {
            // Handle signup button click
            // You can navigate to the signup activity or perform other actions here
            Intent intent = new Intent(this, SignUp.class);
            startActivity(intent);
        });
    }
}