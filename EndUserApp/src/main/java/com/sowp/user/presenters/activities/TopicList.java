package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.sowp.user.R;
import com.sowp.user.repositories.firebase.TopicRepository;
import com.sowp.user.repositories.firebase.UserRepository;
import com.sowp.user.adapters.TopicListAdapter;
import com.sowp.user.models.Topic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TopicList extends AppCompatActivity {
    private static final String TAG = "TopicList";
    private static final int ITEMS_PER_PAGE = 10;

    // UI Components
    private RecyclerView recyclerView;
    private View emptyView;
    private ProgressBar progressBar;
    private TextView pageInfo;
    private MaterialButton btnPrevious, btnNext;
    private TextInputEditText searchEditText;
    private Spinner categorySpinner, semesterSpinner;
    private Toolbar toolbar;

    // Adapters and Data
    private TopicListAdapter adapter;
    private TopicRepository topicRepository;
    private UserRepository userRepository;

    // Data and State
    private int courseId;
    private String[] topicCategories;
    private List<Topic> allTopics = new ArrayList<>();
    private List<Topic> filteredTopics = new ArrayList<>();

    // Pagination variables
    private int currentPage = 0;
    private int totalPages = 0;
    private int totalItems = 0;
    private boolean isLoading = false;

    // Filter variables
    private String currentSearchQuery = "";
    private String selectedCategory = "All Categories";
    private String selectedSemester = "All Semesters";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        // Initialize repositories
        topicRepository = new TopicRepository();
        userRepository = new UserRepository(this);

        // Get intent data
        courseId = getIntent().getIntExtra("courseID", 0);
        String categoriesString = getIntent().getStringExtra("topicCategories");
        if (categoriesString != null && !categoriesString.isEmpty()) {
            topicCategories = categoriesString.split(",");
            // Trim whitespace
            for (int i = 0; i < topicCategories.length; i++) {
                topicCategories[i] = topicCategories[i].trim();
            }
        } else {
            topicCategories = new String[0];
        }

        // Initialize components
        initializeComponents();
        setupUI();

        // Load topics
        loadTopicsFromFirestore();
    }

    private void initializeComponents() {
        // Find views
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progressBar);
        pageInfo = findViewById(R.id.pageInfo);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        searchEditText = findViewById(R.id.searchEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        semesterSpinner = findViewById(R.id.semesterSpinner);
    }

    private void setupUI() {
        setupToolbar();
        setupRecyclerView();
        setupPaginationButtons();
        setupSearch();
        setupFilters();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Course Topics");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new TopicListAdapter(this, new ArrayList<>(), "ONLINE");
        adapter.setOnTopicClickListener(this::updateTopicViews);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
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
                    applyFiltersAndSearch();
                }
            }
        });
    }

    private void setupFilters() {
        setupCategoryFilter();
        setupSemesterFilter();
    }

    private void setupCategoryFilter() {
        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        categories.addAll(Arrays.asList(topicCategories));

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newCategory = categories.get(position);
                if (!newCategory.equals(selectedCategory)) {
                    selectedCategory = newCategory;
                    applyFiltersAndSearch();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSemesterFilter() {
        // We'll populate this after loading topics to get unique semesters
        List<String> semesters = new ArrayList<>();
        semesters.add("All Semesters");

        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, semesters);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        semesterSpinner.setAdapter(semesterAdapter);

        semesterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newSemester = (String) parent.getItemAtPosition(position);
                if (!newSemester.equals(selectedSemester)) {
                    selectedSemester = newSemester;
                    applyFiltersAndSearch();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadTopicsFromFirestore() {
        if (isLoading) return;

        setLoadingState(true);
        allTopics.clear();
        filteredTopics.clear();
        currentPage = 0;

        topicRepository.loadTopicsOfCourse(courseId, new TopicRepository.Callback() {
            @Override
            public void onSuccess(List<Topic> topics) {
                allTopics.clear();
                allTopics.addAll(topics);

                Log.d(TAG, "Successfully loaded " + allTopics.size() + " topics");

                // Update semester filter with unique semesters from topics
                updateSemesterFilter();

                // Apply filters and display
                applyFiltersAndSearch();
                setLoadingState(false);
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Error loading topics: " + message);
                setLoadingState(false);
                showError("Failed to load topics: " + message);
                showEmptyState(true);
            }
        });
    }

    private void updateSemesterFilter() {
        List<String> semesters = new ArrayList<>();
        semesters.add("All Semesters");

        // Get unique semesters from topics
        for (Topic topic : allTopics) {
            if (topic.getSemester() != null && !topic.getSemester().isEmpty()) {
                if (!semesters.contains(topic.getSemester())) {
                    semesters.add(topic.getSemester());
                }
            }
        }

        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, semesters);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        semesterSpinner.setAdapter(semesterAdapter);
    }

    private void applyFiltersAndSearch() {
        filteredTopics.clear();

        for (Topic topic : allTopics) {
            if (matchesFilters(topic)) {
                filteredTopics.add(topic);
            }
        }

        // Reset to first page and recalculate pagination
        currentPage = 0;
        calculatePagination();
        displayCurrentPage();
    }

    private boolean matchesFilters(Topic topic) {
        // Search filter
        if (!currentSearchQuery.isEmpty()) {
            if (!topicMatchesSearch(topic, currentSearchQuery.toLowerCase())) {
                return false;
            }
        }

        // Category filter
        if (!selectedCategory.equals("All Categories")) {
            if (topic.getCategories() == null ||
                    !topic.getCategories().toLowerCase().contains(selectedCategory.toLowerCase())) {
                return false;
            }
        }

        // Semester filter
        if (!selectedSemester.equals("All Semesters")) {
            if (topic.getSemester() == null ||
                    !topic.getSemester().equals(selectedSemester)) {
                return false;
            }
        }

        return true;
    }

    private boolean topicMatchesSearch(Topic topic, String searchQuery) {
        if (topic == null || searchQuery == null || searchQuery.isEmpty()) {
            return true;
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

        return false;
    }

    private void calculatePagination() {
        totalItems = filteredTopics.size();
        totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

        if (totalPages == 0) {
            totalPages = 1;
        }

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
            adapter.updateTopics(pageTopics);
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
        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(0);
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
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void updateTopicViews(Topic topic) {
        if (topic == null) return;

        // Update views in TopicRepository
        topicRepository.updateTopicViews(courseId, topic.getOrderIndex(), new TopicRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Topic views updated successfully");
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Failed to update topic views: " + message);
            }
        });

        // Update user's course progress
        userRepository.addViewedTopic(courseId, topic.getOrderIndex(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(com.sowp.user.models.User user) {
                Log.d(TAG, "User progress updated successfully");
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Failed to update user progress: " + message);
            }
        });
    }

    public void refreshTopics() {
        loadTopicsFromFirestore();
    }

    public void clearFilters() {
        searchEditText.setText("");
        categorySpinner.setSelection(0);
        semesterSpinner.setSelection(0);
        currentSearchQuery = "";
        selectedCategory = "All Categories";
        selectedSemester = "All Semesters";
        applyFiltersAndSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from topic view
        if (!allTopics.isEmpty()) {
            loadTopicsFromFirestore();
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
        if (adapter != null) {
            // Clean up adapter if needed
        }
    }
}