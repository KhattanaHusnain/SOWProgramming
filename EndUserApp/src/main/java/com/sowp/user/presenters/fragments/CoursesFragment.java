package com.sowp.user.presenters.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.adapters.CourseAdapter;
import com.sowp.user.R;
import com.sowp.user.models.Course;
import com.sowp.user.presenters.activities.CourseDescriptionActivity;
import com.sowp.user.repositories.CourseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CoursesFragment extends Fragment implements CourseAdapter.OnCourseClickListener, DefaultLifecycleObserver {

    private static final int PAGE_SIZE = 10;

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

    private CourseAdapter courseAdapter;
    private CourseRepository courseRepository;
    private List<Course> allCourses = new ArrayList<>();
    private List<Course> displayedCourses = new ArrayList<>();
    private boolean isLoading = false;

    private String currentSearchQuery = "";
    private String selectedCategory = "All";
    private String selectedLevel = "All";
    private boolean showOnlyPublic = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(this);
    }

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

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onResume(owner);
        if (displayedCourses.isEmpty()) {
            loadCourses();
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onPause(owner);
        isLoading = false;
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onDestroy(owner);
        cleanup();
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

        filterLayout.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(getContext(), displayedCourses, this);
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        coursesRecyclerView.setAdapter(courseAdapter);

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

        filterToggleButton.setOnClickListener(v -> toggleFilters());

        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        levelSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedLevel = parent.getItemAtPosition(position).toString();
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        publicOnlyCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showOnlyPublic = isChecked;
            applyFiltersAndSearch();
        });

        clearFiltersButton.setOnClickListener(v -> clearFilters());
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
            return;
        }

        setLoadingState(true);

        courseRepository.loadCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        handleCoursesLoaded(courses);
                    });
                }
            }

            @Override
            public void onFailure(String message) {
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        handleLoadFailure();
                    });
                }
            }
        });
    }

    private void handleCoursesLoaded(List<Course> courses) {
        allCourses.clear();
        allCourses.addAll(courses);
        applyFiltersAndSearch();
        setLoadingState(false);
    }

    private void handleLoadFailure() {
        setLoadingState(false);
    }

    private void applyFiltersAndSearch() {
        if (allCourses.isEmpty()) {
            updateUI();
            return;
        }

        List<Course> filteredCourses = getFilteredCourses();
        displayedCourses.clear();
        int endIndex = Math.min(PAGE_SIZE, filteredCourses.size());
        if (endIndex > 0) {
            displayedCourses.addAll(filteredCourses.subList(0, endIndex));
        }

        updateUI();
    }

    private List<Course> getFilteredCourses() {
        return allCourses.stream()
                .filter(this::courseMatchesFilters)
                .collect(Collectors.toList());
    }

    private boolean courseMatchesFilters(Course course) {
        if (course == null) return false;

        boolean matchesSearch = currentSearchQuery.isEmpty() ||
                courseMatchesSearchQuery(course, currentSearchQuery);

        boolean matchesCategory = selectedCategory.equals("All") ||
                (course.getCategoryArray() != null && course.getCategoryArray().contains(selectedCategory));

        boolean matchesLevel = selectedLevel.equals("All") ||
                (course.getLevel() != null && selectedLevel.equals(course.getLevel()));

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
        searchEditText.setText("");
        categorySpinner.setSelection(0);
        levelSpinner.setSelection(0);
        publicOnlyCheckBox.setChecked(false);

        currentSearchQuery = "";
        selectedCategory = "All";
        selectedLevel = "All";
        showOnlyPublic = false;

        applyFiltersAndSearch();
    }

    private void refreshCourses() {
        allCourses.clear();
        displayedCourses.clear();

        if (courseAdapter != null) {
            courseAdapter.notifyDataSetChanged();
        }

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

    @Override
    public void onCourseClick(Course course) {
        if (course != null && course.getId() != 0) {
            Intent intent = new Intent(getContext(), CourseDescriptionActivity.class);
            intent.putExtra("COURSE_ID", course.getId());
            startActivity(intent);
        }
    }

    private void cleanup() {
        isLoading = false;

        if (allCourses != null) {
            allCourses.clear();
        }
        if (displayedCourses != null) {
            displayedCourses.clear();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

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

        cleanup();
        getLifecycle().removeObserver(this);
    }
}