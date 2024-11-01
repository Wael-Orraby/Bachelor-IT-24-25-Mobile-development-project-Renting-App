package com.ap.neighborrentapplication.models

data class User(
    val firstName: String = "",
    val lastName: String = "",
    val street: String = "",
    val streetNumber: String = "",
    val city: String = "",
    val postalCode: String = "",
    val email: String = ""
)