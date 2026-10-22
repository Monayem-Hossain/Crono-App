package com.monayem.crono.data

import androidx.room.Embedded
import androidx.room.Relation

data class SubjectWithClassTests(
    @Embedded val subject: Subject,
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId"
    )
    val classTestEntries: List<ClassTestEntry>
)
