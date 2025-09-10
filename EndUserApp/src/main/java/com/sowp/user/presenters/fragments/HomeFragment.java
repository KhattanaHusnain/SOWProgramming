package com.sowp.user.presenters.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.widget.TextView;
import android.widget.ImageView;

import com.sowp.user.R;
import com.sowp.user.adapters.CourseAdapter;
import com.sowp.user.adapters.QuizAdapter;
import com.sowp.user.adapters.AssignmentAdapter;
import com.sowp.user.models.Quiz;
import com.sowp.user.models.Assignment;
import com.sowp.user.models.Course;
import com.sowp.user.models.User;

import com.sowp.user.presenters.activities.AssignmentHistoryActivity;
import com.sowp.user.presenters.activities.CourseDescriptionActivity;
import com.sowp.user.presenters.activities.CoursesProgressActivity;
import com.sowp.user.presenters.activities.Main;
import com.sowp.user.presenters.activities.QuizHistoryActivity;
import com.sowp.user.presenters.activities.ViewNotificationsActivity;
import com.sowp.user.repositories.AssignmentRepository;
import com.sowp.user.repositories.CourseRepository;
import com.sowp.user.repositories.QuizRepository;
import com.sowp.user.repositories.UserRepository;
import com.sowp.user.services.ImageService;
import com.sowp.user.services.UserAuthenticationUtils;
import com.google.firebase.auth.FirebaseAuth;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, DefaultLifecycleObserver {
    private static final long REFRESH_DELAY_MS = 1000;

    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private QuizRepository quizRepository;
    private AssignmentRepository assignmentRepository;
    private UserAuthenticationUtils userAuthenticationUtils;

    private FirebaseAuth auth;

    private CircleImageView profileImage;
    private TextView userFullName;
    private TextView userDegree;
    private TextView userEmail;
    private ImageView notificationBell;

    private TextView completedCoursesCount;
    private TextView enrolledCoursesCount;
    private TextView certificatesCount;
    private TextView quizScore;
    private TextView assignmentScore;

    private RecyclerView coursesRecyclerView;
    private RecyclerView quizzesRecyclerView;
    private RecyclerView assignmentsRecyclerView;

    private CourseAdapter courseAdapter;
    private QuizAdapter quizAdapter;
    private AssignmentAdapter assignmentAdapter;

    private TextView seeAllPopular;
    private TextView seeAllQuizzes;
    private TextView seeAllAssignments;

    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean isRefreshing = false;
    private int refreshTasksCompleted = 0;
    private final int totalRefreshTasks = 4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initialize(view);
        loadUserData();
        loadDashboardData();
        setupClickListeners();
        return view;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onResume(owner);
        refreshUserData();
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onPause(owner);
        isRefreshing = false;
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
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

    private void initialize(View view) {
        userRepository = new UserRepository(getContext());
        userAuthenticationUtils = new UserAuthenticationUtils(getContext());
        courseRepository = new CourseRepository(getContext());
        quizRepository = new QuizRepository(getContext());
        assignmentRepository = new AssignmentRepository(getContext());

        auth = FirebaseAuth.getInstance();

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.accent,
                R.color.primary_dark
        );

        profileImage = view.findViewById(R.id.profile_image);
        userFullName = view.findViewById(R.id.user_full_name);
        userDegree = view.findViewById(R.id.user_degree);
        userEmail = view.findViewById(R.id.user_email);
        notificationBell = view.findViewById(R.id.notification_bell);

        completedCoursesCount = view.findViewById(R.id.completed_courses_count);
        enrolledCoursesCount = view.findViewById(R.id.enrolled_courses_count);
        certificatesCount = view.findViewById(R.id.certificates_count);
        quizScore = view.findViewById(R.id.quiz_score);
        assignmentScore = view.findViewById(R.id.assignment_score);

        coursesRecyclerView = view.findViewById(R.id.courses_recycler_view);
        quizzesRecyclerView = view.findViewById(R.id.quizzes_recycler_view);
        assignmentsRecyclerView = view.findViewById(R.id.assignments_recycler_view);

        seeAllPopular = view.findViewById(R.id.see_all_popular);
        seeAllQuizzes = view.findViewById(R.id.see_all_quizzes);
        seeAllAssignments = view.findViewById(R.id.see_all_assignments);

        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        coursesRecyclerView.setNestedScrollingEnabled(false);
        courseAdapter = new CourseAdapter(getContext(), new ArrayList<>(), new CourseAdapter.OnCourseClickListener() {
            @Override
            public void onCourseClick(Course course) {
                openCourse(course);
            }
        });
        coursesRecyclerView.setAdapter(courseAdapter);

        quizzesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        quizzesRecyclerView.setNestedScrollingEnabled(false);
        quizAdapter = new QuizAdapter(getContext(), new ArrayList<>(), new QuizAdapter.OnQuizClickListener() {
            @Override
            public void onQuizClick(Quiz quiz) {
                openQuiz(quiz);
            }
        });
        quizzesRecyclerView.setAdapter(quizAdapter);

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
            }, REFRESH_DELAY_MS);
        }
    }

    private void loadUserData() {
        if (!userAuthenticationUtils.isUserLoggedIn()) {
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
            }
        });
    }

    private void updateUserProfileUI(User user) {
        userFullName.setText( !user.getFullName().isEmpty() ? user.getFullName() : "User");
        userDegree.setText( !user.getDegree().isEmpty() ? user.getDegree() : "No Degree Assigned");
        userEmail.setText( !user.getEmail().isEmpty() ? user.getEmail() : "No Email Assigned");

        if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
            try {
                profileImage.setImageBitmap(ImageService.base64ToBitmap(user.getPhoto()));
            } catch (Exception e) {
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

        completedCoursesCount.setText("0");
        enrolledCoursesCount.setText("0");
        certificatesCount.setText("0");
        quizScore.setText("0%");
        assignmentScore.setText("0%");
    }

    private void displayCourses(List<Course> courses) {
        List<Course> limitedCourses = courses.size() > 2 ? courses.subList(0, 2) : courses;
        courseAdapter.updateData(limitedCourses);
    }

    private void displayQuizzes(List<Quiz> quizzes) {
        List<Quiz> limitedQuizzes = quizzes.size() > 2 ? quizzes.subList(0, 2) : quizzes;
        quizAdapter.updateData(limitedQuizzes);
    }

    private void displayAssignments(List<Assignment> assignments) {
        List<Assignment> limitedAssignments = assignments.size() > 2 ? assignments.subList(0, 2) : assignments;
        assignmentAdapter.updateData(limitedAssignments);
    }

    private void setupClickListeners() {
        profileImage.setOnClickListener(v -> openProfile());
        notificationBell.setOnClickListener(v -> openNotifications());

        seeAllPopular.setOnClickListener(v -> navigateToAllCourses());
        seeAllQuizzes.setOnClickListener(v -> navigateToAllQuizzes());
        seeAllAssignments.setOnClickListener(v -> navigateToAllAssignments());

        completedCoursesCount.setOnClickListener(v -> navigateToCompletedCourses());
        enrolledCoursesCount.setOnClickListener(v -> navigateToEnrolledCourses());
        certificatesCount.setOnClickListener(v -> navigateToCertificates());
        quizScore.setOnClickListener(v -> navigateToQuizHistory());
        assignmentScore.setOnClickListener(v -> navigateToAssignmentHistory());
    }

    private void openProfile() {
        ((Main) requireActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }

    private void openNotifications() {
        Intent intent = new Intent(getContext(), ViewNotificationsActivity.class);
        startActivity(intent);
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
    }

    private void navigateToEnrolledCourses() {
        Intent intent = new Intent(getContext(), CoursesProgressActivity.class);
        startActivity(intent);
    }

    private void navigateToCertificates() {
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
        Intent intent = new Intent(getContext(), CourseDescriptionActivity.class);
        intent.putExtra("COURSE_ID", course.getId());
        startActivity(intent);
    }

    private void openQuiz(Quiz quiz) {
    }

    private void openAssignment(Assignment assignment) {
    }

    private void cleanup() {
        isRefreshing = false;
        refreshTasksCompleted = 0;

        courseAdapter = null;
        quizAdapter = null;
        assignmentAdapter = null;

        userRepository = null;
        courseRepository = null;
        quizRepository = null;
        assignmentRepository = null;
        userAuthenticationUtils = null;
        auth = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        profileImage = null;
        userFullName = null;
        userDegree = null;
        userEmail = null;
        notificationBell = null;
        completedCoursesCount = null;
        enrolledCoursesCount = null;
        certificatesCount = null;
        quizScore = null;
        assignmentScore = null;
        coursesRecyclerView = null;
        quizzesRecyclerView = null;
        assignmentsRecyclerView = null;
        seeAllPopular = null;
        seeAllQuizzes = null;
        seeAllAssignments = null;
        swipeRefreshLayout = null;

        cleanup();
        getLifecycle().removeObserver(this);
    }
}