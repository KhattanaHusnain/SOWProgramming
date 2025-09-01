package com.sowp.user.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.models.QuizAttempt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizHistoryAdapter extends RecyclerView.Adapter<QuizHistoryAdapter.QuizAttemptViewHolder> {

    private List<QuizAttempt> quizAttempts;
    private OnQuizAttemptClickListener listener;
    private Context context;

    public interface OnQuizAttemptClickListener {
        void onQuizAttemptClick(QuizAttempt quizAttempt);
    }

    public QuizHistoryAdapter(List<QuizAttempt> quizAttempts, OnQuizAttemptClickListener listener) {
        this.quizAttempts = quizAttempts != null ? new ArrayList<>(quizAttempts) : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuizAttemptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_quiz_attempt, parent, false);
        return new QuizAttemptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizAttemptViewHolder holder, int position) {
        if (quizAttempts != null && position < quizAttempts.size() && position >= 0) {
            QuizAttempt attempt = quizAttempts.get(position);
            if (attempt != null) {
                holder.bind(attempt, listener, context);
            }
        }
    }

    @Override
    public int getItemCount() {
        return quizAttempts != null ? quizAttempts.size() : 0;
    }

    public void updateData(List<QuizAttempt> newAttempts) {
        this.quizAttempts.clear();
        if (newAttempts != null) {
            this.quizAttempts.addAll(newAttempts);
        }
        notifyDataSetChanged();
    }

    public void addAttempt(QuizAttempt attempt) {
        if (attempt != null) {
            this.quizAttempts.add(0, attempt); // Add to beginning for latest first
            notifyItemInserted(0);
        }
    }

    public void removeAttempt(int position) {
        if (position >= 0 && position < quizAttempts.size()) {
            quizAttempts.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class QuizAttemptViewHolder extends RecyclerView.ViewHolder {
        private TextView quizTitleText;
        private TextView statusText;
        private TextView scoreText;
        private TextView questionsText;
        private TextView timeTakenText;
        private TextView dateText;
        private View statusIndicator;

        public QuizAttemptViewHolder(@NonNull View itemView) {
            super(itemView);
            quizTitleText = itemView.findViewById(R.id.quizTitleText);
            statusText = itemView.findViewById(R.id.statusText);
            scoreText = itemView.findViewById(R.id.scoreText);
            questionsText = itemView.findViewById(R.id.questionsText);
            timeTakenText = itemView.findViewById(R.id.timeTakenText);
            dateText = itemView.findViewById(R.id.dateText);
            statusIndicator = itemView.findViewById(R.id.statusIndicator); // Optional status indicator
        }

        public void bind(QuizAttempt attempt, OnQuizAttemptClickListener listener, Context context) {
            try {
                // Set quiz title with null check
                String title = attempt.getQuizTitle();
                quizTitleText.setText(title != null && !title.isEmpty() ? title : "Unknown Quiz");

                // Set status with appropriate background and colors
                String statusStr = attempt.getStatusText();
                statusText.setText(statusStr);

                if (attempt.isPassed()) {
                    statusText.setBackgroundResource(R.drawable.bg_status_passed);
                    statusText.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                    if (statusIndicator != null) {
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.status_passed));
                    }
                } else {
                    statusText.setBackgroundResource(R.drawable.bg_status_failed);
                    statusText.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                    if (statusIndicator != null) {
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.status_failed));
                    }
                }

                // Set score with color coding
                String scoreStr = attempt.getScorePercentage();
                scoreText.setText(scoreStr);
                if (attempt.isPassed()) {
                    scoreText.setTextColor(ContextCompat.getColor(context, R.color.score_passed));
                } else {
                    scoreText.setTextColor(ContextCompat.getColor(context, R.color.score_failed));
                }

                // Set questions (correct/total)
                String questionsStr = String.format(Locale.getDefault(), "%d/%d",
                        attempt.getCorrectAnswers(), attempt.getTotalQuestions());
                questionsText.setText(questionsStr);

                // Set time taken
                timeTakenText.setText(attempt.getFormattedTimeTaken());

                // Set completion date
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String dateStr = sdf.format(new Date(attempt.getCompletedAt()));
                    dateText.setText(dateStr);
                } catch (Exception e) {
                    dateText.setText("Date unavailable");
                }

                // Set click listener
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onQuizAttemptClick(attempt);
                    }
                });

                // Add ripple effect for better UX
                itemView.setClickable(true);
                itemView.setFocusable(true);
                itemView.setBackgroundResource(R.drawable.item_background_selector);

            } catch (Exception e) {
                // Handle any binding errors gracefully
                quizTitleText.setText("Error loading quiz");
                statusText.setText("Unknown");
                scoreText.setText("0%");
                questionsText.setText("0/0");
                timeTakenText.setText("00:00");
                dateText.setText("Unknown date");
            }
        }
    }
}