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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.sowp.user.models.Topic;
import com.sowp.user.models.User;
import com.sowp.user.repositories.firebase.CourseRepository;
import com.sowp.user.repositories.firebase.TopicRepository;
import com.sowp.user.repositories.firebase.UserRepository;
import com.sowp.user.repositories.roomdb.CourseDao;
import com.sowp.user.R;
import com.sowp.user.database.AppDatabase;
import com.sowp.user.models.Course;
import com.sowp.user.repositories.roomdb.TopicDao;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Description extends AppCompatActivity {
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
    private TextView tvCourseInstructor;
    private TextView tvCourseCreditHours;
    private TextView tvCourseLevel;
    private TextView tvCourseLanguage;
    private MaterialButton btnEnrollCourse;
    private MaterialButton btnAddFavorite;
    private MaterialButton btnMakeOffline;
    private ProgressBar progressBar;

    private LinearLayout layoutCourseHeaderContent;
    private LinearLayout layoutCourseOutlineContent;
    private LinearLayout layoutQuickActionsContent;
    private ConstraintLayout layoutBottomActionsContent;

    private CourseDao courseDao;
    private boolean isFavorite = false;
    private boolean isOfflineAvailable = false;
    private boolean isEnrolled = false;
    private int courseId;
    private Course course;

    private CourseRepository courseRepository;
    private UserRepository userRepository;
    private TopicRepository topicRepository;
    private TopicDao topicDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);

        executor = Executors.newSingleThreadExecutor();
        initialize();
        setupToolbar();
        showLoading();

        courseId = getIntent().getIntExtra("COURSE_ID", 0);
        loadAllData();
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private void initialize() {
        ivCourseIllustration = findViewById(R.id.iv_course_illustration);
        tvCourseCategory = findViewById(R.id.tv_course_category);
        tvCourseTitle = findViewById(R.id.tv_course_title);
        tvCourseDescription = findViewById(R.id.tv_course_description);
        tvCourseDuration = findViewById(R.id.tv_course_duration);
        tvCourseLectures = findViewById(R.id.tv_course_lectures);
        tvCourseMembers = findViewById(R.id.tv_course_members);
        tvCourseVisibility = findViewById(R.id.tv_course_visibility);
        tvCourseUpdated = findViewById(R.id.tv_course_updated);
        tvCourseInstructor = findViewById(R.id.tv_course_instructor);
        tvCourseCreditHours = findViewById(R.id.tv_course_credit_hours);
        tvCourseLevel = findViewById(R.id.tv_course_level);
        tvCourseLanguage = findViewById(R.id.tv_course_language);
        tvCourseOutline = findViewById(R.id.tv_course_outline);
        btnEnrollCourse = findViewById(R.id.btn_enroll_course);
        btnAddFavorite = findViewById(R.id.btn_add_favorite);
        btnMakeOffline = findViewById(R.id.btn_make_offline);
        progressBar = findViewById(R.id.progress_bar);

        layoutCourseHeaderContent = findViewById(R.id.layout_course_header_content);
        layoutCourseOutlineContent = findViewById(R.id.layout_course_outline_content);
        layoutQuickActionsContent = findViewById(R.id.layout_quick_actions_content);
        layoutBottomActionsContent = findViewById(R.id.layout_bottom_actions_content);

        courseRepository = new CourseRepository(this);
        userRepository = new UserRepository(this);
        courseDao = AppDatabase.getInstance(this).courseDao();
        topicRepository = new TopicRepository();
        topicDao = AppDatabase.getInstance(this).topicDao();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        layoutCourseHeaderContent.setVisibility(View.GONE);
        layoutCourseOutlineContent.setVisibility(View.GONE);
        layoutQuickActionsContent.setVisibility(View.GONE);
        layoutBottomActionsContent.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        layoutCourseHeaderContent.setVisibility(View.VISIBLE);
        layoutCourseOutlineContent.setVisibility(View.VISIBLE);
        layoutQuickActionsContent.setVisibility(View.VISIBLE);
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

        checkOfflineAvailability(courseId, () -> {
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

        btnAddFavorite.setOnClickListener(v -> toggleFavorite());
        btnMakeOffline.setOnClickListener(v -> toggleOfflineAvailability());
    }

    private void loadCourseData(int courseId, Runnable onComplete) {
        courseRepository.getCourse(courseId, new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                course = courses.get(0);
                populateCourseData();
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(String message) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void populateCourseData() {
        tvCourseTitle.setText(course.getTitle());
        tvCourseDescription.setText(course.getDescription());
        tvCourseDuration.setText(course.getDuration());
        tvCourseLectures.setText(String.valueOf(course.getLectures()));
        tvCourseMembers.setText(String.valueOf(course.getMembers()));
        tvCourseVisibility.setText(course.isPublic() ? "Public" : "Private");
        tvCourseInstructor.setText(course.getInstructor());
        tvCourseCreditHours.setText(String.valueOf(course.getCreditHours()));
        tvCourseLevel.setText(course.getLevel());
        tvCourseLanguage.setText(course.getLanguage());

        if (course.getCategoryArray() != null && !course.getCategoryArray().isEmpty()) {
            tvCourseCategory.setText(course.getCategoryArray().get(0));
        }

        Date date = new Date(course.getUpdatedAt());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = sdf.format(date);
        tvCourseUpdated.setText("Updated: " + formattedDate);
        tvCourseOutline.setText(course.getOutline());

        loadBase64Image(course.getIllustration());
    }

    private void loadBase64Image(String base64Image) {
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivCourseIllustration.setImageBitmap(decodedByte);
            } catch (Exception e) {
                ivCourseIllustration.setImageResource(R.drawable.ic_course);
            }
        }
    }

    private void enrollInCourse() {
        courseRepository.updateEnrollmentCount(courseId, new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                userRepository.enrollUserInCourse(courseId, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        isEnrolled = true;
                        updateEnrollButton();
                        navigateToTopicList();
                    }

                    @Override
                    public void onFailure(String message) {
                    }
                });
            }

            @Override
            public void onFailure(String message) {
            }
        });
    }

    private void navigateToTopicList() {
        Intent intent = new Intent(Description.this, TopicList.class);
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
        userRepository.unenrollUserFromCourse(courseId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                isEnrolled = false;
                updateEnrollButton();
            }

            @Override
            public void onFailure(String message) {
            }
        });
    }

    private void checkEnrollmentStatus(int courseId, Runnable onComplete) {
        userRepository.checkEnrollmentStatus(courseId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                isEnrolled = true;
                updateEnrollButton();
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(String message) {
                isEnrolled = false;
                updateEnrollButton();
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void updateEnrollButton() {
        if (isEnrolled) {
            btnEnrollCourse.setText("CONTINUE LEARNING");
            btnEnrollCourse.setIcon(getDrawable(R.drawable.ic_play_arrow));
        } else {
            btnEnrollCourse.setText("ENROLL NOW");
            btnEnrollCourse.setIcon(null);
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
        if (isFavorite) {
            btnAddFavorite.setIcon(getDrawable(R.drawable.ic_favorite));
            btnAddFavorite.setIconTint(getColorStateList(R.color.accent));
        } else {
            btnAddFavorite.setIcon(getDrawable(R.drawable.ic_favorite_border));
            btnAddFavorite.setIconTint(getColorStateList(R.color.primary));
        }
    }

    private void toggleOfflineAvailability() {
        if (isOfflineAvailable) {
            executor.execute(() -> {
                try {
                    courseDao.delete(course);
                    runOnUiThread(() -> {
                        isOfflineAvailable = false;
                        updateOfflineButton();
                    });
                } catch (Exception e) {
                }
            });
        } else {
            executor.execute(() -> {
                try {
                    courseDao.insert(course);
                    runOnUiThread(() -> {
                        isOfflineAvailable = true;
                        updateOfflineButton();
                        loadTopicsForOfflineStorage();
                    });
                } catch (Exception e) {
                }
            });
        }
    }

    private void loadTopicsForOfflineStorage() {
        topicRepository.loadTopicsOfCourse(courseId, new TopicRepository.Callback() {
            @Override
            public void onSuccess(List<Topic> topics) {
                executor.execute(() -> {
                    try {
                        topicDao.insertAll(topics);
                    } catch (Exception e) {
                    }
                });
            }

            @Override
            public void onFailure(String message) {
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