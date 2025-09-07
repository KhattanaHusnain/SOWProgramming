package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sowp.user.R;
import com.sowp.user.adapters.QuizAdapter;
import com.sowp.user.models.Course;
import com.sowp.user.models.Quiz;
import com.sowp.user.repositories.CourseRepository;
import com.sowp.user.repositories.QuizRepository;

import java.util.ArrayList;
import java.util.List;

public class ViewQuizzesActivity extends AppCompatActivity implements QuizAdapter.OnQuizClickListener {

    private ImageButton btnBack;
    private TextView tvCourseTitle, tvCourseCode, tvQuizCount, tvPageInfo;
    private TextInputEditText etSearch;
    private Spinner spinnerFilter;
    private RecyclerView recyclerViewQuizzes;
    private LinearLayout layoutEmptyState, layoutPagination;
    private ProgressBar progressBar;
    private Button btnPrevious, btnNext;

    private FirebaseFirestore firestore;
    private QuizRepository quizRepository;
    private CourseRepository courseRepository;
    private QuizAdapter quizAdapter;
    private List<Quiz> quizzes;
    private List<Quiz> filteredQuizzes;
    private Course currentCourse;
    private int courseId;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    private String[] filterOptions = {"All Quizzes", "Active", "Inactive", "Beginner", "Intermediate", "Advanced"};
    private String currentFilter = "All Quizzes";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_quizzes);

        courseId = getIntent().getIntExtra("COURSE_ID", -1);
        if (courseId == -1) {
            finish();
            return;
        }

        initializeViews();
        setupFirestore();
        setupRecyclerView();
        setupSpinner();
        setupClickListeners();

        loadCourseDetails();
        loadQuizzes();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvCourseTitle = findViewById(R.id.tvCourseTitle);
        tvCourseCode = findViewById(R.id.tvCourseCode);
        tvQuizCount = findViewById(R.id.tvQuizCount);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        etSearch = findViewById(R.id.etSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        recyclerViewQuizzes = findViewById(R.id.recyclerViewQuizzes);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutPagination = findViewById(R.id.layoutPagination);
        progressBar = findViewById(R.id.progressBar);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
    }

    private void setupFirestore() {
        firestore = FirebaseFirestore.getInstance();
        quizRepository = new QuizRepository(this);
        courseRepository = new CourseRepository(this);
        quizzes = new ArrayList<>();
        filteredQuizzes = new ArrayList<>();
    }

    private void setupRecyclerView() {
        quizAdapter = new QuizAdapter(this, filteredQuizzes, this);
        recyclerViewQuizzes.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewQuizzes.setAdapter(quizAdapter);
    }

    private void setupSpinner() {
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filterOptions);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = filterOptions[position];
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                updatePaginatedDisplay();
                updatePaginationControls();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                updatePaginatedDisplay();
                updatePaginationControls();
            }
        });
    }

    private void loadCourseDetails() {
        courseRepository.getCourse(courseId, new CourseRepository.Callback() {
            @Override
            public void onSuccess(List<Course> courses) {
                if (!courses.isEmpty()) {
                    currentCourse = courses.get(0);
                    tvCourseTitle.setText(currentCourse.getTitle());
                    tvCourseCode.setText(currentCourse.getCourseCode());
                }
            }

            @Override
            public void onFailure(String message) {}
        });
    }

    private void loadQuizzes() {
        if (isLoading) return;

        isLoading = true;
        showLoading(true);

        quizRepository.loadQuizzesForCourse(courseId, new QuizRepository.Callback() {
            @Override
            public void onSuccess(List<Quiz> loadedQuizzes) {
                isLoading = false;
                showLoading(false);

                List<Quiz> newQuizzes = new ArrayList<>();
                if (loadedQuizzes != null) {
                    newQuizzes.addAll(loadedQuizzes);
                }

                quizzes.clear();
                quizzes.addAll(newQuizzes);

                applyFilters();
                updateQuizCount();
            }

            @Override
            public void onFailure(String message) {
                isLoading = false;
                showLoading(false);
                showEmptyState();
            }
        });
    }

    private void applyFilters() {
        List<Quiz> tempFilteredQuizzes = new ArrayList<>();

        List<Quiz> quizzesCopy;
        synchronized (quizzes) {
            quizzesCopy = new ArrayList<>(quizzes);
        }

        for (Quiz quiz : quizzesCopy) {
            if (quiz == null) continue;

            String title = quiz.getTitle() != null ? quiz.getTitle() : "";
            String description = quiz.getDescription() != null ? quiz.getDescription() : "";
            String level = quiz.getLevel() != null ? quiz.getLevel() : "";

            boolean matchesSearch = searchQuery.isEmpty() ||
                    title.toLowerCase().contains(searchQuery.toLowerCase()) ||
                    description.toLowerCase().contains(searchQuery.toLowerCase());

            boolean matchesFilter = currentFilter.equals("All Quizzes") ||
                    (currentFilter.equals("Active") && quiz.isActive()) ||
                    (currentFilter.equals("Inactive") && !quiz.isActive()) ||
                    currentFilter.equals(level);

            if (matchesSearch && matchesFilter) {
                tempFilteredQuizzes.add(quiz);
            }
        }

        synchronized (filteredQuizzes) {
            filteredQuizzes.clear();
            filteredQuizzes.addAll(tempFilteredQuizzes);
        }

        calculatePagination();
        currentPage = 1;
        updatePaginatedDisplay();
        updatePaginationControls();
        updateQuizCount();
    }

    private void updatePaginatedDisplay() {
        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, filteredQuizzes.size());

        List<Quiz> paginatedQuizzes = new ArrayList<>();

        synchronized (filteredQuizzes) {
            if (startIndex < filteredQuizzes.size()) {
                for (int i = startIndex; i < endIndex && i < filteredQuizzes.size(); i++) {
                    paginatedQuizzes.add(filteredQuizzes.get(i));
                }
            }
        }

        quizAdapter.updateData(paginatedQuizzes);

        if (paginatedQuizzes.isEmpty() && filteredQuizzes.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void updateQuizCount() {
        int count;
        synchronized (filteredQuizzes) {
            count = filteredQuizzes.size();
        }

        String countText;
        if (count == 0) {
            countText = "No quizzes found";
        } else if (count == 1) {
            countText = "1 quiz found";
        } else {
            countText = count + " quizzes found";
        }
        tvQuizCount.setText(countText);
    }

    private void calculatePagination() {
        totalPages = (int) Math.ceil((double) filteredQuizzes.size() / PAGE_SIZE);
        if (totalPages == 0) totalPages = 1;
    }

    private void updatePaginationControls() {
        btnPrevious.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
        tvPageInfo.setText("Page " + currentPage + " of " + totalPages);

        layoutPagination.setVisibility(totalPages > 1 ? View.VISIBLE : View.GONE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            tvQuizCount.setText("Loading quizzes...");
        }
    }

    private void showEmptyState() {
        recyclerViewQuizzes.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        layoutPagination.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        recyclerViewQuizzes.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    @Override
    public void onQuizClick(Quiz quiz) {
        Intent intent = new Intent(this, TakeQuizActivity.class);
        intent.putExtra("COURSE_ID", String.valueOf(courseId));
        intent.putExtra("QUIZ_ID", String.valueOf(quiz.getQuizId()));
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadQuizzes();
    }
}