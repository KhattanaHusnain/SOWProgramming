package com.sowp.admin.topicmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.ViewCoursesActivity;

public class TopicManagementActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout cardViewTopics;
    private LinearLayout cardUploadTopic;
    private TextView txtTotalQuizzes;
    private TextView txtActiveQuizzes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_management);

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Load quiz statistics
        loadQuizStatistics();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        cardViewTopics = findViewById(R.id.cardViewTopics);
        cardUploadTopic = findViewById(R.id.cardUploadTopic);
        txtTotalQuizzes = findViewById(R.id.txtTotalQuizzes);
        txtActiveQuizzes = findViewById(R.id.txtActiveQuizzes);
    }

    private void setClickListeners() {

        // Back button click listener
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // View Quizzes List card click listener

        cardViewTopics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TopicManagementActivity.this, ViewCoursesActivity.class);
                intent.putExtra("cameForTopics", true);
                startActivity(intent);
            }
        });

        // Upload New Quiz card click listener
        cardUploadTopic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TopicManagementActivity.this, AddTopicActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadQuizStatistics() {
        // This method would typically load data from a database or API
        // For now, we'll use dummy data

        // Simulate loading quiz statistics
        int totalQuizzes = getTotalQuizzesCount();
        int activeQuizzes = getActiveQuizzesCount();

        // Update UI with the statistics
        txtTotalQuizzes.setText(String.valueOf(totalQuizzes));
        txtActiveQuizzes.setText(String.valueOf(activeQuizzes));
    }

    // Simulate getting total quizzes count from database/API
    private int getTotalQuizzesCount() {
        // TODO: Replace with actual database query or API call
        return 12; // Dummy data
    }

    // Simulate getting active quizzes count from database/API
    private int getActiveQuizzesCount() {
        // TODO: Replace with actual database query or API call
        return 8; // Dummy data
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Optional: Add custom back navigation logic here
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics when returning to this activity
        loadQuizStatistics();
    }

}