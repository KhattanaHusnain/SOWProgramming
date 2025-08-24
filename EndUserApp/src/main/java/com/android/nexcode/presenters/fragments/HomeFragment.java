package com.android.nexcode.presenters.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.nexcode.adapters.HomeAdapter;
import com.android.nexcode.adapters.QuizAdapter;
import com.android.nexcode.adapters.AssignmentAdapter;
import com.android.nexcode.R;
import com.android.nexcode.models.Quiz;
import com.android.nexcode.models.Assignment;
import com.android.nexcode.models.Course;
import com.android.nexcode.models.User;

import com.android.nexcode.presenters.activities.Main;
import com.android.nexcode.presenters.activities.QuizHistoryActivity;
import com.android.nexcode.repositories.firebase.AssignmentRepository;
import com.android.nexcode.repositories.firebase.CourseRepository;
import com.android.nexcode.repositories.firebase.QuizRepository;
import com.android.nexcode.repositories.firebase.UserRepository;
import com.android.nexcode.utils.UserAuthenticationUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "HomeFragment";
    private static final int GRID_SPAN_COUNT = 2;
    private static final long REFRESH_DELAY_MS = 1000; // Minimum refresh duration for better UX

    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private QuizRepository quizRepository;
    private AssignmentRepository assignmentRepository;
    private UserAuthenticationUtils userAuthenticationUtils;

    // Firebase
    private FirebaseAuth auth;

    // Adapters
    private HomeAdapter coursesAdapter;
    private QuizAdapter quizAdapter;
    private AssignmentAdapter assignmentAdapter;

    // RecyclerViews
    private RecyclerView popularCoursesRecycler;
    private RecyclerView quizzesRecycler;
    private RecyclerView assignmentsRecycler;

    // Profile Views
    private CircleImageView profileImage;
    private TextView userFullName;
    private TextView userDegree;
    private TextView userEmail;
    private ImageView notificationBell;

    // Dashboard Stats Views
    private TextView completedCoursesCount;
    private TextView enrolledCoursesCount;
    private TextView certificatesCount;
    private TextView quizScore;
    private TextView assignmentScore;

    // See All buttons
    private TextView seeAllPopular;
    private TextView seeAllQuizzes;
    private TextView seeAllAssignments;

    // Shimmer Loading Views
    private ShimmerFrameLayout profileShimmer;
    private ShimmerFrameLayout dashboardShimmer;
    private ShimmerFrameLayout coursesShimmer;
    private ShimmerFrameLayout quizzesShimmer;
    private ShimmerFrameLayout assignmentsShimmer;

    // Content containers
    private View profileContent;
    private View dashboardContent;

    // SwipeRefreshLayout
    private SwipeRefreshLayout swipeRefreshLayout;

    // Track refresh state
    private boolean isRefreshing = false;
    private int refreshTasksCompleted = 0;
    private final int totalRefreshTasks = 4; // User data, courses, quizzes, assignments

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initialize(view);
        showSkeletonLoading();
        loadUserData();
        loadDashboardData();
        setupClickListeners();

        return view;
    }

    private void initialize(View view) {
        // Initialize Firebase
        userRepository = new UserRepository(getContext());
        userAuthenticationUtils = new UserAuthenticationUtils(getContext());
        courseRepository = new CourseRepository(getContext());
        quizRepository = new QuizRepository(getContext());
        assignmentRepository = new AssignmentRepository(getContext());

        auth = FirebaseAuth.getInstance();

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorPrimaryDark,
                R.color.colorAccent
        );

        // Profile Views
        profileImage = view.findViewById(R.id.profile_image);
        userFullName = view.findViewById(R.id.user_full_name);
        userDegree = view.findViewById(R.id.user_degree);
        userEmail = view.findViewById(R.id.user_email);
        notificationBell = view.findViewById(R.id.notification_bell);

        // Dashboard Stats Views
        completedCoursesCount = view.findViewById(R.id.completed_courses_count);
        enrolledCoursesCount = view.findViewById(R.id.enrolled_courses_count);
        certificatesCount = view.findViewById(R.id.certificates_count);
        quizScore = view.findViewById(R.id.quiz_score);
        assignmentScore = view.findViewById(R.id.assignment_score);

        // RecyclerViews
        popularCoursesRecycler = view.findViewById(R.id.popular_courses_recycler);
        quizzesRecycler = view.findViewById(R.id.quizzes_recycler);
        assignmentsRecycler = view.findViewById(R.id.assignments_recycler);

        // See All buttons
        seeAllPopular = view.findViewById(R.id.see_all_popular);
        seeAllQuizzes = view.findViewById(R.id.see_all_quizzes);
        seeAllAssignments = view.findViewById(R.id.see_all_assignments);

        // Shimmer Views
        profileShimmer = view.findViewById(R.id.profile_shimmer);
        dashboardShimmer = view.findViewById(R.id.dashboard_shimmer);
        coursesShimmer = view.findViewById(R.id.courses_shimmer);
        quizzesShimmer = view.findViewById(R.id.quizzes_shimmer);
        assignmentsShimmer = view.findViewById(R.id.assignments_shimmer);

        // Content containers
        profileContent = view.findViewById(R.id.profile_content);
        dashboardContent = view.findViewById(R.id.dashboard_content);

        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        // Setup Popular Courses RecyclerView (Grid Layout)
        popularCoursesRecycler.setLayoutManager(new GridLayoutManager(getContext(), GRID_SPAN_COUNT));
        coursesAdapter = new HomeAdapter(requireContext(), new ArrayList<>());
        popularCoursesRecycler.setAdapter(coursesAdapter);

        // Setup Quizzes RecyclerView (Horizontal Layout)
        quizzesRecycler.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        quizAdapter = new QuizAdapter(requireContext(), new ArrayList<>());
        quizzesRecycler.setAdapter(quizAdapter);

        // Setup Assignments RecyclerView (Horizontal Layout)
        assignmentsRecycler.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        assignmentAdapter = new AssignmentAdapter(requireContext(), new ArrayList<>());
        assignmentAdapter.setOnAssignmentItemClickListener(assignment -> {
            openAssignment(assignment);
        });
        assignmentsRecycler.setAdapter(assignmentAdapter);
    }

    @Override
    public void onRefresh() {
        if (isRefreshing) return;

        isRefreshing = true;
        refreshTasksCompleted = 0;

        Log.d(TAG, "Starting refresh...");

        // Don't show skeleton loading during refresh, just use the SwipeRefreshLayout indicator
        refreshAllData();
    }

    private void refreshAllData() {
        // Refresh user data
        refreshUserData();

        // Refresh dashboard data
        refreshPopularCourses();
        refreshRecentQuizzes();
        refreshRecentAssignments();
    }

    private void refreshUserData() {
        if (!userAuthenticationUtils.isUserLoggedIn()) {
            Log.w(TAG, "Current user ID is null");
            onRefreshTaskCompleted();
            return;
        }

        userRepository.loadUserData(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // Update profile UI
                userFullName.setText(user.getFullName() != null ? user.getFullName() : "User");
                userDegree.setText(user.getDegree() != null ? user.getDegree() : "No Degree Assigned");
                userEmail.setText(user.getEmail() != null ? user.getEmail() : "No Email Assigned");

                // Set profile image from Base64 string
                if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(user.getPhoto(), Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        profileImage.setImageBitmap(decodedBitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding profile image", e);
                        profileImage.setImageResource(R.drawable.ic_profile);
                    }
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile);
                }

                // Update dashboard stats
                completedCoursesCount.setText(user.getCompletedCourses() != null ? user.getCompletedCourses().size()+"" : "0");
                enrolledCoursesCount.setText(user.getEnrolledCourses() != null ? user.getEnrolledCourses().size()+"" : "0");
                quizScore.setText(String.format("%.0f%%", user.getQuizzesAvg()));
                assignmentScore.setText(String.format("%.0f%%", user.getAssignmentAvg()));
                certificatesCount.setText(user.getCertificates() != null ? user.getCertificates().size()+"" : "0");

                onRefreshTaskCompleted();
            }

            @Override
            public void onFailure(String message) {
                if (isRefreshing) {
                    Toast.makeText(getContext(), "Failed to refresh profile: " + message, Toast.LENGTH_SHORT).show();
                }

                userFullName.setText("User");
                userDegree.setText("");
                userEmail.setText(auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "");

                onRefreshTaskCompleted();
            }
        });
    }

    private void refreshPopularCourses() {
        courseRepository.loadPopularCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                coursesAdapter.updateCourses(courses);
                onRefreshTaskCompleted();
            }

            @Override
            public void onFailure(String message) {
                if (isRefreshing) {
                    Toast.makeText(getContext(), "Failed to refresh courses: " + message, Toast.LENGTH_SHORT).show();
                }
                onRefreshTaskCompleted();
            }
        });
    }

    private void refreshRecentQuizzes() {
        quizRepository.loadRecentQuizzes(new QuizRepository.Callback() {
            @Override
            public void onSuccess(List<Quiz> quizzes) {
                quizAdapter.updateQuizzes(quizzes);
                onRefreshTaskCompleted();
            }

            @Override
            public void onFailure(String message) {
                if (isRefreshing) {
                    Toast.makeText(getContext(), "Failed to refresh quizzes: " + message, Toast.LENGTH_SHORT).show();
                }
                onRefreshTaskCompleted();
            }
        });
    }

    private void refreshRecentAssignments() {
        assignmentRepository.loadRecentAssignments(new AssignmentRepository.Callback() {
            @Override
            public void onSuccess(List<Assignment> assignments) {
                assignmentAdapter.updateAssignments(assignments);
                onRefreshTaskCompleted();
            }

            @Override
            public void onFailure(String message) {
                if (isRefreshing) {
                    Toast.makeText(getContext(), "Failed to refresh assignments: " + message, Toast.LENGTH_SHORT).show();
                }
                onRefreshTaskCompleted();
            }
        });
    }

    private void onRefreshTaskCompleted() {
        refreshTasksCompleted++;

        if (refreshTasksCompleted >= totalRefreshTasks) {
            // All refresh tasks completed, stop the refresh indicator
            // Add a small delay for better UX
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                isRefreshing = false;
                Log.d(TAG, "Refresh completed");
            }, REFRESH_DELAY_MS);
        }
    }

    private void showSkeletonLoading() {
        // Show shimmer effects and hide content
        profileShimmer.setVisibility(View.VISIBLE);
        profileShimmer.startShimmer();
        profileContent.setVisibility(View.GONE);

        dashboardShimmer.setVisibility(View.VISIBLE);
        dashboardShimmer.startShimmer();
        dashboardContent.setVisibility(View.GONE);

        coursesShimmer.setVisibility(View.VISIBLE);
        coursesShimmer.startShimmer();
        popularCoursesRecycler.setVisibility(View.GONE);

        quizzesShimmer.setVisibility(View.VISIBLE);
        quizzesShimmer.startShimmer();
        quizzesRecycler.setVisibility(View.GONE);

        assignmentsShimmer.setVisibility(View.VISIBLE);
        assignmentsShimmer.startShimmer();
        assignmentsRecycler.setVisibility(View.GONE);
    }

    private void hideProfileSkeleton() {
        profileShimmer.stopShimmer();
        profileShimmer.setVisibility(View.GONE);
        profileContent.setVisibility(View.VISIBLE);
    }

    private void hideDashboardSkeleton() {
        dashboardShimmer.stopShimmer();
        dashboardShimmer.setVisibility(View.GONE);
        dashboardContent.setVisibility(View.VISIBLE);
    }

    private void hideCoursesSkeleton() {
        coursesShimmer.stopShimmer();
        coursesShimmer.setVisibility(View.GONE);
        popularCoursesRecycler.setVisibility(View.VISIBLE);
    }

    private void hideQuizzesSkeleton() {
        quizzesShimmer.stopShimmer();
        quizzesShimmer.setVisibility(View.GONE);
        quizzesRecycler.setVisibility(View.VISIBLE);
    }

    private void hideAssignmentsSkeleton() {
        assignmentsShimmer.stopShimmer();
        assignmentsShimmer.setVisibility(View.GONE);
        assignmentsRecycler.setVisibility(View.VISIBLE);
    }

    private void loadUserData() {
        if (!userAuthenticationUtils.isUserLoggedIn()) {
            Log.w(TAG, "Current user ID is null");
            hideProfileSkeleton();
            hideDashboardSkeleton();
            return;
        }

        userRepository.loadUserData(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // Update profile UI
                userFullName.setText(user.getFullName() != null ? user.getFullName() : "User");
                userDegree.setText(user.getDegree() != null ? user.getDegree() : "No Degree Assigned");
                userEmail.setText(user.getEmail() != null ? user.getEmail() : "No Email Assigned");

                // Set profile image from Base64 string
                if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(user.getPhoto(), Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        profileImage.setImageBitmap(decodedBitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding profile image", e);
                        profileImage.setImageResource(R.drawable.ic_profile);
                    }
                } else {
                    profileImage.setImageResource(R.drawable.ic_profile);
                }

                // Update dashboard stats
                completedCoursesCount.setText(user.getCompletedCourses() != null ? user.getCompletedCourses().size()+"" : "0");
                enrolledCoursesCount.setText(user.getEnrolledCourses() != null ? user.getEnrolledCourses().size()+"" : "0");
                quizScore.setText(String.format("%.0f%%", user.getQuizzesAvg()));
                assignmentScore.setText(String.format("%.0f%%", user.getAssignmentAvg()));
                certificatesCount.setText(user.getCertificates() != null ? user.getCertificates().size()+"" : "0");

                // Hide skeleton loading
                hideProfileSkeleton();
                hideDashboardSkeleton();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                userFullName.setText("User");
                userDegree.setText("");
                userEmail.setText(auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "");

                // Hide skeleton loading even on failure
                hideProfileSkeleton();
                hideDashboardSkeleton();
            }
        });
    }

    private void loadDashboardData() {
        // Load content for RecyclerViews
        loadPopularCourses();
        loadRecentQuizzes();
        loadRecentAssignments();
    }

    private void loadPopularCourses() {
        courseRepository.loadPopularCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                coursesAdapter.updateCourses(courses);
                hideCoursesSkeleton();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                hideCoursesSkeleton();
            }
        });
    }

    private void loadRecentQuizzes() {
        quizRepository.loadRecentQuizzes(new QuizRepository.Callback() {
            @Override
            public void onSuccess(List<Quiz> quizzes) {
                quizAdapter.updateQuizzes(quizzes);
                hideQuizzesSkeleton();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                hideQuizzesSkeleton();
            }
        });
    }

    private void loadRecentAssignments() {
        assignmentRepository.loadRecentAssignments(new AssignmentRepository.Callback() {
            @Override
            public void onSuccess(List<Assignment> assignments) {
                assignmentAdapter.updateAssignments(assignments);
                hideAssignmentsSkeleton();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                hideAssignmentsSkeleton();
            }
        });
    }

    private void setupClickListeners() {
        // Profile image click - open profile
        profileImage.setOnClickListener(v -> openProfile());

        // Notification bell click
        notificationBell.setOnClickListener(v -> openNotifications());

        // See All buttons
        seeAllPopular.setOnClickListener(v -> navigateToAllCourses());
        seeAllQuizzes.setOnClickListener(v -> navigateToAllQuizzes());
        seeAllAssignments.setOnClickListener(v -> navigateToAllAssignments());

        // Dashboard stats click listeners for detailed views
        completedCoursesCount.setOnClickListener(v -> navigateToCompletedCourses());
        enrolledCoursesCount.setOnClickListener(v -> navigateToEnrolledCourses());
        certificatesCount.setOnClickListener(v -> navigateToCertificates());
        quizScore.setOnClickListener(v -> navigateToQuizHistory());
        assignmentScore.setOnClickListener(v -> navigateToAssignmentHistory());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Stop all shimmer effects to prevent memory leaks
        if (profileShimmer != null) profileShimmer.stopShimmer();
        if (dashboardShimmer != null) dashboardShimmer.stopShimmer();
        if (coursesShimmer != null) coursesShimmer.stopShimmer();
        if (quizzesShimmer != null) quizzesShimmer.stopShimmer();
        if (assignmentsShimmer != null) assignmentsShimmer.stopShimmer();

        // Clean up refresh state
        isRefreshing = false;
        refreshTasksCompleted = 0;
    }

    // Navigation methods
    private void openProfile() {
        ((Main) requireActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }

    private void openNotifications() {
        // Navigate to notifications screen
    }

    private void navigateToAllCourses() {
        ((Main) getActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_courses);
    }

    private void navigateToAllQuizzes() {
        ((Main) requireActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_quizzes);
    }

    private void navigateToAllAssignments() {
        ((Main) requireActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_quizzes);
    }

    private void navigateToCompletedCourses() {
        // Navigate to completed courses screen
    }

    private void navigateToEnrolledCourses() {
        // Navigate to enrolled courses screen
    }

    private void navigateToCertificates() {
        // Navigate to certificates screen
    }

    private void navigateToQuizHistory() {
        // Navigate to quiz history screen
        Intent intent = new Intent(getContext(), QuizHistoryActivity.class);
        startActivity(intent);
    }

    private void navigateToAssignmentHistory() {
        // Navigate to assignment history screen
    }

    private void openAssignment(Assignment assignment) {
        // Navigate to assignment details/submission screen
    }
}