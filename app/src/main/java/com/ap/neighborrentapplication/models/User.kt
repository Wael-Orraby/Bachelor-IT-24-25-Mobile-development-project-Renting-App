package com.ap.neighborrentapplication.models

data class User(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val street: String = "",
    val streetNumber: String = "",
    val city: String = "",
    val postalCode: String = "",
    val email: String = "",
    var itemsRented: Int = 0,
    var rating: Double = 0.0,
    var responseRatio: Int = 0,
    var isActive: Boolean = true,
    var profileImageUrl: String = ""
)