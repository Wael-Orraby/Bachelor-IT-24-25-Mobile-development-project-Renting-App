package com.ap.neighborrentapplication.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

data class Device(
    @DocumentId
    var id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerEmail: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val categoryName: String = "",
    val city: String = "",
    val postalCode: String = "",
    val pricePerDay: Double = 0.0,
    val available: Boolean = true
)