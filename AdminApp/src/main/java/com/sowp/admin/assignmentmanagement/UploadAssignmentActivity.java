package com.sowp.admin.assignmentmanagement;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.Course;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadAssignmentActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    // UI Components
    private ImageView btnBack;
    private Spinner spinnerCourse;
    private TextInputEditText etAssignmentTitle, etDescription, etScore, etPassingScore;
    private ChipGroup chipGroupCategories, chipGroupTags;
    private MaterialButton btnAddImage, btnUploadAssignment, btnAddTag;
    private LinearLayout imagesContainer;
    private EditText etNewTag;

    // Data structures
    private List<Course> coursesList;
    private Course selectedCourse;
    private List<String> selectedCategories;
    private List<String> selectedTags;
    private List<String> base64Images;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_assignment);

        // Initialize data structures
        coursesList = new ArrayList<>();
        selectedCategories = new ArrayList<>();
        selectedTags = new ArrayList<>();
        base64Images = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Load courses from Firebase
        loadCoursesFromFirebase();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        spinnerCourse = findViewById(R.id.spinnerCourse);
        etAssignmentTitle = findViewById(R.id.etAssignmentTitle);
        etDescription = findViewById(R.id.etDescription);
        etScore = findViewById(R.id.etScore);
        etPassingScore = findViewById(R.id.etPassingScore);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        chipGroupTags = findViewById(R.id.chipGroupTags);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnUploadAssignment = findViewById(R.id.btnUploadAssignment);
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

        // Upload assignment button
        btnUploadAssignment.setOnClickListener(v -> uploadAssignment());
    }

    private void loadCoursesFromFirebase() {
        showToast("Loading courses...");
        firestore.collection("Course")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        coursesList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            Course course = document.toObject(Course.class);
                            if (course != null) {
                                coursesList.add(course);
                            }
                        }
                        setupCourseSpinner();
                    } else {
                        showToast("Failed to load courses: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void setupCourseSpinner() {
        List<String> courseNames = new ArrayList<>();
        courseNames.add("Select a course");

        for (Course course : coursesList) {
            courseNames.add(course.getTitle() + " (" + course.getCourseCode() + ")");
        }

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, courseNames);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(courseAdapter);

        spinnerCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select a course" option
                    selectedCourse = coursesList.get(position - 1);
                    loadCategoriesForCourse();
                } else {
                    selectedCourse = null;
                    chipGroupCategories.removeAllViews();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCourse = null;
            }
        });
    }

    private void loadCategoriesForCourse() {
        if (selectedCourse == null || selectedCourse.getTopicCategories() == null) {
            return;
        }

        chipGroupCategories.removeAllViews();
        selectedCategories.clear();

        for (String category : selectedCourse.getTopicCategories()) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.chip_background);
            chip.setTextColor(getResources().getColor(R.color.chip_text_color));

            chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (isChecked) {
                    selectedCategories.add(category);
                } else {
                    selectedCategories.remove(category);
                }
            });

            chipGroupCategories.addView(chip);
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

    private void saveDraft() {
        if (!validateBasicAssignmentData()) {
            return;
        }

        showToast("Assignment saved as draft");
        // TODO: Implement actual save functionality for drafts
    }

    private void uploadAssignment() {
        if (!validateCompleteAssignmentData()) {
            return;
        }

        Assignment assignment = createAssignmentObject();
        uploadAssignmentToFirebase(assignment);
    }

    private boolean validateBasicAssignmentData() {
        if (selectedCourse == null) {
            showToast("Please select a course");
            return false;
        }
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
        return true;
    }

    private boolean validateCompleteAssignmentData() {
        if (!validateBasicAssignmentData()) {
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

            if (passingScore > totalScore) {
                showToast("Passing score cannot be greater than total score");
                return false;
            }
        } catch (NumberFormatException e) {
            showToast("Please enter valid numeric scores");
            return false;
        }

        return true;
    }

    private Assignment createAssignmentObject() {
        Assignment assignment = new Assignment();
        assignment.setCourseId(selectedCourse.getId());
        assignment.setTitle(etAssignmentTitle.getText().toString().trim());
        assignment.setDescription(etDescription.getText().toString().trim());
        assignment.setScore(Double.parseDouble(etScore.getText().toString().trim()));
        assignment.setPassingScore(Double.parseDouble(etPassingScore.getText().toString().trim()));
        assignment.setSemester(selectedCourse.getSemester());
        assignment.setCategories(selectedCategories);
        assignment.setTags(selectedTags);
        assignment.setBase64Images(base64Images);
        assignment.setCreatedAt(System.currentTimeMillis());
        assignment.setUpdatedAt(System.currentTimeMillis());

        return assignment;
    }

    private void uploadAssignmentToFirebase(Assignment assignment) {
        showToast("Uploading assignment...");

        // First get the next assignment ID for this course
        getNextAssignmentId(selectedCourse.getId(), nextAssignmentId -> {
            assignment.setId(nextAssignmentId);
            assignment.setOrderIndex(nextAssignmentId);

            // Create assignment data map
            Map<String, Object> assignmentData = new HashMap<>();
            assignmentData.put("id", assignment.getId());
            assignmentData.put("courseId", assignment.getCourseId());
            assignmentData.put("semester", assignment.getSemester());
            assignmentData.put("orderIndex", assignment.getOrderIndex());
            assignmentData.put("title", assignment.getTitle());
            assignmentData.put("description", assignment.getDescription());
            assignmentData.put("score", assignment.getScore());
            assignmentData.put("passingScore", assignment.getPassingScore());
            assignmentData.put("base64Images", assignment.getBase64Images());
            assignmentData.put("tags", assignment.getTags());
            assignmentData.put("categories", assignment.getCategories());
            assignmentData.put("createdAt", assignment.getCreatedAt());
            assignmentData.put("updatedAt", assignment.getUpdatedAt());

            // Upload assignment to the course's Assignments subcollection
            firestore.collection("Course")
                    .document(String.valueOf(selectedCourse.getId()))
                    .collection("Assignments")
                    .document(String.valueOf(nextAssignmentId))
                    .set(assignmentData)
                    .addOnSuccessListener(aVoid -> {
                        showToast("Assignment uploaded successfully!");
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        showToast("Failed to upload assignment: " + e.getMessage());
                    });
        });
    }

    private void getNextAssignmentId(int courseId, OnAssignmentIdCallback callback) {
        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Assignments")
                .get()
                .addOnCompleteListener(task -> {
                    int nextId = 1;
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();
                        nextId = querySnapshot.size() + 1;
                    }
                    callback.onCallback(nextId);
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Callback interface for getting next assignment ID
    private interface OnAssignmentIdCallback {
        void onCallback(int nextId);
    }
}