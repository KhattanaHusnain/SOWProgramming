package com.android.nexcode.presenters.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
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

import com.android.nexcode.R;
import com.android.nexcode.models.AssignmentAttempt;
import com.android.nexcode.repositories.firebase.UserRepository;
import com.android.nexcode.utils.Base64ImageUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssignmentDetailActivity extends AppCompatActivity {

    private TextView tvAssignmentTitle, tvScore, tvPercentage, tvMaxScore, tvStatus,
            tvSubmissionTime, tvCheckedStatus, tvImageCount;
    private Chip chipStatus, chipChecked;
    private LinearLayout imagesContainer;
    private ProgressBar progressBar, imageLoadingProgress;
    private MaterialCardView scoreCard, detailsCard, imagesCard;
    private View statusIndicator;

    private UserRepository userRepository;
    private String attemptId;
    private String assignmentId;
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
        assignmentId = getIntent().getStringExtra("assignmentId");

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
        tvAssignmentTitle = findViewById(R.id.tvAssignmentTitle);
        tvScore = findViewById(R.id.tvScore);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvMaxScore = findViewById(R.id.tvMaxScore);
        tvStatus = findViewById(R.id.tvStatus);
        tvSubmissionTime = findViewById(R.id.tvSubmissionTime);
        tvCheckedStatus = findViewById(R.id.tvCheckedStatus);
        tvImageCount = findViewById(R.id.tvImageCount);

        chipStatus = findViewById(R.id.chipStatus);
        chipChecked = findViewById(R.id.chipChecked);

        imagesContainer = findViewById(R.id.imagesContainer);
        progressBar = findViewById(R.id.progressBar);
        imageLoadingProgress = findViewById(R.id.imageLoadingProgress);

        scoreCard = findViewById(R.id.scoreCard);
        detailsCard = findViewById(R.id.detailsCard);
        imagesCard = findViewById(R.id.imagesCard);

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
            }
        });
    }

    private void populateDetails() {
        if (currentAttempt == null) return;

        // Set assignment title
        tvAssignmentTitle.setText(formatAssignmentId(currentAttempt.getAssignmentId()));

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
        tvCheckedStatus.setText(currentAttempt.isChecked() ? "Checked by instructor" : "Not yet checked");

        // Set submission time
        tvSubmissionTime.setText(formatSubmissionTime(currentAttempt.getSubmissionTimestamp()));

        // Set image count
        int imageCount = currentAttempt.getSubmittedImagesCount();
        tvImageCount.setText(imageCount + " image" + (imageCount != 1 ? "s" : "") + " submitted");

        // Show/hide images card based on whether there are images
        imagesCard.setVisibility(imageCount > 0 ? View.VISIBLE : View.GONE);

        // Set status indicator color
        setStatusIndicatorColor(currentAttempt.getStatus());
    }

    private String formatAssignmentId(String assignmentId) {
        if (assignmentId != null && assignmentId.toLowerCase().startsWith("assignment")) {
            String number = assignmentId.substring(10);
            return "Assignment " + number;
        }
        return assignmentId != null ? assignmentId : "Unknown Assignment";
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
            case "pending":
                backgroundColorRes = R.color.status_pending_bg;
                textColorRes = R.color.status_pending_text;
                break;
            case "late":
                backgroundColorRes = R.color.status_late_bg;
                textColorRes = R.color.status_late_text;
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
            chipChecked.setText("Checked");
            chipChecked.setChipBackgroundColorResource(R.color.success_bg);
            chipChecked.setTextColor(ContextCompat.getColor(this, R.color.success_color));
            chipChecked.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_checked));
        } else {
            chipChecked.setText("Pending");
            chipChecked.setChipBackgroundColorResource(R.color.warning_bg);
            chipChecked.setTextColor(ContextCompat.getColor(this, R.color.warning_color));
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
            case "pending":
                colorRes = R.color.status_pending_bg;
                break;
            case "late":
                colorRes = R.color.status_late_bg;
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

        // Load each image
        for (int i = 0; i < imageBase64List.size(); i++) {
            final int imageIndex = i + 1;
            final String base64Image = imageBase64List.get(i);

            if (base64Image != null && !base64Image.trim().isEmpty()) {
                loadSingleImage(base64Image, imageIndex, i == imageBase64List.size() - 1);
            }
        }
    }

    private void loadSingleImage(String base64Image, int imageIndex, boolean isLast) {
        // Validate Base64 string
        if (!Base64ImageUtils.isValidBase64Image(base64Image)) {
            addErrorImageView("Invalid image data for Image " + imageIndex);
            if (isLast) imageLoadingProgress.setVisibility(View.GONE);
            return;
        }

        // Convert Base64 to Bitmap
        Bitmap bitmap = Base64ImageUtils.base64ToBitmap(base64Image);
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
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void showFullScreenImage(Bitmap bitmap, String title) {
        // Create full screen image dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
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

        // Make image zoomable with pinch gestures
        setupImageZoom(fullImageView);

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

    private void setupImageZoom(ImageView imageView) {
        // Enable zoom functionality
        android.view.ScaleGestureDetector scaleDetector = new android.view.ScaleGestureDetector(this, new ScaleListener(imageView));
        android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this, new GestureListener(imageView));

        imageView.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    // Scale gesture listener for pinch zoom
    private static class ScaleListener extends android.view.ScaleGestureDetector.SimpleOnScaleGestureListener {
        private ImageView imageView;
        private float scaleFactor = 1.0f;

        public ScaleListener(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        public boolean onScale(android.view.ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 5.0f)); // Limit zoom range
            imageView.setScaleX(scaleFactor);
            imageView.setScaleY(scaleFactor);
            return true;
        }
    }

    // Gesture listener for double tap to reset zoom
    private static class GestureListener extends android.view.GestureDetector.SimpleOnGestureListener {
        private ImageView imageView;

        public GestureListener(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        public boolean onDoubleTap(android.view.MotionEvent e) {
            // Reset zoom on double tap
            imageView.setScaleX(1.0f);
            imageView.setScaleY(1.0f);
            return true;
        }
    }

    /**
     * Share assignment details as text
     */
    private void shareAssignmentDetails() {
        if (currentAttempt == null) return;

        StringBuilder shareText = new StringBuilder();
        shareText.append("Assignment Details\n");
        shareText.append("==================\n\n");
        shareText.append("Assignment: ").append(formatAssignmentId(currentAttempt.getAssignmentId())).append("\n");
        shareText.append("Score: ").append(currentAttempt.getScore()).append("/").append(currentAttempt.getMaxScore());
        shareText.append(" (").append(currentAttempt.getFormattedPercentage()).append(")\n");
        shareText.append("Status: ").append(currentAttempt.getStatus()).append("\n");
        shareText.append("Grading Status: ").append(currentAttempt.isChecked() ? "Checked" : "Pending").append("\n");
        shareText.append("Submitted: ").append(formatSubmissionTime(currentAttempt.getSubmissionTimestamp())).append("\n");
        shareText.append("Images: ").append(currentAttempt.getSubmittedImagesCount()).append(" submitted\n");

        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText.toString());
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Assignment Details - " + formatAssignmentId(currentAttempt.getAssignmentId()));

        startActivity(android.content.Intent.createChooser(shareIntent, "Share Assignment Details"));
    }

    /**
     * Show assignment statistics dialog
     */
    private void showAssignmentStats() {
        if (currentAttempt == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_assignment_stats, null);

        TextView tvAssignmentId = dialogView.findViewById(R.id.tvStatsAssignmentId);
        TextView tvScore = dialogView.findViewById(R.id.tvStatsScore);
        TextView tvPercentage = dialogView.findViewById(R.id.tvStatsPercentage);
        TextView tvStatus = dialogView.findViewById(R.id.tvStatsStatus);
        TextView tvSubmissionDate = dialogView.findViewById(R.id.tvStatsSubmissionDate);
        TextView tvImageCount = dialogView.findViewById(R.id.tvStatsImageCount);
        TextView tvGradingStatus = dialogView.findViewById(R.id.tvStatsGradingStatus);

        // Populate stats
        tvAssignmentId.setText(formatAssignmentId(currentAttempt.getAssignmentId()));
        tvScore.setText(currentAttempt.getScore() + " / " + currentAttempt.getMaxScore());
        tvPercentage.setText(currentAttempt.getFormattedPercentage());
        tvStatus.setText(currentAttempt.getStatus());
        tvSubmissionDate.setText(formatSubmissionTime(currentAttempt.getSubmissionTimestamp()));
        tvImageCount.setText(String.valueOf(currentAttempt.getSubmittedImagesCount()));
        tvGradingStatus.setText(currentAttempt.isChecked() ? "Checked by instructor" : "Awaiting grading");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Assignment Statistics")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_assignment_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
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

        if (!show && currentAttempt != null && currentAttempt.hasSubmittedImages()) {
            imagesCard.setVisibility(View.VISIBLE);
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