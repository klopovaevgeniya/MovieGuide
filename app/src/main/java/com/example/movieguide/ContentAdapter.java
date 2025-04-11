package com.example.movieguide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ContentViewHolder> {

    private Context context;
    private List<Content> contentList;
    private OnContentActionListener actionListener;
    private boolean isAdmin;

    public ContentAdapter(Context context, List<Content> contentList, OnContentActionListener actionListener, boolean isAdmin) {
        this.context = context;
        this.contentList = contentList;
        this.actionListener = actionListener;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public ContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_content_admin, parent, false);
        return new ContentViewHolder(view, actionListener, contentList);
    }

    @Override
    public void onBindViewHolder(@NonNull ContentViewHolder holder, int position) {
        Content content = contentList.get(position);

        holder.textViewTitle.setText(content.getTitle());
        holder.textViewGenre.setText(content.getGenre());
        holder.textViewRating.setText(String.format("Рейтинг: %.1f", content.getRating()));
        holder.textViewDescription.setText("Тип: " + content.getContentType());

        Glide.with(context)
                .load(content.getImageUrl())
                .into(holder.imageView);

        if (isAdmin) {
            holder.buttonEdit.setVisibility(View.VISIBLE);
            holder.buttonDelete.setVisibility(View.VISIBLE);

            holder.buttonEdit.setOnClickListener(v -> actionListener.onEditContent(content));
            holder.buttonDelete.setOnClickListener(v -> actionListener.onDeleteContent(content));
        } else {
            holder.buttonEdit.setVisibility(View.GONE);
            holder.buttonDelete.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return contentList.size();
    }

    public static class ContentViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle, textViewDescription, textViewGenre, textViewRating;
        ImageView imageView;
        Button buttonEdit, buttonDelete;

        public ContentViewHolder(View itemView, OnContentActionListener listener, List<Content> contentList) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDescription = itemView.findViewById(R.id.textViewContentType);
            textViewGenre = itemView.findViewById(R.id.textViewGenre);
            textViewRating = itemView.findViewById(R.id.textViewRating);
            imageView = itemView.findViewById(R.id.imageView);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Content content = contentList.get(position);
                    listener.onContentClick(content);
                }
            });
        }
    }

    public interface OnContentActionListener {
        void onEditContent(Content content);
        void onDeleteContent(Content content);
        void onContentClick(Content content);
    }

    public void updateContentList(List<Content> newContentList) {
        this.contentList.clear();
        this.contentList.addAll(newContentList);
        notifyDataSetChanged();
    }
}