package com.android.nexcode.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.android.nexcode.R;
import com.android.nexcode.database.AppDatabase;
import com.android.nexcode.course.Course;
import com.android.nexcode.course.CourseDao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SplashScreen extends AppCompatActivity {
    private CourseDao courseDao;

    // UI elements
    private ImageView logo;
    private TextView appName;
    private TextView tagline;
    private LottieAnimationView mainAnimation;
    private TextView versionText;
    private ProgressBar loadingIndicator;

    // Boolean to track data loading
    private boolean isDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply the splash screen theme
        setTheme(R.style.SplashTheme);

        // Make the activity fullscreen
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_splash_screen);

        // Initialize UI elements
        initializeViews();

        // Start animations
        startAnimations();

        // Start data loading process
        courseDao = AppDatabase.getInstance(this).courseDao();
        checkDatabase();
    }

    private void initializeViews() {
        logo = findViewById(R.id.logo);
        appName = findViewById(R.id.app_name);
        tagline = findViewById(R.id.tagline);
        mainAnimation = findViewById(R.id.main_animation);
        versionText = findViewById(R.id.version_text);
        loadingIndicator = findViewById(R.id.loading_indicator);

        // Set initial alpha to 0 (invisible)
        logo.setAlpha(0f);
        appName.setAlpha(0f);
        tagline.setAlpha(0f);
        mainAnimation.setAlpha(0f);
        versionText.setAlpha(0f);
        loadingIndicator.setAlpha(0f);
    }

    private void startAnimations() {
        // Logo animation - fade in with bounce
        logo.animate()
                .alpha(1f)
                .setDuration(1000)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .setStartDelay(300)
                .start();

        // App name animation - fade in
        appName.animate()
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(600)
                .start();

        // Tagline animation - fade in
        tagline.animate()
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(800)
                .start();

        // Main animation - fade in
        mainAnimation.animate()
                .alpha(1f)
                .setDuration(1000)
                .setInterpolator(new DecelerateInterpolator())
                .setStartDelay(1000)
                .start();

        // Version text - fade in
        versionText.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(1200)
                .start();

        // Loading indicator - fade in
        loadingIndicator.animate()
                .alpha(1f)
                .setDuration(600)
                .setStartDelay(1400)
                .start();
    }

    private void checkDatabase() {
        courseDao.getAllCoursesLive().observe(this, courses -> {
            if (courses == null || courses.isEmpty()) {
                loadFirebaseData();
            } else {
                isDataLoaded = true;
                // Set minimum display time to 2.5 seconds
                new Handler(Looper.getMainLooper()).postDelayed(this::goToNextScreen, 2500);
            }
        });
    }

    private void loadFirebaseData() {
        FirebaseFirestore.getInstance().collection("Course")
                .get()
                .addOnSuccessListener(documents -> {
                    List<Course> courses = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : documents) {
                        Course course = new Course(
                                Integer.parseInt(doc.getId()),
                                doc.getString("illustration"),
                                doc.getString("title"),
                                doc.getLong("members").intValue(),
                                doc.getString("description"),
                                doc.getString("duration"),
                                doc.getString("category"),
                                doc.getString("outline")
                        );
                        courses.add(course);
                    }

                    saveCourses(courses);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading courses", Toast.LENGTH_SHORT).show();
                    // Still proceed to next screen even if failed
                    isDataLoaded = true;
                    new Handler(Looper.getMainLooper()).postDelayed(this::goToNextScreen, 2500);
                });
    }

    private void saveCourses(List<Course> courses) {
        new Thread(() -> {
            courseDao.insertCourses(courses);
            isDataLoaded = true;
            runOnUiThread(() ->
                    new Handler(Looper.getMainLooper()).postDelayed(this::goToNextScreen, 2500)
            );
        }).start();
    }

    private void goToNextScreen() {
        // Only proceed if we haven't already navigated away
        if (!isFinishing() && isDataLoaded) {
            Intent intent = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? new Intent(this, Main.class)
                    : new Intent(this, Authorization.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        // Prevent back button during splash screen
        // Do nothing
    }
}