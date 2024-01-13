package com.example.location

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.location.adapter.LocationHistoryAdapter
import com.example.location.databinding.ActivityHistoryBinding
import com.example.location.models.LocationData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyAdapter: LocationHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val historyRecyclerView = binding.historyRecyclerView
        historyAdapter = LocationHistoryAdapter(mutableListOf())
        historyRecyclerView.adapter = historyAdapter
        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        showLocationHistory()

        binding.back.setOnClickListener {
            onBackPressed()
        }
    }

    private fun showLocationHistory() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { firebaseUser ->
            val firestore = FirebaseFirestore.getInstance()
            val locationsCollection = firestore.collection("users")
                .document(firebaseUser.uid)
                .collection("locations")

            locationsCollection.get()
                .addOnSuccessListener { querySnapshot ->
                    val locationHistory = mutableListOf<LocationData>()
                    for (documentSnapshot in querySnapshot.documents) {
                        val locationData = documentSnapshot.toObject(LocationData::class.java)
                        locationData?.let {
                            locationHistory.add(it)
                        }
                    }
                    locationHistory.sortByDescending { it.timestamp }
                    historyAdapter.locationHistory = locationHistory
                    historyAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    showToast("Failed to retrieve location history from Firestore: ${e.message}")
                }
        } ?: showToast("User not authenticated")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
