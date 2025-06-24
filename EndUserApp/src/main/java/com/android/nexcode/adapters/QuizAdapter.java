package com.android.nexcode.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.android.nexcode.R;
import com.android.nexcode.models.Quiz;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private Context context;
    private List<Quiz> quizList;
    private OnQuizItemClickListener onQuizItemClickListener;

    public interface OnQuizItemClickListener {
        void onQuizItemClick(Quiz quiz);
    }

    public QuizAdapter(Context context, List<Quiz> quizList) {
        this.context = context;
        this.quizList = quizList;
    }

    public void setOnQuizItemClickListener(OnQuizItemClickListener listener) {
        this.onQuizItemClickListener = listener;
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
        holder.bind(quiz);
    }

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

        public void bind(Quiz quiz) {
            // Set title
            titleTextView.setText(quiz.getTitle() != null ? quiz.getTitle() : "Untitled Quiz");

            // Set description
            descriptionTextView.setText(quiz.getDescription() != null ? quiz.getDescription() : "No description available");

            // Set due date
            if (quiz.getDueDate() != null) {
                dueDateTextView.setText(quiz.getDueDate());
            } else {
                dueDateTextView.setText("No due date");
            }

            // Check if quiz is overdue and update button accordingly
            updateButtonState(quiz);

            // Set click listeners
            startQuizButton.setOnClickListener(v -> {
                if (onQuizItemClickListener != null) {
                    onQuizItemClickListener.onQuizItemClick(quiz);
                }
            });

            // Set item click listener for the entire card
            itemView.setOnClickListener(v -> {
                if (onQuizItemClickListener != null) {
                    onQuizItemClickListener.onQuizItemClick(quiz);
                }
            });
        }

        private void updateButtonState(Quiz quiz) {
//            if (quiz.getDueDate() != null) {
//                Date currentDate = new Date();
//                if (quiz.getDueDate().before(currentDate)) {
//                    // Quiz is overdue
//                    startQuizButton.setText("Overdue");
//                    startQuizButton.setEnabled(false);
//                    startQuizButton.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
//                } else {
//                    // Quiz is still available
//                    startQuizButton.setText("Start Quiz");
//                    startQuizButton.setEnabled(true);
//                }
//            } else {
                // No due date, quiz is always available
                startQuizButton.setText("Start Quiz");
                startQuizButton.setEnabled(true);
            //}
        }

    }
}