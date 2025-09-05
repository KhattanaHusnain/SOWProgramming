package com.sowp.user.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.user.R;
import com.sowp.user.models.QuizAttempt.QuestionAttempt;

import java.util.ArrayList;
import java.util.List;

public class QuestionDetailAdapter extends RecyclerView.Adapter<QuestionDetailAdapter.QuestionDetailViewHolder> {

    private List<QuestionAttempt> questionAttempts;

    public QuestionDetailAdapter(List<QuestionAttempt> questionAttempts) {
        this.questionAttempts = questionAttempts != null ? questionAttempts : new ArrayList<>();
    }

    @NonNull
    @Override
    public QuestionDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question_detail, parent, false);
        return new QuestionDetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionDetailViewHolder holder, int position) {
        QuestionAttempt questionAttempt = questionAttempts.get(position);
        holder.bind(questionAttempt);
    }

    @Override
    public int getItemCount() {
        return questionAttempts != null ? questionAttempts.size() : 0;
    }

    public static class QuestionDetailViewHolder extends RecyclerView.ViewHolder {
        private TextView questionNumberText;
        private TextView resultStatusText;
        private TextView questionText;
        private LinearLayout optionsContainer;
        private TextView userAnswerText;
        private TextView correctAnswerText;
        private Context context;

        public QuestionDetailViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            questionNumberText = itemView.findViewById(R.id.questionNumberText);
            resultStatusText = itemView.findViewById(R.id.resultStatusText);
            questionText = itemView.findViewById(R.id.questionText);
            optionsContainer = itemView.findViewById(R.id.optionsContainer);
            userAnswerText = itemView.findViewById(R.id.userAnswerText);
            correctAnswerText = itemView.findViewById(R.id.correctAnswerText);
        }

        public void bind(QuestionAttempt questionAttempt) {
            questionNumberText.setText(String.format("Q%d.", questionAttempt.getQuestionNumber()));

            String status = questionAttempt.getResultStatus();
            resultStatusText.setText(status);

            switch (status) {
                case "CORRECT":
                    resultStatusText.setBackgroundResource(R.drawable.bg_status_correct);
                    resultStatusText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    break;
                case "INCORRECT":
                    resultStatusText.setBackgroundResource(R.drawable.bg_status_incorrect);
                    resultStatusText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    break;
                case "NOT ANSWERED":
                    resultStatusText.setBackgroundResource(R.drawable.bg_status_not_answered);
                    resultStatusText.setTextColor(ContextCompat.getColor(context, R.color.white));
                    break;
            }

            questionText.setText(questionAttempt.getQuestionText());

            optionsContainer.removeAllViews();
            if (questionAttempt.getOptions() != null && !questionAttempt.getOptions().isEmpty()) {
                for (int i = 0; i < questionAttempt.getOptions().size(); i++) {
                    String option = questionAttempt.getOptions().get(i);
                    TextView optionView = createOptionView(option, i, questionAttempt);
                    optionsContainer.addView(optionView);
                }
            }

            String userAnswer = questionAttempt.getUserAnswer();
            userAnswerText.setText(userAnswer != null && !userAnswer.trim().isEmpty() ?
                    userAnswer : "Not answered");
            correctAnswerText.setText(questionAttempt.getCorrectAnswer());

            if (questionAttempt.wasAnswered()) {
                if (questionAttempt.isCorrect()) {
                    userAnswerText.setTextColor(ContextCompat.getColor(context, R.color.success));
                } else {
                    userAnswerText.setTextColor(ContextCompat.getColor(context, R.color.error));
                }
            } else {
                userAnswerText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }
        }

        private TextView createOptionView(String option, int index, QuestionAttempt questionAttempt) {
            TextView optionView = new TextView(context);

            char optionLetter = (char) ('A' + index);
            optionView.setText(String.format("%c. %s", optionLetter, option));

            optionView.setTextSize(14);
            optionView.setPadding(16, 12, 16, 12);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 4, 0, 4);
            optionView.setLayoutParams(params);

            String userAnswer = questionAttempt.getUserAnswer();
            String correctAnswer = questionAttempt.getCorrectAnswer();

            boolean isUserAnswer = option.equals(userAnswer);
            boolean isCorrectAnswer = option.equals(correctAnswer);

            if (isCorrectAnswer) {
                optionView.setBackgroundResource(R.drawable.bg_option_correct);
                optionView.setTextColor(ContextCompat.getColor(context, R.color.success));
                optionView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else if (isUserAnswer && !questionAttempt.isCorrect()) {
                optionView.setBackgroundResource(R.drawable.bg_option_incorrect);
                optionView.setTextColor(ContextCompat.getColor(context, R.color.error));
                optionView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                optionView.setBackgroundResource(R.drawable.bg_option_default);
                optionView.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }

            return optionView;
        }
    }
}