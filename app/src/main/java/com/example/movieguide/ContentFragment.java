package com.example.movieguide;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentFragment extends Fragment implements ContentAdapter.OnContentActionListener {

    private RecyclerView recyclerView;
    private ContentAdapter contentAdapter;
    private List<Content> contentList;
    private FirebaseFirestore db;
    private Button buttonAddContent;
    private boolean isAdmin = true;

    private List<String> genreList = new ArrayList<>();
    private List<String> contentTypeList = Arrays.asList("Фильм", "Сериал", "Короткометражка");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_content, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewContent);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        contentList = new ArrayList<>();
        contentAdapter = new ContentAdapter(getContext(), contentList, this, isAdmin);
        recyclerView.setAdapter(contentAdapter);

        db = FirebaseFirestore.getInstance();

        buttonAddContent = view.findViewById(R.id.buttonAddContent);
        buttonAddContent.setOnClickListener(v -> showAddContentDialog());

        loadContentFromFirestore();
        loadGenresFromFirestore();

        return view;
    }

    private void loadContentFromFirestore() {
        db.collection("contents")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        contentList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Content content = document.toObject(Content.class);
                            content.setId(document.getId());
                            contentList.add(content);
                        }
                        contentAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки данных: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadGenresFromFirestore() {
        db.collection("genres")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        genreList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String genre = document.getString("name");
                            genreList.add(genre);
                        }
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки жанров: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onEditContent(Content content) {
        showEditContentDialog(content);
    }

    @Override
    public void onDeleteContent(Content content) {
        showDeleteConfirmationDialog(content);
    }


    private void showAddContentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_content, null);
        builder.setView(dialogView);

        EditText editTextTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextDescription);
        Spinner spinnerGenre = dialogView.findViewById(R.id.spinnerGenre);
        Spinner spinnerContentType = dialogView.findViewById(R.id.spinnerContentType);
        EditText editTextImageUrl = dialogView.findViewById(R.id.editTextImageUrl);
        EditText editTextTrailerUrl = dialogView.findViewById(R.id.editTextTrailerUrl);
        Button buttonSave = dialogView.findViewById(R.id.buttonSave);

        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(
                getContext(),
                R.layout.spinner_item,
                genreList
        );
        genreAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerGenre.setAdapter(genreAdapter);

        ArrayAdapter<String> contentTypeAdapter = new ArrayAdapter<>(
                getContext(),
                R.layout.spinner_item,
                contentTypeList
        );
        contentTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerContentType.setAdapter(contentTypeAdapter);

        AlertDialog dialog = builder.create();
        dialog.show();

        buttonSave.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            String description = editTextDescription.getText().toString().trim();
            String genre = spinnerGenre.getSelectedItem().toString();
            String contentType = spinnerContentType.getSelectedItem().toString();
            String imageUrl = editTextImageUrl.getText().toString().trim();
            String trailerUrl = editTextTrailerUrl.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty() || imageUrl.isEmpty() || trailerUrl.isEmpty()) {
                Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            addContentToFirestore(title, description, genre, contentType, imageUrl, trailerUrl);
            dialog.dismiss();
        });
    }

    private void addContentToFirestore(String title, String description, String genre,
                                       String contentType, String imageUrl, String trailerUrl) {
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("title", title);
        contentMap.put("description", description);
        contentMap.put("genre", genre);
        contentMap.put("imageUrl", imageUrl);
        contentMap.put("trailerUrl", trailerUrl);
        contentMap.put("contentType", contentType);
        contentMap.put("sumRatings", 0L);
        contentMap.put("totalRatings", 0L);

        db.collection("contents")
                .add(contentMap)
                .addOnSuccessListener(documentReference -> {
                    Content newContent = new Content();
                    newContent.setId(documentReference.getId());
                    newContent.setTitle(title);
                    newContent.setDescription(description);
                    newContent.setGenre(genre);
                    newContent.setImageUrl(imageUrl);
                    newContent.setTrailerUrl(trailerUrl);
                    newContent.setContentType(contentType);
                    newContent.setSumRatings(0L);
                    newContent.setTotalRatings(0L);

                    contentList.add(newContent);
                    contentAdapter.notifyItemInserted(contentList.size() - 1);
                    Toast.makeText(getContext(), "Контент добавлен", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка добавления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditContentDialog(Content content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_content, null);
        builder.setView(dialogView);

        EditText editTextTitle = dialogView.findViewById(R.id.editTextTitle);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextDescription);
        Spinner spinnerGenre = dialogView.findViewById(R.id.spinnerGenre);
        Spinner spinnerContentType = dialogView.findViewById(R.id.spinnerContentType);
        EditText editTextImageUrl = dialogView.findViewById(R.id.editTextImageUrl);
        EditText editTextTrailerUrl = dialogView.findViewById(R.id.editTextTrailerUrl);
        Button buttonSave = dialogView.findViewById(R.id.buttonSave);

        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(
                getContext(),
                R.layout.spinner_item,
                genreList
        );
        genreAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerGenre.setAdapter(genreAdapter);

        ArrayAdapter<String> contentTypeAdapter = new ArrayAdapter<>(
                getContext(),
                R.layout.spinner_item,
                contentTypeList
        );
        contentTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerContentType.setAdapter(contentTypeAdapter);

        editTextTitle.setText(content.getTitle());
        editTextDescription.setText(content.getDescription());
        spinnerGenre.setSelection(genreList.indexOf(content.getGenre()));
        spinnerContentType.setSelection(contentTypeList.indexOf(content.getContentType()));
        editTextImageUrl.setText(content.getImageUrl());
        editTextTrailerUrl.setText(content.getTrailerUrl());

        AlertDialog dialog = builder.create();
        dialog.show();

        buttonSave.setOnClickListener(v -> {
            String title = editTextTitle.getText().toString().trim();
            String description = editTextDescription.getText().toString().trim();
            String genre = spinnerGenre.getSelectedItem().toString();
            String contentType = spinnerContentType.getSelectedItem().toString();
            String imageUrl = editTextImageUrl.getText().toString().trim();
            String trailerUrl = editTextTrailerUrl.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty() || imageUrl.isEmpty() || trailerUrl.isEmpty()) {
                Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            updateContentInFirestore(content.getId(), title, description, genre, contentType, imageUrl, trailerUrl);
            dialog.dismiss();
        });
    }

    private void updateContentInFirestore(String id, String title, String description, String genre, String contentType, String imageUrl, String trailerUrl) {
        Map<String, Object> content = new HashMap<>();
        content.put("title", title);
        content.put("description", description);
        content.put("genre", genre);
        content.put("contentType", contentType); // Новое поле
        content.put("imageUrl", imageUrl);
        content.put("trailerUrl", trailerUrl);

        db.collection("contents").document(id)
                .update(content)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Контент обновлен", Toast.LENGTH_SHORT).show();
                    loadContentFromFirestore(); // Обновляем список
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmationDialog(Content content) {
        new AlertDialog.Builder(getContext())
                .setTitle("Подтверждение удаления")
                .setMessage("Вы уверены, что хотите удалить этот контент?")
                .setPositiveButton("Да", (dialog, which) -> deleteContentFromFirestore(content))
                .setNegativeButton("Нет", null)
                .show();
    }

    private void deleteContentFromFirestore(Content content) {
        db.collection("contents").document(content.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Контент удален", Toast.LENGTH_SHORT).show();
                    loadContentFromFirestore();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    @Override
    public void onContentClick(Content content) {
        Intent intent = new Intent(getContext(), ContentDetailActivity.class);
        intent.putExtra("content", content);
        startActivity(intent);
    }
}