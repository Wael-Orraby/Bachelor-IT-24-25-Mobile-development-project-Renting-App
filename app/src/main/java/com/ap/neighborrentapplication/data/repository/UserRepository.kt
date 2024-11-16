package com.ap.neighborrentapplication.data.repository

import com.ap.neighborrentapplication.models.User
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Functie om de gebruiker aan te maken
    suspend fun registerUser(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
    }

    // Functie om gebruikersgegevens op te slaan in Firestore
    suspend fun saveUserToFirestore(user: User): Task<Void> {
        val uid = auth.currentUser?.uid
        return if (uid != null) {
            firestore.collection("users").document(uid).set(user)
        } else {
            val task = TaskCompletionSource<Void>()
            task.setException(Exception("User not authenticated"))
            task.task
        }
    }
}

