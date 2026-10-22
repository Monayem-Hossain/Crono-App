package com.monayem.crono.data

import androidx.room.Embedded
import androidx.room.Relation

data class SubjectWithExams(
    @Embedded val subject: Subject,
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId"
    )
    val examEntries: List<ExamEntry>
)
