package com.monayem.crono.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "class_test_entries",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ClassTestEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectId: Int,
    val date: String, // Format: dd-MM-yyyy
    val startTime: String,
    val endTime: String,
    val roomNumber: String,
    val isConfirmed: Boolean = false // To track if user confirmed it's taken
)
