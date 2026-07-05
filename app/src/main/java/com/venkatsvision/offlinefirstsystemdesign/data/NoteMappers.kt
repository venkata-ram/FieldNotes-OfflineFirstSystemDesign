package com.venkatsvision.offlinefirstsystemdesign.data

import com.venkatsvision.offlinefirstsystemdesign.data.local.NoteEntity
import com.venkatsvision.offlinefirstsystemdesign.domain.FieldNote

fun NoteEntity.toDomain(): FieldNote =
    FieldNote(
        id = localId,
        title = title,
        body = body,
        localLabel = localLabel,
    )
