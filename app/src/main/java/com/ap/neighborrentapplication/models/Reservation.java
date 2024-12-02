package com.ap.neighborrentapplication.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import java.util.Date;
import java.util.Arrays;

public class Reservation {
    private static final String TAG = "Reservation";
    private String id;
    private String deviceId;
    private String renterId;
    private String ownerId;
    private Date startDate;
    private Date endDate;
    private double totalPrice;
    private String status;  // "pending", "accepted", "rejected", "completed"
    private Timestamp createdAt;

    public Reservation() {
        // Required empty constructor for Firebase
    }

    public Reservation(String deviceId, String renterId, String ownerId, Date startDate, Date endDate, double totalPrice) {
        this.deviceId = deviceId;
        this.renterId = renterId;
        this.ownerId = ownerId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.status = "pending";
        this.createdAt = Timestamp.now();
    }

    // Method to update status in Firestore
    public void updateStatus(String newStatus, OnStatusUpdateListener listener) {
        if (id == null || id.isEmpty()) {
            Log.e(TAG, "Cannot update status: reservation ID is null or empty");
            if (listener != null) {
                listener.onFailure("Invalid reservation ID");
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("reservations")
            .document(id)
            .update("status", newStatus)
            .addOnSuccessListener(aVoid -> {
                this.status = newStatus;
                Log.d(TAG, "Status successfully updated to: " + newStatus);
                if (listener != null) {
                    listener.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating status: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onFailure(e.getMessage());
                }
            });
    }

    // Interface for status update callbacks
    public interface OnStatusUpdateListener {
        void onSuccess();
        void onFailure(String error);
    }

    // Method to check and update completion status
    public void checkAndUpdateCompletionStatus() {
        if (status != null && (status.equals("accepted") || status.equals("pending"))) {
            Date now = new Date();
            if (endDate != null && now.after(endDate)) {
                updateStatus("completed", new OnStatusUpdateListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Reservation marked as completed automatically");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "Failed to mark reservation as completed: " + error);
                    }
                });
            }
        }
    }

    // Static method to check all reservations in Firestore
    public static void checkAndUpdateAllReservations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Date now = new Date();

        db.collection("reservations")
            .whereIn("status", Arrays.asList("accepted", "pending"))
            .whereLessThan("endDate", now)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    Reservation reservation = document.toObject(Reservation.class);
                    if (reservation != null) {
                        reservation.setId(document.getId());
                        reservation.updateStatus("completed", new OnStatusUpdateListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Batch update: Reservation " + document.getId() + " marked as completed");
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.e(TAG, "Batch update: Failed to mark reservation " + document.getId() + " as completed: " + error);
                            }
                        });
                    }
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error checking reservations: " + e.getMessage(), e));
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getRenterId() {
        return renterId;
    }

    public void setRenterId(String renterId) {
        this.renterId = renterId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
