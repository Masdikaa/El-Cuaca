package com.masdika.elcuaca

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.masdika.elcuaca.databinding.ActivityMainBinding
import com.masdika.elcuaca.databinding.SnackbarNetworkDisconnectedBinding
import com.masdika.elcuaca.model.WeatherData
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

    private lateinit var networkMonitor: NotifyNetworkConnection

    private var key: String = BuildConfig.API_KEY

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initializeUI(binding.outlinedTextField)

        networkMonitor = NotifyNetworkConnection(this)
        networkMonitor.startNetworkCallback()
        monitorNetworkChanges()

        //Main Thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d("MainThread", "Running - ${Thread.currentThread().name} | ${Date()}")
                val location = withContext(Dispatchers.IO) { getCurrentLocation() }
                fetchCurrentLocation(location)

                val addressDeferred = async(Dispatchers.IO) { getAddress(location) }
                val dateDeferred = async(Dispatchers.IO) { getDate() }
                val weatherDataDeferred = async(Dispatchers.IO) { fetchWeatherData(geoPointUser, key) }

                address = addressDeferred.await()
                currentDate = dateDeferred.await()
                val jsonWeatherResponse: String? = weatherDataDeferred.await()

                if (jsonWeatherResponse == null) {
                    Log.e("MainThread", "Failed to fetch weather data")
                    // #TODO UI Handling for null response ￼
                } else {
                    // Proceed responses
                    val weatherData = parseWeatherData(jsonWeatherResponse).toString()
                    updateUI(weatherData)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("MainThread", e.message.toString())
            } finally {
                Log.d("MainThread", "Finish - ${Thread.currentThread().name} | ${Date()}")
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

    private fun fetchCurrentLocation(location: Location?) {
        Log.d("fetchCurrentLocation", "Running - ${Thread.currentThread().name} | ${Date()}")
        if (location == null) {
            Log.e("fetchLocationAndDisplayData", "Location not available")
        } else {
            geoPointUser = "${location.latitude},${location.longitude}"
        }
    }

    private suspend fun getAddress(location: Location?): String {
        return withContext(Dispatchers.IO) {
            if (location != null) {
                try {
                    val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
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

    private fun monitorNetworkChanges() {
        networkMonitor.observe(this) { isConnected ->
            if (!isConnected) {
                showCustomSnackBar(binding.root)
            }
        }
    }

    // ========================= UI Conf ================================================
    private fun initializeUI(inputLayout: TextInputLayout) {
        binding.indicatorProgress.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE

        inputLayout.editText?.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            inputLayout.startIconDrawable?.setColorFilter(
                if (hasFocus) colorPrimary else colorOutline,
                PorterDuff.Mode.SRC_IN
            )
            if (hasFocus) {
                Log.d("initializeUI", "TextInputLayout is focused")
            } else {
                Log.d("initializeUI", "TextInputLayout is lost focused")
            }
        }
    }

    private fun updateUI(value: String) {
        binding.locationTv.text = address
        binding.dateTv.text = currentDate
        binding.tvTest.text = value

        binding.indicatorProgress.visibility = View.GONE
        binding.contentLayout.visibility = View.VISIBLE
        val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in_up)
        binding.contentLayout.startAnimation(animation)
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

    private fun Context.showCustomSnackBar(container: View?) {
        container?.let {
            val snackView = View.inflate(this, R.layout.snackbar_network_disconnected, null)
            val snackbarBinding = SnackbarNetworkDisconnectedBinding.bind(snackView)
            val snackBar = Snackbar.make(container, "", Snackbar.LENGTH_LONG)
            snackBar.view.setBackgroundColor(Color.TRANSPARENT)

            // Tambahkan fungsi untuk refresh button
            val refreshButton = snackbarBinding.tvRefresh
            snackBar.apply {
                (view as ViewGroup).addView(snackbarBinding.root)
                refreshButton.setOnClickListener {
                    snackBar.dismiss()
                    Toast.makeText(this@MainActivity, "Refresh", Toast.LENGTH_SHORT).show()
                }
                show()
            }
        }
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

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopNetworkCallback()
    }

} //MainActivity