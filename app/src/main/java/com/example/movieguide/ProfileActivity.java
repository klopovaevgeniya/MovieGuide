package com.example.movieguide;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail;
    private Button buttonChangePassword, buttonSave;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonSave = findViewById(R.id.buttonSave);

        loadUserData();

        buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        buttonSave.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            editTextEmail.setText(user.getEmail());

            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            editTextName.setText(name);
                        }
                    });
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText editTextOldPassword = dialogView.findViewById(R.id.editTextOldPassword);
        EditText editTextNewPassword = dialogView.findViewById(R.id.editTextNewPassword);
        EditText editTextConfirmPassword = dialogView.findViewById(R.id.editTextConfirmPassword);

        new AlertDialog.Builder(this)
                .setTitle("Сменить пароль")
                .setView(dialogView)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String oldPassword = editTextOldPassword.getText().toString();
                    String newPassword = editTextNewPassword.getText().toString();
                    String confirmPassword = editTextConfirmPassword.getText().toString();

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), oldPassword))
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        user.updatePassword(newPassword)
                                                .addOnCompleteListener(updateTask -> {
                                                    if (updateTask.isSuccessful()) {
                                                        Toast.makeText(this, "Пароль успешно изменен", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(this, "Ошибка изменения пароля", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(this, "Неверный старый пароль", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void saveProfile() {
        String name = editTextName.getText().toString();
        String email = editTextEmail.getText().toString();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updateEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            db.collection("users").document(user.getUid())
                                    .update("name", name, "email", email)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(this, "Профиль успешно обновлен", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(this, "Ошибка обновления данных", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "Ошибка обновления email", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}