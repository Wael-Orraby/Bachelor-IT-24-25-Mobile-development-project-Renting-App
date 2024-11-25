package com.ap.neighborrentapplication.ui.activity

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ap.neighborrentapplication.adapter.ProfileAdapter
import com.ap.neighborrentapplication.databinding.ActivityProfileBinding
import com.ap.neighborrentapplication.models.Device
import com.ap.neighborrentapplication.models.Profile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import android.widget.Toast
import com.ap.neighborrentapplication.R
import com.ap.neighborrentapplication.adapter.DevicesAdapter

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
        
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val profile = Profile(
                        id = userId,
                        name = "${document.getString("firstName")} ${document.getString("lastName")}",
                        email = document.getString("email") ?: "",
                        phoneNumber = document.getString("phoneNumber") ?: "",
                        location = "${document.getString("city")}, ${document.getString("postalCode")}",
                        profileImageUrl = document.getString("profileImageUrl") ?: "",
                        itemsRented = document.getLong("itemsRented")?.toInt() ?: 0,
                        rating = document.getDouble("rating") ?: 0.0,
                        responseRatio = document.getLong("responseRatio")?.toInt() ?: 0,
                        isActive = document.getBoolean("isActive") ?: true
                    )
                    
                    // Update UI met profile data
                    updateProfileUI(profile)
                }
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
