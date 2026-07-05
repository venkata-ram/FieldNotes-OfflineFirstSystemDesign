package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val context = LocalContext.current.applicationContext
    val connectivityObserver = remember(context) {
        AppContainer.connectivityObserver(context)
    }
    val isOnline by connectivityObserver.isOnline.collectAsStateWithLifecycle(initialValue = true)

    FieldNotesScreen(
        uiState = uiState.value,
        isOnline = isOnline,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun notesViewModel(): NotesViewModel {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        AppContainer.notesRepository(context)
    }
    val syncScheduler = remember(context) {
        AppContainer.notesSyncScheduler(context)
    }

    return viewModel(
        factory = NotesViewModelFactory(
            notesRepository = repository,
            scheduleBackgroundSync = syncScheduler::enqueueOneTimeSync,
        ),
    )
}

private class NotesViewModelFactory(
    private val notesRepository: NotesRepository,
    private val scheduleBackgroundSync: () -> Unit,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            return NotesViewModel(
                notesRepository = notesRepository,
                scheduleBackgroundSync = scheduleBackgroundSync,
            ) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
