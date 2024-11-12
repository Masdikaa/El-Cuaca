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


/*

Weather Available Condition
1 Clear           -> 1000
2 Cloudy          -> 1001
3 Drizzle         -> 4000
4 Light Rain      -> 4200
5 Rain            -> 4001
6 Partly Cloud    -> 1101
7 Snow            -> 5000
8 Thunderstorm    -> 8000

*/