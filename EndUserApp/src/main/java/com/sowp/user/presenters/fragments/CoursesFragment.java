package com.sowp.user.presenters.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.adapters.CourseAdapter;
import com.sowp.user.R;
import com.sowp.user.models.Course;
import com.sowp.user.presenters.activities.CourseDescriptionActivity;
import com.sowp.user.presenters.activities.Main;
import com.sowp.user.repositories.CourseRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CoursesFragment extends Fragment implements CourseAdapter.OnCourseClickListener, DefaultLifecycleObserver {

    private static final int PAGE_SIZE = 10;

    private EditText searchEditText;
    private Spinner categorySpinner;
    private Spinner semesterSpinner;
    private Spinner levelSpinner;
    private CheckBox publicOnlyCheckBox;
    private Button clearFiltersButton;
    private TextView resultsCountTextView;
    private ProgressBar progressBar;
    private RecyclerView coursesRecyclerView;
    private LinearLayout emptyStateLayout;
    private LinearLayout paginationLayout;
    private Button btnPrevious;
    private Button btnNext;
    private TextView pageInfo;

    private CourseAdapter courseAdapter;
    private CourseRepository courseRepository;
    private List<Course> allCourses = new ArrayList<>();
    private List<Course> filteredCourses = new ArrayList<>();
    private List<Course> displayedCourses = new ArrayList<>();
    private boolean isLoading = false;

    private String currentSearchQuery = "";
    private String selectedCategory = "All";
    private String selectedSemester = "All";
    private String selectedLevel = "All";
    private boolean showOnlyPublic = false;

    // Pagination
    private int currentPage = 1;
    private int totalPages = 1;

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
        setupSpinners();
        setupRecyclerView();
        setupSearchAndFilters();
        setupPagination();
        loadCourses();

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        ((Main) requireActivity()).bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    }
                }
        );
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
        categorySpinner = view.findViewById(R.id.categorySpinner);
        semesterSpinner = view.findViewById(R.id.semesterSpinner);
        levelSpinner = view.findViewById(R.id.levelSpinner);
        publicOnlyCheckBox = view.findViewById(R.id.publicOnlyCheckBox);
        clearFiltersButton = view.findViewById(R.id.clearFiltersButton);
        resultsCountTextView = view.findViewById(R.id.resultsCountTextView);
        progressBar = view.findViewById(R.id.progressBar);
        coursesRecyclerView = view.findViewById(R.id.coursesRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        paginationLayout = view.findViewById(R.id.paginationLayout);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
        pageInfo = view.findViewById(R.id.pageInfo);
    }

    private void setupSpinners() {
        // Category Spinner
        String[] categories = {
                "All", "Programming", "Non Programming", "Networking",
                "Database", "Major", "Minor", "Web Development",
                "Mobile Development", "Data Science", "AI/ML",
                "Cybersecurity", "Software Engineering"
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(categoryAdapter);

        // Semester Spinner
        String[] semesters = {
                "All", "1st Semester", "2nd Semester", "3rd Semester",
                "4th Semester", "5th Semester", "6th Semester",
                "7th Semester", "8th Semester"
        };
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, semesters);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        semesterSpinner.setAdapter(semesterAdapter);

        // Level Spinner
        String[] levels = {"All", "Beginner", "Intermediate", "Advanced"};
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, levels);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(levelAdapter);
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(getContext(), displayedCourses, this);
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        coursesRecyclerView.setAdapter(courseAdapter);
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

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        semesterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSemester = parent.getItemAtPosition(position).toString();
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLevel = parent.getItemAtPosition(position).toString();
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        publicOnlyCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showOnlyPublic = isChecked;
            applyFiltersAndSearch();
        });

        clearFiltersButton.setOnClickListener(v -> clearFilters());
    }

    private void setupPagination() {
        btnPrevious.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updateDisplayedCourses();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                updateDisplayedCourses();
            }
        });
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

        filteredCourses = getFilteredCourses();
        currentPage = 1;
        calculatePagination();
        updateDisplayedCourses();
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
                courseMatchesCategory(course, selectedCategory);

        boolean matchesSemester = selectedSemester.equals("All") ||
                (course.getSemester() != null && selectedSemester.equals(course.getSemester()));

        boolean matchesLevel = selectedLevel.equals("All") ||
                (course.getLevel() != null && selectedLevel.equals(course.getLevel()));

        boolean matchesPublic = !showOnlyPublic || course.isPublic();

        return matchesSearch && matchesCategory && matchesSemester && matchesLevel && matchesPublic;
    }

    private boolean courseMatchesSearchQuery(Course course, String query) {
        String lowerQuery = query.toLowerCase();

        // Search in title
        if (course.getTitle() != null && course.getTitle().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in short title
        if (course.getShortTitle() != null && course.getShortTitle().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in description
        if (course.getDescription() != null && course.getDescription().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in instructor
        if (course.getInstructor() != null && course.getInstructor().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in course code
        if (course.getCourseCode() != null && course.getCourseCode().toLowerCase().contains(lowerQuery)) {
            return true;
        }

        // Search in tags
        if (course.getTags() != null) {
            for (String tag : course.getTags()) {
                if (tag != null && tag.toLowerCase().contains(lowerQuery)) {
                    return true;
                }
            }
        }

        // Search in category array
        if (course.getCategoryArray() != null) {
            for (String category : course.getCategoryArray()) {
                if (category != null && category.toLowerCase().contains(lowerQuery)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean courseMatchesCategory(Course course, String selectedCategory) {
        if (course.getCategoryArray() != null && !course.getCategoryArray().isEmpty()) {
            return course.getCategoryArray().contains(selectedCategory);
        }
        return false;
    }

    private void calculatePagination() {
        totalPages = (int) Math.ceil((double) filteredCourses.size() / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
    }

    private void updateDisplayedCourses() {
        displayedCourses.clear();

        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, filteredCourses.size());

        if (startIndex < filteredCourses.size()) {
            displayedCourses.addAll(filteredCourses.subList(startIndex, endIndex));
        }

        if (courseAdapter != null) {
            courseAdapter.notifyDataSetChanged();
        }

        updatePaginationControls();
    }

    private void updatePaginationControls() {
        if (totalPages > 1) {
            paginationLayout.setVisibility(View.VISIBLE);

            btnPrevious.setEnabled(currentPage > 1);
            btnNext.setEnabled(currentPage < totalPages);

            pageInfo.setText(String.format("Page %d of %d", currentPage, totalPages));
        } else {
            paginationLayout.setVisibility(View.GONE);
        }
    }

    private void updateUI() {
        updateResultsCount();
        updateEmptyState();
    }

    private void updateResultsCount() {
        if (resultsCountTextView != null) {
            String countText = String.format("Showing %d of %d courses",
                    displayedCourses.size(), filteredCourses.size());
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
        semesterSpinner.setSelection(0);
        levelSpinner.setSelection(0);
        publicOnlyCheckBox.setChecked(false);

        currentSearchQuery = "";
        selectedCategory = "All";
        selectedSemester = "All";
        selectedLevel = "All";
        showOnlyPublic = false;

        applyFiltersAndSearch();
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;

        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }

        if (coursesRecyclerView != null) {
            coursesRecyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        }

        if (clearFiltersButton != null) {
            clearFiltersButton.setEnabled(!loading);
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
        if (filteredCourses != null) {
            filteredCourses.clear();
        }
        if (displayedCourses != null) {
            displayedCourses.clear();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        searchEditText = null;
        categorySpinner = null;
        semesterSpinner = null;
        levelSpinner = null;
        publicOnlyCheckBox = null;
        clearFiltersButton = null;
        resultsCountTextView = null;
        progressBar = null;
        coursesRecyclerView = null;
        emptyStateLayout = null;
        paginationLayout = null;
        btnPrevious = null;
        btnNext = null;
        pageInfo = null;
        courseAdapter = null;
        courseRepository = null;

        cleanup();
        getLifecycle().removeObserver(this);
    }
}