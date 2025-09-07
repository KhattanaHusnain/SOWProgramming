package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sowp.user.R;
import com.sowp.user.adapters.CourseProgressAdapter;
import com.sowp.user.models.CourseProgress;
import com.sowp.user.repositories.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoursesProgressActivity extends AppCompatActivity implements CourseProgressAdapter.OnItemClickListener {

    private static final int ITEMS_PER_PAGE = 10;

    // UI Components
    private Toolbar toolbar;
    private Spinner spinnerStatusFilter;
    private Button btnApplyFilter;
    private TextView tvTotalCourses;
    private TextView tvEnrolledCourses;
    private TextView tvCompletedCourses;
    private RecyclerView recyclerViewProgress;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View layoutEmptyState;
    private TextView tvEmptyMessage;
    private ProgressBar progressBarLoading;

    // Adapter and Data
    private CourseProgressAdapter adapter;
    private UserRepository userRepository;

    // Pagination and Filter
    private String currentFilter = "All";
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private int currentPage = 0;
    private List<CourseProgress> allProgressData = new ArrayList<>(); // Store all data

    // Statistics
    private int totalCoursesCount = 0;
    private int enrolledCoursesCount = 0;
    private int completedCoursesCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses_progress);

        initializeViews();
        setupToolbar();
        setupSpinner();
        setupRecyclerView();
        setupListeners();
        initializeRepository();

        loadInitialData();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerStatusFilter = findViewById(R.id.spinnerStatusFilter);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        tvTotalCourses = findViewById(R.id.tvTotalCourses);
        tvEnrolledCourses = findViewById(R.id.tvEnrolledCourses);
        tvCompletedCourses = findViewById(R.id.tvCompletedCourses);
        recyclerViewProgress = findViewById(R.id.recyclerViewProgress);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        progressBarLoading = findViewById(R.id.progressBarLoading);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Courses Progress");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        List<String> filterOptions = Arrays.asList("All", "Enrolled", "Completed", "Unenrolled");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filterOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatusFilter.setAdapter(spinnerAdapter);
    }

    private void setupRecyclerView() {
        adapter = new CourseProgressAdapter(this);
        adapter.setOnItemClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewProgress.setLayoutManager(layoutManager);
        recyclerViewProgress.setAdapter(adapter);

        // Add scroll listener for pagination
        recyclerViewProgress.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2) {
                        loadMoreData();
                    }
                }
            }
        });
    }

    private void setupListeners() {
        btnApplyFilter.setOnClickListener(v -> applyFilter());

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        spinnerStatusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Auto-apply filter when selection changes
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void initializeRepository() {
        userRepository = new UserRepository(this);
    }

    private void loadInitialData() {
        showLoading(true);
        resetPagination();
        loadCourseProgress();
        loadStatistics();
    }

    private void applyFilter() {
        String selectedFilter = spinnerStatusFilter.getSelectedItem().toString();
        if (!currentFilter.equals(selectedFilter)) {
            currentFilter = selectedFilter;
            refreshData();
        }
    }

    private void refreshData() {
        resetPagination();
        allProgressData.clear();
        adapter.clearList();
        loadCourseProgress();
        loadStatistics();
    }

    private void resetPagination() {
        currentPage = 0;
        hasMoreData = true;
        isLoading = false;
    }

    private void loadCourseProgress() {
        if (isLoading) return;

        // Use synchronized block to prevent concurrent access
        synchronized (this) {
            if (isLoading) return;
            isLoading = true;
        }

        // Load a large number to get all data, then handle pagination locally
        int loadLimit = 1000; // Load all available data

        userRepository.getAllCourseProgress(currentFilter, loadLimit, new UserRepository.CourseProgressListCallback() {
            @Override
            public void onSuccess(List<CourseProgress> progressList) {
                synchronized (CoursesProgressActivity.this) {
                    isLoading = false;
                }

                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showLoading(false);

                    // Store all data with defensive copy
                    allProgressData = progressList != null ? new ArrayList<>(progressList) : new ArrayList<>();

                    // Show first page
                    displayPageData();

                    updateEmptyState(allProgressData.isEmpty());
                });
            }

            @Override
            public void onFailure(String message) {
                synchronized (CoursesProgressActivity.this) {
                    isLoading = false;
                }

                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showLoading(false);

                    showToast("Failed to load course progress: " + message);
                    updateEmptyState(true);
                });
            }
        });
    }

    private void displayPageData() {
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allProgressData.size());

        if (startIndex >= allProgressData.size()) {
            hasMoreData = false;
            return;
        }

        List<CourseProgress> pageData = allProgressData.subList(startIndex, endIndex);

        if (currentPage == 0) {
            adapter.setProgressList(pageData);
        } else {
            adapter.addProgressList(pageData);
        }

        currentPage++;
        hasMoreData = endIndex < allProgressData.size();
    }

    private void loadMoreData() {
        if (!hasMoreData || isLoading) return;
        displayPageData();
    }

    private void loadStatistics() {
        // Load statistics for all statuses
        loadStatisticsForStatus("All", (count) -> {
            totalCoursesCount = count;
            tvTotalCourses.setText(String.valueOf(count));
        });

        loadStatisticsForStatus("Enrolled", (count) -> {
            enrolledCoursesCount = count;
            tvEnrolledCourses.setText(String.valueOf(count));
        });

        loadStatisticsForStatus("Completed", (count) -> {
            completedCoursesCount = count;
            tvCompletedCourses.setText(String.valueOf(count));
        });
    }

    private void loadStatisticsForStatus(String status, StatisticsCallback callback) {
        userRepository.getAllCourseProgress(status, 1000, new UserRepository.CourseProgressListCallback() {
            @Override
            public void onSuccess(List<CourseProgress> progressList) {
                callback.onResult(progressList.size());
            }

            @Override
            public void onFailure(String message) {
                callback.onResult(0);
            }
        });
    }

    private interface StatisticsCallback {
        void onResult(int count);
    }

    private void showLoading(boolean show) {
        progressBarLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewProgress.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerViewProgress.setVisibility(View.GONE);

            // Update empty message based on filter
            String emptyMessage;
            switch (currentFilter) {
                case "Enrolled":
                    emptyMessage = "You are not currently enrolled in any courses.";
                    break;
                case "Completed":
                    emptyMessage = "You haven't completed any courses yet.";
                    break;
                case "Unenrolled":
                    emptyMessage = "You haven't unenrolled from any courses.";
                    break;
                default:
                    emptyMessage = "You haven't enrolled in any courses yet.";
                    break;
            }
            tvEmptyMessage.setText(emptyMessage);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerViewProgress.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onViewDetailsClick(CourseProgress courseProgress) {
        // Navigate to CourseDescriptionActivity
        Intent intent = new Intent(this, CourseDescriptionActivity.class);
        intent.putExtra("COURSE_ID", courseProgress.getCourseId());
        startActivity(intent);
    }

    @Override
    public void onItemClick(CourseProgress courseProgress) {
        // Navigate to CourseDescriptionActivity on item click
        Intent intent = new Intent(this, CourseDescriptionActivity.class);
        intent.putExtra("COURSE_ID", courseProgress.getCourseId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity in case progress changed
        refreshData();
    }
}