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
import com.masdika.elcuaca.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

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

        //Variables
        val API_KEY = BuildConfig.API_KEY //TomorrowIO API KEY
        val searchInput = binding.outlinedTextField

        textInputCustomization(searchInput)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocationAndDisplayData()
    }

    private fun fetchLocationAndDisplayData() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val location = withContext(Dispatchers.IO) { getCurrentLocation() }

                val addressDeferred = async(Dispatchers.IO) { getAddressFromLocation(location) }
                val dateDeferred = async(Dispatchers.IO) { getFormattedDate() }

                val address = addressDeferred.await()
                val date = dateDeferred.await()

                binding.locationTv.text = address
                binding.dateTv.text = date

                binding.indicatorProgress.visibility = View.GONE // Hide ProgressBar
                binding.contentLayout.visibility = View.VISIBLE
                val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.fade_in_up)
                binding.contentLayout.startAnimation(animation)

            } catch (e: Exception) {
                e.printStackTrace()
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

}