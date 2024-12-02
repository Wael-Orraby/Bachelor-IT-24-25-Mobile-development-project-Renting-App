package com.ap.neighborrentapplication.ui.activity

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ap.neighborrentapplication.adapter.ProfileAdapter
import com.ap.neighborrentapplication.adapter.DevicesAdapter
import com.ap.neighborrentapplication.databinding.ActivityProfileBinding
import com.ap.neighborrentapplication.models.Device
import com.ap.neighborrentapplication.models.Profile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import android.widget.Toast
import com.ap.neighborrentapplication.R
import android.util.Log

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var isEditMode = false
    private var currentProfile: Profile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar("Mijn Profiel", true)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Zet initiële waarden voor statistieken
        binding.itemsRentedCount.text = "0"
        binding.averageRating.text = "0.0"
        binding.responseRatio.text = "0%"

        setupRecyclerView()
        setupProfileData()
        loadUserDevices()

        binding.editProfileFab.setOnClickListener {
            if (isEditMode) {
                // Save changes
                saveProfileChanges()
            } else {
                // Enter edit mode
                toggleEditMode(true)
            }
        }
    }

    private fun setupRecyclerView() {
        devicesAdapter = DevicesAdapter(ArrayList(), this)
        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = devicesAdapter
        }
    }

    private fun setupProfileData() {
        val userId = auth.currentUser?.uid ?: return
        
        // Haal gebruikersgegevens op
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    try {
                        // Veilig ophalen van velden met null checks en type conversie
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val email = document.getString("email") ?: ""
                        val phoneNumber = document.getString("phoneNumber") ?: ""
                        val city = document.getString("city") ?: ""
                        val postalCode = document.getString("postalCode") ?: ""
                        val profileImageUrl = document.getString("profileImageUrl") ?: ""
                        
                        // Veilig ophalen van numerieke velden met initiële waarden
                        val itemsRented = document.getLong("itemsRented")?.toInt() ?: 0
                        binding.itemsRentedCount.text = itemsRented.toString()

                        val rating = document.getDouble("rating") ?: 0.0
                        binding.averageRating.text = String.format("%.1f", rating)

                        val responseRatio = document.getLong("responseRatio")?.toInt() ?: 0
                        binding.responseRatio.text = "$responseRatio%"
                        
                        val isActive = document.getBoolean("isActive") ?: true

                        val profile = Profile(
                            id = userId,
                            name = "$firstName $lastName",
                            email = email,
                            phoneNumber = phoneNumber,
                            location = "$city, $postalCode",
                            profileImageUrl = profileImageUrl,
                            itemsRented = itemsRented,
                            rating = rating,
                            responseRatio = responseRatio,
                            isActive = isActive
                        )
                        
                        // Update UI met basis profiel data
                        updateProfileUI(profile)
                        
                        // Initialiseer statistieken als ze niet bestaan
                        val updates = mutableMapOf<String, Any>()
                        if (!document.contains("itemsRented")) updates["itemsRented"] = 0
                        if (!document.contains("rating")) updates["rating"] = 0.0
                        if (!document.contains("responseRatio")) updates["responseRatio"] = 0
                        
                        if (updates.isNotEmpty()) {
                            firestore.collection("users").document(userId)
                                .update(updates)
                                .addOnSuccessListener {
                                    Log.d("ProfileActivity", "Initialized missing statistics fields")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("ProfileActivity", "Error initializing statistics", e)
                                }
                        }
                        
                        // Haal statistieken op
                        loadProfileStatistics(userId)
                    } catch (e: Exception) {
                        Log.e("ProfileActivity", "Error setting up profile", e)
                        showToast("Er is een fout opgetreden bij het laden van je profiel")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error loading profile", e)
                showToast("Er is een fout opgetreden bij het laden van je profiel")
            }
    }

    private fun loadProfileStatistics(userId: String) {
        val userRef = firestore.collection("users").document(userId)
        
        Log.d("ProfileActivity", "Loading statistics for user: $userId")
        
        // 1. Aantal verhuurde items (completed reservations)
        firestore.collection("reservations")
            .whereEqualTo("ownerId", userId)  
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener { reservations ->
                Log.d("ProfileActivity", "Reservations query returned ${reservations.size()} documents")
                reservations.forEach { doc ->
                    Log.d("ProfileActivity", "Reservation: ${doc.data}")
                }
                
                val completedCount = reservations.size()
                binding.itemsRentedCount.text = completedCount.toString()
                currentProfile?.itemsRented = completedCount
                
                // Update in Firebase
                userRef.update("itemsRented", completedCount)
                    .addOnSuccessListener {
                        Log.d("ProfileActivity", "Updated itemsRented to $completedCount")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileActivity", "Failed to update itemsRented", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Failed to get reservations", e)
            }

        // 2. Gemiddelde rating
        // Eerst haal de devices van de gebruiker op
        firestore.collection("devices")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { devices ->
                Log.d("ProfileActivity", "Found ${devices.size()} devices for user")
                
                if (devices.isEmpty) {
                    // Geen devices, dus geen ratings
                    binding.averageRating.text = "0.0"
                    currentProfile?.rating = 0.0
                    userRef.update("rating", 0.0)
                    return@addOnSuccessListener
                }
                
                // Maak een lijst van alle device IDs
                val deviceIds = devices.documents.map { it.id }
                Log.d("ProfileActivity", "Device IDs: $deviceIds")
                
                // Haal reviews op voor alle devices
                firestore.collection("reviews")
                    .whereIn("deviceId", deviceIds)
                    .get()
                    .addOnSuccessListener { reviews ->
                        Log.d("ProfileActivity", "Found ${reviews.size()} reviews for all devices")
                        
                        var totalRating = 0.0
                        var reviewCount = 0
                        
                        reviews.forEach { review ->
                            val rating = review.getDouble("rating")
                            if (rating != null) {
                                totalRating += rating
                                reviewCount++
                            }
                        }
                        
                        val averageRating = if (reviewCount > 0) {
                            totalRating / reviewCount
                        } else {
                            0.0
                        }
                        
                        Log.d("ProfileActivity", "Calculated rating: $averageRating from $reviewCount reviews")
                        val formattedRating = String.format("%.1f", averageRating)
                        binding.averageRating.text = formattedRating
                        currentProfile?.rating = averageRating
                        
                        // Update in Firebase
                        userRef.update("rating", averageRating)
                            .addOnSuccessListener {
                                Log.d("ProfileActivity", "Updated rating to $averageRating")
                            }
                            .addOnFailureListener { e ->
                                Log.e("ProfileActivity", "Failed to update rating", e)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileActivity", "Error getting reviews for devices", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Error getting user devices", e)
            }

        // 3. Reactieratio (responded / total reservations)
        firestore.collection("reservations")
            .whereEqualTo("ownerId", userId)  
            .get()
            .addOnSuccessListener { reservations ->
                Log.d("ProfileActivity", "Response ratio query returned ${reservations.size()} documents")
                reservations.forEach { doc ->
                    Log.d("ProfileActivity", "Reservation status: ${doc.getString("status")}")
                }
                
                var totalRequests = 0
                var respondedRequests = 0
                
                reservations.forEach { reservation ->
                    val status = reservation.getString("status")
                    totalRequests++
                    if (status != null && status != "pending") {
                        respondedRequests++
                    }
                }
                
                val ratio = if (totalRequests > 0) {
                    (respondedRequests * 100) / totalRequests
                } else {
                    0
                }
                
                binding.responseRatio.text = "${ratio}%"
                currentProfile?.responseRatio = ratio
                
                // Update in Firebase
                userRef.update("responseRatio", ratio)
                    .addOnSuccessListener {
                        Log.d("ProfileActivity", "Updated responseRatio to $ratio")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileActivity", "Failed to update responseRatio", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileActivity", "Failed to get reservations for ratio", e)
            }
    }

    private fun updateProfileUI(profile: Profile) {
        currentProfile = profile
        binding.apply {
            userName.setText(profile.name)
            userEmail.setText(profile.email)
            userPhone.setText(profile.phoneNumber)
            userLocation.setText(profile.location)
            itemsRentedCount.text = profile.itemsRented.toString()
            averageRating.text = profile.rating.toString()
            responseRatio.text = "${profile.responseRatio}%"

            // Laad profielfoto
            if (profile.profileImageUrl.isNotEmpty()) {
                // Als er een URL is, laad die
                Glide.with(this@ProfileActivity)
                    .load(profile.profileImageUrl)
                    .placeholder(R.drawable.user1)
                    .error(R.drawable.user1)
                    .circleCrop()
                    .into(profileImage)
            } else {
                // Anders, gebruik de default user1 image
                Glide.with(this@ProfileActivity)
                    .load(R.drawable.user1)
                    .circleCrop()
                    .into(profileImage)
            }

            // Update active status
            activeStatus.visibility = if (profile.isActive) View.VISIBLE else View.GONE

            // Ensure fields are not editable by default
            toggleEditMode(false)
        }
    }

    private fun loadUserDevices() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("devices")
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val devicesList = ArrayList<Device>()
                snapshot?.forEach { document ->
                    document.toObject(Device::class.java).let {
                        devicesList.add(it)
                    }
                }
                devicesAdapter.updateDevices(devicesList)
            }
    }

    private fun toggleEditMode(enabled: Boolean) {
        isEditMode = enabled
        binding.apply {
            // Update FAB icon
            editProfileFab.setImageResource(
                if (enabled) android.R.drawable.ic_menu_save
                else android.R.drawable.ic_menu_edit
            )

            // Make fields editable/non-editable (email blijft altijd non-editable)
            userName.isEnabled = enabled
            userPhone.isEnabled = enabled
            userLocation.isEnabled = enabled
            userEmail.isEnabled = false // Email blijft altijd non-editable

            // Update background for better visual feedback
            userName.setBackgroundResource(if (enabled) android.R.drawable.edit_text else android.R.color.transparent)
            userPhone.setBackgroundResource(if (enabled) android.R.drawable.edit_text else android.R.color.transparent)
            userLocation.setBackgroundResource(if (enabled) android.R.drawable.edit_text else android.R.color.transparent)
            userEmail.setBackgroundResource(android.R.color.transparent) // Email blijft altijd transparent
        }
    }

    private fun saveProfileChanges() {
        val userId = auth.currentUser?.uid ?: return
        val currentUser = auth.currentUser ?: return

        val updatedName = binding.userName.text.toString().trim()
        val updatedPhone = binding.userPhone.text.toString().trim()
        val updatedLocation = binding.userLocation.text.toString().trim()
        val currentEmail = currentUser.email ?: ""

        // Validatie
        if (updatedName.isEmpty()) {
            showToast("Naam is verplicht.")
            return
        }

        // Update gebruikersgegevens
        updateUserData(userId, updatedName, currentEmail, updatedPhone, updatedLocation)
    }

    private fun updateUserData(userId: String, name: String, email: String, phone: String, location: String) {
        val nameParts = name.split(" ", limit = 2)
        val locationParts = location.split(",", limit = 2)

        val updates = hashMapOf(
            "firstName" to (nameParts.getOrNull(0)?.trim() ?: ""),
            "lastName" to (nameParts.getOrNull(1)?.trim() ?: ""),
            "phoneNumber" to phone,
            "city" to (locationParts.getOrNull(0)?.trim() ?: ""),
            "postalCode" to (locationParts.getOrNull(1)?.trim() ?: "")
        )

        firestore.collection("users").document(userId)
            .update(updates as Map<String, Any>)
            .addOnSuccessListener {
                toggleEditMode(false)
                setupProfileData()
                showToast("Profiel succesvol bijgewerkt!")
            }
            .addOnFailureListener { e ->
                showToast("Fout bij opslaan in database: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
