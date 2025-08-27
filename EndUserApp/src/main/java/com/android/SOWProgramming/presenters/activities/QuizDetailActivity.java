package com.android.SOWProgramming.presenters.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.SOWProgramming.R;
import com.android.SOWProgramming.adapters.QuestionDetailAdapter;
import com.android.SOWProgramming.models.QuizAttempt;
import com.android.SOWProgramming.repositories.firebase.UserRepository;

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
    private RecyclerView questionsRecyclerView;

    private UserRepository userRepository;
    private QuestionDetailAdapter adapter;

    private String attemptId;
    private String quizId;
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
        questionsRecyclerView = findViewById(R.id.questionsRecyclerView);
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

    private void getIntentData() {
        attemptId = getIntent().getStringExtra("attemptId");
        quizId = getIntent().getStringExtra("quizId");
        quizTitle = getIntent().getStringExtra("quizTitle");

        if (quizTitle != null) {
            quizTitleText.setText(quizTitle);
        }
    }

    private void loadQuizDetails() {
        if (attemptId == null) {
            Toast.makeText(this, "Invalid quiz attempt", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRepository.getQuizAttemptDetails(attemptId, new UserRepository.QuizAttemptCallback() {
            @Override
            public void onSuccess(QuizAttempt quizAttempt) {
                runOnUiThread(() -> populateQuizDetails(quizAttempt));
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(QuizDetailActivity.this, "Error loading quiz details: " + message, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void populateQuizDetails(QuizAttempt quizAttempt) {
        quizTitleText.setText(quizAttempt.getQuizTitle());

        statusText.setText(quizAttempt.getStatusText());
        if (quizAttempt.isPassed()) {
            statusText.setBackgroundResource(R.drawable.bg_status_passed);
        } else {
            statusText.setBackgroundResource(R.drawable.bg_status_failed);
        }

        scoreText.setText(quizAttempt.getScorePercentage());
        correctAnswersText.setText(String.valueOf(quizAttempt.getCorrectAnswers()));
        totalQuestionsText.setText(String.valueOf(quizAttempt.getTotalQuestions()));
        timeTakenText.setText(quizAttempt.getFormattedTimeTaken());

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.getDefault());
        String formattedDate = "Completed on " + sdf.format(new Date(quizAttempt.getCompletedAt()));
        dateText.setText(formattedDate);

        if (quizAttempt.getAnswers() != null && !quizAttempt.getAnswers().isEmpty()) {
            Toast.makeText(this, "Questions: " + quizAttempt.getAnswers().size(), Toast.LENGTH_SHORT).show();
            adapter = new QuestionDetailAdapter(quizAttempt.getAnswers());
            questionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            questionsRecyclerView.setAdapter(adapter);
        }
    }
}