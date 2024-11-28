package com.ap.neighborrentapplication.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.models.Device;
import com.ap.neighborrentapplication.models.Reservation;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupedReservationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "GroupedReservationAdapter";
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    
    private List<Object> items = new ArrayList<>();
    private List<Reservation> allReservations = new ArrayList<>();
    private Map<String, String> statusTranslations = new LinkedHashMap<>();
    private Map<String, Boolean> expandedStates = new LinkedHashMap<>();
    private boolean isLeaseView;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    
    public GroupedReservationAdapter(boolean isLeaseView) {
        this.isLeaseView = isLeaseView;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("nl"));
        setupStatusTranslations();
        initExpandedStates();
    }
    
    private void setupStatusTranslations() {
        statusTranslations.put("pending", "In Afwachting");
        statusTranslations.put("accepted", "Geaccepteerd");
        statusTranslations.put("completed", "Afgerond");
        statusTranslations.put("rejected", "Geweigerd");
    }
    
    private void initExpandedStates() {
        expandedStates.put("In Afwachting", true); // Standaard open
        expandedStates.put("Geaccepteerd", false);
        expandedStates.put("Afgerond", false);
        expandedStates.put("Geweigerd", false);
    }
    
    public void updateReservations(List<Reservation> reservations) {
        Log.d("GroupedAdapter", "Updating reservations: " + reservations.size());
        items.clear();
        allReservations.clear();
        allReservations.addAll(reservations);
        Map<String, List<Reservation>> groupedReservations = new LinkedHashMap<>();
        
        // Initialize groups in desired order
        for (String status : statusTranslations.keySet()) {
            groupedReservations.put(status, new ArrayList<>());
        }
        
        // Group reservations by status
        for (Reservation reservation : reservations) {
            String status = reservation.getStatus();
            if (status == null) status = "pending";
            status = status.toLowerCase();
            
            List<Reservation> group = groupedReservations.get(status);
            if (group != null) {
                group.add(reservation);
            }
        }
        
        // Add headers and items
        for (Map.Entry<String, List<Reservation>> entry : groupedReservations.entrySet()) {
            String translatedStatus = statusTranslations.get(entry.getKey());
            if (!entry.getValue().isEmpty()) {
                Log.d("GroupedAdapter", "Adding group " + translatedStatus + " with " + entry.getValue().size() + " items");
                // Add header with count
                items.add(new GroupHeader(translatedStatus, entry.getValue().size()));
                // Add items if expanded
                if (expandedStates.get(translatedStatus)) {
                    Log.d("GroupedAdapter", "Group " + translatedStatus + " is expanded, adding items");
                    items.addAll(entry.getValue());
                }
            }
        }
        
        Log.d("GroupedAdapter", "Total items after update: " + items.size());
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof GroupHeader ? TYPE_HEADER : TYPE_ITEM;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_reservation_group_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(isLeaseView ? R.layout.item_lease_reservation : R.layout.item_reservation, parent, false);
            return new ItemViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            bindHeaderViewHolder((HeaderViewHolder) holder, position);
        } else {
            bindItemViewHolder((ItemViewHolder) holder, position);
        }
    }

    private void bindHeaderViewHolder(HeaderViewHolder holder, int position) {
        GroupHeader header = (GroupHeader) items.get(position);
        holder.headerText.setText(header.getTitle());
        holder.itemCount.setText(String.format("(%d)", header.getCount()));
        
        boolean isExpanded = expandedStates.get(header.getTitle());
        holder.arrowIcon.setRotation(isExpanded ? 180 : 0);
        
        holder.itemView.setOnClickListener(v -> toggleGroup(header.getTitle(), position));
    }

    private void bindItemViewHolder(ItemViewHolder holder, int position) {
        Reservation reservation = (Reservation) items.get(position);
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
        String statusText = statusTranslations.get(status);
        switch (status) {
            case "accepted":
                chipColor = context.getColor(R.color.accepted_color);
                if (isLeaseView && holder.actionButtons != null) {
                    holder.actionButtons.setVisibility(View.GONE);
                }
                break;
            case "completed":
                chipColor = context.getColor(R.color.completed_color);
                if (isLeaseView && holder.actionButtons != null) {
                    holder.actionButtons.setVisibility(View.GONE);
                }
                break;
            case "rejected":
                chipColor = context.getColor(R.color.rejected_color);
                if (isLeaseView && holder.actionButtons != null) {
                    holder.actionButtons.setVisibility(View.GONE);
                }
                break;
            default: // pending
                chipColor = context.getColor(R.color.pending_color);
                if (isLeaseView && holder.actionButtons != null) {
                    holder.actionButtons.setVisibility(View.VISIBLE);
                }
                break;
        }
        holder.statusChip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(chipColor));
        holder.statusChip.setText(statusText);

        // Load renter details for lease view
        if (isLeaseView && holder.renterName != null) {
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
        }

        // Load device details
        String deviceId = reservation.getDeviceId();
        Log.d(TAG, "Loading device with ID: " + deviceId);

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
                    }
                } else {
                    holder.deviceName.setText("Device Not Found");
                    holder.deviceImage.setImageResource(R.drawable.ic_device_placeholder);
                }
            })
            .addOnFailureListener(e -> {
                holder.deviceName.setText("Error Loading Device");
                holder.deviceImage.setImageResource(R.drawable.ic_device_placeholder);
                Log.e(TAG, "Error loading device: " + e.getMessage(), e);
            });

        // Set action button listeners for lease view
        if (isLeaseView && holder.actionButtons != null) {
            holder.acceptButton.setOnClickListener(v -> {
                reservation.updateStatus("accepted", new Reservation.OnStatusUpdateListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(context, "Reservering geaccepteerd", Toast.LENGTH_SHORT).show();
                        notifyItemChanged(position);
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
                        Toast.makeText(context, "Reservering geweigerd", Toast.LENGTH_SHORT).show();
                        notifyItemChanged(position);
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(context, "Fout bij het weigeren: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    private void toggleGroup(String groupTitle, int headerPosition) {
        boolean isExpanded = expandedStates.get(groupTitle);
        expandedStates.put(groupTitle, !isExpanded);
        Log.d("GroupedAdapter", "Toggling group " + groupTitle + " at position " + headerPosition + ", expanded: " + !isExpanded);
        
        // Find the corresponding status
        String status = null;
        for (Map.Entry<String, String> entry : statusTranslations.entrySet()) {
            if (entry.getValue().equals(groupTitle)) {
                status = entry.getKey();
                break;
            }
        }
        
        if (status != null) {
            final String finalStatus = status;
            // Find all reservations for this status from the original list
            List<Reservation> groupReservations = new ArrayList<>();
            for (Reservation res : allReservations) {
                String resStatus = res.getStatus();
                if (resStatus == null) resStatus = "pending";
                if (finalStatus.equalsIgnoreCase(resStatus)) {
                    groupReservations.add(res);
                }
            }
            
            Log.d("GroupedAdapter", "Found " + groupReservations.size() + " reservations for status " + status);
            
            if (isExpanded) {
                // Remove items
                int startPosition = headerPosition + 1;
                Log.d("GroupedAdapter", "Removing " + groupReservations.size() + " items starting at position " + startPosition);
                for (int i = 0; i < groupReservations.size(); i++) {
                    items.remove(startPosition);
                }
                notifyItemRangeRemoved(startPosition, groupReservations.size());
            } else {
                // Add items
                int startPosition = headerPosition + 1;
                Log.d("GroupedAdapter", "Adding " + groupReservations.size() + " items at position " + startPosition);
                items.addAll(startPosition, groupReservations);
                notifyItemRangeInserted(startPosition, groupReservations.size());
            }
            
            // Rotate arrow
            notifyItemChanged(headerPosition);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;
        TextView itemCount;
        ImageView arrowIcon;
        
        HeaderViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.header_text);
            itemCount = itemView.findViewById(R.id.item_count);
            arrowIcon = itemView.findViewById(R.id.arrow_icon);
        }
    }
    
    private static class GroupHeader {
        private final String title;
        private final int count;
        
        GroupHeader(String title, int count) {
            this.title = title;
            this.count = count;
        }
        
        String getTitle() {
            return title;
        }
        
        int getCount() {
            return count;
        }
    }
    
    private class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView reservationDates;
        TextView totalPrice;
        TextView durationText;
        TextView renterName;
        ImageView deviceImage;
        com.google.android.material.chip.Chip statusChip;
        LinearLayout actionButtons;
        Button acceptButton;
        Button rejectButton;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            reservationDates = itemView.findViewById(R.id.reservationDates);
            totalPrice = itemView.findViewById(R.id.totalPrice);
            durationText = itemView.findViewById(R.id.durationText);
            deviceImage = itemView.findViewById(R.id.deviceImage);
            statusChip = itemView.findViewById(R.id.statusChip);
            
            // Alleen voor lease view
            if (isLeaseView) {
                renterName = itemView.findViewById(R.id.renterName);
                actionButtons = itemView.findViewById(R.id.actionButtons);
                acceptButton = itemView.findViewById(R.id.acceptButton);
                rejectButton = itemView.findViewById(R.id.rejectButton);
            }
        }
    }
}
