package com.sowp.admin.assignmentmanagement;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.sowp.admin.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AssignmentDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AssignmentDetailsActivity";

    // UI Components
    private TextView tvAssignmentTitle, tvStudentEmail, tvSubmissionTime, tvImageCount, tvMaxScore;
    private LinearLayout imagesContainer;
    private ProgressBar progressBar, imageLoadingProgress;
    private MaterialCardView detailsCard, imagesCard, gradingCard;
    private Button btnGradeAssignment;
    private TextInputLayout tilScore, tilFeedback;
    private TextInputEditText etScore, etFeedback;

    // Data
    private FirebaseFirestore db;
    private String uncheckedAssignmentId;
    private String assignmentAttemptRefPath;
    private Map<String, Object> assignmentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_details);

        initializeViews();
        setupToolbar();

        db = FirebaseFirestore.getInstance();

        // Get intent extras
        uncheckedAssignmentId = getIntent().getStringExtra("uncheckedAssignmentId");
        assignmentAttemptRefPath = getIntent().getStringExtra("assignmentAttemptRef");

        if (uncheckedAssignmentId == null || assignmentAttemptRefPath == null) {
            showError("Invalid assignment data");
            finish();
            return;
        }

        loadAssignmentDetails();
    }

    private void initializeViews() {
        // Text Views
        tvAssignmentTitle = findViewById(R.id.tvAssignmentTitle);
        tvStudentEmail = findViewById(R.id.tvStudentEmail);
        tvSubmissionTime = findViewById(R.id.tvSubmissionTime);
        tvImageCount = findViewById(R.id.tvImageCount);
        tvMaxScore = findViewById(R.id.tvMaxScore);

        // Layouts and containers
        imagesContainer = findViewById(R.id.imagesContainer);

        // Progress bars
        progressBar = findViewById(R.id.progressBar);
        imageLoadingProgress = findViewById(R.id.imageLoadingProgress);

        // Cards
        detailsCard = findViewById(R.id.detailsCard);
        imagesCard = findViewById(R.id.imagesCard);
        gradingCard = findViewById(R.id.gradingCard);

        // Grading components
        btnGradeAssignment = findViewById(R.id.btnGradeAssignment);
        tilScore = findViewById(R.id.tilScore);
        tilFeedback = findViewById(R.id.tilFeedback);
        etScore = findViewById(R.id.etScore);
        etFeedback = findViewById(R.id.etFeedback);

        // Set click listeners
        btnGradeAssignment.setOnClickListener(v -> showGradingDialog());
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

        // Load assignment attempt details using the reference path
        db.document(assignmentAttemptRefPath)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        assignmentData = documentSnapshot.getData();
                        showLoading(false);
                        populateDetails();
                        loadSubmittedImages();
                    } else {
                        showLoading(false);
                        showError("Assignment attempt not found");
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError("Failed to load assignment details: " + e.getMessage());
                    Log.e(TAG, "Failed to load assignment details: ", e);
                });
    }

    private void populateDetails() {
        if (assignmentData == null) return;

        // Set assignment title
        String title = (String) assignmentData.get("assignmentTitle");
        if (title != null && !title.trim().isEmpty()) {
            tvAssignmentTitle.setText(title);
        } else {
            Object assignmentId = assignmentData.get("assignmentId");
            tvAssignmentTitle.setText("Assignment #" + (assignmentId != null ? assignmentId.toString() : "Unknown"));
        }

        // Set student email
        String userEmail = (String) assignmentData.get("userEmail");
        tvStudentEmail.setText("Student: " + (userEmail != null ? userEmail : "Unknown"));

        // Set submission time
        Object submissionTimestamp = assignmentData.get("submissionTimestamp");
        if (submissionTimestamp instanceof Long) {
            tvSubmissionTime.setText("Submitted: " + formatTimestamp((Long) submissionTimestamp));
        } else {
            tvSubmissionTime.setText("Submitted: Unknown");
        }

        // Set image count
        List<?> images = (List<?>) assignmentData.get("submittedImages");
        int imageCount = images != null ? images.size() : 0;
        tvImageCount.setText(imageCount + " image" + (imageCount != 1 ? "s" : "") + " submitted");

        // Show/hide cards based on content
        imagesCard.setVisibility(imageCount > 0 ? View.VISIBLE : View.GONE);

        // Set max score from assignment data or default
        Object maxScoreObj = assignmentData.get("maxScore");
        if (maxScoreObj != null) {
            tvMaxScore.setText(maxScoreObj.toString());
        } else {
            // Default max score if not found in assignment data
            tvMaxScore.setText("100");
        }
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void loadSubmittedImages() {
        List<?> imagesList = (List<?>) assignmentData.get("submittedImages");
        if (imagesList == null || imagesList.isEmpty()) {
            return;
        }

        imageLoadingProgress.setVisibility(View.VISIBLE);
        imagesContainer.removeAllViews();

        // Load each image in background thread
        new Thread(() -> {
            for (int i = 0; i < imagesList.size(); i++) {
                final int imageIndex = i + 1;
                final Object imageObj = imagesList.get(i);
                final boolean isLast = i == imagesList.size() - 1;

                if (imageObj instanceof String) {
                    String base64Image = (String) imageObj;
                    if (!base64Image.trim().isEmpty()) {
                        loadSingleImage(base64Image, imageIndex, isLast);
                    } else {
                        runOnUiThread(() -> {
                            addErrorImageView("Invalid image data for Image " + imageIndex);
                            if (isLast) imageLoadingProgress.setVisibility(View.GONE);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        addErrorImageView("Invalid image format for Image " + imageIndex);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_fullscreen_image_assignment, null);

        ImageView fullImageView = dialogView.findViewById(R.id.ivFullscreenImage);
        TextView tvFullImageTitle = dialogView.findViewById(R.id.tvFullImageTitle);
        ImageView btnClose = dialogView.findViewById(R.id.btnCloseFullscreen);

        fullImageView.setImageBitmap(bitmap);
        tvFullImageTitle.setText(title);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

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

    private void showGradingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Grade Assignment");
        builder.setMessage("Are you sure you want to submit this grade? This action cannot be undone.");

        builder.setPositiveButton("Submit Grade", (dialog, which) -> submitGrade());
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void submitGrade() {
        // Validate input
        String scoreText = etScore.getText().toString().trim();
        String maxScoreText = tvMaxScore.getText().toString().trim();
        String feedback = etFeedback.getText().toString().trim();

        if (scoreText.isEmpty()) {
            tilScore.setError("Score is required");
            etScore.requestFocus();
            return;
        }

        // Max score validation (should always be valid since it's from database, but check anyway)
        if (maxScoreText.isEmpty()) {
            showError("Max score not found. Please try again.");
            return;
        }

        try {
            int score = Integer.parseInt(scoreText);
            int maxScore = Integer.parseInt(maxScoreText);

            if (score < 0) {
                tilScore.setError("Score cannot be negative");
                etScore.requestFocus();
                return;
            }

            if (score > maxScore) {
                tilScore.setError("Score cannot be greater than max score (" + maxScore + ")");
                etScore.requestFocus();
                return;
            }

            // Clear errors
            tilScore.setError(null);

            // Disable grading button and show progress
            btnGradeAssignment.setEnabled(false);
            btnGradeAssignment.setText("Submitting Grade...");

            // Submit grade
            submitGradeToFirestore(score, maxScore, feedback);

        } catch (NumberFormatException e) {
            if (!scoreText.matches("\\d+")) {
                tilScore.setError("Please enter a valid number");
                etScore.requestFocus();
            } else {
                showError("Invalid max score data. Please try again.");
            }
        }
    }

    private void submitGradeToFirestore(int score, int maxScore, String feedback) {
        // Create batch operation
        WriteBatch batch = db.batch();

        // Update assignment attempt with grade
        DocumentReference attemptRef = db.document(assignmentAttemptRefPath);
        Map<String, Object> gradeUpdates = new HashMap<>();
        gradeUpdates.put("score", score);
        gradeUpdates.put("maxScore", maxScore);
        gradeUpdates.put("checked", true);
        gradeUpdates.put("gradedAt", System.currentTimeMillis());
        gradeUpdates.put("status", "graded");

        if (!feedback.isEmpty()) {
            gradeUpdates.put("feedback", feedback);
        }

        batch.update(attemptRef, gradeUpdates);

        // Delete from unchecked assignments
        DocumentReference uncheckedRef = db.collection("uncheckedAssignments").document(uncheckedAssignmentId);
        batch.delete(uncheckedRef);

        // Commit batch
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Grade submitted successfully!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Grade submitted and unchecked assignment deleted");

                    // Show success dialog and finish activity
                    showSuccessDialog(score, maxScore);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to submit grade: ", e);
                    showError("Failed to submit grade: " + e.getMessage());

                    // Re-enable grading button
                    btnGradeAssignment.setEnabled(true);
                    btnGradeAssignment.setText("Grade Assignment");
                });
    }

    private void showSuccessDialog(int score, int maxScore) {
        double percentage = (double) score / maxScore * 100;
        String message = String.format(Locale.getDefault(),
                "Grade: %d/%d (%.1f%%)\nThe assignment has been graded successfully and removed from pending list.",
                score, maxScore, percentage);

        new AlertDialog.Builder(this)
                .setTitle("Grade Submitted")
                .setMessage(message)
                .setPositiveButton("Done", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        detailsCard.setVisibility(show ? View.GONE : View.VISIBLE);
        gradingCard.setVisibility(show ? View.GONE : View.VISIBLE);
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