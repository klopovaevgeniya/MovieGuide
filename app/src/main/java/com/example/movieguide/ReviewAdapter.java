package com.example.movieguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviews;
    private OnItemClickListener onItemClickListener;

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.userNameTextView.setText(review.getUserName());
        holder.reviewTextView.setText(review.getReviewText());
        holder.ratingBar.setRating(review.getRating());

        // Обработка нажатия на элемент списка
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position, review);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView userNameTextView;
        TextView reviewTextView;
        RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            reviewTextView = itemView.findViewById(R.id.reviewTextView);
            ratingBar = itemView.findViewById(R.id.ratingBar);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, Review review);
    }
}