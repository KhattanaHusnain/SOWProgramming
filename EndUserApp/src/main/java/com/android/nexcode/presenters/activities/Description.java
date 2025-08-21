package com.android.nexcode.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.android.nexcode.models.Topic;
import com.android.nexcode.models.User;
import com.android.nexcode.repositories.firebase.CourseRepository;
import com.android.nexcode.repositories.firebase.TopicRepository;
import com.android.nexcode.repositories.firebase.UserRepository;
import com.android.nexcode.repositories.roomdb.CourseDao;
import com.android.nexcode.R;
import com.android.nexcode.database.AppDatabase;
import com.android.nexcode.models.Course;
import com.android.nexcode.repositories.roomdb.TopicDao;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Description extends AppCompatActivity {
    // Non-static executor - prevents memory leaks
    private ExecutorService executor;

    private ImageView ivCourseIllustration;
    private TextView tvCourseCategory;
    private TextView tvCourseTitle;
    private TextView tvCourseDescription;
    private TextView tvCourseOutline;
    private TextView tvCourseDuration;
    private TextView tvCourseLectures;
    private TextView tvCourseMembers;
    private TextView tvCourseVisibility;
    private TextView tvCourseUpdated;
    private MaterialButton btnEnrollCourse;
    private MaterialButton btnAddFavorite;
    private MaterialButton btnMakeOffline;

    // Skeleton Views
    private View skeletonCourseIllustration;
    private LinearLayout skeletonCourseHeader;
    private LinearLayout skeletonCourseOutline;
    private LinearLayout skeletonQuickActions;
    private ConstraintLayout skeletonBottomActions;

    // Content Views
    private LinearLayout layoutCourseHeaderContent;
    private LinearLayout layoutCourseOutlineContent;
    private LinearLayout layoutQuickActionsContent;
    private ConstraintLayout layoutBottomActionsContent;

    // Data components
    private CourseDao courseDao;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private boolean isFavorite = false;
    private boolean isOfflineAvailable = false;
    private int courseId;
    private Course course;

    private CourseRepository courseRepository;
    UserRepository userReposititory;
    TopicRepository topicRepository;
    TopicDao topicDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);

        // Initialize executor
        executor = Executors.newSingleThreadExecutor();

        initialize();
        setupToolbar();

        // Show skeleton loading initially
        showSkeletonLoading();

        courseId = getIntent().getIntExtra("ID", 0);

        // Load data and hide skeleton when complete
        loadAllData();

        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up executor to prevent memory leaks
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void initialize() {
        // Course illustration
        ivCourseIllustration = findViewById(R.id.iv_course_illustration);

        // Course header components
        tvCourseCategory = findViewById(R.id.tv_course_category);
        tvCourseTitle = findViewById(R.id.tv_course_title);
        tvCourseDescription = findViewById(R.id.tv_course_description);

        // Course metadata components
        tvCourseDuration = findViewById(R.id.tv_course_duration);
        tvCourseLectures = findViewById(R.id.tv_course_lectures);
        tvCourseMembers = findViewById(R.id.tv_course_members);
        tvCourseVisibility = findViewById(R.id.tv_course_visibility);
        tvCourseUpdated = findViewById(R.id.tv_course_updated);

        // Course outline component
        tvCourseOutline = findViewById(R.id.tv_course_outline);

        // Action components
        btnEnrollCourse = findViewById(R.id.btn_enroll_course);
        btnAddFavorite = findViewById(R.id.btn_add_favorite);
        btnMakeOffline = findViewById(R.id.btn_make_offline);

        // Skeleton views
        skeletonCourseIllustration = findViewById(R.id.skeleton_course_illustration);
        skeletonCourseHeader = findViewById(R.id.skeleton_course_header);
        skeletonCourseOutline = findViewById(R.id.skeleton_course_outline);
        skeletonQuickActions = findViewById(R.id.skeleton_quick_actions);
        skeletonBottomActions = findViewById(R.id.skeleton_bottom_actions);

        // Content views
        layoutCourseHeaderContent = findViewById(R.id.layout_course_header_content);
        layoutCourseOutlineContent = findViewById(R.id.layout_course_outline_content);
        layoutQuickActionsContent = findViewById(R.id.layout_quick_actions_content);
        layoutBottomActionsContent = findViewById(R.id.layout_bottom_actions_content);

        courseRepository = new CourseRepository(this);
        userReposititory = new UserRepository(this);
        courseDao = AppDatabase.getInstance(this).courseDao();
        topicRepository = new TopicRepository();
        topicDao = AppDatabase.getInstance(this).topicDao();
    }

    private void showSkeletonLoading() {
        // Show skeleton views
        skeletonCourseIllustration.setVisibility(View.VISIBLE);
        skeletonCourseHeader.setVisibility(View.VISIBLE);
        skeletonCourseOutline.setVisibility(View.VISIBLE);
        skeletonQuickActions.setVisibility(View.VISIBLE);
        skeletonBottomActions.setVisibility(View.VISIBLE);

        // Hide content views
        layoutCourseHeaderContent.setVisibility(View.GONE);
        layoutCourseOutlineContent.setVisibility(View.GONE);
        layoutQuickActionsContent.setVisibility(View.GONE);
        layoutBottomActionsContent.setVisibility(View.GONE);

        // Start shimmer animation
        startShimmerAnimation();
    }

    private void hideSkeletonLoading() {
        // Clear shimmer animations
        skeletonCourseIllustration.clearAnimation();
        skeletonCourseHeader.clearAnimation();
        skeletonCourseOutline.clearAnimation();
        skeletonQuickActions.clearAnimation();
        skeletonBottomActions.clearAnimation();

        // Hide skeleton views
        skeletonCourseIllustration.setVisibility(View.GONE);
        skeletonCourseHeader.setVisibility(View.GONE);
        skeletonCourseOutline.setVisibility(View.GONE);
        skeletonQuickActions.setVisibility(View.GONE);
        skeletonBottomActions.setVisibility(View.GONE);

        // Show content views with fade-in animation
        showContentWithAnimation();
    }


    private void startShimmerAnimation() {
        android.view.animation.Animation shimmerAnimation = AnimationUtils.loadAnimation(this, R.anim.shimmer_animation);

        skeletonCourseIllustration.startAnimation(shimmerAnimation);
        skeletonCourseHeader.startAnimation(shimmerAnimation);
        skeletonCourseOutline.startAnimation(shimmerAnimation);
        skeletonQuickActions.startAnimation(shimmerAnimation);
        skeletonBottomActions.startAnimation(shimmerAnimation);
    }

    private void showContentWithAnimation() {
        android.view.animation.Animation fadeInAnimation = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

        layoutCourseHeaderContent.setVisibility(View.VISIBLE);
        layoutCourseHeaderContent.startAnimation(fadeInAnimation);

        layoutCourseOutlineContent.setVisibility(View.VISIBLE);
        layoutCourseOutlineContent.startAnimation(fadeInAnimation);

        layoutQuickActionsContent.setVisibility(View.VISIBLE);
        layoutQuickActionsContent.startAnimation(fadeInAnimation);

        layoutBottomActionsContent.setVisibility(View.VISIBLE);
        layoutBottomActionsContent.startAnimation(fadeInAnimation);
    }

    private void loadAllData() {
        // Counter to track completed operations
        final int[] completedOperations = {0};
        final int totalOperations = 4; // course data, enrollment, favorite, offline status

        // Load course data
        loadCourseData(courseId, () -> {
            completedOperations[0]++;
            checkDataLoadingComplete(completedOperations[0], totalOperations);
        });

        // Check enrollment status
        checkEnrollmentStatus(courseId, () -> {
            completedOperations[0]++;
            checkDataLoadingComplete(completedOperations[0], totalOperations);
        });

        // Check favorite status
        checkFavoriteStatus(courseId, () -> {
            completedOperations[0]++;
            checkDataLoadingComplete(completedOperations[0], totalOperations);
        });

        // Check offline availability
        checkOfflineAvailability(courseId, () -> {
            completedOperations[0]++;
            checkDataLoadingComplete(completedOperations[0], totalOperations);
        });
    }

    private void checkDataLoadingComplete(int completed, int total) {
        if (completed >= total) {
            // All data loaded, hide skeleton
            hideSkeletonLoading();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }

            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupListeners() {
        btnEnrollCourse.setOnClickListener(v -> enrollInCourse());
        btnAddFavorite.setOnClickListener(v -> toggleFavorite());
        btnMakeOffline.setOnClickListener(v -> toggleOfflineAvailability());
    }

    private void loadCourseData(int courseId, Runnable onComplete) {
        courseRepository.getCourse(courseId, new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                course = courses.get(0);
                tvCourseCategory.setText(course.getCategory());
                tvCourseTitle.setText(course.getTitle());
                tvCourseDescription.setText(course.getDescription());
                tvCourseDuration.setText(course.getDuration());
                tvCourseLectures.setText(String.valueOf(course.getLectures()));
                tvCourseMembers.setText(String.valueOf(course.getMembers()));
                tvCourseVisibility.setText(course.isPublic() ? "Public" : "Private");
                Date date = new Date(course.getUpdatedAt());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                String formattedDate = sdf.format(date);
                tvCourseUpdated.setText("Updated: " + formattedDate);
                tvCourseOutline.setText(course.getOutline());
                Glide.with(Description.this)
                        .load(course.getIllustration())
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(ivCourseIllustration);

                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(Description.this, message, Toast.LENGTH_SHORT).show();
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void enrollInCourse() {
        courseRepository.updateEnrollmentCount(courseId, new CourseRepository.Callback() {

            @Override
            public void onSuccess(List<Course> courses) {
                userReposititory.enrollUserInCourse(courseId, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        Intent intent = new Intent(Description.this, TopicList.class);
                        intent.putExtra("courseID", courseId);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(Description.this, "Failed to update user profile: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(Description.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkEnrollmentStatus(int courseId, Runnable onComplete) {
        userReposititory.checkEnrollmentStatus(courseId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                btnEnrollCourse.setText("CONTINUE LEARNING");
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(String message) {
                btnEnrollCourse.setText("ENROLL NOW");
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void toggleFavorite() {
        if (isFavorite) {
            // Remove from favorites
            userReposititory.removeFromFavorite(courseId, new UserRepository.UserCallback() {

                @Override
                public void onSuccess(User user) {
                    isFavorite = false;
                    updateFavoriteButton();
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(Description.this, message, Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            // Add to favorites
            userReposititory.addtoFavorite(courseId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    isFavorite = true;
                    updateFavoriteButton();
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(Description.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkFavoriteStatus(int courseId, Runnable onComplete) {
        userReposititory.checkFavoriteStatus(courseId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                isFavorite = true;
                updateFavoriteButton();
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(String message) {
                isFavorite = false;
                updateFavoriteButton();
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void updateFavoriteButton() {
        if (isFavorite) {
            btnAddFavorite.setText("FAVORITED");
            btnAddFavorite.setIcon(getDrawable(R.drawable.ic_favorite));
        } else {
            btnAddFavorite.setText("FAVORITE");
            btnAddFavorite.setIcon(getDrawable(R.drawable.ic_favorite_border));
        }
    }

    private void toggleOfflineAvailability() {
        if (isOfflineAvailable) {
            // Delete from Room database
            executor.execute(() -> {
                try {
                    courseDao.delete(course);
                    runOnUiThread(() -> {
                        isOfflineAvailable = false;
                        updateOfflineButton();
                        Toast.makeText(Description.this, "Course removed from offline storage", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(Description.this, "Error removing course: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } else {
            // Add to offline storage
            executor.execute(() -> {
                try {
                    // Insert course first
                    courseDao.insert(course);

                    runOnUiThread(() -> {
                        isOfflineAvailable = true;
                        updateOfflineButton();
                        Toast.makeText(Description.this, "Course added to offline storage", Toast.LENGTH_SHORT).show();

                        // Load topics after updating UI (network operation should be separate)
                        loadTopicsForOfflineStorage();
                    });
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(Description.this, "Error adding course: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            });
        }
    }

    private void loadTopicsForOfflineStorage() {
        // This handles the network operation separately from Room operations
        topicRepository.loadTopicsOfCourse(courseId, new TopicRepository.Callback() {
            @Override
            public void onSuccess(List<Topic> topics) {
                // Insert topics in background thread
                executor.execute(() -> {
                    try {
                        topicDao.insertAll(topics);
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                Toast.makeText(Description.this, "Error saving topics: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(Description.this, "Failed to load topics: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkOfflineAvailability(int courseId, Runnable onComplete) {
        executor.execute(() -> {
            try {
                Course course = courseDao.getCourseById(courseId);
                runOnUiThread(() -> {
                    isOfflineAvailable = (course != null);
                    updateOfflineButton();
                    if (onComplete != null) onComplete.run();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    isOfflineAvailable = false;
                    updateOfflineButton();
                    if (onComplete != null) onComplete.run();
                    Toast.makeText(Description.this, "Error checking offline status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateOfflineButton() {
        if (isOfflineAvailable) {
            btnMakeOffline.setText("DELETE FROM OFFLINE");
            btnMakeOffline.setIcon(getDrawable(R.drawable.ic_delete));
        } else {
            btnMakeOffline.setText("DOWNLOAD FOR OFFLINE");
            btnMakeOffline.setIcon(getDrawable(R.drawable.ic_download));
        }
    }
}