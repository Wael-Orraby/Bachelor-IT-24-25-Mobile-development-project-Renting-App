package com.ap.neighborrentapplication.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.activity.CategorySearchActivity;
import com.ap.neighborrentapplication.adapter.DevicesAdapter;
import com.ap.neighborrentapplication.models.Device;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);

        firestore = FirebaseFirestore.getInstance();

        homeBtnImage = findViewById(R.id.homeBtnImage);
        homeBtnTxt = findViewById(R.id.homeBtnTxt);
        homeBtnImage.setColorFilter(Color.parseColor("#FF3700B3"), PorterDuff.Mode.SRC_IN);
        homeBtnTxt.setTextColor(Color.parseColor("#32357A"));

        fabAddDevice = findViewById(R.id.fab_add_device);
        fabAddDevice.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, AddDeviceActivity.class)));

        searchBtn = findViewById(R.id.searchBtn);
        searchBtn.setOnClickListener(v ->  startActivity(new Intent(DashboardActivity.this, CategorySearchActivity.class)));

        initRecyclerView();
        loadDevicesFromFirestore();
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recylerview);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
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
                    return;  // Log eventueel een foutmelding
                }

                if (value != null) {
                    deviceList.clear();  // Leeg de lijst zodat we nieuwe data kunnen toevoegen

                    for (QueryDocumentSnapshot document : value) {
                        Device device = document.toObject(Device.class);
                        deviceList.add(device);
                    }

                    adapterList.notifyDataSetChanged();  // Adapter updaten met nieuwe data
                }
            }
        });
    }
}
