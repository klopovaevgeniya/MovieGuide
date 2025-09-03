package com.example.movieguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RecomendationAdapter extends RecyclerView.Adapter<RecomendationAdapter.RecommendationViewHolder> {

    private List<Content> contentList;
    private OnRecommendationClickListener listener;

    public interface OnRecommendationClickListener {
        void onRecommendationClick(Content content);
    }

    public RecomendationAdapter(List<Content> contentList, OnRecommendationClickListener listener) {
        this.contentList = contentList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        Content content = contentList.get(position);

        holder.titleTextView.setText(content.getTitle());
        holder.ratingTextView.setText(String.format("Рейтинг: %.1f", content.getRating()));

        Glide.with(holder.itemView.getContext())
                .load(content.getImageUrl())
                .placeholder(R.drawable.placeholder_poster)
                .into(holder.posterImageView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecommendationClick(content);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImageView;
        TextView titleTextView;
        TextView ratingTextView;

        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImageView = itemView.findViewById(R.id.posterImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            ratingTextView = itemView.findViewById(R.id.ratingTextView);
        }
    }
}