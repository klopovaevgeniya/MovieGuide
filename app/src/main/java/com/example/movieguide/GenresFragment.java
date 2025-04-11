package com.example.movieguide;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GenresFragment extends Fragment {

    private FirebaseFirestore firestore;
    private GenreAdapter adapter;
    private List<Genre> genres = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_genres, container, false);

        firestore = FirebaseFirestore.getInstance();

        RecyclerView recyclerView = view.findViewById(R.id.genresRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GenreAdapter(genres, new GenreAdapter.OnGenreClickListener() {
            @Override
            public void onEditClick(Genre genre) {
                showEditGenreDialog(genre);
            }

            @Override
            public void onDeleteClick(Genre genre) {
                showDeleteConfirmationDialog(genre);
            }
        });
        recyclerView.setAdapter(adapter);

        Button addButton = view.findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> showAddGenreDialog());
        loadGenres();
        return view;
    }

    private void loadGenres() {
        firestore.collection("genres")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        genres.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Genre genre = document.toObject(Genre.class);
                            genre.setId(document.getId());
                            genres.add(genre);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки жанров: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddGenreDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_genre, null);
        EditText genreNameEditText = dialogView.findViewById(R.id.genreNameEditText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Добавить жанр")
                .setView(dialogView)
                .setNegativeButton("Отмена", null)
                .create();

        saveButton.setOnClickListener(v -> {
            String name = genreNameEditText.getText().toString();
            if (!name.isEmpty()) {
                addGenre(name);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void addGenre(String name) {
        Genre genre = new Genre(null, name);
        firestore.collection("genres")
                .add(genre)
                .addOnSuccessListener(documentReference -> {
                    genre.setId(documentReference.getId());
                    genres.add(genre);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка добавления жанра: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditGenreDialog(Genre genre) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_genre, null);
        EditText genreNameEditText = dialogView.findViewById(R.id.genreNameEditText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        genreNameEditText.setText(genre.getName());

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Редактировать жанр")
                .setView(dialogView)
                .setNegativeButton("Отмена", null)
                .create();

        saveButton.setOnClickListener(v -> {
            String newName = genreNameEditText.getText().toString();
            if (!newName.isEmpty()) {
                genre.setName(newName);
                updateGenre(genre);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updateGenre(Genre genre) {
        firestore.collection("genres")
                .document(genre.getId())
                .set(genre)
                .addOnSuccessListener(aVoid -> {
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка обновления жанра: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmationDialog(Genre genre) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удалить жанр")
                .setMessage("Вы уверены, что хотите удалить жанр " + genre.getName() + "?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteGenre(genre))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteGenre(Genre genre) {
        firestore.collection("genres")
                .document(genre.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    genres.remove(genre);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка удаления жанра: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}