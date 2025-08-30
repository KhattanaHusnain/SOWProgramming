package com.sowp.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditTopicActivity extends AppCompatActivity {

    private static final String TAG = "EditTopicActivity";

    // Firebase
    private FirebaseFirestore db;
    private String courseId;
    private String topicId;

    // UI Components
    private Toolbar toolbar;
    private TextView tvTopicId, tvCreatedAt, tvUpdatedAt, tvProgressMessage;
    private TextInputEditText etTopicName, etTopicDescription, etTopicContent, etVideoId, etTags, etOrderIndex, etViews;
    private TextInputLayout tilTopicName, tilTopicDescription, tilTopicContent, tilVideoId, tilTags, tilOrderIndex, tilViews, tilSemester;
    private AutoCompleteTextView actvSemester;
    private ChipGroup chipGroupCategories;
    private Chip chipPublicStatus;
    private MaterialButton btnRefreshCategories;
    private ExtendedFloatingActionButton fabSave;
    private FloatingActionButton fabDelete;
    private ProgressBar progressCategories;
    private LinearLayout progressOverlay;

    // Data
    private Topic currentTopic;
    private List<String> availableCategories = new ArrayList<>();
    private List<String> selectedCategories = new ArrayList<>();
    private List<String> availableSemesters = new ArrayList<>();
    private SimpleDateFormat dateFormat;
    private boolean isLoading = false;
    private boolean hasUnsavedChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_topics);

        // Initialize components
        initializeComponents();
        setupUI();

        // Get data from intent
        getIntentData();

        // Load topic data
        if (topicId != null && courseId != null) {
            loadTopicData();
            loadCourseData();
        } else {
            showError("Missing topic or course information");
            finish();
        }
    }

    private void initializeComponents() {
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());

        // Find views
        toolbar = findViewById(R.id.toolbar);
        tvTopicId = findViewById(R.id.tvTopicId);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        tvProgressMessage = findViewById(R.id.tvProgressMessage);

        etTopicName = findViewById(R.id.etTopicName);
        etTopicDescription = findViewById(R.id.etTopicDescription);
        etTopicContent = findViewById(R.id.etTopicContent);
        etVideoId = findViewById(R.id.etVideoId);
        etTags = findViewById(R.id.etTags);
        etOrderIndex = findViewById(R.id.etOrderIndex);
        etViews = findViewById(R.id.etViews);

        tilTopicName = findViewById(R.id.tilTopicName);
        tilTopicDescription = findViewById(R.id.tilTopicDescription);
        tilTopicContent = findViewById(R.id.tilTopicContent);
        tilVideoId = findViewById(R.id.tilVideoId);
        tilTags = findViewById(R.id.tilTags);
        tilOrderIndex = findViewById(R.id.tilOrderIndex);
        tilViews = findViewById(R.id.tilViews);
        tilSemester = findViewById(R.id.tilSemester);

        actvSemester = findViewById(R.id.actvSemester);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        chipPublicStatus = findViewById(R.id.chipPublicStatus);
        btnRefreshCategories = findViewById(R.id.btnRefreshCategories);
        fabSave = findViewById(R.id.fabSave);
        fabDelete = findViewById(R.id.fabDelete);
        progressCategories = findViewById(R.id.progressCategories);
        progressOverlay = findViewById(R.id.progressOverlay);
    }

    private void setupUI() {
        setupToolbar();
        setupClickListeners();
        setupTextChangeListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupClickListeners() {
        fabSave.setOnClickListener(v -> saveTopicChanges());
        fabDelete.setOnClickListener(v -> confirmDeleteTopic());
        btnRefreshCategories.setOnClickListener(v -> loadCourseData());

        chipPublicStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hasUnsavedChanges = true;
            chipPublicStatus.setText(isChecked ? "Public" : "Private");
        });
    }

    private void setupTextChangeListeners() {
        // Add text change listeners to detect unsaved changes
        etTopicName.addTextChangedListener(new SimpleTextWatcher(() -> hasUnsavedChanges = true));
        etTopicDescription.addTextChangedListener(new SimpleTextWatcher(() -> hasUnsavedChanges = true));
        etTopicContent.addTextChangedListener(new SimpleTextWatcher(() -> hasUnsavedChanges = true));
        etVideoId.addTextChangedListener(new SimpleTextWatcher(() -> hasUnsavedChanges = true));
        etTags.addTextChangedListener(new SimpleTextWatcher(() -> hasUnsavedChanges = true));
        etOrderIndex.addTextChangedListener(new SimpleTextWatcher(() -> hasUnsavedChanges = true));

        actvSemester.setOnItemClickListener((parent, view, position, id) -> hasUnsavedChanges = true);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        courseId = intent.getStringExtra("COURSE_ID");
        topicId = intent.getStringExtra("TOPIC_ID");

        Log.d(TAG, "Course ID: " + courseId + ", Topic ID: " + topicId);
    }

    private void loadTopicData() {
        if (isLoading) return;

        showProgress(true, "Loading topic data...");

        db.collection("Course")
                .document(courseId)
                .collection("Topics")
                .document(topicId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentTopic = documentSnapshot.toObject(Topic.class);

                        if (currentTopic != null) {
                            populateTopicData();
                            Log.d(TAG, "Topic data loaded successfully");
                        } else {
                            showError("Failed to parse topic data");
                        }
                    } else {
                        showError("Topic not found");
                    }
                    showProgress(false, "");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading topic", e);
                    showError("Failed to load topic: " + e.getMessage());
                    showProgress(false, "");
                });
    }

    private void loadCourseData() {
        showCategoriesProgress(true);

        db.collection("Course")
                .document(courseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Load topic categories
                        Object categoriesObj = documentSnapshot.get("topicCategories");
                        availableCategories.clear();

                        if (categoriesObj instanceof List) {
                            List<?> categoriesList = (List<?>) categoriesObj;
                            for (Object category : categoriesList) {
                                if (category instanceof String) {
                                    availableCategories.add((String) category);
                                }
                            }
                        } else if (categoriesObj instanceof String) {
                            String categoriesString = (String) categoriesObj;
                            if (!categoriesString.trim().isEmpty()) {
                                availableCategories.addAll(Arrays.asList(categoriesString.split(",")));
                                // Clean up categories
                                for (int i = 0; i < availableCategories.size(); i++) {
                                    availableCategories.set(i, availableCategories.get(i).trim());
                                }
                            }
                        }

                        // Load semesters
                        Object semesterObj = documentSnapshot.get("semester");
                        availableSemesters.clear();

                        if (semesterObj instanceof String) {
                            String currentSemester = (String) semesterObj;
                            availableSemesters.add(currentSemester);

                            // Add common semester options
                            String[] commonSemesters = { "1st Semester", "2nd Semester", "3rd Semester", "4th Semester", "5th Semester", "6th Semester", "7th Semester", "8th Semester" };

                            for (String semester : commonSemesters) {
                                if (!availableSemesters.contains(semester)) {
                                    availableSemesters.add(semester);
                                }
                            }
                        }

                        // Update UI
                        setupCategoriesChips();
                        setupSemesterDropdown();

                        Log.d(TAG, "Course data loaded. Categories: " + availableCategories.size() +
                                ", Semesters: " + availableSemesters.size());
                    } else {
                        showError("Course not found");
                    }
                    showCategoriesProgress(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading course data", e);
                    showError("Failed to load course data: " + e.getMessage());
                    showCategoriesProgress(false);
                });
    }

    private void populateTopicData() {
        if (currentTopic == null) return;

        // Basic information
        tvTopicId.setText(String.valueOf(currentTopic.getTopicId()));
        etTopicName.setText(currentTopic.getName());
        etTopicDescription.setText(currentTopic.getDescription());
        etTopicContent.setText(currentTopic.getContent());
        etVideoId.setText(currentTopic.getVideoID());
        etTags.setText(currentTopic.getTags());
        etOrderIndex.setText(String.valueOf(currentTopic.getOrderIndex()));
        etViews.setText(String.valueOf(currentTopic.getViews()));

        // Public status
        chipPublicStatus.setChecked(currentTopic.isPublic());
        chipPublicStatus.setText(currentTopic.isPublic() ? "Public" : "Private");

        // Semester
        if (currentTopic.getSemester() != null) {
            actvSemester.setText(currentTopic.getSemester(), false);
        }

        // Parse and store selected categories
        selectedCategories.clear();
        if (currentTopic.getCategories() != null && !currentTopic.getCategories().trim().isEmpty()) {
            String[] categories = currentTopic.getCategories().split(",");
            for (String category : categories) {
                selectedCategories.add(category.trim());
            }
        }

        // Timestamps
        if (currentTopic.getCreatedAt() > 0) {
            tvCreatedAt.setText(dateFormat.format(new Date(currentTopic.getCreatedAt())));
        } else {
            tvCreatedAt.setText("Not available");
        }

        if (currentTopic.getUpdatedAt() > 0) {
            tvUpdatedAt.setText(dateFormat.format(new Date(currentTopic.getUpdatedAt())));
        } else {
            tvUpdatedAt.setText("Not updated");
        }

        hasUnsavedChanges = false;
    }

    private void setupCategoriesChips() {
        chipGroupCategories.removeAllViews();

        for (String category : availableCategories) {
            if (category != null && !category.trim().isEmpty()) {
                Chip chip = new Chip(this);
                chip.setText(category.trim());
                chip.setCheckable(true);
                chip.setCheckedIconVisible(true);

                // Check if this category is selected
                chip.setChecked(selectedCategories.contains(category.trim()));

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    hasUnsavedChanges = true;
                    String chipText = chip.getText().toString();

                    if (isChecked) {
                        if (!selectedCategories.contains(chipText)) {
                            selectedCategories.add(chipText);
                        }
                    } else {
                        selectedCategories.remove(chipText);
                    }

                    Log.d(TAG, "Selected categories: " + selectedCategories);
                });

                chipGroupCategories.addView(chip);
            }
        }

        if (availableCategories.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No categories available. Contact your administrator to add categories.");
            emptyView.setTextColor(getColor(android.R.color.darker_gray));
            emptyView.setPadding(16, 16, 16, 16);
            chipGroupCategories.addView(emptyView);
        }
    }

    private void setupSemesterDropdown() {
        if (!availableSemesters.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, availableSemesters);
            actvSemester.setAdapter(adapter);
        }
    }

    private void saveTopicChanges() {
        if (isLoading) return;

        if (!validateInput()) {
            return;
        }

        showProgress(true, "Saving changes...");

        // Create updated topic data
        Map<String, Object> updateData = new HashMap<>();

        // Basic fields
        updateData.put("name", etTopicName.getText().toString().trim());
        updateData.put("isPublic", chipPublicStatus.isChecked());
        updateData.put("topicId", Integer.parseInt(topicId));
        updateData.put("description", etTopicDescription.getText().toString().trim());
        updateData.put("content", etTopicContent.getText().toString().trim());
        updateData.put("videoID", etVideoId.getText().toString().trim());
        updateData.put("tags", etTags.getText().toString().trim());
        updateData.put("isPublic", chipPublicStatus.isChecked());
        updateData.put("semester", actvSemester.getText().toString().trim());
        updateData.put("updatedAt", System.currentTimeMillis());

        // Order index
        try {
            int orderIndex = Integer.parseInt(etOrderIndex.getText().toString().trim());
            updateData.put("orderIndex", orderIndex);
        } catch (NumberFormatException e) {
            updateData.put("orderIndex", 0);
        }

        // Categories
        String categoriesString = TextUtils.join(", ", selectedCategories);
        updateData.put("categories", categoriesString);

        // Update the document
        db.collection("Course/" + courseId + "/Topics")
                .document(topicId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Topic updated successfully");
                    showProgress(false, "");
                    hasUnsavedChanges = false;
                    Toast.makeText(this, "Topic updated successfully!", Toast.LENGTH_SHORT).show();

                    // Reload topic data to get updated timestamps
                    loadTopicData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating topic", e);
                    showProgress(false, "");
                    showError("Failed to update topic: " + e.getMessage());
                });

    }

    private boolean validateInput() {
        boolean isValid = true;

        // Clear previous errors
        tilTopicName.setError(null);
        tilOrderIndex.setError(null);

        // Validate topic name
        String topicName = etTopicName.getText().toString().trim();
        if (TextUtils.isEmpty(topicName)) {
            tilTopicName.setError("Topic name is required");
            isValid = false;
        } else if (topicName.length() < 3) {
            tilTopicName.setError("Topic name must be at least 3 characters");
            isValid = false;
        }

        // Validate order index
        String orderIndexStr = etOrderIndex.getText().toString().trim();
        if (!TextUtils.isEmpty(orderIndexStr)) {
            try {
                int orderIndex = Integer.parseInt(orderIndexStr);
                if (orderIndex < 0) {
                    tilOrderIndex.setError("Order index must be 0 or positive");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                tilOrderIndex.setError("Order index must be a valid number");
                isValid = false;
            }
        }

        // Validate video ID format (basic YouTube ID validation)
        String videoId = etVideoId.getText().toString().trim();
        if (!TextUtils.isEmpty(videoId)) {
            if (videoId.length() != 11 || !videoId.matches("[a-zA-Z0-9_-]+")) {
                tilVideoId.setError("Invalid YouTube video ID format");
                isValid = false;
            }
        }

        return isValid;
    }

    private void confirmDeleteTopic() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Topic")
                .setMessage("Are you sure you want to delete this topic? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteTopic())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTopic() {
        if (isLoading) return;

        showProgress(true, "Deleting topic...");

        // Delete the document
        db.collection("Course/" + courseId + "/Topics")
                .document(topicId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Topic deleted successfully");
                    showProgress(false, "");
                    Toast.makeText(this, "Topic deleted successfully!", Toast.LENGTH_SHORT).show();

                    // Return to previous activity
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting topic", e);
                    showProgress(false, "");
                    showError("Failed to delete topic: " + e.getMessage());
                });
    }

    private void showProgress(boolean show, String message) {
        isLoading = show;
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && !TextUtils.isEmpty(message)) {
            tvProgressMessage.setText(message);
        }
    }

    private void showCategoriesProgress(boolean show) {
        progressCategories.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Are you sure you want to leave without saving?")
                    .setPositiveButton("Leave", (dialog, which) -> super.onBackPressed())
                    .setNegativeButton("Stay", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Simple TextWatcher implementation
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable onTextChanged;

        public SimpleTextWatcher(Runnable onTextChanged) {
            this.onTextChanged = onTextChanged;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(android.text.Editable s) {
            if (onTextChanged != null) {
                onTextChanged.run();
            }
        }
    }
}