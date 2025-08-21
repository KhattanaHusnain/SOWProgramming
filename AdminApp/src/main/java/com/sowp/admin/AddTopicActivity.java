package com.sowp.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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

import java.util.HashMap;
import java.util.Map;

public class AddTopicActivity extends AppCompatActivity {

    private TextInputEditText etCourseId, etName, etDescription, etVideoId,
            etOrderIndex, etContent;
    private SwitchMaterial switchIsPublic;
    private MaterialButton btnAddTopic;
    private ProgressBar progressBar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_topic);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Set click listener
        btnAddTopic.setOnClickListener(v -> addTopicToFirestore());
    }

    private void initViews() {
        etCourseId = findViewById(R.id.etCourseId);
        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etVideoId = findViewById(R.id.etVideoId);
        etOrderIndex = findViewById(R.id.etOrderIndex);
        etContent = findViewById(R.id.etContent);
        switchIsPublic = findViewById(R.id.switchIsPublic);
        btnAddTopic = findViewById(R.id.btnAddTopic);
        progressBar = findViewById(R.id.progressBar);
    }

    private void addTopicToFirestore() {
        if (!validateInputs()) return;

        showProgressBar(true);

        long now = System.currentTimeMillis();

        // Generate document ID using timestamp for uniqueness
        String docId = String.valueOf(getIntegerFromEditText(etOrderIndex));

        Map<String, Object> topic = new HashMap<>();
        topic.put("courseId", getIntegerFromEditText(etCourseId));
        topic.put("name", getTextFromEditText(etName));
        topic.put("description", getTextFromEditText(etDescription));
        topic.put("videoID", getTextFromEditText(etVideoId));
        topic.put("orderIndex", getIntegerFromEditText(etOrderIndex));
        topic.put("content", getTextFromEditText(etContent));
        topic.put("isPublic", switchIsPublic.isChecked());
        topic.put("createdAt", now);
        topic.put("updatedAt", now);

        db.collection("Course/" + getIntegerFromEditText(etCourseId) + "/Topics")
                .document(docId)
                .set(topic)
                .addOnCompleteListener(task -> {
                    showProgressBar(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Topic added successfully!", Toast.LENGTH_LONG).show();
                        clearForm();
                    } else {
                        Exception e = task.getException();
                        Toast.makeText(this, "Error adding topic: " + (e != null ? e.getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();
                        android.util.Log.e("AddTopic", "Firestore write failed", e);
                    }
                });
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(getTextFromEditText(etCourseId))) {
            etCourseId.setError("Course ID is required");
            etCourseId.requestFocus();
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

        String courseIdText = getTextFromEditText(etCourseId);
        if (TextUtils.isEmpty(courseIdText)) {
            etCourseId.setError("Course ID is required");
            etCourseId.requestFocus();
            return false;
        }

        String orderIndexText = getTextFromEditText(etOrderIndex);
        if (TextUtils.isEmpty(orderIndexText)) {
            etOrderIndex.setError("Order index is required");
            etOrderIndex.requestFocus();
            return false;
        }

        // Validate numeric fields
        try {
            Integer.parseInt(courseIdText);
        } catch (NumberFormatException e) {
            etCourseId.setError("Please enter a valid course ID number");
            etCourseId.requestFocus();
            return false;
        }

        try {
            Integer.parseInt(orderIndexText);
        } catch (NumberFormatException e) {
            etOrderIndex.setError("Please enter a valid order index number");
            etOrderIndex.requestFocus();
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
        btnAddTopic.setEnabled(!show);
    }

    private void clearForm() {
        etCourseId.setText("");
        etName.setText("");
        etDescription.setText("");
        etVideoId.setText("");
        etOrderIndex.setText("");
        etContent.setText("");
        switchIsPublic.setChecked(true);
    }
}