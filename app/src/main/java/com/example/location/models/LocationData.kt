package com.example.location.models

import android.os.Parcel
import android.os.Parcelable

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocationData> {
        override fun createFromParcel(parcel: Parcel): LocationData {
            return LocationData(parcel)
        }

        override fun newArray(size: Int): Array<LocationData?> {
            return arrayOfNulls(size)
        }
    }
}

