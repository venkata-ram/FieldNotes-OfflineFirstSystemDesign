package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FieldNotesRoute(
    viewModel: NotesViewModel = viewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    FieldNotesScreen(
        uiState = uiState.value,
        onEvent = viewModel::onEvent,
    )
}
