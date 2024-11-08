package com.masdika.elcuaca.model

data class WeatherData(
    val data: Data,
    val location: Location
)

data class Data(
    val time: String,
    val values: Values
)

data class Values(
    val cloudBase: Double?,
    val cloudCeiling: Double?,
    val cloudCover: Double,
    val dewPoint: Double,
    val freezingRainIntensity: Double,
    val humidity: Double,
    val precipitationProbability: Double,
    val pressureSurfaceLevel: Double,
    val rainIntensity: Double,
    val sleetIntensity: Double,
    val snowIntensity: Double,
    val temperature: Double,
    val temperatureApparent: Double,
    val uvHealthConcern: Double,
    val uvIndex: Double,
    val visibility: Double,
    val weatherCode: Int,
    val windDirection: Double,
    val windGust: Double,
    val windSpeed: Double
)

data class Location(
    val lat: Double,
    val lon: Double
)

//                          {
//                             "data": {
//                                      "time": "2024-11-05T15:25:00Z",
//                                      "values": {
//                                                  "cloudBase": 0.23,
//                                                  "cloudCeiling": null,
//                                                  "cloudCover": 38,
//                                                  "dewPoint": 22.31,
//                                                  "freezingRainIntensity": 0,
//                                                  "humidity": 90,
//                                                  "precipitationProbability": 0,
//                                                  "pressureSurfaceLevel": 1004.51,
//                                                  "rainIntensity": 0,
//                                                  "sleetIntensity": 0,
//                                                  "snowIntensity": 0,
//                                                  "temperature": 24.13,
//                                                  "temperatureApparent": 24.13,
//                                                  "uvHealthConcern": 0,
//                                                  "uvIndex": 0,
//                                                  "visibility": 11.94,
//                                                  "weatherCode": 1101,
//                                                  "windDirection": 102.63,
//                                                  "windGust": 0.5,
//                                                  "windSpeed": 0.19
//                                                 }
//                                     },
//                             "location": {
//                                 "lat": -7.8532301,
//                                 "lon": 111.5369704
//                              }
//                          }

