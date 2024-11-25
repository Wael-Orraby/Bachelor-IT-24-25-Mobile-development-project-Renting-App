package com.ap.neighborrentapplication.activity

import android.os.Bundle
import android.view.View
import android.widget.SearchView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ap.neighborrentapplication.R
import com.ap.neighborrentapplication.adapter.CategoryAdapter
import com.ap.neighborrentapplication.adapter.DevicesAdapter
import com.ap.neighborrentapplication.models.Category
import com.ap.neighborrentapplication.models.Device
import com.ap.neighborrentapplication.ui.activity.BaseActivity
import com.google.firebase.firestore.FirebaseFirestore

class CategorySearchActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var deviceRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var deviceAdapter: DevicesAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var noResultsText: TextView



    private val categories = listOf(
        Category("kitchen", "Keukenapparaten", R.drawable.keuken),
        Category("cleaning", "Schoonmaakapparaten", R.drawable.schoonmaakapparaten),
        Category("garden", "Tuinapparaten", R.drawable.tuinapparaten)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_search)
        setupToolbar("Categorieën",true);

        firestore = FirebaseFirestore.getInstance()
        setupViews()
        setupSearch()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.categoryRecyclerView)
        deviceRecyclerView = findViewById(R.id.deviceRecyclerView)
        searchView = findViewById(R.id.searchView)
        noResultsText = findViewById(R.id.noResultsText)

        // Setup categories
        recyclerView.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(categories) { category ->
            showDevicesForCategory(category)
        }
        recyclerView.adapter = categoryAdapter

        // Setup devices
        deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        deviceAdapter = DevicesAdapter(ArrayList(), this)
        deviceRecyclerView.adapter = deviceAdapter


    }

    private fun showDevicesForCategory(category: Category) {
        recyclerView.visibility = View.GONE
        deviceRecyclerView.visibility = View.VISIBLE

        firestore.collection("devices")
            .whereEqualTo("category", category.id)
            .get()
            .addOnSuccessListener { documents ->
                val devices = documents.mapNotNull { it.toObject(Device::class.java) }
                updateDevicesList(devices)
            }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchDevices(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun searchDevices(query: String) {
        recyclerView.visibility = View.GONE
        deviceRecyclerView.visibility = View.VISIBLE

        firestore.collection("devices")
            .whereGreaterThanOrEqualTo("name", query.toLowerCase())
            .whereLessThanOrEqualTo("name", query.toLowerCase() + '\uf8ff')
            .get()
            .addOnSuccessListener { documents ->
                val devices = documents.mapNotNull { it.toObject(Device::class.java) }
                updateDevicesList(devices)
            }
    }

    private fun updateDevicesList(devices: List<Device>) {
        if (devices.isEmpty()) {
            noResultsText.visibility = View.VISIBLE
            deviceRecyclerView.visibility = View.GONE
        } else {
            noResultsText.visibility = View.GONE
            deviceRecyclerView.visibility = View.VISIBLE
            deviceAdapter.updateDevices(ArrayList(devices))
        }
    }
}