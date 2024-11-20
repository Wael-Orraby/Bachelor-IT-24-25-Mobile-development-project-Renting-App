package com.ap.neighborrentapplication.models

data class Profile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val location: String = "",
    val profileImageUrl: String = "",
    val itemsRented: Int = 0,
    val rating: Double = 0.0,
    val responseRatio: Int = 0,
    val isActive: Boolean = true
) {
    // Lege constructor voor Firebase
    constructor() : this("", "", "", "", "")
}
