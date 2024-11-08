package com.masdika.elcuaca

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NotifyNetworkConnection(context: Context) {

    // Handling connection
    // Getting internet connection result
    /* In Case Of
    * App opened with internet and then disconnected
    * App opened without internet
    * */
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun startNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkConnection", "Connected to internet.")
            }

            override fun onLost(network: Network) {
                Log.d("NetworkConnection", "No internet connection.")
            }
        })
    }

    fun stopNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(ConnectivityManager.NetworkCallback())
    }

}