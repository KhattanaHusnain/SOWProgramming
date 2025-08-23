package com.sowp.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AddCourseActivity extends AppCompatActivity {

    private TextInputEditText etId, etTitle, etCategory, etDuration, etTags, etIllustration,
            etDescription, etOutline, etLectures, etMembers;
    private SwitchMaterial switchIsPublic;
    private MaterialButton btnAddCourse;
    private AutoCompleteTextView etSemester;
    private ProgressBar progressBar;
    private String[] semesterOptions = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "Graduated"};
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Set click listener
        btnAddCourse.setOnClickListener(v -> addCourseToFirestore());
    }

    private void initViews() {
        etId = findViewById(R.id.etId);
        etTitle = findViewById(R.id.etTitle);
        etCategory = findViewById(R.id.etCategory);
        etDuration = findViewById(R.id.etDuration);
        etIllustration = findViewById(R.id.etIllustration);
        etDescription = findViewById(R.id.etDescription);
        etOutline = findViewById(R.id.etOutline);
        etLectures = findViewById(R.id.etLectures);
        etMembers = findViewById(R.id.etMembers);
        switchIsPublic = findViewById(R.id.switchIsPublic);
        btnAddCourse = findViewById(R.id.btnAddCourse);
        progressBar = findViewById(R.id.progressBar);
        etSemester = findViewById(R.id.etSemester);
        etTags = findViewById(R.id.etTags);
        // Setup Semester dropdown
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, semesterOptions);
        etSemester.setAdapter(semesterAdapter);
    }

    private void addCourseToFirestore() {
        if (!validateInputs()) return;

        showProgressBar(true);

        long now = System.currentTimeMillis();
        String docId = getTextFromEditText(etId);

        Map<String, Object> course = new HashMap<>();
        course.put("id", Integer.parseInt(docId));
        course.put("title", getTextFromEditText(etTitle));
        course.put("category", getTextFromEditText(etCategory));
        course.put("duration", getTextFromEditText(etDuration));
        course.put("illustration", getTextFromEditText(etIllustration));
        course.put("description", getTextFromEditText(etDescription));
        course.put("outline", getTextFromEditText(etOutline));
        course.put("lectures", getIntegerFromEditText(etLectures));
        course.put("members", getIntegerFromEditText(etMembers));
        course.put("isPublic", switchIsPublic.isChecked());
        course.put("createdAt", now);
        course.put("updatedAt", now);
        course.put("completed", false);
        course.put("semester", etSemester.getText().toString());

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
        String idText = getTextFromEditText(etId);
        if (TextUtils.isEmpty(getTextFromEditText(etId))) {
            etId.setError("Id is required");
            etId.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etTitle))) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etCategory))) {
            etCategory.setError("Category is required");
            etCategory.requestFocus();
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

        if (TextUtils.isEmpty(getTextFromEditText(etIllustration))) {
            etIllustration.setError("Illustration is required");
            etIllustration.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etDescription))) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etTags))) {
            etTags.setError("Tags is required");
            etTags.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etOutline))) {
            etOutline.setError("Outline is required");
            etOutline.requestFocus();
            return false;
        }

        String lecturesText = getTextFromEditText(etLectures);
        if (TextUtils.isEmpty(lecturesText)) {
            etLectures.setError("Number of lectures is required");
            etLectures.requestFocus();
            return false;
        }



        String membersText = getTextFromEditText(etMembers);
        if (TextUtils.isEmpty(membersText)) {
            etMembers.setError("Number of members is required");
            etMembers.requestFocus();
            return false;
        }

        // Validate numeric fields
        try {
            Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            etId.setError("Please enter a valid number");
            etId.requestFocus();
            return false;
        }
        try {
            Integer.parseInt(lecturesText);
        } catch (NumberFormatException e) {
            etLectures.setError("Please enter a valid number");
            etLectures.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(membersText);
        } catch (NumberFormatException e) {
            etMembers.setError("Please enter a valid number");
            etMembers.requestFocus();
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

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAddCourse.setEnabled(!show);
    }

    private List<String> getTags(TextInputEditText editText) {
        // Split by commas and remove extra spaces
        // remove spaces
        // remove empty tags
        return Arrays.stream(getTextFromEditText(editText).split(","))
                .map(String::trim)                 // remove spaces
                .filter(s -> !s.isEmpty())         // remove empty tags
                .collect(Collectors.toList());
    }

    private void clearForm() {
        etId.setText("");
        etTags.setText("");
        etSemester.setText("");
        etTitle.setText("");
        etCategory.setText("");
        etDuration.setText("");
        etIllustration.setText("");
        etDescription.setText("");
        etOutline.setText("");
        etLectures.setText("");
        etMembers.setText("");
        switchIsPublic.setChecked(true);
    }
}