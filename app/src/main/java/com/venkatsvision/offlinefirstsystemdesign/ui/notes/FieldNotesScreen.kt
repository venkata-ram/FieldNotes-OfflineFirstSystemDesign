package com.venkatsvision.offlinefirstsystemdesign.ui.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import com.venkatsvision.offlinefirstsystemdesign.ui.theme.OfflineFirstSystemDesignTheme

private enum class DemoScreen(
    val label: String,
    val title: String,
    val subtitle: String,
    val showInBottomBar: Boolean = true,
) {
    Notes("Notes", "Field Notes", "Capture and inspect local state"),
    Remote("Remote", "Remote API", "Edit the fake server copy"),
    Sync("Sync", "Sync Control", "Push, pull, conflict, retry"),
    Learn("Learn", "Architecture", "Offline-first system design"),
    Editor("Edit", "Note Editor", "Write locally first, sync when ready", showInBottomBar = false),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldNotesScreen(
    uiState: NotesUiState,
    isOnline: Boolean,
    onEvent: (NotesUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedScreen by rememberSaveable { mutableStateOf(DemoScreen.Notes) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedScreen.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = selectedScreen.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (selectedScreen.showInBottomBar) {
                NavigationBar {
                    DemoScreen.entries.filter { it.showInBottomBar }.forEach { screen ->
                        NavigationBarItem(
                            selected = selectedScreen == screen,
                            onClick = { selectedScreen = screen },
                            icon = {
                                Text(
                                    text = screen.label.take(1),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            label = { Text(screen.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedScreen == DemoScreen.Notes) {
                FloatingActionButton(
                    onClick = {
                        onEvent(NotesUiEvent.ClearEditor)
                        selectedScreen = DemoScreen.Editor
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (selectedScreen) {
                DemoScreen.Notes -> notesScreenContent(
                    uiState = uiState,
                    onCreateNote = {
                        onEvent(NotesUiEvent.ClearEditor)
                        selectedScreen = DemoScreen.Editor
                    },
                    onEditNote = { noteId ->
                        onEvent(NotesUiEvent.EditNote(noteId))
                        selectedScreen = DemoScreen.Editor
                    },
                    onEvent = onEvent,
                )
                DemoScreen.Editor -> editorScreenContent(
                    uiState = uiState,
                    onEvent = onEvent,
                    onDone = { selectedScreen = DemoScreen.Notes },
                )
                DemoScreen.Remote -> remoteScreenContent(
                    uiState = uiState,
                    onEvent = onEvent,
                )
                DemoScreen.Sync -> syncScreenContent(
                    uiState = uiState,
                    isOnline = isOnline,
                    onEvent = onEvent,
                )
                DemoScreen.Learn -> learnScreenContent()
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.notesScreenContent(
    uiState: NotesUiState,
    onCreateNote: () -> Unit,
    onEditNote: (Long) -> Unit,
    onEvent: (NotesUiEvent) -> Unit,
) {
    item {
        SectionTitle(
            title = "Local source of truth",
            subtitle = "${uiState.notes.size} visible note(s)",
        )
    }

    item {
        Button(
            onClick = onCreateNote,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Text("Create local note")
        }
    }

    if (uiState.notes.isEmpty()) {
        item {
            EmptyNotesState()
        }
    } else {
        items(uiState.notes, key = { it.id }) { note ->
            NoteListItem(
                note = note,
                onClick = { onEditNote(note.id) },
                onDelete = { onEvent(NotesUiEvent.DeleteNote(note.id)) },
                onKeepLocal = { onEvent(NotesUiEvent.KeepLocalConflict(note.id)) },
                onUseRemote = { onEvent(NotesUiEvent.UseRemoteConflict(note.id)) },
                onMergeBoth = { onEvent(NotesUiEvent.MergeBothConflict(note.id)) },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.editorScreenContent(
    uiState: NotesUiState,
    onEvent: (NotesUiEvent) -> Unit,
    onDone: () -> Unit,
) {
    item {
        SectionTitle(
            title = if (uiState.isEditing) "Edit local note" else "New local note",
            subtitle = "The app saves here first, then sync moves it to remote.",
        )
        NoteEditor(
            isEditing = uiState.isEditing,
            title = uiState.editorTitle,
            body = uiState.editorBody,
            onTitleChange = { onEvent(NotesUiEvent.TitleChanged(it)) },
            onBodyChange = { onEvent(NotesUiEvent.BodyChanged(it)) },
            onSave = {
                onEvent(NotesUiEvent.SaveNote)
                onDone()
            },
            onClear = {
                onEvent(NotesUiEvent.ClearEditor)
                onDone()
            },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.remoteScreenContent(
    uiState: NotesUiState,
    onEvent: (NotesUiEvent) -> Unit,
) {
    item {
        SectionTitle(
            title = "Fake remote server",
            subtitle = "Edit this copy to create a conflict demo",
        )
    }

    if (uiState.remoteNotes.isEmpty()) {
        item {
            EmptyRemoteState()
        }
    } else {
        if (uiState.isEditingRemote) {
            item {
                RemoteEditor(
                    title = uiState.remoteEditorTitle,
                    body = uiState.remoteEditorBody,
                    onTitleChange = { onEvent(NotesUiEvent.RemoteTitleChanged(it)) },
                    onBodyChange = { onEvent(NotesUiEvent.RemoteBodyChanged(it)) },
                    onSave = { onEvent(NotesUiEvent.SaveRemoteNote) },
                    onClear = { onEvent(NotesUiEvent.ClearRemoteEditor) },
                )
            }
        }

        items(uiState.remoteNotes, key = { it.remoteId }) { note ->
            RemoteNoteCard(
                remoteId = note.remoteId,
                title = note.title,
                body = note.body,
                onEdit = { onEvent(NotesUiEvent.EditRemoteNote(note.remoteId)) },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.syncScreenContent(
    uiState: NotesUiState,
    isOnline: Boolean,
    onEvent: (NotesUiEvent) -> Unit,
) {
    item {
        OverviewPanel(
            notes = uiState.notes,
            isSyncing = uiState.isSyncing,
            isOnline = isOnline,
            lastSyncMessage = uiState.lastSyncMessage,
            onSync = { onEvent(NotesUiEvent.SyncNow(isOnline = isOnline)) },
        )
    }
    item {
        SyncStatsGrid(notes = uiState.notes)
    }
    item {
        AutoSyncPanel(
            enabled = uiState.autoBackgroundSyncEnabled,
            onEnabledChange = { onEvent(NotesUiEvent.AutoBackgroundSyncChanged(it)) },
        )
    }
    item {
        SectionTitle(
            title = "Sync trace",
            subtitle = "Recent repository events",
        )
        SyncLogPanel(entries = uiState.syncLog)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.learnScreenContent() {
    item {
        LearningCard(
            title = "1. UI reads local data",
            body = "Compose observes NotesUiState. The state comes from Room through Flow, not directly from the network.",
        )
    }
    item {
        LearningCard(
            title = "2. Writes are local first",
            body = "Create, edit, and delete actions update Room immediately with pending operations.",
        )
    }
    item {
        LearningCard(
            title = "3. Sync is a separate engine",
            body = "Manual sync and WorkManager both call the repository sync loop. The UI stays local-first.",
        )
    }
    item {
        LearningCard(
            title = "4. Conflicts are explicit",
            body = "When local and remote versions diverge, the app marks a conflict instead of silently overwriting data.",
        )
    }
    item {
        LearningCard(
            title = "5. Merge both keeps intent",
            body = "Merge both combines the local edit and remote edit into one local note, clears the conflict, and marks it pending update so the merged result is pushed on the next sync.",
        )
    }
    item {
        MergeConflictFlow()
    }
}

@Composable
private fun AutoSyncPanel(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Auto background sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (enabled) {
                        "WorkManager can sync after local writes."
                    } else {
                        "Paused so manual demos and conflicts are easy to control."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun EmptyRemoteState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No remote items yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Create a local note, go to Sync, then tap Sync pending changes. The remote copy will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RemoteEditor(
    title: String,
    body: String,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE9EEF5),
        border = BorderStroke(1.dp, Color(0xFFB8C8DA)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Edit remote copy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF285F8F),
            )
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Remote title") },
                singleLine = true,
            )
            OutlinedTextField(
                value = body,
                onValueChange = onBodyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Remote body") },
                minLines = 4,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClear) {
                    Text("Cancel")
                }
                Button(onClick = onSave) {
                    Text("Save remote edit")
                }
            }
        }
    }
}

@Composable
private fun RemoteNoteCard(
    remoteId: String,
    title: String,
    body: String,
    onEdit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFFB8C8DA)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = remoteId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(onClick = onEdit) {
                    Text("Edit remote")
                }
            }
            if (body.isNotBlank()) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OverviewPanel(
    notes: List<FieldNote>,
    isSyncing: Boolean,
    isOnline: Boolean,
    lastSyncMessage: String,
    onSync: () -> Unit,
) {
    val pendingCount = notes.count { it.pendingOperation != PendingOperation.None }
    val conflictCount = notes.count { it.syncStatus == SyncStatus.Conflict }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Offline-first control room",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = lastSyncMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusPill(
                    label = if (isOnline) "Online" else "Offline",
                    containerColor = if (isOnline) Color(0xFFE0F5E9) else Color(0xFFFFE3D7),
                    contentColor = if (isOnline) Color(0xFF0E5D42) else Color(0xFF8A3200),
                )
                StatusPill(
                    label = "$pendingCount pending",
                    containerColor = Color(0xFFFFF0C7),
                    contentColor = Color(0xFF715000),
                )
                StatusPill(
                    label = "$conflictCount conflict",
                    containerColor = if (conflictCount == 0) Color(0xFFE9EEF5) else Color(0xFFFFDAD6),
                    contentColor = if (conflictCount == 0) Color(0xFF36526F) else Color(0xFF8A1F11),
                )
            }

            Button(
                onClick = onSync,
                enabled = !isSyncing && isOnline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFFFFF),
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color(0xFF0F5A47),
                    disabledContentColor = Color(0xFF9CCDBF),
                ),
            ) {
                Text(
                    text = when {
                        isSyncing -> "Syncing changes..."
                        !isOnline -> "Connect internet to sync"
                        else -> "Sync pending changes"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SyncStatsGrid(notes: List<FieldNote>) {
    val synced = notes.count { it.syncStatus == SyncStatus.Synced }
    val pending = notes.count { it.pendingOperation != PendingOperation.None }
    val conflicts = notes.count { it.syncStatus == SyncStatus.Conflict }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = "Sync dashboard",
            subtitle = "Current local database state",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                label = "Synced",
                value = synced.toString(),
                color = Color(0xFFE0F5E9),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Pending",
                value = pending.toString(),
                color = Color(0xFFFFF0C7),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Conflict",
                value = conflicts.toString(),
                color = Color(0xFFFFDAD6),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LearningCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MergeConflictFlow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5FAF7),
        border = BorderStroke(1.dp, Color(0xFFBFD8CC)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Merge conflict flow",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0E5D42),
            )
            FlowStep(
                title = "1. Local edit",
                body = "User changes the Room copy while offline or before syncing.",
            )
            FlowArrow()
            FlowStep(
                title = "2. Remote edit",
                body = "The server copy changed too, so versions no longer match.",
            )
            FlowArrow()
            FlowStep(
                title = "3. Conflict",
                body = "Sync stops for that note and asks the user to choose.",
            )
            FlowArrow()
            FlowStep(
                title = "4. Merge both",
                body = "The app creates one combined local note, then pushes that merged note on the next sync.",
            )
        }
    }
}

@Composable
private fun FlowStep(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color(0xFFD7E6DE)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0E5D42),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FlowArrow() {
    Text(
        text = "v",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0E5D42),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun NoteEditor(
    isEditing: Boolean,
    title: String,
    body: String,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    val canSave = title.isNotBlank() || body.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isEditing) "Edit note" else "New field note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                StatusPill(
                    label = if (isEditing) "Local edit" else "Local draft",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.secondary,
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
            )
            OutlinedTextField(
                value = body,
                onValueChange = onBodyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Body") },
                minLines = 4,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClear) {
                    Text("Cancel")
                }
                Button(
                    onClick = onSave,
                    enabled = canSave,
                ) {
                    Text(if (isEditing) "Update local note" else "Save local note")
                }
            }
        }
    }
}

@Composable
private fun SyncLogPanel(entries: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.take(5).forEach { entry ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        modifier = Modifier.padding(top = 5.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                    Text(
                        text = entry,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotesState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                text = "No notes yet. Tap Create local note or the + button.",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoteListItem(
    note: FieldNote,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onKeepLocal: () -> Unit,
    onUseRemote: () -> Unit,
    onMergeBoth: () -> Unit,
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Delete note?",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text("This removes \"${note.title}\" from the local source of truth.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = note.title,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text("Edit")
                    }
                    TextButton(onClick = { showDeleteConfirmation = true }) {
                        Text("Delete")
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(
                label = note.syncStatus.label,
                containerColor = note.syncStatus.containerColor(),
                contentColor = note.syncStatus.contentColor(),
            )
            if (note.conflictTitle != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF1EF),
                    border = BorderStroke(1.dp, Color(0xFFFFC7C0)),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Remote version: ${note.conflictTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onKeepLocal,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Keep local")
                            }
                            OutlinedButton(
                                onClick = onMergeBoth,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Merge both")
                            }
                            Button(
                                onClick = onUseRemote,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Use remote")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatus.containerColor(): Color =
    when (this) {
        SyncStatus.Synced -> Color(0xFFE0F5E9)
        SyncStatus.PendingCreate,
        SyncStatus.PendingUpdate,
        SyncStatus.PendingDelete -> Color(0xFFFFF0C7)
        SyncStatus.Conflict -> Color(0xFFFFDAD6)
        SyncStatus.Failed -> Color(0xFFFFDAD6)
    }

@Composable
private fun SyncStatus.contentColor(): Color =
    when (this) {
        SyncStatus.Synced -> Color(0xFF0E5D42)
        SyncStatus.PendingCreate,
        SyncStatus.PendingUpdate,
        SyncStatus.PendingDelete -> Color(0xFF715000)
        SyncStatus.Conflict -> Color(0xFF8A1F11)
        SyncStatus.Failed -> Color(0xFF8A1F11)
    }

@Preview(showBackground = true)
@Composable
private fun FieldNotesPreview() {
    OfflineFirstSystemDesignTheme {
        FieldNotesScreen(
            uiState = NotesUiState(
                notes = listOf(
                    FieldNote(
                        id = 1L,
                        remoteId = "remote-1",
                        title = "Inspect storage before syncing",
                        body = "Offline-first screens should read from local state first.",
                        syncStatus = com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus.Synced,
                        pendingOperation = com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation.None,
                    ),
                ),
            ),
            isOnline = true,
            onEvent = {},
        )
    }
}
