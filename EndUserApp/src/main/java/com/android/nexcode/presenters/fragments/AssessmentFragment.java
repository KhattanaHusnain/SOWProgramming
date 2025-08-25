package com.android.nexcode.presenters.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.nexcode.R;
import com.android.nexcode.models.Assignment;
import com.android.nexcode.models.Quiz;
import com.android.nexcode.repositories.firebase.QuizRepository;
import com.android.nexcode.repositories.firebase.AssignmentRepository;
import com.android.nexcode.utils.UserAuthenticationUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AssessmentFragment extends Fragment {

    private static final String TAG = "AssessmentFragment";
    private static final String[] TAB_TITLES = new String[]{"Quizzes", "Assignments"};

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ProgressBar progressBar;

    private List<Quiz> quizList = new ArrayList<>();
    private List<Assignment> assignmentList = new ArrayList<>();
    private List<Integer> courses = new ArrayList<>();
    private DocumentSnapshot lastQuizDocument = null;
    private DocumentSnapshot lastAssignmentDocument = null;
    private boolean hasMoreQuizzes = false;
    private boolean hasMoreAssignments = false;

    private FirebaseFirestore db;
    private UserAuthenticationUtils userAuthenticationUtils;
    private QuizRepository quizRepository;
    private AssignmentRepository assignmentRepository;
    private PagerAdapter pagerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_assessment, container, false);

        // Initialize views
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
        progressBar = view.findViewById(R.id.progressBar);
        userAuthenticationUtils = new UserAuthenticationUtils(getContext());

        // Initialize Firestore and Repository
        db = FirebaseFirestore.getInstance();
        quizRepository = new QuizRepository(getContext());
        assignmentRepository = new AssignmentRepository(getContext());

        // Load data from Firestore
        loadData();

        return view;
    }

    private void loadData() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("User").document(userAuthenticationUtils.getCurrentUserEmail()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                courses = (List<Integer>) task.getResult().get("enrolledCourses");
                if (courses == null) {
                    Toast.makeText(getContext(),"Join a course to view it's quizzes", Toast.LENGTH_SHORT).show();
                    loadAssignmentsFirstPage(); // Still load assignments
                } else {
                    loadQuizzesFirstPage();
                }
            } else {
                Log.e(TAG, "Error loading user courses", task.getException());
                loadAssignmentsFirstPage(); // Still try to load assignments
            }
        });
    }

    private void loadQuizzesFirstPage() {
        quizRepository.loadFirstPageQuizzes(courses, new QuizRepository.PaginatedCallback() {
            @Override
            public void onSuccess(List<Quiz> quizzes, DocumentSnapshot lastDocument, boolean hasMore) {
                quizList.clear();
                quizList.addAll(quizzes);
                lastQuizDocument = lastDocument;
                hasMoreQuizzes = hasMore;
                Log.d(TAG, "Loaded " + quizList.size() + " quizzes (first page)");
                loadAssignmentsFirstPage();
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Error loading quizzes: " + message);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                loadAssignmentsFirstPage(); // Still try to load assignments
            }
        });
    }

    public void loadMoreQuizzes(QuizRepository.PaginatedCallback callback) {
        if (!hasMoreQuizzes) {
            callback.onSuccess(new ArrayList<>(), null, false);
            return;
        }

        quizRepository.loadQuizzesWithPagination(courses, lastQuizDocument, new QuizRepository.PaginatedCallback() {
            @Override
            public void onSuccess(List<Quiz> quizzes, DocumentSnapshot lastDocument, boolean hasMore) {
                quizList.addAll(quizzes);
                lastQuizDocument = lastDocument;
                hasMoreQuizzes = hasMore;
                Log.d(TAG, "Loaded " + quizzes.size() + " more quizzes. Total: " + quizList.size());
                callback.onSuccess(quizzes, lastDocument, hasMore);
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Error loading more quizzes: " + message);
                callback.onFailure(message);
            }
        });
    }

    public void refreshQuizzes(QuizRepository.PaginatedCallback callback) {
        // Reset pagination state
        lastQuizDocument = null;
        hasMoreQuizzes = false;

        quizRepository.loadFirstPageQuizzes(courses, new QuizRepository.PaginatedCallback() {
            @Override
            public void onSuccess(List<Quiz> quizzes, DocumentSnapshot lastDocument, boolean hasMore) {
                quizList.clear();
                quizList.addAll(quizzes);
                lastQuizDocument = lastDocument;
                hasMoreQuizzes = hasMore;
                Log.d(TAG, "Refreshed quizzes. Loaded: " + quizList.size());
                callback.onSuccess(quizzes, lastDocument, hasMore);
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Error refreshing quizzes: " + message);
                callback.onFailure(message);
            }
        });
    }

    public void loadMoreAssignments(AssignmentRepository.PaginatedCallback callback) {
        if (!hasMoreAssignments) {
            callback.onSuccess(new ArrayList<>(), null, false);
            return;
        }

        assignmentRepository.loadAssignmentsWithPagination(lastAssignmentDocument, new AssignmentRepository.PaginatedCallback() {
            @Override
            public void onSuccess(List<Assignment> assignments, DocumentSnapshot lastDocument, boolean hasMore) {
                assignmentList.addAll(assignments);
                lastAssignmentDocument = lastDocument;
                hasMoreAssignments = hasMore;
                Log.d(TAG, "Loaded " + assignments.size() + " more assignments. Total: " + assignmentList.size());
                callback.onSuccess(assignments, lastDocument, hasMore);
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Error loading more assignments: " + message);
                callback.onFailure(message);
            }
        });
    }

    public void refreshAssignments(AssignmentRepository.PaginatedCallback callback) {
        // Reset pagination state
        lastAssignmentDocument = null;
        hasMoreAssignments = false;

        assignmentRepository.loadFirstPageAssignments(new AssignmentRepository.PaginatedCallback() {
            @Override
            public void onSuccess(List<Assignment> assignments, DocumentSnapshot lastDocument, boolean hasMore) {
                assignmentList.clear();
                assignmentList.addAll(assignments);
                lastAssignmentDocument = lastDocument;
                hasMoreAssignments = hasMore;
                Log.d(TAG, "Refreshed assignments. Loaded: " + assignmentList.size());

                // Load assignment progress for each assignment
                loadAssignmentProgress(callback, assignments, lastDocument, hasMore);
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Error refreshing assignments: " + message);
                callback.onFailure(message);
            }
        });
    }

    private void loadAssignmentsFirstPage() {
        assignmentRepository.loadFirstPageAssignments(new AssignmentRepository.PaginatedCallback() {
            @Override
            public void onSuccess(List<Assignment> assignments, DocumentSnapshot lastDocument, boolean hasMore) {
                assignmentList.clear();
                assignmentList.addAll(assignments);
                lastAssignmentDocument = lastDocument;
                hasMoreAssignments = hasMore;
                Log.d(TAG, "Loaded " + assignmentList.size() + " assignments (first page)");

                // Load assignment progress for each assignment
                loadAssignmentProgress(null, assignments, lastDocument, hasMore);
            }

            @Override
            public void onFailure(String message) {
                Log.e(TAG, "Error loading assignments: " + message);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                checkDataLoaded(); // Still setup UI even if assignments fail
            }
        });
    }

    private void loadAssignmentProgress(AssignmentRepository.PaginatedCallback callback, List<Assignment> assignments, DocumentSnapshot lastDocument, boolean hasMore) {
        if (assignments.isEmpty()) {
            checkDataLoaded();
            if (callback != null) {
                callback.onSuccess(assignments, lastDocument, hasMore);
            }
            return;
        }

        int[] completedCount = {0}; // Array to allow modification in lambda

        for (Assignment assignment : assignments) {
            db.collection("User/" + userAuthenticationUtils.getCurrentUserEmail() + "/AssignmentProgress")
                    .whereEqualTo("assignmentId", assignment.getId())
                    .get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            if (!task1.getResult().isEmpty()) {
                                assignment.setStatus("Submitted");
                                assignment.setEarnedScore(task1.getResult().getDocuments().get(0).getDouble("score"));
                            } else {
                                assignment.setStatus("Not Started");
                            }
                        } else {
                            assignment.setStatus("Not Started");
                        }

                        completedCount[0]++;
                        if (completedCount[0] == assignments.size()) {
                            // All assignment progress loaded
                            checkDataLoaded();
                            if (callback != null) {
                                callback.onSuccess(assignments, lastDocument, hasMore);
                            }
                        }
                    });
        }
    }

    private void checkDataLoaded() {
        // If both quizzes and assignments are loaded, set up the ViewPager
        if (quizList.size() >= 0 && assignmentList.size() >= 0) {
            setupViewPager();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setupViewPager() {
        pagerAdapter = new PagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();
    }

    // ViewPager2 adapter
    private class PagerAdapter extends FragmentStateAdapter {

        public PagerAdapter(Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Return the appropriate fragment for each tab
            if (position == 0) {
                return QuizListFragment.newInstance(quizList, AssessmentFragment.this);
            } else {
                return AssignmentListFragment.newInstance(assignmentList, AssessmentFragment.this);
            }
        }

        @Override
        public int getItemCount() {
            return TAB_TITLES.length;
        }
    }
}