package com.example.movieguide;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private HistoryAdapter adapter;
    private List<Content> historyContents = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ContentAdapter contentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        emptyStateText = findViewById(R.id.emptyStateText);
        ImageButton backButton = findViewById(R.id.backButton);
        Button clearButton = findViewById(R.id.clearButton);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        adapter = new HistoryAdapter(historyContents, new HistoryAdapter.HistoryClickListener() {
            @Override
            public void onHistoryItemClicked(Content content) {
                showReviewDialog(content);
            }

            @Override
            public void onRemoveFromHistoryClicked(Content content) {
                showDeleteConfirmationDialog(content);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> finish());
        clearButton.setOnClickListener(v -> showClearHistoryDialog());

        loadHistory();
    }

    private void loadHistory() {
        FirebaseUser  currentUser  = auth.getCurrentUser ();
        if (currentUser  == null) {
            showEmptyState("Пользователь не авторизован");
            return;
        }

        String userId = currentUser .getUid();
        Log.d("History", "Loading history for user: " + userId);

        db.collection("history")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> contentIds = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String contentId = doc.getString("contentId");
                            Log.d("History", "Found history: " + contentId);
                            if (contentId != null) {
                                contentIds.add(contentId);
                            }
                        }

                        Log.d("Favorites", "Total favorites found: " + contentIds.size());
                        if (contentIds.isEmpty()) {
                            showEmptyState("В истории пока ничего нет");
                            historyContents.clear();
                            adapter.updateContents(historyContents);
                            return;
                        }

                        loadContentDetails(contentIds);
                    } else {
                        Log.e("History", "Error loading history", task.getException());
                        showEmptyState("Ошибка загрузки истории");
                    }
                });
    }

    private void loadContentDetails(List<String> contentIds) {
        Log.d("History", "Loading content details for IDs: " + contentIds);

        db.collection("contents")
                .whereIn(FieldPath.documentId(), contentIds)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        historyContents.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Log.d("History", "Found content: " + doc.getId());
                            Content content = doc.toObject(Content.class);
                            content.setId(doc.getId());
                            historyContents.add (content);
                        }

                        Log.d("History", "Loaded contents: " + historyContents.size());
                        if (historyContents.isEmpty()) {
                            showEmptyState("В истории пока ничего нет");
                        } else {
                            showContentList();
                        }
                        adapter.updateContents(historyContents);
                    } else {
                        Log.e("History", "Error loading content", task.getException());
                        showEmptyState("Ошибка загрузки данных");
                    }
                });
    }

    private void showDeleteConfirmationDialog(Content content) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Удалить из истории")
                .setMessage("Вы уверены, что хотите удалить этот элемент из истории?")
                .setPositiveButton("Удалить", (dialog1, which) -> removeFromHistory(content))
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

            TextView titleView = dialog.findViewById(android.R.id.title);
            TextView messageView = dialog.findViewById(android.R.id.message);

            if (titleView != null) titleView.setTextColor(Color.WHITE);
            if (messageView != null) messageView.setTextColor(Color.WHITE);

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(getResources().getColor(R.color.burgundy_primary));
            negativeButton.setTextColor(getResources().getColor(R.color.burgundy_primary));
        });

        dialog.show();
    }

    private void removeFromHistory(Content content) {
        if (auth.getCurrentUser () == null) return;

        String userId = auth.getCurrentUser ().getUid();

        db.collection("history")
                .whereEqualTo("userId", userId)
                .whereEqualTo("contentId", content.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            db.collection("history")
                                    .document(doc.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        historyContents.remove(content);
                                        adapter.updateContents(historyContents);
                                        if (historyContents.isEmpty()) {
                                            showEmptyState("История просмотров пуста");
                                        }
                                        Toast.makeText(this, "Удалено из истории", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                });
    }

    private void showClearHistoryDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Очистить историю")
                .setMessage("Вы уверены, что хотите очистить всю историю просмотров?")
                .setPositiveButton("Очистить", (dialog1, which) -> clearAllHistory())
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

            TextView titleView = dialog.findViewById(android.R.id.title);
            TextView messageView = dialog.findViewById(android.R.id.message);

            if (titleView != null) titleView.setTextColor(Color.WHITE);
            if (messageView != null) messageView.setTextColor(Color.WHITE);

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(getResources().getColor(R.color.burgundy_primary));
            negativeButton.setTextColor(getResources().getColor(R.color.burgundy_primary));
        });

        dialog.show();
    }

    private void clearAllHistory() {
        if (auth.getCurrentUser () == null) return;

        String userId = auth.getCurrentUser ().getUid();

        db.collection("history")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            db.collection("history")
                                    .document(doc.getId())
                                    .delete();
                        }

                        historyContents.clear();
                        adapter.updateContents(historyContents);
                        showEmptyState("История просмотров пуста");
                        Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Ошибка при очистке истории", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showReviewDialog(Content content) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Необходимо авторизоваться", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("reviews")
                .whereEqualTo("contentId", content.getId())
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            Toast.makeText(this, "Вы уже оставили отзыв", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        showReviewInputDialog(content);
                    } else {
                        Toast.makeText(this, "Ошибка проверки отзывов", Toast.LENGTH_SHORT).show();
                        Log.e("Review", "Error checking reviews", task.getException());
                    }
                });
    }

    private void showReviewInputDialog(Content content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Оставить отзыв");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null);
        builder.setView(dialogView);

        EditText reviewInput = dialogView.findViewById(R.id.review_input);
        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);

        ratingBar.setStepSize(1.0f);
        ratingBar.setRating(0);

        AlertDialog dialog = builder.create();

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Отправить", (dialog1, which) -> {
            String reviewText = reviewInput.getText().toString().trim();
            float rating = ratingBar.getRating();

            if (!reviewText.isEmpty() && rating > 0) {
                saveReview(content, reviewText, rating);
            } else {
                Toast.makeText(this, "Заполните отзыв и поставьте оценку", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Отмена", (dialog1, which) -> {
            dialog1.dismiss();
        });

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

            TextView titleView = dialog.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setTextColor(Color.WHITE);
            }

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setTextColor(getResources().getColor(R.color.burgundy_primary));
            negativeButton.setTextColor(getResources().getColor(R.color.burgundy_primary));

            reviewInput.setTextColor(Color.WHITE);
            reviewInput.setHintTextColor(Color.GRAY);

            LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
            stars.getDrawable(2).setTint(getResources().getColor(R.color.burgundy_primary));
            stars.getDrawable(1).setTint(getResources().getColor(R.color.burgundy_primary));
            stars.getDrawable(0).setTint(Color.GRAY);
        });

        dialog.show();
    }

    private void saveReview(Content content, String reviewText, float rating) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Необходимо авторизоваться", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        String userName = (user != null && user.getName() != null) ? user.getName() : "Аноним";

                        Review review = new Review();
                        review.setId(db.collection("reviews").document().getId());
                        review.setContentId(content.getId());
                        review.setUserId(currentUser.getUid());
                        review.setUserName(userName);
                        review.setReviewText(reviewText);
                        review.setRating(rating);

                        db.collection("reviews")
                                .document(review.getId())
                                .set(review)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Отзыв сохранен", Toast.LENGTH_SHORT).show();
                                    updateContentRating(content.getId(), rating);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Ошибка сохранения отзыва: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("Review", "Error saving review", e);
                                });
                    } else {
                        Toast.makeText(this, "Данные пользователя не найдены", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка получения данных пользователя", Toast.LENGTH_SHORT).show();
                    Log.e("Review", "Error getting user data", e);
                });
    }

    private void updateContentRating(String contentId, float newRating) {
        db.collection("contents").document(contentId)
                .update(
                        "totalRatings", FieldValue.increment(1),
                        "sumRatings", FieldValue.increment((long) newRating)
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d("Review", "Content ratings updated");

                    for (Content content : historyContents) {
                        if (content.getId().equals(contentId)) {
                            content.setTotalRatings(content.getTotalRatings() + 1);
                            content.setSumRatings(content.getSumRatings() + (long) newRating);
                            adapter.updateContentRating(contentId, content.getRating());
                            break;
                        }
                    }

                    Toast.makeText(this, "Рейтинг обновлен", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Review", "Error updating content ratings", e);
                    Toast.makeText(this, "Ошибка обновления рейтинга", Toast.LENGTH_SHORT).show();
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
    private void updateContentStats(String contentId, String contentTitle, int rating, Context context) {
        if (rating < 1 || rating > 5) {
            Log.e("Stats", "Invalid rating value: " + rating);
            if (context != null) {
                Toast.makeText(context, "Некорректная оценка", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        DatabaseReference contentStatsRef = FirebaseDatabase.getInstance()
                .getReference("stats")
                .child(contentId);

        contentStatsRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Stats stats = mutableData.getValue(Stats.class);
                if (stats == null) {
                    stats = new Stats(contentId, contentTitle);
                }
                stats.addRating(rating);
                mutableData.setValue(stats);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean committed, @Nullable DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e("Stats", "Error updating stats", databaseError.toException());
                    if (context != null) {
                        Toast.makeText(context, "Ошибка обновления статистики: " + databaseError.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else if (!committed) {
                    Log.w("Stats", "Stats update not committed");
                    if (context != null) {
                        Toast.makeText(context, "Не удалось обновить статистику",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("Stats", "Stats updated successfully");

                }
            }
        });
    }
}