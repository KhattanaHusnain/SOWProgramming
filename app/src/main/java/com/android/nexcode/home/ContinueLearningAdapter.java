package com.android.nexcode.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;

import java.util.List;

public class ContinueLearningAdapter extends RecyclerView.Adapter<ContinueLearningAdapter.ContinueLearningViewHolder> {

    private final Context context;
    private List<UserProgress> progressItems;
    private OnProgressItemClickListener listener;

    public ContinueLearningAdapter(Context context, List<UserProgress> progressItems) {
        this.context = context;
        this.progressItems = progressItems;
    }

    public void setOnProgressItemClickListener(OnProgressItemClickListener listener) {
        this.listener = listener;
    }

    public void updateProgressItems(List<UserProgress> newProgressItems) {
        this.progressItems = newProgressItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContinueLearningViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.continue_learning_item, parent, false);
        return new ContinueLearningViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContinueLearningViewHolder holder, int position) {
        UserProgress progress = progressItems.get(position);

        holder.courseTitle.setText(progress.getCourseTitle());

        // Set the current module info
        if (progress.getCurrentModuleName() != null) {
            holder.moduleInfo.setText("Module " + progress.getCurrentModuleNumber() + ": " +
                    progress.getCurrentModuleName());
        } else {
            holder.moduleInfo.setText("Not started yet");
        }

        // Set progress
        int progressPercentage = progress.getCompletionPercentage();
        holder.courseProgress.setProgress(progressPercentage);
        holder.progressText.setText(progressPercentage + "% Completed");

        // Set time remaining
        if (progress.getEstimatedTimeRemaining() > 0) {
            holder.timeRemaining.setText(progress.getEstimatedTimeRemaining() + " minutes left");
        } else {
            holder.timeRemaining.setText("Just started");
        }

        // Set thumbnail if available
        if (progress.getCourseImageResourceId() != 0) {
            holder.courseThumbnail.setImageResource(progress.getCourseImageResourceId());
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProgressItemClick(progress);
            }
        });
    }

    @Override
    public int getItemCount() {
        return progressItems.size();
    }

    static class ContinueLearningViewHolder extends RecyclerView.ViewHolder {
        ImageView courseThumbnail;
        TextView courseTitle;
        TextView moduleInfo;
        TextView progressText;
        TextView timeRemaining;
        ProgressBar courseProgress;

        public ContinueLearningViewHolder(@NonNull View itemView) {
            super(itemView);
            courseThumbnail = itemView.findViewById(R.id.course_thumbnail);
            courseTitle = itemView.findViewById(R.id.course_title);
            moduleInfo = itemView.findViewById(R.id.module_info);
            progressText = itemView.findViewById(R.id.progress_text);
            timeRemaining = itemView.findViewById(R.id.time_remaining);
            courseProgress = itemView.findViewById(R.id.course_progress);
        }
    }

    public interface OnProgressItemClickListener {
        void onProgressItemClick(UserProgress progress);
    }
}