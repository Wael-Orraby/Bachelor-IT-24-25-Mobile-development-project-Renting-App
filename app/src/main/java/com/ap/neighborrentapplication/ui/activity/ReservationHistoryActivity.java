package com.ap.neighborrentapplication.ui.activity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.adapter.GroupedReservationAdapter;
import com.ap.neighborrentapplication.models.Reservation;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReservationHistoryActivity extends AppCompatActivity {
    private static final String TAG = "ReservationHistory";
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private GroupedReservationAdapter rentAdapter;
    private GroupedReservationAdapter leaseAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Handler autoUpdateHandler;
    private static final long AUTO_UPDATE_INTERVAL = 60000; // Check every minute

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_history);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupViews();
        setupViewPager();
        setupAutoUpdate();

        // Check network connectivity before loading
        if (isNetworkAvailable()) {
            loadReservations();
        } else {
            Toast.makeText(this, "Geen internetverbinding. Controleer je netwerkinstellingen.", Toast.LENGTH_LONG).show();
        }
    }

    private void setupViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupViewPager() {
        rentAdapter = new GroupedReservationAdapter(false, this);
        leaseAdapter = new GroupedReservationAdapter(true, this);

        viewPager.setAdapter(new ReservationPagerAdapter());

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Mijn Huren");
                    break;
                case 1:
                    tab.setText("Mijn Verhuren");
                    break;
            }
        }).attach();
    }

    private void setupAutoUpdate() {
        autoUpdateHandler = new Handler(Looper.getMainLooper());
        startAutoUpdate();
    }

    private void startAutoUpdate() {
        autoUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Check for completed reservations
                Reservation.checkAndUpdateAllReservations();
                // Schedule the next update
                autoUpdateHandler.postDelayed(this, AUTO_UPDATE_INTERVAL);
            }
        }, AUTO_UPDATE_INTERVAL);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for completed reservations immediately when activity resumes
        Reservation.checkAndUpdateAllReservations();
        startAutoUpdate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop auto-update when activity is paused
        autoUpdateHandler.removeCallbacksAndMessages(null);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void loadReservations() {
        String userId = auth.getCurrentUser().getUid();
        Log.d("ReservationHistory", "Loading reservations for user: " + userId);

        // Load rentals (where user is renter)
        db.collection("reservations")
            .whereEqualTo("renterId", userId)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e("ReservationHistory", "Error loading rentals", error);
                    return;
                }

                if (value != null) {
                    List<Reservation> rentals = new ArrayList<>();
                    value.forEach(doc -> {
                        Reservation reservation = doc.toObject(Reservation.class);
                        reservation.setId(doc.getId());
                        
                        // Check if reservation should be marked as completed
                        if ((reservation.getStatus().equals("accepted") || reservation.getStatus().equals("pending")) 
                            && reservation.getEndDate().before(new Date())) {
                            // Update status in Firestore
                            doc.getReference().update("status", "completed")
                                .addOnSuccessListener(aVoid -> 
                                    Log.d(TAG, "Reservation " + doc.getId() + " marked as completed"))
                                .addOnFailureListener(e -> 
                                    Log.e(TAG, "Error updating reservation status: " + e.getMessage()));
                            reservation.setStatus("completed");
                        }
                        
                        rentals.add(reservation);
                    });
                    Log.d("ReservationHistory", "Loaded " + rentals.size() + " rentals");
                    rentAdapter.updateReservations(rentals);
                }
            });

        // Load leases (where user is owner)
        db.collection("reservations")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e("ReservationHistory", "Error loading leases", error);
                    return;
                }

                if (value != null) {
                    List<Reservation> leases = new ArrayList<>();
                    value.forEach(doc -> {
                        Reservation reservation = doc.toObject(Reservation.class);
                        reservation.setId(doc.getId());
                        
                        // Check if reservation should be marked as completed
                        if ((reservation.getStatus().equals("accepted") || reservation.getStatus().equals("pending")) 
                            && reservation.getEndDate().before(new Date())) {
                            // Update status in Firestore
                            doc.getReference().update("status", "completed")
                                .addOnSuccessListener(aVoid -> 
                                    Log.d(TAG, "Reservation " + doc.getId() + " marked as completed"))
                                .addOnFailureListener(e -> 
                                    Log.e(TAG, "Error updating reservation status: " + e.getMessage()));
                            reservation.setStatus("completed");
                        }
                        
                        leases.add(reservation);
                    });
                    Log.d("ReservationHistory", "Loaded " + leases.size() + " leases");
                    leaseAdapter.updateReservations(leases);
                }
            });
    }

    private class ReservationPagerAdapter extends RecyclerView.Adapter<ReservationPagerAdapter.PageViewHolder> {
        @NonNull
        @Override
        public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecyclerView recyclerView = new RecyclerView(parent.getContext());
            recyclerView.setLayoutManager(new LinearLayoutManager(parent.getContext()));
            recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
            return new PageViewHolder(recyclerView);
        }

        @Override
        public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
            holder.recyclerView.setAdapter(position == 0 ? rentAdapter : leaseAdapter);
        }

        @Override
        public int getItemCount() {
            return 2; // Two pages: Rentals and Leases
        }

        class PageViewHolder extends RecyclerView.ViewHolder {
            final RecyclerView recyclerView;

            PageViewHolder(RecyclerView itemView) {
                super(itemView);
                this.recyclerView = itemView;
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
