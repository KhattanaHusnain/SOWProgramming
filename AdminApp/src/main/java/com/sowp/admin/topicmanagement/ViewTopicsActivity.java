package com.sowp.admin.topicmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sowp.admin.R;

import java.util.ArrayList;
import java.util.List;

public class ViewTopicsActivity extends AppCompatActivity implements TopicsAdapter.OnTopicClickListener {

    private static final String TAG = "ViewTopicsActivity";
    private static final int ITEMS_PER_PAGE = 10;

    // Firebase
    private FirebaseFirestore db;
    private String courseId;

    // UI Components
    private RecyclerView topicsRecyclerView;
    private TopicsAdapter topicsAdapter;
    private ProgressBar progressBar;
    private TextView pageInfo;
    private MaterialButton btnPrevious, btnNext;
    private TextInputEditText searchEditText;
    private View emptyStateLayout;
    private FloatingActionButton fabAddTopic;
    private Toolbar toolbar;

    // Pagination variables
    private int currentPage = 0;
    private int totalPages = 0;
    private int totalItems = 0;
    private String currentSearchQuery = "";
    private boolean isLoading = false;

    // Data storage
    private List<Topic> allTopics = new ArrayList<>();
    private List<Topic> filteredTopics = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_topics);

        // Initialize components
        initializeComponents();
        setupUI();

        // Get course ID from intent
        courseId = getIntent().getStringExtra("COURSE_ID");
        if (courseId == null || courseId.isEmpty()) {
            Toast.makeText(this, "Course ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load topics
        loadTopicsFromFirebase();
    }

    private void initializeComponents() {
        db = FirebaseFirestore.getInstance();

        // Find views
        toolbar = findViewById(R.id.toolbar);
        topicsRecyclerView = findViewById(R.id.topicsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        pageInfo = findViewById(R.id.pageInfo);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        searchEditText = findViewById(R.id.searchEditText);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        fabAddTopic = findViewById(R.id.fabAddTopic);
    }

    private void setupUI() {
        setupToolbar();
        setupRecyclerView();
        setupPaginationButtons();
        setupSearch();
        setupFAB();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Course Topics");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        topicsAdapter = new TopicsAdapter(this, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        topicsRecyclerView.setLayoutManager(layoutManager);
        topicsRecyclerView.setAdapter(topicsAdapter);

        // Optional: Add item decoration for spacing
        // topicsRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void setupPaginationButtons() {
        btnPrevious.setOnClickListener(v -> {
            if (canGoPreviousPage()) {
                currentPage--;
                displayCurrentPage();
                scrollToTop();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (canGoNextPage()) {
                currentPage++;
                displayCurrentPage();
                scrollToTop();
            }
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newQuery = s.toString().trim();
                if (!newQuery.equals(currentSearchQuery)) {
                    currentSearchQuery = newQuery;
                    performSearch();
                }
            }
        });
    }

    private void setupFAB() {
        fabAddTopic.setOnClickListener(v -> {
            // Navigate to add topic activity
            Intent intent = new Intent(this, AddTopicActivity.class);
            intent.putExtra("COURSE_ID", courseId);
            startActivity(intent);
        });
    }

    private void loadTopicsFromFirebase() {
        if (isLoading) return;

        setLoadingState(true);

        // Clear existing data
        allTopics.clear();
        filteredTopics.clear();
        currentPage = 0;

        // Load all topics from Firebase
        db.collection("Course")
                .document(courseId)
                .collection("Topics")
                .orderBy("orderIndex")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTopics.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        try {
                            Topic topic = document.toObject(Topic.class);
                            if (topic != null) {
                                allTopics.add(topic);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error parsing topic document: " + document.getId(), e);
                        }
                    }

                    Log.d(TAG, "Successfully loaded " + allTopics.size() + " topics");

                    // Apply current search and display
                    performSearch();
                    setLoadingState(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading topics", e);
                    setLoadingState(false);
                    showError("Failed to load topics: " + e.getMessage());
                    showEmptyState(true);
                });
    }

    private void performSearch() {
        filteredTopics.clear();

        if (currentSearchQuery.isEmpty()) {
            // No search query - show all topics
            filteredTopics.addAll(allTopics);
        } else {
            // Filter topics based on search query
            String searchLower = currentSearchQuery.toLowerCase();
            for (Topic topic : allTopics) {
                if (topicMatchesSearch(topic, searchLower)) {
                    filteredTopics.add(topic);
                }
            }
        }

        // Reset to first page and recalculate pagination
        currentPage = 0;
        calculatePagination();
        displayCurrentPage();
    }

    private boolean topicMatchesSearch(Topic topic, String searchQuery) {
        if (topic == null || searchQuery == null || searchQuery.isEmpty()) {
            return false;
        }

        // Search in topic name
        if (topic.getName() != null && topic.getName().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Search in description
        if (topic.getDescription() != null && topic.getDescription().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Search in tags
        if (topic.getTags() != null && topic.getTags().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Search in categories
        if (topic.getCategories() != null && topic.getCategories().toLowerCase().contains(searchQuery)) {
            return true;
        }

        // Search in semester
        if (topic.getSemester() != null && topic.getSemester().toLowerCase().contains(searchQuery)) {
            return true;
        }

        return false;
    }

    private void calculatePagination() {
        totalItems = filteredTopics.size();
        totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

        // Ensure we have at least 1 page for UI consistency
        if (totalPages == 0) {
            totalPages = 1;
        }

        // Ensure current page is valid
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }
    }

    private void displayCurrentPage() {
        List<Topic> pageTopics = getCurrentPageTopics();

        if (pageTopics.isEmpty() && !isLoading) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            topicsAdapter.setTopics(pageTopics);
        }

        updatePaginationUI();
    }

    private List<Topic> getCurrentPageTopics() {
        if (filteredTopics.isEmpty()) {
            return new ArrayList<>();
        }

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredTopics.size());

        if (startIndex >= filteredTopics.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(filteredTopics.subList(startIndex, endIndex));
    }

    private void updatePaginationUI() {
        // Update page info text
        if (totalItems == 0) {
            pageInfo.setText("No topics found");
        } else {
            String info = String.format("Page %d of %d (%d %s)",
                    currentPage + 1,
                    totalPages,
                    totalItems,
                    totalItems == 1 ? "topic" : "topics");
            pageInfo.setText(info);
        }

        // Update button states
        btnPrevious.setEnabled(canGoPreviousPage());
        btnNext.setEnabled(canGoNextPage());
    }

    private boolean canGoPreviousPage() {
        return currentPage > 0;
    }

    private boolean canGoNextPage() {
        return currentPage < totalPages - 1;
    }

    private void scrollToTop() {
        if (topicsRecyclerView != null) {
            topicsRecyclerView.smoothScrollToPosition(0);
        }
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (loading) {
            showEmptyState(false);
        }
    }

    private void showEmptyState(boolean show) {
        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        topicsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // Pagination utility methods
    public void goToFirstPage() {
        if (currentPage != 0) {
            currentPage = 0;
            displayCurrentPage();
            scrollToTop();
        }
    }

    public void goToLastPage() {
        int lastPage = totalPages - 1;
        if (currentPage != lastPage) {
            currentPage = lastPage;
            displayCurrentPage();
            scrollToTop();
        }
    }

    public void goToPage(int page) {
        if (page >= 0 && page < totalPages && page != currentPage) {
            currentPage = page;
            displayCurrentPage();
            scrollToTop();
        }
    }

    // Public getters for pagination info (useful for testing or external access)
    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public boolean isLoading() {
        return isLoading;
    }

    // Interface implementation
    @Override
    public void onTopicClick(Topic topic, int position) {
        if (topic == null) {
            showError("Invalid topic selected");
            return;
        }

        // Navigate to topic details/edit activity
        Intent intent = new Intent(this, EditTopicActivity.class);
        intent.putExtra("COURSE_ID", courseId);
        intent.putExtra("TOPIC_ID", String.valueOf(topic.getOrderIndex()));
        startActivity(intent);
    }

    // Activity lifecycle methods
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from other activities
        // Only reload if we have existing data (avoid unnecessary loading on first create)
        if (!allTopics.isEmpty()) {
            loadTopicsFromFirebase();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources if needed
        if (topicsAdapter != null) {
            topicsAdapter.clearTopics();
        }
    }

    // Refresh method that can be called from outside
    public void refreshTopics() {
        loadTopicsFromFirebase();
    }

    // Method to clear search and show all topics
    public void clearSearch() {
        searchEditText.setText("");
        currentSearchQuery = "";
        performSearch();
    }
}