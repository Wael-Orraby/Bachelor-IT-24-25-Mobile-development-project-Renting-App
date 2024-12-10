package com.ap.neighborrentapplication.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
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
import org.osmdroid.views.overlay.Polygon
import java.util.Locale

class MapSearchActivity : BaseActivity() {
    private lateinit var mapView: MapView
    private lateinit var searchEditText: EditText
    private lateinit var radiusSeekBar: SeekBar
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore
    private lateinit var radiusTextView: TextView
    private var currentCircle: Polygon? = null

    private val markers = mutableListOf<Marker>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1
        private const val DEFAULT_ZOOM = 15.0
        private const val MAX_RADIUS_KM = 50.0
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
        setupMap()
        setupLocationServices()
        setupSearch()
        setupRadiusSeekBar()

        // Set default location to Antwerp and perform search
        val antwerpLocation = GeoPoint(51.2194, 4.4025)
        searchDevicesNearLocation(antwerpLocation, radiusSeekBar.progress.toDouble())
    }
    private fun setupLocationServices() {
        // Controleer locatiepermissies
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
                    searchDevicesNearLocation(currentLocation, radiusSeekBar.progress.toDouble())
                }
            }
            .addOnFailureListener { e ->
                // Fallback naar een standaardlocatie (bijvoorbeeld Antwerpen)
                val defaultLocation = GeoPoint(51.2194, 4.4025)
                mapView.controller.animateTo(defaultLocation)
                Toast.makeText(this, "Locatie niet beschikbaar. Standaardlocatie ingesteld.", Toast.LENGTH_SHORT).show()
            }
    }



    private fun initializeViews() {
        mapView = findViewById(R.id.mapView)
        searchEditText = findViewById(R.id.editTextText)
        radiusSeekBar = findViewById(R.id.radiusSeekBar)
        radiusTextView = findViewById(R.id.radiusTextView)
        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.controller.setZoom(DEFAULT_ZOOM)
        mapView.setMultiTouchControls(true)

        val antwerpLocation = GeoPoint(51.2194, 4.4025)
        mapView.controller.setCenter(antwerpLocation)
    }

    private fun setupSearch() {
        searchEditText.setOnEditorActionListener { _, _, _ ->
            performSearch(searchEditText.text.toString())
            true
        }
    }

   private fun setupRadiusSeekBar() {
       radiusSeekBar.max = 50 // Maximum 50 km
       radiusSeekBar.progress = 5 // Default value 5 km
       updateRadiusTextView(radiusSeekBar.progress)

       radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
           override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
               if (progress == 0) return // Skip invalid radius
               updateRadiusTextView(progress)
               val currentLocation = mapView.mapCenter as GeoPoint
               performSearch(searchEditText.text.toString(), progress.toDouble())
           }

           override fun onStartTrackingTouch(seekBar: SeekBar?) {}
           override fun onStopTrackingTouch(seekBar: SeekBar?) {}
       })
   }
    private fun updateRadiusTextView(radius: Int) {
        radiusTextView.text = "Radius: $radius km"
    }



    private fun performSearch(query: String, radius: Double = radiusSeekBar.progress.toDouble()) {
        clearMarkers()
        mapView.overlays.clear()
        if (query.isEmpty()) {
            val antwerpLocation = GeoPoint(51.2194, 4.4025)
            searchDevicesNearLocation(antwerpLocation, radius)
        } else if (query.matches(Regex("^\\d{4}$"))) {
            searchDevicesByPostalCode(query, radius)
        } else {
            searchByGeocoding(query, radius)
        }
    }

    private fun searchDevicesByPostalCode(postalCode: String, radius: Double) {
        firestore.collection("devices")
            .whereEqualTo("postalCode", postalCode)
            .get()
            .addOnSuccessListener { documents ->
                clearMarkers()
                if (documents.isEmpty) {
                    Toast.makeText(this, "Geen toestellen gevonden in deze postcode", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val firstDevice = documents.first().toObject(Device::class.java)
                val location = getDeviceLocation(firstDevice)
                val searchPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(searchPoint)
                searchDevicesNearLocation(searchPoint, radius)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Fout bij zoeken: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchByGeocoding(query: String, radius: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                val searchPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(searchPoint)
                searchDevicesNearLocation(searchPoint, radius)
            } else {
                Toast.makeText(this, "Locatie niet gevonden", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Ongeldige locatie: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Fout bij zoeken locatie: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchDevicesNearLocation(location: GeoPoint, searchRadius: Double) {
        adjustMapZoomForRadius(location, searchRadius)
        drawCircle(location, searchRadius)
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
            .addOnFailureListener { e ->
                Toast.makeText(this, "Fout bij ophalen van apparaten: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addMarkerForDevice(device: Device, location: GeoPoint) {
        val marker = Marker(mapView).apply {
            position = location
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Stel een aangepast marker-icoon in
            icon = BitmapDrawable(
                resources,
                createMarkerWithLabel(this@MapSearchActivity, "${device.name}\n${device.pricePerDay}€/dag")
            )

            // Kliklistener om naar DashboardActivity te navigeren met details van dit apparaat
            setOnMarkerClickListener { _, _ ->
                val intent = Intent(this@MapSearchActivity, DashboardActivity::class.java).apply {
                    putExtra("deviceId", device.id) // Stuur apparaat-ID mee
                }
                startActivity(intent)
                true
            }
        }

        // Voeg de marker toe aan de kaart
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    private fun createMarkerWithLabel(context: Context, labelText: String): Bitmap {
        val markerLayout = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val textView = TextView(context).apply {
            text = labelText
            setTextColor(Color.BLACK)
            textSize = 12f
            setBackgroundColor(Color.WHITE)
            setPadding(10, 5, 10, 5)
            elevation = 10f
        }
        val textLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = -16
            bottomMargin = 40
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
        markerLayout.addView(textView, textLayoutParams)

        val markerIcon = View(context).apply {
            setBackgroundResource(R.drawable.custom_marker)
        }
        val iconLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
        markerLayout.addView(markerIcon, iconLayoutParams)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        markerLayout.measure(widthSpec, heightSpec)
        markerLayout.layout(0, 0, markerLayout.measuredWidth, markerLayout.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            markerLayout.measuredWidth,
            markerLayout.measuredHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        markerLayout.draw(canvas)
        return bitmap
    }

    private fun adjustMapZoomForRadius(center: GeoPoint, radiusKm: Double) {
        val meters = radiusKm * 1000
        mapView.controller.setCenter(center)
        mapView.controller.setZoom(calculateZoomLevelForRadius(meters))
    }

    private fun calculateZoomLevelForRadius(radiusMeters: Double): Double {
        val scaleFactor = radiusMeters / 455
        return Math.log(40075000.0 / scaleFactor) / Math.log(2.0) - 8
    }

    private fun drawCircle(center: GeoPoint, radiusKm: Double) {
        currentCircle?.let {
            mapView.overlays.remove(it)
        }

        // Maak een nieuwe cirkel
        val circle = Polygon().apply {
            points = Polygon.pointsAsCircle(center, radiusKm * 1000.0) // Radius in meters
            fillColor = 0x12121212
            strokeColor = 0xFF0000FF.toInt()
            strokeWidth = 2f
        }

        currentCircle = circle
        mapView.overlays.add(circle)
        mapView.invalidate()
    }

    private fun getDeviceLocation(device: Device): GeoPoint {
        val geocoder = Geocoder(this, Locale.getDefault())
        val address = "${device.postalCode} ${device.city}"

        return try {
            val locations = geocoder.getFromLocationName(address, 1)
            if (!locations.isNullOrEmpty()) {
                GeoPoint(locations[0].latitude, locations[0].longitude)
            } else {
                GeoPoint(51.2194, 4.4025)
            }
        } catch (e: Exception) {
            GeoPoint(51.2194, 4.4025)
        }
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
        Log.d("MapSearchActivity", "Distance to point: $distance km, Radius: $radiusKm km")
        return distance <= radiusKm
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

}
