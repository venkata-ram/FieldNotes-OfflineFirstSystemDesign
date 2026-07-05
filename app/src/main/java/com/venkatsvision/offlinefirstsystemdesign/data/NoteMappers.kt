package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteEntity
import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote
import com.venkatsvision.offlinefirstsystemdesign.domain.PendingOperation
import com.venkatsvision.offlinefirstsystemdesign.domain.SyncStatus

fun NoteEntity.toDomain(): FieldNote =
    FieldNote(
        id = localId,
        title = title,
        body = body,
        syncStatus = enumValueOrDefault(syncStatus, SyncStatus.PendingCreate),
        pendingOperation = enumValueOrDefault(pendingOperation, PendingOperation.Create),
    )

private inline fun <reified T : Enum<T>> enumValueOrDefault(
    value: String,
    defaultValue: T,
): T =
    enumValues<T>().firstOrNull { it.name == value } ?: defaultValue
