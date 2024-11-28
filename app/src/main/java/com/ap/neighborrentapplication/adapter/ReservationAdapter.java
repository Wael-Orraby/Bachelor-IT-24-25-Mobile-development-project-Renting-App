package com.ap.neighborrentapplication.adapter;

import android.content.Context;
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
            holder.renterName.setText("Loading...");
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
                            holder.renterName.setText(fullName);
                            Log.d(TAG, "Setting renter name to: " + fullName);
                        } else {
                            holder.renterName.setText("Onbekende gebruiker");
                            Log.d(TAG, "Renter name fields are null");
                        }
                    } else {
                        holder.renterName.setText("Gebruiker niet gevonden");
                        Log.d(TAG, "Renter document does not exist");
                    }
                })
                .addOnFailureListener(e -> {
                    holder.renterName.setText("Fout bij laden gebruiker");
                    Log.e(TAG, "Error loading renter: " + e.getMessage(), e);
                });
        } else {
            holder.renterName.setText("Geen huurder ID");
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
        TextView deviceName;
        TextView reservationDates;
        TextView totalPrice;
        TextView durationText;
        TextView renterName;
        ImageView deviceImage;
        Chip statusChip;
        
        public ReservationViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            reservationDates = itemView.findViewById(R.id.reservationDates);
            totalPrice = itemView.findViewById(R.id.totalPrice);
            durationText = itemView.findViewById(R.id.durationText);
            deviceImage = itemView.findViewById(R.id.deviceImage);
            statusChip = itemView.findViewById(R.id.statusChip);
            renterName = itemView.findViewById(R.id.renterName);
        }
    }
}
