package com.sowp.admin.topicmanagement;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sowp.admin.NotificationHelper;
import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.Course;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddTopicActivity extends AppCompatActivity {

    private ImageView btnBack;
    private AutoCompleteTextView actvCourse;
    private TextInputEditText etName, etDescription, etVideoId, etContent, etTags, etSemester;
    private ChipGroup chipGroupCategories;
    private SwitchMaterial switchIsPublic;
    private MaterialButton btnAddTopic;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private List<Course> courseList;
    private Course selectedCourse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_topic);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        courseList = new ArrayList<>();

        // Initialize views
        initViews();

        // Load courses from Firestore
        loadCourses();

        // Set click listeners
        btnBack.setOnClickListener(v -> finish());
        btnAddTopic.setOnClickListener(v -> addTopicToFirestore());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        actvCourse = findViewById(R.id.actvCourse);
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etVideoId = findViewById(R.id.etVideoId);
        etContent = findViewById(R.id.etContent);
        etTags = findViewById(R.id.etTags);
        etSemester = findViewById(R.id.etSemester);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        switchIsPublic = findViewById(R.id.switchIsPublic);
        btnAddTopic = findViewById(R.id.btnAddTopic);
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadCourses() {
        showProgressBar(true);

        db.collection("Course")
                .get()
                .addOnCompleteListener(task -> {
                    showProgressBar(false);
                    if (task.isSuccessful()) {
                        courseList.clear();
                        List<String> courseNames = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Course course = document.toObject(Course.class);
                            course.setId(Integer.parseInt(document.getId()));
                            courseList.add(course);
                            courseNames.add(course.getTitle() + " (" + course.getCourseCode() + ")");
                        }

                        // Setup course dropdown
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line, courseNames);
                        actvCourse.setAdapter(adapter);

                        actvCourse.setOnItemClickListener((parent, view, position, id) -> {
                            selectedCourse = courseList.get(position);
                            etSemester.setText(selectedCourse.getSemester());
                            etSemester.setEnabled(false);
                            loadCategoriesForCourse(selectedCourse);
                        });

                    } else {
                        Toast.makeText(this, "Error loading courses: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadCategoriesForCourse(Course course) {
        chipGroupCategories.removeAllViews();

        if (course.getTopicCategories() != null) {
            for (String category : course.getTopicCategories()) {
                Chip chip = new Chip(this);
                chip.setText(category);
                chip.setCheckable(true);
                chip.setClickable(true);
                chipGroupCategories.addView(chip);
            }
        }
    }

    private void addTopicToFirestore() {
        if (!validateInputs()) return;

        showProgressBar(true);

        getNextTopicId(selectedCourse.getId(), (nextTopicId) -> {
            // Prepare topic data
            Map<String, Object> topicData = new HashMap<>();
            topicData.put("topicId", nextTopicId);
            topicData.put("courseId", selectedCourse.getId());
            topicData.put("name", getTextFromEditText(etName));
            topicData.put("description", getTextFromEditText(etDescription));
            topicData.put("content", getTextFromEditText(etContent));
            topicData.put("videoID", getTextFromEditText(etVideoId));
            topicData.put("createdAt", System.currentTimeMillis());
            topicData.put("updatedAt", System.currentTimeMillis());
            topicData.put("isPublic", switchIsPublic.isChecked());
            topicData.put("tags", getTextFromEditText(etTags));
            topicData.put("categories", getSelectedCategories());
            topicData.put("views", 0);
            topicData.put("semester", getTextFromEditText(etSemester));
            topicData.put("orderIndex", nextTopicId);

            // Add topic to Firestore and increment lectures count
            db.collection("Course")
                    .document(String.valueOf(selectedCourse.getId()))
                    .collection("Topics")
                    .document(String.valueOf(nextTopicId))
                    .set(topicData)
                    .addOnSuccessListener(aVoid -> {
                        NotificationHelper.addNotification("New Topic added in Course: " + selectedCourse.getTitle());
                        incrementLecturesCount();
                    })
                    .addOnFailureListener(e -> {
                        showProgressBar(false);
                        Toast.makeText(this, "Error adding topic: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        android.util.Log.e("AddTopic", "Firestore write failed", e);
                    });
        });
    }

    private void incrementLecturesCount() {
        db.collection("Course")
                .document(String.valueOf(selectedCourse.getId()))
                .update("lectures", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    showProgressBar(false);
                    Toast.makeText(this, "Topic added successfully!", Toast.LENGTH_LONG).show();
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    showProgressBar(false);
                    Toast.makeText(this, "Topic added but failed to update lecture count: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    android.util.Log.e("AddTopic", "Failed to increment lectures count", e);
                    clearForm();
                });
    }

    private void getNextTopicId(int courseId, OnTopicIdCallback callback) {
        db.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Topics")
                .orderBy("topicId", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    int nextId = 1;

                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot lastTopic = task.getResult().getDocuments().get(0);
                        Object topicIdObj = lastTopic.get("topicId");

                        if (topicIdObj instanceof Long) {
                            nextId = ((Long) topicIdObj).intValue() + 1;
                        } else if (topicIdObj instanceof Integer) {
                            nextId = (Integer) topicIdObj + 1;
                        } else if (topicIdObj instanceof String) {
                            try {
                                nextId = Integer.parseInt((String) topicIdObj) + 1;
                            } catch (NumberFormatException e) {
                                Long orderIndex = lastTopic.getLong("orderIndex");
                                nextId = (orderIndex != null) ? orderIndex.intValue() + 1 : 1;
                            }
                        } else {
                            Long orderIndex = lastTopic.getLong("orderIndex");
                            nextId = (orderIndex != null) ? orderIndex.intValue() + 1 : 1;
                        }
                    }

                    callback.onTopicId(nextId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AddTopic", "Failed to get next topic ID", e);
                    Toast.makeText(AddTopicActivity.this, "Error getting topic ID, using default", Toast.LENGTH_SHORT).show();
                    callback.onTopicId(1);
                });
    }

    interface OnTopicIdCallback {
        void onTopicId(int topicId);
    }

    private String getSelectedCategories() {
        List<String> selectedCategories = new ArrayList<>();

        for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupCategories.getChildAt(i);
            if (chip.isChecked()) {
                selectedCategories.add(chip.getText().toString());
            }
        }

        return String.join(", ", selectedCategories);
    }

    private boolean validateInputs() {
        if (selectedCourse == null) {
            actvCourse.setError("Please select a course");
            actvCourse.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etName))) {
            etName.setError("Topic name is required");
            etName.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etDescription))) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(getTextFromEditText(etVideoId))) {
            etVideoId.setError("Video ID is required");
            etVideoId.requestFocus();
            return false;
        }

        return true;
    }

    private String getTextFromEditText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void showProgressBar(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnAddTopic.setEnabled(!show);
    }

    private void clearForm() {
        actvCourse.setText("");
        etName.setText("");
        etDescription.setText("");
        etVideoId.setText("");
        etContent.setText("");
        etTags.setText("");
        etSemester.setText("");
        chipGroupCategories.removeAllViews();
        switchIsPublic.setChecked(true);
        selectedCourse = null;
    }
}