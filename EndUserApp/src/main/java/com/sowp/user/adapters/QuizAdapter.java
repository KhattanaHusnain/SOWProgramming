package com.sowp.user.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.sowp.user.R;
import com.sowp.user.models.Quiz;

import java.util.List;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private Context context;
    private List<Quiz> quizList;
    private OnQuizClickListener listener;

    public interface OnQuizClickListener {
        void onQuizClick(Quiz quiz);
    }

    public QuizAdapter(Context context, List<Quiz> quizList, OnQuizClickListener listener) {
        this.context = context;
        this.quizList = quizList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_quiz, parent, false);
        return new QuizViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        Quiz quiz = quizList.get(position);
        holder.bind(quiz, listener);
    }

    @Override
    public int getItemCount() {
        return quizList != null ? quizList.size() : 0;
    }

    public void updateData(List<Quiz> newQuizList) {
        this.quizList.clear();
        this.quizList.addAll(newQuizList);
        notifyDataSetChanged();
    }

    public static class QuizViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvQuizTitle;
        private final TextView tvQuizIcon;
        private final TextView tvCourseName;
        private final TextView tvQuizDescription;
        private final TextView tvTotalQuestions;
        private final TextView tvPassingScore;
        private final TextView tvQuizLevel;
        private final CardView quizCardView;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);

            tvQuizTitle = itemView.findViewById(R.id.tvQuizTitle);
            tvQuizIcon = itemView.findViewById(R.id.tvQuizIcon);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvQuizDescription = itemView.findViewById(R.id.tvQuizDescription);
            tvTotalQuestions = itemView.findViewById(R.id.tvTotalQuestions);
            tvPassingScore = itemView.findViewById(R.id.tvPassingScore);
            tvQuizLevel = itemView.findViewById(R.id.tvQuizLevel);
            quizCardView = itemView.findViewById(R.id.quizCardView);
        }

        void bind(Quiz quiz, OnQuizClickListener listener) {
            if (quiz == null) return;

            // Populate quiz data
            tvQuizTitle.setText(quiz.getTitle() != null ? quiz.getTitle() : "Untitled Quiz");
            tvQuizIcon.setText( String.valueOf(quiz.getQuizId()));
            tvQuizDescription.setText(quiz.getDescription() != null ? quiz.getDescription() : "No description available");
            tvTotalQuestions.setText(String.valueOf(quiz.getTotalQuestions()));
            tvPassingScore.setText(quiz.getPassingScore() + "%");
            tvQuizLevel.setText(quiz.getLevel() != null ? quiz.getLevel() : "Beginner");

            // Set course name (you'll need to fetch this from courseRepository)
            tvCourseName.setText("Course " + quiz.getCourseId());

            // Set click listener
            quizCardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuizClick(quiz);
                }
            });
        }
    }
}