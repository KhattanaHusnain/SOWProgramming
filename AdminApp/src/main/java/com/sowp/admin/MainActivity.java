package com.sowp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnCourseManagement;
    private Button btnUserManagement;
    private Button btnQuizManagement;
    private Button btnAssignmentManagement;
    private Button btnChatManagement;
    private Button btnNotificationManagement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize buttons
        initializeViews();

        // Set click listeners
        setClickListeners();
    }

    private void initializeViews() {
        btnCourseManagement = findViewById(R.id.btnCourseManagement);
        btnUserManagement = findViewById(R.id.btnUserManagement);
        btnQuizManagement = findViewById(R.id.btnQuizManagement);
        btnAssignmentManagement = findViewById(R.id.btnAssignmentManagement);
        btnChatManagement = findViewById(R.id.btnChatManagement);
        btnNotificationManagement = findViewById(R.id.btnNotificationManagement);
    }

    private void setClickListeners() {

        // Course Management Click Listener
        btnCourseManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CoursesManagementActivity.class);
                startActivity(intent);
            }
        });

        // User Management Click Listener
        btnUserManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, UserManagementActivity.class);
//                startActivity(intent);
                showToast("Opening User Management");
            }
        });

        // Quiz Management Click Listener
        btnQuizManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, QuizManagementActivity.class);
                startActivity(intent);
                showToast("Opening Quiz Management");
            }
        });

        // Assignment Management Click Listener
        btnAssignmentManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, AssignmentManagementActivity.class);
//                startActivity(intent);
                showToast("Opening Assignment Management");
            }
        });

        // Chat Management Click Listener
        btnChatManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(MainActivity.this, ChatManagementActivity.class);
//                startActivity(intent);
                showToast("Opening Chat Management");
            }
        });

        // Notification Management Click Listener
        btnNotificationManagement.setOnClickListener(new View.OnClickListener() {
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