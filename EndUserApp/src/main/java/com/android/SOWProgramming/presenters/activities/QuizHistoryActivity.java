package com.android.SOWProgramming.presenters.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.SOWProgramming.R;
import com.android.SOWProgramming.adapters.QuizHistoryAdapter;
import com.android.SOWProgramming.models.QuizAttempt;
import com.android.SOWProgramming.repositories.firebase.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class QuizHistoryActivity extends AppCompatActivity implements QuizHistoryAdapter.OnQuizAttemptClickListener {

    private RecyclerView quizHistoryRecyclerView;
    private QuizHistoryAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private LinearLayout contentLayout;
    private LinearLayout paginationLayout;
    private TextView totalAttemptsText;
    private TextView averageScoreText;
    private TextView passedQuizzesText;
    private Button previousButton;
    private Button nextButton;
    private TextView pageIndicator;

    private UserRepository userRepository;
    private List<QuizAttempt> allAttempts;
    private List<QuizAttempt> currentPageAttempts;

    private static final int ITEMS_PER_PAGE = 10;
    private int currentPage = 0;
    private int totalPages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_history);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupPagination();

        userRepository = new UserRepository(this);
        allAttempts = new ArrayList<>();
        currentPageAttempts = new ArrayList<>();

        loadQuizHistory();
    }

    private void initViews() {
        quizHistoryRecyclerView = findViewById(R.id.quizHistoryRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        contentLayout = findViewById(R.id.contentLayout);
        paginationLayout = findViewById(R.id.paginationLayout);
        totalAttemptsText = findViewById(R.id.totalAttemptsText);
        averageScoreText = findViewById(R.id.averageScoreText);
        passedQuizzesText = findViewById(R.id.passedQuizzesText);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        pageIndicator = findViewById(R.id.pageIndicator);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new QuizHistoryAdapter(currentPageAttempts, this);
        quizHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        quizHistoryRecyclerView.setAdapter(adapter);
    }

    private void setupPagination() {
        previousButton.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                updateCurrentPage();
            }
        });

        nextButton.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateCurrentPage();
            }
        });
    }

    private void loadQuizHistory() {
        showLoading(true);

        userRepository.getAllQuizAttempts(new UserRepository.QuizAttemptsCallback() {
            @Override
            public void onSuccess(List<QuizAttempt> attempts) {
                runOnUiThread(() -> {
                    showLoading(false);
                    allAttempts.clear();
                    allAttempts.addAll(attempts);

                    if (allAttempts.isEmpty()) {
                        showEmptyState();
                    } else {
                        setupPaginationData();
                        updateStats();
                        updateCurrentPage();
                        showContent();
                        adapter.updateData(currentPageAttempts);
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmptyState();
                    Toast.makeText(QuizHistoryActivity.this, "Error loading quiz history: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupPaginationData() {
        if (allAttempts != null && !allAttempts.isEmpty()) {
            totalPages = (int) Math.ceil((double) allAttempts.size() / ITEMS_PER_PAGE);
        } else {
            totalPages = 1;
        }
        currentPage = 0;
    }

    private void updateCurrentPage() {
        currentPageAttempts.clear();

        if (allAttempts != null && !allAttempts.isEmpty()) {
            int startIndex = currentPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allAttempts.size());

            for (int i = startIndex; i < endIndex; i++) {
                currentPageAttempts.add(allAttempts.get(i));
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        updatePaginationControls();
    }

    private void updatePaginationControls() {
        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalPages - 1);
        pageIndicator.setText(String.format("Page %d of %d", currentPage + 1, Math.max(totalPages, 1)));
    }

    private void updateStats() {
        if (allAttempts == null || allAttempts.isEmpty()) {
            totalAttemptsText.setText("0");
            averageScoreText.setText("0%");
            passedQuizzesText.setText("0");
            return;
        }

        int totalAttempts = allAttempts.size();
        int passedCount = 0;
        double totalScore = 0;

        for (QuizAttempt attempt : allAttempts) {
            if (attempt.isPassed()) {
                passedCount++;
            }
            totalScore += attempt.getScore();
        }

        double averageScore = totalAttempts > 0 ? totalScore / totalAttempts : 0;

        totalAttemptsText.setText(String.valueOf(totalAttempts));
        averageScoreText.setText(String.format("%.1f%%", averageScore));
        passedQuizzesText.setText(String.valueOf(passedCount));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onQuizAttemptClick(QuizAttempt quizAttempt) {
        Intent intent = new Intent(this, QuizDetailActivity.class);
        intent.putExtra("attemptId", quizAttempt.getAttemptId());
        intent.putExtra("quizId", quizAttempt.getQuizId());
        intent.putExtra("quizTitle", quizAttempt.getQuizTitle());
        startActivity(intent);
    }
}