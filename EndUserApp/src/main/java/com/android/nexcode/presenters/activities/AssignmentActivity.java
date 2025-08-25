package com.android.nexcode.presenters.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
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

import com.android.nexcode.R;
import com.android.nexcode.models.Assignment;
import com.android.nexcode.repositories.firebase.UserRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AssignmentActivity extends AppCompatActivity {

    private static final String TAG = "AssignmentActivity";
    private static final int PICK_IMAGES_REQUEST = 1;
    private static final String EXTRA_ASSIGNMENT = "extra_assignment";

    // UI Components
    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView dueDateTextView;
    private TextView statusTextView;
    private TextView maxScoreTextView;
    private TextView submissionTitleTextView;
    private ImageView assignmentImageView;
    private LinearLayout selectedImagesContainer;
    private Button selectImagesButton;
    private Button submitButton;
    private ProgressBar progressBar;
    private RecyclerView selectedImagesRecyclerView;

    // Data
    private Assignment assignment;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private SelectedImagesAdapter selectedImagesAdapter;

    // Repository
    private UserRepository userRepository;

    public static Intent createIntent(android.content.Context context, Assignment assignment) {
        Intent intent = new Intent(context, AssignmentActivity.class);
        intent.putExtra(EXTRA_ASSIGNMENT, assignment);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment);

        initializeRepository();
        initializeViews();
        getAssignmentFromIntent();
        setupUI();
        setupClickListeners();
        checkAssignmentStatus();
    }

    private void initializeRepository() {
        userRepository = new UserRepository(this);
    }

    private void initializeViews() {
        titleTextView = findViewById(R.id.titleTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        dueDateTextView = findViewById(R.id.dueDateTextView);
        statusTextView = findViewById(R.id.statusTextView);
        maxScoreTextView = findViewById(R.id.maxScoreTextView);
        submissionTitleTextView = findViewById(R.id.submissionTitle);
        assignmentImageView = findViewById(R.id.assignmentImageView);
        selectedImagesContainer = findViewById(R.id.selectedImagesContainer);
        selectImagesButton = findViewById(R.id.selectImagesButton);
        submitButton = findViewById(R.id.submitButton);
        progressBar = findViewById(R.id.progressBar);
        selectedImagesRecyclerView = findViewById(R.id.selectedImagesRecyclerView);
    }

    private void getAssignmentFromIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_ASSIGNMENT)) {
            assignment = intent.getParcelableExtra(EXTRA_ASSIGNMENT);
        }

        if (assignment == null) {
            Toast.makeText(this, "Assignment not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupUI() {
        if (assignment == null) return;

        // Set assignment details
        titleTextView.setText(assignment.getTitle());
        descriptionTextView.setText(assignment.getDescription());
        dueDateTextView.setText("Due: " + assignment.getDueDate());
        maxScoreTextView.setText("Max Score: " + assignment.getMaxScore());

        // Load assignment image from base64 if available
        if (assignment.getFileUrl() != null && !assignment.getFileUrl().isEmpty()) {
            loadImageFromBase64(assignment.getFileUrl());
        } else {
            assignmentImageView.setVisibility(View.GONE);
        }

        // Setup RecyclerView for selected images
        selectedImagesAdapter = new SelectedImagesAdapter(selectedImageUris);
        selectedImagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedImagesRecyclerView.setAdapter(selectedImagesAdapter);

        // Set initial submit button state
        updateSubmitButtonState();
    }

    private void checkAssignmentStatus() {
        if (assignment == null) return;

        userRepository.checkAssignmentStatus(assignment.getId(), new UserRepository.AssignmentStatusCallback() {
            @Override
            public void onSuccess(String status, Double score) {
                assignment.setStatus(status);
                if (score != null) {
                    assignment.setEarnedScore(score);
                }

                runOnUiThread(() -> {
                    statusTextView.setText("Status: " + status);

                    if ("Submitted".equals(status)) {
                        selectImagesButton.setVisibility(View.GONE);
                        submitButton.setVisibility(View.GONE);
                        selectedImagesContainer.setVisibility(View.GONE);

                        if (score != null) {
                            submissionTitleTextView.setText("Score: " + score);
                        } else {
                            submissionTitleTextView.setText("Submitted - Awaiting Grade");
                        }
                    } else {
                        submissionTitleTextView.setText("Submit Your Work");
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    statusTextView.setText("Status: Not Started");
                    submissionTitleTextView.setText("Submit Your Work");
                });
            }
        });
    }

    private void loadImageFromBase64(String base64String) {
        try {
            // Remove data:image/jpeg;base64, prefix if present
            String cleanBase64 = base64String;
            if (base64String.contains(",")) {
                cleanBase64 = base64String.split(",")[1];
            }

            byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            assignmentImageView.setImageBitmap(bitmap);
            assignmentImageView.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from base64", e);
            assignmentImageView.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        selectImagesButton.setOnClickListener(v -> openImagePicker());
        submitButton.setOnClickListener(v -> submitAssignment());
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
            selectedImageUris.clear();

            if (data.getClipData() != null) {
                // Multiple images selected
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    selectedImageUris.add(imageUri);
                }
            } else if (data.getData() != null) {
                // Single image selected
                selectedImageUris.add(data.getData());
            }

            selectedImagesAdapter.notifyDataSetChanged();
            updateSubmitButtonState();

            Toast.makeText(this, selectedImageUris.size() + " image(s) selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSubmitButtonState() {
        submitButton.setEnabled(!selectedImageUris.isEmpty());
        submitButton.setAlpha(selectedImageUris.isEmpty() ? 0.5f : 1.0f);
    }

    private void submitAssignment() {
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);
        selectImagesButton.setEnabled(false);

        // Convert images to base64 and submit
        new Thread(() -> {
            try {
                List<String> base64Images = convertImagesToBase64();

                userRepository.submitAssignment(
                        assignment.getId(),
                        assignment.getTitle(),
                        base64Images,
                        assignment.getMaxScore(),
                        new UserRepository.AssignmentSubmissionCallback() {
                            @Override
                            public void onSuccess() {
                                runOnUiThread(() -> {
                                    hideProgressBar();
                                    Toast.makeText(AssignmentActivity.this, "Assignment submitted successfully!", Toast.LENGTH_LONG).show();

                                    // Update UI
                                    assignment.setStatus("Submitted");
                                    statusTextView.setText("Status: Submitted");

                                    // Hide submission UI
                                    selectImagesButton.setVisibility(View.GONE);
                                    submitButton.setVisibility(View.GONE);
                                    selectedImagesContainer.setVisibility(View.GONE);
                                    submissionTitleTextView.setText("Submitted - Awaiting Grade");
                                });
                            }

                            @Override
                            public void onFailure(String message) {
                                runOnUiThread(() -> {
                                    hideProgressBar();
                                    Toast.makeText(AssignmentActivity.this, "Failed to submit: " + message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                );
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error converting images", e);
                    Toast.makeText(AssignmentActivity.this, "Error processing images", Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                });
            }
        }).start();
    }

    private List<String> convertImagesToBase64() throws IOException {
        List<String> base64Images = new ArrayList<>();

        for (Uri imageUri : selectedImageUris) {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Compress image if needed
            if (bitmap.getWidth() > 1024 || bitmap.getHeight() > 1024) {
                bitmap = resizeBitmap(bitmap, 1024, 1024);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            String base64String = Base64.encodeToString(byteArray, Base64.DEFAULT);

            base64Images.add(base64String);

            inputStream.close();
            outputStream.close();
        }

        return base64Images;
    }

    private Bitmap resizeBitmap(Bitmap original, int maxWidth, int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();

        float ratio = Math.min((float) maxWidth / width, (float) maxHeight / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        submitButton.setEnabled(true);
        selectImagesButton.setEnabled(true);
    }

    // RecyclerView Adapter for selected images preview
    private class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder> {
        private List<Uri> imageUris;

        public SelectedImagesAdapter(List<Uri> imageUris) {
            this.imageUris = imageUris;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_selected_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Uri imageUri = imageUris.get(position);
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                holder.imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                Log.e(TAG, "Error loading image preview", e);
            }

            holder.removeButton.setOnClickListener(v -> {
                imageUris.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, imageUris.size());
                updateSubmitButtonState();
            });
        }

        @Override
        public int getItemCount() {
            return imageUris.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            Button removeButton;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.selectedImageView);
                removeButton = itemView.findViewById(R.id.removeImageButton);
            }
        }
    }
}