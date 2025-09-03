package com.example.movieguide;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView emptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        userList = new ArrayList<>();
        userAdapter = new UserAdapter(requireContext(), userList);
        recyclerView.setAdapter(userAdapter);

        db = FirebaseFirestore.getInstance();
        loadUsers();

        return view;
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        db.collection("users")
                .whereEqualTo("role", "user")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            user.setUserId(document.getId());
                            userList.add(user);
                        }

                        userAdapter.notifyDataSetChanged();

                        if (userList.isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("Пользователи не найдены");
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("Ошибка загрузки");
                        Toast.makeText(getContext(), "Ошибка загрузки пользователей", Toast.LENGTH_SHORT).show();
                        Log.e("UsersFragment", "Error loading users", task.getException());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userAdapter != null) {
            userAdapter = null;
        }
    }
}