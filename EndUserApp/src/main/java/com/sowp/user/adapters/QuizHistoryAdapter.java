package com.sowp.user.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    public interface OnQuizAttemptClickListener {
        void onQuizAttemptClick(QuizAttempt quizAttempt);
    }

    public QuizHistoryAdapter(List<QuizAttempt> quizAttempts, OnQuizAttemptClickListener listener) {
        this.quizAttempts = quizAttempts != null ? quizAttempts : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuizAttemptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_attempt, parent, false);
        return new QuizAttemptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizAttemptViewHolder holder, int position) {
        if (quizAttempts != null && position < quizAttempts.size()) {
            QuizAttempt attempt = quizAttempts.get(position);
            holder.bind(attempt, listener);
        }
    }

    @Override
    public int getItemCount() {
        return quizAttempts != null ? quizAttempts.size() : 0;
    }

    public void updateData(List<QuizAttempt> newAttempts) {
        this.quizAttempts = newAttempts != null ? newAttempts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class QuizAttemptViewHolder extends RecyclerView.ViewHolder {
        private TextView quizTitleText;
        private TextView statusText;
        private TextView scoreText;
        private TextView questionsText;
        private TextView timeTakenText;
        private TextView dateText;

        public QuizAttemptViewHolder(@NonNull View itemView) {
            super(itemView);
            quizTitleText = itemView.findViewById(R.id.quizTitleText);
            statusText = itemView.findViewById(R.id.statusText);
            scoreText = itemView.findViewById(R.id.scoreText);
            questionsText = itemView.findViewById(R.id.questionsText);
            timeTakenText = itemView.findViewById(R.id.timeTakenText);
            dateText = itemView.findViewById(R.id.dateText);
        }

        public void bind(QuizAttempt attempt, OnQuizAttemptClickListener listener) {
            quizTitleText.setText(attempt.getQuizTitle());

            // Set status with appropriate background
            statusText.setText(attempt.getStatusText());
            if (attempt.isPassed()) {
                statusText.setBackgroundResource(R.drawable.bg_status_passed);
            } else {
                statusText.setBackgroundResource(R.drawable.bg_status_failed);
            }

            // Set score
            scoreText.setText(attempt.getScorePercentage());

            // Set questions (correct/total)
            questionsText.setText(String.format("%d/%d", attempt.getCorrectAnswers(), attempt.getTotalQuestions()));

            // Set time taken
            timeTakenText.setText(attempt.getFormattedTimeTaken());

            // Set date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            dateText.setText(sdf.format(new Date(attempt.getCompletedAt())));

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuizAttemptClick(attempt);
                }
            });
        }
    }
}