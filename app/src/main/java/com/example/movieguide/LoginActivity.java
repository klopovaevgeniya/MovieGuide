package com.example.movieguide;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buttonLogin.setOnClickListener(v -> loginUser());
    }

    public void onRegisterClick(android.view.View view) {
        startActivity(new Intent(this, RegistrationActivity.class));
        finish();
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            checkUserRoleAndBlockStatus(user.getUid());
                        }
                    } else {
                        String errorMessage = "Ошибка авторизации";
                        Exception exception = task.getException();

                        if (exception instanceof FirebaseAuthInvalidUserException) {
                            errorMessage = "Пользователь с таким email не найден";
                        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Неверный логин или пароль";
                        } else if (exception != null && exception.getMessage() != null) {
                            if (exception.getMessage().contains("INVALID_EMAIL")) {
                                errorMessage = "Некорректный email";
                            } else if (exception.getMessage().contains("USER_DISABLED")) {
                                errorMessage = "Аккаунт заблокирован";
                            }
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRoleAndBlockStatus(String userId) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                boolean isBlocked = Boolean.TRUE.equals(documentSnapshot.getBoolean("isBlocked"));
                if (isBlocked) {
                    showBlockedDialog();
                } else {
                    String role = documentSnapshot.getString("role");
                    if ("admin".equals(role)) {
                        startActivity(new Intent(this, AdminActivity.class));
                    } else {
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish();
                }
            } else {
                Toast.makeText(this, "Ошибка: данные пользователя не найдены", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
        );
    }

    private void showBlockedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Аккаунт заблокирован");
        builder.setMessage("Аккаунт, зарегистрированный по данной почте, был заблокирован.");
        builder.setPositiveButton("ОК", (dialog, which) -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        builder.setCancelable(false);
        builder.show();
    }
}