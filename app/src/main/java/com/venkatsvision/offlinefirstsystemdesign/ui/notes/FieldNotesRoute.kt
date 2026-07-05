package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.venkatsvision.offlinefirstsystemdesign.data.AppContainer
import com.venkatsvision.offlinefirstsystemdesign.domain.NotesRepository

@Composable
fun FieldNotesRoute(
    viewModel: NotesViewModel = notesViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    FieldNotesScreen(
        uiState = uiState.value,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun notesViewModel(): NotesViewModel {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        AppContainer.notesRepository(context)
    }

    return viewModel(
        factory = NotesViewModelFactory(repository),
    )
}

private class NotesViewModelFactory(
    private val notesRepository: NotesRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            return NotesViewModel(notesRepository) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
