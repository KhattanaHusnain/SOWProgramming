package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.models.Assignment;
import com.sowp.user.models.AssignmentAttempt;
import com.sowp.user.repositories.firebase.AssignmentRepository;
import com.sowp.user.repositories.firebase.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubmitAssignmentActivity extends AppCompatActivity {

    private static final String TAG = "SubmitAssignmentActivity";
    private static final int PICK_IMAGES_REQUEST = 1;

    // UI Components
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

    // Data
    private int courseId;
    private int assignmentId;
    private Assignment assignment;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private SelectedImagesAdapter selectedImagesAdapter;

    // Repositories
    private AssignmentRepository assignmentRepository;
    private UserRepository userRepository;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_assignment);

        initializeRepositories();
        initializeViews();
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

    private void getAssignmentDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            courseId = intent.getIntExtra("COURSE_ID", 0);
            assignmentId = intent.getIntExtra("ASSIGNMENT_ID", 0);
        }

        if (courseId == 0 || assignmentId == 0) {
            Toast.makeText(this, "Invalid assignment data", Toast.LENGTH_SHORT).show();
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
        selectImagesButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> submitAssignment());
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
                    Toast.makeText(SubmitAssignmentActivity.this, "Assignment not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(String message) {
                showLoading(false);
                Toast.makeText(SubmitAssignmentActivity.this, "Failed to load assignment: " + message, Toast.LENGTH_SHORT).show();
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
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

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
                Log.e(TAG, "Error loading assignment image", e);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            handleImageSelection(data);
        }
    }

    private void handleImageSelection(Intent data) {
        selectedImageUris.clear();

        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                selectedImageUris.add(imageUri);
            }
        } else if (data.getData() != null) {
            selectedImageUris.add(data.getData());
        }

        selectedImagesAdapter.notifyDataSetChanged();
        updateUIState();

        String message = selectedImageUris.size() + " image(s) selected";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateUIState() {
        boolean hasImages = !selectedImageUris.isEmpty();

        submitButton.setEnabled(hasImages);
        submitButton.setAlpha(hasImages ? 1.0f : 0.5f);
        selectedImagesContainer.setVisibility(hasImages ? View.VISIBLE : View.GONE);
    }

    private void submitAssignment() {
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        processAndSubmitImages();
    }

    private void processAndSubmitImages() {
        new Thread(() -> {
            try {
                List<String> base64Images = convertImagesToBase64();
                runOnUiThread(() -> submitToFirestore(base64Images));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error processing images", e);
                    Toast.makeText(this, "Error processing images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
            }
        }).start();
    }

    private List<String> convertImagesToBase64() throws IOException {
        List<String> base64Images = new ArrayList<>();

        for (Uri imageUri : selectedImageUris) {
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                if (inputStream == null) {
                    throw new IOException("Cannot open image stream");
                }

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) {
                    throw new IOException("Cannot decode image");
                }

                bitmap = resizeBitmapIfNeeded(bitmap);
                String base64String = bitmapToBase64(bitmap);
                base64Images.add(base64String);
                bitmap.recycle();
            }
        }

        return base64Images;
    }

    private Bitmap resizeBitmapIfNeeded(Bitmap bitmap) {
        int maxDimension = 1024;

        if (bitmap.getWidth() <= maxDimension && bitmap.getHeight() <= maxDimension) {
            return bitmap;
        }

        float ratio = Math.min(
                (float) maxDimension / bitmap.getWidth(),
                (float) maxDimension / bitmap.getHeight()
        );

        int newWidth = Math.round(bitmap.getWidth() * ratio);
        int newHeight = Math.round(bitmap.getHeight() * ratio);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {
            outputStream.close();
        } catch (IOException e) {
            Log.w(TAG, "Error closing output stream", e);
        }

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void submitToFirestore(List<String> base64Images) {
        // Create AssignmentAttempt object with all necessary data
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
        attempt.setScore(0); // Initial score
        attempt.setStatus("Submitted");
        attempt.setSubmissionTimestamp(currentTime);
        attempt.setSubmittedImages(base64Images);
        attempt.setFeedback("");
        attempt.setGradedAt(0);

        return attempt;
    }

    private void handleSubmissionSuccess() {
        showLoading(false);
        Toast.makeText(this, "Assignment submitted successfully!", Toast.LENGTH_LONG).show();

        statusTextView.setText("Status: Submitted");
        selectImagesButton.setVisibility(View.GONE);
        submitButton.setVisibility(View.GONE);
        selectedImagesContainer.setVisibility(View.GONE);
        submissionTitleTextView.setText("Assignment Submitted");
    }

    private void handleSubmissionFailure(Exception e) {
        showLoading(false);
        Log.e(TAG, "Error submitting assignment", e);
        Toast.makeText(this, "Failed to submit assignment. Please try again.", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        submitButton.setEnabled(!show);
        selectImagesButton.setEnabled(!show);
    }

    // RecyclerView Adapter for selected images
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
            Uri imageUri = selectedImageUris.get(position);
            holder.bind(imageUri, position);
        }

        @Override
        public int getItemCount() {
            return selectedImageUris.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            private ImageView imageView;
            private Button removeButton;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.selectedImageView);
                removeButton = itemView.findViewById(R.id.removeImageButton);
            }

            public void bind(Uri imageUri, int position) {
                loadImageIntoView(imageUri);
                setupRemoveButton(position);
            }

            private void loadImageIntoView(Uri imageUri) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    Log.e(TAG, "Error loading image preview", e);
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }

            private void setupRemoveButton(int position) {
                removeButton.setOnClickListener(v -> {
                    selectedImageUris.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, selectedImageUris.size());
                    updateUIState();

                    String message = "Image removed. " + selectedImageUris.size() + " remaining";
                    Toast.makeText(SubmitAssignmentActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}