package com.sowp.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
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
        this.quizzes = quizzes;
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
        Quiz quiz = quizzes.get(position);
        holder.bind(quiz, position);
    }

    @Override
    public int getItemCount() {
        return quizzes.size();
    }

    public void updateQuizzes(List<Quiz> newQuizzes) {
        this.quizzes.clear();
        this.quizzes.addAll(newQuizzes);
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
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onQuizClick(quizzes.get(position));
                }
            });
        }

        public void bind(Quiz quiz, int position) {
            tvQuizNumber.setText(String.valueOf(quiz.getQuizId()));
            tvQuizTitle.setText(quiz.getTitle());
            tvQuizDescription.setText(quiz.getDescription());

            // Set status
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

            // Format date
            if (quiz.getCreatedAt() > 0) {
                Date date = new Date(quiz.getCreatedAt());
                tvCreatedDate.setText(dateFormat.format(date));
            } else {
                tvCreatedDate.setText("N/A");
            }
        }
    }
}