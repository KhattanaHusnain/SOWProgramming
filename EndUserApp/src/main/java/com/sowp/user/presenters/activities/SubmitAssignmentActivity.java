package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.models.Assignment;
import com.sowp.user.models.AssignmentAttempt;
import com.sowp.user.repositories.AssignmentRepository;
import com.sowp.user.repositories.UserRepository;
import com.sowp.user.services.ImageService;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubmitAssignmentActivity extends AppCompatActivity implements ImageService.ImageCallback {

    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView dueDateTextView;
    private TextView statusTextView;
    private TextView maxScoreTextView;
    private TextView submissionTitleTextView;
    private TextView tagsTextView;
    private TextView categoriesTextView;
    private LinearLayout assignmentImagesContainer;
    private LinearLayout selectedImagesContainer;
    private Button selectImagesButton;
    private Button submitButton;
    private ProgressBar progressBar;
    private RecyclerView selectedImagesRecyclerView;

    private int courseId;
    private int assignmentId;
    private Assignment assignment;
    private List<String> selectedImageBase64List = new ArrayList<>();
    private SelectedImagesAdapter selectedImagesAdapter;

    private AssignmentRepository assignmentRepository;
    private UserRepository userRepository;
    private FirebaseAuth firebaseAuth;
    private ImageService imageService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_assignment);

        initializeRepositories();
        initializeViews();
        initializeImageService();
        getAssignmentDataFromIntent();
        setupRecyclerView();
        setupClickListeners();
        loadAssignmentData();
    }

    private void initializeRepositories() {
        assignmentRepository = new AssignmentRepository(this);
        userRepository = new UserRepository(this);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        titleTextView = findViewById(R.id.titleTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        dueDateTextView = findViewById(R.id.dueDateTextView);
        statusTextView = findViewById(R.id.statusTextView);
        maxScoreTextView = findViewById(R.id.maxScoreTextView);
        submissionTitleTextView = findViewById(R.id.submissionTitle);
        assignmentImagesContainer = findViewById(R.id.assignmentImagesContainer);
        selectedImagesContainer = findViewById(R.id.selectedImagesContainer);
        selectImagesButton = findViewById(R.id.selectImagesButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);
        selectedImagesRecyclerView = findViewById(R.id.selectedImagesRecyclerView);
        tagsTextView = findViewById(R.id.tagsTextView);
        categoriesTextView = findViewById(R.id.categoriesTextView);
    }

    private void initializeImageService() {
        imageService = new ImageService(this, this);
    }

    private void getAssignmentDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            courseId = intent.getIntExtra("COURSE_ID", 0);
            assignmentId = intent.getIntExtra("ASSIGNMENT_ID", 0);
        }

        if (courseId == 0 || assignmentId == 0) {
            finish();
        }
    }

    private void setupRecyclerView() {
        selectedImagesAdapter = new SelectedImagesAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        selectedImagesRecyclerView.setLayoutManager(layoutManager);
        selectedImagesRecyclerView.setAdapter(selectedImagesAdapter);
    }

    private void setupClickListeners() {
        selectImagesButton.setOnClickListener(v -> imageService.requestPermissions());
        submitButton.setOnClickListener(v -> submitAssignment());
    }

    // ImageService.ImageCallback implementation
    @Override
    public void onImageSelected(String base64String) {
        if (!selectedImageBase64List.contains(base64String)) {
            selectedImageBase64List.add(base64String);
            selectedImagesAdapter.notifyDataSetChanged();
            updateUIState();
            Toast.makeText(this, "Image added successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Image already selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(String error) {
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionDenied() {
        Toast.makeText(this, "Permission denied. Cannot select images.", Toast.LENGTH_LONG).show();
    }

    private void loadAssignmentData() {
        showLoading(true);

        List<Integer> courseIds = new ArrayList<>();
        courseIds.add(courseId);

        assignmentRepository.loadAllAssignmentsFromMultipleCourses(courseIds, new AssignmentRepository.Callback() {
            @Override
            public void onSuccess(List<Assignment> assignments) {
                showLoading(false);

                Assignment targetAssignment = null;
                for (Assignment a : assignments) {
                    if (a.getId() == assignmentId) {
                        targetAssignment = a;
                        break;
                    }
                }

                if (targetAssignment != null) {
                    assignment = targetAssignment;
                    setupUI();
                } else {
                    finish();
                }
            }

            @Override
            public void onFailure(String message) {
                showLoading(false);
                finish();
            }
        });
    }

    private void setupUI() {
        if (assignment == null) return;

        titleTextView.setText(assignment.getTitle());
        descriptionTextView.setText(assignment.getDescription());

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dueDateText = "Due: " + dateFormat.format(new Date(assignment.getCreatedAt() + (7 * 24 * 60 * 60 * 1000)));
        dueDateTextView.setText(dueDateText);

        maxScoreTextView.setText("Max Score: " + (int) assignment.getScore());

        if (assignment.getTags() != null && !assignment.getTags().isEmpty()) {
            String tagsText = "Tags: " + String.join(", ", assignment.getTags());
            tagsTextView.setText(tagsText);
            tagsTextView.setVisibility(View.VISIBLE);
        } else {
            tagsTextView.setVisibility(View.GONE);
        }

        if (assignment.getCategories() != null && !assignment.getCategories().isEmpty()) {
            String categoriesText = "Categories: " + String.join(", ", assignment.getCategories());
            categoriesTextView.setText(categoriesText);
            categoriesTextView.setVisibility(View.VISIBLE);
        } else {
            categoriesTextView.setVisibility(View.GONE);
        }

        loadAssignmentImages();
        updateUIState();
        statusTextView.setText("Status: Not Started");
    }

    private void loadAssignmentImages() {
        if (assignment.getBase64Images() == null || assignment.getBase64Images().isEmpty()) {
            assignmentImagesContainer.setVisibility(View.GONE);
            return;
        }

        assignmentImagesContainer.setVisibility(View.VISIBLE);
        assignmentImagesContainer.removeAllViews();

        for (String base64Image : assignment.getBase64Images()) {
            try {
                // Use ImageService for consistent base64 to bitmap conversion
                Bitmap bitmap = ImageService.base64ToBitmap(base64Image);

                if (bitmap != null) {
                    ImageView imageView = new ImageView(this);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            400
                    );
                    params.setMargins(0, 0, 0, 16);
                    imageView.setLayoutParams(params);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setImageBitmap(bitmap);

                    assignmentImagesContainer.addView(imageView);
                }
            } catch (Exception e) {
                // Silent error handling
            }
        }
    }

    private void updateUIState() {
        boolean hasImages = !selectedImageBase64List.isEmpty();

        submitButton.setEnabled(hasImages);
        submitButton.setAlpha(hasImages ? 1.0f : 0.5f);
        selectedImagesContainer.setVisibility(hasImages ? View.VISIBLE : View.GONE);

        // Update button text to show image count
        if (hasImages) {
            selectImagesButton.setText("Add More Images (" + selectedImageBase64List.size() + ")");
        } else {
            selectImagesButton.setText("Select Images");
        }
    }

    private void submitAssignment() {
        if (selectedImageBase64List.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        submitToFirestore(selectedImageBase64List);
    }

    private void submitToFirestore(List<String> base64Images) {
        AssignmentAttempt attemptData = createAssignmentAttempt(base64Images);

        userRepository.submitAssignmentAttempt(
                attemptData,
                new UserRepository.AssignmentAttemptCallback() {
                    @Override
                    public void onSuccess(AssignmentAttempt attempt) {
                        handleSubmissionSuccess();
                    }

                    @Override
                    public void onFailure(String message) {
                        handleSubmissionFailure(new Exception(message));
                    }
                }
        );
    }

    private AssignmentAttempt createAssignmentAttempt(List<String> base64Images) {
        long currentTime = System.currentTimeMillis();
        String attemptId = assignmentId + "_" + currentTime;

        AssignmentAttempt attempt = new AssignmentAttempt();
        attempt.setAttemptId(attemptId);
        attempt.setAssignmentId(assignmentId);
        attempt.setAssignmentTitle(assignment.getTitle());
        attempt.setCourseId(courseId);
        attempt.setChecked(false);
        attempt.setMaxScore((int) assignment.getScore());
        attempt.setScore(0);
        attempt.setStatus("Submitted");
        attempt.setSubmissionTimestamp(currentTime);
        attempt.setSubmittedImages(base64Images);
        attempt.setFeedback("");
        attempt.setGradedAt(0);

        return attempt;
    }

    private void handleSubmissionSuccess() {
        showLoading(false);

        statusTextView.setText("Status: Submitted");
        selectImagesButton.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        selectedImagesContainer.setVisibility(View.GONE);
        submissionTitleTextView.setText("Assignment Submitted");

        Toast.makeText(this, "Assignment submitted successfully!", Toast.LENGTH_LONG).show();
    }

    private void handleSubmissionFailure(Exception e) {
        showLoading(false);
        Toast.makeText(this, "Failed to submit assignment. Please try again.", Toast.LENGTH_LONG).show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!show);
        selectImagesButton.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageService != null) {
            imageService.onDestroy();
        }
    }

    private class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder> {

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_selected_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String base64Image = selectedImageBase64List.get(position);
            holder.bind(base64Image, position);
        }

        @Override
        public int getItemCount() {
            return selectedImageBase64List.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;
            private ImageView removeButton;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.selectedImageView);
                removeButton = itemView.findViewById(R.id.removeImageButton);
            }

            public void bind(String base64Image, int position) {
                loadImageIntoView(base64Image);
                setupRemoveButton(position);
            }

            private void loadImageIntoView(String base64Image) {
                try {
                    // Use ImageService for consistent base64 to bitmap conversion
                    Bitmap bitmap = ImageService.base64ToBitmap(base64Image);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    } else {
                        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                    }
                } catch (Exception e) {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }

            private void setupRemoveButton(int position) {
                removeButton.setOnClickListener(v -> {
                    selectedImageBase64List.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, selectedImageBase64List.size());
                    updateUIState();
                    Toast.makeText(SubmitAssignmentActivity.this, "Image removed", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}