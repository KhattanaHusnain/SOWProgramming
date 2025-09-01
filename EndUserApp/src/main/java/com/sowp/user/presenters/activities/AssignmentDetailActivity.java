package com.sowp.user.presenters.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sowp.user.R;
import com.sowp.user.models.AssignmentAttempt;
import com.sowp.user.repositories.firebase.UserRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssignmentDetailActivity extends AppCompatActivity {

    private static final String TAG = "AssignmentDetailActivity";

    // UI Components
    private TextView tvAssignmentTitle, tvScore, tvPercentage, tvMaxScore, tvStatus,
            tvSubmissionTime, tvCheckedStatus, tvImageCount, tvFeedback, tvGradedTime;
    private Chip chipStatus, chipChecked;
    private LinearLayout imagesContainer, feedbackLayout;
    private ProgressBar progressBar, imageLoadingProgress;
    private MaterialCardView scoreCard, detailsCard, imagesCard, feedbackCard;
    private View statusIndicator;

    // Data
    private UserRepository userRepository;
    private String attemptId;
    private int assignmentId;
    private int courseId;
    private AssignmentAttempt currentAttempt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_assignment_detail);

        setupWindowInsets();
        initializeViews();
        setupToolbar();

        userRepository = new UserRepository(this);

        // Get intent extras
        attemptId = getIntent().getStringExtra("attemptId");
        assignmentId = getIntent().getIntExtra("assignmentId", 0);
        courseId = getIntent().getIntExtra("courseId", 0);

        if (attemptId == null) {
            showError("Invalid assignment attempt");
            finish();
            return;
        }

        loadAssignmentDetails();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        // Text Views
        tvAssignmentTitle = findViewById(R.id.tvAssignmentTitle);
        tvScore = findViewById(R.id.tvScore);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvMaxScore = findViewById(R.id.tvMaxScore);
        tvStatus = findViewById(R.id.tvStatus);
        tvSubmissionTime = findViewById(R.id.tvSubmissionTime);
        tvCheckedStatus = findViewById(R.id.tvCheckedStatus);
        tvImageCount = findViewById(R.id.tvImageCount);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvGradedTime = findViewById(R.id.tvGradedTime);

        // Chips
        chipStatus = findViewById(R.id.chipStatus);
        chipChecked = findViewById(R.id.chipChecked);

        // Layouts and containers
        imagesContainer = findViewById(R.id.imagesContainer);
        feedbackLayout = findViewById(R.id.feedbackLayout);

        // Progress bars
        progressBar = findViewById(R.id.progressBar);
        imageLoadingProgress = findViewById(R.id.imageLoadingProgress);

        // Cards
        scoreCard = findViewById(R.id.scoreCard);
        detailsCard = findViewById(R.id.detailsCard);
        imagesCard = findViewById(R.id.imagesCard);
        feedbackCard = findViewById(R.id.feedbackCard);

        // Other views
        statusIndicator = findViewById(R.id.statusIndicator);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Assignment Details");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadAssignmentDetails() {
        showLoading(true);

        userRepository.getAssignmentAttemptDetails(attemptId, new UserRepository.AssignmentAttemptCallback() {
            @Override
            public void onSuccess(AssignmentAttempt attempt) {
                currentAttempt = attempt;
                showLoading(false);
                populateDetails();
                loadSubmittedImages();
            }

            @Override
            public void onFailure(String message) {
                showLoading(false);
                showError("Failed to load assignment details: " + message);
                Log.e(TAG, "Failed to load assignment details: " + message);
            }
        });
    }

    private void populateDetails() {
        if (currentAttempt == null) return;

        // Set assignment title
        String title = currentAttempt.getAssignmentTitle();
        if (title != null && !title.trim().isEmpty()) {
            tvAssignmentTitle.setText(title);
        } else {
            tvAssignmentTitle.setText(formatAssignmentId(currentAttempt.getAssignmentId()));
        }

        // Set score information
        tvScore.setText(String.valueOf(currentAttempt.getScore()));
        tvMaxScore.setText("/ " + currentAttempt.getMaxScore());
        tvPercentage.setText(currentAttempt.getFormattedPercentage());

        // Set score color based on percentage
        setScoreColor(currentAttempt.getPercentageScore());

        // Set status
        setupStatusChip(currentAttempt.getStatus());
        tvStatus.setText(currentAttempt.getStatus());

        // Set checked status
        setupCheckedChip(currentAttempt.isChecked());
        if (currentAttempt.isChecked()) {
            tvCheckedStatus.setText("Graded by instructor");
            if (currentAttempt.getGradedAt() > 0) {
                tvGradedTime.setText("Graded on " + currentAttempt.getFormattedGradedDate());
                tvGradedTime.setVisibility(View.VISIBLE);
            }
        } else {
            tvCheckedStatus.setText("Awaiting grading");
            tvGradedTime.setVisibility(View.GONE);
        }

        // Set submission time
        tvSubmissionTime.setText(formatSubmissionTime(currentAttempt.getSubmissionTimestamp()));

        // Set feedback
        setupFeedback();

        // Set image count
        int imageCount = currentAttempt.getSubmittedImagesCount();
        tvImageCount.setText(imageCount + " image" + (imageCount != 1 ? "s" : "") + " submitted");

        // Show/hide cards based on content
        imagesCard.setVisibility(imageCount > 0 ? View.VISIBLE : View.GONE);

        // Set status indicator color
        setStatusIndicatorColor(currentAttempt.getStatus());
    }

    private void setupFeedback() {
        if (currentAttempt.hasFeedback()) {
            tvFeedback.setText(currentAttempt.getFeedback());
            feedbackCard.setVisibility(View.VISIBLE);
        } else {
            feedbackCard.setVisibility(View.GONE);
        }
    }

    private String formatAssignmentId(int assignmentId) {
        return "Assignment #" + assignmentId;
    }

    private String formatSubmissionTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void setScoreColor(double percentage) {
        int colorRes;
        if (percentage >= 80) {
            colorRes = R.color.success_color;
        } else if (percentage >= 60) {
            colorRes = R.color.warning_color;
        } else {
            colorRes = R.color.error_color;
        }

        int color = ContextCompat.getColor(this, colorRes);
        tvScore.setTextColor(color);
        tvPercentage.setTextColor(color);
    }

    private void setupStatusChip(String status) {
        chipStatus.setText(status);

        int backgroundColorRes, textColorRes;
        switch (status.toLowerCase()) {
            case "submitted":
                backgroundColorRes = R.color.status_submitted_bg;
                textColorRes = R.color.status_submitted_text;
                break;
            case "graded":
                backgroundColorRes = R.color.status_graded_bg;
                textColorRes = R.color.status_graded_text;
                break;
            case "failed":
                backgroundColorRes = R.color.status_late_bg;
                textColorRes = R.color.status_late_text;
                break;
            case "pending":
                backgroundColorRes = R.color.status_pending_bg;
                textColorRes = R.color.status_pending_text;
                break;
            default:
                backgroundColorRes = R.color.status_default_bg;
                textColorRes = R.color.status_default_text;
                break;
        }

        chipStatus.setChipBackgroundColorResource(backgroundColorRes);
        chipStatus.setTextColor(ContextCompat.getColor(this, textColorRes));
    }

    private void setupCheckedChip(boolean isChecked) {
        if (isChecked) {
            chipChecked.setText("Graded");
            chipChecked.setChipBackgroundColorResource(R.color.success_color);
            chipChecked.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            chipChecked.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_checked));
        } else {
            chipChecked.setText("Pending");
            chipChecked.setChipBackgroundColorResource(R.color.warning_color);
            chipChecked.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            chipChecked.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_pending));
        }
    }

    private void setStatusIndicatorColor(String status) {
        int colorRes;
        switch (status.toLowerCase()) {
            case "submitted":
                colorRes = R.color.status_submitted_bg;
                break;
            case "graded":
                colorRes = R.color.status_graded_bg;
                break;
            case "failed":
                colorRes = R.color.status_late_bg;
                break;
            case "pending":
                colorRes = R.color.status_pending_bg;
                break;
            default:
                colorRes = R.color.status_default_bg;
                break;
        }

        statusIndicator.setBackgroundColor(ContextCompat.getColor(this, colorRes));
    }

    private void loadSubmittedImages() {
        if (currentAttempt == null || !currentAttempt.hasSubmittedImages()) {
            return;
        }

        List<String> imageBase64List = currentAttempt.getSubmittedImages();
        imageLoadingProgress.setVisibility(View.VISIBLE);

        // Clear existing images
        imagesContainer.removeAllViews();

        // Load each image in background thread
        new Thread(() -> {
            for (int i = 0; i < imageBase64List.size(); i++) {
                final int imageIndex = i + 1;
                final String base64Image = imageBase64List.get(i);
                final boolean isLast = i == imageBase64List.size() - 1;

                if (base64Image != null && !base64Image.trim().isEmpty()) {
                    loadSingleImage(base64Image, imageIndex, isLast);
                } else {
                    runOnUiThread(() -> {
                        addErrorImageView("Invalid image data for Image " + imageIndex);
                        if (isLast) imageLoadingProgress.setVisibility(View.GONE);
                    });
                }
            }
        }).start();
    }

    private void loadSingleImage(String base64Image, int imageIndex, boolean isLast) {
        try {
            // Convert Base64 to Bitmap
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            runOnUiThread(() -> {
                if (bitmap != null) {
                    addImageView(bitmap, imageIndex);
                } else {
                    addErrorImageView("Failed to load Image " + imageIndex);
                }

                if (isLast) {
                    imageLoadingProgress.setVisibility(View.GONE);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading image " + imageIndex, e);
            runOnUiThread(() -> {
                addErrorImageView("Error loading Image " + imageIndex);
                if (isLast) imageLoadingProgress.setVisibility(View.GONE);
            });
        }
    }

    private void addImageView(Bitmap bitmap, int imageIndex) {
        View imageItemView = LayoutInflater.from(this).inflate(R.layout.item_submitted_image, imagesContainer, false);

        ImageView imageView = imageItemView.findViewById(R.id.ivSubmittedImage);
        TextView tvImageTitle = imageItemView.findViewById(R.id.tvImageTitle);
        TextView tvImageSize = imageItemView.findViewById(R.id.tvImageSize);

        // Set image
        imageView.setImageBitmap(bitmap);

        // Set title
        tvImageTitle.setText("Image " + imageIndex);

        // Calculate and display image size
        long sizeInBytes = bitmap.getByteCount();
        String sizeText = formatFileSize(sizeInBytes);
        tvImageSize.setText(bitmap.getWidth() + " × " + bitmap.getHeight() + " • " + sizeText);

        // Add click listener for full screen view
        imageView.setOnClickListener(v -> showFullScreenImage(bitmap, "Image " + imageIndex));

        imagesContainer.addView(imageItemView);
    }

    private void addErrorImageView(String errorMessage) {
        View errorView = LayoutInflater.from(this).inflate(R.layout.item_image_error, imagesContainer, false);
        TextView tvErrorMessage = errorView.findViewById(R.id.tvErrorMessage);
        tvErrorMessage.setText(errorMessage);
        imagesContainer.addView(errorView);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void showFullScreenImage(Bitmap bitmap, String title) {
        // Create full screen image dialog
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_fullscreen_image, null);

        ImageView fullImageView = dialogView.findViewById(R.id.ivFullscreenImage);
        TextView tvFullImageTitle = dialogView.findViewById(R.id.tvFullImageTitle);
        ImageView btnClose = dialogView.findViewById(R.id.btnCloseFullscreen);

        fullImageView.setImageBitmap(bitmap);
        tvFullImageTitle.setText(title);

        builder.setView(dialogView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Set close button click listener
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Make dialog full screen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            dialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT
            );
        }
    }

    private void shareAssignmentDetails() {
        if (currentAttempt == null) return;

        StringBuilder shareText = new StringBuilder();
        shareText.append("Assignment Submission Details\n");
        shareText.append("============================\n\n");

        if (currentAttempt.getAssignmentTitle() != null) {
            shareText.append("Assignment: ").append(currentAttempt.getAssignmentTitle()).append("\n");
        }
        shareText.append("Assignment ID: #").append(currentAttempt.getAssignmentId()).append("\n");
        shareText.append("Score: ").append(currentAttempt.getScore()).append("/").append(currentAttempt.getMaxScore());
        shareText.append(" (").append(currentAttempt.getFormattedPercentage()).append(")\n");
        shareText.append("Status: ").append(currentAttempt.getStatus()).append("\n");
        shareText.append("Grading Status: ").append(currentAttempt.isChecked() ? "Graded" : "Pending").append("\n");
        shareText.append("Submitted: ").append(formatSubmissionTime(currentAttempt.getSubmissionTimestamp())).append("\n");

        if (currentAttempt.isGraded()) {
            shareText.append("Graded: ").append(currentAttempt.getFormattedGradedDate()).append("\n");
        }

        shareText.append("Images: ").append(currentAttempt.getSubmittedImagesCount()).append(" submitted\n");

        if (currentAttempt.hasFeedback()) {
            shareText.append("\nFeedback:\n").append(currentAttempt.getFeedback()).append("\n");
        }

        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText.toString());
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Assignment Submission - " +
                        (currentAttempt.getAssignmentTitle() != null ?
                                currentAttempt.getAssignmentTitle() :
                                formatAssignmentId(currentAttempt.getAssignmentId())));

        startActivity(android.content.Intent.createChooser(shareIntent, "Share Assignment Details"));
    }

    private void showAssignmentStats() {
        if (currentAttempt == null) return;

        StringBuilder statsText = new StringBuilder();
        statsText.append("Assignment Statistics\n\n");

        if (currentAttempt.getAssignmentTitle() != null) {
            statsText.append("Title: ").append(currentAttempt.getAssignmentTitle()).append("\n");
        }
        statsText.append("Assignment ID: #").append(currentAttempt.getAssignmentId()).append("\n");
        statsText.append("Course ID: #").append(currentAttempt.getCourseId()).append("\n");
        statsText.append("Score: ").append(currentAttempt.getScore()).append("/").append(currentAttempt.getMaxScore()).append("\n");
        statsText.append("Percentage: ").append(currentAttempt.getFormattedPercentage()).append("\n");
        statsText.append("Status: ").append(currentAttempt.getStatus()).append("\n");
        statsText.append("Submitted: ").append(formatSubmissionTime(currentAttempt.getSubmissionTimestamp())).append("\n");

        if (currentAttempt.isGraded()) {
            statsText.append("Graded: ").append(currentAttempt.getFormattedGradedDate()).append("\n");
        }

        statsText.append("Images: ").append(currentAttempt.getSubmittedImagesCount()).append(" submitted\n");
        statsText.append("Grading Status: ").append(currentAttempt.isChecked() ? "Graded" : "Pending").append("\n");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Assignment Statistics")
                .setMessage(statsText.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_assignment_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_share) {
            shareAssignmentDetails();
            return true;
        } else if (id == R.id.action_stats) {
            showAssignmentStats();
            return true;
        } else if (id == R.id.action_refresh) {
            loadAssignmentDetails();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        scoreCard.setVisibility(show ? View.GONE : View.VISIBLE);
        detailsCard.setVisibility(show ? View.GONE : View.VISIBLE);

        if (!show && currentAttempt != null) {
            if (currentAttempt.hasSubmittedImages()) {
                imagesCard.setVisibility(View.VISIBLE);
            }
            if (currentAttempt.hasFeedback()) {
                feedbackCard.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}