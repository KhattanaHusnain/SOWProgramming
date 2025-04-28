package com.android.nexcode.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.nexcode.R;
import java.util.ArrayList;
import java.util.List;

public class QuizListFragment extends Fragment {

    private static final String ARG_QUIZ_LIST = "quiz_list";
    private List<Quiz> quizList = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView emptyView;

    public QuizListFragment() {
        // Required empty constructor
    }

    public static QuizListFragment newInstance(List<Quiz> quizList) {
        QuizListFragment fragment = new QuizListFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_QUIZ_LIST, new ArrayList<>(quizList));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            quizList = getArguments().getParcelableArrayList(ARG_QUIZ_LIST);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);

        setupRecyclerView();

        return view;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        QuizAdapter adapter = new QuizAdapter(quizList);
        recyclerView.setAdapter(adapter);

        // Show empty view if list is empty
        if (quizList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("No data available");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    // RecyclerView adapter for Quizzes
    private class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

        private final List<Quiz> quizList;

        public QuizAdapter(List<Quiz> quizList) {
            this.quizList = quizList;
        }

        @NonNull
        @Override
        public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz, parent, false);
            return new QuizViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
            Quiz quiz = quizList.get(position);
            holder.titleTextView.setText(quiz.getTitle());
            holder.descriptionTextView.setText(quiz.getDescription());
            holder.dueDateTextView.setText(quiz.getDueDate());

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), QuizActivity.class);
                intent.putExtra("QUIZ_ID", quiz.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return quizList.size();
        }

        class QuizViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView;
            TextView descriptionTextView;
            TextView dueDateTextView;

            public QuizViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.titleTextView);
                descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
                dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
            }
        }
    }
}