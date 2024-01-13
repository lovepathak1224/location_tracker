package com.example.location.models

data class WeatherData(
    val coord: Coord,
    val weather: List<Weather>,
    val base: String,
    val main: Main,
    val visibility: Int,
    val wind: Wind,
    val clouds: Clouds,
    val dt: Long,
    val sys: Sys,
    val timezone: Int,
    val id: Long,
    val name: String,
    val cod: Int
)

data class Coord(
    val lon: Double,
    val lat: Double
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Main(
    val temp: Double,
    val feelsLike: Double,
    val tempMin: Double,
    val tempMax: Double,
    val pressure: Int,
    val humidity: Int,
    val seaLevel: Int,
    val grndLevel: Int
)

data class Wind(
    val speed: Double,
    val deg: Int,
    val gust: Double
)

data class Clouds(
    val all: Int
)

data class Sys(
    val country: String,
    val sunrise: Long,
    val sunset: Long
)


