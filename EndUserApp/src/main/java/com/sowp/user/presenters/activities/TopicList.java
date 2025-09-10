package com.sowp.user.presenters.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sowp.user.R;
import com.sowp.user.repositories.TopicRepository;
import com.sowp.user.repositories.UserRepository;
import com.sowp.user.adapters.TopicListAdapter;
import com.sowp.user.models.Topic;

import java.util.ArrayList;
import java.util.List;

public class TopicList extends AppCompatActivity {
    private static final int ITEMS_PER_PAGE = 10;

    // UI Components
    private RecyclerView recyclerView;
    private View emptyView;
    private ProgressBar progressBar;
    private TextView pageInfo;
    private MaterialButton btnPrevious, btnNext;
    private TextInputEditText searchEditText;
    private Spinner categorySpinner;
    private Toolbar toolbar;

    // Data and Repository
    private TopicListAdapter adapter;
    private TopicRepository topicRepository;
    private UserRepository userRepository;
    private int courseId;
    private List<String> topicCategories = new ArrayList<>();

    // Topic lists
    private List<Topic> allTopics = new ArrayList<>();
    private List<Topic> filteredTopics = new ArrayList<>();

    // Pagination
    private int currentPage = 0;
    private int totalPages = 0;
    private int totalItems = 0;
    private boolean isLoading = false;

    // Filter states
    private String currentSearchQuery = "";
    private String selectedCategory = "All Categories";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_list);

        initializeRepositories();
        extractIntentData();
        initializeUI();
        loadTopics();
    }

    private void initializeRepositories() {
        topicRepository = new TopicRepository();
        userRepository = new UserRepository(this);
    }

    private void extractIntentData() {
        courseId = getIntent().getIntExtra("courseID", 0);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Course")
                .document(String.valueOf(courseId))
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        topicCategories = (List<String>) task.getResult().get("topicCategories");
                        setupCategoryFilter();
                    }
                });
    }

    private void initializeUI() {
        findViews();
        setupToolbar();
        setupRecyclerView();
        setupPagination();
        setupSearch();
    }

    private void findViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.empty_view);
        progressBar = findViewById(R.id.progressBar);
        pageInfo = findViewById(R.id.pageInfo);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        searchEditText = findViewById(R.id.searchEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
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
        adapter = new TopicListAdapter(this, new ArrayList<>());
        adapter.setOnTopicClickListener(this::onTopicClicked);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupPagination() {
        btnPrevious.setOnClickListener(v -> {
            if (canNavigateToPreviousPage()) {
                currentPage--;
                displayCurrentPage();
                scrollToTop();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (canNavigateToNextPage()) {
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
                String query = s.toString().trim();
                if (!query.equals(currentSearchQuery)) {
                    currentSearchQuery = query;
                    applyFiltersAndSearch();
                }
            }
        });
    }


    private void setupCategoryFilter() {
        List<String> categories = createCategoryList();
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

    private List<String> createCategoryList() {
        List<String> categories = new ArrayList<>();
        categories.addAll(topicCategories);
        return categories;
    }


    private void loadTopics() {
        if (isLoading) return;

        setLoadingState(true);
        clearTopicLists();

        topicRepository.loadTopicsOfCourse(courseId, new TopicRepository.Callback() {
            @Override
            public void onSuccess(List<Topic> topics) {
                handleTopicsLoadSuccess(topics);
            }

            @Override
            public void onFailure(String message) {
                handleTopicsLoadFailure();
            }
        });
    }

    private void clearTopicLists() {
        allTopics.clear();
        filteredTopics.clear();
        currentPage = 0;
    }

    private void handleTopicsLoadSuccess(List<Topic> topics) {
        allTopics.clear();
        allTopics.addAll(topics);
        applyFiltersAndSearch();
        setLoadingState(false);
    }

    private void handleTopicsLoadFailure() {
        setLoadingState(false);
        showEmptyState(true);
    }


    private void applyFiltersAndSearch() {
        filteredTopics.clear();

        for (Topic topic : allTopics) {
            if (topicMatchesAllFilters(topic)) {
                filteredTopics.add(topic);
            }
        }

        resetPaginationAndDisplay();
    }

    private boolean topicMatchesAllFilters(Topic topic) {
        return matchesSearchQuery(topic) &&
                matchesCategoryFilter(topic);
    }

    private boolean matchesSearchQuery(Topic topic) {
        if (currentSearchQuery.isEmpty()) return true;

        String query = currentSearchQuery.toLowerCase();
        return (topic.getName() != null && topic.getName().toLowerCase().contains(query)) ||
                (topic.getDescription() != null && topic.getDescription().toLowerCase().contains(query)) ||
                (topic.getTags() != null && topic.getTags().toLowerCase().contains(query)) ||
                (topic.getCategories() != null && topic.getCategories().toLowerCase().contains(query));
    }

    private boolean matchesCategoryFilter(Topic topic) {
        if (selectedCategory.equals("All Categories")) return true;

        return topic.getCategories() != null &&
                topic.getCategories().toLowerCase().contains(selectedCategory.toLowerCase());
    }



    private void resetPaginationAndDisplay() {
        currentPage = 0;
        calculatePaginationData();
        displayCurrentPage();
    }

    private void calculatePaginationData() {
        totalItems = filteredTopics.size();
        totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));

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
        updatePageInfo();
        updateNavigationButtons();
    }

    private void updatePageInfo() {
        if (totalItems == 0) {
            pageInfo.setText("No topics found");
        } else {
            String info = String.format("Page %d of %d (%d %s)",
                    currentPage + 1, totalPages, totalItems,
                    totalItems == 1 ? "topic" : "topics");
            pageInfo.setText(info);
        }
    }

    private void updateNavigationButtons() {
        btnPrevious.setEnabled(canNavigateToPreviousPage());
        btnNext.setEnabled(canNavigateToNextPage());
    }

    private boolean canNavigateToPreviousPage() {
        return currentPage > 0;
    }

    private boolean canNavigateToNextPage() {
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

    private void onTopicClicked(Topic topic) {
        if (topic == null) return;

        // Update topic view count
        updateTopicViewCount(topic);

        // Update user's viewed topics (handled by adapter as well, but keeping for redundancy)
        updateUserViewedTopics(topic);
    }

    private void updateTopicViewCount(Topic topic) {
        topicRepository.updateTopicViews(courseId, topic.getOrderIndex(), new TopicRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                // Topic view count updated
            }

            @Override
            public void onFailure(String message) {
                // Handle silently
            }
        });
    }

    private void updateUserViewedTopics(Topic topic) {
        userRepository.addViewedTopic(courseId, topic.getOrderIndex(), new UserRepository.UserCallback() {
            @Override
            public void onSuccess(com.sowp.user.models.User user) {
                // Successfully tracked
            }

            @Override
            public void onFailure(String message) {
                // Handle silently
            }
        });
    }

    // Public methods for external control
    public void refreshTopics() {
        loadTopics();
    }

    public void clearFilters() {
        searchEditText.setText("");
        categorySpinner.setSelection(0);
        currentSearchQuery = "";
        selectedCategory = "All Categories";
        applyFiltersAndSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!allTopics.isEmpty()) {
            loadTopics();
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
        // Cleanup if needed
    }
}