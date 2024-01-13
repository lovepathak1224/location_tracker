package com.example.location.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.location.models.LocationData
import com.example.location.databinding.ItemLocationHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class LocationHistoryAdapter(var locationHistory: List<LocationData>) :
    RecyclerView.Adapter<LocationHistoryAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val locationData = locationHistory[position]
        holder.bind(locationData)
    }

    override fun getItemCount(): Int {
        return locationHistory.size
    }

    inner class ViewHolder(private val binding: ItemLocationHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(locationData: LocationData) {
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(locationData.timestamp))
            val formattedTime = timeFormat.format(Date(locationData.timestamp))

            val coordinatesText = "Latitude: ${locationData.latitude}, Longitude: ${locationData.longitude}"
            binding.coordinatesTextView.text = coordinatesText
            binding.dateTimeTextView.text = "Date: $formattedDate\nTime: $formattedTime"
        }
    }
}
