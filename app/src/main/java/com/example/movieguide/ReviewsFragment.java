package com.example.movieguide;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class ReviewsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reviews, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewList);
        recyclerView.setAdapter(reviewAdapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadReviews();

        reviewAdapter.setOnItemClickListener((position, review) -> {
            checkAdminAndShowDialog(review, position);
        });

        return view;
    }

    private void checkAdminAndShowDialog(Review review, int position) {
        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User currentUser = documentSnapshot.toObject(User.class);
                    if (currentUser != null && "admin".equals(currentUser.getRole())) {
                        showWarningDialog(review, position);
                    } else {
                        Toast.makeText(getContext(), "Только администратор может отправлять предупреждения", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadReviews() {
        db.collection("reviews")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reviewList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Review review = document.toObject(Review.class);
                            review.setId(document.getId());
                            reviewList.add(review);
                        }
                        reviewAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки отзывов", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showWarningDialog(Review review, int position) {
        db.collection("users").document(review.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                .setTitle("Выслать предупреждение")
                                .setMessage("Вы уверены, что хотите выслать предупреждение на почту " + user.getEmail() + "?")
                                .setPositiveButton("Да", (dialogInterface, which) -> {
                                    sendWarningEmail(user.getName(), user.getEmail());
                                    deleteReview(review, position);
                                })
                                .setNegativeButton("Нет", null)
                                .create();

                        dialog.setOnShowListener(dialogInterface -> {
                            View background = dialog.getWindow().getDecorView().getRootView();
                            background.setBackgroundColor(getResources().getColor(R.color.background_dark));

                            int titleId = getResources().getIdentifier("alertTitle", "id", "android");
                            if (titleId > 0) {
                                TextView titleView = dialog.findViewById(titleId);
                                if (titleView != null) {
                                    titleView.setTextColor(getResources().getColor(R.color.text_light));
                                }
                            }

                            TextView messageView = dialog.findViewById(android.R.id.message);
                            if (messageView != null) {
                                messageView.setTextColor(getResources().getColor(R.color.text_light));
                            }

                            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            positiveButton.setTextColor(getResources().getColor(R.color.white));
                            positiveButton.setBackgroundColor(getResources().getColor(R.color.burgundy_primary));

                            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                            negativeButton.setTextColor(getResources().getColor(R.color.white));
                            negativeButton.setBackgroundColor(getResources().getColor(R.color.burgundy_primary));

                            int dividerId = getResources().getIdentifier("titleDivider", "id", "android");
                            if (dividerId > 0) {
                                View divider = dialog.findViewById(dividerId);
                                if (divider != null) {
                                    divider.setBackgroundColor(getResources().getColor(R.color.burgundy_primary));
                                }
                            }
                        });

                        dialog.show();
                    } else {
                        Toast.makeText(getContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при получении данных пользователя", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteReview(Review review, int position) {
        db.collection("reviews").document(review.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    reviewList.remove(position);
                    reviewAdapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Отзыв удален", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка удаления отзыва: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendWarningEmail(String userName, String email) {
        executorService.execute(() -> {
            try {
                String host = "smtp.gmail.com";
                String port = "587";
                String username = "klopovaevgeniya25@gmail.com";
                String password = "hxwb ijtm zvnr oypa";

                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", host);
                props.put("mail.smtp.port", port);

                Session session = Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(username, password);
                            }
                        });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject("Предупреждение");
                message.setText("Уважаемый " + userName + ",\n\nВаш отзыв был удален администратором за нарушение норм. " +
                        "В случае повторного нарушения норм, Ваш аккаунт будет заблокирован.");

                Transport.send(message);

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Письмо отправлено на " + email, Toast.LENGTH_SHORT).show());
            } catch (MessagingException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Ошибка отправки email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}