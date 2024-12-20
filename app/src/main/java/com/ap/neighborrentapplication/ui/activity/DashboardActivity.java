package com.ap.neighborrentapplication.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.activity.CategorySearchActivity;
import com.ap.neighborrentapplication.adapter.DevicesAdapter;
import com.ap.neighborrentapplication.models.Device;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class DashboardActivity extends AppCompatActivity {
    private DevicesAdapter adapterList;
    private RecyclerView recyclerView;
    private ArrayList<Device> deviceList = new ArrayList<>();

    private FirebaseFirestore firestore;

    private FloatingActionButton fabAddDevice;
    private ImageView homeBtnImage;
    private TextView homeBtnTxt;
    private  ImageView searchBtn;
    private  ImageView profileBtn;
    private ImageView reservationsBtn;

    private View favoritesSection;

    private boolean isFavoritesShowing = false;
    private ImageView favoritesSectionIcon;

    private TextView userGreetingText;
    private FirebaseAuth auth;

    private boolean isSingleDeviceView = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        userGreetingText = findViewById(R.id.textView5);
        loadUserData();

        homeBtnImage = findViewById(R.id.homeBtnImage);
        homeBtnTxt = findViewById(R.id.homeBtnTxt);
        homeBtnImage.setColorFilter(Color.parseColor("#FF3700B3"), PorterDuff.Mode.SRC_IN);
        homeBtnTxt.setTextColor(Color.parseColor("#32357A"));

        fabAddDevice = findViewById(R.id.fab_add_device);
        fabAddDevice.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, AddDeviceActivity.class)));


        EditText searchEditText = findViewById(R.id.editTextText);
        searchEditText.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, MapSearchActivity.class);
            startActivity(intent);
        });

        profileBtn = findViewById(R.id.searchBtn);
        profileBtn.setOnClickListener(v ->  startActivity(new Intent(DashboardActivity.this, CategorySearchActivity.class)));

        searchBtn = findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(v ->  startActivity(new Intent(DashboardActivity.this, CategorySearchActivity.class)));
        searchBtn = findViewById(R.id.searchBtn);

        profileBtn = findViewById(R.id.profileBtn);
        profileBtn.setOnClickListener(v ->  startActivity(new Intent(DashboardActivity.this, ProfileActivity.class)));


        reservationsBtn = findViewById(R.id.reservationsBtn);
        reservationsBtn.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, ReservationHistoryActivity.class)));

        favoritesSection = findViewById(R.id.favoritesSection);
        favoritesSectionIcon = findViewById(R.id.imageView8);

        favoritesSection.setOnClickListener(v -> {
            if (isFavoritesShowing) {
                favoritesSectionIcon.setImageResource(R.drawable.favorites);
                loadDevicesFromFirestore();
                isFavoritesShowing = false;
            } else {
                favoritesSectionIcon.setImageResource(R.drawable.ic_redheart);
                showFavorites();
                isFavoritesShowing = true;
            }
        });

        initRecyclerView();

        // Controleer of een apparaat-ID is meegegeven
        String deviceId = getIntent().getStringExtra("deviceId");
        if (deviceId != null) {
            loadSingleDeviceFromFirestore(deviceId); // Laad één apparaat
        } else {
            loadDevicesFromFirestore(); // Laad alle apparaten
        }
        // Stel de home-knop in
        homeBtnImage.setOnClickListener(v -> {
            if (isSingleDeviceView) {
                loadDevicesFromFirestore(); // Toon de volledige lijst
                isSingleDeviceView = false;
                Toast.makeText(this, "Alle apparaten weergegeven", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Je bent al op de hoofdweergave", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadSingleDeviceFromFirestore(String deviceId) {
        firestore.collection("devices")
                .document(deviceId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Device device = document.toObject(Device.class);
                        deviceList.clear();
                        deviceList.add(device);
                        adapterList.notifyDataSetChanged();
                        isSingleDeviceView = true; // Zet de toestand op enkel apparaatweergave
                    } else {
                        Toast.makeText(this, "Apparaat niet gevonden", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Fout bij ophalen apparaat", Toast.LENGTH_SHORT).show());
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recylerview);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 1);
        recyclerView.setLayoutManager(gridLayoutManager);

        adapterList = new DevicesAdapter(deviceList, this);  // Context toevoegen aan adapter
        recyclerView.setAdapter(adapterList);
    }

    private void loadDevicesFromFirestore() {
        CollectionReference devicesRef = firestore.collection("devices");

        devicesRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    return;
                }

                if (value != null) {
                    deviceList.clear();

                    for (QueryDocumentSnapshot document : value) {
                        Device device = document.toObject(Device.class);
                        deviceList.add(device);
                    }

                    adapterList.notifyDataSetChanged();
                }
            }
        });
    }


    private void showFavorites() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("favorites")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(documents -> {
                    deviceList.clear();

                    if (documents.isEmpty()) {
                        Toast.makeText(this, "Je hebt nog geen favorieten", Toast.LENGTH_SHORT).show();
                        loadDevicesFromFirestore();
                        favoritesSectionIcon.setImageResource(R.drawable.favorites);
                        isFavoritesShowing = false;
                        return;
                    }

                    for (DocumentSnapshot document : documents) {
                        String deviceId = document.getString("deviceId");

                        if (deviceId != null) {
                            // Gebruik document() in plaats van whereEqualTo()
                            firestore.collection("devices")
                                    .document(deviceId)
                                    .get()
                                    .addOnSuccessListener(deviceDoc -> {
                                        if (deviceDoc.exists()) {
                                            Device device = deviceDoc.toObject(Device.class);
                                            if (device != null) {
                                                // Zet het document ID in het device object
                                                device.setId(deviceDoc.getId());
                                                deviceList.add(device);
                                                adapterList.notifyDataSetChanged();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Fout bij ophalen apparaat: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Fout bij ophalen favorieten: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserData() {
        String userId = auth.getCurrentUser().getUid();
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        userGreetingText.setText("Hallo, " + firstName + " 😀");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Fout bij ophalen gebruikersgegevens", Toast.LENGTH_SHORT).show();
                });
    }
}