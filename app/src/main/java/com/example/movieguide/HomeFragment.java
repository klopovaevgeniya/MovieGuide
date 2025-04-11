package com.example.movieguide;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment implements ContentAdapter.OnContentActionListener {

    private RecyclerView recyclerView;
    private ContentAdapter contentAdapter;
    private FirebaseFirestore db;
    private List<Content> contentList;
    private List<Content> filteredContentList;
    private EditText searchEditText;
    private TextView textViewNoResults;
    private boolean isAdmin = false;

    // Элементы фильтрации
    private ImageButton filterButton;
    private View overlayView;
    private LinearLayout filtersMenu;
    private Button applyFiltersButton;
    private Button resetFiltersButton;
    private SeekBar ratingSeekBar;
    private TextView ratingValueText;
    private CheckBox checkBoxAllGenres;
    private CheckBox checkBoxAllTypes;
    private LinearLayout genresContainer;
    private LinearLayout typesContainer;

    private List<String> genreList = new ArrayList<>();
    private List<String> contentTypeList = Arrays.asList("Фильм", "Сериал", "Короткометражка");
    private List<CheckBox> genreCheckBoxes = new ArrayList<>();
    private List<CheckBox> typeCheckBoxes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Инициализация Firebase
        db = FirebaseFirestore.getInstance();

        // Инициализация UI элементов
        initViews(view);

        // Настройка слушателей
        setupListeners();

        // Загрузка данных
        loadContent();
        loadGenres();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        contentList = new ArrayList<>();
        filteredContentList = new ArrayList<>();
        contentAdapter = new ContentAdapter(getContext(), filteredContentList, this, isAdmin);
        recyclerView.setAdapter(contentAdapter);

        searchEditText = view.findViewById(R.id.searchEditText);
        textViewNoResults = view.findViewById(R.id.textViewNoResults);
        filterButton = view.findViewById(R.id.filterButton);
        overlayView = view.findViewById(R.id.overlayView);
        filtersMenu = view.findViewById(R.id.filtersMenu);
        applyFiltersButton = view.findViewById(R.id.applyFiltersButton);
        resetFiltersButton = view.findViewById(R.id.resetFiltersButton);
        ratingSeekBar = view.findViewById(R.id.ratingSeekBar);
        ratingValueText = view.findViewById(R.id.ratingValueText);
        checkBoxAllGenres = view.findViewById(R.id.checkBoxAllGenres);
        checkBoxAllTypes = view.findViewById(R.id.checkBoxAllTypes);
        genresContainer = view.findViewById(R.id.genresContainer);
        typesContainer = view.findViewById(R.id.typesContainer);
    }

    private void setupListeners() {
        // Поиск
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContent(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Кнопка фильтра
        filterButton.setOnClickListener(v -> toggleFiltersMenu());

        // Overlay
        overlayView.setOnClickListener(v -> toggleFiltersMenu());

        // Кнопки фильтров
        applyFiltersButton.setOnClickListener(v -> {
            applyFilters();
            toggleFiltersMenu();
        });

        resetFiltersButton.setOnClickListener(v -> {
            resetFilters();
            toggleFiltersMenu();
        });

        // SeekBar // SeekBar рейтинга
        ratingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser ) {
                ratingValueText.setText("Рейтинг: " + (progress > 0 ? progress + "+" : "любой"));
                checkIfFiltersChanged();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Чекбоксы "Все жанры/типы"
        checkBoxAllGenres.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CheckBox checkBox : genreCheckBoxes) {
                checkBox.setChecked(isChecked);
            }
            checkIfFiltersChanged();
        });

        checkBoxAllTypes.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (CheckBox checkBox : typeCheckBoxes) {
                checkBox.setChecked(isChecked);
            }
            checkIfFiltersChanged();
        });
    }

    private void toggleFiltersMenu() {
        if (filtersMenu.getVisibility() == View.VISIBLE) {
            filtersMenu.setVisibility(View.GONE);
            overlayView.setVisibility(View.GONE);
        } else {
            filtersMenu.setVisibility(View.VISIBLE);
            overlayView.setVisibility(View.VISIBLE);
        }
    }

    private void setupGenreCheckboxes() {
        genresContainer.removeAllViews();
        genreCheckBoxes.clear();

        for (String genre : genreList) {
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(genre);
            checkBox.setTextColor(getResources().getColor(android.R.color.white));
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                checkBoxAllGenres.setChecked(false);
                checkIfFiltersChanged();
            });
            genresContainer.addView(checkBox);
            genreCheckBoxes.add(checkBox);
        }
    }

    private void setupTypeCheckboxes() {
        typesContainer.removeAllViews();
        typeCheckBoxes.clear();

        for (String type : contentTypeList) {
            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(type);
            checkBox.setTextColor(getResources().getColor(android.R.color.white));
            checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                checkBoxAllTypes.setChecked(false);
                checkIfFiltersChanged();
            });
            typesContainer.addView(checkBox);
            typeCheckBoxes.add(checkBox);
        }
    }

    private void checkIfFiltersChanged() {
        boolean isRatingSelected = ratingSeekBar.getProgress() > 0;
        boolean isGenreSelected = false;
        boolean isTypeSelected = false;

        for (CheckBox checkBox : genreCheckBoxes) {
            if (checkBox.isChecked()) {
                isGenreSelected = true;
                break;
            }
        }

        for (CheckBox checkBox : typeCheckBoxes) {
            if (checkBox.isChecked()) {
                isTypeSelected = true;
                break;
            }
        }

        applyFiltersButton.setEnabled(isRatingSelected || isGenreSelected || isTypeSelected);
    }

    private void applyFilters() {
        List<Content> filteredList = new ArrayList<>();

        int minRating = ratingSeekBar.getProgress();
        List<String> selectedGenres = new ArrayList<>();
        List<String> selectedTypes = new ArrayList<>();

        for (CheckBox checkBox : genreCheckBoxes) {
            if (checkBox.isChecked()) {
                selectedGenres.add(checkBox.getText().toString());
            }
        }

        for (CheckBox checkBox : typeCheckBoxes) {
            if (checkBox.isChecked()) {
                selectedTypes.add(checkBox.getText().toString());
            }
        }

        for (Content content : contentList) {
            boolean matchesRating = minRating == 0 || content.getRating() >= minRating;
            boolean matchesGenre = selectedGenres.isEmpty() || selectedGenres.contains(content.getGenre());
            boolean matchesType = selectedTypes.isEmpty() || selectedTypes.contains(content.getContentType());

            if (matchesRating && matchesGenre && matchesType) {
                filteredList.add(content);
            }
        }

        filteredContentList.clear();
        filteredContentList.addAll(filteredList);
        contentAdapter.notifyDataSetChanged();
        resetFiltersButton.setEnabled(true);
        checkIfEmpty();
    }

    private void resetFilters() {
        filteredContentList.clear();
        filteredContentList.addAll(contentList);
        contentAdapter.notifyDataSetChanged();

        ratingSeekBar.setProgress(0);
        checkBoxAllGenres.setChecked(true);
        checkBoxAllTypes.setChecked(true);

        resetFiltersButton.setEnabled(false);
        applyFiltersButton.setEnabled(false);
        checkIfEmpty();
    }

    private void loadContent() {
        db.collection("contents")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    contentList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Content content = document.toObject(Content.class);
                        if (content != null) {
                            content.setId(document.getId());
                            contentList.add(content);
                        }
                    }
                    filteredContentList.clear();
                    filteredContentList.addAll(contentList);
                    contentAdapter.notifyDataSetChanged();
                    checkIfEmpty();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadGenres() {
        db.collection("genres")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    genreList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String genre = document.getString("name");
                        if (genre != null) {
                            genreList.add(genre);
                        }
                    }
                    setupGenreCheckboxes();
                    setupTypeCheckboxes();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки жанров: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterContent(String query) {
        filteredContentList.clear();
        if (query.isEmpty()) {
            filteredContentList.addAll(contentList);
        } else {
            for (Content content : contentList) {
                if (content.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredContentList.add(content);
                }
            }
        }
        contentAdapter.notifyDataSetChanged();
        checkIfEmpty();
    }

    private void checkIfEmpty() {
        textViewNoResults.setVisibility(filteredContentList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEditContent(Content content) {
    }

    @Override
    public void onDeleteContent(Content content) {
    }

    @Override
    public void onContentClick(Content content) {
        Intent intent = new Intent(getContext(), ContentDetailActivity.class);
        intent.putExtra("content", content);
        startActivity(intent);
    }
}