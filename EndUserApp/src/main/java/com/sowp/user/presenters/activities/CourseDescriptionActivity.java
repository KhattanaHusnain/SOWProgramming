package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.sowp.user.models.User;
import com.sowp.user.models.CourseProgress;
import com.sowp.user.repositories.CourseRepository;
import com.sowp.user.repositories.TopicRepository;
import com.sowp.user.repositories.UserRepository;
import com.sowp.user.R;
import com.sowp.user.models.Course;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseDescriptionActivity extends AppCompatActivity {
    private ImageView ivCourseIllustration;
    private TextView tvCourseCategory;
    private TextView tvCourseTitle;
    private TextView tvCourseDescription;
    private TextView tvCourseOutline;
    private TextView tvCourseDuration;
    private TextView tvCourseLectures;
    private TextView tvCourseMembers;
    private TextView tvCourseUpdated;
    private TextView tvCourseInstructor;
    private TextView tvCourseCreditHours;
    private TextView tvCourseLevel;
    private TextView tvCourseLanguage;
    private TextView tvCourseRating;
    private TextView tvAverageRating;
    private TextView tvRatingCount;
    private TextView tvCurrentRating;
    private MaterialButton btnEnrollCourse;
    private MaterialButton btnAddFavorite;
    private MaterialButton btnUnenrollCourse;
    private MaterialButton btnSubmitRating;
    private ProgressBar progressBar;
    private RatingBar ratingBar;

    private LinearLayout layoutCourseHeaderContent;
    private LinearLayout layoutCourseOutlineContent;
    private LinearLayout layoutCourseRatingContent;
    private ConstraintLayout layoutBottomActionsContent;

    private boolean isFavorite = false;
    private boolean isEnrolled = false;
    private int courseId;
    private Course course;
    private float currentUserRating = 0f;
    private CourseProgress courseProgress;

    private CourseRepository courseRepository;
    private UserRepository userRepository;
    private TopicRepository topicRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_description);

        initialize();
        setupToolbar();
        showLoading();

        courseId = getIntent().getIntExtra("COURSE_ID", 0);
        if (courseId == 0) {
            // Handle invalid course ID
            finish();
            return;
        }

        loadAllData();
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initialize() {
        ivCourseIllustration = findViewById(R.id.iv_course_illustration);
        tvCourseCategory = findViewById(R.id.tv_course_category);
        tvCourseTitle = findViewById(R.id.tv_course_title);
        tvCourseDescription = findViewById(R.id.tv_course_description);
        tvCourseDuration = findViewById(R.id.tv_course_duration);
        tvCourseLectures = findViewById(R.id.tv_course_lectures);
        tvCourseMembers = findViewById(R.id.tv_course_members);
        tvCourseUpdated = findViewById(R.id.tv_course_updated);
        tvCourseInstructor = findViewById(R.id.tv_course_instructor);
        tvCourseCreditHours = findViewById(R.id.tv_course_credit_hours);
        tvCourseLevel = findViewById(R.id.tv_course_level);
        tvCourseLanguage = findViewById(R.id.tv_course_language);
        tvCourseRating = findViewById(R.id.tv_course_rating);
        tvAverageRating = findViewById(R.id.tv_average_rating);
        tvRatingCount = findViewById(R.id.tv_rating_count);
        tvCurrentRating = findViewById(R.id.tv_current_rating);
        tvCourseOutline = findViewById(R.id.tv_course_outline);
        btnEnrollCourse = findViewById(R.id.btn_enroll_course);
        btnAddFavorite = findViewById(R.id.btn_add_favorite);
        btnUnenrollCourse = findViewById(R.id.btn_unenroll_course);
        btnSubmitRating = findViewById(R.id.btn_submit_rating);
        progressBar = findViewById(R.id.progress_bar);
        ratingBar = findViewById(R.id.rating_bar);

        layoutCourseHeaderContent = findViewById(R.id.layout_course_header_content);
        layoutCourseOutlineContent = findViewById(R.id.layout_course_outline_content);
        layoutCourseRatingContent = findViewById(R.id.layout_course_rating_content);
        layoutBottomActionsContent = findViewById(R.id.layout_bottom_actions_content);

        courseRepository = new CourseRepository(this);
        userRepository = new UserRepository(this);
        topicRepository = new TopicRepository();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        layoutCourseHeaderContent.setVisibility(View.GONE);
        layoutCourseOutlineContent.setVisibility(View.GONE);
        layoutCourseRatingContent.setVisibility(View.GONE);
        layoutBottomActionsContent.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        layoutCourseHeaderContent.setVisibility(View.VISIBLE);
        layoutCourseOutlineContent.setVisibility(View.VISIBLE);
        layoutCourseRatingContent.setVisibility(View.VISIBLE);
        layoutBottomActionsContent.setVisibility(View.VISIBLE);
    }

    private void loadAllData() {
        final int[] completedOperations = {0};
        final int totalOperations = 4;

        loadCourseData(courseId, () -> {
            completedOperations[0]++;
            checkDataLoadingComplete(completedOperations[0], totalOperations);
        });

        checkEnrollmentStatus(courseId, () -> {
            completedOperations[0]++;
            checkDataLoadingComplete(completedOperations[0], totalOperations);
        });

        checkFavoriteStatus(courseId, () -> {
            completedOperations[0]++;
            checkDataLoadingComplete(completedOperations[0], totalOperations);
        });

        loadCourseRatingData(() -> {
            completedOperations[0]++;
            checkDataLoadingComplete(completedOperations[0], totalOperations);
        });
    }

    private void checkDataLoadingComplete(int completed, int total) {
        if (completed >= total) {
            hideLoading();
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
        btnEnrollCourse.setOnClickListener(v -> {
            if (isEnrolled) {
                navigateToTopicList();
            } else {
                enrollInCourse();
            }
        });

        btnEnrollCourse.setOnLongClickListener(v -> {
            if (isEnrolled) {
                showUnenrollDialog();
                return true;
            }
            return false;
        });

        btnUnenrollCourse.setOnClickListener(v -> showUnenrollDialog());

        btnAddFavorite.setOnClickListener(v -> toggleFavorite());

        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                currentUserRating = rating;
                updateCurrentRatingText();
                btnSubmitRating.setVisibility(rating > 0 ? View.VISIBLE : View.GONE);
            }
        });

        btnSubmitRating.setOnClickListener(v -> submitRating());
    }

    private void loadCourseData(int courseId, Runnable onComplete) {
        courseRepository.getCourse(courseId, new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                if (courses != null && !courses.isEmpty()) {
                    course = courses.get(0);
                    populateCourseData();
                }
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(String message) {
                // Handle failure gracefully - don't crash, just complete
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void populateCourseData() {
        if (course == null) return;

        // Safe setText operations with null checks
        if (tvCourseTitle != null && course.getTitle() != null) {
            tvCourseTitle.setText(course.getTitle());
        }

        if (tvCourseDescription != null && course.getDescription() != null) {
            tvCourseDescription.setText(course.getDescription());
        }

        if (tvCourseDuration != null && course.getDuration() != null) {
            tvCourseDuration.setText(course.getDuration());
        }

        if (tvCourseLectures != null) {
            tvCourseLectures.setText(String.valueOf(course.getLectures()));
        }

        if (tvCourseMembers != null) {
            tvCourseMembers.setText(String.valueOf(course.getMembers()));
        }

        if (tvCourseInstructor != null && course.getInstructor() != null) {
            tvCourseInstructor.setText(course.getInstructor());
        }

        if (tvCourseCreditHours != null) {
            tvCourseCreditHours.setText(String.valueOf(course.getCreditHours()));
        }

        if (tvCourseLevel != null && course.getLevel() != null) {
            tvCourseLevel.setText(course.getLevel());
        }

        if (tvCourseLanguage != null && course.getLanguage() != null) {
            tvCourseLanguage.setText(course.getLanguage());
        }

        if (course.getCategoryArray() != null && !course.getCategoryArray().isEmpty()) {
            if (tvCourseCategory != null) {
                tvCourseCategory.setText(course.getCategoryArray().get(0));
            }
        }

        // Safe date formatting
        try {
            Date date = new Date(course.getUpdatedAt());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String formattedDate = sdf.format(date);
            if (tvCourseUpdated != null) {
                tvCourseUpdated.setText("Updated: " + formattedDate);
            }
        } catch (Exception e) {
            if (tvCourseUpdated != null) {
                tvCourseUpdated.setText("Updated: N/A");
            }
        }

        if (tvCourseOutline != null && course.getOutline() != null) {
            tvCourseOutline.setText(course.getOutline());
        }

        loadBase64Image(course.getIllustration());
    }

    private void loadBase64Image(String base64Image) {
        if (base64Image != null && !base64Image.isEmpty() && ivCourseIllustration != null) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    ivCourseIllustration.setImageBitmap(decodedByte);
                } else {
                    ivCourseIllustration.setImageResource(R.drawable.ic_course);
                }
            } catch (Exception e) {
                ivCourseIllustration.setImageResource(R.drawable.ic_course);
            }
        } else if (ivCourseIllustration != null) {
            ivCourseIllustration.setImageResource(R.drawable.ic_course);
        }
    }

    private void enrollInCourse() {
        courseRepository.updateEnrollmentCount(courseId, new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                userRepository.enrollUserInCourse(courseId, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        createCourseProgress();
                        isEnrolled = true;
                        updateEnrollButton();
                        updateUnenrollButton();
                        navigateToTopicList();
                    }

                    @Override
                    public void onFailure(String message) {
                        // Handle enrollment failure
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                // Handle enrollment count update failure
            }
        });
    }

    private void createCourseProgress() {
        // Don't create course progress here - let the UserRepository handle it
        // The UserRepository.enrollUserInCourse() will handle progress creation
        // and preserve existing data if the user is re-enrolling
    }

    private void navigateToTopicList() {
        Intent intent = new Intent(CourseDescriptionActivity.this, TopicList.class);
        intent.putExtra("courseID", courseId);
        startActivity(intent);
    }

    private void showUnenrollDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Unenroll from Course")
                .setMessage("Are you sure you want to unenroll from this course? This will remove your progress.")
                .setPositiveButton("Unenroll", (dialog, which) -> unenrollFromCourse())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unenrollFromCourse() {
        courseRepository.decrementEnrollmentCount(courseId, new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                userRepository.unenrollUserFromCourse(courseId, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        updateCourseProgressOnUnenroll();
                        isEnrolled = false;
                        updateEnrollButton();
                        updateUnenrollButton();
                    }

                    @Override
                    public void onFailure(String message) {
                        // Handle failure gracefully
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                // Handle failure gracefully
            }
        });
    }

    private void updateCourseProgressOnUnenroll() {
        if (courseProgress != null) {
            try {
                courseProgress.setCurrentlyEnrolled(false);
                courseProgress.setUnenrolledAt(System.currentTimeMillis()); // This now accepts Long instead of long

                userRepository.updateCourseProgress(courseId, courseProgress, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        // Progress updated
                    }

                    @Override
                    public void onFailure(String message) {
                        // Handle failure
                    }
                });
            } catch (Exception e) {
                // Handle any exception during progress update
            }
        }
    }

    private void checkEnrollmentStatus(int courseId, Runnable onComplete) {
        userRepository.checkEnrollmentStatus(courseId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                isEnrolled = true;
                loadCourseProgress(); // Only load if enrolled
                updateEnrollButton();
                updateUnenrollButton();
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(String message) {
                isEnrolled = false;
                // Don't try to load course progress if not enrolled
                updateEnrollButton();
                updateUnenrollButton();
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void loadCourseProgress() {
        // Only load if user is enrolled
        if (!isEnrolled) {
            // Clear rating UI for non-enrolled users
            currentUserRating = 0f;
            if (ratingBar != null) {
                ratingBar.setRating(0f);
            }
            updateCurrentRatingText();
            return;
        }

        userRepository.getCourseProgress(courseId, new UserRepository.CourseProgressCallback() {
            @Override
            public void onSuccess(CourseProgress progress) {
                if (progress != null) {
                    courseProgress = progress;
                    currentUserRating = progress.getUserRating();
                    if (ratingBar != null) {
                        ratingBar.setRating(currentUserRating);
                    }
                    updateCurrentRatingText();
                }
                // Also try to load user's rating from the course ratings subcollection
                loadUserRatingFromCourse();
            }

            @Override
            public void onFailure(String message) {
                // Progress doesn't exist yet - this is normal for new enrollments
                // Still try to load rating
                loadUserRatingFromCourse();
            }
        });
    }

    private void loadUserRatingFromCourse() {
        // Only try to load rating if user is enrolled
        if (!isEnrolled) {
            currentUserRating = 0f;
            if (ratingBar != null) {
                ratingBar.setRating(0f);
            }
            updateCurrentRatingText();
            return;
        }

        userRepository.getUserCourseRating(courseId, new UserRepository.RatingCallback() {
            @Override
            public void onSuccess(float averageRating, int ratingCount) {
                // This is actually the user's specific rating, not average
                currentUserRating = averageRating;
                if (ratingBar != null) {
                    ratingBar.setRating(currentUserRating);
                }
                updateCurrentRatingText();
            }

            @Override
            public void onFailure(String message) {
                // User hasn't rated this course yet - this is normal
                currentUserRating = 0f;
                if (ratingBar != null) {
                    ratingBar.setRating(0f);
                }
                updateCurrentRatingText();
            }
        });
    }

    private void updateEnrollButton() {
        if (btnEnrollCourse != null) {
            if (isEnrolled) {
                btnEnrollCourse.setText("CONTINUE LEARNING");
                btnEnrollCourse.setIcon(getDrawable(R.drawable.ic_play_arrow));
            } else {
                btnEnrollCourse.setText("ENROLL NOW");
                btnEnrollCourse.setIcon(null);
            }
        }
    }

    private void updateUnenrollButton() {
        if (btnUnenrollCourse != null) {
            btnUnenrollCourse.setVisibility(isEnrolled ? View.VISIBLE : View.GONE);
        }
    }

    private void toggleFavorite() {
        if (isFavorite) {
            userRepository.removeFromFavorite(courseId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    isFavorite = false;
                    updateFavoriteButton();
                }

                @Override
                public void onFailure(String message) {
                    // Handle failure gracefully
                }
            });
        } else {
            userRepository.addToFavorite(courseId, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    isFavorite = true;
                    updateFavoriteButton();
                }

                @Override
                public void onFailure(String message) {
                    // Handle failure gracefully
                }
            });
        }
    }

    private void checkFavoriteStatus(int courseId, Runnable onComplete) {
        userRepository.checkFavoriteStatus(courseId, new UserRepository.UserCallback() {
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
        if (btnAddFavorite != null) {
            try {
                if (isFavorite) {
                    btnAddFavorite.setIcon(getDrawable(R.drawable.ic_favorite));
                    btnAddFavorite.setIconTint(getColorStateList(R.color.accent));
                } else {
                    btnAddFavorite.setIcon(getDrawable(R.drawable.ic_favorite_border));
                    btnAddFavorite.setIconTint(getColorStateList(R.color.primary));
                }
            } catch (Exception e) {
                // Handle any resource loading issues
            }
        }
    }

    private void loadCourseRatingData(Runnable onComplete) {
        courseRepository.getCourseRatingData(courseId, new CourseRepository.RatingCallback() {
            @Override
            public void onSuccess(float averageRating, int ratingCount) {
                // Safely update UI with rating data
                if (tvAverageRating != null) {
                    tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
                }
                if (tvCourseRating != null) {
                    tvCourseRating.setText(String.format(Locale.getDefault(), "%.1f", averageRating));
                }
                if (tvRatingCount != null) {
                    tvRatingCount.setText(String.format(Locale.getDefault(), "(%d ratings)", ratingCount));
                }
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(String message) {
                // Set default values when no rating data exists
                if (tvAverageRating != null) {
                    tvAverageRating.setText("0.0");
                }
                if (tvCourseRating != null) {
                    tvCourseRating.setText("0.0");
                }
                if (tvRatingCount != null) {
                    tvRatingCount.setText("(0 ratings)");
                }
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void submitRating() {
        if (currentUserRating > 0 && isEnrolled) { // Only allow rating if enrolled
            userRepository.submitCourseRating(courseId, currentUserRating, new UserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (btnSubmitRating != null) {
                        btnSubmitRating.setVisibility(View.GONE);
                    }
                    updateCurrentRatingText();
                    // Reload rating data to get updated average
                    loadCourseRatingData(null);

                    // Update course progress with new rating if it exists
                    if (courseProgress != null) {
                        courseProgress.setUserRating(currentUserRating);
                        userRepository.updateCourseProgress(courseId, courseProgress, new UserRepository.UserCallback() {
                            @Override
                            public void onSuccess(User user) {}
                            @Override
                            public void onFailure(String message) {}
                        });
                    }
                }

                @Override
                public void onFailure(String message) {
                    // Handle rating submission error gracefully
                }
            });
        }
    }

    private void updateCurrentRatingText() {
        if (tvCurrentRating != null) {
            if (currentUserRating > 0) {
                tvCurrentRating.setText(String.format(Locale.getDefault(), "You rated: %.0f/5", currentUserRating));
            } else {
                tvCurrentRating.setText("Not rated yet");
            }
        }
    }
}