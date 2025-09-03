package com.example.movieguide;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail;
    private Button buttonChangePassword, buttonSave;
    private ImageButton backButton;
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
        backButton = findViewById(R.id.backButton);

        loadUserData();

        buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        buttonSave.setOnClickListener(v -> saveProfile());
        backButton.setOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onBackPressed() {
        if (hasChanges()) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasChanges() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String currentEmail = user.getEmail();
            String newEmail = editTextEmail.getText().toString().trim();

            if (!newEmail.equals(currentEmail)) {
                return true;
            }

            String newName = editTextName.getText().toString().trim();
            String currentName = editTextName.getTag() != null ? editTextName.getTag().toString() : "";

            if (!newName.equals(currentName)) {
                return true;
            }
        }
        return false;
    }

    private void showUnsavedChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        AlertDialog dialog = builder.setTitle("Несохраненные изменения")
                .setMessage("У вас есть несохраненные изменения. Вы уверены, что хотите выйти?")
                .setPositiveButton("Выйти", (dialog1, which) -> finish())
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            positiveButton.setTextColor(Color.WHITE);
            positiveButton.setBackgroundColor(Color.parseColor("#800020"));

            negativeButton.setTextColor(Color.WHITE);
            negativeButton.setBackgroundColor(Color.parseColor("#800020"));

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        });

        dialog.show();
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
                            editTextName.setTag(name);
                        }
                    });
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText editTextOldPassword = dialogView.findViewById(R.id.editTextOldPassword);
        EditText editTextNewPassword = dialogView.findViewById(R.id.editTextNewPassword);
        EditText editTextConfirmPassword = dialogView.findViewById(R.id.editTextConfirmPassword);
        Button buttonSavePassword = dialogView.findViewById(R.id.buttonSavePassword);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setTitle("Сменить пароль")
                .setView(dialogView)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                negativeButton.setTextColor(Color.WHITE);
                negativeButton.setBackgroundColor(Color.parseColor("#800020"));
            }

            buttonSavePassword.setTextColor(Color.WHITE);
            buttonSavePassword.setBackgroundColor(Color.parseColor("#800020"));

            editTextOldPassword.setTextColor(Color.WHITE);
            editTextOldPassword.setHintTextColor(Color.GRAY);
            editTextOldPassword.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));

            editTextNewPassword.setTextColor(Color.WHITE);
            editTextNewPassword.setHintTextColor(Color.GRAY);
            editTextNewPassword.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));

            editTextConfirmPassword.setTextColor(Color.WHITE);
            editTextConfirmPassword.setHintTextColor(Color.GRAY);
            editTextConfirmPassword.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        });

        buttonSavePassword.setOnClickListener(v -> {
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
                                                dialog.dismiss();
                                            } else {
                                                Toast.makeText(this, "Ошибка изменения пароля", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(this, "Неверный старый пароль", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        dialog.show();
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