package com.ap.neighborrentapplication.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
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

    private View favoritesSection;

    private boolean isFavoritesShowing = false;
    private ImageView favoritesSectionIcon;

    private TextView userGreetingText;
    private FirebaseAuth auth;

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

        searchBtn = findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(v ->  startActivity(new Intent(DashboardActivity.this, CategorySearchActivity.class)));
        searchBtn = findViewById(R.id.searchBtn);


        profileBtn = findViewById(R.id.profileBtn);
        profileBtn.setOnClickListener(v ->  startActivity(new Intent(DashboardActivity.this, ProfileActivity.class)));




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
        loadDevicesFromFirestore();
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
                            firestore.collection("devices")
                                    .whereEqualTo("id", deviceId)
                                    .get()
                                    .addOnSuccessListener(deviceDocs -> {
                                        if (!deviceDocs.isEmpty()) {
                                            Device device = deviceDocs.getDocuments().get(0).toObject(Device.class);
                                            if (device != null) {
                                                deviceList.add(device);
                                                adapterList.notifyDataSetChanged();
                                            }
                                        }
                                    });
                        }
                    }
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
