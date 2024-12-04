package com.ap.neighborrentapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.models.Device;
import com.ap.neighborrentapplication.models.Reservation;
import com.ap.neighborrentapplication.ui.activity.ProfileActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class LeaseReservationAdapter extends RecyclerView.Adapter<LeaseReservationAdapter.LeaseViewHolder> {
    private static final String TAG = "LeaseReservationAdapter";
    private List<Reservation> reservations;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    
    public LeaseReservationAdapter(List<Reservation> reservations) {
        this.reservations = reservations;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("nl"));
    }
    
    @NonNull
    @Override
    public LeaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lease_reservation, parent, false);
        return new LeaseViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LeaseViewHolder holder, int position) {
        Reservation reservation = reservations.get(position);
        Context context = holder.itemView.getContext();
        
        // Load device details
        db.collection("devices")
            .document(reservation.getDeviceId())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Device device = documentSnapshot.toObject(Device.class);
                if (device != null) {
                    device.setId(documentSnapshot.getId());
                    holder.deviceName.setText(device.getName());
                    
                    if (device.getImageUrl() != null && !device.getImageUrl().isEmpty()) {
                        Glide.with(context)
                            .load(device.getImageUrl())
                            .centerCrop()
                            .placeholder(R.drawable.ic_device_placeholder)
                            .error(R.drawable.ic_device_placeholder)
                            .into(holder.deviceImage);
                    }
                }
            });
            
        // Load renter details
        String renterId = reservation.getRenterId();
        if (renterId != null && !renterId.isEmpty()) {
            holder.ownerName.setText("Laden...");
            db.collection("users")
                .document(renterId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    if (firstName != null && lastName != null) {
                        String fullName = firstName + " " + lastName;
                        holder.ownerName.setText(fullName);
                        
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
        }
        
        // Set reservation dates
        String dateRange = String.format("%s - %s",
            dateFormat.format(reservation.getStartDate()),
            dateFormat.format(reservation.getEndDate()));
        holder.reservationDates.setText(dateRange);
        
        // Set total price
        holder.totalPrice.setText(String.format("€%.2f", reservation.getTotalPrice()));
        
        // Update status and buttons
        updateStatusAndButtons(holder, reservation, context);
    }
    
    private void updateStatusAndButtons(LeaseViewHolder holder, Reservation reservation, Context context) {
        String status = reservation.getStatus();
        if (status == null) status = "pending";
        
        // Update status chip
        int backgroundColor;
        String displayText;
        
        switch (status.toLowerCase()) {
            case "pending":
                backgroundColor = R.color.pending_color;
                displayText = "In afwachting";
                holder.actionButtons.setVisibility(View.VISIBLE);
                break;
            case "accepted":
                backgroundColor = R.color.accepted_color;
                displayText = "Geaccepteerd";
                holder.actionButtons.setVisibility(View.GONE);
                break;
            case "completed":
                backgroundColor = R.color.completed_color;
                displayText = "Afgerond";
                holder.actionButtons.setVisibility(View.GONE);
                break;
            case "rejected":
                backgroundColor = R.color.rejected_color;
                displayText = "Geweigerd";
                holder.actionButtons.setVisibility(View.GONE);
                break;
            default:
                backgroundColor = R.color.dark_blue;
                displayText = status;
                holder.actionButtons.setVisibility(View.GONE);
                break;
        }
        
        holder.statusChip.setChipBackgroundColorResource(backgroundColor);
        holder.statusChip.setText(displayText);
        
        // Set button click listeners
        holder.acceptButton.setOnClickListener(v -> {
            reservation.updateStatus("accepted", new Reservation.OnStatusUpdateListener() {
                @Override
                public void onSuccess() {
                    updateStatusAndButtons(holder, reservation, context);
                    Toast.makeText(context, "Reservering geaccepteerd", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(context, "Fout bij het accepteren: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
        
        holder.rejectButton.setOnClickListener(v -> {
            reservation.updateStatus("rejected", new Reservation.OnStatusUpdateListener() {
                @Override
                public void onSuccess() {
                    updateStatusAndButtons(holder, reservation, context);
                    Toast.makeText(context, "Reservering geweigerd", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(context, "Fout bij het weigeren: " + error, Toast.LENGTH_SHORT).show();
                }
            });
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
    
    static class LeaseViewHolder extends RecyclerView.ViewHolder {
        ImageView deviceImage;
        TextView deviceName;
        TextView ownerName;
        TextView reservationDates;
        TextView totalPrice;
        Chip statusChip;
        LinearLayout actionButtons;
        MaterialButton acceptButton;
        MaterialButton rejectButton;
        View ownerContainer;
        
        LeaseViewHolder(View itemView) {
            super(itemView);
            deviceImage = itemView.findViewById(R.id.deviceImage);
            deviceName = itemView.findViewById(R.id.deviceName);
            ownerName = itemView.findViewById(R.id.ownerName);
            reservationDates = itemView.findViewById(R.id.reservationDates);
            totalPrice = itemView.findViewById(R.id.totalPrice);
            statusChip = itemView.findViewById(R.id.statusChip);
            actionButtons = itemView.findViewById(R.id.actionButtons);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            ownerContainer = itemView.findViewById(R.id.ownerContainer);
        }
    }
}
