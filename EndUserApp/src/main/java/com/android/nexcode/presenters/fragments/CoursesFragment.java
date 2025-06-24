package com.android.nexcode.presenters.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.nexcode.R;
import com.android.nexcode.adapters.CourseAdapter;
import com.android.nexcode.models.Course;
import com.android.nexcode.repositories.firebase.CourseRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class CoursesFragment extends Fragment {
    private SearchView searchView;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TabLayout tabLayout;
    private ConstraintLayout emptyStateLayout;
    private FloatingActionButton filterFab;
    private CourseAdapter adapter;
    private List<Course> allCourses = new ArrayList<>();
    private List<Course> favoriteCourses = new ArrayList<>();
    private List<Course> offlineCourses = new ArrayList<>();
    private String currentQuery = "";
    private int currentTabPosition = 0;
    private CourseRepository courseRepository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courses, container, false);

        // Initialize views
        searchView = view.findViewById(R.id.searchView);
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tabLayout = view.findViewById(R.id.tabLayout);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        filterFab = view.findViewById(R.id.filterFab);
        Button exploreButton = view.findViewById(R.id.emptyStateButton);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize adapter with empty list
        adapter = new CourseAdapter(requireContext(), allCourses, "ONLINE");
        recyclerView.setAdapter(adapter);
        courseRepository = new CourseRepository(getContext());

        // Load default course data
        loadCourseData();

        // Set up SearchView
        setupSearchView();

        // Setup SwipeRefreshLayout
        setupSwipeRefresh();

        // Setup TabLayout
        setupTabLayout();

        // Setup FloatingActionButton
        setupFilterFab();

        // Setup EmptyState button
        exploreButton.setOnClickListener(v -> {
            // Reset filters and search
            searchView.setQuery("", false);
            tabLayout.selectTab(tabLayout.getTabAt(0));
            filterCoursesWithCurrentSettings();
        });

        return view;
    }

    private void loadCourseData() {

        courseRepository.loadCourses(new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                allCourses = courses;
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });



        // Apply initial filter
        filterCoursesWithCurrentSettings();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query;
                filterCoursesWithCurrentSettings();
                searchView.clearFocus(); // Dismiss the keyboard
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                filterCoursesWithCurrentSettings();
                return true;
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Reload default data
            loadCourseData();
            swipeRefreshLayout.setRefreshing(false);
        });

        swipeRefreshLayout.setColorSchemeResources(
                R.color.accent_color,
                R.color.primary_color,
                R.color.primary_dark
        );
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                filterCoursesWithCurrentSettings();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    private void setupFilterFab() {
        filterFab.setOnClickListener(v -> {
            // Show filter dialog
            showFilterDialog();
        });
    }

    private void showFilterDialog() {
        // Implement filter dialog logic here
        // This can be a BottomSheetDialog with options like:
        // - Sort by (newest, oldest, duration, members)
        // - Filter by category (Programming, Design, Data Science, etc.)
        // - Filter by public/private status
        // - Filter by duration range
    }

    private void filterCoursesWithCurrentSettings() {
        List<Course> filteredList = new ArrayList<>();

        // First apply tab filters
        List<Course> tabFilteredList = filterByTab();

        // Then apply search query
        if (currentQuery.isEmpty()) {
            filteredList = tabFilteredList;
        } else {
            for (Course course : tabFilteredList) {
                if (matchesSearchQuery(course, currentQuery)) {
                    filteredList.add(course);
                }
            }
        }

        adapter.updateData(filteredList);
        showEmptyState(filteredList.isEmpty());
    }

    private boolean matchesSearchQuery(Course course, String query) {
        String lowerQuery = query.toLowerCase();
        return course.getTitle().toLowerCase().contains(lowerQuery) ||
                course.getCategory().toLowerCase().contains(lowerQuery) ||
                course.getDescription().toLowerCase().contains(lowerQuery);
    }

    private List<Course> filterByTab() {
        switch (currentTabPosition) {
            case 0: // "All" tab
                return new ArrayList<>(allCourses);
            case 1: // "Favorite" tab
                return new ArrayList<>(favoriteCourses);
            case 2: // "Offline" tab
                return new ArrayList<>(offlineCourses);
            default:
                return new ArrayList<>(allCourses);
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        filterCoursesWithCurrentSettings();
    }
}