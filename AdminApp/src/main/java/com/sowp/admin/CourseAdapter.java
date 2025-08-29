package com.sowp.admin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private List<Course> courseList;
    private Context context;
    private OnCourseClickListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnCourseClickListener {
        void onCourseClick(Course course);
        void onEditClick(Course course);
        void onDeleteClick(Course course);
    }

    public CourseAdapter(List<Course> courseList, OnCourseClickListener listener) {
        this.courseList = courseList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);

        // Basic information
        holder.tvTitle.setText(course.getTitle());
        holder.tvCourseCode.setText(course.getCourseCode());
        holder.tvInstructor.setText("Instructor: " + (course.getInstructor() != null ? course.getInstructor() : "N/A"));
        holder.tvCategory.setText(course.getPrimaryCategory() != null ? course.getPrimaryCategory() : course.getCategory());
        holder.tvSemester.setText(course.getSemester());
        holder.tvDuration.setText(course.getDuration());

        // Description with truncation
        String description = course.getDescription();
        if (!TextUtils.isEmpty(description)) {
            if (description.length() > 150) {
                description = description.substring(0, 150) + "...";
            }
            holder.tvDescription.setText(description);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Course stats
        holder.tvLectures.setText(String.valueOf(course.getLectures()));
        holder.tvMembers.setText(String.valueOf(course.getMembers()));
        holder.tvCreditHours.setText(String.valueOf(course.getCreditHours()));

        // Level and type indicators
        holder.tvLevel.setText(course.getLevel());

        // Course type chips
        holder.chipGroup.removeAllViews();

        if (course.isLab()) {
            addChip(holder.chipGroup, "Lab", R.color.chip_lab);
        } else {
            addChip(holder.chipGroup, "Theory", R.color.chip_theory);
        }

        if (course.isComputer()) {
            addChip(holder.chipGroup, "Computer", R.color.chip_computer);
        }

        if (course.isPaid()) {
            addChip(holder.chipGroup, "Paid", R.color.chip_paid);
        } else {
            addChip(holder.chipGroup, "Free", R.color.chip_free);
        }

        if (!course.isPublic()) {
            addChip(holder.chipGroup, "Private", R.color.chip_private);
        }

        // Tags
        if (course.getTags() != null && !course.getTags().isEmpty()) {
            StringBuilder tagsText = new StringBuilder();
            for (int i = 0; i < Math.min(3, course.getTags().size()); i++) {
                if (i > 0) tagsText.append(" • ");
                tagsText.append(course.getTags().get(i));
            }
            if (course.getTags().size() > 3) {
                tagsText.append(" +").append(course.getTags().size() - 3).append(" more");
            }
            holder.tvTags.setText(tagsText.toString());
            holder.tvTags.setVisibility(View.VISIBLE);
        } else {
            holder.tvTags.setVisibility(View.GONE);
        }

        // Created date
        if (course.getCreatedAt() > 0) {
            Date date = new Date(course.getCreatedAt());
            holder.tvCreatedAt.setText("Created: " + dateFormat.format(date));
        } else {
            holder.tvCreatedAt.setText("");
        }

        // Course image
        loadCourseImage(holder.ivCourseImage, course.getIllustration());

        // Progress indicator
        holder.progressIndicator.setVisibility(course.getCompleted() ? View.VISIBLE : View.GONE);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCourseClick(course);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(course);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(course);
            }
        });

        // Card click animation
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCourseClick(course);
            }
        });
    }

    private void addChip(ChipGroup chipGroup, String text, int colorRes) {
        Chip chip = new Chip(context);
        chip.setText(text);
        chip.setChipBackgroundColorResource(colorRes);
        chip.setTextColor(context.getResources().getColor(android.R.color.white));
        chip.setTextSize(10f);
        chip.setClickable(false);
        chipGroup.addView(chip);
    }

    private void loadCourseImage(ImageView imageView, String base64Image) {
        if (!TextUtils.isEmpty(base64Image)) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (decodedByte != null) {
                    imageView.setImageBitmap(decodedByte);
                } else {
                    imageView.setImageResource(R.drawable.placeholder_course);
                }
            } catch (Exception e) {
                imageView.setImageResource(R.drawable.placeholder_course);
            }
        } else {
            imageView.setImageResource(R.drawable.placeholder_course);
        }
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        ImageView ivCourseImage;
        TextView tvTitle, tvCourseCode, tvInstructor, tvCategory, tvSemester, tvDuration;
        TextView tvDescription, tvLectures, tvMembers, tvCreditHours, tvLevel, tvTags, tvCreatedAt;
        ChipGroup chipGroup;
        MaterialButton btnEdit, btnDelete;
        View progressIndicator;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.cardView);
            ivCourseImage = itemView.findViewById(R.id.ivCourseImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCourseCode = itemView.findViewById(R.id.tvCourseCode);
            tvInstructor = itemView.findViewById(R.id.tvInstructor);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvSemester = itemView.findViewById(R.id.tvSemester);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLectures = itemView.findViewById(R.id.tvLectures);
            tvMembers = itemView.findViewById(R.id.tvMembers);
            tvCreditHours = itemView.findViewById(R.id.tvCreditHours);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvTags = itemView.findViewById(R.id.tvTags);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            chipGroup = itemView.findViewById(R.id.chipGroup);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            progressIndicator = itemView.findViewById(R.id.progressIndicator);
        }
    }
}