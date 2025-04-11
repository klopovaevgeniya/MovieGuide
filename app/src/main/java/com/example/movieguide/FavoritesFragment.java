package com.example.movieguide;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private FavoritesAdapter adapter;
    private List<Content> favoriteContents = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        emptyStateText = view.findViewById(R.id.emptyStateText);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new FavoritesAdapter(favoriteContents, new FavoritesAdapter.FavoriteClickListener() {
            @Override
            public void onFavoriteClicked(Content content) {
                removeFromFavorites(content);
            }

            @Override
            public void onAddToHistoryClicked(Content content) {
                addToHistory(content);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        auth.addAuthStateListener(firebaseAuth -> checkAuthAndLoad());
    }

    @Override
    public void onStart() {
        super.onStart();
        checkAuthAndLoad();
    }

    private void checkAuthAndLoad() {
        if (auth.getCurrentUser() == null) {
            showEmptyState("Войдите, чтобы просмотреть избранное");
            favoriteContents.clear();
            adapter.updateContents(favoriteContents);
            return;
        }
        loadFavorites();
    }

    private void loadFavorites() {
        String userId = auth.getCurrentUser().getUid();
        Log.d("Favorites", "Loading favorites for user: " + userId);

        db.collection("favorites")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> contentIds = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String contentId = doc.getString("contentId");
                            Log.d("Favorites", "Found favorite: " + contentId);
                            if (contentId != null) {
                                contentIds.add(contentId);
                            }
                        }

                        Log.d("Favorites", "Total favorites found: " + contentIds.size());
                        if (contentIds.isEmpty()) {
                            showEmptyState("В избранном пока ничего нет");
                            favoriteContents.clear();
                            adapter.updateContents(favoriteContents);
                            return;
                        }

                        loadContentDetails(contentIds);
                    } else {
                        Log.e("Favorites", "Error loading favorites", task.getException());
                        showEmptyState("Ошибка загрузки избранного");
                    }
                });
    }

    private void loadContentDetails(List<String> contentIds) {
        Log.d("Favorites", "Loading content details for IDs: " + contentIds);

        db.collection("contents")
                .whereIn(FieldPath.documentId(), contentIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        favoriteContents.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Log.d("Favorites", "Found content: " + doc.getId());
                            Content content = doc.toObject(Content.class);
                            content.setId(doc.getId());
                            favoriteContents.add(content);
                        }

                        Log.d("Favorites", "Loaded contents: " + favoriteContents.size());
                        if (favoriteContents.isEmpty()) {
                            showEmptyState("В избранном пока ничего нет");
                        } else {
                            showContentList();
                        }
                        adapter.updateContents(favoriteContents);
                    } else {
                        Log.e("Favorites", "Error loading content", task.getException());
                        showEmptyState("Ошибка загрузки данных");
                    }
                });
    }
    private void removeFromFavorites(Content content) {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();

        db.collection("favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("contentId", content.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            db.collection("favorites")
                                    .document(doc.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        favoriteContents.remove(content);
                                        adapter.updateContents(favoriteContents);
                                        if (favoriteContents.isEmpty()) {
                                            showEmptyState("В избранном пока ничего нет");
                                        }
                                        Toast.makeText(getContext(), "Удалено из избранного", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                                        Log.e("Favorites", "Error removing favorite", e);
                                    });
                        }
                    }
                });
    }

    private void addToHistory(Content content) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Войдите, чтобы сохранить историю просмотров", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        String userId = currentUser.getUid();
        String contentId = content.getId();

        Map<String, Object> historyItem = new HashMap<>();
        historyItem.put("userId", userId);
        historyItem.put("contentId", contentId);
        historyItem.put("contentTitle", content.getTitle());
        historyItem.put("timestamp", FieldValue.serverTimestamp());

        db.collection("history")
                .add(historyItem)
                .addOnSuccessListener(documentReference -> {
                    removeFromFavorites(content);

                    Toast.makeText(getContext(), "Добавлено в историю: " + content.getTitle(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при сохранении истории: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEmptyState(String message) {
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }

    private void showContentList() {
        emptyStateText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}