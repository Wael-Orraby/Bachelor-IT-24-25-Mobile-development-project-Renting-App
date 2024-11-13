package com.ap.neighborrentapplication.models

data class Device(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val pricePerDay: Double = 0.0,
    val available: Boolean = true,
    val city: String = "",
    val postalCode: String=""
)