package com.android.nexcode.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.android.nexcode.database.AppDatabase;
import com.android.nexcode.course.Course;
import com.android.nexcode.course.CourseDao;
import com.android.nexcode.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final int GRID_SPAN_COUNT = 2;
    private static final String SEARCH_WILDCARD = "%%%s%%";

    private CourseDao courseDao;
    private UserProgressDao userProgressDao;
    private HomeAdapter coursesAdapter;
    private ContinueLearningAdapter continueAdapter;
    private LearningPathsAdapter pathsAdapter;
    private TabLayout categoryTabs;
    private TextInputEditText searchInput;
    private RecyclerView recyclerView;
    private RecyclerView continueRecyclerView;
    private RecyclerView pathsRecyclerView;
    private TextView userName;
    private TextView coursesCompleted;
    private TextView hoursSpent;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        setupRecyclerViews();
        loadUserData();
        loadCoursesFromDatabase();
        setupSearchAndFilters(view);

        return view;
    }

    private void initializeViews(View view) {
        // Get DAOs
        AppDatabase db = AppDatabase.getInstance(requireContext());
        courseDao = db.courseDao();
        userProgressDao = db.userProgressDao(); // Add this DAO to your AppDatabase

        // User profile and dashboard views
        userName = view.findViewById(R.id.user_name);
        coursesCompleted = view.findViewById(R.id.courses_completed);
        hoursSpent = view.findViewById(R.id.hours_spent);

        // Search input
        searchInput = view.findViewById(R.id.search_input);

        // Category tabs
        categoryTabs = view.findViewById(R.id.category_tabs);

        // Recycler views
        recyclerView = view.findViewById(R.id.recycler_view);
        continueRecyclerView = view.findViewById(R.id.continue_learning_recycler);
        pathsRecyclerView = view.findViewById(R.id.learning_paths_recycler);

        // Setup "See All" buttons
        view.findViewById(R.id.see_all_continue).setOnClickListener(v -> navigateToAllCourses("continue"));
        view.findViewById(R.id.see_all_popular).setOnClickListener(v -> navigateToAllCourses("popular"));
        view.findViewById(R.id.see_all_paths).setOnClickListener(v -> navigateToAllCourses("paths"));

        // Setup continue learning button
        view.findViewById(R.id.btn_continue_learning).setOnClickListener(v -> resumeLastCourse());
    }

    private void setupRecyclerViews() {
        // Setup Popular Courses RecyclerView
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), GRID_SPAN_COUNT));
        coursesAdapter = new HomeAdapter(requireContext(), new ArrayList<>());
        recyclerView.setAdapter(coursesAdapter);

        // Setup Continue Learning RecyclerView
        continueRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        continueAdapter = new ContinueLearningAdapter(requireContext(), new ArrayList<>());
        continueAdapter.setOnProgressItemClickListener(progress -> {
            // Handle click on a user progress item
            // For example, open the course at the specific module/position
            openCourseAtProgress(progress);
        });
        continueRecyclerView.setAdapter(continueAdapter);

        // Setup Learning Paths RecyclerView
        pathsRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        pathsAdapter = new LearningPathsAdapter(requireContext(), new ArrayList<>());
        pathsRecyclerView.setAdapter(pathsAdapter);
    }

    private void loadUserData() {
        // In a real app, this would come from user repository or preferences
        userName.setText("Hey, Alex!");

        // Get course completion stats from user progress
        userProgressDao.getCompletedCoursesCount().observe(getViewLifecycleOwner(), count ->
                coursesCompleted.setText(String.valueOf(count)));

        userProgressDao.getTotalHoursSpent().observe(getViewLifecycleOwner(), hours ->
                hoursSpent.setText(String.valueOf(hours)));

        // You would also update the progress bar here
        // Example: weeklyProgress.setProgress(75);
    }

    private void setupSearchAndFilters(View view) {
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchInput.getText().toString().trim();
            if (!query.isEmpty()) {
                observeSearchResults(courseDao.searchCourses(String.format(SEARCH_WILDCARD, query)));
                return true;
            }
            return false;
        });

        // Setup category tabs listener
        categoryTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterCoursesByCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Optionally refresh data
                filterCoursesByCategory(tab.getPosition());
            }
        });

        // Setup filter button click listener
        view.findViewById(R.id.filter_button).setOnClickListener(v -> showFilterDialog());
    }

    private void filterCoursesByCategory(int position) {
        switch (position) {
            case 0: // All
                observeCourses(courseDao.getAllCoursesLive());
                break;
            case 1: // Popular
                observeCourses(courseDao.getPopularCourses());
                break;
            case 2: // Programming
                observeCourses(courseDao.getProgrammingCourses());
                break;
            case 3: // Non-Programming
                observeCourses(courseDao.getNonProgrammingCourses());
                break;
            case 4: // New
                observeCourses(courseDao.getAllCoursesLive());
                break;
        }
    }

    private void loadCoursesFromDatabase() {
        // Load all three types of data
        observeCourses(courseDao.getAllCoursesLive());

        // Use user progress data for continue learning section
        observeUserProgress(userProgressDao.getUserInProgressCourses());

        //observeLearningPaths(courseDao.getLearningPaths());
    }

    private void observeCourses(LiveData<List<Course>> coursesLiveData) {
        coursesLiveData.observe(getViewLifecycleOwner(), courses ->
                coursesAdapter.updateCourses(courses));
    }

    private void observeUserProgress(LiveData<List<UserProgress>> progressLiveData) {
        progressLiveData.observe(getViewLifecycleOwner(), progressItems ->
                continueAdapter.updateProgressItems(progressItems));
    }

    private void observeLearningPaths(LiveData<List<LearningPath>> pathsLiveData) {
        pathsLiveData.observe(getViewLifecycleOwner(), paths ->
                pathsAdapter.updatePaths(paths));
    }

    private void observeSearchResults(LiveData<List<Course>> searchResults) {
        // Clear all adapters first
        coursesAdapter.updateCourses(new ArrayList<>());

        // Observe and update with search results
        searchResults.observe(getViewLifecycleOwner(), courses ->
                coursesAdapter.updateCourses(courses));
    }

    private void showFilterDialog() {
        // Show a dialog with filter options
        // This would be implemented in a real app
    }

    private void navigateToAllCourses(String type) {
        // Navigate to a screen showing all courses of a specific type
        // This would use Navigation Component or start a new activity
        // Based on the type parameter (continue, popular, paths)
    }

    private void resumeLastCourse() {
        // Navigate to the last accessed course
        // Get the most recently accessed progress item and open it
        userProgressDao.getLastAccessedCourse().observe(getViewLifecycleOwner(), progress -> {
            if (progress != null) {
                openCourseAtProgress(progress);
            }
        });
    }

    private void openCourseAtProgress(UserProgress progress) {
        // Navigate to course details with the specific progress information
        // In a real app, you would use Navigation Component or an Intent
        // to open the course at the specific module/position
    }
}