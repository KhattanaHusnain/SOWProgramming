// Fixed QuizAdapter.java - Complete class with ConcurrentModificationException fixes

package com.sowp.admin.quizmanagement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.admin.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private Context context;
    private List<Quiz> quizzes;
    private OnQuizClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnQuizClickListener {
        void onQuizClick(Quiz quiz);
    }

    public QuizAdapter(Context context, List<Quiz> quizzes, OnQuizClickListener listener) {
        this.context = context;
        // Create defensive copy to prevent concurrent modification
        this.quizzes = new ArrayList<>(quizzes);
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_quiz, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        if (position >= 0 && position < quizzes.size()) {
            Quiz quiz = quizzes.get(position);
            holder.bind(quiz, position);
        }
    }

    @Override
    public int getItemCount() {
        return quizzes.size();
    }

    /**
     * Thread-safe method to update quizzes list
     * Creates defensive copies to prevent ConcurrentModificationException
     */
    public void updateQuizzes(List<Quiz> newQuizzes) {
        if (newQuizzes == null) {
            this.quizzes.clear();
        } else {
            // Create a defensive copy to avoid ConcurrentModificationException
            List<Quiz> safeCopy = new ArrayList<>(newQuizzes);
            this.quizzes.clear();
            this.quizzes.addAll(safeCopy);
        }
        notifyDataSetChanged();
    }

    public class QuizViewHolder extends RecyclerView.ViewHolder {

        private TextView tvQuizNumber, tvQuizTitle, tvQuizDescription, tvQuizStatus;
        private TextView tvTotalQuestions, tvPassingScore, tvQuizLevel, tvCreatedDate;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);

            tvQuizNumber = itemView.findViewById(R.id.tvQuizNumber);
            tvQuizTitle = itemView.findViewById(R.id.tvQuizTitle);
            tvQuizDescription = itemView.findViewById(R.id.tvQuizDescription);
            tvQuizStatus = itemView.findViewById(R.id.tvQuizStatus);
            tvTotalQuestions = itemView.findViewById(R.id.tvTotalQuestions);
            tvPassingScore = itemView.findViewById(R.id.tvPassingScore);
            tvQuizLevel = itemView.findViewById(R.id.tvQuizLevel);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < quizzes.size() && listener != null) {
                    listener.onQuizClick(quizzes.get(position));
                }
            });
        }

        public void bind(Quiz quiz, int position) {
            if (quiz == null) return;

            tvQuizNumber.setText(String.valueOf(quiz.getQuizId()));

            // Safe string handling with null checks
            tvQuizTitle.setText(quiz.getTitle() != null ? quiz.getTitle() : "Untitled Quiz");
            tvQuizDescription.setText(quiz.getDescription() != null ? quiz.getDescription() : "No description");

            // Set status with null safety
            if (quiz.isActive()) {
                tvQuizStatus.setText("Active");
                tvQuizStatus.setBackgroundResource(R.drawable.status_active_background);
            } else {
                tvQuizStatus.setText("Inactive");
                tvQuizStatus.setBackgroundResource(R.drawable.status_inactive_background);
            }

            tvTotalQuestions.setText(String.valueOf(quiz.getTotalQuestions()));
            tvPassingScore.setText(quiz.getPassingScore() + "%");
            tvQuizLevel.setText(quiz.getLevel() != null ? quiz.getLevel() : "N/A");

            // Format date with null safety
            if (quiz.getCreatedAt() > 0) {
                try {
                    Date date = new Date(quiz.getCreatedAt());
                    tvCreatedDate.setText(dateFormat.format(date));
                } catch (Exception e) {
                    tvCreatedDate.setText("N/A");
                }
            } else {
                tvCreatedDate.setText("N/A");
            }
        }
    }
}