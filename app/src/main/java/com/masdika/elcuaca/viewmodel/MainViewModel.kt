package com.masdika.elcuaca.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.masdika.elcuaca.BuildConfig
import com.masdika.elcuaca.model.UserDataModel
import com.masdika.elcuaca.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()
    private var key: String = BuildConfig.API_KEY

    // Data that can be subscribed by View Class
    val userLiveData = MutableLiveData<UserDataModel>()
    val weatherLiveData = MutableLiveData<WeatherData?>()

    fun fetchData() {
        viewModelScope.launch {
            try {
                val location = withContext(Dispatchers.IO) { getCurrentLocation() }

                location?.let {
                    val latitude = it.latitude.toString()
                    val longitude = it.longitude.toString()

                    val addressDeferred = async(Dispatchers.IO) { getAddress(it) }
                    val dateDeferred = async(Dispatchers.IO) { getDate() }
                    val weatherDataDeferred = async(Dispatchers.IO) {
                        fetchWeatherData("${latitude},${longitude}", key)
                    }

                    val jsonWeatherResponse = weatherDataDeferred.await()
                    if (jsonWeatherResponse != null) {
                        val userDataModel = UserDataModel(latitude, longitude, addressDeferred.await(), dateDeferred.await())
                        val weatherDataModel = parseWeatherData(jsonWeatherResponse)

                        userLiveData.postValue(userDataModel)
                        weatherLiveData.postValue(weatherDataModel)

                    } else {
                        Log.e("MainViewModel", "Failed to fetch weather data")
                        // #TODO
                        //  Handling for null response
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("MainViewModel", "Error fetching user data: ${e.message}")
            } finally {
                Log.d("MainViewModel", "Finish - ${Thread.currentThread().name} | ${Date()}")
            }

        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        var isResumed = false
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null && !isResumed) {
                    isResumed = true
                    cont.resume(location) { _ ->
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                } else if (!isResumed) {
                    isResumed = true
                    cont.resume(null) { _ ->
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        cont.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }


    private suspend fun getAddress(location: Location?): String {
        return withContext(Dispatchers.IO) {
            if (location != null) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    addresses?.firstOrNull()?.let { address ->
                        "${address.subLocality}, ${address.subAdminArea}"
                    } ?: "Address not found"
                } catch (e: IOException) {
                    Log.e("getAddress", "Geocoder error: ${e.message}")
                    "Address not found"
                }
            } else {
                "Location not available"
            }
        }
    }

    private fun getDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)
        return dateFormat.format(calendar.time)
    }

    private suspend fun fetchWeatherData(geoPoint: String, key: String): String? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        var stringResponse: String? = null

        val request = Request.Builder()
            .url("https://api.tomorrow.io/v4/weather/realtime?location=$geoPoint&apikey=$key")
            .get()
            .addHeader("accept", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            response.use {
                val responseBody = response.body?.string()
                stringResponse = if (!responseBody.isNullOrEmpty()) {
                    responseBody
                } else {
                    "Error: Response body is empty"
                }

                if (!response.isSuccessful) {
                    Log.e("APIResponse", "Error : ${response.code} - ${response.message}")
                    stringResponse = "Error: ${response.message}"
                }

            }
        } catch (e: IOException) {
            Log.e("fetchWeatherData", "Network error: ${e.message}")
        }
        Log.d("fetchWeatherData", stringResponse.toString())
        stringResponse
    }

    private fun parseWeatherData(json: String): WeatherData? {
        return try {
            val gson = Gson()
            gson.fromJson(json, WeatherData::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}