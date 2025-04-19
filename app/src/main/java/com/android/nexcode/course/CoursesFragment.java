package com.android.nexcode.course;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.nexcode.R;
import com.android.nexcode.database.AppDatabase;
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
    private LiveData<List<Course>> courses;
    private List<Course> allCourses = new ArrayList<>();
    private String currentQuery = "";
    private int currentTabPosition = 0;

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
        adapter = new CourseAdapter(requireContext(), allCourses);
        recyclerView.setAdapter(adapter);

        // Observe LiveData changes
        loadCourses();

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
            loadCourses();
        });

        return view;
    }

    private void loadCourses() {
        courses = AppDatabase.getInstance(requireContext()).courseDao().getAllCoursesLive();
        courses.observe(getViewLifecycleOwner(), coursesList -> {
            if (coursesList == null || coursesList.isEmpty()) {
                allCourses.clear();
                adapter.updateData(new ArrayList<>());
                showEmptyState(true);
            } else {
                allCourses = new ArrayList<>(coursesList);
                filterCoursesWithCurrentSettings();
                showEmptyState(false);
            }
        });
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
            // Reload data from database
            loadCourses();
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
        // - Sort by (newest, highest rated, etc.)
        // - Filter by category
        // - Filter by difficulty level
    }

    private void filterCoursesWithCurrentSettings() {
        List<Course> filteredList = new ArrayList<>();

        // First apply tab filters
        List<Course> tabFilteredList = filterByTab(allCourses);

        // Then apply search query
        if (currentQuery.isEmpty()) {
            filteredList = tabFilteredList;
        } else {
            for (Course course : tabFilteredList) {
                if (course.getTitle().toLowerCase().contains(currentQuery.toLowerCase())) {
                    filteredList.add(course);
                }
            }
        }

        adapter.updateData(filteredList);
        showEmptyState(filteredList.isEmpty());
    }

    private List<Course> filterByTab(List<Course> courses) {
        if (currentTabPosition == 0) {
            // "All" tab
            return new ArrayList<>(courses);
        } else if (currentTabPosition == 1) {
            // "In Progress" tab
            List<Course> inProgressCourses = new ArrayList<>();
            for (Course course : courses) {
                if (course.getProgress() > 0 && course.getProgress() < 100) {
                    inProgressCourses.add(course);
                }
            }
            return inProgressCourses;
        } else {
            // "Completed" tab
            List<Course> completedCourses = new ArrayList<>();
            for (Course course : courses) {
                if (course.getProgress() == 100) {
                    completedCourses.add(course);
                }
            }
            return completedCourses;
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
        loadCourses();
    }
}