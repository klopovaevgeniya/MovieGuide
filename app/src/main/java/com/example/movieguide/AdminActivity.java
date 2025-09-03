package com.example.movieguide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    private ImageView navContent, navGenre, navReviews, navUsers, navAnalitik;
    private ImageButton btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        navAnalitik = findViewById(R.id.nav_analitik);
        navContent = findViewById(R.id.nav_content);
        navGenre = findViewById(R.id.nav_genre);
        navReviews = findViewById(R.id.nav_reviews);
        navUsers = findViewById(R.id.nav_users);
        btnLogout = findViewById(R.id.btn_logout);

        loadFragment(new ContentFragment());

        navContent.setOnClickListener(v -> loadFragment(new ContentFragment()));
        navGenre.setOnClickListener(v -> loadFragment(new GenresFragment()));
        navReviews.setOnClickListener(v -> loadFragment(new ReviewsFragment()));
        navUsers.setOnClickListener(v -> loadFragment(new UsersFragment()));
        navAnalitik.setOnClickListener(v -> loadFragment(new AnalitikFragment()));

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Подтверждение выхода")
                .setMessage("Вы уверены, что хотите выйти из аккаунта?")
                .setPositiveButton("Выйти", (dialogInterface, which) -> logoutUser())
                .setNegativeButton("Отмена", null)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            View background = dialog.getWindow().getDecorView().getRootView();
            background.setBackgroundColor(ContextCompat.getColor(this, R.color.background_dark));

            int titleId = getResources().getIdentifier("alertTitle", "id", "android");
            if (titleId > 0) {
                TextView titleView = dialog.findViewById(titleId);
                if (titleView != null) {
                    titleView.setTextColor(ContextCompat.getColor(this, R.color.text_light));
                }
            }

            TextView messageView = dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setTextColor(ContextCompat.getColor(this, R.color.text_light));
            }

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            positiveButton.setBackgroundColor(ContextCompat.getColor(this, R.color.burgundy_primary));

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            negativeButton.setBackgroundColor(ContextCompat.getColor(this, R.color.burgundy_primary));

            int dividerId = getResources().getIdentifier("titleDivider", "id", "android");
            if (dividerId > 0) {
                View divider = dialog.findViewById(dividerId);
                if (divider != null) {
                    divider.setBackgroundColor(ContextCompat.getColor(this, R.color.burgundy_primary));
                }
            }
        });

        dialog.show();
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}