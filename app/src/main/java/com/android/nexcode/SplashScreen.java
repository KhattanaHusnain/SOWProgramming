package com.android.nexcode;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Initialize the database and add sample data
        AppDatabase database = AppDatabase.getInstance(this);
        CourseDao courseDao = database.courseDao();

        // Check if data exists and add sample data if necessary
        LiveData<List<Course>> coursesLiveData = courseDao.getAllCoursesLive();
        coursesLiveData.observe(this, courses -> {
            if (courses == null || courses.isEmpty()) {
                addSampleData(courseDao, this::navigateToNextScreen);
            } else {
                navigateToNextScreen();
            }
        });
    }

    private void addSampleData(CourseDao courseDao, Runnable onComplete) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Course> sampleCourses = new ArrayList<>();
            sampleCourses.add(new Course(0, R.drawable.ic_course, "Java Language", 1800,
                    "Master the Java programming language and object-oriented programming concepts.",
                    "15 hours", "Programming"));
            sampleCourses.add(new Course(0, R.drawable.ic_course, "OOP Fundamentals", 1100,
                    "Understand the principles of Object-Oriented Programming with practical examples.",
                    "8 hours", "Programming"));
            sampleCourses.add(new Course(0, R.drawable.ic_course, "Linear Algebra", 900,
                    "A comprehensive guide to linear algebra for mathematical modeling and data analysis.",
                    "6 hours", "Non-Programming"));
            sampleCourses.add(new Course(0, R.drawable.ic_course, "Statistics Basics", 700,
                    "Learn essential statistical techniques and their applications in real-world scenarios.",
                    "7 hours", "Non-Programming"));
            sampleCourses.add(new Course(0, R.drawable.ic_course, "Database Systems", 950,
                    "Explore database design, management, and SQL programming.",
                    "10 hours", "Programming"));
            sampleCourses.add(new Course(0, R.drawable.ic_course, "Introduction to Programming", 2100,
                    "Beginner-friendly course to learn programming concepts and logic building.",
                    "12 hours", "Programming"));

            for (Course course : sampleCourses) {
                courseDao.insert(course);
            }

            // Notify when data insertion is complete
            runOnUiThread(onComplete);
        });
    }

    private void navigateToNextScreen() {
        Intent intent = new Intent(SplashScreen.this, Authorization.class);
        startActivity(intent);
        finish();
    }
}
