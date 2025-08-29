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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
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

public class EditCourseActivity extends AppCompatActivity {

    // UI Components
    private ImageView ivBack, ivCourseImage, ivEditImage;
    private ProgressBar progressBar;
    private MaterialButton btnSaveCourse, btnDeleteCourse;

    // Text Input Fields
    private TextInputLayout tilTitle, tilShortTitle, tilCourseCode, tilDuration, tilTags,
            tilDescription, tilOutline, tilLectures, tilMembers, tilPreRequisite, tilFollowUp,
            tilCreditHours, tilInstructor, tilLanguage, tilNoOfQuizzes, tilNoOfAssignments,
            tilLevel, tilSemester;

    private TextInputEditText etId, etTitle, etShortTitle, etCourseCode, etDuration, etTags,
            etDescription, etOutline, etLectures, etMembers, etPreRequisite, etFollowUp,
            etCreditHours, etInstructor, etNoOfQuizzes, etNoOfAssignments;

    private AutoCompleteTextView etSemester, etLevelDropdown, etLanguage;
    private SwitchMaterial switchIsPublic, switchIsLab, switchIsComputer, switchIsPaid;

    // Department Switches
    private SwitchMaterial switchIT, switchSoftwareEng, switchComputerScience,
            switchAI, switchCyberSecurity;

    // Category Switches
    private SwitchMaterial switchCatAll, switchCatProgramming, switchCatNonProgramming,
            switchCatMajor, switchCatMinor, switchCatTheoretical, switchCatMathematical,
            switchCatOthers;

    // Edit Icons
    private ImageView ivEditTitle, ivEditShortTitle, ivEditCourseCode, ivEditDuration,
            ivEditTags, ivEditDescription, ivEditOutline, ivEditLectures, ivEditMembers,
            ivEditPreRequisite, ivEditFollowUp, ivEditCreditHours, ivEditInstructor,
            ivEditLanguage, ivEditNoOfQuizzes, ivEditNoOfAssignments, ivEditLevel,
            ivEditSemester;

    // Data
    private FirebaseFirestore db;
    private String courseId;
    private String base64Image = "";
    private boolean isEditMode = false;
    private boolean imageChanged = false;

