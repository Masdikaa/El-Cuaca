package com.masdika.elcuaca

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Rect
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.*
import com.masdika.elcuaca.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private lateinit var userAddress: String
    private lateinit var dayDate: String

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
        dayDate = getFormattedDate()
        binding.dateTv.text = dayDate

        val searchInput = binding.outlinedTextField
        searchInput.editText?.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            searchInput.startIconDrawable?.setColorFilter(
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

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                val geocoder = Geocoder(this@MainActivity)
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (addresses != null) {
                    val address = addresses[0]
                    val locationText = "${address.subLocality}, ${address.subAdminArea}"
                    userAddress = locationText
                    binding.locationTv.text = userAddress
                    // val locationText = "${address.subLocality}, ${address.locality}, ${address.countryName}"
                    // binding.etSearchLocation.setText(locationText)
                }
                fusedLocationClient.removeLocationUpdates(this)
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

    private fun getFormattedDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)
        return dateFormat.format(calendar.time)
    }

}