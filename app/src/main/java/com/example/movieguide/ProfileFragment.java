package com.example.movieguide;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView textViewGreeting, textViewHistory, textViewNotifications,
            textViewLogout, textViewRules;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        initViews();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserData();

        setupClickListeners();

        return view;
    }

    private void initViews() {
        textViewGreeting = view.findViewById(R.id.textViewGreeting);
        textViewHistory = view.findViewById(R.id.textViewHistory);
        textViewNotifications = view.findViewById(R.id.textViewNotifications);
        textViewLogout = view.findViewById(R.id.textViewLogout);
        textViewRules = view.findViewById(R.id.textViewRules);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            String greeting = "Здравствуй, ";
                            greeting += document.exists() ?
                                    document.toObject(User.class).getName() : "Пользователь";
                            textViewGreeting.setText(greeting);
                        } else {
                            textViewGreeting.setText("Здравствуй, Пользователь");
                            Toast.makeText(getContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            textViewGreeting.setText("Здравствуй, Гость");
        }
    }

    private void setupClickListeners() {
        textViewHistory.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                startActivity(new Intent(getActivity(), HistoryActivity.class));
            } else {
                showToast("Войдите в аккаунт для просмотра истории");
            }
        });

        textViewNotifications.setOnClickListener(v -> showNotificationConfirmationDialog());

        textViewLogout.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                showLogoutConfirmationDialog();
            } else {
                showToast("Вы не авторизованы");
            }
        });

        view.findViewById(R.id.editProfileItem).setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                startActivity(new Intent(getActivity(), ProfileActivity.class));
            } else {
                showToast("Войдите в аккаунт для редактирования профиля");
            }
        });

        // Добавляем обработчик для Правил приложения
        textViewRules.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AppRulesActivity.class));
        });
    }

    private void showNotificationConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Отключение уведомлений")
                .setMessage("Вы уверены, что хотите отключить уведомления?")
                .setPositiveButton("Да", (dialog, which) ->
                        showToast("Уведомления отключены"))
                .setNegativeButton("Нет", null)
                .show();
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Выход из профиля")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Да", (dialog, which) -> {
                    mAuth.signOut();
                    redirectToLogin();
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(getContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}