//package com.sowp.user.presenters.activities;
//
//import android.os.Bundle;
//import android.view.View;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.sowp.user.R;
//import com.sowp.user.adapters.AssignmentHistoryAdapter;
//import com.sowp.user.models.AssignmentAttempt;
//import com.sowp.user.repositories.firebase.UserRepository;
//import com.google.android.material.button.MaterialButton;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class AssignmentHistoryActivity extends AppCompatActivity {
//
//    private Toolbar toolbar;
//    private RecyclerView recyclerViewAssignments;
//    private TextView tvTotalSubmissions;
//    private TextView tvPageInfo;
//    private MaterialButton btnFilterStatus;
//    private MaterialButton btnSortBy;
//    private MaterialButton btnPrevious;
//    private MaterialButton btnNext;
//    private ProgressBar progressBar;
//    private View emptyStateLayout;
//
//    private AssignmentHistoryAdapter adapter;
//    private List<AssignmentAttempt> allAttempts;
//    private List<AssignmentAttempt> filteredAttempts;
//    private UserRepository userRepository;
//
//    // Pagination
//    private static final int ITEMS_PER_PAGE = 10;
//    private int currentPage = 0; // Firebase uses 0-based pagination
//    private int totalItems = 0;
//    private int totalPages = 1;
//
//    // Filter and sort options
//    private String currentStatusFilter = "All Status";
//    private String currentSortOrder = "Recent"; // Recent, Score, Status, Assignment
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_assignment_history);
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        initViews();
//        setupToolbar();
//        setupRecyclerView();
//        setupClickListeners();
//
//        // Initialize UserRepository
//        userRepository = new UserRepository(this);
//
//        loadAssignmentHistory();
//    }
//
//    private void initViews() {
//        toolbar = findViewById(R.id.toolbar);
//        recyclerViewAssignments = findViewById(R.id.recyclerViewAssignments);
//        tvTotalSubmissions = findViewById(R.id.tvTotalSubmissions);
//        tvPageInfo = findViewById(R.id.tvPageInfo);
//        btnFilterStatus = findViewById(R.id.btnFilterStatus);
//        btnSortBy = findViewById(R.id.btnSortBy);
//        btnPrevious = findViewById(R.id.btnPrevious);
//        btnNext = findViewById(R.id.btnNext);
//        progressBar = findViewById(R.id.progressBar);
//        emptyStateLayout = findViewById(R.id.emptyStateLayout);
//    }
//
//    private void setupToolbar() {
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setDisplayShowHomeEnabled(true);
//        }
//
//        toolbar.setNavigationOnClickListener(v -> onBackPressed());
//    }
//
//    private void setupRecyclerView() {
//        recyclerViewAssignments.setLayoutManager(new LinearLayoutManager(this));
//        adapter = new AssignmentHistoryAdapter(this, new ArrayList<>());
//        recyclerViewAssignments.setAdapter(adapter);
//    }
//
//    private void setupClickListeners() {
//        btnFilterStatus.setOnClickListener(v -> showFilterDialog());
//        btnSortBy.setOnClickListener(v -> showSortDialog());
//        btnPrevious.setOnClickListener(v -> goToPreviousPage());
//        btnNext.setOnClickListener(v -> goToNextPage());
//    }
//
//    private void loadAssignmentHistory() {
//        showLoading(true);
//
//        // First get the total count for pagination
//        userRepository.getAssignmentAttemptsCount(currentStatusFilter, new UserRepository.AssignmentCountCallback() {
//            @Override
//            public void onSuccess(int count) {
//                totalItems = count;
//                totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
//
//                // Load the actual data
//                loadAssignmentPage();
//            }
//
//            @Override
//            public void onFailure(String message) {
//                showLoading(false);
//                showError("Failed to load assignment count: " + message);
//                showEmptyState(true);
//            }
//        });
//    }
//
//    private void loadAssignmentPage() {
//        userRepository.getAllAssignmentAttempts(
//                currentPage,
//                ITEMS_PER_PAGE,
//                currentSortOrder,
//                currentStatusFilter,
//                new UserRepository.AssignmentAttemptsCallback() {
//                    @Override
//                    public void onSuccess(List<AssignmentAttempt> attempts) {
//                        showLoading(false);
//                        allAttempts = attempts;
//                        filteredAttempts = new ArrayList<>(attempts);
//                        updateUI();
//                        updatePaginationUI();
//                    }
//
//                    @Override
//                    public void onFailure(String message) {
//                        showLoading(false);
//                        showError("Failed to load assignments: " + message);
//                        showEmptyState(true);
//                    }
//                }
//        );
//    }
//
//    private List<AssignmentAttempt> generateSampleData() {
//        // This method is no longer needed as we're using Firebase data
//        return new ArrayList<>();
//    }
//
//    private void showError(String message) {
//        runOnUiThread(() -> {
//            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
//        });
//    }
//
//    private void updateUI() {
//        tvTotalSubmissions.setText("Total Submissions: " + totalItems);
//
//        if (filteredAttempts == null || filteredAttempts.isEmpty()) {
//            showEmptyState(true);
//        } else {
//            showEmptyState(false);
//            adapter.updateAttempts(filteredAttempts);
//        }
//    }
//
//    private void updatePaginationUI() {
//        currentPage = Math.max(0, currentPage); // Ensure page is not negative
//        int displayPage = currentPage + 1; // Display 1-based page numbers
//
//        tvPageInfo.setText("Page " + displayPage + " of " + Math.max(1, totalPages));
//        btnPrevious.setEnabled(currentPage > 0);
//        btnNext.setEnabled(currentPage < totalPages - 1);
//    }
//
//    private void goToPreviousPage() {
//        if (currentPage > 0) {
//            currentPage--;
//            loadAssignmentPage();
//            recyclerViewAssignments.smoothScrollToPosition(0);
//        }
//    }
//
//    private void goToNextPage() {
//        if (currentPage < totalPages - 1) {
//            currentPage++;
//            loadAssignmentPage();
//            recyclerViewAssignments.smoothScrollToPosition(0);
//        }
//    }
//
//    private void showFilterDialog() {
//        String[] filterOptions = {"All Status", "Submitted", "Graded", "Pending", "Late"};
//
//        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
//        builder.setTitle("Filter by Status");
//        builder.setSingleChoiceItems(filterOptions, getCurrentFilterIndex(), (dialog, which) -> {
//            String newFilter = filterOptions[which];
//            if (!newFilter.equals(currentStatusFilter)) {
//                currentStatusFilter = newFilter;
//                btnFilterStatus.setText(newFilter);
//                currentPage = 0; // Reset to first page
//                loadAssignmentHistory(); // Reload with new filter
//            }
//            dialog.dismiss();
//        });
//        builder.setNegativeButton("Cancel", null);
//        builder.show();
//    }
//
//    private int getCurrentFilterIndex() {
//        String[] filterOptions = {"All Status", "Submitted", "Graded", "Pending", "Late"};
//        for (int i = 0; i < filterOptions.length; i++) {
//            if (filterOptions[i].equals(currentStatusFilter)) {
//                return i;
//            }
//        }
//        return 0;
//    }
//
//    private void showSortDialog() {
//        String[] sortOptions = {"Sort: Recent", "Sort: Score", "Sort: Status", "Sort: Assignment"};
//        String[] sortValues = {"Recent", "Score", "Status", "Assignment"};
//
//        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
//        builder.setTitle("Sort By");
//        builder.setSingleChoiceItems(sortOptions, getCurrentSortIndex(), (dialog, which) -> {
//            String newSort = sortValues[which];
//            if (!newSort.equals(currentSortOrder)) {
//                currentSortOrder = newSort;
//                btnSortBy.setText(sortOptions[which]);
//                currentPage = 0; // Reset to first page
//                loadAssignmentHistory(); // Reload with new sort
//            }
//            dialog.dismiss();
//        });
//        builder.setNegativeButton("Cancel", null);
//        builder.show();
//    }
//
//    private int getCurrentSortIndex() {
//        String[] sortValues = {"Recent", "Score", "Status", "Assignment"};
//        for (int i = 0; i < sortValues.length; i++) {
//            if (sortValues[i].equals(currentSortOrder)) {
//                return i;
//            }
//        }
//        return 0;
//    }
//
//    private void showLoading(boolean show) {
//        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
//        recyclerViewAssignments.setVisibility(show ? View.GONE : View.VISIBLE);
//        findViewById(R.id.paginationLayout).setVisibility(show ? View.GONE : View.VISIBLE);
//    }
//
//    private void showEmptyState(boolean show) {
//        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
//        recyclerViewAssignments.setVisibility(show ? View.GONE : View.VISIBLE);
//        findViewById(R.id.paginationLayout).setVisibility(show ? View.GONE : View.VISIBLE);
//    }
//}