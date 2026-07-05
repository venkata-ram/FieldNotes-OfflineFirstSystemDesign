package com.venkatsvision.offlinefirstsystemdesign

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.venkatsvision.offlinefirstsystemdesign.ui.theme.OfflineFirstSystemDesignTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OfflineFirstSystemDesignTheme {
                FieldNotesApp()
            }
        }
    }
}

private data class FieldNote(
    val id: Int,
    val title: String,
    val body: String,
    val localLabel: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldNotesApp() {
    val notes = remember {
        mutableStateListOf(
            FieldNote(
                id = 1,
                title = "Inspect storage before syncing",
                body = "Offline-first screens should read from local state first. Later milestones will replace this in-memory list with Room.",
                localLabel = "Local only",
            ),
        )
    }
    var nextId by remember { mutableIntStateOf(2) }
    var editingNote by remember { mutableStateOf<FieldNote?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    fun startNewNote() {
        editingNote = null
        title = ""
        body = ""
    }

    fun startEditing(note: FieldNote) {
        editingNote = note
        title = note.title
        body = note.body
    }

    fun saveNote() {
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        if (cleanTitle.isEmpty() && cleanBody.isEmpty()) return

        val existing = editingNote
        if (existing == null) {
            notes.add(
                0,
                FieldNote(
                    id = nextId,
                    title = cleanTitle.ifEmpty { "Untitled note" },
                    body = cleanBody,
                    localLabel = "Local only",
                ),
            )
            nextId += 1
        } else {
            val index = notes.indexOfFirst { it.id == existing.id }
            if (index >= 0) {
                notes[index] = existing.copy(
                    title = cleanTitle.ifEmpty { "Untitled note" },
                    body = cleanBody,
                    localLabel = "Edited locally",
                )
            }
        }
        startNewNote()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Field Notes", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Offline-first demo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        FieldNotesScreen(
            notes = notes,
            editingNote = editingNote,
            title = title,
            body = body,
            onTitleChange = { title = it },
            onBodyChange = { body = it },
            onSave = ::saveNote,
            onNew = ::startNewNote,
            onEdit = ::startEditing,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun FieldNotesScreen(
    notes: List<FieldNote>,
    editingNote: FieldNote?,
    title: String,
    body: String,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onNew: () -> Unit,
    onEdit: (FieldNote) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            NoteEditor(
                editingNote = editingNote,
                title = title,
                body = body,
                onTitleChange = onTitleChange,
                onBodyChange = onBodyChange,
                onSave = onSave,
                onNew = onNew,
            )
        }

        item {
            Text(
                text = "Local notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (notes.isEmpty()) {
            item {
                EmptyNotesState()
            }
        } else {
            items(notes, key = { it.id }) { note ->
                NoteListItem(note = note, onClick = { onEdit(note) })
            }
        }
    }
}

@Composable
private fun NoteEditor(
    editingNote: FieldNote?,
    title: String,
    body: String,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onNew: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (editingNote == null) "Capture a note" else "Edit note",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
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
                TextButton(onClick = onNew) {
                    Text("Clear")
                }
                Button(onClick = onSave) {
                    Text(if (editingNote == null) "Save local note" else "Update local note")
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
        Text(
            text = "No notes yet. Add one above.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoteListItem(note: FieldNote, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                OutlinedButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Text("Edit")
                }
            }
            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = note.localLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FieldNotesPreview() {
    OfflineFirstSystemDesignTheme {
        FieldNotesApp()
    }
}
