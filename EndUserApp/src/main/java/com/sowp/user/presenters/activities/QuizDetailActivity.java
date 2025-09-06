package com.sowp.user.presenters.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.adapters.QuestionDetailAdapter;
import com.sowp.user.models.QuizAttempt;
import com.sowp.user.repositories.firebase.UserRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QuizDetailActivity extends AppCompatActivity {

    private TextView quizTitleText;
    private TextView statusText;
    private TextView scoreText;
    private TextView correctAnswersText;
    private TextView totalQuestionsText;
    private TextView timeTakenText;
    private TextView dateText;
    private TextView passingScoreText;
    private RecyclerView questionsRecyclerView;
    private ProgressBar progressBar;
    private View contentLayout;
    private TextView noQuestionsText;

    private UserRepository userRepository;
    private QuestionDetailAdapter adapter;

    private String attemptId;
    private String quizId;
    private String courseId;
    private String quizTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_detail);

        initViews();
        setupToolbar();
        getIntentData();

        userRepository = new UserRepository(this);
        loadQuizDetails();
    }

    private void initViews() {
        quizTitleText = findViewById(R.id.quizTitleText);
        statusText = findViewById(R.id.statusText);
        scoreText = findViewById(R.id.scoreText);
        correctAnswersText = findViewById(R.id.correctAnswersText);
        totalQuestionsText = findViewById(R.id.totalQuestionsText);
        timeTakenText = findViewById(R.id.timeTakenText);
        dateText = findViewById(R.id.dateText);
        passingScoreText = findViewById(R.id.passingScoreText);
        questionsRecyclerView = findViewById(R.id.questionsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);
        noQuestionsText = findViewById(R.id.noQuestionsText);

        if (progressBar == null || contentLayout == null) {
            throw new RuntimeException("Required views not found in layout");
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Quiz Details");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void getIntentData() {
        attemptId = getIntent().getStringExtra("attemptId");
        quizId = getIntent().getStringExtra("quizId");
        courseId = getIntent().getStringExtra("courseId");
        quizTitle = getIntent().getStringExtra("quizTitle");

        if (quizTitle != null && !quizTitle.isEmpty()) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(quizTitle);
            }
        }
    }

    private void loadQuizDetails() {
        if (attemptId == null || attemptId.isEmpty()) {
            finish();
            return;
        }

        showLoading(true);

        userRepository.getQuizAttemptDetails(attemptId, new UserRepository.QuizAttemptCallback() {
            @Override
            public void onSuccess(QuizAttempt quizAttempt) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (quizAttempt != null) {
                        populateQuizDetails(quizAttempt);
                    } else {
                        showError();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError();
                });
            }
        });
    }

    private void populateQuizDetails(QuizAttempt quizAttempt) {
        try {
            if (quizTitleText != null) {
                quizTitleText.setText(quizAttempt.getQuizTitle() != null ?
                        quizAttempt.getQuizTitle() : "Unknown Quiz");
            }

            if (statusText != null) {
                statusText.setText(quizAttempt.getStatusText());
                if (quizAttempt.isPassed()) {
                    statusText.setBackgroundResource(R.drawable.bg_status_passed);
                } else {
                    statusText.setBackgroundResource(R.drawable.bg_status_failed);
                }
            }

            if (scoreText != null) {
                scoreText.setText(quizAttempt.getScorePercentage());
            }

            if (correctAnswersText != null) {
                correctAnswersText.setText(String.valueOf(quizAttempt.getCorrectAnswers()));
            }

            if (totalQuestionsText != null) {
                totalQuestionsText.setText(String.valueOf(quizAttempt.getTotalQuestions()));
            }

            if (timeTakenText != null) {
                timeTakenText.setText(quizAttempt.getFormattedTimeTaken());
            }

            if (passingScoreText != null) {
                passingScoreText.setText(String.format(Locale.getDefault(),
                        "Pass: %.0f%%", quizAttempt.getPassingScore()));
            }

            if (dateText != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault());
                    String formattedDate = "Completed on " + sdf.format(new Date(quizAttempt.getCompletedAt()));
                    dateText.setText(formattedDate);
                } catch (Exception e) {
                    dateText.setText("Date not available");
                }
            }

            setupQuestionsRecyclerView(quizAttempt);

        } catch (Exception e) {
            showError();
        }
    }

    private void setupQuestionsRecyclerView(QuizAttempt quizAttempt) {
        if (questionsRecyclerView == null) return;

        if (quizAttempt.getAnswers() != null && !quizAttempt.getAnswers().isEmpty()) {
            questionsRecyclerView.setVisibility(View.VISIBLE);
            if (noQuestionsText != null) {
                noQuestionsText.setVisibility(View.GONE);
            }

            adapter = new QuestionDetailAdapter(quizAttempt.getAnswers());
            questionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            questionsRecyclerView.setAdapter(adapter);
        } else {
            questionsRecyclerView.setVisibility(View.GONE);
            if (noQuestionsText != null) {
                noQuestionsText.setVisibility(View.VISIBLE);
                noQuestionsText.setText("No detailed question data available for this attempt.");
            }
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (contentLayout != null) {
            contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void showError() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter = null;
        if (userRepository != null) {
            userRepository = null;
        }
    }
}