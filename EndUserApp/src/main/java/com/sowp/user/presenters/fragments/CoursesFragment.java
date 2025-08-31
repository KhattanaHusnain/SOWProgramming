package com.sowp.user.presenters.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.adapters.CourseAdapter;
import com.sowp.user.R;
import com.sowp.user.models.Course;
import com.sowp.user.presenters.activities.Description;
import com.sowp.user.repositories.firebase.CourseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoursesFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private static final String TAG = "CoursesFragment";
    private static final int PAGE_SIZE = 10;

    // UI Components
    private EditText searchEditText;
    private Button filterToggleButton;
    private CardView filterLayout;
    private Spinner categorySpinner;
    private Spinner levelSpinner;
    private CheckBox publicOnlyCheckBox;
    private Button clearFiltersButton;
    private Button refreshButton;
    private TextView resultsCountTextView;
    private ProgressBar progressBar;
    private RecyclerView coursesRecyclerView;
    private LinearLayout emptyStateLayout;

    // Data and State
    private CourseAdapter courseAdapter;
    private CourseRepository courseRepository;
    private List<Course> allCourses = new ArrayList<>();
    private List<Course> displayedCourses = new ArrayList<>();
    private boolean isLoading = false;

    // Filter State
    private String currentSearchQuery = "";
    private String selectedCategory = "All";
    private String selectedLevel = "All";
    private boolean showOnlyPublic = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_courses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupSearchAndFilters();
        loadCourses();
    }

    private void initializeViews(View view) {
        courseRepository = new CourseRepository(getContext());

        searchEditText = view.findViewById(R.id.searchEditText);
        filterToggleButton = view.findViewById(R.id.filterToggleButton);
        filterLayout = view.findViewById(R.id.filterLayout);
        categorySpinner = view.findViewById(R.id.categorySpinner);
        levelSpinner = view.findViewById(R.id.levelSpinner);
        publicOnlyCheckBox = view.findViewById(R.id.publicOnlyCheckBox);
        clearFiltersButton = view.findViewById(R.id.clearFiltersButton);
        refreshButton = view.findViewById(R.id.refreshButton);
        resultsCountTextView = view.findViewById(R.id.resultsCountTextView);
        progressBar = view.findViewById(R.id.progressBar);
        coursesRecyclerView = view.findViewById(R.id.coursesRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        // Initially hide filter layout
        filterLayout.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(getContext(), displayedCourses, this);
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        coursesRecyclerView.setAdapter(courseAdapter);

        // Add scroll listener for pagination
        coursesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading) {
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();

                    if (lastVisibleItem >= totalItemCount - 5 && totalItemCount > 0) {
                        loadMoreCourses();
                    }
                }
            }
        });
    }

    private void setupSearchAndFilters() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                applyFiltersAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter toggle
        filterToggleButton.setOnClickListener(v -> toggleFilters());

        // Category filter
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Level filter
        levelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedLevel = parent.getItemAtPosition(position).toString();
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Public only filter
        publicOnlyCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showOnlyPublic = isChecked;
            applyFiltersAndSearch();
        });

        // Clear filters button
        clearFiltersButton.setOnClickListener(v -> clearFilters());

        // Refresh button
        refreshButton.setOnClickListener(v -> refreshCourses());
    }

    private void toggleFilters() {
        if (filterLayout.getVisibility() == View.GONE) {
            filterLayout.setVisibility(View.VISIBLE);
            filterToggleButton.setText("Hide Filters");
        } else {
            filterLayout.setVisibility(View.GONE);
            filterToggleButton.setText("Show Filters");
        }
    }

    private void loadCourses() {
        if (isLoading) {
            Log.d(TAG, "Already loading, skipping request");
            return;
        }

        Log.d(TAG, "Starting to load courses");
        setLoadingState(true);

        courseRepository.loadCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                Log.d(TAG, "Courses loaded successfully: " + courses.size() + " courses");

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        handleCoursesLoaded(courses);
                    });
                }
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Failed to load courses: " + message);

                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        handleLoadFailure(message);
                    });
                }
            }
        });
    }

    private void handleCoursesLoaded(List<Course> courses) {
        allCourses.clear();
        allCourses.addAll(courses);

        Log.d(TAG, "All courses populated: " + allCourses.size());

        // Apply filters and update display
        applyFiltersAndSearch();
        setLoadingState(false);
    }

    private void handleLoadFailure(String message) {
        setLoadingState(false);
        showError("Failed to load courses: " + message);
    }

    private void applyFiltersAndSearch() {
        if (allCourses.isEmpty()) {
            Log.d(TAG, "No courses to filter");
            updateUI();
            return;
        }

        Log.d(TAG, "Applying filters - Total courses: " + allCourses.size());

        // Get all filtered courses
        List<Course> filteredCourses = getFilteredCourses();
        Log.d(TAG, "Filtered courses: " + filteredCourses.size());

        // Update displayed courses with pagination
        displayedCourses.clear();
        int endIndex = Math.min(PAGE_SIZE, filteredCourses.size());
        if (endIndex > 0) {
            displayedCourses.addAll(filteredCourses.subList(0, endIndex));
        }

        Log.d(TAG, "Displayed courses: " + displayedCourses.size());

        // Update UI
        updateUI();
    }

    private List<Course> getFilteredCourses() {
        return allCourses.stream()
                .filter(this::courseMatchesFilters)
                .collect(Collectors.toList());
    }

    private boolean courseMatchesFilters(Course course) {
        if (course == null) return false;

        // Search filter
        boolean matchesSearch = currentSearchQuery.isEmpty() ||
                courseMatchesSearchQuery(course, currentSearchQuery);

        // Category filter
        boolean matchesCategory = selectedCategory.equals("All") ||
                (course.getCategoryArray() != null && course.getCategoryArray().contains(selectedCategory));

        // Level filter
        boolean matchesLevel = selectedLevel.equals("All") ||
                (course.getLevel() != null && selectedLevel.equals(course.getLevel()));

        // Public filter
        boolean matchesPublic = !showOnlyPublic || course.isPublic();

        return matchesSearch && matchesCategory && matchesLevel && matchesPublic;
    }

    private boolean courseMatchesSearchQuery(Course course, String query) {
        String lowerQuery = query.toLowerCase();

        return (course.getTitle() != null && course.getTitle().toLowerCase().contains(lowerQuery)) ||
                (course.getDescription() != null && course.getDescription().toLowerCase().contains(lowerQuery)) ||
                (course.getInstructor() != null && course.getInstructor().toLowerCase().contains(lowerQuery));
    }

    private void loadMoreCourses() {
        if (isLoading) return;

        List<Course> allFiltered = getFilteredCourses();
        int currentSize = displayedCourses.size();

        if (currentSize < allFiltered.size()) {
            int endIndex = Math.min(currentSize + PAGE_SIZE, allFiltered.size());
            List<Course> newCourses = allFiltered.subList(currentSize, endIndex);

            int insertPosition = displayedCourses.size();
            displayedCourses.addAll(newCourses);

            courseAdapter.notifyItemRangeInserted(insertPosition, newCourses.size());
            updateResultsCount();

            Log.d(TAG, "Loaded more courses: " + newCourses.size() + ", Total displayed: " + displayedCourses.size());
        }
    }

    private void updateUI() {
        if (courseAdapter != null) {
            courseAdapter.notifyDataSetChanged();
        }

        updateResultsCount();
        updateEmptyState();
    }

    private void updateResultsCount() {
        if (resultsCountTextView != null) {
            int totalFiltered = getFilteredCourses().size();
            String countText = String.format("Showing %d of %d courses", displayedCourses.size(), totalFiltered);
            resultsCountTextView.setText(countText);
            Log.d(TAG, countText);
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = displayedCourses.isEmpty();

        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }

        if (coursesRecyclerView != null) {
            coursesRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void clearFilters() {
        // Reset UI components
        searchEditText.setText("");
        categorySpinner.setSelection(0);
        levelSpinner.setSelection(0);
        publicOnlyCheckBox.setChecked(false);

        // Reset filter state
        currentSearchQuery = "";
        selectedCategory = "All";
        selectedLevel = "All";
        showOnlyPublic = false;

        // Reapply filters (which will show all courses)
        applyFiltersAndSearch();
    }

    private void refreshCourses() {
        Log.d(TAG, "Refreshing courses");

        // Clear all data
        allCourses.clear();
        displayedCourses.clear();

        // Notify adapter of changes
        if (courseAdapter != null) {
            courseAdapter.notifyDataSetChanged();
        }

        // Reload courses
        loadCourses();
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;

        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }

        if (coursesRecyclerView != null) {
            coursesRecyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        }

        if (refreshButton != null) {
            refreshButton.setEnabled(!loading);
        }
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        Log.e(TAG, "Error: " + message);
    }

    @Override
    public void onCourseClick(Course course) {
        if (course != null && course.getId() != 0) {
            Log.d(TAG, "Course clicked: " + course.getTitle() + " (ID: " + course.getId() + ")");

            Intent intent = new Intent(getContext(), Description.class);
            intent.putExtra("COURSE_ID", course.getId());
            startActivity(intent);
        } else {
            Log.w(TAG, "Invalid course clicked");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up references
        searchEditText = null;
        filterToggleButton = null;
        filterLayout = null;
        categorySpinner = null;
        levelSpinner = null;
        publicOnlyCheckBox = null;
        clearFiltersButton = null;
        refreshButton = null;
        resultsCountTextView = null;
        progressBar = null;
        coursesRecyclerView = null;
        emptyStateLayout = null;
        courseAdapter = null;
        courseRepository = null;

        // Clear data lists
        if (allCourses != null) {
            allCourses.clear();
        }
        if (displayedCourses != null) {
            displayedCourses.clear();
        }
    }
}