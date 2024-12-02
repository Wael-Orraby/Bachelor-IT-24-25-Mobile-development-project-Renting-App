package com.ap.neighborrentapplication.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ap.neighborrentapplication.R
import com.ap.neighborrentapplication.models.Device
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapSearchActivity : BaseActivity() {
    private lateinit var mapView: MapView
    private lateinit var searchEditText: EditText
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore
    private val markers = mutableListOf<Marker>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1
        private const val DEFAULT_ZOOM = 15.0
        private const val SEARCH_RADIUS_KM = 5.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_search)
        setupToolbar("Zoeken op kaart", true)

        Configuration.getInstance().userAgentValue = applicationContext.packageName

        org.osmdroid.config.Configuration.getInstance().load(
            applicationContext,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )

        initializeViews()
        setupRadiusSpinner()
        setupMap()
        setupLocationServices()
        setupSearch()
    }

    private fun initializeViews() {
        mapView = findViewById(R.id.mapView)
        searchEditText = findViewById(R.id.editTextText)
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.controller.setZoom(DEFAULT_ZOOM)
        mapView.setMultiTouchControls(true)

        // Set default location to Antwerp
        val antwerpLocation = GeoPoint(51.2194, 4.4025)
        mapView.controller.setCenter(antwerpLocation)
    }

    private fun setupSearch() {
        searchEditText.setOnEditorActionListener { _, _, _ ->
            performSearch(searchEditText.text.toString())
            true
        }
    }

    private fun performSearch(query: String) {
        // Check if input is a Belgian postal code (4 digits)
        if (query.matches(Regex("^\\d{4}$"))) {
            // Search directly in Firestore for devices with matching postal code
            searchDevicesByPostalCode(query)
        } else {
            // Use geocoding for other searches
            searchByGeocoding(query)
        }
    }

    private fun searchDevicesByPostalCode(postalCode: String) {
        firestore.collection("devices")
            .whereEqualTo("postalCode", postalCode)
            .get()
            .addOnSuccessListener { documents ->
                clearMarkers()
                if (documents.isEmpty) {
                    Toast.makeText(this, "Geen toestellen gevonden in deze postcode", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                // Get the first device's location to center the map
                val firstDevice = documents.first().toObject(Device::class.java)
                val location = getDeviceLocation(firstDevice)
                mapView.controller.animateTo(location)
                
                // Add markers for all devices
                for (document in documents) {
                    val device = document.toObject(Device::class.java)
                    val deviceLocation = getDeviceLocation(device)
                    addMarkerForDevice(device, deviceLocation)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Fout bij zoeken: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchByGeocoding(query: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                val searchPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(searchPoint)
                searchDevicesNearLocation(searchPoint)
            } else {
                Toast.makeText(this, "Locatie niet gevonden", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Fout bij zoeken locatie: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchDevicesNearLocation(location: GeoPoint, searchRadius: Double = SEARCH_RADIUS_KM) {
        firestore.collection("devices")
            .get()
            .addOnSuccessListener { documents ->
                clearMarkers()
                
                for (document in documents) {
                    val device = document.toObject(Device::class.java)
                    val deviceLocation = getDeviceLocation(device)
                    
                    if (isWithinRadius(location, deviceLocation, searchRadius)) {
                        addMarkerForDevice(device, deviceLocation)
                    }
                }
            }
    }

    private fun getDeviceLocation(device: Device): GeoPoint {
        // Use Geocoder to get coordinates from device's city and postal code
        val geocoder = Geocoder(this, Locale.getDefault())
        val address = "${device.postalCode} ${device.city}"
        
        return try {
            val locations = geocoder.getFromLocationName(address, 1)
            if (!locations.isNullOrEmpty()) {
                GeoPoint(locations[0].latitude, locations[0].longitude)
            } else {
                GeoPoint(51.2194, 4.4025) // Default to Antwerp
            }
        } catch (e: Exception) {
            GeoPoint(51.2194, 4.4025)
        }
    }

    private fun addMarkerForDevice(device: Device, location: GeoPoint) {
        val marker = Marker(mapView).apply {
            position = location
            title = device.name
            snippet = "${device.pricePerDay}€ per dag"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        
        markers.add(marker)
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    private fun clearMarkers() {
        markers.forEach { mapView.overlays.remove(it) }
        markers.clear()
        mapView.invalidate()
    }

    private fun isWithinRadius(center: GeoPoint, point: GeoPoint, radiusKm: Double): Boolean {
        val distance = calculateDistance(
            center.latitude, center.longitude,
            point.latitude, point.longitude
        )
        return distance <= radiusKm
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private fun setupLocationServices() {
        // Check locatie permissies
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        // Initialiseer locatie services
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    val currentLocation = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.animateTo(currentLocation)
                    searchDevicesNearLocation(currentLocation)
                }
            }
            .addOnFailureListener { e ->
                // Fallback naar default locatie (Antwerpen)
                val defaultLocation = GeoPoint(51.2194, 4.4025)
                mapView.controller.animateTo(defaultLocation)
            }
    }

    private fun setupRadiusSpinner() {
        val radiusSpinner = findViewById<Spinner>(R.id.radiusSpinner)
        val radiusOptions = arrayOf("5 km", "10 km", "20 km", "50 km")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, radiusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        radiusSpinner.adapter = adapter
        
        radiusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val radius = when(position) {
                    0 -> 5.0
                    1 -> 10.0
                    2 -> 20.0
                    3 -> 50.0
                    else -> 5.0
                }
                // Update de zoekradius met de huidige kaartlocatie
                val currentLocation = mapView.mapCenter as GeoPoint
                searchDevicesNearLocation(currentLocation, radius)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
} 