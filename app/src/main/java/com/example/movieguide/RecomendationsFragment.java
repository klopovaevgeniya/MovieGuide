package com.example.movieguide;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RecomendationsFragment extends Fragment {

    private RecyclerView recommendationsRecyclerView;
    private TextView recommendationsTitle;
    private RecomendationAdapter adapter;
    private List<Content> recommendedContentList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recomendation_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recommendationsTitle = view.findViewById(R.id.recommendationsTitle);
        recommendationsRecyclerView = view.findViewById(R.id.recommendationsRecyclerView);

        setupRecyclerView();
        loadRecommendedContent();
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recommendationsRecyclerView.setLayoutManager(layoutManager);

        adapter = new RecomendationAdapter(recommendedContentList, content -> {
            Intent intent = new Intent(getActivity(), ContentDetailActivity.class);
            intent.putExtra("content", content);
            startActivity(intent);
        });

        recommendationsRecyclerView.setAdapter(adapter);
    }

    private void loadRecommendedContent() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("contents")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        recommendedContentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Content content = document.toObject(Content.class);
                            content.setId(document.getId());

                            if (content.getRating() > 4.5f) {
                                recommendedContentList.add(content);
                            }
                        }

                        if (recommendedContentList.isEmpty()) {
                            recommendationsTitle.setText("Нет рекомендаций");
                            Toast.makeText(getContext(), "Нет подходящего контента",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            recommendationsTitle.setText(String.format("Рекомендовано для Вас",
                                    recommendedContentList.size()));
                        }

                        adapter.notifyDataSetChanged();
                    } else {
                        recommendationsTitle.setText("Ошибка загрузки данных");
                        Toast.makeText(getContext(), "Ошибка загрузки данных",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}