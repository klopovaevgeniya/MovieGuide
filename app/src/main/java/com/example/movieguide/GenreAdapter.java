package com.example.movieguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {

    private List<Genre> genres;
    private OnGenreClickListener listener;

    public GenreAdapter(List<Genre> genres, OnGenreClickListener listener) {
        this.genres = genres;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_genre, parent, false);
        return new GenreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        Genre genre = genres.get(position);
        holder.bind(genre, listener);
    }

    @Override
    public int getItemCount() {
        return genres.size();
    }

    public static class GenreViewHolder extends RecyclerView.ViewHolder {
        private TextView genreName;
        private Button editButton;
        private Button deleteButton;

        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
            genreName = itemView.findViewById(R.id.genreName);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Genre genre, OnGenreClickListener listener) {
            genreName.setText(genre.getName());

            editButton.setOnClickListener(v -> listener.onEditClick(genre));
            deleteButton.setOnClickListener(v -> listener.onDeleteClick(genre));
        }
    }

    public interface OnGenreClickListener {
        void onEditClick(Genre genre);
        void onDeleteClick(Genre genre);
    }
}