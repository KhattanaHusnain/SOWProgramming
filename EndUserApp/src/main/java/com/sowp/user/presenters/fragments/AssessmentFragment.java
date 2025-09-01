package com.sowp.user.presenters.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sowp.user.R;
import com.sowp.user.adapters.CourseAdapter;
import com.sowp.user.models.Course;
import com.sowp.user.models.User;
import com.sowp.user.presenters.activities.ViewAssignmentsActivity;
import com.sowp.user.presenters.activities.ViewQuizzesActivity;
import com.sowp.user.repositories.firebase.CourseRepository;
import com.sowp.user.repositories.firebase.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssessmentFragment extends Fragment implements CourseAdapter.OnCourseClickListener {
    private static final String TAG = "AssessmentFragment";
    private static final int PAGE_SIZE = 10;

    // Views
    private TextView quizAvgTextView;
    private TextView assignmentAvgTextView;
    private TextView totalCoursesTextView;
    private RecyclerView coursesRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar loadingProgressBar;
    private TextView emptyStateTextView;

    // Data and repositories
    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private CourseAdapter courseAdapter;
    private List<Course> allCourses;
    private List<Course> enrolledCourses;
    private User currentUser;

    // Pagination
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assessment, container, false);
        initViews(view);
        initRepositories();
        setupRecyclerView();
        setupSwipeRefresh();
        loadData();
        return view;
    }

    private void initViews(View view) {
        quizAvgTextView = view.findViewById(R.id.quizAvgTextView);
        assignmentAvgTextView = view.findViewById(R.id.assignmentAvgTextView);
        totalCoursesTextView = view.findViewById(R.id.totalCoursesTextView);
        coursesRecyclerView = view.findViewById(R.id.coursesRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
    }

    private void initRepositories() {
        userRepository = new UserRepository(getContext());
        courseRepository = new CourseRepository(getContext());
        allCourses = new ArrayList<>();
        enrolledCourses = new ArrayList<>();
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(getContext(), enrolledCourses, this);
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        coursesRecyclerView.setAdapter(courseAdapter);

        // Add pagination scroll listener
        coursesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMoreData) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreCourses();
                    }
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage = 0;
            hasMoreData = true;
            enrolledCourses.clear();
            courseAdapter.notifyDataSetChanged();
            loadData();
        });
    }

    private void loadData() {
        showLoading(true);
        loadUserData();
    }

    private void loadUserData() {
        userRepository.loadUserData(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                updateUserPerformance(user);
                loadAllCourses();
            }

            @Override
            public void onFailure(String message) {
                showLoading(false);
                showError("Failed to load user data: " + message);
                Log.e(TAG, "Failed to load user data: " + message);
            }
        });
    }

    private void updateUserPerformance(User user) {
        if (getContext() == null) return;

        // Update quiz average
        float quizAvg = user.getQuizzesAvg();
        quizAvgTextView.setText(String.format(Locale.getDefault(), "%.1f%%", quizAvg));

        // Update assignment average
        float assignmentAvg = user.getAssignmentAvg();
        assignmentAvgTextView.setText(String.format(Locale.getDefault(), "%.1f%%", assignmentAvg));

        // Update total enrolled courses count
        List<Integer> enrolledCourseIds = user.getEnrolledCourses();
        int totalEnrolled = enrolledCourseIds != null ? enrolledCourseIds.size() : 0;
        totalCoursesTextView.setText(String.valueOf(totalEnrolled));
    }

    private void loadAllCourses() {
        courseRepository.loadCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                allCourses.clear();
                allCourses.addAll(courses);
                filterEnrolledCourses();
                loadMoreCourses();
            }

            @Override
            public void onFailure(String message) {
                showLoading(false);
                showError("Failed to load courses: " + message);
                Log.e(TAG, "Failed to load courses: " + message);
            }
        });
    }

    private void filterEnrolledCourses() {
        if (currentUser == null || currentUser.getEnrolledCourses() == null) {
            showLoading(false);
            showEmptyState(true);
            return;
        }

        List<Integer> enrolledIds = currentUser.getEnrolledCourses();
        List<Course> filteredCourses = new ArrayList<>();

        for (Course course : allCourses) {
            for (Integer enrolledId : enrolledIds) {
                if (enrolledId == course.getId()) {
                    filteredCourses.add(course);
                    break;
                }
            }
        }

        allCourses.clear();
        allCourses.addAll(filteredCourses);
    }

    private void loadMoreCourses() {
        if (isLoading || !hasMoreData) return;

        isLoading = true;

        int startIndex = currentPage * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, allCourses.size());

        if (startIndex >= allCourses.size()) {
            hasMoreData = false;
            isLoading = false;
            showLoading(false);
            return;
        }

        List<Course> pageData = allCourses.subList(startIndex, endIndex);
        enrolledCourses.addAll(pageData);

        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                courseAdapter.notifyDataSetChanged();
                currentPage++;
                isLoading = false;
                showLoading(false);
                swipeRefreshLayout.setRefreshing(false);

                // Check if we have more data
                if (endIndex >= allCourses.size()) {
                    hasMoreData = false;
                }

                // Show empty state if no courses
                showEmptyState(enrolledCourses.isEmpty());
            });
        }
    }

    @Override
    public void onCourseClick(Course course) {
        showAssessmentTypeDialog(course);
    }

    private void showAssessmentTypeDialog(Course course) {
        if (getContext() == null) return;

        String[] options = {"Quizzes", "Assignments"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose Assessment Type");
        builder.setItems(options, (dialog, which) -> {
            Intent intent;
            switch (which) {
                case 0: // Quizzes
                    intent = new Intent(getContext(), ViewQuizzesActivity.class);
                    break;
                case 1: // Assignments
                    intent = new Intent(getContext(), ViewAssignmentsActivity.class);
                    break;
                default:
                    return;
            }
            intent.putExtra("COURSE_ID", course.getId());
            startActivity(intent);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showLoading(boolean show) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showEmptyState(boolean show) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (emptyStateTextView != null) {
                emptyStateTextView.setVisibility(show ? View.VISIBLE : View.GONE);
                emptyStateTextView.setText("No enrolled courses found. Enroll in courses to view your assessments.");
            }
            if (coursesRecyclerView != null) {
                coursesRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    public void refreshData() {
        currentPage = 0;
        hasMoreData = true;
        enrolledCourses.clear();
        if (courseAdapter != null) {
            courseAdapter.notifyDataSetChanged();
        }
        loadData();
    }
}