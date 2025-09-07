package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sowp.user.R;
import com.sowp.user.models.AssignmentAttempt;
import com.sowp.user.repositories.UserRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssignmentDetailActivity extends AppCompatActivity {

    private static final String TAG = "AssignmentDetailActivity";

    // Intent Keys
    public static final String EXTRA_ATTEMPT_ID = "attemptId";
    public static final String EXTRA_ASSIGNMENT_ID = "assignmentId";
    public static final String EXTRA_COURSE_ID = "courseId";

    // UI Components
    private TextView tvAssignmentTitle, tvScore, tvPercentage, tvMaxScore, tvStatus;
    private TextView tvSubmissionTime, tvCheckedStatus, tvImageCount, tvFeedback, tvGradedTime;
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

        initializeActivity();
        validateIntentData();
        loadAssignmentDetails();
    }

    private void initializeActivity() {
        setupWindowInsets();
        initializeViews();
        setupToolbar();
        userRepository = new UserRepository(this);
        extractIntentExtras();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        bindTextViews();
        bindChips();
        bindLayouts();
        bindProgressBars();
        bindCards();
        bindOtherViews();
    }

    private void bindTextViews() {
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
    }

    private void bindChips() {
        chipStatus = findViewById(R.id.chipStatus);
        chipChecked = findViewById(R.id.chipChecked);
    }

    private void bindLayouts() {
        imagesContainer = findViewById(R.id.imagesContainer);
        feedbackLayout = findViewById(R.id.feedbackLayout);
    }

    private void bindProgressBars() {
        progressBar = findViewById(R.id.progressBar);
        imageLoadingProgress = findViewById(R.id.imageLoadingProgress);
    }

    private void bindCards() {
        scoreCard = findViewById(R.id.scoreCard);
        detailsCard = findViewById(R.id.detailsCard);
        imagesCard = findViewById(R.id.imagesCard);
        feedbackCard = findViewById(R.id.feedbackCard);
    }

    private void bindOtherViews() {
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

    private void extractIntentExtras() {
        Intent intent = getIntent();
        attemptId = intent.getStringExtra(EXTRA_ATTEMPT_ID);
        assignmentId = intent.getIntExtra(EXTRA_ASSIGNMENT_ID, 0);
        courseId = intent.getIntExtra(EXTRA_COURSE_ID, 0);
    }

    private void validateIntentData() {
        if (attemptId == null) {
            showError("Invalid assignment attempt");
            finish();
        }
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

        setAssignmentTitle();
        setScoreInformation();
        setStatusInformation();
        setSubmissionTime();
        setFeedbackInformation();
        setImageInformation();
        updateCardVisibility();
        setStatusIndicatorColor(currentAttempt.getStatus());
    }

    private void setAssignmentTitle() {
        String title = currentAttempt.getAssignmentTitle();
        if (isValidString(title)) {
            tvAssignmentTitle.setText(title);
        } else {
            tvAssignmentTitle.setText(formatAssignmentId(currentAttempt.getAssignmentId()));
        }
    }

    private void setScoreInformation() {
        tvScore.setText(String.valueOf(currentAttempt.getScore()));
        tvMaxScore.setText("/ " + currentAttempt.getMaxScore());
        tvPercentage.setText(currentAttempt.getFormattedPercentage());
        setScoreColor(currentAttempt.getPercentageScore());
    }

    private void setStatusInformation() {
        String status = currentAttempt.getStatus();
        setupStatusChip(status);
        tvStatus.setText(status);

        boolean isChecked = currentAttempt.isChecked();
        setupCheckedChip(isChecked);

        if (isChecked) {
            tvCheckedStatus.setText("Graded by instructor");
            setGradedTime();
        } else {
            tvCheckedStatus.setText("Awaiting grading");
            tvGradedTime.setVisibility(View.GONE);
        }
    }

    private void setGradedTime() {
        if (currentAttempt.getGradedAt() > 0) {
            tvGradedTime.setText("Graded on " + currentAttempt.getFormattedGradedDate());
            tvGradedTime.setVisibility(View.VISIBLE);
        }
    }

    private void setSubmissionTime() {
        tvSubmissionTime.setText(formatSubmissionTime(currentAttempt.getSubmissionTimestamp()));
    }

    private void setFeedbackInformation() {
        if (currentAttempt.hasFeedback()) {
            tvFeedback.setText(currentAttempt.getFeedback());
            feedbackCard.setVisibility(View.VISIBLE);
        } else {
            feedbackCard.setVisibility(View.GONE);
        }
    }

    private void setImageInformation() {
        int imageCount = currentAttempt.getSubmittedImagesCount();
        String imageText = imageCount + " image" + (imageCount != 1 ? "s" : "") + " submitted";
        tvImageCount.setText(imageText);
    }

    private void updateCardVisibility() {
        int imageCount = currentAttempt.getSubmittedImagesCount();
        imagesCard.setVisibility(imageCount > 0 ? View.VISIBLE : View.GONE);
    }

    private String formatAssignmentId(int assignmentId) {
        return "Assignment #" + assignmentId;
    }

    private String formatSubmissionTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void setScoreColor(double percentage) {
        int colorRes = getScoreColorResource(percentage);
        int color = ContextCompat.getColor(this, colorRes);
        tvScore.setTextColor(color);
        tvPercentage.setTextColor(color);
    }

    private int getScoreColorResource(double percentage) {
        if (percentage >= 80) {
            return R.color.success;
        } else if (percentage >= 60) {
            return R.color.warning;
        } else {
            return R.color.error;
        }
    }

    private void setupStatusChip(String status) {
        chipStatus.setText(status);

        StatusColors colors = getStatusColors(status);
        chipStatus.setChipBackgroundColorResource(colors.backgroundColorRes);
        chipStatus.setTextColor(ContextCompat.getColor(this, colors.textColorRes));
    }

    private StatusColors getStatusColors(String status) {
        switch (status.toLowerCase()) {
            case "submitted":
                return new StatusColors(R.color.status_submitted, R.color.text_on_primary);
            case "graded":
                return new StatusColors(R.color.success, R.color.text_on_primary);
            case "failed":
                return new StatusColors(R.color.status_overdue, R.color.text_on_primary);
            case "pending":
                return new StatusColors(R.color.status_in_progress, R.color.text_on_primary);
            default:
                return new StatusColors(R.color.gray_medium, R.color.text_on_primary);
        }
    }

    private void setupCheckedChip(boolean isChecked) {
        if (isChecked) {
            setupGradedChip();
        } else {
            setupPendingChip();
        }
    }

    private void setupGradedChip() {
        chipChecked.setText("Graded");
        chipChecked.setChipBackgroundColorResource(R.color.success);
        chipChecked.setTextColor(ContextCompat.getColor(this, R.color.white));
        chipChecked.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_checked));
    }

    private void setupPendingChip() {
        chipChecked.setText("Pending");
        chipChecked.setChipBackgroundColorResource(R.color.warning);
        chipChecked.setTextColor(ContextCompat.getColor(this, R.color.white));
        chipChecked.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_pending));
    }

    private void setStatusIndicatorColor(String status) {
        int colorRes = getStatusColorResource(status);
        statusIndicator.setBackgroundColor(ContextCompat.getColor(this, colorRes));
    }

    private int getStatusColorResource(String status) {
        switch (status.toLowerCase()) {
            case "submitted":
                return R.color.status_submitted;
            case "graded":
                return R.color.success;
            case "failed":
                return R.color.status_overdue;
            case "pending":
                return R.color.status_in_progress;
            default:
                return R.color.gray_medium;
        }
    }

    private void loadSubmittedImages() {
        if (!hasSubmittedImages()) return;

        List<String> imageBase64List = currentAttempt.getSubmittedImages();
        imageLoadingProgress.setVisibility(View.VISIBLE);
        imagesContainer.removeAllViews();

        loadImagesInBackground(imageBase64List);
    }

    private boolean hasSubmittedImages() {
        return currentAttempt != null && currentAttempt.hasSubmittedImages();
    }

    private void loadImagesInBackground(List<String> imageBase64List) {
        new Thread(() -> {
            for (int i = 0; i < imageBase64List.size(); i++) {
                final int imageIndex = i + 1;
                final String base64Image = imageBase64List.get(i);
                final boolean isLast = i == imageBase64List.size() - 1;

                if (isValidString(base64Image)) {
                    loadSingleImage(base64Image, imageIndex, isLast);
                } else {
                    handleInvalidImage(imageIndex, isLast);
                }
            }
        }).start();
    }

    private void handleInvalidImage(int imageIndex, boolean isLast) {
        runOnUiThread(() -> {
            addErrorImageView("Invalid image data for Image " + imageIndex);
            if (isLast) imageLoadingProgress.setVisibility(View.GONE);
        });
    }

    private void loadSingleImage(String base64Image, int imageIndex, boolean isLast) {
        try {
            Bitmap bitmap = decodeBase64ToBitmap(base64Image);
            runOnUiThread(() -> {
                handleImageLoadResult(bitmap, imageIndex, isLast);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading image " + imageIndex, e);
            runOnUiThread(() -> {
                addErrorImageView("Error loading Image " + imageIndex);
                if (isLast) imageLoadingProgress.setVisibility(View.GONE);
            });
        }
    }

    private Bitmap decodeBase64ToBitmap(String base64Image) {
        byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private void handleImageLoadResult(Bitmap bitmap, int imageIndex, boolean isLast) {
        if (bitmap != null) {
            addImageView(bitmap, imageIndex);
        } else {
            addErrorImageView("Failed to load Image " + imageIndex);
        }

        if (isLast) {
            imageLoadingProgress.setVisibility(View.GONE);
        }
    }

    private void addImageView(Bitmap bitmap, int imageIndex) {
        View imageItemView = LayoutInflater.from(this)
                .inflate(R.layout.item_submitted_image, imagesContainer, false);

        ImageView imageView = imageItemView.findViewById(R.id.ivSubmittedImage);
        TextView tvImageTitle = imageItemView.findViewById(R.id.tvImageTitle);
        TextView tvImageSize = imageItemView.findViewById(R.id.tvImageSize);

        setupImageView(imageView, bitmap, imageIndex);
        setupImageInfo(tvImageTitle, tvImageSize, bitmap, imageIndex);

        imagesContainer.addView(imageItemView);
    }

    private void setupImageView(ImageView imageView, Bitmap bitmap, int imageIndex) {
        imageView.setImageBitmap(bitmap);
        imageView.setOnClickListener(v -> showFullScreenImage(bitmap, "Image " + imageIndex));
    }

    private void setupImageInfo(TextView tvImageTitle, TextView tvImageSize, Bitmap bitmap, int imageIndex) {
        tvImageTitle.setText("Image " + imageIndex);

        long sizeInBytes = bitmap.getByteCount();
        String sizeText = formatFileSize(sizeInBytes);
        String dimensionsText = bitmap.getWidth() + " × " + bitmap.getHeight() + " • " + sizeText;
        tvImageSize.setText(dimensionsText);
    }

    private void addErrorImageView(String errorMessage) {
        View errorView = LayoutInflater.from(this)
                .inflate(R.layout.item_image_error, imagesContainer, false);
        TextView tvErrorMessage = errorView.findViewById(R.id.tvErrorMessage);
        tvErrorMessage.setText(errorMessage);
        imagesContainer.addView(errorView);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";

        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String prefix = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(1024, exp), prefix);
    }

    private void showFullScreenImage(Bitmap bitmap, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_fullscreen_image, null);

        setupFullScreenDialog(dialogView, bitmap, title);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        setupFullScreenImageDialog(dialog, dialogView);
        dialog.show();
    }

    private void setupFullScreenDialog(View dialogView, Bitmap bitmap, String title) {
        ImageView fullImageView = dialogView.findViewById(R.id.ivFullscreenImage);
        TextView tvFullImageTitle = dialogView.findViewById(R.id.tvFullImageTitle);
        ImageView btnClose = dialogView.findViewById(R.id.btnCloseFullscreen);

        fullImageView.setImageBitmap(bitmap);
        tvFullImageTitle.setText(title);
    }

    private void setupFullScreenImageDialog(AlertDialog dialog, View dialogView) {
        ImageView btnClose = dialogView.findViewById(R.id.btnCloseFullscreen);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
            );
        }
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

    private void shareAssignmentDetails() {
        if (currentAttempt == null) return;

        String shareText = buildShareText();
        String subject = buildShareSubject();

        Intent shareIntent = createShareIntent(shareText, subject);
        startActivity(Intent.createChooser(shareIntent, "Share Assignment Details"));
    }

    private String buildShareText() {
        StringBuilder shareText = new StringBuilder();
        shareText.append("Assignment Submission Details\n");
        shareText.append("============================\n\n");

        appendAssignmentInfo(shareText);
        appendScoreInfo(shareText);
        appendStatusInfo(shareText);
        appendTimestampInfo(shareText);
        appendImageInfo(shareText);
        appendFeedbackInfo(shareText);

        return shareText.toString();
    }

    private void appendAssignmentInfo(StringBuilder shareText) {
        if (currentAttempt.getAssignmentTitle() != null) {
            shareText.append("Assignment: ").append(currentAttempt.getAssignmentTitle()).append("\n");
        }
        shareText.append("Assignment ID: #").append(currentAttempt.getAssignmentId()).append("\n");
    }

    private void appendScoreInfo(StringBuilder shareText) {
        shareText.append("Score: ").append(currentAttempt.getScore())
                .append("/").append(currentAttempt.getMaxScore())
                .append(" (").append(currentAttempt.getFormattedPercentage()).append(")\n");
    }

    private void appendStatusInfo(StringBuilder shareText) {
        shareText.append("Status: ").append(currentAttempt.getStatus()).append("\n");
        shareText.append("Grading Status: ").append(currentAttempt.isChecked() ? "Graded" : "Pending").append("\n");
    }

    private void appendTimestampInfo(StringBuilder shareText) {
        shareText.append("Submitted: ").append(formatSubmissionTime(currentAttempt.getSubmissionTimestamp())).append("\n");

        if (currentAttempt.isGraded()) {
            shareText.append("Graded: ").append(currentAttempt.getFormattedGradedDate()).append("\n");
        }
    }

    private void appendImageInfo(StringBuilder shareText) {
        shareText.append("Images: ").append(currentAttempt.getSubmittedImagesCount()).append(" submitted\n");
    }

    private void appendFeedbackInfo(StringBuilder shareText) {
        if (currentAttempt.hasFeedback()) {
            shareText.append("\nFeedback:\n").append(currentAttempt.getFeedback()).append("\n");
        }
    }

    private String buildShareSubject() {
        return "Assignment Submission - " +
                (currentAttempt.getAssignmentTitle() != null ?
                        currentAttempt.getAssignmentTitle() :
                        formatAssignmentId(currentAttempt.getAssignmentId()));
    }

    private Intent createShareIntent(String shareText, String subject) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        return shareIntent;
    }

    private void showAssignmentStats() {
        if (currentAttempt == null) return;

        String statsText = buildStatsText();
        new AlertDialog.Builder(this)
                .setTitle("Assignment Statistics")
                .setMessage(statsText)
                .setPositiveButton("Close", null)
                .show();
    }

    private String buildStatsText() {
        StringBuilder statsText = new StringBuilder();
        statsText.append("Assignment Statistics\n\n");

        appendAssignmentInfo(statsText);
        statsText.append("Course ID: #").append(currentAttempt.getCourseId()).append("\n");
        appendScoreInfo(statsText);
        statsText.append("Percentage: ").append(currentAttempt.getFormattedPercentage()).append("\n");
        appendStatusInfo(statsText);
        appendTimestampInfo(statsText);
        appendImageInfo(statsText);
        statsText.append("Grading Status: ").append(currentAttempt.isChecked() ? "Graded" : "Pending").append("\n");

        return statsText.toString();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        scoreCard.setVisibility(show ? View.GONE : View.VISIBLE);
        detailsCard.setVisibility(show ? View.GONE : View.VISIBLE);

        if (!show && currentAttempt != null) {
            updateCardVisibilityAfterLoad();
        }
    }

    private void updateCardVisibilityAfterLoad() {
        if (currentAttempt.hasSubmittedImages()) {
            imagesCard.setVisibility(View.VISIBLE);
        }
        if (currentAttempt.hasFeedback()) {
            feedbackCard.setVisibility(View.VISIBLE);
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

    private boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private static class StatusColors {
        final int backgroundColorRes;
        final int textColorRes;

        StatusColors(int backgroundColorRes, int textColorRes) {
            this.backgroundColorRes = backgroundColorRes;
            this.textColorRes = textColorRes;
        }
    }
}