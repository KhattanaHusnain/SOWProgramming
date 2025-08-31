package com.sowp.user.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.presenters.activities.QuizActivity;
import com.google.android.material.button.MaterialButton;
import com.sowp.user.R;
import com.sowp.user.models.Quiz;

import java.util.List;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private Context context;
    private List<Quiz> quizList;


    public QuizAdapter(Context context, List<Quiz> quizList) {
        this.context = context;
        this.quizList = quizList;
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
        // Set title
        holder.titleTextView.setText(quiz.getTitle() != null ? quiz.getTitle() : "Untitled Quiz");

        // Set description
        holder.descriptionTextView.setText(quiz.getDescription() != null ? quiz.getDescription() : "No description available");

        // Set due date

            holder.dueDateTextView.setText("No due date");

        // Check if quiz is overdue and update button accordingly
        //updateButtonState(quiz, holder);

        // Set click listeners
        holder.startQuizButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, QuizActivity.class);
            intent.putExtra("QUIZ_ID", quiz.getQuizId());
            context.startActivity(intent);
        });

        // Set item click listener for the entire card
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, QuizActivity.class);
            intent.putExtra("QUIZ_ID", quiz.getQuizId());
            context.startActivity(intent);
        });
    }
//    private void updateButtonState(Quiz quiz, QuizViewHolder holder) {
////            if (quiz.getDueDate() != null) {
////                Date currentDate = new Date();
////                if (quiz.getDueDate().before(currentDate)) {
////                    // Quiz is overdue
////                    startQuizButton.setText("Overdue");
////                    startQuizButton.setEnabled(false);
////                    startQuizButton.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
////                } else {
////                    // Quiz is still available
////                    startQuizButton.setText("Start Quiz");
////                    startQuizButton.setEnabled(true);
////                }
////            } else {
//        // No due date, quiz is always available
//        holder.startQuizButton.setText("Start Quiz");
//        startQuizButton.setEnabled(true);
//        //}
//    }


    @Override
    public int getItemCount() {
        return quizList != null ? quizList.size() : 0;
    }

    public void updateQuizzes(List<Quiz> newQuizList) {
        this.quizList = newQuizList;
        notifyDataSetChanged();
    }

    public class QuizViewHolder extends RecyclerView.ViewHolder {

        private TextView titleTextView;
        private TextView descriptionTextView;
        private TextView dueDateTextView;
        private MaterialButton startQuizButton;

        public QuizViewHolder(@NonNull View itemView) {
            super(itemView);

            titleTextView = itemView.findViewById(R.id.titleTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
            startQuizButton = itemView.findViewById(R.id.startQuizButton);
        }

    }
}