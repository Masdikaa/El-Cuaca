package com.masdika.elcuaca.model

data class WeatherData(
    val data: Data?,
    val location: Location?
)

data class Data(
    val time: String?,
    val values: Values
)

data class Values(
    val cloudBase: Double?,
    val cloudCeiling: Double?,
    val cloudCover: Double?,
    val dewPoint: Double?,
    val freezingRainIntensity: Double?,
    val humidity: Double?,
    val precipitationProbability: Double?,
    val pressureSurfaceLevel: Double?,
    val rainIntensity: Double?,
    val sleetIntensity: Double?,
    val snowIntensity: Double?,
    val temperature: Double?,
    val temperatureApparent: Double?,
    val uvHealthConcern: Double?,
    val uvIndex: Double?,
    val visibility: Double?,
    val weatherCode: Int?,
    val windDirection: Double?,
    val windGust: Double?,
    val windSpeed: Double?
)

data class Location(
    val lat: Double?,
    val lon: Double?
)