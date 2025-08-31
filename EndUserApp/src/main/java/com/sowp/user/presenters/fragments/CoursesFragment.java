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
    private List<Course> filteredCourses = new ArrayList<>();
    private boolean isLoading = false;
    private static final int PAGE_SIZE = 10;

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

        filterLayout.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        courseAdapter = new CourseAdapter(getContext(), filteredCourses, this);
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

                    if (lastVisibleItem >= totalItemCount - 5) {
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
                currentSearchQuery = s.toString();
                applyFiltersAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        filterToggleButton.setOnClickListener(v -> {
            if (filterLayout.getVisibility() == View.GONE) {
                filterLayout.setVisibility(View.VISIBLE);
                filterToggleButton.setText("Hide Filters");
            } else {
                filterLayout.setVisibility(View.GONE);
                filterToggleButton.setText("Show Filters");
            }
        });

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

        refreshButton.setOnClickListener(v -> {
            allCourses.clear();
            filteredCourses.clear();
            courseAdapter.notifyDataSetChanged();
            loadCourses();
        });
    }

    private void loadCourses() {
        if (isLoading) return;

        showLoading(true);
        isLoading = true;

        courseRepository.loadCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allCourses.clear();
                        allCourses.addAll(courses);
                        applyFiltersAndSearch();
                        showLoading(false);
                        isLoading = false;
                        updateResultsCount();
                    });
                }
            }

            @Override
            public void onFailure(String message) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        isLoading = false;
                        showError(message);
                    });
                }
            }
        });
    }

    private void loadMoreCourses() {
        List<Course> allFiltered = getAllFilteredCourses();
        if (filteredCourses.size() < allFiltered.size()) {
            int currentSize = filteredCourses.size();
            int endIndex = Math.min(currentSize + PAGE_SIZE, allFiltered.size());

            List<Course> newCourses = allFiltered.subList(currentSize, endIndex);
            filteredCourses.addAll(newCourses);
            courseAdapter.notifyItemRangeInserted(currentSize, newCourses.size());
            updateResultsCount();
        }
    }

    private void applyFiltersAndSearch() {
        List<Course> filtered = getAllFilteredCourses();

        filteredCourses.clear();

        int endIndex = Math.min(PAGE_SIZE, filtered.size());
        if (endIndex > 0) {
            filteredCourses.addAll(filtered.subList(0, endIndex));
        }

        courseAdapter.updateData(filteredCourses);
        updateResultsCount();

        emptyStateLayout.setVisibility(filteredCourses.isEmpty() ? View.VISIBLE : View.GONE);
        coursesRecyclerView.setVisibility(filteredCourses.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private List<Course> getAllFilteredCourses() {
        return allCourses.stream()
                .filter(course -> {
                    if (course == null) return false;

                    boolean matchesSearch = currentSearchQuery.isEmpty() ||
                            (course.getTitle() != null && course.getTitle().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                            (course.getDescription() != null && course.getDescription().toLowerCase().contains(currentSearchQuery.toLowerCase())) ||
                            (course.getInstructor() != null && course.getInstructor().toLowerCase().contains(currentSearchQuery.toLowerCase()));

                    boolean matchesCategory = selectedCategory.equals("All") ||
                            (course.getCategoryArray() != null && course.getCategoryArray().contains(selectedCategory));

                    boolean matchesLevel = selectedLevel.equals("All") ||
                            (course.getLevel() != null && selectedLevel.equals(course.getLevel()));

                    boolean matchesPublic = !showOnlyPublic || course.isPublic();

                    return matchesSearch && matchesCategory && matchesLevel && matchesPublic;
                })
                .collect(Collectors.toList());
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

    private void updateResultsCount() {
        int totalFiltered = getAllFilteredCourses().size();
        resultsCountTextView.setText(String.format("Showing %d of %d courses",
                filteredCourses.size(), totalFiltered));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        coursesRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCourseClick(Course course) {
        if (course != null && course.getId() != 0) {
            Intent intent = new Intent(getContext(), Description.class);
            intent.putExtra("ID", course.getId());
            startActivity(intent);
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
    }
}