package com.android.nexcode;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignUp extends AppCompatActivity {

    Button signUpButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        signUpButton = findViewById(R.id.signup_button);
        signUpButton.setOnClickListener(v -> {
            // Handle login button click
            // You can navigate to the login activity or perform other actions here
            Intent intent = new Intent(this, Home.class);
            startActivity(intent);
            finish();
        });
    }
}