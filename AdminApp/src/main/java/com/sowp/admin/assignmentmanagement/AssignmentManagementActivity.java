package com.sowp.admin.assignmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.ViewCoursesActivity;

import java.util.List;

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

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Load quiz statistics
        loadAssignments();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        cardViewAssignments = findViewById(R.id.cardViewAssignments);
        cardUploadAssignment = findViewById(R.id.cardUploadAssignment);
        cardViewUncheckedAssignment = findViewById(R.id.cardViewUncheckedAssignment);
        txtTotalAssignments = findViewById(R.id.txtTotalAssignments);
    }

    private void setClickListeners() {

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // View Quizzes List card click listener
        cardViewAssignments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AssignmentManagementActivity.this, ViewCoursesActivity.class);
                intent.putExtra("cameForAssignments", true);
                startActivity(intent);
            }
        });

        // Upload New Quiz card click listener
        cardUploadAssignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AssignmentManagementActivity.this, UploadAssignmentActivity.class);
                startActivity(intent);
            }
        });
        // View Unchecked Assignments card click listener
        cardViewUncheckedAssignment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AssignmentManagementActivity.this, ViewUncheckedAssignmentsActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadAssignments() {
        FirebaseFirestore fb = FirebaseFirestore.getInstance();
        fb.collection("Course")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        int TotalAssignment= 0;
                        List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot document:documents){
                            Long assignments=document.getLong("noOfAssignments");
                            TotalAssignment += assignments.intValue();
                        }
                        txtTotalAssignments.setText(String.valueOf(TotalAssignment));

                    }
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadAssignments();
    }

}