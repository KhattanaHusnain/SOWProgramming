package com.android.nexcode;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.List;

public class HomeFragment extends Fragment {

    ImageButton menuButton;
    RecyclerView recyclerView;
    TextView btnAll, btnPopular, btnProgramming, btnNonProgramming;
    CourseAdapter adapter;
    SearchView searchView;

    private CourseDao courseDao;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Room database
        AppDatabase courseDatabase = AppDatabase.getInstance(getContext());
        courseDao = courseDatabase.courseDao();

        // Menu button reference
        menuButton = view.findViewById(R.id.menu_button);
        recyclerView = view.findViewById(R.id.recycler_view);
        btnAll = view.findViewById(R.id.btn_all);
        btnPopular = view.findViewById(R.id.btn_popular);
        btnProgramming = view.findViewById(R.id.btn_programming);
        btnNonProgramming = view.findViewById(R.id.btn_non_programming);
        searchView = view.findViewById(R.id.searchView);

        // Set OnClickListener for All Button
        setButtonListeners();
        setupSearchListener();

        // Create a GridLayoutManager with 2 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        // Load all courses from the database
        loadCoursesFromDatabase();

        return view;
    }

    private void loadCoursesFromDatabase() {
        LiveData<List<Course>> allCoursesLiveData = (LiveData<List<Course>>) courseDao.getAllCoursesLive();
        allCoursesLiveData.observe(getViewLifecycleOwner(), new Observer<List<Course>>() {
            @Override
            public void onChanged(List<Course> courses) {
                adapter = new CourseAdapter(getContext(), courses);
                recyclerView.setAdapter(adapter);
            }
        });
    }

    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterCourses(newText);
                return true;
            }
        });
    }

    private void filterCourses(String query) {

        LiveData<List<Course>> filteredCoursesLiveData = courseDao.searchCourses("%" + query + "%");
        filteredCoursesLiveData.observe(getViewLifecycleOwner(), new Observer<List<Course>>() {
            @Override
            public void onChanged(List<Course> courses) {
                adapter = new CourseAdapter(getContext(), courses);
                recyclerView.setAdapter(adapter);
            }
        });
    }

    private void setButtonListeners() {
        btnAll.setOnClickListener(v -> {
            highlightSelectedButton(btnAll);
            loadCoursesFromDatabase();
        });

        btnPopular.setOnClickListener(v -> {
            highlightSelectedButton(btnPopular);
            LiveData<List<Course>> popularCourses = courseDao.getPopularCourses();
            popularCourses.observe(getViewLifecycleOwner(), courses -> {
                adapter = new CourseAdapter(getContext(), courses);
                recyclerView.setAdapter(adapter);
            });
        });

        btnProgramming.setOnClickListener(v -> {
            highlightSelectedButton(btnProgramming);
            LiveData<List<Course>> programmingCourses = courseDao.getProgrammingCourses();
            programmingCourses.observe(getViewLifecycleOwner(), courses -> {
                adapter = new CourseAdapter(getContext(), courses);
                recyclerView.setAdapter(adapter);
            });
        });

        btnNonProgramming.setOnClickListener(v -> {
            highlightSelectedButton(btnNonProgramming);
            LiveData<List<Course>> nonProgrammingCourses = courseDao.getNonProgrammingCourses();
            nonProgrammingCourses.observe(getViewLifecycleOwner(), courses -> {
                adapter = new CourseAdapter(getContext(), courses);
                recyclerView.setAdapter(adapter);
            });
        });
    }
    private void highlightSelectedButton(TextView selectedButton) {
        // Reset all button styles to default
        btnAll.setBackgroundColor(Color.parseColor("#ffffff"));
        btnAll.setTextColor(Color.parseColor("#002B5B"));
        btnPopular.setBackgroundColor(Color.parseColor("#ffffff"));
        btnPopular.setTextColor(Color.parseColor("#002B5B"));
        btnProgramming.setBackgroundColor(Color.parseColor("#ffffff"));
        btnProgramming.setTextColor(Color.parseColor("#002B5B"));
        btnNonProgramming.setBackgroundColor(Color.parseColor("#ffffff"));
        btnNonProgramming.setTextColor(Color.parseColor("#002B5B"));

        // Highlight the selected button
        selectedButton.setBackgroundColor(Color.parseColor("#002B5B"));
        selectedButton.setTextColor(Color.parseColor("#FFFFFF"));
    }

}
