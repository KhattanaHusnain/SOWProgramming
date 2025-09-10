package com.sowp.admin.assignmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.ViewCoursesActivity;

public class AssignmentManagementActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout cardViewAssignments;
    private LinearLayout cardUploadAssignment;
    private LinearLayout cardViewUncheckedAssignment;
    private TextView txtTotalAssignments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_management);

        initializeViews();
        setClickListeners();
        loadAssignmentStatistics();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        cardViewAssignments = findViewById(R.id.cardViewAssignments);
        cardUploadAssignment = findViewById(R.id.cardUploadAssignment);
        cardViewUncheckedAssignment = findViewById(R.id.cardViewUncheckedAssignment);
        txtTotalAssignments = findViewById(R.id.txtTotalAssignments);
    }

    private void setClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        cardViewAssignments.setOnClickListener(v -> {
            Intent intent = new Intent(AssignmentManagementActivity.this, ViewCoursesActivity.class);
            intent.putExtra("cameForAssignments", true);
            startActivity(intent);
        });

        cardUploadAssignment.setOnClickListener(v -> {
            Intent intent = new Intent(AssignmentManagementActivity.this, UploadAssignmentActivity.class);
            startActivity(intent);
        });

        cardViewUncheckedAssignment.setOnClickListener(v -> {
            Intent intent = new Intent(AssignmentManagementActivity.this, ViewUncheckedAssignmentsActivity.class);
            startActivity(intent);
        });
    }

    private void loadAssignmentStatistics() {
        // This method would typically load data from a database or API
        // For now, we'll use dummy data
        int totalAssignments = getTotalAssignmentsCount();

        // Update UI with the statistics
        txtTotalAssignments.setText(String.valueOf(totalAssignments));
    }

    // Simulate getting total assignments count from database/API
    private int getTotalAssignmentsCount() {
        // TODO: Replace with actual database query or API call
        return 12; // Dummy data
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics when returning to this activity
        loadAssignmentStatistics();
    }
}