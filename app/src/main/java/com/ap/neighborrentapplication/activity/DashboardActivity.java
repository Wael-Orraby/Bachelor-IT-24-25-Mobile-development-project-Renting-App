package com.ap.neighborrentapplication.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.adapter.DevicesAdapter;
import com.ap.neighborrentapplication.domain.Device;

import java.util.ArrayList;

public class DashboardActivity extends AppCompatActivity {
    private RecyclerView.Adapter adapterList;
    private RecyclerView recyclerView;


    private ImageView homeBtnImage;
    private TextView homeBtnTxt;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initRecyclerView();
        homeBtnImage = findViewById(R.id.homeBtnImage);
        homeBtnTxt = findViewById(R.id.homeBtnTxt);
        homeBtnImage.setColorFilter(Color.parseColor("#FF3700B3"), PorterDuff.Mode.SRC_IN);
        homeBtnTxt.setTextColor(Color.parseColor("#32357A"));
        homeBtnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, DashboardActivity.class));
            }
        });
    }

    private void initRecyclerView() {
        ArrayList<Device> items = new ArrayList<>();

        items.add(new Device("1", "Boormachine", "Krachtige boormachine voor doe-het-zelf projecten", "https://th.bing.com/th/id/OIP.0v0WSI9bszvLAmV1lus4-wHaDt?rs=1&pid=ImgDetMain", "10.00", "Beschikbaar", "Merksem",true));
        items.add(new Device("2", "Wasmachine", "Energiezuinige wasmachine", "https://dataconomy.com/wp-content/uploads/2022/10/NightCafe-AI-image-generator-7.jpg", "15.00", "Beschikbaar", "Merksem",false));
        items.add(new Device("3", "Stofzuiger", "Krachtige stofzuiger met HEPA-filter", "https://imgv3.fotor.com/images/gallery/cartoon-character-generated-by-Fotor-ai-art-creator.jpg", "8.00", "Beschikbaar", "Merksem",true));
        items.add(new Device("4", "Boormachine", "Krachtige boormachine voor doe-het-zelf projecten", "https://th.bing.com/th/id/OIP.0v0WSI9bszvLAmV1lus4-wHaDt?rs=1&pid=ImgDetMain", "10.00", "Beschikbaar", "Merksem",false));
        items.add(new Device("5", "Wasmachine", "Energiezuinige wasmachine", "https://dataconomy.com/wp-content/uploads/2022/10/NightCafe-AI-image-generator-7.jpg", "15.00", "Beschikbaar", "Merksem",true));
        items.add(new Device("6", "Stofzuiger", "Krachtige stofzuiger met HEPA-filter", "https://imgv3.fotor.com/images/gallery/cartoon-character-generated-by-Fotor-ai-art-creator.jpg", "8.00", "Beschikbaar", "Merksem",false));

        recyclerView = findViewById(R.id.recylerview);

        // Stel GridLayoutManager in met 2 kolommen
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        // Adapter instellen
        adapterList = new DevicesAdapter(items);
        recyclerView.setAdapter(adapterList);
    }

}


