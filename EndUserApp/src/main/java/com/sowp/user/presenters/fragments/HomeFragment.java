package com.sowp.user.presenters.fragments;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.sowp.user.R;
import com.sowp.user.adapters.CourseAdapter;
import com.sowp.user.adapters.QuizAdapter;
import com.sowp.user.adapters.AssignmentAdapter;
import com.sowp.user.models.Quiz;
import com.sowp.user.models.Assignment;
import com.sowp.user.models.Course;
import com.sowp.user.models.User;

import com.sowp.user.presenters.activities.AssignmentHistoryActivity;
import com.sowp.user.presenters.activities.Description;
import com.sowp.user.presenters.activities.Main;
import com.sowp.user.presenters.activities.QuizHistoryActivity;
import com.sowp.user.repositories.firebase.AssignmentRepository;
import com.sowp.user.repositories.firebase.CourseRepository;
import com.sowp.user.repositories.firebase.QuizRepository;
import com.sowp.user.repositories.firebase.UserRepository;
import com.sowp.user.utils.UserAuthenticationUtils;
import com.google.firebase.auth.FirebaseAuth;

import de.hdodenhof.circleimageview.CircleImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "HomeFragment";
    private static final long REFRESH_DELAY_MS = 1000;

    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private QuizRepository quizRepository;
    private AssignmentRepository assignmentRepository;
    private UserAuthenticationUtils userAuthenticationUtils;

    // Firebase
    private FirebaseAuth auth;

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

    // RecyclerViews and Adapters
    private RecyclerView coursesRecyclerView;
    private RecyclerView quizzesRecyclerView;
    private RecyclerView assignmentsRecyclerView;

    private CourseAdapter courseAdapter;
    private QuizAdapter quizAdapter;
    private AssignmentAdapter assignmentAdapter;

    // See All buttons
    private TextView seeAllPopular;
    private TextView seeAllQuizzes;
    private TextView seeAllAssignments;

    // SwipeRefreshLayout
    private SwipeRefreshLayout swipeRefreshLayout;

    // Track refresh state
    private boolean isRefreshing = false;
    private int refreshTasksCompleted = 0;
    private final int totalRefreshTasks = 4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initialize(view);
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
        coursesRecyclerView = view.findViewById(R.id.courses_recycler_view);
        quizzesRecyclerView = view.findViewById(R.id.quizzes_recycler_view);
        assignmentsRecyclerView = view.findViewById(R.id.assignments_recycler_view);

        // See All buttons
        seeAllPopular = view.findViewById(R.id.see_all_popular);
        seeAllQuizzes = view.findViewById(R.id.see_all_quizzes);
        seeAllAssignments = view.findViewById(R.id.see_all_assignments);

        // Initialize RecyclerViews
        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        // Setup Courses RecyclerView
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        coursesRecyclerView.setNestedScrollingEnabled(false);
        courseAdapter = new CourseAdapter(getContext(), new ArrayList<>(), new CourseAdapter.OnCourseClickListener() {
            @Override
            public void onCourseClick(Course course) {
                openCourse(course);
            }
        });
        coursesRecyclerView.setAdapter(courseAdapter);

        // Setup Quizzes RecyclerView
        quizzesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        quizzesRecyclerView.setNestedScrollingEnabled(false);
        quizAdapter = new QuizAdapter(getContext(), new ArrayList<>(), new QuizAdapter.OnQuizClickListener() {
            @Override
            public void onQuizClick(Quiz quiz) {
                openQuiz(quiz);
            }
        });
        quizzesRecyclerView.setAdapter(quizAdapter);

        // Setup Assignments RecyclerView
        assignmentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        assignmentsRecyclerView.setNestedScrollingEnabled(false);
        assignmentAdapter = new AssignmentAdapter(getContext(), new ArrayList<>(), new AssignmentAdapter.OnAssignmentClickListener() {
            @Override
            public void onAssignmentClick(Assignment assignment) {
                openAssignment(assignment);
            }
        });
        assignmentsRecyclerView.setAdapter(assignmentAdapter);
    }

    @Override
    public void onRefresh() {
        if (isRefreshing) return;

        isRefreshing = true;
        refreshTasksCompleted = 0;

        Log.d(TAG, "Starting refresh...");
        refreshAllData();
    }

    private void refreshAllData() {
        refreshUserData();
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
                updateUserProfileUI(user);
                updateDashboardStatsUI(user);
                onRefreshTaskCompleted();
            }

            @Override
            public void onFailure(String message) {
                if (isRefreshing) {
                    Toast.makeText(getContext(), "Failed to refresh profile: " + message, Toast.LENGTH_SHORT).show();
                }
                setDefaultUserData();
                onRefreshTaskCompleted();
            }
        });
    }

    private void refreshPopularCourses() {
        courseRepository.loadPopularCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                displayCourses(courses);
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
                displayQuizzes(quizzes);
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
                displayAssignments(assignments);
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
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                isRefreshing = false;
                Log.d(TAG, "Refresh completed");
            }, REFRESH_DELAY_MS);
        }
    }

    private void loadUserData() {
        if (!userAuthenticationUtils.isUserLoggedIn()) {
            Log.w(TAG, "Current user ID is null");
            return;
        }

        userRepository.loadUserData(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                updateUserProfileUI(user);
                updateDashboardStatsUI(user);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                setDefaultUserData();
            }
        });
    }

    private void loadDashboardData() {
        loadPopularCourses();
        loadRecentQuizzes();
        loadRecentAssignments();
    }

    private void loadPopularCourses() {
        courseRepository.loadPopularCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                displayCourses(courses);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecentQuizzes() {
        quizRepository.loadRecentQuizzes(new QuizRepository.Callback() {
            @Override
            public void onSuccess(List<Quiz> quizzes) {
                displayQuizzes(quizzes);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecentAssignments() {
        assignmentRepository.loadRecentAssignments(new AssignmentRepository.Callback() {
            @Override
            public void onSuccess(List<Assignment> assignments) {
                displayAssignments(assignments);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserProfileUI(User user) {
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
    }

    private void updateDashboardStatsUI(User user) {
        completedCoursesCount.setText(user.getCompletedCourses() != null ? user.getCompletedCourses().size() + "" : "0");
        enrolledCoursesCount.setText(user.getEnrolledCourses() != null ? user.getEnrolledCourses().size() + "" : "0");
        quizScore.setText(String.format("%.0f%%", user.getQuizzesAvg()));
        assignmentScore.setText(String.format("%.0f%%", user.getAssignmentAvg()));
        certificatesCount.setText(user.getCertificates() != null ? user.getCertificates().size() + "" : "0");
    }

    private void setDefaultUserData() {
        userFullName.setText("User");
        userDegree.setText("");
        userEmail.setText(auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "");
        profileImage.setImageResource(R.drawable.ic_profile);

        // Reset dashboard stats
        completedCoursesCount.setText("0");
        enrolledCoursesCount.setText("0");
        certificatesCount.setText("0");
        quizScore.setText("0%");
        assignmentScore.setText("0%");
    }

    private void displayCourses(List<Course> courses) {
        // Limit to 2 courses for home screen
        List<Course> limitedCourses = courses.size() > 2 ? courses.subList(0, 2) : courses;
        courseAdapter.updateData(limitedCourses);
    }

    private void displayQuizzes(List<Quiz> quizzes) {
        // Limit to 2 quizzes for home screen
        List<Quiz> limitedQuizzes = quizzes.size() > 2 ? quizzes.subList(0, 2) : quizzes;
        quizAdapter.updateData(limitedQuizzes);
    }

    private void displayAssignments(List<Assignment> assignments) {
        // Limit to 2 assignments for home screen
        List<Assignment> limitedAssignments = assignments.size() > 2 ? assignments.subList(0, 2) : assignments;
        assignmentAdapter.updateData(limitedAssignments);
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
        Intent intent = new Intent(getContext(), QuizHistoryActivity.class);
        startActivity(intent);
    }

    private void navigateToAssignmentHistory() {
        Intent intent = new Intent(getContext(), AssignmentHistoryActivity.class);
        startActivity(intent);
    }

    private void openCourse(Course course) {
        // Navigate to course details screen
        Intent intent = new Intent(getContext(), Description.class);
        intent.putExtra("COURSE_ID", course.getId());
        startActivity(intent);
    }

    private void openQuiz(Quiz quiz) {
        // Navigate to quiz details/taking screen
    }

    private void openAssignment(Assignment assignment) {
        // Navigate to assignment details/submission screen
    }
}