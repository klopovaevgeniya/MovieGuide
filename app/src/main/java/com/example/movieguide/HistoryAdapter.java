package com.example.movieguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<Content> contents;
    private final HistoryClickListener clickListener;

    public interface HistoryClickListener {
        void onHistoryItemClicked(Content content);
        void onRemoveFromHistoryClicked(Content content);
    }

    public HistoryAdapter(List<Content> contents, HistoryClickListener listener) {
        this.contents = contents;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Content content = contents.get(position);

        holder.title.setText(content.getTitle());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onHistoryItemClicked(content);
            }
        });

        holder.removeBtn.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRemoveFromHistoryClicked(content);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contents.size();
    }

    public void updateContents(List<Content> newContents) {
        contents = newContents;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageButton removeBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.history_item_title);
            removeBtn = itemView.findViewById(R.id.history_remove_btn);
        }
    }
    public void updateContentRating(String contentId, float newRating) {
        for (Content content : contents) {
            if (content.getId().equals(contentId)) {
                notifyDataSetChanged();
                break;
            }
        }
    }
}
