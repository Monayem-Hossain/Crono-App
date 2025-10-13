package com.monayem.classschedule.data

import androidx.room.Embedded
import androidx.room.Relation

data class SubjectWithScheduleEntries(
    @Embedded val subject: Subject,
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId"
    )
    val scheduleEntries: List<ScheduleEntry>
)
