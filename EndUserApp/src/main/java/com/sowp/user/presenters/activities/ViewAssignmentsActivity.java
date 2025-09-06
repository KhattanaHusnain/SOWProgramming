package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.sowp.user.R;
import com.sowp.user.adapters.AssignmentAdapter;
import com.sowp.user.models.Assignment;
import com.sowp.user.presenters.activities.SubmitAssignmentActivity;

import java.util.ArrayList;
import java.util.List;

public class ViewAssignmentsActivity extends AppCompatActivity implements AssignmentAdapter.OnAssignmentClickListener {

    private ImageView btnBack;
    private TextView tvCourseTitle, tvCourseCode, tvAssignmentCount, tvPageInfo;
    private TextInputEditText etSearch;
    private AutoCompleteTextView spinnerFilter;
    private RecyclerView recyclerViewAssignments;
    private LinearLayout layoutEmptyState, layoutPagination;
    private ProgressBar progressBar;
    private Button btnPrevious, btnNext;

    private FirebaseFirestore firestore;
    private AssignmentAdapter assignmentAdapter;
    private List<Assignment> allAssignments;
    private List<Assignment> filteredAssignments;
    private int courseId;
    private String courseTitle = "";
    private String courseCode = "";

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    private String[] filterOptions = {"All Assignments", "High Score (90+)", "Medium Score (70-89)", "Low Score (<70)", "With Images", "No Images", "Recent First", "Oldest First"};
    private String currentFilter = "All Assignments";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_assignments);

        courseId = getIntent().getIntExtra("COURSE_ID", 0);
        if (courseId == 0) {
            finish();
            return;
        }

        initializeViews();
        setupFirestore();
        setupRecyclerView();
        setupSpinner();
        setupClickListeners();

        loadCourseDetails();
        loadAssignments();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvCourseTitle = findViewById(R.id.tvCourseTitle);
        tvCourseCode = findViewById(R.id.tvCourseCode);
        tvAssignmentCount = findViewById(R.id.tvAssignmentCount);
        tvPageInfo = findViewById(R.id.tvPageInfo);
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
        allAssignments = new ArrayList<>();
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

        spinnerFilter.setOnItemClickListener((parent, view, position, id) -> {
            currentFilter = filterOptions[position];
            applyFiltersAndPagination();
        });
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim();
                    applyFiltersAndPagination();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

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
                .document(String.valueOf(courseId))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        courseTitle = documentSnapshot.getString("title");
                        courseCode = documentSnapshot.getString("courseCode");

                        if (courseTitle != null) {
                            tvCourseTitle.setText(courseTitle);
                        }
                        if (courseCode != null) {
                            tvCourseCode.setText(courseCode);
                        }
                    }
                })
                .addOnFailureListener(e -> {});
    }

    private void loadAssignments() {
        if (isLoading) return;

        isLoading = true;
        showLoading(true);

        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Assignments")
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    isLoading = false;
                    showLoading(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Assignment> newAssignments = new ArrayList<>();

                        for (DocumentSnapshot document : task.getResult()) {
                            Assignment assignment = document.toObject(Assignment.class);
                            if (assignment != null) {
                                try {
                                    assignment.setId(Integer.parseInt(document.getId()));
                                } catch (NumberFormatException e) {
                                    assignment.setId(document.getId().hashCode());
                                }
                                assignment.setCourseId(Integer.parseInt(String.valueOf(courseId)));
                                newAssignments.add(assignment);
                            }
                        }

                        allAssignments.clear();
                        allAssignments.addAll(newAssignments);

                        applyFiltersAndPagination();
                    } else {
                        showEmptyState();
                    }
                });
    }

    private void applyFiltersAndPagination() {
        List<Assignment> tempFilteredAssignments = new ArrayList<>();

        for (Assignment assignment : allAssignments) {
            if (assignment == null) continue;

            boolean matchesSearch = true;
            if (!searchQuery.isEmpty()) {
                String title = assignment.getTitle() != null ? assignment.getTitle().toLowerCase() : "";
                String description = assignment.getDescription() != null ? assignment.getDescription().toLowerCase() : "";
                String semester = assignment.getSemester() != null ? assignment.getSemester().toLowerCase() : "";

                matchesSearch = title.contains(searchQuery.toLowerCase()) ||
                        description.contains(searchQuery.toLowerCase()) ||
                        semester.contains(searchQuery.toLowerCase());
            }

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
                case "Recent First":
                case "Oldest First":
                    matchesFilter = true;
                    break;
            }

            if (matchesSearch && matchesFilter) {
                tempFilteredAssignments.add(assignment);
            }
        }

        switch (currentFilter) {
            case "Recent First":
                tempFilteredAssignments.sort((a1, a2) -> Long.compare(a2.getCreatedAt(), a1.getCreatedAt()));
                break;
            case "Oldest First":
                tempFilteredAssignments.sort((a1, a2) -> Long.compare(a1.getCreatedAt(), a2.getCreatedAt()));
                break;
            default:
                tempFilteredAssignments.sort((a1, a2) -> Integer.compare(a1.getOrderIndex(), a2.getOrderIndex()));
                break;
        }

        filteredAssignments.clear();
        filteredAssignments.addAll(tempFilteredAssignments);

        calculatePagination();
        currentPage = 1;
        updatePaginatedDisplay();
        updatePaginationControls();
        updateAssignmentCount();
    }

    private void updatePaginatedDisplay() {
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, filteredAssignments.size());

        List<Assignment> paginatedAssignments = new ArrayList<>();
        if (startIndex < filteredAssignments.size()) {
            paginatedAssignments.addAll(filteredAssignments.subList(startIndex, endIndex));
        }

        assignmentAdapter.updateData(paginatedAssignments);

        if (paginatedAssignments.isEmpty() && filteredAssignments.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void updateAssignmentCount() {
        int count = filteredAssignments.size();
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
        Intent intent = new Intent(this, SubmitAssignmentActivity.class);
        intent.putExtra("COURSE_ID", courseId);
        intent.putExtra("ASSIGNMENT_ID", assignment.getId());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAssignments();
    }
}