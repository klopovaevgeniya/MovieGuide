package com.example.movieguide;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private Context context;
    private List<User> userList;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private FirebaseFirestore db;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.textViewName.setText(user.getName());
        holder.textViewEmail.setText(user.getEmail());
        holder.textViewRole.setText(user.getRole());

        if (user.isBlocked()) {
            holder.buttonBlock.setText("Разблокировать");
            holder.buttonBlock.setOnClickListener(v -> showUnblockUserDialog(user, position));
        } else {
            holder.buttonBlock.setText("Заблокировать");
            holder.buttonBlock.setOnClickListener(v -> showBlockUserDialog(user, position));
        }
    }

    private void showBlockUserDialog(User user, int position) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Подтверждение блокировки")
                .setMessage("Выслать предупреждение и заблокировать пользователя " + user.getEmail() + "?")
                .setPositiveButton("Подтвердить", (dialogInterface, which) -> {
                    blockUser(user, position);
                    sendWarningEmail(user.getEmail(), user.getName(), true);
                })
                .setNegativeButton("Отмена", null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    private void showUnblockUserDialog(User user, int position) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Подтверждение разблокировки")
                .setMessage("Разблокировать пользователя " + user.getEmail() + "?")
                .setPositiveButton("Подтвердить", (dialogInterface, which) -> {
                    unblockUser(user, position);
                    sendWarningEmail(user.getEmail(), user.getName(), false);
                })
                .setNegativeButton("Отмена", null)
                .create();

        styleDialog(dialog);
        dialog.show();
    }

    private void styleDialog(AlertDialog dialog) {
        dialog.setOnShowListener(dialogInterface -> {
            View background = dialog.getWindow().getDecorView().getRootView();
            background.setBackgroundColor(ContextCompat.getColor(context, R.color.background_dark));

            int titleId = context.getResources().getIdentifier("alertTitle", "id", "android");
            if (titleId > 0) {
                TextView titleView = dialog.findViewById(titleId);
                if (titleView != null) {
                    titleView.setTextColor(ContextCompat.getColor(context, R.color.text_light));
                }
            }

            TextView messageView = dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setTextColor(ContextCompat.getColor(context, R.color.text_light));
            }

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(context, R.color.white));
            positiveButton.setBackgroundColor(ContextCompat.getColor(context, R.color.burgundy_primary));

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(ContextCompat.getColor(context, R.color.white));
            negativeButton.setBackgroundColor(ContextCompat.getColor(context, R.color.burgundy_primary));

            int dividerId = context.getResources().getIdentifier("titleDivider", "id", "android");
            if (dividerId > 0) {
                View divider = dialog.findViewById(dividerId);
                if (divider != null) {
                    divider.setBackgroundColor(ContextCompat.getColor(context, R.color.burgundy_primary));
                }
            }
        });
    }

    private void blockUser(User user, int position) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isBlocked", true);

        db.collection("users").document(user.getUserId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    user.setBlocked(true);
                    notifyItemChanged(position);
                    Toast.makeText(context, "Пользователь заблокирован", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Ошибка блокировки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void unblockUser(User user, int position) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isBlocked", false);

        db.collection("users").document(user.getUserId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    user.setBlocked(false);
                    notifyItemChanged(position);
                    Toast.makeText(context, "Пользователь разблокирован", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Ошибка разблокировки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendWarningEmail(String email, String userName, boolean isBlocked) {
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

                if (isBlocked) {
                    message.setSubject("Ваш аккаунт заблокирован");
                    message.setText("Уважаемый " + userName + ",\n\nВаш аккаунт был заблокирован администратором за нарушение правил.\n" +
                            "По всем вопросам обращайтесь в поддержку.");
                } else {
                    message.setSubject("Ваш аккаунт разблокирован");
                    message.setText("Уважаемый " + userName + ",\n\nВаш аккаунт был разблокирован администратором.\n" +
                            "Теперь вы снова можете пользоваться сервисом.");
                }

                Transport.send(message);

                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Уведомление отправлено на " + email, Toast.LENGTH_SHORT).show());
            } catch (MessagingException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Ошибка отправки email", Toast.LENGTH_SHORT).show();
                    Log.e("EmailError", "Ошибка: " + e.getMessage());
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewEmail, textViewRole;
        Button buttonBlock;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewRole = itemView.findViewById(R.id.textViewRole);
            buttonBlock = itemView.findViewById(R.id.buttonBlock);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        executorService.shutdown();
    }
}