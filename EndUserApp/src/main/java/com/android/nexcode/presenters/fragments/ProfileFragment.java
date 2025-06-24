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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.android.nexcode.R;
import com.android.nexcode.models.User;
import com.android.nexcode.presenters.activities.Login;
import com.android.nexcode.repositories.firebase.UserRepository;
import com.android.nexcode.utils.UserAuthenticationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
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
    private TextView txtAssignmentsTaken, txtAssignmentsScore;
    private TextView txtQuizzesTaken, txtQuizzesScore;

    // UI Components - Certificates
    private TextView txtCertificatesCount;

    // UI Components - Settings
    private SwitchCompat switchNotifications;
    private LinearLayout layoutLogout;

    // Firebase
    UserRepository userRepository;
    UserAuthenticationUtils userAuthenticationUtils;
    User user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        userRepository = new UserRepository(getContext());
        userAuthenticationUtils = new UserAuthenticationUtils(getContext());

        // Initialize UI components
        initViews(view);

        // Setup click listeners
        setupClickListeners();

        // Load user data from Firestore
        if (userAuthenticationUtils.isUserLoggedIn()) {
            loadUserData();
        } else {
            // Handle not logged in state
            Toast.makeText(getContext(), "No user is logged in", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void initViews(View view) {
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
        txtAssignmentsScore = view.findViewById(R.id.txt_assignments_score);
        txtQuizzesTaken = view.findViewById(R.id.txt_quizzes_taken);
        txtQuizzesScore = view.findViewById(R.id.txt_quizzes_score);

        // Certificates
        txtCertificatesCount = view.findViewById(R.id.txt_certificates_count);

        // Settings
        switchNotifications = view.findViewById(R.id.switch_notifications);
        layoutLogout = view.findViewById(R.id.layout_logout);
    }

    private void setupClickListeners() {
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


                // Update progress UI
                txtEnrolledCourses.setText("0");
                txtCompletedCourses.setText("0");
                txtFavouriteCourses.setText("0");
                txtAssignmentsTaken.setText("0");
                txtAssignmentsScore.setText("0%");
                txtQuizzesTaken.setText("0");
                txtQuizzesScore.setText("0%");
                txtCertificatesCount.setText("0");

            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

    }

    // Optional: Utils to encode/decode profile images
    public static String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

}