package com.venkatsvision.offlinefirstsystemdesign.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class AndroidConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
) : ConnectivityObserver {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override val isOnline: Flow<Boolean> =
        callbackFlow {
            trySend(connectivityManager.isCurrentlyOnline())

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(connectivityManager.isCurrentlyOnline())
                }

                override fun onLost(network: Network) {
                    trySend(connectivityManager.isCurrentlyOnline())
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    trySend(connectivityManager.isCurrentlyOnline())
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()

    private fun ConnectivityManager.isCurrentlyOnline(): Boolean {
        val network = activeNetwork ?: return false
        val capabilities = getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
