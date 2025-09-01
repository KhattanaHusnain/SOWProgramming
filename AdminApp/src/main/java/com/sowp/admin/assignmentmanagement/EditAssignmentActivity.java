package com.sowp.admin.assignmentmanagement;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.Course;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditAssignmentActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    // UI Components
    private ImageView btnBack;
    private TextInputEditText etCourseDisplay, etAssignmentTitle, etDescription, etScore, etPassingScore;
    private ChipGroup chipGroupCategories, chipGroupTags;
    private MaterialButton btnAddImage, btnUpdateAssignment;
    private LinearLayout imagesContainer;
    private EditText etNewTag;
    private Button btnAddTag;

    // Data structures
    private String courseId;
    private String assignmentId;
    private Course currentCourse;
    private Assignment currentAssignment;
    private List<String> availableCategories;
    private List<String> selectedCategories;
    private List<String> selectedTags;
    private List<String> base64Images;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_assignment);

        // Get intent data
        courseId = getIntent().getStringExtra("COURSE_ID");
        assignmentId = getIntent().getStringExtra("ASSIGNMENT_ID");

        if (courseId == null || assignmentId == null) {
            showToast("Error: Missing assignment information");
            finish();
            return;
        }

        // Initialize data structures
        availableCategories = new ArrayList<>();
        selectedCategories = new ArrayList<>();
        selectedTags = new ArrayList<>();
        base64Images = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Load assignment data
        loadAssignmentData();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        etCourseDisplay = findViewById(R.id.etCourseDisplay);
        etAssignmentTitle = findViewById(R.id.etAssignmentTitle);
        etDescription = findViewById(R.id.etDescription);
        etScore = findViewById(R.id.etScore);
        etPassingScore = findViewById(R.id.etPassingScore);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        chipGroupTags = findViewById(R.id.chipGroupTags);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnUpdateAssignment = findViewById(R.id.btnUpdateAssignment);
        imagesContainer = findViewById(R.id.imagesContainer);
        etNewTag = findViewById(R.id.etNewTag);
        btnAddTag = findViewById(R.id.btnAddTag);
    }

    private void setClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Add image button
        btnAddImage.setOnClickListener(v -> selectImage());

        // Add tag button
        btnAddTag.setOnClickListener(v -> addCustomTag());

        // Update assignment button
        btnUpdateAssignment.setOnClickListener(v -> updateAssignment());
    }

    private void loadAssignmentData() {
        showToast("Loading assignment data...");

        // First load the course data
        firestore.collection("Course")
                .document(courseId)
                .get()
                .addOnSuccessListener(courseDoc -> {
                    if (courseDoc.exists()) {
                        currentCourse = courseDoc.toObject(Course.class);
                        if (currentCourse != null) {
                            // Display course info
                            etCourseDisplay.setText(currentCourse.getTitle() + " (" + currentCourse.getCourseCode() + ")");
                            availableCategories = currentCourse.getTopicCategories();

                            // Now load the assignment
                            loadAssignmentFromFirebase();
                        }
                    } else {
                        showToast("Course not found");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load course: " + e.getMessage());
                    finish();
                });
    }

    private void loadAssignmentFromFirebase() {
        firestore.collection("Course")
                .document(courseId)
                .collection("Assignments")
                .document(assignmentId)
                .get()
                .addOnSuccessListener(assignmentDoc -> {
                    if (assignmentDoc.exists()) {
                        currentAssignment = assignmentDoc.toObject(Assignment.class);
                        if (currentAssignment != null) {
                            populateAssignmentData();
                        }
                    } else {
                        showToast("Assignment not found");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load assignment: " + e.getMessage());
                    finish();
                });
    }

    private void populateAssignmentData() {
        // Populate basic fields
        etAssignmentTitle.setText(currentAssignment.getTitle());
        etDescription.setText(currentAssignment.getDescription());
        etScore.setText(String.valueOf(currentAssignment.getScore()));
        etPassingScore.setText(String.valueOf(currentAssignment.getPassingScore()));

        // Populate categories
        if (currentAssignment.getCategories() != null) {
            selectedCategories.addAll(currentAssignment.getCategories());
        }
        setupCategoriesChips();

        // Populate tags
        if (currentAssignment.getTags() != null) {
            selectedTags.addAll(currentAssignment.getTags());
            setupTagsChips();
        }

        // Populate images
        if (currentAssignment.getBase64Images() != null) {
            base64Images.addAll(currentAssignment.getBase64Images());
            setupImagePreviews();
        }
    }

    private void setupCategoriesChips() {
        chipGroupCategories.removeAllViews();

        if (availableCategories != null) {
            for (String category : availableCategories) {
                Chip chip = new Chip(this);
                chip.setText(category);
                chip.setCheckable(true);
                chip.setChecked(selectedCategories.contains(category));
                chip.setChipBackgroundColorResource(R.color.chip_background);
                chip.setTextColor(getResources().getColor(R.color.chip_text_color));

                chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                    if (isChecked) {
                        if (!selectedCategories.contains(category)) {
                            selectedCategories.add(category);
                        }
                    } else {
                        selectedCategories.remove(category);
                    }
                });

                chipGroupCategories.addView(chip);
            }
        }
    }

    private void setupTagsChips() {
        chipGroupTags.removeAllViews();
        for (String tag : selectedTags) {
            addTagChip(tag);
        }
    }

    private void setupImagePreviews() {
        imagesContainer.removeAllViews();
        for (String base64Image : base64Images) {
            try {
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                addImagePreview(bitmap);
            } catch (Exception e) {
                showToast("Error loading image: " + e.getMessage());
            }
        }
    }

    private void addCustomTag() {
        String tagText = etNewTag.getText().toString().trim();
        if (tagText.isEmpty()) {
            showToast("Please enter a tag");
            return;
        }

        if (selectedTags.contains(tagText)) {
            showToast("Tag already added");
            return;
        }

        selectedTags.add(tagText);
        addTagChip(tagText);
        etNewTag.setText("");
    }

    private void addTagChip(String tagText) {
        Chip chip = new Chip(this);
        chip.setText(tagText);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.chip_background);
        chip.setTextColor(getResources().getColor(R.color.chip_text_color));

        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
            selectedTags.remove(tagText);
        });

        chipGroupTags.addView(chip);
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                // Multiple images selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    convertImageToBase64(imageUri);
                }
            } else if (data.getData() != null) {
                // Single image selected
                Uri imageUri = data.getData();
                convertImageToBase64(imageUri);
            }
        }
    }

    private void convertImageToBase64(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64String = Base64.encodeToString(byteArray, Base64.DEFAULT);

            base64Images.add(base64String);
            addImagePreview(bitmap);
            showToast("Image added successfully");

        } catch (IOException e) {
            showToast("Failed to process image: " + e.getMessage());
        }
    }

    private void addImagePreview(Bitmap bitmap) {
        View imagePreview = LayoutInflater.from(this).inflate(R.layout.item_image_preview, null);

        ImageView imgPreview = imagePreview.findViewById(R.id.imgPreview);
        Button btnRemoveImage = imagePreview.findViewById(R.id.btnRemoveImage);

        imgPreview.setImageBitmap(bitmap);

        btnRemoveImage.setOnClickListener(v -> {
            int index = imagesContainer.indexOfChild(imagePreview);
            if (index >= 0 && index < base64Images.size()) {
                base64Images.remove(index);
                imagesContainer.removeView(imagePreview);
                showToast("Image removed");
            }
        });

        imagesContainer.addView(imagePreview);
    }

    private void updateAssignment() {
        if (!validateAssignmentData()) {
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Update Assignment")
                .setMessage("Are you sure you want to update this assignment?")
                .setPositiveButton("Update", (dialog, which) -> performUpdate())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performUpdate() {
        showToast("Updating assignment...");

        // Create updated assignment data
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("title", etAssignmentTitle.getText().toString().trim());
        updatedData.put("description", etDescription.getText().toString().trim());
        updatedData.put("score", Double.parseDouble(etScore.getText().toString().trim()));
        updatedData.put("passingScore", Double.parseDouble(etPassingScore.getText().toString().trim()));
        updatedData.put("categories", selectedCategories);
        updatedData.put("tags", selectedTags);
        updatedData.put("base64Images", base64Images);
        updatedData.put("updatedAt", System.currentTimeMillis());

        // Update in Firebase
        firestore.collection("Course")
                .document(courseId)
                .collection("Assignments")
                .document(assignmentId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    showToast("Assignment updated successfully!");
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to update assignment: " + e.getMessage());
                });
    }

    private boolean validateAssignmentData() {
        if (etAssignmentTitle.getText().toString().trim().isEmpty()) {
            showToast("Assignment title is required");
            etAssignmentTitle.requestFocus();
            return false;
        }

        if (etDescription.getText().toString().trim().isEmpty()) {
            showToast("Description is required");
            etDescription.requestFocus();
            return false;
        }

        if (etScore.getText().toString().trim().isEmpty()) {
            showToast("Total score is required");
            etScore.requestFocus();
            return false;
        }

        if (etPassingScore.getText().toString().trim().isEmpty()) {
            showToast("Passing score is required");
            etPassingScore.requestFocus();
            return false;
        }

        try {
            double totalScore = Double.parseDouble(etScore.getText().toString().trim());
            double passingScore = Double.parseDouble(etPassingScore.getText().toString().trim());

            if (totalScore <= 0) {
                showToast("Total score must be greater than 0");
                etScore.requestFocus();
                return false;
            }

            if (passingScore <= 0) {
                showToast("Passing score must be greater than 0");
                etPassingScore.requestFocus();
                return false;
            }

            if (passingScore > totalScore) {
                showToast("Passing score cannot be greater than total score");
                etPassingScore.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showToast("Please enter valid numeric scores");
            return false;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle("Cancel Editing")
                    .setMessage("Are you sure you want to cancel? Any unsaved changes will be lost.")
                    .setPositiveButton("Yes", (dialog, which) -> finish())
                    .setNegativeButton("No", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        if (currentAssignment == null) return false;

        // Check if any field has changed
        String currentTitle = etAssignmentTitle.getText().toString().trim();
        String currentDescription = etDescription.getText().toString().trim();

        try {
            double currentScore = Double.parseDouble(etScore.getText().toString().trim());
            double currentPassingScore = Double.parseDouble(etPassingScore.getText().toString().trim());

            return !currentTitle.equals(currentAssignment.getTitle()) ||
                    !currentDescription.equals(currentAssignment.getDescription()) ||
                    currentScore != currentAssignment.getScore() ||
                    currentPassingScore != currentAssignment.getPassingScore() ||
                    !selectedCategories.equals(currentAssignment.getCategories()) ||
                    !selectedTags.equals(currentAssignment.getTags()) ||
                    !base64Images.equals(currentAssignment.getBase64Images());
        } catch (NumberFormatException e) {
            return true; // If there's invalid input, consider it as changes
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}