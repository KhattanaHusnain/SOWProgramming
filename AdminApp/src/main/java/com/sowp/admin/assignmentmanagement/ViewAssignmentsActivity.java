package com.sowp.admin.assignmentmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.Course;

import java.util.ArrayList;
import java.util.List;

public class ViewAssignmentsActivity extends AppCompatActivity implements AssignmentAdapter.OnAssignmentClickListener {

    // UI Components
    private ImageView btnBack;
    private TextView tvCourseTitle, tvCourseCode, tvAssignmentCount, tvPageInfo;
    private MaterialButton btnAddAssignment;
    private TextInputEditText etSearch;
    private AutoCompleteTextView spinnerFilter;
    private RecyclerView recyclerViewAssignments;
    private LinearLayout layoutEmptyState, layoutPagination;
    private ProgressBar progressBar;
    private Button btnPrevious, btnNext;

    // Data
    private FirebaseFirestore firestore;
    private AssignmentAdapter assignmentAdapter;
    private List<Assignment> assignments;
    private List<Assignment> filteredAssignments;
    private Course currentCourse;
    private String courseId;

    // Pagination
    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    // Filter options
    private String[] filterOptions = {"All Assignments", "High Score (90+)", "Medium Score (70-89)", "Low Score (<70)", "With Images", "No Images"};
    private String currentFilter = "All Assignments";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_assignments);

        // Get course ID from intent
        courseId = getIntent().getStringExtra("COURSE_ID");
        if (courseId == null || courseId.isEmpty()) {
            Toast.makeText(this, "Invalid course ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize components
        initializeViews();
        setupFirestore();
        setupRecyclerView();
        setupSpinner();
        setupClickListeners();

        // Load data
        loadCourseDetails();
        loadAssignments();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvCourseTitle = findViewById(R.id.tvCourseTitle);
        tvCourseCode = findViewById(R.id.tvCourseCode);
        tvAssignmentCount = findViewById(R.id.tvAssignmentCount);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        btnAddAssignment = findViewById(R.id.btnAddAssignment);
        etSearch = findViewById(R.id.etSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        recyclerViewAssignments = findViewById(R.id.recyclerViewAssignments);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutPagination = findViewById(R.id.layoutPagination);
        progressBar = findViewById(R.id.progressBar);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
    }

    private void setupFirestore() {
        firestore = FirebaseFirestore.getInstance();
        assignments = new ArrayList<>();
        filteredAssignments = new ArrayList<>();
    }

    private void setupRecyclerView() {
        assignmentAdapter = new AssignmentAdapter(this, filteredAssignments, this);
        recyclerViewAssignments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAssignments.setAdapter(assignmentAdapter);
    }

    private void setupSpinner() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, filterOptions);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = filterOptions[position];
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        if (btnAddAssignment != null) {
            btnAddAssignment.setOnClickListener(v -> {
                Intent intent = new Intent(this, UploadAssignmentActivity.class);
                intent.putExtra("COURSE_ID", courseId);
                startActivity(intent);
            });
        }

        // Search functionality
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim();
                    applyFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Pagination buttons
        if (btnPrevious != null) {
            btnPrevious.setOnClickListener(v -> {
                if (currentPage > 1) {
                    currentPage--;
                    updatePaginatedDisplay();
                    updatePaginationControls();
                }
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (currentPage < totalPages) {
                    currentPage++;
                    updatePaginatedDisplay();
                    updatePaginationControls();
                }
            });
        }
    }

    private void loadCourseDetails() {
        firestore.collection("Course")
                .document(courseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentCourse = documentSnapshot.toObject(Course.class);
                        if (currentCourse != null) {
                            tvCourseTitle.setText(currentCourse.getTitle());
                            tvCourseCode.setText(currentCourse.getCourseCode());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load course details", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAssignments() {
        if (isLoading) return;

        isLoading = true;
        showLoading(true);

        firestore.collection("Course")
                .document(courseId)
                .collection("Assignments")
                .orderBy("orderIndex")
                .get()
                .addOnCompleteListener(task -> {
                    isLoading = false;
                    showLoading(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        // Create a new list to avoid concurrent modification
                        List<Assignment> newAssignments = new ArrayList<>();

                        for (DocumentSnapshot document : task.getResult()) {
                            Assignment assignment = document.toObject(Assignment.class);
                            if (assignment != null) {
                                newAssignments.add(assignment);
                            }
                        }

                        // Thread-safe update of the main assignments list
                        assignments.clear();
                        assignments.addAll(newAssignments);

                        applyFilters();
                        updateAssignmentCount();
                    } else {
                        Toast.makeText(this, "Failed to load assignments: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });
    }

    private void applyFilters() {
        // Create a temporary list to avoid modifying filteredAssignments during iteration
        List<Assignment> tempFilteredAssignments = new ArrayList<>();

        // Use a defensive copy of the original assignments list to prevent concurrent modification
        List<Assignment> assignmentsCopy;
        synchronized (assignments) {
            assignmentsCopy = new ArrayList<>(assignments);
        }

        for (Assignment assignment : assignmentsCopy) {
            if (assignment == null) continue;

            // Add null checks for safety
            String title = assignment.getTitle() != null ? assignment.getTitle() : "";
            String description = assignment.getDescription() != null ? assignment.getDescription() : "";

            boolean matchesSearch = searchQuery.isEmpty() ||
                    title.toLowerCase().contains(searchQuery.toLowerCase()) ||
                    description.toLowerCase().contains(searchQuery.toLowerCase());

            boolean matchesFilter = true;
            switch (currentFilter) {
                case "All Assignments":
                    matchesFilter = true;
                    break;
                case "High Score (90+)":
                    matchesFilter = assignment.getScore() >= 90;
                    break;
                case "Medium Score (70-89)":
                    matchesFilter = assignment.getScore() >= 70 && assignment.getScore() < 90;
                    break;
                case "Low Score (<70)":
                    matchesFilter = assignment.getScore() < 70;
                    break;
                case "With Images":
                    matchesFilter = assignment.getBase64Images() != null && !assignment.getBase64Images().isEmpty();
                    break;
                case "No Images":
                    matchesFilter = assignment.getBase64Images() == null || assignment.getBase64Images().isEmpty();
                    break;
            }

            if (matchesSearch && matchesFilter) {
                tempFilteredAssignments.add(assignment);
            }
        }

        // Now safely update the filteredAssignments list
        synchronized (filteredAssignments) {
            filteredAssignments.clear();
            filteredAssignments.addAll(tempFilteredAssignments);
        }

        calculatePagination();
        currentPage = 1; // Reset to first page when filter changes
        updatePaginatedDisplay();
        updatePaginationControls();
        updateAssignmentCount();
    }

    private void updatePaginatedDisplay() {
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, filteredAssignments.size());

        List<Assignment> paginatedAssignments = new ArrayList<>();

        // Thread-safe access to filteredAssignments
        synchronized (filteredAssignments) {
            if (startIndex < filteredAssignments.size()) {
                // Create a safe copy instead of using subList directly
                for (int i = startIndex; i < endIndex && i < filteredAssignments.size(); i++) {
                    paginatedAssignments.add(filteredAssignments.get(i));
                }
            }
        }

        // Update adapter with the safe copy
        assignmentAdapter.updateAssignments(paginatedAssignments);

        if (paginatedAssignments.isEmpty() && filteredAssignments.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void updateAssignmentCount() {
        int count;
        synchronized (filteredAssignments) {
            count = filteredAssignments.size();
        }

        String countText;
        if (count == 0) {
            countText = "No assignments found";
        } else if (count == 1) {
            countText = "1 assignment found";
        } else {
            countText = count + " assignments found";
        }

        if (tvAssignmentCount != null) {
            tvAssignmentCount.setText(countText);
        }
    }

    private void calculatePagination() {
        totalPages = (int) Math.ceil((double) filteredAssignments.size() / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
    }

    private void updatePaginationControls() {
        if (btnPrevious != null) {
            btnPrevious.setEnabled(currentPage > 1);
        }
        if (btnNext != null) {
            btnNext.setEnabled(currentPage < totalPages);
        }
        if (tvPageInfo != null) {
            tvPageInfo.setText("Page " + currentPage + " of " + totalPages);
        }
        if (layoutPagination != null) {
            layoutPagination.setVisibility(totalPages > 1 ? View.VISIBLE : View.GONE);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show && tvAssignmentCount != null) {
            tvAssignmentCount.setText("Loading assignments...");
        }
    }

    private void showEmptyState() {
        if (recyclerViewAssignments != null) {
            recyclerViewAssignments.setVisibility(View.GONE);
        }
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.VISIBLE);
        }
        if (layoutPagination != null) {
            layoutPagination.setVisibility(View.GONE);
        }
    }

    private void hideEmptyState() {
        if (recyclerViewAssignments != null) {
            recyclerViewAssignments.setVisibility(View.VISIBLE);
        }
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAssignmentClick(Assignment assignment) {
        Intent intent = new Intent(this, EditAssignmentActivity.class);
        intent.putExtra("COURSE_ID", courseId);
        intent.putExtra("ASSIGNMENT_ID", String.valueOf(assignment.getId()));
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload assignments when returning from EditAssignmentActivity or UploadAssignmentActivity
        loadAssignments();
    }
}