package com.sowp.admin.quizmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.ViewCoursesActivity;

public class QuizManagementActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private LinearLayout cardViewQuizzes;
    private LinearLayout cardUploadQuiz;
    private TextView txtTotalQuizzes;
    private TextView txtActiveQuizzes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_management);

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Load quiz statistics
        loadQuizStatistics();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        cardViewQuizzes = findViewById(R.id.cardViewQuizzes);
        cardUploadQuiz = findViewById(R.id.cardUploadQuiz);
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
        cardViewQuizzes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuizManagementActivity.this, ViewCoursesActivity.class);
                intent.putExtra("cameForQuizzes", true);
                startActivity(intent);
            }
        });

        // Upload New Quiz card click listener
        cardUploadQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuizManagementActivity.this, UploadQuizActivity.class);
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