package com.venkatsvision.offlinefirstsystemdesign

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.venkatsvision.offlinefirstsystemdesign.data.connectivity.ConnectivityObserver
import com.venkatsvision.offlinefirstsystemdesign.ui.notes.FieldNotesRoute
import com.venkatsvision.offlinefirstsystemdesign.ui.theme.OfflineFirstSystemDesignTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var connectivityObserver: ConnectivityObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OfflineFirstSystemDesignTheme {
                FieldNotesRoute(connectivityObserver = connectivityObserver)
            }
        }
    }
}
