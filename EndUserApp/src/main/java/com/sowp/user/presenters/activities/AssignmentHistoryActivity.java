package com.sowp.user.presenters.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.adapters.AssignmentHistoryAdapter;
import com.sowp.user.models.AssignmentAttempt;
import com.sowp.user.repositories.firebase.UserRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class AssignmentHistoryActivity extends AppCompatActivity {

    private static final int ITEMS_PER_PAGE = 10;
    private static final String[] FILTER_OPTIONS = {"All Status", "Submitted", "Graded", "Failed", "Checked"};
    private static final String[] SORT_OPTIONS = {"Sort: Recent", "Sort: Score", "Sort: Status", "Sort: Assignment"};
    private static final String[] SORT_VALUES = {"Recent", "Score", "Status", "Assignment"};

    private RecyclerView recyclerViewAssignments;
    private TextView tvTotalSubmissions, tvPageInfo;
    private MaterialButton btnFilterStatus, btnSortBy, btnPrevious, btnNext;
    private ProgressBar progressBar;
    private View emptyStateLayout, paginationLayout;

    private AssignmentHistoryAdapter adapter;
    private List<AssignmentAttempt> filteredAttempts;
    private UserRepository userRepository;

    private int currentPage = 1;
    private int totalPages = 1;
    private String currentStatusFilter = "All Status";
    private String currentSortOrder = "Recent";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_assignment_history);

        setupWindowInsets();
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();

        userRepository = new UserRepository(this);
        loadAssignmentHistory();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        recyclerViewAssignments = findViewById(R.id.recyclerViewAssignments);
        tvTotalSubmissions = findViewById(R.id.tvTotalSubmissions);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        btnFilterStatus = findViewById(R.id.btnFilterStatus);
        btnSortBy = findViewById(R.id.btnSortBy);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        paginationLayout = findViewById(R.id.paginationLayout);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        recyclerViewAssignments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AssignmentHistoryAdapter(this, new ArrayList<>());
        recyclerViewAssignments.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnFilterStatus.setOnClickListener(v -> showFilterDialog());
        btnSortBy.setOnClickListener(v -> showSortDialog());
        btnPrevious.setOnClickListener(v -> goToPreviousPage());
        btnNext.setOnClickListener(v -> goToNextPage());
    }

    private void loadAssignmentHistory() {
        showLoading(true);
        currentPage = 1;
        loadAssignmentPage();
    }

    private void loadAssignmentPage() {
        String filterStatus = currentStatusFilter.equals("All Status") ? null : currentStatusFilter;

        userRepository.getAllAssignmentAttempts(
                currentPage - 1,
                ITEMS_PER_PAGE,
                currentSortOrder,
                filterStatus,
                new UserRepository.AssignmentAttemptsCallback() {
                    @Override
                    public void onSuccess(List<AssignmentAttempt> attempts) {
                        showLoading(false);
                        filteredAttempts = new ArrayList<>(attempts);
                        calculatePagination(attempts.size());
                        updateUI();
                    }

                    @Override
                    public void onFailure(String message) {
                        showLoading(false);
                        showError("Failed to load assignments: " + message);
                        showEmptyState(true);
                    }
                }
        );
    }

    private void calculatePagination(int totalItems) {
        totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
    }

    private void updateUI() {
        if (isEmptyResult()) {
            showEmptyState(true);
            tvTotalSubmissions.setText("Total Submissions: 0");
        } else {
            showEmptyState(false);
            tvTotalSubmissions.setText("Total Submissions: " + filteredAttempts.size());
            adapter.updateAttempts(filteredAttempts);
            updatePaginationUI();
        }
    }

    private boolean isEmptyResult() {
        return filteredAttempts == null || filteredAttempts.isEmpty();
    }

    private void updatePaginationUI() {
        tvPageInfo.setText("Page " + currentPage + " of " + Math.max(1, totalPages));
        btnPrevious.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages && filteredAttempts.size() == ITEMS_PER_PAGE);
    }

    private void goToPreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadAssignmentPage();
            recyclerViewAssignments.smoothScrollToPosition(0);
        }
    }

    private void goToNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadAssignmentPage();
            recyclerViewAssignments.smoothScrollToPosition(0);
        }
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Status");
        builder.setSingleChoiceItems(FILTER_OPTIONS, getCurrentFilterIndex(), (dialog, which) -> {
            String newFilter = FILTER_OPTIONS[which];
            if (!newFilter.equals(currentStatusFilter)) {
                currentStatusFilter = newFilter;
                btnFilterStatus.setText(newFilter);
                loadAssignmentHistory();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private int getCurrentFilterIndex() {
        for (int i = 0; i < FILTER_OPTIONS.length; i++) {
            if (FILTER_OPTIONS[i].equals(currentStatusFilter)) {
                return i;
            }
        }
        return 0;
    }

    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sort By");
        builder.setSingleChoiceItems(SORT_OPTIONS, getCurrentSortIndex(), (dialog, which) -> {
            String newSort = SORT_VALUES[which];
            if (!newSort.equals(currentSortOrder)) {
                currentSortOrder = newSort;
                btnSortBy.setText(SORT_OPTIONS[which]);
                loadAssignmentHistory();
            }
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private int getCurrentSortIndex() {
        for (int i = 0; i < SORT_VALUES.length; i++) {
            if (SORT_VALUES[i].equals(currentSortOrder)) {
                return i;
            }
        }
        return 0;
    }

    private void showError(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewAssignments.setVisibility(show ? View.GONE : View.VISIBLE);
        paginationLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewAssignments.setVisibility(show ? View.GONE : View.VISIBLE);
        paginationLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}