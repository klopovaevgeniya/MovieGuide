package com.example.movieguide;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalitikFragment extends Fragment {

    private static final String TAG = "AnalitikFragment";
    private Button btnSelectContent;
    private RecyclerView statsRecyclerView;
    private LineChart chart;
    private FirebaseFirestore db;
    private StatsAdapter statsAdapter;
    private List<Stats> currentStatsList = new ArrayList<>();
    private String lastSelectedContentId = "";
    private boolean isChartVisible = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        setRetainInstance(true);

        if (savedInstanceState != null) {
            currentStatsList = savedInstanceState.getParcelableArrayList("statsList");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("statsList", new ArrayList<>(currentStatsList));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_analitik, container, false);

        btnSelectContent = view.findViewById(R.id.btnSelectContent);
        statsRecyclerView = view.findViewById(R.id.statsRecyclerView);
        chart = view.findViewById(R.id.chart);

        setupRecyclerView();
        setupChart();

        btnSelectContent.setOnClickListener(v -> showContentSelectionDialog());

        if (!currentStatsList.isEmpty()) {
            statsAdapter.updateStats(currentStatsList);
        }

        return view;
    }

    private void setupRecyclerView() {
        statsAdapter = new StatsAdapter(currentStatsList, stats -> {
            if (lastSelectedContentId.equals(stats.getContentId())) {
                if (isChartVisible) {
                    chart.setVisibility(View.GONE);
                    isChartVisible = false;
                } else {
                    loadReviewsForChart(stats.getContentId());
                    isChartVisible = true;
                }
            } else {
                lastSelectedContentId = stats.getContentId();
                loadReviewsForChart(stats.getContentId());
                isChartVisible = true;
            }
        });
        statsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        statsRecyclerView.setAdapter(statsAdapter);
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false);
        chart.setBackgroundColor(Color.parseColor("#121212"));
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setVisibility(View.GONE);
    }

    private void showContentSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Выберите контент");

        List<String> contentTitles = new ArrayList<>();
        List<String> contentIds = new ArrayList<>();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                contentTitles
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(Color.WHITE);
                return textView;
            }
        };

        db.collection("contents")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        contentTitles.clear();
                        contentIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            if (title != null) {
                                contentTitles.add(title);
                                contentIds.add(document.getId());
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (contentTitles.isEmpty()) {
                            Toast.makeText(getContext(), "Нет доступного контента", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });

        builder.setAdapter(adapter, (dialog, which) -> {
            String contentId = contentIds.get(which);
            loadContentStats(contentId);
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#121212")));
        dialog.show();
    }

    private void loadContentStats(String contentId) {
        chart.setVisibility(View.GONE);
        isChartVisible = false;
        Toast.makeText(getContext(), "Загрузка данных...", Toast.LENGTH_SHORT).show();

        db.collection("stats")
                .whereEqualTo("contentId", contentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentStatsList.clear();
                        if (!task.getResult().isEmpty()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                Stats stats = new Stats();
                                stats.setId(document.getId());
                                stats.setContentId(document.getString("contentId"));
                                stats.setContentTitle(document.getString("contentTitle"));
                                stats.setDate(document.getString("date"));
                                stats.setRating1Count(document.getLong("rating1Count").intValue());
                                stats.setRating2Count(document.getLong("rating2Count").intValue());
                                stats.setRating3Count(document.getLong("rating3Count").intValue());
                                stats.setRating4Count(document.getLong("rating4Count").intValue());
                                stats.setRating5Count(document.getLong("rating5Count").intValue());
                                currentStatsList.add(stats);
                            }
                        } else {
                            createStatsFromReviews(contentId);
                            return;
                        }
                        statsAdapter.updateStats(currentStatsList);
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки статистики", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createStatsFromReviews(String contentId) {
        db.collection("contents").document(contentId).get()
                .addOnSuccessListener(contentDoc -> {
                    String contentTitle = contentDoc.getString("title");

                    db.collection("reviews")
                            .whereEqualTo("contentId", contentId)
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    int[] ratingCounts = new int[5];

                                    for (DocumentSnapshot document : task.getResult()) {
                                        Review review = document.toObject(Review.class);
                                        if (review != null && review.getRating() >= 1 && review.getRating() <= 5) {
                                            ratingCounts[(int)review.getRating() - 1]++;
                                        }
                                    }

                                    String currentDate = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

                                    Map<String, Object> statsData = new HashMap<>();
                                    statsData.put("contentId", contentId);
                                    statsData.put("contentTitle", contentTitle != null ? contentTitle : "");
                                    statsData.put("date", currentDate);
                                    statsData.put("rating1Count", ratingCounts[0]);
                                    statsData.put("rating2Count", ratingCounts[1]);
                                    statsData.put("rating3Count", ratingCounts[2]);
                                    statsData.put("rating4Count", ratingCounts[3]);
                                    statsData.put("rating5Count", ratingCounts[4]);

                                    db.collection("stats")
                                            .add(statsData)
                                            .addOnSuccessListener(documentReference -> {
                                                Stats newStats = new Stats();
                                                newStats.setId(documentReference.getId());
                                                newStats.setContentId(contentId);
                                                newStats.setContentTitle(contentTitle);
                                                newStats.setDate(currentDate);
                                                newStats.setRating1Count(ratingCounts[0]);
                                                newStats.setRating2Count(ratingCounts[1]);
                                                newStats.setRating3Count(ratingCounts[2]);
                                                newStats.setRating4Count(ratingCounts[3]);
                                                newStats.setRating5Count(ratingCounts[4]);

                                                currentStatsList.add(newStats);
                                                statsAdapter.updateStats(currentStatsList);
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(getContext(), "Ошибка сохранения статистики", Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Toast.makeText(getContext(), "Ошибка загрузки отзывов", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки контента", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReviewsForChart(String contentId) {
        db.collection("reviews")
                .whereEqualTo("contentId", contentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Entry> entries = new ArrayList<>();
                        int index = 0;

                        for (DocumentSnapshot document : task.getResult()) {
                            Review review = document.toObject(Review.class);
                            if (review != null && review.getRating() >= 1 && review.getRating() <= 5) {
                                entries.add(new Entry(index++, review.getRating()));
                                Log.d(TAG, "Добавлена оценка: " + review.getRating() + " в позиции " + index);
                            }
                        }

                        if (entries.isEmpty()) {
                            showNoDataMessage();
                        } else {
                            showRatingChart(entries);
                        }
                    } else {
                        Toast.makeText(getContext(),
                                "Ошибка загрузки отзывов: " + task.getException(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRatingChart(List<Entry> entries) {
        try {
            chart.setVisibility(View.VISIBLE);

            LineDataSet dataSet = new LineDataSet(entries, "Оценки");
            dataSet.setColor(Color.parseColor("#fa1b35"));
            dataSet.setCircleColor(Color.parseColor("#410566"));
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);

            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
            chart.invalidate();
            chart.animateY(1000);

        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Ошибка при построении графика",
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Ошибка графика", e);
            chart.setVisibility(View.GONE);
        }
    }

    private void showNoDataMessage() {
        Toast.makeText(getContext(),
                "Нет данных оценок для отображения",
                Toast.LENGTH_SHORT).show();
        chart.setVisibility(View.GONE);
    }
}