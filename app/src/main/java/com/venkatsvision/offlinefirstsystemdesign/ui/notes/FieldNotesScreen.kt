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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus
import com.venkatsvision.offlinefirstsystemdesign.ui.theme.OfflineFirstSystemDesignTheme

private enum class DemoScreen(val label: String, val title: String, val subtitle: String) {
    Notes("Notes", "Field Notes", "Capture and inspect local state"),
    Sync("Sync", "Sync Control", "Push, pull, conflict, retry"),
    Learn("Learn", "Architecture", "Offline-first system design"),
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
            NavigationBar {
                DemoScreen.entries.forEach { screen ->
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
    onEvent: (NotesUiEvent) -> Unit,
) {
    item {
        SectionTitle(
            title = "Capture",
            subtitle = if (uiState.isEditing) "Editing local state" else "Write first, sync later",
        )
        NoteEditor(
            isEditing = uiState.isEditing,
            title = uiState.editorTitle,
            body = uiState.editorBody,
            onTitleChange = { onEvent(NotesUiEvent.TitleChanged(it)) },
            onBodyChange = { onEvent(NotesUiEvent.BodyChanged(it)) },
            onSave = { onEvent(NotesUiEvent.SaveNote) },
            onClear = { onEvent(NotesUiEvent.ClearEditor) },
        )
    }

    item {
        SectionTitle(
            title = "Local source of truth",
            subtitle = "${uiState.notes.size} visible note(s)",
        )
    }

    if (uiState.notes.isEmpty()) {
        item {
            EmptyNotesState()
        }
    } else {
        items(uiState.notes, key = { it.id }) { note ->
            NoteListItem(
                note = note,
                onClick = { onEvent(NotesUiEvent.EditNote(note.id)) },
                onDelete = { onEvent(NotesUiEvent.DeleteNote(note.id)) },
                onSimulateRemoteEdit = { onEvent(NotesUiEvent.SimulateRemoteEdit(note.id)) },
                onKeepLocal = { onEvent(NotesUiEvent.KeepLocalConflict(note.id)) },
                onUseRemote = { onEvent(NotesUiEvent.UseRemoteConflict(note.id)) },
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
private fun NoteEditor(
    isEditing: Boolean,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Body") },
                minLines = 4,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
                Button(onClick = onSave) {
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
                text = "No notes yet. Add one above.",
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
    onSimulateRemoteEdit: () -> Unit,
    onKeepLocal: () -> Unit,
    onUseRemote: () -> Unit,
) {
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
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = note.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        Text("Edit")
                    }
                    TextButton(onClick = onDelete) {
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onKeepLocal) {
                                Text("Keep local")
                            }
                            Button(onClick = onUseRemote) {
                                Text("Use remote")
                            }
                        }
                    }
                }
            }
            if (note.remoteId != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onSimulateRemoteEdit) {
                        Text("Simulate remote edit")
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
