package com.example.movieguide;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    interface FavoriteClickListener {
        void onFavoriteClicked(Content content);
        void onAddToHistoryClicked(Content content); // Добавлен отдельный метод для истории
    }

    private List<Content> contents;
    private FavoriteClickListener listener;

    public FavoritesAdapter(List<Content> contents, FavoriteClickListener listener) {
        this.contents = new ArrayList<>(contents); // Защитная копия
        this.listener = listener;
    }

    public void updateContents(List<Content> newContents) {
        this.contents = new ArrayList<>(newContents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Content content = contents.get(position);
        holder.bind(content, listener);
    }

    @Override
    public int getItemCount() {
        return contents.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView textViewTitle;
        private final TextView textViewGenre;
        private final TextView textViewRating;
        private final TextView textViewType;
        private final Button btnRemove;
        private final Button btnAddToHistory;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewGenre = itemView.findViewById(R.id.textViewGenre);
            textViewRating = itemView.findViewById(R.id.textViewRating);
            textViewType = itemView.findViewById(R.id.textViewType);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            btnAddToHistory = itemView.findViewById(R.id.btnAddToHistory);
        }

        public void bind(Content content, FavoriteClickListener listener) {
            // Установка текстовых значений
            textViewTitle.setText(content.getTitle());
            textViewGenre.setText(content.getGenre());
            textViewRating.setText(String.format("Рейтинг: %.1f", content.getRating()));
            textViewType.setText(String.format("Тип: %s", content.getContentType()));

            // Загрузка изображения с обработкой ошибок
            Glide.with(itemView.getContext())
                    .load(content.getImageUrl())
                    .into(imageView);

            // Обработчики кликов
            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClicked(content);
                }
            });

            btnAddToHistory.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToHistoryClicked(content); // Используем новый метод
                }
            });

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(itemView.getContext(), ContentDetailActivity.class);
                intent.putExtra("content", content);
                itemView.getContext().startActivity(intent);
            });
        }
    }
}