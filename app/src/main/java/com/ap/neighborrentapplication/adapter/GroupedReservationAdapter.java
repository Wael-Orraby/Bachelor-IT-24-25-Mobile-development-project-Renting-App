package com.ap.neighborrentapplication.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ap.neighborrentapplication.R;
import com.ap.neighborrentapplication.models.Device;
import com.ap.neighborrentapplication.models.Reservation;
import com.ap.neighborrentapplication.models.Review;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private FirebaseAuth auth;
    private Context context;
    
    public GroupedReservationAdapter(boolean isLeaseView, Context context) {
        this.isLeaseView = isLeaseView;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
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
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            Reservation reservation = (Reservation) items.get(position);
            Context context = holder.itemView.getContext();
            
            // Load device details
            db.collection("devices")
                .document(reservation.getDeviceId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Device device = documentSnapshot.toObject(Device.class);
                    if (device != null) {
                        itemHolder.deviceName.setText(device.getName());
                        // Load device image
                        if (device.getImageUrl() != null) {
                            Glide.with(context)
                                .load(device.getImageUrl())
                                .centerCrop()
                                .into(itemHolder.deviceImage);
                        }
                    }
                });
            
            // Set reservation dates
            String dateRange = String.format("%s - %s",
                dateFormat.format(reservation.getStartDate()),
                dateFormat.format(reservation.getEndDate()));
            itemHolder.reservationDates.setText(dateRange);
            
            // Set price and duration
            itemHolder.totalPrice.setText(String.format(Locale.getDefault(), "€%.2f", reservation.getTotalPrice()));
            
            // Calculate duration
            long diffInMillis = reservation.getEndDate().getTime() - reservation.getStartDate().getTime();
            int days = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
            itemHolder.durationText.setText(String.format("(%d dagen)", days));
            
            // Set status chip
            String status = reservation.getStatus();
            if (status == null) status = "pending";
            status = status.toLowerCase();
            
            int chipColor;
            String statusText = statusTranslations.get(status);
            itemHolder.statusChip.setText(statusText);
            
            switch (status) {
                case "accepted":
                    chipColor = context.getColor(R.color.accepted_color);
                    if (isLeaseView && itemHolder.actionButtons != null) {
                        itemHolder.actionButtons.setVisibility(View.GONE);
                    }
                    if (!isLeaseView && itemHolder.reviewButton != null) {
                        itemHolder.reviewButton.setVisibility(View.GONE);
                    }
                    break;
                case "completed":
                    chipColor = context.getColor(R.color.completed_color);
                    if (isLeaseView && itemHolder.actionButtons != null) {
                        itemHolder.actionButtons.setVisibility(View.GONE);
                    }
                    
                    // Show reviews for completed reservations
                    setupReviews(itemHolder, reservation);
                    break;
                case "rejected":
                    chipColor = context.getColor(R.color.rejected_color);
                    if (isLeaseView && itemHolder.actionButtons != null) {
                        itemHolder.actionButtons.setVisibility(View.GONE);
                    }
                    if (!isLeaseView && itemHolder.reviewButton != null) {
                        itemHolder.reviewButton.setVisibility(View.GONE);
                    }
                    break;
                default: // pending
                    chipColor = context.getColor(R.color.pending_color);
                    if (isLeaseView && itemHolder.actionButtons != null) {
                        itemHolder.actionButtons.setVisibility(View.VISIBLE);
                        setupActionButtons(itemHolder, reservation);
                    }
                    if (!isLeaseView && itemHolder.reviewButton != null) {
                        itemHolder.reviewButton.setVisibility(View.GONE);
                    }
                    break;
            }
            
            itemHolder.statusChip.setChipBackgroundColor(ColorStateList.valueOf(chipColor));
            
            // Load renter name for lease view
            if (isLeaseView && itemHolder.renterName != null) {
                db.collection("users")
                    .document(reservation.getRenterId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        if (firstName != null && lastName != null) {
                            itemHolder.renterName.setText(firstName + " " + lastName);
                        }
                    });
            }
        }
    }
    
    private void setupActionButtons(ItemViewHolder holder, Reservation reservation) {
        if (holder.acceptButton != null && holder.rejectButton != null) {
            holder.acceptButton.setOnClickListener(v -> {
                reservation.setStatus("accepted");
                updateReservationStatus(reservation, "accepted");
            });
            
            holder.rejectButton.setOnClickListener(v -> {
                reservation.setStatus("rejected");
                updateReservationStatus(reservation, "rejected");
            });
        }
    }
    
    private void updateReservationStatus(Reservation reservation, String newStatus) {
        db.collection("reservations")
            .document(reservation.getId())
            .update("status", newStatus)
            .addOnSuccessListener(aVoid -> {
                String message = newStatus.equals("accepted") ? 
                    "Reservering geaccepteerd" : "Reservering geweigerd";
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating reservation status", e);
                Toast.makeText(context, "Fout bij bijwerken status", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void setupReviews(ItemViewHolder holder, Reservation reservation) {
        // Check if the views are available
        if (holder.reviewsContainer == null || holder.reviewsRecyclerView == null) {
            Log.e(TAG, "Reviews container or RecyclerView is null");
            return;
        }

        try {
            LinearLayoutManager layoutManager = new LinearLayoutManager(holder.itemView.getContext());
            holder.reviewsRecyclerView.setLayoutManager(layoutManager);
            ReviewAdapter adapter = new ReviewAdapter(holder.itemView.getContext());
            holder.reviewsRecyclerView.setAdapter(adapter);

            db.collection("reviews")
                .whereEqualTo("reservationId", reservation.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Review review = doc.toObject(Review.class);
                        if (review != null) {
                            review.setId(doc.getId());
                            reviews.add(review);
                        }
                    }
                    adapter.setReviews(reviews);
                    
                    // Show reviews container if there are reviews
                    if (holder.reviewsContainer != null) {
                        holder.reviewsContainer.setVisibility(reviews.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    // Handle review button visibility
                    if (holder.reviewButton != null) {
                        // Hide review button in lease view
                        if (isLeaseView) {
                            holder.reviewButton.setVisibility(View.GONE);
                        } else {
                            // In rent view, check if user has already reviewed
                            String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                            if (currentUserId != null && currentUserId.equals(reservation.getRenterId())) {
                                boolean hasReviewed = false;
                                for (Review review : reviews) {
                                    if (currentUserId.equals(review.getUserId())) {
                                        hasReviewed = true;
                                        break;
                                    }
                                }
                                holder.reviewButton.setVisibility(hasReviewed ? View.GONE : View.VISIBLE);
                                if (!hasReviewed) {
                                    holder.reviewButton.setOnClickListener(v -> showReviewDialog(reservation));
                                }
                            } else {
                                holder.reviewButton.setVisibility(View.GONE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews", e);
                    if (holder.reviewsContainer != null) {
                        holder.reviewsContainer.setVisibility(View.GONE);
                    }
                    if (holder.reviewButton != null) {
                        holder.reviewButton.setVisibility(View.GONE);
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up reviews", e);
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

    private void showReviewDialog(Reservation reservation) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);

        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        TextInputEditText commentEditText = dialogView.findViewById(R.id.commentEditText);
        Button submitButton = dialogView.findViewById(R.id.submitButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        AlertDialog dialog = builder.create();

        submitButton.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = commentEditText.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(context, "Geef een beoordeling", Toast.LENGTH_SHORT).show();
                return;
            }

            Review review = new Review();
            review.setDeviceId(reservation.getDeviceId());
            review.setReservationId(reservation.getId());
            review.setUserId(auth.getCurrentUser().getUid());
            review.setRating(rating);
            review.setComment(comment);
            review.setCreatedAt(new Date());

            db.collection("reviews")
                .add(review)
                .addOnSuccessListener(documentReference -> {
                    dialog.dismiss();
                    Toast.makeText(context, "Beoordeling geplaatst", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding review", e);
                    Toast.makeText(context, "Fout bij plaatsen beoordeling", Toast.LENGTH_SHORT).show();
                });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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
        Chip statusChip;
        LinearLayout actionButtons;
        Button acceptButton;
        Button rejectButton;
        Button reviewButton;
        LinearLayout reviewsContainer;
        RecyclerView reviewsRecyclerView;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            reservationDates = itemView.findViewById(R.id.reservationDates);
            totalPrice = itemView.findViewById(R.id.totalPrice);
            durationText = itemView.findViewById(R.id.durationText);
            deviceImage = itemView.findViewById(R.id.deviceImage);
            statusChip = itemView.findViewById(R.id.statusChip);
            reviewButton = itemView.findViewById(R.id.reviewButton);
            reviewsContainer = itemView.findViewById(R.id.reviewsContainer);
            reviewsRecyclerView = itemView.findViewById(R.id.reviewsRecyclerView);
            
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
