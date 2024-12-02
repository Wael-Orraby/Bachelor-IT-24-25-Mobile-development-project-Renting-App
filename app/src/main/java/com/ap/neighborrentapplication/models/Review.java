package com.ap.neighborrentapplication.models;

import com.google.firebase.firestore.DocumentId;
import java.util.Date;

public class Review {
    @DocumentId
    private String id;
    private String deviceId;
    private String reservationId;
    private String userId;
    private float rating;
    private String comment;
    private Date createdAt;

    public Review() {
        // Required empty constructor for Firestore
    }

    public Review(String deviceId, String reservationId, String userId, float rating, String comment) {
        this.deviceId = deviceId;
        this.reservationId = reservationId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = new Date();
    }

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

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
