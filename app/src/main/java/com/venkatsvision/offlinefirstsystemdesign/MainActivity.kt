package com.venkatsvision.offlinefirstsystemdesign

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.venkatsvision.offlinefirstsystemdesign.ui.notes.FieldNotesRoute
import com.venkatsvision.offlinefirstsystemdesign.ui.theme.OfflineFirstSystemDesignTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OfflineFirstSystemDesignTheme {
                FieldNotesRoute()
            }
        }
    }
}
