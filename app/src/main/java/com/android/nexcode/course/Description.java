package com.android.nexcode.course;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.nexcode.R;
import com.android.nexcode.database.AppDatabase;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Description extends AppCompatActivity {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ImageView ivCourseBanner;
    private TextView courseTitle;
    private TextView courseSubtitle;
    private TextView courseDescription;
    private TextView courseOutline;
    private Button btnEnrollNow;
    private FloatingActionButton fabFavorite;
    private TextView tvCourseDuration;
    private TextView tvCourseLessons;
    private TextView tvCourseLevel;
    private TextView tvInstructorName;
    private TextView tvInstructorTitle;
    private TextView tvInstructorRatingText;
    private RatingBar rbInstructorRating;
    private TextView tvCoursePrice;

    // Data components
    private CourseDao courseDao;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private boolean isFavorite = false;
    private boolean isEnrolled = false;
    private int courseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);

        try {
            initializeFirebase();
            initializeViews();
            initializeDatabase();
            setupToolbar();

            courseId = getIntent().getIntExtra("ID", 0);
            loadCourseDetails(courseId);

            // Uncomment these when Firebase is properly set up
            checkEnrollmentStatus(courseId);
            checkFavoriteStatus(courseId);

            setupListeners();
        } catch (Exception e) {
            // Catch any initialization errors to prevent crashes
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void initializeFirebase() {
        try {
            db = FirebaseFirestore.getInstance();
            currentUser = FirebaseAuth.getInstance().getCurrentUser();
        } catch (Exception e) {
            // Handle Firebase initialization error
            Toast.makeText(this, "Firebase initialization error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void initializeViews() {
        // Toolbar components
        try {
            ivCourseBanner = findViewById(R.id.iv_course_banner);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing ivCourseBanner", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Course header components
        try {
            courseTitle = findViewById(R.id.course_title);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing courseTitle", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            courseSubtitle = findViewById(R.id.tv_course_subtitle);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing courseSubtitle", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            courseDescription = findViewById(R.id.course_description);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing courseDescription", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Course metadata components
        try {
            tvCourseDuration = findViewById(R.id.tv_course_duration);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing tvCourseDuration", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            tvCourseLessons = findViewById(R.id.tv_course_lessons);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing tvCourseLessons", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            tvCourseLevel = findViewById(R.id.tv_course_level);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing tvCourseLevel", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Course outline component
        try {
            courseOutline = findViewById(R.id.outline);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing courseOutline", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Instructor components
        try {
            tvInstructorName = findViewById(R.id.tv_instructor_name);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing tvInstructorName", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            tvInstructorTitle = findViewById(R.id.tv_instructor_title);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing tvInstructorTitle", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            rbInstructorRating = findViewById(R.id.rb_instructor_rating);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing rbInstructorRating", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            tvInstructorRatingText = findViewById(R.id.tv_instructor_rating_text);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing tvInstructorRatingText", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Action components
        try {
            btnEnrollNow = findViewById(R.id.btn_join_course);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing btnEnrollNow", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        try {
            fabFavorite = findViewById(R.id.fab_favorite);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing fabFavorite", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Price components
        try {
            tvCoursePrice = findViewById(R.id.tv_course_price);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing tvCoursePrice", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void setupToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowTitleEnabled(false);
                }

                toolbar.setNavigationOnClickListener(v -> finish());
            }
        } catch (Exception e) {
            // Handle toolbar setup error
            Toast.makeText(this, "Toolbar setup error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void initializeDatabase() {
        try {
            // Add null check for AppDatabase
            AppDatabase appDatabase = AppDatabase.getInstance(this);
            if (appDatabase != null) {
                courseDao = appDatabase.courseDao();
            } else {
                Toast.makeText(this, "Database initialization error", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Handle database initialization error
            Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        try {
            // Enroll button click listener
            if (btnEnrollNow != null) {
                btnEnrollNow.setOnClickListener(v -> enrollInCourse());
            }

            // Favorite button click listener
            if (fabFavorite != null) {
                fabFavorite.setOnClickListener(v -> toggleFavorite());
            }
        } catch (Exception e) {
            // Handle listener setup error
            Toast.makeText(this, "Error setting up button listeners", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadCourseDetails(int courseId) {
        if (courseId == 0) {
            Toast.makeText(this, "Invalid course ID", Toast.LENGTH_SHORT).show();
            handleInvalidCourse();
            return;
        }

        // Check if courseDao is initialized
        if (courseDao == null) {
            setDefaultCourseData();
            return;
        }

        try {
            executor.execute(() -> {
                try {
                    Course course = courseDao.getCourseById(courseId);
                    runOnUiThread(() -> {
                        if (course != null) {
                            updateCourseUI(course);
                            // Only call this if Firebase is properly initialized
                            if (db != null) {
                                loadAdditionalCourseData(courseId);
                            } else {
                                setDefaultCourseMetadata();
                            }
                        } else {
                            handleInvalidCourse();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(Description.this, "Error loading course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        setDefaultCourseData();
                    });
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            // Handle executor error
            Toast.makeText(this, "Error in background processing", Toast.LENGTH_SHORT).show();
            setDefaultCourseData();
            e.printStackTrace();
        }
    }

    private void setDefaultCourseData() {
        // Set default values for course data if we can't load from database
        if (courseTitle != null) courseTitle.setText("Data Structure Algorithms");
        if (courseDescription != null) courseDescription.setText("A data structure is a way of organizing and storing data so that it can be accessed and modified efficiently. Data structures are fundamental concepts in computer science and are essential for designing efficient algorithms.");
        if (tvCourseDuration != null) tvCourseDuration.setText("12 weeks");
        if (tvCourseLessons != null) tvCourseLessons.setText("24 Lessons");
        if (tvCourseLevel != null) tvCourseLevel.setText("All Levels");
        if (tvInstructorName != null) tvInstructorName.setText("Prof. Alex Morgan");
        if (tvInstructorTitle != null) tvInstructorTitle.setText("Senior Software Engineer");
        if (rbInstructorRating != null) rbInstructorRating.setRating(4.8f);
        if (tvInstructorRatingText != null) tvInstructorRatingText.setText("4.8 (1,245 reviews)");
        if (tvCoursePrice != null) tvCoursePrice.setText("$49.99");
    }

    private void setDefaultCourseMetadata() {
        // Set default values for course metadata
        if (tvCourseDuration != null) tvCourseDuration.setText("12 weeks");
        if (tvCourseLessons != null) tvCourseLessons.setText("24 Lessons");
        if (tvCourseLevel != null) tvCourseLevel.setText("All Levels");
        if (tvInstructorName != null) tvInstructorName.setText("Prof. Alex Morgan");
        if (tvInstructorTitle != null) tvInstructorTitle.setText("Senior Software Engineer");
        if (rbInstructorRating != null) rbInstructorRating.setRating(4.8f);
        if (tvInstructorRatingText != null) tvInstructorRatingText.setText("4.8 (1,245 reviews)");
        if (tvCoursePrice != null) tvCoursePrice.setText("$49.99");
    }

    private void loadAdditionalCourseData(int courseId) {
        if (db == null) {
            setDefaultCourseMetadata();
            return;
        }

        try {
            db.collection("Course").document(String.valueOf(courseId))
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            updateCourseMetadata(documentSnapshot);
                            updateInstructorInfo(documentSnapshot);
                            updatePriceInfo(documentSnapshot);
                        } else {
                            setDefaultCourseMetadata();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load additional course data", Toast.LENGTH_SHORT).show();
                        setDefaultCourseMetadata();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error accessing Firebase", Toast.LENGTH_SHORT).show();
            setDefaultCourseMetadata();
            e.printStackTrace();
        }
    }

    private void updateCourseUI(Course course) {
        try {
            if (course == null) {
                setDefaultCourseData();
                return;
            }

            String title = course.getTitle() != null ? course.getTitle() : getString(R.string.untitled_course);
            String description = course.getDescription() != null ? course.getDescription() :
                    getString(R.string.no_description);
            String outline = course.getOutline() != null ? course.getOutline() : "";

            if (courseTitle != null) courseTitle.setText(title);
            if (courseDescription != null) courseDescription.setText(description);

            // Format the outline from plain text to a structured content for the modules
            formatAndSetOutline(outline);

            // Load the course banner image
            loadCourseBanner(course.getIllustration());
        } catch (Exception e) {
            Toast.makeText(this, "Error updating course UI", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadCourseBanner(String imageUrl) {
        try {
            if (ivCourseBanner == null) return;

            if (imageUrl == null || imageUrl.isEmpty()) {
                ivCourseBanner.setImageResource(R.drawable.course_placeholder);
                return;
            }

            RequestOptions requestOptions = new RequestOptions()
                    .placeholder(R.drawable.course_placeholder)
                    .error(R.drawable.course_error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            Glide.with(this)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .into(ivCourseBanner);
        } catch (Exception e) {
            if (ivCourseBanner != null) {
                ivCourseBanner.setImageResource(R.drawable.course_placeholder);
            }
            Toast.makeText(this, "Error loading course image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void updateCourseMetadata(DocumentSnapshot documentSnapshot) {
        try {
            if (documentSnapshot == null) return;

            // Get course metadata from Firestore with null checks
            String duration = documentSnapshot.getString("duration");
            Long lessonsCount = documentSnapshot.getLong("lessonsCount");
            String level = documentSnapshot.getString("level");

            // Set default values if null
            if (duration == null) duration = "12 weeks";
            if (lessonsCount == null) lessonsCount = 24L;
            if (level == null) level = "All Levels";

            // Update UI with null checks
            if (tvCourseDuration != null) tvCourseDuration.setText(duration);
            if (tvCourseLessons != null) tvCourseLessons.setText(lessonsCount + " Lessons");
            if (tvCourseLevel != null) tvCourseLevel.setText(level);
        } catch (Exception e) {
            setDefaultCourseMetadata();
            e.printStackTrace();
        }
    }

    private void updateInstructorInfo(DocumentSnapshot documentSnapshot) {
        try {
            if (documentSnapshot == null || db == null) {
                // Set default values
                if (tvInstructorName != null) tvInstructorName.setText("Prof. Alex Morgan");
                if (tvInstructorTitle != null) tvInstructorTitle.setText("Senior Software Engineer");
                if (rbInstructorRating != null) rbInstructorRating.setRating(4.8f);
                if (tvInstructorRatingText != null) tvInstructorRatingText.setText("4.8 (1,245 reviews)");
                return;
            }

            // Get instructor data
            String instructorId = documentSnapshot.getString("instructorId");

            if (instructorId != null) {
                db.collection("Instructors").document(instructorId)
                        .get()
                        .addOnSuccessListener(instructorDoc -> {
                            if (instructorDoc != null && instructorDoc.exists()) {
                                String name = instructorDoc.getString("name");
                                String title = instructorDoc.getString("title");
                                Double rating = instructorDoc.getDouble("rating");
                                Long reviewsCount = instructorDoc.getLong("reviewsCount");

                                // Set default values if null
                                if (name == null) name = "Prof. Alex Morgan";
                                if (title == null) title = "Senior Software Engineer";
                                if (rating == null) rating = 4.8;
                                if (reviewsCount == null) reviewsCount = 1245L;

                                // Update UI with null checks
                                if (tvInstructorName != null) tvInstructorName.setText(name);
                                if (tvInstructorTitle != null) tvInstructorTitle.setText(title);
                                if (rbInstructorRating != null) rbInstructorRating.setRating(rating.floatValue());
                                if (tvInstructorRatingText != null) tvInstructorRatingText.setText(rating + " (" + reviewsCount + " reviews)");
                            } else {
                                // Set default values
                                if (tvInstructorName != null) tvInstructorName.setText("Prof. Alex Morgan");
                                if (tvInstructorTitle != null) tvInstructorTitle.setText("Senior Software Engineer");
                                if (rbInstructorRating != null) rbInstructorRating.setRating(4.8f);
                                if (tvInstructorRatingText != null) tvInstructorRatingText.setText("4.8 (1,245 reviews)");
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Set default values on failure
                            if (tvInstructorName != null) tvInstructorName.setText("Prof. Alex Morgan");
                            if (tvInstructorTitle != null) tvInstructorTitle.setText("Senior Software Engineer");
                            if (rbInstructorRating != null) rbInstructorRating.setRating(4.8f);
                            if (tvInstructorRatingText != null) tvInstructorRatingText.setText("4.8 (1,245 reviews)");
                        });
            } else {
                // Set default values
                if (tvInstructorName != null) tvInstructorName.setText("Prof. Alex Morgan");
                if (tvInstructorTitle != null) tvInstructorTitle.setText("Senior Software Engineer");
                if (rbInstructorRating != null) rbInstructorRating.setRating(4.8f);
                if (tvInstructorRatingText != null) tvInstructorRatingText.setText("4.8 (1,245 reviews)");
            }
        } catch (Exception e) {
            // Set default values on exception
            if (tvInstructorName != null) tvInstructorName.setText("Prof. Alex Morgan");
            if (tvInstructorTitle != null) tvInstructorTitle.setText("Senior Software Engineer");
            if (rbInstructorRating != null) rbInstructorRating.setRating(4.8f);
            if (tvInstructorRatingText != null) tvInstructorRatingText.setText("4.8 (1,245 reviews)");
            e.printStackTrace();
        }
    }

    private void updatePriceInfo(DocumentSnapshot documentSnapshot) {
        try {
            if (documentSnapshot == null || tvCoursePrice == null) {
                if (tvCoursePrice != null) tvCoursePrice.setText("$49.99");
                return;
            }

            // Get price data
            Double price = documentSnapshot.getDouble("price");

            // Set default values if null
            if (price == null) price = 49.99;

            // Update UI
            tvCoursePrice.setText("$" + price);
        } catch (Exception e) {
            if (tvCoursePrice != null) tvCoursePrice.setText("$49.99");
            e.printStackTrace();
        }
    }

    private void formatAndSetOutline(String outlineText) {
        // This method is empty in the original code, just leave it as a stub
        // for future implementation
    }

    private void enrollInCourse() {
        try {
            if (db == null) {
                Toast.makeText(this, "Firebase not initialized", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser == null) {
                Toast.makeText(this, "Please sign in to enroll in this course", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update Firestore
            db.collection("Course").document(String.valueOf(courseId))
                    .update("members", FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> {
                        if (!isEnrolled) {
                            // Add the course to user's enrolled courses
                            db.collection("users").document(currentUser.getUid())
                                    .update("courses", FieldValue.arrayUnion(courseId))
                                    .addOnSuccessListener(aVoid1 -> {
                                        // Navigate to the topic list
                                        Intent intent = new Intent(Description.this, TopicList.class);
                                        intent.putExtra("courseID", courseId);
                                        startActivity(intent);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(Description.this, "Failed to update user profile", Toast.LENGTH_SHORT).show();
                                    });
                        }
                        else {
                            // Navigate to the topic list
                            Intent intent = new Intent(Description.this, TopicList.class);
                            intent.putExtra("courseID", courseId);
                            startActivity(intent);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Description.this, "Failed to enroll in course", Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error during enrollment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void checkEnrollmentStatus(int courseId) {
        try {
            if (db == null || currentUser == null) {
                if (btnEnrollNow != null) btnEnrollNow.setText("SIGN IN TO ENROLL");
                return;
            }

            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            List<Long> enrolledCourses = (List<Long>) documentSnapshot.get("courses");

                            if (enrolledCourses != null && enrolledCourses.contains((long) courseId)) {
                                if (btnEnrollNow != null) {
                                    btnEnrollNow.setText("CONTINUE LEARNING");
                                    isEnrolled = true;
                                }
                            } else {
                                if (btnEnrollNow != null) btnEnrollNow.setText("ENROLL NOW");
                            }
                        } else {
                            if (btnEnrollNow != null) btnEnrollNow.setText("ENROLL NOW");
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (btnEnrollNow != null) btnEnrollNow.setText("ENROLL NOW");
                    });
        } catch (Exception e) {
            if (btnEnrollNow != null) btnEnrollNow.setText("ENROLL NOW");
            e.printStackTrace();
        }
    }

    private void toggleFavorite() {
        try {
            if (db == null) {
                Toast.makeText(this, "Firebase not initialized", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUser == null) {
                Toast.makeText(this, "Please sign in to add to favorites", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFavorite) {
                // Remove from favorites
                db.collection("users").document(currentUser.getUid())
                        .update("favorites", FieldValue.arrayRemove(courseId))
                        .addOnSuccessListener(aVoid -> {
                            isFavorite = false;
                            if (fabFavorite != null) fabFavorite.setImageResource(R.drawable.ic_favorite_border);
                            Toast.makeText(Description.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(Description.this, "Failed to update favorites", Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Add to favorites
                db.collection("users").document(currentUser.getUid())
                        .update("favorites", FieldValue.arrayUnion(courseId))
                        .addOnSuccessListener(aVoid -> {
                            isFavorite = true;
                            if (fabFavorite != null) fabFavorite.setImageResource(R.drawable.ic_favorite);
                            Toast.makeText(Description.this, "Added to favorites", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(Description.this, "Failed to update favorites", Toast.LENGTH_SHORT).show();
                        });
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error updating favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void checkFavoriteStatus(int courseId) {
        try {
            if (db == null || currentUser == null || fabFavorite == null) {
                return;
            }

            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            List<Long> favorites = (List<Long>) documentSnapshot.get("favorites");

                            if (favorites != null && favorites.contains((long) courseId)) {
                                isFavorite = true;
                                fabFavorite.setImageResource(R.drawable.ic_favorite);
                            } else {
                                isFavorite = false;
                                fabFavorite.setImageResource(R.drawable.ic_favorite_border);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        isFavorite = false;
                        fabFavorite.setImageResource(R.drawable.ic_favorite_border);
                    });
        } catch (Exception e) {
            if (fabFavorite != null) {
                isFavorite = false;
                fabFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
            e.printStackTrace();
        }
    }

    private void handleInvalidCourse() {
        try {
            if (courseTitle != null) courseTitle.setText(R.string.course_not_found);
            if (courseDescription != null) courseDescription.setText("");
            if (courseOutline != null) courseOutline.setText("");
            if (ivCourseBanner != null) ivCourseBanner.setImageResource(R.drawable.course_error);

            if (btnEnrollNow != null) btnEnrollNow.setEnabled(false);
            if (fabFavorite != null) fabFavorite.setVisibility(View.GONE);
        } catch (Exception e) {
            Toast.makeText(this, "Error handling invalid course", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}