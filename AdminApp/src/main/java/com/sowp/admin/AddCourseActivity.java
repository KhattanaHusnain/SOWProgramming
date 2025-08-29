package com.sowp.admin;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddCourseActivity extends AppCompatActivity {

    // Existing fields
    private TextInputEditText etId, etTitle, etShortTitle, etCategory, etPrimaryCategory,
            etCategoryArray, etDuration, etTags, etDescription, etOutline, etLectures,
            etMembers, etCourseCode, etPreRequisite, etFollowUp, etCreditHours,
            etInstructor, etLanguage, etNoOfQuizzes, etNoOfAssignments,
            etDepartmentArray;

    private SwitchMaterial switchIsPublic, switchIsLab, switchIsComputer, switchIsPaid;
    private MaterialButton btnAddCourse, btnSelectImage;
    private AutoCompleteTextView etSemester, etLevelDropdown;
    private ImageView ivCourseImage;
    private ProgressBar progressBar;

    // Dropdown options
    private String[] semesterOptions = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "Graduated"};
    private String[] levelOptions = {"Beginner", "Intermediate", "Advanced", "Expert"};

    private FirebaseFirestore db;
    private String base64Image = "";

    // Activity result launcher for image selection
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize image picker launcher
        initImagePickerLauncher();

        // Initialize views
        initViews();

        // Set click listeners
        btnAddCourse.setOnClickListener(v -> checkCourseIdAndAdd());
        btnSelectImage.setOnClickListener(v -> selectImageFromGallery());
    }

    private void initImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                // Display selected image
                                ivCourseImage.setImageURI(imageUri);
                                ivCourseImage.setVisibility(View.VISIBLE);

                                // Convert to base64
                                convertImageToBase64(imageUri);

                            } catch (Exception e) {
                                Toast.makeText(this, "Error loading image: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize bitmap to reduce size (optional)
            bitmap = resizeBitmap(bitmap, 800, 600);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            inputStream.close();
            outputStream.close();

            Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Error converting image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float aspectRatio = (float) width / height;

        if (width > height) {
            width = maxWidth;
            height = Math.round(width / aspectRatio);
        } else {
            height = maxHeight;
            width = Math.round(height * aspectRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void initViews() {
        // Basic info fields
        etId = findViewById(R.id.etId);
        etTitle = findViewById(R.id.etTitle);
        etShortTitle = findViewById(R.id.etShortTitle);
        etCategory = findViewById(R.id.etCategory);
        etPrimaryCategory = findViewById(R.id.etPrimaryCategory);
        etCategoryArray = findViewById(R.id.etCategoryArray);
        etDuration = findViewById(R.id.etDuration);
        etTags = findViewById(R.id.etTags);
        etDescription = findViewById(R.id.etDescription);
        etOutline = findViewById(R.id.etOutline);
        etLectures = findViewById(R.id.etLectures);
        etMembers = findViewById(R.id.etMembers);

        // New fields
        etCourseCode = findViewById(R.id.etCourseCode);
        etPreRequisite = findViewById(R.id.etPreRequisite);
        etFollowUp = findViewById(R.id.etFollowUp);
        etCreditHours = findViewById(R.id.etCreditHours);
        etInstructor = findViewById(R.id.etInstructor);
        etLanguage = findViewById(R.id.etLanguage);
        etNoOfQuizzes = findViewById(R.id.etNoOfQuizzes);
        etNoOfAssignments = findViewById(R.id.etNoOfAssignments);
        etDepartmentArray = findViewById(R.id.etDepartmentArray);

        // Switches
        switchIsPublic = findViewById(R.id.switchIsPublic);
        switchIsLab = findViewById(R.id.switchIsLab);
        switchIsComputer = findViewById(R.id.switchIsComputer);
        switchIsPaid = findViewById(R.id.switchIsPaid);

        // Buttons and other views
        btnAddCourse = findViewById(R.id.btnAddCourse);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivCourseImage = findViewById(R.id.ivCourseImage);
        progressBar = findViewById(R.id.progressBar);
        etSemester = findViewById(R.id.etSemester);
        etLevelDropdown = findViewById(R.id.etLevelDropdown);

        // Setup dropdowns
        setupDropdowns();
    }

    private void setupDropdowns() {
        // Semester dropdown
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, semesterOptions);
        etSemester.setAdapter(semesterAdapter);

        // Level dropdown
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, levelOptions);
        etLevelDropdown.setAdapter(levelAdapter);
    }

    private void checkCourseIdAndAdd() {
        String courseId = getTextFromEditText(etId);

        if (TextUtils.isEmpty(courseId)) {
            etId.setError("Course ID is required");
            etId.requestFocus();
            return;
        }

        showProgressBar(true);

        // Check if course ID already exists
        db.collection("Course")
                .document(courseId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            showProgressBar(false);
                            Toast.makeText(this, "Course ID already exists! Please use a different ID.",
                                    Toast.LENGTH_LONG).show();
                            etId.setError("Course ID already exists");
                            etId.requestFocus();
                        } else {
                            // Course ID doesn't exist, proceed with adding
                            addCourseToFirestore();
                        }
                    } else {
                        showProgressBar(false);
                        Toast.makeText(this, "Error checking course ID: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void addCourseToFirestore() {
        if (!validateInputs()) {
            showProgressBar(false);
            return;
        }

        long now = System.currentTimeMillis();
        String docId = getTextFromEditText(etId);

        Map<String, Object> course = new HashMap<>();

        // Basic fields
        course.put("id", Integer.parseInt(docId));
        course.put("title", getTextFromEditText(etTitle));
        course.put("shortTitle", getTextFromEditText(etShortTitle));
        course.put("category", getTextFromEditText(etCategory));
        course.put("primaryCategory", getTextFromEditText(etPrimaryCategory));
        course.put("categoryArray", getListFromEditText(etCategoryArray));
        course.put("duration", getTextFromEditText(etDuration));
        course.put("illustration", base64Image); // Base64 image instead of URL
        course.put("description", getTextFromEditText(etDescription));
        course.put("outline", getTextFromEditText(etOutline));
        course.put("lectures", getIntegerFromEditText(etLectures));
        course.put("members", getIntegerFromEditText(etMembers));
        course.put("semester", etSemester.getText().toString());
        course.put("tags", getListFromEditText(etTags));

        // New fields
        course.put("courseCode", getTextFromEditText(etCourseCode));
        course.put("preRequisite", getListFromEditText(etPreRequisite));
        course.put("followUp", getListFromEditText(etFollowUp));
        course.put("creditHours", getIntegerFromEditText(etCreditHours));
        course.put("instructor", getTextFromEditText(etInstructor));
        course.put("language", getTextFromEditText(etLanguage));
        course.put("noOfQuizzes", getIntegerFromEditText(etNoOfQuizzes));
        course.put("noOfAssignments", getIntegerFromEditText(etNoOfAssignments));
        course.put("level", etLevelDropdown.getText().toString());
        course.put("departmentArray", getListFromEditText(etDepartmentArray));

        // Boolean fields
        course.put("isPublic", switchIsPublic.isChecked());
        course.put("isLab", switchIsLab.isChecked());
        course.put("isComputer", switchIsComputer.isChecked());
        course.put("isPaid", switchIsPaid.isChecked());
        course.put("completed", false);

        // Timestamps
        course.put("createdAt", now);
        course.put("updatedAt", now);

        // Default values
        course.put("avgCourseRating", 0.0);

        db.collection("Course")
                .document(docId)
                .set(course)
                .addOnCompleteListener(task -> {
                    showProgressBar(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Course added successfully!", Toast.LENGTH_LONG).show();
                        clearForm();
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this, "Error adding course: " + (e != null ? e.getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                        android.util.Log.e("AddCourse", "Firestore write failed", e);
                    }
                });
    }

    private boolean validateInputs() {
        // Required field validations
        if (TextUtils.isEmpty(getTextFromEditText(etId))) {
            etId.setError("Course ID is required");
            etId.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etTitle))) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etCourseCode))) {
            etCourseCode.setError("Course Code is required");
            etCourseCode.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etPrimaryCategory))) {
            etPrimaryCategory.setError("Primary Category is required");
            etPrimaryCategory.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etDuration))) {
            etDuration.setError("Duration is required");
            etDuration.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etSemester.getText().toString())) {
            etSemester.setError("Semester is required");
            etSemester.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(base64Image)) {
            Toast.makeText(this, "Please select a course image", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etDescription))) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etInstructor))) {
            etInstructor.setError("Instructor name is required");
            etInstructor.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etCreditHours))) {
            etCreditHours.setError("Credit hours is required");
            etCreditHours.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(etLevelDropdown.getText().toString())) {
            etLevelDropdown.setError("Level is required");
            etLevelDropdown.requestFocus();
            return false;
        }

        // Validate numeric fields
        try {
            Integer.parseInt(getTextFromEditText(etId));
        } catch (NumberFormatException e) {
            etId.setError("Please enter a valid number");
            etId.requestFocus();
            return false;
        }

        String lecturesText = getTextFromEditText(etLectures);
        if (!TextUtils.isEmpty(lecturesText)) {
            try {
                Integer.parseInt(lecturesText);
            } catch (NumberFormatException e) {
                etLectures.setError("Please enter a valid number");
                etLectures.requestFocus();
                return false;
            }
        }

        String membersText = getTextFromEditText(etMembers);
        if (!TextUtils.isEmpty(membersText)) {
            try {
                Integer.parseInt(membersText);
            } catch (NumberFormatException e) {
                etMembers.setError("Please enter a valid number");
                etMembers.requestFocus();
                return false;
            }
        }

        try {
            Integer.parseInt(getTextFromEditText(etCreditHours));
        } catch (NumberFormatException e) {
            etCreditHours.setError("Please enter a valid number");
            etCreditHours.requestFocus();
            return false;
        }

        return true;
    }

    private String getTextFromEditText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private int getIntegerFromEditText(TextInputEditText editText) {
        String text = getTextFromEditText(editText);
        return TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text);
    }

    private List<String> getListFromEditText(TextInputEditText editText) {
        return Arrays.stream(getTextFromEditText(editText).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAddCourse.setEnabled(!show);
    }

    private void clearForm() {
        // Clear all text fields
        etId.setText("");
        etTitle.setText("");
        etShortTitle.setText("");
        etCategory.setText("");
        etPrimaryCategory.setText("");
        etCategoryArray.setText("");
        etTags.setText("");
        etSemester.setText("");
        etDuration.setText("");
        etDescription.setText("");
        etOutline.setText("");
        etLectures.setText("");
        etMembers.setText("");
        etCourseCode.setText("");
        etPreRequisite.setText("");
        etFollowUp.setText("");
        etCreditHours.setText("");
        etInstructor.setText("");
        etLanguage.setText("");
        etNoOfQuizzes.setText("");
        etNoOfAssignments.setText("");
        etLevelDropdown.setText("");
        etDepartmentArray.setText("");

        // Reset switches
        switchIsPublic.setChecked(true);
        switchIsLab.setChecked(false);
        switchIsComputer.setChecked(true);
        switchIsPaid.setChecked(false);

        // Clear image
        ivCourseImage.setImageDrawable(null);
        ivCourseImage.setVisibility(View.GONE);
        base64Image = "";
    }
}