package com.sowp.admin.coursemanagement;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sowp.admin.R;
import com.sowp.admin.assignmentmanagement.ViewAssignmentsActivity;
import com.sowp.admin.quizmanagement.ViewQuizzesActivity;
import com.sowp.admin.topicmanagement.ViewTopicsActivity;

import java.util.ArrayList;
import java.util.List;

public class ViewCoursesActivity extends AppCompatActivity implements CourseAdapter.OnCourseClickListener {

    // UI Components
    private ImageView back;
    private RecyclerView recyclerView;
    private CourseAdapter adapter;
    private ProgressBar progressBar, paginationProgressBar;
    private TextView tvTotalCourses;
    private LinearLayout tvNoData;
    private TextInputEditText etSearch;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddCourse;

    // Data
    private List<Course> courseList;
    private List<Course> filteredCourseList;
    private FirebaseFirestore db;

    // Pagination
    private static final int PAGE_SIZE = 10;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentSearchQuery = "";

    // Layout Manager
    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_courses);

        initViews();
        setupRecyclerView();
        setupSearch();
        setupSwipeRefresh();
        loadCourses(false);

        back.setOnClickListener(v -> finish());

        fabAddCourse.setOnClickListener(v -> {
            startActivity(new Intent(this, AddCourseActivity.class));
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        paginationProgressBar = findViewById(R.id.paginationProgressBar);
        tvNoData = findViewById(R.id.tvNoData);
        tvTotalCourses = findViewById(R.id.tvTotalCourses);
        etSearch = findViewById(R.id.etSearch);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        fabAddCourse = findViewById(R.id.fabAddCourse);
        back = findViewById(R.id.back);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize lists
        courseList = new ArrayList<>();
        filteredCourseList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new CourseAdapter(filteredCourseList, this);
        recyclerView.setAdapter(adapter);

        // Pagination scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage && currentSearchQuery.isEmpty()) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadCourses(true);
                    }
                }
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim();
                if (currentSearchQuery.isEmpty()) {
                    // Show all courses
                    filteredCourseList.clear();
                    filteredCourseList.addAll(courseList);
                    adapter.notifyDataSetChanged();
                    updateUI();
                } else {
                    // Filter courses
                    filterCourses(currentSearchQuery);
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });

        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.primary_dark,
                R.color.accent
        );
    }

    private void refreshData() {
        courseList.clear();
        filteredCourseList.clear();
        lastVisible = null;
        isLastPage = false;
        currentSearchQuery = "";
        etSearch.setText("");

        loadCourses(false);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void loadCourses(boolean isPagination) {
        if (isLoading) return;

        isLoading = true;

        if (isPagination) {
            paginationProgressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.VISIBLE);
            tvNoData.setVisibility(View.GONE);
        }

        Query query = db.collection("Course")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        if (isPagination && lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get()
                .addOnCompleteListener(task -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    paginationProgressBar.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Course> newCourses = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Course course = documentToCourse(document);
                            if (course != null) {
                                newCourses.add(course);
                            }
                        }

                        if (newCourses.isEmpty()) {
                            isLastPage = true;
                            if (!isPagination && courseList.isEmpty()) {
                                tvNoData.setVisibility(View.VISIBLE);
                            }
                        } else {
                            // Update last visible document for pagination
                            List<DocumentSnapshot> documents = task.getResult().getDocuments();
                            if (!documents.isEmpty()) {
                                lastVisible = documents.get(documents.size() - 1);
                            }

                            if (isPagination) {
                                int startPosition = courseList.size();
                                courseList.addAll(newCourses);
                                filteredCourseList.addAll(newCourses);
                                adapter.notifyItemRangeInserted(startPosition, newCourses.size());
                            } else {
                                courseList.addAll(newCourses);
                                filteredCourseList.addAll(newCourses);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        updateUI();

                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Error loading courses: " + errorMessage,
                                Toast.LENGTH_SHORT).show();

                        if (!isPagination && courseList.isEmpty()) {
                            tvNoData.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    paginationProgressBar.setVisibility(View.GONE);

                    Toast.makeText(this, "Failed to load courses: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    if (!isPagination && courseList.isEmpty()) {
                        tvNoData.setVisibility(View.VISIBLE);
                    }
                });
    }

    private Course documentToCourse(DocumentSnapshot document) {
        try {
            Course course = new Course();

            // Basic fields
            Long id = document.getLong("id");
            if (id != null) course.setId(id.intValue());

            course.setTitle(document.getString("title"));
            course.setShortTitle(document.getString("shortTitle"));
            course.setCourseCode(document.getString("courseCode"));
            course.setDescription(document.getString("description"));
            course.setIllustration(document.getString("illustration"));
            course.setInstructor(document.getString("instructor"));
            course.setDuration(document.getString("duration"));
            course.setLanguage(document.getString("language"));
            course.setSemester(document.getString("semester"));
            course.setLevel(document.getString("level"));
            course.setOutline(document.getString("outline"));

            // Numeric fields
            Long lectures = document.getLong("lectures");
            if (lectures != null) course.setLectures(lectures.intValue());

            Long members = document.getLong("members");
            if (members != null) course.setMembers(members.intValue());

            Long creditHours = document.getLong("creditHours");
            if (creditHours != null) course.setCreditHours(creditHours.intValue());

            Long noOfQuizzes = document.getLong("noOfQuizzes");
            if (noOfQuizzes != null) course.setNoOfQuizzes(noOfQuizzes.intValue());

            Long noOfAssignments = document.getLong("noOfAssignments");
            if (noOfAssignments != null) course.setNoOfAssignments(noOfAssignments.intValue());

            // Boolean fields
            Boolean isPublic = document.getBoolean("isPublic");
            if (isPublic != null) course.setPublic(isPublic);

            Boolean isLab = document.getBoolean("isLab");
            if (isLab != null) course.setLab(isLab);

            Boolean isComputer = document.getBoolean("isComputer");
            if (isComputer != null) course.setComputer(isComputer);

            Boolean isPaid = document.getBoolean("isPaid");
            if (isPaid != null) course.setPaid(isPaid);

            Boolean completed = document.getBoolean("completed");
            if (completed != null) course.setCompleted(completed);

            // Timestamp fields
            Long createdAt = document.getLong("createdAt");
            if (createdAt != null) course.setCreatedAt(createdAt);

            Long updatedAt = document.getLong("updatedAt");
            if (updatedAt != null) course.setUpdatedAt(updatedAt);

            // Double fields
            Double avgRating = document.getDouble("avgCourseRating");
            if (avgRating != null) course.setAvgCourseRating(avgRating);

            // List fields
            List<String> categoryArray = (List<String>) document.get("categoryArray");
            if (categoryArray != null) course.setCategoryArray(categoryArray);

            List<String> tags = (List<String>) document.get("tags");
            if (tags != null) course.setTags(tags);

            List<String> preRequisite = (List<String>) document.get("preRequisite");
            if (preRequisite != null) course.setPreRequisite(preRequisite);

            List<String> followUp = (List<String>) document.get("followUp");
            if (followUp != null) course.setFollowUp(followUp);

            List<String> departmentArray = (List<String>) document.get("departmentArray");
            if (departmentArray != null) course.setDepartmentArray(departmentArray);

            return course;

        } catch (Exception e) {
            android.util.Log.e("ViewCourses", "Error converting document to course", e);
            return null;
        }
    }

    private void filterCourses(String query) {
        filteredCourseList.clear();

        if (query.isEmpty()) {
            filteredCourseList.addAll(courseList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Course course : courseList) {
                if (matchesSearchQuery(course, lowerQuery)) {
                    filteredCourseList.add(course);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateUI();
    }

    private boolean matchesSearchQuery(Course course, String query) {
        return (course.getTitle() != null && course.getTitle().toLowerCase().contains(query)) ||
                (course.getCourseCode() != null && course.getCourseCode().toLowerCase().contains(query)) ||
                (course.getInstructor() != null && course.getInstructor().toLowerCase().contains(query)) ||
                (course.getSemester() != null && course.getSemester().toLowerCase().contains(query)) ||
                (course.getLevel() != null && course.getLevel().toLowerCase().contains(query)) ||
                (course.getTags() != null && course.getTags().toString().toLowerCase().contains(query));
    }

    private void updateUI() {
        int totalCount = currentSearchQuery.isEmpty() ? courseList.size() : filteredCourseList.size();

        if (totalCount == 0) {
            tvNoData.setVisibility(View.VISIBLE);
            tvTotalCourses.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoData.setVisibility(View.GONE);
            tvTotalCourses.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.VISIBLE);

            String countText = currentSearchQuery.isEmpty() ?
                    "Total Courses: " + totalCount :
                    "Found: " + totalCount + " courses";
            tvTotalCourses.setText(countText);
        }
    }

    @Override
    public void onCourseClick(Course course) {
        // Navigate to course details/edit page
        Intent intnt = getIntent();
        boolean cameForTopics = intnt.getBooleanExtra("cameForTopics",false);
        boolean cameForQuizzes = intnt.getBooleanExtra("cameForQuizzes",false);
        boolean cameForAssignments = intnt.getBooleanExtra("cameForAssignments",false);

        if(cameForTopics){
            Intent intent = new Intent(this, ViewTopicsActivity.class);
            intent.putExtra("COURSE_ID", String.valueOf(course.getId()));
            startActivity(intent);
        } else if (cameForQuizzes) {
            Intent intent = new Intent(this, ViewQuizzesActivity.class);
            intent.putExtra("COURSE_ID", String.valueOf(course.getId()));
            startActivity(intent);
        } else if (cameForAssignments) {
            Intent intent = new Intent(this, ViewAssignmentsActivity.class);
            intent.putExtra("COURSE_ID", String.valueOf(course.getId()));
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, EditCourseActivity.class);
            intent.putExtra("COURSE_ID", String.valueOf(course.getId()));
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from edit activity
        if (!courseList.isEmpty()) {
            refreshData();
        }
    }
}