package com.example.movieguide;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentDetailActivity extends AppCompatActivity {

    private TextView textViewTitle, textViewDescription, textViewGenre, textViewRating, tvNoReviews;
    private ImageView imageView;
    private ImageButton ibBack, ibFavorite;
    private Button btnTrailer;
    private RecyclerView rvReviews;

    private boolean isFavorite = false;
    private Content content;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_detail);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Инициализация существующих view
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewGenre = findViewById(R.id.textViewGenre);
        textViewRating = findViewById(R.id.textViewRating);
        imageView = findViewById(R.id.imageView);
        ibBack = findViewById(R.id.ibBack);
        ibFavorite = findViewById(R.id.ibFavorite);
        btnTrailer = findViewById(R.id.btnTrailer);

        // Инициализация новых view для отзывов
        tvNoReviews = findViewById(R.id.tvNoReviews);
        rvReviews = findViewById(R.id.rvReviews);

        // Настройка RecyclerView
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(reviewList);
        rvReviews.setAdapter(reviewAdapter);

        content = (Content) getIntent().getSerializableExtra("content");

        if (content != null) {
            textViewTitle.setText(content.getTitle());
            textViewDescription.setText(content.getDescription());
            textViewGenre.setText(content.getGenre());
            textViewRating.setText(String.format("Рейтинг: %.1f", content.getRating()));

            Glide.with(this)
                    .load(content.getImageUrl())
                    .into(imageView);

            checkIfFavorite();
            loadReviews(); // Загружаем отзывы

            ibBack.setOnClickListener(v -> finish());

            ibFavorite.setOnClickListener(v -> {
                if (currentUser == null) {
                    Toast.makeText(this, "Войдите, чтобы добавить в избранное", Toast.LENGTH_SHORT).show();
                    return;
                }

                isFavorite = !isFavorite;
                updateFavoriteIcon();

                if (isFavorite) {
                    saveToFavorites();
                } else {
                    removeFromFavorites();
                }
            });

            btnTrailer.setOnClickListener(v -> showTrailer(content.getTrailerUrl()));
        }
    }

    private void loadReviews() {
        if (content == null || content.getId() == null) {
            tvNoReviews.setVisibility(View.VISIBLE);
            rvReviews.setVisibility(View.GONE);
            return;
        }

        String contentId = content.getId();
        Log.d("Reviews", "Loading reviews for content: " + contentId);

        db.collection("reviews")
                .whereEqualTo("contentId", contentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reviewList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Review review = doc.toObject(Review.class);
                            review.setId(doc.getId());
                            reviewList.add(review);
                            Log.d("Reviews", "Found review by: " + review.getUserName());
                        }

                        // Сортировка отзывов по времени в коде
                        reviewList.sort((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()));

                        Log.d("Reviews", "Total reviews found: " + reviewList.size());
                        if (reviewList.isEmpty()) {
                            tvNoReviews.setVisibility(View.VISIBLE);
                            rvReviews.setVisibility(View.GONE);
                            tvNoReviews.setText("Пока нет отзывов");
                        } else {
                            tvNoReviews.setVisibility(View.GONE);
                            rvReviews.setVisibility(View.VISIBLE);
                        }
                        reviewAdapter.notifyDataSetChanged();
                    } else {
                        Log.e("Reviews", "Error loading reviews", task.getException());
                        tvNoReviews.setVisibility(View.VISIBLE);
                        rvReviews.setVisibility(View.GONE);
                        tvNoReviews.setText("Ошибка загрузки отзывов");
                    }
                });
    }

    private void checkIfFavorite() {
        if (currentUser == null) {
            ibFavorite.setVisibility(View.GONE);
            return;
        }

        db.collection("favorites")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("contentId", content.getId())
                .get()
                .addOnSuccessListener(query -> {
                    isFavorite = !query.isEmpty();
                    updateFavoriteIcon();
                })
                .addOnFailureListener(e -> {
                    Log.e("ContentDetail", "Error checking favorite", e);
                });
    }

    private void saveToFavorites() {
        if (currentUser == null) {
            Toast.makeText(this, "Войдите, чтобы добавить в избранное", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> favorite = new HashMap<>();
        favorite.put("userId", currentUser.getUid());
        favorite.put("contentId", content.getId());
        favorite.put("timestamp", FieldValue.serverTimestamp());

        db.collection("favorites")
                .add(favorite)
                .addOnSuccessListener(documentReference -> {
                    isFavorite = true;
                    updateFavoriteIcon();
                    Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFromFavorites() {
        db.collection("favorites")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("contentId", content.getId())
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot document : query.getDocuments()) {
                        db.collection("favorites")
                                .document(document.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    isFavorite = true;
                                    updateFavoriteIcon();
                                    Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    isFavorite = true;
                    updateFavoriteIcon();
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFavoriteIcon() {
        ibFavorite.setImageResource(isFavorite ?
                R.drawable.ic_favorite : R.drawable.ic_favorite_border);
    }

    private void showTrailer(String trailerUrl) {
        if (trailerUrl == null || trailerUrl.isEmpty()) {
            Toast.makeText(this, "Трейлер недоступен", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(trailerUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть трейлер", Toast.LENGTH_SHORT).show();
        }
    }
}