package com.sowp.user.presenters.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sowp.user.R;
import com.sowp.user.models.User;
import com.sowp.user.presenters.activities.AssignmentHistoryActivity;
import com.sowp.user.presenters.activities.Login;
import com.sowp.user.presenters.activities.Main;
import com.sowp.user.presenters.activities.QuizHistoryActivity;
import com.sowp.user.repositories.UserRepository;
import com.sowp.user.services.ImageService;
import com.sowp.user.services.UserAuthenticationUtils;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment implements DefaultLifecycleObserver {

    private CircleImageView profileImageView;
    private TextView txtFullName, txtEmail, txtPhone, txtDateOfBirth;
    private TextView txtGender, txtDegree, txtAccountCreated;

    private TextView txtEnrolledCourses, txtCompletedCourses, txtFavouriteCourses;
    private TextView txtAssignmentsTaken;
    private TextView txtQuizzesTaken;
    private MaterialButton assignmentHistory, quizHistory; // Fixed typo

    private TextView txtCertificatesCount;

    private SwitchCompat switchNotifications;
    private LinearLayout layoutLogout;

    private ProgressBar loadingProgress;
    private TextView loadingText;
    private View loadingContainer;
    private View actualContent;

    private SwipeRefreshLayout swipeRefreshLayout;

    private UserRepository userRepository;
    private UserAuthenticationUtils userAuthenticationUtils;
    private User user;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initialize(view);
        setupSwipeRefresh();
        showLoading(true);

        if (userAuthenticationUtils.isUserLoggedIn()) {
            loadUserData();
        } else {
            showLoading(false);
            swipeRefreshLayout.setRefreshing(false);
        }

        setupClickListeners();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @NonNull Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLifecycle().addObserver(this);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        ((Main) requireActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    }
                }
        );
    }


    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onResume(owner);
        if (userAuthenticationUtils.isUserLoggedIn() && user == null) {
            loadUserData();
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onPause(owner);
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

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        loadingContainer = view.findViewById(R.id.loading_container);
        loadingProgress = view.findViewById(R.id.loading_progress);
        loadingText = view.findViewById(R.id.loading_text);
        actualContent = view.findViewById(R.id.actual_content);

        profileImageView = view.findViewById(R.id.profile_image);
        txtFullName = view.findViewById(R.id.txt_full_name);
        txtEmail = view.findViewById(R.id.txt_email);
        txtPhone = view.findViewById(R.id.txt_phone);
        txtDateOfBirth = view.findViewById(R.id.txt_date_of_birth);
        txtGender = view.findViewById(R.id.txt_gender);
        txtDegree = view.findViewById(R.id.txt_degree);
        txtAccountCreated = view.findViewById(R.id.txt_account_created);

        txtEnrolledCourses = view.findViewById(R.id.txt_enrolled_courses);
        txtCompletedCourses = view.findViewById(R.id.txt_completed_courses);
        txtFavouriteCourses = view.findViewById(R.id.txt_favourite_courses);
        txtAssignmentsTaken = view.findViewById(R.id.txt_assignments_taken);
        txtQuizzesTaken = view.findViewById(R.id.txt_quizzes_taken);
        assignmentHistory = view.findViewById(R.id.assignmentHistory);
        quizHistory = view.findViewById(R.id.quizHistory); // Fixed typo

        txtCertificatesCount = view.findViewById(R.id.txt_certificates_count);

        switchNotifications = view.findViewById(R.id.switch_notifications);
        layoutLogout = view.findViewById(R.id.layout_logout);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.accent,
                R.color.primary_dark
        );

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshUserData();
            }
        });

        swipeRefreshLayout.setDistanceToTriggerSync(300);
        swipeRefreshLayout.setSize(SwipeRefreshLayout.DEFAULT);
    }

    private void refreshUserData() {
        if (userAuthenticationUtils.isUserLoggedIn()) {
            loadUserData();
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            if (loadingContainer != null) loadingContainer.setVisibility(View.VISIBLE);
            if (actualContent != null) actualContent.setVisibility(View.GONE);
        } else {
            if (loadingContainer != null) loadingContainer.setVisibility(View.GONE);
            if (actualContent != null) actualContent.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        assignmentHistory.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), AssignmentHistoryActivity.class);
            startActivity(intent);
        });

        quizHistory.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), QuizHistoryActivity.class);
            startActivity(intent);
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userRepository.updateNotificationPreference(isChecked);
        });

        layoutLogout.setOnClickListener(v -> {
            userAuthenticationUtils.logoutUser();
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

                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                showLoading(false);
                updateUIWithUserData();
            }

            @Override
            public void onFailure(String message) {
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }

                showLoading(false);
            }
        });
    }

    private void updateUIWithUserData() {
        // Use ImageService method to convert base64 to bitmap
        if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
            Bitmap profileBitmap = ImageService.base64ToBitmap(user.getPhoto());
            if (profileBitmap != null) {
                profileImageView.setImageBitmap(profileBitmap);
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile);
            }
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile);
        }

        txtFullName.setText(user.getFullName() != null ? user.getFullName() : "Not specified");
        txtEmail.setText(user.getEmail() != null ? user.getEmail() : "Not specified");
        txtPhone.setText(user.getPhone() != null ? user.getPhone() : "Not specified");
        txtDateOfBirth.setText(user.getBirthdate() != null ? user.getBirthdate() : "Not specified");
        txtGender.setText(user.getGender() != null ? user.getGender() : "Not specified");
        txtDegree.setText(user.getDegree() != null ? user.getDegree() : "Not specified");

        // Fixed date format to match XML style
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy");
        Date date = new Date(user.getCreatedAt());
        String accountCreated = dateFormat.format(date);
        txtAccountCreated.setText(accountCreated);

        txtEnrolledCourses.setText(user.getEnrolledCourses() != null ? user.getEnrolledCourses().size() + "" : "0");
        txtCompletedCourses.setText(user.getCompletedCourses() != null ? user.getCompletedCourses().size() + "" : "0");
        txtFavouriteCourses.setText(user.getFavorites() != null ? user.getFavorites().size() + "" : "0");
        txtAssignmentsTaken.setText(user.getAssignments() != null ? user.getAssignments().size() + "" : "0");
        txtQuizzesTaken.setText(user.getQuizzes() != null ? user.getQuizzes().size() + "" : "0");
        txtCertificatesCount.setText(user.getCertificates() != null ? user.getCertificates().size() + "" : "0");
    }

    private void cleanup() {
        user = null;
        userRepository = null;
        userAuthenticationUtils = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        profileImageView = null;
        txtFullName = null;
        txtEmail = null;
        txtPhone = null;
        txtDateOfBirth = null;
        txtGender = null;
        txtDegree = null;
        txtAccountCreated = null;
        txtEnrolledCourses = null;
        txtCompletedCourses = null;
        txtFavouriteCourses = null;
        txtAssignmentsTaken = null;
        txtQuizzesTaken = null;
        assignmentHistory = null;
        quizHistory = null; // Fixed typo
        txtCertificatesCount = null;
        switchNotifications = null;
        layoutLogout = null;
        loadingContainer = null;
        loadingProgress = null;
        loadingText = null;
        actualContent = null;
        swipeRefreshLayout = null;

        cleanup();
        getLifecycle().removeObserver(this);
    }
}