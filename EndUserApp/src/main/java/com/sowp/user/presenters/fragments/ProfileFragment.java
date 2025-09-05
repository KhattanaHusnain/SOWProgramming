package com.sowp.user.presenters.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sowp.user.R;
import com.sowp.user.models.User;
//import com.sowp.user.presenters.activities.AssignmentHistoryActivity;
import com.sowp.user.presenters.activities.Login;
import com.sowp.user.presenters.activities.QuizHistoryActivity;
import com.sowp.user.repositories.firebase.UserRepository;
import com.sowp.user.utils.UserAuthenticationUtils;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    // UI Components - Personal Information
    private CircleImageView profileImageView;
    private TextView txtFullName, txtEmail, txtPhone, txtDateOfBirth;
    private TextView txtGender, txtDegree, txtAccountCreated;

    // UI Components - Learning Progress
    private TextView txtEnrolledCourses, txtCompletedCourses, txtFavouriteCourses;
    private TextView txtAssignmentsTaken;
    private TextView txtQuizzesTaken;
    private MaterialButton assignmentHistory, quizeHistory;

    // UI Components - Certificates
    private TextView txtCertificatesCount;

    // UI Components - Settings
    private SwitchCompat switchNotifications;
    private LinearLayout layoutLogout;

    // Skeleton Loading Views
    private View skeletonPersonalInfo;
    private View skeletonProgress;
    private View skeletonCertificates;
    private View actualContent;

    // SwipeRefreshLayout
    private SwipeRefreshLayout swipeRefreshLayout;

    // Firebase
    UserRepository userRepository;
    UserAuthenticationUtils userAuthenticationUtils;
    User user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize UI components
        initialize(view);

        // Setup SwipeRefreshLayout
        setupSwipeRefresh();

        // Show skeleton loading initially
        showSkeletonLoading(true);

        // Load user data from Firestore
        if (userAuthenticationUtils.isUserLoggedIn()) {
            loadUserData();
        } else {
            // Handle not logged in state
            showSkeletonLoading(false);
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getContext(), "No user is logged in", Toast.LENGTH_SHORT).show();
        }

        // Setup click listeners
        setupClickListeners();

        return view;
    }

    private void initialize(View view) {
        userRepository = new UserRepository(getContext());
        userAuthenticationUtils = new UserAuthenticationUtils(getContext());

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        // Find skeleton and actual content views
        skeletonPersonalInfo = view.findViewById(R.id.skeleton_personal_info);
        skeletonProgress = view.findViewById(R.id.skeleton_progress);
        skeletonCertificates = view.findViewById(R.id.skeleton_certificates);
        actualContent = view.findViewById(R.id.actual_content);

        // Personal Information
        profileImageView = view.findViewById(R.id.profile_image);
        txtFullName = view.findViewById(R.id.txt_full_name);
        txtEmail = view.findViewById(R.id.txt_email);
        txtPhone = view.findViewById(R.id.txt_phone);
        txtDateOfBirth = view.findViewById(R.id.txt_date_of_birth);
        txtGender = view.findViewById(R.id.txt_gender);
        txtDegree = view.findViewById(R.id.txt_degree);
        txtAccountCreated = view.findViewById(R.id.txt_account_created);

        // Learning Progress
        txtEnrolledCourses = view.findViewById(R.id.txt_enrolled_courses);
        txtCompletedCourses = view.findViewById(R.id.txt_completed_courses);
        txtFavouriteCourses = view.findViewById(R.id.txt_favourite_courses);
        txtAssignmentsTaken = view.findViewById(R.id.txt_assignments_taken);
        txtQuizzesTaken = view.findViewById(R.id.txt_quizzes_taken);
        assignmentHistory = view.findViewById(R.id.assignmentHistory);
        quizeHistory = view.findViewById(R.id.quizeHistory);

        // Certificates
        txtCertificatesCount = view.findViewById(R.id.txt_certificates_count);

        // Settings
        switchNotifications = view.findViewById(R.id.switch_notifications);
        layoutLogout = view.findViewById(R.id.layout_logout);
    }

    private void setupSwipeRefresh() {
        // Configure SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,        // instead of R.color.colorPrimary
                R.color.accent,         // instead of R.color.colorAccent
                R.color.primary_dark    // instead of R.color.colorPrimaryDark
        );

        // Set refresh listener
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshUserData();
            }
        });

        // Optional: Configure the refresh trigger distance
        swipeRefreshLayout.setDistanceToTriggerSync(300);

        // Optional: Set the size of the refresh indicator
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
    }

    private void refreshUserData() {
        Log.d(TAG, "Refreshing user data...");

        if (userAuthenticationUtils.isUserLoggedIn()) {
            // Don't show skeleton loading during refresh, just use the refresh indicator
            loadUserData();
        } else {
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getContext(), "No user is logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSkeletonLoading(boolean show) {
        if (show) {
            // Show skeleton views
            if (skeletonPersonalInfo != null) skeletonPersonalInfo.setVisibility(View.VISIBLE);
            if (skeletonProgress != null) skeletonProgress.setVisibility(View.VISIBLE);
            if (skeletonCertificates != null) skeletonCertificates.setVisibility(View.VISIBLE);

            // Hide actual content
            if (actualContent != null) actualContent.setVisibility(View.GONE);

            // Start shimmer animation if using Shimmer library
            startShimmerAnimation();
        } else {
            // Hide skeleton views
            if (skeletonPersonalInfo != null) skeletonPersonalInfo.setVisibility(View.GONE);
            if (skeletonProgress != null) skeletonProgress.setVisibility(View.GONE);
            if (skeletonCertificates != null) skeletonCertificates.setVisibility(View.GONE);

            // Show actual content
            if (actualContent != null) actualContent.setVisibility(View.VISIBLE);

            // Stop shimmer animation
            stopShimmerAnimation();
        }
    }

    private void startShimmerAnimation() {
        // If using Shimmer library, start animation
        // Example: ((ShimmerFrameLayout) skeletonPersonalInfo).startShimmer();

        // Alternative: Create a simple fade animation for skeleton views
        if (skeletonPersonalInfo != null) {
            skeletonPersonalInfo.animate()
                    .alpha(0.3f)
                    .setDuration(1000)
                    .withEndAction(() -> {
                        if (skeletonPersonalInfo.getVisibility() == View.VISIBLE) {
                            skeletonPersonalInfo.animate()
                                    .alpha(1.0f)
                                    .setDuration(1000)
                                    .withEndAction(this::startShimmerAnimation)
                                    .start();
                        }
                    })
                    .start();
        }
    }

    private void stopShimmerAnimation() {
        // Stop any ongoing animations
        if (skeletonPersonalInfo != null) {
            skeletonPersonalInfo.clearAnimation();
        }
        if (skeletonProgress != null) {
            skeletonProgress.clearAnimation();
        }
        if (skeletonCertificates != null) {
            skeletonCertificates.clearAnimation();
        }
    }

    private void setupClickListeners() {

        assignmentHistory.setOnClickListener(view -> {
//            Intent intent = new Intent(getContext(), AssignmentHistoryActivity.class);
//            startActivity(intent);
        });

        quizeHistory.setOnClickListener(view -> {
//            Intent intent = new Intent(getContext(), QuizHistoryActivity.class);
//            startActivity(intent);
        });
        // Notification switch listener
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Handle notification toggle
            // You can save this preference to SharedPreferences or Firebase
            userRepository.updateNotificationPreference(isChecked);
            Toast.makeText(getContext(),
                    "Notifications " + (isChecked ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT).show();
        });

        // Logout click listener
        layoutLogout.setOnClickListener(v -> {
            // Sign out from Firebase Auth
            userAuthenticationUtils.logoutUser();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            // Navigate to login screen
            Intent intent = new Intent(getContext(), Login.class);
            startActivity(intent);
            getActivity().finish();
        });
    }

    private void loadUserData() {
        userRepository.loadUserData(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User userData) {
                user = userData;
                Log.d(TAG, "User data loaded successfully: " + user.toString());

                // Stop refresh indicator
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                // Hide skeleton loading and show actual content
                showSkeletonLoading(false);

                // Update UI with user data
                updateUIWithUserData();

                // Show success message for manual refresh
                if (swipeRefreshLayout != null) {
                    Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String message) {
                // Stop refresh indicator
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                // Hide skeleton loading even on failure
                showSkeletonLoading(false);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIWithUserData() {
        // Set profile image from Base64 string
        if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(user.getPhoto(), Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                profileImageView.setImageBitmap(decodedBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding profile image", e);
                profileImageView.setImageResource(R.drawable.ic_profile);
            }
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile);
        }

        // Update personal information
        txtFullName.setText(user.getFullName() != null ? user.getFullName() : "Not specified");
        txtEmail.setText(user.getEmail() != null ? user.getEmail() : "Not specified");
        txtPhone.setText(user.getPhone() != null ? user.getPhone() : "Not specified");
        txtDateOfBirth.setText(user.getBirthdate() != null ? user.getBirthdate() : "Not specified");
        txtGender.setText(user.getGender() != null ? user.getGender() : "Not specified");
        txtDegree.setText(user.getDegree() != null ? user.getDegree() : "Not specified");

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date(user.getCreatedAt());
        String accountCreated = dateFormat.format(date);
        txtAccountCreated.setText(accountCreated);

        // Update progress UI
        txtEnrolledCourses.setText(user.getEnrolledCourses() != null ? user.getEnrolledCourses().size() + "" : "0");
        txtCompletedCourses.setText("0");
        txtFavouriteCourses.setText(user.getFavorites() != null ? user.getFavorites().size() + "" : "0");
        txtAssignmentsTaken.setText(user.getAssignments() != null ? user.getAssignments().size() + "" : "0");
        txtQuizzesTaken.setText(user.getQuizzes() != null ? user.getQuizzes().size() + "" : "0");
        txtCertificatesCount.setText("0");
    }
}