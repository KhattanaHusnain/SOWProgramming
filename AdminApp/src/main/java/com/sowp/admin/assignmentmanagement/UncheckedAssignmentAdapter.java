package com.sowp.admin.assignmentmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sowp.admin.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UncheckedAssignmentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 0;
    private static final int VIEW_TYPE_LOADING = 1;

    private List<UncheckedAssignment> assignments;
    private OnAssignmentClickListener listener;
    private boolean isLoadingFooterVisible = false;

    public interface OnAssignmentClickListener {
        void onAssignmentClick(UncheckedAssignment assignment);
    }

    public UncheckedAssignmentAdapter(List<UncheckedAssignment> assignments, OnAssignmentClickListener listener) {
        this.assignments = assignments;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingFooterVisible && position == assignments.size()) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_unchecked_assignment, parent, false);
            return new AssignmentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AssignmentViewHolder) {
            UncheckedAssignment assignment = assignments.get(position);
            ((AssignmentViewHolder) holder).bind(assignment, listener);
        }
    }

    @Override
    public int getItemCount() {
        return assignments.size() + (isLoadingFooterVisible ? 1 : 0);
    }

    public void showLoadingFooter(boolean show) {
        boolean wasVisible = isLoadingFooterVisible;
        isLoadingFooterVisible = show;

        if (wasVisible && !show) {
            notifyItemRemoved(assignments.size());
        } else if (!wasVisible && show) {
            notifyItemInserted(assignments.size());
        }
    }

    static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        private TextView textAssignmentTitle;
        private TextView textUserEmail;
        private TextView textCreatedAt;

        public AssignmentViewHolder(@NonNull View itemView) {
            super(itemView);
            textAssignmentTitle = itemView.findViewById(R.id.textAssignmentTitle);
            textUserEmail = itemView.findViewById(R.id.textUserEmail);
            textCreatedAt = itemView.findViewById(R.id.textCreatedAt);
        }

        public void bind(UncheckedAssignment assignment, OnAssignmentClickListener listener) {
            textAssignmentTitle.setText(assignment.getAssignmentTitle());
            textUserEmail.setText("Student: " + assignment.getUserEmail());

            // Format the created date
            if (assignment.getCreatedAt() != null) {
                Date date = new Date(assignment.getCreatedAt());
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault());
                textCreatedAt.setText("Submitted: " + sdf.format(date));
            } else {
                textCreatedAt.setText("Submitted: Unknown");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAssignmentClick(assignment);
                }
            });
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}