package com.android.nexcode.presenters.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.nexcode.adapters.HomeAdapter;
import com.android.nexcode.adapters.QuizAdapter;
import com.android.nexcode.adapters.AssignmentAdapter;
import com.android.nexcode.R;
import com.android.nexcode.models.UserProgress;
import com.android.nexcode.models.Quiz;
import com.android.nexcode.models.Assignment;
import com.android.nexcode.models.Course;
import com.android.nexcode.models.User;

import com.android.nexcode.presenters.activities.QuizActivity;
import com.android.nexcode.repositories.firebase.AssignmentRepository;
import com.android.nexcode.repositories.firebase.CourseRepository;
import com.android.nexcode.repositories.firebase.QuizRepository;
import com.android.nexcode.repositories.firebase.UserRepository;
import com.android.nexcode.utils.UserAuthenticationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final int GRID_SPAN_COUNT = 2;
    private static final int RECENT_ITEMS_LIMIT = 2;

    private UserRepository userRepository;
    private CourseRepository courseRepository;
    private QuizRepository quizRepository;
    private AssignmentRepository assignmentRepository;
    private UserAuthenticationUtils userAuthenticationUtils;
    // Firebase
    private FirebaseFirestore db;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initialize(view);
        setupRecyclerViews();
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

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

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
        quizAdapter.setOnQuizItemClickListener(quiz -> {
            openQuiz(quiz);
        });
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

    private void loadUserData() {
        if (userAuthenticationUtils.getUserId() == null) {
            Log.w(TAG, "Current user ID is null");
            return;
        }


        userRepository.loadUserData(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                userFullName.setText(user.getFullName() != null ? user.getFullName() : "User");
                userDegree.setText(user.getDegree() != null ? user.getDegree() : "");
                userEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                // Set profile image from Base64 string
                Log.d(TAG, "p0");
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
                completedCoursesCount.setText(user.getCompletedCourses() != null ? user.getCompletedCourses().size()+"" : "0");
                Log.d(TAG, "p1");
                enrolledCoursesCount.setText(user.getEnrolledCourses() != null ? user.getEnrolledCourses().size()+"" : "0");
                Log.d(TAG, "p2");
                quizScore.setText(String.format("%.0f%%", user.getQuizzesAvg()));
                Log.d(TAG, "p3");
                assignmentScore.setText(String.format("%.0f%%", user.getAssignmentAvg()));
                Log.d(TAG, "p4");
                certificatesCount.setText(user.getCertificates() != null ? user.getCertificates().size()+"" : "0");
                Log.d(TAG, "p5");
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                userFullName.setText("User");
                userDegree.setText("");
                userEmail.setText(auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "");
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
                quizAdapter.updateQuizzes(quizzes);
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
                assignmentAdapter.updateAssignments(assignments);
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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

    // Navigation methods
    private void openProfile() {
        ProfileFragment fragment = new ProfileFragment();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openNotifications() {
        // Navigate to notifications screen
        // Use Navigation Component or start NotificationsActivity
    }

    private void navigateToAllCourses() {
        // Navigate to all courses screen
        // Use Navigation Component or start CoursesActivity
    }

    private void navigateToAllQuizzes() {
        // Navigate to all quizzes screen
        // Use Navigation Component or start QuizzesActivity
    }

    private void navigateToAllAssignments() {
        // Navigate to all assignments screen
        // Use Navigation Component or start AssignmentsActivity
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
    }

    private void navigateToAssignmentHistory() {
        // Navigate to assignment history screen
    }

    private void openQuiz(Quiz quiz) {
        Intent intent = new Intent(getContext(), QuizActivity.class);
        intent.putExtra("QUIZ_ID", quiz.getId());
        startActivity(intent);
    }

    private void openAssignment(Assignment assignment) {
        // Navigate to assignment details/submission screen
        // Pass assignment ID and other necessary data
    }
}