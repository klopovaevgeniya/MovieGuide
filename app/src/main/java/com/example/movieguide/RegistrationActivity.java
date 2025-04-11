package com.example.movieguide;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegistrationActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword, editTextName;
    private Button buttonRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextName = findViewById(R.id.editTextName);
        buttonRegister = findViewById(R.id.buttonRegister);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buttonRegister.setOnClickListener(v -> registerUser());
    }

    public void onLoginClick(View view) {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String name = editTextName.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        checkIfUserExists(email, password, name);
    }

    private void checkIfUserExists(String email, String password, String name) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentReference userRef = task.getResult().getDocuments().get(0).getReference();
                        userRef.get().addOnSuccessListener(documentSnapshot -> {
                            boolean isBlocked = Boolean.TRUE.equals(documentSnapshot.getBoolean("isBlocked"));
                            if (isBlocked) {
                                showBlockedDialog();
                            } else {
                                Toast.makeText(this, "Пользователь с таким email уже существует", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        createUser(email, password, name);
                    }
                });
    }

    private void createUser(String email, String password, String name) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            User user = new User(firebaseUser.getUid(), email, name, "user", false); // По умолчанию isBlocked = false
                            saveUserToFirestore(user);
                        }
                    } else {
                        Toast.makeText(this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(User user) {
        db.collection("users")
                .document(user.getUserId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка сохранения пользователя: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showBlockedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Аккаунт заблокирован");
        builder.setMessage("Аккаунт, зарегестрированный по данной почте, был заблокирован.");
        builder.setPositiveButton("ОК", (dialog, which) -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        builder.setCancelable(false);
        builder.show();
    }
}