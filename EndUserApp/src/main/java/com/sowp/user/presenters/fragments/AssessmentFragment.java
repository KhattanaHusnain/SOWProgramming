package com.sowp.user.presenters.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sowp.user.R;
import com.sowp.user.adapters.CourseAdapter;
import com.sowp.user.models.Course;
import com.sowp.user.models.User;
import com.sowp.user.presenters.activities.ViewAssignmentsActivity;
import com.sowp.user.presenters.activities.ViewQuizzesActivity;
import com.sowp.user.repositories.CourseRepository;
import com.sowp.user.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AssessmentFragment extends Fragment implements CourseAdapter.OnCourseClickListener, DefaultLifecycleObserver {
    private static final int PAGE_SIZE = 10;

    private TextView quizAvgTextView;
    private TextView assignmentAvgTextView;
    private TextView totalCoursesTextView;
    private RecyclerView coursesRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar loadingProgressBar;
    private TextView emptyStateTextView;

    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private CourseAdapter courseAdapter;
    private List<Course> allCourses;
    private List<Course> enrolledCourses;
    private User currentUser;

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
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLifecycle().addObserver(this);
        loadData();
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onCreate(owner);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onResume(owner);
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onPause(owner);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onDestroy(owner);
        cleanup();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getLifecycle().removeObserver(this);
        cleanup();
    }

    private void cleanup() {
        if (userRepository != null) {
            userRepository = null;
        }
        if (courseRepository != null) {
            courseRepository = null;
        }
        if (allCourses != null) {
            allCourses.clear();
            allCourses = null;
        }
        if (enrolledCourses != null) {
            enrolledCourses.clear();
            enrolledCourses = null;
        }
        currentUser = null;
        courseAdapter = null;
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
        if (userRepository == null) return;

        userRepository.loadUserData(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isAdded()) return;

                currentUser = user;
                updateUserPerformance(user);
                loadAllCourses();
            }

            @Override
            public void onFailure(String message) {
                if (!isAdded()) return;
                showLoading(false);
            }
        });
    }

    private void updateUserPerformance(User user) {
        if (!isAdded() || getContext() == null) return;

        float quizAvg = user.getQuizzesAvg();
        quizAvgTextView.setText(String.format(Locale.getDefault(), "%.1f%%", quizAvg));

        float assignmentAvg = user.getAssignmentAvg();
        assignmentAvgTextView.setText(String.format(Locale.getDefault(), "%.1f%%", assignmentAvg));

        List<Integer> enrolledCourseIds = user.getEnrolledCourses();
        int totalEnrolled = enrolledCourseIds != null ? enrolledCourseIds.size() : 0;
        totalCoursesTextView.setText(String.valueOf(totalEnrolled));
    }

    private void loadAllCourses() {
        if (courseRepository == null) return;

        courseRepository.loadCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                if (!isAdded()) return;

                allCourses.clear();
                allCourses.addAll(courses);
                filterEnrolledCourses();
                loadMoreCourses();
            }

            @Override
            public void onFailure(String message) {
                if (!isAdded()) return;
                showLoading(false);
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

        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;

                courseAdapter.notifyDataSetChanged();
                currentPage++;
                isLoading = false;
                showLoading(false);
                swipeRefreshLayout.setRefreshing(false);

                if (endIndex >= allCourses.size()) {
                    hasMoreData = false;
                }

                showEmptyState(enrolledCourses.isEmpty());
            });
        }
    }

    @Override
    public void onCourseClick(Course course) {
        showAssessmentTypeDialog(course);
    }

    private void showAssessmentTypeDialog(Course course) {
        if (!isAdded() || getContext() == null) return;

        String[] options = {"Quizzes", "Assignments"};

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Choose Assessment Type");
        builder.setItems(options, (dialog, which) -> {
            if (!isAdded()) return;

            Intent intent;
            switch (which) {
                case 0:
                    intent = new Intent(getContext(), ViewQuizzesActivity.class);
                    break;
                case 1:
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
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showEmptyState(boolean show) {
        if (!isAdded() || getActivity() == null) return;

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

    public void refreshData() {
        if (!isAdded()) return;

        currentPage = 0;
        hasMoreData = true;
        enrolledCourses.clear();
        if (courseAdapter != null) {
            courseAdapter.notifyDataSetChanged();
        }
        loadData();
    }
}