    // Dropdown options
    private String[] semesterOptions = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "Graduated"};
    private String[] levelOptions = {"Beginner", "Intermediate", "Advanced", "Expert"};
    private String[] languageOptions = {"English", "Urdu"};

    // Activity result launcher for image selection
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_courses);

        // Get course ID from intent
        courseId = getIntent().getStringExtra("COURSE_ID");
        if (TextUtils.isEmpty(courseId)) {
            Toast.makeText(this, "Course ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize image picker launcher
        initImagePickerLauncher();

        // Initialize views
        initViews();

        // Setup click listeners
        setupClickListeners();

        // Load course data
        loadCourseData();

        // Initially disable all fields (view mode)
        setEditMode(false);
    }

    private void initImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                ivCourseImage.setImageURI(imageUri);
                                convertImageToBase64(imageUri);
                                imageChanged = true;
                            } catch (Exception e) {
                                Toast.makeText(this, "Error loading image: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );
    }

    private void initViews() {
        // Basic views
        ivBack = findViewById(R.id.ivBack);
        ivCourseImage = findViewById(R.id.ivCourseImage);
        ivEditImage = findViewById(R.id.ivEditImage);
        progressBar = findViewById(R.id.progressBar);
        btnSaveCourse = findViewById(R.id.btnSaveCourse);
        btnDeleteCourse = findViewById(R.id.btnDeleteCourse);

        // Text Input Layouts
        tilTitle = findViewById(R.id.tilTitle);
        tilShortTitle = findViewById(R.id.tilShortTitle);
        tilCourseCode = findViewById(R.id.tilCourseCode);
        tilDuration = findViewById(R.id.tilDuration);
        tilTags = findViewById(R.id.tilTags);
        tilDescription = findViewById(R.id.tilDescription);
        tilOutline = findViewById(R.id.tilOutline);
        tilLectures = findViewById(R.id.tilLectures);
        tilMembers = findViewById(R.id.tilMembers);
        tilPreRequisite = findViewById(R.id.tilPreRequisite);
        tilFollowUp = findViewById(R.id.tilFollowUp);
        tilCreditHours = findViewById(R.id.tilCreditHours);
        tilInstructor = findViewById(R.id.tilInstructor);
        tilLanguage = findViewById(R.id.tilLanguage);
        tilNoOfQuizzes = findViewById(R.id.tilNoOfQuizzes);
        tilNoOfAssignments = findViewById(R.id.tilNoOfAssignments);
        tilLevel = findViewById(R.id.tilLevel);
        tilSemester = findViewById(R.id.tilSemester);

        // Edit Text Fields
        etId = findViewById(R.id.etId);
        etTitle = findViewById(R.id.etTitle);
        etShortTitle = findViewById(R.id.etShortTitle);
        etCourseCode = findViewById(R.id.etCourseCode);
        etDuration = findViewById(R.id.etDuration);
        etTags = findViewById(R.id.etTags);
        etDescription = findViewById(R.id.etDescription);
        etOutline = findViewById(R.id.etOutline);
        etLectures = findViewById(R.id.etLectures);
        etMembers = findViewById(R.id.etMembers);
        etPreRequisite = findViewById(R.id.etPreRequisite);
        etFollowUp = findViewById(R.id.etFollowUp);
        etCreditHours = findViewById(R.id.etCreditHours);
        etInstructor = findViewById(R.id.etInstructor);
        etNoOfQuizzes = findViewById(R.id.etNoOfQuizzes);
        etNoOfAssignments = findViewById(R.id.etNoOfAssignments);

        // Dropdowns
        etSemester = findViewById(R.id.etSemester);
        etLevelDropdown = findViewById(R.id.etLevelDropdown);
        etLanguage = findViewById(R.id.etLanguage);

        // Switches
        switchIsPublic = findViewById(R.id.switchIsPublic);
        switchIsLab = findViewById(R.id.switchIsLab);
        switchIsComputer = findViewById(R.id.switchIsComputer);
        switchIsPaid = findViewById(R.id.switchIsPaid);

        // Department Switches
        switchIT = findViewById(R.id.switchIT);
        switchSoftwareEng = findViewById(R.id.switchSoftwareEng);
        switchComputerScience = findViewById(R.id.switchComputerScience);
        switchAI = findViewById(R.id.switchAI);
        switchCyberSecurity = findViewById(R.id.switchCyberSecurity);

        // Category Switches
        switchCatAll = findViewById(R.id.switchCatAll);
        switchCatProgramming = findViewById(R.id.switchCatProgramming);
        switchCatNonProgramming = findViewById(R.id.switchCatNonProgramming);
        switchCatMajor = findViewById(R.id.switchCatMajor);
        switchCatMinor = findViewById(R.id.switchCatMinor);
        switchCatTheoretical = findViewById(R.id.switchCatTheoretical);
        switchCatMathematical = findViewById(R.id.switchCatMathematical);
        switchCatOthers = findViewById(R.id.switchCatOthers);

        // Edit Icons
        ivEditTitle = findViewById(R.id.ivEditTitle);
        ivEditShortTitle = findViewById(R.id.ivEditShortTitle);
        ivEditCourseCode = findViewById(R.id.ivEditCourseCode);
        ivEditDuration = findViewById(R.id.ivEditDuration);
        ivEditTags = findViewById(R.id.ivEditTags);
        ivEditDescription = findViewById(R.id.ivEditDescription);
        ivEditOutline = findViewById(R.id.ivEditOutline);
        ivEditLectures = findViewById(R.id.ivEditLectures);
        ivEditMembers = findViewById(R.id.ivEditMembers);
        ivEditPreRequisite = findViewById(R.id.ivEditPreRequisite);
        ivEditFollowUp = findViewById(R.id.ivEditFollowUp);
        ivEditCreditHours = findViewById(R.id.ivEditCreditHours);
        ivEditInstructor = findViewById(R.id.ivEditInstructor);
        ivEditLanguage = findViewById(R.id.ivEditLanguage);
        ivEditNoOfQuizzes = findViewById(R.id.ivEditNoOfQuizzes);
        ivEditNoOfAssignments = findViewById(R.id.ivEditNoOfAssignments);
        ivEditLevel = findViewById(R.id.ivEditLevel);
        ivEditSemester = findViewById(R.id.ivEditSemester);

        // Setup dropdowns
        setupDropdowns();
    }

    private void setupDropdowns() {
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, semesterOptions);
        etSemester.setAdapter(semesterAdapter);

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, levelOptions);
        etLevelDropdown.setAdapter(levelAdapter);

        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, languageOptions);
        etLanguage.setAdapter(languageAdapter);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSaveCourse.setOnClickListener(v -> {
            if (isEditMode) {
                saveCourseData();
            } else {
                setEditMode(true);
            }
        });

        btnDeleteCourse.setOnClickListener(v -> showDeleteConfirmation());

        ivEditImage.setOnClickListener(v -> {
            if (isEditMode) {
                selectImageFromGallery();
            }
        });

        // Setup edit icon click listeners
        setupEditIconListeners();
    }

    private void setupEditIconListeners() {
        ivEditTitle.setOnClickListener(v -> toggleFieldEdit(tilTitle, etTitle));
        ivEditShortTitle.setOnClickListener(v -> toggleFieldEdit(tilShortTitle, etShortTitle));
        ivEditCourseCode.setOnClickListener(v -> toggleFieldEdit(tilCourseCode, etCourseCode));
        ivEditDuration.setOnClickListener(v -> toggleFieldEdit(tilDuration, etDuration));
        ivEditTags.setOnClickListener(v -> toggleFieldEdit(tilTags, etTags));
        ivEditDescription.setOnClickListener(v -> toggleFieldEdit(tilDescription, etDescription));
        ivEditOutline.setOnClickListener(v -> toggleFieldEdit(tilOutline, etOutline));
        ivEditLectures.setOnClickListener(v -> toggleFieldEdit(tilLectures, etLectures));
        ivEditMembers.setOnClickListener(v -> toggleFieldEdit(tilMembers, etMembers));
        ivEditPreRequisite.setOnClickListener(v -> toggleFieldEdit(tilPreRequisite, etPreRequisite));
        ivEditFollowUp.setOnClickListener(v -> toggleFieldEdit(tilFollowUp, etFollowUp));
        ivEditCreditHours.setOnClickListener(v -> toggleFieldEdit(tilCreditHours, etCreditHours));
        ivEditInstructor.setOnClickListener(v -> toggleFieldEdit(tilInstructor, etInstructor));
        ivEditLanguage.setOnClickListener(v -> toggleFieldEdit(tilLanguage, etLanguage));
        ivEditNoOfQuizzes.setOnClickListener(v -> toggleFieldEdit(tilNoOfQuizzes, etNoOfQuizzes));
        ivEditNoOfAssignments.setOnClickListener(v -> toggleFieldEdit(tilNoOfAssignments, etNoOfAssignments));
        ivEditLevel.setOnClickListener(v -> toggleFieldEdit(tilLevel, etLevelDropdown));
        ivEditSemester.setOnClickListener(v -> toggleFieldEdit(tilSemester, etSemester));
    }

    private void toggleFieldEdit(TextInputLayout layout, View editText) {
        if (!isEditMode) {
            // Enable specific field for editing
            editText.setEnabled(true);
            editText.setFocusable(true);
            editText.setFocusableInTouchMode(true);
            editText.requestFocus();

            // Show keyboard for text fields
            if (editText instanceof TextInputEditText) {
                ((TextInputEditText) editText).selectAll();
            }
        }
    }

    private void loadCourseData() {
        showProgressBar(true);

        db.collection("Course")
                .document(courseId)
                .get()
                .addOnCompleteListener(task -> {
                    showProgressBar(false);

                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot document = task.getResult();
                        populateFields(document);
                    } else {
                        Toast.makeText(this, "Course not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showProgressBar(false);
                    Toast.makeText(this, "Error loading course: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void populateFields(DocumentSnapshot document) {
        try {
            // Basic fields with null checks
            setTextSafely(etId, document.getLong("id"));
            setTextSafely(etTitle, document.getString("title"));
            setTextSafely(etShortTitle, document.getString("shortTitle"));
            setTextSafely(etCourseCode, document.getString("courseCode"));
            setTextSafely(etDuration, document.getString("duration"));
            setTextSafely(etDescription, document.getString("description"));
            setTextSafely(etOutline, document.getString("outline"));
            setTextSafely(etInstructor, document.getString("instructor"));

            // Numeric fields with null checks
            setTextSafely(etLectures, document.getLong("lectures"));
            setTextSafely(etMembers, document.getLong("members"));
            setTextSafely(etCreditHours, document.getLong("creditHours"));
            setTextSafely(etNoOfQuizzes, document.getLong("noOfQuizzes"));
            setTextSafely(etNoOfAssignments, document.getLong("noOfAssignments"));

            // List fields with null checks and safe conversion
            setListTextSafely(etTags, (List<String>) document.get("tags"));
            setListTextSafely(etPreRequisite, (List<String>) document.get("preRequisite"));
            setListTextSafely(etFollowUp, (List<String>) document.get("followUp"));

            // Dropdown fields with null checks
            String semester = document.getString("semester");
            if (semester != null) {
                etSemester.setText(semester, false);
            }

            String level = document.getString("level");
            if (level != null) {
                etLevelDropdown.setText(level, false);
            }

            String language = document.getString("language");
            if (language != null) {
                etLanguage.setText(language, false);
            }

            // Department checkboxes - handle both old departmentArray and new individual fields
            List<String> departmentArray = (List<String>) document.get("departmentArray");
            if (departmentArray != null) {
                // Handle old format
                switchIT.setChecked(departmentArray.contains("Information Technology"));
                switchSoftwareEng.setChecked(departmentArray.contains("Software Engineering"));
                switchComputerScience.setChecked(departmentArray.contains("Computer Science"));
                switchAI.setChecked(departmentArray.contains("Artificial Intelligence"));
                switchCyberSecurity.setChecked(departmentArray.contains("Cyber Security"));
            } else {
                // Handle new format with individual boolean fields
                switchIT.setChecked(getBooleanSafely(document, "departmentIT", false));
                switchSoftwareEng.setChecked(getBooleanSafely(document, "departmentSoftwareEng", false));
                switchComputerScience.setChecked(getBooleanSafely(document, "departmentComputerScience", false));
                switchAI.setChecked(getBooleanSafely(document, "departmentAI", false));
                switchCyberSecurity.setChecked(getBooleanSafely(document, "departmentCyberSecurity", false));
            }

            // Category checkboxes - handle both old categoryArray and new individual fields
            List<String> categoryArray = (List<String>) document.get("categoryArray");
            if (categoryArray != null) {
                // Handle old format
                switchCatAll.setChecked(categoryArray.contains("All"));
                switchCatProgramming.setChecked(categoryArray.contains("Programming"));
                switchCatNonProgramming.setChecked(categoryArray.contains("Non Programming"));
                switchCatMajor.setChecked(categoryArray.contains("Major"));
                switchCatMinor.setChecked(categoryArray.contains("Minor"));
                switchCatTheoretical.setChecked(categoryArray.contains("Theoretical"));
                switchCatMathematical.setChecked(categoryArray.contains("Mathematical"));
                switchCatOthers.setChecked(categoryArray.contains("Others"));
            } else {
                // Handle new format with individual boolean fields
                switchCatAll.setChecked(getBooleanSafely(document, "categoryAll", false));
                switchCatProgramming.setChecked(getBooleanSafely(document, "categoryProgramming", false));
                switchCatNonProgramming.setChecked(getBooleanSafely(document, "categoryNonProgramming", false));
                switchCatMajor.setChecked(getBooleanSafely(document, "categoryMajor", false));
                switchCatMinor.setChecked(getBooleanSafely(document, "categoryMinor", false));
                switchCatTheoretical.setChecked(getBooleanSafely(document, "categoryTheoretical", false));
                switchCatMathematical.setChecked(getBooleanSafely(document, "categoryMathematical", false));
                switchCatOthers.setChecked(getBooleanSafely(document, "categoryOthers", false));
            }

            // Boolean fields with null checks (default to false if null)
            switchIsPublic.setChecked(getBooleanSafely(document, "isPublic", false));
            switchIsLab.setChecked(getBooleanSafely(document, "isLab", false));
            switchIsComputer.setChecked(getBooleanSafely(document, "isComputer", false));
            switchIsPaid.setChecked(getBooleanSafely(document, "isPaid", false));

            // Load base64 image with proper error handling
            String illustration = document.getString("illustration");
            if (!TextUtils.isEmpty(illustration)) {
                base64Image = illustration;
                loadBase64Image(illustration);
            } else {
                // Set default placeholder image if no illustration exists
                ivCourseImage.setImageResource(R.drawable.placeholder_course);
            }

        } catch (Exception e) {
            android.util.Log.e("EditCourse", "Error populating fields", e);
            Toast.makeText(this, "Error loading some course data", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper methods for safe data setting
    private void setTextSafely(TextInputEditText editText, String value) {
        if (editText != null) {
            editText.setText(value != null ? value : "");
        }
    }

    private void setTextSafely(TextInputEditText editText, Long value) {
        if (editText != null) {
            editText.setText(value != null ? String.valueOf(value) : "");
        }
    }

    private void setListTextSafely(TextInputEditText editText, List<String> list) {
        if (editText != null && list != null && !list.isEmpty()) {
            editText.setText(String.join(", ", list));
        } else if (editText != null) {
            editText.setText("");
        }
    }

    private boolean getBooleanSafely(DocumentSnapshot document, String field, boolean defaultValue) {
        Boolean value = document.getBoolean(field);
        return value != null ? value : defaultValue;
    }

    private void loadBase64Image(String base64String) {
        try {
            // Remove data URL prefix if present (e.g., "data:image/jpeg;base64,")
            String cleanBase64 = base64String;
            if (base64String.contains(",")) {
                cleanBase64 = base64String.substring(base64String.indexOf(",") + 1);
            }

            byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            if (decodedByte != null) {
                ivCourseImage.setImageBitmap(decodedByte);
            } else {
                android.util.Log.w("EditCourse", "Failed to decode base64 image");
                ivCourseImage.setImageResource(R.drawable.placeholder_course);
            }
        } catch (IllegalArgumentException e) {
            android.util.Log.e("EditCourse", "Invalid base64 image format", e);
            ivCourseImage.setImageResource(R.drawable.placeholder_course);
        } catch (Exception e) {
            android.util.Log.e("EditCourse", "Error loading base64 image", e);
            ivCourseImage.setImageResource(R.drawable.placeholder_course);
        }
    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "Unable to read image file", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                Toast.makeText(this, "Invalid image format", Toast.LENGTH_SHORT).show();
                inputStream.close();
                return;
            }

            // Resize bitmap to reasonable size
            bitmap = resizeBitmap(bitmap, 800, 600);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            inputStream.close();
            outputStream.close();

            Toast.makeText(this, "Image updated successfully", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            android.util.Log.e("EditCourse", "Error converting image to base64", e);
            Toast.makeText(this, "Error processing image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        } catch (OutOfMemoryError e) {
            android.util.Log.e("EditCourse", "Out of memory while processing image", e);
            Toast.makeText(this, "Image too large, please select a smaller image",
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

    private void setEditMode(boolean editMode) {
        isEditMode = editMode;

        // Update button text and appearance
        if (editMode) {
            btnSaveCourse.setText("Save Changes");
            btnSaveCourse.setIcon(getDrawable(R.drawable.ic_save));
            ivEditImage.setVisibility(View.VISIBLE);
        } else {
            btnSaveCourse.setText("Edit Course");
            btnSaveCourse.setIcon(getDrawable(R.drawable.ic_edit));
            ivEditImage.setVisibility(View.GONE);
        }

        // Enable/disable all fields
        setFieldsEnabled(editMode);
    }

    private void setFieldsEnabled(boolean enabled) {
        // Text fields (ID is always disabled)
        etTitle.setEnabled(enabled);
        etShortTitle.setEnabled(enabled);
        etCourseCode.setEnabled(enabled);
        etDuration.setEnabled(enabled);
        etTags.setEnabled(enabled);
        etDescription.setEnabled(enabled);
        etOutline.setEnabled(enabled);
        etLectures.setEnabled(enabled);
        etMembers.setEnabled(enabled);
        etPreRequisite.setEnabled(enabled);
        etFollowUp.setEnabled(enabled);
        etCreditHours.setEnabled(enabled);
        etInstructor.setEnabled(enabled);
        etNoOfQuizzes.setEnabled(enabled);
        etNoOfAssignments.setEnabled(enabled);
        etSemester.setEnabled(enabled);
        etLevelDropdown.setEnabled(enabled);
        etLanguage.setEnabled(enabled);

        // Switches
        switchIsPublic.setEnabled(enabled);
        switchIsLab.setEnabled(enabled);
        switchIsComputer.setEnabled(enabled);
        switchIsPaid.setEnabled(enabled);

        // Department switches
        switchIT.setEnabled(enabled);
        switchSoftwareEng.setEnabled(enabled);
        switchComputerScience.setEnabled(enabled);
        switchAI.setEnabled(enabled);
        switchCyberSecurity.setEnabled(enabled);

        // Category switches
        switchCatAll.setEnabled(enabled);
        switchCatProgramming.setEnabled(enabled);
        switchCatNonProgramming.setEnabled(enabled);
        switchCatMajor.setEnabled(enabled);
        switchCatMinor.setEnabled(enabled);
        switchCatTheoretical.setEnabled(enabled);
        switchCatMathematical.setEnabled(enabled);
        switchCatOthers.setEnabled(enabled);

        // Edit icons visibility
        int visibility = enabled ? View.VISIBLE : View.GONE;
        ivEditTitle.setVisibility(visibility);
        ivEditShortTitle.setVisibility(visibility);
        ivEditCourseCode.setVisibility(visibility);
        ivEditDuration.setVisibility(visibility);
        ivEditTags.setVisibility(visibility);
        ivEditDescription.setVisibility(visibility);
        ivEditOutline.setVisibility(visibility);
        ivEditLectures.setVisibility(visibility);
        ivEditMembers.setVisibility(visibility);
        ivEditPreRequisite.setVisibility(visibility);
        ivEditFollowUp.setVisibility(visibility);
        ivEditCreditHours.setVisibility(visibility);
        ivEditInstructor.setVisibility(visibility);
        ivEditLanguage.setVisibility(visibility);
        ivEditNoOfQuizzes.setVisibility(visibility);
        ivEditNoOfAssignments.setVisibility(visibility);
        ivEditLevel.setVisibility(visibility);
        ivEditSemester.setVisibility(visibility);
    }

    private void saveCourseData() {
        if (!validateInputs()) return;

        showProgressBar(true);

        Map<String, Object> updates = new HashMap<>();

        // Only include non-empty fields in updates
        addUpdateIfNotEmpty(updates, "title", getTextFromEditText(etTitle));
        addUpdateIfNotEmpty(updates, "shortTitle", getTextFromEditText(etShortTitle));
        addUpdateIfNotEmpty(updates, "courseCode", getTextFromEditText(etCourseCode));
        addUpdateIfNotEmpty(updates, "duration", getTextFromEditText(etDuration));
        addUpdateIfNotEmpty(updates, "description", getTextFromEditText(etDescription));
        addUpdateIfNotEmpty(updates, "outline", getTextFromEditText(etOutline));
        addUpdateIfNotEmpty(updates, "instructor", getTextFromEditText(etInstructor));

        // Numeric fields - only add if not empty
        String lectures = getTextFromEditText(etLectures);
        if (!TextUtils.isEmpty(lectures)) {
            updates.put("lectures", getIntegerFromEditText(etLectures));
        }

        String members = getTextFromEditText(etMembers);
        if (!TextUtils.isEmpty(members)) {
            updates.put("members", getIntegerFromEditText(etMembers));
        }

        String creditHours = getTextFromEditText(etCreditHours);
        if (!TextUtils.isEmpty(creditHours)) {
            updates.put("creditHours", getIntegerFromEditText(etCreditHours));
        }

        String noOfQuizzes = getTextFromEditText(etNoOfQuizzes);
        if (!TextUtils.isEmpty(noOfQuizzes)) {
            updates.put("noOfQuizzes", getIntegerFromEditText(etNoOfQuizzes));
        }

        String noOfAssignments = getTextFromEditText(etNoOfAssignments);
        if (!TextUtils.isEmpty(noOfAssignments)) {
            updates.put("noOfAssignments", getIntegerFromEditText(etNoOfAssignments));
        }

        // List fields - only add if not empty
        List<String> tags = getListFromEditText(etTags);
        if (!tags.isEmpty()) {
            updates.put("tags", tags);
        }

        List<String> preRequisite = getListFromEditText(etPreRequisite);
        if (!preRequisite.isEmpty()) {
            updates.put("preRequisite", preRequisite);
        }

        List<String> followUp = getListFromEditText(etFollowUp);
        if (!followUp.isEmpty()) {
            updates.put("followUp", followUp);
        }

        // Department checkboxes - save as both individual booleans and array for compatibility
        updates.put("departmentIT", switchIT.isChecked());
        updates.put("departmentSoftwareEng", switchSoftwareEng.isChecked());
        updates.put("departmentComputerScience", switchComputerScience.isChecked());
        updates.put("departmentAI", switchAI.isChecked());
        updates.put("departmentCyberSecurity", switchCyberSecurity.isChecked());

        // Also maintain departmentArray for backward compatibility
        List<String> departmentArray = new ArrayList<>();
        if (switchIT.isChecked()) departmentArray.add("Information Technology");
        if (switchSoftwareEng.isChecked()) departmentArray.add("Software Engineering");
        if (switchComputerScience.isChecked()) departmentArray.add("Computer Science");
        if (switchAI.isChecked()) departmentArray.add("Artificial Intelligence");
        if (switchCyberSecurity.isChecked()) departmentArray.add("Cyber Security");
        updates.put("departmentArray", departmentArray);

        // Category checkboxes - save as both individual booleans and array for compatibility
        updates.put("categoryAll", switchCatAll.isChecked());
        updates.put("categoryProgramming", switchCatProgramming.isChecked());
        updates.put("categoryNonProgramming", switchCatNonProgramming.isChecked());
        updates.put("categoryMajor", switchCatMajor.isChecked());
        updates.put("categoryMinor", switchCatMinor.isChecked());
        updates.put("categoryTheoretical", switchCatTheoretical.isChecked());
        updates.put("categoryMathematical", switchCatMathematical.isChecked());
        updates.put("categoryOthers", switchCatOthers.isChecked());

        // Also maintain categoryArray for backward compatibility
        List<String> categoryArray = new ArrayList<>();
        if (switchCatAll.isChecked()) categoryArray.add("All");
        if (switchCatProgramming.isChecked()) categoryArray.add("Programming");
        if (switchCatNonProgramming.isChecked()) categoryArray.add("Non Programming");
        if (switchCatMajor.isChecked()) categoryArray.add("Major");
        if (switchCatMinor.isChecked()) categoryArray.add("Minor");
        if (switchCatTheoretical.isChecked()) categoryArray.add("Theoretical");
        if (switchCatMathematical.isChecked()) categoryArray.add("Mathematical");
        if (switchCatOthers.isChecked()) categoryArray.add("Others");
        updates.put("categoryArray", categoryArray);

        // Dropdown fields
        String semester = etSemester.getText().toString().trim();
        if (!TextUtils.isEmpty(semester)) {
            updates.put("semester", semester);
        }

        String level = etLevelDropdown.getText().toString().trim();
        if (!TextUtils.isEmpty(level)) {
            updates.put("level", level);
        }

        String language = etLanguage.getText().toString().trim();
        if (!TextUtils.isEmpty(language)) {
            updates.put("language", language);
        }

        // Boolean fields - always update these
        updates.put("isPublic", switchIsPublic.isChecked());
        updates.put("isLab", switchIsLab.isChecked());
        updates.put("isComputer", switchIsComputer.isChecked());
        updates.put("isPaid", switchIsPaid.isChecked());

        // Image - only update if changed
        if (imageChanged && !TextUtils.isEmpty(base64Image)) {
            updates.put("illustration", base64Image);
        }

        // Update timestamp
        updates.put("updatedAt", System.currentTimeMillis());

        // Perform the update
        db.collection("Course")
                .document(courseId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showProgressBar(false);
                    Toast.makeText(this, "Course updated successfully!", Toast.LENGTH_LONG).show();
                    setEditMode(false);
                    imageChanged = false; // Reset image changed flag
                })
                .addOnFailureListener(e -> {
                    showProgressBar(false);
                    android.util.Log.e("EditCourse", "Error updating course", e);
                    Toast.makeText(this, "Error updating course: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // Helper method to add updates only if value is not empty
    private void addUpdateIfNotEmpty(Map<String, Object> updates, String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            updates.put(key, value);
        }
    }

    private void showDeleteConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete this course? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCourse() {
        showProgressBar(true);

        db.collection("Course")
                .document(courseId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    showProgressBar(false);
                    Toast.makeText(this, "Course deleted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showProgressBar(false);
                    android.util.Log.e("EditCourse", "Error deleting course", e);
                    Toast.makeText(this, "Error deleting course: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInputs() {
        // Clear any existing errors
        clearFieldErrors();

        boolean isValid = true;

        // Required fields validation
        if (TextUtils.isEmpty(getTextFromEditText(etTitle))) {
            etTitle.setError("Title is required");
            if (isValid) etTitle.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etCourseCode))) {
            etCourseCode.setError("Course Code is required");
            if (isValid) etCourseCode.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etInstructor))) {
            etInstructor.setError("Instructor is required");
            if (isValid) etInstructor.requestFocus();
            isValid = false;
        }

        // Validate numeric fields if not empty
        if (!validateNumericField(etLectures, "Number of Lectures")) {
            isValid = false;
        }

        if (!validateNumericField(etMembers, "Number of Members")) {
            isValid = false;
        }

        if (!validateNumericField(etCreditHours, "Credit Hours")) {
            isValid = false;
        }

        if (!validateNumericField(etNoOfQuizzes, "Number of Quizzes")) {
            isValid = false;
        }

        if (!validateNumericField(etNoOfAssignments, "Number of Assignments")) {
            isValid = false;
        }

        return isValid;
    }

    private boolean validateNumericField(TextInputEditText editText, String fieldName) {
        String text = getTextFromEditText(editText);
        if (!TextUtils.isEmpty(text)) {
            try {
                int value = Integer.parseInt(text);
                if (value < 0) {
                    editText.setError(fieldName + " cannot be negative");
                    return false;
                }
            } catch (NumberFormatException e) {
                editText.setError("Invalid number format for " + fieldName);
                return false;
            }
        }
        return true;
    }

    private void clearFieldErrors() {
        etTitle.setError(null);
        etCourseCode.setError(null);
        etInstructor.setError(null);
        etLectures.setError(null);
        etMembers.setError(null);
        etCreditHours.setError(null);
        etNoOfQuizzes.setError(null);
        etNoOfAssignments.setError(null);
    }

    private String getTextFromEditText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private int getIntegerFromEditText(TextInputEditText editText) {
        String text = getTextFromEditText(editText);
        try {
            return TextUtils.isEmpty(text) ? 0 : Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<String> getListFromEditText(TextInputEditText editText) {
        String text = getTextFromEditText(editText);
        if (TextUtils.isEmpty(text)) {
            return new ArrayList<>();
        }

        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveCourse.setEnabled(!show);
        btnDeleteCourse.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources if needed
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            // Show confirmation dialog if in edit mode
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Discard Changes")
                    .setMessage("You have unsaved changes. Are you sure you want to go back?")
                    .setPositiveButton("Discard", (dialog, which) -> {
                        setEditMode(false);
                        super.onBackPressed();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}