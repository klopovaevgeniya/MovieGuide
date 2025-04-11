package com.example.movieguide;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    private ImageView navContent, navGenre, navReviews, navUsers;
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

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Подтверждение выхода")
                .setMessage("Вы уверены, что хотите выйти из аккаунта?")
                .setPositiveButton("Выйти", (dialog, which) -> logoutUser())
                .setNegativeButton("Отмена", null)
                .setCancelable(true)
                .show();
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