package com.ap.neighborrentapplication.ui.activity

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.ap.neighborrentapplication.adapter.DevicesAdapter
import com.ap.neighborrentapplication.adapter.ProfileAdapter
import com.ap.neighborrentapplication.databinding.ActivityProfileBinding
import com.ap.neighborrentapplication.models.Device
import com.ap.neighborrentapplication.models.Profile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide

class ProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var profileAdapter: ProfileAdapter
    private lateinit var devicesAdapter: DevicesAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

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
            // Start EditProfileActivity (to be implemented)
            //startActivity(Intent(this, EditProfileActivity::class.java))
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
        binding.apply {
            userName.text = profile.name
            userEmail.text = profile.email
            userPhone.text = profile.phoneNumber
            userLocation.text = profile.location
            itemsRentedCount.text = profile.itemsRented.toString()
            averageRating.text = profile.rating.toString()
            responseRatio.text = "${profile.responseRatio}%"

            // Laad profielfoto
            Glide.with(this@ProfileActivity)
                .load(profile.profileImageUrl)
                .circleCrop()
                .into(profileImage)

            // Update active status
            activeStatus.visibility = if (profile.isActive) View.VISIBLE else View.GONE
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
}
