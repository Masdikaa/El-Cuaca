package com.masdika.elcuaca

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Rect
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.masdika.elcuaca.databinding.ActivityMainBinding
import com.masdika.elcuaca.weathermodel.WeatherData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var API_KEY: String = BuildConfig.API_KEY

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var geoPointUser: String
    private lateinit var currentDate: String
    private lateinit var address: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val searchInput = binding.outlinedTextField
        textInputCustomization(searchInput)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocationAndDisplayData()
    }

    // Main Thread
    private fun fetchLocationAndDisplayData() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("fetchLocationAndDisplayData", " Running - ${Thread.currentThread().name} - ${Date()}}")
                val location = withContext(Dispatchers.IO) { getCurrentLocation() }
                if (location == null) {
                    Log.e("fetchLocationAndDisplayData", "Location not available")
                    return@launch
                }
                geoPointUser = "${location.latitude}, ${location.longitude}"

                // Asynchronous
                val addressDeferred = async(Dispatchers.IO) { getAddressFromLocation(location) }
                val dateDeferred = async(Dispatchers.IO) { getFormattedDate() }
                val jsonResponseDeferred = async(Dispatchers.IO) { fetchWeatherData(geoPointUser, API_KEY) }

                address = addressDeferred.await()
                currentDate = dateDeferred.await()
                val jsonResponse: String? = jsonResponseDeferred.await()
                if (jsonResponse == null) {
                    Log.e("fetchLocationAndDisplayData", "Failed to fetch weather data")
                    return@launch
                }

                val weatherData = parseWeatherData(jsonResponse.toString())

                /*
                * Get JSON data example
                * val temperature = weatherData!!.data.values.temperature
                */

                binding.locationTv.text = address
                binding.dateTv.text = currentDate
                binding.tvTest.text = weatherData.toString()

                binding.indicatorProgress.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
                val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in_up)
                binding.contentLayout.startAnimation(animation)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("fetchLocationAndDisplayData", e.message.toString())
            } finally {
                Log.d("fetchLocationAndDisplayData", " Finish - ${Thread.currentThread().name} - ${Date()}}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        var isResumed = false

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

    private fun getAddressFromLocation(location: Location?): String {
        return if (location != null) {
            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            addresses?.firstOrNull()?.let { address ->
                "${address.subLocality}, ${address.subAdminArea}"
            } ?: "Address not found"
        } else {
            "Location not available"
        }
    }

    private fun getFormattedDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)
        return dateFormat.format(calendar.time)
    }

//    private fun fetchWeatherData(geoPoint: String, key: String): String {
//        val client = OkHttpClient()
//        var stringResponse = ""
//
//        val request = Request.Builder()
//            .url("https://api.tomorrow.io/v4/weather/realtime?location=$geoPoint&apikey=$key")
//            .get()
//            .addHeader("accept", "application/json")
//            .build()
//
//        try {
//            val response = client.newCall(request).execute()
//            if (response.isSuccessful) {
//                stringResponse = response.body?.string() ?: "Error: Empty response"
//            } else {
//                stringResponse = response.message
//            }
//        } catch (e: IOException) {
//            Log.d("APIResponse", "${e.message}")
//        }
//
//        return stringResponse
//    }

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
                stringResponse = if (response.isSuccessful) {
                    response.body?.string() ?: "Error: Empty response"
                } else {
                    "Error: ${response.message}"
                }
            }
        } catch (e: IOException) {
            Log.d("APIResponse", "${e.message}")
        }

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

    // Custom styling
    private fun textInputCustomization(textInput: TextInputLayout) {
        textInput.editText?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            textInput.startIconDrawable?.setColorFilter(
                if (hasFocus) colorPrimary else colorOutline,
                PorterDuff.Mode.SRC_IN
            )
            if (hasFocus) {
                Log.d("MainActivity", "TextInputLayout is focused")
            } else {
                Log.d("MainActivity", "TextInputLayout is lost focused")
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private val colorPrimary by lazy {
        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        typedValue.data
    }

    private val colorOutline by lazy {
        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValue, true)
        typedValue.data
    }

}