package com.android.nexcode.profile;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.nexcode.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    // UI Components
    private CircleImageView profileImageView;
    private FloatingActionButton editProfileBtn;
    private TextView txtUserName, txtUserEmail;
    private TextView txtCoursesCount, txtCertificatesCount, txtPoints;
    private TextView txtFullName, txtPhone, txtBirthdate;
    private TextView txtCurrentCourse, txtCourseProgress;
    private TextView txtHoursStudied, txtQuizScore, txtStreak;
    private ProgressBar progressBarCourse;
    private LinearLayout layoutAccountSettings, layoutNotificationSettings, layoutHelpSupport, layoutLogout;
    private TextView txtVersion;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Initialize UI components
        initViews(view);

        // Setup click listeners
        setupClickListeners();

        // Load user data from Firestore
        if (currentUser != null) {
            loadUserData();
        } else {
            // Handle not logged in state
            Toast.makeText(getContext(), "No user is logged in", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void initViews(View view) {
        // Profile header
        profileImageView = view.findViewById(R.id.profile_image);
        editProfileBtn = view.findViewById(R.id.btn_edit_profile);
        txtUserName = view.findViewById(R.id.txt_user_name);
        txtUserEmail = view.findViewById(R.id.txt_user_email);
        txtCoursesCount = view.findViewById(R.id.txt_courses_count);
        txtCertificatesCount = view.findViewById(R.id.txt_certificates_count);
        txtPoints = view.findViewById(R.id.txt_points);

        // Personal information
        txtFullName = view.findViewById(R.id.txt_full_name);
        txtPhone = view.findViewById(R.id.txt_phone);
        txtBirthdate = view.findViewById(R.id.txt_birthdate);

        // Learning progress
        txtCurrentCourse = view.findViewById(R.id.txt_current_course);
        txtCourseProgress = view.findViewById(R.id.txt_course_progress);
        progressBarCourse = view.findViewById(R.id.progress_bar_course);
        txtHoursStudied = view.findViewById(R.id.txt_hours_studied);
        txtQuizScore = view.findViewById(R.id.txt_quiz_score);
        txtStreak = view.findViewById(R.id.txt_streak);

        // Settings
        layoutAccountSettings = view.findViewById(R.id.layout_account_settings);
        layoutNotificationSettings = view.findViewById(R.id.layout_notification_settings);
        layoutHelpSupport = view.findViewById(R.id.layout_help_support);
        layoutLogout = view.findViewById(R.id.layout_logout);

        // Version
        txtVersion = view.findViewById(R.id.txt_version);
        txtVersion.setText("Version " + getAppVersion());
    }

    private void setupClickListeners() {
        editProfileBtn.setOnClickListener(v -> {
            // Navigate to edit profile screen or show dialog
            Intent intent = new Intent(getActivity(), EditProfile.class);
            startActivity(intent);
        });

        layoutAccountSettings.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Account Settings clicked", Toast.LENGTH_SHORT).show();
            // Navigate to account settings
        });

        layoutNotificationSettings.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notification Settings clicked", Toast.LENGTH_SHORT).show();
            // Navigate to notification settings
        });

        layoutHelpSupport.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Help & Support clicked", Toast.LENGTH_SHORT).show();
            // Navigate to help & support
        });

        layoutLogout.setOnClickListener(v -> {
            // Sign out from Firebase Auth
            firebaseAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

            // Navigate to login screen
            // For example:
            // ((MainActivity) requireActivity()).navigateToLogin();
        });
    }

    private void loadUserData() {
        String userId = currentUser.getUid();

        // Show loading indicator (optional)
        // showLoading(true);

        firestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    // Hide loading indicator (optional)
                    // showLoading(false);

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "User data loaded successfully");
                            updateUIWithUserData(document);
                        } else {
                            Log.d(TAG, "No such user document");
                            Toast.makeText(getContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Error loading user data", task.getException());
                        Toast.makeText(getContext(), "Error loading profile: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Load learning progress data
        firestore.collection("user_progress").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            updateUIWithProgressData(document);
                        } else {
                            Log.d(TAG, "No progress data found");
                        }
                    } else {
                        Log.w(TAG, "Error loading progress data", task.getException());
                    }
                });
    }

    private void updateUIWithUserData(DocumentSnapshot document) {
        // Basic info
        String userName = document.getString("displayName");
        String email = document.getString("email");
        String fullName = document.getString("fullName");
        String phone = document.getString("phone");
        String birthdate = document.getString("birthdate");
        Long coursesCount = document.getLong("coursesCount");
        Long certificatesCount = document.getLong("certificatesCount");
        Long points = document.getLong("points");

        // Profile image (stored as Base64 string)
        String profileImageBase64 = document.getString("profileImageBase64");

        // Update UI
        txtUserName.setText(userName != null ? userName : "");
        txtUserEmail.setText(email != null ? email : "");
        txtFullName.setText(fullName != null ? fullName : "");
        txtPhone.setText(phone != null ? phone : "");
        txtBirthdate.setText(birthdate != null ? birthdate : "");
        txtCoursesCount.setText(coursesCount != null ? String.valueOf(coursesCount) : "0");
        txtCertificatesCount.setText(certificatesCount != null ? String.valueOf(certificatesCount) : "0");
        txtPoints.setText(points != null ? String.valueOf(points) : "0");

        // Set profile image from Base64 string
        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                profileImageView.setImageBitmap(decodedBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding profile image", e);
                profileImageView.setImageResource(R.drawable.ic_profile);
            }
        } else {
            profileImageView.setImageResource(R.drawable.ic_profile);
        }
    }

    private void updateUIWithProgressData(DocumentSnapshot document) {
        // Learning progress
        String currentCourse = document.getString("currentCourse");
        Long courseProgressLong = document.getLong("courseProgress");
        Long hoursStudied = document.getLong("hoursStudied");
        Double quizScore = document.getDouble("quizScore");
        Long streak = document.getLong("streak");

        int courseProgress = courseProgressLong != null ? courseProgressLong.intValue() : 0;

        // Update UI
        txtCurrentCourse.setText(currentCourse != null ? currentCourse : "No active course");
        txtCourseProgress.setText(courseProgress + "%");
        progressBarCourse.setProgress(courseProgress);
        txtHoursStudied.setText(hoursStudied != null ? hoursStudied + "h" : "0h");
        txtQuizScore.setText(quizScore != null ? String.format("%.1f%%", quizScore) : "0%");
        txtStreak.setText(streak != null ? streak + " days" : "0 days");
    }

    private String getAppVersion() {
        try {
            return requireActivity().getPackageManager()
                    .getPackageInfo(requireActivity().getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.e(TAG, "Error getting app version", e);
            return "1.0.0";
        }
    }

    // Optional: Utils to encode/decode profile images
    public static String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}