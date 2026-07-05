package com.venkatsvision.offlinefirstsystemdesign.data.connectivity

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}
