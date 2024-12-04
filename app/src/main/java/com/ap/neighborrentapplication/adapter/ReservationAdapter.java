package com.ap.neighborrentapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.models.Device;
import com.ap.neighborrentapplication.models.Reservation;
import com.ap.neighborrentapplication.ui.activity.ProfileActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder> {
    private static final String TAG = "ReservationAdapter";
    private List<Reservation> reservations;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    
    public ReservationAdapter(List<Reservation> reservations) {
        this.reservations = reservations;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("nl"));
    }
    
    @NonNull
    @Override
    public ReservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lease_reservation, parent, false);
        return new ReservationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReservationViewHolder holder, int position) {
        Reservation reservation = reservations.get(position);
        Context context = holder.itemView.getContext();
        
        // Set reservation dates
        String dateRange = String.format("%s - %s",
            dateFormat.format(reservation.getStartDate()),
            dateFormat.format(reservation.getEndDate()));
        holder.reservationDates.setText(dateRange);
        
        // Set price and duration
        holder.totalPrice.setText(String.format(Locale.getDefault(), "€%.2f", reservation.getTotalPrice()));
        
        // Calculate duration
        long diffInMillis = reservation.getEndDate().getTime() - reservation.getStartDate().getTime();
        int days = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
        holder.durationText.setText(String.format("(%d dagen)", days));
        
        // Set status chip
        String status = reservation.getStatus();
        if (status == null) status = "pending";
        status = status.toLowerCase();
        
        int chipColor;
        String statusText;
        switch (status) {
            case "accepted":
                chipColor = context.getColor(R.color.accepted_color);
                statusText = "Geaccepteerd";
                break;
            case "completed":
                chipColor = context.getColor(R.color.completed_color);
                statusText = "Afgerond";
                break;
            case "rejected":
                chipColor = context.getColor(R.color.rejected_color);
                statusText = "Geweigerd";
                break;
            default: // pending
                chipColor = context.getColor(R.color.pending_color);
                statusText = "In behandeling";
                break;
        }
        holder.statusChip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(chipColor));
        holder.statusChip.setText(statusText);

        // Load renter details
        String renterId = reservation.getRenterId();
        Log.d(TAG, "Loading renter with ID: " + renterId);
        
        if (renterId != null && !renterId.isEmpty()) {
            holder.ownerName.setText("Laden...");
            db.collection("users")
                .document(renterId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        Log.d(TAG, "Renter data loaded - firstName: " + firstName + ", lastName: " + lastName);
                        if (firstName != null && lastName != null) {
                            String fullName = firstName + " " + lastName;
                            holder.ownerName.setText(fullName);
                            Log.d(TAG, "Setting renter name to: " + fullName);
                            
                            // Add click listener to owner container
                            if (holder.ownerContainer != null) {
                                holder.ownerContainer.setOnClickListener(v -> {
                                    Intent intent = new Intent(context, ProfileActivity.class);
                                    intent.putExtra("userId", renterId);
                                    context.startActivity(intent);
                                });
                                holder.ownerContainer.setClickable(true);
                            }
                        } else {
                            holder.ownerName.setText("Onbekende gebruiker");
                            if (holder.ownerContainer != null) {
                                holder.ownerContainer.setClickable(false);
                            }
                            Log.d(TAG, "Renter name fields are null");
                        }
                    } else {
                        holder.ownerName.setText("Gebruiker niet gevonden");
                        if (holder.ownerContainer != null) {
                            holder.ownerContainer.setClickable(false);
                        }
                        Log.d(TAG, "Renter document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    holder.ownerName.setText("Fout bij laden gebruiker");
                    if (holder.ownerContainer != null) {
                        holder.ownerContainer.setClickable(false);
                    }
                    Log.e(TAG, "Error loading renter: " + e.getMessage(), e);
                });
        } else {
            holder.ownerName.setText("Geen huurder ID");
            if (holder.ownerContainer != null) {
                holder.ownerContainer.setClickable(false);
            }
            Log.d(TAG, "No renter ID available");
        }
        
        String deviceId = reservation.getDeviceId();
        Log.d(TAG, "Loading device with ID: " + deviceId);
        
        // Set default values
        holder.deviceName.setText("Loading...");
        
        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "Device ID is null or empty");
            holder.deviceName.setText("Invalid Device ID");
            return;
        }
        
        // Load device details
        db.collection("devices")
            .document(deviceId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    try {
                        Device device = documentSnapshot.toObject(Device.class);
                        if (device != null) {
                            device.setId(documentSnapshot.getId());
                            String name = device.getName();
                            String imageUrl = device.getImageUrl();
                            
                            holder.deviceName.setText(name != null && !name.isEmpty() ? name : "Unnamed Device");
                            
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(context)
                                    .load(imageUrl)
                                    .centerCrop()
                                    .placeholder(R.drawable.ic_device_placeholder)
                                    .error(R.drawable.ic_device_placeholder)
                                    .into(holder.deviceImage);
                            } else {
                                holder.deviceImage.setImageResource(R.drawable.ic_device_placeholder);
                            }
                        } else {
                            holder.deviceName.setText("Device Data Error");
                            holder.deviceImage.setImageResource(R.drawable.ic_device_placeholder);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting document to Device: " + e.getMessage(), e);
                        holder.deviceName.setText("Error Loading Device");
                        holder.deviceImage.setImageResource(R.drawable.ic_device_placeholder);
                    }
                } else {
                    holder.deviceName.setText("Device Not Found");
                    holder.deviceImage.setImageResource(R.drawable.ic_device_placeholder);
                }
            })
            .addOnFailureListener(e -> {
                holder.deviceName.setText("Network Error");
                holder.deviceImage.setImageResource(R.drawable.ic_device_placeholder);
                Log.e(TAG, "Error loading device: " + e.getMessage(), e);
            });
    }
    
    @Override
    public int getItemCount() {
        return reservations.size();
    }
    
    public void updateReservations(List<Reservation> newReservations) {
        this.reservations = newReservations;
        notifyDataSetChanged();
    }
    
    public static class ReservationViewHolder extends RecyclerView.ViewHolder {
        ImageView deviceImage;
        TextView deviceName;
        TextView ownerName;
        TextView reservationDates;
        TextView totalPrice;
        TextView durationText;
        Chip statusChip;
        View ownerContainer;

        public ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceImage = itemView.findViewById(R.id.deviceImage);
            deviceName = itemView.findViewById(R.id.deviceName);
            ownerName = itemView.findViewById(R.id.ownerName);
            reservationDates = itemView.findViewById(R.id.reservationDates);
            totalPrice = itemView.findViewById(R.id.totalPrice);
            durationText = itemView.findViewById(R.id.durationText);
            statusChip = itemView.findViewById(R.id.statusChip);
            ownerContainer = itemView.findViewById(R.id.ownerContainer);
        }
    }
}
