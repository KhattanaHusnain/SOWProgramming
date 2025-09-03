package com.sowp.admin.coursemanagement;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sowp.admin.R;

public class CoursesManagementActivity extends AppCompatActivity {
    TextView tvTotalCourses;
    TextView tvActiveCourses;
    TextView tvTotalTopics;
    CardView cardViewCourses, cardAddCourse;
    CardView cardManage, cardAnalytics, cardSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses_management);

        initViews();
        setupClickListeners();
        loadStatistics();
    }

    private void initViews() {
        // TextViews - Fixed incorrect ID assignments
        tvTotalCourses = findViewById(R.id.tvTotalCourses);
        tvActiveCourses = findViewById(R.id.tvActiveCourses);
        tvTotalTopics = findViewById(R.id.tvTotalTopics); // Fixed: was using tvTotalCourses ID

        // Course CardViews - Fixed duplicate ID assignments
        cardViewCourses = findViewById(R.id.cardViewCourses);
        cardAddCourse = findViewById(R.id.cardAddCourse); // Fixed: was using cardViewCourses ID


        // Management CardViews - Fixed duplicate ID assignments
        cardManage = findViewById(R.id.cardManageCategories);
        cardAnalytics = findViewById(R.id.cardCourseAnalytics); // Fixed: was using cardManageCategories ID
        cardSetting = findViewById(R.id.cardCourseSettings);
    }

    private void setupClickListeners() {
        cardViewCourses.setOnClickListener(v -> {
            Intent intent = new Intent(CoursesManagementActivity.this, ViewCoursesActivity.class);
            startActivity(intent);
        });

        cardAddCourse.setOnClickListener(v -> {
            Intent intent = new Intent(CoursesManagementActivity.this, AddCourseActivity.class);
            startActivity(intent);
        });


        cardManage.setOnClickListener(v -> {
            Toast.makeText(CoursesManagementActivity.this, "Manage courses category", Toast.LENGTH_SHORT).show();
            // TODO: Implement category management activity
        });

        cardAnalytics.setOnClickListener(v -> {
            Toast.makeText(CoursesManagementActivity.this, "Courses analytics", Toast.LENGTH_SHORT).show();
            // TODO: Implement analytics activity
        });

        cardSetting.setOnClickListener(v -> {
            Toast.makeText(CoursesManagementActivity.this, "Settings", Toast.LENGTH_SHORT).show();
            // TODO: Implement settings activity
        });
    }

    private void loadStatistics() {
        loadTotalCourses();
        loadActiveCourses();
    }

    private void loadTotalCourses() {
        FirebaseFirestore fb= FirebaseFirestore.getInstance();
        fb.collection("Course")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(aggregateQuerySnapshot -> {
                    tvTotalCourses.setText(String.valueOf(aggregateQuerySnapshot.getCount()));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading total courses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvTotalCourses.setText("0");
                });
    }

    private void loadActiveCourses() {
        FirebaseFirestore fb = FirebaseFirestore.getInstance();
        fb.collection("Course")
                .whereEqualTo("isPublic", true)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(aggregateQuerySnapshot -> {
                    tvActiveCourses.setText(String.valueOf(aggregateQuerySnapshot.getCount()));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading active courses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    tvActiveCourses.setText("0");
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics when returning to this activity
        loadStatistics();
    }
}