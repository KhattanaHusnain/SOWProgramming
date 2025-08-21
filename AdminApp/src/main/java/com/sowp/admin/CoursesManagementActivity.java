package com.sowp.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;

public class CoursesManagementActivity extends AppCompatActivity implements View.OnClickListener {

    // UI Components
    private TextView tvTotalCourses, tvActiveCourses, tvTotalTopics;
    private CardView cardViewCourses, cardAddCourse, cardEditCourses;
    private CardView cardAddTopic, cardEditTopic, cardViewTopics;
    private CardView cardManageCategories, cardCourseAnalytics, cardCourseSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_courses);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Load course statistics
        loadCourseStatistics();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        // Statistics TextViews
        tvTotalCourses = findViewById(R.id.tvTotalCourses);
        tvActiveCourses = findViewById(R.id.tvActiveCourses);
        tvTotalTopics = findViewById(R.id.tvTotalTopics);

        // Course Management Cards
        cardViewCourses = findViewById(R.id.cardViewCourses);
        cardAddCourse = findViewById(R.id.cardAddCourse);
        cardEditCourses = findViewById(R.id.cardEditCourses);

        // Topic Management Cards
        cardAddTopic = findViewById(R.id.cardAddTopic);
        cardEditTopic = findViewById(R.id.cardEditTopic);
        cardViewTopics = findViewById(R.id.cardViewTopics);

        // Additional Management Cards
        cardManageCategories = findViewById(R.id.cardManageCategories);
        cardCourseAnalytics = findViewById(R.id.cardCourseAnalytics);
        cardCourseSettings = findViewById(R.id.cardCourseSettings);
    }

    /**
     * Set click listeners for all cards
     */
    private void setClickListeners() {
        cardViewCourses.setOnClickListener(this);
        cardAddCourse.setOnClickListener(this);
        cardEditCourses.setOnClickListener(this);
        cardAddTopic.setOnClickListener(this);
        cardEditTopic.setOnClickListener(this);
        cardViewTopics.setOnClickListener(this);
        cardManageCategories.setOnClickListener(this);
        cardCourseAnalytics.setOnClickListener(this);
        cardCourseSettings.setOnClickListener(this);
    }

    /**
     * Load and display course statistics
     */
    private void loadCourseStatistics() {
        // TODO: Replace with actual database calls
        // For now, using sample data

        // Simulate loading statistics from database
        try {
            // Get statistics from your database or API
            getTotalCoursesCount(new CountCallback() {
                @Override
                public void onCountReceived(int count) {
                    tvTotalCourses.setText(String.valueOf(count));
                }

                @Override
                public void onError(Exception e) {

                }
            });
            getActiveCoursesCount(new CountCallback() {
                @Override
                public void onCountReceived(int count) {
                    tvActiveCourses.setText(String.valueOf(count));

                }

                @Override
                public void onError(Exception e) {

                }
            });
            int totalTopics = getTotalTopicsCount();

            // Update UI
            tvTotalTopics.setText(String.valueOf(totalTopics));

        } catch (Exception e) {
            // Handle errors
            showToast("Error loading course statistics: " + e.getMessage());

            // Set default values
            tvTotalCourses.setText("0");
            tvActiveCourses.setText("0");
            tvTotalTopics.setText("0");
        }
    }

    /**
     * Handle click events for all cards
     */
    @Override
    public void onClick(View v) {
        Intent intent;

        if (v.getId() == R.id.cardViewCourses) {
            // Navigate to View All Courses Activity
            intent = new Intent(this, ViewCoursesActivity.class);
            startActivity(intent);

        } else if (v.getId() == R.id.cardAddCourse) {
            // Navigate to Add Course Activity
            intent = new Intent(this, AddCourseActivity.class);
            startActivity(intent);

        } else if (v.getId() == R.id.cardEditCourses) {
            // Navigate to Edit Courses Activity
            intent = new Intent(this, EditCoursesActivity.class);
            startActivity(intent);

        } else if (v.getId() == R.id.cardAddTopic) {
            // Navigate to Add Topic Activity
            intent = new Intent(this, AddTopicActivity.class);
            startActivity(intent);

        } else if (v.getId() == R.id.cardEditTopic) {
            // Navigate to Edit Topic Activity
            intent = new Intent(this, EditTopicActivity.class);
            startActivity(intent);

        } else if (v.getId() == R.id.cardViewTopics) {
            // Navigate to View Topics Activity
            intent = new Intent(this, ViewTopicsActivity.class);
            startActivity(intent);

        } else if (v.getId() == R.id.cardManageCategories) {
            // Navigate to Manage Categories Activity
//            intent = new Intent(this, ManageCategoriesActivity.class);
//            startActivity(intent);
            Toast.makeText(this,"Manage Course Categories", Toast.LENGTH_SHORT).show();

        } else if (v.getId() == R.id.cardCourseAnalytics) {
            // Navigate to Course Analytics Activity
//            intent = new Intent(this, CourseAnalyticsActivity.class);
//            startActivity(intent);
            Toast.makeText(this,"Course Analytics", Toast.LENGTH_SHORT).show();

        } else if (v.getId() == R.id.cardCourseSettings) {
            // Navigate to Course Settings Activity
//            intent = new Intent(this, CourseSettingsActivity.class);
//            startActivity(intent);
            Toast.makeText(this,"Course Settings", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Get total courses count from database
     * @return Total number of courses
     */
    public interface CountCallback {
        void onCountReceived(int count);
        void onError(Exception e);
    }

    private void getTotalCoursesCount(CountCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Course")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(aggregateQuerySnapshot -> {
                    int count = (int) aggregateQuerySnapshot.getCount();
                    callback.onCountReceived(count);
                })
                .addOnFailureListener(e -> {
                    callback.onError(e);
                });
    }



    private void getActiveCoursesCount(CountCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Course")
                .whereEqualTo("isPublic",true)
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(aggregateQuerySnapshot -> {
                    int count = (int) aggregateQuerySnapshot.getCount();
                    callback.onCountReceived(count);
                })
                .addOnFailureListener(e -> {
                    callback.onError(e);
                });
    }


    private int getTotalTopicsCount() {
        // TODO: Implement database query to get total topics count
        // Example implementation:
        /*
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM topics", null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
        */

        // Sample data for demonstration
        return 89; // Replace with actual database call
    }

    /**
     * Utility method to show toast messages
     * @param message Message to display
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Refresh course statistics when returning from other activities
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Reload statistics in case they were updated in other activities
        loadCourseStatistics();
    }

    /**
     * Handle back button press
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // You can add custom logic here if needed
        finish();
    }

    /**
     * Clean up resources when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO: Clean up any resources, close database connections, cancel background tasks
    }
}