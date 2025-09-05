package com.sowp.user.utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.sowp.user.R;
import com.sowp.user.models.User;

/**
 * Simplified popup dialog to display basic user profile information in chat
 */
public class UserProfilePopup {
    private final Context context;
    private Dialog dialog;
    private final LayoutInflater inflater;

    // Views
    private ImageView profileImageView;
    private TextView nameTextView;
    private TextView semesterTextView;
    private TextView degreeTextView;
    private TextView roleTextView;
    private TextView memberSinceTextView;
    private ImageView closeButton;

    public UserProfilePopup(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        initializeDialog();
    }

    private void initializeDialog() {
        // Create dialog
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        // Inflate layout
        View dialogView = inflater.inflate(R.layout.dialog_user_profile_popup, null);
        dialog.setContentView(dialogView);

        // Initialize views
        initializeViews(dialogView);
        setupClickListeners();

        // Set dialog properties
        setupDialogWindow();
    }

    private void initializeViews(View dialogView) {
        profileImageView = dialogView.findViewById(R.id.profile_image);
        nameTextView = dialogView.findViewById(R.id.user_name);
        semesterTextView = dialogView.findViewById(R.id.user_semester);
        degreeTextView = dialogView.findViewById(R.id.user_degree);
        roleTextView = dialogView.findViewById(R.id.user_role);
        memberSinceTextView = dialogView.findViewById(R.id.member_since);
        closeButton = dialogView.findViewById(R.id.close_button);
    }

    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> dismiss());

        // Allow clicking outside to dismiss
        dialog.setCanceledOnTouchOutside(true);
    }

    private void setupDialogWindow() {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );

            // Add some margin
            WindowManager.LayoutParams params = window.getAttributes();
            params.horizontalMargin = 0.1f;
            window.setAttributes(params);
        }
    }

    /**
     * Shows the profile popup with user data
     * @param userData The user data to display
     */
    public void showProfile(User userData) {
        if (userData == null) {
            return;
        }

        populateUserData(userData);

        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    private void populateUserData(User userData) {
        // Set profile image
        setProfileImage(userData);

        // Set basic info
        nameTextView.setText(userData.getFullName() != null ? userData.getFullName() : "Unknown User");

        // Set academic info
        setSemesterInfo(userData);
        setDegreeInfo(userData);
        setRoleInfo(userData);

        // Set member since date
        setMemberSinceInfo(userData);
    }

    private void setProfileImage(User userData) {
        if (userData.getPhoto() != null && !userData.getPhoto().isEmpty()) {
            Bitmap bitmap = Base64ImageUtils.base64ToBitmap(userData.getPhoto());
            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap);
                return;
            }
        }

        // Set default profile image
        profileImageView.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_profile)
        );
    }

    private void setSemesterInfo(User userData) {
        if (userData.getSemester() != null && !userData.getSemester().trim().isEmpty()) {
            semesterTextView.setText(userData.getSemester());
            semesterTextView.setVisibility(View.VISIBLE);
        } else {
            semesterTextView.setText("Not specified");
            semesterTextView.setVisibility(View.VISIBLE);
        }
    }

    private void setDegreeInfo(User userData) {
        if (userData.getDegree() != null && !userData.getDegree().trim().isEmpty()) {
            degreeTextView.setText(userData.getDegree());
            degreeTextView.setVisibility(View.VISIBLE);
        } else {
            degreeTextView.setText("Not specified");
            degreeTextView.setVisibility(View.VISIBLE);
        }
    }

    private void setRoleInfo(User userData) {
        if (userData.getRole() != null && !userData.getRole().trim().isEmpty()) {
            roleTextView.setText(userData.getRole());
            roleTextView.setVisibility(View.VISIBLE);

            // Set role-specific styling
            if ("Admin".equalsIgnoreCase(userData.getRole())) {
                roleTextView.setTextColor(ContextCompat.getColor(context, R.color.role_admin));
            } else {
                roleTextView.setTextColor(ContextCompat.getColor(context, R.color.role_teacher));
            }
        } else {
            roleTextView.setText("Student");
            roleTextView.setTextColor(ContextCompat.getColor(context, R.color.role_student));
            roleTextView.setVisibility(View.VISIBLE);
        }
    }

    private void setMemberSinceInfo(User userData) {
        if (userData.getCreatedAt() > 0) {
            String memberSince = formatMemberSinceDate(userData.getCreatedAt());
            memberSinceTextView.setText(memberSince);
            memberSinceTextView.setVisibility(View.VISIBLE);
        } else {
            memberSinceTextView.setText("Unknown");
            memberSinceTextView.setVisibility(View.VISIBLE);
        }
    }

    private String formatMemberSinceDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM yyyy",
                java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    /**
     * Dismisses the profile popup
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * Checks if the popup is currently showing
     * @return true if showing, false otherwise
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    /**
     * Cleanup method to prevent memory leaks
     */
    public void cleanup() {
        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = null;
        }
    }
}