package com.example.movieguide;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
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

import java.util.regex.Pattern;

public class RegistrationActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword, editTextName;
    private Button buttonRegister;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-zA-Z]).{8,}$");

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

        if (!validateName(name) || !validateEmail(email) || !validatePassword(password)) {
            return;
        }

        checkIfUserExists(email, password, name);
    }

    private boolean validateName(String name) {
        if (name.isEmpty()) {
            editTextName.setError("Введите имя");
            editTextName.requestFocus();
            return false;
        }

        if (name.length() < 2 || name.length() > 30) {
            editTextName.setError("Имя должно быть от 2 до 30 символов");
            editTextName.requestFocus();
            return false;
        }

        editTextName.setError(null);
        return true;
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            editTextEmail.setError("Введите email");
            editTextEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Введите корректный email");
            editTextEmail.requestFocus();
            return false;
        }

        editTextEmail.setError(null);
        return true;
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            editTextPassword.setError("Введите пароль");
            editTextPassword.requestFocus();
            return false;
        }

        if (password.length() < 8) {
            editTextPassword.setError("Пароль должен содержать минимум 8 символов");
            editTextPassword.requestFocus();
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            editTextPassword.setError("Пароль должен содержать буквы и цифры");
            editTextPassword.requestFocus();
            return false;
        }

        editTextPassword.setError(null);
        return true;
    }

    private void checkIfUserExists(String email, String password, String name) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentReference userRef = task.getResult().getDocuments().get(0).getReference();
                            userRef.get().addOnSuccessListener(documentSnapshot -> {
                                boolean isBlocked = Boolean.TRUE.equals(documentSnapshot.getBoolean("isBlocked"));
                                if (isBlocked) {
                                    showBlockedDialog();
                                } else {
                                    editTextEmail.setError("Пользователь с таким email уже существует");
                                    editTextEmail.requestFocus();
                                }
                            });
                        } else {
                            createUser(email, password, name);
                        }
                    } else {
                        Toast.makeText(this, "Ошибка проверки пользователя", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUser(String email, String password, String name) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            User user = new User(firebaseUser.getUid(), email, name, "user", false);
                            showAppRulesDialog(user);
                        }
                    } else {
                        String errorMessage = "Ошибка регистрации";
                        if (task.getException() != null) {
                            if (task.getException().getMessage().contains("WEAK_PASSWORD")) {
                                errorMessage = "Пароль слишком слабый";
                            } else if (task.getException().getMessage().contains("EMAIL_EXISTS")) {
                                errorMessage = "Email уже используется";
                            }
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAppRulesDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Правила приложения");
        builder.setMessage("Правила использования приложения MovieGuide:\n\n" +
                "1. Общие положения:\n" +
                "1.1. Запрещено размещение любого незаконного контента (экстремизм, порнография, пропаганда насилия и т.д.)\n" +
                "1.2. Администрация оставляет право блокировать аккаунты без предупреждения за нарушение правил\n" +
                "1.3. Не передавайте свои учетные данные третьим лицам - вы несете ответственность за все действия под вашим аккаунтом\n\n" +

                "2. Правила для пользователей:\n" +
                "2.1. Запрещено создание фейковых/мультиаккаунтов для обхода блокировок\n" +
                "2.2. Нельзя выдавать себя за других пользователей или представителей администрации\n" +
                "2.3. Коммерческая деятельность (реклама, продажи) без согласования с администрацией запрещена\n" +
                "2.4. Запрещены любые формы мошенничества, фишинга или попытки взлома\n\n" +

                "3. Правила публикации отзывов:\n" +
                "3.1. Отзывы должны быть объективными и основанными на личном опыте просмотра\n" +
                "3.2. Запрещены: плагиат, спам, флуд, бессмысленные отзывы (например 'норм' или 'отстой' без пояснений)\n" +
                "3.3. Не допускается размещение отзывов с спойлерами без соответствующей пометки\n" +
                "3.4. Запрещена искусственная накрутка/занижение рейтингов фильмов\n\n" +

                "4. Правила общения и лексика:\n" +
                "4.1. Категорически запрещена нецензурная брань, оскорбления, дискриминация по любым признакам\n" +
                "4.2. Не допускаются угрозы, травля (буллинг), шантаж или домогательства\n" +
                "4.3. Запрещен троллинг, провокации и деструктивное поведение\n" +
                "4.4. Конфликты между пользователями должны решаться цивилизованно или через обращение к модераторам\n\n" +

                "5. Конфиденциальность:\n" +
                "5.1. Запрещено публиковать персональные данные других пользователей без их согласия\n" +
                "5.2. Нельзя требовать или распространять приватную переписку\n\n" +

                "Нарушение этих правил может привести к предупреждению, временной или постоянной блокировке аккаунта.\n" +
                "Администрация оставляет за собой право удалять любой контент без объяснения причин.");

        builder.setPositiveButton("Принимаю", (dialog, which) -> {
            saveUserToFirestore(user);
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> {
            deleteUserFromAuth();
            Toast.makeText(this, "Регистрация отменена", Toast.LENGTH_SHORT).show();
        });

        builder.setCancelable(false);
        builder.show();
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
                    deleteUserFromAuth();
                    Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteUserFromAuth() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(this, "Ошибка отмены регистрации", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showBlockedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Аккаунт заблокирован");
        builder.setMessage("Аккаунт, зарегистрированный по данной почте, был заблокирован.");
        builder.setPositiveButton("ОК", (dialog, which) -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        builder.setCancelable(false);
        builder.show();
    }
}