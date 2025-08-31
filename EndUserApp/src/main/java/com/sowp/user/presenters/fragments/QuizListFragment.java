//package com.sowp.user.presenters.fragments;
//
//import android.content.Context;
//import android.content.Intent;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Parcelable;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
//
//import com.sowp.user.R;
//import com.sowp.user.models.Quiz;
//import com.sowp.user.models.User;
//import com.sowp.user.presenters.activities.QuizActivity;
//import com.sowp.user.repositories.firebase.QuizRepository;
//import com.sowp.user.repositories.firebase.UserRepository;
//import com.google.android.material.button.MaterialButton;
//import com.google.firebase.firestore.DocumentSnapshot;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//public class QuizListFragment extends Fragment {
//
//    private static final String ARG_QUIZ_LIST = "quiz_list";
//    private List<Parcelable> quizList = new ArrayList<>();
//    private RecyclerView recyclerView;
//    private TextView emptyView;
//    private SwipeRefreshLayout swipeRefreshLayout;
//    private ProgressBar loadMoreProgressBar;
//
//    private QuizAdapter adapter;
//    private AssessmentFragment parentFragment;
//    private boolean isLoading = false;
//    private boolean hasMoreData = false;
//
//    public QuizListFragment() {
//        // Required empty constructor
//    }
//
//    public static QuizListFragment newInstance(List<Quiz> quizList, AssessmentFragment parentFragment) {
//        QuizListFragment fragment = new QuizListFragment();
//        Bundle args = new Bundle();
//
//        fragment.parentFragment = parentFragment;
//        return fragment;
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
//                quizList = getArguments().getParcelableArrayList(ARG_QUIZ_LIST).reversed();
//            }
//        }
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_quiz_list, container, false);
//
//        recyclerView = view.findViewById(R.id.recyclerView);
//        emptyView = view.findViewById(R.id.emptyView);
//        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
//        loadMoreProgressBar = view.findViewById(R.id.loadMoreProgressBar);
//
//        setupSwipeRefresh();
//        setupRecyclerView();
//
//        return view;
//    }
//
//    private void setupSwipeRefresh() {
//        swipeRefreshLayout.setOnRefreshListener(() -> {
//            if (parentFragment != null) {
//                parentFragment.refreshQuizzes(new QuizRepository.PaginatedCallback() {
//                    @Override
//                    public void onSuccess(List<Quiz> quizzes, DocumentSnapshot lastDocument, boolean hasMore) {
//                        quizList.clear();
//                       // quizList.addAll((Collection<? extends Parcelable>) quizzes);
//                        hasMoreData = hasMore;
//                        adapter.notifyDataSetChanged();
//                        updateEmptyView();
//                        swipeRefreshLayout.setRefreshing(false);
//                        Toast.makeText(getContext(), "Quizzes refreshed", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onFailure(String message) {
//                        swipeRefreshLayout.setRefreshing(false);
//                        Toast.makeText(getContext(), "Failed to refresh: " + message, Toast.LENGTH_SHORT).show();
//                    }
//                });
//            } else {
//                swipeRefreshLayout.setRefreshing(false);
//            }
//        });
//    }
//
//    private void setupRecyclerView() {
//        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
//        recyclerView.setLayoutManager(layoutManager);
//
//        adapter = new QuizAdapter(new ArrayList<>(), getContext());
//        recyclerView.setAdapter(adapter);
//
//        // Add scroll listener for pagination
//        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//
//                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
//                if (layoutManager != null && !isLoading && hasMoreData) {
//                    int visibleItemCount = layoutManager.getChildCount();
//                    int totalItemCount = layoutManager.getItemCount();
//                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
//
//                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
//                            && firstVisibleItemPosition >= 0) {
//                        loadMoreQuizzes();
//                    }
//                }
//            }
//        });
//
//        updateEmptyView();
//    }
//
//    private void loadMoreQuizzes() {
//        if (parentFragment == null || isLoading) return;
//
//        isLoading = true;
//        loadMoreProgressBar.setVisibility(View.VISIBLE);
//
//        parentFragment.loadMoreQuizzes(new QuizRepository.PaginatedCallback() {
//            @Override
//            public void onSuccess(List<Quiz> quizzes, DocumentSnapshot lastDocument, boolean hasMore) {
//                isLoading = false;
//                loadMoreProgressBar.setVisibility(View.GONE);
//                hasMoreData = hasMore;
//
//                if (!quizzes.isEmpty()) {
//                    int startPosition = quizList.size();
//                   // quizList.addAll(quizzes);
//                    adapter.notifyItemRangeInserted(startPosition, quizzes.size());
//                }
//
//                updateEmptyView();
//            }
//
//            @Override
//            public void onFailure(String message) {
//                isLoading = false;
//                loadMoreProgressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), "Failed to load more: " + message, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void updateEmptyView() {
//        if (quizList.isEmpty()) {
//            recyclerView.setVisibility(View.GONE);
//            emptyView.setVisibility(View.VISIBLE);
//            emptyView.setText("No quizzes available");
//        } else {
//            recyclerView.setVisibility(View.VISIBLE);
//            emptyView.setVisibility(View.GONE);
//        }
//    }
//
//    // RecyclerView adapter for Quizzes
//    private class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {
//
//        private final List<Quiz> quizList;
//        UserRepository userRepository;
//
//        public QuizAdapter(List<Quiz> quizList, Context context) {
//            this.quizList = quizList;
//            userRepository = new UserRepository(context);
//        }
//
//        @NonNull
//        @Override
//        public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.item_quiz, parent, false);
//            return new QuizViewHolder(view);
//        }
//
//        @Override
//        public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
//            Quiz quiz = quizList.get(position);
//            holder.titleTextView.setText(quiz.getTitle());
//            holder.descriptionTextView.setText(quiz.getDescription());
//
//            holder.startQuizButton.setOnClickListener(v -> {
//                Intent intent = new Intent(getActivity(), QuizActivity.class);
//                intent.putExtra("QUIZ_ID", quiz.getQuizId());
//                userRepository.addQuiz(String.valueOf(quiz.getQuizId()), new UserRepository.UserCallback() {
//
//                    @Override
//                    public void onSuccess(User user) {
//                        // nothing
//                    }
//
//                    @Override
//                    public void onFailure(String message) {
//                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
//                    }
//                });
//                startActivity(intent);
//            });
//        }
//
//        @Override
//        public int getItemCount() {
//            return quizList.size();
//        }
//
//        class QuizViewHolder extends RecyclerView.ViewHolder {
//            TextView titleTextView;
//            TextView descriptionTextView;
//            TextView dueDateTextView;
//            MaterialButton startQuizButton;
//
//            public QuizViewHolder(@NonNull View itemView) {
//                super(itemView);
//                titleTextView = itemView.findViewById(R.id.titleTextView);
//                descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
//                dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
//                startQuizButton = itemView.findViewById(R.id.startQuizButton);
//            }
//        }
//    }
//}