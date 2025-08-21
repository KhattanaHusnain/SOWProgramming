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
import com.android.nexcode.utils.UserAuthenticationUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
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
    List<Integer> courses = new ArrayList<>();


    private FirebaseFirestore db;
    private UserAuthenticationUtils userAuthenticationUtils;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quizzes, container, false);

        // Initialize views
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
        progressBar = view.findViewById(R.id.progressBar);
        userAuthenticationUtils = new UserAuthenticationUtils(getContext());

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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
                } else {
                    // Load quizzes
                    db.collection("quizzes")
                            .whereEqualTo("active", true)
                            .whereIn("course", courses)
                            .get()
                            .addOnCompleteListener(task1 -> {
                                if (task.isSuccessful()) {
                                    quizList.clear();
                                    for (QueryDocumentSnapshot document : task1.getResult()) {
                                        Quiz quiz = document.toObject(Quiz.class);
                                        quiz.setId(document.getId());
                                        quizList.add(quiz);
                                    }
                                    Log.d(TAG, "Loaded " + quizList.size() + " quizzes");

                                    // Check if both data sets are loaded
                                    checkDataLoaded();
                                } else {
                                    Log.e(TAG, "Error loading quizzes", task1.getException());
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                }
            }
        });



        // Load assignments
        db.collection("assignments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        assignmentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Assignment assignment = document.toObject(Assignment.class);
                            assignment.setId(document.getId());
                            db.collection("User/"+userAuthenticationUtils.getCurrentUserEmail()+"/AssignmentProgress")
                                    .whereEqualTo("assignmentId", assignment.getId())
                                    .get().addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            if (!task1.getResult().isEmpty()) {
                                            assignment.setStatus("Submitted");
                                            assignment.setEarnedScore(task1.getResult().getDocuments().get(0).getDouble("score"));
                                            }
                                        }
                                        else {
                                            assignment.setStatus("Not Started");
                                        }
                                    });
                            assignmentList.add(assignment);
                        }
                        Log.d(TAG, "Loaded " + assignmentList.size() + " assignments");

                        // Check if both data sets are loaded
                        checkDataLoaded();
                    } else {
                        Log.e(TAG, "Error loading assignments", task.getException());
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void checkDataLoaded() {
        // If both quizzes and assignments are loaded, set up the ViewPager
        if (quizList.size() >= 0 && assignmentList.size() >= 0) {
            setupViewPager();
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setupViewPager() {
        PagerAdapter pagerAdapter = new PagerAdapter(this);
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
                return QuizListFragment.newInstance(quizList);
            } else {
                return AssignmentListFragment.newInstance(assignmentList);
            }
        }

        @Override
        public int getItemCount() {
            return TAB_TITLES.length;
        }
    }
}