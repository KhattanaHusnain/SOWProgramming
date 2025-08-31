//package com.sowp.user.presenters.activities;
//
//import android.os.Bundle;
//import android.view.View;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.lifecycle.LiveData;
//import androidx.lifecycle.Observer;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.sowp.user.R;
//import com.sowp.user.adapters.CourseAdapter;
//import com.sowp.user.database.AppDatabase;
//import com.sowp.user.models.Course;
//import com.sowp.user.repositories.roomdb.CourseDao;
//import com.google.android.material.appbar.MaterialToolbar;
//import com.google.android.material.progressindicator.CircularProgressIndicator;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class OfflineCourseActivity extends AppCompatActivity {
//
//    // Views from XML layout
//    private MaterialToolbar toolbar;
//    private TextView courseCountTextView;
//    private LinearLayout emptyStateLayout;
//    private RecyclerView recyclerView;
//    private CircularProgressIndicator progressIndicator;
//
//    // Data components
//    private CourseAdapter courseAdapter;
//    private CourseDao courseDao;
//    private LiveData<List<Course>> courses;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_offline_course);
//
//        // Handle window insets for edge-to-edge display
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Initialize views
//        initializeViews();
//
//        // Setup toolbar
//        setupToolbar();
//
//        // Initialize database
//        courseDao = AppDatabase.getInstance(this).courseDao();
//
//        // Setup RecyclerView
//        setupRecyclerView();
//
//        // Observe courses data
//        observeCoursesData();
//
//        // Show loading initially
//        showLoading(true);
//    }
//
//    private void initializeViews() {
//        toolbar = findViewById(R.id.toolbar);
//        courseCountTextView = findViewById(R.id.courseCountTextView);
//        emptyStateLayout = findViewById(R.id.emptyStateLayout);
//        recyclerView = findViewById(R.id.recycler_view);
//        progressIndicator = findViewById(R.id.progressIndicator);
//    }
//
//    private void setupToolbar() {
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
//        }
//
//        toolbar.setNavigationOnClickListener(v -> onBackPressed());
//    }
//
//    private void setupRecyclerView() {
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        courseAdapter = new CourseAdapter(this, new ArrayList<>(), "OFFLINE");
//        recyclerView.setAdapter(courseAdapter);
//    }
//
//
//    private void observeCoursesData() {
//        courses = courseDao.getAllCoursesLive();
//        courses.observe(this, new Observer<List<Course>>() {
//            @Override
//            public void onChanged(List<Course> courseList) {
//                showLoading(false);
//
//                if (courseList != null && !courseList.isEmpty()) {
//                    // Show courses
//                    showEmptyState(false);
//                    courseAdapter.updateData(courseList);
//                    updateCourseCount(courseList.size());
//                } else {
//                    // Show empty state
//                    showEmptyState(true);
//                    updateCourseCount(0);
//                }
//            }
//        });
//    }
//
//    private void showLoading(boolean show) {
//        progressIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
//        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
//    }
//
//    private void showEmptyState(boolean show) {
//        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
//        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
//    }
//
//    private void updateCourseCount(int count) {
//        if (count == 0) {
//            courseCountTextView.setText("No courses available offline");
//        } else if (count == 1) {
//            courseCountTextView.setText("1 course available");
//        } else {
//            courseCountTextView.setText(count + " courses available");
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // Remove observers to prevent memory leaks
//        if (courses != null) {
//            courses.removeObservers(this);
//        }
//    }
//}