package com.sowp.admin.assignmentmanagement;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sowp.admin.NotificationHelper;
import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.Course;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadAssignmentActivity extends AppCompatActivity {

    private static final String TAG = "UploadAssignment";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI Components
    private ImageView btnBack;
    private AutoCompleteTextView spinnerCourse;
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

        initializeComponents();
        setupViews();
        loadCoursesFromFirebase();
    }

    private void initializeComponents() {
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();

        // Initialize data structures
        coursesList = new ArrayList<>();
        selectedCategories = new ArrayList<>();
        selectedTags = new ArrayList<>();
        base64Images = new ArrayList<>();

        // Initialize views
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

    private void setupViews() {
        btnBack.setOnClickListener(v -> finish());
        btnAddImage.setOnClickListener(v -> openImagePicker());
        btnAddTag.setOnClickListener(v -> addCustomTag());
        btnUploadAssignment.setOnClickListener(v -> handleUploadAssignment());

        setupCourseSelection();
    }

    private void setupCourseSelection() {
        spinnerCourse.setHint("Select a course");

        spinnerCourse.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < coursesList.size()) {
                selectedCourse = coursesList.get(position);
                loadCategoriesForSelectedCourse();
            }
        });

        spinnerCourse.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateCourseSelection();
            }
        });

        spinnerCourse.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    selectedCourse = null;
                    clearCategories();
                }
            }
        });
    }

    private void validateCourseSelection() {
        String inputText = spinnerCourse.getText().toString().trim();

        if (inputText.isEmpty()) {
            selectedCourse = null;
            clearCategories();
            return;
        }

        // Find matching course
        for (Course course : coursesList) {
            String courseDisplayName = course.getTitle() + " (" + course.getCourseCode() + ")";
            if (courseDisplayName.equals(inputText)) {
                if (selectedCourse == null || !(selectedCourse.getId() == course.getId())) {
                    selectedCourse = course;
                    loadCategoriesForSelectedCourse();
                }
                return;
            }
        }

        // No match found - clear selection
        selectedCourse = null;
        clearCategories();
    }

    private void loadCoursesFromFirebase() {
        showToast("Loading courses...");

        firestore.collection("Course")
                .get()
                .addOnSuccessListener(this::handleCoursesLoaded)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load courses", e);
                    showToast("Failed to load courses: " + e.getMessage());
                });
    }

    private void handleCoursesLoaded(QuerySnapshot querySnapshot) {
        coursesList.clear();
        List<String> courseDisplayNames = new ArrayList<>();

        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
            Course course = document.toObject(Course.class);
            if (course != null) {
                coursesList.add(course);
                courseDisplayNames.add(course.getTitle() + " (" + course.getCourseCode() + ")");
            }
        }

        setupCourseAdapter(courseDisplayNames);
        showToast("Courses loaded successfully");
    }

    private void setupCourseAdapter(List<String> courseDisplayNames) {
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                courseDisplayNames
        );
        spinnerCourse.setAdapter(courseAdapter);
        spinnerCourse.setThreshold(1);
    }

    private void loadCategoriesForSelectedCourse() {
        if (selectedCourse == null) {
            clearCategories();
            return;
        }

        clearCategories();

        List<String> categories = selectedCourse.getTopicCategories();
        if (categories == null || categories.isEmpty()) {
            return;
        }

        for (String category : categories) {
            addCategoryChip(category);
        }
    }

    private void addCategoryChip(String category) {
        Chip chip = new Chip(this);
        chip.setText(category);
        chip.setCheckable(true);
        chip.setChipBackgroundColorResource(R.color.chip_background);
        chip.setTextColor(getResources().getColor(R.color.chip_text_color, null));

        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
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

    private void clearCategories() {
        chipGroupCategories.removeAllViews();
        selectedCategories.clear();
    }

    private void addCustomTag() {
        String tagText = etNewTag.getText().toString().trim();

        if (tagText.isEmpty()) {
            showToast("Please enter a tag");
            etNewTag.requestFocus();
            return;
        }

        if (selectedTags.contains(tagText)) {
            showToast("Tag already exists");
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
        chip.setTextColor(getResources().getColor(R.color.chip_text_color, null));

        chip.setOnCloseIconClickListener(v -> {
            chipGroupTags.removeView(chip);
            selectedTags.remove(tagText);
        });

        chipGroupTags.addView(chip);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            handleImageSelection(data);
        }
    }

    private void handleImageSelection(Intent data) {
        if (data.getClipData() != null) {
            // Multiple images selected
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                processSelectedImage(imageUri);
            }
        } else if (data.getData() != null) {
            // Single image selected
            Uri imageUri = data.getData();
            processSelectedImage(imageUri);
        }
    }

    private void processSelectedImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            String base64String = convertBitmapToBase64(bitmap);

            base64Images.add(base64String);
            addImagePreview(bitmap);

            showToast("Image added successfully");

        } catch (IOException e) {
            Log.e(TAG, "Failed to process image", e);
            showToast("Failed to process image: " + e.getMessage());
        }
    }

    private String convertBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void addImagePreview(Bitmap bitmap) {
        View previewView = LayoutInflater.from(this)
                .inflate(R.layout.item_image_preview, imagesContainer, false);

        ImageView imgPreview = previewView.findViewById(R.id.imgPreview);
        Button btnRemove = previewView.findViewById(R.id.btnRemoveImage);

        imgPreview.setImageBitmap(bitmap);
        btnRemove.setOnClickListener(v -> removeImagePreview(previewView));

        imagesContainer.addView(previewView);
    }

    private void removeImagePreview(View previewView) {
        int index = imagesContainer.indexOfChild(previewView);
        if (index >= 0 && index < base64Images.size()) {
            base64Images.remove(index);
            imagesContainer.removeView(previewView);
            showToast("Image removed");
        }
    }

    private void handleUploadAssignment() {
        if (!validateAllFields()) {
            return;
        }

        Assignment assignment = createAssignmentObject();
        uploadToFirebase(assignment);
    }

    private boolean validateAllFields() {
        // Validate course selection
        if (selectedCourse == null) {
            showToast("Please select a valid course");
            spinnerCourse.requestFocus();
            return false;
        }

        // Validate title
        if (etAssignmentTitle.getText().toString().trim().isEmpty()) {
            showToast("Assignment title is required");
            etAssignmentTitle.requestFocus();
            return false;
        }

        // Validate description
        if (etDescription.getText().toString().trim().isEmpty()) {
            showToast("Description is required");
            etDescription.requestFocus();
            return false;
        }

        return validateScores();
    }

    private boolean validateScores() {
        String scoreText = etScore.getText().toString().trim();
        String passingScoreText = etPassingScore.getText().toString().trim();

        if (scoreText.isEmpty()) {
            showToast("Total score is required");
            etScore.requestFocus();
            return false;
        }

        if (passingScoreText.isEmpty()) {
            showToast("Passing score is required");
            etPassingScore.requestFocus();
            return false;
        }

        try {
            double totalScore = Double.parseDouble(scoreText);
            double passingScore = Double.parseDouble(passingScoreText);

            if (totalScore <= 0) {
                showToast("Total score must be greater than 0");
                etScore.requestFocus();
                return false;
            }

            if (passingScore < 0) {
                showToast("Passing score cannot be negative");
                etPassingScore.requestFocus();
                return false;
            }

            if (passingScore > totalScore) {
                showToast("Passing score cannot exceed total score");
                etPassingScore.requestFocus();
                return false;
            }

        } catch (NumberFormatException e) {
            showToast("Please enter valid numeric values for scores");
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
        assignment.setCategories(new ArrayList<>(selectedCategories));
        assignment.setTags(new ArrayList<>(selectedTags));
        assignment.setBase64Images(new ArrayList<>(base64Images));

        long currentTime = System.currentTimeMillis();
        assignment.setCreatedAt(currentTime);
        assignment.setUpdatedAt(currentTime);

        return assignment;
    }

    private void uploadToFirebase(Assignment assignment) {
        showToast("Uploading assignment...");

        getNextAssignmentId(selectedCourse.getId(), nextId -> {
            assignment.setId(nextId);
            assignment.setOrderIndex(nextId);

            saveAssignmentToFirestore(assignment);
        });
    }

    private void getNextAssignmentId(Integer courseId, AssignmentIdCallback callback) {
        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Assignments")
                .orderBy("id", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int nextId = 1;

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot lastAssignment = querySnapshot.getDocuments().get(0);
                        Object idObj = lastAssignment.get("id");

                        if (idObj instanceof Number) {
                            nextId = ((Number) idObj).intValue() + 1;
                        }
                    }

                    callback.onIdGenerated(nextId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get last assignment ID", e);
                    getAssignmentCountBasedId(courseId, callback);
                });
    }

    private void getAssignmentCountBasedId(Integer courseId, AssignmentIdCallback callback) {
        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Assignments")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int nextId = querySnapshot.size() + 1;
                    callback.onIdGenerated(nextId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get assignment count, using timestamp-based ID", e);
                    int fallbackId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                    callback.onIdGenerated(fallbackId);
                });
    }

    private void saveAssignmentToFirestore(Assignment assignment) {
        Map<String, Object> assignmentData = createAssignmentDataMap(assignment);

        String courseDocId = String.valueOf(selectedCourse.getId());
        String assignmentDocId = String.valueOf(assignment.getId());

        firestore.collection("Course")
                .document(courseDocId)
                .collection("Assignments")
                .document(assignmentDocId)
                .set(assignmentData)
                .addOnSuccessListener(aVoid -> {
                    // Increment assignments count after successful upload
                    incrementAssignmentsCount();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload assignment", e);
                    showToast("Failed to upload assignment: " + e.getMessage());
                });
    }

    private void incrementAssignmentsCount() {
        firestore.collection("Course")
                .document(String.valueOf(selectedCourse.getId()))
                .update("noOfAssignments", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    NotificationHelper.addNotification("New assignment added in Course: " + selectedCourse.getTitle());
                    showToast("Assignment uploaded successfully!");
                    finish();
                })
                .addOnFailureListener(e -> {
                    showToast("Assignment uploaded but failed to update assignment count: " + e.getMessage());
                    Log.e(TAG, "Failed to increment assignments count", e);
                    finish();
                });
    }

    private Map<String, Object> createAssignmentDataMap(Assignment assignment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", assignment.getId());
        data.put("courseId", assignment.getCourseId());
        data.put("semester", assignment.getSemester());
        data.put("orderIndex", assignment.getOrderIndex());
        data.put("title", assignment.getTitle());
        data.put("description", assignment.getDescription());
        data.put("score", assignment.getScore());
        data.put("passingScore", assignment.getPassingScore());
        data.put("base64Images", assignment.getBase64Images());
        data.put("tags", assignment.getTags());
        data.put("categories", assignment.getCategories());
        data.put("createdAt", assignment.getCreatedAt());
        data.put("updatedAt", assignment.getUpdatedAt());
        return data;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private interface AssignmentIdCallback {
        void onIdGenerated(int id);
    }
}