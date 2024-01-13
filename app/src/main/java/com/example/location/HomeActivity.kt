package com.example.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.location.databinding.ActivityHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import android.widget.PopupMenu
import android.view.View
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Calendar
import android.app.DatePickerDialog
import java.util.Date
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color
import com.example.location.models.LocationData
import com.example.location.models.WeatherData


class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        mapView = binding.mapView
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setBuiltInZoomControls(false)

        // locate button
        val locateButton: ImageButton = binding.locateButton
        locateButton.setOnClickListener {
            requestLocation()
        }

        // Three dots button
        val dotsButton: ImageButton = binding.dotsButton
        dotsButton.setOnClickListener { showPopupMenu(dotsButton) }
    }



    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val currentLocation = GeoPoint(it.latitude, it.longitude)
                        mapView.controller.animateTo(currentLocation)
                        mapView.controller.setZoom(ZOOM_LEVEL)
                        val rotationGestureOverlay = RotationGestureOverlay(mapView)
                        rotationGestureOverlay.isEnabled
                        mapView.setMultiTouchControls(true)
                        mapView.overlays.add(rotationGestureOverlay)
                        addMarker(currentLocation)
                        showToast("User Location: ${it.latitude}, ${it.longitude}")

                        // Store location data in Firestore
                        val user = FirebaseAuth.getInstance().currentUser
                        user?.let { firebaseUser ->
                            val firestore = FirebaseFirestore.getInstance()
                            val locationData = LocationData(it.latitude, it.longitude, System.currentTimeMillis())

                            firestore.collection("users")
                                .document(firebaseUser.uid)
                                .collection("locations")
                                .add(locationData)
                                .addOnSuccessListener {
                                    showToast("Location data added to Firestore")
                                }
                                .addOnFailureListener { e ->
                                    showToast("Failed to add location data to Firestore: ${e.message}")
                                }
                        } ?: showToast("Location not available")
                    }
                }
        }else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun addMarker(geoPoint: GeoPoint) {
        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.snippet = "Lat: ${geoPoint.latitude}, Lon: ${geoPoint.longitude}"

        marker.setOnMarkerClickListener { marker1, _ ->
            showCoordinates(marker1.position)
            true
        }

        mapView.overlays.add(marker)
    }

    private fun showCoordinates(geoPoint: GeoPoint) {
        val latitude = geoPoint.latitude
        val longitude = geoPoint.longitude
        Toast.makeText(this, "Latitude: $latitude\nLongitude: $longitude", Toast.LENGTH_SHORT).show()
    }


    private fun showLocationHistory() {
        val intent = Intent(this, HistoryActivity::class.java)
        startActivity(intent)
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.popup_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_history -> {
                    showLocationHistory()
                    true
                }
                R.id.menu_show_path -> {
                    showDatePicker()
                    true
                }
                R.id.menu_weather -> {
                    requestWeather()
                    true
                }
                R.id.menu_logout -> {
                    logOutUser()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun requestWeather() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val apiKey = "4d42c66c30afc0998661bc68329fa9eb"
                    val lat = it.latitude
                    val lon = it.longitude

                    val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey"

                    val request = Request.Builder().url(url).build()
                    val client = OkHttpClient()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            showToast("Failed to fetch weather data: ${e.message}")
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val responseBody = response.body?.string()
                            val weatherData = Gson().fromJson(responseBody, WeatherData::class.java)

                            runOnUiThread {
                                showWeatherDialog(weatherData)
                            }
                        }
                    })
                }
            }
    }

    private fun showWeatherDialog(weatherData: WeatherData) {
        val tempInCelsius = weatherData.main.temp - 273.15

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Current Weather")
            .setMessage(
                         "Location: ${weatherData.name}\n" +
                        "Temperature: ${String.format("%.2f", tempInCelsius)} °C\n" +
                        "Humidity: ${weatherData.main.humidity}%\n" +
                        "Weather: ${weatherData.weather[0].description}\n"
            )
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.alertdialog)
        dialog.show()

    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            {  _, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                showPathForDate(selectedDate.time)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun showPathForDate(selectedDate: Date) {
        val startOfDay = Calendar.getInstance()
        startOfDay.time = selectedDate
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        val startTimeStamp = startOfDay.timeInMillis

        val endOfDay = Calendar.getInstance()
        endOfDay.time = selectedDate
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(Calendar.MINUTE, 59)
        endOfDay.set(Calendar.SECOND, 59)
        val endTimeStamp = endOfDay.timeInMillis

        val user = FirebaseAuth.getInstance().currentUser
        user?.let { firebaseUser ->
            val firestore = FirebaseFirestore.getInstance()
            val locationsCollection = firestore.collection("users")
                .document(firebaseUser.uid)
                .collection("locations")

            locationsCollection
                .whereGreaterThanOrEqualTo("timestamp", startTimeStamp)
                .whereLessThanOrEqualTo("timestamp", endTimeStamp)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val locationHistory = mutableListOf<GeoPoint>()

                    for (documentSnapshot in querySnapshot.documents) {
                        val locationData = documentSnapshot.toObject(LocationData::class.java)
                        locationData?.let {
                            val geoPoint = GeoPoint(it.latitude, it.longitude)
                            locationHistory.add(geoPoint)
                        }
                    }

                    drawPathOnMap(locationHistory)
                }
                .addOnFailureListener { e ->
                    showToast("Failed to retrieve location history from Firestore: ${e.message}")
                }
        } ?: showToast("User not authenticated")
    }



    private fun drawPathOnMap(locationHistory: List<GeoPoint>) {
        mapView.overlays.clear()

        if (locationHistory.isEmpty()) {
            showToast("No data available for the selected date")
        } else {

            for (i in 1 until locationHistory.size) {
                val start = locationHistory[i - 1]
                val end = locationHistory[i]
                mapView.overlays.add(createConnectingDot(start))
                mapView.overlays.add(createPolyline(start, end))
            }


            mapView.overlays.add(createConnectingDot(locationHistory.last()))


            mapView.invalidate()
        }
    }

    private fun createConnectingDot(geoPoint: GeoPoint): Marker {
        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.icon = resources.getDrawable(R.drawable.baseline_brightness_1_24, theme)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        return marker
    }

    private fun createPolyline(start: GeoPoint, end: GeoPoint): Polyline {
        val line = Polyline()
        line.addPoint(start)
        line.addPoint(end)
        line.color = Color.BLUE
        line.width = 10.0f
        return line
    }

    private fun logOutUser() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val ZOOM_LEVEL=13.4
    }
}