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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // Firebase
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    public static Intent createIntent(android.content.Context context, Assignment assignment) {
        Intent intent = new Intent(context, AssignmentActivity.class);
        intent.putExtra(EXTRA_ASSIGNMENT, assignment);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment);

        initializeFirebase();
        initializeViews();
        getAssignmentFromIntent();
        setupRecyclerView();
        setupUI();
        setupClickListeners();
    }

    private void initializeFirebase() {
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
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

    private void setupRecyclerView() {
        selectedImagesAdapter = new SelectedImagesAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        selectedImagesRecyclerView.setLayoutManager(layoutManager);
        selectedImagesRecyclerView.setAdapter(selectedImagesAdapter);
    }

    private void setupUI() {
        if (assignment == null) return;

        // Set assignment details
        titleTextView.setText(assignment.getTitle());
        descriptionTextView.setText(assignment.getDescription());
        dueDateTextView.setText("Due: " + assignment.getDueDate());
        statusTextView.setText("Status: " + assignment.getStatus());
        maxScoreTextView.setText("Max Score: " + assignment.getMaxScore());

        // Load assignment image from base64 if available
        loadAssignmentImage();

        // Initialize UI state
        updateUIState();

        // Handle submitted assignments
        handleSubmittedAssignment();
    }

    private void loadAssignmentImage() {
        if (assignment.getFileUrl() != null && !assignment.getFileUrl().isEmpty()) {
            try {
                String cleanBase64 = assignment.getFileUrl();
                if (assignment.getFileUrl().contains(",")) {
                    cleanBase64 = assignment.getFileUrl().split(",")[1];
                }

                byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                assignmentImageView.setImageBitmap(bitmap);
                assignmentImageView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error loading assignment image", e);
                assignmentImageView.setVisibility(View.GONE);
            }
        } else {
            assignmentImageView.setVisibility(View.GONE);
        }
    }

    private void handleSubmittedAssignment() {
        if ("Submitted".equals(assignment.getStatus())) {
            selectImagesButton.setVisibility(View.GONE);
            submitButton.setVisibility(View.GONE);
            selectedImagesContainer.setVisibility(View.GONE);
            submissionTitleTextView.setText("Score: " + assignment.getEarnedScore());
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
            handleImageSelection(data);
        }
    }

    private void handleImageSelection(Intent data) {
        selectedImageUris.clear();

        if (data.getClipData() != null) {
            // Multiple images selected
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                selectedImageUris.add(imageUri);
            }
        } else if (data.getData() != null) {
            // Single image selected
            selectedImageUris.add(data.getData());
        }

        // Update UI
        selectedImagesAdapter.notifyDataSetChanged();
        updateUIState();

        String message = selectedImageUris.size() + " image(s) selected";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateUIState() {
        boolean hasImages = !selectedImageUris.isEmpty();

        // Update submit button
        submitButton.setEnabled(hasImages);
        submitButton.setAlpha(hasImages ? 1.0f : 0.5f);

        // Show/hide selected images container
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

                // Resize if needed
                bitmap = resizeBitmapIfNeeded(bitmap);

                // Convert to base64
                String base64String = bitmapToBase64(bitmap);
                base64Images.add(base64String);

                // Clean up
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
        String email = firebaseAuth.getCurrentUser().getEmail();
        if (email == null) {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }

        Map<String, Object> submissionData = createSubmissionData(base64Images);

        // Update user assignments array
        firestore.collection("User")
                .document(email)
                .update("assignments", FieldValue.arrayUnion(assignment.getId()))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User assignments updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating user assignments", e));

        // Submit assignment progress
        firestore.collection("User/" + email + "/AssignmentProgress")
                .document(assignment.getId())
                .set(submissionData)
                .addOnSuccessListener(aVoid -> handleSubmissionSuccess())
                .addOnFailureListener(this::handleSubmissionFailure);
    }

    private Map<String, Object> createSubmissionData(List<String> base64Images) {
        Map<String, Object> submissionData = new HashMap<>();
        submissionData.put("assignmentId", assignment.getId());
        submissionData.put("submittedImages", base64Images);
        submissionData.put("submissionTimestamp", System.currentTimeMillis());
        submissionData.put("score", 0);
        submissionData.put("maxScore", assignment.getMaxScore());
        submissionData.put("checked", false);
        submissionData.put("status", "Submitted");
        return submissionData;
    }

    private void handleSubmissionSuccess() {
        showLoading(false);
        Toast.makeText(this, "Assignment submitted successfully!", Toast.LENGTH_LONG).show();

        // Update UI
        assignment.setStatus("Submitted");
        statusTextView.setText("Status: Submitted");

        // Hide submission controls
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
                    Toast.makeText(AssignmentActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}