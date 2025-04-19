package com.android.nexcode.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;
import com.android.nexcode.course.Course;
import com.android.nexcode.course.Description;
import com.android.nexcode.databinding.HomeItemBinding;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.CourseViewHolder> {
    private final Context context;
    private List<Course> courses;
    private static final String LEARNING_FORMAT = "%d Learning";

    public HomeAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses != null ? courses : new ArrayList<>();
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        HomeItemBinding binding = HomeItemBinding.inflate(
                LayoutInflater.from(context), parent, false
        );
        return new CourseViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course, context);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    public void updateCourses(List<Course> newCourses) {
        this.courses = newCourses != null ? newCourses : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        private final HomeItemBinding binding;

        CourseViewHolder(@NonNull HomeItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Course course, Context context) {
            Glide.with(context)
                    .load(course.getIllustration())
                    .placeholder(R.drawable.course_placeholder)
                    .error(R.drawable.course_error)
                    .into(binding.courseImage);

            binding.courseTitle.setText(course.getTitle());
            binding.courseMembers.setText(String.format(LEARNING_FORMAT, course.getMembers()));

//            binding.openCourseButton.setOnClickListener(v -> navigateToCourseDescription(course, context));
            itemView.setOnClickListener(v -> navigateToCourseDescription(course,context));
        }

        private void navigateToCourseDescription(Course course, Context context) {
            Intent intent = new Intent(context, Description.class)
                    .putExtra("ID", course.getId());
            context.startActivity(intent);
        }
    }
}