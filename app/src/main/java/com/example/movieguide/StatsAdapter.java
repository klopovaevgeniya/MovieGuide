package com.example.movieguide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.StatsViewHolder> {

    private List<Stats> statsList;
    private final StatsClickListener clickListener;

    public StatsAdapter(List<Stats> statsList, StatsClickListener clickListener) {
        this.statsList = statsList;
        this.clickListener = clickListener;
    }

    public void updateStats(List<Stats> newStats) {
        this.statsList = newStats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stats, parent, false);
        return new StatsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatsViewHolder holder, int position) {
        Stats stats = statsList.get(position);
        holder.tvContentName.setText(stats.getContentTitle());
        holder.tvDate.setText(stats.getDate());

        holder.itemView.setOnClickListener(v -> clickListener.onStatsClick(stats));
    }

    @Override
    public int getItemCount() {
        return statsList.size();
    }

    static class StatsViewHolder extends RecyclerView.ViewHolder {
        TextView tvContentName;
        TextView tvDate;

        public StatsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContentName = itemView.findViewById(R.id.tvContentName);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }

    public interface StatsClickListener {
        void onStatsClick(Stats stats);
    }
}