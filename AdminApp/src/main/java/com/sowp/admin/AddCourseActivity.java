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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddCourseActivity extends AppCompatActivity {

    // Text input fields matching XML IDs
    private TextInputEditText etId, etCourseCode, etTitle, etShortTitle, etInstructor,
            etDuration, etCreditHours, etTags, etTopicCategories, etPreRequisite, etFollowUp,
            etDescription, etOutline, etLectures, etMembers, etNoOfQuizzes, etNoOfAssignments;

    // Dropdown fields
    private AutoCompleteTextView etSemester, etLevelDropdown, etLanguage;

    // Department switches
    private SwitchMaterial switchIT, switchSoftwareEng, switchComputerScience,
            switchAI, switchCyberSecurity;

    // Category switches
    private SwitchMaterial switchCatAll, switchCatProgramming, switchCatNonProgramming,
            switchCatMajor, switchCatMinor, switchCatTheoretical, switchCatMathematical,
            switchCatOthers;

    // Settings switches
    private SwitchMaterial switchIsPublic, switchIsLab, switchIsComputer, switchIsPaid;

    // Other UI elements
    private MaterialButton btnAddCourse, btnSelectImage;
    private ImageView ivCourseImage;
    private ProgressBar progressBar;

    // Dropdown options
    private String[] semesterOptions = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "Graduated"};
    private String[] levelOptions = {"Beginner", "Intermediate", "Advanced", "Expert"};
    private String[] languageOptions = {"English", "Urdu"};

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

        // Setup category switch logic
        setupCategorySwitchLogic();

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
        // Text input fields
        etId = findViewById(R.id.etId);
        etCourseCode = findViewById(R.id.etCourseCode);
        etTitle = findViewById(R.id.etTitle);
        etShortTitle = findViewById(R.id.etShortTitle);
        etInstructor = findViewById(R.id.etInstructor);
        etDuration = findViewById(R.id.etDuration);
        etCreditHours = findViewById(R.id.etCreditHours);
        etTags = findViewById(R.id.etTags);
        etTopicCategories = findViewById(R.id.etTopicCategories);
        etPreRequisite = findViewById(R.id.etPreRequisite);
        etFollowUp = findViewById(R.id.etFollowUp);
        etDescription = findViewById(R.id.etDescription);
        etOutline = findViewById(R.id.etOutline);
        etLectures = findViewById(R.id.etLectures);
        etMembers = findViewById(R.id.etMembers);
        etNoOfQuizzes = findViewById(R.id.etNoOfQuizzes);
        etNoOfAssignments = findViewById(R.id.etNoOfAssignments);

        // Dropdown fields
        etSemester = findViewById(R.id.etSemester);
        etLevelDropdown = findViewById(R.id.etLevelDropdown);
        etLanguage = findViewById(R.id.etLanguage);

        // Department switches
        switchIT = findViewById(R.id.switchIT);
        switchSoftwareEng = findViewById(R.id.switchSoftwareEng);
        switchComputerScience = findViewById(R.id.switchComputerScience);
        switchAI = findViewById(R.id.switchAI);
        switchCyberSecurity = findViewById(R.id.switchCyberSecurity);

        // Category switches
        switchCatAll = findViewById(R.id.switchCatAll);
        switchCatProgramming = findViewById(R.id.switchCatProgramming);
        switchCatNonProgramming = findViewById(R.id.switchCatNonProgramming);
        switchCatMajor = findViewById(R.id.switchCatMajor);
        switchCatMinor = findViewById(R.id.switchCatMinor);
        switchCatTheoretical = findViewById(R.id.switchCatTheoretical);
        switchCatMathematical = findViewById(R.id.switchCatMathematical);
        switchCatOthers = findViewById(R.id.switchCatOthers);

        // Settings switches
        switchIsPublic = findViewById(R.id.switchIsPublic);
        switchIsLab = findViewById(R.id.switchIsLab);
        switchIsComputer = findViewById(R.id.switchIsComputer);
        switchIsPaid = findViewById(R.id.switchIsPaid);

        // Buttons and other views
        btnAddCourse = findViewById(R.id.btnAddCourse);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        ivCourseImage = findViewById(R.id.ivCourseImage);
        progressBar = findViewById(R.id.progressBar);

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

        // Language dropdown
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, languageOptions);
        etLanguage.setAdapter(languageAdapter);
    }

    private void setupCategorySwitchLogic() {
        // When "All" is selected, disable other category switches
        switchCatAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchCatProgramming.setChecked(false);
                switchCatNonProgramming.setChecked(false);
                switchCatMajor.setChecked(false);
                switchCatMinor.setChecked(false);
                switchCatTheoretical.setChecked(false);
                switchCatMathematical.setChecked(false);
                switchCatOthers.setChecked(false);
            }
        });

        // When any other category is selected, uncheck "All"
        SwitchMaterial.OnCheckedChangeListener uncheckAllListener = (buttonView, isChecked) -> {
            if (isChecked && switchCatAll.isChecked()) {
                switchCatAll.setChecked(false);
            }
        };

        switchCatProgramming.setOnCheckedChangeListener(uncheckAllListener);
        switchCatNonProgramming.setOnCheckedChangeListener(uncheckAllListener);
        switchCatMajor.setOnCheckedChangeListener(uncheckAllListener);
        switchCatMinor.setOnCheckedChangeListener(uncheckAllListener);
        switchCatTheoretical.setOnCheckedChangeListener(uncheckAllListener);
        switchCatMathematical.setOnCheckedChangeListener(uncheckAllListener);
        switchCatOthers.setOnCheckedChangeListener(uncheckAllListener);
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
        course.put("courseCode", getTextFromEditText(etCourseCode));
        course.put("instructor", getTextFromEditText(etInstructor));
        course.put("duration", getTextFromEditText(etDuration));
        course.put("illustration", base64Image);
        course.put("semester", etSemester.getText().toString());
        course.put("creditHours", getIntegerFromEditText(etCreditHours));
        course.put("level", etLevelDropdown.getText().toString());
        course.put("language", etLanguage.getText().toString());
        course.put("tags", getListFromEditText(etTags));
        course.put("topicCategories", getListFromEditText(etTopicCategories));
        course.put("preRequisite", getListFromEditText(etPreRequisite));
        course.put("followUp", getListFromEditText(etFollowUp));
        course.put("description", getTextFromEditText(etDescription));
        course.put("outline", getTextFromEditText(etOutline));
        course.put("lectures", getIntegerFromEditText(etLectures));
        course.put("members", getIntegerFromEditText(etMembers));
        course.put("noOfQuizzes", getIntegerFromEditText(etNoOfQuizzes));
        course.put("noOfAssignments", getIntegerFromEditText(etNoOfAssignments));


        // Department array based on switches
        course.put("departmentArray", getSelectedDepartments());

        // Category array based on switches
        course.put("categoryArray", getSelectedCategories());

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

    private List<String> getSelectedDepartments() {
        List<String> departments = new ArrayList<>();

        if (switchIT.isChecked()) {
            departments.add("Information Technology");
        }
        if (switchSoftwareEng.isChecked()) {
            departments.add("Software Engineering");
        }
        if (switchComputerScience.isChecked()) {
            departments.add("Computer Science");
        }
        if (switchAI.isChecked()) {
            departments.add("Artificial Intelligence");
        }
        if (switchCyberSecurity.isChecked()) {
            departments.add("Cyber Security");
        }

        return departments;
    }

    private List<String> getSelectedCategories() {
        List<String> categories = new ArrayList<>();

        if (switchCatAll.isChecked()) {
            categories.add("All");
        }
        if (switchCatProgramming.isChecked()) {
            categories.add("Programming");
        }
        if (switchCatNonProgramming.isChecked()) {
            categories.add("Non Programming");
        }
        if (switchCatMajor.isChecked()) {
            categories.add("Major");
        }
        if (switchCatMinor.isChecked()) {
            categories.add("Minor");
        }
        if (switchCatTheoretical.isChecked()) {
            categories.add("Theoretical");
        }
        if (switchCatMathematical.isChecked()) {
            categories.add("Mathematical");
        }
        if (switchCatOthers.isChecked()) {
            categories.add("Others");
        }

        return categories;
    }

    private boolean validateInputs() {
        // Required field validations
        if (TextUtils.isEmpty(getTextFromEditText(etId))) {
            etId.setError("Course ID is required");
            etId.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etCourseCode))) {
            etCourseCode.setError("Course Code is required");
            etCourseCode.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etTitle))) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etInstructor))) {
            etInstructor.setError("Instructor name is required");
            etInstructor.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(base64Image)) {
            Toast.makeText(this, "Please select a course image", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etShortTitle))) {
            etShortTitle.setError("Short title is required");
            etShortTitle.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etTags))) {
            etTags.setError("Tags are required");
            etTags.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etTopicCategories))) {
            etTopicCategories.setError("Topic categories are required");
            etTopicCategories.requestFocus();
            return false;
        }


        if (TextUtils.isEmpty(etSemester.getText().toString())) {
            etSemester.setError("Semester is required");
            etSemester.requestFocus();
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

        if (TextUtils.isEmpty(getTextFromEditText(etDuration))) {
            etDuration.setError("Duration is required");
            etDuration.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etDescription))) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
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

        try {
            Integer.parseInt(getTextFromEditText(etCreditHours));
        } catch (NumberFormatException e) {
            etCreditHours.setError("Please enter a valid number");
            etCreditHours.requestFocus();
            return false;
        }

        // Validate optional numeric fields
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

        String quizzesText = getTextFromEditText(etNoOfQuizzes);
        if (!TextUtils.isEmpty(quizzesText)) {
            try {
                Integer.parseInt(quizzesText);
            } catch (NumberFormatException e) {
                etNoOfQuizzes.setError("Please enter a valid number");
                etNoOfQuizzes.requestFocus();
                return false;
            }
        }

        String assignmentsText = getTextFromEditText(etNoOfAssignments);
        if (!TextUtils.isEmpty(assignmentsText)) {
            try {
                Integer.parseInt(assignmentsText);
            } catch (NumberFormatException e) {
                etNoOfAssignments.setError("Please enter a valid number");
                etNoOfAssignments.requestFocus();
                return false;
            }
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
        etCourseCode.setText("");
        etTitle.setText("");
        etShortTitle.setText("");
        etInstructor.setText("");
        etDuration.setText("");
        etCreditHours.setText("");
        etTags.setText("");
        etTopicCategories.setText("");
        etPreRequisite.setText("");
        etFollowUp.setText("");
        etDescription.setText("");
        etOutline.setText("");
        etLectures.setText("");
        etMembers.setText("");
        etNoOfQuizzes.setText("");
        etNoOfAssignments.setText("");

        // Clear dropdowns
        etSemester.setText("");
        etLevelDropdown.setText("");
        etLanguage.setText("");

        // Reset department switches
        switchIT.setChecked(false);
        switchSoftwareEng.setChecked(false);
        switchComputerScience.setChecked(false);
        switchAI.setChecked(false);
        switchCyberSecurity.setChecked(false);

        // Reset category switches
        switchCatAll.setChecked(false);
        switchCatProgramming.setChecked(false);
        switchCatNonProgramming.setChecked(false);
        switchCatMajor.setChecked(false);
        switchCatMinor.setChecked(false);
        switchCatTheoretical.setChecked(false);
        switchCatMathematical.setChecked(false);
        switchCatOthers.setChecked(false);

        // Reset settings switches to default values
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