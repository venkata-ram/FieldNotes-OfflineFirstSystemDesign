package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venkatsvision.offlinefirstsystemdesign.data.connectivity.ConnectivityObserver

@Composable
fun FieldNotesRoute(
    connectivityObserver: ConnectivityObserver,
    viewModel: NotesViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by connectivityObserver.isOnline.collectAsStateWithLifecycle(initialValue = true)

    FieldNotesScreen(
        uiState = uiState.value,
        isOnline = isOnline,
        onEvent = viewModel::onEvent,
    )
}
