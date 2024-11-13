package com.masdika.elcuaca.view

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.masdika.elcuaca.R
import com.masdika.elcuaca.databinding.ActivityMainBinding
import com.masdika.elcuaca.databinding.SnackbarNetworkDisconnectedBinding
import com.masdika.elcuaca.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initializeUI(binding.outlinedTextField)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkCallback", "On Available")
            }

            override fun onLost(network: Network) {
                Log.d("NetworkCallback", "On Lost")
                showCustomSnackBar(binding.snackbarLayout)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        fetchDataUI()
    }

    // ========================= UI Conf ================================================
    private fun initializeUI(inputLayout: TextInputLayout) {
        binding.indicatorProgress.visibility = View.VISIBLE
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

    private fun updateUI(bundle: Bundle) {
        binding.indicatorProgress.visibility = View.GONE
        val fragment = ContentFragment()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().add(R.id.frame_layout, fragment)
            .commit()
    }

    private fun fetchDataUI() {
        val bundle = Bundle()
        viewModel.userLiveData.observe(this) { userData ->
            bundle.putString("address", userData.address)
            bundle.putString("currentDate", userData.currentDate)
        }

        viewModel.weatherLiveData.observe(this) { weatherData ->
            weatherData?.let {
                bundle.putDouble("temperature", weatherData.data!!.values.temperature!!)
                bundle.putInt("weatherCode", weatherData.data.values.weatherCode!!)
                updateUI(bundle)
            } ?: run {
                // Handle error
                Log.d("MainActivity-weatherLiveData", "Empty Data")
            }
        }
        viewModel.fetchData()
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
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            Log.e("NetworkCallback", "Unregistered Callback.")
        }
    }

} //MainActivity