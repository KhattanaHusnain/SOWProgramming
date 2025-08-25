package com.sowp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    private CardView cardCourseManagement;
    private CardView cardUserManagement;
    private CardView cardQuizManagement;
    private CardView cardAssignmentManagement;
    private CardView cardChatManagement;
    private CardView cardNotificationManagement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize cards
        initializeViews();

        // Set click listeners
        setClickListeners();
    }

    private void initializeViews() {
        cardCourseManagement = findViewById(R.id.cardCourseManagement);
        cardUserManagement = findViewById(R.id.cardUserManagement);
        cardQuizManagement = findViewById(R.id.cardQuizManagement);
        cardAssignmentManagement = findViewById(R.id.cardAssignmentManagement);
        cardChatManagement = findViewById(R.id.cardChatManagement);
        cardNotificationManagement = findViewById(R.id.cardNotificationManagement);
    }

    private void setClickListeners() {

        // Course Management Click Listener
        cardCourseManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CoursesManagementActivity.class);
                startActivity(intent);
            }
        });

        // User Management Click Listener
        cardUserManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, UserManagementActivity.class);
                startActivity(intent);
            }
        });

        // Quiz Management Click Listener
        cardQuizManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, QuizManagementActivity.class);
                startActivity(intent);
                showToast("Opening Quiz Management");
            }
        });

        // Assignment Management Click Listener
        cardAssignmentManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, AssignmentManagementActivity.class);
//                startActivity(intent);
                showToast("Opening Assignment Management");
            }
        });

        // Chat Management Click Listener
        cardChatManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, ChatManagementActivity.class);
//                startActivity(intent);
                showToast("Opening Chat Management");
            }
        });

        // Notification Management Click Listener
        cardNotificationManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, NotificationManagementActivity.class);
//                startActivity(intent);
                showToast("Opening Notification Management");
            }
        });
    }

    // Method to show toast messages
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}